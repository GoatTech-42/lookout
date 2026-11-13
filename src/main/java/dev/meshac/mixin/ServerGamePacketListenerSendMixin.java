package dev.meshac.mixin;

import dev.meshac.packet.OutboundDispatcher;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Caller-side tracking except self-velocity/explosion, which require native after-write tasks. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerGamePacketListenerSendMixin {


	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
	private void meshac$send(Packet<?> packet, CallbackInfo ci) {
		// send is declared on the common listener, but tracked state only exists in PLAY.
		if ((Object) this instanceof ServerGamePacketListenerImpl game) {
			if (packet instanceof net.minecraft.network.protocol.game.ClientboundLoginPacket)
				dev.meshac.data.PlayerDataManager.onJoin(game.player);
			if (!(packet instanceof net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket)
					&& !(packet instanceof net.minecraft.network.protocol.game.ClientboundExplodePacket))
				OutboundDispatcher.onSend(game.player, packet);
		}
	}
}
