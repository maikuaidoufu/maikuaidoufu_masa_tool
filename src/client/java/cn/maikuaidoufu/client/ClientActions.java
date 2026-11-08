package cn.maikuaidoufu.client;

import cn.maikuaidoufu.net.CreateEntryPayload;
import cn.maikuaidoufu.net.CreateGroupPayload;
import cn.maikuaidoufu.net.EntryControlPayload;
import cn.maikuaidoufu.net.EntryDraft;
import cn.maikuaidoufu.net.GroupControlPayload;
import cn.maikuaidoufu.net.PortalTogglePayload;
import cn.maikuaidoufu.net.RequestSnapshotPayload;
import cn.maikuaidoufu.region.Box;
import cn.maikuaidoufu.region.RegionGroup;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClientActions
{
    private ClientActions()
    {
    }

    public static void requestSnapshot()
    {
        if (ClientPlayNetworking.canSend(RequestSnapshotPayload.TYPE))
        {
            ClientPlayNetworking.send(new RequestSnapshotPayload());
        }
    }

    public static void createGroup(String name, String source, List<EntryDraft> entries)
    {
        if (!canSend("maikuaidoufu_masa_tool.message.server_unavailable"))
        {
            return;
        }

        ClientPlayNetworking.send(new CreateGroupPayload(name, source, entries));
    }

    public static void createEntry(UUID groupId, String name, String dimension, Box box, boolean enabled)
    {
        if (!canSend("maikuaidoufu_masa_tool.message.server_unavailable"))
        {
            return;
        }

        ClientPlayNetworking.send(new CreateEntryPayload(groupId, name, dimension, box, enabled));
    }

    public static void setGroupEnabled(UUID groupId, boolean enabled)
    {
        if (canSend("maikuaidoufu_masa_tool.message.server_unavailable"))
        {
            ClientPlayNetworking.send(new GroupControlPayload(
                groupId, GroupControlPayload.Action.SET_ENABLED, "", enabled));
        }
    }

    public static void renameGroup(UUID groupId, String name)
    {
        if (canSend("maikuaidoufu_masa_tool.message.server_unavailable"))
        {
            ClientPlayNetworking.send(new GroupControlPayload(
                groupId, GroupControlPayload.Action.RENAME, name, false));
        }
    }

    public static void deleteGroup(UUID groupId)
    {
        if (canSend("maikuaidoufu_masa_tool.message.server_unavailable"))
        {
            ClientPlayNetworking.send(new GroupControlPayload(
                groupId, GroupControlPayload.Action.DELETE, "", false));
        }
    }

    public static void clearGroup(UUID groupId)
    {
        if (canSend("maikuaidoufu_masa_tool.message.server_unavailable"))
        {
            ClientPlayNetworking.send(new GroupControlPayload(
                groupId, GroupControlPayload.Action.CLEAR, "", false));
        }
    }

    public static void setEntryEnabled(UUID groupId, UUID entryId, boolean enabled)
    {
        if (canSend("maikuaidoufu_masa_tool.message.server_unavailable"))
        {
            ClientPlayNetworking.send(new EntryControlPayload(
                groupId, entryId, EntryControlPayload.Action.SET_ENABLED, enabled));
        }
    }

    public static void deleteEntry(UUID groupId, UUID entryId)
    {
        if (canSend("maikuaidoufu_masa_tool.message.server_unavailable"))
        {
            ClientPlayNetworking.send(new EntryControlPayload(
                groupId, entryId, EntryControlPayload.Action.DELETE, false));
        }
    }

    public static void setPortalLogEnabled(boolean enabled)
    {
        if (ClientPlayNetworking.canSend(PortalTogglePayload.TYPE))
        {
            ClientPlayNetworking.send(new PortalTogglePayload(enabled));
        }
    }

    public static String uniqueGroupName(String base)
    {
        String name = base == null || base.isBlank()
            ? StringUtils.translate("maikuaidoufu_masa_tool.name.imported")
            : base.trim();

        if (!nameExists(name))
        {
            return name;
        }

        for (int i = 2; i < 1000; i++)
        {
            String candidate = name + " (" + i + ")";

            if (!nameExists(candidate))
            {
                return candidate;
            }
        }

        return name;
    }

    private static boolean nameExists(String name)
    {
        for (RegionGroup group : ClientGroupStore.groups())
        {
            if (group.name().equalsIgnoreCase(name))
            {
                return true;
            }
        }

        return false;
    }

    private static boolean canSend(String messageKey)
    {
        if (ClientPlayNetworking.canSend(CreateGroupPayload.TYPE))
        {
            return true;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null)
        {
            mc.player.sendSystemMessage(Component.translatable(messageKey));
        }

        return false;
    }
}
