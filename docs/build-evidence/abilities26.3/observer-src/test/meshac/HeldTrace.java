package test.meshac;
public final class HeldTrace {
 private static final java.util.Map<Object,Integer> ids=new java.util.IdentityHashMap<>();private static int next;
 public static synchronized void record(String stage,net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket p){int id=ids.computeIfAbsent(p,k->++next);System.out.println("[HELD-OBSERVER] "+stage+" packet="+id+" slot="+p.getSlot()+" thread="+Thread.currentThread().getName());}
}