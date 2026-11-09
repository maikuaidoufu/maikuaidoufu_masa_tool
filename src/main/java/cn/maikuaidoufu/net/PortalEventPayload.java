package cn.maikuaidoufu.net;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.region.RegionLimits;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * One language-neutral portal travel event. The client localises it with its
 * own language file and adds the category prefix before display.
 */
public record PortalEventPayload(PortalEvent event) implements CustomPacketPayload
{
    // v2 uses a different channel id: older clients never declare it, so the
    // server simply skips them instead of sending an undecodable payload.
    public static final CustomPacketPayload.Type<PortalEventPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MasaToolMod.MOD_ID, "portal_event_v2"));

    public static final StreamCodec<FriendlyByteBuf, PortalEventPayload> CODEC =
        StreamCodec.of(PortalEventPayload::write, PortalEventPayload::read);

    private static final int MAX_TEXT_BYTES = 1024;
    private static final int MAX_DIMENSION_BYTES = RegionLimits.MAX_DIMENSION_LENGTH;

    public PortalEventPayload
    {
        event = event == null ? PortalEvent.suppressed(0) : event;
    }

    private static void write(FriendlyByteBuf buf, PortalEventPayload payload)
    {
        PortalEvent e = payload.event;
        buf.writeVarInt(e.kind());
        writeUtf(buf, e.entityName());
        writeUtf(buf, e.entityType());
        buf.writeVarInt(e.itemCount());
        buf.writeBoolean(e.customNamed());
        buf.writeUtf(e.fromDimension(), MAX_DIMENSION_BYTES);
        writeUtf(buf, e.fromPos());
        buf.writeUtf(e.toDimension(), MAX_DIMENSION_BYTES);
        writeUtf(buf, e.toPos());
        writeUtf(buf, e.entryPos());
        writeUtf(buf, e.portalPos());
        writeUtf(buf, e.portalSize());
        buf.writeVarInt(e.radius());
        buf.writeDouble(e.idealDistance());
        buf.writeBoolean(e.created());
        buf.writeBoolean(e.searched());
        writeUtf(buf, e.failure());
        buf.writeVarInt(e.droppedLines());
    }

    private static PortalEventPayload read(FriendlyByteBuf buf)
    {
        int kind = buf.readVarInt();

        if (kind < 0 || kind > 2)
        {
            throw new DecoderException("Invalid portal event kind: " + kind);
        }

        PortalEvent event = new PortalEvent(
            kind,
            buf.readUtf(MAX_TEXT_BYTES),
            buf.readUtf(MAX_TEXT_BYTES),
            buf.readVarInt(),
            buf.readBoolean(),
            buf.readUtf(MAX_DIMENSION_BYTES),
            buf.readUtf(MAX_TEXT_BYTES),
            buf.readUtf(MAX_DIMENSION_BYTES),
            buf.readUtf(MAX_TEXT_BYTES),
            buf.readUtf(MAX_TEXT_BYTES),
            buf.readUtf(MAX_TEXT_BYTES),
            buf.readUtf(MAX_TEXT_BYTES),
            buf.readVarInt(),
            buf.readDouble(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readUtf(MAX_TEXT_BYTES),
            buf.readVarInt()
        );

        return new PortalEventPayload(event);
    }

    private static void writeUtf(FriendlyByteBuf buf, String value)
    {
        buf.writeUtf(value, MAX_TEXT_BYTES);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
