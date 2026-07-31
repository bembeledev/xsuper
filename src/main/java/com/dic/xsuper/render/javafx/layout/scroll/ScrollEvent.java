package com.dic.xsuper.render.javafx.layout.scroll;

import com.dic.xsuper.dom.node.XplElement;

public class ScrollEvent {
    public enum Type { SCROLL_VERTICAL, SCROLL_HORIZONTAL, SCROLL_START, SCROLL_END }
    private final XplElement source;
    private final Type type;
    private final float value;
    private final float scrollLeft, scrollTop;

    public ScrollEvent(XplElement source, Type type, float value, float scrollLeft, float scrollTop) {
        this.source = source; this.type = type; this.value = value;
        this.scrollLeft = scrollLeft; this.scrollTop = scrollTop;
    }
    // getters...
}

