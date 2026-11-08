package cn.maikuaidoufu;

import cn.maikuaidoufu.net.RegionNetworking;
import cn.maikuaidoufu.portal.PortalLog;
import cn.maikuaidoufu.server.RegionManager;
import cn.maikuaidoufu.server.RegionStore;
import cn.maikuaidoufu.server.ServerConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MasaToolMod implements ModInitializer
{
    public static final String MOD_ID = "maikuaidoufu_masa_tool";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize()
    {
        RegionNetworking.registerPayloads();
        RegionNetworking.registerServerHandlers();

        ServerLifecycleEvents.SERVER_STARTED.register(server ->
        {
            ServerConfig.load();
            RegionStore.load(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> RegionStore.save());
        ServerLifecycleEvents.SERVER_STOPPED.register(server ->
        {
            RegionManager.clear();
            PortalLog.reset();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            RegionNetworking.sendSnapshot(handler.player)
        );

        LOGGER.info("Doufu's Masa Tool common/server initialized");
    }
}
