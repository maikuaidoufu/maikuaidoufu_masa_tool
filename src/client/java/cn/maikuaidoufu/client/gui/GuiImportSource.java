package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.ClientActions;
import cn.maikuaidoufu.client.ClientGroupStore;
import cn.maikuaidoufu.client.compat.LitematicaSupport;
import cn.maikuaidoufu.client.compat.PlacementInfo;
import cn.maikuaidoufu.client.compat.SelectionInfo;
import cn.maikuaidoufu.net.EntryDraft;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;

public final class GuiImportSource extends GuiBase
{
    private final GuiGroupList parent;
    private List<PlacementInfo> placements = List.of();
    private SelectionInfo selection;

    public GuiImportSource(GuiGroupList parent)
    {
        this.parent = parent;
        setTitle(StringUtils.translate("maikuaidoufu_masa_tool.title.import"));
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.placements = LitematicaSupport.placements();
        this.selection = LitematicaSupport.selection();

        int x = 30;
        int y = 55;
        int width = 300;

        ButtonGeneric fromPlacement = new HintButton(x, y, width, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.import_placement"));
        fromPlacement.setEnabled(!this.placements.isEmpty());
        fromPlacement.setHoverStrings(this.placements.isEmpty()
            ? StringUtils.translate("maikuaidoufu_masa_tool.hover.no_placements") : "");
        addButton(fromPlacement, (button, mouseButton) ->
            GuiBase.openGui(new GuiPlacementPicker(this)));
        y += 26;

        ButtonGeneric fromSelection = new HintButton(x, y, width, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.import_selection"));
        fromSelection.setEnabled(this.selection != null);
        fromSelection.setHoverStrings(this.selection == null
            ? StringUtils.translate("maikuaidoufu_masa_tool.hover.no_selection") : "");
        addButton(fromSelection, (button, mouseButton) -> importSelection());
        y += 34;

        ButtonGeneric back = new ButtonGeneric(x, y, width, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.back"));
        addButton(back, (button, mouseButton) -> closeToParent());
    }

    private void importSelection()
    {
        if (this.selection == null || this.selection.boxes().isEmpty())
        {
            return;
        }

        List<EntryDraft> drafts = new ArrayList<>();

        for (int i = 0; i < this.selection.boxes().size(); i++)
        {
            String name = i < this.selection.boxNames().size()
                ? this.selection.boxNames().get(i)
                : StringUtils.translate("maikuaidoufu_masa_tool.default_entry_name") + " " + (i + 1);
            drafts.add(new EntryDraft(name, this.selection.dimension(), this.selection.boxes().get(i), true));
        }

        String groupName = ClientActions.uniqueGroupName(this.selection.name());
        String source = StringUtils.translate("maikuaidoufu_masa_tool.source.selection", this.selection.name());
        ClientActions.createGroup(groupName, source, drafts);
        GuiBase.openGui(new GuiGroupList());
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(ctx, mouseX, mouseY, partialTicks);

        if (!LitematicaSupport.isAvailable())
        {
            drawString(ctx, StringUtils.translate("maikuaidoufu_masa_tool.hover.need_litematica"),
                30, 180, 0xFFFF7777);
            return;
        }

        String info = StringUtils.translate("maikuaidoufu_masa_tool.import.available",
            this.placements.size(), this.selection == null ? 0 : this.selection.boxes().size());
        drawString(ctx, info, 30, 175, 0xFF999999);
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
