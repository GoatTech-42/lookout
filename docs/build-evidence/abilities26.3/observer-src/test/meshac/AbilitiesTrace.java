package test.meshac;
public final class AbilitiesTrace {
 private static final java.util.Map<Object,Integer> ids=new java.util.IdentityHashMap<>();private static int next;
 public static synchronized void record(String stage,net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket p){int id=ids.computeIfAbsent(p,k->++next);System.out.println("[ABILITIES-OBSERVER] "+stage+" packet="+id+" flying="+p.isFlying()+" thread="+Thread.currentThread().getName());}
}