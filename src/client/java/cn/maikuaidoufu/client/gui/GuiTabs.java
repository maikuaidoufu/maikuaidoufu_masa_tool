package cn.maikuaidoufu.client.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;

public final class GuiTabs
{
    public enum Tab
    {
        GROUPS,
        GENERAL
    }

    private GuiTabs()
    {
    }

    public static void add(GuiBase gui, Tab selected, int y)
    {
        int x = 10;

        for (Tab tab : Tab.values())
        {
            String label = label(tab);
            int width = gui.getStringWidth(label) + 20;
            ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);
            button.setEnabled(tab != selected);
            gui.addButton(button, (b, mouseButton) -> open(tab));
            x += width + 2;
        }
    }

    public static String label(Tab tab)
    {
        return StringUtils.translate(tab == Tab.GROUPS
            ? "maikuaidoufu_masa_tool.tab.regions"
            : "maikuaidoufu_masa_tool.tab.general");
    }

    private static void open(Tab tab)
    {
        GuiBase.openGui(tab == Tab.GROUPS ? new GuiGroupList() : new GuiGeneral());
    }
}
