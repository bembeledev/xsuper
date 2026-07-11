package com.dic.xsuper.lang.ui.tags;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;

/**
 * Tag HTML <a> – Hyperlink blindado e maduro.
 * Suporta navegação (href), filhos internos (como imagens) e ignora os estilos feios do JavaFX.
 */
public class AnchorTag extends NativeTag {

    private Hyperlink fxLink;

    public AnchorTag(XplNode node) {
        super(node);
    }

    @Override
    protected Node createNode() {
        fxLink = new Hyperlink();

        // 1. Limpeza Bruta do JavaFX: Remove as bordas tracejadas (foco) e os paddings extra
        fxLink.setStyle("-fx-border-color: transparent; -fx-padding: 0; -fx-cursor: hand; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        // 2. Extrai o texto solto (se existir)
        if (sourceNode.textContent != null && !sourceNode.textContent.trim().isEmpty()) {
            fxLink.setText(sourceNode.textContent.trim());
        }

        // 3. Lê o URL de destino
        String href = (String) sourceNode.attributes.getOrDefault("href", "");

        // 4. Aciona a navegação
        fxLink.setOnAction(e -> handleNavigation(href));

        // 5. A MAGIA: Impede o link de ficar "Roxo" (Visited) após o primeiro clique!
        fxLink.visitedProperty().addListener((obs, old, isVisited) -> {
            if (isVisited) {
                fxLink.setVisited(false); // Força a voltar ao normal instantaneamente
            }
        });

        return fxLink;
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Se o programador NÃO definiu uma cor explícita no style="...", aplicamos o padrão da Web
        String currentStyle = fxLink.getStyle();
        if (!currentStyle.contains("-fx-text-fill")) {
            // Azul padrão (#2563eb) com sublinhado
            fxLink.setStyle(currentStyle + " -fx-text-fill: #2563eb;");
        }

        // O underline na Web é padrão em links, se não foi desativado explicitamente
        if (!currentStyle.contains("-fx-underline") && !currentStyle.contains("none")) {
            fxLink.setStyle(fxLink.getStyle() + " -fx-underline: true;");
        }
    }

    @Override
    protected void addChildren() {
        // Se o link for apenas texto (ex: <a href="x">Clica aqui</a>), não precisamos de fazer nada
        if (children.isEmpty()) return;

        // Se o <a> tiver tags filhas (ex: <img>, <span>, <div>), temos de as renderizar!
        // Como o Hyperlink não é um Pane (contentor), usamos a sua propriedade 'Graphic' para agrupar os filhos.
        HBox contentWrapper = new HBox(5);
        contentWrapper.setAlignment(Pos.CENTER_LEFT);

        for (NativeTag child : children) {
            contentWrapper.getChildren().add(child.build());
        }

        fxLink.setGraphic(contentWrapper);

        // Se o link for puramente um contentor de imagens/elementos, ocultamos o texto padrão vazio
        if (sourceNode.textContent == null || sourceNode.textContent.trim().isEmpty()) {
            fxLink.setText("");
        }
    }

    /**
     * Motor central de redirecionamento.
     */
    private void handleNavigation(String href) {
        if (href == null || href.trim().isEmpty() || href.equals("#")) return;

        // Se for um link web absoluto (http/https), podes abrir no browser nativo do utilizador:
        if (href.startsWith("http://") || href.startsWith("https://")) {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(href));
            } catch (Exception e) {
                System.err.println("Erro ao abrir link externo: " + e.getMessage());
            }
        }
        else {
            // Se for um link interno da tua aplicação (roteamento interno do teu motor XPL)
            // Exemplo: <a href="/pagina2.xpl">
            System.out.println("[Router XPL] A navegar para vista interna: " + href);

            // Aqui podes disparar um evento global para o teu motor de Layout/Páginas carregar o novo XplNode.
            // dispatchEvent("navigate", sourceNode.id, href);
        }
    }
}