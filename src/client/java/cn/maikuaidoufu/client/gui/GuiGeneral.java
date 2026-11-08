package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.client.ClientActions;
import cn.maikuaidoufu.client.ClientGroupStore;
import cn.maikuaidoufu.client.Configs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import net.fabricmc.loader.api.FabricLoader;

public final class GuiGeneral extends GuiBase
{
    public GuiGeneral()
    {
        setTitle(StringUtils.translate("maikuaidoufu_masa_tool.title.general"));
    }

    @Override
    public void initGui()
    {
        super.initGui();
        GuiTabs.add(this, GuiTabs.Tab.GENERAL, 26);

        ButtonOnOff render = new ButtonOnOff(30, 60, 220, false,
            "maikuaidoufu_masa_tool.button.render_highlights", Configs.RENDER_HIGHLIGHTS.getBooleanValue());
        addButton(render, (button, mouseButton) ->
        {
            Configs.RENDER_HIGHLIGHTS.setBooleanValue(!Configs.RENDER_HIGHLIGHTS.getBooleanValue());
            Configs.saveConfig();
            initGui();
        });

        ButtonGeneric request = new ButtonGeneric(30, 90, 220, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.request_snapshot"));
        request.setEnabled(ClientGroupStore.isConnected());
        addButton(request, (button, mouseButton) -> ClientActions.requestSnapshot());

        ButtonGeneric back = new ButtonGeneric(30, 130, 220, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.back"));
        addButton(back, (button, mouseButton) -> GuiBase.openGui(new GuiGroupList()));
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(ctx, mouseX, mouseY, partialTicks);

        String version = FabricLoader.getInstance().getModContainer(MasaToolMod.MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("?");
        String status = ClientGroupStore.isConnected()
            ? StringUtils.translate("maikuaidoufu_masa_tool.status.connected")
            : StringUtils.translate("maikuaidoufu_masa_tool.status.disconnected");
        String keybind = Configs.OPEN_GUI.getHotkeyStringValue();

        int y = 180;
        drawString(ctx, StringUtils.translate("maikuaidoufu_masa_tool.general.version", version), 30, y, 0xFFAAAAAA);
        drawString(ctx, StringUtils.translate("maikuaidoufu_masa_tool.general.status", status), 30, y + 14, 0xFFAAAAAA);
        drawString(ctx, StringUtils.translate("maikuaidoufu_masa_tool.general.keybind", keybind), 30, y + 28, 0xFFAAAAAA);
        drawString(ctx, StringUtils.translate("maikuaidoufu_masa_tool.general.note"), 30, y + 50, 0xFF777777);
    }
}
