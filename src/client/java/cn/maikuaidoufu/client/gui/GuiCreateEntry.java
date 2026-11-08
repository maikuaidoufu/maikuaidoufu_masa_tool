package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.ClientActions;
import cn.maikuaidoufu.client.ClientGroupStore;
import cn.maikuaidoufu.region.Box;
import cn.maikuaidoufu.region.RegionGroup;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.GuiTextFieldInteger;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;

public final class GuiCreateEntry extends GuiBase
{
    private final UUID groupId;
    private final Screen parent;
    private final Box presetBox;
    private final String presetName;

    private GuiTextFieldGeneric nameField;
    private GuiTextFieldInteger x1;
    private GuiTextFieldInteger y1;
    private GuiTextFieldInteger z1;
    private GuiTextFieldInteger x2;
    private GuiTextFieldInteger y2;
    private GuiTextFieldInteger z2;

    public GuiCreateEntry(UUID groupId, Screen parent)
    {
        this(groupId, parent, null, null);
    }

    public GuiCreateEntry(UUID groupId, Screen parent, Box presetBox, String presetName)
    {
        this.groupId = groupId;
        this.parent = parent;
        this.presetBox = presetBox;
        this.presetName = presetName;
        setParent(parent);
        setTitle(StringUtils.translate("maikuaidoufu_masa_tool.title.create_entry"));
    }

    @Override
    public void initGui()
    {
        super.initGui();

        RegionGroup group = ClientGroupStore.group(this.groupId);

        if (group == null)
        {
            closeToParent();
            return;
        }

        BlockPos pos = currentPos();
        int corner1X = this.presetBox != null ? this.presetBox.minX() : pos.getX();
        int corner1Y = this.presetBox != null ? this.presetBox.minY() : pos.getY();
        int corner1Z = this.presetBox != null ? this.presetBox.minZ() : pos.getZ();
        int corner2X = this.presetBox != null ? this.presetBox.maxX() : pos.getX() + 1;
        int corner2Y = this.presetBox != null ? this.presetBox.maxY() : pos.getY() + 1;
        int corner2Z = this.presetBox != null ? this.presetBox.maxZ() : pos.getZ() + 1;

        String name = this.presetName == null || this.presetName.isBlank()
            ? StringUtils.translate("maikuaidoufu_masa_tool.default_entry_name")
            : this.presetName;

        if (name.length() > 64)
        {
            name = name.substring(0, 64);
        }

        this.nameField = new GuiTextFieldGeneric(110, 55, 260, 20, this.font);
        this.nameField.setMaxLengthWrapper(64);
        this.nameField.setValue(name);
        addTextField(this.nameField, field -> true);

        this.x1 = createIntegerField(110, 85, corner1X);
        this.y1 = createIntegerField(185, 85, corner1Y);
        this.z1 = createIntegerField(260, 85, corner1Z);
        this.x2 = createIntegerField(110, 115, corner2X);
        this.y2 = createIntegerField(185, 115, corner2Y);
        this.z2 = createIntegerField(260, 115, corner2Z);

        ButtonGeneric here1 = new ButtonGeneric(340, 85, -1, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.current_pos"));
        here1.setX(340);
        addButton(here1, (button, mouseButton) -> setCorner(this.x1, this.y1, this.z1, currentPos()));

        ButtonGeneric here2 = new ButtonGeneric(340, 115, -1, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.current_pos"));
        here2.setX(340);
        addButton(here2, (button, mouseButton) -> setCorner(this.x2, this.y2, this.z2, currentPos()));

        ButtonGeneric create = new ButtonGeneric(110, 155, 100, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.create"));
        addButton(create, (button, mouseButton) -> create(group));

        ButtonGeneric cancel = new ButtonGeneric(220, 155, 100, 20,
            StringUtils.translate("maikuaidoufu_masa_tool.button.cancel"));
        addButton(cancel, (button, mouseButton) -> closeToParent());
    }

    private GuiTextFieldInteger createIntegerField(int x, int y, int value)
    {
        GuiTextFieldInteger field = new GuiTextFieldInteger(x, y, 65, 20, this.font);
        field.setValue(Integer.toString(value));
        addTextField(field, f -> true);
        return field;
    }

    private static BlockPos currentPos()
    {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? BlockPos.ZERO : mc.player.blockPosition();
    }

    private void setCorner(GuiTextFieldInteger x, GuiTextFieldInteger y, GuiTextFieldInteger z, BlockPos pos)
    {
        x.setValue(Integer.toString(pos.getX()));
        y.setValue(Integer.toString(pos.getY()));
        z.setValue(Integer.toString(pos.getZ()));
    }

    private void create(RegionGroup group)
    {
        if (!ClientGroupStore.canCreateEntry(group))
        {
            addMessage(fi.dy.masa.malilib.gui.Message.MessageType.ERROR, "maikuaidoufu_masa_tool.message.no_permission");
            return;
        }

        try
        {
            int ix1 = Integer.parseInt(this.x1.getValue());
            int iy1 = Integer.parseInt(this.y1.getValue());
            int iz1 = Integer.parseInt(this.z1.getValue());
            int ix2 = Integer.parseInt(this.x2.getValue());
            int iy2 = Integer.parseInt(this.y2.getValue());
            int iz2 = Integer.parseInt(this.z2.getValue());
            Box box = new Box(ix1, iy1, iz1, ix2, iy2, iz2);

            if (!box.isValid())
            {
                addMessage(fi.dy.masa.malilib.gui.Message.MessageType.ERROR, "maikuaidoufu_masa_tool.message.invalid_region");
                return;
            }

            String name = this.nameField.getValue().trim();

            if (name.isEmpty())
            {
                name = StringUtils.translate("maikuaidoufu_masa_tool.default_entry_name");
            }

            Minecraft mc = Minecraft.getInstance();

            if (mc.level == null)
            {
                return;
            }

            String dimension = mc.level.dimension().identifier().toString();
            ClientActions.createEntry(group.id(), name, dimension, box, true);
            closeToParent();
        }
        catch (NumberFormatException e)
        {
            addMessage(fi.dy.masa.malilib.gui.Message.MessageType.ERROR, "maikuaidoufu_masa_tool.message.invalid_region");
        }
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(ctx, mouseX, mouseY, partialTicks);
        drawString(ctx, StringUtils.translate("maikuaidoufu_masa_tool.label.name"), 30, 61, 0xFFCCCCCC);
        drawString(ctx, StringUtils.translate("maikuaidoufu_masa_tool.label.corner1"), 30, 91, 0xFFCCCCCC);
        drawString(ctx, StringUtils.translate("maikuaidoufu_masa_tool.label.corner2"), 30, 121, 0xFFCCCCCC);
        drawString(ctx, "X", 105, 76, 0xFF888888);
        drawString(ctx, "Y", 180, 76, 0xFF888888);
        drawString(ctx, "Z", 255, 76, 0xFF888888);
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
