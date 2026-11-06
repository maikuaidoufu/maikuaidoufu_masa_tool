package cn.maikuaidoufu.net;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.region.RegionLimits;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CreateGroupPayload(String name, String source, List<EntryDraft> entries)
    implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<CreateGroupPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MasaToolMod.MOD_ID, "create_group"));

    public static final StreamCodec<FriendlyByteBuf, CreateGroupPayload> CODEC =
        StreamCodec.of(CreateGroupPayload::write, CreateGroupPayload::read);

    public CreateGroupPayload
    {
        name = RegionLimits.cleanName(name, "Imported");
        source = RegionLimits.cleanSource(source);
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    private static void write(FriendlyByteBuf buf, CreateGroupPayload payload)
    {
        buf.writeUtf(payload.name, RegionLimits.MAX_NAME_LENGTH);
        buf.writeUtf(payload.source, RegionLimits.MAX_SOURCE_LENGTH);
        buf.writeVarInt(payload.entries.size());

        for (EntryDraft entry : payload.entries)
        {
            RegionCodec.writeDraft(buf, entry);
        }
    }

    private static CreateGroupPayload read(FriendlyByteBuf buf)
    {
        String name = buf.readUtf(RegionLimits.MAX_NAME_LENGTH);
        String source = buf.readUtf(RegionLimits.MAX_SOURCE_LENGTH);
        int count = buf.readVarInt();

        if (count < 0 || count > RegionLimits.MAX_ENTRIES_PER_GROUP)
        {
            throw new DecoderException("Invalid imported entry count: " + count);
        }

        List<EntryDraft> entries = new ArrayList<>(count);

        for (int i = 0; i < count; i++)
        {
            entries.add(RegionCodec.readDraft(buf));
        }

        return new CreateGroupPayload(name, source, entries);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
