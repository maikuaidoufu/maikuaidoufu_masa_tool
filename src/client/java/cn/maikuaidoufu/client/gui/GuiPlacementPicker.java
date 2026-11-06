package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.ClientActions;
import cn.maikuaidoufu.client.compat.LitematicaSupport;
import cn.maikuaidoufu.client.compat.PlacementInfo;
import cn.maikuaidoufu.net.EntryDraft;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.ArrayList;
import java.util.List;

public final class GuiPlacementPicker
    extends GuiListBase<PlacementInfo, WidgetPlacementEntry, WidgetPlacementList>
    implements ISelectionListener<PlacementInfo>
{
    private final GuiImportSource parent;
    private final List<PlacementInfo> placements;

    public GuiPlacementPicker(GuiImportSource parent)
    {
        super(12, 64);
        this.parent = parent;
        this.placements = LitematicaSupport.placements();
        setParent(parent);
        setTitle(StringUtils.translate("maikuaidoufu_masa_tool.title.pick_placement"));
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
        setListPosition(12, 64);
        reCreateListWidget();
        getListWidget().refreshEntries();
    }

    @Override
    protected WidgetPlacementList createListWidget(int x, int y)
    {
        return new WidgetPlacementList(x, y, getBrowserWidth(), getBrowserHeight(), this, this.placements);
    }

    @Override
    protected ISelectionListener<PlacementInfo> getSelectionListener()
    {
        return this;
    }

    @Override
    public void onSelectionChange(PlacementInfo entry)
    {
    }

    public void select(PlacementInfo placement)
    {
        if (placement == null || placement.boxes().isEmpty())
        {
            return;
        }

        List<EntryDraft> drafts = new ArrayList<>();

        for (int i = 0; i < placement.boxes().size(); i++)
        {
            String name = i < placement.boxNames().size()
                ? placement.boxNames().get(i)
                : StringUtils.translate("maikuaidoufu_masa_tool.default_entry_name") + " " + (i + 1);
            drafts.add(new EntryDraft(name, placement.dimension(), placement.boxes().get(i), true));
        }

        String groupName = ClientActions.uniqueGroupName(placement.name());
        String source = StringUtils.translate("maikuaidoufu_masa_tool.source.projection",
            placement.source(), placement.name());
        ClientActions.createGroup(groupName, source, drafts);
        fi.dy.masa.malilib.gui.GuiBase.openGui(new GuiGroupList());
    }
}
