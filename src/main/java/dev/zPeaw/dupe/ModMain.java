package dev.zPeaw.dupe;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import org.lwjgl.glfw.GLFW;

import dev.zPeaw.dupe.gui.InventoryScreenHandler;
import dev.zPeaw.dupe.gui.UpdateScreen;
import dev.zPeaw.dupe.utils.UpdateChecker;

public class ModMain implements ClientModInitializer {

    private boolean wasKeyPressed = false;
    private boolean shownUpdateScreen = false;

    @Override
    public void onInitializeClient() {
        Config.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (Config.INSTANCE.dupeKey != GLFW.GLFW_KEY_UNKNOWN) {
                boolean isPressed = GLFW.glfwGetKey(client.getWindow().getHandle(),
                        Config.INSTANCE.dupeKey) == GLFW.GLFW_PRESS;
                if (isPressed && !wasKeyPressed) {
                    if (client.player != null) {
                        DuperManager.onDupeKeyPressed();
                    }
                }
                wasKeyPressed = isPressed;
            }

            DuperManager.tick();

            if (client.currentScreen instanceof TitleScreen) {
                if (UpdateChecker.isUpdateAvailable && !shownUpdateScreen) {
                    if (!UpdateChecker.latestVersion.equals(Config.INSTANCE.ignoreVersion)) {
                        shownUpdateScreen = true;
                        client.setScreen(new UpdateScreen(client.currentScreen,
                                UpdateChecker.latestVersion));
                    } else {
                        shownUpdateScreen = true;
                    }
                }
            }
        });

        DupeCommands.register();

        InventoryScreenHandler.register();

        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT
                .register(new dev.zPeaw.dupe.gui.DupeHud());
        dev.zPeaw.dupe.gui.FrameRender.register();

        UpdateChecker.checkForUpdates();
    }
}
