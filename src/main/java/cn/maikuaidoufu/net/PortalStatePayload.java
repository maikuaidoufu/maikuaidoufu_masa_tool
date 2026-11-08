package cn.maikuaidoufu.net;

import cn.maikuaidoufu.MasaToolMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: whether portal logging is currently running.
 */
public record PortalStatePayload(boolean enabled) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PortalStatePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MasaToolMod.MOD_ID, "portal_state"));

    public static final StreamCodec<FriendlyByteBuf, PortalStatePayload> CODEC =
        StreamCodec.of((buf, payload) -> buf.writeBoolean(payload.enabled),
            buf -> new PortalStatePayload(buf.readBoolean()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
