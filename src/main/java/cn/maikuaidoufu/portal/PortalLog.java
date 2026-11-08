package cn.maikuaidoufu.portal;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.net.PortalEventPayload;
import cn.maikuaidoufu.net.PortalStatePayload;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.BlockUtil.FoundRectangle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Captures nether portal travels on the server and forwards them to the client terminal.
 */
public final class PortalLog
{
    public static final int KIND_NEW = 0;
    public static final int KIND_LINK = 1;
    public static final int KIND_OTHER = 2;

    private static final int MAX_MESSAGES_PER_TICK = 20;

    private static volatile boolean enabled;

    private static final ThreadLocal<TravelCtx> CURRENT = new ThreadLocal<>();

    private static long budgetTick = Long.MIN_VALUE;
    private static int sentThisTick;
    private static int suppressed;

    private PortalLog()
    {
    }

    public static boolean isEnabled()
    {
        return enabled;
    }

    public static void setEnabled(boolean value, MinecraftServer server)
    {
        if (enabled == value)
        {
            return;
        }

        enabled = value;
        CURRENT.remove();
        budgetTick = Long.MIN_VALUE;
        sentThisTick = 0;
        suppressed = 0;

        if (server != null)
        {
            for (ServerPlayer player : server.getPlayerList().getPlayers())
            {
                sendState(player);
            }
        }
    }

    public static void reset()
    {
        enabled = false;
        CURRENT.remove();
    }

    public static void sendState(ServerPlayer player)
    {
        if (player == null || !ServerPlayNetworking.canSend(player, PortalStatePayload.TYPE))
        {
            return;
        }

        ServerPlayNetworking.send(player, new PortalStatePayload(enabled));
    }

    // ------------------------------------------------------------------
    // capture, called from the mixins
    // ------------------------------------------------------------------

    public static void onTravelBegin(ServerLevel level, Entity entity, BlockPos entryPos)
    {
        if (!enabled)
        {
            return;
        }

        CURRENT.set(new TravelCtx());
    }

    public static void onTravelEnd(ServerLevel level, Entity entity, BlockPos entryPos, TeleportTransition result)
    {
        TravelCtx ctx = CURRENT.get();
        CURRENT.remove();

        if (!enabled)
        {
            return;
        }

        if (ctx == null)
        {
            ctx = new TravelCtx();
        }

        send(level, entity, entryPos, result, ctx);
    }

    public static void onPortalSearch(BlockPos ideal, boolean toNether, Optional<BlockPos> found)
    {
        if (!enabled)
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
        if (!enabled)
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
            ctx.failure = "无法创建传送门（目标超出世界边界）";
            return;
        }

        ctx.created = rectangle;
    }

    // ------------------------------------------------------------------
    // rendering
    // ------------------------------------------------------------------

    private static void send(ServerLevel level, Entity entity, BlockPos entryPos, TeleportTransition result,
                             TravelCtx ctx)
    {
        int kind;
        String text;

        if (ctx.created != null)
        {
            kind = KIND_NEW;
            text = describeCreated(entity, entryPos, result, ctx);
        }
        else if (ctx.searched && ctx.found != null)
        {
            kind = KIND_LINK;
            text = describeLinked(entity, entryPos, result, ctx);
        }
        else
        {
            kind = KIND_OTHER;
            text = describeOther(entity, entryPos, result, ctx);
        }

        broadcast(level, text, kind);
    }

    private static String describeCreated(Entity entity, BlockPos entryPos, TeleportTransition result, TravelCtx ctx)
    {
        String size = ctx.created.axis1Size + "x" + ctx.created.axis2Size;
        StringBuilder sb = new StringBuilder();
        sb.append(blockPos(ctx.created.minCorner)).append(' ').append(size);
        sb.append(" · ").append(describeTravel(entity, entryPos, result));

        if (ctx.radius > 0)
        {
            sb.append(" · 原因: 目标维度方形范围 ").append(ctx.radius).append(" 格内没有门");
        }
        else
        {
            sb.append(" · 原因: 目标维度没有可链接的门");
        }

        return sb.toString();
    }

    private static String describeLinked(Entity entity, BlockPos entryPos, TeleportTransition result, TravelCtx ctx)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(blockPos(ctx.found));
        sb.append(" · ").append(describeTravel(entity, entryPos, result));

        if (ctx.ideal != null)
        {
            String distance = String.format(Locale.ROOT, "%.1f", Math.sqrt(ctx.found.distSqr(ctx.ideal)));
            sb.append(" · 距预想点 ").append(distance).append(" 格");
        }

        sb.append(" · 搜索半径 ").append(ctx.radius).append(" 格");
        return sb.toString();
    }

    private static String describeOther(Entity entity, BlockPos entryPos, TeleportTransition result, TravelCtx ctx)
    {
        StringBuilder sb = new StringBuilder(describeTravel(entity, entryPos, result));

        if (ctx.failure != null)
        {
            sb.append(" · 失败: ").append(ctx.failure);
        }

        return sb.toString();
    }

    private static String describeTravel(Entity entity, BlockPos entryPos, TeleportTransition result)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(describeEntity(entity));
        sb.append(' ').append(dimensionName(entity.level().dimension())).append(vector(entity.position()));
        sb.append(" → ");

        if (result == null)
        {
            sb.append("未确定");
        }
        else
        {
            sb.append(dimensionName(result.newLevel().dimension())).append(vector(result.position()));
        }

        sb.append(" · 入口").append(blockPos(entryPos));
        return sb.toString();
    }

    private static String describeEntity(Entity entity)
    {
        if (entity instanceof Player)
        {
            return entity.getName().getString();
        }

        if (entity instanceof ItemEntity itemEntity)
        {
            ItemStack stack = itemEntity.getItem();
            return "物品 " + stack.getHoverName().getString() + (stack.getCount() > 1 ? "×" + stack.getCount() : "");
        }

        String type = entity.getType().getDescription().getString();
        return entity.hasCustomName() ? entity.getName().getString() + "(" + type + ")" : type;
    }

    private static String dimensionName(ResourceKey<Level> key)
    {
        if (key.equals(Level.OVERWORLD))
        {
            return "主世界";
        }

        if (key.equals(Level.NETHER))
        {
            return "下界";
        }

        if (key.equals(Level.END))
        {
            return "末地";
        }

        return key.identifier().toString();
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

    private static void broadcast(ServerLevel level, String text, int kind)
    {
        MinecraftServer server = level.getServer();

        if (server == null)
        {
            return;
        }

        MasaToolMod.LOGGER.info("[PortalLog] {}", text);

        long tick = level.getGameTime();

        if (tick != budgetTick)
        {
            budgetTick = tick;
            sentThisTick = 0;

            if (suppressed > 0)
            {
                sendToAll(server, "上一 tick 还有 " + suppressed + " 条记录未显示", KIND_OTHER);
                suppressed = 0;
            }
        }

        if (sentThisTick >= MAX_MESSAGES_PER_TICK)
        {
            suppressed++;
            return;
        }

        sentThisTick++;
        sendToAll(server, text, kind);
    }

    private static void sendToAll(MinecraftServer server, String text, int kind)
    {
        PortalEventPayload payload = new PortalEventPayload(text, kind);

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (ServerPlayNetworking.canSend(player, PortalEventPayload.TYPE))
            {
                ServerPlayNetworking.send(player, payload);
            }
        }
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
