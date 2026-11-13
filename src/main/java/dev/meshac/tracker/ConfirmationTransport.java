package dev.meshac.tracker;

import dev.meshac.data.PlayerData;
import dev.meshac.data.PlayerDataManager;
import dev.meshac.mixin.ConnectionAccessor;
import dev.meshac.mixin.PacketEncoderAccessor;
import dev.meshac.mixin.ServerCommonPacketListenerImplAccessor;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketEncoder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** Native boundary for confirmation sends. Does not establish ownership of other tracker state. */
public final class ConfirmationTransport {
    private ConfirmationTransport() { }

    public static Connection connection(PlayerData data) {
        ServerPlayer player = data.getPlayer();
        return player == null || player.connection == null ? null
                : ((ServerCommonPacketListenerImplAccessor) player.connection).meshac$getConnection();
    }

    public static boolean isPlay(PlayerData data, Connection connection) {
        if (connection == null || !connection.isConnected()) return false;
        ServerPlayer player = data.getPlayer();
        if (player == null || PlayerDataManager.get(player) != data
                || !(connection.getPacketListener() instanceof ServerGamePacketListenerImpl game)
                || game.player != player || connection(data) != connection) return false;
        Channel channel = ((ConnectionAccessor) connection).meshac$getChannel();
        PacketEncoder<?> encoder = channel == null ? null : channel.pipeline().get(PacketEncoder.class);
        return encoder != null && ((PacketEncoderAccessor) encoder).meshac$getProtocolInfo().id() == ConnectionProtocol.PLAY;
    }

    public static void inEventLoop(PlayerData data, Runnable action) {
        Connection connection = connection(data);
        if (!isPlay(data, connection)) return;
        Channel channel = ((ConnectionAccessor) connection).meshac$getChannel();
        Runnable guarded = () -> { if (isPlay(data, connection)) action.run(); };
        if (channel.eventLoop().inEventLoop()) guarded.run();
        else channel.eventLoop().execute(guarded);
    }
}
