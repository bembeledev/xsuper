package com.dic.xsuper.lang.ui.layout;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.layout.Pane;

public interface LayoutManager {
    // O motor de layout lê as propriedades da Tag e devolve o Pane ideal
    Pane createContainer(NativeTag tag);
}