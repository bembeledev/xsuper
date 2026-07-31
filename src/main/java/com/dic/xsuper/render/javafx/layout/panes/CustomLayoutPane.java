package com.dic.xsuper.render.javafx.layout.panes;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.dom.properties.cssunit.CssContext;
import java.util.List;

public interface CustomLayoutPane {
    void populateChildren(List<NativeTag> children, CssContext context);
}