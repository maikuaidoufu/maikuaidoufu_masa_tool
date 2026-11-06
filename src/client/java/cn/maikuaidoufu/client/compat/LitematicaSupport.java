package cn.maikuaidoufu.client.compat;

import cn.maikuaidoufu.MasaToolMod;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Classloading-safe facade for the optional Litematica integration. The bridge
 * implementation is only loaded when Litematica is present.
 */
public final class LitematicaSupport
{
    public interface Bridge
    {
        List<PlacementInfo> getPlacements();

        SelectionInfo getSelection();
    }

    private static final Bridge BRIDGE = createBridge();

    private LitematicaSupport()
    {
    }

    private static Bridge createBridge()
    {
        if (!FabricLoader.getInstance().isModLoaded("litematica"))
        {
            return null;
        }

        try
        {
            Class<?> clazz = Class.forName("cn.maikuaidoufu.client.compat.LitematicaBridgeImpl");
            return (Bridge) clazz.getDeclaredConstructor().newInstance();
        }
        catch (Throwable e)
        {
            MasaToolMod.LOGGER.error("Failed to initialize the optional Litematica bridge", e);
            return null;
        }
    }

    public static boolean isAvailable()
    {
        return BRIDGE != null;
    }

    public static List<PlacementInfo> placements()
    {
        if (BRIDGE == null)
        {
            return List.of();
        }

        try
        {
            return BRIDGE.getPlacements();
        }
        catch (Throwable e)
        {
            MasaToolMod.LOGGER.warn("Failed to read Litematica placements: {}", e.getMessage());
            return List.of();
        }
    }

    public static SelectionInfo selection()
    {
        if (BRIDGE == null)
        {
            return null;
        }

        try
        {
            return BRIDGE.getSelection();
        }
        catch (Throwable e)
        {
            MasaToolMod.LOGGER.warn("Failed to read Litematica selection: {}", e.getMessage());
            return null;
        }
    }
}
