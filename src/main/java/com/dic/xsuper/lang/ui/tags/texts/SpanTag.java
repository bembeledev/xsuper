package com.dic.xsuper.lang.ui.tags.texts;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.text.TextFlow;

public class SpanTag extends NativeTag {

    public SpanTag(XplNode sourceNode) {
        super(preProcessNode(sourceNode));
    }

    private static XplNode preProcessNode(XplNode node) {
        injectDefaultStyle(node, "display", "inline");
        return node;
    }

    private static void injectDefaultStyle(XplNode node, String property, String value) {
        if (node.attributes == null) return;
        String currentStyle = (String) node.attributes.getOrDefault("style", "");
        if (!currentStyle.toLowerCase().contains(property + ":")) {
            node.attributes.put("style", property + ": " + value + "; " + currentStyle);
        }
    }

    @Override
    protected Node createNode() {
        TextFlow textFlow = new TextFlow();
        // A natureza inline força-o a abraçar rigorosamente o seu conteúdo interno
        textFlow.setMinHeight(Region.USE_PREF_SIZE);
        textFlow.setMaxWidth(Double.MAX_VALUE);
        textFlow.setMinWidth(0);
        return textFlow;
    }

    @Override
    protected void applyTagSpecificStyles() {}
}