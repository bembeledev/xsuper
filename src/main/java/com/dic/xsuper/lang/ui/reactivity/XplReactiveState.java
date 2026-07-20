package com.dic.xsuper.lang.ui.reactivity;

import com.dic.xsuper.lang.ui.html.XplNode;
import java.util.*;

/**
 * O Estado Reactivo do Componente (Estilo Angular Signals).
 * Um Map avançado que escuta e notifica alterações de estado.
 */
public class XplReactiveState {

    // Onde os valores reais vivem
    private final Map<String, Object> state = new HashMap<>();

    // Os "Ouvidos Pesados": [Nome da Variável] -> [Lista de Nós DOM que dependem dela]
    private final Map<String, Set<XplNode>> listeners = new HashMap<>();

    // O motor que fará a cirurgia de atualização
    private final XplReactivityRenderer renderer;

    public XplReactiveState(XplReactivityRenderer renderer) {
        this.renderer = renderer;
    }

    // ⭐ 1. LER E REGISTAR DEPENDENCIA (Fase de Carregamento do Template)
    public Object getAndTrack(String key, XplNode dependentNode) {
        // Regista que este nó tem os "ouvidos" nesta variável
        if (dependentNode != null) {
            listeners.computeIfAbsent(key, k -> new HashSet<>()).add(dependentNode);
        }
        return state.get(key);
    }

    // ⭐ 2. ESCREVER E DISPARAR (A Reatividade em Ação)
    public void put(String key, Object newValue) {
        Object oldValue = state.get(key);

        // Só dispara se o valor realmente mudou (evita renderizações inúteis)
        if (!Objects.equals(oldValue, newValue)) {
            state.put(key, newValue);

            // Acorda os ouvidos pesados!
            notifyListeners(key);
        }
    }

    private void notifyListeners(String key) {
        Set<XplNode> affectedNodes = listeners.get(key);
        if (affectedNodes != null && !affectedNodes.isEmpty()) {
            System.out.println("[Signal] ⚡ Variável '" + key + "' mudou! A atualizar " + affectedNodes.size() + " nó(s)...");

            // Manda o motor atualizar APENAS estes nós
            for (XplNode node : affectedNodes) {
                renderer.patchPartialDom(node, this.state);
            }
        }
    }

    // ⭐ A CURA DOS SINAIS: Extrai apenas os nomes reais das variáveis!
    public void track(String expression, XplNode dependentNode) {
        if (dependentNode == null || expression == null) return;

        // Corta a string por símbolos e espaços para isolar as palavras
        String[] words = expression.split("[^a-zA-Z0-9_]+");

        for (String word : words) {
            // Ignora números soltos, booleanos e nulos
            if (!word.isEmpty() && !Character.isDigit(word.charAt(0))
                    && !word.equals("true") && !word.equals("false") && !word.equals("null")) {

                listeners.computeIfAbsent(word, k -> new HashSet<>()).add(dependentNode);
            }
        }
    }
}