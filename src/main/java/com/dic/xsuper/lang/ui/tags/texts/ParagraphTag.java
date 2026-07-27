package com.dic.xsuper.lang.ui.tags.texts;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.text.TextFlow;

public class ParagraphTag extends NativeTag {

    public ParagraphTag(XplNode sourceNode) {
        super(preProcessNode(sourceNode));
    }

    private static XplNode preProcessNode(XplNode node) {
        injectDefaultStyle(node, "display", "block");
        injectDefaultStyle(node, "margin-top", "1em");
        injectDefaultStyle(node, "margin-bottom", "1em");
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
        textFlow.setMaxWidth(Double.MAX_VALUE);
        textFlow.setMinWidth(0);
        textFlow.setPrefWidth(Region.USE_COMPUTED_SIZE); // ⭐ Corrigido para largura dinâmica
        textFlow.setMinHeight(Region.USE_PREF_SIZE);

        String align = getRawStyles().getOrDefault("text-align", "left").toLowerCase();
        switch (align) {
            case "center" -> textFlow.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            case "right" -> textFlow.setTextAlignment(javafx.scene.text.TextAlignment.RIGHT);
            case "justify" -> textFlow.setTextAlignment(javafx.scene.text.TextAlignment.JUSTIFY);
        }
        return textFlow;
    }

    @Override
    protected void applyTagSpecificStyles() {}
}