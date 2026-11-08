package cn.maikuaidoufu.client;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Per-client highlight toggles, saved in the config directory.
 */
public final class ClientHighlightStore
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "maikuaidoufu_masa_tool-highlights.json";

    private static final Map<UUID, Boolean> GROUP_OVERRIDES = new HashMap<>();
    private static final Map<UUID, Boolean> ENTRY_OVERRIDES = new HashMap<>();

    private ClientHighlightStore()
    {
    }

    public static void load()
    {
        GROUP_OVERRIDES.clear();
        ENTRY_OVERRIDES.clear();

        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

        if (!Files.exists(file))
        {
            return;
        }

        try
        {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            readMap(root, "groups", GROUP_OVERRIDES);
            readMap(root, "entries", ENTRY_OVERRIDES);
        }
        catch (Exception e)
        {
            MasaToolMod.LOGGER.warn("Failed to load local highlight settings: {}", e.getMessage());
        }
    }

    private static void readMap(JsonObject root, String key, Map<UUID, Boolean> target)
    {
        if (!root.has(key) || !root.get(key).isJsonObject())
        {
            return;
        }

        for (Map.Entry<String, com.google.gson.JsonElement> entry : root.getAsJsonObject(key).entrySet())
        {
            try
            {
                target.put(UUID.fromString(entry.getKey()), entry.getValue().getAsBoolean());
            }
            catch (IllegalArgumentException ignored)
            {
            }
        }
    }

    public static boolean isGroupHighlighted(RegionGroup group)
    {
        if (!Configs.RENDER_HIGHLIGHTS.getBooleanValue())
        {
            return false;
        }

        return GROUP_OVERRIDES.getOrDefault(group.id(), true);
    }

    public static boolean isEntryHighlighted(RegionGroup group, RegionEntry entry)
    {
        if (!isGroupHighlighted(group))
        {
            return false;
        }

        return ENTRY_OVERRIDES.getOrDefault(entry.id(), true);
    }

    public static boolean groupOverride(UUID groupId)
    {
        return GROUP_OVERRIDES.getOrDefault(groupId, true);
    }

    public static boolean entryOverride(UUID entryId)
    {
        return ENTRY_OVERRIDES.getOrDefault(entryId, true);
    }

    public static void toggleGroup(UUID groupId)
    {
        GROUP_OVERRIDES.put(groupId, !groupOverride(groupId));
        save();
    }

    public static void toggleEntry(UUID entryId)
    {
        ENTRY_OVERRIDES.put(entryId, !entryOverride(entryId));
        save();
    }

    public static void save()
    {
        JsonObject root = new JsonObject();
        root.add("groups", writeMap(GROUP_OVERRIDES));
        root.add("entries", writeMap(ENTRY_OVERRIDES));

        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

        try
        {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            MasaToolMod.LOGGER.warn("Failed to save local highlight settings: {}", e.getMessage());
        }
    }

    private static JsonObject writeMap(Map<UUID, Boolean> source)
    {
        JsonObject object = new JsonObject();

        for (Map.Entry<UUID, Boolean> entry : source.entrySet())
        {
            object.addProperty(entry.getKey().toString(), entry.getValue());
        }

        return object;
    }
}
