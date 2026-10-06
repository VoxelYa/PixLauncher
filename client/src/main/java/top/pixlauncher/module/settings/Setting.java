package top.pixlauncher.module.settings;

/**
 * Minimal settings base (Boolean/Number/Mode/Color/Bind subclasses follow).
 * Visibility suppliers and change listeners arrive with the ClickGUI (M3).
 */
public abstract class Setting<T> {

    private final String id;
    private final String label;
    private T value;

    protected Setting(String id, String label, T defaultValue) {
        this.id = id;
        this.label = label;
        this.value = defaultValue;
    }

    public String id() { return id; }
    public String label() { return label; }
    public T get() { return value; }
    public void set(T value) { this.value = value; }
}
