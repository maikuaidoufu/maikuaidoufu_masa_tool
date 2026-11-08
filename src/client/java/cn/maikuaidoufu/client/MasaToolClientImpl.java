package cn.maikuaidoufu.client;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.net.GroupListPayload;
import cn.maikuaidoufu.net.PortalEventPayload;
import fi.dy.masa.malilib.event.RenderEventHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class MasaToolClientImpl
{
    public void init()
    {
        Configs.init();
        ClientHighlightStore.load();

        ClientPlayNetworking.registerGlobalReceiver(GroupListPayload.TYPE, (payload, context) ->
            context.client().execute(() -> handleSnapshot(context.client(), payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(PortalEventPayload.TYPE, (payload, context) ->
            context.client().execute(() -> ClientPortalLog.add(payload.text(), payload.kind()))
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
        {
            ClientGroupStore.clear();
            ClientPortalLog.clear();
            ClientPortalLog.setEnabled(false);
            ClientActions.requestSnapshot();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
        {
            ClientGroupStore.clear();
            ClientPortalLog.clear();
            ClientPortalLog.setEnabled(false);
        });

        RenderEventHandler.getInstance().registerWorldLastRenderer(new RegionHighlightRenderer());

        MasaToolMod.LOGGER.info("Doufu's Masa Tool client initialized");
    }

    private static void handleSnapshot(Minecraft mc, GroupListPayload payload)
    {
        if (payload.protocolVersion() != cn.maikuaidoufu.region.RegionLimits.PROTOCOL_VERSION)
        {
            ClientGroupStore.clear();

            if (mc.player != null)
            {
                mc.player.sendSystemMessage(
                    Component.translatable("maikuaidoufu_masa_tool.message.protocol_mismatch"));
            }

            return;
        }

        ClientGroupStore.apply(payload);

        if (mc.screen instanceof GroupStoreListener listener)
        {
            listener.onGroupsChanged();
        }
    }
}
