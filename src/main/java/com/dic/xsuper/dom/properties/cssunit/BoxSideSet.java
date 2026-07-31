package com.dic.xsuper.dom.properties.cssunit;

import java.util.Objects;

/**
 * Classe base para propriedades com 4 lados (top, right, bottom, left).
 * Usada por Margin, Padding, Border, etc.
 */
public abstract class BoxSideSet {

    protected final CssValue top;
    protected final CssValue right;
    protected final CssValue bottom;
    protected final CssValue left;

    protected BoxSideSet(CssValue top, CssValue right, CssValue bottom, CssValue left) {
        this.top = Objects.requireNonNull(top);
        this.right = Objects.requireNonNull(right);
        this.bottom = Objects.requireNonNull(bottom);
        this.left = Objects.requireNonNull(left);
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public CssValue getTop() { return top; }
    public CssValue getRight() { return right; }
    public CssValue getBottom() { return bottom; }
    public CssValue getLeft() { return left; }

    // ─── Utilitários ────────────────────────────────────────────────────────

    public boolean isAllAuto() {
        return top.isAuto() && right.isAuto() && bottom.isAuto() && left.isAuto();
    }

    public boolean isZero() {
        return top.isZero() && right.isZero() && bottom.isZero() && left.isZero();
    }

    public boolean isAllEqual() {
        return top.equals(right) && right.equals(bottom) && bottom.equals(left);
    }

    // ─── Conversão para pixels (com contexto) ──────────────────────────────

    public float getTopPixels(CssContext context) {
        return (float) top.toPixels(context);
    }

    public float getRightPixels(CssContext context) {
        return (float) right.toPixels(context);
    }

    public float getBottomPixels(CssContext context) {
        return (float) bottom.toPixels(context);
    }

    public float getLeftPixels(CssContext context) {
        return (float) left.toPixels(context);
    }

    public float getHorizontalTotal(CssContext context) {
        return getLeftPixels(context) + getRightPixels(context);
    }

    public float getVerticalTotal(CssContext context) {
        return getTopPixels(context) + getBottomPixels(context);
    }

    // ─── Parsing (factory method para as subclasses) ────────────────────────

    /**
     * Parseia uma string CSS com 1-4 valores e cria uma instância da subclasse.
     * As subclasses devem fornecer a factory.
     */
    protected static BoxSideSet parse(String cssValue, BoxSideSetFactory factory) {
        if (cssValue == null || cssValue.trim().isEmpty()) {
            CssValue zero = CssValue.zero();
            return factory.create(zero, zero, zero, zero);
        }

        String[] parts = cssValue.trim().split("\\s+");
        CssValue[] values = new CssValue[4];
        // Fallback: todos a zero
        for (int i = 0; i < 4; i++) {
            values[i] = CssValue.zero();
        }

        // Converte cada parte
        for (int i = 0; i < Math.min(parts.length, 4); i++) {
            values[i] = CssParser.parseValue(parts[i]);
        }

        // Aplica a lógica CSS (1-4 valores)
        if (parts.length == 1) {
            CssValue v = values[0];
            return factory.create(v, v, v, v);
        } else if (parts.length == 2) {
            CssValue v1 = values[0];
            CssValue v2 = values[1];
            return factory.create(v1, v2, v1, v2); // top=left, right=bottom
        } else if (parts.length == 3) {
            CssValue v1 = values[0];
            CssValue v2 = values[1];
            CssValue v3 = values[2];
            return factory.create(v1, v2, v3, v2); // top, right, bottom, left
        } else {
            CssValue v1 = values[0];
            CssValue v2 = values[1];
            CssValue v3 = values[2];
            CssValue v4 = values[3];
            return factory.create(v1, v2, v3, v4);
        }
    }

    // ─── Factory funcional ──────────────────────────────────────────────────

    @FunctionalInterface
    protected interface BoxSideSetFactory {
        BoxSideSet create(CssValue top, CssValue right, CssValue bottom, CssValue left);
    }

    // ─── toString ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{top=" + top + ", right=" + right +
                ", bottom=" + bottom + ", left=" + left + "}";
    }
}