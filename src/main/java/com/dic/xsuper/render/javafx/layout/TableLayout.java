package com.dic.xsuper.render.javafx.layout;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.render.javafx.layout.panes.TableContainerPane;
import javafx.scene.layout.Pane;

public class TableLayout implements LayoutManager {
    @Override
    public Pane createContainer(NativeTag tag) {
        return new TableContainerPane(tag.getRawStyles());
    }
}