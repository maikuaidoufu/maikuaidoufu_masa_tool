package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.ClientActions;
import cn.maikuaidoufu.client.ClientGroupStore;
import cn.maikuaidoufu.client.ClientHighlightStore;
import cn.maikuaidoufu.region.Box;
import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class WidgetEntry extends WidgetListEntryBase<RegionEntry>
{
    private final WidgetEntryList parent;
    private final RegionEntry entry;
    private final RegionGroup group;
    private final boolean isOdd;
    private final int buttonsStartX;

    public WidgetEntry(int x, int y, int width, int height, boolean isOdd,
                       RegionEntry entry, int listIndex, WidgetEntryList parent)
    {
        super(x, y, width, height, entry, listIndex);
        this.parent = parent;
        this.entry = entry;
        this.isOdd = isOdd;
        this.group = ClientGroupStore.group(parent.getParentGui().getGroupId());

        int right = x + width - 2;
        int buttonY = y + 2;

        boolean canEdit = this.group != null && ClientGroupStore.canEditEntry(this.group, entry);
        ButtonGeneric delete = new HintButton(right, buttonY, -1, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.delete"));
        delete.setX(right - delete.getWidth());
        delete.setEnabled(canEdit);
        GuiHints.set(delete, canEdit ? "" : StringUtils.translate("maikuaidoufu_masa_tool.hover.no_permission"));
        addButton(delete, (button, mouseButton) -> confirmDelete());
        right = delete.getX() - 2;

        ButtonOnOff highlight = new ButtonOnOff(right, buttonY, -1, true,
            "maikuaidoufu_masa_tool.button.highlight", ClientHighlightStore.entryOverride(entry.id()));
        addButton(highlight, (button, mouseButton) ->
        {
            ClientHighlightStore.toggleEntry(this.entry.id());
            this.parent.getParentGui().initGui();
        });
        right = highlight.getX() - 2;

        ButtonOnOff enabled = new HintOnOff(right, buttonY, -1, true,
            "maikuaidoufu_masa_tool.button.enabled", entry.enabled());
        enabled.setEnabled(canEdit);
        GuiHints.set(enabled, !canEdit
            ? StringUtils.translate("maikuaidoufu_masa_tool.hover.no_permission")
            : this.group != null && !this.group.enabled()
                ? StringUtils.translate("maikuaidoufu_masa_tool.hover.group_disabled")
                : "");
        addButton(enabled, (button, mouseButton) ->
        {
            if (this.group != null)
            {
                ClientActions.setEntryEnabled(this.group.id(), this.entry.id(), !this.entry.enabled());
            }
        });
        this.buttonsStartX = enabled.getX() - 2;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        boolean hovered = isMouseOver(mouseX, mouseY);
        int color = selected || hovered ? 0x40FFFFFF : this.isOdd ? 0x24FFFFFF : 0x14FFFFFF;
        RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, color);

        boolean effectivelyEnabled = this.group != null && this.group.enabled() && this.entry.enabled();
        int nameColor = effectivelyEnabled ? 0xFFFFFFFF : 0xFF9A9A9A;
        Minecraft mc = Minecraft.getInstance();
        int maxWidth = Math.max(10, this.buttonsStartX - this.x - 8);
        String name = mc.font.plainSubstrByWidth(this.entry.name(), maxWidth);
        drawString(ctx, this.x + 4, this.y + 4, nameColor, name);

        Box box = this.entry.box();
        String coords = "(" + box.minX() + ", " + box.minY() + ", " + box.minZ() + ") → ("
            + box.maxX() + ", " + box.maxY() + ", " + box.maxZ() + ")  §8["
            + box.sizeX() + "×" + box.sizeY() + "×" + box.sizeZ() + "]";
        String meta = mc.font.plainSubstrByWidth(coords, maxWidth);
        drawString(ctx, this.x + 4, this.y + 14, 0xFF9A9A9A, "§7" + meta);

        super.render(ctx, mouseX, mouseY, selected);
    }

    private void confirmDelete()
    {
        if (this.group == null)
        {
            return;
        }

        String title = StringUtils.translate("maikuaidoufu_masa_tool.confirm.delete_entry_title");
        String message = StringUtils.translate("maikuaidoufu_masa_tool.confirm.delete_entry_message", this.entry.name());
        GuiBase.openGui(new GuiConfirm(title, List.of(message), () ->
            ClientActions.deleteEntry(this.group.id(), this.entry.id()), this.parent.getParentGui()));
    }
}
