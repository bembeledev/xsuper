package com.dic.xsuper.lang.ui.tags;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class DivTag extends NativeTag {
    public DivTag(XplNode node) {
        super(node);
    }

    @Override
    protected Node createNode() {
        return new Pane();
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Nada específico para div, tudo já foi aplicado na base
    }
}