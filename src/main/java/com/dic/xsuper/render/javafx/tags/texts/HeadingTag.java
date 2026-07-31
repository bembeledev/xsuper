package com.dic.xsuper.render.javafx.tags.texts;

import com.dic.xsuper.dom.node.XplNode;

public class HeadingTag extends TextBaseTag {

    public HeadingTag(XplNode sourceNode) {
        super(preProcessNode(sourceNode));
    }

    private static XplNode preProcessNode(XplNode node) {
        injectDefaultStyle(node, "display", "block");
        injectDefaultStyle(node, "font-weight", "bold");

        switch (node.tag.toLowerCase()) {
            case "h1" -> { injectDefaultStyle(node, "font-size", "32px"); injectDefaultStyle(node, "margin", "21px 0"); }
            case "h2" -> { injectDefaultStyle(node, "font-size", "24px"); injectDefaultStyle(node, "margin", "19px 0"); }
            case "h3" -> { injectDefaultStyle(node, "font-size", "18.72px"); injectDefaultStyle(node, "margin", "18px 0"); }
            case "h4" -> { injectDefaultStyle(node, "font-size", "16px"); injectDefaultStyle(node, "margin", "21px 0"); }
            case "h5" -> { injectDefaultStyle(node, "font-size", "13.28px"); injectDefaultStyle(node, "margin", "22px 0"); }
            case "h6" -> { injectDefaultStyle(node, "font-size", "10.72px"); injectDefaultStyle(node, "margin", "24px 0"); }
        }
        return node;
    }
}