package com.dic.xsuper.lang.ui;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.tags.TagFactory;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Map;

/**
 * O Renderizador Oficial da SuperUI para Desktop.
 * Converte a árvore XplNode em componentes reais do JavaFX através da TagFactory.
 */
public class JavaFxRenderer implements XplUiBridge {

    private final Pane windowRoot;
    private EngineCallback engineCallback;
    private final Map<String, Node> fxNodeRegistry = new HashMap<>();

    public JavaFxRenderer(Pane windowRoot) {
        this.windowRoot = windowRoot;
    }

    @Override
    public void setEngineCallback(EngineCallback callback) {
        this.engineCallback = callback;
    }

    // =====================================================================
    // 1. A CONVERSÃO PRINCIPAL (Delegada à Arquitetura NativeTag)
    // =====================================================================
    @Override
    public void renderView(XplNode activeDomRoot) {
        Platform.runLater(() -> {
            windowRoot.getChildren().clear(); // Limpa a tela
            fxNodeRegistry.clear();           // Limpa a memória

            // Usa o teu TagFactory para invocar toda a lógica de CSS/Properties!
            for (XplNode xplChild : activeDomRoot.children) {
                NativeTag tag = TagFactory.create(xplChild);
                if (tag != null) {
                    Node fxNode = tag.build(); // O build() resolve o layout, cores, borders, etc!
                    if (fxNode != null) {
                        // Percorre a árvore gerada para capturar IDs e injetar Eventos
                        registerNodeRecursively(tag);
                        windowRoot.getChildren().add(fxNode);
                    }
                }
            }
        });
    }

    // =====================================================================
    // 2. REGISTO RECURSIVO DE IDs E EVENTOS
    // =====================================================================
    private void registerNodeRecursively(NativeTag tag) {
        Node fxNode = tag.getFxNode();
        if (fxNode != null) {
            String id = tag.getId();
            if (id != null && !id.isEmpty()) {
                fxNodeRegistry.put(id, fxNode);
            }

            // ⭐ LIGAÇÃO UNIVERSAL DE EVENTOS ⭐
            for (Map.Entry<String, String> entry : tag.getEvents().entrySet()) {
                String eventName = entry.getKey();
                String action = entry.getValue();

                if (eventName.equals("click")) {
                    if (fxNode instanceof ButtonBase btn) {
                        btn.setOnAction(e -> {
                            if (engineCallback != null) engineCallback.onEvent(action, null);
                        });
                    } else {
                        // Agora qualquer elemento (Div, Img, Span) pode receber um clique!
                        fxNode.setOnMouseClicked(e -> {
                            if (engineCallback != null) engineCallback.onEvent(action, null);
                        });
                    }
                } else if (eventName.equals("input") && fxNode instanceof TextInputControl input) {
                    input.textProperty().addListener((obs, oldV, newV) -> {
                        if (engineCallback != null) engineCallback.onEvent(action, newV);
                    });
                }
            }
        }

        for (NativeTag child : tag.getChildren()) {
            registerNodeRecursively(child);
        }
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
                    if (fxNode instanceof TextInputControl input) {
                        input.setText(newValue.toString());
                    }
                    break;
                case "text":
                    if (fxNode instanceof ButtonBase btn) {
                        btn.setText(newValue.toString());
                    } else if (fxNode instanceof Label lbl) {
                        lbl.setText(newValue.toString());
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