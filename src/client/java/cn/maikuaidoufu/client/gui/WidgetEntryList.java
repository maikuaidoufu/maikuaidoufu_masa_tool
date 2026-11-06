package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.ClientGroupStore;
import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import java.util.Collection;
import java.util.List;

public final class WidgetEntryList extends WidgetListBase<RegionEntry, WidgetEntry>
{
    private final GuiGroupDetail parent;

    public WidgetEntryList(int x, int y, int width, int height, GuiGroupDetail parent)
    {
        super(x, y, width, height, parent);
        this.parent = parent;
    }

    public GuiGroupDetail getParentGui()
    {
        return this.parent;
    }

    @Override
    protected Collection<RegionEntry> getAllEntries()
    {
        RegionGroup group = ClientGroupStore.group(this.parent.getGroupId());
        return group == null ? List.of() : group.entries();
    }

    @Override
    protected int getBrowserEntryHeightFor(RegionEntry entry)
    {
        return 24;
    }

    @Override
    protected WidgetEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, RegionEntry entry)
    {
        return new WidgetEntry(x, y, this.browserEntryWidth, getBrowserEntryHeightFor(entry),
            isOdd, entry, listIndex, this);
    }

    @Override
    protected List<String> getEntryStringsForFilter(RegionEntry entry)
    {
        return List.of(entry.name(), entry.dimension());
    }
}
