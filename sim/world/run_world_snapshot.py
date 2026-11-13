#!/usr/bin/env python3
"""Headless actual WorldBlockTracker with a packet-buffer fixture. No Minecraft session."""
from pathlib import Path
import os,tempfile,subprocess
ROOT=Path(__file__).resolve().parents[2]; JDK=Path(os.environ.get('JAVA_HOME',str(Path.home()/'jdk25')))/'bin'
s={}
def put(p,t):s[p+'.java']='package '+p.rsplit('/',1)[0].replace('/','.')+'; '+t
put('net/minecraft/core/Direction','public enum Direction {UP}')
put('net/minecraft/core/SectionPos','public class SectionPos {}')
put('net/minecraft/core/BlockPos','public class BlockPos {public int x,y,z;public BlockPos(int a,int b,int c){x=a;y=b;z=c;} public int getX(){return x;} public int getY(){return y;} public int getZ(){return z;} public BlockPos immutable(){return new BlockPos(x,y,z);} public BlockPos relative(Direction d){return new BlockPos(x,y+1,z);}}')
put('net/minecraft/resources/ResourceKey','public class ResourceKey<T> {}')
put('net/minecraft/world/level/Level','public class Level {}')
put('net/minecraft/world/level/ChunkPos','public record ChunkPos(int x,int z) {public static int getX(long p){return (int)p;}public static int getZ(long p){return (int)(p>>32);}}')
put('net/minecraft/world/level/block/state/BlockState','public record BlockState(boolean wet) {}')
put('net/minecraft/world/level/block/Blocks','public class Blocks {public static Blocks AIR=new Blocks(); public net.minecraft.world.level.block.state.BlockState defaultBlockState(){return new net.minecraft.world.level.block.state.BlockState(false);}}')
put('net/minecraft/network/FriendlyByteBuf','public class FriendlyByteBuf {public java.util.List<net.minecraft.core.BlockPos> wet;public FriendlyByteBuf(java.util.List<net.minecraft.core.BlockPos> w){wet=w;}public boolean isReadable(){return false;} public int readableBytes(){return 0;} public boolean release(){return true;}}')
put('net/minecraft/world/level/chunk/LevelChunkSection','public class LevelChunkSection {private java.util.List<net.minecraft.core.BlockPos> wet=java.util.List.of();public LevelChunkSection(Object factory){}public void read(net.minecraft.network.FriendlyByteBuf b){wet=new java.util.ArrayList<>(b.wet);}public boolean hasOnlyAir(){return wet.isEmpty();}public net.minecraft.world.level.block.state.BlockState getBlockState(int x,int y,int z){return new net.minecraft.world.level.block.state.BlockState(wet.stream().anyMatch(p->(p.x&15)==x&&(p.y&15)==y&&(p.z&15)==z));}}')
put('net/minecraft/world/level/chunk/LevelChunk','public class LevelChunk {public net.minecraft.world.level.ChunkPos getPos(){return new net.minecraft.world.level.ChunkPos(0,0);}public LevelChunkSection[] getSections(){return new LevelChunkSection[0];}public int getSectionYFromSectionIndex(int i){return i-4;}}')
put('net/minecraft/network/protocol/game/ClientboundLevelChunkWithLightPacket','public class ClientboundLevelChunkWithLightPacket {public int x,z; public java.util.List<net.minecraft.core.BlockPos> wet,tiles;public ClientboundLevelChunkWithLightPacket(int a,int b,java.util.List<net.minecraft.core.BlockPos> w,java.util.List<net.minecraft.core.BlockPos> t){x=a;z=b;wet=w;tiles=t;} public ClientboundLevelChunkWithLightPacket data(){return this;}public net.minecraft.network.FriendlyByteBuf getReadBuffer(){return new net.minecraft.network.FriendlyByteBuf(wet);}}')
put('net/minecraft/server/level/ServerLevel','public class ServerLevel {public int getSectionsCount(){return 1;}public int getMinSectionY(){return -4;}public int getMinY(){return -64;}public Object palettedContainerFactory(){return null;}public Object getChunk(int x,int z){throw new AssertionError("Live terrain must not be read");}}')
put('dev/meshac/MeshServices','public class MeshServices {public static void log(String s){System.out.println(s);}}')
put('dev/meshac/data/PlayerData','public class PlayerData {public final dev.meshac.tracker.ConfirmationTracker confirmation=new dev.meshac.tracker.ConfirmationTracker();public dev.meshac.tracker.ConfirmationTracker getConfirmationTracker(){return confirmation;}}')
put('dev/meshac/tracker/ConfirmationTracker','public class ConfirmationTracker {public final java.util.List<Runnable> pending=new java.util.ArrayList<>();public void runAfterConfirmation(Runnable r){pending.add(r);}public void ack(){var tasks=new java.util.ArrayList<>(pending);pending.clear();tasks.forEach(Runnable::run);}}')
put('dev/meshac/veil/Veil','public class Veil {public static java.util.Set<Integer> takeEmittedMods(Object d,Object p,int i){throw new AssertionError("Global emitted metadata must not be read");}}')
put('dev/meshac/compat/VersionBridge','''public class VersionBridge {public static boolean wetSulfur(net.minecraft.world.level.block.state.BlockState s){return s.wet();}public static long chunkKey(net.minecraft.world.level.ChunkPos p){return ((long)p.z()<<32)|(p.x()&0xffffffffL);}public static int chunkX(net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket p){return p.x;}public static int chunkZ(net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket p){return p.z;}public static net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket chunkData(net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket p){return p;}public static void blockEntityPositions(net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket p,int x,int z,java.util.function.Consumer<net.minecraft.core.BlockPos> c){p.tiles.forEach(c);}}''')
with tempfile.TemporaryDirectory(prefix='meshac-world-test-') as tmp:
 t=Path(tmp);files=[]
 for name,text in s.items():
  p=t/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(str(p))
 files.extend([str(ROOT/'src/main/java/dev/meshac/tracker/WorldBlockTracker.java'),str(ROOT/'sim/world/WorldSnapshotTest.java')])
 subprocess.run([str(JDK/'javac'),'-d',str(t/'classes'),*files],check=True)
 subprocess.run([str(JDK/'java'),'-cp',str(t/'classes'),'WorldSnapshotTest'],check=True)
