#!/usr/bin/env python3
"""Mutation gate for the JOIN stage-flow harness v5 (all three classpath modes): each mutation must
fail run_join_stage_flow.py with AssertionError and a nonzero exit. NOTE: a choose()-level
bedrock-veto-dropped mutant cannot be killed here because resolve() wiring never consults Via or
the handshake when bedrock classes are present (contract A is doubly enforced); that mutant is
gated in run_translator_hook_mutations.py, where choose() is called directly."""
from pathlib import Path
import tempfile,shutil,subprocess,sys
root=Path(__file__).resolve().parents[2]
mutations=[
 ('via-handshake-fallback','data/TranslatorHooks.java','if (viaClassPresence != Presence.ABSENT) return viaLoaded && via != null ? via : MeshVersion.UNKNOWN;','if (viaClassPresence != Presence.ABSENT) return viaLoaded && via != null ? via : handshake;'),
 ('join-resolver-removed','data/PlayerDataManager.java','\t\tif (data.getClientVersion() == dev.meshac.config.MeshVersion.UNKNOWN) {\n\t\t\tdata.setClientVersion(TranslatorHooks.resolve(player.getUUID(), connection(player)));\n\t\t}',''),
 ('null-guard-removed','data/TranslatorHooks.java','\t\tif (playerId == null) return MeshVersion.UNKNOWN;\n',''),
 ('join-stage-guard-inverted','data/PlayerDataManager.java','if (data.getClientVersion() == dev.meshac.config.MeshVersion.UNKNOWN) {','if (data.getClientVersion() != dev.meshac.config.MeshVersion.UNKNOWN) {'),
]
with tempfile.TemporaryDirectory(prefix='meshac-stageflow-mutations-') as d:
 t=Path(d);shutil.copytree(root/'src',t/'src');shutil.copytree(root/'sim',t/'sim')
 for name,path,old,new in mutations:
  p=t/'src/main/java/dev/meshac'/path;o=p.read_text();assert o.count(old)==1,(name,'pattern not unique')
  p.write_text(o.replace(old,new));r=subprocess.run([sys.executable,str(t/'sim/version/run_join_stage_flow.py')],text=True,capture_output=True)
  p.write_text(o);assert r.returncode!=0 and 'AssertionError' in r.stderr,(name,r.stderr[-1500:]);print('REJECTED:',name)
print('Join stage flow mutation checks PASS')
