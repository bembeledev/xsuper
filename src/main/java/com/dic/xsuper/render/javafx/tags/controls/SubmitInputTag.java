package com.dic.xsuper.render.javafx.tags.controls;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.control.Button;

public class SubmitInputTag extends ButtonInputTag {

    public SubmitInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        fxButton = new Button();
        fxButton.setText(value.isEmpty() ? "Submeter" : value);
        applyCommonAttributes(fxButton);

        // Evento de submissão (podes ligar a um formulário)
        //fxButton.setOnAction(e -> dispatchEvent("submit", sourceNode.id, null));

        return fxButton;
    }
}