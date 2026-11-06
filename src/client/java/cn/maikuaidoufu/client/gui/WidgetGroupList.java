package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.ClientGroupStore;
import cn.maikuaidoufu.region.RegionGroup;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import java.util.Collection;
import java.util.List;

public final class WidgetGroupList extends WidgetListBase<RegionGroup, WidgetGroupEntry>
{
    private final GuiGroupList parent;

    public WidgetGroupList(int x, int y, int width, int height, GuiGroupList parent)
    {
        super(x, y, width, height, parent);
        this.parent = parent;
    }

    public GuiGroupList getParentGui()
    {
        return this.parent;
    }

    @Override
    protected Collection<RegionGroup> getAllEntries()
    {
        return ClientGroupStore.groups();
    }

    @Override
    protected int getBrowserEntryHeightFor(RegionGroup entry)
    {
        return 22;
    }

    @Override
    protected WidgetGroupEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, RegionGroup entry)
    {
        return new WidgetGroupEntry(x, y, this.browserEntryWidth, getBrowserEntryHeightFor(entry),
            isOdd, entry, listIndex, this);
    }

    @Override
    protected List<String> getEntryStringsForFilter(RegionGroup entry)
    {
        return List.of(entry.name(), entry.source());
    }
}
