package cn.maikuaidoufu.client.gui;

import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Minimal checkbox: an outlined square with a filled marker when selected.
 */
public class WidgetExportCheckbox extends WidgetBase
{
    private final String label;
    private boolean checked;

    public WidgetExportCheckbox(int x, int y, int width, String label, boolean checked)
    {
        super(x, y, width, 12);
        this.label = label;
        this.checked = checked;
    }

    public boolean isChecked()
    {
        return this.checked;
    }

    @Override
    protected boolean onMouseClickedImpl(MouseButtonEvent event, boolean doubleClick)
    {
        if (event.button() == 0)
        {
            this.checked = !this.checked;
            return true;
        }

        return false;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        int color = this.isMouseOver(mouseX, mouseY) ? 0xFFFFFFFF : 0xFFAAAAAA;
        RenderUtils.drawRect(ctx, this.x, this.y, 9, 9, 0xFF000000);
        RenderUtils.drawOutline(ctx, this.x, this.y, 9, 9, color);

        if (this.checked)
        {
            RenderUtils.drawRect(ctx, this.x + 2, this.y + 2, 5, 5, 0xFF55FF55);
        }

        this.drawString(ctx, this.x + 16, this.y + 1, 0xFFFFFFFF, this.label);
    }
}
