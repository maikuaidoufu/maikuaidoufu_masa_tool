package cn.maikuaidoufu.server;

import cn.maikuaidoufu.net.ChunkLevelEvent;
import cn.maikuaidoufu.net.ChunkLevelEventPayload;
import cn.maikuaidoufu.net.ChunkWatchStatePayload;
import cn.maikuaidoufu.region.RegionLimits;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side watch list for chunk loading-level changes.
 *
 * <p>Only chunks explicitly watched by a subscribed player are reported, so the
 * mixin can bail out before touching the ticket storage in the common case.</p>
 */
public final class ChunkLoadMonitor
{
    private static final Map<UUID, WatchState> STATES = new HashMap<>();
    private static volatile boolean anySubscribed;

    private ChunkLoadMonitor()
    {
    }

    private record ChunkKey(String dimension, int x, int z)
    {
    }

    private static final class WatchState
    {
        private boolean enabled;
        private final Set<ChunkKey> watches = new LinkedHashSet<>();
    }

    public static boolean hasWatchers()
    {
        return anySubscribed;
    }

    /**
     * Fast check used from the chunk scheduling hot path.
     */
    public static boolean isWatched(String dimension, int x, int z)
    {
        if (!anySubscribed)
        {
            return false;
        }

        ChunkKey key = new ChunkKey(dimension, x, z);

        synchronized (STATES)
        {
            for (WatchState state : STATES.values())
            {
                if (state.enabled && state.watches.contains(key))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public static void onLevelChange(ServerLevel level, String dimension, int x, int z,
                                      int oldLevel, int newLevel, List<String> tickets)
    {
        if (!anySubscribed || oldLevel == newLevel)
        {
            return;
        }

        FullChunkStatus oldStatus = ChunkLevel.fullStatus(oldLevel);
        FullChunkStatus newStatus = ChunkLevel.fullStatus(newLevel);

        // Only report transitions between loading tiers, not level jitter inside one tier.
        if (oldStatus == newStatus)
        {
            return;
        }

        MinecraftServer server = level.getServer();

        if (server == null)
        {
            return;
        }

        ChunkKey key = new ChunkKey(dimension, x, z);
        ChunkLevelEvent event = new ChunkLevelEvent(dimension, x, z, oldLevel, newLevel,
            oldStatus.name(), newStatus.name(), tickets, level.getGameTime());
        ChunkLevelEventPayload payload = new ChunkLevelEventPayload(event);

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (isWatching(player.getUUID(), key)
                && ServerPlayNetworking.canSend(player, ChunkLevelEventPayload.TYPE))
            {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void setSubscribed(ServerPlayer player, boolean enabled)
    {
        if (player == null)
        {
            return;
        }

        synchronized (STATES)
        {
            state(player).enabled = enabled;
            refreshFlag();
        }
    }

    public static void addWatch(ServerPlayer player, String dimension, int x, int z)
    {
        if (player == null || !validDimension(dimension))
        {
            return;
        }

        synchronized (STATES)
        {
            WatchState state = state(player);
            state.enabled = true;

            if (state.watches.size() < ChunkWatchStatePayload.MAX_WATCHES)
            {
                state.watches.add(new ChunkKey(dimension, x, z));
            }

            refreshFlag();
        }
    }

    public static void removeWatch(ServerPlayer player, String dimension, int x, int z)
    {
        if (player == null)
        {
            return;
        }

        synchronized (STATES)
        {
            state(player).watches.remove(new ChunkKey(dimension, x, z));
            refreshFlag();
        }
    }

    public static void unsubscribe(ServerPlayer player)
    {
        if (player == null)
        {
            return;
        }

        synchronized (STATES)
        {
            STATES.remove(player.getUUID());
            refreshFlag();
        }
    }

    public static void reset()
    {
        synchronized (STATES)
        {
            STATES.clear();
            anySubscribed = false;
        }
    }

    public static ChunkWatchStatePayload statePayload(ServerPlayer player)
    {
        if (player == null)
        {
            return new ChunkWatchStatePayload(false, List.of());
        }

        synchronized (STATES)
        {
            WatchState state = STATES.get(player.getUUID());

            if (state == null)
            {
                return new ChunkWatchStatePayload(false, List.of());
            }

            List<ChunkWatchStatePayload.Entry> entries = new ArrayList<>(state.watches.size());

            for (ChunkKey key : state.watches)
            {
                entries.add(new ChunkWatchStatePayload.Entry(key.dimension(), key.x(), key.z()));
            }

            return new ChunkWatchStatePayload(state.enabled, entries);
        }
    }

    private static boolean isWatching(UUID playerId, ChunkKey key)
    {
        synchronized (STATES)
        {
            WatchState state = STATES.get(playerId);
            return state != null && state.enabled && state.watches.contains(key);
        }
    }

    private static WatchState state(ServerPlayer player)
    {
        return STATES.computeIfAbsent(player.getUUID(), id -> new WatchState());
    }

    private static void refreshFlag()
    {
        boolean active = false;

        for (WatchState state : STATES.values())
        {
            if (state.enabled && !state.watches.isEmpty())
            {
                active = true;
                break;
            }
        }

        anySubscribed = active;
    }

    private static boolean validDimension(String dimension)
    {
        return RegionLimits.validDimension(dimension) && Identifier.tryParse(dimension) != null;
    }
}
