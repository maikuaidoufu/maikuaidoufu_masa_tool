package cn.maikuaidoufu.client.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;

public final class GuiTabs
{
    public enum Tab
    {
        GENERAL,
        GROUPS,
        PORTAL,
        HOTKEYS
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
        return switch (tab)
        {
            case GENERAL -> StringUtils.translate("maikuaidoufu_masa_tool.tab.general");
            case HOTKEYS -> StringUtils.translate("maikuaidoufu_masa_tool.tab.hotkeys");
            case GROUPS -> StringUtils.translate("maikuaidoufu_masa_tool.tab.regions");
            case PORTAL -> StringUtils.translate("maikuaidoufu_masa_tool.tab.portal");
        };
    }

    private static void open(Tab tab)
    {
        GuiBase.openGui(switch (tab)
        {
            case GENERAL -> new GuiGeneral();
            case HOTKEYS -> new GuiHotkeys();
            case GROUPS -> new GuiGroupList();
            case PORTAL -> new GuiPortalLog();
        });
    }
}
