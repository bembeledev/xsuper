package com.dic.xsuper.dom.properties.borderunit;


import com.dic.xsuper.dom.properties.cssunit.BoxSideSet;
import com.dic.xsuper.dom.properties.cssunit.CssUnit;
import com.dic.xsuper.dom.properties.cssunit.CssValue;

public class BorderWidth extends BoxSideSet {
    private BorderWidth(CssValue top, CssValue right, CssValue bottom, CssValue left) {
        super(top, right, bottom, left);
    }

    public static BorderWidth all(CssValue value) {
        return new BorderWidth(value, value, value, value);
    }

    public static BorderWidth allPx(float px) {
        return all(CssValue.px(px));
    }

    public static BorderWidth zero() {
        return all(CssValue.zero());
    }

    public static BorderWidth parse(String cssValue) {
        return (BorderWidth) BoxSideSet.parse(cssValue, BorderWidth::new);
    }

    public static BorderWidth of(float top, float right, float bottom, float left, CssUnit unit) {
        return new BorderWidth(
                new CssValue(top, unit),
                new CssValue(right, unit),
                new CssValue(bottom, unit),
                new CssValue(left, unit)
        );
    }
}