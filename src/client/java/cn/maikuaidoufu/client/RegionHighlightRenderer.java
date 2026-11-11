package cn.maikuaidoufu.client;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.region.Box;
import cn.maikuaidoufu.region.RegionEntry;
import cn.maikuaidoufu.region.RegionGroup;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class RegionHighlightRenderer implements IRenderer
{
    private static final float LINE_WIDTH = 1.5f;
    private static boolean loggedError;
    private static boolean loggedOutlineError;

    @Override
    public void onRenderWorldLastAdvanced(RenderTarget target, Matrix4f matrix, Matrix4f projectionMatrix,
                                          Frustum frustum, Camera camera, RenderBuffers buffers,
                                          ProfilerFiller profiler)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || !Configs.RENDER_HIGHLIGHTS.getBooleanValue())
        {
            return;
        }

        boolean xray = Configs.XRAY_HIGHLIGHTS.getBooleanValue();
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
                    BlockPos pos1 = new BlockPos(box.minX(), box.minY(), box.minZ());
                    BlockPos pos2 = new BlockPos(box.maxX(), box.maxY(), box.maxZ());

                    if (xray)
                    {
                        // MaLiLib's helper uses a NO_DEPTH pipeline: outlines stay visible
                        // through terrain. That is the opt-in "X-ray render" behaviour.
                        RenderUtils.renderAreaOutline(pos1, pos2, LINE_WIDTH,
                            entryColor, entryColor, entryColor);
                    }
                    else
                    {
                        renderOccludedOutline(pos1, pos2, entryColor);
                    }
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

    /**
     * Draws the same 12 edges as MaLiLib's area outline, but with a pipeline
     * that respects the depth buffer, so blocks in front hide the outline.
     */
    private static void renderOccludedOutline(BlockPos pos1, BlockPos pos2, Color4f color)
    {
        RenderPipeline pipeline = MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH;

        if (pipeline == null)
        {
            pipeline = MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_CULL;
        }

        if (pipeline == null)
        {
            pipeline = MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE;
        }

        if (pipeline == null)
        {
            // MaLiLib did not register a depth-tested pipeline. Keep the outlines
            // visible instead of failing or silently hiding everything.
            RenderUtils.renderAreaOutline(pos1, pos2, LINE_WIDTH, color, color, color);
            return;
        }

        Vec3 cam = RenderUtils.camPos();
        float minX = (float) (Math.min(pos1.getX(), pos2.getX()) - cam.x);
        float minY = (float) (Math.min(pos1.getY(), pos2.getY()) - cam.y);
        float minZ = (float) (Math.min(pos1.getZ(), pos2.getZ()) - cam.z);
        float maxX = (float) (Math.max(pos1.getX(), pos2.getX()) - cam.x + 1.0);
        float maxY = (float) (Math.max(pos1.getY(), pos2.getY()) - cam.y + 1.0);
        float maxZ = (float) (Math.max(pos1.getZ(), pos2.getZ()) - cam.z + 1.0);

        try (RenderContext ctx = new RenderContext(
            () -> "maikuaidoufu_masa_tool/region_outline", pipeline))
        {
            BufferBuilder builder = ctx.getBuilder();

            // Edges along X
            line(builder, minX, minY, minZ, maxX, minY, minZ, color);
            line(builder, minX, maxY, minZ, maxX, maxY, minZ, color);
            line(builder, minX, minY, maxZ, maxX, minY, maxZ, color);
            line(builder, minX, maxY, maxZ, maxX, maxY, maxZ, color);

            // Edges along Y
            line(builder, minX, minY, minZ, minX, maxY, minZ, color);
            line(builder, maxX, minY, minZ, maxX, maxY, minZ, color);
            line(builder, minX, minY, maxZ, minX, maxY, maxZ, color);
            line(builder, maxX, minY, maxZ, maxX, maxY, maxZ, color);

            // Edges along Z
            line(builder, minX, minY, minZ, minX, minY, maxZ, color);
            line(builder, maxX, minY, minZ, maxX, minY, maxZ, color);
            line(builder, minX, maxY, minZ, minX, maxY, maxZ, color);
            line(builder, maxX, maxY, minZ, maxX, maxY, maxZ, color);

            MeshData mesh = builder.build();

            if (mesh != null)
            {
                try
                {
                    ctx.draw(mesh, false, true);
                }
                finally
                {
                    mesh.close();
                }
            }
        }
        catch (Exception e)
        {
            if (!loggedOutlineError)
            {
                loggedOutlineError = true;
                MasaToolMod.LOGGER.warn("Failed to render occluded region outlines", e);
            }
        }
    }

    private static void line(BufferBuilder builder, float x1, float y1, float z1,
                              float x2, float y2, float z2, Color4f color)
    {
        vertex(builder, x1, y1, z1, color);
        vertex(builder, x2, y2, z2, color);
    }

    private static void vertex(BufferBuilder builder, float x, float y, float z, Color4f color)
    {
        builder.addVertex(x, y, z)
            .setColor(color.r, color.g, color.b, color.a)
            .setLineWidth(LINE_WIDTH);
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
