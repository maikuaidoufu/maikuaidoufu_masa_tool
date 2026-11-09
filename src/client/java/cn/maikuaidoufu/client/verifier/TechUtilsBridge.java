package cn.maikuaidoufu.client.verifier;

import cn.maikuaidoufu.MasaToolMod;
import dev.kikugie.techutils.feature.containerscan.verifier.BlockMismatchExtension;
import dev.kikugie.techutils.feature.containerscan.verifier.SchematicVerifierExtension;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.BlockMismatch;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.MismatchType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
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

    public static List<String> collect(SchematicVerifier verifier)
    {
        List<String> lines = new ArrayList<>();
        MismatchType type = SchematicVerifierExtension.WRONG_INVENTORIES;

        List<BlockMismatch> mismatches;

        try
        {
            mismatches = verifier.getMismatchOverviewFor(type);
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

                List<String> slotLines = new ArrayList<>();
                int size = Math.min(expected.getContainerSize(), found.getContainerSize());

                for (int slot = 0; slot < size; slot++)
                {
                    ItemStack want = expected.getItem(slot);
                    ItemStack got = found.getItem(slot);

                    if (want.getItem() != got.getItem() || want.getCount() != got.getCount())
                    {
                        slotLines.add(String.format("    槽位 %d: 期望 %s，实际 %s",
                            slot, VerifierExport.itemName(want), VerifierExport.itemName(got)));
                    }
                }

                if (!slotLines.isEmpty())
                {
                    BlockPos pos = blockEntity.getBlockPos();
                    lines.add(String.format("(%d, %d, %d) 容器",
                        pos.getX(), pos.getY(), pos.getZ()));
                    lines.addAll(slotLines);
                }
            }
            catch (Throwable t)
            {
                MasaToolMod.LOGGER.warn("Could not export a container mismatch: {}", t.toString());
            }
        }

        return lines;
    }


}
