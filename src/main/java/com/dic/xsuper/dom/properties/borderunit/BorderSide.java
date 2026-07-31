package com.dic.xsuper.dom.properties.borderunit;


import com.dic.xsuper.dom.properties.cssunit.CssParser;
import com.dic.xsuper.dom.properties.cssunit.CssValue;
import com.dic.xsuper.dom.properties.style.XplBorderStyle;

public class BorderSide {
    private final CssValue width;
    private final int color;
    private final XplBorderStyle style;

    public BorderSide(CssValue width, int color, XplBorderStyle style) {
        this.width = width;
        this.color = color;
        this.style = style;
    }

    public CssValue getWidth() { return width; }
    public int getColor() { return color; }
    public XplBorderStyle getStyle() { return style; }

    public boolean hasBorder() {
        return style != XplBorderStyle.NONE && width != null && width.getValue() > 0;
    }

    public static BorderSide none() {
        return new BorderSide(CssValue.zero(), 0, XplBorderStyle.NONE);
    }

    public static BorderSide of(CssValue width, int color, XplBorderStyle style) {
        return new BorderSide(width, color, style);
    }

    public static BorderSide parse(String widthStr, String colorStr, String styleStr) {
        CssValue w = CssParser.parseValue(widthStr);
        int c = parseHexColor(colorStr, 0xFF000000);
        XplBorderStyle s = XplBorderStyle.fromString(styleStr);
        return new BorderSide(w, c, s);
    }

    private static int parseHexColor(String hexStr, int defaultColor) {
        if (hexStr == null || hexStr.trim().isEmpty()) return defaultColor;
        try {
            String clean = hexStr.replace("#", "").trim();
            if (clean.length() == 6) clean = "FF" + clean;
            return (int) Long.parseLong(clean, 16);
        } catch (NumberFormatException e) {
            return defaultColor;
        }
    }

    @Override
    public String toString() {
        return "BorderSide{" +
                "width=" + width +
                ", color=" + color +
                ", style=" + style +
                '}';
    }
}