package com.dic.xsuper.lang.ui.layout.panes;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;
import java.util.List;

public interface CustomLayoutPane {
    void populateChildren(List<NativeTag> children, CssContext context);
}