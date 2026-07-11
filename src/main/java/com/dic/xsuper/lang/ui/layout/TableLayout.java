package com.dic.xsuper.lang.ui.layout;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.layout.panes.TableContainerPane;
import javafx.scene.layout.Pane;

public class TableLayout implements LayoutManager {
    @Override
    public Pane createContainer(NativeTag tag) {
        return new TableContainerPane(tag.getRawStyles());
    }
}