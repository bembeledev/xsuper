package com.dic.xsuper.render.javafx.tags.texts;

import com.dic.xsuper.dom.node.XplNode;

public class FormattingTag extends TextBaseTag {

    public FormattingTag(XplNode sourceNode) {
        super(preProcessNode(sourceNode));
    }

    private static XplNode preProcessNode(XplNode node) {
        String tag = node.tag.toLowerCase();

        switch (tag) {
            case "b", "strong" -> injectDefaultStyle(node, "font-weight", "bold");
            case "i", "em" -> injectDefaultStyle(node, "font-style", "italic");
            case "u" -> injectDefaultStyle(node, "text-decoration", "underline");
            case "s", "strike", "del" -> injectDefaultStyle(node, "text-decoration", "line-through");
            case "small" -> injectDefaultStyle(node, "font-size", "0.8em");
            case "mark" -> {
                injectDefaultStyle(node, "background-color", "yellow");
                injectDefaultStyle(node, "color", "black");
            }
        }

        return node;
    }
}