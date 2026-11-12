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
 * Full watch state of one client, sent after every change and on join.
 */
public record ChunkWatchStatePayload(boolean enabled, List<Entry> watches) implements CustomPacketPayload
{
    public record Entry(String dimension, int chunkX, int chunkZ)
    {
    }

    public static final int MAX_WATCHES = 64;

    public static final CustomPacketPayload.Type<ChunkWatchStatePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MasaToolMod.MOD_ID, "chunk_watch_state"));

    public static final StreamCodec<FriendlyByteBuf, ChunkWatchStatePayload> CODEC =
        StreamCodec.of(ChunkWatchStatePayload::write, ChunkWatchStatePayload::read);

    private static final int MAX_DIMENSION_BYTES = RegionLimits.MAX_DIMENSION_LENGTH;

    public ChunkWatchStatePayload
    {
        watches = watches == null ? List.of() : List.copyOf(watches);
    }

    private static void write(FriendlyByteBuf buf, ChunkWatchStatePayload payload)
    {
        buf.writeBoolean(payload.enabled());
        buf.writeVarInt(payload.watches().size());

        for (Entry entry : payload.watches())
        {
            buf.writeUtf(entry.dimension(), MAX_DIMENSION_BYTES);
            buf.writeVarInt(entry.chunkX());
            buf.writeVarInt(entry.chunkZ());
        }
    }

    private static ChunkWatchStatePayload read(FriendlyByteBuf buf)
    {
        boolean enabled = buf.readBoolean();
        int count = buf.readVarInt();

        if (count < 0 || count > MAX_WATCHES)
        {
            throw new DecoderException("Invalid chunk watch count: " + count);
        }

        List<Entry> watches = new ArrayList<>(count);

        for (int i = 0; i < count; i++)
        {
            watches.add(new Entry(
                buf.readUtf(MAX_DIMENSION_BYTES),
                buf.readVarInt(),
                buf.readVarInt()
            ));
        }

        return new ChunkWatchStatePayload(enabled, watches);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
