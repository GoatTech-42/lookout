#!/usr/bin/env python3
"""Actual encoded-write operation callback on real Netty. Invocation capture is modeled, not a weave test."""
from pathlib import Path
import os,subprocess,tempfile,json,hashlib
r=Path(__file__).resolve().parents[2];jdk=Path(os.environ.get('JAVA_HOME',str(Path.home()/'jdk25')))/'bin'
jars=sorted((Path.home()/'.gradle-meshac/caches/modules-2/files-2.1/io.netty').glob('*/'+os.environ.get('NETTY_VERSION','4.2.16.Final')+'/*/*.jar'));cp=os.pathsep.join(map(str,jars))
s={}
def put(n,b):s[n+'.java']='package '+n.rsplit('/',1)[0].replace('/','.')+';'+b
put('org/spongepowered/asm/mixin/Mixin','public @interface Mixin{Class<?> value();boolean remap() default true;}')
put('org/spongepowered/asm/mixin/injection/At','public @interface At{String value();String target() default "";}')
put('com/llamalad7/mixinextras/injector/wrapoperation/Operation','public interface Operation<R>{R call(Object...args);}')
put('com/llamalad7/mixinextras/injector/wrapoperation/WrapOperation','public @interface WrapOperation{String method();org.spongepowered.asm.mixin.injection.At at();int require() default -1;int allow() default -1;boolean remap() default true;}')
put('com/llamalad7/mixinextras/sugar/Local','public @interface Local{boolean argsOnly() default false;}')
put('net/minecraft/network/Connection','public class Connection extends io.netty.channel.ChannelInboundHandlerAdapter{}')
put('net/minecraft/network/PacketEncoder','public class PacketEncoder<T> extends dev.meshac.mixin.ByteEncoderCompletionMixin{}')
put('dev/meshac/packet/OutboundDispatcher','public class OutboundDispatcher{public static java.util.List<Object> completed=new java.util.ArrayList<>();public static java.util.List<String> events=new java.util.ArrayList<>();public static int prepare,complete;public static Runnable beforeVelocityWrite(net.minecraft.network.Connection c,Object p){prepare++;return ()->{complete++;completed.add(p);events.add("after");};}}')
test=r'''package fixture;
import io.netty.channel.*;import io.netty.channel.embedded.EmbeddedChannel;import io.netty.buffer.*;import io.netty.handler.codec.*;
public class EncodedHookTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static Object raw;static Object target;static boolean failEncode;
 static ChannelFuture invoke(ChannelHandlerContext ctx,Object bytes,ChannelPromise promise){try{
  var m=dev.meshac.mixin.ByteEncoderCompletionMixin.class.getDeclaredMethod("meshac$encodedWrite",io.netty.channel.ChannelHandlerContext.class,Object.class,io.netty.channel.ChannelPromise.class,com.llamalad7.mixinextras.injector.wrapoperation.Operation.class,Object.class);m.setAccessible(true);
  return (ChannelFuture)m.invoke(target,ctx,bytes,promise,(com.llamalad7.mixinextras.injector.wrapoperation.Operation<ChannelFuture>)(args)->{
   check(args.length==3&&args[0]==ctx&&args[1]==bytes,"native operation arguments changed");return ctx.write(args[1],(ChannelPromise)args[2]);},raw);
 }catch(java.lang.reflect.InvocationTargetException e){if(e.getCause() instanceof RuntimeException x)throw x;throw new RuntimeException(e.getCause());}catch(Exception e){throw new RuntimeException(e);}}
 static class Sink extends ChannelOutboundHandlerAdapter {ChannelPromise pending;Object bytes;boolean sync,failed;boolean completedBeforeReturn;Runnable nested;java.util.List<Object> outputs=new java.util.ArrayList<>();
  public void write(ChannelHandlerContext ctx,Object m,ChannelPromise p){pending=p;bytes=m;outputs.add(m);if(nested!=null){var n=nested;nested=null;n.run();}if(sync){if(failed)p.setFailure(new IllegalStateException("write-failure"));else p.setSuccess();completedBeforeReturn=dev.meshac.packet.OutboundDispatcher.complete>0;}}
 }
 public static void main(String[]args){
  for(boolean fail:new boolean[]{false,true}){
   dev.meshac.packet.OutboundDispatcher.prepare=0;dev.meshac.packet.OutboundDispatcher.complete=0;
   var c=new EmbeddedChannel();var sink=new Sink();c.pipeline().addLast("sink",sink);
   c.pipeline().addLast("capture-model",new ChannelOutboundHandlerAdapter(){public void write(ChannelHandlerContext ctx,Object m,ChannelPromise p){invoke(ctx,m,p);}});
   c.pipeline().addLast("native-codec",new MessageToByteEncoder<Object>(){protected void encode(ChannelHandlerContext ctx,Object m,ByteBuf b){raw=m;if(failEncode)throw new EncoderException("encode-failure");b.writeByte(7);}});
   c.pipeline().addLast("connection",new net.minecraft.network.Connection());
   target=new net.minecraft.network.PacketEncoder();raw=new Object();var p=c.newPromise();dev.meshac.packet.OutboundDispatcher.events.clear();p.addListener(f->dev.meshac.packet.OutboundDispatcher.events.add("caller"));c.write(raw,p);
   check(dev.meshac.packet.OutboundDispatcher.prepare==1&&dev.meshac.packet.OutboundDispatcher.complete==0,"preparation missing or completion early");check(sink.pending==p,"native promise replaced");
   if(fail)p.setFailure(new IllegalArgumentException("expected"));else p.setSuccess();check(dev.meshac.packet.OutboundDispatcher.complete==1,"failed/success completion missing");check(dev.meshac.packet.OutboundDispatcher.events.equals(java.util.List.of("caller","after")),"native caller listener order changed");io.netty.util.ReferenceCountUtil.release(sink.bytes);
   int before=dev.meshac.packet.OutboundDispatcher.prepare;failEncode=true;var failed=c.write(new Object());check(!failed.isSuccess()&&dev.meshac.packet.OutboundDispatcher.prepare==before,"encode failure prepared a task");failEncode=false;
   dev.meshac.packet.OutboundDispatcher.complete=0;sink.sync=true;c.write(new Object());check(sink.completedBeforeReturn,"completion listener registered after write");io.netty.util.ReferenceCountUtil.release(sink.bytes);
   before=dev.meshac.packet.OutboundDispatcher.prepare;target=new dev.meshac.mixin.ByteEncoderCompletionMixin(){};c.write(new Object());check(dev.meshac.packet.OutboundDispatcher.prepare==before,"non-native encoder tracked");io.netty.util.ReferenceCountUtil.release(sink.bytes);
   target=new net.minecraft.network.PacketEncoder();
   before=dev.meshac.packet.OutboundDispatcher.prepare;c.pipeline().context("native-codec").write(Unpooled.EMPTY_BUFFER);check(dev.meshac.packet.OutboundDispatcher.prepare==before,"empty encoded message tracked");
   sink.sync=false;dev.meshac.packet.OutboundDispatcher.completed.clear();raw=new Object();var first=raw;c.write(raw);var firstPromise=sink.pending;io.netty.util.ReferenceCountUtil.release(sink.bytes);raw=new Object();var second=raw;c.write(raw);var secondPromise=sink.pending;io.netty.util.ReferenceCountUtil.release(sink.bytes);secondPromise.setSuccess();firstPromise.setSuccess();check(dev.meshac.packet.OutboundDispatcher.completed.equals(java.util.List.of(second,first)),"deferred invocation callbacks crossed");sink.sync=true;
   dev.meshac.packet.OutboundDispatcher.completed.clear();sink.outputs.clear();sink.nested=()->c.write("inner");c.write("outer");check(dev.meshac.packet.OutboundDispatcher.completed.equals(java.util.List.of("inner","outer")),"reentrant invocation tasks crossed");for(var bytes:sink.outputs)io.netty.util.ReferenceCountUtil.release(bytes);
   c.pipeline().addAfter("native-codec","unbundler",new MessageToMessageEncoder<java.util.List<?>>() {protected void encode(ChannelHandlerContext ctx,java.util.List<?> m,java.util.List<Object> out){out.addAll(m);}});
   dev.meshac.packet.OutboundDispatcher.completed.clear();sink.outputs.clear();c.write(java.util.List.of("child1","child2"));check(dev.meshac.packet.OutboundDispatcher.completed.equals(java.util.List.of("child1","child2")),"bundle child preparation/completion missing");for(var bytes:sink.outputs)io.netty.util.ReferenceCountUtil.release(bytes);c.pipeline().remove("unbundler");
   sink.pending=null;var v=c.voidPromise();c.write(new Object(),v);check(sink.pending!=null,"tracked void write did not reach sink");check(!sink.pending.isVoid(),"void promise not unvoided");io.netty.util.ReferenceCountUtil.release(sink.bytes);c.finishAndReleaseAll();
  }
  System.out.println("EncodedHookTest PASS actual operation callback, real native codec encode-failure and delayed/failed/sync/void/reentrant/bundle promise; argument capture modeled, no weave claim");
 }
}'''
with tempfile.TemporaryDirectory(prefix='meshac-encoded-hook-') as d:
 t=Path(d);files=[]
 for n,b in s.items():
  p=t/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(b);files.append(str(p))
 p=t/'EncodedHookTest.java';p.write_text(test);files.append(str(p));files.append(str(r/'src/main/java/dev/meshac/mixin/ByteEncoderCompletionMixin.java'))
 out=Path(os.environ.get('MESHAC_PREP_OUTPUT','/tmp/meshac-encoded-inputs'));out.mkdir(parents=True,exist_ok=True)
 (out/'run_outbound_encoded-inputs.json').write_text(json.dumps({str(Path(f).relative_to(t)) if Path(f).is_relative_to(t) else str(Path(f)):hashlib.sha256(Path(f).read_bytes()).hexdigest() for f in [*files,*map(str,jars)]},indent=2)+'\n')
 subprocess.run([str(jdk/'javac'),'-cp',cp,'-d',str(t/'classes'),*files],check=True)
 subprocess.run([str(jdk/'java'),'-cp',str(t/'classes')+os.pathsep+cp,'fixture.EncodedHookTest'],check=True)
