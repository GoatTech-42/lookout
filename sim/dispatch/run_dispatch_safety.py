#!/usr/bin/env python3
"""Exercise unchanged production dispatcher bodies with synthetic checks and headless boundary doubles.
All four evaluation routes are invoked from a compiled fixture jar with no source on its runtime path.
This is not proof of Minecraft mixin/runtime integration or the released mod jar's full class graph.
"""
from pathlib import Path
import tempfile,subprocess,os
ROOT=Path(__file__).resolve().parents[2]
JDK=Path(os.environ.get('JAVA_HOME',str(Path.home()/'jdk25')))/'bin'
# Remove-entities getter differs by target jar (javap-verified): entityIds() on 26.3,
# getEntityIds() on 26.2 and 26.1.2. The stub pair below is shaped per variant so the
# fixture cannot silently hardcode one variant's API. Variant-jar truth stays with the
# per-variant gradle compile; this pins the sim's fixture seam per variant.
VARIANTS={'26.3':'entityIds','26.2':'getEntityIds','26.1.2':'getEntityIds','1.21.11':'getEntityIds'}
stubs={}
def put(name,text): stubs[name+'.java']='package '+name.rsplit('/',1)[0].replace('/','.')+'; '+text
put('dev/meshac/compat/VersionBridge','public class VersionBridge {public static Object hand(net.minecraft.network.protocol.game.ServerboundUseItemOnPacket p){return p.hand();} public static net.minecraft.network.protocol.game.ServerboundUseItemOnPacket hit(net.minecraft.network.protocol.game.ServerboundUseItemOnPacket p){return p.hitResult();} public static int chunkX(net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket p){return p.x();} public static int chunkZ(net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket p){return p.z();} public static int motionEntity(net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket p){return p.id();} public static net.minecraft.world.phys.Vec3 motion(net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket p){return p.movement();} public static int[] removedIds(net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket p){return p.REMOVED_IDS_GETTER().toIntArray();} public static net.minecraft.world.phys.Vec3 relativeDelta(Object a,Object t){return new net.minecraft.world.phys.Vec3(0,0,0);}}')
put('dev/meshac/check/AbstractCheck','''public class AbstractCheck { public int handlers,vl,alerts,heat,effects; public void handlePacket(dev.meshac.packet.MeshPacketEvent e){handlers++;vl++;alerts++;heat++;effects++;} public void handleEvent(dev.meshac.event.MeshEvent e){handlers++;vl++;alerts++;heat++;effects++;} }''')
put('dev/meshac/MeshServices','public class MeshServices {public static void log(String s){} }')
put('dev/meshac/data/PlayerDataManager','public class PlayerDataManager {public static PlayerData current; public static PlayerData get(net.minecraft.server.level.ServerPlayer p){return current;}}')
put('dev/meshac/data/PlayerData','''public class PlayerData { public java.util.List<dev.meshac.check.AbstractCheck> checks=new java.util.ArrayList<>(); public java.util.List<dev.meshac.check.AbstractCheck> getChecks(){return checks;} public dev.meshac.config.MeshVersion getClientVersion(){return dev.meshac.config.MeshVersion.UNKNOWN;}
public dev.meshac.player.CollisionState getCollisionState(){return new dev.meshac.player.CollisionState();} public Support getRotationTracker(){return new Support();} public Support getMotionState(){return new Support();} public Support getVelocityTracker(){return new Support();} public Support getConnectionTracker(){return new Support();} public Support getRelativeMoveTracker(){return new Support();} public Support getClickTimingData(){return new Support();} public Support getConfirmationTracker(){return new Support();} public Support getWorldBlockTracker(){return new Support();} public Support getEntityTracker(){return new Support();} public Support getVehicleTracker(){return new Support();}
public void setLastFlyingPacket(Object p){} public void setLastInteractionPacket(Object p){} public void setLastEntityActionPacket(Object p){} public void setLastDiggingPacket(Object p){} public void setLastPlacementPacket(Object p){} public void setLastHeldItemPacket(Object p){} public void setHandshakeComplete(boolean b){} public boolean isHandshakeComplete(){return false;} public boolean isMitigationPending(){return false;} public boolean isConnectionKickQueued(){return false;} public void setMitigationPending(boolean b){} public int getEntityId(){return 1;} public void setEntityId(int i){} public java.util.Set<Integer> getRawConfirmationIds(){return new java.util.HashSet<>();} public java.util.Set<Integer> getRepliedConfirmationIds(){return new java.util.HashSet<>();} }''')
put('dev/meshac/data/Support','''public class Support {
 public void handleRotation(float a,float b){} public void processMovement(Object...o){} public void handleEntityInteraction(Object o){} public void handleInteract(int i,boolean b){} public void handleEntityAction(Object o){} public void handleDigging(Object o){} public void recordDig(){} public void recordPlacement(){} public void recordSwing(){} public void recordKeepAlive(){} public void setLastRespawnTick(){} public void checkResponseTimeout(){} public int getJoinConfirmationId(){return 0;} public void handleConfirmationReply(int i){} public void recordSentConfirmation(int i){} public void reset(){} public void handleChunkPacket(Object level,Object packet){} public void handleChunkForget(Object p){} public void handleBlockUpdate(Object p,Object s){} public void handleSectionUpdates(dev.meshac.tracker.WorldBlockTracker.ClientboundSectionUpdatesConsumer c){}
 public Runnable handleExplosion(Object v){return null;} public Runnable handleVelocity(Object v){return null;} public void handleAttributes(Object p){} public void handlePotionEffect(Object a,Object b){} public void handleRemovedPotionEffect(Object p){} public void handleServerPosition(double a,double b,double c){} public void clearTargetPositions(){} public void handlePlayerMetadata(Object p){} public void handleGameState(byte b,float f){} public dev.meshac.tracker.TrackedEntity getTrackedEntity(int i){return null;} public void handleRelativeMoveAndRotation(Object...o){} public void handleTargetMove(Object...o){} public void handleRelativeMove(Object...o){} public void handleTeleport(Object...o){} public void handleSpawn(Object...o){} public void handleDestroy(int[] a){} public void handlePassengers(int a,int[] b){} public float getYaw(){return 0;} public float getPitch(){return 0;}
}''')
put('dev/meshac/player/CollisionState','''public class CollisionState {public record ServerAbilities(boolean canFly,float speed){} public void handlePlayerAbilities(ServerAbilities a){} public void processMovement(Object o){} public boolean isFrozen(){return false;} public double getSetbackX(){return 0;} public double getSetbackY(){return 0;} public double getSetbackZ(){return 0;} public void runAcknowledgedTasks(){} public void setSurroundingMaterials(Object o){} public void setDeepFloorMaterials(Object o){} public void recordFeetWaterlogged(){} public void recordHeadWaterlogged(){} public void setGroundMaterial(Object o){} public void setSubGroundMaterial(Object o){} public void setDeepGroundMaterial(Object o){} public void setAreaScanMaterials(Object o){} public void setNearbyEntities(Object o){} }''')
put('dev/meshac/config/MeshSettings','public class MeshSettings {public static String messagesFrozen="fixture";}')
put('dev/meshac/util/TextUtil','public class TextUtil {public static Object parse(String s){return s;}}')
put('dev/meshac/util/Vec3d','public record Vec3d(double x,double y,double z){}')
put('dev/meshac/util/Location','public record Location(Object level,double x,double y,double z,float yaw,float pitch){public double getX(){return x;} public double getY(){return y;} public double getZ(){return z;}}')
put('dev/meshac/event/MeshEvent','public class MeshEvent {}')
put('dev/meshac/event/MeshMoveEvent','public class MeshMoveEvent extends MeshEvent {dev.meshac.util.Location to; public MeshMoveEvent(Object p,dev.meshac.util.Location f,dev.meshac.util.Location t){to=t;} public dev.meshac.util.Location getTo(){return to;} public void setTo(dev.meshac.util.Location t){to=t;}}')
put('dev/meshac/event/MeshBlock','public class MeshBlock {public MeshBlock(Object l,Object p){}}')
put('dev/meshac/event/MeshInteractEvent','public class MeshInteractEvent extends MeshEvent {public enum Action {LEFT_CLICK_BLOCK} public MeshInteractEvent(Object...o){}}')
put('dev/meshac/world/BlockUtils','public class BlockUtils {public static java.util.List<net.minecraft.world.level.block.state.BlockState> getSurroundingMaterials(Object l,int x,int y,int z,int r){return java.util.List.of();} public static java.util.List<net.minecraft.world.level.block.state.BlockState> getDeepFloorMaterials(Object l,int x,int y,int z){return java.util.List.of();}}')
put('net/minecraft/core/BlockPos','public class BlockPos {public BlockPos(int x,int y,int z){} public BlockPos below(int y){return this;} public BlockPos above(int y){return this;} public BlockPos relative(Object d){return this;}}')
put('net/minecraft/world/level/block/state/properties/BlockStateProperties','public class BlockStateProperties {public static final Object WATERLOGGED=new Object();}')
put('net/minecraft/world/level/block/state/BlockState','public class BlockState {public boolean hasProperty(Object o){return false;} public boolean getValue(Object o){return false;}}')
put('net/minecraft/world/phys/Vec3','public class Vec3 {public double x,y,z; public Vec3(double a,double b,double c){x=a;y=b;z=c;}}')
put('net/minecraft/world/phys/AABB','public class AABB {public static AABB ofSize(Object p,double a,double b,double c){return new AABB();}}')
put('net/minecraft/world/entity/Entity','public class Entity {}')
put('net/minecraft/world/level/chunk/LevelChunk','public class LevelChunk {}')
put('net/minecraft/server/level/ServerLevel','''public class ServerLevel {public net.minecraft.world.level.block.state.BlockState getBlockState(Object p){return new net.minecraft.world.level.block.state.BlockState();} public java.util.List getEntities(Object p,Object b){return java.util.List.of();} public ServerLevel getServer(){return this;} public int getTickCount(){return 0;} public net.minecraft.world.entity.Entity getEntity(int i){return null;} public net.minecraft.world.level.chunk.LevelChunk getChunk(int x,int z){return new net.minecraft.world.level.chunk.LevelChunk();} public Object dimension(){return "fixture";}}''')
put('net/minecraft/server/level/ServerPlayer','''public class ServerPlayer {public ServerLevel level(){return new ServerLevel();} public double getX(){return 1;} public double getY(){return 2;} public double getZ(){return 3;} public float getYRot(){return 0;} public float getXRot(){return 0;} public Object getVehicle(){return null;} public Object position(){return null;} public void sendSystemMessage(Object o){} public Object getMainHandItem(){return null;} public net.minecraft.world.item.ItemStack getItemInHand(Object h){return new net.minecraft.world.item.ItemStack();}}''')
put('net/minecraft/world/InteractionHand','public enum InteractionHand {MAIN_HAND}')
put('net/minecraft/world/item/ItemStack','public class ItemStack {public Object getItem(){return new Object();}}')
put('net/minecraft/world/item/BlockItem','public class BlockItem {public BlockItem getBlock(){return this;} public Object defaultBlockState(){return null;}}')
put('dev/meshac/veil/Veil','public class Veil {public static void attacked(Object a,Object b,int t){}}')
put('dev/meshac/tracker/TrackedEntity','public class TrackedEntity {public double getX(){return 0;} public double getY(){return 0;} public double getZ(){return 0;}}')
put('dev/meshac/tracker/WorldBlockTracker','public class WorldBlockTracker {public interface ClientboundSectionUpdatesConsumer {void consume(java.util.function.BiConsumer<Object,Object> out);}}')
put('dev/meshac/mixin/ClientboundMoveEntityPacketAccessor','public interface ClientboundMoveEntityPacketAccessor {int meshac$getEntityId(); boolean meshac$hasPos(); boolean meshac$hasRot(); byte meshac$getYRot(); byte meshac$getXRot(); net.minecraft.network.protocol.game.VecDeltaCodec meshac$getDelta();}')
put('dev/meshac/packet/Flying','public class Flying {public boolean hasRotationChanged(){return false;} public float getYaw(){return 0;} public float getPitch(){return 0;}}')
put('dev/meshac/packet/InteractEntity','public class InteractEntity {public enum Action {ATTACK} public Action getAction(){return Action.ATTACK;} public int getEntityId(){return 1;}}')
put('dev/meshac/packet/EntityAction','public class EntityAction {}')
put('dev/meshac/packet/Digging','public class Digging {public enum Action {START_DIGGING} public Action getAction(){return Action.START_DIGGING;} public Object getBlockPosition(){return null;} public Object getBlockFace(){return null;}}')
put('dev/meshac/packet/Packets','''public class Packets {public static Flying toFlying(Object p,Object pl){return new Flying();} public static MeshPacketType flyingType(Object p){return MeshPacketType.SERVER_POSITION;} public static InteractEntity toInteractEntity(Object p){return new InteractEntity();} public static InteractEntity toAttackEntity(Object p){return new InteractEntity();} public static EntityAction toEntityAction(Object p){return null;} public static Digging toDigging(Object p){return new Digging();} public static Object toPlacement(Object p){return p;} public static Object toHeldItemChange(Object p){return p;}}''')
put('dev/meshac/packet/MeshPacketType','public enum MeshPacketType {SERVER_POSITION,INTERACT_ENTITY,ENTITY_ACTION,PLAYER_DIGGING,PLAYER_ABILITIES,PLAYER_BLOCK_PLACEMENT,HELD_ITEM_CHANGE,ANIMATION,KEEP_ALIVE,CLIENT_STATUS}')
put('dev/meshac/packet/MeshPacketEvent','public record MeshPacketEvent(MeshPacketType type,Object player,Object packet,boolean inbound) {}')
put('net/minecraft/network/protocol/Packet','public interface Packet<T> {}')
# Packet boundary doubles are data-only. Only position and placement are instantiated in this suite.
packets={
'ClientboundLoginPacket':'public int playerId(){return 1;}',
'ClientboundLevelChunkWithLightPacket':'public int x(){return 0;} public int z(){return 0;}',
'ClientboundForgetLevelChunkPacket':'public Object pos(){return null;}',
'ClientboundBlockUpdatePacket':'public Object getPos(){return null;} public Object getBlockState(){return null;}',
'ClientboundSectionBlocksUpdatePacket':'public void runUpdates(java.util.function.BiConsumer<Object,Object> c){}',
'ClientboundRespawnPacket':'',
'ClientboundExplodePacket':'public java.util.Optional<net.minecraft.world.phys.Vec3> playerKnockback(){return java.util.Optional.empty();}',
'ClientboundSetEntityMotionPacket':'public int id(){return 1;} public net.minecraft.world.phys.Vec3 movement(){return new net.minecraft.world.phys.Vec3(0,0,0);}',
'ClientboundUpdateAttributesPacket':'public int getEntityId(){return 1;}',
'ClientboundUpdateMobEffectPacket':'public int getEntityId(){return 1;} public Object getEffect(){return null;} public int getEffectAmplifier(){return 0;}',
'ClientboundRemoveMobEffectPacket':'public int entityId(){return 1;} public Object effect(){return null;}',
'ClientboundPlayerPositionPacket':'public ClientboundPlayerPositionPacket change(){return this;} public net.minecraft.world.phys.Vec3 position(){return new net.minecraft.world.phys.Vec3(0,0,0);}',
'ClientboundSetEntityDataPacket':'public int id(){return 1;}',
'ClientboundPlayerAbilitiesPacket':'public boolean canFly(){return false;} public float getFlyingSpeed(){return 0;}',
'ClientboundGameEventPacket':'public static final Object CHANGE_GAME_MODE=new Object(),START_RAINING=new Object(),STOP_RAINING=new Object(); public Object getEvent(){return null;} public float getParam(){return 0;}',
'ClientboundMoveEntityPacket':'',
'VecDeltaCodec':'public void setBase(net.minecraft.world.phys.Vec3 b){} public VecDeltaCodec decode(VecDeltaCodec c){return this;} public net.minecraft.world.phys.Vec3 endPosition(){return new net.minecraft.world.phys.Vec3(0,0,0);}',
'ClientboundTeleportEntityPacket':'public int id(){return 1;} public ClientboundTeleportEntityPacket change(){return this;} public net.minecraft.world.phys.Vec3 position(){return new net.minecraft.world.phys.Vec3(0,0,0);} public float yRot(){return 0;} public float xRot(){return 0;}',
'ClientboundAddEntityPacket':'public int getId(){return 1;} public Object getUUID(){return null;} public Object getType(){return null;} public double getX(){return 0;} public double getY(){return 0;} public double getZ(){return 0;}',
'ClientboundRemoveEntitiesPacket':'public ClientboundRemoveEntitiesPacket REMOVED_IDS_GETTER(){return this;} public int[] toIntArray(){return new int[0];}',
'ClientboundSetPassengersPacket':'public int getVehicle(){return 1;} public int[] getPassengers(){return new int[0];}',
'ServerboundUseItemOnPacket':'public Object hand(){return null;} public ServerboundUseItemOnPacket hitResult(){return this;} public net.minecraft.core.BlockPos getBlockPos(){return new net.minecraft.core.BlockPos(0,0,0);} public Object getDirection(){return null;}'
}
for name in ['ServerboundInteractPacket','ServerboundMovePlayerPacket','ServerboundPlayerActionPacket','ServerboundPlayerCommandPacket','ServerboundSetCarriedItemPacket','ServerboundPlayerAbilitiesPacket','ServerboundAttackPacket']: packets[name]=''
for name,body in packets.items():put('net/minecraft/network/protocol/game/'+name,'public class '+name+' implements net.minecraft.network.protocol.Packet<Object> {'+body+'}')
put('net/minecraft/network/protocol/common/ClientboundPingPacket','public class ClientboundPingPacket implements net.minecraft.network.protocol.Packet<Object> {public int getId(){return -1;}}')
stubs['dev/meshac/data/PlayerData.java']=stubs['dev/meshac/data/PlayerData.java'].replace('public class PlayerData {','public class PlayerData {public net.minecraft.server.level.ServerPlayer getPlayer(){return null;}')
stubs['net/minecraft/server/level/ServerPlayer.java']=stubs['net/minecraft/server/level/ServerPlayer.java'].replace('public class ServerPlayer {','public class ServerPlayer {public net.minecraft.server.network.ServerGamePacketListenerImpl connection;')
for variant,getter in VARIANTS.items():
 with tempfile.TemporaryDirectory(prefix='meshac-dispatch-') as tmp:
  put('net/minecraft/network/Connection','public class Connection {public Object getPacketListener(){return null;}}')
  put('net/minecraft/server/network/ServerGamePacketListenerImpl','public class ServerGamePacketListenerImpl {public net.minecraft.server.level.ServerPlayer player;}')
  put('dev/meshac/tracker/ConfirmationTransport','public class ConfirmationTransport {public static boolean isPlay(dev.meshac.data.PlayerData d,net.minecraft.network.Connection c){return false;}}')
  t=Path(tmp); files=[]
  for name,text in stubs.items():
   p=t/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text.replace('REMOVED_IDS_GETTER',getter));files.append(str(p))
  for name in ['check/ContextGate','check/CheckDependencies','config/MeshVersion','packet/PacketDispatcher','packet/OutboundDispatcher','event/MoveEventDispatcher']:
   files.append(str(ROOT/'src/main/java/dev/meshac'/(name+'.java')))
  files.append(str(ROOT/'sim/dispatch/DispatchSafetyTest.java'))
  subprocess.run([str(JDK/'javac'),'-d',str(t/'classes'),*files],check=True)
  subprocess.run([str(JDK/'jar'),'--create','--file',str(t/'fixture.jar'),'-C',str(t/'classes'),'.'],check=True)
  subprocess.run([str(JDK/'java'),'-cp',str(t/'fixture.jar'),'DispatchSafetyTest'],check=True,cwd=t)
  print('PASS variant-'+variant+' (getter '+getter+')')