package com.dic.xsuper.lang.ui.properties;

import com.dic.xsuper.lang.ui.properties.borderunit.BorderRadius;
import com.dic.xsuper.lang.ui.properties.cssunit.CssValue;
import com.dic.xsuper.lang.ui.properties.gradient.Gradient;
import com.dic.xsuper.lang.ui.properties.size.BoxSize;

import java.util.ArrayList;
import java.util.List;

public class ResolvedStyles {
    public BoxSize boxSize = BoxSize.defaults();
    public Padding padding = Padding.zero();
    public Margin margin = Margin.zero();
    public Border border = Border.none();
    public BorderRadius borderRadius = BorderRadius.zero();
    public List<BoxShadow> boxShadows = new ArrayList<>();
    public List<Gradient> gradients = new ArrayList<>();
    public int backgroundColor = 0x00000000;   // transparente
    public int textColor = 0xFF000000;          // preto
    public String fontFamily = null;
    public CssValue fontSize = CssValue.px(16);
    public String fontWeight = "normal";
    public String display = "block";
}