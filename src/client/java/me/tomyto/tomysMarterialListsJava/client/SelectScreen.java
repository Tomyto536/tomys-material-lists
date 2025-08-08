package me.tomyto.tomysMarterialListsJava.client;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static me.tomyto.tomysMarterialListsJava.client.FileUtils.saveLastUsedFile;

public class SelectScreen extends BaseOwoScreen<FlowLayout> {

    private Path selectedFile = null;

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    private void openMaterialList(Path file) {
        List<MaterialEntry> entries =
                MaterialParser.parseMaterialFile(file);

        saveLastUsedFile(file);
        TomysMarterialListsJavaClient.lastUsedMaterialFile = file;

        MinecraftClient.getInstance().setScreen(
                new MaterialListScreen(entries, file)
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