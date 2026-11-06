package cn.maikuaidoufu.net;

import cn.maikuaidoufu.MasaToolMod;
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
 * Full server-authoritative group list. Sent on join, on request, and after
 * every mutation. Permission flags are per recipient.
 */
public record GroupListPayload(
    int protocolVersion,
    boolean canCreate,
    boolean canEditAll,
    boolean allowOwnerEdit,
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
        buf.writeBoolean(payload.canCreate);
        buf.writeBoolean(payload.canEditAll);
        buf.writeBoolean(payload.allowOwnerEdit);
        buf.writeVarInt(payload.groups.size());

        for (RegionGroup group : payload.groups)
        {
            RegionCodec.writeGroup(buf, group);
        }
    }

    private static GroupListPayload read(FriendlyByteBuf buf)
    {
        int protocol = buf.readVarInt();
        boolean canCreate = buf.readBoolean();
        boolean canEditAll = buf.readBoolean();
        boolean allowOwnerEdit = buf.readBoolean();
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

        return new GroupListPayload(protocol, canCreate, canEditAll, allowOwnerEdit, groups);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
