# Repository consolidation and recovery

The confirmed plan makes the OpenVulcan Fabric rebuild the main line, keeps old main on a recovery branch until v1 is finished and published, and archives unique old branch work before cleanup. No public release is part of this change.

## Recovery

- `legacy/pre-openvulcan`: old main `db12796db170c7f5cc705146b264dc024bfb4ebf`.
- Each pre-cleanup remote branch tip is preserved by the exact archive tag below.
- A complete local Git bundle was verified before consolidation. Its SHA256 is `62d766267234b5c1c269fe63c012edd7152d84ad5c71d2aad78b25623a2b7a79`. Remote archive tags are the durable recovery route.

| original branch | exact tip | archive tag |
| --- | --- | --- |
| `ban-durations` | `1dd904459a3f7927b6e6e1be35eb38bb40608cf5` | `archive/pre-openvulcan-20261113/ban-durations` |
| `bedrock-exempt` | `4a73975498b9c62dcfa6d30b798e91156a570800` | `archive/pre-openvulcan-20261113/bedrock-exempt` |
| `bedrock-split` | `27db21c04f08c7d61f915bde02adf0c7d42f9b57` | `archive/pre-openvulcan-20261113/bedrock-split` |
| `ci/meshac-1211-controls-d944357c-once` | `04c5753ed39ae883b34a4b8eb74e3c57e6047dab` | `archive/pre-openvulcan-20261113/ci/meshac-1211-controls-d944357c-once` |
| `ci/meshac-1211-dslfix-package-d944357c-once` | `e89942902e699718bd957b9d2d46c8d0e5f83cb3` | `archive/pre-openvulcan-20261113/ci/meshac-1211-dslfix-package-d944357c-once` |
| `ci/meshac-1211-layoutfix-package-d944357c-once` | `e2cbf7d15032bfd1ee564a661639edda0362ae9f` | `archive/pre-openvulcan-20261113/ci/meshac-1211-layoutfix-package-d944357c-once` |
| `ci/meshac-1211-mount-controls-d944357c-once` | `687f33e9e875fb88fdc720235cdf49953ef7057f` | `archive/pre-openvulcan-20261113/ci/meshac-1211-mount-controls-d944357c-once` |
| `ci/meshac-1211-package-d944357c-once` | `4771b0666426eb32ee9d93829bd8fabe0fb4c97f` | `archive/pre-openvulcan-20261113/ci/meshac-1211-package-d944357c-once` |
| `ci/meshac-1211-qualified-package-d944357c-once` | `82f3f4772ea98bf24b3e90a187608713304c7553` | `archive/pre-openvulcan-20261113/ci/meshac-1211-qualified-package-d944357c-once` |
| `docs/contributing-bug-report-20261016` | `e8b637f4a655bbc51cf6b39f3c8d8d3826f8398e` | `archive/pre-openvulcan-20261113/docs/contributing-bug-report-20261016` |
| `docs/readme-release-scope-20261016` | `57ba57b2bcd57ea93391a9e2312fb8c9df74d9af` | `archive/pre-openvulcan-20261113/docs/readme-release-scope-20261016` |
| `docs/sanitize-fp-notes-20261016` | `9e701c8bd0ede0b9cf6bd733c1c6347916e1ab16` | `archive/pre-openvulcan-20261113/docs/sanitize-fp-notes-20261016` |
| `docs/sanitize-paths-20261016` | `d1988bdde9e7dab8f08f691a0581dd72567a2e17` | `archive/pre-openvulcan-20261113/docs/sanitize-paths-20261016` |
| `fix/highjump-airchain-laundering` | `045a6182554656c6a7d753eebf7a9b0e41d4655f` | `archive/pre-openvulcan-20261113/fix/highjump-airchain-laundering` |
| `flatseries-0067-0103` | `41da62cea0099abe79de9512489505e87cc492c8` | `archive/pre-openvulcan-20261113/flatseries-0067-0103` |
| `flatseries-0107-0124` | `773e0c88455b47840090e0d92aaa052e5545f136` | `archive/pre-openvulcan-20261113/flatseries-0107-0124` |
| `main` | `db12796db170c7f5cc705146b264dc024bfb4ebf` | `archive/pre-openvulcan-20261113/main` |
| `port/airchain-main-20261026` | `d944357c7b1edf20ac90b330cdbfd266ee1101c8` | `archive/pre-openvulcan-20261113/port/airchain-main-20261026` |
| `rebuild/openvulcan-fabric` | `d2be3ab3a95a0d90fec50f9eb4bcf4e69222a4b0` | `archive/pre-openvulcan-20261113/rebuild/openvulcan-fabric` |
| `review-stack-20261015` | `3656aec0946c63bc2368732d8ce406a421a9e983` | `archive/pre-openvulcan-20261113/review-stack-20261015` |
| `round1-fixes-20261015` | `dcf3e6e6291956369a8317be7619fc5e7116c430` | `archive/pre-openvulcan-20261113/round1-fixes-20261015` |
| `test/ground-witness-r2-code` | `4f3f9e1f1e333a7ce71bcfb345d5573f7607d296` | `archive/pre-openvulcan-20261113/test/ground-witness-r2-code` |

## Preservation policy

Old detection branches are not blindly merged into the port. Some are already ancestors; others hold unique patch history. The archive tags preserve every tip and its reachable history. Review a patch against the upstream port before applying it. Old-code removal is deferred until a finished, rights-cleared v1 has actually been published. Archive history remains recoverable.

The existing main README-only commit is merged into the rebuild history, then replaced with current project documentation. Main advances normally, without force pushing or dropping its prior history. Visibility stays private.

## Completed consolidation

The rebuild and old-main histories were merged as `34b721f11c573c38b044e36910092a67b3c57d88`. Main advanced normally, with no force push. All 22 original branch tips were checked against their remote archive tags. The 21 obsolete branch heads were then removed in one atomic push with an exact expected-tip lease per head. A head that had moved would have stopped the deletion. The two remaining remote heads are `main` and `legacy/pre-openvulcan`.

[branch-audit-20261113.tsv](branch-audit-20261113.tsv) records reachability/unique-patch counts before cleanup. [branch-cleanup-20261113.json](branch-cleanup-20261113.json) records the exact removed tips, archive tags and verified remaining heads. Unique work remains in reachable tagged history, not erased. The old-code recovery branch is still present; v1 has not been published.

Receipt details: 22 archive tags include old main, which is not among the 21 deleted heads. The rebuild archive captured d2be3ab before subsequent commits; its deleted head34b721f was already the same merge on main, so those later commits remain reachable there. The cleanup JSON main34b721f is the deletion-time snapshot. The subsequent5f5ebf8 commit added receipts/docs only. These are ordering differences, not missing recovery history.
