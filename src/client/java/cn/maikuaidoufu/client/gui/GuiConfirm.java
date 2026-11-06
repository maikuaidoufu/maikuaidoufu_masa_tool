package cn.maikuaidoufu.client.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;

public final class GuiConfirm extends GuiBase
{
    private final List<String> lines;
    private final Runnable onConfirm;
    private final Screen parent;

    public GuiConfirm(String title, List<String> lines, Runnable onConfirm, Screen parent)
    {
        this.lines = List.copyOf(lines);
        this.onConfirm = onConfirm;
        this.parent = parent;
        setTitle(title);
    }

    @Override
    public void initGui()
    {
        super.initGui();
        clearButtons();

        int buttonWidth = 100;
        int y = Math.max(100, getScreenHeight() / 2 + 30);
        int x = getScreenWidth() / 2 - buttonWidth - 5;

        ButtonGeneric confirm = new ButtonGeneric(x, y, buttonWidth, 20,
            fi.dy.masa.malilib.util.StringUtils.translate("maikuaidoufu_masa_tool.button.confirm"));
        addButton(confirm, (button, mouseButton) ->
        {
            closeToParent();

            if (this.onConfirm != null)
            {
                this.onConfirm.run();
            }
        });

        ButtonGeneric cancel = new ButtonGeneric(x + buttonWidth + 10, y, buttonWidth, 20,
            fi.dy.masa.malilib.util.StringUtils.translate("maikuaidoufu_masa_tool.button.cancel"));
        addButton(cancel, (button, mouseButton) -> closeToParent());
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(ctx, mouseX, mouseY, partialTicks);

        int y = Math.max(55, getScreenHeight() / 2 - this.lines.size() * 10 - 20);

        for (String line : this.lines)
        {
            drawString(ctx, line, getScreenWidth() / 2 - getStringWidth(line) / 2, y, 0xFFFFFFFF);
            y += 12;
        }
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
