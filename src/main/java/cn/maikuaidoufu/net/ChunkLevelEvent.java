package cn.maikuaidoufu.net;

import cn.maikuaidoufu.region.RegionLimits;
import java.util.List;

/**
 * Language-neutral description of one chunk ticket-level change, including the
 * exact tickets that were added or removed since the previous tier change.
 */
public record ChunkLevelEvent(
    String dimension,
    int chunkX,
    int chunkZ,
    int oldLevel,
    int newLevel,
    String oldStatus,
    String newStatus,
    List<String> tickets,
    List<String> addedTickets,
    List<String> removedTickets,
    long gameTime
)
{
    private static final int MAX_STATUS = 32;
    private static final int MAX_TICKET_TEXT = 128;
    private static final int MAX_TICKETS = 32;

    public ChunkLevelEvent
    {
        dimension = clip(dimension, RegionLimits.MAX_DIMENSION_LENGTH);
        oldStatus = clip(oldStatus, MAX_STATUS);
        newStatus = clip(newStatus, MAX_STATUS);
        tickets = clean(tickets);
        addedTickets = clean(addedTickets);
        removedTickets = clean(removedTickets);
    }

    private static List<String> clean(List<String> values)
    {
        return values == null
            ? List.of()
            : values.stream().limit(MAX_TICKETS).map(value -> clip(value, MAX_TICKET_TEXT)).toList();
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
