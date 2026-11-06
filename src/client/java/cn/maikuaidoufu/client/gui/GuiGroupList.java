package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.ClientGroupStore;
import cn.maikuaidoufu.client.GroupStoreListener;
import cn.maikuaidoufu.client.compat.LitematicaSupport;
import cn.maikuaidoufu.region.RegionGroup;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
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
        createImportButton();
        setListPosition(12, 64);
        reCreateListWidget();
        getListWidget().refreshEntries();
    }

    private void createImportButton()
    {
        String label = StringUtils.translate("maikuaidoufu_masa_tool.button.import");
        int width = getStringWidth(label) + 20;
        ButtonGeneric button = new ButtonGeneric(getScreenWidth() - width - 10, 26, width, 20, label);
        boolean available = LitematicaSupport.isAvailable();
        boolean permitted = ClientGroupStore.canCreate();
        button.setEnabled(available && permitted);
        button.setHoverStrings(!available
            ? StringUtils.translate("maikuaidoufu_masa_tool.hover.need_litematica")
            : !ClientGroupStore.isConnected()
                ? StringUtils.translate("maikuaidoufu_masa_tool.hover.not_connected")
                : !permitted ? StringUtils.translate("maikuaidoufu_masa_tool.hover.no_permission") : "");
        addButton(button, (b, mouseButton) -> GuiBase.openGui(new GuiImportSource(this)));
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
