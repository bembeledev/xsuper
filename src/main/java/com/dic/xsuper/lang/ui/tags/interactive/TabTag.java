package com.dic.xsuper.lang.ui.tags.interactive;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.layout.LayoutEngine;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;

public class TabTag extends NativeTag {

    public TabTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        // Delega para o motor de layout exatamente como a tag <div> e <main> fazem!
        // Isto permite-te usar 'display: grid' ou 'display: flex' diretamente na <tab>
        return LayoutEngine.resolveLayout(this).createContainer(this);
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
}