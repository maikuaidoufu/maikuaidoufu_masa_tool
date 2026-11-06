package cn.maikuaidoufu.region;

import java.util.UUID;

/**
 * One sub-region inside a group. Entry ids are stable; the owner is the player
 * that created this entry (or the group owner for imported entries).
 */
public record RegionEntry(
    UUID id,
    String name,
    String dimension,
    Box box,
    boolean enabled,
    UUID owner,
    String ownerName,
    long createdAt
)
{
    public RegionEntry
    {
        name = RegionLimits.cleanName(name, "Sub-region");
        dimension = dimension == null ? "" : dimension;
        ownerName = ownerName == null ? "" : ownerName;
        box = box == null ? new Box(0, 0, 0, 0, 0, 0) : box;
    }

    public RegionEntry withEnabled(boolean enabled)
    {
        return new RegionEntry(this.id, this.name, this.dimension, this.box, enabled,
            this.owner, this.ownerName, this.createdAt);
    }
}
