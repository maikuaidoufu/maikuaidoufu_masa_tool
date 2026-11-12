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

/**
 * One chunk load-level change sent to a watching client.
 */
public record ChunkLevelEventPayload(ChunkLevelEvent event) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ChunkLevelEventPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MasaToolMod.MOD_ID, "chunk_level_event"));

    public static final StreamCodec<FriendlyByteBuf, ChunkLevelEventPayload> CODEC =
        StreamCodec.of(ChunkLevelEventPayload::write, ChunkLevelEventPayload::read);

    private static final int MAX_TEXT_BYTES = 512;
    private static final int MAX_DIMENSION_BYTES = RegionLimits.MAX_DIMENSION_LENGTH;
    private static final int MAX_STATUS_BYTES = 64;
    private static final int MAX_TICKETS = 32;

    public ChunkLevelEventPayload
    {
        event = event == null
            ? new ChunkLevelEvent("", 0, 0, 0, 0, "", "",
                List.of(), List.of(), List.of(), 0L)
            : event;
    }

    private static void write(FriendlyByteBuf buf, ChunkLevelEventPayload payload)
    {
        ChunkLevelEvent e = payload.event;
        buf.writeUtf(e.dimension(), MAX_DIMENSION_BYTES);
        buf.writeVarInt(e.chunkX());
        buf.writeVarInt(e.chunkZ());
        buf.writeVarInt(e.oldLevel());
        buf.writeVarInt(e.newLevel());
        buf.writeUtf(e.oldStatus(), MAX_STATUS_BYTES);
        buf.writeUtf(e.newStatus(), MAX_STATUS_BYTES);
        writeTickets(buf, e.tickets());
        writeTickets(buf, e.addedTickets());
        writeTickets(buf, e.removedTickets());
        buf.writeLong(e.gameTime());
    }

    private static ChunkLevelEventPayload read(FriendlyByteBuf buf)
    {
        String dimension = buf.readUtf(MAX_DIMENSION_BYTES);
        int x = buf.readVarInt();
        int z = buf.readVarInt();
        int oldLevel = buf.readVarInt();
        int newLevel = buf.readVarInt();
        String oldStatus = buf.readUtf(MAX_STATUS_BYTES);
        String newStatus = buf.readUtf(MAX_STATUS_BYTES);
        List<String> tickets = readTickets(buf);
        List<String> added = readTickets(buf);
        List<String> removed = readTickets(buf);
        long gameTime = buf.readLong();

        return new ChunkLevelEventPayload(new ChunkLevelEvent(
            dimension, x, z, oldLevel, newLevel, oldStatus, newStatus,
            tickets, added, removed, gameTime));
    }

    private static void writeTickets(FriendlyByteBuf buf, List<String> tickets)
    {
        buf.writeVarInt(tickets.size());

        for (String ticket : tickets)
        {
            buf.writeUtf(ticket, MAX_TEXT_BYTES);
        }
    }

    private static List<String> readTickets(FriendlyByteBuf buf)
    {
        int count = buf.readVarInt();

        if (count < 0 || count > MAX_TICKETS)
        {
            throw new DecoderException("Invalid chunk ticket count: " + count);
        }

        List<String> tickets = new ArrayList<>(count);

        for (int i = 0; i < count; i++)
        {
            tickets.add(buf.readUtf(MAX_TEXT_BYTES));
        }

        return tickets;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
