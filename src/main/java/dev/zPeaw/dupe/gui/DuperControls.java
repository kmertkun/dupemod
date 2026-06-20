package dev.zPeaw.dupe.gui;

import dev.zPeaw.dupe.Config;
import dev.zPeaw.dupe.DuperManager;
import net.minecraft.client.MinecraftClient;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class DuperControls {

    private final Screen screen;
    private final List<net.minecraft.client.gui.widget.ClickableWidget> children = new ArrayList<>();
    private final List<net.minecraft.client.gui.Drawable> drawables = new ArrayList<>();

    public DuperControls(Screen screen) {
        this.screen = screen;
    }

    public void init(int inventoryX, int inventoryY) {
        children.clear();
        drawables.clear();

        int x = inventoryX - 105;
        int y = inventoryY + 5;
        int width = 100;
        int height = 20;

        addDrawableChild(CyclingButtonWidget
                .onOffBuilder(
                        Text.translatable("dupe.controls.enabled"),
                        Text.translatable("dupe.controls.disabled"),
                        Config.INSTANCE.enabled)
                .build(x, y, width, height, Text.translatable("dupe.controls.status"), (button, value) -> {
                    Config.INSTANCE.enabled = value;
                    Config.save();
                    DuperManager.reloadFrames();
                }));
        y += 24;

        addDrawableChild(CyclingButtonWidget.<Config.Mode>builder(value -> Text.of(value.name()), Config.INSTANCE.mode)
                .values(Config.Mode.values())
                .build(x, y, width, height, Text.translatable("dupe.controls.mode"), (button, value) -> {
                    Config.INSTANCE.mode = value;
                    Config.save();
                    if (Config.INSTANCE.enabled) {
                        DuperManager.reloadFrames();
                    }
                }));
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.translatable("dupe.screen.settings.title"), button -> {
            MinecraftClient.getInstance().setScreen(new SettingsScreen(this.screen));
        }).dimensions(x, y, width, height).build());
    }

    private void addDrawableChild(net.minecraft.client.gui.widget.ClickableWidget widget) {
        children.add(widget);
        drawables.add(widget);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (net.minecraft.client.gui.Drawable drawable : drawables) {
            drawable.render(context, mouseX, mouseY, delta);
        }

    }

    public boolean mouseClicked(Click click, boolean doubled) {
        for (net.minecraft.client.gui.Element element : children) {
            if (element.mouseClicked(click, doubled)) {
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public List<net.minecraft.client.gui.widget.ClickableWidget> getChildren() {
        return children;
    }

    public void setVisible(boolean visible) {
        for (net.minecraft.client.gui.widget.ClickableWidget widget : children) {
            widget.visible = visible;
        }
    }

    public void updatePositions(int inventoryX, int inventoryY) {
        int x = inventoryX - 105;
        int y = inventoryY + 5;

        for (net.minecraft.client.gui.widget.ClickableWidget widget : children) {
            widget.setX(x);
            widget.setY(y);
            y += 24;
        }
    }
}
