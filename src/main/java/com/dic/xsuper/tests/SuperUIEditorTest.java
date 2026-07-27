package com.dic.xsuper.lang.ui.tags.editor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public class SuperUIEditorTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Instancia o CodeArea puro
        CodeArea editorArea = new CodeArea();
        editorArea.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: #cccccc; -fx-font-family: 'Consolas', monospace; -fx-font-size: 14px;");

        // 2. Configura a Fábrica de Margem Customizada
        GutterFactory gutterFactory = new GutterFactory(editorArea);
        gutterFactory.setLineNumberNodeFactory(LineNumberFactory.get(editorArea));

        // Regista o plugin do quadradinho de cor
        gutterFactory.registerProvider(new ColorPreviewProvider());

        editorArea.setParagraphGraphicFactory(gutterFactory);

        // 3. Adiciona o Listener de Digitação (Scanner em Background)
        editorArea.textProperty().addListener((obs, oldText, newText) -> {
            int currentLine = editorArea.getCurrentParagraph();
            MetadataScanner.scanLinesAsync(newText, currentLine);
        });

        // 4. Adiciona o Listener do Barramento de Eventos
        EditorEventBus.getInstance().getMetadataChangedEvent().addListener((obs, oldLine, newLine) -> {
            if (newLine != null && newLine >= 0 && newLine < editorArea.getParagraphs().size()) {
                // Força o recálculo apenas da margem da linha que tem a cor
                editorArea.recreateParagraphGraphic(newLine);
            }
        });

        // 5. Texto Inicial de Teste
        String sampleCode = """
                /* Teste de Cores do SuperUI */
                .button {
                    background-color: #ff0000;
                    color: #00ff00;
                    border: 1px solid #333333;
                }
                """;
        editorArea.replaceText(sampleCode);

        // 6. Monta a Janela com o VirtualizedScrollPane (para evitar Screen Shaking)
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(editorArea);
        Scene scene = new Scene(scrollPane, 800, 600);

        // Estilo rápido para ver o texto branco no teste (curando o text-fill)
        scene.getStylesheets().add("data:text/css;base64,LnN0eWxlZC10ZXh0LWFyZWEgLnRleHQgeyAtZngtZmlsbDogI2NjY2NjYzsgfQ==");

        primaryStage.setTitle("SuperUI - Teste de Gutter Annotations");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}