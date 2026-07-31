package com.dic.xsuper.render.javafx.tags.texts;

import com.dic.xsuper.dom.node.XplNode;

public class SpanTag extends TextBaseTag {

    public SpanTag(XplNode sourceNode) {
        super(preProcessNode(sourceNode));
    }

    private static XplNode preProcessNode(XplNode node) {
        injectDefaultStyle(node, "display", "inline");
        return node;
    }
}