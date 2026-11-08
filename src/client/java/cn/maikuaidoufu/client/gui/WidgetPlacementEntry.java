package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.compat.PlacementInfo;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;

public final class WidgetPlacementEntry extends WidgetListEntryBase<PlacementInfo>
{
    private final WidgetPlacementList parent;
    private final PlacementInfo placement;
    private final boolean isOdd;
    private final int buttonsStartX;

    public WidgetPlacementEntry(int x, int y, int width, int height, boolean isOdd,
                                PlacementInfo placement, int listIndex, WidgetPlacementList parent)
    {
        super(x, y, width, height, placement, listIndex);
        this.parent = parent;
        this.placement = placement;
        this.isOdd = isOdd;

        ButtonGeneric select = new ButtonGeneric(x + width - 2, y + 2, -1, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.import_selected"));
        select.setX(x + width - 2 - select.getWidth());
        addButton(select, (button, mouseButton) -> this.parent.getParentGui().select(this.placement));
        this.buttonsStartX = select.getX() - 4;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        boolean hovered = isMouseOver(mouseX, mouseY);
        int color = selected || hovered ? 0x40FFFFFF : this.isOdd ? 0x24FFFFFF : 0x14FFFFFF;
        RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, color);

        Minecraft mc = Minecraft.getInstance();
        String name = this.placement.name();
        String meta = this.placement.source() + " · " + dimensionName(this.placement.dimension())
            + " · " + StringUtils.translate("maikuaidoufu_masa_tool.label.subregions", this.placement.boxes().size());
        int maxWidth = Math.max(10, this.buttonsStartX - this.x - 8);
        drawString(ctx, this.x + 4, this.y + 4, 0xFFFFFFFF, mc.font.plainSubstrByWidth(name, maxWidth));
        drawString(ctx, this.x + 4, this.y + 14, 0xFF999999,
            "§7" + mc.font.plainSubstrByWidth(meta, maxWidth));

        super.render(ctx, mouseX, mouseY, selected);
    }

    private static String dimensionName(String dimension)
    {
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
}
