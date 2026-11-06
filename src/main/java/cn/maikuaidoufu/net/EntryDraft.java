package cn.maikuaidoufu.net;

import cn.maikuaidoufu.region.Box;
import cn.maikuaidoufu.region.RegionLimits;

/**
 * A client-created entry before the server assigns ids and ownership.
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
