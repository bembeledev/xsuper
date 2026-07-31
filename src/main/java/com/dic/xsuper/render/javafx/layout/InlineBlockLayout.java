package com.dic.xsuper.render.javafx.layout;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.render.javafx.layout.panes.InlineBlockContainerPane;
import javafx.scene.layout.Pane;

public class InlineBlockLayout implements LayoutManager {
    @Override
    public Pane createContainer(NativeTag tag) {
        return new InlineBlockContainerPane();
    }
}