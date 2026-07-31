package com.dic.xsuper.render.javafx.tags;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class ImageTag extends NativeTag {

    private StackPane container;
    private ImageView imageView;

    public ImageTag(XplNode node) {
        super(node);
    }

    @Override
    protected Node createNode() {
        // Usamos um StackPane como root para podermos centrar o texto 'alt' se a imagem falhar
        container = new StackPane();
        imageView = new ImageView();

        String src = (String) sourceNode.attributes.getOrDefault("src", "");
        String alt = (String) sourceNode.attributes.getOrDefault("alt", "Imagem quebrada");

        if (!src.isEmpty()) {
            try {
                // true = carregar a imagem em background (assíncrono) para não congelar a UI!
                Image image = new Image(src, true);

                image.errorProperty().addListener((obs, old, isError) -> {
                    if (isError) showAltText(alt);
                });

                imageView.setImage(image);
                imageView.setSmooth(true); // Antialiasing de alta qualidade

                // Tratar o "object-fit" da Web
                String objectFit = style.getOrDefault("object-fit", "fill").toLowerCase().trim();
                if (objectFit.equals("contain") || objectFit.equals("cover")) {
                    imageView.setPreserveRatio(true);
                } else {
                    imageView.setPreserveRatio(false); // Estica a imagem (comportamento padrão de preenchimento)
                }

                // O ImageView adapta-se ao tamanho imposto ao StackPane pelo teu LayoutEngine (CSS)
                imageView.fitWidthProperty().bind(container.widthProperty());
                imageView.fitHeightProperty().bind(container.heightProperty());

                container.getChildren().add(imageView);

            } catch (Exception e) {
                showAltText(alt);
            }
        } else {
            showAltText(alt);
        }

        applyCommonStyles();
        return container;
    }

    private void showAltText(String alt) {
        container.getChildren().clear();
        Label altLabel = new Label("🖼️ " + alt);
        altLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-padding: 10px; -fx-text-alignment: center; -fx-wrap-text: true;");
        container.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #cbd5e1; -fx-border-style: dashed; -fx-border-width: 2px; -fx-border-radius: 4px;");
        container.getChildren().add(altLabel);
    }

    @Override
    protected void applyTagSpecificStyles() {
    }

    @Override
    protected void addChildren() {
        // A tag <img> é um elemento vazio (void element), não tem filhos.
    }
}