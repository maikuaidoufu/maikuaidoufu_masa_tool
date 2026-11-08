package cn.maikuaidoufu.client.gui;

import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * On/off button variant of {@link HintButton}.
 */
public class HintOnOff extends ButtonOnOff
{
    public HintOnOff(int x, int y, int width, boolean rightAlign, String translationKey, boolean state)
    {
        super(x, y, width, rightAlign, translationKey, state);
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
