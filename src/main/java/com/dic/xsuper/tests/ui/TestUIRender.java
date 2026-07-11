package com.dic.xsuper.tests.ui;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.tags.TagFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class TestUIRender extends Application {

    public static void main(String[] args) {
        // Teste lógico (sem UI)
        testStyleResolution();

        // Teste visual (JavaFX)
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Cria uma árvore XplNode simulada
        XplNode root = createSampleTree();

        // Converte para NativeTag
        NativeTag rootTag = TagFactory.create(root);
        javafx.scene.Node fxRoot = rootTag.build();

        // Mostra numa janela
        StackPane pane = new StackPane(fxRoot);
        Scene scene = new Scene(pane, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Teste UI - HTML para JavaFX");
        primaryStage.show();
    }

    private static void testStyleResolution() {
        System.out.println("--- Teste de Resolução de Estilos ---");

        // Simula um nó div com estilo inline
        XplNode node = new XplNode("div");
        node.id = "testDiv";
        node.className = "container";
        node.attributes.put("style", "padding: 20px; background: #f0f0f0; border: 2px solid #333; border-radius: 10px;");
        node.textContent = "Olá mundo!";

        NativeTag tag = TagFactory.create(node);
        // Acessamos os resolvedStyles via reflexão ou método público (adicionar getter)
        // Como exemplo, usamos a instância diretamente (assumindo que os campos são públicos ou têm getters)
        // Vamos imprimir as propriedades
        System.out.println("Tag criada: " + tag.getClass().getSimpleName());
        System.out.println("Padding: " + tag.getResolvedStyles().padding);  // Método fictício
        System.out.println("Background: " + tag.getResolvedStyles().margin);
        System.out.println("Border: " + tag.getResolvedStyles().border);
    }

    private static XplNode createSampleTree() {
        XplNode root = new XplNode("div");
        root.attributes.put("style", "padding: 20px; display: flex; gap: 10px; border: 2px solid #ff0000;");

        XplNode child1 = new XplNode("button");
        child1.attributes.put("style", "padding: 10px; background: #007bff; color: white; border-radius: 5px;");
        child1.textContent = "Clique";

        XplNode child2 = new XplNode("input");
        child2.attributes.put("style", "padding: 1px; border: 6px solid #ccc;");
        child2.attributes.put("placeholder", "Digite...");

        root.addChild(child2);
        root.addChild(child1);
        return root;
    }


}