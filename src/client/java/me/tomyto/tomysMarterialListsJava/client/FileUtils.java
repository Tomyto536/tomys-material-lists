package me.tomyto.tomysMarterialListsJava.client;

import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.*;

public class FileUtils {

    private static final Path dataFile = MinecraftClient.getInstance().runDirectory.toPath()
            .resolve("config")
            .resolve("litematica")
            .resolve("tomys_material_lists_data.txt");

    public static void saveLastUsedFile(Path path) {
        ensureFileExists();
        try {
            Files.writeString(dataFile, path.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Path loadLastUsedFilePath() {
        ensureFileExists();
        try {
            if (Files.exists(dataFile)) {
                return Paths.get(Files.readString(dataFile));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void ensureFileExists() {
        if (Files.notExists(dataFile)) {
            try {
                Files.createFile(dataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
