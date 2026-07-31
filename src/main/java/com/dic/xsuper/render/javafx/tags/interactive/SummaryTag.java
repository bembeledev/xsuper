package com.dic.xsuper.render.javafx.tags.interactive;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Tag HTML &lt;summary&gt; – usada dentro de &lt;details&gt;.
 * Define o cabeçalho visível do details.
 * <p>
 * Esta tag não é renderizada diretamente; é usada pelo DetailsTag
 * para construir o cabeçalho personalizado.
 */
public class SummaryTag extends NativeTag {

    public SummaryTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        HBox container = new HBox(8);
        container.setStyle("-fx-alignment: center-left; -fx-padding: 4px 0;");

        Label label = new Label();
        if (sourceNode.textContent != null && !sourceNode.textContent.trim().isEmpty()) {
            label.setText(sourceNode.textContent.trim());
        } else {
            label.setText("Detalhes");
        }
        // ⭐ Alterado para texto escuro (#0f172a) em vez de branco/cinza claro
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");

        for (NativeTag child : children) {
            container.getChildren().add(child.build());
        }

        container.getChildren().add(0, label);

        applyCommonStyles();
        applyTagSpecificStyles();

        return container;
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Estilos específicos para <summary>
    }

    @Override
    protected void addChildren() {
        // <summary> pode ter filhos (ex: ícones), são adicionados no createNode
    }
}