#!/usr/bin/env python3
"""Mutation gate for the JOIN stage-flow harness (both classpath modes): each mutation must fail
run_join_stage_flow.py with AssertionError and a nonzero exit."""
from pathlib import Path
import tempfile,shutil,subprocess,sys
root=Path(__file__).resolve().parents[2]
mutations=[
 ('join-stage-provisional-flag','data/PlayerDataManager.java','data.setClientVersion(TranslatorHooks.resolve(player.getUUID(), connection(player), false));','data.setClientVersion(TranslatorHooks.resolve(player.getUUID(), connection(player), true));'),
 ('join-stage-guard-inverted','data/PlayerDataManager.java','if (data.getClientVersion() == dev.meshac.config.MeshVersion.UNKNOWN) {','if (data.getClientVersion() != dev.meshac.config.MeshVersion.UNKNOWN) {'),
 ('provisional-ignores-via-class','data/TranslatorHooks.java','\t\t\tif (provisional && (bedrockPresence != Presence.ABSENT || viaClassPresence != Presence.ABSENT)) {','\t\t\tif (provisional && (bedrockPresence != Presence.ABSENT || viaPresence != Presence.ABSENT)) {'),
 ('join-stage-return-after-init','data/PlayerDataManager.java','\t\t\tonJoin(player);\n\t\t\tdata = get(player);\n\t\t\tif (data == null || data.getPlayer() != player) return;','\t\t\tonJoin(player);\n\t\t\treturn;'),
]
with tempfile.TemporaryDirectory(prefix='meshac-stageflow-mutations-') as d:
 t=Path(d);shutil.copytree(root/'src',t/'src');shutil.copytree(root/'sim',t/'sim')
 for name,path,old,new in mutations:
  p=t/'src/main/java/dev/meshac'/path;o=p.read_text();assert o.count(old)==1,(name,'pattern not unique')
  p.write_text(o.replace(old,new));r=subprocess.run([sys.executable,str(t/'sim/version/run_join_stage_flow.py')],text=True,capture_output=True)
  p.write_text(o);assert r.returncode!=0 and 'AssertionError' in r.stderr,(name,r.stderr[-1500:]);print('REJECTED:',name)
print('Join stage flow mutation checks PASS')
