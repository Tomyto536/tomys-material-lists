package me.tomyto.tomysMarterialListsJava.client;

import com.mojang.brigadier.context.ContextChain;
import com.mojang.datafixers.types.templates.Check;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.*;
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
import java.awt.Color;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.option.KeyBinding;



import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Flow;
import java.util.regex.*;
import java.util.regex.Pattern;

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
                    if (materialFile == null) {
                        materialFile = getPath();
                    }
                    TomysMarterialListsJavaClient.lastUsedMaterialFile = materialFile;
                    List<MaterialEntry> entries = MaterialParser.parseMaterialFile(materialFile);

                    client.setScreen(new MaterialListScreen(entries, materialFile));
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
        public String name;
        public int total;
        public boolean marked;

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
                        int missing = Integer.parseInt(matcher.group(3));
                        boolean marked = rawName.startsWith("*");

                        String name = marked ? rawName.substring(1).trim() : rawName;

                        materials.add(new MaterialEntry(name, missing, marked));
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

    public static void saveLastUsedFile(Path path) {
        Path savePath = MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("config")
                .resolve("litematica")
                .resolve("tomys_material_lists_data.txt");

        ensureLastUsedFileExists();

        try {
            Files.writeString(savePath, path.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Path loadLastUsedFilePath() {
        Path savePath = MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("config")
                .resolve("litematica")
                .resolve("tomys_material_lists_data.txt");

        ensureLastUsedFileExists();

        try {
            if (Files.exists(savePath)) {
                return Paths.get(Files.readString(savePath));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void ensureLastUsedFileExists() {
        Path savePath = MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("config")
                .resolve("litematica")
                .resolve("tomys_material_lists_data.txt");

        if (Files.notExists(savePath)) {
            try {
                Files.createFile(savePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public class MaterialListScreen extends BaseOwoScreen<io.wispforest.owo.ui.container.FlowLayout> {

        private ScrollContainer<FlowLayout> scrollContainer;
        private final List<MaterialEntry> materialEntries;
        private final List<GridLayout> materialRows = new ArrayList<>();
        private final Path materialFilePath;
        private int highlightedIndex = -1;
        private enum HighlightMode {KEYBOARD, MOUSE}
        private HighlightMode highlightMode = HighlightMode.KEYBOARD;
        private boolean highlightFromKeyboard = true;
        private final List<MaterialEntry> visibleEntries = new ArrayList<>();
        private Integer initialHighlightIndex = null;


        public MaterialListScreen(List<MaterialEntry> materialEntries, Path materialFilePath) {
            this.materialEntries = materialEntries;
            this.materialFilePath = materialFilePath;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (openScreenKeyBind.matchesKey(keyCode, scanCode)) {
                MinecraftClient.getInstance().setScreen(null);
                return true;
            }
            switch (keyCode) {
                case GLFW.GLFW_KEY_W -> {
                    //Move up the material list
                    if (!materialRows.isEmpty() && highlightedIndex > 0) {
                        highlightedIndex--;
                        highlightMode = HighlightMode.KEYBOARD;
                        highlightFromKeyboard = true;
                        highlight(highlightedIndex);
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_A -> {
                    if (highlightedIndex >= 0 && highlightedIndex < materialEntries.size()) {
                        MaterialEntry entry = visibleEntries.get(highlightedIndex);
                        boolean newValue = !entry.marked; // Toggle current state

                        // Save index before removing entry
                        int newHighlightIndex = highlightedIndex;

                        checkItemOff(entry, newValue);
                        writeToFile();

                        if (newHighlightIndex >= visibleEntries.size() - 1) {
                            newHighlightIndex = Math.max(visibleEntries.size() - 2, 0); // avoid -1
                        }
                        MaterialListScreen newScreen = new MaterialListScreen(materialEntries, materialFilePath);
                        newScreen.setInitialHighlightIndex(newHighlightIndex);
                        MinecraftClient.getInstance().setScreen(newScreen);
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_S -> {
                    //move down the list
                    if (!materialRows.isEmpty() && highlightedIndex < materialRows.size() - 1) {
                        highlightedIndex++;
                        highlightMode = HighlightMode.KEYBOARD;
                        highlightFromKeyboard = true;
                        highlight(highlightedIndex);
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_D -> {
                    System.out.println("D key pressed");
                    return true;
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void mouseMoved(double mouseX, double mouseY) {
            super.mouseMoved(mouseX, mouseY);
            highlightMode = HighlightMode.MOUSE;
        }

        @Override
        protected @NotNull OwoUIAdapter<io.wispforest.owo.ui.container.FlowLayout> createAdapter() {
            return OwoUIAdapter.create(this, Containers::verticalFlow);
        }

        public Integer numberShulkers(MaterialEntry entry) {
            return (int) entry.total/1728;
        }
        public Integer numberStacks(MaterialEntry entry) {
            return (int) (entry.total % 1728)/64;
        }
        public Integer numberItems(MaterialEntry entry) {
            return (int) (entry.total % 64);
        }

        //Function to higlight the row. Un-highlights all other rows
        private void highlight(int index) {
            for (int i = 0; i < materialRows.size(); i++) {
                GridLayout row = materialRows.get(i);
                if (i == index) {
                    int color = new Color(44, 106, 125, 255).getRGB();
                    row.surface(Surface.flat(color));
                } else {
                    row.surface(Surface.DARK_PANEL);
                }
            }

            highlightedIndex = index;

            if (highlightFromKeyboard) {
                ensureRowVisible(index);
                highlightFromKeyboard = false; // reset
            }
        }

        public void setInitialHighlightIndex(int index) {
            this.initialHighlightIndex = index;
        }

        private void ensureRowVisible(int index) {
            if (scrollContainer == null) return;

            int viewportTop = scrollContainer.y();
            int viewportBottom = viewportTop + scrollContainer.height();

            Component row = materialRows.get(index);
            int rowTop = row.y();
            int rowBottom = rowTop + row.height();

            if (rowTop < viewportTop || rowBottom > viewportBottom) {
                scrollContainer.scrollTo(row);
            }
        }

        private void writeToFile() {
            try (BufferedWriter writer = Files.newBufferedWriter(materialFilePath)) {
                for (MaterialEntry entry : materialEntries) {
                    // Keep the same formatting as in your original material file
                    String line = String.format(
                            "| %-30s | %5d | %5d | %5d |",
                            entry.marked ? "* " + entry.name : entry.name,
                            0, // Replace with actual needed value if applicable
                            entry.total,
                            0  // Replace with actual needed value if applicable
                    );
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void checkItemOff(MaterialEntry entry, boolean checked) {
            if (checked) {
                if (!entry.marked) {
                    entry.name = entry.name;
                    entry.marked = true;
                }
                materialEntries.remove(entry);
                materialEntries.add(0, entry);
            } else {
                if (entry.marked) {
                    entry.name = entry.name.replaceFirst("^\\*\\s*", "");
                    entry.marked = false;
                }
            }
        }

        public Component createMaterialRowGrid(MaterialEntry entry, ItemStack itemStack, int index) {
            GridLayout rowGrid = Containers.grid(Sizing.fill(100), Sizing.fixed(28), 1, 5);
            rowGrid.surface(Surface.DARK_PANEL); //Offer light and dark mode?

            // Item Icon ---------------------------------------------------------
            Component iconComponent = Components.item(itemStack)
                    .sizing(Sizing.fixed(20), Sizing.fixed(20))
                    .margins(Insets.both(12, 4));

            rowGrid.child(iconComponent, 0, 0);

            // Item Name ----------------------------------------------------------
            Component nameLabel = Components.label(Text.literal(entry.name))
                    .horizontalTextAlignment(HorizontalAlignment.LEFT);

            FlowLayout nameContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(28));
            nameContainer.verticalAlignment(VerticalAlignment.CENTER);
            nameContainer.child(nameLabel);
            rowGrid.child(nameContainer.verticalAlignment(VerticalAlignment.CENTER), 0, 1);


            // Container for all the item numbers ---------------------------------------------------------
            FlowLayout quantity = Containers.horizontalFlow(Sizing.fixed(60), Sizing.content());
            quantity.verticalAlignment(VerticalAlignment.CENTER);


            quantity.child(
                    Components.label(Text.literal(String.valueOf(numberShulkers(entry))))
                            .margins(Insets.right(8))
            );
            quantity.child(
                    icon("shulker_icon2").sizing(Sizing.fixed(16), Sizing.fixed(16)) // Icon for number of shulkers
            );

            // Add it to the grid
            rowGrid.child(quantity
                            .horizontalAlignment(HorizontalAlignment.RIGHT)
                            .verticalAlignment(VerticalAlignment.CENTER)
                            .margins(Insets.top(6))
                    , 0, 2);

            //CheckBox ------------------------------------------------------------------------
            CheckboxComponent checkbox = Components.checkbox(Text.empty());
            checkbox.onChanged(newValue -> {
                checkItemOff(entry, newValue);
                writeToFile();
                MinecraftClient.getInstance().setScreen(new MaterialListScreen(materialEntries, materialFilePath));
            });
            FlowLayout checkboxContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
            checkboxContainer.horizontalAlignment(HorizontalAlignment.RIGHT);
            checkboxContainer.child(checkbox);
            checkbox.margins(Insets.both(10, 6));

            rowGrid.child(checkbox, 0, 4);

            //Tracking list
            materialRows.add(rowGrid);

            StackLayout rowWrapper = Containers.stack(Sizing.fill(100), Sizing.fixed(28));
            rowWrapper.child(rowGrid);

            Component hoverLayer = Components.box(Sizing.fill(100), Sizing.fill(100));

            hoverLayer.mouseEnter().subscribe(() -> {
                if (highlightMode == HighlightMode.MOUSE) {
                    highlight(index);
                }
            });
            rowWrapper.child(hoverLayer);

            return rowWrapper;
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

            visibleEntries.clear();

            rootComponent.surface(Surface.VANILLA_TRANSLUCENT)
                    .horizontalAlignment(HorizontalAlignment.LEFT)
                    .verticalAlignment(VerticalAlignment.BOTTOM);


            //Vertical scroll container for the material list
            FlowLayout scrollContent = Containers.verticalFlow(Sizing.content(), Sizing.content());

            //Showing all the materials
            int index = 0;
            for (MaterialEntry entry : materialEntries) {
                //Ignore if * in front (checked off)
                if (entry.marked) continue;

                visibleEntries.add(entry);

                ItemStack stack = resolveItemStack(entry.name);
                scrollContent.child(createMaterialRowGrid(entry, stack, index));
                index++;
            }
            if (initialHighlightIndex != null && initialHighlightIndex >= 0 && initialHighlightIndex < materialRows.size()) {
                highlight(initialHighlightIndex);
                initialHighlightIndex = null; // Reset so it doesn't re-highlight later
            }


            // Scroll container that fills screen
            this.scrollContainer = Containers.verticalScroll(
                    Sizing.fill(100),
                    Sizing.fill(90),
                    scrollContent
            );
            rootComponent.child(this.scrollContainer);

            // Bottom bar (fixed 30px height)
            rootComponent.child(
                    Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(30))
                            .child(
                                    Components.button(
                                            Text.literal("Select Material List"),
                                            button -> MinecraftClient.getInstance().setScreen(new selectScreen())
                                    ).sizing(Sizing.fixed(150), Sizing.fixed(20))
                            )
                            .padding(Insets.of(5))
                            .horizontalAlignment(HorizontalAlignment.LEFT));
        }
    }

    public class selectScreen extends BaseOwoScreen<FlowLayout> {

        private Path selectedFile = null;

        @Override
        protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
            return OwoUIAdapter.create(this, Containers::verticalFlow);
        }

        private void openMaterialList(Path file) {
            List<TomysMarterialListsJavaClient.MaterialEntry> entries =
                    TomysMarterialListsJavaClient.MaterialParser.parseMaterialFile(file);

            TomysMarterialListsJavaClient.saveLastUsedFile(file);
            TomysMarterialListsJavaClient.lastUsedMaterialFile = file;

            MinecraftClient.getInstance().setScreen(
                    new TomysMarterialListsJavaClient.MaterialListScreen(entries, file)
            );
        }

        @Override
        protected void build(FlowLayout rootComponent) {

            Path litematicaFolder = MinecraftClient.getInstance()
                            .runDirectory.toPath()
                            .resolve("config")
                            .resolve("litematica");

            List<Path> txtFiles = new ArrayList<>();
            try {
                if (Files.exists(litematicaFolder)) {
                    txtFiles = Files.list(litematicaFolder)
                            .filter(p -> p.toString().endsWith(".txt"))
                            .filter(p -> !p.toString().endsWith("data.txt")) // ignore the data file
                            .sorted()
                            .toList();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            rootComponent.surface(Surface.VANILLA_TRANSLUCENT)
                    .horizontalAlignment(HorizontalAlignment.LEFT)
                    .verticalAlignment(VerticalAlignment.TOP);

            FlowLayout scrollContent = Containers.verticalFlow(Sizing.content(), Sizing.content());
            for (Path file : txtFiles) {
                String fileName = file.getFileName().toString();
                CheckboxComponent checkBox = Components.checkbox(Text.literal(fileName));

                checkBox.onChanged(newValue -> {
                    if (newValue) {
                        selectedFile = file;

                        // Uncheck all other checkboxes inside wrappers
                        scrollContent.children().forEach(wrapper -> {
                            if (wrapper instanceof FlowLayout flow) {
                                flow.children().forEach(child -> {
                                    if (child instanceof CheckboxComponent cb && cb != checkBox) {
                                        cb.checked(false);
                                    }
                                });
                            }
                        });

                    } else if (selectedFile == file) {
                        selectedFile = null;
                    }
                });

                FlowLayout wrapper = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(20));
                wrapper.child(checkBox);
                wrapper.surface(Surface.DARK_PANEL);
                scrollContent.child(wrapper);
                scrollContent.children().forEach(child -> {
                    if (child instanceof CheckboxComponent cb && cb != checkBox) {
                        cb.checked(false);
                    }
                });
            }


            Component scrollContainer = Containers.verticalScroll(
                    Sizing.fill(100),
                    Sizing.fill(90),
                    scrollContent
            ).margins(Insets.of(10));
            rootComponent.child(scrollContainer);

            //Bar with buttons
            rootComponent.child(
                    Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(30))
                            //Open selected File button
                            .child(
                                    Components.button(
                                            Text.literal("Open Selected File"),
                                            button -> {
                                                if (selectedFile != null) {
                                                    openMaterialList(selectedFile);
                                                }
                                            }
                                    ).sizing(Sizing.fixed(160), Sizing.fixed(20))
                                            .margins(Insets.left(30))
                            )
                            //Back Button
                            .child(
                                    Components.button(
                                            Text.literal("Back"),
                                            button -> MinecraftClient.getInstance().setScreen(null)
                                    ).sizing(Sizing.fixed(80), Sizing.fixed(20))
                            )

            );

        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            // Close the menu on your keybind
            if (TomysMarterialListsJavaClient.openScreenKeyBind.matchesKey(keyCode, scanCode)) {
                MinecraftClient.getInstance().setScreen(null);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

}
