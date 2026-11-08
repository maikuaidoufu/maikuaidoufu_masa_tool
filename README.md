# maikuaidoufu_masa_tool

Doufu's Masa Tool stops observers from pulsing inside chosen regions. Groups and
sub-regions are stored on the server, and the interface runs on MaLiLib. Regions
can also be imported from Litematica placements or area selections.

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
