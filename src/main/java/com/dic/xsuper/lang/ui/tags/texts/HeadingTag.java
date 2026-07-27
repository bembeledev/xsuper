package com.dic.xsuper.lang.ui.tags.texts;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.text.TextFlow;

public class HeadingTag extends NativeTag {

    public HeadingTag(XplNode sourceNode) {
        super(preProcessNode(sourceNode));
    }

    private static XplNode preProcessNode(XplNode node) {
        // Injeta os instintos W3C antes do CSS do utilizador ser lido
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