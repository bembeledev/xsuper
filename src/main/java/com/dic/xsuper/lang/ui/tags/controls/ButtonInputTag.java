package com.dic.xsuper.lang.ui.tags.controls;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.Node;
import javafx.scene.control.Button;

public class ButtonInputTag extends FormControlTag {

    protected Button fxButton;

    public ButtonInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        fxButton = new Button();
        if (!value.isEmpty()) fxButton.setText(value);
        else if (!placeholder.isEmpty()) fxButton.setText(placeholder);
        else fxButton.setText("Botão");

        applyCommonAttributes(fxButton);


        return fxButton;
    }

    @Override
    protected void applyTagSpecificStyles() {

    }

    protected void applyButtonStyles() {
        // Estilos podem ser aplicados aqui
    }

    @Override
    protected void addChildren() {
        // void
    }
}