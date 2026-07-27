package com.dic.xsuper.lang.ui.document;

import com.dic.xsuper.lang.ui.layout.scroll.ScrollContext;
import com.dic.xsuper.lang.ui.layout.scroll.ScrollEngine;

/**
 * Encapsula o estado e comportamento de scroll de um elemento DOM.
 * Desacoplado do XplElement para manter a separação de responsabilidades.
 */
public class XplScroll {
    private final XplElement element;
    private final ScrollContext context;

    private float scrollTop = 0;
    private float scrollLeft = 0;
    private String overflowX = "visible";
    private String overflowY = "visible";

    public XplScroll(XplElement element, ScrollEngine engine) {
        this.element = element;
        this.context = engine.getContext(element);
        // Passa a referência deste XplScroll para o contexto
        this.context.setScrollOwner(this);
    }

    // ─── Estado de scroll ──────────────────────────────────────────
    public float getScrollTop() { return scrollTop; }
    public void setScrollTop(float value) {
        float old = this.scrollTop;
        this.scrollTop = Math.max(0, value);
        if (old != this.scrollTop) {
            context.notifyScrollChanged();
        }
    }

    public float getScrollLeft() { return scrollLeft; }
    public void setScrollLeft(float value) {
        float old = this.scrollLeft;
        this.scrollLeft = Math.max(0, value);
        if (old != this.scrollLeft) {
            context.notifyScrollChanged();
        }
    }

    public String getOverflowX() { return overflowX; }
    public void setOverflowX(String overflowX) {
        this.overflowX = normalizeOverflow(overflowX);
        context.notifyOverflowChanged();
    }

    public String getOverflowY() { return overflowY; }
    public void setOverflowY(String overflowY) {
        this.overflowY = normalizeOverflow(overflowY);
        context.notifyOverflowChanged();
    }

    private String normalizeOverflow(String value) {
        if (value == null) return "visible";
        String v = value.trim().toLowerCase();
        return switch (v) {
            case "visible", "hidden", "scroll", "auto" -> v;
            default -> "visible";
        };
    }

    // ─── Atualização a partir de CSS ─────────────────────────────
    public void parseOverflowStyles() {
        String overflow = element.getStyle("overflow");
        if (overflow != null) {
            setOverflowX(overflow);
            setOverflowY(overflow);
        }
        String ox = element.getStyle("overflow-x");
        if (ox != null) setOverflowX(ox);
        String oy = element.getStyle("overflow-y");
        if (oy != null) setOverflowY(oy);
    }

    // ─── Delegação para o ScrollContext ──────────────────────────
    public ScrollContext getContext() { return context; }

    public boolean hasScroll() {
        return context.hasScroll();
    }

    // Métodos para interação com o mouse (delegados ao contexto)
    public boolean handleMouseDown(double x, double y) {
        return context.handleMouseDown(x, y);
    }

    public void handleMouseMove(double x, double y) {
        context.handleMouseMove(x, y);
    }

    public void handleMouseUp() {
        context.handleMouseUp();
    }

    // ─── Dimensões (para compatibilidade com o XplElement) ──────
    public int getClientWidth() { return (int) context.getViewportWidth(); }
    public int getClientHeight() { return (int) context.getViewportHeight(); }
    public int getScrollWidth() { return (int) context.getContentWidth(); }
    public int getScrollHeight() { return (int) context.getContentHeight(); }
    public int getOffsetWidth() { return getClientWidth(); }
    public int getOffsetHeight() { return getClientHeight(); }
}