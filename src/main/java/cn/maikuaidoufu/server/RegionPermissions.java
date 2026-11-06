package cn.maikuaidoufu.server;

import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import net.minecraft.server.level.ServerPlayer;

public final class RegionPermissions
{
    private RegionPermissions()
    {
    }

    public static boolean canEditGroup(ServerPlayer player, RegionGroup group)
    {
        if (player == null || group == null)
        {
            return false;
        }

        if (ServerConfig.isOp(player))
        {
            return true;
        }

        return ServerConfig.allowOwnerEdit()
            && !group.defaultGroup()
            && group.owner() != null
            && group.owner().equals(player.getUUID());
    }

    public static boolean canCreateEntry(ServerPlayer player, RegionGroup group)
    {
        if (group == null)
        {
            return false;
        }

        if (group.defaultGroup())
        {
            return ServerConfig.canCreate(player);
        }

        return canEditGroup(player, group);
    }

    public static boolean canEditEntry(ServerPlayer player, RegionGroup group, RegionEntry entry)
    {
        if (player == null || group == null || entry == null)
        {
            return false;
        }

        if (ServerConfig.isOp(player))
        {
            return true;
        }

        if (!ServerConfig.allowOwnerEdit())
        {
            return false;
        }

        if (entry.owner() != null && entry.owner().equals(player.getUUID()))
        {
            return true;
        }

        return !group.defaultGroup()
            && group.owner() != null
            && group.owner().equals(player.getUUID());
    }

    public static boolean canDeleteGroup(ServerPlayer player, RegionGroup group)
    {
        return group != null && !group.defaultGroup() && canEditGroup(player, group);
    }

    public static boolean canClearGroup(ServerPlayer player, RegionGroup group)
    {
        if (group == null)
        {
            return false;
        }

        if (group.defaultGroup())
        {
            return ServerConfig.isOp(player);
        }

        return canEditGroup(player, group);
    }
}
