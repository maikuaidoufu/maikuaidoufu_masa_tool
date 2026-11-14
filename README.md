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

The "Chunk load watcher" tab watches selected chunks on the server. Type chunk
coordinates (or click "Current chunk" to fill the input) and press Add, then
enable the subscription; the tab logs every transition between the four
loading tiers: entity ticking (strong),
block ticking, loaded-only (weak), and unloaded. Each line includes the old and
new ticket levels and the exact tickets added or removed since the previous tier
change (player simulation, `/forceload`, portals, ender pearls and so on), plus
the tickets currently in effect. A per-chunk ticket snapshot makes that diff
possible; only watched chunks are reported, so an empty watch list costs almost
nothing on the server.

## Permissions

Server-side feature access lives in its own file,
`config/maikuaidoufu_masa_tool-permissions.json`, created on the first server
start. **Every feature defaults to `OPS`.** Edit the file, then run
`/masatool reload` (or restart the server) to apply it without a restart.

```json
// 权限配置：每一项控制一个功能所需的权限，默认全部 OPS。
// 可选值：EVERYONE / MODERATOR / OPS / ADMIN / OWNER / NOBODY。改完执行 /masatool reload。
{
  // 查看区域列表；设为 EVERYONE 可让所有玩家看到区域框
  "viewRegions": "OPS",
  "createGroup": "OPS",
  "addDefaultRegion": "OPS",
  "editGroup": "OPS",
  "deleteGroup": "OPS",
  "clearGroup": "OPS",
  "addRegion": "OPS",
  "editRegion": "OPS",
  "portalLog": "OPS",
  "chunkMonitor": "OPS",
  "reloadConfig": "OPS",
  "ownerBypass": false
}
```

The generated file has a **Chinese comment above every setting** plus a short
header. Both `//` line comments and `/* ... */` block comments are accepted.
When the file needs to be regenerated (a new feature key, an invalid value, or
an old file using the `_comment` field), hand-written comments are **preserved**
next to the key they were written above; only missing comments are regenerated.
Put notes directly above their setting, or before the opening brace for
file-level notes.

Allowed values:

| Value | Meaning |
|---|---|
| `EVERYONE` | any player |
| `MODERATOR` / `OPS` / `ADMIN` / `OWNER` | Minecraft command permission level 1 / 2 / 3 / 4. `OPS` is the default and matches the old OP-only check; a higher level also satisfies a lower one |
| `NOBODY` | feature disabled for everyone, including OPs |

| Key | Controls |
|---|---|
| `viewRegions` | receiving the region list (and therefore seeing outlines/GUI entries) |
| `createGroup` | creating a new group |
| `addDefaultRegion` | adding a sub-region to the built-in default group |
| `editGroup` | enabling/disabling and renaming a group |
| `deleteGroup`, `clearGroup` | deleting a group / removing all of its sub-regions |
| `addRegion`, `editRegion` | adding, toggling or deleting sub-regions in a regular group |
| `portalLog` | subscribing to the nether portal travel terminal |
| `chunkMonitor` | subscribing to / managing the chunk loading watcher |
| `reloadConfig` | running `/masatool reload` |

`ownerBypass` (default `false`) lets players manage groups and sub-regions they
created themselves even without the matching permission. The built-in default
group has no owner, so `clearGroup` still needs an explicit permission.

Notes:

- With the default `viewRegions: OPS`, ordinary players get an **empty region
  list and no outlines**. Set it to `EVERYONE` if everyone should see the
  regions. Observer suppression itself is server-authoritative and still works.
- `allowRegionCreation` and `allowOwnerEdit` in the old
  `maikuaidoufu_masa_tool-server.json` are no longer used; that file now only
  holds the group/entry limits. There is no automatic migration, so set
  `ownerBypass` or the individual keys yourself if you relied on them.
- Client-only features (schematic verifier export, outline rendering, hotkeys)
  are unaffected by this file.
- The server re-checks every action, so a modified client cannot bypass these
  rules. The permission mask is sent in the group snapshot (protocol v4), which
  means client and server must be updated together.

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
