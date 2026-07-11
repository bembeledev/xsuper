package com.dic.xsuper.lang.ui.event;

@FunctionalInterface
public interface XplEventListener {

    /**
     * Contrato universal para tratamento de eventos.
     * Sempre que um evento (como clique, foco ou tecla) for disparado numa tag,
     * este método é executado passando os dados consolidados do evento.
     *
     * @param event O objeto contendo o target, coordenadas e metadados do evento.
     */
    void handleEvent(XplEvent event);
}
