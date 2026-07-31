package com.dic.xsuper.dom.properties.cssunit;

/**
 * Contexto para conversão de unidades CSS.
 * Fornece valores de referência: tamanho da fonte, viewport, etc.
 */
public class CssContext {
    private double fontSize = 16.0;        // font-size padrão do elemento
    private double rootFontSize = 16.0;     // font-size da raiz (html)
    private double xHeight = 8.0;           // altura da letra 'x' (estimada)
    private double zeroWidth = 8.0;         // largura do '0' (estimada)
    private double viewportWidth = 1920.0;  // largura da viewport
    private double viewportHeight = 1080.0; // altura da viewport
    private double parentSize = 0.0;        // tamanho do pai (para %)

    // --- Getters e Setters ---

    public double getFontSize() { return fontSize; }
    public void setFontSize(double fontSize) { this.fontSize = fontSize; }

    public double getRootFontSize() { return rootFontSize; }
    public void setRootFontSize(double rootFontSize) { this.rootFontSize = rootFontSize; }

    public double getXHeight() { return xHeight; }
    public void setXHeight(double xHeight) { this.xHeight = xHeight; }

    public double getZeroWidth() { return zeroWidth; }
    public void setZeroWidth(double zeroWidth) { this.zeroWidth = zeroWidth; }

    public double getViewportWidth() { return viewportWidth; }
    public void setViewportWidth(double viewportWidth) { this.viewportWidth = viewportWidth; }

    public float getViewportHeight() { return (float) viewportHeight; }
    public void setViewportHeight(double viewportHeight) { this.viewportHeight = viewportHeight; }

    public double getParentSize() { return parentSize; }
    public void setParentSize(double parentSize) { this.parentSize = parentSize; }

    // --- Builder fluente ---

    public CssContext withFontSize(double fontSize) { this.fontSize = fontSize; return this; }
    public CssContext withRootFontSize(double rootFontSize) { this.rootFontSize = rootFontSize; return this; }
    public CssContext withViewport(double width, double height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
        return this;
    }
    public CssContext withParentSize(double parentSize) { this.parentSize = parentSize; return this; }
}