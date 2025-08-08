package me.tomyto.tomysMarterialListsJava.client;

public class MaterialEntry {
    public String name;
    public int total;
    public boolean marked;

    public MaterialEntry(String rawName, int total) {
        if (rawName.startsWith("*")) {
            this.name = rawName.substring(1).trim();
            this.marked = true;
        } else {
            this.name = rawName.trim();
            this.marked = false;
        }
        this.total = total;
    }

    public String cleanedName() {
        return name;
    }

    @Override
    public String toString() {
        String safeName = (name == null) ? "[null]" : name;
        return (marked ? "* " : "") + safeName + " - " + total;
    }
}
