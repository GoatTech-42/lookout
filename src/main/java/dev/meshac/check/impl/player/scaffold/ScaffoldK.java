package dev.meshac.check.impl.player.scaffold;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.EntityAction;
import dev.meshac.packet.EntityAction.Action;
import java.util.HashMap;
import java.util.Map;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.CheckUtils;
import dev.meshac.check.ExemptType;
import dev.meshac.check.ExemptionManager;
import dev.meshac.config.MeshSettings;
import dev.meshac.player.MotionState;
import dev.meshac.data.PlayerData;
import dev.meshac.world.BlockUtils;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshBlockPlaceEvent;

@CheckInfo(name = "Scaffold", type = 'K', complexType = "Limit", description = "Bridging too quickly.")
public class ScaffoldK extends AbstractCheck {

    private int lastSneakTick;

    private final Map cache;

    private int placementCount;

    public ScaffoldK(PlayerData playerData) {
        super(playerData);
        int index = 0;
        this.cache = new HashMap();
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isEntityActionPacket(packetEvent)) {
            EntityAction wrapperPlayClientEntityAction = this.playerData.getLastEntityActionPacket();
            if (wrapperPlayClientEntityAction.getAction() == Action.START_SNEAKING || wrapperPlayClientEntityAction.getAction() == Action.STOP_SNEAKING) {
                this.lastSneakTick = this.getCurrentTick();
            }
        }
    }

    public void handleEvent(MeshEvent event2) {
        if (event2 instanceof MeshBlockPlaceEvent blockPlaceEvent) {
            if (blockPlaceEvent.isCancelled() || BlockUtils.isSlab(blockPlaceEvent.getBlock().getType())) {
                return;
            }
            if (this.isExempt(new ExemptType[] { ExemptType.SLAB })) {
                return;
            }
            if (ExemptionManager.isAtLeast1_19()) {
                MotionState motionState = this.playerData.getMotionState();
                if (motionState.isSwiftSneak()) {
                    return;
                }
            }
            if (CheckUtils.isValidBlockPlacement(blockPlaceEvent) && this.getCurrentTick() - this.lastSneakTick > 20) {
                this.placementCount++;
                this.cache.put(this.placementCount, System.currentTimeMillis());
                if (System.currentTimeMillis() - (Long) this.cache.get(1) > 1000L) {
                    int cacheSize = this.cache.size();
                    if (cacheSize >= MeshSettings.checksPlayerScaffoldKMaxBlocks) {
                        if (this.increaseBuffer() > this.maximumBuffer) {
                            this.fail("amount=" + cacheSize);
                        }
                    } else {
                        this.decayBuffer();
                    }
                    this.cache.clear();
                    this.placementCount = 0;
                }
            }
        }
    }
}
