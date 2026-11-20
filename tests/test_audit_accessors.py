#!/usr/bin/env python3
"""Matcher fixtures for scripts/audit_accessors.py (QA AMEND 2026-11-20).

Raw substring matching produced false positives: SPECTRAL_ARROW satisfied a
check for ARROW. The anchored matchers must accept the exact declaration
token and reject lookalikes. Synthetic javap text only; no jars needed.
Run from the repo root: python3 tests/test_audit_accessors.py"""
import importlib.util, os, sys

spec = importlib.util.spec_from_file_location(
    "audit_accessors",
    os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "scripts", "audit_accessors.py"))
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

fails = []
def expect(name, ok):
    print(("PASS " if ok else "FAIL ") + name)
    if not ok: fails.append(name)

m_arrow = mod.field_re("ARROW")
expect("ARROW matches exact declaration",
       m_arrow.search("public static final net.minecraft.world.entity.EntityType ARROW;") is not None)
expect("ARROW matches initialized constant",
       m_arrow.search('public static final int ARROW = 3;') is not None)
expect("ARROW does not match SPECTRAL_ARROW",
       m_arrow.search("public static final net.minecraft.world.entity.EntityType SPECTRAL_ARROW;") is None)
expect("ARROW does not match ARROW_DAMAGE",
       m_arrow.search("public static final int ARROW_DAMAGE;") is None)

m_id = mod.method_re("id")
expect("id( matches exact declaration",
       m_id.search("public int id();") is not None)
expect("id( does not match getId(",
       m_id.search("public int getId();") is None)
expect("id( does not match entityIds(",
       m_id.search("public int[] entityIds();") is None)

m_get = mod.method_re("get")
expect("get( matches exact declaration",
       m_get.search("public <T> T get(net.minecraft.core.component.DataComponentType<? extends T>);") is not None)
expect("get( does not match getId(",
       m_get.search("public int getId();") is None)
expect("get( does not match getEntityIds(",
       m_get.search("public int[] getEntityIds();") is None)

# supertypes parsing
sup = mod.supertypes("public class net.minecraft.client.multiplayer.ClientLevel extends net.minecraft.world.level.Level implements net.minecraft.client.multiplayer.CacheSlot$Cleaner<net.minecraft.client.multiplayer.ClientLevel> {")
expect("chain parses superclass", "net.minecraft.world.level.Level" in sup)
expect("chain strips generics from interfaces", "net.minecraft.client.multiplayer.CacheSlot$Cleaner" in sup)

print("SUITE:", "all pass" if not fails else f"failures {fails}")
sys.exit(1 if fails else 0)
