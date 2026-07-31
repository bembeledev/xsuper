package com.dic.xsuper.render.javafx.tags.editor;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {

    private Pattern dynamicPattern;
    private List<String> groupClasses; // Guarda a relação entre o Grupo Regex e a Classe CSS

    // Compila as regras enviadas pelo programador via XPL
    public void compileRules(List<Map<String, Object>> rules) {
        if (rules == null || rules.isEmpty()) {
            dynamicPattern = null;
            return;
        }

        StringBuilder regexBuilder = new StringBuilder();
        groupClasses = new ArrayList<>();
        int groupIndex = 0;

        for (Map<String, Object> rule : rules) {
            // Ignora a regra se não tiver classe ou regex definido
            if (!rule.containsKey("classe") || !rule.containsKey("regex")) continue;

            String cssClass = String.valueOf(rule.get("classe"));
            String regex = String.valueOf(rule.get("regex"));

            // O Regex do Java exige que os nomes dos grupos sejam alfanuméricos
            String groupName = "GRP" + groupIndex;
            groupClasses.add(cssClass);

            if (regexBuilder.length() > 0) {
                regexBuilder.append("|");
            }

            // Cria o grupo nomeado: (?<GRP0>expressao)
            regexBuilder.append("(?<").append(groupName).append(">").append(regex).append(")");

            groupIndex++;
        }

        // Compila o motor de alta performance do Java
        if (regexBuilder.length() > 0) {
            dynamicPattern = Pattern.compile(regexBuilder.toString());
        } else {
            dynamicPattern = null;
        }
    }

    // Processa o texto e devolve a lista de estilos para o RichTextFX
    public StyleSpans<Collection<String>> computeHighlighting(String text) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        // Se não houver regras ou o texto estiver vazio, devolve tudo sem cor
        if (dynamicPattern == null || text.isEmpty()) {
            spansBuilder.add(Collections.emptyList(), text.length());
            return spansBuilder.create();
        }

        Matcher matcher = dynamicPattern.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            String styleClass = null;

            // Descobre qual dos grupos do Regex fez o "match"
            for (int i = 0; i < groupClasses.size(); i++) {
                if (matcher.group("GRP" + i) != null) {
                    styleClass = groupClasses.get(i);
                    break;
                }
            }

            // Adiciona o espaço vazio (texto normal) entre a última palavra colorida e a atual
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastEnd);

            // Aplica a cor à palavra encontrada
            if (styleClass != null) {
                spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            } else {
                spansBuilder.add(Collections.emptyList(), matcher.end() - matcher.start());
            }

            lastEnd = matcher.end();
        }

        // Adiciona o restante texto normal no final
        spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);
        return spansBuilder.create();
    }
}