package cn.maikuaidoufu.client;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.region.Box;
import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public final class RegionHighlightRenderer implements IRenderer
{
    private static final float LINE_WIDTH = 1.5f;
    private static boolean loggedError;

    @Override
    public void onRenderWorldLast(RenderTarget target, Matrix4fc matrix, CameraRenderState cameraState,
                                  Frustum frustum, RenderBuffers buffers, GpuBufferSlice bufferSlice,
                                  Vector4f fogColor, ProfilerFiller profiler)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || !Configs.RENDER_HIGHLIGHTS.getBooleanValue())
        {
            return;
        }

        String dimension = mc.level.dimension().identifier().toString();

        try
        {
            for (RegionGroup group : ClientGroupStore.groups())
            {
                if (!ClientHighlightStore.isGroupHighlighted(group))
                {
                    continue;
                }

                Color4f color = colorFor(group);

                for (RegionEntry entry : group.entries())
                {
                    if (!entry.dimension().equals(dimension)
                        || !ClientHighlightStore.isEntryHighlighted(group, entry))
                    {
                        continue;
                    }

                    Color4f entryColor = group.enabled() && entry.enabled()
                        ? color
                        : new Color4f(0.55f, 0.55f, 0.55f, 1.0f);

                    Box box = entry.box();
                    RenderUtils.renderAreaOutline(
                        new BlockPos(box.minX(), box.minY(), box.minZ()),
                        new BlockPos(box.maxX(), box.maxY(), box.maxZ()),
                        LINE_WIDTH,
                        entryColor,
                        entryColor,
                        entryColor
                    );
                }
            }
        }
        catch (Throwable e)
        {
            if (!loggedError)
            {
                loggedError = true;
                MasaToolMod.LOGGER.warn("Failed to render region highlights", e);
            }
        }
    }

    private static Color4f colorFor(RegionGroup group)
    {
        int hash = group.id().hashCode();
        float hue = Math.floorMod(hash, 360) / 360.0f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.55f, 1.0f);
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        return new Color4f(r, g, b, 1.0f);
    }
}
