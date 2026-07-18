package com.dic.xsuper.lang.ui.tags.controls;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

// Podemos usar um mapa estático para garantir que Rádios com o mesmo "name" partilham o mesmo ToggleGroup!
import java.util.HashMap;
import java.util.Map;

public class RadioInputTag extends FormControlTag {

    private static final Map<String, ToggleGroup> radioGroups = new HashMap<>();
    private javafx.scene.control.Control fxControl;

    public RadioInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        String appearance = (String) sourceNode.attributes.getOrDefault("appearance", "radio");
        boolean isChecked = sourceNode.attributes.containsKey("checked");

        if ("button".equalsIgnoreCase(appearance)) {
            ToggleButton tb = new ToggleButton(this.value); // Usa this.value do FormControlTag
            tb.setSelected(isChecked);
            assignToGroup(tb, this.name); // Usa this.name
            fxControl = tb;
        } else {
            RadioButton rb = new RadioButton();
            rb.setSelected(isChecked);
            assignToGroup(rb, this.name);
            fxControl = rb;
        }

        applyCommonAttributes(fxControl); // <-- Aplica disabled, required, etc.

        return fxControl;
    }

    @Override
    protected void applyTagSpecificStyles() {

    }

    private void assignToGroup(javafx.scene.control.Toggle toggle, String groupName) {
        ToggleGroup group = radioGroups.computeIfAbsent(groupName, k -> new ToggleGroup());
        toggle.setToggleGroup(group);
    }

    @Override
    protected void applyCommonStyles() {
        super.applyCommonStyles();

        // Aqui vais poder intercetar estilos customizados.
        // Exemplo: O utilizador passou uma cor para o acento do rádio?
        Map<String, String> styles = getRawStyles();
        if (styles.containsKey("accent-color") && fxControl instanceof RadioButton) {
            String color = styles.get("accent-color");
            // Sobrescreve a cor do "dot" nativo do JavaFX
            fxControl.setStyle(fxControl.getStyle() + " -fx-mark-color: " + color + ";");
        }
    }

    @Override
    protected void addChildren() {
        // Void element
    }

    @Override
    protected void bindEvents() {
        super.bindEvents();
        if (fxControl instanceof javafx.scene.control.ToggleButton tb) {
            bindTwoWayProperty(tb.selectedProperty(), "checked");
        }
    }
}