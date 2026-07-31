package com.dic.xsuper.render.javafx.css.media;

import java.util.Map;

public class XplMediaNode {
    public final String condition;
    public final Map<String, Map<String, String>> selectorsAndStyles;
    public boolean isActive = false;

    // Sensores pré-calculados
    private double maxWidth = Double.MAX_VALUE;
    private double minWidth = 0;

    public XplMediaNode(String condition, Map<String, Map<String, String>> selectorsAndStyles) {
        this.condition = condition;
        this.selectorsAndStyles = selectorsAndStyles;
        parseCondition(condition);
    }

    private void parseCondition(String cond) {
        String clean = cond.toLowerCase().replace(" ", "");
        if (clean.contains("max-width:")) {
            maxWidth = extractPixels(clean, "max-width:");
        }
        if (clean.contains("min-width:")) {
            minWidth = extractPixels(clean, "min-width:");
        }
    }

    public boolean evaluate(double currentWidth) {
        return currentWidth <= maxWidth && currentWidth >= minWidth;
    }

    private double extractPixels(String source, String key) {
        try {
            int start = source.indexOf(key) + key.length();
            int end = source.indexOf("px", start);
            if (end == -1) end = source.indexOf(")", start);
            return Double.parseDouble(source.substring(start, end).trim());
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public String toString() {
        return "XplMediaNode{" +
                "condition='" + condition + '\'' +
                ", selectorsAndStyles=" + selectorsAndStyles +
                ", isActive=" + isActive +
                ", maxWidth=" + maxWidth +
                ", minWidth=" + minWidth +
                '}';
    }
}