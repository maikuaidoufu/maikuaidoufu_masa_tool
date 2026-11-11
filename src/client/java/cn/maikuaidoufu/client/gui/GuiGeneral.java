package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.client.Configs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.util.StringUtils;
import net.fabricmc.loader.api.FabricLoader;

/**
 * General tab: feature switches, author and version.
 */
public class GuiGeneral extends GuiBase
{
    public GuiGeneral()
    {
        this.setTitle(StringUtils.translate("maikuaidoufu_masa_tool.title.general"));
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearWidgets();
        this.clearButtons();
        GuiTabs.add(this, GuiTabs.Tab.GENERAL, 26);

        ButtonOnOff export = new ButtonOnOff(12, 52, -1, false,
            "maikuaidoufu_masa_tool.button.verifier_export_toggle",
            Configs.VERIFIER_EXPORT.getBooleanValue());
        this.addButton(export, (button, mouseButton) ->
        {
            Configs.VERIFIER_EXPORT.setBooleanValue(!Configs.VERIFIER_EXPORT.getBooleanValue());
            Configs.saveConfig();
            initGui();
        });

        ButtonOnOff xray = new ButtonOnOff(12, 76, -1, false,
            "maikuaidoufu_masa_tool.button.xray_render",
            Configs.XRAY_HIGHLIGHTS.getBooleanValue());
        xray.setHoverStrings(StringUtils.translate("maikuaidoufu_masa_tool.hover.xray_render"));
        this.addButton(xray, (button, mouseButton) ->
        {
            Configs.XRAY_HIGHLIGHTS.setBooleanValue(!Configs.XRAY_HIGHLIGHTS.getBooleanValue());
            Configs.saveConfig();
            initGui();
        });

        this.addLabel(12, 108, 240, 12, 0xFFAAAAAA, "by maikuaidoufu");
        this.addLabel(12, 124, 240, 12, 0xFFAAAAAA,
            StringUtils.translate("maikuaidoufu_masa_tool.general.version", version()));
    }

    private static String version()
    {
        return FabricLoader.getInstance().getModContainer(MasaToolMod.MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("?");
    }
}
