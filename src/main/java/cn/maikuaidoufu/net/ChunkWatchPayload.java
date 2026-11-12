package cn.maikuaidoufu.net;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.region.RegionLimits;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client request: toggle the subscription or add/remove a watched chunk.
 */
public record ChunkWatchPayload(Action action, boolean enabled, String dimension, int chunkX, int chunkZ)
    implements CustomPacketPayload
{
    public enum Action
    {
        TOGGLE,
        ADD,
        REMOVE
    }

    public static final CustomPacketPayload.Type<ChunkWatchPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MasaToolMod.MOD_ID, "chunk_watch"));

    public static final StreamCodec<FriendlyByteBuf, ChunkWatchPayload> CODEC =
        StreamCodec.of(ChunkWatchPayload::write, ChunkWatchPayload::read);

    private static final int MAX_DIMENSION_BYTES = RegionLimits.MAX_DIMENSION_LENGTH;

    public ChunkWatchPayload
    {
        action = action == null ? Action.TOGGLE : action;
        dimension = dimension == null ? "" : dimension;
    }

    private static void write(FriendlyByteBuf buf, ChunkWatchPayload payload)
    {
        buf.writeVarInt(payload.action().ordinal());
        buf.writeBoolean(payload.enabled());
        buf.writeUtf(payload.dimension(), MAX_DIMENSION_BYTES);
        buf.writeVarInt(payload.chunkX());
        buf.writeVarInt(payload.chunkZ());
    }

    private static ChunkWatchPayload read(FriendlyByteBuf buf)
    {
        int ordinal = buf.readVarInt();

        if (ordinal < 0 || ordinal >= Action.values().length)
        {
            throw new DecoderException("Invalid chunk watch action: " + ordinal);
        }

        return new ChunkWatchPayload(
            Action.values()[ordinal],
            buf.readBoolean(),
            buf.readUtf(MAX_DIMENSION_BYTES),
            buf.readVarInt(),
            buf.readVarInt()
        );
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
