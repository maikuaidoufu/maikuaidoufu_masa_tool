package cn.maikuaidoufu.net;

import cn.maikuaidoufu.permissions.Feature;
import cn.maikuaidoufu.server.ChunkLoadMonitor;
import cn.maikuaidoufu.server.FeaturePermissions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ChunkNetworking
{
    private ChunkNetworking()
    {
    }

    public static void registerPayloads()
    {
        PayloadTypeRegistry.serverboundPlay().register(ChunkWatchPayload.TYPE, ChunkWatchPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ChunkLevelEventPayload.TYPE, ChunkLevelEventPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ChunkWatchStatePayload.TYPE, ChunkWatchStatePayload.CODEC);
    }

    public static void registerServerHandlers()
    {
        ServerPlayNetworking.registerGlobalReceiver(ChunkWatchPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handle(context.player(), payload))
        );
    }

    public static void sendState(ServerPlayer player)
    {
        if (player != null && ServerPlayNetworking.canSend(player, ChunkWatchStatePayload.TYPE))
        {
            ServerPlayNetworking.send(player, ChunkLoadMonitor.statePayload(player));
        }
    }

    private static void handle(ServerPlayer player, ChunkWatchPayload payload)
    {
        // Turning the subscription off is always allowed.
        boolean needsPermission = payload.action() != ChunkWatchPayload.Action.TOGGLE || payload.enabled();

        if (needsPermission && !FeaturePermissions.allows(player, Feature.CHUNK_MONITOR))
        {
            if (player != null)
            {
                player.sendSystemMessage(
                    Component.translatable("maikuaidoufu_masa_tool.message.no_permission"));
            }

            sendState(player);
            return;
        }

        switch (payload.action())
        {
            case TOGGLE -> ChunkLoadMonitor.setSubscribed(player, payload.enabled());
            case ADD -> ChunkLoadMonitor.addWatch(player,
                payload.dimension(), payload.chunkX(), payload.chunkZ());
            case REMOVE -> ChunkLoadMonitor.removeWatch(player,
                payload.dimension(), payload.chunkX(), payload.chunkZ());
        }

        sendState(player);
    }
}
