package cn.maikuaidoufu.permissions;

/**
 * Server-side features whose required permission can be configured individually.
 *
 * <p>This enum is deliberately free of Minecraft/server classes: the client
 * reuses {@link #bit()} to decode the permission mask from the group snapshot,
 * while the actual checks live in {@code server.FeaturePermissions}.</p>
 */
public enum Feature
{
    /** Receive the region list (and therefore see the outlines in the GUI). */
    VIEW_REGIONS("viewRegions"),
    /** Create a new region group. */
    CREATE_GROUP("createGroup"),
    /** Add a sub-region to the built-in default group. */
    ADD_DEFAULT_REGION("addDefaultRegion"),
    /** Enable/disable and rename a group. */
    EDIT_GROUP("editGroup"),
    /** Delete a group. */
    DELETE_GROUP("deleteGroup"),
    /** Remove every sub-region from a group. */
    CLEAR_GROUP("clearGroup"),
    /** Add a sub-region to a regular group. */
    ADD_REGION("addRegion"),
    /** Enable/disable or delete a sub-region. */
    EDIT_REGION("editRegion"),
    /** Subscribe to the nether portal travel terminal. */
    PORTAL_LOG("portalLog"),
    /** Subscribe to / manage the chunk loading watcher. */
    CHUNK_MONITOR("chunkMonitor"),
    /** Run /masatool reload. */
    RELOAD_CONFIG("reloadConfig");

    /**
     * High bit of the snapshot mask: when set, players may manage groups and
     * sub-regions they created themselves even without the matching permission.
     */
    public static final int OWNER_BYPASS_BIT = 1 << 30;

    private final String configKey;

    Feature(String configKey)
    {
        this.configKey = configKey;
    }

    public String configKey()
    {
        return configKey;
    }

    public int bit()
    {
        return 1 << ordinal();
    }
}
