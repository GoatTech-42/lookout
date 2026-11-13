package dev.meshac.check;

import java.util.List;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.CheckData;
import dev.meshac.check.manager.CheckManager;
import dev.meshac.config.MeshSettings;

public final class CheckSettingsLoader {

    public static void clearCheckSettings() {
        MeshSettings.checkDataByName.clear();
        MeshSettings.checkEnabled.clear();
        MeshSettings.maximumViolations.clear();
        MeshSettings.alertIntervals.clear();
        MeshSettings.minimumAlertViolations.clear();
        MeshSettings.punishmentCommands.clear();
        MeshSettings.checkPunishable.clear();
        MeshSettings.broadcastPunishments.clear();
        MeshSettings.setbackEnabled.clear();
        MeshSettings.maximumBuffers.clear();
        MeshSettings.bufferDecays.clear();
        MeshSettings.bufferMultipliers.clear();
        MeshSettings.hotbarShuffleEnabled.clear();
        MeshSettings.minimumShuffleViolations.clear();
        MeshSettings.shuffleIntervals.clear();
        MeshSettings.randomRotationEnabled.clear();
        MeshSettings.minimumRotationViolations.clear();
        MeshSettings.rotationIntervals.clear();
        MeshSettings.discordAlertIntervals.clear();
        MeshSettings.minimumSetbackViolations.clear();
    }

    public static void loadCheckSettings() {
        Class<?>[] checkTypes = CheckManager.checkTypes;
        int checkCount = checkTypes.length;
        for (int checkIndex = 0; checkIndex < checkCount; ++checkIndex) {
            Class<?> checkType = checkTypes[checkIndex];
            CheckInfo information = checkType.getAnnotation(CheckInfo.class);
            String category = "";
            if (checkType.getName().contains("combat")) {
                category = "combat";
            } else if (checkType.getName().contains("movement")) {
                category = "movement";
            } else if (checkType.getName().contains("player")) {
                category = "player";
            }
            String checkName = information.name().replaceAll(" ", "").toLowerCase();
            char checkVariant = Character.toLowerCase(information.type());
            String path = "checks." + category + "." + checkName + "." + checkVariant + ".";
            boolean enabled = MeshSettings.getBoolean(path + "enabled");
            int maximumViolations = MeshSettings.getInt(path + "max-violations");
            int minimumAlertViolations = MeshSettings.getInt(path + "dont-alert-until");
            int alertInterval = MeshSettings.getInt(path + "alert-interval");
            int maximumPing = MeshSettings.getInt(path + "maximum-ping");
            double minimumTps = MeshSettings.getDouble(path + "minimum-tps");
            List<String> punishmentCommands = MeshSettings.getStringList(path + "punishment-commands");
            boolean punishable = MeshSettings.getBoolean(path + "punishable");
            boolean broadcastPunishment = MeshSettings.getBoolean(path + "broadcast-punishment");
            boolean setback = MeshSettings.getBoolean(path + "setback");
            int discordAlertInterval = MeshSettings.getInt(path + "discord-alert-interval");
            int minimumSetbackViolations = MeshSettings.getInt(path + "min-vl-to-setback");
            MeshSettings.checkEnabled.put(checkType.getSimpleName(), enabled);
            MeshSettings.maximumViolations.put(checkType.getSimpleName(), maximumViolations);
            MeshSettings.minimumAlertViolations.put(checkType.getSimpleName(), minimumAlertViolations);
            MeshSettings.alertIntervals.put(checkType.getSimpleName(), alertInterval);
            MeshSettings.maximumPing.put(checkType.getSimpleName(), maximumPing);
            MeshSettings.minimumTps.put(checkType.getSimpleName(), minimumTps);
            MeshSettings.punishmentCommands.put(checkType.getSimpleName(), punishmentCommands);
            MeshSettings.checkPunishable.put(checkType.getSimpleName(), punishable);
            MeshSettings.broadcastPunishments.put(checkType.getSimpleName(), broadcastPunishment);
            MeshSettings.setbackEnabled.put(checkType.getSimpleName(), setback);
            MeshSettings.discordAlertIntervals.put(checkType.getSimpleName(), discordAlertInterval);
            MeshSettings.minimumSetbackViolations.put(checkType.getSimpleName(), minimumSetbackViolations);
            int maximumBuffer = MeshSettings.getInt(path + "buffer.max");
            double bufferMultiplier = MeshSettings.getDouble(path + "buffer.multiple");
            double bufferDecay = MeshSettings.getDouble(path + "buffer.decay");
            MeshSettings.maximumBuffers.put(checkType.getSimpleName(), maximumBuffer);
            MeshSettings.bufferMultipliers.put(checkType.getSimpleName(), bufferMultiplier);
            MeshSettings.bufferDecays.put(checkType.getSimpleName(), bufferDecay);
            boolean hotbarShuffle = MeshSettings.getBoolean(path + "hotbar-shuffle.enabled");
            int minimumShuffleViolations = MeshSettings.getInt(path + "hotbar-shuffle.minimum-vl-to-shuffle");
            int shuffleInterval = MeshSettings.getInt(path + "hotbar-shuffle.shuffle-every");
            MeshSettings.hotbarShuffleEnabled.put(checkType.getSimpleName(), hotbarShuffle);
            MeshSettings.minimumShuffleViolations.put(checkType.getSimpleName(), minimumShuffleViolations);
            MeshSettings.shuffleIntervals.put(checkType.getSimpleName(), shuffleInterval);
            boolean randomRotation = MeshSettings.getBoolean(path + "random-rotation.enabled");
            int minimumRotationViolations = MeshSettings.getInt(path + "random-rotation.minimum-vl-to-randomly-rotate");
            int rotationInterval = MeshSettings.getInt(path + "random-rotation.rotate-every");
            MeshSettings.randomRotationEnabled.put(checkType.getSimpleName(), randomRotation);
            MeshSettings.minimumRotationViolations.put(checkType.getSimpleName(), minimumRotationViolations);
            MeshSettings.rotationIntervals.put(checkType.getSimpleName(), rotationInterval);
            if (!MeshSettings.settingsEnableApi)
                continue;
            CheckData checkData = new CheckData(checkType.getSimpleName());
            checkData.setEnabled(enabled);
            checkData.setPunishable(punishable);
            checkData.setBroadcastPunishment(broadcastPunishment);
            checkData.setHotbarShuffle(hotbarShuffle);
            checkData.setRandomRotation(randomRotation);
            checkData.setMaxViolations(maximumViolations);
            checkData.setAlertInterval(alertInterval);
            checkData.setMinimumVlToNotify(minimumAlertViolations);
            checkData.setMaxPing(maximumPing);
            checkData.setRandomRotationInterval(rotationInterval);
            checkData.setMinimumVlToRandomlyRotate(minimumRotationViolations);
            checkData.setMinimumVlToShuffleHotbar(minimumShuffleViolations);
            checkData.setHotbarShuffleInterval(shuffleInterval);
            checkData.setMinimumTps(minimumTps);
            checkData.setMaxBuffer((double) maximumBuffer);
            checkData.setBufferDecay(bufferDecay);
            checkData.setBufferMultiple(bufferMultiplier);
            checkData.setPunishmentCommands(punishmentCommands);
            MeshSettings.checkDataByName.put(checkType.getSimpleName(), checkData);
        }
    }

    private CheckSettingsLoader() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
