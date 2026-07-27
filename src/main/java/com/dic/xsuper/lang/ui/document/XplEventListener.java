package com.dic.xsuper.lang.ui.document;

import com.dic.xsuper.lang.ui.event.XplEvent;

/**
 * Interface funcional para ouvintes de eventos.
 * Equivalente a EventListener no DOM (e ao tipo função em JavaScript).
 */
@FunctionalInterface
public interface XplEventListener {
    /**
     * Método chamado quando o evento ocorre.
     * @param event O objecto evento.
     */
    void handleEvent(XplEvent event);

    // Pode adicionar métodos default para compatibilidade (ex: equals para remoção)
    // Em Java, a igualdade é por referência; para remover, guardamos a instância.
}