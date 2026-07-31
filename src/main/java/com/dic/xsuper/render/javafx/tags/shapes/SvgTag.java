package com.dic.xsuper.render.javafx.tags.shapes;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class SvgTag extends NativeTag {

    private Pane svgContainer;

    public SvgTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        // O Pane atua como uma tela absoluta (tal como o SVG W3C)
        svgContainer = new Pane();

        // 1. Dimensões estritas da tela SVG
        if (sourceNode.attributes.containsKey("width")) {
            try {
                svgContainer.setPrefWidth(Double.parseDouble(sourceNode.attributes.get("width").toString().replace("px","")));
            } catch (Exception ignored) {}
        }
        if (sourceNode.attributes.containsKey("height")) {
            try {
                svgContainer.setPrefHeight(Double.parseDouble(sourceNode.attributes.get("height").toString().replace("px","")));
            } catch (Exception ignored) {}
        }

        applyCommonStyles();
        return svgContainer;
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Estilos extra para o container
    }

    // ⭐ Apagámos o addChildren()!
    // Agora a classe base NativeTag encarrega-se de adicionar os Shapes corretamente inicializados.
}