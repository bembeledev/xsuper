package com.dic.xsuper.render.javafx.layout.scroll;

import com.dic.xsuper.dom.node.XplElement;

import java.util.HashMap;
import java.util.Map;

public class ScrollEngine {
    private final Map<XplElement, ScrollContext> contexts = new HashMap<>();
    private ScrollBarRenderer renderer;

    public ScrollEngine(ScrollBarRenderer renderer) {
        this.renderer = renderer;
    }

    public ScrollContext getContext(XplElement element) {
        return contexts.computeIfAbsent(element, e -> new ScrollContext(e));
    }

    public void removeContext(XplElement element) {
        contexts.remove(element);
    }

    public void updateElement(XplElement element, double viewportWidth, double viewportHeight) {
        ScrollContext ctx = getContext(element);
        ctx.setViewportSize(viewportWidth, viewportHeight);
    }

    public void setContentSize(XplElement element, double width, double height) {
        ScrollContext ctx = getContext(element);
        ctx.setContentSize(width, height);
    }

    public boolean handleMouseDown(XplElement element, double x, double y) {
        ScrollContext ctx = contexts.get(element);
        return ctx != null && ctx.handleMouseDown(x, y);
    }

    public void handleMouseMove(XplElement element, double x, double y) {
        ScrollContext ctx = contexts.get(element);
        if (ctx != null) ctx.handleMouseMove(x, y);
    }

    public void handleMouseUp(XplElement element) {
        ScrollContext ctx = contexts.get(element);
        if (ctx != null) ctx.handleMouseUp();
    }

    public ScrollBarRenderer getRenderer() { return renderer; }
    public void setRenderer(ScrollBarRenderer renderer) { this.renderer = renderer; }

    public void clear() { contexts.clear(); }
}