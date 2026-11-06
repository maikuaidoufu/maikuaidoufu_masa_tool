package cn.maikuaidoufu.server;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.region.Box;
import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * World-local persistence for the complete group tree.
 */
public final class RegionStore
{
    private static final int SCHEMA_VERSION = 1;
    private static final String DIRECTORY = "maikuaidoufu_masa_tool";
    private static final String FILE_NAME = "regions.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path file;

    private RegionStore()
    {
    }

    public static void load(MinecraftServer server)
    {
        file = server.getWorldPath(LevelResource.ROOT).resolve(DIRECTORY).resolve(FILE_NAME);
        RegionManager.clear();

        boolean fileExisted = Files.exists(file);

        if (fileExisted)
        {
            try
            {
                JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonArray groupsJson = root.has("groups") ? root.getAsJsonArray("groups") : new JsonArray();
                List<RegionGroup> loaded = new ArrayList<>();

                for (JsonElement element : groupsJson)
                {
                    RegionGroup group = readGroup(element);

                    if (group != null)
                    {
                        loaded.add(group);
                    }
                }

                RegionManager.setGroups(loaded);
                MasaToolMod.LOGGER.info("Loaded {} groups ({} entries) from {}",
                    loaded.size(), RegionManager.totalEntryCount(), file);
            }
            catch (Exception e)
            {
                MasaToolMod.LOGGER.error("Failed to load region data from {}", file, e);
            }
        }
        else
        {
            MasaToolMod.LOGGER.info("No region data yet, creating {}", file);
        }

        boolean hadDefault = hasDefaultGroup();
        RegionManager.ensureDefaultGroup(System.currentTimeMillis());

        if (!fileExisted || !hadDefault)
        {
            save();
        }
    }

    private static boolean hasDefaultGroup()
    {
        for (RegionGroup group : RegionManager.getGroups())
        {
            if (group.defaultGroup())
            {
                return true;
            }
        }

        return false;
    }

    public static void save()
    {
        if (file == null)
        {
            return;
        }

        JsonObject root = new JsonObject();
        root.addProperty("version", SCHEMA_VERSION);
        JsonArray groups = new JsonArray();

        for (RegionGroup group : RegionManager.getGroups())
        {
            groups.add(writeGroup(group));
        }

        root.add("groups", groups);

        try
        {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(FILE_NAME + ".tmp");
            Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);

            try
            {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException e)
            {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e)
        {
            MasaToolMod.LOGGER.error("Failed to save region data to {}", file, e);
        }
    }

    private static JsonObject writeGroup(RegionGroup group)
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", group.id().toString());
        obj.addProperty("name", group.name());
        obj.addProperty("default_group", group.defaultGroup());
        obj.addProperty("enabled", group.enabled());
        obj.addProperty("source", group.source());
        obj.addProperty("owner", group.owner() == null ? "" : group.owner().toString());
        obj.addProperty("owner_name", group.ownerName());
        obj.addProperty("created_at", group.createdAt());
        obj.addProperty("updated_at", group.updatedAt());

        JsonArray entries = new JsonArray();

        for (RegionEntry entry : group.entries())
        {
            JsonObject entryJson = new JsonObject();
            entryJson.addProperty("id", entry.id().toString());
            entryJson.addProperty("name", entry.name());
            entryJson.addProperty("dimension", entry.dimension());
            entryJson.addProperty("enabled", entry.enabled());
            entryJson.addProperty("owner", entry.owner() == null ? "" : entry.owner().toString());
            entryJson.addProperty("owner_name", entry.ownerName());
            entryJson.addProperty("created_at", entry.createdAt());
            entryJson.add("box", writeBox(entry.box()));
            entries.add(entryJson);
        }

        obj.add("entries", entries);
        return obj;
    }

    private static JsonArray writeBox(Box box)
    {
        JsonArray array = new JsonArray();
        array.add(box.minX());
        array.add(box.minY());
        array.add(box.minZ());
        array.add(box.maxX());
        array.add(box.maxY());
        array.add(box.maxZ());
        return array;
    }

    private static RegionGroup readGroup(JsonElement element)
    {
        if (!element.isJsonObject())
        {
            return null;
        }

        try
        {
            JsonObject obj = element.getAsJsonObject();
            UUID id = UUID.fromString(obj.get("id").getAsString());
            String name = obj.get("name").getAsString();
            boolean defaultGroup = obj.has("default_group") && obj.get("default_group").getAsBoolean();
            boolean enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();
            String source = obj.has("source") ? obj.get("source").getAsString() : "";
            UUID owner = readUuid(obj, "owner");
            String ownerName = obj.has("owner_name") ? obj.get("owner_name").getAsString() : "";
            long createdAt = obj.has("created_at") ? obj.get("created_at").getAsLong() : 0L;
            long updatedAt = obj.has("updated_at") ? obj.get("updated_at").getAsLong() : createdAt;
            List<RegionEntry> entries = new ArrayList<>();

            if (obj.has("entries") && obj.get("entries").isJsonArray())
            {
                for (JsonElement entryElement : obj.getAsJsonArray("entries"))
                {
                    RegionEntry entry = readEntry(entryElement);

                    if (entry != null)
                    {
                        entries.add(entry);
                    }
                }
            }

            return new RegionGroup(id, name, defaultGroup, enabled, source, owner, ownerName,
                createdAt, updatedAt, entries);
        }
        catch (Exception e)
        {
            MasaToolMod.LOGGER.warn("Skipping invalid region group: {}", e.getMessage());
            return null;
        }
    }

    private static RegionEntry readEntry(JsonElement element)
    {
        if (!element.isJsonObject())
        {
            return null;
        }

        try
        {
            JsonObject obj = element.getAsJsonObject();
            UUID id = UUID.fromString(obj.get("id").getAsString());
            String name = obj.get("name").getAsString();
            String dimension = obj.get("dimension").getAsString();
            boolean enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();
            UUID owner = readUuid(obj, "owner");
            String ownerName = obj.has("owner_name") ? obj.get("owner_name").getAsString() : "";
            long createdAt = obj.has("created_at") ? obj.get("created_at").getAsLong() : 0L;
            Box box = readBox(obj.getAsJsonArray("box"));

            if (box == null || !box.isValid())
            {
                return null;
            }

            return new RegionEntry(id, name, dimension, box, enabled, owner, ownerName, createdAt);
        }
        catch (Exception e)
        {
            MasaToolMod.LOGGER.warn("Skipping invalid region entry: {}", e.getMessage());
            return null;
        }
    }

    private static UUID readUuid(JsonObject obj, String key)
    {
        if (!obj.has(key) || obj.get(key).isJsonNull())
        {
            return null;
        }

        String value = obj.get(key).getAsString();

        if (value.isEmpty())
        {
            return null;
        }

        try
        {
            return UUID.fromString(value);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    private static Box readBox(JsonArray array)
    {
        if (array == null || array.size() != 6)
        {
            return null;
        }

        return new Box(
            array.get(0).getAsInt(),
            array.get(1).getAsInt(),
            array.get(2).getAsInt(),
            array.get(3).getAsInt(),
            array.get(4).getAsInt(),
            array.get(5).getAsInt()
        );
    }
}
