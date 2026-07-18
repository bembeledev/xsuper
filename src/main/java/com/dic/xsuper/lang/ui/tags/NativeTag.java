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
        this.cssContext = new CssContext()
                .withFontSize((float) resolvedStyles.fontSize.toPixels(new CssContext()))
                .withParentSize(800)
                .withViewport(800, 600);

        for (XplNode childNode : node.children) {
            NativeTag childTag = TagFactory.create(childNode);
            if (childTag != null) this.children.add(childTag);
        }
    }

    public Node build() {
        fxNode = createNode();
        applyCommonStyles();
        applyTagSpecificStyles();
        // Usamos new HashMap<> para compatibilidade entre Map<String, String> e Map<String, Object>
        StyleTransformUtils.applyTransforms(fxNode, new HashMap<>(this.style));
        addChildren();
        bindEvents();
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
        // ⭐ 4. O GATILHO DAS ANIMAÇÕES (Coloca aqui!)
        // ==========================================================

        if (styles.containsKey("animation")) {
            String animationConfig = styles.get("animation");
            // Ex: "fadeIn 2s ease-in-out forwards"
            String[] parts = animationConfig.trim().split("\\s+");

            if (parts.length > 0) {
                String animName = parts[0];

                // Vai buscar a planta ao registo da Engine
                XplKeyframeAnimation anim = SuperUiEngine.getInstance().getKeyframe(animName);

                if (anim != null) {
                    // Constrói os overrides (duração, easing, etc.) a partir da string
                    Map<String, String> overrides = extractAnimationOverrides(parts);

                    // Dispara o motor!
                    com.dic.xsuper.lang.ui.animation.XplAnimationEngine.applyKeyframeAnimation(fxNode, anim, overrides);
                } else {
                    System.err.println("[TagFactory] ⚠️ Animação não encontrada no CSS global: " + animName);
                }
            }
        }

        if (overflowY.equals("auto") || overflowY.equals("scroll") ||
                overflowX.equals("auto") || overflowX.equals("scroll")) {
            return wrapInWebScroll(fxNode, overflowX, overflowY, styles);
        }



        return fxNode;
    }

    /**
     * Lê as partes da string de 'animation' e tenta descobrir o que é duração, fill-mode, iterações, etc.
     */
    private Map<String, String> extractAnimationOverrides(String[] parts) {
        Map<String, String> overrides = new java.util.HashMap<>();

        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].toLowerCase();

            // É tempo? (Duração)
            if (p.endsWith("ms") || p.endsWith("s")) {
                // Se já tivermos duração, poderíamos assumir delay, mas vamos focar na duração principal
                if (!overrides.containsKey("duration")) overrides.put("duration", p);
            }
            // É fill-mode?
            else if (p.equals("forwards") || p.equals("backwards") || p.equals("both") || p.equals("none")) {
                overrides.put("fill-mode", p);
            }
            // É direção?
            else if (p.equals("normal") || p.equals("reverse") || p.equals("alternate") || p.equals("alternate-reverse")) {
                overrides.put("direction", p);
            }
            // É iterações?
            else if (p.equals("infinite")) {
                overrides.put("iterations", "-1");
            } else if (p.matches("\\d+")) {
                overrides.put("iterations", p);
            }
            // (Opcional: podes mapear o Easing aqui também, se precisares de o substituir)
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

    protected void addChildren() {
        if (fxNode instanceof javafx.scene.layout.Pane pane) {

            // ⭐ A CURA DO TEXTO PERDIDO ⭐
            // Se o contentor tiver texto livre, o JavaFX precisa de um Label fantasma para o desenhar!
            if (sourceNode.textContent != null && !sourceNode.textContent.trim().isEmpty()) {
                javafx.scene.control.Label inlineText = new javafx.scene.control.Label(sourceNode.textContent.trim());

                // Transfere as regras de texto (Cor, Fonte) do contentor para o texto injetado!
                if (resolvedStyles.textColor != 0xFF000000) {
                    inlineText.setStyle("-fx-text-fill: " + toJavaFxCssColor(resolvedStyles.textColor) + ";");
                }
                if (resolvedStyles.fontSize != null && resolvedStyles.fontSize.toPixels(cssContext) > 0) {
                    inlineText.setStyle(inlineText.getStyle() + "-fx-font-size: " + resolvedStyles.fontSize.toPixels(cssContext) + "px; ");
                }

                pane.getChildren().add(inlineText);
            }

            // ⭐ 1. LÓGICA DO GRID (Calcula colunas automaticamente!)
            switch (fxNode) {

                case CustomLayoutPane customPane -> {
                    customPane.populateChildren(children, cssContext);
                }
                case null, default -> {
                    // StackPane e outros
                    for (NativeTag child : children) {
                        Node childNode = child.build();
                        applyJavaFxMargins(childNode, child); // ⭐ Aplica margem!
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

            // ⭐ A CURA PARA O WIDTH: 100%
            String widthStr = style.get("width");
            if ("100%".equals(widthStr)) {
                region.setMaxWidth(Double.MAX_VALUE); // Força a expansão máxima permitida pelo pai
                region.setPrefWidth(2000); // Hack visual para garantir stretch
            } else {
                float w = resolvedStyles.boxSize.getWidthPixels(cssContext);
                if (w > 0) css.append("-fx-pref-width: ").append(w).append("px; ");
            }

            float h = resolvedStyles.boxSize.getHeightPixels(cssContext);
            if (h > 0) css.append("-fx-pref-height: ").append(h).append("px; ");

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

        if (!css.isEmpty()) {
            fxNode.setStyle(fxNode.getStyle() + (fxNode.getStyle().isEmpty() ? "" : "; ") + css.toString());
        }
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