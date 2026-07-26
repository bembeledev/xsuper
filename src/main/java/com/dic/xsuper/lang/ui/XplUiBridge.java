package com.dic.xsuper.lang.ui;

import com.dic.xsuper.lang.ui.document.XplElement;
import com.dic.xsuper.lang.ui.html.XplNode;

/**
 * A Ponte de Comunicação: O contrato agnóstico entre a SuperUiEngine (Cérebro)
 * e o Renderizador Gráfico (Músculos - ex: JavaFX).
 */
public interface XplUiBridge {

    // =====================================================================
    // 1. FLUXO: ENGINE -> UI (O Cérebro dá ordens)
    // =====================================================================

    /**
     * Recebe a Árvore VDOM limpa (activa) gerada pelo DomEvaluator e desenha-a no ecrã.
     * O Renderizador deve apagar a tela anterior e desenhar esta nova estrutura.
     */
    void renderView(XplNode activeDomRoot);

    /**
     * Actualização Cirúrgica (Otimização de Performance).
     * Em vez de redesenhar a tela toda, a Engine pode mandar mudar apenas uma propriedade.
     * Ex: updateProperty("btn-1", "disabled", "true")
     */
    void updateProperty(String nodeId, String propertyName, Object newValue);

    // =====================================================================
    // 2. FLUXO: UI -> ENGINE (Os Músculos reagem ao utilizador)
    // =====================================================================

    /**
     * O Renderizador não tem cérebro, por isso precisa de saber a quem "gritar"
     * quando o utilizador clica num botão ou digita num input.
     * A Engine vai injectar-se a si mesma aqui.
     */
    void setEngineCallback(EngineCallback callback);

    void rebuildFullView(String targetId, com.dic.xsuper.lang.ui.html.XplNode virtualNode);

    void invokeMethodOnNode(String targetId, String methodName, Object[] args);

    /**
     * O canal de comunicação para enviar eventos de volta.
     */
    interface EngineCallback {
        /**
         * @param eventName O nome do evento (ex: "compilarTudo()")
         * @param payload Os dados extra (ex: o texto digitado num input, ou as coordenadas do rato)
         */
        void onEvent(String eventName, Object payload);
    }

    // =====================================================================
    // 3. TRATAMENTO DE ERROS VISUAIS
    // =====================================================================

    /**
     * Se o motor gráfico (JavaFX) não conseguir pintar algo, ou um recurso falhar,
     * ele envia o erro de volta para a Engine para ser tratado no XPL.
     */
    void reportError(String message);
}