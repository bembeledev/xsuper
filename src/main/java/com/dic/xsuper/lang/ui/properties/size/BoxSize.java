package com.dic.xsuper.lang.ui.properties.size;


import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;

public class BoxSize {
    private final Dimension width;
    private final Dimension height;
    private final Dimension minWidth;
    private final Dimension maxWidth;
    private final Dimension minHeight;
    private final Dimension maxHeight;

    public BoxSize(Dimension width, Dimension height,
                   Dimension minWidth, Dimension maxWidth,
                   Dimension minHeight, Dimension maxHeight) {
        this.width = width;
        this.height = height;
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }

    // ─── Getters ──────────────────────────────────────────────────────

    public Dimension getWidth() { return width; }
    public Dimension getHeight() { return height; }
    public Dimension getMinWidth() { return minWidth; }
    public Dimension getMaxWidth() { return maxWidth; }
    public Dimension getMinHeight() { return minHeight; }
    public Dimension getMaxHeight() { return maxHeight; }

    // ─── Métodos de conveniência ────────────────────────────────────

    public float getWidthPixels(CssContext context) {
        return width.toPixels(context);
    }
    public float getHeightPixels(CssContext context) {
        return height.toPixels(context);
    }
    public float getMinWidthPixels(CssContext context) {
        return minWidth.toPixels(context);
    }
    public float getMaxWidthPixels(CssContext context) {
        return maxWidth.toPixels(context);
    }
    public float getMinHeightPixels(CssContext context) {
        return minHeight.toPixels(context);
    }
    public float getMaxHeightPixels(CssContext context) {
        return maxHeight.toPixels(context);
    }

    // ─── Factory a partir de um elemento ────────────────────────────

    // ─── Valores padrão ──────────────────────────────────────────────

    public static BoxSize defaults() {
        return new BoxSize(
                Dimension.none(), Dimension.none(),
                Dimension.none(), Dimension.none(),
                Dimension.none(), Dimension.none()
        );
    }
}