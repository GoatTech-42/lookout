package dev.meshac.util;

import net.minecraft.network.chat.Component;

/** Chat text with legacy &-color parsing. */
public final class TextUtil {

	private TextUtil() {
	}

	public static Component parse(String text) {
		if (text == null) return Component.empty();
		return Component.literal(text.replace('&', '\u00a7'));
	}

	public static String colorize(String text) {
		return text == null ? "" : text.replace('&', '\u00a7');
	}
}
