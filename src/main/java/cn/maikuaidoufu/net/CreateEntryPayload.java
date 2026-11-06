package cn.maikuaidoufu.net;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.region.Box;
import cn.maikuaidoufu.region.RegionLimits;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CreateEntryPayload(
    UUID groupId,
    String name,
    String dimension,
    Box box,
    boolean enabled
) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<CreateEntryPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MasaToolMod.MOD_ID, "create_entry"));

    public static final StreamCodec<FriendlyByteBuf, CreateEntryPayload> CODEC =
        StreamCodec.of(CreateEntryPayload::write, CreateEntryPayload::read);

    public CreateEntryPayload
    {
        name = RegionLimits.cleanName(name, "Sub-region");
        dimension = dimension == null ? "" : dimension;
        box = box == null ? new Box(0, 0, 0, 0, 0, 0) : box;
    }

    private static void write(FriendlyByteBuf buf, CreateEntryPayload payload)
    {
        RegionCodec.writeUuid(buf, payload.groupId);
        buf.writeUtf(payload.name, RegionLimits.MAX_NAME_LENGTH);
        buf.writeUtf(payload.dimension, RegionLimits.MAX_DIMENSION_LENGTH);
        RegionCodec.writeBox(buf, payload.box);
        buf.writeBoolean(payload.enabled);
    }

    private static CreateEntryPayload read(FriendlyByteBuf buf)
    {
        return new CreateEntryPayload(
            RegionCodec.readUuid(buf),
            buf.readUtf(RegionLimits.MAX_NAME_LENGTH),
            buf.readUtf(RegionLimits.MAX_DIMENSION_LENGTH),
            RegionCodec.readBox(buf),
            buf.readBoolean()
        );
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
