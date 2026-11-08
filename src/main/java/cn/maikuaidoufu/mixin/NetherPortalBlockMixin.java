package cn.maikuaidoufu.mixin;

import cn.maikuaidoufu.portal.PortalLog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wraps the portal travel decision so the logger knows the entry position and the destination.
 */
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin
{
    @Inject(method = "getPortalDestination", at = @At("HEAD"))
    private void doufuMasaTool$beforePortalDestination(ServerLevel level, Entity entity, BlockPos entryPos,
                                                       CallbackInfoReturnable<TeleportTransition> cir)
    {
        PortalLog.onTravelBegin(level, entity, entryPos);
    }

    @Inject(method = "getPortalDestination", at = @At("RETURN"))
    private void doufuMasaTool$afterPortalDestination(ServerLevel level, Entity entity, BlockPos entryPos,
                                                      CallbackInfoReturnable<TeleportTransition> cir)
    {
        PortalLog.onTravelEnd(level, entity, entryPos, cir.getReturnValue());
    }
}
