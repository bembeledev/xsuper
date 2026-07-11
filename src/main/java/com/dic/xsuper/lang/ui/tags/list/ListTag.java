package com.dic.xsuper.lang.ui.tags.list;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.layout.LayoutEngine;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * Classe base para &lt;ul&gt; e &lt;ol&gt;.
 * Processa os filhos &lt;li&gt; e adiciona marcadores ou numeração.
 */
public abstract class ListTag extends NativeTag {

    protected String listStyleType = "disc"; // disc, circle, square, decimal, lower-alpha, etc.

    public ListTag(XplNode node) {
        super(node);
        // Lê o estilo de lista do atributo style (list-style-type)
        parseListStyle();
    }

    private void parseListStyle() {
        if (style.containsKey("list-style-type")) {
            listStyleType = style.get("list-style-type").toLowerCase();
        }
    }

    @Override
    protected Node createNode() {
        VBox vbox = new VBox(4);
        vbox.setPadding(new Insets(8, 0, 8, 16));
        return vbox;
    }

    @Override
    protected void addChildren() {
        if (!(fxNode instanceof VBox vbox)) return;

        int index = 1;
        for (NativeTag child : children) {
            if (child instanceof LiTag li) {
                String marker = getMarker(index);

                // ⭐ Construímos o <li> mas garantimos que não dobra estilos
                Node childNode = li.build();

                javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(8);
                hbox.setAlignment(javafx.geometry.Pos.TOP_LEFT);

                Label markerLabel = new Label(marker);
                markerLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 25px; -fx-alignment: top-right;");

                hbox.getChildren().addAll(markerLabel, childNode);
                vbox.getChildren().add(hbox);

                index++;
            } else {
                vbox.getChildren().add(child.build());
            }
        }
    }

    /**
     * Obtém o marcador para um item com base no tipo de lista e no índice.
     * As subclasses (UlTag, OlTag) devem sobrescrever este método.
     */
    protected abstract String getMarker(int index);

    @Override
    protected void applyTagSpecificStyles() {
        // Estilos adicionais para a lista (ex: list-style-position, padding-left)
    }
}