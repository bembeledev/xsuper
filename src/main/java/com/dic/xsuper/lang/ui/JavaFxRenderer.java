package com.dic.xsuper.lang.ui;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

/**
 * O Renderizador Oficial da SuperUI para Desktop.
 * Converte a árvore XplNode em componentes reais do JavaFX.
 */
public class JavaFxRenderer implements XplUiBridge {

    // A raiz da tua janela no JavaFX (Onde tudo vai ser desenhado)
    private final Pane windowRoot;

    // O canal de comunicação para gritar para a SuperUiEngine
    private EngineCallback engineCallback;

    // Um registo rápido para encontrarmos os nós pelo ID quando a Engine pedir atualizações
    private final Map<String, Node> fxNodeRegistry = new HashMap<>();

    public JavaFxRenderer(Pane windowRoot) {
        this.windowRoot = windowRoot;
    }

    @Override
    public void setEngineCallback(EngineCallback callback) {
        this.engineCallback = callback;
    }

    // =====================================================================
    // 1. A CONVERSÃO PRINCIPAL (XPL -> JavaFX)
    // =====================================================================
    @Override
    public void renderView(XplNode activeDomRoot) {
        // ⭐ REGRA DE OURO DO JAVAFX: Toda a alteração visual tem de correr na Thread da UI!
        Platform.runLater(() -> {
            windowRoot.getChildren().clear(); // Limpa a tela
            fxNodeRegistry.clear();           // Limpa a memória

            // Converte e adiciona todos os filhos do root
            for (XplNode xplChild : activeDomRoot.children) {
                Node fxNode = convertNode(xplChild);
                if (fxNode != null) {
                    windowRoot.getChildren().add(fxNode);
                }
            }
        });
    }

    // =====================================================================
    // 2. A FÁBRICA DE COMPONENTES (A Magia da Tradução)
    // =====================================================================
    private Node convertNode(XplNode xplNode) {
        Node fxNode = null;

        // 1. MAPEAMENTO DE TAGS HTML PARA CLASSES JAVAFX
        switch (xplNode.tag.toLowerCase()) {
            case "div":
            case "main":
            case "section":
                VBox box = new VBox(5); // VBox agrupa elementos na vertical (como uma div padrão)
                // RECURSÃO: Uma div pode ter filhos, convertemos todos eles!
                for (XplNode child : xplNode.children) {
                    Node c = convertNode(child);
                    if (c != null) box.getChildren().add(c);
                }
                fxNode = box;
                break;

            case "button":
                Button btn = new Button(xplNode.textContent.trim());

                // ⭐ LIGAÇÃO DE EVENTOS: O JavaFX "ouve" o clique e grita para a Engine!
                if (xplNode.events.containsKey("click")) {
                    String eventAction = xplNode.events.get("click"); // ex: "compilarTudo()"
                    btn.setOnAction(e -> {
                        if (engineCallback != null) {
                            engineCallback.onEvent(eventAction, null);
                        }
                    });
                }
                fxNode = btn;
                break;

            case "input":
                TextField input = new TextField();
                if (xplNode.attributes.containsKey("value")) {
                    input.setText(xplNode.attributes.get("value").toString());
                }
                if (xplNode.attributes.containsKey("placeholder")) {
                    input.setPromptText(xplNode.attributes.get("placeholder").toString());
                }
                // Exemplo: Quando o utilizador digita, avisa a Engine
                if (xplNode.events.containsKey("input")) {
                    String eventAction = xplNode.events.get("input");
                    input.textProperty().addListener((obs, oldV, newV) -> {
                        if (engineCallback != null) engineCallback.onEvent(eventAction, newV);
                    });
                }
                fxNode = input;
                break;

            case "h1":
            case "h2":
            case "h3":
            case "span":
            case "p":
                Label label = new Label(xplNode.textContent.trim());
                // Aqui no futuro aplicaríamos tamanhos de fonte baseados na tag (H1 maior que P)
                fxNode = label;
                break;

            case "text":
                // Se for um nó de texto solto, vira uma Label simples
                if (!xplNode.textContent.trim().isEmpty()) {
                    fxNode = new Label(xplNode.textContent.trim());
                }
                break;
        }

        // 2. APLICAÇÃO DE PROPRIEDADES GLOBAIS (Se o nó foi criado com sucesso)
        if (fxNode != null) {
            // Registar o ID
            if (!xplNode.id.isEmpty()) {
                fxNode.setId(xplNode.id);
                fxNodeRegistry.put(xplNode.id, fxNode);
            }

            // Adicionar Classes CSS (Para o teu Dark Mode ou Tailwind!)
            if (!xplNode.className.isEmpty()) {
                fxNode.getStyleClass().addAll(xplNode.className.split(" "));
            }

            // Atributos de Estado (ex: disabled="true")
            if (xplNode.attributes.containsKey("disabled")) {
                boolean isDisabled = Boolean.parseBoolean(xplNode.attributes.get("disabled").toString());
                fxNode.setDisable(isDisabled);
            }
        }

        return fxNode;
    }

    // =====================================================================
    // 3. ATUALIZAÇÃO CIRÚRGICA (Alta Performance)
    // =====================================================================
    @Override
    public void updateProperty(String nodeId, String propertyName, Object newValue) {
        Platform.runLater(() -> {
            Node fxNode = fxNodeRegistry.get(nodeId);
            if (fxNode == null) return;

            switch (propertyName) {
                case "disabled":
                    fxNode.setDisable(Boolean.parseBoolean(newValue.toString()));
                    break;
                case "value":
                    if (fxNode instanceof TextField) {
                        ((TextField) fxNode).setText(newValue.toString());
                    }
                    break;
                case "text":
                    if (fxNode instanceof Button) {
                        ((Button) fxNode).setText(newValue.toString());
                    } else if (fxNode instanceof Label) {
                        ((Label) fxNode).setText(newValue.toString());
                    }
                    break;
            }
        });
    }

    @Override
    public void reportError(String message) {
        System.err.println("[JavaFxRenderer ERROR]: " + message);
    }
}