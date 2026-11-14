# maikuaidoufu_masa_tool

> 本分支适用于 **Minecraft 1.21.11**；26.1.2 请使用 `main` 分支。
> This branch targets **Minecraft 1.21.11**; use `main` for 26.1.2.

[简体中文](#zh-cn) · [English](#en)

---

<a id="zh-cn"></a>

## 简体中文

Doufu's Masa Tool 是一个 Fabric 模组：在指定区域内阻止观察者（侦测器）发出脉冲、记录地狱门传送、检测区块加载等级的变化。区域组与子区域保存在服务端，界面基于 MaLiLib；区域也可以从 Litematica 投影或选区导入。

安装 techutils 后，原理图验证界面会多出一个导出按钮：勾选类别后，把错误方块、缺失／多余方块以及容器槽位差异按坐标逐行写入 `maikuaidoufu_masa_tool-exports/` 下的文本文件，每行给出期望值与实际值。

「区块加载检测」页签监视服务端上选定的区块。输入区块坐标（或点「当前区块」把坐标填入输入框）后点「添加」，再开启订阅，页签就会记录四级加载状态之间的每一次变化：实体刻（强加载）、方块刻、仅加载（弱加载）、未加载。每行都带旧／新 ticket 等级，以及自上次跨级变化以来精确新增或移除的 ticket（玩家模拟距离、`/forceload`、传送门、末影珍珠等）和当前生效的 ticket。这靠每个区块的 ticket 快照做差集实现；只上报被关注的区块，所以关注列表为空时服务端几乎没有开销。

### 权限配置

服务端逐功能权限放在独立文件 `config/maikuaidoufu_masa_tool-permissions.json`，服务器首次启动时生成。**每一项默认都是 `OPS`。** 改完执行 `/masatool reload`（或重启服务器）即可生效，无需重启。

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

生成的文件在每个配置项上方都有一行**中文注释**，开头还有一段说明。支持 `//` 行注释和 `/* ... */` 块注释。当文件需要被重写时（新增功能键、值非法、或旧版使用 `_comment` 字段），手写的注释会**保留**在原来对应的键上方，只有缺失的注释才重新生成。注释请写在配置项正上方，或写在左花括号之前作为文件级说明。

可选值：

| 值 | 含义 |
|---|---|
| `EVERYONE` | 所有玩家 |
| `MODERATOR` / `OPS` / `ADMIN` / `OWNER` | 对应 Minecraft 命令权限等级 1 / 2 / 3 / 4。`OPS` 是默认值，等价于以前的 OP 判定；高等级自动满足低等级要求 |
| `NOBODY` | 对所有人禁用该功能，包括 OP |

| 键 | 控制什么 |
|---|---|
| `viewRegions` | 接收区域列表（决定能否看到高亮框与界面条目） |
| `createGroup` | 创建新的区域组 |
| `addDefaultRegion` | 向内置默认组添加子区域 |
| `editGroup` | 启用／禁用、重命名组 |
| `deleteGroup`, `clearGroup` | 删除组／清空组内全部子区域 |
| `addRegion`, `editRegion` | 在普通组中增删、开关子区域 |
| `portalLog` | 订阅地狱门传送日志 |
| `chunkMonitor` | 订阅／管理区块加载检测 |
| `reloadConfig` | 执行 `/masatool reload` |

`ownerBypass`（默认 `false`）允许玩家管理自己创建的组和子区域，即使没有对应权限。内置默认组没有 owner，因此清空它仍需要 `clearGroup` 权限。

注意：

- 默认 `viewRegions: OPS` 意味着普通玩家拿到的是**空的区域列表，也看不到高亮框**。想让所有人都能看到就设为 `EVERYONE`。观察者抑制本身由服务端判定，不受影响。
- 旧 `maikuaidoufu_masa_tool-server.json` 里的 `allowRegionCreation` 和 `allowOwnerEdit` 已不再使用，该文件现在只保存数量限额。**没有自动迁移**，如果之前依赖这两项，请手动设置 `ownerBypass` 或对应功能键。
- 纯客户端功能（原理图验证导出、区域高亮、热键）不受此文件影响。
- 服务端会重新校验每一次操作，改客户端也绕不过去。权限位掩码通过区域快照下发（协议 v4），因此客户端和服务端必须一起更新。

### 客户端日志缓存

地狱门日志和区块加载检测各自在客户端保留最近若干条记录（默认 1000，可在「通用」页签调整，范围 100~10000）。历史会带节流地写入 `config/maikuaidoufu_masa_tool-logcache.json`，因此重新打开界面或重启游戏后旧记录仍在。保存的是渲染后的文本，所以已有记录保持记录时的语言。这些内容不会发给服务端；两个页签的「清空」按钮也会一并清掉已保存的历史。

### 构建

需要 JDK 21。

```
./gradlew build
```

产物在 `build/libs/`，`./gradlew runClient` 启动开发客户端。

### 许可

MIT，见 [LICENSE](LICENSE)。

`libs/` 里三个 jar 是第三方依赖，各有独立许可：

- `malilib` 0.27.20 by masa — LGPLv3
- `litematica` 0.26.16 by masa — LGPLv3
- `techutils` 0.8.2+1.21.11 by KikuGie — Unlicense

---

<a id="en"></a>

## English

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

### Permissions

Server-side feature access lives in its own file,
`config/maikuaidoufu_masa_tool-permissions.json`, created on the first server
start. **Every feature defaults to `OPS`.** Edit the file, then run
`/masatool reload` (or restart the server) to apply the changes.

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

### Client-side log cache

The portal log and the chunk watcher each keep their most recent lines on the
client (default 1000; adjustable in the General tab, range 100-10000). The
history is written to `config/maikuaidoufu_masa_tool-logcache.json` with a
short throttle, so re-opening the GUI or restarting the game keeps earlier
lines. Only the rendered text is stored, so existing lines keep the language
they were recorded in. Nothing is sent to the server, and the Clear buttons in
both tabs erase the saved history as well.

### Build

Requires JDK 21.

```
./gradlew build
```

The jar lands in `build/libs/`. `./gradlew runClient` starts a development client.

### License

MIT, see [LICENSE](LICENSE).

The three jars in `libs/` are third-party dependencies with separate licenses:

- `malilib` 0.27.20 by masa — LGPLv3
- `litematica` 0.26.16 by masa — LGPLv3
- `techutils` 0.8.2+1.21.11 by KikuGie — Unlicense
