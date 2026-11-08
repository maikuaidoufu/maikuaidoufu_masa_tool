package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.client.Configs;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import java.util.List;

public final class GuiHotkeys extends GuiConfigsBase
{
    public GuiHotkeys()
    {
        super(10, 50, MasaToolMod.MOD_ID, null, "maikuaidoufu_masa_tool.title.hotkeys");
    }

    @Override
    public void initGui()
    {
        super.initGui();
        GuiTabs.add(this, GuiTabs.Tab.HOTKEYS, 26);
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs()
    {
        return ConfigOptionWrapper.createFor(List.of(Configs.OPEN_GUI));
    }
}
