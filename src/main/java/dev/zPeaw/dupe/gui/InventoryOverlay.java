package dev.zPeaw.dupe.gui;

import dev.zPeaw.dupe.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public final class InventoryOverlay {

    private InventoryOverlay() {
    }

    public static void renderDupeSlots(DrawContext context, int screenX, int screenY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }

        List<Item> dupeItems = Config.getDupeItems();
        PlayerInventory inv = mc.player.getInventory();

        for (int i = 0; i < 9; i++) {
            if (isDupeItem(inv.getStack(i), dupeItems)) {
                drawSlotHighlight(context, screenX + 8 + i * 18, screenY + 142);
            }
        }

        for (int i = 9; i < 36; i++) {
            if (isDupeItem(inv.getStack(i), dupeItems)) {
                int row = (i - 9) / 9;
                int col = (i - 9) % 9;
                drawSlotHighlight(context, screenX + 8 + col * 18, screenY + 84 + row * 18);
            }
        }

        if (isDupeItem(inv.getStack(PlayerInventory.OFF_HAND_SLOT), dupeItems)) {
            drawSlotHighlight(context, screenX + 77, screenY + 62);
        }
    }

    private static boolean isDupeItem(ItemStack stack, List<Item> dupeItems) {
        return !stack.isEmpty() && dupeItems.contains(stack.getItem());
    }

    private static void drawSlotHighlight(DrawContext context, int x, int y) {
        context.fill(x, y, x + 16, y + 16, 0x5500FF00);
        context.fill(x, y, x + 16, y + 1, 0xFF00FF00);
        context.fill(x, y + 15, x + 16, y + 16, 0xFF00FF00);
        context.fill(x, y, x + 1, y + 16, 0xFF00FF00);
        context.fill(x + 15, y, x + 16, y + 16, 0xFF00FF00);
    }
}
