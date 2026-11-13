package test.meshac.mixin;
@org.spongepowered.asm.mixin.Mixin(value=net.minecraft.server.network.ServerGamePacketListenerImpl.class,priority=900)
public abstract class MovementHandlerMixin {
 @org.spongepowered.asm.mixin.Shadow public net.minecraft.server.level.ServerPlayer player;
 @org.spongepowered.asm.mixin.injection.Inject(method="handleMovePlayer",at=@org.spongepowered.asm.mixin.injection.At("HEAD"))
 private void head(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket p,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){test.meshac.MovementTrace.record("HANDLER_HEAD",p);}
 @org.spongepowered.asm.mixin.injection.Inject(method="handleMovePlayer",at=@org.spongepowered.asm.mixin.injection.At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=org.spongepowered.asm.mixin.injection.At.Shift.AFTER))
 private void capture(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket p,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){test.meshac.MovementTrace.capture(p,player);}
 @org.spongepowered.asm.mixin.injection.Inject(method="handleMovePlayer",at=@org.spongepowered.asm.mixin.injection.At("TAIL"))
 private void tail(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket p,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){test.meshac.MovementTrace.record("HANDLER_TAIL",p);}
}
