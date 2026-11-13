package test.meshac;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.world.entity.player.Abilities;
public final class AbilitiesDriver implements DedicatedServerModInitializer {
 private boolean done;
 public void onInitializeServer(){ServerTickEvents.END_SERVER_TICK.register(server->{
  if(done||server.getPlayerList().getPlayers().isEmpty())return;
  var player=server.getPlayerList().getPlayers().getFirst();
  var data=dev.meshac.data.PlayerDataManager.get(player);
  if(data==null||!data.isHandshakeComplete())return;
  var connection=((dev.meshac.mixin.ServerCommonPacketListenerImplAccessor)player.connection).meshac$getConnection();
  if(!dev.meshac.tracker.ConfirmationTransport.isPlay(data,connection))return;
  done=true;
  var first=new Abilities();first.flying=true;
  var second=new Abilities();second.flying=false;
  var a=new ServerboundPlayerAbilitiesPacket(first);
  var b=new ServerboundPlayerAbilitiesPacket(second);
  var channel=((dev.meshac.mixin.ConnectionAccessor)connection).meshac$getChannel();
  channel.eventLoop().execute(()->{
   if(!channel.isOpen()||connection.getPacketListener()!=player.connection||data.getPlayer()!=player||!dev.meshac.tracker.ConfirmationTransport.isPlay(data,connection))throw new AssertionError("test session retired");
   if(channel.pipeline().get("packet_handler")!=connection)throw new AssertionError("native handler context not found");
   channel.pipeline().addBefore("packet_handler","test-only-abilities-ingress",new io.netty.channel.ChannelInboundHandlerAdapter());
   var ctx=channel.pipeline().context("test-only-abilities-ingress");
   System.out.println("[ABILITIES-DRIVER] NATIVE_CONSTRUCTED_ONLY entry=ctx.fireChannelRead next=packet_handler eventLoop="+channel.eventLoop().inEventLoop()+" livePLAY=true distinct="+(a!=b));
   AbilitiesTrace.record("DRIVER",a);ctx.fireChannelRead(a);
   AbilitiesTrace.record("DRIVER",b);ctx.fireChannelRead(b);
   channel.pipeline().remove("test-only-abilities-ingress");
   System.out.println("[ABILITIES-DRIVER] queued two native constructed packets; codec/wire/UI/permission unqualified");
  });
 });}
}
