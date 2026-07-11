package com.dic.xsuper.lang.ui.tags.controls;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;

public class TextInputTag extends FormControlTag {

    protected Control fxControl;

    public TextInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        String type = ((String) sourceNode.attributes.getOrDefault("type", "text")).toLowerCase().trim();

        if ("password".equals(type)) {
            PasswordField pf = new PasswordField();
            pf.setPromptText(placeholder);
            pf.setText(value);
            fxControl = pf;
        } else if ("number".equals(type)) {
            // Usa os valores da tua superclasse, ou valores padrão se estiverem vazios
            int minVal = this.min.isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(this.min);
            int maxVal = this.max.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(this.max);
            int stepVal = this.step.isEmpty() ? 1 : Integer.parseInt(this.step);
            int initialVal = this.value.isEmpty() ? 0 : Integer.parseInt(this.value);

            Spinner<Integer> spinner = new Spinner<>(minVal, maxVal, initialVal, stepVal);
            spinner.setEditable(true);
            fxControl = spinner;
        } else {
            TextField tf = new TextField();
            tf.setPromptText(placeholder);
            tf.setText(value);
            fxControl = tf;
        }

        applyCommonAttributes(fxControl);

        // Aplica readonly para TextField/PasswordField
        if (fxControl instanceof TextField tf && readonly) {
            tf.setEditable(false);
        }
        if (fxControl instanceof PasswordField pf && readonly) {
            pf.setEditable(false);
        }

        return fxControl;
    }

    @Override
    protected void applyTagSpecificStyles() {

    }

    @Override
    protected void addChildren() {
        // void element
    }
}