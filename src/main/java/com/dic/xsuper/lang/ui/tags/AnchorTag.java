package com.dic.xsuper.lang.ui.tags;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.texts.TextBaseTag; // ⭐ Herda o poder do motor de texto!
import javafx.scene.Node;

/**
 * Tag HTML <a> – Hyperlink purificado.
 * Agora é renderizado como TextFlow, partilhando perfeitamente a linha com <p>, <span> e <strong>,
 * e suportando herança de CSS (font-size, text-shadow, etc).
 */
public class AnchorTag extends TextBaseTag {

    public AnchorTag(XplNode node) {
        super(preProcessNode(node));
    }

    private static XplNode preProcessNode(XplNode node) {
        // ⭐ ESTILO W3C PADRÃO:
        // Injetamos as propriedades de link antes do nó nascer.
        // O motor Top-Down encarrega-se de passar isto para o TextNodeTag filho!
        injectDefaultStyle(node, "color", "#2563eb");
        injectDefaultStyle(node, "text-decoration", "underline");
        injectDefaultStyle(node, "cursor", "hand");
        return node;
    }

    @Override
    protected Node createNode() {
        // 1. Puxa a caixa mágica TextFlow da TextBaseTag
        Node fxNode = super.createNode();

        // 2. Lê o URL de destino
        String href = (String) sourceNode.attributes.getOrDefault("href", "");

        // 3. Aciona a navegação no clique
        fxNode.setOnMouseClicked(e -> handleNavigation(href));

        return fxNode;
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Deixamos vazio! O motor TextBaseTag e o W3C Adapter tratam de tudo.
    }

    /**
     * Motor central de redirecionamento.
     */
    private void handleNavigation(String href) {
        if (href == null || href.trim().isEmpty() || href.equals("#")) return;

        if (href.startsWith("http://") || href.startsWith("https://")) {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(href));
            } catch (Exception e) {
                System.err.println("Erro ao abrir link externo: " + e.getMessage());
            }
        } else if (href.startsWith("#")) {
            // ⭐ A CURA DAS ÂNCORAS: Faz scroll automático até à ID!
            String targetId = href.substring(1);
            com.dic.xsuper.lang.ui.SuperUiEngine engine = com.dic.xsuper.lang.ui.SuperUiEngine.getInstance();

            if (engine != null && engine.getActiveDom() != null) {
                com.dic.xsuper.lang.ui.html.XplNode targetNode = engine.getActiveDom().getElementById(targetId);
                if (targetNode != null && targetNode.nativeNode != null) {
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.Node targetFxNode = (javafx.scene.Node) targetNode.nativeNode;
                        if (targetFxNode.getParent() != null) {
                            targetFxNode.getParent().requestLayout();
                        }
                        targetFxNode.requestFocus(); // Puxa o ecrã instantaneamente para este elemento!
                    });
                }
            }
        }
    }
}