package cn.maikuaidoufu.client.gui;

import cn.maikuaidoufu.client.verifier.VerifierExport;
import cn.maikuaidoufu.client.verifier.VerifierExport.Category;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Second step of the verifier export: pick the categories, then write the file.
 */
public class GuiVerifierExport extends GuiBase
{
    private final SchematicVerifier verifier;
    private final String placementName;
    private final Map<Category, WidgetExportCheckbox> boxes = new EnumMap<>(Category.class);

    public GuiVerifierExport(SchematicVerifier verifier, String placementName, Screen parent)
    {
        this.verifier = verifier;
        this.placementName = placementName == null ? "" : placementName;
        this.setTitle(StringUtils.translate("maikuaidoufu_masa_tool.title.verifier_export"));
        this.setParent(parent);
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearWidgets();
        this.clearButtons();
        this.boxes.clear();

        boolean techutils = FabricLoader.getInstance().isModLoaded("techutils");
        int x = 20;
        int y = 40;

        for (Category category : Category.values())
        {
            if (category == Category.CONTAINERS && !techutils)
            {
                continue;
            }

            WidgetExportCheckbox box = new WidgetExportCheckbox(x, y, 260,
                StringUtils.translate(keyFor(category)), true);
            this.addWidget(box);
            this.boxes.put(category, box);
            y += 20;
        }

        int buttonY = this.getScreenHeight() - 34;
        this.addButton(new ButtonGeneric(x, buttonY, 90, 20,
                StringUtils.translate("maikuaidoufu_masa_tool.button.export")),
            (button, mouseButton) -> this.doExport());
        this.addButton(new ButtonGeneric(x + 96, buttonY, 90, 20,
                StringUtils.translate("maikuaidoufu_masa_tool.button.cancel")),
            (button, mouseButton) -> this.closeGui(true));
    }

    private void doExport()
    {
        Set<Category> selected = EnumSet.noneOf(Category.class);

        for (Map.Entry<Category, WidgetExportCheckbox> entry : this.boxes.entrySet())
        {
            if (entry.getValue().isChecked())
            {
                selected.add(entry.getKey());
            }
        }

        try
        {
            Path file = VerifierExport.export(this.verifier, this.placementName, selected);

            if (file == null)
            {
                message("maikuaidoufu_masa_tool.verifier.message.none");
            }
            else
            {
                message("maikuaidoufu_masa_tool.verifier.message.exported", file.toString());
            }
        }
        catch (Exception e)
        {
            message("maikuaidoufu_masa_tool.verifier.message.failed", e.getMessage());
        }

        this.closeGui(true);
    }

    private static void message(String key, Object... args)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null)
        {
            mc.player.sendSystemMessage(Component.translatable(key, args));
        }
    }

    private static String keyFor(Category category)
    {
        return switch (category)
        {
            case CONTAINERS -> "maikuaidoufu_masa_tool.verifier.category.containers";
            case MISSING -> "maikuaidoufu_masa_tool.verifier.category.missing";
            case EXTRA -> "maikuaidoufu_masa_tool.verifier.category.extra";
            case WRONG_BLOCK -> "maikuaidoufu_masa_tool.verifier.category.wrong_block";
            case WRONG_STATE -> "maikuaidoufu_masa_tool.verifier.category.wrong_state";
            case DIFF_BLOCK -> "maikuaidoufu_masa_tool.verifier.category.diff_block";
        };
    }
}
