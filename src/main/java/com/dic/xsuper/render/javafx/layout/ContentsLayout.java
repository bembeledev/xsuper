package com.dic.xsuper.render.javafx.layout;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.render.javafx.layout.panes.ContentsContainerPane;
import javafx.scene.layout.Pane;

public class ContentsLayout implements LayoutManager {
    @Override
    public Pane createContainer(NativeTag tag) {
        return new ContentsContainerPane();
    }
}