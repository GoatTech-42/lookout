#!/usr/bin/env python3
"""Run real trace consumers against test-only Minecraft/logging boundaries.

No game launch. Compile fresh source into a temporary directory. Separate JVMs
are required because TRACE is static final. Logger stub prints captured arguments;
GUI is null, so this also checks that R9 evidence precedes the GUI exit.
"""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
ENGINE = Path('src/main/java/dev/lookout/core/LookoutEngine.java')
ALERTER = Path('src/main/java/dev/lookout/mc/ChatAlerter.java')
STUBS = {
    'net/minecraft/client/Minecraft.java': '''package net.minecraft.client;
public class Minecraft {
 public ObjectGui gui = null;
 public static Minecraft getInstance() { return new Minecraft(); }
 public static class ObjectGui { public Listener chatListener() { return new Listener(); } }
 public static class Listener { public void handleSystemMessage(Object c, boolean b) {} }
}''',
    'net/minecraft/network/chat/Component.java': '''package net.minecraft.network.chat;
public class Component { public static Component literal(String s) { return new Component(); } }''',
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
  } else {
   new ChatAlerter().alert(17, "PrivatePlayer", "reach", "PrivateMessage");
  }
 }
}''',
}


def evaluate(mutation=None, empty_guard=False):
    results = set()
    with tempfile.TemporaryDirectory(prefix='trace-env-') as d:
        root = Path(d)
        sources = []
        for path in sorted((ROOT / 'src/main/java/dev/lookout/core').rglob('*.java')) + [ROOT / ALERTER]:
            text = path.read_text()
            if mutation and path == ROOT / mutation:
                if empty_guard:
                    old = '!System.getenv().getOrDefault("MESHAC_TRACE", "").isEmpty()'
                    assert text.count(old) == 1
                    text = text.replace(old, 'System.getenv("MESHAC_TRACE") != null', 1)
                else:
                    assert text.count('if (TRACE)') == 1
                    text = text.replace('if (TRACE)', 'if (true)', 1)
            target = root / path.relative_to(ROOT / 'src/main/java')
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text)
            sources.append(str(target))
        for name, text in STUBS.items():
            path = root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text)
            sources.append(str(path))
        subprocess.run(['javac', '-d', str(root / 'classes'), *sources], check=True, timeout=60)
        for consumer in ('engine', 'alerter'):
            for case, value, enabled in (('unset', None, False), ('empty', '', False), ('nonempty', '1', True)):
                env = os.environ.copy()
                env.pop('MESHAC_TRACE', None)
                if value is not None:
                    env['MESHAC_TRACE'] = value
                p = subprocess.run(['java', '-cp', str(root / 'classes'), 'TraceEnvProbe', consumer],
                                   env=env, capture_output=True, text=True, check=True, timeout=10)
                if consumer == 'engine':
                    wanted = '[lookout-trace] onSwing id=17\n' if enabled else ''
                else:
                    wanted = ('LOG [lookout] {}: {} ARGS=[PrivatePlayer, PrivateMessage]\n' if enabled else
                              'LOG [lookout] alert entity={} check={} ARGS=[17, reach]\n')
                name = consumer + '-' + case
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
    if args.mutants:
        for path, expected in ((ENGINE, {'engine-unset', 'engine-empty'}),
                               (ALERTER, {'alerter-unset', 'alerter-empty'})):
            actual = evaluate(path)
            ok = actual == expected
            print(('PASS ' if ok else 'FAIL ') + 'guard-only-' + path.stem +
                  ' expected=' + repr(sorted(expected)) + ' actual=' + repr(sorted(actual)))
            if not ok:
                failures.add('mutant-' + path.stem)
            actual = evaluate(path, empty_guard=True)
            expected = {('engine' if path == ENGINE else 'alerter') + '-empty'}
            ok = actual == expected
            print(('PASS ' if ok else 'FAIL ') + 'empty-guard-only-' + path.stem +
                  ' expected=' + repr(sorted(expected)) + ' actual=' + repr(sorted(actual)))
            if not ok:
                failures.add('empty-guard-mutant-' + path.stem)
    print('SUITE: ' + ('all pass' if not failures else 'failures=' + repr(sorted(failures))))
    raise SystemExit(bool(failures))


if __name__ == '__main__':
    main()
