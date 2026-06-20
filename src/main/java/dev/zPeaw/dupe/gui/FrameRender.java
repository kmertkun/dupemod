package dev.zPeaw.dupe.gui;

import dev.zPeaw.dupe.Config;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.List;

public final class FrameRender {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private FrameRender() {
    }

    public static void register() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(FrameRender::render);
    }

    private static void render(WorldRenderContext context) {
        if (!Config.INSTANCE.enabled || !Config.INSTANCE.render || mc.player == null || mc.world == null) {
            return;
        }

        Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null || context.matrices() == null) {
            return;
        }

        VertexConsumer vertexConsumer = consumers.getBuffer(RenderLayers.lines());
        List<Item> dupeItems = Config.getDupeItems();
        double renderRange = Math.max(Config.INSTANCE.range, Config.INSTANCE.renderRange);
        double rangeSq = renderRange * renderRange;
        double pulse = 0.025 + (Math.sin(System.currentTimeMillis() / 180.0) * 0.015);

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemFrameEntity frame)) {
                continue;
            }
            if (frame.isRemoved() || !frame.isAlive() || frame.squaredDistanceTo(mc.player.getEyePos()) > rangeSq) {
                continue;
            }

            int color = getFrameColor(frame, dupeItems);
            drawFrameBox(context, vertexConsumer, frame.getBoundingBox().expand(0.035 + pulse), camera, color);
            drawFrameBox(context, vertexConsumer, frame.getBoundingBox().expand(0.012), camera, 0xFFEAFBF1);
        }
    }

    private static int getFrameColor(ItemFrameEntity frame, List<Item> dupeItems) {
        if (frame.getHeldItemStack().isEmpty()) {
            return 0xFF36F28B;
        }
        if (dupeItems.contains(frame.getHeldItemStack().getItem())) {
            return Config.INSTANCE.mode == Config.Mode.Speed ? 0xFF58A6FF : 0xFF20E3B2;
        }
        return 0xFFFFD166;
    }

    private static void drawFrameBox(WorldRenderContext context, VertexConsumer vertexConsumer, Box box, Vec3d camera,
            int color) {
        // Ensure a fully opaque alpha if none was provided (matches the pre-1.21.9 behaviour).
        int argb = ((color >>> 24) == 0) ? (0xFF000000 | color) : color;

        // 1.21.9 removed WorldRenderer.drawBox; in-world outlines are now drawn from a VoxelShape
        // via VertexRendering.drawOutline. Build the shape at the origin and position it with the
        // camera-relative offset so large world coordinates stay out of the voxel grid.
        VoxelShape shape = VoxelShapes.cuboid(
                0.0, 0.0, 0.0,
                box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ);

        VertexRendering.drawOutline(
                context.matrices(), vertexConsumer, shape,
                box.minX - camera.x, box.minY - camera.y, box.minZ - camera.z,
                argb, 1.5F);
    }
}
