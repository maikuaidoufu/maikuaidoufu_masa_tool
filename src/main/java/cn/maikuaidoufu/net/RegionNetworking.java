package cn.maikuaidoufu.net;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.region.Box;
import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import cn.maikuaidoufu.region.RegionLimits;
import cn.maikuaidoufu.server.RegionManager;
import cn.maikuaidoufu.server.RegionPermissions;
import cn.maikuaidoufu.server.RegionStore;
import cn.maikuaidoufu.server.ServerConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class RegionNetworking
{
    private RegionNetworking()
    {
    }

    public static void registerPayloads()
    {
        PayloadTypeRegistry.serverboundPlay().registerLarge(CreateGroupPayload.TYPE, CreateGroupPayload.CODEC, RegionLimits.MAX_PAYLOAD_SIZE);
        PayloadTypeRegistry.serverboundPlay().registerLarge(CreateEntryPayload.TYPE, CreateEntryPayload.CODEC, RegionLimits.MAX_PAYLOAD_SIZE);
        PayloadTypeRegistry.serverboundPlay().registerLarge(GroupControlPayload.TYPE, GroupControlPayload.CODEC, RegionLimits.MAX_PAYLOAD_SIZE);
        PayloadTypeRegistry.serverboundPlay().registerLarge(EntryControlPayload.TYPE, EntryControlPayload.CODEC, RegionLimits.MAX_PAYLOAD_SIZE);
        PayloadTypeRegistry.serverboundPlay().register(RequestSnapshotPayload.TYPE, RequestSnapshotPayload.CODEC);

        PayloadTypeRegistry.clientboundPlay().registerLarge(GroupListPayload.TYPE, GroupListPayload.CODEC, RegionLimits.MAX_PAYLOAD_SIZE);
    }

    public static void registerServerHandlers()
    {
        ServerPlayNetworking.registerGlobalReceiver(CreateGroupPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleCreateGroup(context.player(), context.server(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(CreateEntryPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleCreateEntry(context.player(), context.server(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(GroupControlPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleGroupControl(context.player(), context.server(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(EntryControlPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleEntryControl(context.player(), context.server(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(RequestSnapshotPayload.TYPE, (payload, context) ->
            context.server().execute(() -> sendSnapshot(context.player()))
        );
    }

    public static void sendSnapshot(ServerPlayer player)
    {
        if (player == null || !ServerPlayNetworking.canSend(player, GroupListPayload.TYPE))
        {
            return;
        }

        GroupListPayload payload = new GroupListPayload(
            RegionLimits.PROTOCOL_VERSION,
            ServerConfig.canCreate(player),
            ServerConfig.isOp(player),
            ServerConfig.allowOwnerEdit(),
            RegionManager.getGroups()
        );

        ServerPlayNetworking.send(player, payload);
    }

    public static void broadcast(MinecraftServer server)
    {
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            sendSnapshot(player);
        }
    }

    private static void handleCreateGroup(ServerPlayer player, MinecraftServer server, CreateGroupPayload payload)
    {
        if (!ServerConfig.canCreate(player))
        {
            message(player, "maikuaidoufu_masa_tool.message.no_permission");
            return;
        }

        if (payload.entries().isEmpty()
            || payload.entries().size() > ServerConfig.maxEntriesPerGroup()
            || RegionManager.totalEntryCount() + payload.entries().size() > ServerConfig.maxTotalEntries())
        {
            message(player, "maikuaidoufu_masa_tool.message.entry_limit");
            return;
        }

        if (RegionManager.getGroups().size() >= ServerConfig.maxGroups())
        {
            message(player, "maikuaidoufu_masa_tool.message.group_limit");
            return;
        }

        for (EntryDraft draft : payload.entries())
        {
            if (!validDraft(server, draft))
            {
                message(player, "maikuaidoufu_masa_tool.message.invalid_region");
                return;
            }
        }

        RegionGroup group = RegionManager.createGroup(
            payload.name(),
            payload.source(),
            payload.entries(),
            player,
            System.currentTimeMillis()
        );

        RegionStore.save();
        broadcast(server);
        message(player, "maikuaidoufu_masa_tool.message.group_created", group.name());
    }

    private static void handleCreateEntry(ServerPlayer player, MinecraftServer server, CreateEntryPayload payload)
    {
        RegionGroup group = RegionManager.getGroup(payload.groupId());

        if (group == null)
        {
            message(player, "maikuaidoufu_masa_tool.message.group_missing");
            return;
        }

        if (!RegionPermissions.canCreateEntry(player, group))
        {
            message(player, "maikuaidoufu_masa_tool.message.no_permission");
            return;
        }

        if (group.entries().size() >= ServerConfig.maxEntriesPerGroup()
            || RegionManager.totalEntryCount() >= ServerConfig.maxTotalEntries())
        {
            message(player, "maikuaidoufu_masa_tool.message.entry_limit");
            return;
        }

        if (!validDimension(server, payload.dimension())
            || !payload.box().isValid()
            || !RegionLimits.validName(payload.name()))
        {
            message(player, "maikuaidoufu_masa_tool.message.invalid_region");
            return;
        }

        RegionGroup next = RegionManager.createEntry(
            payload.groupId(),
            payload.name(),
            payload.dimension(),
            payload.box(),
            payload.enabled(),
            player,
            System.currentTimeMillis()
        );

        if (next == null)
        {
            message(player, "maikuaidoufu_masa_tool.message.group_missing");
            return;
        }

        RegionStore.save();
        broadcast(server);
        message(player, "maikuaidoufu_masa_tool.message.entry_created", payload.name());
    }

    private static void handleGroupControl(ServerPlayer player, MinecraftServer server, GroupControlPayload payload)
    {
        RegionGroup group = RegionManager.getGroup(payload.groupId());

        if (group == null)
        {
            message(player, "maikuaidoufu_masa_tool.message.group_missing");
            return;
        }

        if (!RegionPermissions.canEditGroup(player, group))
        {
            message(player, "maikuaidoufu_masa_tool.message.no_permission");
            return;
        }

        long now = System.currentTimeMillis();
        boolean changed = false;

        switch (payload.action())
        {
            case SET_ENABLED ->
            {
                RegionManager.setGroupEnabled(group.id(), payload.enabled(), now);
                changed = true;
            }
            case RENAME ->
            {
                if (!group.defaultGroup() && RegionLimits.validName(payload.name()))
                {
                    RegionManager.renameGroup(group.id(), payload.name(), now);
                    changed = true;
                }
                else
                {
                    message(player, "maikuaidoufu_masa_tool.message.invalid_name");
                }
            }
            case DELETE ->
            {
                if (!RegionPermissions.canDeleteGroup(player, group))
                {
                    message(player, "maikuaidoufu_masa_tool.message.no_permission");
                    return;
                }

                changed = RegionManager.deleteGroup(group.id());
            }
            case CLEAR ->
            {
                if (!RegionPermissions.canClearGroup(player, group))
                {
                    message(player, "maikuaidoufu_masa_tool.message.no_permission");
                    return;
                }

                RegionManager.clearGroup(group.id(), now);
                changed = true;
            }
        }

        if (changed)
        {
            RegionStore.save();
            broadcast(server);

            if (payload.action() == GroupControlPayload.Action.DELETE)
            {
                message(player, "maikuaidoufu_masa_tool.message.group_deleted");
            }
        }
    }

    private static void handleEntryControl(ServerPlayer player, MinecraftServer server, EntryControlPayload payload)
    {
        RegionGroup group = RegionManager.getGroup(payload.groupId());

        if (group == null)
        {
            message(player, "maikuaidoufu_masa_tool.message.group_missing");
            return;
        }

        RegionEntry entry = group.entry(payload.entryId());

        if (entry == null)
        {
            message(player, "maikuaidoufu_masa_tool.message.entry_missing");
            return;
        }

        if (!RegionPermissions.canEditEntry(player, group, entry))
        {
            message(player, "maikuaidoufu_masa_tool.message.no_permission");
            return;
        }

        long now = System.currentTimeMillis();

        if (payload.action() == EntryControlPayload.Action.SET_ENABLED)
        {
            RegionManager.setEntryEnabled(group.id(), entry.id(), payload.enabled(), now);
        }
        else
        {
            RegionManager.deleteEntry(group.id(), entry.id(), now);
        }

        RegionStore.save();
        broadcast(server);
    }

    private static boolean validDraft(MinecraftServer server, EntryDraft draft)
    {
        return draft != null
            && RegionLimits.validName(draft.name())
            && validDimension(server, draft.dimension())
            && draft.box().isValid();
    }

    private static boolean validDimension(MinecraftServer server, String dimension)
    {
        if (!RegionLimits.validDimension(dimension))
        {
            return false;
        }

        Identifier id = Identifier.tryParse(dimension);

        if (id == null)
        {
            return false;
        }

        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        return server.getLevel(key) != null;
    }

    private static void message(ServerPlayer player, String key, Object... args)
    {
        if (player != null)
        {
            player.sendSystemMessage(Component.translatable(key, args));
        }
    }
}
