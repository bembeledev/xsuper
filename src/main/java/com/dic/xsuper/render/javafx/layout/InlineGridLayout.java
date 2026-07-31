package com.dic.xsuper.render.javafx.layout;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.render.javafx.layout.panes.InlineGridContainerPane;
import javafx.scene.layout.Pane;

public class InlineGridLayout implements LayoutManager {
    @Override
    public Pane createContainer(NativeTag tag) {
        return new InlineGridContainerPane(tag.getRawStyles());
    }
}