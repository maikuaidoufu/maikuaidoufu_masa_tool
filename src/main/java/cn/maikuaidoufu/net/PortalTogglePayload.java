package cn.maikuaidoufu.net;

import cn.maikuaidoufu.MasaToolMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: turn portal logging on or off.
 */
public record PortalTogglePayload(boolean enabled) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PortalTogglePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MasaToolMod.MOD_ID, "portal_toggle"));

    public static final StreamCodec<FriendlyByteBuf, PortalTogglePayload> CODEC =
        StreamCodec.of((buf, payload) -> buf.writeBoolean(payload.enabled),
            buf -> new PortalTogglePayload(buf.readBoolean()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
