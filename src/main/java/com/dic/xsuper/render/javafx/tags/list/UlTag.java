package com.dic.xsuper.render.javafx.tags.list;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.Map;

/**
 * Tag HTML <ul> – lista não ordenada com suporte a ícones personalizados e LayoutEngine.
 */
public class UlTag extends NativeTag {

    private String iconType = "text";   // text, emoji, image, svg, custom
    private String iconText = "•";      // marcador padrão
    private String iconUrl = null;
    private String iconSvg = null;
    private String listStyleType = "disc";

    public UlTag(XplNode sourceNode) {
        super(sourceNode);
        parseIconAttributes();
    }

    private void parseIconAttributes() {
        Map<String, String> styles = getRawStyles();
        Map<String, Object> attrs = sourceNode.attributes;

        if (attrs.containsKey("icon-type")) {
            iconType = attrs.get("icon-type").toString().toLowerCase();
        }
        if (attrs.containsKey("icon")) {
            iconText = attrs.get("icon").toString();
        }
        if (attrs.containsKey("icon-url")) {
            iconUrl = attrs.get("icon-url").toString();
        }
        if (attrs.containsKey("icon-svg")) {
            iconSvg = attrs.get("icon-svg").toString();
        }

        if (styles.containsKey("list-style-type")) {
            listStyleType = styles.get("list-style-type").toLowerCase().trim();
            if (listStyleType.equals("circle")) {
                iconText = "○";
            } else if (listStyleType.equals("square")) {
                iconText = "■";
            } else if (listStyleType.equals("none")) {
                iconText = "";
            } else if (listStyleType.equals("custom")) {
                iconType = "custom";
            }
        }
    }

    @Override
    protected Node createNode() {
        VBox vbox = new VBox(6);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    @Override
    protected void applyTagSpecificStyles() {}

    @Override
    protected void addChildren() {
        if (!(fxNode instanceof VBox vbox)) return;

        for (NativeTag child : children) {
            if (child instanceof LiTag li) {
                // 1. O LI processa o seu próprio layout (backgrounds, paddings, etc.)
                Node liContent = li.build();

                // 2. Envolvente para alinhar Marcador + Conteúdo
                HBox wrapper = new HBox(8);
                wrapper.setAlignment(Pos.TOP_LEFT);

                // 3. Verifica se deve renderizar o marcador
                if (!"none".equals(listStyleType) || (iconType.equals("text") && !iconText.isEmpty())) {
                    Node markerNode = createMarkerNode(li);

                    // Força o marcador numa caixa de largura fixa para alinhar perfeitamente com todas as linhas
                    HBox markerContainer = new HBox(markerNode);
                    markerContainer.setPrefWidth(20);
                    markerContainer.setAlignment(Pos.TOP_RIGHT);
                    markerContainer.setPadding(new Insets(2, 0, 0, 0)); // Pequeno recuo topo para alinhar com texto

                    wrapper.getChildren().add(markerContainer);
                }

                // 4. O conteúdo do LI ocupa o restante espaço
                HBox.setHgrow(liContent, Priority.ALWAYS);
                wrapper.getChildren().add(liContent);

                vbox.getChildren().add(wrapper);
            } else {
                vbox.getChildren().add(child.build());
            }
        }
    }

    /**
     * Motor inteligente que decide se o marcador é SVG, Imagem, Emoji ou Texto.
     */
    private Node createMarkerNode(LiTag liTag) {
        Map<String, Object> liAttrs = liTag.getSourceNode().attributes;

        // 1. Prioridade Máxima: Imagem (no LI ou global)
        String targetImgUrl = liAttrs.containsKey("data-icon-url") ? liAttrs.get("data-icon-url").toString() : iconUrl;
        if (targetImgUrl != null) {
            try {
                ImageView iv = new ImageView(new Image(targetImgUrl));
                iv.setFitWidth(16);
                iv.setFitHeight(16);
                return iv;
            } catch (Exception ignored) {}
        }

        // 2. Prioridade Secundária: SVG (no LI ou global)
        String targetSvg = liAttrs.containsKey("data-icon-svg") ? liAttrs.get("data-icon-svg").toString() : iconSvg;
        if (targetSvg != null) {
            try {
                SVGPath svg = new SVGPath();
                svg.setContent(targetSvg);

                // Herda cor se possível
                String color = resolvedStyles.textColor != 0xFF000000 ? toJavaFxCssColor(resolvedStyles.textColor) : "#333333";
                svg.setStyle("-fx-fill: " + color + "; -fx-stroke: none;");
                svg.setScaleX(0.8);
                svg.setScaleY(0.8);
                return svg;
            } catch (Exception ignored) {}
        }

        // 3. Fallback: Texto, Custom Icon (data-icon) ou Emoji
        String bullet = liAttrs.containsKey("data-icon") ? liAttrs.get("data-icon").toString() : iconText;
        if (bullet.isEmpty()) bullet = "•"; // Prevenção extra

        Label bulletLabel = new Label(bullet);
        String color = resolvedStyles.textColor != 0xFF000000 ? toJavaFxCssColor(resolvedStyles.textColor) : "#000000";

        // Verifica se é um emoji pelo code point
        if (bullet.length() == 1 && bullet.codePointAt(0) > 0x1F000) {
            bulletLabel.setStyle("-fx-font-size: 18px;"); // Emojis ficam um pouco maiores
        } else {
            bulletLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + color + ";");
        }

        return bulletLabel;
    }
}