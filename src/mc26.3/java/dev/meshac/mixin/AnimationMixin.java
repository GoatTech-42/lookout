package dev.meshac.mixin;
import dev.meshac.packet.PacketDispatcher;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Exact per-version animation packet descriptor, preserving the raw packet feed. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class AnimationMixin {
 @Shadow public ServerPlayer player;
 @Inject(method="handlePunch", at=@At("HEAD"))
 private void meshac$animate(ServerboundPunchPacket packet, CallbackInfo ci) { PacketDispatcher.onSwing(player, packet); }
}
