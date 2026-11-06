package cn.maikuaidoufu.client.compat;

import cn.maikuaidoufu.region.Box;
import java.util.List;

public record SelectionInfo(
    String name,
    String source,
    String dimension,
    List<Box> boxes,
    List<String> boxNames
)
{
    public SelectionInfo
    {
        boxes = boxes == null ? List.of() : List.copyOf(boxes);
        boxNames = boxNames == null ? List.of() : List.copyOf(boxNames);
    }
}
