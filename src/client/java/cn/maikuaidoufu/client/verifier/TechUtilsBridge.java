package cn.maikuaidoufu.client.verifier;

import cn.maikuaidoufu.MasaToolMod;
import dev.kikugie.techutils.feature.containerscan.verifier.BlockMismatchExtension;
import dev.kikugie.techutils.feature.containerscan.verifier.SchematicVerifierExtension;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.BlockMismatch;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Reads the container verification results that techutils adds to the Litematica
 * verifier. This class must only be loaded when techutils is present.
 */
public final class TechUtilsBridge
{
    private TechUtilsBridge()
    {
    }

    public static List<String> collect(SchematicVerifier verifier, boolean compress)
    {
        List<String> lines = new ArrayList<>();
        List<BlockMismatch> mismatches;

        try
        {
            mismatches = verifier.getMismatchOverviewFor(SchematicVerifierExtension.WRONG_INVENTORIES);
        }
        catch (Throwable t)
        {
            MasaToolMod.LOGGER.warn("Could not read container mismatches: {}", t.toString());
            return lines;
        }

        for (BlockMismatch mismatch : mismatches)
        {
            try
            {
                @SuppressWarnings("rawtypes")
                BlockMismatchExtension extension = (BlockMismatchExtension) mismatch;
                @SuppressWarnings("rawtypes")
                var pair = extension.getInventories$techutils();

                if (pair == null
                    || !(pair.getLeft() instanceof Container expected)
                    || !(pair.getRight() instanceof Container found)
                    || !(pair.getRight() instanceof net.minecraft.world.level.block.entity.BlockEntity blockEntity))
                {
                    continue;
                }

                List<String> body = compress ? compressedLines(expected, found) : slotLines(expected, found);

                if (!body.isEmpty())
                {
                    BlockPos pos = blockEntity.getBlockPos();
                    lines.add(String.format("(%d, %d, %d) 容器", pos.getX(), pos.getY(), pos.getZ()));
                    lines.addAll(body);
                }
            }
            catch (Throwable t)
            {
                MasaToolMod.LOGGER.warn("Could not export a container mismatch: {}", t.toString());
            }
        }

        return lines;
    }

    private static List<String> slotLines(Container expected, Container found)
    {
        List<String> lines = new ArrayList<>();
        int size = Math.min(expected.getContainerSize(), found.getContainerSize());

        for (int slot = 0; slot < size; slot++)
        {
            ItemStack want = expected.getItem(slot);
            ItemStack got = found.getItem(slot);

            if (want.getItem() != got.getItem() || want.getCount() != got.getCount())
            {
                lines.add(String.format("    槽位 %d: 期望 %s，实际 %s",
                    slot, VerifierExport.itemName(want), VerifierExport.itemName(got)));
            }
        }

        return lines;
    }

    private static List<String> compressedLines(Container expected, Container found)
    {
        Map<Item, Integer> want = counts(expected);
        Map<Item, Integer> got = counts(found);
        Set<Item> items = new LinkedHashSet<>(want.keySet());
        items.addAll(got.keySet());
        List<String> lines = new ArrayList<>();

        for (Item item : items)
        {
            int expectedCount = want.getOrDefault(item, 0);
            int foundCount = got.getOrDefault(item, 0);

            if (expectedCount != foundCount)
            {
                lines.add(String.format("    %s: 期望 x%d，实际 x%d",
                    VerifierExport.itemLabel(item), expectedCount, foundCount));
            }
        }

        return lines;
    }

    private static Map<Item, Integer> counts(Container container)
    {
        Map<Item, Integer> counts = new LinkedHashMap<>();

        for (int slot = 0; slot < container.getContainerSize(); slot++)
        {
            ItemStack stack = container.getItem(slot);

            if (!stack.isEmpty())
            {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }

        return counts;
    }
}
