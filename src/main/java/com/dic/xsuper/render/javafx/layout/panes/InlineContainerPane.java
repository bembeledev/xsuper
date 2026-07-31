package com.dic.xsuper.render.javafx.layout.panes;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.dom.properties.cssunit.CssContext;
import javafx.scene.Node;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import java.util.List;
import java.util.Map;

public class InlineContainerPane extends TextFlow implements CustomLayoutPane {

    private final Map<String, String> style;

    public InlineContainerPane(Map<String, String> style) {
        this.style = style != null ? style : Map.of();
    }

    @Override
    public void populateChildren(List<NativeTag> children, CssContext context) {
        // 1. Processa e adiciona todos os filhos (Textos, Spans, Imagens Inline)
        for (NativeTag child : children) {
            Node fxChild = child.build();

            // O TextFlow ignora a maioria das margens, pois foca-se no fluxo de leitura,
            // mas aceita perfeitamente nós visuais.
            getChildren().add(fxChild);
        }

        // ⭐ 2. Suporte mágico para text-align (Esquerda, Centro, Direita, Justificado)
        String textAlign = style.getOrDefault("text-align", "left").toLowerCase().trim();
        switch (textAlign) {
            case "center" -> setTextAlignment(TextAlignment.CENTER);
            case "right" -> setTextAlignment(TextAlignment.RIGHT);
            case "justify" -> setTextAlignment(TextAlignment.JUSTIFY);
            default -> setTextAlignment(TextAlignment.LEFT);
        }

        // Espaçamento entre linhas (line-height)
        if (style.containsKey("line-height")) {
            try {
                double lh = Double.parseDouble(style.get("line-height").replace("px", "").trim());
                setLineSpacing(lh - 12); // Ajuste simples baseado no tamanho padrão da fonte
            } catch (Exception ignored) {}
        }
    }
}