package cn.maikuaidoufu.mixin;

import cn.maikuaidoufu.server.ChunkLoadMonitor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Observes chunk ticket-level changes so watched chunks can be reported with
 * the tickets that currently keep them loaded.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin
{
    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private TicketStorage ticketStorage;

    @Inject(method = "updateChunkScheduling", at = @At("HEAD"))
    private void doufuMasaTool$onChunkLevelChange(long pos, int oldLevel, ChunkHolder holder, int newLevel,
                                                   CallbackInfoReturnable<ChunkHolder> cir)
    {
        if (!ChunkLoadMonitor.hasWatchers() || oldLevel == newLevel)
        {
            return;
        }

        int chunkX = ChunkPos.getX(pos);
        int chunkZ = ChunkPos.getZ(pos);
        String dimension = this.level.dimension().identifier().toString();

        if (!ChunkLoadMonitor.isWatched(dimension, chunkX, chunkZ))
        {
            return;
        }

        List<String> tickets = new ArrayList<>();

        try
        {
            for (Ticket ticket : this.ticketStorage.getTickets(pos))
            {
                Identifier id = BuiltInRegistries.TICKET_TYPE.getKey(ticket.getType());
                tickets.add((id == null ? "unknown" : id.toString()) + ":" + ticket.getTicketLevel());
            }
        }
        catch (Throwable ignored)
        {
            // Ticket storage is an implementation detail; never break chunk scheduling.
        }

        ChunkLoadMonitor.onLevelChange(this.level, dimension, chunkX, chunkZ,
            oldLevel, newLevel, tickets);
    }
}
