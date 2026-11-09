package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.client.Configs;
import cn.maikuaidoufu.client.compat.ReflectionSupport;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * Adds the export button to the Litematica schematic verifier screen, on the
 * first button row right after the HUD toggle.
 *
 * <p>MaLiLib has no public accessor for its button list, so the list is read
 * through {@link ReflectionSupport}. The button itself is tracked by identity:
 * no reflective read of its private label is needed, and a rebuilt screen gets
 * a fresh button automatically.</p>
 */
public final class VerifierGuiHook
{
    private static final String LABEL_KEY = "maikuaidoufu_masa_tool.button.export_verifier";
    private static final int ROW_Y = 20;
    private static final int MIN_BUTTON_WIDTH = 44;
    private static final Map<GuiBase, ButtonBase> ADDED_BUTTONS = new WeakHashMap<>();
    private static boolean loggedFailure;

    private VerifierGuiHook()
    {
    }

    public static void onClientTick(Minecraft mc)
    {
        if (!Configs.VERIFIER_EXPORT.getBooleanValue())
        {
            return;
        }

        if (mc.screen == null
            || !mc.screen.getClass().getName().equals("fi.dy.masa.litematica.gui.GuiSchematicVerifier")
            || !FabricLoader.getInstance().isModLoaded("litematica"))
        {
            return;
        }

        try
        {
            apply((GuiSchematicVerifier) mc.screen);
        }
        catch (Throwable t)
        {
            if (!loggedFailure)
            {
                loggedFailure = true;
                MasaToolMod.LOGGER.warn("Could not add the verifier export button", t);
            }
        }
    }

    private static void apply(GuiSchematicVerifier gui)
    {
        SchematicPlacement placement = gui.getPlacement();

        if (placement == null)
        {
            return;
        }

        SchematicVerifier verifier = placement.getSchematicVerifier();

        if (verifier == null)
        {
            return;
        }

        List<ButtonBase> buttons = buttons(gui);

        if (buttons == null)
        {
            return;
        }

        ButtonBase previous = ADDED_BUTTONS.get(gui);

        if (previous != null && containsIdentity(buttons, previous))
        {
            return;
        }

        int x = 12;

        for (ButtonBase button : buttons)
        {
            if (button.getY() == ROW_Y)
            {
                x = Math.max(x, button.getX() + button.getWidth() + 4);
            }
        }

        String label = StringUtils.translate(LABEL_KEY);
        int buttonWidth = Math.max(MIN_BUTTON_WIDTH, gui.getStringWidth(label) + 10);
        ButtonGeneric button = new ButtonGeneric(x, ROW_Y, buttonWidth, 20, label);
        gui.addButton(button, (clicked, mouseButton) ->
            GuiBase.openGui(new GuiVerifierExport(verifier, placement.getName(), gui)));
        ADDED_BUTTONS.put(gui, button);
    }

    @SuppressWarnings("unchecked")
    private static List<ButtonBase> buttons(GuiBase gui)
    {
        Optional<Field> field = ReflectionSupport.field(GuiBase.class, "buttons");

        if (field.isEmpty())
        {
            return null;
        }

        return ReflectionSupport.read(field.get(), gui, List.class)
            .map(value -> (List<ButtonBase>) value)
            .orElse(null);
    }

    private static boolean containsIdentity(List<ButtonBase> buttons, ButtonBase target)
    {
        for (ButtonBase button : buttons)
        {
            if (button == target)
            {
                return true;
            }
        }

        return false;
    }
}
