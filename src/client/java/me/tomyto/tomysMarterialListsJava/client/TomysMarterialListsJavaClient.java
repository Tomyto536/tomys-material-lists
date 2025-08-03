package me.tomyto.tomysMarterialListsJava.client;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Insets;
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



import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.regex.Pattern;

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

    // Line content for the material list
    public static class MaterialEntry {
        public final String name;
        public final int total;
        public final boolean marked;

        public MaterialEntry(String name, int total, boolean marked) {
            this.name = name;
            this.total = total;
            this.marked = marked;
        }

        @Override
        public String toString() {
            return (marked ? "* " : "") + name + " - " + total;
        }
    }

    // Reading the file
    public class MaterialParser {

        private static final Pattern MATERIAL_LINE_PATTERN =
                Pattern.compile("\\|\\s(.+?)\\s+\\|\\s+(\\d+)\\s+\\|\\s+(\\d+)\\s+\\|\\s+(\\d+)\\s+\\|");

        public static List<MaterialEntry> parseMaterialFile(Path filePath) {
            List<MaterialEntry> materials = new ArrayList<>();

            try {
                List<String> lines = Files.readAllLines(filePath);

                for (String line : lines) {
                    Matcher matcher = MATERIAL_LINE_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String rawName = matcher.group(1).trim();
                        int total = Integer.parseInt(matcher.group(2));
                        boolean marked = rawName.startsWith("*");

                        String name = marked ? rawName.substring(1).trim() : rawName;

                        materials.add(new MaterialEntry(name, total, marked));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return materials;
        }

    }


    public class MaterialListScreen extends BaseOwoScreen<io.wispforest.owo.ui.container.FlowLayout> {


        @Override
        protected @NotNull OwoUIAdapter<io.wispforest.owo.ui.container.FlowLayout> createAdapter() {
            return OwoUIAdapter.create(this, Containers::verticalFlow);
        }

        @Override
        protected void build(FlowLayout rootComponent) {

            Path filePath = Paths.get("Minecraft", "Mods", "Tomys-Marterial-Lists-Java", "run", "config", "litematica", "example.txt");
            List<MaterialParser.MaterialEntry> materials = MaterialParser.parseMaterialFile(filePath);

            rootComponent.surface(Surface.VANILLA_TRANSLUCENT)
                    .horizontalAlignment(HorizontalAlignment.LEFT)
                    .verticalAlignment(VerticalAlignment.BOTTOM);

            //Vertical scroll container for the material list
            FlowLayout scrollContent = Containers.verticalFlow(Sizing.content(), Sizing.content());

            //Testing
            for (int i = 1; i <= 20; i++) {
                scrollContent.child(
                        Components.button(
                                Text.literal("Testing " + i),
                                button -> System.out.println("Clicked button ")
                        )
                );
            }


            // Scroll container that fills screen
            Component scrollContainer = Containers.verticalScroll(
                    Sizing.fill(100),                      // full width
                    Sizing.fill(90),          // full height minus 30px
                    scrollContent
            ).margins(Insets.of(10, 15, 5, 15));

            rootComponent.child(scrollContainer);

            // Bottom bar (fixed 30px height)
            rootComponent.child(
                    Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(30))
                            .child(Components.label(Text.literal("Bottom Bar")))
                            .padding(Insets.of(5))
                            .horizontalAlignment(HorizontalAlignment.CENTER)
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
