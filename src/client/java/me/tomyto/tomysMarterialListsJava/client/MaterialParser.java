package me.tomyto.tomysMarterialListsJava.client;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class MaterialParser {

    private static final Pattern MATERIAL_LINE_PATTERN =
            Pattern.compile("\\|\\s(.+?)\\s+\\|\\s+(\\d+)\\s+\\|\\s+(\\d+)\\s+\\|\\s+(\\d+)\\s+\\|");

    public static List<MaterialEntry> parseMaterialFile(Path filePath) {
        List<MaterialEntry> materials = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(filePath);

            for (String line : lines) {
                // Ignore irrelevant lines
                if (!line.contains("|")) continue;

                String[] parts = line.split("\\|");

                if (parts.length < 5) continue; // Not enough columns

                String rawName = parts[1].trim();
                String countStr = parts[3].trim(); // We're using the 3rd column: missing count

                if (rawName.isEmpty() || countStr.isEmpty()) continue;

                boolean marked = rawName.startsWith("*");
                String name = marked ? rawName.substring(1).trim() : rawName;

                int missing;
                try {
                    missing = Integer.parseInt(countStr);
                } catch (NumberFormatException e) {
                    continue; // Skip lines with invalid numbers
                }

                materials.add(new MaterialEntry(name, missing));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return materials;
    }

}