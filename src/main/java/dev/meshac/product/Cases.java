package dev.meshac.product;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;

/** meshac's own case store (config/meshac-cases.json). Kicks, tempbans and bans all live here, with the evidence that caused them. */
public final class Cases {
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("meshac");
	public static final class Case {
		public String id, player, uuid, action, reason, by;
		public long at, until;          // until: 0 = no expiry (kick, permanent ban)
		public boolean pardoned, lifted;
		public transient boolean persisted = true; // false when the store write for this record failed (in memory only)
		public List<String> evidence = new ArrayList<>();
	}
	private static final Gson G = new GsonBuilder().setPrettyPrinting().create();
	/** Randomness behind case-id minting; package-private and replaceable so tests can force collisions deterministically. */
	static java.util.Random CASE_ID_RNG = new java.util.Random();
	private static List<Case> all = new ArrayList<>();
	private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("meshac-cases.json"); }
	private static Path backupFile() { return file().resolveSibling("meshac-cases.json.bak"); }
	/** Name of the meshac-cases.json.corrupt-<ts>-<hash> evidence copy made by the most recent failed load, or null. */
	public static volatile String lastPreservedCorrupt = null;
	/** True when the most recent commit failed with an undetermined on-disk outcome (a rename that threw
	 *  after possibly taking effect, and the target matched neither the new nor the old content). Memory
	 *  then intentionally keeps the mutation and commands report uncertainty instead of "nothing changed";
	 *  /mesh reload reconciles. Cleared by a successful save or load. */
	public static volatile boolean lastCommitUncertain = false;
	/** True when the most recent BACKUP rotation rename had an undetermined outcome. The main store was
	 *  never committed in that case, so removals roll back cleanly, but further writes are blocked until
	 *  /mesh reload reconciles (main is re-read and the next save re-rotates the backup atomically). */
	public static volatile boolean lastBackupUncertain = false;
	/** Unsaved (persisted=false) in-memory cases carried through the most recent load(); for /mesh reload wording. */
	public static volatile int lastMergedUnsaved = 0;

	public enum LoadResult { OK, MISSING, RECOVERED_FROM_BACKUP, FAILED }

	/** Loads the store into a validated CANDIDATE and only then publishes it; the live in-memory store is
	 *  never touched by a bad file. A record is invalid when id/player/uuid/action is null or blank, the
	 *  uuid does not parse or is not canonical (string matching elsewhere would silently miss it), the
	 *  action is not a known value (kick/tempban/ban - an unknown action would be silently inert), or a
	 *  tempban has until <= 0; null reason/evidence/by are legacy-tolerated and normalized. ANY invalid
	 *  record, a null list, a blank file, or the literal "null" makes the whole file corrupt - partial
	 *  salvage would silently retire bans. A corrupt file is copied to meshac-cases.json.corrupt-<ts>-<hash>
	 *  BEFORE any later save can overwrite it (deduped by content hash, so repeated reloads do not pile up
	 *  copies), then the last-good backup is tried: RECOVERED_FROM_BACKUP when it parses, FAILED otherwise.
	 *  If the corrupt file cannot be preserved, evidencePending blocks every later save until preservation
	 *  succeeds. FAILED during a running session keeps the previous in-memory cases in force; at cold start
	 *  the in-memory store is empty, so NOTHING is enforced until the store is repaired from the preserved
	 *  copies - that is not fail-closed enforcement and the log says so. A missing file is an empty store
	 *  (MISSING), not an error; when in-memory cases existed, this clearing is warned about. */
	/** Unsaved in-memory cases (persisted=false) are NEVER silently dropped by a reload: they are staff
	 *  enforcement decisions the dirty contract promised to protect. On every definitive read outcome they
	 *  are carried into the newly loaded memory (skipping duplicate ids), kept marked unsaved, and persist
	 *  on the next successful save. No automatic save is triggered here: after reconciliation the truth is
	 *  the loaded disk state plus these still-unsaved cases, and the normal save path owns persistence. */
	public static synchronized LoadResult load() {
		List<Case> dirty = new ArrayList<>();
		for (Case c : all) if (!c.persisted) dirty.add(c);
		lastMergedUnsaved = 0;
		Path f = file();
		if (!Files.exists(f)) {
			if (!all.isEmpty()) LOG.warn("[meshac] case store file {} is gone; clearing {} in-memory case(s)", f, all.size() - dirty.size());
			all = new ArrayList<>(); mergeDirty(dirty);
			lastCommitUncertain = false; lastBackupUncertain = false; return LoadResult.MISSING;
		}
		List<Case> candidate = readValidStore(f);
		if (candidate != null) { all = candidate; mergeDirty(dirty); lastCommitUncertain = false; lastBackupUncertain = false; return LoadResult.OK; }
		lastPreservedCorrupt = preserve(f); // best-effort forensics; save() re-guards before any overwrite
		List<Case> backup = Files.exists(backupFile()) ? readValidStore(backupFile()) : null;
		if (backup != null) {
			all = backup; mergeDirty(dirty);
			lastCommitUncertain = false; lastBackupUncertain = false;
			LOG.warn("[meshac] case store {} was unreadable; recovered {} case(s) from the last good backup (the newest CHANGES - cases, lifts and pardons - may be missing); unreadable file {}", f, backup.size(), preservedWording());
			return LoadResult.RECOVERED_FROM_BACKUP;
		}
		LOG.warn("[meshac] case store {} is unreadable and no usable backup exists - a single invalid record makes the whole store unreadable; unreadable file {}; {} in-memory case(s) remain in force (at startup that is none: ZERO historical cases are enforced after this restart until the store is repaired from the preserved copy)", f, preservedWording(), all.size());
		return LoadResult.FAILED;
	}
	private static void mergeDirty(List<Case> dirty) {
		// Dedup ONLY exact record duplicates (same id AND same content). Legacy ids are not unique, so an id
		// alone is not identity: when a DIFFERENT stored record carries the same id, the dirty case is KEPT
		// (enforcement is never silently dropped on a conflict) and the resulting duplicate id is handled by
		// the normal ambiguity refusals in find/pardon, never by silently choosing one record.
		for (Case c : dirty) {
			boolean exactDup = false, conflict = false;
			for (Case x : all) if (x.id.equalsIgnoreCase(c.id)) { if (sameRecord(x, c)) exactDup = true; else conflict = true; }
			if (exactDup) continue; // the identical record is already on disk; the dirty copy is redundant (any OTHER differing same-id record stays as-is in the loaded store - it is not this case)
			all.add(c); lastMergedUnsaved++;
			if (conflict) LOG.warn("[meshac] unsaved case {} conflicts with a DIFFERENT stored record carrying the same id; both are kept and the id is ambiguous (pardon by id will refuse) until staff resolve it", c.id);
		}
		if (lastMergedUnsaved > 0) LOG.warn("[meshac] {} unsaved case(s) carried through the reload; still enforced, still unsaved - they persist on the next successful save", lastMergedUnsaved);
	}
	/** Full content equality over the durable record fields (the transient persisted flag excluded). */
	private static boolean sameRecord(Case a, Case b) {
		return a.id.equalsIgnoreCase(b.id) && a.player.equals(b.player) && a.uuid.equals(b.uuid) && a.action.equals(b.action)
			&& java.util.Objects.equals(a.reason, b.reason) && java.util.Objects.equals(a.by, b.by)
			&& a.at == b.at && a.until == b.until && a.pardoned == b.pardoned && a.lifted == b.lifted
			&& java.util.Objects.equals(a.evidence, b.evidence);
	}
	private static String preservedWording() {
		return lastPreservedCorrupt != null ? "preserved as " + lastPreservedCorrupt
			: "could NOT be preserved; all saves are BLOCKED until it can be preserved (is the config directory writable?)";
	}
	/** Parses and fully validates a store file; returns null when anything is structurally wrong. */
	private static List<Case> readValidStore(Path f) {
		try {
			String text = Files.readString(f);
			if (text.isBlank()) return null;
			List<Case> parsed = G.fromJson(text, new TypeToken<List<Case>>() {}.getType());
			if (parsed == null) return null; // zero-byte-equivalent content or the literal "null"
			for (Case c : parsed) {
				if (c == null) return null;
				if (c.id == null || c.id.isBlank() || c.player == null || c.player.isBlank()
						|| c.action == null || c.action.isBlank() || c.uuid == null || c.uuid.isBlank()) return null;
				try { if (!UUID.fromString(c.uuid).toString().equals(c.uuid)) return null; } // canonical form only
				catch (IllegalArgumentException badUuid) { return null; }
				if (!c.action.equals("kick") && !c.action.equals("tempban") && !c.action.equals("ban")) return null;
				if (c.action.equals("tempban") && c.until <= 0) return null; // a tempban that never expires is a mislabeled permanent ban
				if (c.evidence == null) c.evidence = new ArrayList<>(); // legacy-compatible defaults
				if (c.by == null) c.by = "unknown";
				if (c.reason == null) c.reason = "";
			}
			return parsed;
		} catch (Exception e) { return null; }
	}
	/** Copies an unreadable store file aside for manual recovery, deduped by content hash; returns the copy's file name or null. */
	private static String preserve(Path f) {
		try {
			String hash = sha256(f);
			String want = f.getFileName().toString() + ".corrupt-";
			try (var stream = Files.list(f.toAbsolutePath().getParent())) {
				for (Path p : (Iterable<Path>) stream::iterator)
					if (p.getFileName().toString().startsWith(want) && hash.equals(sha256(p))) return p.getFileName().toString();
			}
			Path copy = f.resolveSibling(want + System.currentTimeMillis() + "-" + hash.substring(0, 8));
			Files.copy(f, copy);
			return copy.getFileName().toString();
		} catch (Exception e) { LOG.warn("[meshac] could not preserve unreadable case store: {}", e.toString()); return null; }
	}
	private static String sha256(Path f) throws Exception {
		byte[] h = java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f));
		StringBuilder sb = new StringBuilder(); for (byte x : h) sb.append(String.format("%02x", x)); return sb.toString();
	}

	/** Replaceable move operation so tests can force rename failures deterministically. */
	interface MoveOp { void move(Path source, Path target, java.nio.file.CopyOption... options) throws java.io.IOException; }
	static MoveOp ATOMIC_MOVER = Files::move;

	/** How a store commit ended. PERSISTED: the new content is on disk (the rename returned, or it threw
	 *  but the target bytes match the new content). FAILED_CLEAN: the target is provably untouched (temp
	 *  write/fsync failed, the rename was not performed, or it threw but the target bytes match the old
	 *  content). FAILED_UNCERTAIN: the rename threw and the target matches NEITHER content - the on-disk
	 *  outcome is undetermined, memory keeps the mutation, and callers must report uncertainty instead of
	 *  "nothing changed". There is deliberately NO non-atomic fallback: a plain replace can fail
	 *  mid-commit and leave the target partially replaced; atomic-only plus byte reconciliation keeps
	 *  every outcome truthfully reportable. On a filesystem without atomic-move support every save fails
	 *  loudly (enforcement continues in memory, staff are told) - a store that cannot be written
	 *  atomically is a store the admin must move to a filesystem that can. */
	enum Commit { PERSISTED, FAILED_CLEAN, FAILED_UNCERTAIN }

	/** Writes content to target through a sibling temp file: write, fsync, atomic rename over the target.
	 *  A crash leaves at worst a torn .tmp beside an intact target. A rename that THROWS is reconciled
	 *  against the actual target bytes (an exception does not prove the rename did not happen): matching
	 *  the new content is PERSISTED, matching the old is FAILED_CLEAN, anything else FAILED_UNCERTAIN. */
	private static Commit writeCommit(Path target, String content, String what) {
		Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
		boolean existedBefore = Files.exists(target);
		byte[] oldBytes = null;
		try { oldBytes = existedBefore ? Files.readAllBytes(target) : null; } catch (Exception ignored) {}
		byte[] newBytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		try {
			Files.writeString(tmp, content);
			try (java.nio.channels.FileChannel ch = java.nio.channels.FileChannel.open(tmp, java.nio.file.StandardOpenOption.WRITE)) { ch.force(true); }
		} catch (Exception e) { LOG.warn("[meshac] could not write {} temp file: {}", what, e.toString()); try { Files.deleteIfExists(tmp); } catch (Exception ignored) {} return Commit.FAILED_CLEAN; }
		try { ATOMIC_MOVER.move(tmp, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING); return Commit.PERSISTED; }
		catch (java.nio.file.AtomicMoveNotSupportedException e) { LOG.warn("[meshac] filesystem does not support atomic move for {}; saving is disabled (no non-atomic fallback - a torn store is worse than a loud failure)", target); try { Files.deleteIfExists(tmp); } catch (Exception ignored) {} return Commit.FAILED_CLEAN; }
		catch (Exception e) {
			byte[] cur = null; boolean readOk = true;
			try { cur = Files.exists(target) ? Files.readAllBytes(target) : null; } catch (Exception unreadable) { readOk = false; }
			if (readOk && java.util.Arrays.equals(cur, newBytes)) { LOG.warn("[meshac] {} rename signalled failure but the new content IS on disk; treating the commit as persisted: {}", what, e.toString()); return Commit.PERSISTED; }
			if (readOk && (cur == null ? !existedBefore : java.util.Arrays.equals(cur, oldBytes))) { LOG.warn("[meshac] {} rename was not performed (the target still holds exactly the old content): {}", what, e.toString()); try { Files.deleteIfExists(tmp); } catch (Exception ignored) {} return Commit.FAILED_CLEAN; }
			LOG.warn("[meshac] {} rename threw and the target matches neither the old nor the new content (or cannot be read); on-disk state is UNCERTAIN: {}", what, e.toString());
			return Commit.FAILED_UNCERTAIN;
		}
	}

	/** Persists the store. Order: preserve any unpreserved corrupt evidence (blocking otherwise); rotate
	 *  the backup ONLY from a currently valid store file (a good backup is never overwritten by a corrupt
	 *  main - after a backup recovery the old backup survives until the store is healthy again); commit
	 *  the new store via writeCommit; fsync the directory best-effort (some filesystems, e.g. Windows
	 *  directories, refuse it - logged only, so PERSISTED means file fsync + rename returned, with rename
	 *  durability uncertain on such systems). A backup-rotation failure blocks the save with the main
	 *  file untouched. On PERSISTED every in-memory case (including earlier failed adds) is now on disk,
	 *  so all persisted flags are set. */
	private static Commit save() {
		if (lastCommitUncertain || lastBackupUncertain) { LOG.warn("[meshac] refusing to save: the store is unreconciled after an undetermined rename; run /mesh reload first"); return lastCommitUncertain ? Commit.FAILED_UNCERTAIN : Commit.FAILED_CLEAN; }
		Path f = file();
		if (Files.exists(f)) {
			if (readValidStore(f) != null) {
				try {
					Commit b = writeCommit(backupFile(), Files.readString(f), "case-store backup");
					if (b == Commit.FAILED_UNCERTAIN) {
						// The MAIN store was never committed; only the backup's state is undetermined. That is
						// NOT main uncertainty: callers roll back removals cleanly, and the latch blocks further
						// writes until /mesh reload reconciles and the next save re-rotates the backup atomically.
						lastBackupUncertain = true;
						LOG.warn("[meshac] the recovery-backup rename had an UNDETERMINED outcome; the main store is untouched; refusing further saves until /mesh reload reconciles");
						return Commit.FAILED_CLEAN;
					}
					if (b != Commit.PERSISTED) { LOG.warn("[meshac] refusing to save: could not refresh the recovery backup"); return Commit.FAILED_CLEAN; }
				} catch (Exception e) { LOG.warn("[meshac] refusing to save: could not refresh the recovery backup: {}", e.toString()); return Commit.FAILED_CLEAN; }
			} else {
				// The main file went bad since the last load (external edit, disk rot, interrupted write):
				// it is EVIDENCE. Preserve it before anything overwrites it, and never rotate it into the backup.
				lastPreservedCorrupt = preserve(f);
				if (lastPreservedCorrupt == null) { LOG.warn("[meshac] refusing to save: the corrupt store has not been preserved yet"); return Commit.FAILED_CLEAN; }
			}
		}
		Commit c = writeCommit(f, G.toJson(all), "case store");
		if (c == Commit.PERSISTED) {
			try (java.nio.channels.FileChannel dir = java.nio.channels.FileChannel.open(f.toAbsolutePath().getParent(), java.nio.file.StandardOpenOption.READ)) { dir.force(true); }
			catch (Exception e) { LOG.warn("[meshac] could not fsync case-store directory (durability of the rename is uncertain on this system): {}", e.toString()); }
			for (Case x : all) x.persisted = true; // a successful save persists every earlier in-memory-only case too
			lastCommitUncertain = false;
		} else if (c == Commit.FAILED_UNCERTAIN) {
			lastCommitUncertain = true;
			// The unknown main bytes are FORENSICS: preserve them and block; NEVER auto-restore over them.
			// Reconciliation is deliberate: /mesh reload (which recovers from the backup if one is usable).
			lastPreservedCorrupt = preserve(f);
			if (lastPreservedCorrupt != null) LOG.warn("[meshac] main store bytes of undetermined state preserved as {}; writes blocked until /mesh reload reconciles (in-memory changes not yet on disk may be undone by reconciliation)", lastPreservedCorrupt);
			else LOG.warn("[meshac] could NOT preserve the undetermined main store bytes; writes blocked; reconcile manually from {}", backupFile().getFileName());
		}
		return c;
	}
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
		all.add(c);
		// Failure direction for a NEW case is keep-and-enforce: an unsaved ban must still protect the
		// server (rejoin check and escalation ladder see it), it persists on the next successful save,
		// and it is lost on restart otherwise - the caller must say so. This differs from pardon/unban,
		// where a failed removal rolls back because a lift that did not persist must not count as done.
		Commit r = save();
		c.persisted = r == Commit.PERSISTED;
		if (r == Commit.FAILED_CLEAN) LOG.warn("[meshac] case {} could not be saved; it stays enforced in memory only, persists on the next successful save, and is lost on restart otherwise", c.id);
		if (r == Commit.FAILED_UNCERTAIN) LOG.warn("[meshac] case {} commit outcome is UNDETERMINED (a rename threw mid-commit); the case stays enforced in memory - reconcile with /mesh reload", c.id);
		return c;
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
	public record UnbanResult(int lifted, boolean persisted, boolean ambiguousName) {}
	/** Lifts every active ban on this player but keeps the offences on record, so the next one still escalates.
	 *  Name/UUID recovery: a uuid string matches directly (rename-proof, works for offline players). A name
	 *  resolves through ALL stored case records to the uuids ever associated with it, whatever the case
	 *  status: exactly one associated uuid resolves (even when that player's active ban was recorded under
	 *  a newer name - that is rename recovery); several associated uuids (a recycled name) refuse with
	 *  ZERO mutation and staff must use an explicit uuid from the list. Resolution over every association
	 *  never silently changes target as cases age. */
	public static synchronized UnbanResult unban(String identifier) {
		if (dev.meshac.safety.Containment.mode() != dev.meshac.safety.Containment.Mode.ENFORCE) return new UnbanResult(0, true, false);
		if (lastCommitUncertain || lastBackupUncertain) return new UnbanResult(0, false, false); // unreconciled store/backup: refuse new removals until /mesh reload
		long now = System.currentTimeMillis();
		Set<String> targets = new HashSet<>();
		try { targets.add(UUID.fromString(identifier).toString()); }
		catch (IllegalArgumentException notUuid) {
			for (Case c : all) if (c.player.equalsIgnoreCase(identifier)) targets.add(c.uuid);
			if (targets.size() > 1) return new UnbanResult(0, true, true); // ambiguous recycled name: refuse, mutate nothing
		}
		List<Case> lifting = new ArrayList<>();
		for (Case c : all)
			if (targets.contains(c.uuid) && !c.pardoned && !c.lifted && (c.action.equals("ban") || (c.action.equals("tempban") && c.until > now))) lifting.add(c);
		if (lifting.isEmpty()) return new UnbanResult(0, true, false);
		for (Case c : lifting) c.lifted = true;
		Commit r = save();
		if (r == Commit.PERSISTED) return new UnbanResult(lifting.size(), true, false);
		if (r == Commit.FAILED_UNCERTAIN) return new UnbanResult(lifting.size(), false, false); // undetermined commit: keep the lift, report uncertainty, reconcile via /mesh reload
		for (Case c : lifting) c.lifted = false; // clean failure: rollback - a lift that did not persist did not happen; staff retry
		return new UnbanResult(0, false, false);
	}
	/** Distinct uuids ever associated with this stored player name; lists exactly which identities an
	 *  ambiguous /mesh unban <name> refused to touch. */
	public static synchronized Set<String> uuidsForName(String name) {
		Set<String> out = new HashSet<>();
		for (Case c : all) if (c.player.equalsIgnoreCase(name)) out.add(c.uuid);
		return out;
	}
	/** All cases carrying this id, in record order. Legacy stores can contain duplicate ids (the old 4-char minter had no uniqueness check); callers must handle more than one. */
	public static synchronized List<Case> findAll(String id) { List<Case> o = new ArrayList<>(); for (Case c : all) if (c.id.equalsIgnoreCase(id)) o.add(c); return o; }
	/** Compatibility lookup for legacy callers (sim/safety tests): returns the record only when exactly ONE
	 *  stored case carries this id; duplicated historical ids yield null so no caller can silently choose one.
	 *  New code should call findAll and handle ambiguity explicitly. */
	public static synchronized Case find(String id) { List<Case> m = findAll(id); return m.size() == 1 ? m.get(0) : null; }
	public enum PardonResult { PARDONED, PARDONED_NOT_SAVED, PARDON_UNCERTAIN, NOT_FOUND, AMBIGUOUS }
	/** Pardons the case with this id. Refuses - without mutating anything - unless exactly ONE stored record
	 *  carries it: a duplicated historical id stays ambiguous even when pardons/expiry leave a single live case,
	 *  so an operator's original case reference can never change meaning over time. */
	public static synchronized PardonResult pardon(String id) {
		if (dev.meshac.safety.Containment.mode() != dev.meshac.safety.Containment.Mode.ENFORCE) return PardonResult.NOT_FOUND;
		if (lastCommitUncertain || lastBackupUncertain) return PardonResult.PARDON_UNCERTAIN; // unreconciled store/backup: refuse new removals until /mesh reload
		List<Case> matches = findAll(id);
		if (matches.isEmpty()) return PardonResult.NOT_FOUND;
		if (matches.size() > 1) return PardonResult.AMBIGUOUS;
		Case target = matches.get(0);
		if (target.pardoned) return PardonResult.NOT_FOUND; // already pardoned
		target.pardoned = true;
		Commit r = save();
		if (r == Commit.PERSISTED) return PardonResult.PARDONED;
		if (r == Commit.FAILED_UNCERTAIN) return PardonResult.PARDON_UNCERTAIN; // undetermined commit: keep the pardon, report uncertainty, reconcile via /mesh reload
		target.pardoned = false; // clean failure: rollback - a pardon that did not persist did not happen; staff retry
		return PardonResult.PARDONED_NOT_SAVED;
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

