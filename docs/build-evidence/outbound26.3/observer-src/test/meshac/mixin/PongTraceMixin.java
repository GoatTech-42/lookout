package test.meshac.mixin;
import test.meshac.AnchorTrace;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ServerCommonPacketListenerImpl.class,priority=900)
public abstract class PongTraceMixin {
 @Inject(method="handlePong",at=@At("TAIL"))
 private void after(ServerboundPongPacket p,CallbackInfo ci){
  if((Object)this instanceof ServerGamePacketListenerImpl game){
   var data=dev.meshac.data.PlayerDataManager.get(game.player);
   if(data!=null&&p.getId()==data.getConfirmationTracker().getJoinConfirmationId())
    AnchorTrace.record(((dev.meshac.mixin.ServerCommonPacketListenerImplAccessor)game).meshac$getConnection(),"PONG_POST_HANDLE",p.getId());
  }
 }
}
