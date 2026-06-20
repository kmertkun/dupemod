package dev.zPeaw.dupe.gui;

import dev.zPeaw.dupe.mixin.HandledScreenAccessor;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.lwjgl.glfw.GLFW;

public final class InventoryScreenHandler {

    private InventoryScreenHandler() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen inventoryScreen)) {
                return;
            }

            HandledScreenAccessor handledScreen = (HandledScreenAccessor) inventoryScreen;
            DuperControls duperControls = new DuperControls(screen);
            duperControls.init(handledScreen.getX(), handledScreen.getY());

            ScreenEvents.afterRender(screen).register((renderScreen, context, mouseX, mouseY, delta) -> {
                duperControls.setVisible(true);
                duperControls.updatePositions(handledScreen.getX(), handledScreen.getY());
                duperControls.render(context, mouseX, mouseY, delta);
                InventoryOverlay.renderDupeSlots(context, handledScreen.getX(), handledScreen.getY());
            });

            ScreenKeyboardEvents.allowKeyPress(screen).register((keyScreen, input) -> {
                int key = input.key();
                int scancode = input.scancode();
                int modifiers = input.modifiers();
                if (key == GLFW.GLFW_KEY_K && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                    client.setScreen(new DebugScreen(screen));
                    return false;
                }
                if (duperControls.keyPressed(key, scancode, modifiers)) {
                    return false;
                }
                return true;
            });

            ScreenMouseEvents.allowMouseClick(screen).register((mouseScreen, click) -> {
                if (duperControls.mouseClicked(click, false)) {
                    return false;
                }
                return true;
            });
        });
    }
}
