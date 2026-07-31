package com.dic.xsuper.render.javafx.tags.customs;

import javafx.geometry.VPos;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class W3CTextBlock extends Region {

    private final String rawText;
    private final Map<String, String> cssStyles;

    // Lista de palavras individuais desenhadas na GPU
    private final List<Text> wordNodes = new ArrayList<>();

    // Propriedades W3C
    private double w3cLineHeight = 1.2;
    private String w3cTextAlign = "left";
    private boolean w3cNoWrap = false;
    private double spaceWidth = 4.0;

    public W3CTextBlock(String text, Map<String, String> cssStyles) {
        this.rawText = text == null ? "" : text;
        this.cssStyles = cssStyles != null ? cssStyles : Map.of();

        parseW3CProperties();
        buildWordNodes();
    }

    /**
     * Extrai e traduz o CSS W3C para a matemática do bloco
     */
    private void parseW3CProperties() {
        this.w3cTextAlign = cssStyles.getOrDefault("text-align", "left").toLowerCase();

        String whiteSpace = cssStyles.getOrDefault("white-space", "normal").toLowerCase();
        this.w3cNoWrap = whiteSpace.equals("nowrap");

        String lineHeightStr = cssStyles.getOrDefault("line-height", "1.5");
        try {
            if (lineHeightStr.contains("px")) {
                // Simplificação: podes converter pixels reais depois
                this.w3cLineHeight = 1.5;
            } else {
                this.w3cLineHeight = Double.parseDouble(lineHeightStr);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Parte a string nas suas palavras e injeta os estilos W3C (Fonte, Cor, etc.)
     */
    private void buildWordNodes() {
        getChildren().clear();
        wordNodes.clear();

        // 1. Estilos de Fonte W3C
        String fontFamily = cssStyles.getOrDefault("font-family", "System").split(",")[0].replace("\"", "");

        double fontSize = 16.0;
        try {
            String fSize = cssStyles.getOrDefault("font-size", "16px").replaceAll("[^0-9.]", "");
            fontSize = Double.parseDouble(fSize);
        } catch (Exception ignored) {}

        String fontWeightStr = cssStyles.getOrDefault("font-weight", "normal");
        FontWeight weight = (fontWeightStr.equals("bold") || fontWeightStr.equals("700")) ? FontWeight.BOLD : FontWeight.NORMAL;

        String fontStyleStr = cssStyles.getOrDefault("font-style", "normal");
        FontPosture posture = fontStyleStr.equals("italic") ? FontPosture.ITALIC : FontPosture.REGULAR;

        Font font = Font.font(fontFamily, weight, posture, fontSize);

        // 2. Cor (Fill)
        String color = cssStyles.getOrDefault("color", "#1e293b"); // Fallback escuro

        // 3. Decorações
        String decoration = cssStyles.getOrDefault("text-decoration", "none");
        boolean isUnderline = decoration.contains("underline");
        boolean isStrike = decoration.contains("line-through");

        // 4. Medição de um "Espaço" em branco nesta fonte
        Text spaceNode = new Text(" ");
        spaceNode.setFont(font);
        this.spaceWidth = spaceNode.getLayoutBounds().getWidth();

        // 5. Instanciação matemática das palavras
        String[] words = rawText.split(" ");
        for (String w : words) {
            if (w.isEmpty()) continue;

            Text wordNode = new Text(w);
            wordNode.setFont(font);
            wordNode.setStyle("-fx-fill: " + color + ";");
            wordNode.setTextOrigin(VPos.TOP);
            wordNode.setUnderline(isUnderline);
            wordNode.setStrikethrough(isStrike);

            wordNodes.add(wordNode);
            getChildren().add(wordNode); // Adiciona ao Region
        }
    }

    /**
     * ⭐ O CORAÇÃO DO MOTOR W3C:
     * Aqui tu dizes exatamente onde cada palavra vai ficar. Adeus limites estranhos do JavaFX!
     */
    @Override
    protected void layoutChildren() {
        if (wordNodes.isEmpty()) return;

        double layoutWidth = getWidth();
        if (layoutWidth <= 0) layoutWidth = 800; // Proteção contra janelas colapsadas

        double currentX = 0;
        double currentY = 0;
        double maxLineHeight = 0;

        List<Text> currentLine = new ArrayList<>();
        double currentLineWidth = 0;

        for (Text word : wordNodes) {
            double wordWidth = word.getLayoutBounds().getWidth();
            double wordHeight = word.getLayoutBounds().getHeight();

            // Se for NOWRAP, a palavra entra na linha indefinidamente
            // Se tiver WRAP, e a palavra ultrapassar a largura, quebramos a linha!
            if (!w3cNoWrap && currentX + wordWidth > layoutWidth && !currentLine.isEmpty()) {
                // Alinha a linha que acabou de ser processada
                alignLine(currentLine, currentLineWidth, layoutWidth, currentY);

                // Salta de linha
                currentX = 0;
                currentY += maxLineHeight * w3cLineHeight;
                currentLine.clear();
                currentLineWidth = 0;
                maxLineHeight = 0;
            }

            // Posiciona na linha atual
            currentLine.add(word);
            currentLineWidth += wordWidth + spaceWidth;
            currentX += wordWidth + spaceWidth;
            maxLineHeight = Math.max(maxLineHeight, wordHeight);
        }

        // Alinha a última linha
        if (!currentLine.isEmpty()) {
            // A última linha de um parágrafo não costuma justificar, logo alinhamos normal
            alignLine(currentLine, currentLineWidth, layoutWidth, currentY);
        }
    }

    /**
     * Resolve o text-align (Left, Center, Right, Justify)
     */
    private void alignLine(List<Text> lineWords, double lineWidth, double layoutWidth, double yPos) {
        // Removemos o último espaço fantasma para ter a largura real da linha
        double realLineWidth = lineWidth - spaceWidth;
        double startX = 0;
        double currentX = 0;

        // Calcula o offset inicial com base no alinhamento W3C
        switch (w3cTextAlign) {
            case "center":
                startX = Math.max(0, (layoutWidth - realLineWidth) / 2);
                break;
            case "right":
                startX = Math.max(0, layoutWidth - realLineWidth);
                break;
            case "justify":
                // Justify só faz sentido se a linha for mais ou menos longa e não for a última
                startX = 0;
                break;
            default: // left
                startX = 0;
                break;
        }

        currentX = startX;

        // Espaçamento dinâmico para o 'justify'
        double extraSpacePerWord = 0;
        if (w3cTextAlign.equals("justify") && lineWords.size() > 1 && realLineWidth < layoutWidth * 0.9) {
            extraSpacePerWord = (layoutWidth - realLineWidth) / (lineWords.size() - 1);
        }

        for (Text word : lineWords) {
            word.relocate(currentX, yPos);
            currentX += word.getLayoutBounds().getWidth() + spaceWidth + extraSpacePerWord;
        }
    }

    /**
     * O LayoutEngine (FlexColumn/Row) do JavaFX vai chamar isto para perguntar:
     * "Qual é a altura que precisas para este texto?"
     */
    @Override
    protected double computePrefHeight(double width) {
        if (wordNodes.isEmpty()) return 0;

        double currentX = 0;
        double currentY = 0;
        double maxLineHeight = 0;

        for (Text word : wordNodes) {
            double wordWidth = word.getLayoutBounds().getWidth();
            double wordHeight = word.getLayoutBounds().getHeight();

            if (!w3cNoWrap && currentX + wordWidth > width && currentX > 0) {
                currentX = 0;
                currentY += maxLineHeight * w3cLineHeight;
                maxLineHeight = 0;
            }

            currentX += wordWidth + spaceWidth;
            maxLineHeight = Math.max(maxLineHeight, wordHeight);
        }

        return currentY + (maxLineHeight * w3cLineHeight);
    }
}