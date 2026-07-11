package com.dic.xsuper.lang.ui.event;

/**
 * Enum que mapeia todos os eventos DOM da web (e alguns específicos do Xplorer).
 * Baseado nas especificações W3C e WHATWG.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Events">MDN Web Events</a>
 */
public enum XplEventType {

    // =====================================================================
    // 1. EVENTOS DE MOUSE
    // =====================================================================

    /** Clique simples (botão esquerdo) */
    CLICK("click"),
    /** Duplo clique */
    DBLCLICK("dblclick"),
    /** Botão do mouse pressionado */
    MOUSEDOWN("mousedown"),
    /** Botão do mouse solto */
    MOUSEUP("mouseup"),
    /** Movimento do mouse */
    MOUSEMOVE("mousemove"),
    /** Mouse entra no elemento (sem bubble) */
    MOUSEENTER("mouseenter"),
    /** Mouse sai do elemento (sem bubble) */
    MOUSELEAVE("mouseleave"),
    /** Mouse entra ou passa por cima (com bubble) */
    MOUSEOVER("mouseover"),
    /** Mouse sai (com bubble) */
    MOUSEOUT("mouseout"),
    /** Clique com botão direito */
    CONTEXTMENU("contextmenu"),
    /** Roda do mouse (scroll) */
    WHEEL("wheel"),
    /** Roda do mouse (obsoleto, mas mantido para compatibilidade) */
    SCROLL("scroll"),

    // =====================================================================
    // 2. EVENTOS DE TECLADO
    // =====================================================================

    /** Tecla pressionada (qualquer tecla) */
    KEYDOWN("keydown"),
    /** Tecla solta */
    KEYUP("keyup"),
    /** Tecla que produz caractere (obsoleto, mas suportado) */
    KEYPRESS("keypress"),
    /** Caractere digitado (similar ao keypress, mas mais moderno) */
    INPUT_TEXT("inputtext"),

    // =====================================================================
    // 3. EVENTOS DE FOCUS
    // =====================================================================

    /** Elemento ganha foco (sem bubble) */
    FOCUS("focus"),
    /** Elemento perde foco (sem bubble) */
    BLUR("blur"),
    /** Foco entra (com bubble) */
    FOCUSIN("focusin"),
    /** Foco sai (com bubble) */
    FOCUSOUT("focusout"),

    // =====================================================================
    // 4. EVENTOS DE FORMULÁRIO
    // =====================================================================

    /** Valor de input muda (em cada caractere) */
    INPUT("input"),
    /** Mudança confirmada (Enter ou perda de foco) */
    CHANGE("change"),
    /** Formulário submetido */
    SUBMIT("submit"),
    /** Formulário resetado */
    RESET("reset"),
    /** Texto selecionado */
    SELECT("select"),
    /** Campo fica inválido */
    INVALID("invalid"),

    // =====================================================================
    // 5. EVENTOS DE CLIPBOARD
    // =====================================================================

    /** Cópia (Ctrl+C) */
    COPY("copy"),
    /** Corte (Ctrl+X) */
    CUT("cut"),
    /** Colar (Ctrl+V) */
    PASTE("paste"),
    /** Antes de colar (permite validar) */
    PASTE_START("pastestart"),

    // =====================================================================
    // 6. EVENTOS DE DRAG & DROP
    // =====================================================================

    /** Início do arrasto */
    DRAGSTART("dragstart"),
    /** Durante o arrasto */
    DRAG("drag"),
    /** Fim do arrasto */
    DRAGEND("dragend"),
    /** Entra num alvo de drop */
    DRAGENTER("dragenter"),
    /** Sai do alvo de drop */
    DRAGLEAVE("dragleave"),
    /** Sobre um alvo de drop (em movimento) */
    DRAGOVER("dragover"),
    /** Largar sobre alvo */
    DROP("drop"),

    // =====================================================================
    // 7. EVENTOS DE JANELA / DOCUMENTO
    // =====================================================================

    /** Página carregada */
    LOAD("load"),
    /** Página descarregada (fecho da janela) */
    UNLOAD("unload"),
    /** Antes de descarregar (permite confirmar) */
    BEFOREUNLOAD("beforeunload"),
    /** Janela redimensionada */
    RESIZE("resize"),
    /** Janela movida */
    MOVE("move"),
    /** Janela ganhou foco */
    WINDOW_FOCUS("windowfocus"),
    /** Janela perdeu foco */
    WINDOW_BLUR("windowblur"),
    /** Visibilidade da página mudou */
    VISIBILITY_CHANGE("visibilitychange"),
    /** Estado de carregamento mudou */
    READYSTATE_CHANGE("readystatechange"),
    /** Erro (ex: carregamento de imagem) */
    ERROR("error"),

    // =====================================================================
    // 8. EVENTOS DE TELA CHEIA
    // =====================================================================

    /** Entrou em tela cheia */
    FULLSCREEN_CHANGE("fullscreenchange"),
    /** Falha ao entrar em tela cheia */
    FULLSCREEN_ERROR("fullscreenerror"),

    // =====================================================================
    // 9. EVENTOS DE TOQUE (para mobile / touch)
    // =====================================================================

    /** Toque na tela */
    TOUCHSTART("touchstart"),
    /** Movimento do toque */
    TOUCHMOVE("touchmove"),
    /** Fim do toque */
    TOUCHEND("touchend"),
    /** Toque cancelado */
    TOUCHCANCEL("touchcancel"),

    // =====================================================================
    // 10. EVENTOS DE MÍDIA (áudio / vídeo)
    // =====================================================================

    /** Início da reprodução */
    PLAY("play"),
    /** Pausa */
    PAUSE("pause"),
    /** Fim da reprodução */
    ENDED("ended"),
    /** Atualização do tempo */
    TIMEUPDATE("timeupdate"),
    /** Mudança de volume */
    VOLUMECHANGE("volumechange"),
    /** Dados carregados */
    LOADEDDATA("loadeddata"),
    /** Metadados carregados */
    LOADEDMETADATA("loadedmetadata"),
    /** Início do carregamento */
    LOADSTART("loadstart"),
    /** Progresso do carregamento */
    PROGRESS("progress"),
    /** Reprodução pronta */
    CANPLAY("canplay"),
    /** Reprodução pronta sem interrupções */
    CANPLAYTHROUGH("canplaythrough"),
    /** Reprodução parou (buffer underrun) */
    WAITING("waiting"),
    /** Áudio/vídeo está sendo buscado */
    SEEKING("seeking"),
    /** Busca finalizada */
    SEEKED("seeked"),

    // =====================================================================
    // 11. EVENTOS DE ANIMAÇÃO / TRANSIÇÃO
    // =====================================================================

    /** Início da animação CSS */
    ANIMATIONSTART("animationstart"),
    /** Fim da animação CSS */
    ANIMATIONEND("animationend"),
    /** Animação CSS repetida */
    ANIMATIONITERATION("animationiteration"),
    /** Fim da transição CSS */
    TRANSITIONEND("transitionend"),
    /** Início da transição CSS */
    TRANSITIONSTART("transitionstart"),
    /** Transição CSS cancelada */
    TRANSITIONCANCEL("transitioncancel"),

    // =====================================================================
    // 12. EVENTOS DE HISTÓRICO E NAVEGAÇÃO
    // =====================================================================

    /** Navegação para trás/para frente no histórico */
    POPSTATE("popstate"),
    /** Mudança de hash na URL */
    HASHCHANGE("hashchange"),
    /** Página carregada (com cache) */
    PAGESHOW("pageshow"),
    /** Página descarregada */
    PAGEHIDE("pagehide"),

    // =====================================================================
    // 13. EVENTOS DE REDE (OFFLINE / ONLINE)
    // =====================================================================

    /** Conexão online */
    ONLINE("online"),
    /** Conexão offline */
    OFFLINE("offline"),

    // =====================================================================
    // 14. EVENTOS DE TECLADO (CONTROLE XPL)
    // =====================================================================

    /** Tecla de controle (Backspace, Enter, Escape, etc.) */
    CONTROL_KEY("controlkey"),
    /** Cancelamento (Escape) */
    CANCEL("cancel"),

    // =====================================================================
    // 15. EVENTOS ESTRUTURAIS (MUTAÇÃO DO DOM - XPL)
    // =====================================================================

    /** Mudança na estrutura da árvore DOM */
    STRUCTURE_CHANGE("structure-change"),
    /** Mudança no texto de um elemento */
    TEXT_CHANGE("text-change"),
    /** Elemento adicionado ao DOM */
    NODE_ADDED("node-added"),
    /** Elemento removido do DOM */
    NODE_REMOVED("node-removed"),
    /** Mudança em um atributo */
    ATTRIBUTE_CHANGED("attribute-changed"),

    // =====================================================================
    // 16. EVENTOS DE PORTABILIDADE / RENDERIZAÇÃO
    // =====================================================================

    /** Antes de renderizar o frame */
    BEFORE_RENDER("beforerender"),
    /** Após renderizar o frame */
    AFTER_RENDER("afterrender"),
    /** Layout foi recalculado */
    LAYOUT_UPDATED("layout-updated"),

    // =====================================================================
    // 17. EVENTOS DE CONSOLE / DEBUG (XPL)
    // =====================================================================

    /** Mensagem de log */
    CONSOLE_LOG("console-log"),
    /** Mensagem de erro */
    CONSOLE_ERROR("console-error"),
    /** Aviso */
    CONSOLE_WARN("console-warn"),

    // =====================================================================
    // 18. EVENTOS DE DRAG (W3C) - ATALHOS
    // =====================================================================

    /** Início do arraste (trigger) */
    DRAG_START("drag-start"),
    /** Enquanto arrasta */
    DRAG_MOVE("drag-move"),
    /** Fim do arraste */
    DRAG_END("drag-end"),

    // =====================================================================
    // 19. EVENTOS DE GESTOS (para dispositivos touch/tablet)
    // =====================================================================

    /** Pinch (zoom) */
    PINCH("pinch"),
    /** Rotação com dois dedos */
    ROTATE("rotate"),
    /** Swipe (deslizar) */
    SWIPE("swipe"),

    // =====================================================================
    // 20. EVENTOS DE TECLADO (FÍSICO / VIRTUAL)
    // =====================================================================

    /** Tecla digitada (apenas caracteres imprimíveis) */
    KEY_TYPED("keytyped"),
    /** Tecla de atalho (combinação de teclas) */
    KEY_SHORTCUT("keyshortcut"),

    // =====================================================================
    // 21. EVENTOS DE FIM DE VIDA / DESTRUIÇÃO
    // =====================================================================

    /** Elemento está prestes a ser destruído */
    BEFORE_DESTROY("beforedestroy"),
    /** Elemento foi destruído */
    DESTROYED("destroyed");

    // =====================================================================
    // ATRIBUTO E CONSTRUTOR
    // =====================================================================

    private final String webName;

    XplEventType(String webName) {
        this.webName = webName;
    }

    public String getWebName() {
        return this.webName;
    }

    /**
     * Traduz uma string vinda da Xplorer para o tipo Enum correspondente de forma segura.
     */
    public static XplEventType fromString(String text) {
        if (text == null) return null;
        String clean = text.toLowerCase().trim();
        for (XplEventType type : values()) {
            if (type.webName.equals(clean)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Verifica se este evento pertence à categoria de mouse.
     */
    public boolean isMouseEvent() {
        return this == CLICK || this == DBLCLICK || this == MOUSEDOWN ||
                this == MOUSEUP || this == MOUSEMOVE || this == MOUSEENTER ||
                this == MOUSELEAVE || this == MOUSEOVER || this == MOUSEOUT ||
                this == CONTEXTMENU || this == WHEEL || this == SCROLL;
    }

    /**
     * Verifica se este evento pertence à categoria de teclado.
     */
    public boolean isKeyboardEvent() {
        return this == KEYDOWN || this == KEYUP || this == KEYPRESS || this == INPUT_TEXT ||
                this == CONTROL_KEY || this == CANCEL || this == KEY_TYPED || this == KEY_SHORTCUT;
    }

    /**
     * Verifica se este evento pertence à categoria de foco.
     */
    public boolean isFocusEvent() {
        return this == FOCUS || this == BLUR || this == FOCUSIN || this == FOCUSOUT;
    }

    /**
     * Verifica se este evento pertence à categoria de formulário.
     */
    public boolean isFormEvent() {
        return this == INPUT || this == CHANGE || this == SUBMIT ||
                this == RESET || this == SELECT || this == INVALID;
    }

    /**
     * Verifica se este evento pertence à categoria de clipboard.
     */
    public boolean isClipboardEvent() {
        return this == COPY || this == CUT || this == PASTE || this == PASTE_START;
    }

    /**
     * Verifica se este evento pertence à categoria de drag & drop.
     */
    public boolean isDragEvent() {
        return this == DRAGSTART || this == DRAG || this == DRAGEND ||
                this == DRAGENTER || this == DRAGLEAVE || this == DRAGOVER ||
                this == DROP || this == DRAG_START || this == DRAG_MOVE || this == DRAG_END;
    }

    /**
     * Verifica se este evento pertence à categoria de janela.
     */
    public boolean isWindowEvent() {
        return this == LOAD || this == UNLOAD || this == BEFOREUNLOAD ||
                this == RESIZE || this == MOVE || this == WINDOW_FOCUS ||
                this == WINDOW_BLUR || this == VISIBILITY_CHANGE ||
                this == READYSTATE_CHANGE || this == ERROR;
    }

    /**
     * Verifica se este evento é específico do Xplorer (não existe na web).
     */
    public boolean isXplEvent() {
        return this == STRUCTURE_CHANGE || this == TEXT_CHANGE ||
                this == NODE_ADDED || this == NODE_REMOVED ||
                this == ATTRIBUTE_CHANGED || this == BEFORE_RENDER ||
                this == AFTER_RENDER || this == LAYOUT_UPDATED ||
                this == CONSOLE_LOG || this == CONSOLE_ERROR ||
                this == CONSOLE_WARN || this == BEFORE_DESTROY ||
                this == DESTROYED;
    }

    @Override
    public String toString() {
        return this.webName;
    }
}