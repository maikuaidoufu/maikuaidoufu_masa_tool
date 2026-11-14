package cn.maikuaidoufu.client;

import cn.maikuaidoufu.net.GroupListPayload;
import cn.maikuaidoufu.permissions.Feature;
import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Client mirror of the region list and the effective permission mask. These
 * checks only drive the GUI; the server re-validates every action.
 */
public final class ClientGroupStore
{
    private static volatile List<RegionGroup> groups = List.of();
    private static volatile boolean connected;
    private static volatile int permissions;
    private static volatile int protocolVersion = -1;

    private ClientGroupStore()
    {
    }

    public static void apply(GroupListPayload payload)
    {
        protocolVersion = payload.protocolVersion();
        permissions = payload.permissions();
        groups = payload.groups();
        connected = true;
    }

    public static void clear()
    {
        groups = List.of();
        connected = false;
        permissions = 0;
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

    /** Whether the server currently grants this player the given feature. */
    public static boolean allows(Feature feature)
    {
        return connected && feature != null && (permissions & feature.bit()) != 0;
    }

    public static boolean ownerBypass()
    {
        return connected && (permissions & Feature.OWNER_BYPASS_BIT) != 0;
    }

    public static boolean canCreate()
    {
        return allows(Feature.CREATE_GROUP);
    }

    public static boolean canEditAll()
    {
        return allows(Feature.EDIT_GROUP);
    }

    public static boolean allowOwnerEdit()
    {
        return ownerBypass();
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

        return allows(Feature.EDIT_GROUP) || ownsGroup(group);
    }

    public static boolean canCreateEntry(RegionGroup group)
    {
        if (group == null)
        {
            return false;
        }

        if (group.defaultGroup())
        {
            return allows(Feature.ADD_DEFAULT_REGION);
        }

        return allows(Feature.ADD_REGION) || ownsGroup(group);
    }

    public static boolean canEditEntry(RegionGroup group, RegionEntry entry)
    {
        if (!connected || group == null || entry == null)
        {
            return false;
        }

        if (allows(Feature.EDIT_REGION))
        {
            return true;
        }

        if (!ownerBypass())
        {
            return false;
        }

        UUID playerId = playerId();

        if (entry.owner() != null && entry.owner().equals(playerId))
        {
            return true;
        }

        return ownsGroup(group);
    }

    public static boolean canDeleteGroup(RegionGroup group)
    {
        return group != null && !group.defaultGroup()
            && (allows(Feature.DELETE_GROUP) || ownsGroup(group));
    }

    public static boolean canClearGroup(RegionGroup group)
    {
        if (group == null)
        {
            return false;
        }

        if (group.defaultGroup())
        {
            return allows(Feature.CLEAR_GROUP);
        }

        return allows(Feature.CLEAR_GROUP) || ownsGroup(group);
    }

    private static boolean ownsGroup(RegionGroup group)
    {
        if (!ownerBypass() || group.defaultGroup() || group.owner() == null)
        {
            return false;
        }

        UUID playerId = playerId();
        return playerId != null && group.owner().equals(playerId);
    }

    public static UUID playerId()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : player.getUUID();
    }
}
