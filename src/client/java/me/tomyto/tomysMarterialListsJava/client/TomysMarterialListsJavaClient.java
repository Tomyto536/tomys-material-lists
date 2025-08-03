package me.tomyto.tomysMarterialListsJava.client;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.core.Component;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import io.wispforest.owo.ui.core.OwoUIAdapter;


import org.lwjgl.glfw.GLFW;
import net.minecraft.client.option.KeyBinding;


import java.awt.*;

public class TomysMarterialListsJavaClient implements ClientModInitializer {

    public static KeyBinding openScreenKeyBind;


    @Override
    public void onInitializeClient() {
        // Register a keybind for LEFT CONTROL
        openScreenKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Open Material List", // translation key
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_CONTROL,
                "Tomy's Material List" // category shown in Controls menu
        ));

        // Register the event that checks for key presses every client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openScreenKeyBind.wasPressed()) {
                if (client.currentScreen == null ) {
                    client.setScreen(new MaterialListScreen());
                }
            }
        });

    }

    public class MaterialListScreen extends BaseOwoScreen<io.wispforest.owo.ui.container.FlowLayout> {

        @Override
        protected @NotNull OwoUIAdapter<io.wispforest.owo.ui.container.FlowLayout> createAdapter() {
            return OwoUIAdapter.create(this, Containers::verticalFlow);
        }

        @Override
        protected void build(FlowLayout rootComponent) {
            rootComponent.surface(Surface.VANILLA_TRANSLUCENT)
                    .horizontalAlignment(HorizontalAlignment.LEFT)
                    .verticalAlignment(VerticalAlignment.BOTTOM);

            rootComponent.child(
                    Components.button(
                            Text.literal("Test"),
                            button -> { System.out.println("click");}
                    )
            );
        }



        // Close gui when button pressed - toggolable GUI mechanics
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (openScreenKeyBind.matchesKey(keyCode, scanCode)) {
                MinecraftClient.getInstance().setScreen(null);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

    }

}
