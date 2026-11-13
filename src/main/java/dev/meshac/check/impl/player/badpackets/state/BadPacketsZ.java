package dev.meshac.check.impl.player.badpackets.state;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptionManager;
import dev.meshac.data.PlayerData;
import net.minecraft.world.level.GameType;

@CheckInfo(name = "Bad Packets", type = 'Z', complexType = "Spectate", description = "Invalid Spectate Packets.")
public class BadPacketsZ extends AbstractCheck {

    public BadPacketsZ(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        boolean condition;
        if (this.isSpectatePacket(packetEvent)) {
            if (ExemptionManager.isBefore1_8()) {
                return;
            }
            if (this.playerData.getPlayer() == null) {
                return;
            }
            condition = ((this.playerData.getPlayer().gameMode.getGameModeForPlayer() != GameType.SPECTATOR) && (this.playerData.getMotionState().getGameMode() != GameType.SPECTATOR));
            boolean condition2 = condition;
            if (condition2) {
                this.fail();
            }
        }
    }

    @Override
    public void update() {
    }
}
