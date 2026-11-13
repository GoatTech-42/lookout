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

    // This invocation follows native isOpen/shouldHandleMessage admission, before thread replay.
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))
    private void meshac$movementIngress(io.netty.channel.ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket move)
                || !(getPacketListener() instanceof ServerGamePacketListenerImpl game)
                || game.player.connection != game) return;
        PlayerData data = PlayerDataManager.get(game.player);
        if (data != null && dev.meshac.tracker.ConfirmationTransport.isPlay(data, (Connection) (Object) this))
            dev.meshac.packet.PacketDispatcher.onMove(game.player, move);
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))
    private void meshac$heldIngress(io.netty.channel.ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket held)
                || !(getPacketListener() instanceof ServerGamePacketListenerImpl game)
                || game.player.connection != game) return;
        PlayerData data = PlayerDataManager.get(game.player);
        if (data != null && dev.meshac.tracker.ConfirmationTransport.isPlay(data, (Connection) (Object) this))
            dev.meshac.packet.PacketDispatcher.onHeldItemChange(game.player, held);
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))
    private void meshac$abilitiesIngress(io.netty.channel.ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket abilities)
                || !(getPacketListener() instanceof ServerGamePacketListenerImpl game)
                || game.player.connection != game) return;
        PlayerData data = PlayerDataManager.get(game.player);
        if (data != null && dev.meshac.tracker.ConfirmationTransport.isPlay(data, (Connection) (Object) this))
            dev.meshac.packet.PacketDispatcher.onAbilities(game.player, abilities);
    }

    @Inject(method = "doSendPacket(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("TAIL"))
    private void meshac$afterSend(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (!(packet instanceof ClientboundLoginPacket login)
                || !(getPacketListener() instanceof ServerGamePacketListenerImpl game)) return;
        PlayerData data = PlayerDataManager.get(game.player);
        if (data != null && data.getPlayer() == game.player && data.getEntityId() == login.playerId())
            data.getConfirmationTracker().sendJoinConfirmation();
    }
}
