package com.dic.xsuper.lang.ui.tags;

import com.dic.xsuper.lang.ui.SuperUiEngine;
import com.dic.xsuper.lang.ui.animation.XplKeyframeAnimation;
import com.dic.xsuper.lang.ui.cursor.StyleCursorUtils;
import com.dic.xsuper.lang.ui.event.AttachJavaFxListener;
import com.dic.xsuper.lang.ui.event.XplEventType;
import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.layout.panes.CustomLayoutPane;
import com.dic.xsuper.lang.ui.properties.*;
import com.dic.xsuper.lang.ui.properties.borderunit.BorderSide;
import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;
import com.dic.xsuper.lang.ui.properties.gradient.Gradient;
import com.dic.xsuper.lang.ui.properties.gradient.GradientStop;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import java.util.*;

public abstract class NativeTag{
    protected String id;
    protected String className;
    protected Map<String, String> style = new HashMap<>();
    protected Map<String, String> events = new HashMap<>();
    protected List<NativeTag> children = new ArrayList<>();
    protected XplNode sourceNode;
    protected Node fxNode;

    protected ResolvedStyles resolvedStyles;
    protected CssContext cssContext;

    public NativeTag(XplNode node) {

        this.sourceNode = node;
        this.id = node.id;
        this.className = node.className;

        if (node.events != null) this.events.putAll(node.events);

        if (node.attributes != null && node.attributes.containsKey("style")) {
            parseStyle(node.attributes.get("style").toString());
        }
        if (node.style != null) {
            this.style.putAll(node.style);
        }

        this.resolvedStyles = StyleResolver.resolve(node.tag, this.style);

        // ⭐ Vai buscar as dimensões dinâmicas à Engine!
        com.dic.xsuper.lang.ui.SuperUiEngine engine = com.dic.xsuper.lang.ui.SuperUiEngine.getInstance();
        double vw = engine != null ? engine.getViewportWidth() : 800;
        double vh = engine != null ? engine.getViewportHeight() : 600;

        this.cssContext = new com.dic.xsuper.lang.ui.properties.cssunit.CssContext()
                .withFontSize((float) resolvedStyles.fontSize.toPixels(new com.dic.xsuper.lang.ui.properties.cssunit.CssContext()))

                // O "Parent" assume o tamanho do ecrã (Viewport) inicialmente.
                // Num motor avançado, este valor seria atualizado pelo teu LayoutEngine
                // no momento em que o pai é posicionado no ecrã.
                .withParentSize(vw)

                // Dimensão global do ecrã para as medidas 'vw' e 'vh' do CSS
                .withViewport(vw, vh);

        for (XplNode childNode : node.children) {
            NativeTag childTag = TagFactory.create(childNode);
            if (childTag != null) this.children.add(childTag);
        }
    }

    // Em NativeTag.java

    public Node build() {
        fxNode = createNode();
        applyCommonStyles();
        applyTagSpecificStyles();

        // Usamos new HashMap<> para compatibilidade entre Map<String, String> e Map<String, Object>
        com.dic.xsuper.lang.ui.helpers.StyleTransformUtils.applyTransforms(fxNode, new HashMap<>(this.style));
        addChildren();
        bindEvents();

        bindContextMenu();

        // 🌐 PADRÃO DA WEB: Qualquer nó vira scroll se tiver "overflow: auto" ou "scroll"
        Map<String, String> styles = getRawStyles();
        String overflow = styles.getOrDefault("overflow", "visible").toLowerCase();
        String overflowY = styles.getOrDefault("overflow-y", overflow).toLowerCase();
        String overflowX = styles.getOrDefault("overflow-x", overflow).toLowerCase();

        // ==========================================================
        // ⭐ MEMÓRIA DA TRANSIÇÃO (Para o JavaFxRenderer ler mais tarde)
        // ==========================================================
        if (styles.containsKey("transition")) {
            fxNode.getProperties().put("transition", styles.get("transition"));
        }

// ==========================================================
        // ⭐ 4. O GATILHO DAS ANIMAÇÕES (Agora entregue ao Maestro)
        // ==========================================================
        if (styles.containsKey("animation")) {
            String animationConfig = styles.get("animation");
            // Ex: "bounce 2s infinite" ou "meuEfeitoCustomizado 1s forwards"
            String[] parts = animationConfig.trim().split("\\s+");

            if (parts.length > 0) {
                String animName = parts[0];
                Map<String, String> overrides = extractAnimationOverrides(parts);

                // Enviamos o Nó, o Nome, os Overrides e o teu Gestor de Animações CSS para o Maestro!
                com.dic.xsuper.lang.ui.animation.XplAnimationEngine.playAnimation(
                        fxNode,
                        animName,
                        overrides,
                        SuperUiEngine.getInstance().getAnimationManager()
                );
            }
        }

        // ⭐ A 1ª ÂNCORA: Liga o Nó Físico ao Nó Virtual (Cura a Amnésia do Diffing!)
        this.sourceNode.nativeNode = fxNode;

        // ⭐ A 2ª ÂNCORA: Liga o Nó Físico ao Cérebro (NativeTag)
        fxNode.getProperties().put("xpl_native_tag", this);

        // ==========================================================
        // ⭐ O TRADUTOR UNIVERSAL DO FLEXBOX (Comportamento de Browser)
        // ==========================================================
        // 1. As Tags Raiz do W3C nascem a pedir o ecrã inteiro!
        boolean isRoot = "html".equalsIgnoreCase(sourceNode.tag) || "body".equalsIgnoreCase(sourceNode.tag);

        // 2. Ou se o CSS mandou esticar este elemento especificamente...
        boolean requestsGrowth = styles.containsKey("flex") && styles.get("flex").startsWith("1");
        if (styles.containsKey("height") && (styles.get("height").equals("100%") || styles.get("height").equals("100vh"))) requestsGrowth = true;
        if (styles.containsKey("width") && (styles.get("width").equals("100%") || styles.get("width").equals("100vw"))) requestsGrowth = true;

        if (isRoot || requestsGrowth) {
            // Usamos o Platform.runLater porque, no exato milissegundo do build(),
            // o JavaFX ainda não anexou este nó ao Pai. Esperamos 1 frame e damos a ordem!
            javafx.application.Platform.runLater(() -> {
                javafx.scene.Parent parent = fxNode.getParent();
                if (parent instanceof javafx.scene.layout.VBox) {
                    javafx.scene.layout.VBox.setVgrow(fxNode, javafx.scene.layout.Priority.ALWAYS);
                } else if (parent instanceof javafx.scene.layout.HBox) {
                    javafx.scene.layout.HBox.setHgrow(fxNode, javafx.scene.layout.Priority.ALWAYS);
                }
            });
        }
        // ==========================================================

        if (overflowY.equals("auto") || overflowY.equals("scroll") ||
                overflowX.equals("auto") || overflowX.equals("scroll")) {
            return wrapInWebScroll(fxNode, overflowX, overflowY, styles);
        }

        return fxNode;
    }


    // ==========================================================
    // 🛠️ Helper para extrair a Duração, Iterações, etc. (Cola isto na NativeTag se não tiveres)
    // ==========================================================
    private Map<String, String> extractAnimationOverrides(String[] parts) {
        Map<String, String> overrides = new HashMap<>();

        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].toLowerCase();
            if (p.endsWith("ms") || p.endsWith("s")) {
                if (!overrides.containsKey("duration")) overrides.put("duration", p);
            } else if (p.equals("forwards") || p.equals("backwards") || p.equals("both") || p.equals("none")) {
                overrides.put("fill-mode", p);
            } else if (p.equals("normal") || p.equals("reverse") || p.equals("alternate") || p.equals("alternate-reverse")) {
                overrides.put("direction", p);
            } else if (p.equals("infinite")) {
                overrides.put("iterations", "-1");
            } else if (p.matches("\\d+")) {
                overrides.put("iterations", p);
            }
        }
        return overrides;
    }

    /**
     * Envolve o nó atual numa barra de rolagem moderna e transfere as dimensões.
     */
    private Node wrapInWebScroll(Node content, String overflowX, String overflowY, Map<String, String> styles) {
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(content);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setFitToWidth(true); // O conteúdo adapta-se à largura (comum na web)

        if (content instanceof javafx.scene.layout.Region region) {
            // A MAGIA DIMENSIONAL:
            // 1. Transfere a altura restrita do <div> para a janela do ScrollPane
            scrollPane.setPrefHeight(region.getPrefHeight());
            scrollPane.setMaxHeight(region.getMaxHeight());

            // 2. Liberta o <div> interno para poder crescer infinitamente para baixo!
            region.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        }

        // Ativa a rolagem apenas onde foi pedida (eixo X ou Y)
        scrollPane.setHbarPolicy(overflowX.equals("auto") || overflowX.equals("scroll") ?
                javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED :
                javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);

        scrollPane.setVbarPolicy(overflowY.equals("auto") || overflowY.equals("scroll") ?
                javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED :
                javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);

        // === SCROLLBAR CUSTOMIZÁVEL MODERNA (Estilo Tailwind) ===
        String thumbColor = styles.getOrDefault("scrollbar-thumb", "#cbd5e1");
        String hoverColor = styles.getOrDefault("scrollbar-hover", "#94a3b8");
        String trackColor = styles.getOrDefault("scrollbar-track", "transparent");
        String width = styles.getOrDefault("scrollbar-width", "8px");
        String radius = styles.getOrDefault("scrollbar-radius", "10px");

        String css = String.format("""
            .scroll-pane > .viewport { -fx-background-color: transparent; }
            .scroll-bar:vertical, .scroll-bar:horizontal { -fx-background-color: %s; -fx-pref-width: %s; -fx-pref-height: %s; }
            .scroll-bar .track { -fx-background-color: %s; -fx-background-radius: %s; }
            .scroll-bar .thumb { -fx-background-color: %s; -fx-background-radius: %s; }
            .scroll-bar .thumb:hover { -fx-background-color: %s; }
            .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; }
            .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-padding: 0; -fx-shape: null; }
            """, trackColor, width, width, trackColor, radius, thumbColor, radius, hoverColor);

        // Injeta a folha de estilos virtual baseada em Base64 para dominar o JavaFX nativo
        String encodedCss = java.util.Base64.getEncoder().encodeToString(css.getBytes());
        scrollPane.getStylesheets().add("data:text/css;base64," + encodedCss);

        return scrollPane;
    }

    protected abstract Node createNode();
    protected abstract void applyTagSpecificStyles();

    // Em NativeTag.java (adiciona este método)
    public void invokeMethod(String methodName, Object... args) {
        // Por padrão, as tags normais (div, span) não têm métodos especiais.
        // Pode emitir um aviso no console se necessário.
        System.err.println("[NativeTag] Método '" + methodName + "' não suportado nesta tag.");
    }

    // Em NativeTag.java
    public void onReactiveAttributeChange(String attrName, Object newValue) {
        // Por predefinição, as tags base não fazem nada de especial com a reatividade pura de atributos
        // (Deixamos as propriedades CSS para o JavaFxRenderer tratar)
        this.sourceNode.attributes.put(attrName, newValue);
    }

    protected void addChildren() {
        if (fxNode instanceof javafx.scene.layout.Pane pane) {

            // ⭐ 1. LÓGICA DO GRID E LAYOUTS
            switch (fxNode) {

                case com.dic.xsuper.lang.ui.layout.panes.CustomLayoutPane customPane -> {
                    customPane.populateChildren(children, cssContext);
                }
                case null, default -> {
                    // StackPane, VBox, HBox e outros
                    for (NativeTag child : children) {
                        Node childNode = child.build();

                        applyJavaFxMargins(childNode, child); // ⭐ Aplica margem!

                        // ==========================================================
                        // ⭐ 2. O TRADUTOR W3C INTELIGENTE (Instinto + CSS)
                        // ==========================================================
                        boolean requestsGrowth = child.isGreedyByDefault(); // Lê o instinto natural

                        java.util.Map<String, String> childStyles = child.getRawStyles();

                        // O CSS do programador tem sempre a palavra final
                        if (childStyles.containsKey("flex")) {
                            requestsGrowth = childStyles.get("flex").startsWith("1");
                        }
                        if (childStyles.containsKey("flex-grow")) {
                            requestsGrowth = childStyles.get("flex-grow").trim().equals("1");
                        }
                        if (childStyles.containsKey("height") && (childStyles.get("height").trim().equals("100%") || childStyles.get("height").trim().equals("100vh"))) {
                            requestsGrowth = true;
                        }
                        if (childStyles.containsKey("width") && (childStyles.get("width").trim().equals("100%") || childStyles.get("width").trim().equals("100vw"))) {
                            requestsGrowth = true;
                        }

                        String childTagName = child.getSourceNode().tag.toLowerCase();

                        // O HTML, o BODY e as tags "Gulosas" recebem Priority.ALWAYS
                        if (requestsGrowth || "html".equals(childTagName) || "body".equals(childTagName)) {
                            if (pane instanceof javafx.scene.layout.VBox) {
                                javafx.scene.layout.VBox.setVgrow(childNode, javafx.scene.layout.Priority.ALWAYS);
                            }
                            if (pane instanceof javafx.scene.layout.HBox) {
                                javafx.scene.layout.HBox.setHgrow(childNode, javafx.scene.layout.Priority.ALWAYS);
                            }
                        }
                        // ==========================================================

                        pane.getChildren().add(childNode);
                    }
                }
            }
        }
    }

    // ⭐ NOVO: O motor de Margens do JavaFX
    private void applyJavaFxMargins(Node fxChild, NativeTag childTag) {
        if (!childTag.getResolvedStyles().margin.isZero()) {
            Insets margin = new Insets(
                    childTag.getResolvedStyles().margin.getTopPixels(cssContext),
                    childTag.getResolvedStyles().margin.getRightPixels(cssContext),
                    childTag.getResolvedStyles().margin.getBottomPixels(cssContext),
                    childTag.getResolvedStyles().margin.getLeftPixels(cssContext)
            );
            if (fxNode instanceof javafx.scene.layout.VBox) javafx.scene.layout.VBox.setMargin(fxChild, margin);
            if (fxNode instanceof javafx.scene.layout.HBox) javafx.scene.layout.HBox.setMargin(fxChild, margin);
            if (fxNode instanceof javafx.scene.layout.GridPane) javafx.scene.layout.GridPane.setMargin(fxChild, margin);
        }
    }


    /**
     * Motor de interceção otimizado. Lê DIRETAMENTE da nova gaveta de eventos.
     */
    protected void bindEvents() {
        // Sai imediatamente se não houver eventos para poupar memória e processamento
        if (fxNode == null || sourceNode.events == null || sourceNode.events.isEmpty()) return;

        // Itera apenas sobre a gaveta exclusiva de eventos (click, keydown, etc.)
        for (Map.Entry<String, String> entry : sourceNode.events.entrySet()) {
            String eventName = entry.getKey();           // Ex: "click"
            String scriptCallback = entry.getValue();    // Ex: "submeterDados(event)"

            // Converte a string limpa para o teu enum W3C
            XplEventType eventType = XplEventType.fromString(eventName);

            if (eventType != null) {
                AttachJavaFxListener.attachJavaFxListener(this.sourceNode,fxNode, eventType, scriptCallback);
            } else {
                System.err.println("[NativeTag] Aviso: Evento W3C não suportado -> " + eventName);
            }
        }
    }


    protected void applyCommonStyles() {
        if (fxNode == null) return;

        if (className != null && !className.isEmpty()) fxNode.getStyleClass().addAll(className.split(" "));
        if (id != null && !id.isEmpty()) fxNode.setId(id);

        if (sourceNode.attributes.containsKey("disabled")) {
            fxNode.setDisable(Boolean.parseBoolean(sourceNode.attributes.get("disabled").toString()));
        }

        // ─── Cursor ─────────────────────────────────────────────────────────────
        if (style.containsKey("cursor")) {
            StyleCursorUtils.applyCursor(fxNode, style.get("cursor"));
        }

        StringBuilder css = new StringBuilder();

        if (fxNode instanceof Region region) {

            // ⭐ A CURA PARA O WIDTH: 100% E DIMENSÕES FIXAS
            String widthStr = style.get("width");
            if ("100%".equals(widthStr)) {
                region.setMaxWidth(Double.MAX_VALUE);
            } else {
                float w = resolvedStyles.boxSize.getWidthPixels(cssContext);
                if (w > 0) {
                    // O bloqueio absoluto do W3C Box Model!
                    css.append("-fx-pref-width: ").append(w).append("px; ");
                    css.append("-fx-min-width: ").append(w).append("px; ");
                    css.append("-fx-max-width: ").append(w).append("px; ");
                }
            }

            // O mesmo para o height
            String heightStr = style.get("height");
            if ("100%".equals(heightStr)) {
                region.setMaxHeight(Double.MAX_VALUE);
                region.setPrefHeight(Region.USE_PREF_SIZE);
            } else {
                float h = resolvedStyles.boxSize.getHeightPixels(cssContext);
                if (h > 0) {
                    css.append("-fx-pref-height: ").append(h).append("px; ");
                    css.append("-fx-min-height: ").append(h).append("px; ");
                    css.append("-fx-max-height: ").append(h).append("px; ");
                }
            }

            // Padding CSS
            css.append("-fx-padding: ")
                    .append(resolvedStyles.padding.getTopPixels(cssContext)).append("px ")
                    .append(resolvedStyles.padding.getRightPixels(cssContext)).append("px ")
                    .append(resolvedStyles.padding.getBottomPixels(cssContext)).append("px ")
                    .append(resolvedStyles.padding.getLeftPixels(cssContext)).append("px; ");

            // Background Gradiente ou Cor Sólida
            if (resolvedStyles.gradients != null && !resolvedStyles.gradients.isEmpty()) {
                css.append("-fx-background-color: ").append(generateGradientCSS(resolvedStyles.gradients.get(0))).append("; ");
            } else if (resolvedStyles.backgroundColor != 0x00000000) {
                css.append("-fx-background-color: ").append(toJavaFxCssColor(resolvedStyles.backgroundColor)).append("; ");
            } else {
                // ⭐ A CURA DAS COLISÕES: Sem isto, as DIVs vazias são fantasmas para o rato!
                css.append("-fx-background-color: transparent; ");
            }

            // Border
            if (resolvedStyles.border.hasBorder()) {
                css.append(generateBorderCSS(resolvedStyles.border));
            }

            // Border Radius (Cantos: TopLeft TopRight BottomRight BottomLeft)
            if (!resolvedStyles.borderRadius.isZero()) {
                float tl = resolvedStyles.borderRadius.getTopPixels(cssContext);
                float tr = resolvedStyles.borderRadius.getRightPixels(cssContext);
                float br = resolvedStyles.borderRadius.getBottomPixels(cssContext);
                float bl = resolvedStyles.borderRadius.getLeftPixels(cssContext);

                // Formato exigido pelo CSS do JavaFX para os 4 cantos
                String radii = tl + "px " + tr + "px " + br + "px " + bl + "px; ";

                css.append("-fx-background-radius: ").append(radii);
                css.append("-fx-border-radius: ").append(radii);
            }

            // ⭐ A CURA DA SOMBRA (Injeta no StringBuilder!) ⭐
            if (!resolvedStyles.boxShadows.isEmpty()) {
                css.append(generateBoxShadowCSS(resolvedStyles.boxShadows));
            }
        }

        // Fontes e Texto
        if (resolvedStyles.textColor != 0xFF000000) {
            css.append("-fx-text-fill: ").append(toJavaFxCssColor(resolvedStyles.textColor)).append("; ");
            css.append("-fx-text-inner-color: ").append(toJavaFxCssColor(resolvedStyles.textColor)).append("; ");
        }
        if (resolvedStyles.fontSize != null && resolvedStyles.fontSize.toPixels(cssContext) > 0) {
            css.append("-fx-font-size: ").append(resolvedStyles.fontSize.toPixels(cssContext)).append("px; ");
        }
        if ("bold".equals(resolvedStyles.fontWeight)) {
            css.append("-fx-font-weight: bold; ");
        }

        // ⭐ A INTEGRAÇÃO DO NOVO ADAPTADOR W3C
        com.dic.xsuper.lang.ui.css.W3cCssAdapter.applyW3cToNative(fxNode, style, css);

        if (!css.isEmpty()) {
            // ⭐ A CURA DEFINITIVA (O Escudo Anti-Vazios) ⭐
            // Esta expressão regular varre o CSS e extermina qualquer propriedade
            // que tenha ficado órfã e sem valor (Ex: "-fx-background-color: ;")
            String cleanCss = css.toString().replaceAll("[a-zA-Z0-9\\-]+:\\s*;\\s*", "");

            if (!cleanCss.isEmpty()) {
                fxNode.setStyle(fxNode.getStyle() + (fxNode.getStyle().isEmpty() ? "" : "; ") + cleanCss);
            }
        }
    }


    // =========================================================================
    // ⭐ SISTEMA DE MENUS DE CONTEXTO GLOBAIS REUTILIZÁVEIS
    // =========================================================================
    protected void bindContextMenu() {
        if (fxNode == null || sourceNode.attributes == null) return;

        // Verifica se o programador XPL pediu um menu para esta tag
        Object menuIdAttr = sourceNode.attributes.get("context-menu");

        if (menuIdAttr != null) {
            String menuId = menuIdAttr.toString();

            // O JavaFX dispara isto no clique direito do rato ou tecla de menu do teclado!
            fxNode.setOnContextMenuRequested(event -> {
                // ⭐ A CURA DOS DUPLICADOS: Impede o evento de "borbulhar" para o Pai.
                // Se clicares numa tag com menu que está dentro de outra tag com menu, só abre o mais pequeno!
                event.consume();

                // 1. Usa o Singleton da Engine para mergulhar no VDOM
                com.dic.xsuper.lang.ui.SuperUiEngine engine = com.dic.xsuper.lang.ui.SuperUiEngine.getInstance();
                if (engine != null && engine.getActiveDom() != null) {

                    // 2. Procura a tag <contextmenu> em qualquer parte da memória usando o ID
                    com.dic.xsuper.lang.ui.html.XplNode menuVirtualNode = engine.getActiveDom().getElementById(menuId);

                    if (menuVirtualNode != null && menuVirtualNode.nativeNode instanceof javafx.scene.Node dummyNode) {

                        // 3. Lê o menu que a ContextMenuTag guardou em segredo!
                        Object rawMenu = dummyNode.getProperties().get("xpl_context_menu");

                        if (rawMenu instanceof javafx.scene.control.ContextMenu ctxMenu) {
                            // 4. Exibe o menu exatamente na coordenada (X,Y) do ecrã onde o rato clicou
                            ctxMenu.show(fxNode, event.getScreenX(), event.getScreenY());
                        }
                    } else {
                        System.err.println("[NativeTag] Aviso: O menu de contexto com id '" + menuId + "' não foi encontrado no DOM.");
                    }
                }
            });
        }
    }

    // =========================================================================
    // ⭐ INSTINTOS NATURAIS DA TAG (User Agent Defaults)
    // =========================================================================

    /**
     * Define se a tag, por sua natureza semântica, deve tentar expandir-se no Layout do pai.
     * Ex: <tabs>, <editor>, <web>, <main> devem devolver TRUE.
     * Ex: <button>, <span>, <label> devem devolver FALSE.
     */
    public boolean isGreedyByDefault() {
        return false; // A maioria das tags só ocupa o espaço do seu conteúdo
    }

    /**
     * Injeta regras de CSS invisíveis antes do CSS do programador ser lido.
     */
    protected void applyUserAgentStyles() {
        // As tags filhas podem usar isto para definir o seu comportamento natural!
    }

    // ⭐ O TRADUTOR EXATO DE CORES (ARGB -> RGBA CSS)
    protected String toJavaFxCssColor(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        // JavaFX entende rgba(R, G, B, A) perfeitamente!
        return String.format(Locale.US, "rgba(%d, %d, %d, %.2f)", r, g, b, a / 255.0);
    }

    private String toHex(int argb) {
        return String.format("%08X", argb);
    }

    // ⭐ 1. O TRADUTOR DE GRADIENTES BLINDADO ⭐
    private String generateGradientCSS(Gradient gradient) {
        StringBuilder sb = new StringBuilder("linear-gradient(");

        // O JavaFX não entende '135.0deg' diretamente. Convertemos para direções!
        if (gradient.getDirection() != null) {
            sb.append(gradient.getDirection()).append(", ");
        } else {
            // Fallback inteligente para ângulos comuns
            float angle = gradient.getAngle();
            if (angle >= 100 && angle <= 170) sb.append("to bottom right, ");
            else if (angle >= 10 && angle <= 80) sb.append("to top right, ");
            else if (angle >= 180 && angle <= 260) sb.append("to bottom left, ");
            else sb.append("to bottom, "); // Padrão
        }

        for (int i = 0; i < gradient.getStops().size(); i++) {
            GradientStop stop = gradient.getStops().get(i);

            // A CURA: Usar o conversor RGBA seguro em vez de Hexadecimal!
            sb.append(toJavaFxCssColor(stop.getColorWithOpacity()));

            if (stop.getPosition() != null && !stop.getPosition().isAuto()) {
                sb.append(" ").append(stop.getPosition().toString());
            }
            if (i < gradient.getStops().size() - 1) sb.append(", ");
        }
        return sb.append(")").toString();
    }

    // ⭐ 2. O TRADUTOR DE BORDAS ASSIMÉTRICAS ⭐
    private String generateBorderCSS(Border border) {
        if (!border.hasBorder()) return "";

        StringBuilder sb = new StringBuilder();

        // Puxamos os 4 lados independentes que a tua arquitetura BoxSideSet já suporta!
        BorderSide t = border.getTop();
        BorderSide r = border.getRight();
        BorderSide b = border.getBottom();
        BorderSide l = border.getLeft();

        // 1. Cores (Cima Direita Baixo Esquerda)
        sb.append("-fx-border-color: ")
                .append(toJavaFxCssColor(t.getColor())).append(" ")
                .append(toJavaFxCssColor(r.getColor())).append(" ")
                .append(toJavaFxCssColor(b.getColor())).append(" ")
                .append(toJavaFxCssColor(l.getColor())).append("; ");

        // 2. Espessuras
        sb.append("-fx-border-width: ")
                .append(t.getWidth().getValue()).append("px ")
                .append(r.getWidth().getValue()).append("px ")
                .append(b.getWidth().getValue()).append("px ")
                .append(l.getWidth().getValue()).append("px; ");

        // 3. Estilos (solid, dashed, none, etc.)
        sb.append("-fx-border-style: ")
                .append(t.getStyle().name().toLowerCase()).append(" ")
                .append(r.getStyle().name().toLowerCase()).append(" ")
                .append(b.getStyle().name().toLowerCase()).append(" ")
                .append(l.getStyle().name().toLowerCase()).append("; ");

        return sb.toString();
    }

    // ⭐ 3. A SOMBRA DESBLOQUEADA ⭐
    private String generateBoxShadowCSS(List<BoxShadow> shadows) {
        if (shadows.isEmpty()) return "";
        BoxShadow shadow = shadows.getFirst();
        // Sintaxe restrita do JavaFX: dropshadow(blur-type, color, radius, spread, offsetX, offsetY)
        return "-fx-effect: dropshadow(gaussian, " + toJavaFxCssColor(shadow.getColor()) + ", " +
                shadow.getBlurRadius().getValue() + ", 0.0, " +
                shadow.getOffsetXPixels(cssContext) + ", " + shadow.getOffsetYPixels(cssContext) + "); ";
    }

    private void parseStyle(String styleStr) {
        if (styleStr == null) return;
        for (String part : styleStr.split(";")) {
            String[] kv = part.trim().split(":", 2);
            if (kv.length == 2) style.put(kv[0].trim().toLowerCase(), kv[1].trim());
        }
    }

    public String getId() { return id; }
    public Node getFxNode() { return fxNode; }
    public Map<String, String> getEvents() { return events; }
    public List<NativeTag> getChildren() { return children; }

    public String getClassName() {
        return className;
    }

    public Map<String, String> getStyle() {
        return style;
    }

    public XplNode getSourceNode() {
        return sourceNode;
    }

    public ResolvedStyles getResolvedStyles() {
        return resolvedStyles;
    }

    public CssContext getCssContext() {
        return cssContext;
    }

    // Adiciona junto aos outros Getters da classe NativeTag
    public Map<String, String> getRawStyles() {
        return style;
    }
}