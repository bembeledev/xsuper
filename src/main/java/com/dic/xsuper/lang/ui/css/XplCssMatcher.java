package com.dic.xsuper.lang.ui.css;

import com.dic.xsuper.lang.ui.html.XplNode;
import java.util.Map;

/**
 * Motor que percorre a árvore de UI e aplica as regras CSS achatadas e resolvidas.
 */
public class XplCssMatcher {

    /**
     * Aplica o CSS a toda a árvore de UI recursivamente.
     */
    public static void applyStyles(XplNode uiRoot, XplNode flatCssAst, XplCssResolver resolver) {
        if (uiRoot == null || flatCssAst == null) return;

        // 1. Aplica estilos ao nó atual
        applyToNode(uiRoot, flatCssAst, resolver);

        // 2. Desce na árvore de UI (recursão)
        for (XplNode uiChild : uiRoot.children) {
            applyStyles(uiChild, flatCssAst, resolver);
        }
    }

    /**
     * Fase Final da Cascata: Lê o atributo 'style' inline do nó (ex: style="color: red; margin: 10;")
     * e sobrescreve as propriedades globais sem destruir as restantes.
     */
    public static void applyInlineStyles(XplNode uiRoot, XplCssResolver resolver) {
        if (uiRoot == null) return;

        // 1. Verifica se o nó tem CSS inline escrito pelo utilizador
        if (uiRoot.attributes.containsKey("style")) {
            String inlineStyleRaw = uiRoot.attributes.get("style").toString();

            // Separa as regras por ponto-e-vírgula (;)
            String[] declarations = inlineStyleRaw.split(";");
            for (String dec : declarations) {
                if (dec.trim().isEmpty()) continue;

                // Separa a propriedade do valor (ex: "color: red")
                String[] parts = dec.split(":", 2);
                if (parts.length == 2) {
                    String propName = parts[0].trim();
                    String rawValue = parts[1].trim();

                    // ⭐ Bónus: Se o utilizador usar var() no inline, o Resolver limpa!
                    String finalValue = rawValue;
                    if (rawValue.contains("var(")) {
                        finalValue = resolveInlineVar(rawValue, resolver);
                    }

                    // Sobrescreve/Adiciona ao mapa final do nó
                    uiRoot.style.put(propName, finalValue);
                }
            }
        }

        // 2. Desce na árvore recursivamente
        for (XplNode child : uiRoot.children) {
            applyInlineStyles(child, resolver);
        }
    }

    // Auxiliar para resolver variáveis caso sejam usadas no CSS inline
    private static String resolveInlineVar(String rawValue, XplCssResolver resolver) {
        // Extrai o nome da variável de "var(--nome)"
        int start = rawValue.indexOf("var(") + 4;
        int end = rawValue.indexOf(")", start);
        if (start > 3 && end > start) {
            String varContent = rawValue.substring(start, end);
            String[] varParts = varContent.split(",");
            String varName = varParts[0].trim();

            if (resolver.getRootVariables().containsKey(varName)) {
                return rawValue.replace("var(" + varContent + ")", resolver.getRootVariables().get(varName));
            } else if (varParts.length > 1) {
                return rawValue.replace("var(" + varContent + ")", varParts[1].trim()); // Fallback
            }
        }
        return rawValue;
    }

    private static void applyToNode(XplNode uiNode, XplNode flatCssAst, XplCssResolver resolver) {
        for (XplNode ruleNode : flatCssAst.children) {

            // Tratamento de Media Queries (se a janela do JavaFX corresponder, validamos as regras internas)
            if ("@media".equals(ruleNode.tag)) {
                continue;
            }

            // Tratamento de Regras normais
            if ("rule".equals(ruleNode.tag)) {
                matchAndInject(uiNode, ruleNode, resolver);
            }
        }
    }

    private static void matchAndInject(XplNode uiNode, XplNode ruleNode, XplCssResolver resolver) {
        String selector = ruleNode.attributes.get("selector").toString();

        // Se o seletor casar com o nosso nó de UI
        if (matchesSelector(uiNode, selector)) {
            // Injeta todas as propriedades no mapa "style" do nó de UI
            for (XplNode prop : ruleNode.children) {
                if ("property".equals(prop.tag)) {
                    String propName = prop.attributes.get("name").toString();

                    // ⭐ LÊ TODOS OS VALORES (Múltiplos filhos fundidos num só)
                    StringBuilder finalValue = new StringBuilder();
                    for (XplNode childNode : prop.children) {
                        finalValue.append(resolver.resolveValueNode(childNode)).append(" ");
                    }

                    String resolvedStyle = finalValue.toString().trim();
                    if (!resolvedStyle.isEmpty()) {
                        appendStyle(uiNode, propName, resolvedStyle);
                    }
                }
            }
        }
    }

    /**
     * O cérebro do Matcher: Verifica se o nó de UI satisfaz o seletor CSS.
     */
    private static boolean matchesSelector(XplNode uiNode, String selector) {
        // Seletor universal
        if ("*".equals(selector)) return true;

        // Seletor de ID (ex: #meuHeader)
        if (selector.startsWith("#")) {
            String uiId = uiNode.id != null ? uiNode.id : "";
            return selector.substring(1).equals(uiId);
        }

        // Seletor de Classe (ex: .button)
        if (selector.startsWith(".")) {
            String uiClass = uiNode.className != null ? uiNode.className : "";
            String targetClass = selector.substring(1);

            // Suporta múltiplas classes (ex: class="button danger")
            for (String c : uiClass.split("\\s+")) {
                if (c.equals(targetClass)) return true;
            }
            return false;
        }

        // Seletor de Tag (ex: div, button)
        return selector.equalsIgnoreCase(uiNode.tag);
    }

    /**
     * Auxiliar para concatenar no atributo nativo 'style' do teu XplNode
     */
    private static void appendStyle(XplNode uiNode, String propName, String value) {
        // Se já tivermos um mapa "style" isolado no teu nó, usa-o.
        // Assumindo que gravas no mapa nativo do nó:
        uiNode.style.put(propName, value);
    }
}