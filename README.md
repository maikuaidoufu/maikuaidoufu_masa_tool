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
actual value.

The "Chunk loading" tab watches selected chunks on the server. Add chunk
coordinates (or click "Current chunk"), enable the subscription, and the tab
logs every transition between the four loading tiers: entity ticking (strong),
block ticking, loaded-only (weak), and unloaded. Each line includes the old and
new ticket levels and the exact tickets added or removed since the previous tier
change (player simulation, `/forceload`, portals, ender pearls and so on), plus
the tickets currently in effect. A per-chunk ticket snapshot makes that diff
possible; only watched chunks are reported, so an empty watch list costs almost
nothing on the server.

## Build

Requires JDK 25.

```
./gradlew build
```

The jar lands in `build/libs/`. `./gradlew runClient` starts a development client.

## License

MIT, see [LICENSE](LICENSE).

The three jars in `libs/` are third-party dependencies with separate licenses:

- `malilib` 0.28.12 by masa — LGPLv3
- `litematica` 0.27.14 by masa — LGPLv3
- `techutils` 0.8.2+26.1 by KikuGie — Unlicense
