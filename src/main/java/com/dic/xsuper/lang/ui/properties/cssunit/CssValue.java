package com.dic.xsuper.lang.ui.properties.cssunit;

import java.util.Objects;

/**
 * Representa um valor CSS com a sua unidade.
 * Suporta operações aritméticas básicas e conversão para pixels.
 */
public class CssValue {
    private final double value;
    private final CssUnit unit;

    public CssValue(double value, CssUnit unit) {
        this.value = value;
        this.unit = unit != null ? unit : CssUnit.NONE;
    }

    // --- Getters ---
    public double getValue() { return value; }
    public CssUnit getUnit() { return unit; }

    public boolean isAbsolute() {
        return unit == CssUnit.PX || unit == CssUnit.PT || unit == CssUnit.PC ||
                unit == CssUnit.IN || unit == CssUnit.CM || unit == CssUnit.MM || unit == CssUnit.Q;
    }

    public boolean isRelativeToFont() {
        return unit == CssUnit.EM || unit == CssUnit.REM || unit == CssUnit.EX || unit == CssUnit.CH;
    }

    public boolean isRelativeToViewport() {
        return unit == CssUnit.VW || unit == CssUnit.VH || unit == CssUnit.VMIN || unit == CssUnit.VMAX;
    }

    public boolean isAuto() { return unit == CssUnit.AUTO; }
    public boolean isPercent() { return unit == CssUnit.PERCENT; }
    public boolean isNone() { return unit == CssUnit.NONE; }
    public boolean isFr() { return unit == CssUnit.FR; }

    /**
     * Converte este valor para pixels, com base no contexto fornecido.
     */
    public double toPixels(CssContext context) {
        if (context == null) return value;

        return switch (unit) {
            case PX -> value;
            case PT -> value * 1.333333; // 1pt = 1.333px
            case PC -> value * 16;       // 1pc = 16px
            case IN -> value * 96;
            case CM -> value * 37.795;
            case MM -> value * 3.7795;
            case Q -> value * 0.9449;
            case EM -> value * context.getFontSize();
            case REM -> value * context.getRootFontSize();
            case EX -> value * context.getXHeight();
            case CH -> value * context.getZeroWidth();
            case VW -> value * context.getViewportWidth() / 100.0;
            case VH -> value * context.getViewportHeight() / 100.0;
            case VMIN -> value * Math.min(context.getViewportWidth(), context.getViewportHeight()) / 100.0;
            case VMAX -> value * Math.max(context.getViewportWidth(), context.getViewportHeight()) / 100.0;
            case PERCENT ->
                // Percentagem depende do contexto: largura para margin/padding, altura para outros
                    value * context.getParentSize() / 100.0;
            default -> value; // auto é resolvido pelo layout, devolve 0 por enquanto
        };
    }

    // --- Operações aritméticas ---

    public CssValue plus(CssValue other) {
        if (other == null) return this;
        if (this.unit != other.unit) {
            // Converte ambos para px e soma
            CssContext temp = new CssContext();
            double v1 = this.toPixels(temp);
            double v2 = other.toPixels(temp);
            return new CssValue(v1 + v2, CssUnit.PX);
        }
        return new CssValue(this.value + other.value, this.unit);
    }

    public CssValue minus(CssValue other) {
        if (other == null) return this;
        if (this.unit != other.unit) {
            CssContext temp = new CssContext();
            double v1 = this.toPixels(temp);
            double v2 = other.toPixels(temp);
            return new CssValue(v1 - v2, CssUnit.PX);
        }
        return new CssValue(this.value - other.value, this.unit);
    }

    public CssValue times(double factor) {
        return new CssValue(this.value * factor, this.unit);
    }

    // --- Parsing estático ---

    public static CssValue parse(String str) {
        return CssParser.parseValue(str);
    }

    // --- Utilitários ---

    public static CssValue px(double px) { return new CssValue(px, CssUnit.PX); }
    public static CssValue percent(double pct) { return new CssValue(pct, CssUnit.PERCENT); }
    public static CssValue auto() { return new CssValue(0, CssUnit.AUTO); }
    public static CssValue zero() { return new CssValue(0, CssUnit.PX); }

    @Override
    public String toString() {
        if (unit == CssUnit.NONE) return String.valueOf(value);
        if (unit == CssUnit.AUTO) return "auto";
        if (unit == CssUnit.PERCENT) return value + "%";
        return value + unit.name().toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CssValue that)) return false;
        return Double.compare(that.value, value) == 0 && unit == that.unit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, unit);
    }

    public boolean isZero() {
        return this.value == 0 || (this.unit == CssUnit.AUTO && this.value == 0);
    }
}