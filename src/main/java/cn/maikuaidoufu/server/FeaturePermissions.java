package cn.maikuaidoufu.server;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.permissions.Feature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
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
    private static final String COMMENT_KEY = "_comment";
    private static final String OWNER_BYPASS_KEY = "ownerBypass";
    private static final String COMMENT =
        "Allowed values: EVERYONE, MODERATOR, OPS (default), ADMIN, OWNER, NOBODY. "
            + "OPS = command permission level 2. ownerBypass lets players manage groups/regions they created. "
            + "Run /masatool reload (or restart the server) after editing.";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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

            if (!root.has(COMMENT_KEY) || !root.has(OWNER_BYPASS_KEY))
            {
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
        JsonObject root = new JsonObject();
        root.addProperty(COMMENT_KEY, COMMENT);

        for (Feature feature : Feature.values())
        {
            root.addProperty(feature.configKey(), values.getOrDefault(feature, Level.OPS).name());
        }

        root.addProperty(OWNER_BYPASS_KEY, bypass);

        try
        {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            MasaToolMod.LOGGER.error("Failed to save permissions config to {}", file, e);
        }
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
