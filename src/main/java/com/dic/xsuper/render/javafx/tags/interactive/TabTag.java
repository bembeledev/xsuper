package com.dic.xsuper.render.javafx.tags.interactive;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.layout.LayoutEngine;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.layout.Region;

public class TabTag extends NativeTag {

    public TabTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        Node container = LayoutEngine.resolveLayout(this).createContainer(this);

        // ⭐ BLINDAGEM MÁXIMA: Remove a resistência para o StackPane conseguir esticar a Aba
        if (container instanceof Region region) {
            region.setMinSize(0, 0);
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }

        return container;
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Estilos específicos da aba, geridos pelo NativeTag base
    }

    public String getTitle() {
        return (String) sourceNode.attributes.getOrDefault("title", "Sem Título");
    }

    public boolean isActiveByDefault() {
        return sourceNode.attributes.containsKey("active") &&

                !"false".equalsIgnoreCase(sourceNode.attributes.get("active").toString());
    }

    @Override
    public boolean isGreedyByDefault() { return true; }
}