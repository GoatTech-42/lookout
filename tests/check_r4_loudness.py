#!/usr/bin/env python3
"""R4 loudness gate: an empty damage-type unwrap must LOG LOUDLY at the handler site.

Silent failure mode (arch shim risk R4): if a versioned binding renames the unwrap
path, unwrapKey() yields an empty Optional, typeKey falls back to "", every
type-gated check (airTicks) starves, and nothing says why. The handler must warn.
Mutant: strip the warn (key.isEmpty() block emptied) -> this test FAILS."""
import sys

MIXIN = "src/main/java/dev/lookout/mixin/ClientPacketListenerMixin.java"
src = open(MIXIN).read()
errors = []
handler = src.split("onClientboundDamageEvent", 1)[-1]

def block_after(marker, text):
    i = text.find(marker)
    if i == -1: return None
    j = text.find("{", i)
    if j == -1: return None
    depth, k = 1, j + 1
    while k < len(text) and depth:
        if text[k] == "{": depth += 1
        elif text[k] == "}": depth -= 1
        k += 1
    return text[j:k]

if "unwrapKey();" not in handler:
    errors.append("handler no longer unwraps the damage-type key explicitly")
body = block_after("if (key.isEmpty())", handler)
if body is None:
    errors.append("no key.isEmpty() branch in the handler")
elif "warn" not in body:
    errors.append("empty unwrap is silent: no warn in the key.isEmpty() branch")
if 'orElse("")' not in handler:
    errors.append("empty fallback changed; the empty string must stay the only silent value")
warn_pos = handler.find("warn"); orelse_pos = handler.find('orElse("")')
if warn_pos == -1 or orelse_pos == -1 or warn_pos > orelse_pos:
    errors.append("warn must fire before the orElse fallback is taken")
if errors:
    print("R4 LOUDNESS GATE FAIL")
    for e in errors: print("  -", e)
    sys.exit(1)
print("R4 LOUDNESS GATE PASS: empty unwrap warns before the silent fallback")
