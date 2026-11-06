package cn.maikuaidoufu.net;

import cn.maikuaidoufu.MasaToolMod;
import io.netty.handler.codec.DecoderException;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EntryControlPayload(UUID groupId, UUID entryId, Action action, boolean enabled)
    implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<EntryControlPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MasaToolMod.MOD_ID, "entry_control"));

    public static final StreamCodec<FriendlyByteBuf, EntryControlPayload> CODEC =
        StreamCodec.of(EntryControlPayload::write, EntryControlPayload::read);

    public enum Action
    {
        SET_ENABLED,
        DELETE
    }

    private static void write(FriendlyByteBuf buf, EntryControlPayload payload)
    {
        RegionCodec.writeUuid(buf, payload.groupId);
        RegionCodec.writeUuid(buf, payload.entryId);
        buf.writeVarInt(payload.action.ordinal());
        buf.writeBoolean(payload.enabled);
    }

    private static EntryControlPayload read(FriendlyByteBuf buf)
    {
        UUID groupId = RegionCodec.readUuid(buf);
        UUID entryId = RegionCodec.readUuid(buf);
        int actionId = buf.readVarInt();

        if (actionId < 0 || actionId >= Action.values().length)
        {
            throw new DecoderException("Invalid entry action: " + actionId);
        }

        boolean enabled = buf.readBoolean();
        return new EntryControlPayload(groupId, entryId, Action.values()[actionId], enabled);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
