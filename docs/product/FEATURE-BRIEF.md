# meshac feature brief and release gates

Luke's November13 commit39da966 and subsequent direct confirmation establish these goals:

- Anti-xray: hide ores not exposed to the world surface or cave walls.
- Anti-ESP: hide players through solid occluders, and investigate below-height information suppression against freecam base discovery.
- Movement checks: build on the pinned Vulcan movement layer, with impossible movement, fly and speed as intended coverage, not current readiness claims.
- Anti-duplication: verify server-authoritative inventory additions and common duplication exploits. Existing upstream coverage must be audited rather than assumed.
- Extend the features after establishing correct behavior. Explicit regression cases include non-solid blocks and making hidden players reappear correctly.
- Meteor or equivalent adversarial client testing belongs only on the disposable isolated rig, alongside legitimate-client false-positive tests.

The screenshot was read, its requirements captured here, and its root working-tree copy removed with Luke's confirmation. Commit history is retained. This brief records requested work, not completed feature support.

## Production and publication constraints

Luke intends future production use; no public server exists yet. Reliability and false-positive testing take priority. He explicitly said there is no pressure or rush. Keep detector gates closed, Veil off, and LOG_ONLY until the relevant qualifications pass. Nothing in this brief authorizes live deployment, public-server testing, or gate opening.

No new release until there is an actual jar for every supported target (currently explicit adapters1.21.11,26.1.2,26.2,26.3) and every jar is obfuscated with ZKM. Another obfuscator is not a substitute. ZKM availability/license/access, release cleanup and distribution rights remain separate checks. No spending is approved. Preserve unique historical artifacts and recovery before any destructive release cleanup.
