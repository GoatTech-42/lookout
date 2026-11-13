# Disposable lifecycle observer

This is test-only code, never a shipping meshac component. It reads current sessions and logs Fabric callbacks; it does not patch production state, replace a hook, change gates, or enable Veil.

The retained runners describe the isolated run layout: a disposable directory with server/, client/, observer/ and downloaded public client dependencies. They are evidence, not a ready-to-run general launcher. Read and adapt their paths and stop limits before use. Use only test identities on a loopback endpoint, never real account tokens, user worlds or a public listener.

The observer jar is compiled separately against the exact shipping jar, mapped native jar and Fabric API modules. Hash both jars. The server log must identify observer presence. Bare shipping boot evidence and observer-present lifecycle evidence are different claims.

All 179 actual check instances were gated in the recorded run. JumpD is an empty source placeholder and is not instantiated. These logs do not show a working detector or exactly-once velocity dispatch.
