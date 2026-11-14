package dev.meshac.product;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;

/** meshac's own case store (config/meshac-cases.json). Kicks, tempbans and bans all live here, with the evidence that caused them. */
public final class Cases {
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("meshac");
	public static final class Case {
		public String id, player, uuid, action, reason, by;
		public long at, until;          // until: 0 = no expiry (kick, permanent ban)
		public boolean pardoned, lifted;
		public List<String> evidence = new ArrayList<>();
	}
	private static final Gson G = new GsonBuilder().setPrettyPrinting().create();
	/** Randomness behind case-id minting; package-private and replaceable so tests can force collisions deterministically. */
	static java.util.Random CASE_ID_RNG = new java.util.Random();
	private static List<Case> all = new ArrayList<>();
	private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("meshac-cases.json"); }

	public static synchronized void load() {
		try { if (Files.exists(file())) all = G.fromJson(Files.readString(file()), new TypeToken<List<Case>>() {}.getType()); }
		catch (Exception e) { LOG.warn("[meshac] could not read cases: {}", e.toString()); }
		if (all == null) all = new ArrayList<>();
	}
	private static void save() { try { Files.writeString(file(), G.toJson(all)); } catch (Exception e) { LOG.warn("[meshac] could not save cases: {}", e.toString()); } }

	public static synchronized Case add(String player, UUID uuid, String action, String reason, String by, long until, List<String> evidence) {
		Case c = new Case();
		c.player = player; c.uuid = uuid.toString(); c.action = action; c.reason = reason; c.by = by;
		c.at = System.currentTimeMillis(); c.until = until; c.evidence = new ArrayList<>(evidence);
		if (dev.meshac.safety.Containment.mode() != dev.meshac.safety.Containment.Mode.ENFORCE) {
			c.id = "SIMULATED"; // simulation allocates no real id: the unique-mint path (and its failure modes) is ENFORCE-only
			LOG.info("[meshac] simulated case {} {} (not stored)", action, player);
			return c;
		}
		c.id = newId();
		all.add(c); save(); return c;
	}
	/** Active ban for this player, or null. Permanent bans have action "ban" and until 0; tempbans expire. */
	public static synchronized Case activeBan(UUID id) {
		long now = System.currentTimeMillis();
		for (int i = all.size() - 1; i >= 0; i--) {
			Case c = all.get(i);
			if (!c.uuid.equals(id.toString()) || c.pardoned || c.lifted) continue;
			if (c.action.equals("ban") || (c.action.equals("tempban") && c.until > now)) return c;
		}
		return null;
	}
	/** meshac's own removals (kicks, tempbans, bans) inside the memory window that were not pardoned; this is what escalation counts. Staff actions do not count. */
	public static synchronized double offences(UUID id) {
		double hl = Config.get().offenceHalfLifeDays(); long now = System.currentTimeMillis(); java.util.List<Double> ages = new java.util.ArrayList<>();
		for (Case c : all) if (c.uuid.equals(id.toString()) && !c.pardoned && c.by.equals("meshac") && now - c.at < hl * 8 * 86_400_000L) ages.add((now - c.at) / 86_400_000.0);
		double[] a = new double[ages.size()]; for (int k = 0; k < a.length; k++) a[k] = ages.get(k);
		return Ladder.effectiveOffences(a, hl);
	}
	/** Lifts every active ban on this player but keeps the offences on record, so the next one still escalates. Returns how many were lifted. */
	public static synchronized int unban(String name) {
		if (dev.meshac.safety.Containment.mode() != dev.meshac.safety.Containment.Mode.ENFORCE) return 0;
		long now = System.currentTimeMillis(); int n = 0;
		for (Case c : all) if (c.player.equalsIgnoreCase(name) && !c.pardoned && !c.lifted && (c.action.equals("ban") || (c.action.equals("tempban") && c.until > now))) { c.lifted = true; n++; }
		if (n > 0) save();
		return n;
	}
	/** All cases carrying this id, in record order. Legacy stores can contain duplicate ids (the old 4-char minter had no uniqueness check); callers must handle more than one. */
	public static synchronized List<Case> findAll(String id) { List<Case> o = new ArrayList<>(); for (Case c : all) if (c.id.equalsIgnoreCase(id)) o.add(c); return o; }
	/** Compatibility lookup for legacy callers (sim/safety tests): returns the record only when exactly ONE
	 *  stored case carries this id; duplicated historical ids yield null so no caller can silently choose one.
	 *  New code should call findAll and handle ambiguity explicitly. */
	public static synchronized Case find(String id) { List<Case> m = findAll(id); return m.size() == 1 ? m.get(0) : null; }
	public enum PardonResult { PARDONED, NOT_FOUND, AMBIGUOUS }
	/** Pardons the case with this id. Refuses - without mutating anything - unless exactly ONE stored record
	 *  carries it: a duplicated historical id stays ambiguous even when pardons/expiry leave a single live case,
	 *  so an operator's original case reference can never change meaning over time. */
	public static synchronized PardonResult pardon(String id) {
		if (dev.meshac.safety.Containment.mode() != dev.meshac.safety.Containment.Mode.ENFORCE) return PardonResult.NOT_FOUND;
		List<Case> matches = findAll(id);
		if (matches.isEmpty()) return PardonResult.NOT_FOUND;
		if (matches.size() > 1) return PardonResult.AMBIGUOUS;
		Case target = matches.get(0);
		if (target.pardoned) return PardonResult.NOT_FOUND; // already pardoned
		target.pardoned = true; save(); return PardonResult.PARDONED;
	}
	/** Mints a case id guaranteed unique against every stored case (live or historical). 6 chars from a 31-char alphabet (887,503,681 combinations), retried on collision. */
	private static String newId() {
		String abc = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
		java.util.Random r = CASE_ID_RNG;
		for (int attempt = 0; attempt < 100; attempt++) {
			StringBuilder b = new StringBuilder("MA-");
			for (int i = 0; i < 6; i++) b.append(abc.charAt(r.nextInt(abc.length())));
			String id = b.toString(); boolean taken = false;
			for (Case c : all) if (c.id.equalsIgnoreCase(id)) { taken = true; break; }
			if (!taken) return id;
		}
		throw new IllegalStateException("could not mint a unique case id after 100 attempts");
	}
	public static synchronized List<Case> recent(int n) { return new ArrayList<>(all.subList(Math.max(0, all.size() - n), all.size())); }
	public static synchronized List<Case> of(String name) { List<Case> o = new ArrayList<>(); for (Case c : all) if (c.player.equalsIgnoreCase(name)) o.add(c); return o; }
}

