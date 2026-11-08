package cn.maikuaidoufu.client.gui;

import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Button that keeps its hover text while disabled. A list row skips hover
 * handling for disabled children, so a plain ButtonGeneric loses its hint there.
 */
public class HintButton extends ButtonGeneric
{
    public HintButton(int x, int y, int width, int height, String text, String... hover)
    {
        super(x, y, width, height, text, hover);
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY)
    {
        return this.visible
            && mouseX >= this.x && mouseX < this.x + this.width
            && mouseY >= this.y && mouseY < this.y + this.height;
    }

    @Override
    protected boolean onMouseClickedImpl(MouseButtonEvent event, boolean doubleClick)
    {
        return this.enabled && super.onMouseClickedImpl(event, doubleClick);
    }
}
