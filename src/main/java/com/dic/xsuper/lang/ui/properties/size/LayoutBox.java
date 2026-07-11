package com.dic.xsuper.lang.ui.properties.size;

/**
 * Armazena as dimensões calculadas (em pixels absolutos) após o layout.
 * Estas são as dimensões finais que a renderização usa para desenhar o elemento.
 */
public class LayoutBox {
    private float width = 0;
    private float height = 0;
    private float x = 0;
    private float y = 0;

    // ─── Getters ──────────────────────────────────────────────────────

    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getX() { return x; }
    public float getY() { return y; }

    // ─── Setters ──────────────────────────────────────────────────────

    public void setWidth(float width) { this.width = width; }
    public void setHeight(float height) { this.height = height; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }

    // ─── Utilitários ─────────────────────────────────────────────────

    public void reset() {
        width = 0;
        height = 0;
        x = 0;
        y = 0;
    }

    @Override
    public String toString() {
        return "LayoutBox{x=" + x + ", y=" + y + ", w=" + width + ", h=" + height + "}";
    }
}