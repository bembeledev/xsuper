package com.dic.xsuper.render.javafx.tags;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.layout.LayoutEngine;
import javafx.scene.Node;

public class ContainerTag extends NativeTag {

    public ContainerTag(XplNode node) {
        super(preProcessW3cBlock(node));
    }

    @Override
    protected Node createNode() {
        // ⭐ DELEGAÇÃO PURA! Capturamos o Pane gerado pelo motor de Layout
        javafx.scene.layout.Pane layoutPane = (javafx.scene.layout.Pane) LayoutEngine.resolveLayout(this).createContainer(this);

        // ⭐ A CURA W3C UNIVERSAL PARA CONTENTORES
        // Removemos a resistência do Pane para que o CSS (Flexbox) possa esticá-lo livremente
        //layoutPane.setMinSize(0, 0);
        //layoutPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        return layoutPane;
    }

    // ⭐ O PADRÃO UNIVERSAL W3C (Simples e Extensível)
    private static XplNode preProcessW3cBlock(XplNode node) {
        if (node.attributes == null) return node;

        String currentStyle = (String) node.attributes.getOrDefault("style", "").toString().toLowerCase();

        // Se o CSS já tem qualquer tipo de propriedade "display" definida (seja flex, grid, inline, etc.),
        // o motor deixa passar livremente sem interferir.
        if (currentStyle.contains("display")) {
            return node;
        }

        // Se não tem nenhum display definido, aplicamos o comportamento nativo W3C de bloco (vertical).
        // Isto permite que qualquer novo layout futuro passe sem precisares de mexer aqui outra vez!
        node.attributes.put("style", "display: flex; flex-direction: column; " + currentStyle);

        return node;
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Estilos específicos de contentor
    }
}