package com.dic.xsuper.dom.properties;


import com.dic.xsuper.dom.properties.borderunit.BorderSide;
import com.dic.xsuper.dom.properties.cssunit.CssContext;
import com.dic.xsuper.dom.properties.cssunit.CssParser;
import com.dic.xsuper.dom.properties.cssunit.CssValue;
import com.dic.xsuper.dom.properties.style.XplBorderStyle;

public class Border {
    private final BorderSide top;
    private final BorderSide right;
    private final BorderSide bottom;
    private final BorderSide left;

    // ─── Construtores ────────────────────────────────────────────────

    public Border(BorderSide top, BorderSide right, BorderSide bottom, BorderSide left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    public Border(CssValue width, int color, XplBorderStyle style) {
        BorderSide side = BorderSide.of(width, color, style);
        this.top = side;
        this.right = side;
        this.bottom = side;
        this.left = side;
    }

    public Border() {
        this(CssValue.zero(), 0, XplBorderStyle.NONE);
    }

    // ─── Getters ──────────────────────────────────────────────────────

    public BorderSide getTop() { return top; }
    public BorderSide getRight() { return right; }
    public BorderSide getBottom() { return bottom; }
    public BorderSide getLeft() { return left; }

    public boolean hasBorder() {
        return top.hasBorder() || right.hasBorder() || bottom.hasBorder() || left.hasBorder();
    }

    public boolean isUniform() {
        return top.getWidth().equals(right.getWidth()) &&
                right.getWidth().equals(bottom.getWidth()) &&
                bottom.getWidth().equals(left.getWidth()) &&
                top.getColor() == right.getColor() &&
                right.getColor() == bottom.getColor() &&
                bottom.getColor() == left.getColor() &&
                top.getStyle() == right.getStyle() &&
                right.getStyle() == bottom.getStyle() &&
                bottom.getStyle() == left.getStyle();
    }

    // ─── Fábricas (para uso programático) ──────────────────────────

    public static Border of(float widthPx, int color, XplBorderStyle style) {
        return new Border(CssValue.px(widthPx), color, style);
    }

    public static Border of(CssValue width, int color, XplBorderStyle style) {
        return new Border(width, color, style);
    }

    public static Border none() {
        return new Border();
    }

    /**
     * Cria uma borda a partir de strings CSS individuais.
     * Útil para o syncBoxModelFromStyles().
     */
    public static Border parse(String widthStr, String colorStr, String styleStr, String radiusStr) {
        CssValue w = CssParser.parseValue(widthStr);
        int c = parseHexColor(colorStr, 0xFF000000);
        XplBorderStyle s = XplBorderStyle.fromString(styleStr);
        return new Border(w, c, s);
    }

    // ─── Parser para bordas assimétricas (avançado) ────────────────

    public static Border parseAsymmetric(
            String topWidth, String rightWidth, String bottomWidth, String leftWidth,
            String topColor, String rightColor, String bottomColor, String leftColor,
            String topStyle, String rightStyle, String bottomStyle, String leftStyle) {

        BorderSide t = BorderSide.parse(topWidth, topColor, topStyle);
        BorderSide r = BorderSide.parse(rightWidth, rightColor, rightStyle);
        BorderSide b = BorderSide.parse(bottomWidth, bottomColor, bottomStyle);
        BorderSide l = BorderSide.parse(leftWidth, leftColor, leftStyle);
        return new Border(t, r, b, l);
    }

    // ─── Builder para configuração fluente ───────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private CssValue topW = CssValue.zero(), rightW = CssValue.zero(),
                bottomW = CssValue.zero(), leftW = CssValue.zero();
        private int topC = 0, rightC = 0, bottomC = 0, leftC = 0;
        private XplBorderStyle topS = XplBorderStyle.NONE,
                rightS = XplBorderStyle.NONE,
                bottomS = XplBorderStyle.NONE,
                leftS = XplBorderStyle.NONE;

        public Builder left(CssValue width, int color, XplBorderStyle style) {
            this.leftW = width; this.leftC = color; this.leftS = style; return this;
        }
        public Builder right(CssValue width, int color, XplBorderStyle style) {
            this.rightW = width; this.rightC = color; this.rightS = style; return this;
        }
        public Builder top(CssValue width, int color, XplBorderStyle style) {
            this.topW = width; this.topC = color; this.topS = style; return this;
        }
        public Builder bottom(CssValue width, int color, XplBorderStyle style) {
            this.bottomW = width; this.bottomC = color; this.bottomS = style; return this;
        }
        public Border build() {
            return new Border(
                    BorderSide.of(topW, topC, topS),
                    BorderSide.of(rightW, rightC, rightS),
                    BorderSide.of(bottomW, bottomC, bottomS),
                    BorderSide.of(leftW, leftC, leftS)
            );
        }
    }

    // ─── Auxiliar de parsing de hex ─────────────────────────────────

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

    // ─── Métodos para obter a borda unificada ──────────────────────────

    /**
     * Retorna o lado top (ou o único lado se a borda for uniforme).
     * Útil para shorthand.
     */
    public BorderSide getUniformSide() {
        if (isUniform()) {
            return top;
        }
        return null; // ou lançar exceção
    }

    /**
     * Retorna a largura total da borda (top + bottom ou left + right).
     * Para uso em cálculos de box model.
     */
    public float getHorizontalTotal(CssContext context) {
        return (float) (getLeft().getWidth().toPixels(context) + getRight().getWidth().toPixels(context));
    }

    public float getVerticalTotal(CssContext context) {
        return (float) (getTop().getWidth().toPixels(context) + getBottom().getWidth().toPixels(context));
    }

    /**
     * Retorna a largura da borda para um lado específico.
     */
    public float getWidthForSide(String side, CssContext context) {
        BorderSide sideObj = switch (side.toLowerCase()) {
            case "top" -> top;
            case "right" -> right;
            case "bottom" -> bottom;
            case "left" -> left;
            default -> null;
        };
        return sideObj != null ? (float) sideObj.getWidth().toPixels(context) : 0f;
    }

    /**
     * Retorna a cor da borda para um lado específico.
     */
    public int getColorForSide(String side) {
        return switch (side.toLowerCase()) {
            case "top" -> top.getColor();
            case "right" -> right.getColor();
            case "bottom" -> bottom.getColor();
            case "left" -> left.getColor();
            default -> 0;
        };
    }

    @Override
    public String toString() {
        return "Border{" +
                "top=" + top +
                ", right=" + right +
                ", bottom=" + bottom +
                ", left=" + left +
                '}';
    }
}