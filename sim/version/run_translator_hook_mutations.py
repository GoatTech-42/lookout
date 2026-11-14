#!/usr/bin/env python3
"""Mutation gate for run_translator_hook_contract.py (v5 A/B/C): each mutation must fail the
contract with AssertionError and a nonzero exit (both enforced here)."""
from pathlib import Path
import tempfile,shutil,subprocess,sys
root=Path(__file__).resolve().parents[2]
mutations=[
 ('bedrock-presence-veto-dropped','data/TranslatorHooks.java','if (bedrockPresence != Presence.ABSENT) return MeshVersion.UNKNOWN;','if (false) return MeshVersion.UNKNOWN;'),
 ('via-line-dropped','data/TranslatorHooks.java','if (viaClassPresence != Presence.ABSENT) return viaLoaded && via != null ? via : MeshVersion.UNKNOWN;','if (false) return viaLoaded && via != null ? via : MeshVersion.UNKNOWN;'),
 ('via-handshake-fallback','data/TranslatorHooks.java','if (viaClassPresence != Presence.ABSENT) return viaLoaded && via != null ? via : MeshVersion.UNKNOWN;','if (viaClassPresence != Presence.ABSENT) return viaLoaded && via != null ? via : handshake;'),
 ('broken-treated-absent','data/TranslatorHooks.java','logLookupError("optional translator class " + className + " failed to load", broken);\n\t\t\treturn Presence.BROKEN;','logLookupError("optional translator class " + className + " failed to load", broken);\n\t\t\treturn Presence.ABSENT;'),
 ('protocol-47-mismapped','config/MeshVersion.java','\t\t\tcase 47 -> V_1_8;','\t\t\tcase 47 -> V_1_9;'),
 ('protocol-fail-open','config/MeshVersion.java','\t\t\tcase 777 -> V_26_3;\n\t\t\tdefault -> UNKNOWN;','\t\t\tcase 777 -> V_26_3;\n\t\t\tdefault -> V_26_3;'),
]
with tempfile.TemporaryDirectory(prefix='meshac-hook-mutations-') as d:
 t=Path(d);shutil.copytree(root/'src',t/'src');shutil.copytree(root/'sim',t/'sim')
 for name,path,old,new in mutations:
  p=t/'src/main/java/dev/meshac'/path;o=p.read_text();assert o.count(old)==1,(name,'pattern not unique')
  p.write_text(o.replace(old,new));r=subprocess.run([sys.executable,str(t/'sim/version/run_translator_hook_contract.py')],text=True,capture_output=True)
  p.write_text(o);assert r.returncode!=0 and 'AssertionError' in r.stderr,(name,r.stderr[-1500:]);print('REJECTED:',name)
print('Translator hook mutation checks PASS')
