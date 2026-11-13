# Product layer inventory

Source: original repository main branch, inspected 2026-11-13. This is an
inventory for the rebuild, not a statement that the rebuild implements it.

## Implemented in the original

- MeshCommands: /mesh cases [player], case <id>, pardon <id>, unban <player>,
  ban <player> <duration> <reason>, kick <player> <reason>, status <player>,
  watch, monitor [on|off], reload.
- Screens: meshac-branded kick, permanent-ban and temporary-ban disconnect
  components. Configured accent/server/appeal copy. Case ID and time-left line.
- Cases: local case IDs, evidence records, pardon/lift state and ban history.
- Config: original settings and veil integration. Exact compatibility inventory
  still needed before replacing format or dropping a field.
- Historical heat ladder: product-policy logic to preserve separately from
  Vulcan detection results, not permission to add new detection checks.

## Planned in original docs, not implemented evidence

IDENTITY.md explicitly labels the dialog watchlist, player card, trace viewer,
tuning screen, clickable chat fallback and extra watch/card/trace/tune/ward/
quiet/selftest commands as planned. Do not claim these were preserved merely
by copying documentation. Luke explicitly requested GUIs for the rebuild, so
requested product work remains open even though these were previously planned.

## Rebuild status

Not yet restored: commands, cases/IDs, ban/kick screen product layer, GUIs,
config compatibility and operational staff workflow. Branding and upstream
credits are present in fabric.mod.json; a full user-visible string audit is
still needed. No action/punishment UI may bypass log-only containment or alter
Vulcan detection outcomes. Do not deploy the current jar.
