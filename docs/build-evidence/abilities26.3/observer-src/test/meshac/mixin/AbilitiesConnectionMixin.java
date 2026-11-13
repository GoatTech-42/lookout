package test.meshac.mixin;
@org.spongepowered.asm.mixin.Mixin(value=net.minecraft.network.Connection.class,priority=900)
public abstract class AbilitiesConnectionMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",at=@org.spongepowered.asm.mixin.injection.At(value="INVOKE",target="Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))
 private void read(io.netty.channel.ChannelHandlerContext c,net.minecraft.network.protocol.Packet<?> p,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){if(p instanceof net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket held)test.meshac.AbilitiesTrace.record("ADMITTED_CHANNEL",held);}
 @org.spongepowered.asm.mixin.injection.Inject(method="exceptionCaught",at=@org.spongepowered.asm.mixin.injection.At("HEAD"))
 private void exception(io.netty.channel.ChannelHandlerContext c,Throwable t,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){System.out.println("[MOVE-OBSERVER] NATIVE_EXCEPTION "+t);t.printStackTrace(System.out);}
}

