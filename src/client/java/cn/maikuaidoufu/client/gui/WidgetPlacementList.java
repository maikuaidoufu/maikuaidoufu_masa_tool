package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.compat.PlacementInfo;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import java.util.Collection;
import java.util.List;

public final class WidgetPlacementList extends WidgetListBase<PlacementInfo, WidgetPlacementEntry>
{
    private final GuiPlacementPicker parent;
    private final List<PlacementInfo> placements;

    public WidgetPlacementList(int x, int y, int width, int height, GuiPlacementPicker parent,
                               List<PlacementInfo> placements)
    {
        super(x, y, width, height, parent);
        this.parent = parent;
        this.placements = placements;
    }

    public GuiPlacementPicker getParentGui()
    {
        return this.parent;
    }

    @Override
    protected Collection<PlacementInfo> getAllEntries()
    {
        return this.placements;
    }

    @Override
    protected int getBrowserEntryHeightFor(PlacementInfo entry)
    {
        return 24;
    }

    @Override
    protected WidgetPlacementEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, PlacementInfo entry)
    {
        return new WidgetPlacementEntry(x, y, this.browserEntryWidth, getBrowserEntryHeightFor(entry),
            isOdd, entry, listIndex, this);
    }

    @Override
    protected List<String> getEntryStringsForFilter(PlacementInfo entry)
    {
        return List.of(entry.name(), entry.source(), entry.dimension());
    }
}
