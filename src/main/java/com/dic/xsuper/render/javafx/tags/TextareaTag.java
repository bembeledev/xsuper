package com.dic.xsuper.render.javafx.tags;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.controls.FormControlTag;
import javafx.scene.Node;
import javafx.scene.control.TextArea;

/**
 * Tag HTML <textarea> atualizada para herdar de FormControlTag.
 * Ganha nativamente suporte para name, required, disabled e ligação ao futuro <form>.
 */
public class TextareaTag extends FormControlTag { // ⭐ Agora herda o poder dos formulários!

    private int rows = 3;
    private int cols = 20;

    public TextareaTag(XplNode node) {
        super(node); // Lê instantaneamente placeholder, name, value, disabled, readonly, etc.

        // Processa atributos exclusivos da tag textarea
        if (node.attributes != null) {
            if (node.attributes.containsKey("rows")) {
                try { rows = Integer.parseInt(node.attributes.get("rows").toString()); } catch (NumberFormatException ignored) {}
            }
            if (node.attributes.containsKey("cols")) {
                try { cols = Integer.parseInt(node.attributes.get("cols").toString()); } catch (NumberFormatException ignored) {}
            }
        }

        // Regra de Ouro do HTML: O conteúdo interno (<textarea>Texto</textarea>)
        // tem precedência sobre o atributo value="..."
        if (node.textContent != null && !node.textContent.trim().isEmpty()) {
            this.value = node.textContent;
        }
    }

    @Override
    protected Node createNode() {
        TextArea textArea = new TextArea();

        // Configuração estrutural do JavaFX
        textArea.setPrefRowCount(rows);
        textArea.setPrefColumnCount(cols); // ⭐ O JavaFX afinal tem isto!

        textArea.setText(this.value);
        textArea.setPromptText(this.placeholder);

        // Aplica readonly (disabled já é aplicado pelo FormControlTag)
        if (this.readonly) {
            textArea.setEditable(false);
        }

        // Aplica estilos comuns e atributos de <form> (ex: name, required)
        applyCommonAttributes(textArea);
        applyCommonStyles();

        return textArea;
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Podes usar isto para forçar fontes monoespaçadas por padrão, se quiseres:
        // fxNode.setStyle(fxNode.getStyle() + " -fx-font-family: 'Consolas', monospace;");
    }

    @Override
    protected void addChildren() {
        // Um textarea não renderiza filhos DOM (elementos gráficos), apenas lê o textContent no construtor.
    }

    @Override
    protected void bindEvents() {
        super.bindEvents();
        if (fxNode instanceof javafx.scene.control.TextArea ta) {
            bindTwoWayProperty(ta.textProperty(), "value");
        }
    }
}