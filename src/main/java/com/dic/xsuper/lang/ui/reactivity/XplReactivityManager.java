package com.dic.xsuper.lang.ui.reactivity;

import com.dic.xsuper.lang.ui.SuperUiEngine;
import com.dic.xsuper.lang.ui.html.XplNode;

import java.util.*;

/**
 * O Sistema Nervoso Central de Reatividade de Grão Fino (Fine-Grained Reactivity).
 * Liga variáveis do XPL diretamente a nós específicos do DOM W3C.
 */
public class XplReactivityManager {

    private final SuperUiEngine engine;

    // O Cofre de Dependências: [Nome da Variável] -> [Lista de Nós afetados]
    // Usamos Set para evitar que o mesmo nó seja atualizado duas vezes pela mesma variável
    private final Map<String, Set<XplNode>> dependencies = new HashMap<>();

    public XplReactivityManager(SuperUiEngine engine) {
        this.engine = engine;
    }

    // =====================================================================
    // 1. REGISTAR DEPENDENCIA (Chamado pelo DomEvaluator)
    // =====================================================================
    /**
     * Ensina o motor que um nó precisa de ser atualizado se uma variável mudar.
     * Ex: trackDependency("isOpen", noDoModal);
     */
    public void trackDependency(String varName, XplNode node) {
        if (varName == null || varName.trim().isEmpty() || node == null) return;

        // Limpa a string de expressões complexas para apanhar só o nome base (opcional, pode ser melhorado)
        String cleanVarName = extractBaseVariable(varName);

        dependencies.computeIfAbsent(cleanVarName, k -> new HashSet<>()).add(node);
        System.out.println("[Reactivity] 🔗 Nó '" + node.tag + "' registado como dependente de '" + cleanVarName + "'");
    }

    // =====================================================================
    // 2. DISPARAR ATUALIZAÇÃO (Chamado pelo Interpretador XPL)
    // =====================================================================
    /**
     * Quando o programador faz `isOpen = false` no XPL, esta função é chamada.
     */
    public void notifyStateChange(String varName) {
        String cleanVarName = extractBaseVariable(varName);

        if (!dependencies.containsKey(cleanVarName)) return;

        Set<XplNode> affectedNodes = dependencies.get(cleanVarName);
        if (affectedNodes == null || affectedNodes.isEmpty()) return;

        System.out.println("[Reactivity] ⚡ Variável '" + cleanVarName + "' sofreu mutação. A atualizar " + affectedNodes.size() + " nó(s)...");

        for (XplNode targetNode : affectedNodes) {
            // ⭐ A MAGIA CIRÚRGICA VAI ACONTECER AQUI!
            // 1. Mandar o DomEvaluator reavaliar apenas ESTE nó (e os seus filhos)
            // 2. Mandar o JavaFxRenderer fazer patch parcial no ecrã

            // Exemplo de como vamos ligar isto à SuperUiEngine:
            // engine.processPartialReactivityUpdate(targetNode);
        }
    }

    // =====================================================================
    // 3. LIMPEZA (Chamado na mudança de página/rota)
    // =====================================================================
    public void clearDependencies() {
        dependencies.clear();
    }

    // =====================================================================
    // 🛠️ UTILS
    // =====================================================================
    /**
     * Extrai a variável principal de uma expressão (Ex: "user.nome" -> "user", "lista[0]" -> "lista")
     */
    private String extractBaseVariable(String expression) {
        String clean = expression.trim();
        // Remove operadores, parênteses e apanha a raiz (Isto pode ser afinado consoante a sintaxe do XPL)
        String[] parts = clean.split("[.\\[\\s=+\\-*/]");
        return parts.length > 0 ? parts[0] : clean;
    }
}