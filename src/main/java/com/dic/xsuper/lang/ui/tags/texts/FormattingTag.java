package com.dic.xsuper.lang.ui.tags.texts;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.text.TextFlow;

public class FormattingTag extends NativeTag {

    public FormattingTag(XplNode sourceNode) {
        super(preProcessNode(sourceNode));
    }

    private static XplNode preProcessNode(XplNode node) {
        String tag = node.tag.toLowerCase();
        if (tag.equals("b") || tag.equals("strong")) {
            injectDefaultStyle(node, "font-weight", "bold");
        } else if (tag.equals("i") || tag.equals("em")) {
            injectDefaultStyle(node, "font-style", "italic");
        } else if (tag.equals("u")) {
            injectDefaultStyle(node, "text-decoration", "underline");
        } else if (tag.equals("s") || tag.equals("strike")) {
            injectDefaultStyle(node, "text-decoration", "line-through");
        }
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
        textFlow.setMinHeight(Region.USE_PREF_SIZE);
        textFlow.setMinWidth(0);
        return textFlow;
    }

    @Override
    protected void applyTagSpecificStyles() {}
}