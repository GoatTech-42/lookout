package dev.meshac.check.impl.player.fastplace;

import java.util.ArrayDeque;
import java.util.Deque;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshBlockPlaceEvent;

@CheckInfo(name = "Fast Place", type = 'A', complexType = "Delay", description = "Placing blocks too quickly.")
public class FastPlaceA extends AbstractCheck {

    private final Deque<Long> placementTimes = new ArrayDeque<>();

    public FastPlaceA(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void update() {
    }

    @Override
    public void handleEvent(MeshEvent event) {
        if (!(event instanceof MeshBlockPlaceEvent placement) || placement.isCancelled() || playerData.getPlayer() == null)
            return;
        if (isExempt(new ExemptType[] { ExemptType.CANCELLED_BREAK, ExemptType.VEHICLE }))
            return;
        long now = System.currentTimeMillis();
        while (!placementTimes.isEmpty() && now - placementTimes.peekFirst() >= 1000L) placementTimes.removeFirst();
        placementTimes.addLast(now);
        if (placementTimes.size() > MeshSettings.checksPlayerFastplaceAMaxBlocks) {
            fail("amount=" + placementTimes.size());
            placementTimes.clear();
        }
    }
}
