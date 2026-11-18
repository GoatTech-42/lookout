> SUPERSEDED-BY-r5 NOTICE: where this file's earlier prose (items 1-7, the r2 gate text, 'r3 additions', 'r4 additions') conflicts with x4df-oracle-r2.json rev 5 or x4df_verdict.py r5, the JSON and script win. r2/r3 field lists are superseded by the r4 field list in the script header: the bare `off_unchanged` field is gone, replaced by the OFF window fields; every row carries the normalizer evidence fields.

# Box: X4 dig-first (dig toward an ore) rerun. ONE host job. Bot only, 26.1.2 capped rig, no laptop, no real accounts, foreground, nothing pushed.
Status of the prior attempt: dig-first is UNVERIFIED (not qualified). Setup used the post-hoc corrected command set (meshac-x4-dig-oracle-corrected-38d3b030.json, two legacy gamerule ids dropped, no geometry change). The first pass stays NOT_QUALIFIED_setup_failed. This request reuses the corrected setup unchanged; it does not edit the oracle after seeing results.

Build: the same jar as X4 dig-past v3 (record its sha in the receipt), `sh APPLY-ORDER-v13.sh rig held x7 x8 dd3`, veilXray=true for ON, false for the OFF control. Seed 202611170853. Disk soft 70 / hard 85.

## Oracle (written before any run)
Geometry from the corrected setup: bot starts at (8.5,-20,10.5) and (9.5,-20,25.5); the cover block between bot and ore is stone at (9,-20,10) and (9,-20,26) (the fill leaves 9,-20..-19 at z 23-25 open; the bot digs ONLY the cover block directly adjacent to the ore). Ores: diamond_ore at (10,-20,10) and (10,-20,26).
1. Initial, ON: both ores plain deepslate (source rule: Overworld hidden blocks below y=0 mask to deepslate) in the received chunk. Initial, OFF: both diamond_ore. Anything else = setup invalid, stop, NOT_QUALIFIED.
2. Action: the bot breaks the single cover block next to each ore (dig TOWARD the ore, not past it). Record the server break tick/epoch (RigLog or server log) and the bot receive epoch for the ore ClientboundBlockUpdate (or section update containing the ore).
3. Latency gate (frozen in x4df-oracle-r2.json and x4df_verdict.py; those files, not this prose, are the executed oracle): anchor = server cover-mutation HEAD and RETURN epochs from the rig trace (same as dig-past v3). upper = recv - HEAD + 1; lower = recv - RETURN - 1. upper <= 50 PASS; lower > 50 FAIL; straddles 50 INCONCLUSIVE; valid anchor with no ore update within 5000 ms FAIL. Fallback only if no anchor: bot send-to-receive <= 49 ms = PASS-conservative (named 1 ms uncertainty; assumes send precedes server mutation), else INCONCLUSIVE. Missing anchor or capture = INCONCLUSIVE / NOT_QUALIFIED.
4. FAIL as in 3. A result outside these rules is not a verdict.
5. Control (OFF): the ore shows as diamond_ore from the start; the same cover break must produce no masking change. Record the same timings for comparison.
6. Repeat each of the 2 ore cells 3 times (reset by the corrected setup commands). A single cell pass is not a qualified pass.
7. A capture with no oracle verdict file is NOT_QUALIFIED. Do not claim piston, TNT, X5, E1, E2 or E4 from this run.

## Deliver
Raw logs (server log, bot log, rig.jsonl), per-cell verdict JSON written BEFORE the summary, receipt with jar sha, build line, seed, markers, disk %. Label capped-rig numbers as such.

## r3 additions (QA fail-closed review)
- Executed oracle = x4df-oracle-r2.json (rev 3) + x4df_verdict.py r3. Box must emit one verdict-input JSON {"on":[6 rows],"off":[6 rows]} with the exact row fields listed in the script header (cell, rep 1..3, initial, cover_break_ok, final_block, anchor_valid, head/return/recv/send epochs, capture_end_epoch_ms; OFF: initial, cover_break_ok, off_unchanged), then run `python3 x4df_verdict.py rows.json` and attach the output UNEDITED. Write rows from the raw logs before reading any summary.
- recv=None counts as FAIL only if capture_end - RETURN >= 5000 ms was actually observed; an early-stopped capture is INCONCLUSIVE.
- Custody: BEFORE the run, Box reports the exact setup artifact it executed in the prior attempt (path and full sha256). The label 38d3b030 is a name only; the bytes attached by Lead hash to eb389f87. If Box's bytes differ, stop and report; do not run.

## r4 additions (QA r3 review)
- Row fields now include normalizer evidence on EVERY row: ts_unit "epoch_ms", one clock_id for all rows (the server wall-clock epoch everything was normalized to), anchor_src (which rig trace line the HEAD/RETURN epochs came from), cover_trace_id (trace line of the cover-cell mutation), cover (must equal (9,-20,10) for ore (10,-20,10), (9,-20,26) for ore (10,-20,26)). Box states the unit and clock conversion used for the bot receive times in the receipt.
- OFF control: after the verified cover mutation (record off_mutation_return_epoch_ms) observe at least 2500 ms (off_capture_end_epoch_ms) and count ore block updates (off_ore_updates). Zero updates over the full window = OFF_OK. Bare booleans no longer pass.
- HEAD<=RETURN is allowed. send is optional when an anchor exists. The script exits 0/1/2/3 (pass/fail/inconclusive/not-qualified-or-unreadable). Box attaches the script output unedited.

## r5 addition
- Global precedence after both legs: any NOT_QUALIFIED wins, then FAIL, then INCONCLUSIVE, else the ON verdict. Pre-run setup custody (path+sha256 of the prior-run setup artifact, stop on mismatch vs eb389f87) and raw-log to row provenance (each row traceable to a raw log line) are conditions on QA's signature; Box ships the row-to-log mapping with the receipt.
