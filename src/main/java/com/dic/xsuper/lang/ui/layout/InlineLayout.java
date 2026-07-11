package com.dic.xsuper.lang.ui.layout;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.layout.panes.InlineContainerPane;
import javafx.scene.layout.Pane;

public class InlineLayout implements LayoutManager {

    @Override
    public Pane createContainer(NativeTag tag) {
        // Passa os estilos brutos para que o pane consiga ler o text-align
        return new InlineContainerPane(tag.getRawStyles());
    }

}