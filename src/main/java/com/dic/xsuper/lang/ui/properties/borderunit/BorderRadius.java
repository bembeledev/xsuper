package com.dic.xsuper.lang.ui.properties.borderunit;


import com.dic.xsuper.lang.ui.properties.cssunit.BoxSideSet;
import com.dic.xsuper.lang.ui.properties.cssunit.CssUnit;
import com.dic.xsuper.lang.ui.properties.cssunit.CssValue;

public class BorderRadius extends BoxSideSet {
    private BorderRadius(CssValue top, CssValue right, CssValue bottom, CssValue left) {
        super(top, right, bottom, left);
    }

    public static BorderRadius all(CssValue value) {
        return new BorderRadius(value, value, value, value);
    }

    public static BorderRadius allPx(float px) {
        return all(CssValue.px(px));
    }

    public static BorderRadius zero() {
        return all(CssValue.zero());
    }

    public static BorderRadius parse(String cssValue) {
        return (BorderRadius) BoxSideSet.parse(cssValue, BorderRadius::new);
    }

    public static BorderRadius of(float top, float right, float bottom, float left, CssUnit unit) {
        return new BorderRadius(
                new CssValue(top, unit),
                new CssValue(right, unit),
                new CssValue(bottom, unit),
                new CssValue(left, unit)
        );
    }
}