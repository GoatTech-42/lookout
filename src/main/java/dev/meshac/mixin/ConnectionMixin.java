package dev.meshac.mixin;

import dev.meshac.data.PlayerData;
import dev.meshac.data.PlayerDataManager;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** doSendPacket runs on the channel loop, after sendPacket's native handoff. */
@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Shadow public abstract PacketListener getPacketListener();

    @Inject(method = "doSendPacket(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("TAIL"))
    private void meshac$afterSend(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (!(packet instanceof ClientboundLoginPacket login)
                || !(getPacketListener() instanceof ServerGamePacketListenerImpl game)) return;
        PlayerData data = PlayerDataManager.get(game.player);
        if (data != null && data.getPlayer() == game.player && data.getEntityId() == login.playerId())
            data.getConfirmationTracker().sendJoinConfirmation();
    }
}
