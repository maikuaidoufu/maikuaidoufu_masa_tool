package cn.maikuaidoufu.mixin;

import cn.maikuaidoufu.portal.PortalLog;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.BlockUtil.FoundRectangle;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures the two decisions vanilla makes for one travel: the nearby portal search and,
 * when that finds nothing, the portal that gets built instead.
 */
@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin
{
    @Inject(method = "findClosestPortalPosition", at = @At("RETURN"))
    private void doufuMasaTool$afterPortalSearch(BlockPos ideal, boolean toNether, WorldBorder border,
                                                 CallbackInfoReturnable<Optional<BlockPos>> cir)
    {
        PortalLog.onPortalSearch(ideal, toNether, cir.getReturnValue());
    }

    @Inject(method = "createPortal", at = @At("RETURN"))
    private void doufuMasaTool$afterPortalCreated(BlockPos origin, Direction.Axis axis,
                                                  CallbackInfoReturnable<Optional<FoundRectangle>> cir)
    {
        PortalLog.onPortalCreated(axis, cir.getReturnValue());
    }
}
