package cn.maikuaidoufu.client;

import cn.maikuaidoufu.MasaToolMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Client-side persistence for the two log tabs. The rendered lines are stored
 * locally so re-opening the GUI (or restarting the game) keeps the history.
 *
 * <p>Writes are throttled: incoming events only mark the cache dirty and the
 * client tick flushes it at most once every few seconds. Disconnecting forces
 * an immediate flush.</p>
 */
public final class ClientLogStore
{
    private static final String FILE_NAME = MasaToolMod.MOD_ID + "-logcache.json";
    private static final long SAVE_INTERVAL_MS = 5000L;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static volatile boolean loaded;
    private static volatile boolean dirty;
    private static long lastSave;

    private ClientLogStore()
    {
    }

    private static Path file()
    {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void load()
    {
        loaded = true;
        Path file = file();

        if (!Files.isRegularFile(file))
        {
            lastSave = System.currentTimeMillis();
            return;
        }

        try
        {
            JsonObject root = JsonParser.parseString(
                Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            List<ClientPortalLog.Entry> portal = new ArrayList<>();
            List<ClientChunkLog.Entry> chunk = new ArrayList<>();

            if (root.has("portal") && root.get("portal").isJsonArray())
            {
                for (JsonElement element : root.getAsJsonArray("portal"))
                {
                    if (!element.isJsonObject())
                    {
                        continue;
                    }

                    JsonObject line = element.getAsJsonObject();

                    if (line.has("text") && line.has("kind"))
                    {
                        portal.add(new ClientPortalLog.Entry(
                            line.get("text").getAsString(), line.get("kind").getAsInt()));
                    }
                }
            }

            if (root.has("chunk") && root.get("chunk").isJsonArray())
            {
                for (JsonElement element : root.getAsJsonArray("chunk"))
                {
                    if (!element.isJsonObject())
                    {
                        continue;
                    }

                    JsonObject line = element.getAsJsonObject();

                    if (line.has("text") && line.has("direction") && line.has("gameTime"))
                    {
                        chunk.add(new ClientChunkLog.Entry(
                            line.get("text").getAsString(),
                            line.get("direction").getAsInt(),
                            line.get("gameTime").getAsLong()));
                    }
                }
            }

            ClientPortalLog.restore(portal);
            ClientChunkLog.restore(chunk);
            dirty = false;
            lastSave = System.currentTimeMillis();
            MasaToolMod.LOGGER.info("Loaded client log cache from {}", file);
        }
        catch (Exception e)
        {
            MasaToolMod.LOGGER.warn("Failed to load client log cache: {}", e.getMessage());
            lastSave = System.currentTimeMillis();
        }
    }

    public static void markDirty()
    {
        dirty = true;
    }

    /** Called once per client tick; cheap when nothing changed. */
    public static void tick()
    {
        if (dirty && System.currentTimeMillis() - lastSave >= SAVE_INTERVAL_MS)
        {
            save();
        }
    }

    public static void save()
    {
        // Never overwrite an existing cache before it has been read back.
        if (!loaded)
        {
            return;
        }

        JsonArray portal = new JsonArray();

        for (ClientPortalLog.Entry entry : ClientPortalLog.entries())
        {
            JsonObject line = new JsonObject();
            line.addProperty("text", entry.text());
            line.addProperty("kind", entry.kind());
            portal.add(line);
        }

        JsonArray chunk = new JsonArray();

        for (ClientChunkLog.Entry entry : ClientChunkLog.entries())
        {
            JsonObject line = new JsonObject();
            line.addProperty("text", entry.text());
            line.addProperty("direction", entry.direction());
            line.addProperty("gameTime", entry.gameTime());
            chunk.add(line);
        }

        JsonObject root = new JsonObject();
        root.add("portal", portal);
        root.add("chunk", chunk);

        try
        {
            Path file = file();
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
            dirty = false;
            lastSave = System.currentTimeMillis();
        }
        catch (IOException e)
        {
            MasaToolMod.LOGGER.warn("Failed to save client log cache: {}", e.getMessage());
        }
    }

    /** Applies a changed cache size immediately, including to the file on disk. */
    public static void applyCacheSize()
    {
        ClientPortalLog.trim();
        ClientChunkLog.trim();
        save();
    }
}
