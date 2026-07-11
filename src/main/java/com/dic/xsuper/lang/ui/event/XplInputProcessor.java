package com.dic.xsuper.lang.ui.event;


public interface XplInputProcessor {

    void processMouseClick(Object rootElement, float mouseX, float mouseY);

    void processTextInput(Object rootElement, String character, int codepoint);

    /**
     * Envia um sinal bruto de tecla de controlo do sistema operativo (ex: Backspace, Enter, Delete)
     * capturado pelo orquestrador para ser processado no elemento que detém o foco ativo no DOM.
     *
     * @param rootElement O nó raiz da árvore do DOM (XplWindowView) para iniciar a varredura.
     * @param keyCode     O código numérico nativo da tecla enviado pelo hardware (GLFW_KEY_*).
     */
    void processControlKey(Object rootElement, int keyCode);

    // Substitua o método processMouseScroll dentro do seu XplInputManager.java por este:
    void processMouseScroll(Object rootElement, float mouseX, float mouseY, float scrollDelta);
}
