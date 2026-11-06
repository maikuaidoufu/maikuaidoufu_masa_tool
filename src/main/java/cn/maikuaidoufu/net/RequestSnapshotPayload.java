package cn.maikuaidoufu.net;

import cn.maikuaidoufu.MasaToolMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestSnapshotPayload() implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<RequestSnapshotPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MasaToolMod.MOD_ID, "request_snapshot"));

    public static final StreamCodec<FriendlyByteBuf, RequestSnapshotPayload> CODEC =
        StreamCodec.unit(new RequestSnapshotPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
