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

## Build

Requires JDK 25.

```
./gradlew build
```

The jar lands in `build/libs/`. `./gradlew runClient` starts a development client.

## License

MIT, see [LICENSE](LICENSE).

The two jars in `libs/` are third-party dependencies with separate licenses:

- `malilib` 0.28.12 by masa — LGPLv3
- `litematica` 0.27.14 by masa — LGPLv3
