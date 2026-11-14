package cn.maikuaidoufu.client;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.client.gui.VerifierGuiHook;
import cn.maikuaidoufu.net.ChunkLevelEventPayload;
import cn.maikuaidoufu.net.ChunkWatchStatePayload;
import cn.maikuaidoufu.net.GroupListPayload;
import cn.maikuaidoufu.net.PortalEventPayload;
import fi.dy.masa.malilib.event.RenderEventHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

public final class MasaToolClientImpl
{
    private static final int SNAPSHOT_RETRY_DELAY_TICKS = 20;
    private static final int SNAPSHOT_MAX_RETRIES = 10;

    private static ClientPacketListener activeHandler;
    private static int retriesLeft;
    private static int retryTicks;

    public void init()
    {
        Configs.init();
        ClientHighlightStore.load();
        ClientLogStore.load();

        ClientPlayNetworking.registerGlobalReceiver(GroupListPayload.TYPE, (payload, context) ->
            context.client().execute(() -> handleSnapshot(context.client(), payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(PortalEventPayload.TYPE, (payload, context) ->
            context.client().execute(() -> ClientPortalLog.add(payload.event()))
        );
        ClientPlayNetworking.registerGlobalReceiver(ChunkLevelEventPayload.TYPE, (payload, context) ->
            context.client().execute(() -> ClientChunkLog.add(payload.event()))
        );
        ClientPlayNetworking.registerGlobalReceiver(ChunkWatchStatePayload.TYPE, (payload, context) ->
            context.client().execute(() -> ClientChunkLog.applyState(payload))
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
        {
            activeHandler = handler;
            ClientGroupStore.clear();
            // Log history is client-local: only the per-connection subscription
            // state is reset, so re-opening the GUI keeps earlier lines.
            ClientPortalLog.setEnabled(false);
            ClientChunkLog.resetConnection();
            retriesLeft = SNAPSHOT_MAX_RETRIES;
            retryTicks = 1;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
        {
            // A late disconnect for an old connection must not wipe the new server's data.
            if (handler != activeHandler)
            {
                return;
            }

            activeHandler = null;
            retriesLeft = 0;
            retryTicks = 0;
            ClientLogStore.save();
            ClientGroupStore.clear();
            ClientPortalLog.setEnabled(false);
            ClientChunkLog.resetConnection();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            VerifierGuiHook.onClientTick(client);
            ClientLogStore.tick();

            if (retriesLeft <= 0 || ClientGroupStore.isConnected())
            {
                return;
            }

            if (retryTicks > 0)
            {
                retryTicks--;
                return;
            }

            retriesLeft--;
            retryTicks = SNAPSHOT_RETRY_DELAY_TICKS;
            ClientActions.requestSnapshot();
        });

        RenderEventHandler.getInstance().registerWorldLastRenderer(new RegionHighlightRenderer());

        MasaToolMod.LOGGER.info("Doufu's Masa Tool client initialized");
    }

    private static void handleSnapshot(Minecraft mc, GroupListPayload payload)
    {
        if (payload.protocolVersion() != cn.maikuaidoufu.region.RegionLimits.PROTOCOL_VERSION)
        {
            retriesLeft = 0;
            ClientGroupStore.clear();

            if (mc.player != null)
            {
                mc.player.sendSystemMessage(
                    Component.translatable("maikuaidoufu_masa_tool.message.protocol_mismatch"));
            }

            return;
        }

        ClientGroupStore.apply(payload);
        retriesLeft = 0;

        if (mc.screen instanceof GroupStoreListener listener)
        {
            listener.onGroupsChanged();
        }
    }
}
