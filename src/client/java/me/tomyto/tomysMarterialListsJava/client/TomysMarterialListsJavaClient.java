package me.tomyto.tomysMarterialListsJava.client;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Insets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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
                    Path materialFile = getPath();
                    List<MaterialEntry> entries = MaterialParser.parseMaterialFile(materialFile);

                    //for testing
                    for (MaterialEntry entry: entries) {
                        System.out.println(entry);
                    }

                    client.setScreen(new MaterialListScreen(entries));
                }
            }
        });

    }

    //Get the path of the currently used file
    public Path getPath() {
        return MinecraftClient.getInstance().runDirectory
                .toPath()
                .resolve("config")
                .resolve("litematica")
                .resolve("material_list_2025-08-02_17.01.40.txt");
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

    public static Component icon(String name) {
        return Components.texture(
                Identifier.of("tomys-marterial-lists-java", "textures/gui/" + name + ".png"),
                0, 0,        // u, v (top-left corner)
                32, 32,      // regionWidth
                32, 32       // textureWidth, textureHeight (the full PNG dimensions)
        );
    }


    public class MaterialListScreen extends BaseOwoScreen<io.wispforest.owo.ui.container.FlowLayout> {

        private final List<MaterialEntry> materialEntries;

        public MaterialListScreen(List<MaterialEntry> materialEntries) {
            this.materialEntries = materialEntries;
        }

        @Override
        protected @NotNull OwoUIAdapter<io.wispforest.owo.ui.container.FlowLayout> createAdapter() {
            return OwoUIAdapter.create(this, Containers::verticalFlow);
        }

        public Integer numberShulkers(MaterialEntry entry) {
            return (int) entry.total/1728;
        }
        public Integer numberStacks(MaterialEntry entry) {
            return (int) (entry.total - numberShulkers(entry))/64;
        }
        public Integer numberItems(MaterialEntry entry) {
            return (int) (entry.total - numberShulkers(entry) - numberStacks(entry));
        }

        public GridLayout createMaterialRowGrid(MaterialEntry entry, ItemStack itemStack, int index) {
            GridLayout rowGrid = Containers.grid(Sizing.fill(100), Sizing.fixed(28), 1, 5);

            if (index % 2 == 0) {
                rowGrid.surface(Surface.flat(0x000000));
            } else {
                rowGrid.surface(Surface.flat(0x36403f));
            }

            // Item Icon
            rowGrid.child(Components.item(itemStack)
                    .sizing(Sizing.fixed(20), Sizing.fixed(20)), 0, 0);

            // Item Name
            FlowLayout nameContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(28));
            nameContainer.verticalAlignment(VerticalAlignment.CENTER);

            nameContainer.child(
                    Components.label(Text.literal(entry.name))
                            .horizontalTextAlignment(HorizontalAlignment.LEFT)
            );

            rowGrid.child(nameContainer, 0, 1);


            // Container for all the item numbers
            FlowLayout shulkerContainer = Containers.horizontalFlow(Sizing.fixed(60), Sizing.content());
            shulkerContainer.verticalAlignment(VerticalAlignment.CENTER);

            shulkerContainer.child(
                    Components.label(Text.literal(String.valueOf(numberShulkers(entry))))
            );
            shulkerContainer.child(
                    icon("shulker_icon2").sizing(Sizing.fixed(16), Sizing.fixed(16)) // Icon for number of shulkers
            );

            // Add it to the grid
            rowGrid.child(shulkerContainer, 0, 2);

            return rowGrid;
        }

        private ItemStack resolveItemStack(String name) {
            for (Identifier id : Registries.ITEM.getIds()) {
                Item item = Registries.ITEM.get(id);
                String displayName = item.getName().getString();

                if (displayName.equalsIgnoreCase(name)) {
                    return new ItemStack(item);
                }
            }
            //Fallback just in case
            return new ItemStack(Items.BARRIER);
        }


        @Override
        protected void build(FlowLayout rootComponent) {

            rootComponent.surface(Surface.VANILLA_TRANSLUCENT)
                    .horizontalAlignment(HorizontalAlignment.LEFT)
                    .verticalAlignment(VerticalAlignment.BOTTOM);


            //Vertical scroll container for the material list
            FlowLayout scrollContent = Containers.verticalFlow(Sizing.content(), Sizing.content());

            //Showing all the materials
            int index = 0;
            for (MaterialEntry entry : materialEntries) {
                ItemStack stack = resolveItemStack(entry.name);
                scrollContent.child(createMaterialRowGrid(entry, stack, index));
                index++;
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
