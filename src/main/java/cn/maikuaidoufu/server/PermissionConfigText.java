package cn.maikuaidoufu.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes the permissions config as pretty-printed JSON with line and block
 * comments, and keeps hand-written comments when the file has to be
 * regenerated (e.g. a new feature key was added).
 *
 * <p>This class only deals with text: no Minecraft, Gson or config classes are
 * referenced, so the comment round-trip can be verified without a game
 * runtime.</p>
 */
final class PermissionConfigText
{
    /** Key used for comments that appear before the opening brace. */
    static final String HEADER = "";

    /** Key used for comments left after the last item. */
    static final String FOOTER = "\u0000footer";

    private static final Pattern KEY = Pattern.compile("^\"?([A-Za-z_][A-Za-z0-9_]*)\"?\\s*:");

    public record Item(String key, String value, String defaultNote)
    {
    }

    private PermissionConfigText()
    {
    }

    /**
     * Splits the existing file into comment blocks. A comment block belongs to
     * the first JSON key that follows it, so regenerating the file in a fixed
     * order still keeps each note next to the setting it describes.
     */
    static Map<String, List<String>> readNotes(Path file)
    {
        Map<String, List<String>> notes = new HashMap<>();

        if (file == null || !Files.isRegularFile(file))
        {
            return notes;
        }

        List<String> lines;

        try
        {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            return notes;
        }

        List<String> pending = new ArrayList<>();
        boolean inBlockComment = false;
        boolean beforeBrace = true;

        for (String rawLine : lines)
        {
            String raw = rawLine.replace("\uFEFF", "");
            String line = raw.trim();

            if (inBlockComment)
            {
                pending.add(raw.stripTrailing());
                if (line.contains("*/"))
                {
                    inBlockComment = false;
                }
                continue;
            }

            if (line.startsWith("/*"))
            {
                pending.add(raw.stripTrailing());
                inBlockComment = !line.contains("*/");
                continue;
            }

            if (line.startsWith("//"))
            {
                pending.add(raw.stripTrailing());
                continue;
            }

            if (beforeBrace && line.startsWith("{"))
            {
                if (!pending.isEmpty())
                {
                    notes.computeIfAbsent(HEADER, key -> new ArrayList<>()).addAll(pending);
                    pending.clear();
                }

                beforeBrace = false;
                line = line.substring(1).trim();

                if (line.isEmpty() || line.equals("}"))
                {
                    continue;
                }
            }

            Matcher matcher = KEY.matcher(line);

            if (matcher.find())
            {
                if (!pending.isEmpty())
                {
                    notes.computeIfAbsent(matcher.group(1), key -> new ArrayList<>()).addAll(pending);
                    pending.clear();
                }

                beforeBrace = false;
            }
        }

        if (!pending.isEmpty())
        {
            notes.put(FOOTER, new ArrayList<>(pending));
        }

        return notes;
    }

    /**
     * Renders a complete config. Hand-written comments from {@code notes} win
     * over the generated Chinese notes; only missing blocks are regenerated.
     */
    static String render(List<Item> items, List<String> defaultHeader, Map<String, List<String>> notes)
    {
        List<String> header = notes.getOrDefault(HEADER, List.of());

        if (header.isEmpty())
        {
            header = defaultHeader;
        }

        StringBuilder out = new StringBuilder();

        for (String line : header)
        {
            out.append(line).append('\n');
        }

        out.append("{\n");

        for (int i = 0; i < items.size(); i++)
        {
            Item item = items.get(i);
            List<String> itemNotes = notes.get(item.key());

            if (itemNotes == null || itemNotes.isEmpty())
            {
                out.append("  // ").append(item.defaultNote()).append('\n');
            }
            else
            {
                for (String line : itemNotes)
                {
                    out.append(line).append('\n');
                }
            }

            out.append("  \"").append(item.key()).append("\": ").append(item.value());

            if (i + 1 < items.size())
            {
                out.append(',');
            }

            out.append('\n');
        }

        for (String line : notes.getOrDefault(FOOTER, List.of()))
        {
            out.append(line).append('\n');
        }

        out.append("}\n");
        return out.toString();
    }
}
