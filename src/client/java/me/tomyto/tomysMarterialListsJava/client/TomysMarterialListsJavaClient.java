package me.tomyto.tomysMarterialListsJava.client;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.core.Component;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.client.option.KeyBinding;

import org.lwjgl.glfw.GLFW;


import java.nio.file.*;
import java.util.*;

import static me.tomyto.tomysMarterialListsJava.client.FileUtils.*;

public class TomysMarterialListsJavaClient implements ClientModInitializer {

    public static KeyBinding openScreenKeyBind;

    public static Path lastUsedMaterialFile = null;

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
                    Path materialFile = loadLastUsedFilePath();
                    TomysMarterialListsJavaClient.lastUsedMaterialFile = materialFile;
                    List<MaterialEntry> entries = MaterialParser.parseMaterialFile(materialFile);

                    client.setScreen(new MaterialListScreen(entries, materialFile));
                }
            }
        });

    }

    public static Component icon(String name) {
        return Components.texture(
                Identifier.of("tomys-marterial-lists-java", "textures/gui/" + name + ".png"),
                0, 0,        // u, v (top-left corner)
                32, 32,      // regionWidth
                32, 32       // textureWidth, textureHeight (the full PNG dimensions)
        );
    }
}
