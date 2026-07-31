package com.dic.xsuper.dom.properties;


import com.dic.xsuper.dom.properties.cssunit.BoxSideSet;
import com.dic.xsuper.dom.properties.cssunit.CssUnit;
import com.dic.xsuper.dom.properties.cssunit.CssValue;

public class Margin extends BoxSideSet {

    public Margin(CssValue top, CssValue right, CssValue bottom, CssValue left) {
        super(top, right, bottom, left);
    }

    // ─── Fábricas ────────────────────────────────────────────────────────────

    public static Margin all(CssValue value) {
        return new Margin(value, value, value, value);
    }

    public static Margin allPx(float px) {
        return all(CssValue.px(px));
    }

    public static Margin allPercent(float pct) {
        return all(CssValue.percent(pct));
    }

    public static Margin auto() {
        return all(CssValue.auto());
    }

    public static Margin zero() {
        return all(CssValue.zero());
    }

    public static Margin parse(String cssValue) {
        return (Margin) BoxSideSet.parse(cssValue, Margin::new);
    }

    // ─── Métodos de conveniência ────────────────────────────────────────────

    public static Margin of(float top, float right, float bottom, float left, CssUnit unit) {
        return new Margin(
                new CssValue(top, unit),
                new CssValue(right, unit),
                new CssValue(bottom, unit),
                new CssValue(left, unit)
        );
    }
}