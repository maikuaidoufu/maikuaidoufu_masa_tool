package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.ClientActions;
import cn.maikuaidoufu.client.ClientGroupStore;
import cn.maikuaidoufu.client.ClientHighlightStore;
import cn.maikuaidoufu.client.GroupStoreListener;
import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.screens.Screen;

public final class GuiGroupDetail
    extends GuiListBase<RegionEntry, WidgetEntry, WidgetEntryList>
    implements ISelectionListener<RegionEntry>, GroupStoreListener
{
    private final UUID groupId;
    private final Screen parent;

    public GuiGroupDetail(UUID groupId, Screen parent)
    {
        super(12, 64);
        this.groupId = groupId;
        this.parent = parent;
        setParent(parent);
        RegionGroup group = ClientGroupStore.group(groupId);
        setTitle(group == null
            ? StringUtils.translate("maikuaidoufu_masa_tool.title")
            : group.defaultGroup()
                ? StringUtils.translate("maikuaidoufu_masa_tool.group.default")
                : group.name());
    }

    public UUID getGroupId()
    {
        return this.groupId;
    }

    private RegionGroup group()
    {
        return ClientGroupStore.group(this.groupId);
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
        RegionGroup group = group();

        if (group == null)
        {
            GuiBase.openGui(new GuiGroupList());
            return;
        }

        super.initGui();
        clearWidgets();
        clearButtons();
        setTitle(group.defaultGroup()
            ? StringUtils.translate("maikuaidoufu_masa_tool.group.default")
            : group.name());
        createHeader(group);
        setListPosition(12, 64);
        reCreateListWidget();
        getListWidget().refreshEntries();
    }

    private void createHeader(RegionGroup group)
    {
        int y = 26;
        int x = 12;

        ButtonGeneric back = new ButtonGeneric(x, y, -1, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.back"));
        back.setX(x);
        addButton(back, (button, mouseButton) -> closeToParent());
        x = back.getX() + back.getWidth() + 4;

        if (!group.defaultGroup() && ClientGroupStore.canEditGroup(group))
        {
            ButtonGeneric rename = new ButtonGeneric(x, y, -1, 20,
                StringUtils.translate("maikuaidoufu_masa_tool.button.rename"));
            rename.setX(x);
            addButton(rename, (button, mouseButton) ->
                GuiBase.openGui(new GuiRenameGroup(group.id(), this)));
            x = rename.getX() + rename.getWidth() + 4;
        }

        boolean canMaster = ClientGroupStore.canEditGroup(group);
        ButtonOnOff master = new HintOnOff(x, y, -1, false,
            "maikuaidoufu_masa_tool.button.switch", group.enabled());
        master.setEnabled(canMaster);
        GuiHints.set(master, canMaster ? "" : StringUtils.translate("maikuaidoufu_masa_tool.hover.no_permission"));
        addButton(master, (button, mouseButton) ->
            ClientActions.setGroupEnabled(group.id(), !group.enabled()));
        x = master.getX() + master.getWidth() + 4;

        ButtonOnOff highlight = new ButtonOnOff(x, y, -1, false,
            "maikuaidoufu_masa_tool.button.highlight", ClientHighlightStore.groupOverride(group.id()));
        addButton(highlight, (button, mouseButton) ->
        {
            ClientHighlightStore.toggleGroup(group.id());
            initGui();
        });

        int right = getScreenWidth() - 10;

        boolean clear = group.defaultGroup();
        boolean canDelete = clear ? ClientGroupStore.canClearGroup(group) : ClientGroupStore.canDeleteGroup(group);
        ButtonGeneric delete = new HintButton(right, y, -1, 20,
            StringUtils.translate(clear ? "maikuaidoufu_masa_tool.button.clear" : "maikuaidoufu_masa_tool.button.delete"));
        delete.setX(right - delete.getWidth());
        delete.setEnabled(canDelete);
        GuiHints.set(delete, canDelete ? "" : StringUtils.translate("maikuaidoufu_masa_tool.hover.no_permission"));
        addButton(delete, (button, mouseButton) -> confirmDelete(group));
        right = delete.getX() - 3;

        boolean canAdd = ClientGroupStore.canCreateEntry(group);
        ButtonGeneric add = new HintButton(right, y, -1, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.create_entry"));
        add.setX(right - add.getWidth());
        add.setEnabled(canAdd);
        GuiHints.set(add, canAdd ? "" : StringUtils.translate("maikuaidoufu_masa_tool.hover.no_permission"));
        addButton(add, (button, mouseButton) ->
            GuiBase.openGui(new GuiCreateEntryMenu(group.id(), this)));
    }

    private void confirmDelete(RegionGroup group)
    {
        String title = StringUtils.translate(group.defaultGroup()
            ? "maikuaidoufu_masa_tool.confirm.clear_title"
            : "maikuaidoufu_masa_tool.confirm.delete_title");
        String message = StringUtils.translate(group.defaultGroup()
            ? "maikuaidoufu_masa_tool.confirm.clear_message"
            : "maikuaidoufu_masa_tool.confirm.delete_message", group.name(), group.entries().size());

        GuiBase.openGui(new GuiConfirm(title, List.of(message), () ->
        {
            if (group.defaultGroup())
            {
                ClientActions.clearGroup(group.id());
            }
            else
            {
                ClientActions.deleteGroup(group.id());
            }
        }, this));
    }

    @Override
    protected WidgetEntryList createListWidget(int x, int y)
    {
        return new WidgetEntryList(x, y, getBrowserWidth(), getBrowserHeight(), this);
    }

    @Override
    protected ISelectionListener<RegionEntry> getSelectionListener()
    {
        return this;
    }

    @Override
    public void onSelectionChange(RegionEntry entry)
    {
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(ctx, mouseX, mouseY, partialTicks);
        RegionGroup group = group();

        if (group != null)
        {
            String source = group.defaultGroup()
                ? StringUtils.translate("maikuaidoufu_masa_tool.source.manual")
                : StringUtils.translate("maikuaidoufu_masa_tool.label.source", group.source());
            drawString(ctx, source, 12, 50, 0xFF999999);
        }
    }

    @Override
    public void onGroupsChanged()
    {
        if (group() == null)
        {
            GuiBase.openGui(new GuiGroupList());
        }
        else
        {
            initGui();
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
