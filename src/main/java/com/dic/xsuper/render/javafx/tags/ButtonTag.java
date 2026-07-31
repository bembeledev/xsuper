package com.dic.xsuper.render.javafx.tags;


import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.control.Button;

public class ButtonTag extends NativeTag {
    public ButtonTag(XplNode node) {
        super(node);
    }

    @Override
    protected Node createNode() {
        Button btn = new Button();
        // Se houver texto, define
        if (sourceNode != null && sourceNode.textContent != null && !sourceNode.textContent.isEmpty()) {
            btn.setText(sourceNode.textContent);
        }

        return btn;
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Estilos adicionais específicos de botão podem ser aplicados aqui
    }
}