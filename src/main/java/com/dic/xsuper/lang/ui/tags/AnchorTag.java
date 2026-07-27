package com.dic.xsuper.lang.ui.tags;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;

/**
 * Tag HTML <a> – Hyperlink blindado e inteligente.
 * Resolve o bug de duplicação separando texto puro de conteúdo gráfico misto.
 */
public class AnchorTag extends NativeTag {

    private Hyperlink fxLink;

    public AnchorTag(XplNode node) {
        super(node);
    }

    @Override
    protected Node createNode() {
        fxLink = new Hyperlink();

        // 1. Limpeza Bruta do JavaFX
        fxLink.setStyle("-fx-border-color: transparent; -fx-padding: 0; -fx-cursor: hand; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        // ⭐ O TEXTO FOI REMOVIDO DAQUI!
        // Passou para o `addChildren` para o motor poder analisar e não criar clones visuais.

        // 2. Lê o URL de destino
        String href = (String) sourceNode.attributes.getOrDefault("href", "");

        // 3. Aciona a navegação
        fxLink.setOnAction(e -> handleNavigation(href));

        // 4. A MAGIA: Impede o link de ficar "Roxo" (Visited) após o primeiro clique!
        fxLink.visitedProperty().addListener((obs, old, isVisited) -> {
            if (isVisited) {
                fxLink.setVisited(false);
            }
        });

        return fxLink;
    }

    @Override
    protected void applyTagSpecificStyles() {
        String currentStyle = fxLink.getStyle();
        if (!currentStyle.contains("-fx-text-fill")) {
            // Azul padrão W3C (#2563eb)
            fxLink.setStyle(currentStyle + " -fx-text-fill: #2563eb;");
        }

        if (!currentStyle.contains("-fx-underline") && !currentStyle.contains("none")) {
            fxLink.setStyle(fxLink.getStyle() + " -fx-underline: true;");
        }
    }

    @Override
    protected void addChildren() {
        if (children.isEmpty()) {
            // Caso de fallback: Se por acaso não há filhos (nem de texto), tenta ler do textContent
            if (sourceNode.textContent != null && !sourceNode.textContent.trim().isEmpty()) {
                fxLink.setText(sourceNode.textContent.trim());
            }
            return;
        }

        // 🧠 ESTRATÉGIA INTELIGENTE: Verificar se temos nós complexos (<img>, <svg>, <span>, etc.)
        boolean hasComplexChildren = false;
        for (NativeTag child : children) {
            if (!"#text".equalsIgnoreCase(child.getSourceNode().tag)) {
                hasComplexChildren = true;
                break;
            }
        }

        if (hasComplexChildren) {
            // 🎨 RENDERIZAÇÃO GRÁFICA (Temos HTML Misto)
            // Agrupamos tudo (texto + ícones) num contentor e desligamos o canal de texto nativo!
            HBox contentWrapper = new HBox(5);
            contentWrapper.setAlignment(Pos.CENTER_LEFT);

            for (NativeTag child : children) {
                contentWrapper.getChildren().add(child.build());
            }

            fxLink.setGraphic(contentWrapper);
            fxLink.setText(""); // Silencia o texto raiz para impedir a duplicação!

        } else {
            // 📝 RENDERIZAÇÃO DE TEXTO PURO (HTML Simples)
            // Se o motor deteta que só existem nós "#text", ignora os gráficos e usa a performance nativa
            if (sourceNode.textContent != null && !sourceNode.textContent.trim().isEmpty()) {
                fxLink.setText(sourceNode.textContent.trim());
            }
            fxLink.setGraphic(null); // Assegura que não há lixo gráfico!
        }
    }

    /**
     * Motor central de redirecionamento.
     */
    private void handleNavigation(String href) {
        if (href == null || href.trim().isEmpty() || href.equals("#")) return;

        if (href.startsWith("http://") || href.startsWith("https://")) {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(href));
            } catch (Exception e) {
                System.err.println("Erro ao abrir link externo: " + e.getMessage());
            }
        } else if (href.startsWith("#")) {
            // ⭐ A CURA DAS ÂNCORAS: Faz scroll automático até à ID!
            String targetId = href.substring(1);
            com.dic.xsuper.lang.ui.SuperUiEngine engine = com.dic.xsuper.lang.ui.SuperUiEngine.getInstance();

            if (engine != null && engine.getActiveDom() != null) {
                com.dic.xsuper.lang.ui.html.XplNode targetNode = engine.getActiveDom().getElementById(targetId);
                if (targetNode != null && targetNode.nativeNode != null) {
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.Node targetFxNode = (javafx.scene.Node) targetNode.nativeNode;
                        targetFxNode.getParent().requestLayout();
                        targetFxNode.requestFocus(); // Puxa o ecrã instantaneamente para este elemento!
                    });
                }
            }
        }
    }
}