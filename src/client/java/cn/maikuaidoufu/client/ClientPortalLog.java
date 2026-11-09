package cn.maikuaidoufu.client;

import cn.maikuaidoufu.net.PortalEvent;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Local copy of the portal travel lines received from the server.
 */
public final class ClientPortalLog
{
    public record Entry(String text, int kind)
    {
    }

    public static final int KIND_NEW = 0;
    public static final int KIND_LINK = 1;
    public static final int KIND_OTHER = 2;

    private static final int MAX_ENTRIES = 500;
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private static boolean enabled;
    private static long revision;

    private ClientPortalLog()
    {
    }

    public static void add(PortalEvent event)
    {
        if (event == null)
        {
            return;
        }

        String text = PortalLogFormatter.format(event);

        if (text.isBlank())
        {
            return;
        }

        ENTRIES.add(new Entry(text, event.kind()));

        while (ENTRIES.size() > MAX_ENTRIES)
        {
            ENTRIES.remove(0);
        }

        revision++;
    }

    public static void clear()
    {
        if (!ENTRIES.isEmpty())
        {
            ENTRIES.clear();
            revision++;
        }
    }

    public static List<Entry> entries()
    {
        return List.copyOf(ENTRIES);
    }

    public static boolean isEmpty()
    {
        return ENTRIES.isEmpty();
    }

    public static long revision()
    {
        return revision;
    }

    public static boolean isEnabled()
    {
        return enabled;
    }

    public static void setEnabled(boolean value)
    {
        enabled = value;
    }

    public static String prefix(int kind)
    {
        return switch (kind)
        {
            case KIND_NEW -> StringUtils.translate("maikuaidoufu_masa_tool.portal.prefix.new");
            case KIND_LINK -> StringUtils.translate("maikuaidoufu_masa_tool.portal.prefix.link");
            default -> StringUtils.translate("maikuaidoufu_masa_tool.portal.prefix.other");
        };
    }
}
