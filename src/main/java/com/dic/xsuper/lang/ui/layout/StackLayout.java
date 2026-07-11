package com.dic.xsuper.lang.ui.layout;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.layout.panes.StackContainerPane;
import javafx.scene.layout.Pane;

public class StackLayout implements LayoutManager {
    @Override
    public Pane createContainer(NativeTag tag) {
        return new StackContainerPane();
    }
}