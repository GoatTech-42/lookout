package dev.meshac.mixin;

import dev.meshac.packet.OutboundDispatcher;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clientbound send hook feeding the outbound dispatcher. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerSendMixin {

	@Shadow public ServerPlayer player;

	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
	private void meshac$send(Packet<?> packet, CallbackInfo ci) {
		OutboundDispatcher.onSend(player, packet);
	}
}
