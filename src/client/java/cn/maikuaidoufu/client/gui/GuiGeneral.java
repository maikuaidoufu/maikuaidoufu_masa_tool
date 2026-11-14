package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.client.ClientLogStore;
import cn.maikuaidoufu.client.Configs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldInteger;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.util.StringUtils;
import net.fabricmc.loader.api.FabricLoader;

/**
 * General tab: feature switches, client-side log cache size, author and version.
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

        String cacheLabel = StringUtils.translate("maikuaidoufu_masa_tool.general.log_cache_size");
        int labelWidth = this.getStringWidth(cacheLabel);
        this.addLabel(12, 104, labelWidth + 4, 12, 0xFFAAAAAA, cacheLabel);

        int fieldX = 12 + labelWidth + 8;
        GuiTextFieldInteger cacheField = new GuiTextFieldInteger(fieldX, 100, 60, 20, this.font);
        cacheField.setMaxLengthWrapper(6);
        cacheField.setValue(String.valueOf(Configs.LOG_CACHE_SIZE.getIntegerValue()));
        this.addTextField(cacheField, field -> true);

        ButtonGeneric apply = new ButtonGeneric(fieldX + 64, 100, -1, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.log_cache_apply"));
        this.addButton(apply, (button, mouseButton) ->
        {
            try
            {
                int value = Integer.parseInt(cacheField.getValue().trim());
                // ConfigInteger clamps to the configured 100..10000 range.
                Configs.LOG_CACHE_SIZE.setIntegerValue(value);
                Configs.saveConfig();
                ClientLogStore.applyCacheSize();
            }
            catch (NumberFormatException ignored)
            {
                // Incomplete input: keep the previous value.
            }

            initGui();
        });

        this.addLabel(12, 132, 240, 12, 0xFFAAAAAA, "by maikuaidoufu");
        this.addLabel(12, 148, 240, 12, 0xFFAAAAAA,
            StringUtils.translate("maikuaidoufu_masa_tool.general.version", version()));
    }

    private static String version()
    {
        return FabricLoader.getInstance().getModContainer(MasaToolMod.MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("?");
    }
}
