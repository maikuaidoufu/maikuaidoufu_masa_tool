package cn.maikuaidoufu.server;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.permissions.Feature;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

/**
 * Reads {@code config/maikuaidoufu_masa_tool-permissions.json} and answers every
 * server-side permission question. Each feature defaults to {@link Level#OPS},
 * matching the previous hard-coded "OP only" behaviour.
 *
 * <p>The configuration is a snapshot loaded on server start and on
 * {@code /masatool reload}; permission checks only read an immutable map, so
 * they are safe to call from network handlers and hot paths.</p>
 */
public final class FeaturePermissions
{
    /**
     * Required access level. The four MODERATOR..OWNER levels map to Minecraft's
     * command permission tiers 1..4; a higher tier always satisfies a lower one.
     */
    public enum Level
    {
        EVERYONE,
        MODERATOR,
        OPS,
        ADMIN,
        OWNER,
        NOBODY
    }

    private static final String FILE_NAME = "maikuaidoufu_masa_tool-permissions.json";
    private static final String OWNER_BYPASS_KEY = "ownerBypass";

    /** Configs generated before comments became real comment lines carry this key. */
    private static final String LEGACY_COMMENT_KEY = "_comment";

    private static final List<String> DEFAULT_HEADER = List.of(
        "// 权限配置：每一项控制一个功能所需的权限，默认全部 OPS。",
        "// 可选值：EVERYONE（所有人）/ MODERATOR / OPS / ADMIN / OWNER（对应命令权限 1~4 级，高档满足低档）/ NOBODY（禁用，连 OP 也不能用）。",
        "// OPS = 命令权限等级 2。修改后执行 /masatool reload 或重启服务器生效；手写注释在自动重写时会被保留。"
    );

    private static final Permission[] TIERS = {
        Permissions.COMMANDS_MODERATOR,
        Permissions.COMMANDS_GAMEMASTER,
        Permissions.COMMANDS_ADMIN,
        Permissions.COMMANDS_OWNER
    };

    private static volatile Map<Feature, Level> levels = defaults();
    private static volatile boolean ownerBypass;

    private FeaturePermissions()
    {
    }

    private static Map<Feature, Level> defaults()
    {
        Map<Feature, Level> map = new EnumMap<>(Feature.class);

        for (Feature feature : Feature.values())
        {
            map.put(feature, Level.OPS);
        }

        return Map.copyOf(map);
    }

    public static void load()
    {
        levels = defaults();
        ownerBypass = false;

        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

        if (!Files.exists(file))
        {
            save(file, defaults(), false);
            MasaToolMod.LOGGER.info("Created default permissions config at {}", file);
            return;
        }

        try
        {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<Feature, Level> parsed = defaults();
            Map<Feature, Level> mutable = new EnumMap<>(Feature.class);
            boolean needsRewrite = false;

            for (Feature feature : Feature.values())
            {
                String key = feature.configKey();

                if (!root.has(key) || !root.get(key).isJsonPrimitive())
                {
                    needsRewrite = true;
                    continue;
                }

                String raw = root.get(key).getAsString();
                Level level = parseLevel(raw);

                if (level == null)
                {
                    MasaToolMod.LOGGER.warn("Unknown permission value for {}: {} (using OPS)", key, raw);
                    needsRewrite = true;
                    continue;
                }

                mutable.put(feature, level);
            }

            for (Map.Entry<Feature, Level> entry : parsed.entrySet())
            {
                mutable.putIfAbsent(entry.getKey(), entry.getValue());
            }

            boolean bypass = false;

            if (root.has(OWNER_BYPASS_KEY) && root.get(OWNER_BYPASS_KEY).isJsonPrimitive())
            {
                try
                {
                    bypass = root.get(OWNER_BYPASS_KEY).getAsBoolean();
                }
                catch (RuntimeException e)
                {
                    MasaToolMod.LOGGER.warn("Invalid ownerBypass value (using false)");
                    needsRewrite = true;
                }
            }

            if (!root.has(OWNER_BYPASS_KEY) || root.has(LEGACY_COMMENT_KEY))
            {
                // A missing ownerBypass needs a rewrite; an old-style
                // _comment field is migrated to real comment lines.
                needsRewrite = true;
            }

            levels = Map.copyOf(mutable);
            ownerBypass = bypass;

            // Add newly introduced keys / remove invalid values without
            // clobbering a hand-edited file that is already complete.
            if (needsRewrite)
            {
                save(file, levels, ownerBypass);
            }

            MasaToolMod.LOGGER.info("Loaded permissions config from {}", file);
        }
        catch (Exception e)
        {
            MasaToolMod.LOGGER.error("Failed to load permissions config from {}, using OP-only defaults", file, e);
            levels = defaults();
            ownerBypass = false;
        }
    }

    private static Level parseLevel(String value)
    {
        if (value == null)
        {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');

        // Accept the old server-config spelling and a few common aliases.
        if ("ALL".equals(normalized) || "TRUE".equals(normalized))
        {
            return Level.EVERYONE;
        }

        if ("NONE".equals(normalized) || "FALSE".equals(normalized) || "DISABLED".equals(normalized))
        {
            return Level.NOBODY;
        }

        for (Level level : Level.values())
        {
            if (level.name().equals(normalized))
            {
                return level;
            }
        }

        return null;
    }

    private static void save(Path file, Map<Feature, Level> values, boolean bypass)
    {
        List<PermissionConfigText.Item> items = new ArrayList<>();

        for (Feature feature : Feature.values())
        {
            items.add(new PermissionConfigText.Item(
                feature.configKey(),
                "\"" + values.getOrDefault(feature, Level.OPS).name() + "\"",
                defaultNote(feature)));
        }

        items.add(new PermissionConfigText.Item(
            OWNER_BYPASS_KEY,
            Boolean.toString(bypass),
            "允许玩家管理自己创建的组/子区域，即使没有对应权限（默认关）；默认组无 owner，清空仍需 clearGroup 权限"));

        // Reuse the comments already present in the file so hand-written notes
        // survive a rewrite (new key, invalid value, old _comment format, ...).
        String text = PermissionConfigText.render(
            items, DEFAULT_HEADER, PermissionConfigText.readNotes(file));

        try
        {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            MasaToolMod.LOGGER.error("Failed to save permissions config to {}", file, e);
        }
    }

    private static String defaultNote(Feature feature)
    {
        return switch (feature)
        {
            case VIEW_REGIONS -> "查看区域列表；设为 EVERYONE 可让所有玩家看到区域框";
            case CREATE_GROUP -> "创建新的区域组";
            case ADD_DEFAULT_REGION -> "向内置默认组添加子区域（默认组无 owner）";
            case EDIT_GROUP -> "启用/禁用、重命名组";
            case DELETE_GROUP -> "删除组（默认组不可删除）";
            case CLEAR_GROUP -> "清空组内所有子区域（默认组仍需本权限）";
            case ADD_REGION -> "向普通组添加子区域";
            case EDIT_REGION -> "启用/禁用、删除子区域";
            case PORTAL_LOG -> "订阅地狱门传送日志";
            case CHUNK_MONITOR -> "订阅/管理区块加载检测";
            case RELOAD_CONFIG -> "执行 /masatool reload 热重载本配置";
        };
    }

    public static boolean allows(ServerPlayer player, Feature feature)
    {
        if (player == null || feature == null)
        {
            return false;
        }

        Level level = levels.getOrDefault(feature, Level.OPS);

        return switch (level)
        {
            case EVERYONE -> true;
            case NOBODY -> false;
            case MODERATOR -> hasLevel(player, 1);
            case OPS -> hasLevel(player, 2);
            case ADMIN -> hasLevel(player, 3);
            case OWNER -> hasLevel(player, 4);
        };
    }

    public static boolean isOp(ServerPlayer player)
    {
        return player != null && hasLevel(player, 2);
    }

    public static boolean ownerBypass()
    {
        return ownerBypass;
    }

    public static Level level(Feature feature)
    {
        return levels.getOrDefault(feature, Level.OPS);
    }

    /**
     * Packs the player's effective permissions into the group-list snapshot so
     * the client can mirror the server's decisions without a second request.
     */
    public static int mask(ServerPlayer player)
    {
        int mask = 0;

        for (Feature feature : Feature.values())
        {
            if (allows(player, feature))
            {
                mask |= feature.bit();
            }
        }

        if (ownerBypass)
        {
            mask |= Feature.OWNER_BYPASS_BIT;
        }

        return mask;
    }

    private static boolean hasLevel(ServerPlayer player, int minimum)
    {
        PermissionSet set = player.permissions();

        for (int tier = minimum - 1; tier < TIERS.length; tier++)
        {
            if (set.hasPermission(TIERS[tier]))
            {
                return true;
            }
        }

        return false;
    }
}
