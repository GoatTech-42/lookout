package dev.meshac.mixin;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** 26.x has a separate attack packet; preserve its raw packet and shared processing order. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class AttackMixin {
 @Shadow public ServerPlayer player;
 @Inject(method="handleAttack", at=@At("HEAD"))
 private void meshac$attack(ServerboundAttackPacket packet, CallbackInfo ci) {
  var interaction = new dev.meshac.packet.InteractEntity(packet.entityId(), dev.meshac.packet.InteractEntity.Action.ATTACK, net.minecraft.world.InteractionHand.MAIN_HAND, null, false);
  dev.meshac.packet.PacketDispatcher.dispatchInteraction(player, packet, interaction);
 }
}
