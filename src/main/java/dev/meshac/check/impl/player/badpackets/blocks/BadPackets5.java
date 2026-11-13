package dev.meshac.check.impl.player.badpackets.blocks;

import dev.meshac.check.AbstractCheck;
import dev.meshac.check.ExemptType;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.data.PlayerData;
import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.Placement;

@CheckInfo(name = "Bad Packets", type = '5', complexType = "Scaffold", description = "Invalid Block place.")
public class BadPackets5 extends AbstractCheck {

	public BadPackets5(PlayerData playerData) {
		super(playerData);
	}

	@Override
	public void handlePacket(MeshPacketEvent packetEvent) {
		if (this.isBlockPlacementPacket(packetEvent)) {
			Placement lastPlacementPacket = this.playerData.getLastPlacementPacket();
			boolean exempt = this.isExempt(new ExemptType[] { ExemptType.CREATIVE, ExemptType.CLIENT_VERSION });
			if (exempt) {
				return;
			}
			float x = lastPlacementPacket.getCursorX();
			float y = lastPlacementPacket.getCursorY();
			float z = lastPlacementPacket.getCursorZ();
			if (x > 1.0F || y > 1.0F || z > 1.0F || x < 0.0F || y < 0.0F || z < 0.0F) {
				this.fail("x=" + x + " y=" + y + " z=" + z);
			}
		}
	}

	@Override
	public void update() {
	}
}
