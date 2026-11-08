package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.ClientActions;
import cn.maikuaidoufu.client.ClientGroupStore;
import cn.maikuaidoufu.client.ClientPortalLog;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GuiPortalLog extends GuiBase
{
    private static final int LINE_HEIGHT = 10;
    private static final int PANEL_Y = 104;
    private static final int PANEL_PADDING = 6;

    private static final int COLOR_NEW = 0xFF55FF55;
    private static final int COLOR_LINK = 0xFFFF5555;
    private static final int COLOR_OTHER = 0xFFFFFF55;
    private static final int COLOR_TEXT = 0xFFC0C0C0;
    private static final int COLOR_HINT = 0xFF888888;

    private GuiTextFieldGeneric filterField;
    private String filterText = "";
    private int scroll;

    private List<DisplayLine> lines = List.of();
    private long cachedRevision = -1;
    private String cachedFilter;
    private int cachedWidth = -1;

    public GuiPortalLog()
    {
        setTitle(StringUtils.translate("maikuaidoufu_masa_tool.title.portal"));
    }

    @Override
    public void initGui()
    {
        super.initGui();
        GuiTabs.add(this, GuiTabs.Tab.PORTAL, 26);

        int y = 52;
        int right = getScreenWidth() - 10;

        ButtonGeneric clear = new ButtonGeneric(right, y, -1, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.portal_clear"));
        clear.setX(right - clear.getWidth());
        addButton(clear, (button, mouseButton) -> ClientPortalLog.clear());
        right = clear.getX() - 6;

        boolean canToggle = ClientGroupStore.isConnected() && ClientGroupStore.canEditAll();
        ButtonOnOff toggle = new HintOnOff(12, y, -1, false,
            "maikuaidoufu_masa_tool.button.portal_toggle", ClientPortalLog.isEnabled());
        toggle.setEnabled(canToggle);
        GuiHints.set(toggle, canToggle ? ""
            : !ClientGroupStore.isConnected()
                ? StringUtils.translate("maikuaidoufu_masa_tool.hover.not_connected")
                : StringUtils.translate("maikuaidoufu_masa_tool.hover.no_permission"));
        addButton(toggle, (button, mouseButton) ->
            ClientActions.setPortalLogEnabled(!ClientPortalLog.isEnabled()));

        int filterX = toggle.getX() + toggle.getWidth() + 6;
        int filterWidth = Math.max(60, right - filterX);
        this.filterField = new GuiTextFieldGeneric(filterX, y, filterWidth, 20, this.font);
        this.filterField.setMaxLengthWrapper(64);
        this.filterField.setValue(this.filterText);
        addTextField(this.filterField, field -> true);

        int presetY = y + 26;
        int presetX = 12;

        presetX = addPreset(presetX, presetY, "maikuaidoufu_masa_tool.button.portal_all", "");
        presetX = addPreset(presetX, presetY, "maikuaidoufu_masa_tool.button.portal_new",
            ClientPortalLog.prefix(ClientPortalLog.KIND_NEW));
        addPreset(presetX, presetY, "maikuaidoufu_masa_tool.button.portal_link",
            ClientPortalLog.prefix(ClientPortalLog.KIND_LINK));
    }

    private int addPreset(int x, int y, String labelKey, String filter)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, StringUtils.translate(labelKey));
        button.setX(x);
        addButton(button, (b, mouseButton) -> setFilter(filter));
        return button.getX() + button.getWidth() + 4;
    }

    private void setFilter(String value)
    {
        this.filterText = value;
        this.scroll = 0;

        if (this.filterField != null)
        {
            this.filterField.setValue(value);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (scrollY != 0)
        {
            this.scroll = Math.max(0, this.scroll + (scrollY > 0 ? 3 : -3));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(ctx, mouseX, mouseY, partialTicks);

        int panelX = 8;
        int panelWidth = getScreenWidth() - 16;
        int panelHeight = getScreenHeight() - PANEL_Y - 10;
        RenderUtils.drawRect(ctx, panelX, PANEL_Y, panelWidth, panelHeight, 0xF0101010);

        if (this.filterField != null)
        {
            this.filterText = this.filterField.getValue();
        }

        rebuildLines(panelWidth - PANEL_PADDING * 2 - 4);

        int visible = Math.max(1, (panelHeight - PANEL_PADDING * 2) / LINE_HEIGHT);
        int total = this.lines.size();
        int maxScroll = Math.max(0, total - visible);
        this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));

        int end = total - this.scroll;
        int start = Math.max(0, end - visible);

        for (int i = start; i < end; i++)
        {
            DisplayLine line = this.lines.get(i);
            int x = panelX + PANEL_PADDING;
            int y = PANEL_Y + PANEL_PADDING + (i - start) * LINE_HEIGHT;

            if (!line.prefix().isEmpty())
            {
                drawString(ctx, line.prefix(), x, y, line.color());
                x += getStringWidth(line.prefix());
            }

            drawString(ctx, line.rest(), x, y, COLOR_TEXT);
        }

        if (total == 0)
        {
            String empty = ClientPortalLog.isEmpty()
                ? StringUtils.translate("maikuaidoufu_masa_tool.portal.empty")
                : StringUtils.translate("maikuaidoufu_masa_tool.portal.no_match");
            drawString(ctx, empty,
                panelX + panelWidth / 2 - getStringWidth(empty) / 2,
                PANEL_Y + panelHeight / 2 - 4, COLOR_HINT);
        }

        if (maxScroll > 0)
        {
            int barX = panelX + panelWidth - 4;
            int barY = PANEL_Y + PANEL_PADDING;
            int barHeight = panelHeight - PANEL_PADDING * 2;
            int thumbHeight = Math.max(10, barHeight * visible / total);
            int thumbY = barY + (barHeight - thumbHeight) * (maxScroll - this.scroll) / maxScroll;
            RenderUtils.drawRect(ctx, barX, barY, 2, barHeight, 0x30FFFFFF);
            RenderUtils.drawRect(ctx, barX, thumbY, 2, thumbHeight, 0x80FFFFFF);
        }
    }

    private void rebuildLines(int width)
    {
        long revision = ClientPortalLog.revision();

        if (width == this.cachedWidth && revision == this.cachedRevision
            && this.filterText.equals(this.cachedFilter))
        {
            return;
        }

        this.cachedWidth = width;
        this.cachedRevision = revision;
        this.cachedFilter = this.filterText;

        String filter = this.filterText.trim().toLowerCase(Locale.ROOT);
        List<DisplayLine> result = new ArrayList<>();

        for (ClientPortalLog.Entry entry : ClientPortalLog.entries())
        {
            String text = ClientPortalLog.prefix(entry.kind()) + " " + entry.text();

            if (!filter.isEmpty() && !text.toLowerCase(Locale.ROOT).contains(filter))
            {
                continue;
            }

            List<String> wrapped = wrap(text, width);
            int color = colorOf(entry.kind());

            for (int i = 0; i < wrapped.size(); i++)
            {
                String visual = wrapped.get(i);

                if (i == 0)
                {
                    int space = visual.indexOf(' ');

                    if (space > 0)
                    {
                        result.add(new DisplayLine(visual.substring(0, space), visual.substring(space + 1), color));
                    }
                    else
                    {
                        result.add(new DisplayLine("", visual, color));
                    }
                }
                else
                {
                    result.add(new DisplayLine("", visual, COLOR_TEXT));
                }
            }
        }

        this.lines = result;
    }

    private List<String> wrap(String text, int maxWidth)
    {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int width = 0;

        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            int charWidth = this.font.width(String.valueOf(c));

            if (width + charWidth > maxWidth && current.length() > 0)
            {
                result.add(current.toString());
                current.setLength(0);
                width = 0;
            }

            current.append(c);
            width += charWidth;
        }

        if (current.length() > 0 || result.isEmpty())
        {
            result.add(current.toString());
        }

        return result;
    }

    private static int colorOf(int kind)
    {
        return switch (kind)
        {
            case ClientPortalLog.KIND_NEW -> COLOR_NEW;
            case ClientPortalLog.KIND_LINK -> COLOR_LINK;
            default -> COLOR_OTHER;
        };
    }

    private record DisplayLine(String prefix, String rest, int color)
    {
    }
}
