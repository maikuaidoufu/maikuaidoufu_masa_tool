package cn.maikuaidoufu.net;

import cn.maikuaidoufu.region.RegionLimits;

/**
 * Language-neutral description of one portal travel (or one log notice).
 *
 * <p>The server only collects data; the client turns it into a localised line
 * with its own language file, so no display text crosses the network.</p>
 */
public record PortalEvent(
    int kind,
    String entityName,
    String entityType,
    int itemCount,
    boolean customNamed,
    String fromDimension,
    String fromPos,
    String toDimension,
    String toPos,
    String entryPos,
    String portalPos,
    String portalSize,
    int radius,
    double idealDistance,
    boolean created,
    boolean searched,
    String failure,
    int droppedLines
)
{
    private static final int MAX_TEXT = 256;

    public PortalEvent
    {
        entityName = clip(entityName, MAX_TEXT);
        entityType = clip(entityType, MAX_TEXT);
        fromDimension = clip(fromDimension, RegionLimits.MAX_DIMENSION_LENGTH);
        fromPos = clip(fromPos, MAX_TEXT);
        toDimension = clip(toDimension, RegionLimits.MAX_DIMENSION_LENGTH);
        toPos = clip(toPos, MAX_TEXT);
        entryPos = clip(entryPos, MAX_TEXT);
        portalPos = clip(portalPos, MAX_TEXT);
        portalSize = clip(portalSize, MAX_TEXT);
        failure = clip(failure, MAX_TEXT);
    }

    /**
     * Notice sent when the per-tick message budget dropped entries.
     */
    public static PortalEvent suppressed(int count)
    {
        return new PortalEvent(2, "", "", -1, false, "", "", "", "", "", "", "",
            0, -1.0, false, false, "", count);
    }

    private static String clip(String value, int max)
    {
        if (value == null)
        {
            return "";
        }

        return value.length() > max ? value.substring(0, max) : value;
    }
}
