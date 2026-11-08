package cn.maikuaidoufu.region;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A group of sub-regions: either the default group or one imported from
 * Litematica.
 */
public record RegionGroup(
    UUID id,
    String name,
    boolean defaultGroup,
    boolean enabled,
    String source,
    UUID owner,
    String ownerName,
    long createdAt,
    long updatedAt,
    List<RegionEntry> entries
)
{
    public RegionGroup
    {
        name = RegionLimits.cleanName(name, defaultGroup ? "Default" : "Group");
        source = RegionLimits.cleanSource(source);
        ownerName = ownerName == null ? "" : ownerName;
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public int effectiveEntryCount()
    {
        if (!this.enabled)
        {
            return 0;
        }

        int count = 0;

        for (RegionEntry entry : this.entries)
        {
            if (entry.enabled())
            {
                count++;
            }
        }

        return count;
    }

    public int enabledEntryCount()
    {
        int count = 0;

        for (RegionEntry entry : this.entries)
        {
            if (entry.enabled())
            {
                count++;
            }
        }

        return count;
    }

    public RegionEntry entry(UUID entryId)
    {
        for (RegionEntry entry : this.entries)
        {
            if (entry.id().equals(entryId))
            {
                return entry;
            }
        }

        return null;
    }

    public RegionGroup withEnabled(boolean enabled, long now)
    {
        return new RegionGroup(this.id, this.name, this.defaultGroup, enabled, this.source,
            this.owner, this.ownerName, this.createdAt, now, this.entries);
    }

    public RegionGroup withName(String name, long now)
    {
        return new RegionGroup(this.id, name, this.defaultGroup, this.enabled, this.source,
            this.owner, this.ownerName, this.createdAt, now, this.entries);
    }

    public RegionGroup withEntries(List<RegionEntry> entries, long now)
    {
        return new RegionGroup(this.id, this.name, this.defaultGroup, this.enabled, this.source,
            this.owner, this.ownerName, this.createdAt, now, entries);
    }

    public RegionGroup withEntry(RegionEntry entry, long now)
    {
        List<RegionEntry> next = new ArrayList<>(this.entries);

        for (int i = 0; i < next.size(); i++)
        {
            if (next.get(i).id().equals(entry.id()))
            {
                next.set(i, entry);
                return withEntries(next, now);
            }
        }

        next.add(entry);
        return withEntries(next, now);
    }

    public RegionGroup withoutEntry(UUID entryId, long now)
    {
        List<RegionEntry> next = new ArrayList<>(this.entries);
        next.removeIf(entry -> entry.id().equals(entryId));
        return withEntries(next, now);
    }
}
