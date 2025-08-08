package me.tomyto.tomysMarterialListsJava.client;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Insets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.sound.SoundEvents;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static me.tomyto.tomysMarterialListsJava.client.TomysMarterialListsJavaClient.*;

public class MaterialListScreen extends BaseOwoScreen<FlowLayout> {

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
                //Find the first marked item - most recently checked off
                for (MaterialEntry entry : materialEntries) {
                    if (entry.marked) {
                        checkItemOff(entry, false); //Un-mark it
                        writeToFile();

                        //Reload the screen
                        MaterialListScreen newScreen = new MaterialListScreen(materialEntries, materialFilePath);

                        //Find is index so we can highlight it
                        int newIndex = 0;
                        for(MaterialEntry e : materialEntries) {
                            if (e == entry) break;
                            if (!e.marked) newIndex++;
                        }
                        newScreen.setInitialHighlightIndex(newIndex);
                        MinecraftClient.getInstance().setScreen(newScreen);
                        break;
                    }
                }
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
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
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
            MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(
                            SoundEvents.UI_BUTTON_CLICK, 1.0f
                    )
            );
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

        io.wispforest.owo.ui.core.Component row = materialRows.get(index);
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
                MinecraftClient.getInstance().getSoundManager().play(
                        PositionedSoundInstance.master(
                                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f
                        )
                );
            }
            materialEntries.remove(entry);
            materialEntries.add(0, entry);
        } else {
            if (entry.marked) {
                entry.name = entry.name.replaceFirst("^\\*\\s*", "");
                entry.marked = false;

                MinecraftClient.getInstance().getSoundManager().play(
                        PositionedSoundInstance.master(
                                SoundEvents.BLOCK_STONE_PLACE, 1.0f
                        )
                );
            }
        }
    }

    public io.wispforest.owo.ui.core.Component createMaterialRowGrid(MaterialEntry entry, ItemStack itemStack, int index) {
        GridLayout rowGrid = Containers.grid(Sizing.fill(100), Sizing.fixed(28), 1, 5);
        rowGrid.surface(Surface.DARK_PANEL); //Offer light and dark mode?

        // Item Icon ---------------------------------------------------------
        //if (itemStack != null && !itemStack.isEmpty()) {
            //Component iconComponent = Components.item(itemStack)
                    //.sizing(Sizing.fixed(20), Sizing.fixed(20))
                    //.margins(Insets.both(12, 4));

            //rowGrid.child(iconComponent, 0, 0);
        //}
        if (itemStack == null || itemStack.isEmpty() || itemStack.getItem() == null) {
            itemStack = new ItemStack(Items.BARRIER);
        }

        Component iconComponent = Components.item(itemStack)
                .sizing(Sizing.fixed(20), Sizing.fixed(20))
                .margins(Insets.both(12, 4));

        rowGrid.child(iconComponent, 0, 0);

        // Item Name ----------------------------------------------------------
        String safeName = entry.name != null ? entry.name : "<unknown>";
        io.wispforest.owo.ui.core.Component nameLabel = Components.label(Text.literal(safeName))
                .horizontalTextAlignment(HorizontalAlignment.LEFT);

        FlowLayout nameContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(28));
        nameContainer.verticalAlignment(VerticalAlignment.CENTER);
        nameContainer.child(nameLabel);
        rowGrid.child(nameContainer.verticalAlignment(VerticalAlignment.CENTER), 0, 1);


        // Grid with 6 columns (3 pairs): number, icon, number, icon, number, icon
        int shulkers = numberShulkers(entry);
        int stacks = numberStacks(entry);
        int items = numberItems(entry);
        GridLayout quantityGrid = Containers.grid(Sizing.fixed(150), Sizing.content(), 1, 6); // ← Fixed column width!
        quantityGrid.verticalAlignment(VerticalAlignment.CENTER);

        // SHULKERS
        if (shouldDisplay(0, shulkers, stacks, items)) {
            quantityGrid.child(
                    Components.label(Text.literal(String.valueOf(numberShulkers(entry))))
                            .margins(Insets.right(4)),
                    0, 0
            );
            quantityGrid.child(
                    icon("shulker_icon2").sizing(Sizing.fixed(16), Sizing.fixed(16))
                            .margins(Insets.right(10)),
                    0, 1
            );
        }

        // STACKS
        if (shouldDisplay(1, shulkers, stacks, items)) {
            quantityGrid.child(
                    Components.label(Text.literal(String.valueOf(numberStacks(entry))))
                            .margins(Insets.right(4)),
                    0, 2
            );
            quantityGrid.child(
                    icon("stack_icon").sizing(Sizing.fixed(16), Sizing.fixed(16))
                            .margins(Insets.right(10)),
                    0, 3
            );
        }

        // ITEMS
        if (shouldDisplay(2, shulkers, stacks, items)) {
            quantityGrid.child(
                    Components.label(Text.literal(String.valueOf(numberItems(entry))))
                            .margins(Insets.right(4)),
                    0, 4
            );
            quantityGrid.child(
                    icon("block_icon").sizing(Sizing.fixed(16), Sizing.fixed(16)),
                    0, 5
            );
        }

        // Add to rowGrid
        rowGrid.child(
                quantityGrid
                        .horizontalAlignment(HorizontalAlignment.RIGHT)
                        .verticalAlignment(VerticalAlignment.CENTER)
                        .margins(Insets.of(6)),
                0, 2
        );

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
        checkbox.margins(io.wispforest.owo.ui.core.Insets.both(10, 6));

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
        if (name == null || name.isBlank()) {
            System.out.println("[MaterialList] Empty or null name — using BARRIER");
            return new ItemStack(Items.BARRIER);
        }

        Identifier id = Identifier.tryParse(name.toLowerCase().replace(" ", "_"));
        if (id != null && Registries.ITEM.containsId(id)) {
            Item item = Registries.ITEM.get(id);
            if (item != null) {
                return new ItemStack(item);
            } else {
                System.out.println("[MaterialList] Registry contained ID but item was null: " + id);
            }
        }

        // Fallback: fuzzy match against translated name
        for (Identifier registryId : Registries.ITEM.getIds()) {
            Item item = Registries.ITEM.get(registryId);
            if (item != null) {
                String displayName = item.getName().getString();
                if (displayName.equalsIgnoreCase(name)) {
                    return new ItemStack(item);
                }
            }
        }

        // If we reach here, no match was found
        System.out.println("[MaterialList] Could not resolve item for name: '" + name + "' — using BARRIER");
        return new ItemStack(Items.BARRIER);
    }

    private boolean shouldDisplay(int index, int shulkers, int stacks, int items) {
        if (index == 0) return shulkers > 0;
        if (index == 1) return shulkers > 0 || stacks > 0;
        if (index == 2) return shulkers > 0 || stacks > 0 || items > 0;
        return false;
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

            String cleanName = entry.name.replaceFirst("^\\*\\s*", "").trim(); // strip leading * and whitespace
            ItemStack stack = resolveItemStack(cleanName);
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
                                        button -> MinecraftClient.getInstance().setScreen(new SelectScreen())
                                ).sizing(Sizing.fixed(150), Sizing.fixed(20))
                        )
                        .padding(Insets.of(5))
                        .horizontalAlignment(HorizontalAlignment.LEFT));
    }
}