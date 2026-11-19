package dev.lookout.mixin;

import dev.lookout.LookoutClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feeds S2C entity events (swings, velocity, teleports, totem pops, equipment) to lookout. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Shadow private ClientLevel level;

    @Inject(method = "handleAddEntity", at = @At("TAIL"))
    private void lookout$addEntity(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        if (LookoutClient.ENGINE == null) return;
        if (packet.getType() != EntityTypes.ARROW && packet.getType() != EntityTypes.SPECTRAL_ARROW) return;
        double speed = packet.getMovement().length(); // blocks/tick
        LookoutClient.ENGINE.onProjectileSpawn(packet.getX(), packet.getY(), packet.getZ(), speed,
                packet.getId(), packet.getData());
    }

    @Inject(method = "handleDamageEvent", at = @At("TAIL"))
    private void lookout$damage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        if (LookoutClient.ENGINE == null) return;
        String typeKey = packet.sourceType().unwrapKey().map(k -> k.identifier().toString()).orElse("");
        LookoutClient.ENGINE.onDamage(packet.entityId(), packet.sourceCauseId(), packet.sourceDirectId(), typeKey);
    }

    @Inject(method = "handleAnimate", at = @At("TAIL"))
    private void lookout$animate(ClientboundAnimatePacket packet, CallbackInfo ci) {
        if (LookoutClient.ENGINE != null) LookoutClient.ENGINE.onSwing(packet.getId());
    }

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void lookout$entityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (packet.getEventId() != 35 || LookoutClient.ENGINE == null) return; // 35 = totem of undying pop
        Entity entity = packet.getEntity(this.level);
        if (entity instanceof RemotePlayer) LookoutClient.ENGINE.onTotemPop(entity.getId());
    }

    @Inject(method = "handleSetEntityMotion", at = @At("TAIL"))
    private void lookout$motion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        if (LookoutClient.ENGINE != null) LookoutClient.ENGINE.onVelocity(packet.id());
    }

    @Inject(method = "handleTeleportEntity", at = @At("TAIL"))
    private void lookout$teleport(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
        if (LookoutClient.ENGINE != null) LookoutClient.ENGINE.onTeleport(packet.id());
    }

    @Inject(method = "handleEntityPositionSync", at = @At("TAIL"))
    private void lookout$positionSync(ClientboundEntityPositionSyncPacket packet, CallbackInfo ci) {
        if (LookoutClient.ENGINE != null) LookoutClient.ENGINE.onTeleport(packet.id());
    }

    @Inject(method = "handleSetEquipment", at = @At("TAIL"))
    private void lookout$equipment(ClientboundSetEquipmentPacket packet, CallbackInfo ci) {
        if (LookoutClient.ENGINE == null) return;
        for (com.mojang.datafixers.util.Pair<EquipmentSlot, net.minecraft.world.item.ItemStack> entry : packet.getSlots()) {
            if (entry.getFirst() == EquipmentSlot.OFFHAND && entry.getSecond().is(Items.TOTEM_OF_UNDYING)) {
                LookoutClient.ENGINE.onTotemEquip(packet.getEntity());
            }
        }
    }
}
