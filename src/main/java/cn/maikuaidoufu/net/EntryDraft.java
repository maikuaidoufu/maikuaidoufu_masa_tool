package cn.maikuaidoufu.net;

import cn.maikuaidoufu.region.Box;
import cn.maikuaidoufu.region.RegionLimits;

/**
 * A sub-region sent from the client, before the server assigns ids and owners.
 */
public record EntryDraft(String name, String dimension, Box box, boolean enabled)
{
    public EntryDraft
    {
        name = RegionLimits.cleanName(name, "Sub-region");
        dimension = dimension == null ? "" : dimension;
        box = box == null ? new Box(0, 0, 0, 0, 0, 0) : box;
    }
}
