package test.meshac.mixin;
import test.meshac.AnchorTrace;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import io.netty.channel.ChannelFutureListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=Connection.class,priority=900)
public abstract class ConnectionTraceMixin {
 @Inject(method="doSendPacket(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",at=@At("HEAD"))
 private void before(Packet<?> p,ChannelFutureListener l,boolean flush,CallbackInfo ci){
  if(p instanceof ClientboundLoginPacket login)AnchorTrace.record((Connection)(Object)this,"LOGIN_PRE_WRITE",login.playerId());
  if(p instanceof ClientboundPingPacket ping)AnchorTrace.record((Connection)(Object)this,"PING_PRE_WRITE",ping.getId());
 }
 @Inject(method="doSendPacket(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",at=@At("TAIL"))
 private void after(Packet<?> p,ChannelFutureListener l,boolean flush,CallbackInfo ci){
  if(p instanceof ClientboundLoginPacket login)AnchorTrace.record((Connection)(Object)this,"LOGIN_POST_WRITE",login.playerId());
  if(p instanceof ClientboundPingPacket ping)AnchorTrace.record((Connection)(Object)this,"PING_POST_WRITE",ping.getId());
 }
}
