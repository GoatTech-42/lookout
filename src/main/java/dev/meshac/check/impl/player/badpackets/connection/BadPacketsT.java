package dev.meshac.check.impl.player.badpackets.connection;

import dev.meshac.check.AbstractCheck;
import dev.meshac.check.ExemptType;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.data.PlayerData;
import dev.meshac.packet.MeshPacketEvent;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;

@CheckInfo(name = "Bad Packets", type = 'T', complexType = "Keep Alive", experimental = false, description = "Invalid KeepAlive packets.")
public class BadPacketsT extends AbstractCheck {

	private long lastKeepAliveId = -1L;

	public BadPacketsT(PlayerData playerData) {
		super(playerData);
	}

	@Override
	public void handlePacket(MeshPacketEvent packetEvent) {
		if (this.isKeepAlivePacket(packetEvent)) {
			ServerboundKeepAlivePacket packet = packetEvent.raw();
			long timestamp2 = packet.getId();
			boolean exempt = this.isExempt(new ExemptType[] { ExemptType.JOINED });
			if ((timestamp2 == this.lastKeepAliveId || timestamp2 == 0L) && !exempt) {
				this.fail("id=" + timestamp2 + " lastId=" + this.lastKeepAliveId);
			}
			this.lastKeepAliveId = timestamp2;
		}
	}

	@Override
	public void update() {
	}
}
