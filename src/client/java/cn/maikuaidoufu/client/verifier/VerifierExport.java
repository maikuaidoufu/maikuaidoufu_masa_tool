package cn.maikuaidoufu.client.verifier;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.client.compat.ReflectionSupport;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.BlockMismatch;
import fi.dy.masa.malilib.util.StringUtils;
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
import java.util.Optional;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Collects the Litematica schematic verifier results and writes them to a text
 * file. Container contents come from the optional techutils integration.
 *
 * <p>Litematica does not expose the per-position mismatch map publicly, so it
 * is read reflectively through {@link ReflectionSupport}. If a future version
 * renames the field, the export falls back to the public aggregated overview
 * instead of producing an empty file.</p>
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
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String DIRECTORY = "maikuaidoufu_masa_tool-exports";
    private static final String KEY_PREFIX = "maikuaidoufu_masa_tool.";

    private VerifierExport()
    {
    }

    /**
     * Writes the export file and returns it, or null when the selected
     * categories have no results.
     */
    public static Path export(SchematicVerifier verifier, String placementName, Set<Category> categories,
                               boolean compressContainers)
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
                ? TechUtilsSupport.collect(verifier, compressContainers)
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
        out.append(translate("export.header.title")).append('\n');
        out.append(translate("export.header.placement", placementName == null ? "" : placementName)).append('\n');
        out.append(translate("export.header.time", LocalDateTime.now().format(TIME_FORMAT))).append('\n');
        out.append(translate("export.header.categories", joinNames(categories))).append("\n\n");

        int total = 0;

        for (Map.Entry<Category, List<String>> entry : sections.entrySet())
        {
            List<String> lines = entry.getValue();
            total += lines.size();
            int count = entry.getKey() == Category.CONTAINERS
                ? (int) lines.stream().filter(line -> line.startsWith("(")).count()
                : lines.size();
            out.append(translate("export.section", categoryName(entry.getKey()), count)).append('\n');

            for (String line : lines)
            {
                out.append(line).append('\n');
            }

            out.append('\n');
        }

        Path dir = FabricLoader.getInstance().getGameDir().resolve(DIRECTORY);
        Files.createDirectories(dir);
        Path file = dir.resolve("verifier-" + LocalDateTime.now().format(FILE_FORMAT) + ".txt");
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        MasaToolMod.LOGGER.info("Exported {} verifier entries to {}", total, file);
        return file;
    }

    private static List<String> collectBlockMismatches(SchematicVerifier verifier, Category category)
    {
        Map<BlockPos, BlockMismatch> mismatches = blockMismatches(verifier);

        if (mismatches == null)
        {
            return collectOverview(verifier, category);
        }

        List<String> lines = new ArrayList<>();
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
                lines.add(translate("export.line.mismatch",
                    pos.getX(), pos.getY(), pos.getZ(),
                    stateName(mismatch.stateExpected), stateName(mismatch.stateFound)));
            }
        }

        return lines;
    }

    /**
     * Degraded path: the public overview has no coordinates, but still lists
     * every state pair and how often it occurs.
     */
    private static List<String> collectOverview(SchematicVerifier verifier, Category category)
    {
        SchematicVerifier.MismatchType type = mismatchTypeOf(category);

        if (type == null)
        {
            return List.of();
        }

        List<String> lines = new ArrayList<>();

        try
        {
            for (BlockMismatch mismatch : verifier.getMismatchOverviewFor(type))
            {
                if (mismatch != null)
                {
                    lines.add(translate("export.line.summary",
                        stateName(mismatch.stateExpected), stateName(mismatch.stateFound), mismatch.count));
                }
            }
        }
        catch (Throwable t)
        {
            MasaToolMod.LOGGER.warn("Could not read the verifier overview: {}", t.toString());
        }

        return lines;
    }

    @SuppressWarnings("unchecked")
    private static Map<BlockPos, BlockMismatch> blockMismatches(SchematicVerifier verifier)
    {
        Optional<Field> field = ReflectionSupport.field(SchematicVerifier.class, "blockMismatches");

        if (field.isEmpty())
        {
            return null;
        }

        return ReflectionSupport.read(field.get(), verifier, Map.class)
            .map(value -> (Map<BlockPos, BlockMismatch>) value)
            .orElse(null);
    }

    private static Category categoryOf(SchematicVerifier.MismatchType type)
    {
        if (type == null)
        {
            return null;
        }

        return switch (type.name())
        {
            case "MISSING" -> Category.MISSING;
            case "EXTRA" -> Category.EXTRA;
            case "WRONG_BLOCK" -> Category.WRONG_BLOCK;
            case "WRONG_STATE" -> Category.WRONG_STATE;
            default -> null;
        };
    }

    private static SchematicVerifier.MismatchType mismatchTypeOf(Category category)
    {
        return switch (category)
        {
            case MISSING -> SchematicVerifier.MismatchType.MISSING;
            case EXTRA -> SchematicVerifier.MismatchType.EXTRA;
            case WRONG_BLOCK -> SchematicVerifier.MismatchType.WRONG_BLOCK;
            case WRONG_STATE -> SchematicVerifier.MismatchType.WRONG_STATE;
            case CONTAINERS -> null;
        };
    }

    static String stateName(BlockState state)
    {
        return state == null ? translate("export.value.unknown") : state.toString();
    }

    static String itemName(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return translate("export.value.empty");
        }

        return translate("export.value.item", itemLabel(stack.getItem()), stack.getCount());
    }

    static String itemLabel(Item item)
    {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        String name = new ItemStack(item).getHoverName().getString();
        return name + " (" + id + ")";
    }

    static String categoryName(Category category)
    {
        String suffix = switch (category)
        {
            case CONTAINERS -> "containers";
            case MISSING -> "missing";
            case EXTRA -> "extra";
            case WRONG_BLOCK -> "wrong_block";
            case WRONG_STATE -> "wrong_state";
        };

        return translate("verifier.category." + suffix);
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

    private static String translate(String key, Object... args)
    {
        return StringUtils.translate(KEY_PREFIX + key, args);
    }
}
