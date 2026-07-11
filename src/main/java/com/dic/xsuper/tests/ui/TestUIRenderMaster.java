package com.dic.xsuper.tests.ui;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.tags.TagFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class TestUIRenderMaster extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Constrói a árvore de nós suprema do Xplorer Engine
        XplNode root = createMasterTree();

        // Converte para a árvore de tags nativas polimórficas
        NativeTag rootTag = TagFactory.create(root);
        javafx.scene.Node fxRoot = rootTag.build();

        // Apresenta na janela principal do JavaFX
        StackPane pane = new StackPane(fxRoot);
        Scene scene = new Scene(pane, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Xplorer Rendering Engine - Ultimate Master Test View");
        primaryStage.show();
    }

    private static XplNode createMasterTree() {
        // 1. Raiz com display: border (BorderPane de 5 Regiões)
        XplNode root = new XplNode("div");
        root.attributes.put("style", "display: border; width: 100vw; height: 100vh;");

        // --- ⭐ HEADER (region: top, display: flex) ---
        XplNode header = new XplNode("header");
        header.attributes.put("style", "region: top; display: flex; justify-content: space-between; align-items: center; padding: 15px 25px; background: #667eea;");

        XplNode h2 = new XplNode("h2");
        h2.textContent = "Xplorer Rendering Engine - Ultimate Master View";
        header.addChild(h2);

        XplNode badgeDiv = new XplNode("div");
        badgeDiv.attributes.put("style", "display: inline-block; padding: 6px 12px; border-radius: 8px;");
        XplNode badgeSpan = new XplNode("span");
        badgeSpan.textContent = "v2.6 Live";
        badgeDiv.addChild(badgeSpan);
        header.addChild(badgeDiv);
        root.addChild(header);

        // --- ⭐ SIDEBAR (region: left, display: flex column) ---
        XplNode aside = new XplNode("aside");
        aside.attributes.put("style", "region: left; display: flex; flex-direction: column; gap: 12px; padding: 20px; width: 240px; background: #f8fafc;");

        XplNode sideTitle = new XplNode("h3");
        sideTitle.textContent = "Painel de Controlo";
        aside.addChild(sideTitle);

        XplNode ul = new XplNode("ul");
        ul.attributes.put("style", "display: block; padding: 0;");

        XplNode li1 = new XplNode("li");
        li1.textContent = "• Dashboard Geral";
        li1.attributes.put("style", "display: block; margin-top: 8px;");

        XplNode li2 = new XplNode("li");
        li2.textContent = "• Motores de Layout";
        li2.attributes.put("style", "display: block; margin-top: 8px;");

        XplNode li3 = new XplNode("li");
        li3.textContent = "Este item está invisível (display: none)";
        li3.attributes.put("style", "display: none;");

        ul.addChild(li1);
        ul.addChild(li2);
        ul.addChild(li3);
        aside.addChild(ul);
        root.addChild(aside);

        // --- ⭐ STAGE CENTRAL (region: center, display: block) ---
        XplNode main = new XplNode("main");
        main.attributes.put("style", "region: center; display: block; padding: 25px; background: #edf2f7;");

        // A. Grid system (display: grid, com colunas fracionárias 1fr 2fr 1fr)
        XplNode gridDiv = new XplNode("div");
        gridDiv.attributes.put("style", "display: grid; grid-template-columns: 1fr 2fr 1fr; gap: 15px; margin-bottom: 25px;");

        String[] metricas = {"Métrica A: 1.2M req/s", "Métrica B: Latência 0.4ms", "Métrica C: Status OK"};
        for (String m : metricas) {
            XplNode card = new XplNode("div");
            card.attributes.put("style", "background: #ffffff; padding: 15px; border-radius: 8px;");
            card.textContent = m;
            gridDiv.addChild(card);
        }
        main.addChild(gridDiv);

        // B. Flow wrapper (display: flex, flex-wrap: wrap)
        XplNode flowDiv = new XplNode("div");
        flowDiv.attributes.put("style", "display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 25px; padding: 15px; background: white; border-radius: 10px;");

        String[] techList = {"Angular", "React", "Rust", "JavaFX", "Python", "Spring Boot", "Sublime Text"};
        for (String tech : techList) {
            XplNode btn = new XplNode("button");
            btn.textContent = tech;
            flowDiv.addChild(btn);
        }
        main.addChild(flowDiv);

        // C. Contents / Inline flow (display: contents & display: inline)
        XplNode contentsDiv = new XplNode("div");
        contentsDiv.attributes.put("style", "display: contents;");
        XplNode p = new XplNode("p");
        p.attributes.put("style", "display: inline; text-align: justify; margin-bottom: 25px;");
        p.textContent = "Fluxo contínuo demonstrando TextFlow com elementos integrados via display: contents.";
        contentsDiv.addChild(p);
        main.addChild(contentsDiv);

        // D. Table Engine (display: table)
        XplNode table = new XplNode("table");
        table.attributes.put("style", "display: table; border-spacing: 6px; margin-bottom: 25px; width: 100%;");

        XplNode tr1 = new XplNode("tr");
        tr1.attributes.put("style", "display: table-row;");
        XplNode td1 = new XplNode("td"); td1.textContent = "Subsistema";
        XplNode td2 = new XplNode("td"); td2.textContent = "Modo W3C";
        tr1.addChild(td1); tr1.addChild(td2);

        XplNode tr2 = new XplNode("tr");
        tr2.attributes.put("style", "display: table-row;");
        XplNode td3 = new XplNode("td"); td3.textContent = "LayoutEngine";
        XplNode td4 = new XplNode("td"); td4.textContent = "Polimórfico";
        tr2.addChild(td3); tr2.addChild(td4);

        table.addChild(tr1);
        table.addChild(tr2);
        main.addChild(table);

        // E. Photoshop Layer Canvas (display: layer)
        XplNode layerDiv = new XplNode("div");
        layerDiv.attributes.put("style", "display: layer; width: 100%; height: 140px; background: #1a202c; border-radius: 10px; padding: 10px;");

        XplNode layer1 = new XplNode("div");
        layer1.attributes.put("style", "pos: top-left; background: #e53e3e; padding: 6px; height:20px;width:200px;");
        layer1.textContent = "Camada Fixa (Top-Left)";

        XplNode layer2 = new XplNode("div");
        layer2.attributes.put("style", "pos: center; x: 40px; y: -15px; background: #3182ce; padding: 6px;");
        layer2.textContent = "Camada Flutuante (Center + Offset)";

        layerDiv.addChild(layer1);
        layerDiv.addChild(layer2);
        main.addChild(layerDiv);

        root.addChild(main);
        return root;
    }
}