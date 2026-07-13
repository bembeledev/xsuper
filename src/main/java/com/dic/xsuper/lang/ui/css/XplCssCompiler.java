package com.dic.xsuper.lang.ui.css;

import com.dic.xsuper.lang.ui.html.XplNode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pega na AST de CSS já avaliada (com diretivas resolvidas) e
 * compila para um dicionário final de seletores, substituindo variáveis nativas.
 */
public class XplCssCompiler {

    public static Map<String, Map<String, String>> compileToDictionary(XplNode evaluatedAst) {
        // Usamos LinkedHashMap para PRESERVAR A ORDEM exata de escrita do programador!
        Map<String, Map<String, String>> dictionary = new LinkedHashMap<>();

        if (evaluatedAst == null || evaluatedAst.children == null) return dictionary;

        // ==============================================================
        // 1. CONSTRUÇÃO DO DICIONÁRIO E FUSÃO DE SELETORES
        // ==============================================================
        for (XplNode ruleNode : evaluatedAst.children) {
            if ("rule".equals(ruleNode.tag)) {
                String selector = ruleNode.attributes.get("selector").toString().trim();

                // Se o seletor já existe, pega nele. Se não, cria um novo mapa.
                dictionary.putIfAbsent(selector, new LinkedHashMap<>());
                Map<String, String> properties = dictionary.get(selector);

                // Lê as propriedades (adaptado à forma como o teu Parser guarda a AST)
                for (XplNode propNode : ruleNode.children) {
                    if ("property".equals(propNode.tag) || "variable".equals(propNode.tag)) {
                        String propName = propNode.attributes.get("name").toString().trim().toLowerCase();

                        // Extrai o valor do nó (ajusta conforme a tua estrutura exata, se for preciso)
                        String propValue = "";
                        if (propNode.attributes.containsKey("value")) {
                            propValue = propNode.attributes.get("value").toString().trim();
                        } else if (propNode.attributes.containsKey("data")) {
                            propValue = propNode.attributes.get("data").toString().trim();
                        }

                        properties.put(propName, propValue);
                    }
                }
            }
        }

        // ==============================================================
        // 2. RESOLUÇÃO DE VARIÁVEIS (A substituição dos var(--nome))
        // ==============================================================
        Map<String, String> rootVars = dictionary.getOrDefault(":root", new HashMap<>());
        Pattern varPattern = Pattern.compile("var\\((--[^)]+)\\)");

        for (Map<String, String> properties : dictionary.values()) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String value = entry.getValue();

                if (value.contains("var(")) {
                    Matcher matcher = varPattern.matcher(value);
                    StringBuilder compiledValue = new StringBuilder();

                    while (matcher.find()) {
                        String varName = matcher.group(1);
                        // Troca pelo valor real ou 'transparent' se não existir
                        String resolvedValue = rootVars.getOrDefault(varName, "transparent");
                        matcher.appendReplacement(compiledValue, resolvedValue);
                    }
                    matcher.appendTail(compiledValue);

                    // Atualiza a propriedade no mapa com a cor/valor real compilado
                    entry.setValue(compiledValue.toString());
                }
            }
        }

        return dictionary;
    }
}