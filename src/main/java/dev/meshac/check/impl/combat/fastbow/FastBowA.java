package dev.meshac.check.impl.combat.fastbow;

import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.check.ExemptionManager;
import dev.meshac.data.PlayerData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.server.level.ServerPlayer;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshProjectileLaunchEvent;

@CheckInfo(name = "Fast Bow", type = 'A', complexType = "Limit", description = "Shooting a bow too quickly")
public class FastBowA extends AbstractCheck {

    private long lastShotAtMillis;

    public FastBowA(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void update() {
    }

    @Override
    public void handleEvent(Event event) {
        if (!(event instanceof MeshProjectileLaunchEvent launch) || !(launch.getEntity() instanceof Arrow arrow))
            return;
        Player player = playerData.getPlayer();
        if (player == null)
            return;
        String mainHandType = player.getMainHandItem().getType().name();
        String offHandType = ExemptionManager.isAtLeast1_9() ? player.getOffhandItem().getType().name() : "";
        if (mainHandType.contains("CROSS") || offHandType.contains("CROSS"))
            return;
        if (!mainHandType.contains("BOW") && !offHandType.contains("BOW"))
            return;
        if (ExemptionManager.isAtLeast1_14() && (dev.meshac.util.EnchantUtil.level(player.getMainHandItem(), net.minecraft.world.item.enchantment.Enchantments.MULTISHOT) > 0 || dev.meshac.util.EnchantUtil.level(player.getOffhandItem(), net.minecraft.world.item.enchantment.Enchantments.MULTISHOT) > 0))
            return;
        double velocityLength = arrow.getVelocity().length();
        if (velocityLength <= 2.0)
            return;
        long now = System.currentTimeMillis();
        long shotDelay = now - lastShotAtMillis;
        if (shotDelay < 750L && !isExempt(new ExemptType[] { ExemptType.FAST, ExemptType.ELYTRA, ExemptType.GLIDING })) {
            if (increaseBuffer() > maximumBuffer)
                fail("length=" + velocityLength + " delay=" + shotDelay);
        } else
            decayBuffer();
        lastShotAtMillis = now;
    }
}
