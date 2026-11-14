package cn.maikuaidoufu.server;

import cn.maikuaidoufu.net.ChunkNetworking;
import cn.maikuaidoufu.net.RegionNetworking;
import cn.maikuaidoufu.permissions.Feature;
import cn.maikuaidoufu.portal.PortalLog;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /masatool reload} re-reads the permissions config without restarting
 * the server and pushes the new permission mask to every connected client.
 */
public final class PermissionCommands
{
    private PermissionCommands()
    {
    }

    public static void register()
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
            dispatcher.register(Commands.literal("masatool")
                .then(Commands.literal("reload")
                    .executes(context -> reload(context.getSource())))));
    }

    private static int reload(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();

        if (player != null && !FeaturePermissions.allows(player, Feature.RELOAD_CONFIG))
        {
            source.sendFailure(
                Component.translatable("maikuaidoufu_masa_tool.message.no_permission"));
            return 0;
        }

        FeaturePermissions.load();
        MinecraftServer server = source.getServer();

        for (ServerPlayer online : server.getPlayerList().getPlayers())
        {
            // Revoke live subscriptions when the permission was taken away.
            if (!FeaturePermissions.allows(online, Feature.PORTAL_LOG))
            {
                PortalLog.unsubscribe(online);
            }

            if (!FeaturePermissions.allows(online, Feature.CHUNK_MONITOR))
            {
                ChunkLoadMonitor.setSubscribed(online, false);
            }

            RegionNetworking.sendSnapshot(online);
            ChunkNetworking.sendState(online);
        }

        source.sendSuccess(() ->
            Component.translatable("maikuaidoufu_masa_tool.message.config_reloaded"), false);
        return 1;
    }
}
