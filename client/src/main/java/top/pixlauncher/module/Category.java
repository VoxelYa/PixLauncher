package top.pixlauncher.module;

public enum Category {
    HUD("Interface"),
    RENDER("Render"),
    OPTIMIZE("Optimize"),
    UTILITY("Utility"),
    CORE("Core");

    private final String label;

    Category(String label) { this.label = label; }

    public String label() { return label; }
}
