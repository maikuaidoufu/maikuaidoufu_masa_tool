# maikuaidoufu_masa_tool

Doufu's Masa Tool stops observers from pulsing inside chosen regions and keeps a
terminal of nether portal travels: who went through, where it came out, and whether
the game linked an existing portal or built a new one. Groups and sub-regions are
stored on the server, and the interface runs on MaLiLib. Regions can also be imported
from Litematica placements or area selections.

## Version

This branch targets **Minecraft 1.21.11** (Fabric). The `main` branch targets 26.1.2.

## Build

Requires JDK 21.

```
./gradlew build
```

If your default JDK is newer than 21, point Gradle at a JDK 21 install:

```
./gradlew -Dorg.gradle.java.home="C:\Program Files\Java\jdk-21" build
```

The jar lands in `build/libs/`. `./gradlew runClient` starts a development client.

## License

MIT, see [LICENSE](LICENSE).

The two jars in `libs/` are third-party dependencies with separate licenses:

- `malilib` 0.27.20 by masa — LGPLv3
- `litematica` 0.26.16 by masa — LGPLv3
