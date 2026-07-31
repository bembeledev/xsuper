package com.dic.xsuper.render.javafx.tags;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.layout.LayoutEngine;
import javafx.scene.Node;

public class FigureTag extends NativeTag {

    public FigureTag(XplNode node) {
        super(preProcessNode(node));
    }

    private static XplNode preProcessNode(XplNode node) {
        // Padrão da Web: Display block com margem de 40px nas laterais
        injectDefaultStyleOnNode(node, "display", "flex");
        injectDefaultStyleOnNode(node, "flex-direction", "column");
        injectDefaultStyleOnNode(node, "margin", "16px 40px");
        return node;
    }

    private static void injectDefaultStyleOnNode(XplNode node, String property, String value) {
        if (node.attributes == null) return;
        String currentStyle = (String) node.attributes.getOrDefault("style", "");
        if (!currentStyle.toLowerCase().contains(property + ":")) {
            node.attributes.put("style", property + ": " + value + "; " + currentStyle);
        }
    }

    @Override
    protected Node createNode() {
        return LayoutEngine.resolveLayout(this).createContainer(this);
    }

    @Override
    protected void applyTagSpecificStyles() {}
}