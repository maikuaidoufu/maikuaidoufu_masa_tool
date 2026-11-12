package cn.maikuaidoufu.region;

import java.util.UUID;

public final class RegionLimits
{
    // v2: language-neutral PortalEventPayload. v3: chunk load watcher.
    public static final int PROTOCOL_VERSION = 3;

    public static final int MAX_GROUPS = 256;
    public static final int MAX_ENTRIES_PER_GROUP = 256;
    public static final int MAX_TOTAL_ENTRIES = 8192;
    public static final int MAX_NAME_LENGTH = 64;
    public static final int MAX_SOURCE_LENGTH = 160;
    public static final int MAX_DIMENSION_LENGTH = 128;
    public static final int MAX_COORDINATE = 30_000_000;
    public static final int MAX_PAYLOAD_SIZE = 2 * 1024 * 1024;

    public static final UUID DEFAULT_GROUP_ID = new UUID(0L, 0L);

    private RegionLimits()
    {
    }

    public static boolean validName(String name)
    {
        return name != null && !name.isBlank() && name.length() <= MAX_NAME_LENGTH;
    }

    public static boolean validDimension(String dimension)
    {
        return dimension != null && !dimension.isBlank() && dimension.length() <= MAX_DIMENSION_LENGTH;
    }

    public static String cleanName(String name, String fallback)
    {
        if (name == null)
        {
            return fallback;
        }

        String trimmed = name.trim();

        if (trimmed.isEmpty())
        {
            return fallback;
        }

        return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
    }

    public static String cleanSource(String source)
    {
        if (source == null)
        {
            return "";
        }

        String trimmed = source.trim();
        return trimmed.length() > MAX_SOURCE_LENGTH ? trimmed.substring(0, MAX_SOURCE_LENGTH) : trimmed;
    }
}
