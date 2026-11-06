package cn.maikuaidoufu.client;

import cn.maikuaidoufu.net.GroupListPayload;
import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class ClientGroupStore
{
    private static volatile List<RegionGroup> groups = List.of();
    private static volatile boolean connected;
    private static volatile boolean canCreate;
    private static volatile boolean canEditAll;
    private static volatile boolean allowOwnerEdit;
    private static volatile int protocolVersion = -1;

    private ClientGroupStore()
    {
    }

    public static void apply(GroupListPayload payload)
    {
        protocolVersion = payload.protocolVersion();
        canCreate = payload.canCreate();
        canEditAll = payload.canEditAll();
        allowOwnerEdit = payload.allowOwnerEdit();
        groups = payload.groups();
        connected = true;
    }

    public static void clear()
    {
        groups = List.of();
        connected = false;
        canCreate = false;
        canEditAll = false;
        allowOwnerEdit = false;
        protocolVersion = -1;
    }

    public static boolean isConnected()
    {
        return connected;
    }

    public static int protocolVersion()
    {
        return protocolVersion;
    }

    public static boolean canCreate()
    {
        return connected && canCreate;
    }

    public static boolean canEditAll()
    {
        return connected && canEditAll;
    }

    public static boolean allowOwnerEdit()
    {
        return allowOwnerEdit;
    }

    public static List<RegionGroup> groups()
    {
        return groups;
    }

    public static RegionGroup group(UUID groupId)
    {
        for (RegionGroup group : groups)
        {
            if (group.id().equals(groupId))
            {
                return group;
            }
        }

        return null;
    }

    public static boolean canEditGroup(RegionGroup group)
    {
        if (!connected || group == null)
        {
            return false;
        }

        if (canEditAll)
        {
            return true;
        }

        UUID playerId = playerId();
        return allowOwnerEdit
            && !group.defaultGroup()
            && group.owner() != null
            && group.owner().equals(playerId);
    }

    public static boolean canCreateEntry(RegionGroup group)
    {
        if (group == null)
        {
            return false;
        }

        return group.defaultGroup() ? canCreate() : canEditGroup(group);
    }

    public static boolean canEditEntry(RegionGroup group, RegionEntry entry)
    {
        if (!connected || group == null || entry == null)
        {
            return false;
        }

        if (canEditAll)
        {
            return true;
        }

        UUID playerId = playerId();

        if (entry.owner() != null && entry.owner().equals(playerId))
        {
            return allowOwnerEdit;
        }

        return allowOwnerEdit
            && !group.defaultGroup()
            && group.owner() != null
            && group.owner().equals(playerId);
    }

    public static boolean canDeleteGroup(RegionGroup group)
    {
        return group != null && !group.defaultGroup() && canEditGroup(group);
    }

    public static boolean canClearGroup(RegionGroup group)
    {
        if (group == null)
        {
            return false;
        }

        return group.defaultGroup() ? canEditAll() : canEditGroup(group);
    }

    public static UUID playerId()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : player.getUUID();
    }
}
