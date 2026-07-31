package com.dic.xsuper.tests.ui;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.render.javafx.tags.TagFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

public class TestUITextRender extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        XplNode root = createTypographyTree();

        NativeTag rootTag = TagFactory.create(root);
        javafx.scene.Node fxRoot = rootTag.build();

        // Envolvemos num ScrollPane caso o texto ultrapasse a altura da janela
        ScrollPane scrollPane = new ScrollPane(fxRoot);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(scrollPane, 900, 750);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Xplorer Engine - Laboratório de Tipografia");
        primaryStage.show();
    }

    private static XplNode createTypographyTree() {
        // Container Principal (Usamos flex column para empilhar as secções)
        XplNode root = new XplNode("div");
        root.attributes.put("style", "display: flex; flex-direction: column; gap: 20px; padding: 40px; background: #fdfdfd;");

        // ==========================================
        // 1. TÍTULO PRINCIPAL COM EFEITOS ESPECIAIS
        // ==========================================
        XplNode masterTitle = new XplNode("h1");
        masterTitle.textContent = "Laboratório de Texto XPL";
        masterTitle.attributes.put("style",
                "text-align: center; " +
                        "color: #2c3e50; " +
                        "text-transform: uppercase; " +
                        "text-shadow: 3px 3px 6px #a0a0a0; " +
                        "text-decoration: underline; " +
                        "font-family: 'Segoe UI', sans-serif;"
        );
        root.addChild(masterTitle);

        // ==========================================
        // 2. TESTE SEMÂNTICO (H1 ao H6)
        // ==========================================
        XplNode headingsCard = createCard("Escala Semântica de Cabeçalhos (H1 - H6)");

        String[] hTags = {"h1", "h2", "h3", "h4", "h5", "h6"};
        for (String tag : hTags) {
            XplNode h = new XplNode(tag);
            h.textContent = "Isto é um " + tag.toUpperCase() + " nativo";
            h.attributes.put("style", "color: #34495e; margin: 0;"); // Removemos a margem extra para ver a escala limpa
            headingsCard.addChild(h);
        }
        root.addChild(headingsCard);

        // ==========================================
        // 3. PARÁGRAFO: JUSTIFICADO & LINE-HEIGHT
        // ==========================================
        XplNode pCard = createCard("Parágrafos: Justificado & Espaçamento (Line-Height)");

        XplNode p1 = new XplNode("p");
        p1.textContent = "Este é um teste de parágrafo longo para verificar a capacidade do motor de fazer wrap automático e justificar o texto. O line-height foi ajustado para 28px, o que deve criar um respiro visual elegante entre as linhas. Na web, o espaço entre as letras e as linhas é crucial para uma boa legibilidade e o Xplorer Engine agora domina essa arte com perfeição.";
        p1.attributes.put("style", "text-align: justify; line-height: 28px; font-size: 16px; color: #555555;");

        pCard.addChild(p1);
        root.addChild(pCard);

        // ==========================================
        // 4. TRANSFORMAÇÕES DE TEXTO AUTOMÁTICAS
        // ==========================================
        XplNode transformCard = createCard("Transformações de Texto (Text-Transform)");

        XplNode t1 = new XplNode("p");
        t1.textContent = "este texto foi escrito em minúsculas mas o motor aplicou capitalize.";
        t1.attributes.put("style", "text-transform: capitalize; font-size: 16px; font-weight: bold; color: #d35400;");

        XplNode t2 = new XplNode("p");
        t2.textContent = "ESTE TEXTO ESTAVA EM MAIÚSCULAS MAS O MOTOR PASSOU PARA LOWERCASE.";
        t2.attributes.put("style", "text-transform: lowercase; font-size: 16px; font-style: italic; color: #27ae60;");

        transformCard.addChild(t1);
        transformCard.addChild(t2);
        root.addChild(transformCard);

        // ==========================================
        // 5. DECORAÇÕES E ESTILOS INLINE (Span, B, I)
        // ==========================================
        XplNode inlineCard = createCard("Decorações e Pesos (Bold, Italic, Strike, Underline)");

        // Usamos um FlowPane/Wrap para meter as tags de texto lado a lado
        XplNode flowText = new XplNode("div");
        flowText.attributes.put("style", "display: flex; flex-wrap: wrap; gap: 15px; align-items: center;");

        XplNode strike = new XplNode("span");
        strike.textContent = "Preço Antigo: 200MT";
        strike.attributes.put("style", "text-decoration: line-through; color: #c0392b; font-size: 18px;");

        XplNode bold = new XplNode("strong");
        bold.textContent = "Novo Preço: 150MT";
        bold.attributes.put("style", "color: #16a085; font-size: 20px;"); // O <strong> já garante o bold

        XplNode underlineItalic = new XplNode("em");
        underlineItalic.textContent = "Oferta Limitada!";
        underlineItalic.attributes.put("style", "text-decoration: underline; color: #8e44ad; font-size: 16px;");

        flowText.addChild(strike);
        flowText.addChild(bold);
        flowText.addChild(underlineItalic);

        inlineCard.addChild(flowText);
        root.addChild(inlineCard);

        // ==========================================
        // 6. OPACIDADE E NO-WRAP
        // ==========================================
        XplNode effectsCard = createCard("Opacidade & White-Space (No-Wrap)");

        XplNode opText = new XplNode("label");
        opText.textContent = "Texto com 50% de opacidade (Fantasma)";
        opText.attributes.put("style", "opacity: 0.5; font-size: 18px; font-weight: bold; display: block; margin-bottom: 15px;");

        // Testar nowrap criando uma div pequena que forçaria o wrap, mas com nowrap ele corta
        XplNode noWrapContainer = new XplNode("div");
        noWrapContainer.attributes.put("style", "width: 200px; background: #ecf0f1; padding: 10px; border: 1px solid #bdc3c7;");

        XplNode noWrapText = new XplNode("p");
        noWrapText.textContent = "Este texto é muito longo para a caixa mas não vai quebrar linha por causa do nowrap.";
        noWrapText.attributes.put("style", "white-space: nowrap; color: #e67e22; margin: 0;");

        noWrapContainer.addChild(noWrapText);

        effectsCard.addChild(opText);
        effectsCard.addChild(noWrapContainer);
        root.addChild(effectsCard);

        return root;
    }

    // Método utilitário para criar "Cartões" visuais e organizar os testes
    private static XplNode createCard(String titleText) {
        XplNode card = new XplNode("div");
        card.attributes.put("style", "display: flex; flex-direction: column; gap: 10px; padding: 20px; background: #ffffff; border-radius: 8px; box-shadow: 0px 4px 10px rgba(0,0,0,0.05); border: 1px solid #eaebed;");

        XplNode title = new XplNode("h3");
        title.textContent = titleText;
        title.attributes.put("style", "color: #2980b9; margin-bottom: 10px; border-bottom: 2px solid #ecf0f1; padding-bottom: 5px;");

        card.addChild(title);
        return card;
    }
}