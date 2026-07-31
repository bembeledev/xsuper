package com.dic.xsuper.render.javafx.tags.controls;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.core.SuperUiEngine;
import javafx.scene.Node;
import javafx.scene.control.Control;

public class TextInputTag extends FormControlTag {

    protected Control fxControl;

    public TextInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        String type = ((String) sourceNode.attributes.getOrDefault("type", "text")).toLowerCase().trim();

        if ("password".equals(type)) {
            javafx.scene.control.PasswordField pf = new javafx.scene.control.PasswordField();
            pf.setPromptText(placeholder);
            pf.setText(value);
            fxControl = pf;

            // Força o estado inicial no DOM
            sourceNode.attributes.putIfAbsent("value", this.value);

        } else if ("number".equals(type)) {
            int minVal = this.min.isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(this.min);
            int maxVal = this.max.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(this.max);
            int stepVal = this.step.isEmpty() ? 1 : Integer.parseInt(this.step);
            int initialVal = this.value.isEmpty() ? 0 : Integer.parseInt(this.value);

            javafx.scene.control.Spinner<Integer> spinner = new javafx.scene.control.Spinner<>(minVal, maxVal, initialVal, stepVal);
            spinner.setEditable(true);
            fxControl = spinner;

            // ⭐ MAGIA DO ESTADO INICIAL ⭐
            // Se o HTML não tinha 'value', o Spinner assumiu o 'initialVal' (0).
            // Vamos injetar esse '0' na memória imediatamente!
            sourceNode.attributes.put("value", initialVal);
            if (sourceNode.liveElement != null) {
                sourceNode.liveElement.setAttributeSilently("value", initialVal);
            }

        } else {
            javafx.scene.control.TextField tf = new javafx.scene.control.TextField();
            tf.setPromptText(placeholder);
            tf.setText(value);
            fxControl = tf;

            // Força o estado inicial no DOM
            sourceNode.attributes.putIfAbsent("value", this.value);
        }

        applyCommonAttributes(fxControl);

        if (fxControl instanceof javafx.scene.control.TextField tf && readonly) {
            tf.setEditable(false);
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

    @Override
    protected void bindEvents() {
        super.bindEvents();

        if (fxControl instanceof javafx.scene.control.TextInputControl input) {
            bindTwoWayProperty(input.textProperty(), "value");
        }
        else if (fxControl instanceof javafx.scene.control.Spinner<?> spinner) {
            // Liga as setinhas de subir e descer do Spinner
            bindTwoWayProperty(spinner.getValueFactory().valueProperty(), "value");

            // ⭐ LIGA O TECLADO (Tempo real enquanto escreve os números) ⭐
            spinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                Object currentValue = sourceNode.attributes.get("value");
                String curStr = currentValue != null ? currentValue.toString() : "";

                // Trata como String para não quebrar coisas como "10." (que o Java não sabe transformar em Double ainda)
                if (!newVal.equals(curStr)) {
                    sourceNode.attributes.put("value", newVal);
                    if (sourceNode.liveElement != null) {
                        sourceNode.liveElement.setAttributeSilently("value", newVal);
                    }
                    if (sourceNode.id != null && !sourceNode.id.isEmpty()) {
                        SuperUiEngine.getInstance().dispatchEvent("input", newVal, sourceNode.id);
                    }
                }
            });
        }
    }

}