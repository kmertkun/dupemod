package dev.zPeaw.dupe.gui;

import dev.zPeaw.dupe.Config;
import dev.zPeaw.dupe.DuperManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class DupeHud implements HudRenderCallback {
    private static final int CARD_WIDTH = 172;
    private static final int CARD_HEIGHT = 90;

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!Config.INSTANCE.enabled || !Config.INSTANCE.render)
            return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }

        int dps = DuperManager.getDps();
        int x = 8;
        int y = 8;
        int accent = Config.INSTANCE.mode == Config.Mode.Speed ? 0xFF58A6FF : 0xFF36F28B;
        int softAccent = Config.INSTANCE.mode == Config.Mode.Speed ? 0x7758A6FF : 0x7736F28B;

        drawPanel(context, x, y, CARD_WIDTH, CARD_HEIGHT, accent);
        drawDupeIcon(context, x + 8, y + 8);

        context.drawTextWithShadow(mc.textRenderer, "Item Frame Dupe", x + 31, y + 7, 0xFFFFFFFF);
        context.drawTextWithShadow(mc.textRenderer, Config.INSTANCE.mode.name().toUpperCase(), x + 31, y + 19, accent);
        context.drawTextWithShadow(mc.textRenderer, "by zPeaw", x + 83, y + 19, 0xFFB7C6C0);

        String dpsText = dps + " DPS";
        context.drawTextWithShadow(mc.textRenderer, dpsText,
                x + CARD_WIDTH - 8 - mc.textRenderer.getWidth(dpsText), y + 9, 0xFFFFFFFF);

        int frames = DuperManager.getActiveFrameCount();
        int detected = DuperManager.getDetectedFrameCount();
        String frameText = "Frames " + frames + "/" + Config.INSTANCE.maxFrames;
        String scanText = "Scan " + detected + "  Empty " + DuperManager.getEmptyFrameCount();
        String itemText = "Items " + DuperManager.getDupeItemCount();
        context.drawTextWithShadow(mc.textRenderer, frameText, x + 10, y + 36, 0xFFEAFBF1);
        context.drawTextWithShadow(mc.textRenderer, scanText, x + 10, y + 48, 0xFFB7C6C0);
        context.drawTextWithShadow(mc.textRenderer, itemText, x + 10, y + 60, 0xFFEAFBF1);

        String actionText = "P" + DuperManager.getLastPlacements()
                + " S" + DuperManager.getLastSwaps()
                + " M" + DuperManager.getLastMoves();
        context.drawTextWithShadow(mc.textRenderer, actionText,
                x + CARD_WIDTH - 9 - mc.textRenderer.getWidth(actionText), y + 60, 0xFFB7C6C0);

        if (Config.INSTANCE.mode == Config.Mode.Speed && DuperManager.getMissingFrameCount() > 0) {
            String missingText = "-" + DuperManager.getMissingFrameCount();
            context.drawTextWithShadow(mc.textRenderer, missingText,
                    x + CARD_WIDTH - 9 - mc.textRenderer.getWidth(missingText), y + 36, 0xFFFFD166);
        }

        int barX = x + 10;
        int barY = y + CARD_HEIGHT - 10;
        int barWidth = CARD_WIDTH - 20;
        context.fill(barX, barY, barX + barWidth, barY + 3, 0x66000000);
        int fillWidth = (int) (barWidth * Math.min(1.0, dps / Math.max(1.0, Config.INSTANCE.maxFrames * 6.0)));
        context.fill(barX, barY, barX + fillWidth, barY + 3, accent);
        context.fill(barX, barY, barX + Math.max(2, fillWidth / 3), barY + 3, softAccent);
    }

    private static void drawPanel(DrawContext context, int x, int y, int width, int height, int accent) {
        context.fill(x + 2, y + 3, x + width + 2, y + height + 3, 0x66000000);
        context.fill(x, y, x + width, y + height, 0xCC101419);
        context.fill(x, y, x + 3, y + height, accent);
        context.fill(x, y, x + width, y + 1, 0x55FFFFFF);
        context.fill(x, y + height - 1, x + width, y + height, 0xAA000000);
        context.fill(x, y, x + width, y + height, 0x22000000);
    }

    private static void drawDupeIcon(DrawContext context, int x, int y) {
        List<Item> dupeItems = Config.getDupeItems();
        Item item = dupeItems.isEmpty() ? Items.ITEM_FRAME : dupeItems.get(0);
        context.fill(x - 2, y - 2, x + 18, y + 18, 0x88000000);
        context.fill(x - 2, y - 2, x + 18, y - 1, 0x55FFFFFF);
        context.drawItem(new ItemStack(item), x, y);
    }
}
