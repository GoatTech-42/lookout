#!/usr/bin/env python3
"""Gate: per-target accessor audit for lookout's minecraft API surface.

For every (receiver class, member) the lookout sources reference, resolve the
member against each target's mapped jar by walking the type hierarchy with
javap: receiver -> declared superclasses/interfaces until the member's
declaration is found. Output rows record receiver, member, target, declaring
class, the chain walked, and an anchored verdict; every visited javap dump is
pinned under the evidence dir. Each jar is bound by path and sha256 in the
output header.

Matching is anchored on the declaration token: a method row matches
`name(` only as a full token (getId( never satisfies id(), SPECTRAL_ARROW
never satisfies ARROW). tests/test_audit_accessors.py pins these negatives.

Gate semantics: rows marked required (the targets whose effective sources
reference the member, per the per-variant override layout) must be present;
any required absence, missing required class, or uncovered source import
exits 1. Non-required rows are inventory only.

Coverage: the receiver set is derived from the net.minecraft/com.mojang
imports and fully-qualified references in the effective lookout sources
(fail-closed: an uncovered import aborts the run); the member list per
receiver is curated from the call sites of those sources. Member-level
coverage is exactly what CHECKS lists, nothing more.

Usage: python3 scripts/audit_accessors.py [--evidence-dir DIR]
Exit 0 = all required members resolved on their required targets."""
import glob, hashlib, os, re, subprocess, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JDK = os.environ.get("AUDIT_JDK", os.path.expanduser("~/jdk25/bin"))
EVIDENCE = None
for i, a in enumerate(sys.argv):
    if a == "--evidence-dir":
        EVIDENCE = sys.argv[i + 1]

def _find(pattern):
    hits = sorted(glob.glob(os.path.expanduser(pattern)))
    if not hits:
        sys.exit("mapped jar not found: " + pattern + " (run a gradle build for this target first)")
    return hits[-1]

def jars():
    return {
        "1.21.11": _find("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/1.21.11-*/minecraft-merged-1.21.11-*.jar"),
        "26.1.2": _find("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.1.2/minecraft-merged-deobf-26.1.2.jar"),
        "26.2": _find("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/minecraft-merged-deobf-26.2.jar"),
        "26.3": _find("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3/minecraft-merged-deobf-26.3.jar"),
    }
ALL = ["1.21.11", "26.1.2", "26.2", "26.3"]

def method(name): return (name, "method")
def field(name): return (name, "field")

G = "net.minecraft.network.protocol.game."
# receiver, member, kind, required targets (effective per-variant sources)
CHECKS = [
    (G+"ClientboundAddEntityPacket", method("getType"), ALL), (G+"ClientboundAddEntityPacket", method("getMovement"), ALL),
    (G+"ClientboundAddEntityPacket", method("getX"), ALL), (G+"ClientboundAddEntityPacket", method("getY"), ALL),
    (G+"ClientboundAddEntityPacket", method("getZ"), ALL), (G+"ClientboundAddEntityPacket", method("getId"), ALL),
    (G+"ClientboundAddEntityPacket", method("getData"), ALL),
    (G+"ClientboundRemoveEntitiesPacket", method("entityIds"), ["26.3"]),
    (G+"ClientboundRemoveEntitiesPacket", method("getEntityIds"), ["1.21.11", "26.1.2", "26.2"]),
    (G+"ClientboundDamageEventPacket", method("sourceType"), ALL), (G+"ClientboundDamageEventPacket", method("entityId"), ALL),
    (G+"ClientboundDamageEventPacket", method("sourceCauseId"), ALL), (G+"ClientboundDamageEventPacket", method("sourceDirectId"), ALL),
    (G+"ClientboundAnimatePacket", method("getId"), ALL),
    (G+"ClientboundEntityEventPacket", method("getEventId"), ALL), (G+"ClientboundEntityEventPacket", method("getEntity"), ALL),
    (G+"ClientboundSetEntityMotionPacket", method("id"), ["26.1.2", "26.2", "26.3"]),
    (G+"ClientboundSetEntityMotionPacket", method("getId"), ["1.21.11"]),
    (G+"ClientboundTeleportEntityPacket", method("id"), ALL),
    (G+"ClientboundEntityPositionSyncPacket", method("id"), ALL),
    (G+"ClientboundSetEquipmentPacket", method("getSlots"), ALL), (G+"ClientboundSetEquipmentPacket", method("getEntity"), ALL),
    ("net.minecraft.world.entity.EntityTypes", field("ARROW"), ["26.2", "26.3"]),
    ("net.minecraft.world.entity.EntityTypes", field("SPECTRAL_ARROW"), ["26.2", "26.3"]),
    ("net.minecraft.world.entity.EntityType", field("ARROW"), ["1.21.11", "26.1.2"]),
    ("net.minecraft.world.entity.EntityType", field("SPECTRAL_ARROW"), ["1.21.11", "26.1.2"]),
    ("net.minecraft.world.entity.EquipmentSlot", field("OFFHAND"), ALL), ("net.minecraft.world.entity.EquipmentSlot", field("FEET"), ALL),
    ("net.minecraft.world.entity.Pose", field("SWIMMING"), ALL),
    ("net.minecraft.world.item.Items", field("TOTEM_OF_UNDYING"), ALL), ("net.minecraft.world.item.Items", field("BOW"), ALL),
    ("net.minecraft.world.item.ItemStack", method("is"), ALL), ("net.minecraft.world.item.ItemStack", method("hasFoil"), ALL),
    ("net.minecraft.world.item.ItemStack", method("get"), ALL),
    ("net.minecraft.core.Holder", method("unwrapKey"), ALL),
    ("net.minecraft.resources.ResourceKey", method("identifier"), ALL),
    ("net.minecraft.client.Minecraft", method("getInstance"), ALL), ("net.minecraft.client.Minecraft", field("gui"), ALL),
    ("net.minecraft.client.Minecraft", field("level"), ALL), ("net.minecraft.client.Minecraft", field("player"), ALL),
    ("net.minecraft.client.Minecraft", method("getConnection"), ALL),
    ("net.minecraft.client.gui.Gui", method("chatListener"), ["26.2", "26.3"]),
    ("net.minecraft.client.gui.Gui", method("getChat"), ["1.21.11", "26.1.2"]),
    ("net.minecraft.client.multiplayer.chat.ChatListener", method("handleSystemMessage"), ["26.2", "26.3"]),
    ("net.minecraft.client.gui.components.ChatComponent", method("addClientSystemMessage"), ["26.1.2"]),
    ("net.minecraft.client.gui.components.ChatComponent", method("addMessage"), ["1.21.11"]),
    ("net.minecraft.network.chat.Component", method("literal"), ALL),
    ("net.minecraft.client.multiplayer.ClientLevel", method("isLoaded"), ALL),
    ("net.minecraft.client.multiplayer.ClientLevel", method("getBlockState"), ALL),
    ("net.minecraft.client.multiplayer.ClientLevel", method("getFluidState"), ALL),
    ("net.minecraft.client.multiplayer.ClientLevel", method("entitiesForRendering"), ALL),
    ("net.minecraft.client.multiplayer.ClientPacketListener", method("getPlayerInfo"), ALL),
    ("net.minecraft.client.multiplayer.PlayerInfo", method("getGameMode"), ALL),
    ("net.minecraft.core.BlockPos", method("containing"), ALL),
    ("net.minecraft.world.level.block.Blocks", field("AIR"), ALL),
    ("net.minecraft.world.level.block.Block", method("defaultBlockState"), ALL),
    ("net.minecraft.world.level.block.state.BlockState", method("getCollisionShape"), ALL),
    ("net.minecraft.world.level.block.state.BlockState", method("is"), ALL),
    ("net.minecraft.world.level.block.state.BlockState", method("getBlock"), ALL),
    ("net.minecraft.world.level.material.FluidState", method("isEmpty"), ALL),
    ("net.minecraft.tags.BlockTags", field("CLIMBABLE"), ALL), ("net.minecraft.tags.BlockTags", field("ICE"), ALL),
    ("net.minecraft.tags.BlockTags", field("SOUL_SPEED_BLOCKS"), ALL),
    ("net.minecraft.world.level.block.StairBlock", None, ALL),
    ("net.minecraft.world.level.block.SlabBlock", None, ALL),
    ("net.minecraft.world.level.block.WebBlock", None, ALL),
    ("net.minecraft.world.effect.MobEffects", field("JUMP_BOOST"), ALL), ("net.minecraft.world.effect.MobEffects", field("SPEED"), ALL),
    ("net.minecraft.world.effect.MobEffects", field("LEVITATION"), ALL), ("net.minecraft.world.effect.MobEffects", field("SLOW_FALLING"), ALL),
    ("net.minecraft.world.effect.MobEffectInstance", method("getAmplifier"), ALL),
    ("net.minecraft.core.component.DataComponents", field("USE_EFFECTS"), ALL),
    ("net.minecraft.world.item.component.UseEffects", None, ALL),
    ("net.minecraft.world.entity.Entity", method("getId"), ALL), ("net.minecraft.world.entity.Entity", method("getUUID"), ALL),
    ("net.minecraft.world.entity.Entity", method("getX"), ALL), ("net.minecraft.world.entity.Entity", method("getY"), ALL),
    ("net.minecraft.world.entity.Entity", method("getZ"), ALL), ("net.minecraft.world.entity.Entity", method("isSprinting"), ALL),
    ("net.minecraft.world.entity.Entity", method("getPose"), ALL), ("net.minecraft.world.entity.Entity", method("getVehicle"), ALL),
    ("net.minecraft.world.entity.Entity", method("isInWater"), ALL), ("net.minecraft.world.entity.Entity", method("isSpectator"), ALL),
    ("net.minecraft.world.entity.Entity", method("getYRot"), ALL), ("net.minecraft.world.entity.Entity", method("getXRot"), ALL),
    ("net.minecraft.client.player.RemotePlayer", method("getGameProfile"), ALL),
    ("net.minecraft.client.player.RemotePlayer", method("getEffect"), ALL),
    ("net.minecraft.client.player.RemotePlayer", method("hasEffect"), ALL),
    ("net.minecraft.client.player.RemotePlayer", method("isUsingItem"), ALL),
    ("net.minecraft.client.player.RemotePlayer", method("getUseItem"), ALL),
    ("net.minecraft.client.player.RemotePlayer", method("getItemBySlot"), ALL),
    ("net.minecraft.client.player.RemotePlayer", method("isFallFlying"), ALL),
    ("net.minecraft.world.phys.Vec3", method("length"), ALL),
    ("net.minecraft.world.phys.shapes.VoxelShape", method("isEmpty"), ALL),
    ("net.minecraft.world.damagesource.DamageType", None, ALL),
    ("net.minecraft.world.level.GameType", None, ALL),
]

# library surface: authlib/datafixers ship as their own versioned jars, not
# inside the mapped minecraft jar. Verified against every cached library jar
# (path + sha256 printed); the claim is per-library, not per-target bound.
LIB_CHECKS = [
    ("com.mojang.authlib.GameProfile", method("name")),
    ("com.mojang.datafixers.util.Pair", method("getFirst")),
    ("com.mojang.datafixers.util.Pair", method("getSecond")),
]
LIB_GLOBS = ["~/.gradle/caches/modules-2/files-2.1/com.mojang/authlib/*/*/authlib-*.jar",
             "~/.gradle/caches/modules-2/files-2.1/com.mojang/datafixerupper/*/*/datafixerupper-*.jar"]

def method_re(name): return re.compile(r"(?<![\w$])" + re.escape(name) + r"\s*\(")
def field_re(name): return re.compile(r"(?<![\w$])" + re.escape(name) + r"(?:\s*=[^;]*)?;")

_dump_cache = {}
def dump(jar, cls):
    key = (jar, cls)
    if key not in _dump_cache:
        r = subprocess.run([JDK + "/javap", "-cp", jar, cls], capture_output=True, text=True)
        out = r.stdout if (r.returncode == 0 and "class not found" not in r.stderr) else None
        _dump_cache[key] = out
        if EVIDENCE and out is not None:
            d = os.path.join(EVIDENCE, os.path.basename(jar).split("/")[-1])
            os.makedirs(d, exist_ok=True)
            with open(os.path.join(d, cls + ".txt"), "w") as f:
                f.write(out)
    return _dump_cache[key]

DECL = re.compile(r"(?:class|interface|enum)\s+([\w.$]+)(?:\s+extends\s+([\w.$]+(?:<[^>]*>)?))?(?:\s+implements\s+([^\{]+))?")

def supertypes(decl_line):
    m = DECL.search(decl_line)
    if not m: return []
    out = []
    if m.group(2): out.append(m.group(2).split("<")[0])
    if m.group(3):
        out += [p.strip().split("<")[0] for p in m.group(3).split(",")]
    return out

def resolve(jar, cls, member, kind):
    """Walk cls and its supertypes until member's declaration is found.
    Returns (found, declaring_class, chain-as-string)."""
    chain = []
    todo = [cls]
    seen = set()
    rx = method_re(member) if kind == "method" else field_re(member)
    while todo:
        cur = todo.pop(0)
        if cur in seen or cur.startswith("java."):
            continue
        seen.add(cur)
        out = dump(jar, cur)
        if out is None:
            return (False, None, "->".join(chain + [cur + "(CLASS-ABSENT)"]))
        chain.append(cur)
        lines = out.splitlines()
        if any(rx.search(l) for l in lines[1:]):
            return (True, cur, "->".join(chain))
        decl = next((l for l in lines if re.search(r"(?:class|interface|enum)\s", l)), "")
        todo += supertypes(decl)
    return (False, None, "->".join(chain))

def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()

def source_imports():
    """All net.minecraft/com.mojang classes referenced by the effective lookout
    sources of any variant (imports + fully-qualified references)."""
    files = glob.glob(os.path.join(ROOT, "src/main/java/dev/lookout/**/*.java"), recursive=True)
    files += glob.glob(os.path.join(ROOT, "src/mc*/java/dev/lookout/**/*.java"), recursive=True)
    classes = set()
    for path in files:
        text = open(path).read()
        classes.update(re.findall(r"^import\s+(net\.minecraft[\w.$]+|com\.mojang[\w.$]+);", text, re.M))
        classes.update(re.findall(r"(net\.minecraft(?:\.\w+)+|com\.mojang(?:\.\w+)+)", text))
    # drop package-only fragments: keep tokens whose last segment starts uppercase
    return {c for c in classes if c.rsplit(".", 1)[-1][:1].isupper()}

def main():
    JARS = jars()
    fails = []
    receivers = {c for c, _, _ in CHECKS} | {c for c, _ in LIB_CHECKS}
    for cls in sorted(source_imports()):
        if cls not in receivers:
            print(f"COVERAGE-FAIL {cls} referenced by sources but not in CHECKS")
            fails.append("coverage")

    for t, jar in JARS.items():
        print(f"# jar {t} path={jar}")
        print(f"# jar {t} sha256={sha256(jar)}")

    for cls, mem, targets in CHECKS:
        for t in ALL:
            if mem is None:
                out = dump(JARS[t], cls)
                ok = out is not None
                req = "yes" if t in targets else "no"
                print(f"{cls},<class>,{t},{'' if ok else 'ABSENT'},,{'class-present' if ok else 'CLASS-ABSENT'},{req}")
                if not ok and t in targets:
                    fails.append(f"{cls}@{t}")
                continue
            name, kind = mem
            found, declaring, chain = resolve(JARS[t], cls, name, kind)
            req = "yes" if t in targets else "no"
            print(f"{cls},{name},{t},{declaring or ''},{chain},{'V' if found else 'ABSENT'},{req}")
            if not found and t in targets:
                fails.append(f"{cls}.{name}@{t}")

    lib_jars = []
    for g in LIB_GLOBS:
        lib_jars += glob.glob(os.path.expanduser(g))
    for jar in sorted(set(lib_jars)):
        print(f"# libjar path={jar}")
        print(f"# libjar sha256={sha256(jar)}")
        for cls, (name, kind) in LIB_CHECKS:
            if dump(jar, cls) is None:
                continue  # this library jar does not carry the class; not its surface
            found, declaring, chain = resolve(jar, cls, name, kind)
            print(f"{cls},{name},{os.path.basename(jar)},{declaring or ''},{chain},{'V' if found else 'ABSENT'},yes")
            if not found:
                fails.append(f"{cls}.{name}@{os.path.basename(jar)}")
    for cls, _ in LIB_CHECKS:
        if not any(dump(j, cls) is not None for j in lib_jars):
            print(f"LIB-FAIL {cls} not found in any cached library jar")
            fails.append(cls)

    print("GATE:", "PASS" if not fails else "FAIL " + str(sorted(set(fails))))
    sys.exit(1 if fails else 0)

if __name__ == "__main__":
    main()
