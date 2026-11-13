package dev.meshac.check.impl.player.badpackets.inventory;

import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = 'W', complexType = "Post WindowClick", experimental = false, description = "Post WindowClick packets.")
public class BadPacketsW extends AbstractCheck {

    public BadPacketsW(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void update() {
    }

    @Override
    public void applyConfiguredMitigation() {
    }
}
