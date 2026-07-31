package com.dic.xsuper.render.javafx.tags;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.layout.LayoutEngine;
import javafx.scene.Node;

public class FigcaptionTag extends NativeTag {

    public FigcaptionTag(XplNode node) {
        super(preProcessNode(node));
    }

    private static XplNode preProcessNode(XplNode node) {
        // Padrão da Web para legendas de imagem
        injectDefaultStyleOnNode(node, "display", "block");
        injectDefaultStyleOnNode(node, "text-align", "center");
        injectDefaultStyleOnNode(node, "font-size", "13px");
        injectDefaultStyleOnNode(node, "color", "#6b7280"); // Cinzento neutro
        injectDefaultStyleOnNode(node, "margin-top", "8px");
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