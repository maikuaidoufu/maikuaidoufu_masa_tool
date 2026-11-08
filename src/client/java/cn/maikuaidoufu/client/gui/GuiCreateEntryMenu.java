package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.ClientGroupStore;
import cn.maikuaidoufu.client.compat.LitematicaSupport;
import cn.maikuaidoufu.client.compat.SelectionInfo;
import cn.maikuaidoufu.region.Box;
import cn.maikuaidoufu.region.RegionGroup;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.UUID;
import net.minecraft.client.gui.screens.Screen;

public final class GuiCreateEntryMenu extends GuiBase
{
    private final UUID groupId;
    private final Screen parent;

    public GuiCreateEntryMenu(UUID groupId, Screen parent)
    {
        this.groupId = groupId;
        this.parent = parent;
        setParent(parent);
        setTitle(StringUtils.translate("maikuaidoufu_masa_tool.title.create_entry"));
    }

    @Override
    public void initGui()
    {
        super.initGui();

        RegionGroup group = ClientGroupStore.group(this.groupId);

        if (group == null)
        {
            closeToParent();
            return;
        }

        int x = 30;
        int y = 55;
        int width = 240;

        ButtonGeneric manual = new ButtonGeneric(x, y, width, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.manual_entry"));
        addButton(manual, (button, mouseButton) ->
            GuiBase.openGui(new GuiCreateEntry(this.groupId, this.parent)));
        y += 26;

        SelectionInfo selection = LitematicaSupport.selection();
        boolean hasSelection = selection != null && !selection.boxes().isEmpty();
        ButtonGeneric fromSelection = new HintButton(x, y, width, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.import_selection"));
        fromSelection.setEnabled(hasSelection);
        GuiHints.set(fromSelection, hasSelection
            ? "" : StringUtils.translate("maikuaidoufu_masa_tool.hover.no_selection"));
        addButton(fromSelection, (button, mouseButton) -> openFromSelection(selection));
        y += 34;

        ButtonGeneric back = new ButtonGeneric(x, y, width, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.back"));
        addButton(back, (button, mouseButton) -> closeToParent());
    }

    private void openFromSelection(SelectionInfo selection)
    {
        if (selection == null || selection.boxes().isEmpty())
        {
            return;
        }

        Box box = selection.boxes().get(0);
        GuiBase.openGui(new GuiCreateEntry(this.groupId, this.parent, box,
            StringUtils.translate("maikuaidoufu_masa_tool.default_entry_name")));
    }

    private void closeToParent()
    {
        if (this.parent != null)
        {
            this.mc.setScreen(this.parent);
        }
        else
        {
            this.onClose();
        }
    }
}
