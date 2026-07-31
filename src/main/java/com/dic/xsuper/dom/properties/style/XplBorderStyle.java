package com.dic.xsuper.dom.properties.style;

public enum XplBorderStyle {
    NONE,
    SOLID,
    DASHED,  // Linha tracejada (Photoshop Stroke Dash)
    DOTTED,  // Linha pontilhada (Photoshop Dots)
    DOUBLE;  // Linha dupla decorativa

    public static XplBorderStyle fromString(String text) {
        if (text == null) return SOLID;
        String clean = text.toUpperCase().trim();
        for (XplBorderStyle style : values()) {
            if (style.name().equals(clean)) return style;
        }
        return SOLID;
    }
}
