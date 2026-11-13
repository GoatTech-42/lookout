#!/usr/bin/env python3
"""Pin the post-encode operation boundary and native superclass for every supported target. Not weaving proof."""
from pathlib import Path
import subprocess,re,json,hashlib,os,zipfile
r=Path(__file__).resolve().parents[2];jdk=Path(os.environ.get('JAVA_HOME',str(Path.home()/'jdk25')))/'bin';cache=Path.home()/'.gradle-meshac/caches/fabric-loom';out=r/'docs/build-evidence/outbound-promise-research/native-encoded';out.mkdir(parents=True,exist_ok=True);manifest={}
for v in ['1.21.11','26.1.2','26.2','26.3']:
 info=json.loads((cache/v/'mojang_minecraft_info.json').read_text());coord=next(x['name'] for x in info['libraries'] if x['name'].startswith('io.netty:netty-codec-base:'));nv=coord.rsplit(':',1)[1]
 netty=next((Path.home()/'.gradle-meshac/caches/modules-2/files-2.1/io.netty/netty-codec-base'/nv).glob('*/*.jar'))
 native=next((cache/'minecraftMaven/net/minecraft/minecraft-merged').glob(v+'*/*.jar')) if v=='1.21.11' else next((cache/'minecraftMaven/net/minecraft/minecraft-merged-deobf'/v).glob('*.jar'))
 c=subprocess.check_output([str(jdk/'javap'),'-p','-c','-s','-classpath',str(netty),'io.netty.handler.codec.MessageToByteEncoder'],text=True)
 write=re.search(r'  public void write\(.*?(?=\n  protected)',c,re.S).group();assert 'descriptor: (Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;Lio/netty/channel/ChannelPromise;)V' in write
 assert write.count('InterfaceMethod io/netty/channel/ChannelHandlerContext.write:')==3
 assert write.index('Method encode:')<write.index('InterfaceMethod io/netty/channel/ChannelHandlerContext.write:')
 encoder=subprocess.check_output([str(jdk/'javap'),'-p','-c','-s','-classpath',str(native),'net.minecraft.network.PacketEncoder'],text=True)
 assert 'extends io.netty.handler.codec.MessageToByteEncoder<net.minecraft.network.protocol.Packet<T>>' in encoder
 assert 'public void write(' not in encoder,'native subclass overrides superclass write'
 receipt=write+'\n'+encoder;(out/(v+'.txt')).write_text(receipt)
 built=r/'build/libs'/('meshac-'+v+'-2.9.7.28-meshac-0.1.0.jar')
 with zipfile.ZipFile(built) as z:
  assert 'ByteEncoderCompletionMixin' in json.loads(z.read('meshac.mixins.json'))['mixins']
  assert 'dev/meshac/mixin/ByteEncoderCompletionMixin.class' in z.namelist()
 manifest[v]={'nettyCoordinate':coord,'nettyJarSHA256':hashlib.sha256(netty.read_bytes()).hexdigest(),'nativeJarSHA256':hashlib.sha256(native.read_bytes()).hexdigest(),'builtJarSHA256':hashlib.sha256(built.read_bytes()).hexdigest(),'receiptSHA256':hashlib.sha256(receipt.encode()).hexdigest()}
 print(v,coord,'native superclass/three ctx.write sites after encode and packaged registration PASS')
(out/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
