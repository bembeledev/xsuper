package com.dic.xsuper.render.javafx.layout.scroll;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

public interface ScrollBarRenderer {
    Node renderScrollBar(ScrollBar scrollBar, Pane parent);
    void updateScrollBar(ScrollBar scrollBar, Node scrollBarNode);
    void removeScrollBar(ScrollBar scrollBar, Node scrollBarNode);
    Pane createScrollContainer();
    void applyClip(Pane container, double width, double height);
}