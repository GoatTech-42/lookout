package dev.meshac.mcutil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import dev.meshac.check.ExemptionManager;
import dev.meshac.config.MeshSettings;

public final class ChatColorUtils {

    private static final char COLOR_CHAR = '§';
    private static final Pattern HEX = Pattern.compile("#[a-fA-F0-9]{6}");
    private static final String ALL_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

    public static String colorize(String message) {
        String colored = message.replace("%prefix%", MeshSettings.prefix);
        if (ExemptionManager.isAtLeast1_16()) {
            Matcher matches = HEX.matcher(colored);
            StringBuffer result = new StringBuffer();
            while (matches.find()) matches.appendReplacement(result, Matcher.quoteReplacement(toLegacyHex(matches.group())));
            matches.appendTail(result);
            colored = result.toString();
        }
        return translateAlternateColorCodes('&', colored);
    }

    // Bungee ChatColor.of(hex).toString() equivalent: §x§R§R§G§G§B§B
    private static String toLegacyHex(String hex) {
        StringBuilder sb = new StringBuilder(14);
        sb.append(COLOR_CHAR).append('x');
        for (int i = 1; i < 7; i++) sb.append(COLOR_CHAR).append(hex.charAt(i));
        return sb.toString();
    }

    // Bungee ChatColor.translateAlternateColorCodes equivalent
    private static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && ALL_CODES.indexOf(b[i + 1]) > -1) {
                b[i] = COLOR_CHAR;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    private ChatColorUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
