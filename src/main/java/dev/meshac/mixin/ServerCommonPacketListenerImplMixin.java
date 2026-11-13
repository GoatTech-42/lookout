package dev.meshac.mixin;

import dev.meshac.packet.MeshPacketType;
import dev.meshac.packet.PacketDispatcher;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Common-listener hooks: keepalive + pong (transaction confirmations). */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {

	@Inject(method = "handleKeepAlive", at = @At("HEAD"))
	private void meshac$keepAlive(ServerboundKeepAlivePacket p, CallbackInfo ci) {
		ServerPlayer player = meshac$player();
		if (player != null) PacketDispatcher.onKeepAlive(player, p);
	}

	@Inject(method = "handlePong", at = @At("HEAD"))
	private void meshac$pong(ServerboundPongPacket p, CallbackInfo ci) {
		ServerPlayer player = meshac$player();
		if (player == null) return;
		var data = dev.meshac.data.PlayerDataManager.get(player);
		var connection = ((ServerCommonPacketListenerImplAccessor) (Object) this).meshac$getConnection();
		// A UUID match alone does not authenticate an old listener after session replacement.
		if (data != null && dev.meshac.tracker.ConfirmationTransport.isPlay(data, connection))
			PacketDispatcher.onConfirmation(player, p.getId(), MeshPacketType.PONG, p);
	}

	/** Resolves the ServerPlayer on the game-listener subclass; null during configuration phase. */
	private ServerPlayer meshac$player() {
		Object self = this;
		if (self instanceof net.minecraft.server.network.ServerGamePacketListenerImpl game) return game.player;
		return null;
	}
}
