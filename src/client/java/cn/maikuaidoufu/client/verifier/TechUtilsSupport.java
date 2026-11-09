package cn.maikuaidoufu.client.verifier;

import cn.maikuaidoufu.MasaToolMod;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Loads the techutils integration only when techutils is installed.
 *
 * <p>{@code TechUtilsBridgeImpl} references techutils classes directly, so it
 * must never be loaded on a client without the mod: otherwise the export would
 * fail with a {@link NoClassDefFoundError} that the GUI cannot recover from.</p>
 */
public final class TechUtilsSupport
{
    public interface Bridge
    {
        List<String> collect(SchematicVerifier verifier, boolean compress);
    }

    private static final Bridge BRIDGE = createBridge();
    private static boolean warned;

    private TechUtilsSupport()
    {
    }

    private static Bridge createBridge()
    {
        if (!FabricLoader.getInstance().isModLoaded("techutils"))
        {
            return null;
        }

        try
        {
            Class<?> clazz = Class.forName("cn.maikuaidoufu.client.verifier.TechUtilsBridgeImpl");
            return (Bridge) clazz.getDeclaredConstructor().newInstance();
        }
        catch (Throwable t)
        {
            MasaToolMod.LOGGER.error("Failed to initialize the techutils bridge", t);
            return null;
        }
    }

    public static boolean isAvailable()
    {
        return BRIDGE != null;
    }

    public static List<String> collect(SchematicVerifier verifier, boolean compress)
    {
        if (BRIDGE == null)
        {
            warnOnce("techutils is not available; skipping container export");
            return List.of();
        }

        try
        {
            return BRIDGE.collect(verifier, compress);
        }
        catch (Throwable t)
        {
            warnOnce("Could not read container mismatches: " + t);
            return List.of();
        }
    }

    private static synchronized void warnOnce(String message)
    {
        if (!warned)
        {
            warned = true;
            MasaToolMod.LOGGER.warn(message);
        }
    }
}
