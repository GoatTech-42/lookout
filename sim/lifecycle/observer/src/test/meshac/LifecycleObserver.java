package test.meshac;
import dev.meshac.MeshServices;
import dev.meshac.data.PlayerDataManager;
import dev.meshac.check.ContextGate;
import dev.meshac.safety.Containment;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
public final class LifecycleObserver implements DedicatedServerModInitializer {
 private String last=""; private int copies;
 public void onInitializeServer(){
  System.out.println("[TEST-OBSERVER] separate read-only lifecycle observer present; shipping artifact unchanged");
  ServerPlayConnectionEvents.JOIN.register((h,s,server)->System.out.println("[TEST-OBSERVER] JOIN callback native="+h.getPlayer().getId()));
  ServerPlayConnectionEvents.DISCONNECT.register((h,server)->System.out.println("[TEST-OBSERVER] DISCONNECT callback uuid="+h.getPlayer().getUUID()));
  ServerPlayerEvents.COPY_FROM.register((old,p,alive)->{ copies++; System.out.println("[TEST-OBSERVER] COPY_FROM oldId="+old.getId()+" newId="+p.getId()+" sessionRefNew="+(PlayerDataManager.get(p)!=null&&PlayerDataManager.get(p).getPlayer()==p)); });
  ServerTickEvents.END_SERVER_TICK.register(server->{
   var text=new StringBuilder("copyCount="+copies+" sessions="+MeshServices.players().size()+" nativeOnline="+server.getPlayerList().getPlayers().size()+" mode="+Containment.mode()+" veil="+dev.meshac.veil.Veil.xrayOn+","+dev.meshac.veil.Veil.espOn+","+dev.meshac.veil.Veil.cullMobs);
   for(var p:server.getPlayerList().getPlayers()){
    var d=PlayerDataManager.get(p);text.append(" player="+p.getName().getString()+" nativeId="+p.getId());
    if(d==null)text.append(" NO_SESSION");else{
     long gated=d.getChecks().stream().filter(c->ContextGate.isGated(c.getClass().getSimpleName(),d)).count();
     text.append(" storedId="+d.getEntityId()+" uuidMatch="+p.getUUID().equals(d.getUniqueId())+" name="+d.getPlayerName()+" refCurrent="+(d.getPlayer()==p)+" gated="+gated+"/"+d.getChecks().size()+" version="+d.getClientVersion());
    }
   }
   String value=text.toString();if(!value.equals(last)){System.out.println("[TEST-OBSERVER] "+value);last=value;}
  });
 }
}
