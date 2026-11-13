#!/usr/bin/env python3
"""Mechanical transform: Vulcan check impl -> dev.meshac.check.impl.

Same approach as the CollisionState transform: import/package remap, packetevents
wrapper -> packet record renames. Output is NOT compile-verified; files that still
reference unmapped types are listed for manual cleanup and logged to PARITY.
"""
import os
import re
import sys

SRC = '/tmp/openvulcan-src/src/main/java/me/frep/vulcan/spigot/check/impl'
DST = os.path.expanduser('~/meshac-ng/src/main/java/dev/meshac/check/impl')

IMPORT_MAP = {
    'com.github.retrooper.packetevents.event.PacketEvent': 'dev.meshac.packet.MeshPacketEvent',
    'com.github.retrooper.packetevents.event.PacketReceiveEvent': 'dev.meshac.packet.MeshPacketEvent',
    'com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction': 'dev.meshac.packet.InteractEntity.Action',
    'com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity': 'dev.meshac.packet.InteractEntity',
    'com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging': 'dev.meshac.packet.Digging',
    'com.github.retrooper.packetevents.protocol.player.DiggingAction': 'dev.meshac.packet.Digging.Action',
    'com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action': 'dev.meshac.packet.EntityAction.Action',
    'com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction': 'dev.meshac.packet.EntityAction',
    'com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange': 'dev.meshac.packet.HeldItemChange',
    'com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying': 'dev.meshac.packet.Flying',
    'com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement': 'dev.meshac.packet.Placement',
    'com.github.retrooper.packetevents.protocol.player.ClientVersion': 'dev.meshac.config.MeshVersion',
    'me.frep.vulcan.spigot.player.session.PlayerData': 'dev.meshac.data.PlayerData',
    'me.frep.vulcan.spigot.player.collision.CollisionState': 'dev.meshac.player.CollisionState',
    'me.frep.vulcan.spigot.player.movement.MotionState': 'dev.meshac.player.MotionState',
    'me.frep.vulcan.spigot.player.click.ClickTimingData': 'dev.meshac.player.ClickTimingData',
    'me.frep.vulcan.spigot.player.settings.PlayerToggles': 'dev.meshac.player.PlayerToggles',
    'me.frep.vulcan.spigot.config.VulcanSettings': 'dev.meshac.config.MeshSettings',
    'me.frep.vulcan.spigot.check.exemption.ExemptType': 'dev.meshac.check.ExemptType',
    'me.frep.vulcan.spigot.check.exemption.ExemptionManager': 'dev.meshac.check.ExemptionManager',
    'me.frep.vulcan.spigot.check.configuration.CheckUtils': 'dev.meshac.check.CheckUtils',
    'me.frep.vulcan.spigot.tracker.rotation.RotationTracker': 'dev.meshac.tracker.RotationTracker',
    'me.frep.vulcan.spigot.tracker.velocity.VelocityTracker': 'dev.meshac.tracker.VelocityTracker',
    'me.frep.vulcan.spigot.tracker.movement.RelativeMoveTracker': 'dev.meshac.tracker.RelativeMoveTracker',
    'me.frep.vulcan.spigot.tracker.connection.ConnectionTracker': 'dev.meshac.tracker.ConnectionTracker',
    'me.frep.vulcan.spigot.tracker.vehicle.VehicleTracker': 'dev.meshac.tracker.VehicleTracker',
    'me.frep.vulcan.spigot.tracker.world.WorldBlockTracker': 'dev.meshac.tracker.WorldBlockTracker',
    'me.frep.vulcan.spigot.lifecycle.PluginServices': 'dev.meshac.MeshServices',
    'me.frep.vulcan.spigot.util.numbers.MathUtils': 'dev.meshac.util.MathUtils',
    'me.frep.vulcan.spigot.util.minecraft.MaterialSets': 'dev.meshac.mcutil.MaterialSets',
    'me.frep.vulcan.spigot.collection.list.EvictingList': 'dev.meshac.collection.EvictingList',
    'me.frep.vulcan.spigot.collection.tuple.Pair': 'dev.meshac.collection.tuple.Pair',
    'me.frep.vulcan.spigot.world.block.BlockUtils': 'dev.meshac.world.BlockUtils',
    'me.frep.vulcan.spigot.data.RotationSample': 'dev.meshac.data.RotationSample',
    'me.frep.vulcan.spigot.data.PlayerDataStore': 'dev.meshac.data.PlayerDataManager',
    'me.frep.vulcan.spigot.util.scheduling.MainThreadExecutor': 'dev.meshac.MeshServices',
    'me.frep.vulcan.spigot.task.TickProcessor': 'dev.meshac.MeshServices',
    'org.bukkit.event.Event': 'dev.meshac.event.MeshEvent',
    'org.bukkit.event.block.BlockPlaceEvent': 'dev.meshac.event.MeshBlockPlaceEvent',
    'org.bukkit.event.block.BlockBreakEvent': 'dev.meshac.event.MeshBlockBreakEvent',
    'org.bukkit.event.block.Action': 'dev.meshac.event.MeshInteractEvent.Action',
    'org.bukkit.event.player.PlayerMoveEvent': 'dev.meshac.event.MeshMoveEvent',
    'org.bukkit.event.player.PlayerInteractEvent': 'dev.meshac.event.MeshInteractEvent',
    'org.bukkit.event.entity.ProjectileLaunchEvent': 'dev.meshac.event.MeshProjectileLaunchEvent',
    'org.bukkit.block.Block': 'dev.meshac.event.MeshBlock',
    'org.bukkit.block.BlockFace': 'net.minecraft.core.Direction',
    'org.bukkit.entity.Player': 'net.minecraft.server.level.ServerPlayer',
    'org.bukkit.entity.Entity': 'net.minecraft.world.entity.Entity',
    'org.bukkit.entity.Horse': 'net.minecraft.world.entity.animal.horse.Horse',
    'org.bukkit.entity.Boat': 'net.minecraft.world.entity.vehicle.Boat',
    'org.bukkit.Location': 'dev.meshac.util.Location',
    'org.bukkit.util.Vector': 'dev.meshac.util.Vector',
    'org.bukkit.World': 'net.minecraft.server.level.ServerLevel',
    'org.bukkit.GameMode': 'net.minecraft.world.level.GameType',
    'org.bukkit.entity.Camel': 'net.minecraft.world.entity.animal.camel.Camel',
    'org.bukkit.entity.Donkey': 'net.minecraft.world.entity.animal.horse.Donkey',
    'org.bukkit.entity.Mule': 'net.minecraft.world.entity.animal.horse.Mule',
    'org.bukkit.entity.Llama': 'net.minecraft.world.entity.animal.horse.Llama',
    'org.bukkit.entity.TraderLlama': 'net.minecraft.world.entity.animal.horse.TraderLlama',
    'org.bukkit.entity.SkeletonHorse': 'net.minecraft.world.entity.animal.horse.SkeletonHorse',
    'org.bukkit.entity.ZombieHorse': 'net.minecraft.world.entity.animal.horse.ZombieHorse',
    'org.bukkit.entity.Pig': 'net.minecraft.world.entity.animal.Pig',
    'org.bukkit.entity.Strider': 'net.minecraft.world.entity.monster.Strider',
    'org.bukkit.entity.Arrow': 'net.minecraft.world.entity.projectile.Arrow',
    'org.bukkit.entity.LivingEntity': 'net.minecraft.world.entity.LivingEntity',
    'org.bukkit.potion.PotionEffectType': 'net.minecraft.world.effect.MobEffects',
    'org.bukkit.attribute.Attribute': 'net.minecraft.world.entity.ai.attributes.Attributes',
    'org.bukkit.attribute.AttributeInstance': 'net.minecraft.world.entity.ai.attributes.AttributeInstance',
    'org.bukkit.enchantments.Enchantment': 'net.minecraft.world.item.enchantment.Enchantments',
    'me.frep.vulcan.spigot.geometry.ray.RaySegment': 'dev.meshac.geometry.ray.RaySegment',
    'org.bukkit.Material': 'net.minecraft.world.level.block.state.BlockState',
}

# identifier-level renames (word-boundary)
IDENT_MAP = {
    'VulcanSettings': 'MeshSettings',
    'PacketEvent': 'MeshPacketEvent',
    'WrapperPlayClientInteractEntity.InteractAction': 'InteractEntity.Action',
    'WrapperPlayClientInteractEntity': 'InteractEntity',
    'WrapperPlayClientPlayerDigging': 'Digging',
    'WrapperPlayClientEntityAction': 'EntityAction',
    'WrapperPlayClientHeldItemChange': 'HeldItemChange',
    'WrapperPlayClientPlayerFlying': 'Flying',
    'WrapperPlayClientPlayerBlockPlacement': 'Placement',
    'ClientVersion': 'MeshVersion',
    'DiggingAction': 'Digging.Action',
    'InteractAction': 'InteractEntity.Action',
    'PlayerMoveEvent': 'MeshMoveEvent',
    'BlockPlaceEvent': 'MeshBlockPlaceEvent',
    'BlockBreakEvent': 'MeshBlockBreakEvent',
    'PlayerInteractEvent': 'MeshInteractEvent',
    'ProjectileLaunchEvent': 'MeshProjectileLaunchEvent',
    'BlockFace': 'Direction',
    'Material::isOccluding': 'BlockState::canOcclude',
    'Material::isSolid': 'BlockState::isSolid',
    'Material': 'BlockState',
    'PotionEffectType': 'MobEffects',
    'Enchantment': 'Enchantments',
    'GameMode': 'GameType',
}

MANUAL_MARKERS = [
    'WrapperPlayClientSteerVehicle', 'WrapperPlayClientPlayerAbilities',
    'WrapperPlayClientKeepAlive', 'Vector3f', 'BukkitRegistry', 'RaySegment',
    'MainThreadExecutor', 'PlayerDataStore', 'Bukkit', 'org.bukkit',
    'packetevents', 'WrapperPlay',
]

def transform(path, rel):
    text = open(path).read()
    # package line
    sub = os.path.dirname(rel).replace('/', '.')
    newpkg = 'dev.meshac.check.impl' + ('.' + sub if sub else '')
    text = re.sub(r'^package\s+[\w.]+;', 'package %s;' % newpkg, text, count=1, flags=re.M)
    # imports
    def repl_import(m):
        imp = m.group(1)
        if imp in IMPORT_MAP:
            return 'import %s;' % IMPORT_MAP[imp]
        if imp.startswith('me.frep.vulcan.spigot.check.impl.'):
            rest = imp[len('me.frep.vulcan.spigot.check.impl.'):]
            return 'import dev.meshac.check.impl.%s;' % rest
        if imp.startswith('me.frep.vulcan.spigot.'):
            rest = imp[len('me.frep.vulcan.spigot.'):]
            return 'import dev.meshac.%s; // FIXME-MAP' % rest
        return m.group(0)
    text = re.sub(r'^import\s+(static\s+)?([\w.]+);',
                  lambda m: ('import %s%s;' % (m.group(1) or '', IMPORT_MAP.get(m.group(2), m.group(2))))
                  if not m.group(2).startswith('me.frep.vulcan.spigot.check.impl.')
                  else 'import %sdev.meshac.check.impl.%s;' % (m.group(1) or '', m.group(2)[len('me.frep.vulcan.spigot.check.impl.'):]),
                  text, flags=re.M)
    # structural rewrites
    text = re.sub(r'\n[^\n]*PlayerDataStore \w+ = [^;]+;', '', text)
    text = re.sub(r'\b\w+\.getPlayerData\(', 'PlayerDataManager.get(', text)
    text = re.sub(r'PluginServices\.INSTANCE\.getPlayerDataStore\(\)\.getPlayerData\(', 'PlayerDataManager.get(', text)
    text = re.sub(r'\bMainThreadExecutor\.execute\(', 'dev.meshac.MeshServices.mainThread(', text)
    text = re.sub(r'PluginServices\.INSTANCE\.isFoliaServer\(\)', 'false', text)
    text = re.sub(r'PluginServices\.INSTANCE\.isLegacyFloodgateEnabled\(\)', 'false', text)
    text = re.sub(r'PluginServices\.INSTANCE\.isFloodgateEnabled\(\)', 'false', text)
    text = re.sub(r'PluginServices\.INSTANCE\.getTickProcessor\(\)\.getTotalTicks\(\)', 'dev.meshac.MeshServices.ticks()', text)
    text = re.sub(r'PluginServices\.INSTANCE\.getHookedBoatTicks\(\)', 'dev.meshac.MeshServices.hookedBoatTicks()', text)
    text = re.sub(r'\bgetEntityId\(\)', 'getId()', text)
    text = re.sub(r'\.getInventory\(\)\.getItemInMainHand\(\)', '.getMainHandItem()', text)
    text = re.sub(r'\.getInventory\(\)\.getItemInOffHand\(\)', '.getOffhandItem()', text)
    text = re.sub(r'\.getItemInHand\(\)', '.getMainHandItem()', text)
    text = re.sub(r'\.hasPotionEffect\(', '.hasEffect(', text)
    text = re.sub(r'(\w+(?:\.\w+\(\))*)\.getEnchantmentLevel\(Enchantment\.(\w+)\)', r'dev.meshac.util.EnchantUtil.level(\1, net.minecraft.world.item.enchantment.Enchantments.\2)', text)

    # inline fully-qualified bukkit refs
    text = text.replace('org.bukkit.block.Block', 'dev.meshac.event.MeshBlock')
    text = text.replace('org.bukkit.util.Vector', 'dev.meshac.util.Vector')
    text = text.replace('org.bukkit.Location', 'dev.meshac.util.Location')
    text = text.replace('org.bukkit.World', 'net.minecraft.server.level.ServerLevel')
    # identifier renames, longest first
    for old in sorted(IDENT_MAP, key=len, reverse=True):
        new = IDENT_MAP[old]
        text = re.sub(r'(?<![\w.])' + re.escape(old) + r'\b', new, text)
    return text

def main():
    written, manual = [], []
    for root, dirs, files in os.walk(SRC):
        for f in sorted(files):
            if not f.endswith('.java'):
                continue
            src = os.path.join(root, f)
            rel = os.path.relpath(src, SRC)
            out = transform(src, rel)
            dst = os.path.join(DST, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            if os.path.exists(dst):
                old = open(dst).read()
                if 'TimerA.java' in rel or not any(mk in old for mk in ('org.bukkit', 'FIXME-MAP', 'packetevents', 'PlayerDataStore', 'MainThreadExecutor')):
                    continue  # never clobber hand-ported or clean files
                # previously flagged file: re-transform
            open(dst, 'w').write(out)
            written.append(rel)
            leftover = [mk for mk in MANUAL_MARKERS if mk in out] + (['FIXME-MAP'] if 'FIXME-MAP' in out else [])
            if leftover:
                manual.append((rel, sorted(set(leftover))))
    print('written:', len(written))
    print('manual-needed:', len(manual))
    for rel, marks in manual:
        print('  %s: %s' % (rel, ','.join(marks)))

if __name__ == '__main__':
    main()
