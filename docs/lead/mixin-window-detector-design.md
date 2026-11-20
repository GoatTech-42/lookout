# window detector design: multi-line mixin error forms

bound to main afcd97a. design only; no code lands from this note. it
PROPOSES REDUCING the blind spot documented in
mixin-error-multiline-limits.md: the boot row's `is_mixin_error`
matches severity, dev.lookout and mixin vocabulary on ONE line, so
split forms (summary line + "Caused by:" on the next, stack frames
below the header) score PASS while a real failure sits in the log.
whether it closes the blind spot is a rig question, decided by real
captured logs (rollout below), not by this design.

## current behavior (main afcd97a)

- `parsers.is_mixin_error(line)`: ERROR/WARN + `dev\.lookout` +
  `mixin|apply|inject|callback` (case-insensitive), all on the same
  line, order-insensitive.
- `score.row_boot`: any match = FAIL; no match contributes to PASS.
  PASS today means exactly "no same-line match".
- per-callback failures logged at INFO/DEBUG are deliberately excluded
  by the severity gate; that exclusion is kept.

## entry model and header grammar

match per log ENTRY, not per raw line and not per naive N-line window.

header formats pinned from captured client logs
(docs/build-evidence/abilities26.3/abilities-constructed-client.log):

1. launcher format: `[hh:mm:ss] [thread/LEVEL]: message`
   (e.g. `[15:03:32] [Render thread/ERROR]: ...`). thread names vary
   freely (`main`, `Render thread`, `Download-2`, `Datafixer
   Bootstrap #0`, ...), LEVEL is a bare word.
2. log4j direct format: ISO-8601 UTC timestamp, then thread, then
   LEVEL (e.g. `2026-11-13T23:03:13.292799379Z main ERROR ...`),
   seen for early log4j init lines before the launcher pattern
   attaches. fractional-second precision varies.

a line matching either grammar is a header. anything else is a
continuation line.

entry = one header plus the continuation lines that follow it, up to
the next header, EOF, or a cap of 30 CONTINUATION LINES (31 lines
total including the header), whichever comes first.

orphan continuations (no preceding header: file start, or lines
following a truncated entry once the cap has fired) belong to no
entry. they never open or extend a window, but the same-line scan
(see below) still sees them.

## proposed semantics

- a window finding = severity ERROR/WARN on the entry's header AND
  `dev\.lookout` anywhere in the entry AND vocabulary anywhere in the
  entry. each clause is still matched independently and
  order-insensitively; they just no longer have to share a line.
- one finding per entry, reporting the entry's line range and the
  exact lines that satisfied each clause (for review).
- TRUNCATION: if an ERROR/WARN entry hits the 30-continuation-line
  cap without a finding (the entry continues past the cap), the
  no-match is not clean. the entry is reported TRUNCATED and scores
  UNVERIFIED for human review - a package hit on continuation line 31
  is invisible to the window, and the design says so instead of
  implying operational green. an entry that genuinely ends (next
  header or EOF) within the cap with no match is a clean no-finding.
- SAME-LINE SCAN IS KEPT over ALL lines, including orphans and lines
  beyond any cap, exactly as `is_mixin_error` works today. a same-line
  match anywhere is FAIL, unchanged. the window pass ADDS findings;
  it can never remove or downgrade a same-line FAIL.
- verdict wiring: same-line matches keep today's FAIL semantics.
  window-ONLY findings (no single line carries all three clauses, no
  truncation) score the boot UNVERIFIED and surface the block for
  human review; a confirmed failure then overrides to FAIL. truncated
  ERROR/WARN entries are UNVERIFIED as above. this mirrors the amended
  triage rule (failure-shaped block = investigation + UNVERIFIED until
  a human confirms) because the wider net has a wider false-positive
  surface, and it keeps UNVERIFIED honest: never pass-by-absence,
  never fail-by-absence either.
- residual risk the design does NOT reduce: if the launcher
  interleaves threads such that continuation lines of two entries mix,
  attribution degrades toward a raw 30-line window. the human review
  gate is the backstop; fixtures pin the prefixed formats we actually
  see.

## fixtures needed (red/green pairs, synthetic, id-only)

red (each must flip its own test red before the detector exists):

1. summary + caused-by: `ERROR Mixin apply failed` header, `Caused by:
   ... dev.lookout.client.<Class>` on the next line. the shape QA
   replayed against the healthy fixture.
2. deep frames: ERROR header with no package/vocab; dev.lookout stack
   frames with `inject` several lines below, inside the same entry.
3. caused-by chain: only the innermost `Caused by:` names the failing
   callback and package.
4. boundary: first satisfying line on continuation line 30 (header +
   30 continuations = 31 total) - counts.

green (guards; each pins an exclusion or regression):

5. truncation boundary: satisfying line on continuation line 31 of a
   still-continuing ERROR entry - NO window finding, and the entry is
   reported TRUNCATED (UNVERIFIED for review). pins both that the cap
   holds and that truncation never reads as operational green.
6. clean boundary: same layout as 5 but the entry ends at the cap
   (next header arrives) - clean no-finding, no truncation flag.
7. next-header reset: package/vocab appearing after the NEXT header
   belongs to that entry, not the open one; a clean ERROR entry
   followed by an INFO entry carrying package + vocabulary yields no
   window finding on the ERROR entry (and no finding at all: INFO is
   severity-excluded).
8. EOF flush: an entry running to EOF with no following header is
   still evaluated (not silently dropped).
9. orphan continuations: lines before the first header carrying
   package + vocabulary open no window; a same-line ERROR orphan
   still FAILs via the same-line scan.
10. cap + same-line regression: a same-line match on continuation
    line 40 (beyond the cap, entry truncated) still FAILs via the
    same-line scan - window logic can never hide it.
11. unrelated error: an ERROR entry with no dev.lookout content,
    followed within 30 lines by a routine INFO dev.lookout line.
    entry scoping must keep them apart; a naive sliding window merges
    them.
12. no vocabulary: ERROR entry whose stack names dev.lookout frames
    (exception through injected code) but no mixin/apply/inject/
    callback word anywhere in the entry.
13. severity exclusion: INFO entry carrying package + vocabulary on
    one line - still no finding.
14. dedup: a same-line match in an entry that would also window-match
    - reported once.
15. collision shape (documents the accepted risk): ERROR entry for a
    non-mixin cause containing an everyday word from the vocabulary
    ("Failed to apply config value") plus an unrelated dev.lookout
    frame. window-only match, so UNVERIFIED + human clears; the
    fixture pins that this surfaces for review instead of
    auto-failing or passing silently.
16. header grammar: log4j direct format (ISO timestamp) header opens
    an entry exactly like the launcher format; a malformed near-header
    (e.g. bracketed time with no thread/level) is a continuation.
17. healthy regression: the current good client log stays green under
    the window detector.

## planned mutant mapping

planned, not demonstrated - each mutant is expected to be killed
exactly by the fixture listed, to be proven red-first when the
detector lands:

- no 30-line cap -> fixture 5.
- cap counts header (29 continuations) -> fixtures 4 and 5.
- package and vocabulary required on the same line -> fixtures 1/2/3.
- severity gate dropped -> fixture 13.
- entries not deduped against same-line matches -> fixture 14.
- raw sliding window instead of entry scoping -> fixture 11.
- next header does not reset the entry -> fixture 7.
- EOF entry dropped -> fixture 8.
- truncated entries treated as clean no-finding -> fixture 5.
- same-line scan bounded by the cap -> fixture 10.
- window-only findings auto-FAIL (no human gate) -> fixture 15.
- window-only findings dropped silently -> fixtures 1/2/3 asserting
  UNVERIFIED, not silence.
- launcher-format-only header grammar -> fixture 16.

## rollout

1. rig, when unheld: capture one real boot log per target and run the
   multiline note's block review. any real multi-line block is
   redacted to ids and becomes a fixture BEFORE the window form is
   trusted for scoring.
2. the detector, the fixtures above and their tests land as one
   slice: red-first evidence, healthy baseline before mutant kills,
   line-prefix assertions, no names in any fixture. the planned mutant
   mapping above becomes demonstrated kills in that slice or the
   slice does not ship.
3. with it landed, runbook + multiline note + row_boot's PASS detail
   are updated: PASS then means "no same-line or in-entry match, no
   truncated error entries". until then the current wording stands.

## non-goals

- INFO/DEBUG severity stays excluded (deliberate; a target that logs
  failures at INFO remains invisible to scoring and is a runbook note,
  not a detector change).
- package-less and config-name-only errors stay missable; the triage
  grep and block review remain the backstop, unchanged.
- not a full log audit: this widens one detector's net for known mixin
  failure shapes and nothing else.
