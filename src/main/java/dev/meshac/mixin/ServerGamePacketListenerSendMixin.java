package dev.meshac.mixin;

import dev.meshac.packet.OutboundDispatcher;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clientbound send hook feeding the outbound dispatcher. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerGamePacketListenerSendMixin {


	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
	private void meshac$send(Packet<?> packet, CallbackInfo ci) {
		// send is declared on the common listener, but tracked state only exists in PLAY.
		if ((Object) this instanceof ServerGamePacketListenerImpl game)
			OutboundDispatcher.onSend(game.player, packet);
	}
}
