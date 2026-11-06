package cn.maikuaidoufu.server;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.region.RegionLimits;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class ServerConfig
{
    public enum CreationPermission
    {
        OPS,
        EVERYONE
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "maikuaidoufu_masa_tool-server.json";

    private static CreationPermission creationPermission = CreationPermission.OPS;
    private static boolean allowOwnerEdit = true;
    private static int maxGroups = RegionLimits.MAX_GROUPS;
    private static int maxEntriesPerGroup = RegionLimits.MAX_ENTRIES_PER_GROUP;
    private static int maxTotalEntries = RegionLimits.MAX_TOTAL_ENTRIES;

    private ServerConfig()
    {
    }

    public static void load()
    {
        creationPermission = CreationPermission.OPS;
        allowOwnerEdit = true;
        maxGroups = RegionLimits.MAX_GROUPS;
        maxEntriesPerGroup = RegionLimits.MAX_ENTRIES_PER_GROUP;
        maxTotalEntries = RegionLimits.MAX_TOTAL_ENTRIES;

        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

        if (!Files.exists(file))
        {
            save(file);
            return;
        }

        try
        {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();

            if (root.has("allowRegionCreation"))
            {
                String value = root.get("allowRegionCreation").getAsString();

                if ("EVERYONE".equalsIgnoreCase(value))
                {
                    creationPermission = CreationPermission.EVERYONE;
                }
            }

            if (root.has("allowOwnerEdit"))
            {
                allowOwnerEdit = root.get("allowOwnerEdit").getAsBoolean();
            }

            maxGroups = clamp(root, "maxGroups", maxGroups, 1, RegionLimits.MAX_GROUPS);
            maxEntriesPerGroup = clamp(root, "maxEntriesPerGroup", maxEntriesPerGroup, 1, RegionLimits.MAX_ENTRIES_PER_GROUP);
            maxTotalEntries = clamp(root, "maxTotalEntries", maxTotalEntries, 1, RegionLimits.MAX_TOTAL_ENTRIES);
            MasaToolMod.LOGGER.info("Loaded server config from {}", file);
        }
        catch (Exception e)
        {
            MasaToolMod.LOGGER.error("Failed to load server config from {}", file, e);
        }
    }

    private static int clamp(JsonObject root, String key, int fallback, int min, int max)
    {
        if (!root.has(key))
        {
            return fallback;
        }

        int value = root.get(key).getAsInt();
        return Math.max(min, Math.min(max, value));
    }

    private static void save(Path file)
    {
        JsonObject root = new JsonObject();
        root.addProperty("allowRegionCreation", creationPermission.name());
        root.addProperty("allowOwnerEdit", allowOwnerEdit);
        root.addProperty("maxGroups", maxGroups);
        root.addProperty("maxEntriesPerGroup", maxEntriesPerGroup);
        root.addProperty("maxTotalEntries", maxTotalEntries);

        try
        {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            MasaToolMod.LOGGER.error("Failed to save server config to {}", file, e);
        }
    }

    public static boolean canCreate(ServerPlayer player)
    {
        return player != null && (isOp(player) || creationPermission == CreationPermission.EVERYONE);
    }

    public static boolean isOp(ServerPlayer player)
    {
        return player != null && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static boolean allowOwnerEdit()
    {
        return allowOwnerEdit;
    }

    public static int maxGroups()
    {
        return maxGroups;
    }

    public static int maxEntriesPerGroup()
    {
        return maxEntriesPerGroup;
    }

    public static int maxTotalEntries()
    {
        return maxTotalEntries;
    }
}
