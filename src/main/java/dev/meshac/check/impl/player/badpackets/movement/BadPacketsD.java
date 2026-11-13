package dev.meshac.check.impl.player.badpackets.movement;

import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = 'D', complexType = "Timer", description = "Too many flying packets.", experimental = true)
public class BadPacketsD extends AbstractCheck {

    public BadPacketsD(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void update() {
    }

    @Override
    public void applyConfiguredMitigation() {
    }
}
