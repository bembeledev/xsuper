package com.dic.xsuper.lang.ui.event;

import java.util.ArrayList;
import java.util.List;


public class XplGlobalEventBind {

    private final long window;
    private final Object rootWindowView;
    private final List<XplGlobalEventListener> globalListeners = new ArrayList<>();

    // Rastreia o estado do mouse (para double-click e enter/leave)
    private boolean mouseInsideWindow = false;
    private long lastClickTime = 0;
    private float lastClickX = 0, lastClickY = 0;
    private static final long DOUBLE_CLICK_THRESHOLD = 300_000_000L; // 300ms em nanosegundos
    private final XplInputProcessor inputProcessor;
    private final Object uiContext;
    // Lista auxiliar para compilação estável
    private static final List<XplGlobalEventListener> globalListenersDummy = new ArrayList<>();
    public XplGlobalEventBind(long window, Object rootWindowView, XplInputProcessor inputProcessor, Object uiContext) {
        this.window = window;
        this.rootWindowView = rootWindowView;
        this.inputProcessor = inputProcessor;
        this.uiContext = uiContext;
        setupCallbacks();
    }

    public void addGlobalEventListener(XplGlobalEventListener listener) {
        if (listener != null && !globalListeners.contains(listener)) {
            this.globalListeners.add(listener);
        }
    }

    public void dispatchWindowClose() {
        for (XplGlobalEventListener listener : globalListeners) {
            listener.onWindowUnload();
            listener.onWindowClose();
        }
    }

    private void setupCallbacks() {

    }

    // Chamado no primeiro frame
    private void dispatchLoad() {
        for (XplGlobalEventListener listener : globalListeners) {
            listener.onWindowLoad();
        }
    }
}