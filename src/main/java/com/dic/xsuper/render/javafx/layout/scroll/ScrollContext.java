package com.dic.xsuper.render.javafx.layout.scroll;

import com.dic.xsuper.dom.node.XplElement;
import com.dic.xsuper.render.javafx.document.XplScroll;

public class ScrollContext {
    private final XplElement element;
    private XplScroll scrollOwner;  // referência ao objeto de scroll
    private ScrollBar verticalBar;
    private ScrollBar horizontalBar;
    private double viewportWidth, viewportHeight;
    private double contentWidth, contentHeight;

    public ScrollContext(XplElement element) {
        this.element = element;
    }

    public void setScrollOwner(XplScroll scroll) {
        this.scrollOwner = scroll;
    }

    public XplElement getElement() { return element; }
    public ScrollBar getVerticalBar() { return verticalBar; }
    public ScrollBar getHorizontalBar() { return horizontalBar; }
    public double getViewportWidth() { return viewportWidth; }
    public double getViewportHeight() { return viewportHeight; }
    public double getContentWidth() { return contentWidth; }
    public double getContentHeight() { return contentHeight; }

    public void setViewportSize(double width, double height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
        updateBars();
    }

    public void setContentSize(double width, double height) {
        this.contentWidth = width;
        this.contentHeight = height;
        updateBars();
    }

    // Chamado pelo XplScroll quando o estado de scroll muda
    public void notifyScrollChanged() {
        updateBarGeometry();
    }

    // Chamado pelo XplScroll quando o overflow muda
    public void notifyOverflowChanged() {
        updateBars();
    }

    private void updateBars() {
        if (scrollOwner == null) return;
        ScrollPolicy policyX = ScrollPolicy.fromString(scrollOwner.getOverflowX());
        ScrollPolicy policyY = ScrollPolicy.fromString(scrollOwner.getOverflowY());

        boolean needsV = policyY.showsScrollbars() &&
                (policyY == ScrollPolicy.SCROLL || contentHeight > viewportHeight);
        boolean needsH = policyX.showsScrollbars() &&
                (policyX == ScrollPolicy.SCROLL || contentWidth > viewportWidth);

        if (needsV && verticalBar == null) {
            verticalBar = new ScrollBar(ScrollBar.Orientation.VERTICAL, element.getId());
            verticalBar.setTheme(ScrollTheme.darkModern());
        } else if (!needsV && verticalBar != null) {
            verticalBar = null;
        }

        if (needsH && horizontalBar == null) {
            horizontalBar = new ScrollBar(ScrollBar.Orientation.HORIZONTAL, element.getId());
            horizontalBar.setTheme(ScrollTheme.darkModern());
        } else if (!needsH && horizontalBar != null) {
            horizontalBar = null;
        }

        updateBarGeometry();
    }

    private void updateBarGeometry() {
        if (scrollOwner == null) return;
        double barWidth = 10;
        float scrollTop = scrollOwner.getScrollTop();
        float scrollLeft = scrollOwner.getScrollLeft();

        if (verticalBar != null) {
            double trackX = viewportWidth - barWidth;
            double trackY = 0;
            double trackW = barWidth;
            double trackH = viewportHeight - (horizontalBar != null ? barWidth : 0);
            verticalBar.setTrack(trackX, trackY, trackW, trackH);

            float contentH = (float) contentHeight;
            float viewH = (float) viewportHeight;
            double thumbLen = Math.max(20, trackH * (viewH / contentH));
            double thumbPos = (scrollTop / (contentH - viewH)) * (trackH - thumbLen);
            if (Float.isNaN((float) thumbPos)) thumbPos = 0;
            verticalBar.setThumb(trackX, trackY + thumbPos, trackW, thumbLen);
            verticalBar.setVisible(true);
            verticalBar.setTotalSize(contentH);
            verticalBar.setVisibleSize(viewH);
            verticalBar.setCurrentValue(scrollTop);
        }

        if (horizontalBar != null) {
            double trackX = 0;
            double trackY = viewportHeight - barWidth;
            double trackW = viewportWidth - (verticalBar != null ? barWidth : 0);
            double trackH = barWidth;
            horizontalBar.setTrack(trackX, trackY, trackW, trackH);

            float contentW = (float) contentWidth;
            float viewW = (float) viewportWidth;
            double thumbLen = Math.max(20, trackW * (viewW / contentW));
            double thumbPos = (scrollLeft / (contentW - viewW)) * (trackW - thumbLen);
            if (Float.isNaN((float) thumbPos)) thumbPos = 0;
            horizontalBar.setThumb(trackX + thumbPos, trackY, thumbLen, trackH);
            horizontalBar.setVisible(true);
            horizontalBar.setTotalSize(contentW);
            horizontalBar.setVisibleSize(viewW);
            horizontalBar.setCurrentValue(scrollLeft);
        }
    }

    public boolean handleMouseDown(double x, double y) {
        if (verticalBar != null && verticalBar.isPointOnThumb(x, y)) {
            verticalBar.setState(ScrollBar.State.DRAGGING);
            return true;
        }
        if (horizontalBar != null && horizontalBar.isPointOnThumb(x, y)) {
            horizontalBar.setState(ScrollBar.State.DRAGGING);
            return true;
        }
        if (verticalBar != null && verticalBar.isPointOnTrack(x, y)) {
            float val = (float) verticalBar.valueFromPosition(x, y);
            if (scrollOwner != null) scrollOwner.setScrollTop(val);
            return true;
        }
        if (horizontalBar != null && horizontalBar.isPointOnTrack(x, y)) {
            float val = (float) horizontalBar.valueFromPosition(x, y);
            if (scrollOwner != null) scrollOwner.setScrollLeft(val);
            return true;
        }
        return false;
    }

    public void handleMouseMove(double x, double y) {
        if (verticalBar != null && verticalBar.getState() == ScrollBar.State.DRAGGING) {
            float val = (float) verticalBar.valueFromPosition(x, y);
            if (scrollOwner != null) scrollOwner.setScrollTop(val);
        }
        if (horizontalBar != null && horizontalBar.getState() == ScrollBar.State.DRAGGING) {
            float val = (float) horizontalBar.valueFromPosition(x, y);
            if (scrollOwner != null) scrollOwner.setScrollLeft(val);
        }
    }

    public void handleMouseUp() {
        if (verticalBar != null) verticalBar.setState(ScrollBar.State.IDLE);
        if (horizontalBar != null) horizontalBar.setState(ScrollBar.State.IDLE);
    }

    public boolean hasScroll() {
        return verticalBar != null || horizontalBar != null;
    }
}