package cn.maikuaidoufu.client.verifier;

import cn.maikuaidoufu.MasaToolMod;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.BlockMismatch;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Collects the Litematica schematic verifier results and writes them to a text
 * file. Container contents come from the optional techutils integration.
 */
public final class VerifierExport
{
    public enum Category
    {
        CONTAINERS,
        MISSING,
        EXTRA,
        WRONG_BLOCK,
        WRONG_STATE
    }

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DIRECTORY = "maikuaidoufu_masa_tool-exports";

    private VerifierExport()
    {
    }

    /**
     * Writes the export file and returns it, or null when the selected
     * categories have no results.
     */
    public static Path export(SchematicVerifier verifier, String placementName, Set<Category> categories)
        throws IOException
    {
        Map<Category, List<String>> sections = new LinkedHashMap<>();

        for (Category category : Category.values())
        {
            if (!categories.contains(category))
            {
                continue;
            }

            List<String> lines = category == Category.CONTAINERS
                ? TechUtilsBridge.collect(verifier)
                : collectBlockMismatches(verifier, category);

            if (!lines.isEmpty())
            {
                sections.put(category, lines);
            }
        }

        if (sections.isEmpty())
        {
            return null;
        }

        StringBuilder out = new StringBuilder();
        out.append("# 原理图验证导出\n");
        out.append("# 投影: ").append(placementName == null ? "" : placementName).append('\n');
        out.append("# 时间: ").append(LocalDateTime.now().format(TIME_FORMAT)).append('\n');
        out.append("# 类别: ").append(joinNames(categories)).append("\n\n");

        int total = 0;

        for (Map.Entry<Category, List<String>> entry : sections.entrySet())
        {
            List<String> lines = entry.getValue();
            total += lines.size();
            int count = entry.getKey() == Category.CONTAINERS
                ? (int) lines.stream().filter(line -> line.startsWith("(")).count()
                : lines.size();
            out.append("[").append(categoryName(entry.getKey())).append("] 共 ")
                .append(count).append(" 条\n");

            for (String line : lines)
            {
                out.append(line).append('\n');
            }

            out.append('\n');
        }

        Path dir = FabricLoader.getInstance().getGameDir().resolve(DIRECTORY);
        Files.createDirectories(dir);
        Path file = dir.resolve("verifier-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".txt");
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        MasaToolMod.LOGGER.info("Exported {} verifier entries to {}", total, file);
        return file;
    }

    private static List<String> collectBlockMismatches(SchematicVerifier verifier, Category category)
    {
        List<String> lines = new ArrayList<>();
        Map<BlockPos, BlockMismatch> mismatches = blockMismatches(verifier);
        List<BlockPos> positions = new ArrayList<>(mismatches.keySet());
        positions.sort((a, b) ->
        {
            int compare = Integer.compare(a.getY(), b.getY());

            if (compare != 0)
            {
                return compare;
            }

            compare = Integer.compare(a.getX(), b.getX());
            return compare != 0 ? compare : Integer.compare(a.getZ(), b.getZ());
        });

        for (BlockPos pos : positions)
        {
            BlockMismatch mismatch = mismatches.get(pos);

            if (mismatch != null && categoryOf(mismatch.mismatchType) == category)
            {
                lines.add(String.format("(%d, %d, %d) 期望: %s，实际: %s",
                    pos.getX(), pos.getY(), pos.getZ(),
                    stateName(mismatch.stateExpected), stateName(mismatch.stateFound)));
            }
        }

        return lines;
    }

    @SuppressWarnings("unchecked")
    private static Map<BlockPos, BlockMismatch> blockMismatches(SchematicVerifier verifier)
    {
        try
        {
            Field field = SchematicVerifier.class.getDeclaredField("blockMismatches");
            field.setAccessible(true);
            return (Map<BlockPos, BlockMismatch>) field.get(verifier);
        }
        catch (ReflectiveOperationException e)
        {
            MasaToolMod.LOGGER.warn("Could not read verifier mismatches: {}", e.getMessage());
            return Map.of();
        }
    }

    private static Category categoryOf(SchematicVerifier.MismatchType type)
    {
        String name = type.name();

        return switch (name)
        {
            case "MISSING" -> Category.MISSING;
            case "EXTRA" -> Category.EXTRA;
            case "WRONG_BLOCK" -> Category.WRONG_BLOCK;
            case "WRONG_STATE" -> Category.WRONG_STATE;
            default -> null;
        };
    }

    static String stateName(BlockState state)
    {
        if (state == null)
        {
            return "未知";
        }

        return state.toString();
    }

    static String itemName(net.minecraft.world.item.ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return "空";
        }

        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id + " x" + stack.getCount();
    }

    static String categoryName(Category category)
    {
        return switch (category)
        {
            case CONTAINERS -> "容器物品不匹配";
            case MISSING -> "缺失方块";
            case EXTRA -> "多余方块";
            case WRONG_BLOCK -> "错误方块";
            case WRONG_STATE -> "错误状态";
        };
    }

    private static String joinNames(Set<Category> categories)
    {
        List<String> names = new ArrayList<>();

        for (Category category : EnumSet.copyOf(categories))
        {
            names.add(categoryName(category));
        }

        return String.join(", ", names);
    }
}
