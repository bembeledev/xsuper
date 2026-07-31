package com.dic.xsuper.tests.ui;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.render.javafx.tags.TagFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

public class TestUITableRender extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        XplNode rootTableTree = createTableTree();

        NativeTag rootTag = TagFactory.create(rootTableTree);
        javafx.scene.Node fxRoot = rootTag.build();

        ScrollPane scrollPane = new ScrollPane(fxRoot);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(scrollPane, 800, 500);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Xplorer Engine - Laboratório de Tabelas");
        primaryStage.show();
    }

    private static XplNode createTableTree() {
        // Container principal da view de teste
        XplNode rootWrapper = new XplNode("div");
        rootWrapper.attributes.put("style", "padding: 30px; background: #f8fafc; width: 100vw; height: 100vh;");

        // Tabela principal
        XplNode table = new XplNode("table");
        table.attributes.put("style", "width: 100%; border-spacing: 5px; background: #ecf0f1; padding: 10px;");

        // --- THEAD ---
        XplNode thead = new XplNode("thead");
        XplNode trHead = new XplNode("tr");

        XplNode th1 = new XplNode("th");
        th1.textContent = "ID do Cargo";
        th1.attributes.put("style", "background: #2980b9; color: white; padding: 12px;");

        XplNode th2 = new XplNode("th");
        th2.textContent = "Departamento";
        th2.attributes.put("style", "background: #2980b9; color: white; padding: 12px;");

        XplNode th3 = new XplNode("th");
        th3.textContent = "Vagas";
        th3.attributes.put("style", "background: #2980b9; color: white; padding: 12px;");

        trHead.addChild(th1);
        trHead.addChild(th2);
        trHead.addChild(th3);
        thead.addChild(trHead);
        table.addChild(thead);

        // --- TBODY ---
        XplNode tbody = new XplNode("tbody");

        // Linha 1
        XplNode tr1 = new XplNode("tr");
        XplNode td1_1 = new XplNode("td");
        td1_1.textContent = "#001";
        td1_1.attributes.put("style", "background: white; padding: 10px;");

        XplNode td1_2 = new XplNode("td");
        td1_2.textContent = "Engenharia de Software";
        td1_2.attributes.put("style", "background: white; padding: 10px;");

        XplNode td1_3 = new XplNode("td");
        td1_3.textContent = "3";
        td1_3.attributes.put("style", "background: white; padding: 10px; color: green; font-weight: bold;");

        tr1.addChild(td1_1);
        tr1.addChild(td1_2);
        tr1.addChild(td1_3);

        // Linha 2
        XplNode tr2 = new XplNode("tr");
        XplNode td2_1 = new XplNode("td");
        td2_1.textContent = "#002";
        td2_1.attributes.put("style", "background: white; padding: 10px;");

        XplNode td2_2 = new XplNode("td");
        td2_2.textContent = "Suporte Técnico (TI)";
        td2_2.attributes.put("style", "background: white; padding: 10px;");

        XplNode td2_3 = new XplNode("td");
        td2_3.textContent = "0";
        td2_3.attributes.put("style", "background: white; padding: 10px; color: red;");

        tr2.addChild(td2_1);
        tr2.addChild(td2_2);
        tr2.addChild(td2_3);

        tbody.addChild(tr1);
        tbody.addChild(tr2);
        table.addChild(tbody);

        rootWrapper.addChild(table);
        return rootWrapper;
    }
}