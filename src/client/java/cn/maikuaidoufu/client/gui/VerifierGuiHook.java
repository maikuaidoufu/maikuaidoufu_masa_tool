package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.Configs;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/**
 * Adds the export button to the Litematica schematic verifier screen.
 */
public final class VerifierGuiHook
{
    private VerifierGuiHook()
    {
    }

    public static void onScreen(Screen screen, int width, int height)
    {
        if (!Configs.VERIFIER_EXPORT.getBooleanValue()
            || !FabricLoader.getInstance().isModLoaded("litematica"))
        {
            return;
        }

        if (!(screen instanceof GuiSchematicVerifier gui))
        {
            return;
        }

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

        int buttonWidth = 76;
        ButtonGeneric button = new ButtonGeneric(width - buttonWidth - 10, 6, buttonWidth, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.export_verifier"));
        gui.addButton(button, (b, mouseButton) ->
            GuiBase.openGui(new GuiVerifierExport(verifier, placement.getName())));
    }
}
