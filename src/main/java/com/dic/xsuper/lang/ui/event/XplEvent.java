package com.dic.xsuper.lang.ui.event;


public class XplEvent {

    private final XplEventType type;     // Substituído String por Enum
    private final Object target;
    private final float mouseX;
    private final float mouseY;
    private final XplKeyCode keyCode;    // Substituído int por Enum

    public XplEvent(XplEventType type, Object target, float mouseX, float mouseY, XplKeyCode keyCode) {
        this.type = type;
        this.target = target;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.keyCode = keyCode != null ? keyCode : XplKeyCode.KEY_UNKNOWN;
    }

    // --- Getters Atualizados ---

    public XplEventType getType() {
        return this.type;
    }

    public Object getTarget() {
        return this.target;
    }

    public float getMouseX() {
        return this.mouseX;
    }

    public float getMouseY() {
        return this.mouseY;
    }

    public XplKeyCode getKeyCode() {
        return this.keyCode;
    }
}
