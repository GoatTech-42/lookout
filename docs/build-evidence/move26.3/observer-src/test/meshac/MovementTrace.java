package test.meshac;
public final class MovementTrace {
 private static final java.util.Map<Object,Integer> ids=java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<>());private static int next;
 public static synchronized void record(String stage,Object packet){int id=ids.computeIfAbsent(packet,p->++next);System.out.println("[MOVE-OBSERVER] "+stage+" packet="+id+" thread="+Thread.currentThread().getName());}
 public static void capture(Object packet,net.minecraft.server.level.ServerPlayer p){record("SERVER_CAPTURE",packet);System.out.println("[MOVE-OBSERVER] CAPTURE_COORD x="+p.getX()+" y="+p.getY()+" z="+p.getZ()+" thread="+Thread.currentThread().getName());}
}
