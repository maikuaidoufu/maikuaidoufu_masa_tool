package cn.maikuaidoufu.net;

import cn.maikuaidoufu.region.Box;
import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import cn.maikuaidoufu.region.RegionLimits;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public final class RegionCodec
{
    private RegionCodec()
    {
    }

    public static void writeUuid(FriendlyByteBuf buf, UUID uuid)
    {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static UUID readUuid(FriendlyByteBuf buf)
    {
        return new UUID(buf.readLong(), buf.readLong());
    }

    public static void writeBox(FriendlyByteBuf buf, Box box)
    {
        buf.writeInt(box.minX());
        buf.writeInt(box.minY());
        buf.writeInt(box.minZ());
        buf.writeInt(box.maxX());
        buf.writeInt(box.maxY());
        buf.writeInt(box.maxZ());
    }

    public static Box readBox(FriendlyByteBuf buf)
    {
        return new Box(
            buf.readInt(), buf.readInt(), buf.readInt(),
            buf.readInt(), buf.readInt(), buf.readInt()
        );
    }

    public static void writeEntry(FriendlyByteBuf buf, RegionEntry entry)
    {
        writeUuid(buf, entry.id());
        buf.writeUtf(entry.name(), RegionLimits.MAX_NAME_LENGTH);
        buf.writeUtf(entry.dimension(), RegionLimits.MAX_DIMENSION_LENGTH);
        writeBox(buf, entry.box());
        buf.writeBoolean(entry.enabled());
        writeUuid(buf, entry.owner() == null ? new UUID(0L, 0L) : entry.owner());
        buf.writeUtf(entry.ownerName(), RegionLimits.MAX_NAME_LENGTH);
        buf.writeLong(entry.createdAt());
    }

    public static RegionEntry readEntry(FriendlyByteBuf buf)
    {
        UUID id = readUuid(buf);
        String name = buf.readUtf(RegionLimits.MAX_NAME_LENGTH);
        String dimension = buf.readUtf(RegionLimits.MAX_DIMENSION_LENGTH);
        Box box = readBox(buf);
        boolean enabled = buf.readBoolean();
        UUID owner = readUuid(buf);
        String ownerName = buf.readUtf(RegionLimits.MAX_NAME_LENGTH);
        long createdAt = buf.readLong();
        return new RegionEntry(id, name, dimension, box, enabled, owner, ownerName, createdAt);
    }

    public static void writeDraft(FriendlyByteBuf buf, EntryDraft draft)
    {
        buf.writeUtf(draft.name(), RegionLimits.MAX_NAME_LENGTH);
        buf.writeUtf(draft.dimension(), RegionLimits.MAX_DIMENSION_LENGTH);
        writeBox(buf, draft.box());
        buf.writeBoolean(draft.enabled());
    }

    public static EntryDraft readDraft(FriendlyByteBuf buf)
    {
        String name = buf.readUtf(RegionLimits.MAX_NAME_LENGTH);
        String dimension = buf.readUtf(RegionLimits.MAX_DIMENSION_LENGTH);
        Box box = readBox(buf);
        boolean enabled = buf.readBoolean();
        return new EntryDraft(name, dimension, box, enabled);
    }

    public static void writeGroup(FriendlyByteBuf buf, RegionGroup group)
    {
        writeUuid(buf, group.id());
        buf.writeUtf(group.name(), RegionLimits.MAX_NAME_LENGTH);
        buf.writeBoolean(group.defaultGroup());
        buf.writeBoolean(group.enabled());
        buf.writeUtf(group.source(), RegionLimits.MAX_SOURCE_LENGTH);
        buf.writeBoolean(group.owner() != null);
        writeUuid(buf, group.owner() == null ? new UUID(0L, 0L) : group.owner());
        buf.writeUtf(group.ownerName(), RegionLimits.MAX_NAME_LENGTH);
        buf.writeLong(group.createdAt());
        buf.writeLong(group.updatedAt());
        buf.writeVarInt(group.entries().size());

        for (RegionEntry entry : group.entries())
        {
            writeEntry(buf, entry);
        }
    }

    public static RegionGroup readGroup(FriendlyByteBuf buf)
    {
        UUID id = readUuid(buf);
        String name = buf.readUtf(RegionLimits.MAX_NAME_LENGTH);
        boolean defaultGroup = buf.readBoolean();
        boolean enabled = buf.readBoolean();
        String source = buf.readUtf(RegionLimits.MAX_SOURCE_LENGTH);
        boolean hasOwner = buf.readBoolean();
        UUID ownerBits = readUuid(buf);
        UUID owner = hasOwner ? ownerBits : null;
        String ownerName = buf.readUtf(RegionLimits.MAX_NAME_LENGTH);
        long createdAt = buf.readLong();
        long updatedAt = buf.readLong();
        int count = buf.readVarInt();

        if (count < 0 || count > RegionLimits.MAX_ENTRIES_PER_GROUP)
        {
            throw new DecoderException("Invalid group entry count: " + count);
        }

        List<RegionEntry> entries = new ArrayList<>(count);

        for (int i = 0; i < count; i++)
        {
            entries.add(readEntry(buf));
        }

        return new RegionGroup(id, name, defaultGroup, enabled, source, owner, ownerName,
            createdAt, updatedAt, entries);
    }
}
