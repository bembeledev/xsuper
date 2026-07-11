package com.dic.xsuper.lang.ui.event;

public interface XplGlobalEventListener {

    // ----- EVENTOS DE JANELA -----
    default void onWindowLoad() {}
    default void onWindowUnload() {}
    default void onWindowResize(int newWidth, int newHeight) {}
    default void onWindowMove(int newX, int newY) {}
    default void onWindowFocus() {}
    default void onWindowBlur() {}
    default void onWindowClose() {}

    // ----- EVENTOS DE TECLADO (GLOBAIS) -----
    default void onGlobalKeyDown(int keyCode, int modifiers) {}
    default void onGlobalKeyUp(int keyCode, int modifiers) {}
    default void onGlobalKeyPress(char character) {}

    // ----- EVENTOS DE MOUSE (GLOBAIS) -----
    default void onGlobalMouseMove(float mouseX, float mouseY) {}
    default void onGlobalMouseDown(int button, float mouseX, float mouseY) {}
    default void onGlobalMouseUp(int button, float mouseX, float mouseY) {}
    default void onGlobalMouseClick(int button, float mouseX, float mouseY) {}
    default void onGlobalMouseDoubleClick(float mouseX, float mouseY) {}
    default void onGlobalMouseWheel(float mouseX, float mouseY, float deltaX, float deltaY) {}
    default void onGlobalMouseEnter() {}
    default void onGlobalMouseLeave() {}

    // ----- EVENTOS DE CLIPBOARD (ATALHOS) -----
    default void onGlobalCopy() {}
    default void onGlobalCut() {}
    default void onGlobalPaste() {}

    // ----- EVENTOS DE TELA CHEIA -----
    default void onFullscreenChange(boolean isFullscreen) {}

    // ----- EVENTOS DE TECLADO (FÍSICO) -----
    default void onGlobalKeyTyped(char character) {}
}