package com.dic.xsuper.lang.ui.css;

import com.dic.xsuper.lang.ui.html.XplNode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor CSS Centralizado (CSSOM).
 * Armazena, compila variáveis e aplica a cascata de forma isolada e limpa.
 */
public class XplStyleEngine {

    // O Mapa principal: "Seletor" -> { "propriedade": "valor" }
    private final Map<String, Map<String, String>> rules = new LinkedHashMap<>();

    // O Mapa isolado para as variáveis do :root
    private final Map<String, String> rootVariables = new HashMap<>();

    /**
     * Passo 1: Adiciona uma regra bruta ao motor.
     */
    public void addRule(String selector, String property, String value) {
        String cleanSelector = selector.trim();
        String cleanProperty = property.trim().toLowerCase();
        String cleanValue = value.trim();

        // Se for o :root, interceptamos as variáveis para o nosso mapa interno
        if (":root".equals(cleanSelector)) {
            if (cleanProperty.startsWith("--")) {
                rootVariables.put(cleanProperty, cleanValue);
            }
            return;
        }

        // Adiciona a regra ao mapa de seletores
        rules.computeIfAbsent(cleanSelector, k -> new LinkedHashMap<>()).put(cleanProperty, cleanValue);
    }

    /**
     * Passo 2: Compila o mapa! Substitui todos os "var(--nome)" pelo valor real.
     * Isto evita poluir o ambiente global do XPL.
     */
    public void compileVariables() {
        Pattern varPattern = Pattern.compile("var\\((--[^)]+)\\)");

        for (Map<String, String> declarations : rules.values()) {
            for (Map.Entry<String, String> entry : declarations.entrySet()) {
                String value = entry.getValue();

                // Se o valor contiver 'var(', vamos substituir
                if (value.contains("var(")) {
                    Matcher matcher = varPattern.matcher(value);
                    StringBuilder compiledValue = new StringBuilder();

                    while (matcher.find()) {
                        String varName = matcher.group(1);
                        // Substitui pela variável isolada, ou mantém transparente se não existir
                        String resolvedValue = rootVariables.getOrDefault(varName, "transparent");
                        matcher.appendReplacement(compiledValue, resolvedValue);
                    }
                    matcher.appendTail(compiledValue);

                    // Atualiza a propriedade com o valor compilado (ex: #3b82f6)
                    declarations.put(entry.getKey(), compiledValue.toString());
                }
            }
        }
    }

    /**
     * Passo 3: Aplica o CSS a toda a árvore DOM, respeitando a Cascata (W3C Standard).
     */
    public void applyToTree(XplNode rootNode) {
        if (rootNode == null) return;

        applyToNode(rootNode);

        for (XplNode child : rootNode.children) {
            applyToTree(child);
        }
    }

    /**
     * Motor de Matching (Cascata) para um único nó.
     * Ordem de precedência: * -> tag -> .class -> #id -> inline style
     */
    private void applyToNode(XplNode node) {
        // 1. O mapa temporário para calcular a cascata deste nó
        Map<String, String> computedStyles = new LinkedHashMap<>();

        // Nível 1: Seletor Universal (*)
        if (rules.containsKey("*")) {
            computedStyles.putAll(rules.get("*"));
        }

        // Nível 2: Seletor de Tag (ex: div, h1)
        if (node.tag != null && rules.containsKey(node.tag.toLowerCase())) {
            computedStyles.putAll(rules.get(node.tag.toLowerCase()));
        }

        // Nível 3: Seletor de Classe (ex: .dashboard-card)
        if (node.className != null && !node.className.isEmpty()) {
            String[] classes = node.className.split("\\s+");
            for (String cls : classes) {
                String classSelector = "." + cls;
                if (rules.containsKey(classSelector)) {
                    computedStyles.putAll(rules.get(classSelector));
                }
            }
        }

        // Nível 4: Seletor de ID (ex: #main-panel)
        if (node.id != null && !node.id.isEmpty()) {
            String idSelector = "#" + node.id;
            if (rules.containsKey(idSelector)) {
                computedStyles.putAll(rules.get(idSelector));
            }
        }

        // Nível 5: Inline Styles (ex: <div style="color: red;">) têm precedência absoluta!
        if (node.attributes.containsKey("style")) {
            String rawInline = node.attributes.get("style").toString();
            String[] declarations = rawStyleToArray(rawInline);
            for (String dec : declarations) {
                String[] kv = dec.split(":", 2);
                if (kv.length == 2) {
                    computedStyles.put(kv[0].trim().toLowerCase(), kv[1].trim());
                }
            }
        }

        // ⭐ A MAGIA FINAL: Converte o mapa computado diretamente para o atributo style
        if (!computedStyles.isEmpty()) {
            StringBuilder finalStyleString = new StringBuilder();
            for (Map.Entry<String, String> entry : computedStyles.entrySet()) {
                finalStyleString.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
            }

            // Injeta o CSS limpo e compilado (sem variáveis) de volta no nó
            node.attributes.put("style", finalStyleString.toString().trim());

            // Atualiza também o mapa interno (opcional, para conveniência)
            node.style.clear();
            node.style.putAll(computedStyles);
        }
    }


    private String[] rawStyleToArray(String rawStyle) {
        return rawStyle.split(";");
    }
}