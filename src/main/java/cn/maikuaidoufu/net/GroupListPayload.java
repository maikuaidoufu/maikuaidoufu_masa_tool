package cn.maikuaidoufu.net;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.permissions.Feature;
import cn.maikuaidoufu.region.RegionGroup;
import cn.maikuaidoufu.region.RegionLimits;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The complete group list sent to one client, together with a bit mask of that
 * player's effective {@link Feature} permissions (see FeaturePermissions#mask).
 */
public record GroupListPayload(
    int protocolVersion,
    int permissions,
    List<RegionGroup> groups
) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<GroupListPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MasaToolMod.MOD_ID, "group_list"));

    public static final StreamCodec<FriendlyByteBuf, GroupListPayload> CODEC =
        StreamCodec.of(GroupListPayload::write, GroupListPayload::read);

    public GroupListPayload
    {
        groups = groups == null ? List.of() : List.copyOf(groups);
    }

    private static void write(FriendlyByteBuf buf, GroupListPayload payload)
    {
        buf.writeVarInt(payload.protocolVersion);
        buf.writeVarInt(payload.permissions);
        buf.writeVarInt(payload.groups.size());

        for (RegionGroup group : payload.groups)
        {
            RegionCodec.writeGroup(buf, group);
        }
    }

    private static GroupListPayload read(FriendlyByteBuf buf)
    {
        int protocol = buf.readVarInt();
        int permissions = buf.readVarInt();
        int count = buf.readVarInt();

        if (count < 0 || count > RegionLimits.MAX_GROUPS)
        {
            throw new DecoderException("Invalid group count: " + count);
        }

        List<RegionGroup> groups = new ArrayList<>(count);

        for (int i = 0; i < count; i++)
        {
            groups.add(RegionCodec.readGroup(buf));
        }

        return new GroupListPayload(protocol, permissions, groups);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
