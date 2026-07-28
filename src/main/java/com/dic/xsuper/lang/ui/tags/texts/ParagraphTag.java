package com.dic.xsuper.lang.ui.tags.texts;

import com.dic.xsuper.lang.ui.html.XplNode;

public class ParagraphTag extends TextBaseTag {

    public ParagraphTag(XplNode sourceNode) {
        super(preProcessNode(sourceNode));
    }

    private static XplNode preProcessNode(XplNode node) {
        injectDefaultStyle(node, "display", "block");
        injectDefaultStyle(node, "margin-top", "1em");
        injectDefaultStyle(node, "margin-bottom", "1em");
        return node;
    }
}