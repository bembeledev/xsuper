package com.dic.xsuper.lang.ui.tags.controls;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.Node;
import javafx.scene.control.Button;

public class ResetInputTag extends ButtonInputTag {

    public ResetInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        fxButton = new Button();
        fxButton.setText(value.isEmpty() ? "Resetar" : value);
        applyCommonAttributes(fxButton);

        //fxButton.setOnAction(e -> dispatchEvent("reset", sourceNode.id, null));

        return fxButton;
    }
}