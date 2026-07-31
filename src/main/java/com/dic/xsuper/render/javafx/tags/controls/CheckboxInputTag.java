package com.dic.xsuper.render.javafx.tags.controls;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;

import java.util.Map;

public class CheckboxInputTag extends FormControlTag {

    private CheckBox fxCheckbox;

    public CheckboxInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        fxCheckbox = new CheckBox();

        // 1. ESTADO INICIAL (Checked)
        // O HTML diz que se a palavra "checked" existir, a caixa fica marcada
        if (sourceNode.attributes.containsKey("checked") &&
                !"false".equalsIgnoreCase((String) sourceNode.attributes.get("checked"))) {
            fxCheckbox.setSelected(true);
        }

        // 2. ATENÇÃO À NORMA DA WEB!
        // Removemos o fxCheckbox.setText(value). O "value" na web é o que vai para o servidor.
        // Guardamos o valor oculto e o estado nas Propriedades do JavaFX para a futura tag <form> ler.
        String submitValue = this.value.isEmpty() ? "on" : this.value; // "on" é o padrão web
        fxCheckbox.getProperties().put("xpl_input_value", submitValue);
        fxCheckbox.getProperties().put("xpl_is_checked", fxCheckbox.isSelected());

        // 3. APLICA OS ATRIBUTOS COMUNS (Disabled, Required, Name, etc.)
        applyCommonAttributes(fxCheckbox);

        // 4. BÓNUS DA TUA LINGUAGEM (Shortcut XPL)
        // Se o desenvolvedor não quiser criar um <label> separado, pode fazer: <input type="checkbox" label="Aceito os termos">
        if (sourceNode.attributes.containsKey("label")) {
            fxCheckbox.setText((String) sourceNode.attributes.get("label"));
        }

        return fxCheckbox;
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Exemplo: Permitir mudar a cor do "visto" na checkbox usando CSS
        Map<String, String> styles = getRawStyles();
        if (styles.containsKey("accent-color")) {
            String color = styles.get("accent-color");
            fxCheckbox.setStyle(fxCheckbox.getStyle() + " -fx-mark-color: " + color + ";");
        }
    }

    @Override
    protected void addChildren() {
        // <input> é void element – não fazemos nada!
    }

    @Override
    protected void bindEvents() {
        super.bindEvents();
        if (fxCheckbox != null) {
            // Liga o 'visto' da caixa ao atributo 'checked' do DOM
            bindTwoWayProperty(fxCheckbox.selectedProperty(), "checked");
        }
    }
}