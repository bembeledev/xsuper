package com.dic.xsuper.lang.ui.tags.editor;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import org.fxmisc.richtext.CodeArea;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public class GutterFactory implements IntFunction<Node> {

    private final CodeArea codeArea;
    private IntFunction<Node> lineNumberNodeFactory;
    private final List<GutterProvider> providers = new ArrayList<>();

    public GutterFactory(CodeArea codeArea) {
        this.codeArea = codeArea;
    }

    public void setLineNumberNodeFactory(IntFunction<Node> factory) {
        this.lineNumberNodeFactory = factory;
    }

    public void registerProvider(GutterProvider provider) {
        this.providers.add(provider);
    }

    @Override
    public Node apply(int lineIndex) {
        HBox gutterContainer = new HBox();
        gutterContainer.setAlignment(Pos.CENTER_RIGHT);
        gutterContainer.getStyleClass().add("superui-gutter");
        gutterContainer.setSpacing(5.0);

        // 1. Processa plugins customizados (Ex: Quadrado de Cor)
        String lineText = codeArea.getParagraphs().size() > lineIndex ? codeArea.getParagraphs().get(lineIndex).getText() : "";
        for (GutterProvider provider : providers) {
            Node graphic = provider.createGraphic(lineIndex, lineText);
            if (graphic != null) {
                gutterContainer.getChildren().add(graphic);
            }
        }

        // 2. Adiciona o número da linha original do RichTextFX à direita
        if (lineNumberNodeFactory != null) {
            Node lineNumberNode = lineNumberNodeFactory.apply(lineIndex);
            if (lineNumberNode != null) {
                gutterContainer.getChildren().add(lineNumberNode);
            }
        }

        return gutterContainer;
    }
}