package cn.maikuaidoufu.client;

import cn.maikuaidoufu.net.ChunkLevelEvent;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a language-neutral {@link ChunkLevelEvent} into one localised line.
 */
public final class ChunkLogFormatter
{
    private static final String MOD_PREFIX = "maikuaidoufu_masa_tool.";
    private static final String KEY_PREFIX = MOD_PREFIX + "chunk.";

    private ChunkLogFormatter()
    {
    }

    public static String format(ChunkLevelEvent event)
    {
        if (event == null)
        {
            return "";
        }

        return translate("event",
            dimension(event.dimension()),
            event.chunkX(), event.chunkZ(),
            status(event.oldStatus()), event.oldLevel(),
            status(event.newStatus()), event.newLevel(),
            cause(event.tickets()));
    }

    /**
     * 1 = stronger loading, -1 = weaker, 0 = unchanged tier.
     */
    public static int direction(ChunkLevelEvent event)
    {
        return Integer.compare(rank(event.newStatus()), rank(event.oldStatus()));
    }

    private static int rank(String status)
    {
        return switch (status)
        {
            case "ENTITY_TICKING" -> 3;
            case "BLOCK_TICKING" -> 2;
            case "FULL" -> 1;
            default -> 0;
        };
    }

    private static String status(String status)
    {
        String suffix = switch (status)
        {
            case "ENTITY_TICKING" -> "entity_ticking";
            case "BLOCK_TICKING" -> "block_ticking";
            case "FULL" -> "full";
            default -> "inaccessible";
        };

        return translate("status." + suffix);
    }

    private static String dimension(String id)
    {
        String suffix = switch (id)
        {
            case "minecraft:overworld" -> "overworld";
            case "minecraft:the_nether" -> "nether";
            case "minecraft:the_end" -> "end";
            default -> null;
        };

        return suffix == null
            ? id
            : StringUtils.translate(MOD_PREFIX + "portal.log.dimension." + suffix);
    }

    private static String cause(List<String> tickets)
    {
        if (tickets.isEmpty())
        {
            return translate("reason.none");
        }

        List<String> parts = new ArrayList<>(tickets.size());

        for (String ticket : tickets)
        {
            int separator = ticket.lastIndexOf(':');
            String id = separator > 0 ? ticket.substring(0, separator) : ticket;
            String level = separator > 0 ? ticket.substring(separator + 1) : "?";
            String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
            parts.add(translate("ticket.item", ticketName(path), level));
        }

        return String.join(", ", parts);
    }

    private static String ticketName(String path)
    {
        String text = translate("ticket." + path);
        return text.startsWith(MOD_PREFIX) ? path : text;
    }

    private static String translate(String key, Object... args)
    {
        return StringUtils.translate(KEY_PREFIX + key, args);
    }
}
