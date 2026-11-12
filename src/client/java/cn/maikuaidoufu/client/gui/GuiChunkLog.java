package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.ClientActions;
import cn.maikuaidoufu.client.ClientChunkLog;
import cn.maikuaidoufu.net.ChunkWatchStatePayload;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

/**
 * Chunk load watcher tab: manage watched chunks and read level changes.
 */
public final class GuiChunkLog extends GuiBase
{
    private static final int LINE_HEIGHT = 10;
    private static final int PANEL_Y = 158;
    private static final int PANEL_PADDING = 6;
    private static final int WATCH_COLUMNS = 4;
    private static final int MAX_WATCH_BUTTONS = 8;

    private static final int COLOR_STRONGER = 0xFF55FF55;
    private static final int COLOR_WEAKER = 0xFFFFAA55;
    private static final int COLOR_OTHER = 0xFFC0C0C0;
    private static final int COLOR_HINT = 0xFF888888;

    private GuiTextFieldGeneric inputField;
    private String inputText = "";
    private int scroll;

    private List<DisplayLine> lines = List.of();
    private long cachedRevision = -1;
    private int cachedWidth = -1;

    public GuiChunkLog()
    {
        this.setTitle(StringUtils.translate("maikuaidoufu_masa_tool.chunk.title"));
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearWidgets();
        this.clearButtons();
        GuiTabs.add(this, GuiTabs.Tab.CHUNK, 26);

        boolean canToggle = ClientActions.canWatchChunks();
        ButtonOnOff toggle = new HintOnOff(12, 52, -1, false,
            "maikuaidoufu_masa_tool.chunk.toggle", ClientChunkLog.isEnabled());
        toggle.setEnabled(canToggle);
        GuiHints.set(toggle, canToggle
            ? ""
            : StringUtils.translate("maikuaidoufu_masa_tool.hover.not_connected"));
        this.addButton(toggle, (button, mouseButton) ->
        {
            boolean next = !ClientChunkLog.isEnabled();

            if (ClientActions.setChunkWatchEnabled(next))
            {
                ClientChunkLog.setEnabled(next);
                this.initGui();
            }
        });

        int rowY = 78;
        int x = 12;
        this.inputField = new GuiTextFieldGeneric(x, rowY, 80, 20, this.font);
        this.inputField.setMaxLengthWrapper(32);
        this.inputField.setValue(this.inputText);
        this.addTextField(this.inputField, field -> true);
        x += 84;

        ButtonGeneric add = new ButtonGeneric(x, rowY, -1, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.chunk.add"));
        x += add.getWidth() + 4;
        this.addButton(add, (button, mouseButton) ->
        {
            this.addFromInput();
            this.initGui();
        });

        ButtonGeneric current = new ButtonGeneric(x, rowY, -1, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.chunk.current"));
        x += current.getWidth() + 4;
        this.addButton(current, (button, mouseButton) ->
        {
            this.addCurrentChunk();
            this.initGui();
        });

        int right = this.getScreenWidth() - 10;
        ButtonGeneric clear = new ButtonGeneric(right, rowY, -1, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.chunk.clear"));
        clear.setX(right - clear.getWidth());
        this.addButton(clear, (button, mouseButton) -> ClientChunkLog.clear());

        List<ChunkWatchStatePayload.Entry> watches = ClientChunkLog.watches();
        int shown = Math.min(watches.size(), MAX_WATCH_BUTTONS);

        for (int i = 0; i < shown; i++)
        {
            ChunkWatchStatePayload.Entry entry = watches.get(i);
            int bx = 12 + (i % WATCH_COLUMNS) * 150;
            int by = 106 + (i / WATCH_COLUMNS) * 22;
            String label = shortDimension(entry.dimension())
                + " (" + entry.chunkX() + ", " + entry.chunkZ() + ")";
            ButtonGeneric watch = new ButtonGeneric(bx, by, 144, 20, label);
            this.addButton(watch, (button, mouseButton) ->
            {
                ClientActions.removeChunkWatch(entry.dimension(), entry.chunkX(), entry.chunkZ());
                this.initGui();
            });
        }

        if (watches.isEmpty())
        {
            this.addLabel(12, 110, 400, 12, COLOR_HINT,
                StringUtils.translate("maikuaidoufu_masa_tool.chunk.no_watch"));
        }
        else if (watches.size() > shown)
        {
            this.addLabel(12, 152, 400, 12, COLOR_HINT,
                StringUtils.translate("maikuaidoufu_masa_tool.chunk.more_watches",
                    watches.size() - shown));
        }
    }

    private void addFromInput()
    {
        if (this.inputField != null)
        {
            this.inputText = this.inputField.getValue();
        }

        String raw = this.inputText.trim();

        if (raw.isEmpty())
        {
            this.addCurrentChunk();
            return;
        }

        String[] parts = raw.split("[,\\s]+");

        if (parts.length < 2)
        {
            return;
        }

        try
        {
            int chunkX = Integer.parseInt(parts[0]);
            int chunkZ = Integer.parseInt(parts[1]);
            ClientActions.addChunkWatch(dimension(), chunkX, chunkZ);
            this.inputText = "";

            if (this.inputField != null)
            {
                this.inputField.setValue("");
            }
        }
        catch (NumberFormatException ignored)
        {
            // Ignore malformed coordinates.
        }
    }

    private void addCurrentChunk()
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null)
        {
            return;
        }

        int chunkX = mc.player.blockPosition().getX() >> 4;
        int chunkZ = mc.player.blockPosition().getZ() >> 4;
        ClientActions.addChunkWatch(dimension(), chunkX, chunkZ);
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
        int panelWidth = this.getScreenWidth() - 16;
        int panelHeight = this.getScreenHeight() - PANEL_Y - 10;
        RenderUtils.drawRect(ctx, panelX, PANEL_Y, panelWidth, panelHeight, 0xF0101010);

        if (this.inputField != null)
        {
            this.inputText = this.inputField.getValue();
        }

        this.rebuildLines(panelWidth - PANEL_PADDING * 2 - 4);

        int visible = Math.max(1, (panelHeight - PANEL_PADDING * 2) / LINE_HEIGHT);
        int total = this.lines.size();
        int maxScroll = Math.max(0, total - visible);
        this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));

        int end = total - this.scroll;
        int start = Math.max(0, end - visible);

        for (int i = start; i < end; i++)
        {
            DisplayLine line = this.lines.get(i);
            this.drawString(ctx, line.text(), panelX + PANEL_PADDING,
                PANEL_Y + PANEL_PADDING + (i - start) * LINE_HEIGHT, line.color());
        }

        if (total == 0)
        {
            String empty = StringUtils.translate("maikuaidoufu_masa_tool.chunk.empty");
            this.drawString(ctx, empty,
                panelX + panelWidth / 2 - this.getStringWidth(empty) / 2,
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
        long revision = ClientChunkLog.revision();

        if (width == this.cachedWidth && revision == this.cachedRevision)
        {
            return;
        }

        this.cachedWidth = width;
        this.cachedRevision = revision;

        List<DisplayLine> result = new ArrayList<>();

        for (ClientChunkLog.Entry entry : ClientChunkLog.entries())
        {
            int color = entry.direction() > 0
                ? COLOR_STRONGER
                : entry.direction() < 0 ? COLOR_WEAKER : COLOR_OTHER;

            for (String text : wrap(entry.text(), width))
            {
                result.add(new DisplayLine(text, color));
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

    private static String dimension()
    {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null
            ? "minecraft:overworld"
            : mc.level.dimension().identifier().toString();
    }

    private static String shortDimension(String id)
    {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    private record DisplayLine(String text, int color)
    {
    }
}
