package com.dic.xsuper.lang.ui.tags;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.layout.LayoutEngine;
import javafx.scene.Node;

public class ContainerTag extends NativeTag {

    public ContainerTag(XplNode node) {
        super(node);
    }

    @Override
    protected Node createNode() {
        // ⭐ DELEGAÇÃO PURA! Devolve a estrutura que o motor de Layout decidir!
        return LayoutEngine.resolveLayout(this).createContainer(this);
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Estilos específicos de contentor
    }
}