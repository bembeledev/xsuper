package com.dic.xsuper.render.javafx.layout;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.layout.Pane;

public class NoneLayout implements LayoutManager {
    @Override
    public Pane createContainer(NativeTag tag) {
        Pane ghostPane = new Pane();
        // A Magia: Torna-o invisível e remove-o do cálculo de física/espaço do ecrã!
        ghostPane.setVisible(false);
        ghostPane.setManaged(false);
        return ghostPane;
    }
}