package dev.meshac.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Successful native packet encoding precedes preparation of upstream after-send tasks. */
@Mixin(value = MessageToByteEncoder.class, remap = false)
public abstract class ByteEncoderCompletionMixin {
    @WrapOperation(method = "write(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;Lio/netty/channel/ChannelPromise;)V",
            at = @At(value = "INVOKE", target = "Lio/netty/channel/ChannelHandlerContext;write(Ljava/lang/Object;Lio/netty/channel/ChannelPromise;)Lio/netty/channel/ChannelFuture;"),
            remap = false, require = 3, allow = 3)
    private ChannelFuture meshac$encodedWrite(ChannelHandlerContext context, Object encoded, ChannelPromise promise,
            Operation<ChannelFuture> original, @Local(argsOnly = true) Object packet) {
        if ((Object) this instanceof PacketEncoder<?> && encoded instanceof ByteBuf bytes && bytes.isReadable()) {
            Connection connection = context.pipeline().get(Connection.class);
            if (connection != null) {
                Runnable task = dev.meshac.packet.OutboundDispatcher.beforeVelocityWrite(connection, packet);
                if (task != null) {
                    promise = promise.unvoid();
                    promise.addListener(future -> task.run());
                }
            }
        }
        return original.call(context, encoded, promise);
    }
}
