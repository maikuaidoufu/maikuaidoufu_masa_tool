package cn.maikuaidoufu.server;

import cn.maikuaidoufu.net.EntryDraft;
import cn.maikuaidoufu.region.Box;
import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import cn.maikuaidoufu.region.RegionLimits;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelReader;

/**
 * Server-authoritative group table. Reads use an immutable, enabled-only index
 * so the observer mixin never has to walk disabled groups or lock.
 */
public final class RegionManager
{
    private static volatile List<RegionGroup> groups = List.of();
    private static volatile Map<String, List<RegionEntry>> activeByDimension = Map.of();

    private RegionManager()
    {
    }

    public static synchronized void setGroups(List<RegionGroup> loaded)
    {
        groups = loaded == null ? List.of() : List.copyOf(loaded);
        rebuildIndex();
    }

    public static List<RegionGroup> getGroups()
    {
        return groups;
    }

    public static RegionGroup getGroup(UUID groupId)
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

    public static synchronized void replaceGroup(RegionGroup group)
    {
        List<RegionGroup> next = new ArrayList<>(groups);

        for (int i = 0; i < next.size(); i++)
        {
            if (next.get(i).id().equals(group.id()))
            {
                next.set(i, group);
                groups = List.copyOf(next);
                rebuildIndex();
                return;
            }
        }

        next.add(group);
        groups = List.copyOf(next);
        rebuildIndex();
    }

    public static synchronized void removeGroup(UUID groupId)
    {
        List<RegionGroup> next = new ArrayList<>(groups);
        next.removeIf(group -> group.id().equals(groupId));
        groups = List.copyOf(next);
        rebuildIndex();
    }

    public static synchronized void addGroup(RegionGroup group)
    {
        List<RegionGroup> next = new ArrayList<>(groups);
        next.add(group);
        groups = List.copyOf(next);
        rebuildIndex();
    }

    public static int totalEntryCount()
    {
        int count = 0;

        for (RegionGroup group : groups)
        {
            count += group.entries().size();
        }

        return count;
    }

    public static synchronized RegionGroup createGroup(String name, String source, List<EntryDraft> drafts,
                                                      ServerPlayer player, long now)
    {
        UUID groupId = UUID.randomUUID();
        List<RegionEntry> entries = new ArrayList<>(drafts.size());

        for (EntryDraft draft : drafts)
        {
            entries.add(new RegionEntry(
                UUID.randomUUID(),
                draft.name(),
                draft.dimension(),
                draft.box(),
                draft.enabled(),
                player.getUUID(),
                player.getScoreboardName(),
                now
            ));
        }

        RegionGroup group = new RegionGroup(
            groupId,
            name,
            false,
            true,
            source,
            player.getUUID(),
            player.getScoreboardName(),
            now,
            now,
            entries
        );

        addGroup(group);
        return group;
    }

    public static synchronized RegionGroup createEntry(UUID groupId, String name, String dimension,
                                                       Box box, boolean enabled, ServerPlayer player, long now)
    {
        RegionGroup group = getGroup(groupId);

        if (group == null)
        {
            return null;
        }

        RegionEntry entry = new RegionEntry(
            UUID.randomUUID(),
            name,
            dimension,
            box,
            enabled,
            player.getUUID(),
            player.getScoreboardName(),
            now
        );

        RegionGroup next = group.withEntry(entry, now);
        replaceGroup(next);
        return next;
    }

    public static synchronized RegionGroup setGroupEnabled(UUID groupId, boolean enabled, long now)
    {
        RegionGroup group = getGroup(groupId);

        if (group == null)
        {
            return null;
        }

        RegionGroup next = group.withEnabled(enabled, now);
        replaceGroup(next);
        return next;
    }

    public static synchronized RegionGroup renameGroup(UUID groupId, String name, long now)
    {
        RegionGroup group = getGroup(groupId);

        if (group == null || group.defaultGroup())
        {
            return null;
        }

        RegionGroup next = group.withName(name, now);
        replaceGroup(next);
        return next;
    }

    public static synchronized RegionGroup clearGroup(UUID groupId, long now)
    {
        RegionGroup group = getGroup(groupId);

        if (group == null)
        {
            return null;
        }

        RegionGroup next = group.withEntries(List.of(), now);
        replaceGroup(next);
        return next;
    }

    public static synchronized boolean deleteGroup(UUID groupId)
    {
        RegionGroup group = getGroup(groupId);

        if (group == null || group.defaultGroup())
        {
            return false;
        }

        removeGroup(groupId);
        return true;
    }

    public static synchronized RegionGroup setEntryEnabled(UUID groupId, UUID entryId, boolean enabled, long now)
    {
        RegionGroup group = getGroup(groupId);

        if (group == null)
        {
            return null;
        }

        RegionEntry entry = group.entry(entryId);

        if (entry == null)
        {
            return null;
        }

        RegionGroup next = group.withEntry(entry.withEnabled(enabled), now);
        replaceGroup(next);
        return next;
    }

    public static synchronized RegionGroup deleteEntry(UUID groupId, UUID entryId, long now)
    {
        RegionGroup group = getGroup(groupId);

        if (group == null)
        {
            return null;
        }

        if (group.entry(entryId) == null)
        {
            return null;
        }

        RegionGroup next = group.withoutEntry(entryId, now);
        replaceGroup(next);
        return next;
    }

    public static boolean isSuppressed(LevelReader world, BlockPos pos)
    {
        if (!(world instanceof ServerLevel level))
        {
            return false;
        }

        String dimension = level.dimension().identifier().toString();
        List<RegionEntry> entries = activeByDimension.get(dimension);

        if (entries == null || entries.isEmpty())
        {
            return false;
        }

        for (RegionEntry entry : entries)
        {
            if (entry.box().contains(pos))
            {
                return true;
            }
        }

        return false;
    }

    private static void rebuildIndex()
    {
        Map<String, List<RegionEntry>> next = new HashMap<>();

        for (RegionGroup group : groups)
        {
            if (!group.enabled())
            {
                continue;
            }

            for (RegionEntry entry : group.entries())
            {
                if (entry.enabled()
                    && RegionLimits.validDimension(entry.dimension())
                    && entry.box().isValid())
                {
                    next.computeIfAbsent(entry.dimension(), key -> new ArrayList<>()).add(entry);
                }
            }
        }

        Map<String, List<RegionEntry>> immutable = new HashMap<>();

        for (Map.Entry<String, List<RegionEntry>> entry : next.entrySet())
        {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }

        activeByDimension = Map.copyOf(immutable);
    }

    public static synchronized void clear()
    {
        groups = List.of();
        activeByDimension = Map.of();
    }

    public static RegionGroup createDefaultGroup(long now)
    {
        return new RegionGroup(
            RegionLimits.DEFAULT_GROUP_ID,
            "Default",
            true,
            true,
            "default",
            null,
            "",
            now,
            now,
            List.of()
        );
    }

    public static synchronized void ensureDefaultGroup(long now)
    {
        for (RegionGroup group : groups)
        {
            if (group.defaultGroup())
            {
                return;
            }
        }

        List<RegionGroup> next = new ArrayList<>();
        next.add(createDefaultGroup(now));
        next.addAll(groups);
        groups = List.copyOf(next);
        rebuildIndex();
    }
}
