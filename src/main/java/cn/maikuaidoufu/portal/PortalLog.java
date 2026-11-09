package cn.maikuaidoufu.portal;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.net.PortalEvent;
import cn.maikuaidoufu.net.PortalEventPayload;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.BlockUtil.FoundRectangle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Captures nether portal travels for the players that subscribed to the terminal.
 *
 * <p>Only language-neutral data is collected here. The client renders each
 * event with its own language file, so the server never has to know which
 * language a subscriber uses.</p>
 */
public final class PortalLog
{
    public static final int KIND_NEW = 0;
    public static final int KIND_LINK = 1;
    public static final int KIND_OTHER = 2;

    private static final int MAX_MESSAGES_PER_TICK = 20;

    private static final Set<UUID> SUBSCRIBERS = new HashSet<>();

    private static final ThreadLocal<TravelCtx> CURRENT = new ThreadLocal<>();

    private static long budgetTick = Long.MIN_VALUE;
    private static int sentThisTick;
    private static int suppressed;

    private PortalLog()
    {
    }

    /**
     * Nobody subscribed means nothing is captured at all.
     */
    public static boolean isEnabled()
    {
        return !SUBSCRIBERS.isEmpty();
    }

    public static void setSubscribed(ServerPlayer player, boolean subscribed)
    {
        if (player == null)
        {
            return;
        }

        if (subscribed)
        {
            SUBSCRIBERS.add(player.getUUID());
            return;
        }

        SUBSCRIBERS.remove(player.getUUID());
        clearTransientState();
    }

    public static void unsubscribe(ServerPlayer player)
    {
        if (player != null && SUBSCRIBERS.remove(player.getUUID()))
        {
            clearTransientState();
        }
    }

    public static void reset()
    {
        SUBSCRIBERS.clear();
        clearTransientState();
    }

    private static void clearTransientState()
    {
        if (!SUBSCRIBERS.isEmpty())
        {
            return;
        }

        CURRENT.remove();
        budgetTick = Long.MIN_VALUE;
        sentThisTick = 0;
        suppressed = 0;
    }

    // ------------------------------------------------------------------
    // capture, called from the mixins
    // ------------------------------------------------------------------

    public static void onTravelBegin(ServerLevel level, Entity entity, BlockPos entryPos)
    {
        if (!isEnabled())
        {
            return;
        }

        CURRENT.set(new TravelCtx());
    }

    public static void onTravelEnd(ServerLevel level, Entity entity, BlockPos entryPos, TeleportTransition result)
    {
        TravelCtx ctx = CURRENT.get();
        CURRENT.remove();

        if (!isEnabled())
        {
            return;
        }

        if (ctx == null)
        {
            ctx = new TravelCtx();
        }

        send(level, buildEvent(entity, entryPos, result, ctx));
    }

    public static void onPortalSearch(BlockPos ideal, boolean toNether, Optional<BlockPos> found)
    {
        if (!isEnabled())
        {
            return;
        }

        TravelCtx ctx = CURRENT.get();

        if (ctx == null)
        {
            return;
        }

        ctx.searched = true;
        ctx.ideal = ideal.immutable();
        ctx.radius = toNether ? 16 : 128;
        ctx.found = found == null ? null : found.orElse(null);
    }

    public static void onPortalCreated(Direction.Axis axis, Optional<FoundRectangle> result)
    {
        if (!isEnabled())
        {
            return;
        }

        TravelCtx ctx = CURRENT.get();

        if (ctx == null)
        {
            return;
        }

        FoundRectangle rectangle = result == null ? null : result.orElse(null);

        if (rectangle == null)
        {
            ctx.failure = "border";
            return;
        }

        ctx.created = rectangle;
    }

    // ------------------------------------------------------------------
    // data collection
    // ------------------------------------------------------------------

    private static void send(ServerLevel level, PortalEvent event)
    {
        broadcast(level, event);
    }

    private static PortalEvent buildEvent(Entity entity, BlockPos entryPos, TeleportTransition result,
                                           TravelCtx ctx)
    {
        int kind;

        if (ctx.created != null)
        {
            kind = KIND_NEW;
        }
        else if (ctx.searched && ctx.found != null)
        {
            kind = KIND_LINK;
        }
        else
        {
            kind = KIND_OTHER;
        }

        EntityInfo info = describeEntity(entity);
        String fromDimension = entity.level().dimension().identifier().toString();
        String toDimension = result == null ? "" : result.newLevel().dimension().identifier().toString();
        String toPos = result == null ? "" : vector(result.position());
        String portalPos = ctx.created != null
            ? blockPos(ctx.created.minCorner)
            : ctx.found != null ? blockPos(ctx.found) : "";
        String portalSize = ctx.created != null
            ? ctx.created.axis1Size + "x" + ctx.created.axis2Size : "";
        double idealDistance = ctx.ideal != null && ctx.found != null
            ? Math.sqrt(ctx.found.distSqr(ctx.ideal)) : -1.0;

        return new PortalEvent(
            kind,
            info.name(),
            info.typeName(),
            info.itemCount(),
            info.customNamed(),
            fromDimension,
            vector(entity.position()),
            toDimension,
            toPos,
            blockPos(entryPos),
            portalPos,
            portalSize,
            ctx.radius,
            idealDistance,
            ctx.created != null,
            ctx.searched,
            ctx.failure == null ? "" : ctx.failure,
            0
        );
    }

    private static EntityInfo describeEntity(Entity entity)
    {
        if (entity instanceof Player)
        {
            return new EntityInfo(entity.getName().getString(), "", -1, false);
        }

        if (entity instanceof ItemEntity itemEntity)
        {
            ItemStack stack = itemEntity.getItem();
            return new EntityInfo(stack.getHoverName().getString(), "", stack.getCount(), false);
        }

        String type = entity.getType().getDescription().getString();
        return entity.hasCustomName()
            ? new EntityInfo(entity.getName().getString(), type, -1, true)
            : new EntityInfo(type, "", -1, false);
    }

    private static String blockPos(BlockPos pos)
    {
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }

    private static String vector(Vec3 vec)
    {
        return String.format(Locale.ROOT, "(%.1f, %.1f, %.1f)", vec.x, vec.y, vec.z);
    }

    // ------------------------------------------------------------------
    // output
    // ------------------------------------------------------------------

    private static void broadcast(ServerLevel level, PortalEvent event)
    {
        MinecraftServer server = level.getServer();

        if (server == null || !isEnabled())
        {
            return;
        }

        MasaToolMod.LOGGER.info("[PortalLog] kind={} entity={} {} -> {} portal={}",
            event.kind(), event.entityName(), event.fromPos(), event.toPos(), event.portalPos());

        long tick = level.getGameTime();

        if (tick != budgetTick)
        {
            budgetTick = tick;
            sentThisTick = 0;

            if (suppressed > 0)
            {
                sendToSubscribers(server, PortalEvent.suppressed(suppressed));
                suppressed = 0;
            }
        }

        if (sentThisTick >= MAX_MESSAGES_PER_TICK)
        {
            suppressed++;
            return;
        }

        sentThisTick++;
        sendToSubscribers(server, event);
    }

    private static void sendToSubscribers(MinecraftServer server, PortalEvent event)
    {
        PortalEventPayload payload = new PortalEventPayload(event);

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (SUBSCRIBERS.contains(player.getUUID())
                && ServerPlayNetworking.canSend(player, PortalEventPayload.TYPE))
            {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private record EntityInfo(String name, String typeName, int itemCount, boolean customNamed)
    {
    }

    private static final class TravelCtx
    {
        private boolean searched;
        private BlockPos ideal;
        private int radius;
        private BlockPos found;
        private FoundRectangle created;
        private String failure;
    }
}
