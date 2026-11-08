package cn.maikuaidoufu.mixin;

import cn.maikuaidoufu.server.RegionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.ObserverBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops observer pulses while the observer sits inside an enabled region.
 */
@Mixin(ObserverBlock.class)
public abstract class ObserverBlockMixin
{
    @Inject(method = "startSignal", at = @At("HEAD"), cancellable = true)
    private void doufuMasaTool$cancelStartSignal(LevelReader world, ScheduledTickAccess tickAccess,
                                                  BlockPos pos, CallbackInfo ci)
    {
        if (RegionManager.isSuppressed(world, pos))
        {
            ci.cancel();
        }
    }
}
