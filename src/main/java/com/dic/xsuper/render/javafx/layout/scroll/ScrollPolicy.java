package com.dic.xsuper.render.javafx.layout.scroll;

public enum ScrollPolicy {
    VISIBLE("visible"),
    HIDDEN("hidden"),
    SCROLL("scroll"),
    AUTO("auto");

    private final String value;

    ScrollPolicy(String value) { this.value = value; }

    public String getValue() { return value; }

    public static ScrollPolicy fromString(String value) {
        if (value == null) return VISIBLE;
        return switch (value.trim().toLowerCase()) {
            case "hidden" -> HIDDEN;
            case "scroll" -> SCROLL;
            case "auto" -> AUTO;
            default -> VISIBLE;
        };
    }

    public boolean showsScrollbars() {
        return this == SCROLL || this == AUTO;
    }

    public boolean isVisible() { return this == VISIBLE; }
    public boolean isHidden() { return this == HIDDEN; }
}