package com.dic.xsuper.dom.css;

import com.dic.xsuper.dom.node.XplNode;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsável por gerir o escopo global do CSS (:root) e resolver valores dinâmicos
 * como funções var() e os seus fallbacks antes de aplicar ao JavaFX.
 */
public class XplCssResolver {

    private final Map<String, String> rootVariables = new HashMap<>();

    /**
     * 1. Extrai as variáveis globais do seletor :root para o mapa interno.
     */
    public void extractRootVariables(XplNode flatAst) {
        for (XplNode rule : flatAst.children) {
            if ("rule".equals(rule.tag) && ":root".equals(rule.attributes.get("selector"))) {
                for (XplNode child : rule.children) {
                    if ("variable".equals(child.tag)) {
                        String varName = child.attributes.get("name").toString();
                        // As variáveis no :root geralmente têm um value expression ou literal como filho
                        if (!child.children.isEmpty()) {
                            XplNode valNode = child.children.getFirst();
                            rootVariables.put(varName, valNode.attributes.get("data").toString());
                        }
                    }
                }
            }
        }
    }

    /**
     * 2. Transforma um nó de valor (literal, expression, ou var) numa string final.
     */
    public String resolveValueNode(XplNode valueNode) {
        if (valueNode == null) return "";

        // Se for um literal simples ou número (ex: "red", 10px)
        if ("literal".equals(valueNode.attributes.get("type")) || "number".equals(valueNode.attributes.get("type"))) {
            Object data = valueNode.attributes.get("data");
            if (data == null) data = valueNode.attributes.get("value");
            String str = data != null ? data.toString() : "";

            // Se contiver var(, tentar resolver
            if (str.contains("var(")) {
                if (str.startsWith("\"") && str.endsWith("\"")) {
                    str = str.substring(1, str.length() - 1);
                }

                // ⭐ A MESMA REGEX AQUI!
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("var\\(\\s*(--[^\\s)]+)\\s*\\)");
                java.util.regex.Matcher m = p.matcher(str);
                StringBuilder sb = new StringBuilder();
                while (m.find()) {
                    String varName = m.group(1);
                    String resolved = rootVariables.getOrDefault(varName, "");
                    m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(resolved));
                }
                m.appendTail(sb);
                return sb.toString();
            }
            return str;
        }

        // Se for uma expressão composta (ex: calc(100% - 20px) ou #3b82f6)
        if ("expression".equals(valueNode.attributes.get("type"))) {
            return valueNode.attributes.get("data").toString();
        }

        // ⭐ A MAGIA ACONTECE AQUI: Se for a função var(--nome, fallback)
        if ("var".equals(valueNode.attributes.get("type"))) {
            String varName = valueNode.attributes.get("name").toString();

            // Procura a variável no nosso mapa global carregado do :root
            if (rootVariables.containsKey(varName)) {
                return rootVariables.get(varName);
            }

            // Se a variável não existir, procura o nó de fallback na árvore
            for (XplNode child : valueNode.children) {
                if ("fallback".equals(child.attributes.get("type"))) {
                    return child.attributes.get("value").toString();
                }
            }

            // Se não houver fallback, devolve vazio (comportamento padrão do W3C)
            return "";
        }

        return "";
    }

    // Acesso ao mapa para debug
    public Map<String, String> getRootVariables() {
        return rootVariables;
    }
}