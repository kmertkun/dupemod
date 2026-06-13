package dev.zPeaw.dupe.gui;

import dev.zPeaw.dupe.Config;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

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

        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            return;
        }

        VertexConsumer vertexConsumer = consumers.getBuffer(RenderLayer.getLines());
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
        Box cameraBox = box.offset(-camera.x, -camera.y, -camera.z);
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        if (alpha == 0.0F) alpha = 1.0F;
        WorldRenderer.drawBox(context.matrixStack(), vertexConsumer, cameraBox, red, green, blue, alpha);
    }
}
