# Bounty Highlighter (Fabric, client-side)

Open `/bounty` -> each player head is tinted by how long ago YOUR client first saw it:
green < 5 min, yellow 5-20 min, red 20+ min. Middle-click a head to copy the name.

## Build
1. Get exact versions from https://fabricmc.net/develop and put them in gradle.properties.
2. Add the Gradle wrapper (`gradle wrapper`), then `./gradlew build`.
3. Jar: build/libs/bounty-highlighter-1.0.0.jar

## Publish on Modrinth
1. Create a project at modrinth.com (type: Mod, client-side only, Fabric loader).
2. Upload the jar as a version, set game version 1.21.11, loader Fabric, dependency Fabric API.
3. Add a description, icon, and license (MIT is set in fabric.mod.json).
