package com.dic.xsuper.dom.properties;

import com.dic.xsuper.dom.properties.cssunit.BoxSideSet;
import com.dic.xsuper.dom.properties.cssunit.CssUnit;
import com.dic.xsuper.dom.properties.cssunit.CssValue;

public class Padding extends BoxSideSet {

    public Padding(CssValue top, CssValue right, CssValue bottom, CssValue left) {
        super(top, right, bottom, left);
    }

    // ─── Fábricas ────────────────────────────────────────────────────────────

    public static Padding all(CssValue value) {
        return new Padding(value, value, value, value);
    }

    public static Padding allPx(float px) {
        return all(CssValue.px(px));
    }

    public static Padding allPercent(float pct) {
        return all(CssValue.percent(pct));
    }

    public static Padding zero() {
        return all(CssValue.zero());
    }

    public static Padding parse(String cssValue) {
        return (Padding) BoxSideSet.parse(cssValue, Padding::new);
    }

    // ─── Métodos de conveniência ────────────────────────────────────────────

    public static Padding of(float top, float right, float bottom, float left, CssUnit unit) {
        return new Padding(
                new CssValue(top, unit),
                new CssValue(right, unit),
                new CssValue(bottom, unit),
                new CssValue(left, unit)
        );
    }
}