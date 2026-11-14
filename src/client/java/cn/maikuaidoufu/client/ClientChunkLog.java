package cn.maikuaidoufu.client;

import cn.maikuaidoufu.net.ChunkLevelEvent;
import cn.maikuaidoufu.net.ChunkWatchStatePayload;
import java.util.ArrayList;
import java.util.List;

/**
 * Local copy of the chunk watch state and the received load-level changes.
 */
public final class ClientChunkLog
{
    public record Entry(String text, int direction, long gameTime)
    {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private static List<ChunkWatchStatePayload.Entry> watches = List.of();
    private static boolean enabled;
    private static long revision;

    private ClientChunkLog()
    {
    }

    public static void add(ChunkLevelEvent event)
    {
        if (event == null)
        {
            return;
        }

        String text = ChunkLogFormatter.format(event);

        if (text.isBlank())
        {
            return;
        }

        ENTRIES.add(new Entry(text, ChunkLogFormatter.direction(event), event.gameTime()));
        trimToLimit();
        revision++;
        ClientLogStore.markDirty();
    }

    public static void applyState(ChunkWatchStatePayload payload)
    {
        if (payload == null)
        {
            return;
        }

        enabled = payload.enabled();
        watches = List.copyOf(payload.watches());
        revision++;
    }

    /**
     * Optimistic local update so the watch buttons refresh before the server
     * echoes the authoritative state back.
     */
    public static void addWatchLocal(String dimension, int x, int z)
    {
        ChunkWatchStatePayload.Entry entry =
            new ChunkWatchStatePayload.Entry(dimension, x, z);
        List<ChunkWatchStatePayload.Entry> next = new ArrayList<>(watches);

        if (!next.contains(entry) && next.size() < ChunkWatchStatePayload.MAX_WATCHES)
        {
            next.add(entry);
            watches = List.copyOf(next);
            revision++;
        }
    }

    public static void removeWatchLocal(String dimension, int x, int z)
    {
        List<ChunkWatchStatePayload.Entry> next = new ArrayList<>(watches);

        if (next.remove(new ChunkWatchStatePayload.Entry(dimension, x, z)))
        {
            watches = List.copyOf(next);
            revision++;
        }
    }

    public static void clear()
    {
        if (!ENTRIES.isEmpty())
        {
            ENTRIES.clear();
            revision++;
            // Persist immediately so a manual clear survives a restart.
            ClientLogStore.save();
        }
    }

    /**
     * Connection-scoped state is reset on join/disconnect, but the log history
     * itself is client-local and deliberately kept.
     */
    public static void resetConnection()
    {
        watches = List.of();
        enabled = false;
        revision++;
    }

    /** Replaces the in-memory log with a previously saved snapshot. */
    public static void restore(List<Entry> entries)
    {
        ENTRIES.clear();

        if (entries != null)
        {
            ENTRIES.addAll(entries);
        }

        trimToLimit();
        revision++;
    }

    /** Re-applies the configured cache size after the user changes it. */
    public static void trim()
    {
        int before = ENTRIES.size();
        trimToLimit();

        if (ENTRIES.size() != before)
        {
            revision++;
        }
    }

    private static void trimToLimit()
    {
        int limit = Configs.LOG_CACHE_SIZE.getIntegerValue();
        int excess = ENTRIES.size() - limit;

        if (excess > 0)
        {
            ENTRIES.subList(0, excess).clear();
        }
    }

    public static List<Entry> entries()
    {
        return List.copyOf(ENTRIES);
    }

    public static List<ChunkWatchStatePayload.Entry> watches()
    {
        return watches;
    }

    public static boolean isEnabled()
    {
        return enabled;
    }

    public static void setEnabled(boolean value)
    {
        enabled = value;
        revision++;
    }

    public static long revision()
    {
        return revision;
    }

    public static boolean isEmpty()
    {
        return ENTRIES.isEmpty();
    }
}
