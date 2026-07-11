package com.dic.xsuper.lang.ui.layout.panes;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.List;

public class ContentsContainerPane extends VBox implements CustomLayoutPane {

    public ContentsContainerPane() {
        // ⭐ MAGIA FANTASMA: Torna o painel impercetível
        setPickOnBounds(false); // Ignora cliques e passa-os diretamente aos filhos
        setMaxWidth(Region.USE_PREF_SIZE);
        setMaxHeight(Region.USE_PREF_SIZE);

        // Força a remoção de qualquer vestígio de styling visual que o CSS possa tentar injetar
        setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-border-width: 0;");
    }

    @Override
    public void populateChildren(List<NativeTag> children, CssContext context) {
        // Ele simplesmente cospe os filhos sem calcular margens complexas,
        // agindo como uma mera ponte de ligação!
        for (NativeTag child : children) {
            getChildren().add(child.build());
        }
    }
}