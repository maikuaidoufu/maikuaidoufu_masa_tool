package cn.maikuaidoufu.client.compat;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import java.lang.reflect.Method;

/**
 * Optional Syncmatica bridge, only used for the "shared/local" label in the
 * import picker. It has no effect on the stored region tree.
 */
public final class SyncmaticaSupport
{
    private static boolean initialized;
    private static Object manager;
    private static Method getRenderingSyncmatic;

    private SyncmaticaSupport()
    {
    }

    public static boolean isShared(SchematicPlacement placement)
    {
        try
        {
            init();

            if (manager == null || getRenderingSyncmatic == null)
            {
                return false;
            }

            return getRenderingSyncmatic.invoke(manager, placement) != null;
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    private static void init()
    {
        if (initialized)
        {
            return;
        }

        initialized = true;

        try
        {
            Class<?> managerClass = Class.forName("ch.endte.syncmatica.litematica.LitematicManager");
            Method getInstance = managerClass.getMethod("getInstance");
            manager = getInstance.invoke(null);
            getRenderingSyncmatic = managerClass.getMethod("getRenderingSyncmatic", SchematicPlacement.class);
        }
        catch (Throwable ignored)
        {
            manager = null;
            getRenderingSyncmatic = null;
        }
    }
}
