package com.dic.xsuper.render.javafx.layout;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.layout.Pane;

public interface LayoutManager {
    // O motor de layout lê as propriedades da Tag e devolve o Pane ideal
    Pane createContainer(NativeTag tag);
}