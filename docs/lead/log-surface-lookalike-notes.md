# log-surface lookalike + hardening notes (r4)

companion to the pins in tests/test_rig_proof.py (lookalike-*,
envelope-*, mutant-hardening-*). fixtures (synthetic, ID-only):

- rig/proof/fixtures/client-lookalikes.log
  sha256 3bc5c8555b9311e3c3c431744f073b9a64b082c94f1b6e79c491f234e3120f42
- rig/proof/fixtures/client-lookalikes-watching-echo.log
  sha256 fc0c081af275fcb4d5c67c6f4a529bb47c6a678492033f6aa474f7095179410a
- rig/proof/fixtures/client-hardening-envelopes.log
  sha256 535b1e93fbccb15a061a51997e9381018462d8a80d6da823824e6c3e4356304a

## history

r2 pinned the pre-hardening characterization (chat echoes counted like
real emissions). r3 changes that characterization explicitly, in the
same diff as the parser hardening: the r2 pins were replaced, not
silently edited away.

r4 (this document) is the QA rework of r3. what changed, each with its
own red-first pin:

- scorer precedence (boot): definitive FAIL evidence (real mixin error
  lines) is scored BEFORE ambiguity, so a bare watching marker can no
  longer mask a real mixin failure as UNVERIFIED. ambiguity still
  blocks absence-of-evidence FAILs and the PASS. the other rows were
  checked for the same ordering: boot is the only row with definitive
  error evidence; every other row's FAIL is absence-of-evidence, which
  ambiguity correctly blocks. pins: boot-precedence-fail-beats-ambiguity
  plus the mutant-precedence-* pair (ambiguity moved before the mixin
  check turns the same log UNVERIFIED)
- null-gui: an ambiguous alert line inside a valid operator window is
  unverifiable evidence for the row (UNVERIFIED naming the lines),
  pinned by null-gui-ambiguous-alert-in-window-unverified. the
  previously missing-mod-list/watching absence FAILs keep their order
- "counted nowhere" scoped: ambiguity applies to the four marker kinds
  (watching/alert/stats/null-gui) only. the same-line mixin error scan
  is not a marker scan: it still counts headerless and nested error
  lines (multi-line stack entries carry no headers), and excludes only
  chat-routed lines. wording fixed in the runbook too
- malformed inner header: nested detection covers only FULLY recognized
  inner headers; a payload starting with a malformed header still
  counts (line 10 of the envelope fixture, pinned by
  envelope-malformed-inner-counts-today). frozen known gap, not a
  qualification
- in the qualified corpus, chat-routed lines carry their own INFO
  header, so a chat-routed line closes any open ERROR/WARN entry and
  opens an INFO entry: it cannot join an error entry to manufacture a
  window finding, pinned by envelope-chat-cannot-join-error-entry
- WARN/ERROR-level chat-routed forms (unobserved in real captures,
  pinned on synthetic shapes): chat-routed lines never contribute
  package/vocabulary evidence to window findings, so a chat line
  carrying only the package plus an orphan continuation carrying only
  the vocabulary cannot manufacture an UNVERIFIED (win-f22/f23).
  chat content can also not SUPPRESS window evidence: the window dedup
  check mirrors the same-line scan's eligibility and suppresses only
  on counted same-line errors, so a chat-routed header carrying
  package+vocabulary leaves independent continuation evidence intact
  (win-f25 positive, win-f26 negative, mutant-dedup-unfiltered-killed)
- a single chat line carrying package+vocabulary on one line is absent
  from findings via BOTH filters (the evidence filter empties pkg/voc
  on chat-only lines; dedup is the backstop for counted matches);
  win-f24 freezes that outcome with the mechanism named as both
- open: the entry cap still applies to chat-opened ERROR/WARN entries -
  30+ continuations after a chat-routed error-level header produce a
  truncated entry and UNVERIFIED. this slice closes chat-fed window
  evidence, not the truncation path

## hardened behavior, pinned on these bounded fixtures

envelope rules (rig/proof/parsers.py docstring):

- chat-routed lines - payload starts "[System] [CHAT] " (the real
  client form, shape from committed captures) or "[CHAT] " (server-style
  form) - are never scanned for markers or same-line mixin errors
- a marker-shaped line with no recognizable header (bare), a malformed
  header, or a payload that itself starts with a header (nested) is
  AMBIGUOUS: recorded in ambiguous_markers, counted nowhere, and the
  affected scorer row goes UNVERIFIED (boot / target-delivery /
  chat-capture / null-gui, per marker kind)
- real envelopes count: launcher [hh:mm:ss] [thread/LEVEL]: and log4j
  direct ISO-header lines, watching via log4j direct included
  (envelope-watching-log4j-counted)

scorer-level pins: echoed stats x5 FAIL target-delivery, an echoed
alert FAILs chat-capture, echoed null-gui markers leave the row
UNVERIFIED (no windows), an echoed mixin error no longer FAILs boot
(echo filtered), ambiguity turns the matching row UNVERIFIED with the
offending line numbers, and the header-INFO window-negative stays
(a chat header carrying ERROR text opens no error entry).

## mutation protection

- mutant-hardening-chatfilter-removed-killed: echoes count again
- mutant-hardening-ambiguity-dropped-killed: ambiguous recording gone
- mutant-hardening-nested-disabled-killed: nested-prefixed alert counts
- mutant-precedence-ambiguity-first-killed: ambiguity scored before the
  mixin-error check turns a real-failure log UNVERIFIED
- mutant-chat-window-feed-killed: letting chat lines feed window
  evidence turns f22's chat echo into a window finding
- mutant-dedup-unfiltered-killed: dedup over unfiltered lines lets a
  chat header suppress f25's independent window evidence to PASS

## what this is not

- filtering does NOT authenticate the log: any non-chat writer can
  still echo markers, and a compromised or forged log passes every
  filter here. the rows remain log-surface checks.
- nested-header detection covers fully recognized inner headers only;
  a malformed inner header still counts (frozen known gap above)
- no claim about log writers, marker orderings, or chat routing forms
  beyond these fixtures; new envelope shapes should extend the corpus.
