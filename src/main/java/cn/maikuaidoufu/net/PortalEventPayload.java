package cn.maikuaidoufu.net;

import cn.maikuaidoufu.MasaToolMod;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * One portal travel line. The prefix is added by the client, so the text starts at the position.
 */
public record PortalEventPayload(String text, int kind) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PortalEventPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MasaToolMod.MOD_ID, "portal_event"));

    public static final StreamCodec<FriendlyByteBuf, PortalEventPayload> CODEC =
        StreamCodec.of(PortalEventPayload::write, PortalEventPayload::read);

    private static final int MAX_TEXT_LENGTH = 512;

    public PortalEventPayload
    {
        text = text == null ? "" : text;

        if (text.length() > MAX_TEXT_LENGTH)
        {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }
    }

    private static void write(FriendlyByteBuf buf, PortalEventPayload payload)
    {
        buf.writeUtf(payload.text, MAX_TEXT_LENGTH);
        buf.writeVarInt(payload.kind);
    }

    private static PortalEventPayload read(FriendlyByteBuf buf)
    {
        String text = buf.readUtf(MAX_TEXT_LENGTH);
        int kind = buf.readVarInt();

        if (kind < 0 || kind > 2)
        {
            throw new DecoderException("Invalid portal event kind: " + kind);
        }

        return new PortalEventPayload(text, kind);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
