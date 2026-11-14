package cn.maikuaidoufu.server;

import cn.maikuaidoufu.permissions.Feature;
import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Region-specific permission checks. Every operation is gated by the matching
 * {@link Feature} from the permissions config; {@code ownerBypass} optionally
 * lets non-OP players manage objects they created themselves.
 */
public final class RegionPermissions
{
    private RegionPermissions()
    {
    }

    public static boolean canManageGroup(ServerPlayer player, RegionGroup group, Feature feature)
    {
        if (player == null || group == null)
        {
            return false;
        }

        if (FeaturePermissions.allows(player, feature))
        {
            return true;
        }

        return FeaturePermissions.ownerBypass() && ownsGroup(player, group);
    }

    public static boolean canEditGroup(ServerPlayer player, RegionGroup group)
    {
        return canManageGroup(player, group, Feature.EDIT_GROUP);
    }

    public static boolean canCreateEntry(ServerPlayer player, RegionGroup group)
    {
        if (group == null)
        {
            return false;
        }

        Feature feature = group.defaultGroup()
            ? Feature.ADD_DEFAULT_REGION
            : Feature.ADD_REGION;

        return canManageGroup(player, group, feature);
    }

    public static boolean canEditEntry(ServerPlayer player, RegionGroup group, RegionEntry entry)
    {
        if (player == null || group == null || entry == null)
        {
            return false;
        }

        if (FeaturePermissions.allows(player, Feature.EDIT_REGION))
        {
            return true;
        }

        if (!FeaturePermissions.ownerBypass())
        {
            return false;
        }

        UUID playerId = player.getUUID();

        if (entry.owner() != null && entry.owner().equals(playerId))
        {
            return true;
        }

        return ownsGroup(player, group);
    }

    public static boolean canDeleteGroup(ServerPlayer player, RegionGroup group)
    {
        return group != null && !group.defaultGroup()
            && canManageGroup(player, group, Feature.DELETE_GROUP);
    }

    public static boolean canClearGroup(ServerPlayer player, RegionGroup group)
    {
        if (group == null)
        {
            return false;
        }

        // The built-in default group has no owner: only an explicit permission
        // (or an OP-level rule) can clear it.
        if (group.defaultGroup())
        {
            return FeaturePermissions.allows(player, Feature.CLEAR_GROUP);
        }

        return canManageGroup(player, group, Feature.CLEAR_GROUP);
    }

    private static boolean ownsGroup(ServerPlayer player, RegionGroup group)
    {
        return !group.defaultGroup()
            && group.owner() != null
            && group.owner().equals(player.getUUID());
    }
}
