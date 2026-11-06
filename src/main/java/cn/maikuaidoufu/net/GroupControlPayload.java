package cn.maikuaidoufu.net;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.region.RegionLimits;
import io.netty.handler.codec.DecoderException;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record GroupControlPayload(UUID groupId, Action action, String name, boolean enabled)
    implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<GroupControlPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MasaToolMod.MOD_ID, "group_control"));

    public static final StreamCodec<FriendlyByteBuf, GroupControlPayload> CODEC =
        StreamCodec.of(GroupControlPayload::write, GroupControlPayload::read);

    public GroupControlPayload
    {
        name = name == null ? "" : name;
    }

    public enum Action
    {
        SET_ENABLED,
        RENAME,
        DELETE,
        CLEAR
    }

    private static void write(FriendlyByteBuf buf, GroupControlPayload payload)
    {
        RegionCodec.writeUuid(buf, payload.groupId);
        buf.writeVarInt(payload.action.ordinal());
        buf.writeUtf(payload.name, RegionLimits.MAX_NAME_LENGTH);
        buf.writeBoolean(payload.enabled);
    }

    private static GroupControlPayload read(FriendlyByteBuf buf)
    {
        UUID groupId = RegionCodec.readUuid(buf);
        int actionId = buf.readVarInt();

        if (actionId < 0 || actionId >= Action.values().length)
        {
            throw new DecoderException("Invalid group action: " + actionId);
        }

        String name = buf.readUtf(RegionLimits.MAX_NAME_LENGTH);
        boolean enabled = buf.readBoolean();
        return new GroupControlPayload(groupId, Action.values()[actionId], name, enabled);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
