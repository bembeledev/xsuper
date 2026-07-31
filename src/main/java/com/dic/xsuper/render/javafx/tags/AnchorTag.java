package com.dic.xsuper.render.javafx.tags;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.event.UiEventPublisher;
import com.dic.xsuper.render.javafx.tags.texts.TextBaseTag; // ⭐ Herda o poder do motor de texto!
import com.dic.xsuper.render.javafx.core.SuperUiEngine;
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

        // A tag é cega! Apenas grita para cima que alguém quer navegar.
        if (href.startsWith("#")) {
            // Dispara um evento pedindo para fazer scroll
           UiEventPublisher.publishUiInteracted(sourceNode.id, "scroll-to", href.substring(1));
        } else {
            // Dispara um evento pedindo para abrir um link
            UiEventPublisher.publishUiInteracted(sourceNode.id, "navigate", href);
        }
    }
}