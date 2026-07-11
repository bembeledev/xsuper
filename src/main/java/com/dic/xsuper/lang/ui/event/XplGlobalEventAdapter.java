package com.dic.xsuper.lang.ui.event;

public abstract class XplGlobalEventAdapter implements XplGlobalEventListener {
    @Override public void onWindowLoad() {}
    @Override public void onWindowUnload() {}
    @Override public void onWindowResize(int newWidth, int newHeight) {}
    @Override public void onWindowMove(int newX, int newY) {}
    @Override public void onWindowFocus() {}
    @Override public void onWindowBlur() {}
    @Override public void onWindowClose() {}
    @Override public void onGlobalKeyDown(int keyCode, int modifiers) {}
    @Override public void onGlobalKeyUp(int keyCode, int modifiers) {}
    @Override public void onGlobalKeyPress(char character) {}
    @Override public void onGlobalKeyTyped(char character) {}
    @Override public void onGlobalMouseMove(float mouseX, float mouseY) {}
    @Override public void onGlobalMouseDown(int button, float mouseX, float mouseY) {}
    @Override public void onGlobalMouseUp(int button, float mouseX, float mouseY) {}
    @Override public void onGlobalMouseClick(int button, float mouseX, float mouseY) {}
    @Override public void onGlobalMouseDoubleClick(float mouseX, float mouseY) {}
    @Override public void onGlobalMouseWheel(float mouseX, float mouseY, float deltaX, float deltaY) {}
    @Override public void onGlobalMouseEnter() {}
    @Override public void onGlobalMouseLeave() {}
    @Override public void onGlobalCopy() {}
    @Override public void onGlobalCut() {}
    @Override public void onGlobalPaste() {}
    @Override public void onFullscreenChange(boolean isFullscreen) {}
}