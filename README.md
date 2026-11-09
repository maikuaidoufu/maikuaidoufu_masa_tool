# maikuaidoufu_masa_tool

Doufu's Masa Tool stops observers from pulsing inside chosen regions and keeps a
terminal of nether portal travels: who went through, where it came out, and whether
the game linked an existing portal or built a new one. Groups and sub-regions are
stored on the server, and the interface runs on MaLiLib. Regions can also be imported
from Litematica placements or area selections.

With techutils installed, the schematic verifier screen also gains an export
button: pick the categories, and the mismatched blocks, missing/extra blocks and
container slot differences are written to a text file under
`maikuaidoufu_masa_tool-exports/`, one line per position with the expected and
actual value. The verifier screen also gets a "General" tab with the export toggle.

All user-visible text, including portal log lines and exported files, follows the
client language (English / Chinese). The server only sends language-neutral events.

## Version

This branch targets **Minecraft 1.21.11** (Fabric). The `main` branch targets 26.1.2.
Both branches are feature-aligned at 1.0.4; their network protocol is v2, so the
client and server must use the same mod version.

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

The three jars in `libs/` are third-party dependencies with separate licenses:

- `malilib` 0.27.20 by masa — LGPLv3
- `litematica` 0.26.16 by masa — LGPLv3
- `techutils` 0.8.2+1.21.11 by KikuGie — Unlicense
