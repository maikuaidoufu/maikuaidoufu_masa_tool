package cn.maikuaidoufu.client;

import cn.maikuaidoufu.net.PortalEvent;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.Locale;

/**
 * Turns a language-neutral {@link PortalEvent} into one localised terminal line.
 */
public final class PortalLogFormatter
{
    private static final String MOD_PREFIX = "maikuaidoufu_masa_tool.";
    private static final String KEY_PREFIX = MOD_PREFIX + "portal.log.";

    private PortalLogFormatter()
    {
    }

    public static String format(PortalEvent event)
    {
        if (event == null)
        {
            return "";
        }

        if (event.droppedLines() > 0)
        {
            return translate("suppressed", event.droppedLines());
        }

        if (event.entityName().isEmpty() && event.fromDimension().isEmpty())
        {
            return "";
        }

        String entity = entity(event);
        String from = dimension(event.fromDimension()) + event.fromPos();
        String travel = event.toDimension().isEmpty()
            ? translate("travel_unknown", entity, from, event.entryPos())
            : translate("travel", entity, from,
                dimension(event.toDimension()) + event.toPos(), event.entryPos());

        if (event.created())
        {
            String reason = event.radius() > 0
                ? translate("reason.radius", event.radius())
                : translate("reason.no_portal");
            return translate("created", event.portalPos(), event.portalSize(), travel, reason);
        }

        if (event.searched() && !event.portalPos().isEmpty())
        {
            String distance = String.format(Locale.ROOT, "%.1f", event.idealDistance());
            return event.idealDistance() >= 0
                ? translate("linked", event.portalPos(), travel, distance, event.radius())
                : translate("linked_no_ideal", event.portalPos(), travel, event.radius());
        }

        if (!event.failure().isEmpty())
        {
            return translate("other_failed", travel, failureText(event.failure()));
        }

        return travel;
    }

    private static String entity(PortalEvent event)
    {
        if (event.itemCount() >= 0)
        {
            return event.itemCount() > 1
                ? translate("entity.item_many", event.entityName(), event.itemCount())
                : translate("entity.item", event.entityName());
        }

        return event.customNamed()
            ? translate("entity.named", event.entityName(), event.entityType())
            : event.entityName();
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

        return suffix == null ? id : translate("dimension." + suffix);
    }

    private static String failureText(String code)
    {
        String text = translate("failure." + code);
        return text.startsWith(KEY_PREFIX) || text.startsWith(MOD_PREFIX) ? code : text;
    }

    private static String translate(String key, Object... args)
    {
        return StringUtils.translate(KEY_PREFIX + key, args);
    }
}
