package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.ClientActions;
import cn.maikuaidoufu.client.ClientGroupStore;
import cn.maikuaidoufu.region.RegionGroup;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.UUID;
import net.minecraft.client.gui.screens.Screen;

public final class GuiRenameGroup extends GuiBase
{
    private final UUID groupId;
    private final Screen parent;
    private GuiTextFieldGeneric field;

    public GuiRenameGroup(UUID groupId, Screen parent)
    {
        this.groupId = groupId;
        this.parent = parent;
        setParent(parent);
        setTitle(StringUtils.translate("maikuaidoufu_masa_tool.title.rename"));
    }

    @Override
    public void initGui()
    {
        super.initGui();
        RegionGroup group = ClientGroupStore.group(this.groupId);

        if (group == null || group.defaultGroup())
        {
            closeToParent();
            return;
        }

        this.field = new GuiTextFieldGeneric(120, 60, 260, 20, this.font);
        this.field.setValue(group.name());
        this.field.setMaxLengthWrapper(64);
        addTextField(this.field, field -> true);

        ButtonGeneric save = new ButtonGeneric(120, 95, 100, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.save"));
        addButton(save, (button, mouseButton) -> save(group));

        ButtonGeneric cancel = new ButtonGeneric(230, 95, 100, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.cancel"));
        addButton(cancel, (button, mouseButton) -> closeToParent());
    }

    private void save(RegionGroup group)
    {
        String name = this.field.getValue().trim();

        if (name.isEmpty())
        {
            addMessage(fi.dy.masa.malilib.gui.Message.MessageType.ERROR, "maikuaidoufu_masa_tool.message.invalid_name");
            return;
        }

        if (!ClientGroupStore.canEditGroup(group))
        {
            addMessage(fi.dy.masa.malilib.gui.Message.MessageType.ERROR, "maikuaidoufu_masa_tool.message.no_permission");
            return;
        }

        ClientActions.renameGroup(group.id(), name);
        closeToParent();
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(ctx, mouseX, mouseY, partialTicks);
        drawString(ctx, StringUtils.translate("maikuaidoufu_masa_tool.label.name"), 40, 66, 0xFFCCCCCC);
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
