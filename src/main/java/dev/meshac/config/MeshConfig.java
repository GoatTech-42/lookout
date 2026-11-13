package dev.meshac.config;

import dev.meshac.MeshServices;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Flattened-properties config backing MeshSettings. The bundled resource is generated from the
 * shipped default configuration; an optional config/meshac.properties overrides bundled values.
 */
public final class MeshConfig {

	private static final Properties values = new Properties();

	private MeshConfig() {
	}

	public static void load() {
		values.clear();
		try (InputStream in = MeshConfig.class.getResourceAsStream("/meshac-config.properties")) {
			if (in == null) throw new IllegalStateException("missing bundled meshac-config.properties");
			values.load(in);
		} catch (IOException e) {
			throw new UncheckedIOException("cannot read bundled config", e);
		}
		java.nio.file.Path override = java.nio.file.Path.of("config", "meshac.properties");
		if (java.nio.file.Files.isRegularFile(override)) {
			try (InputStream in = java.nio.file.Files.newInputStream(override)) {
				values.load(in);
			} catch (IOException e) {
				throw new UncheckedIOException("cannot read " + override, e);
			}
		}
		// unescape unicode and newline sequences written by the flattener
		Properties decoded = new Properties();
		for (String name : values.stringPropertyNames()) {
			decoded.setProperty(name, decode(values.getProperty(name)));
		}
		values.clear();
		values.putAll(decoded);
	}

	private static String decode(String s) {
		StringBuilder out = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\' && i + 1 < s.length()) {
				char n = s.charAt(i + 1);
				if (n == 'u' && i + 5 < s.length()) {
					out.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
					i += 5;
					continue;
				}
				if (n == 'n') { out.append('\n'); i++; continue; }
				if (n == 't') { out.append('\t'); i++; continue; }
				if (n == '\\') { out.append('\\'); i++; continue; }
			}
			out.append(c);
		}
		return out.toString();
	}

	public static boolean getBoolean(String path) {
		return Boolean.parseBoolean(values.getProperty(path, "false").trim());
	}

	public static String getString(String path) {
		String v = values.getProperty(path);
		if (v != null && v.startsWith("[")) return v; // list stored in a string slot; leave raw
		return v;
	}

	public static int getInt(String path) {
		String v = values.getProperty(path, "0").trim();
		try { return Integer.parseInt(v); } catch (NumberFormatException e) {
			try { return (int) Double.parseDouble(v); } catch (NumberFormatException e2) { return 0; }
		}
	}

	public static double getDouble(String path) {
		String v = values.getProperty(path, "0").trim();
		try { return Double.parseDouble(v); } catch (NumberFormatException e) { return 0.0; }
	}

	public static long getLong(String path) {
		String v = values.getProperty(path, "0").trim();
		try { return Long.parseLong(v); } catch (NumberFormatException e) {
			try { return (long) Double.parseDouble(v); } catch (NumberFormatException e2) { return 0L; }
		}
	}

	public static List<String> getStringList(String path) {
		String v = values.getProperty(path);
		List<String> out = new ArrayList<>();
		if (v == null || v.isBlank()) return out;
		String t = v.trim();
		if (t.startsWith("[") && t.endsWith("]")) {
			if (t.length() <= 2) return out;
			// minimal JSON string-array reader
			int i = 1;
			while (i < t.length() - 1) {
				while (i < t.length() - 1 && (t.charAt(i) == ' ' || t.charAt(i) == ',')) i++;
				if (i >= t.length() - 1) break;
				if (t.charAt(i) == '"') {
					StringBuilder sb = new StringBuilder();
					i++;
					while (i < t.length() && t.charAt(i) != '"') {
						if (t.charAt(i) == '\\' && i + 1 < t.length()) { i++; sb.append(unescapeJson(t.charAt(i))); }
						else sb.append(t.charAt(i));
						i++;
					}
					i++;
					out.add(sb.toString());
				} else {
					int end = t.indexOf(',', i);
					if (end < 0 || end > t.length() - 1) end = t.length() - 1;
					out.add(t.substring(i, end).trim());
					i = end + 1;
				}
			}
			return out;
		}
		out.add(v);
		return out;
	}

	private static char unescapeJson(char c) {
		return switch (c) {
			case 'n' -> '\n';
			case 't' -> '\t';
			case 'r' -> '\r';
			default -> c;
		};
	}

	public static boolean isString(String path) {
		return values.containsKey(path);
	}
}
