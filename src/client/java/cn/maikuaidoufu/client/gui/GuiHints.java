package cn.maikuaidoufu.client.gui;

import fi.dy.masa.malilib.gui.button.ButtonBase;

public final class GuiHints
{
    private GuiHints()
    {
    }

    /**
     * Sets a hover text only when there is one. An empty string still counts as
     * hover text, which makes MaLiLib draw an empty tooltip box.
     */
    public static void set(ButtonBase button, String hint)
    {
        if (hint != null && !hint.isEmpty())
        {
            button.setHoverStrings(hint);
        }
    }
}
