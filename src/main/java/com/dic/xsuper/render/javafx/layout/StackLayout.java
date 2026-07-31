package com.dic.xsuper.render.javafx.layout;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.render.javafx.layout.panes.StackContainerPane;
import javafx.scene.layout.Pane;

public class StackLayout implements LayoutManager {
    @Override
    public Pane createContainer(NativeTag tag) {
        return new StackContainerPane();
    }
}