# generated lookout mixin Java proof, operator steps

NOT RUN today. Every step below needs a gradle run; expected results are not evidence.
Use the accepted Java-template fix, a clean dedicated checkout and JDK 25 on PATH.
No game, client, server, jar task or rig launch is needed. Coordinate RAM first.
Keep each target's command, exit status, Gradle log and generated JSON separately.

1. **needs a gradle run**, 1.21.11, remap route:
   ```sh
   ./gradlew --no-daemon -Dorg.gradle.jvmargs=-Xmx512m clean processResources -Premap=1 -Pminecraft_version=1.21.11 -Pfabric_api_version=0.141.6+1.21.11 -Pmc_range=1.21.11 -Pjava_min=21
   python3 -c 'import json; p="build/resources/main/lookout.mixins.json"; d=json.load(open(p)); assert d["compatibilityLevel"]=="JAVA_21" and d["required"] is True; print(p, "JAVA_21 required=true")'
   ```
   Expected: Gradle exit 0, processResources executed, BUILD SUCCESSFUL;
   inspection exit 0 prints `build/resources/main/lookout.mixins.json JAVA_21 required=true`.
2. **needs a gradle run**, 26.1.2, standard route:
   ```sh
   ./gradlew --no-daemon -Dorg.gradle.jvmargs=-Xmx512m clean processResources -Pminecraft_version=26.1.2 -Pfabric_api_version=0.155.3+26.1.2 -Pmc_range=26.1.x -Pjava_min=25
   python3 -c 'import json; p="build/resources/main/lookout.mixins.json"; d=json.load(open(p)); assert d["compatibilityLevel"]=="JAVA_25" and d["required"] is True; print(p, "JAVA_25 required=true")'
   ```
   Expected: same Gradle success; inspection prints the same path, JAVA_25 required=true.
3. **needs a gradle run**, 26.2: repeat step 2 with minecraft_version=26.2,
   fabric_api_version=0.159.0+26.2 and mc_range=26.2. Keep java_min=25.
   Expected: successful processResources plus the step-2 JSON assertion/output.
4. **needs a gradle run**, 26.3: repeat step 2 with minecraft_version=26.3,
   fabric_api_version=0.161.0+26.3 and mc_range=26.3. Keep java_min=25.
   Expected: successful processResources plus the step-2 JSON assertion/output.

Stop on Gradle failure, dependency/cache/auth errors, missing file, unresolved
${java}, wrong level or required:false. Never inspect an old file after a failed
command: run inspection only after that target's Gradle command exits 0.
Archive the inspected file before the next clean. clean deletes build outputs,
so use the dedicated checkout, not a checkout holding irreplaceable captures.
Do not pass remap at all for 26.x, even remap=false selects the remap build.
These checks prove processed resource bytes only, not jar contents, target Java
compilation, mixin application or runtime behavior. Jar/boot proof stays unverified.
Basis: settings.gradle, both build routes, versions/*.properties at e3a42fb.
