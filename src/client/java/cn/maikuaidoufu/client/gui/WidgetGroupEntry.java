package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.ClientActions;
import cn.maikuaidoufu.client.ClientGroupStore;
import cn.maikuaidoufu.client.ClientHighlightStore;
import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;

public final class WidgetGroupEntry extends WidgetListEntryBase<RegionGroup>
{
    private final WidgetGroupList parent;
    private final RegionGroup group;
    private final boolean isOdd;
    private final int buttonsStartX;
    private final String meta;

    public WidgetGroupEntry(int x, int y, int width, int height, boolean isOdd,
                            RegionGroup group, int listIndex, WidgetGroupList parent)
    {
        super(x, y, width, height, group, listIndex);
        this.parent = parent;
        this.group = group;
        this.isOdd = isOdd;
        this.meta = metadata();

        int right = x + width - 2;
        int buttonY = y + 1;

        boolean clear = group.defaultGroup();
        String deleteLabel = StringUtils.translate(clear
            ? "maikuaidoufu_masa_tool.button.clear"
            : "maikuaidoufu_masa_tool.button.delete");

        boolean canDelete = clear ? ClientGroupStore.canClearGroup(group) : ClientGroupStore.canDeleteGroup(group);
        ButtonGeneric delete = new HintButton(right, buttonY, -1, 20, deleteLabel);
        delete.setX(right - delete.getWidth());
        delete.setEnabled(canDelete);
        GuiHints.set(delete, canDelete ? "" : StringUtils.translate("maikuaidoufu_masa_tool.hover.no_permission"));
        addButton(delete, (button, mouseButton) -> confirmDelete());
        right = delete.getX() - 2;

        ButtonOnOff highlight = new ButtonOnOff(right, buttonY, -1, true,
            "maikuaidoufu_masa_tool.button.highlight", ClientHighlightStore.groupOverride(group.id()));
        addButton(highlight, (button, mouseButton) ->
        {
            ClientHighlightStore.toggleGroup(this.group.id());
            this.parent.getParentGui().initGui();
        });
        right = highlight.getX() - 2;

        boolean canMaster = ClientGroupStore.canEditGroup(group);
        ButtonOnOff master = new HintOnOff(right, buttonY, -1, true,
            "maikuaidoufu_masa_tool.button.switch", group.enabled());
        master.setEnabled(canMaster);
        GuiHints.set(master, canMaster ? "" : StringUtils.translate("maikuaidoufu_masa_tool.hover.no_permission"));
        addButton(master, (button, mouseButton) ->
            ClientActions.setGroupEnabled(this.group.id(), !this.group.enabled()));
        right = master.getX() - 2;

        ButtonGeneric enter = new ButtonGeneric(right, buttonY, -1, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.configure"));
        enter.setX(right - enter.getWidth());
        addButton(enter, (button, mouseButton) ->
            GuiBase.openGui(new GuiGroupDetail(this.group.id(), this.parent.getParentGui())));
        this.buttonsStartX = enter.getX() - 2;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        boolean hovered = isMouseOver(mouseX, mouseY);
        int color = selected || hovered ? 0x40FFFFFF : this.isOdd ? 0x24FFFFFF : 0x14FFFFFF;
        RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, color);

        Minecraft mc = Minecraft.getInstance();
        String name = this.group.defaultGroup()
            ? StringUtils.translate("maikuaidoufu_masa_tool.group.default")
            : this.group.name();

        int metaWidth = mc.font.width(this.meta);
        int metaX = this.buttonsStartX - metaWidth - 8;
        int maxNameWidth = Math.max(10, metaX - this.x - 6);
        String nameDisplay = mc.font.plainSubstrByWidth(name, maxNameWidth);

        int nameColor = this.group.defaultGroup() ? 0xFFFFD060 : this.group.enabled() ? 0xFFFFFFFF : 0xFF9A9A9A;
        drawString(ctx, this.x + 4, this.y + 7, nameColor, nameDisplay);
        drawString(ctx, metaX, this.y + 7, 0xFF9A9A9A, this.meta);

        super.render(ctx, mouseX, mouseY, selected);
    }

    private String metadata()
    {
        String dimension = dimensionLabel();
        String count = StringUtils.translate("maikuaidoufu_masa_tool.label.subregions", this.group.entries().size());
        String effective = StringUtils.translate("maikuaidoufu_masa_tool.label.effective",
            this.group.effectiveEntryCount(), this.group.entries().size());
        return dimension + " · " + count + " · " + effective;
    }

    private String dimensionLabel()
    {
        Set<String> dimensions = new HashSet<>();
        List<RegionEntry> entries = this.group.entries();

        for (RegionEntry entry : entries)
        {
            dimensions.add(entry.dimension());
        }

        if (dimensions.isEmpty())
        {
            return "-";
        }

        if (dimensions.size() > 1)
        {
            return StringUtils.translate("maikuaidoufu_masa_tool.label.multi_dimension");
        }

        String dimension = dimensions.iterator().next();

        if (dimension.endsWith("overworld"))
        {
            return StringUtils.translate("maikuaidoufu_masa_tool.dimension.overworld");
        }

        if (dimension.endsWith("the_nether"))
        {
            return StringUtils.translate("maikuaidoufu_masa_tool.dimension.nether");
        }

        if (dimension.endsWith("the_end"))
        {
            return StringUtils.translate("maikuaidoufu_masa_tool.dimension.end");
        }

        return dimension;
    }

    private void confirmDelete()
    {
        String title = StringUtils.translate(this.group.defaultGroup()
            ? "maikuaidoufu_masa_tool.confirm.clear_title"
            : "maikuaidoufu_masa_tool.confirm.delete_title");
        String message = StringUtils.translate(this.group.defaultGroup()
            ? "maikuaidoufu_masa_tool.confirm.clear_message"
            : "maikuaidoufu_masa_tool.confirm.delete_message", this.group.name(), this.group.entries().size());

        GuiBase.openGui(new GuiConfirm(title, List.of(message), () ->
        {
            if (this.group.defaultGroup())
            {
                ClientActions.clearGroup(this.group.id());
            }
            else
            {
                ClientActions.deleteGroup(this.group.id());
            }
        }, this.parent.getParentGui()));
    }
}
