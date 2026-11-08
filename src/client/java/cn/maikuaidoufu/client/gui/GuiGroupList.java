package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.ClientGroupStore;
import cn.maikuaidoufu.client.Configs;
import cn.maikuaidoufu.client.GroupStoreListener;
import cn.maikuaidoufu.client.compat.LitematicaSupport;
import cn.maikuaidoufu.region.RegionGroup;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;

public final class GuiGroupList
    extends GuiListBase<RegionGroup, WidgetGroupEntry, WidgetGroupList>
    implements ISelectionListener<RegionGroup>, GroupStoreListener
{
    public GuiGroupList()
    {
        super(12, 64);
        setTitle(StringUtils.translate("maikuaidoufu_masa_tool.title"));
    }

    @Override
    protected int getBrowserWidth()
    {
        return getScreenWidth() - 24;
    }

    @Override
    protected int getBrowserHeight()
    {
        return getScreenHeight() - 88;
    }

    @Override
    public void initGui()
    {
        super.initGui();
        clearWidgets();
        clearButtons();
        GuiTabs.add(this, GuiTabs.Tab.GROUPS, 26);
        createTopButtons();
        setListPosition(12, 64);
        reCreateListWidget();
        getListWidget().refreshEntries();
    }

    private void createTopButtons()
    {
        int right = getScreenWidth() - 10;

        String importLabel = StringUtils.translate("maikuaidoufu_masa_tool.button.import");
        int importWidth = getStringWidth(importLabel) + 20;
        ButtonGeneric importButton = new HintButton(right - importWidth, 26, importWidth, 20, importLabel);
        boolean available = LitematicaSupport.isAvailable();
        boolean permitted = ClientGroupStore.canCreate();
        importButton.setEnabled(available && permitted);
        GuiHints.set(importButton, !available
            ? StringUtils.translate("maikuaidoufu_masa_tool.hover.need_litematica")
            : !ClientGroupStore.isConnected()
                ? StringUtils.translate("maikuaidoufu_masa_tool.hover.not_connected")
                : !permitted ? StringUtils.translate("maikuaidoufu_masa_tool.hover.no_permission") : "");
        addButton(importButton, (button, mouseButton) -> GuiBase.openGui(new GuiImportSource(this)));
        right -= importWidth + 4;

        ButtonOnOff highlight = new ButtonOnOff(right, 26, -1, false,
            "maikuaidoufu_masa_tool.button.render_highlights", Configs.RENDER_HIGHLIGHTS.getBooleanValue());
        highlight.setX(right - highlight.getWidth());
        addButton(highlight, (button, mouseButton) ->
        {
            Configs.RENDER_HIGHLIGHTS.setBooleanValue(!Configs.RENDER_HIGHLIGHTS.getBooleanValue());
            Configs.saveConfig();
            initGui();
        });
    }

    @Override
    protected WidgetGroupList createListWidget(int x, int y)
    {
        return new WidgetGroupList(x, y, getBrowserWidth(), getBrowserHeight(), this);
    }

    @Override
    protected ISelectionListener<RegionGroup> getSelectionListener()
    {
        return this;
    }

    @Override
    public void onSelectionChange(RegionGroup entry)
    {
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(ctx, mouseX, mouseY, partialTicks);

        String status;

        if (!ClientGroupStore.isConnected())
        {
            status = StringUtils.translate("maikuaidoufu_masa_tool.status.waiting_server");
        }
        else
        {
            status = StringUtils.translate("maikuaidoufu_masa_tool.status.group_count", getListWidget().getCurrentEntries().size());
        }

        drawString(ctx, status, 12, 51, 0xFFAAAAAA);

        if (ClientGroupStore.isConnected() && ClientGroupStore.groups().isEmpty())
        {
            String empty = StringUtils.translate("maikuaidoufu_masa_tool.status.no_groups");
            drawString(ctx, empty, getScreenWidth() / 2 - getStringWidth(empty) / 2, getScreenHeight() / 2, 0xFFAAAAAA);
        }
    }

    @Override
    public void onGroupsChanged()
    {
        initGui();
    }
}
