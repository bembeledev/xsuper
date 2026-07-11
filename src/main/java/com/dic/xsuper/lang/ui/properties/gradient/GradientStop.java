package com.dic.xsuper.lang.ui.properties.gradient;

import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;
import com.dic.xsuper.lang.ui.properties.cssunit.CssValue;

public class GradientStop {
    private final int color;           // ARGB
    private final CssValue position;   // posição (em % ou comprimento)
    private final float opacity;       // 0.0 a 1.0 (default 1.0)
    private final float blur;          // raio de desfoque (em px, default 0)
    private final float radius;        // raio individual (para gradientes radiais customizados)
    private final Shape shape;         // CÍRCULO, ELIPSE, POLÍGONO (para custom-gradient)


    public enum Shape {
        CIRCLE, ELLIPSE, POLYGON
    }

    // ─── Construtor completo ──────────────────────────────────────────

    public GradientStop(int color, CssValue position, float opacity, float blur, float radius, Shape shape) {
        this.color = color;
        this.position = position;
        this.opacity = Math.max(0, Math.min(1, opacity));
        this.blur = Math.max(0, blur);
        this.radius = Math.max(0, radius);
        this.shape = shape != null ? shape : Shape.CIRCLE;
    }

    // ─── Construtor simplificado (cor + posição) ─────────────────────

    public GradientStop(int color, CssValue position) {
        this(color, position, 1.0f, 0f, 0f, Shape.CIRCLE);
    }

    // ─── Getters ──────────────────────────────────────────────────────

    public int getColor() { return color; }
    public CssValue getPosition() { return position; }
    public float getOpacity() { return opacity; }
    public float getBlur() { return blur; }
    public float getRadius() { return radius; }
    public Shape getShape() { return shape; }

    // ─── Cor com opacidade aplicada ──────────────────────────────────

    public int getColorWithOpacity() {
        int alpha = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int newAlpha = (int) (alpha * opacity);
        return (newAlpha << 24) | (r << 16) | (g << 8) | b;
    }

    // ─── Conversão de posição para pixels (com contexto) ─────────────
    public float getPositionPixels(CssContext context, float length) {
        // Se for percentagem, o valor numérico é usado diretamente
        if (position.isPercent()) {
            return (float) (position.getValue() / 100.0 * length);
        }
        // Para outras unidades (px, em, etc.), usa o contexto
        return (float) position.toPixels(context);
    }
}