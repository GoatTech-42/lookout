#!/usr/bin/env python3
"""Run real trace consumers against test-only Minecraft/logging boundaries.

No game launch. Compile fresh source into a temporary directory. Separate JVMs
are required because TRACE is static final. Test-only logger captures arguments;
null and present GUI routes distinguish logs from the local named chat line.
Stats default/on cases exercise the real tick path with a test world boundary.
"""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
ENGINE = Path('src/main/java/dev/lookout/core/LookoutEngine.java')
ALERTER = Path('src/main/java/dev/lookout/mc/ChatAlerter.java')
# The chat route differs by variant (javap-verified): 26.3 uses
# Gui.chatListener().handleSystemMessage, 26.1.2 uses Gui.getChat().addClientSystemMessage,
# 1.21.11 uses Gui.getChat().addMessage (its ChatComponent has no addClientSystemMessage).
# Each variant's alerter is compiled against a Gui stub shaped like its own target jar.
VARIANT_ALERTERS = {
    '26.3': (ALERTER, Path('src/main/java')),
    '26.1.2': (Path('src/mc26.1.2/java/dev/lookout/mc/ChatAlerter.java'), Path('src/mc26.1.2/java')),
    '1.21.11': (Path('src/mc1.21.11/java/dev/lookout/mc/ChatAlerter.java'), Path('src/mc1.21.11/java')),
}
GUI_STUBS = {
    '26.3': ''' public static class ObjectGui { public Listener chatListener() { return new Listener(); } }
 public static class Listener { public void handleSystemMessage(Object c, boolean b) { System.out.println("CHAT " + c); } }''',
    '26.1.2': ''' public static class ObjectGui { public ChatComp getChat() { return new ChatComp(); } }
 public static class ChatComp { public void addClientSystemMessage(Object c) { System.out.println("CHAT " + c); } }''',
    '1.21.11': ''' public static class ObjectGui { public ChatComp getChat() { return new ChatComp(); } }
 public static class ChatComp { public void addMessage(Object c) { System.out.println("CHAT " + c); } }''',
}
def make_stubs(variant):
    return {
    'net/minecraft/client/Minecraft.java': '''package net.minecraft.client;
public class Minecraft {
 public ObjectGui gui = Boolean.getBoolean("probe.chat") ? new ObjectGui() : null;
 public static Minecraft getInstance() { return new Minecraft(); }
''' + GUI_STUBS[variant] + '''
}''',
    'net/minecraft/network/chat/Component.java': '''package net.minecraft.network.chat;
public class Component { private String text; private Component(String s) { text=s; } public String toString() { return text; } public static Component literal(String s) { return new Component(s); } }''',
    'org/slf4j/Logger.java': '''package org.slf4j;
public interface Logger { void info(String format, Object... args); }''',
    'org/slf4j/LoggerFactory.java': '''package org.slf4j;
public class LoggerFactory {
 public static Logger getLogger(String name) {
  return (format, args) -> System.out.println("LOG " + format + " ARGS=" + java.util.Arrays.toString(args));
 }
}''',
    'TraceEnvProbe.java': '''import dev.lookout.core.*;
import dev.lookout.mc.ChatAlerter;
public class TraceEnvProbe {
 public static void main(String[] args) {
  if (args[0].equals("engine")) {
   new LookoutEngine(new LookoutConfig(), (id, name, check, message) -> {}).onSwing(17);
  } else if (args[0].startsWith("stats")) {
   LookoutConfig c = new LookoutConfig();
   if (args[0].equals("stats-on")) c.teleportJumpStats = true;
   LookoutEngine engine = new LookoutEngine(c, (id, name, check, message) -> {});
   WorldProbe probe = (WorldProbe) java.lang.reflect.Proxy.newProxyInstance(WorldProbe.class.getClassLoader(), new Class[]{WorldProbe.class}, (o,m,a) -> false);
   for (int t=0; t<2; t++) engine.tick(17, "PrivatePlayer", new ObservedSample(t,t*3,64,0,true,false,false,false,false,false,false,false,false,0,0,false,false,0f,0f,false),probe);
  } else {
   new ChatAlerter().alert(17, "PrivatePlayer", "reach", "PrivateMessage");
  }
 }
}''',
}


def evaluate(mutation=None, empty_guard=False, privacy=None, variant='26.3'):
    alerter_path, alerter_root = VARIANT_ALERTERS[variant]
    results = set()
    with tempfile.TemporaryDirectory(prefix='trace-env-') as d:
        root = Path(d)
        sources = []
        for path in sorted((ROOT / 'src/main/java/dev/lookout/core').rglob('*.java')) + [ROOT / alerter_path]:
            text = path.read_text()
            if mutation and path == ROOT / mutation:
                if empty_guard:
                    old = '!System.getenv().getOrDefault("MESHAC_TRACE", "").isEmpty()'
                    assert text.count(old) == 1
                    text = text.replace(old, 'System.getenv("MESHAC_TRACE") != null', 1)
                else:
                    assert text.count('if (TRACE)') == 1
                    text = text.replace('if (TRACE)', 'if (true)', 1)
            privacy_edits = {
                'named-alert': (alerter_path, 'LOGGER.info("[lookout] trace alert entity={} check={}", entityId, checkId);', 'LOGGER.info("[lookout] {}: {}", name, message);'),
                'named-stats': (ENGINE, '"[lookout-stats] entity=" + entityId', '"[lookout-stats] " + p.name'),
                'stats-always-on': (ENGINE, 'if (config.teleportJumpStats', 'if (true'),
            }
            if privacy:
                which, old, new = privacy_edits[privacy]
                if path == ROOT / which:
                    assert text.count(old) == 1
                    text = text.replace(old, new, 1)
            src_root = alerter_root if 'src/mc' in str(path) else Path('src/main/java')
            target = root / path.relative_to(ROOT / src_root)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text)
            sources.append(str(target))
        for name, text in make_stubs(variant).items():
            path = root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text)
            sources.append(str(path))
        subprocess.run(['javac', '-d', str(root / 'classes'), *sources], check=True, timeout=60)
        for consumer in ('engine', 'alerter', 'stats-default', 'stats-on', 'chat'):
            for case, value, enabled in (('unset', None, False), ('empty', '', False), ('nonempty', '1', True)):
                env = os.environ.copy()
                env.pop('MESHAC_TRACE', None)
                if value is not None:
                    env['MESHAC_TRACE'] = value
                p = subprocess.run(['java', '-cp', str(root / 'classes'), '-Dprobe.chat=' + str(consumer == 'chat').lower(), 'TraceEnvProbe', consumer],
                                   env=env, capture_output=True, text=True, check=True, timeout=10)
                if consumer == 'engine':
                    wanted = '[lookout-trace] onSwing id=17\n' if enabled else ''
                elif consumer.startswith('stats'):
                    wanted = None
                else:
                    wanted = ('LOG [lookout] trace alert entity={} check={} ARGS=[17, reach]\n' if enabled else
                              'LOG [lookout] alert entity={} check={} ARGS=[17, reach]\n')
                if consumer == 'chat':
                    wanted += 'CHAT [lookout] PrivatePlayer: PrivateMessage\n'
                name = variant + '-' + consumer + '-' + case
                if consumer.startswith('stats'):
                    ok = ('PrivatePlayer' not in p.stdout and 'PrivateMessage' not in p.stdout and
                          (('[lookout-stats] entity=17 ' in p.stdout) if consumer == 'stats-on' else p.stdout == '') and p.stderr == '')
                else:
                    ok = p.stdout == wanted and p.stderr == ''
                print(('PASS ' if ok else 'FAIL ') + name)
                if not ok:
                    results.add(name)
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mutants', action='store_true')
    args = parser.parse_args()
    failures = evaluate()
    failures |= evaluate(variant='26.1.2')
    failures |= evaluate(variant='1.21.11')
    if args.mutants:
        for variant in ('26.3', '26.1.2', '1.21.11'):
            variant_alerter = VARIANT_ALERTERS[variant][0]
            for path, expected in ((ENGINE, {variant + '-engine-unset', variant + '-engine-empty'}),
                                   (variant_alerter, {variant + '-alerter-unset', variant + '-alerter-empty',
                                                      variant + '-chat-unset', variant + '-chat-empty'})):
                actual = evaluate(path, variant=variant)
                ok = actual == expected
                print(('PASS ' if ok else 'FAIL ') + variant + '-guard-only-' + path.stem +
                      ' expected=' + repr(sorted(expected)) + ' actual=' + repr(sorted(actual)))
                if not ok:
                    failures.add(variant + '-mutant-' + path.stem)
                actual = evaluate(path, empty_guard=True, variant=variant)
                expected = {variant + '-engine-empty'} if path == ENGINE else {variant + '-alerter-empty', variant + '-chat-empty'}
                ok = actual == expected
                print(('PASS ' if ok else 'FAIL ') + variant + '-empty-guard-only-' + path.stem +
                      ' expected=' + repr(sorted(expected)) + ' actual=' + repr(sorted(actual)))
                if not ok:
                    failures.add(variant + '-empty-guard-mutant-' + path.stem)
            for mutant, expected in (
                ('named-alert', {variant + '-alerter-nonempty', variant + '-chat-nonempty'}),
                ('named-stats', {variant + '-stats-on-unset', variant + '-stats-on-empty', variant + '-stats-on-nonempty'}),
                ('stats-always-on', {variant + '-stats-default-unset', variant + '-stats-default-empty', variant + '-stats-default-nonempty'})):
                actual = evaluate(privacy=mutant, variant=variant)
                ok = actual == expected
                print(('PASS ' if ok else 'FAIL ') + variant + '-' + mutant + ' exact=' + repr(sorted(actual)))
                if not ok:
                    failures.add(variant + '-' + mutant)
    print('SUITE: ' + ('all pass' if not failures else 'failures=' + repr(sorted(failures))))
    raise SystemExit(bool(failures))


if __name__ == '__main__':
    main()
