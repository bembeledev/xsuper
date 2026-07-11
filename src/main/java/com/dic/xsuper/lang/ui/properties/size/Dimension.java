package com.dic.xsuper.lang.ui.properties.size;

import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;
import com.dic.xsuper.lang.ui.properties.cssunit.CssParser;
import com.dic.xsuper.lang.ui.properties.cssunit.CssValue;

public class Dimension {
    public static final String AUTO = "auto";
    public static final String NONE = "none";

    private final CssValue value;
    private final boolean isAuto;
    private final boolean isNone;

    private Dimension(CssValue value, boolean isAuto, boolean isNone) {
        this.value = value;
        this.isAuto = isAuto;
        this.isNone = isNone;
    }

    // ─── Fábricas ──────────────────────────────────────────────────────

    public static Dimension of(CssValue value) {
        return new Dimension(value, false, false);
    }

    public static Dimension of(float px) {
        return of(CssValue.px(px));
    }

    public static Dimension auto() {
        return new Dimension(CssValue.zero(), true, false);
    }

    public static Dimension none() {
        return new Dimension(CssValue.zero(), false, true);
    }

    // ─── Parsing ──────────────────────────────────────────────────────

    public static Dimension parse(String str) {
        if (str == null) return none();
        String trimmed = str.trim().toLowerCase();
        if (trimmed.equals(AUTO)) return auto();
        if (trimmed.equals(NONE)) return none();
        CssValue val = CssParser.parseValue(str);
        return of(val);
    }

    // ─── Getters ──────────────────────────────────────────────────────

    public CssValue getValue() { return value; }
    public boolean isAuto() { return isAuto; }
    public boolean isNone() { return isNone; }

    // ─── Conversão para pixels ──────────────────────────────────────

    public float toPixels(CssContext context) {
        if (isAuto || isNone) return 0f;
        return (float) value.toPixels(context);
    }

    // ─── Verificações ─────────────────────────────────────────────────

    public boolean isFixed() {
        return !isAuto && !isNone;
    }

    public boolean isZero() {
        return isFixed() && value.getValue() == 0;
    }

    @Override
    public String toString() {
        if (isAuto) return AUTO;
        if (isNone) return NONE;
        return value.toString();
    }
}