package com.dic.xsuper.dom.properties.borderunit;


import com.dic.xsuper.dom.properties.style.XplColor;

public class BorderColor {
    private final int top;
    private final int right;
    private final int bottom;
    private final int left;

    private BorderColor(int top, int right, int bottom, int left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    public int getTop() { return top; }
    public int getRight() { return right; }
    public int getBottom() { return bottom; }
    public int getLeft() { return left; }

    public boolean isUniform() {
        return top == right && right == bottom && bottom == left;
    }

    public static BorderColor all(int color) {
        return new BorderColor(color, color, color, color);
    }

    public static BorderColor parse(String cssValue) {
        // Exemplo: "red", "#ff0000", "rgb(255,0,0)"
        // Por simplicidade, usamos XplColor.resolveColor() para cada lado,
        // mas se vier um único valor, aplicamos a todos.
        if (cssValue == null || cssValue.trim().isEmpty()) {
            return all(0xFF000000);
        }
        String[] parts = cssValue.trim().split("\\s+");
        int[] colors = new int[4];
        for (int i = 0; i < 4; i++) {
            if (i < parts.length) {
                colors[i] = XplColor.resolveColor(parts[i].trim());
            } else {
                colors[i] = colors[i == 0 ? 0 : i - 1]; // repete o último
            }
        }
        // Se veio 1 valor: todos iguais; 2: top=left, right=bottom; etc.
        if (parts.length == 1) {
            int c = colors[0];
            return new BorderColor(c, c, c, c);
        } else if (parts.length == 2) {
            int c1 = colors[0];
            int c2 = colors[1];
            return new BorderColor(c1, c2, c1, c2);
        } else if (parts.length == 3) {
            int c1 = colors[0];
            int c2 = colors[1];
            int c3 = colors[2];
            return new BorderColor(c1, c2, c3, c2);
        } else {
            return new BorderColor(colors[0], colors[1], colors[2], colors[3]);
        }
    }

    public static BorderColor of(int top, int right, int bottom, int left) {
        return new BorderColor(top, right, bottom, left);
    }
}