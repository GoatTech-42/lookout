# sim/version/lib - compile/test-only API jars for the translator hook contract
Not shipped in the mod jar; build.gradle declares the same coordinates as compileOnly.
- viaversion-api-5.12.2.jar: https://repo.viaversion.com/com/viaversion/viaversion-api/5.12.2/viaversion-api-5.12.2.jar (release)
- geyser-api-2.12.1-SNAPSHOT.jar: https://repo.opencollab.dev/maven-snapshots/org/geysermc/geyser/api/2.12.1-SNAPSHOT/2.12.1-20261112.131201-30/api-2.12.1-20261112.131201-30.jar
- floodgate-api-2.2.4-SNAPSHOT.jar: https://repo.opencollab.dev/maven-snapshots/org/geysermc/floodgate/api/2.2.4-SNAPSHOT/2.2.4-20250930.181800-5/api-2.2.4-20250930.181800-5.jar
The MeshVersion.fromProtocol table was pinned from the ViaVersion 5.12.2 ProtocolVersion release registry, dumped from this exact viaversion-api jar.
- base-api-1.0.3.jar: https://repo.opencollab.dev/main/org/geysermc/api/base-api/1.0.3/base-api-1.0.3.jar (GeyserApi superinterface; geyser api pom compile dep)
- immutable-2.0.jar: https://repo.opencollab.dev/main/org/cloudburstmc/math/immutable/2.0/immutable-2.0.jar (geyser api pom compile dep)
- annotations-24.0.0.jar: https://repo1.maven.org/maven2/org/jetbrains/annotations/24.0.0/annotations-24.0.0.jar (geyser api pom compile dep)
- guava-33.4.8-jre.jar: https://repo1.maven.org/maven2/com/google/guava/guava/33.4.8-jre/guava-33.4.8-jre.jar (test-harness only: ViaAPI signature/proxy loading)
- netty-buffer-5.0.0.Alpha2.jar + netty-common-5.0.0.Alpha2.jar: https://repo1.maven.org/maven2/io/netty/ (test-harness only: ViaAPI method signatures reference io.netty.buffer.ByteBuf; class-load resolution for proxy generation, no netty code executes)
- cumulus-1.1.2.jar: https://repo.opencollab.dev/maven-releases/org/geysermc/cumulus/cumulus/1.1.2/cumulus-1.1.2.jar (test-harness only: GeyserApi signatures reference org.geysermc.cumulus.form.Form)
- events-1.2-20260725.205006-4.jar: https://repo.opencollab.dev/maven-snapshots/org/geysermc/event/events/1.2-SNAPSHOT/events-1.2-20260725.205006-4.jar (test-harness only: GeyserApi.eventBus() signature)
