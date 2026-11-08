package cn.maikuaidoufu.client.compat;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.region.Box;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.AreaSelectionSimple;
import fi.dy.masa.litematica.selection.SelectionManager;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Litematica 0.27.x implementation of the bridge.
 */
public final class LitematicaBridgeImpl implements LitematicaSupport.Bridge
{
    @Override
    public List<PlacementInfo> getPlacements()
    {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();

        if (manager == null)
        {
            return List.of();
        }

        List<PlacementInfo> result = new ArrayList<>();
        String dimension = currentDimension();

        for (SchematicPlacement placement : manager.getAllSchematicsPlacements())
        {
            try
            {
                PlacementInfo info = convertPlacement(placement, dimension);

                if (info != null)
                {
                    result.add(info);
                }
            }
            catch (Exception e)
            {
                MasaToolMod.LOGGER.warn("Skipping a placement that could not be read", e);
            }
        }

        result.sort(Comparator.comparing(PlacementInfo::name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static PlacementInfo convertPlacement(SchematicPlacement placement, String dimension)
    {
        Map<String, fi.dy.masa.litematica.selection.Box> regions =
            placement.getSubRegionBoxes(SubRegionPlacement.RequiredEnabled.ANY);

        List<Box> boxes = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (Map.Entry<String, fi.dy.masa.litematica.selection.Box> entry : regions.entrySet())
        {
            Box box = convert(entry.getValue());

            if (box != null)
            {
                boxes.add(box);
                names.add(entry.getKey());
            }
        }

        if (boxes.isEmpty())
        {
            Box enclosing = convert(placement.getEclosingBox());

            if (enclosing != null)
            {
                boxes.add(enclosing);
                names.add("区域");
            }
        }

        if (boxes.isEmpty())
        {
            return null;
        }

        String source = SyncmaticaSupport.isShared(placement) ? "共享" : "本地";
        return new PlacementInfo(placementName(placement), source, dimension, boxes, names);
    }

    @Override
    public SelectionInfo getSelection()
    {
        SelectionManager manager = DataManager.getSelectionManager();

        if (manager == null)
        {
            return null;
        }

        AreaSelection selection = manager.getCurrentSelection();

        if (selection == null)
        {
            return null;
        }

        List<Box> boxes = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (Map.Entry<String, fi.dy.masa.litematica.selection.Box> entry : selection.getAllSubRegions().entrySet())
        {
            if (isPlaceholder(selection, entry.getValue()))
            {
                continue;
            }

            Box box = convert(entry.getValue());

            if (box != null)
            {
                boxes.add(box);
                names.add(entry.getKey());
            }
        }

        if (boxes.isEmpty())
        {
            int index = 1;

            for (fi.dy.masa.litematica.selection.Box sourceBox : selection.getAllSubRegionBoxes())
            {
                if (isPlaceholder(selection, sourceBox))
                {
                    continue;
                }

                Box box = convert(sourceBox);

                if (box != null)
                {
                    boxes.add(box);
                    names.add("选区 " + index++);
                }
            }
        }

        if (boxes.isEmpty())
        {
            return null;
        }

        String name = selection.getName();

        if (name == null || name.isBlank())
        {
            name = "选区";
        }

        return new SelectionInfo(name, "选区", currentDimension(), boxes, names);
    }

    /**
     * The simple selection mode hands out one box at the world origin before the
     * player has picked any corner.
     */
    private static boolean isPlaceholder(AreaSelection selection, fi.dy.masa.litematica.selection.Box box)
    {
        if (!(selection instanceof AreaSelectionSimple) || box == null)
        {
            return false;
        }

        BlockPos pos1 = box.getPos1();
        BlockPos pos2 = box.getPos2();

        return pos1 != null && pos2 != null && pos1.equals(BlockPos.ZERO) && pos2.equals(BlockPos.ZERO);
    }

    private static Box convert(fi.dy.masa.litematica.selection.Box box)
    {
        if (box == null)
        {
            return null;
        }

        BlockPos pos1 = box.getPos1();
        BlockPos pos2 = box.getPos2();

        if (pos1 == null || pos2 == null)
        {
            return null;
        }

        return new Box(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    private static String placementName(SchematicPlacement placement)
    {
        Path file = placement.getSchematicFile();

        if (file != null && file.getFileName() != null)
        {
            String name = file.getFileName().toString();

            if (name.endsWith(".litematic"))
            {
                name = name.substring(0, name.length() - ".litematic".length());
            }

            if (!name.isBlank())
            {
                return name;
            }
        }

        String name = placement.getName();
        return name == null || name.isBlank() ? "投影" : name;
    }

    private static String currentDimension()
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null)
        {
            return "minecraft:overworld";
        }

        return mc.level.dimension().identifier().toString();
    }
}
