package com.dic.xsuper.render.javafx.animation;

import java.util.HashMap;
import java.util.Map;

/**
 * Representa um keyframe dentro de uma animação @keyframes.
 * Contém a percentagem (ou "from"/"to") e um mapa de propriedades/valores.
 */
public class XplKeyframe {
    private final double position; // 0.0 a 1.0 ou -1 para "from"/"to" (resolvido depois)
    private final Map<String, String> styles = new HashMap<>();

    public XplKeyframe(double position) {
        this.position = position;
    }

    public XplKeyframe(String positionStr) {
        if ("from".equalsIgnoreCase(positionStr)) {
            this.position = 0.0;
        } else if ("to".equalsIgnoreCase(positionStr)) {
            this.position = 1.0;
        } else {
            // Remove o '%'
            String num = positionStr.replace("%", "").trim();
            this.position = Double.parseDouble(num) / 100.0;
        }
    }

    public double getPosition() { return position; }
    public Map<String, String> getStyles() { return styles; }

    public void addStyle(String property, String value) {
        styles.put(property, value);
    }
}