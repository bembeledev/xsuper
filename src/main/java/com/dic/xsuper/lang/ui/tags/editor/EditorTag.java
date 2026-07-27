package com.dic.xsuper.lang.ui.tags.editor;

import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.ui.SuperUiEngine;
import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class EditorTag extends NativeTag {

    private StackPane container;
    private CodeArea editorArea;
    private GutterFactory gutterFactory;
    private SyntaxHighlighter syntaxHighlighter = new SyntaxHighlighter();
    private boolean isUpdatingFromEngine = false;
    private String dynamicCss = "";
    private final AtomicBoolean cssLoaded = new AtomicBoolean(false);

    public EditorTag(XplNode node) {
        super(node);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Node createNode() {
        container = new StackPane();
        editorArea = new CodeArea();

        // ─── Gutter ──────────────────────────────────────────────────────────
        gutterFactory = new GutterFactory(editorArea);
        boolean showLineNumbers = parseBoolean(sourceNode.attributes.get("line-numbers"), true);
        if (showLineNumbers) {
            gutterFactory.setLineNumberNodeFactory(LineNumberFactory.get(editorArea));
        }
        gutterFactory.registerProvider(new ColorPreviewProvider());
        editorArea.setParagraphGraphicFactory(gutterFactory);

        // ─── Configurações básicas ──────────────────────────────────────────
        editorArea.setEditable(parseBoolean(sourceNode.attributes.get("editable"), true));
        editorArea.setWrapText(parseBoolean(sourceNode.attributes.get("wrap-text"), false));

        int tabSize = parseInt(sourceNode.attributes.get("tab-size"), 4);
        boolean useSoftTabs = parseBoolean(sourceNode.attributes.get("soft-tabs"), true);

        // ─── Tabulação ──────────────────────────────────────────────────────
        editorArea.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.TAB) {
                e.consume();
                if (useSoftTabs) {
                    editorArea.replaceSelection(" ".repeat(Math.max(0, tabSize)));
                } else {
                    editorArea.replaceSelection("\t");
                }
            }
        });

        // ═══════════════════════════════════════════════════════════════════
        // ⭐ PASSO 1: Processar a sintaxe
        // ═══════════════════════════════════════════════════════════════════
        List<Map<String, Object>> syntaxRules = resolveSyntaxAttribute();
        if (syntaxRules != null && !syntaxRules.isEmpty()) {
            syntaxHighlighter.compileRules(syntaxRules);
            generateDynamicCss(syntaxRules);
            System.out.println("[EditorTag] Sintaxe compilada com " + syntaxRules.size() + " regras.");
        }

        // ═══════════════════════════════════════════════════════════════════
        // ⭐ PASSO 2: Aplicar o CSS (forçar inline)
        // ═══════════════════════════════════════════════════════════════════
        applyThemeInline();

        // ═══════════════════════════════════════════════════════════════════
        // ⭐ PASSO 3: Carregar conteúdo
        // ═══════════════════════════════════════════════════════════════════
        String initialContent = getInitialContent();
        if (initialContent != null && !initialContent.isEmpty()) {
            isUpdatingFromEngine = true;
            editorArea.replaceText(0, 0, initialContent);
            isUpdatingFromEngine = false;
        }

        // ═══════════════════════════════════════════════════════════════════
        // ⭐ PASSO 4: Forçar highlighting (com atraso para o CSS carregar)
        // ═══════════════════════════════════════════════════════════════════
        if (!editorArea.getText().isEmpty()) {
            javafx.application.Platform.runLater(() -> {
                applyHighlighting(editorArea.getText());
            });
        }

        // ─── ScrollPane ─────────────────────────────────────────────────────
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(editorArea);
        scrollPane.setMinSize(0, 0);
        scrollPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // ─── Listener de texto ─────────────────────────────────────────────
        editorArea.textProperty().addListener((obs, oldText, newText) -> {
            // Scanner de cores
            int currentLine = editorArea.getCurrentParagraph();
            MetadataScanner.scanLinesAsync(newText, currentLine);

            // Highlighting
            applyHighlighting(newText);

            // Two-way binding
            if (!isUpdatingFromEngine && SuperUiEngine.getInstance() != null &&
                    SuperUiEngine.getInstance().getRendererBridge() != null &&
                    sourceNode._internalUid != null) {
                SuperUiEngine.getInstance().getRendererBridge().updateProperty(
                        sourceNode._internalUid, "content", newText);
            }
        });

        // ─── Evento de metadata ────────────────────────────────────────────
        EditorEventBus.getInstance().getMetadataChangedEvent().addListener((obs, oldLine, newLine) -> {
            if (newLine != null && newLine >= 0 && newLine < editorArea.getParagraphs().size()) {
                editorArea.recreateParagraphGraphic(newLine);
            }
        });

        container.getChildren().add(scrollPane);
        container.setMinSize(0, 0);
        container.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        applyCommonStyles();
        return container;
    }

    // ─── Resolução do atributo `syntax` ──────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveSyntaxAttribute() {
        Object syntaxAttr = sourceNode.attributes.get("syntax");
        if (syntaxAttr == null) return null;

        if (syntaxAttr instanceof List) {
            return (List<Map<String, Object>>) syntaxAttr;
        }

        if (syntaxAttr instanceof String) {
            String str = (String) syntaxAttr;
            // Tenta resolver a variável no interpretador
            Interpreter interpreter = SuperUiEngine.getInstance().getInterpreter();
            if (interpreter != null) {
                try {
                    Object resolved = interpreter.environment.get(str);
                    if (resolved instanceof List) {
                        return (List<Map<String, Object>>) resolved;
                    }
                } catch (Exception e) {
                    System.err.println("[EditorTag] Erro ao resolver '" + str + "': " + e.getMessage());
                }
            }
        }
        return null;
    }

    // ─── Geração de CSS dinâmico ─────────────────────────────────────────
    private void generateDynamicCss(List<Map<String, Object>> rules) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> rule : rules) {
            String cssClass = String.valueOf(rule.get("classe"));
            String color = rule.containsKey("color") ? String.valueOf(rule.get("color")) : null;
            boolean isBold = rule.containsKey("bold") && Boolean.parseBoolean(String.valueOf(rule.get("bold")));
            boolean isItalic = rule.containsKey("italic") && Boolean.parseBoolean(String.valueOf(rule.get("italic")));

            // ⭐ CORREÇÃO: Aumentar especificidade para vencer .styled-text-area .text
            sb.append(".styled-text-area .text.").append(cssClass).append(" {\n");
            if (color != null) sb.append("    -fx-fill: ").append(color).append(";\n");
            if (isBold) sb.append("    -fx-font-weight: bold;\n");
            if (isItalic) sb.append("    -fx-font-style: italic;\n");
            sb.append("}\n");
        }
        dynamicCss = sb.toString();
        System.out.println("[EditorTag] CSS dinâmico gerado:\n" + dynamicCss);
    }

    // ─── Aplicação do tema (FORÇADO) ─────────────────────────────────────

    private void applyThemeInline() {
        String bgColor = resolvedStyles.backgroundColor != 0x00000000 ?
                toJavaFxCssColor(resolvedStyles.backgroundColor) : "#1e1e1e";
        String textColor = resolvedStyles.textColor != 0xFF000000 ?
                toJavaFxCssColor(resolvedStyles.textColor) : "#cccccc";
        String gutterBgColor = "#181818";
        String lineNumberColor = "#6e7681";

        // ⭐ O TRUQUE VISUAL: Reservamos exatos 55 pixels de largura para a margem
        int gutterWidth = 55;

        // O linear-gradient divide o fundo do editor inteiro em duas cores verticais perfeitas!
        String fullCss = String.format("""
            .styled-text-area { 
                -fx-background-color: linear-gradient(to right, %s 0px, %s %dpx, %s %dpx, %s 100%%); 
                -fx-padding: 0px; 
            }
            .styled-text-area .text { -fx-fill: %s; }
            .caret { -fx-stroke: %s; }
            .styled-text-area:focused { -fx-background-insets: 0; }
            
            .superui-gutter {
                -fx-background-color: transparent; /* A cor vem agora do gradient atrás! */
                -fx-min-width: %dpx;
                -fx-max-width: %dpx;
                -fx-padding: 0 12px 0 5px;
                -fx-spacing: 5px;
                -fx-border-color: transparent #2d2d2d transparent transparent;
                -fx-border-width: 0 1px 0 0;
            }
            
            .lineno {
                -fx-background-color: transparent; 
                -fx-text-fill: %s; 
                -fx-font-family: 'Consolas', monospace;
                -fx-font-size: 13px;
                -fx-alignment: center-right;
                -fx-min-width: 35px; 
                -fx-padding: 0 5px 0 0; 
            }
            
            .superui-color-box {
                -fx-stroke: #454545; 
                -fx-stroke-width: 1px;
            }
            
            %s
            """,
                gutterBgColor, gutterBgColor, gutterWidth, bgColor, gutterWidth, bgColor, // Cores do Gradient
                textColor, textColor,
                gutterWidth, gutterWidth, // Largura fixa da Margem
                lineNumberColor, dynamicCss);

        // Injetar via stylesheet
        String encodedCss = Base64.getEncoder().encodeToString(fullCss.getBytes());
        editorArea.getStylesheets().clear();
        editorArea.getStylesheets().add("data:text/css;base64," + encodedCss);
        editorArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 14px;");

        System.out.println("[EditorTag] Tema aplicado com CSS dinâmico e margem infinita.");
        cssLoaded.set(true);
    }

    // ─── Highlighting em background ──────────────────────────────────────
    private void applyHighlighting(String text) {
        if (text == null || text.isEmpty()) return;

        // Se o CSS ainda não foi carregado, esperar um pouco
        if (!cssLoaded.get()) {
            javafx.application.Platform.runLater(() -> applyHighlighting(text));
            return;
        }

        javafx.concurrent.Task<StyleSpans<java.util.Collection<String>>> task =
                new javafx.concurrent.Task<>() {
                    @Override
                    protected StyleSpans<java.util.Collection<String>> call() {
                        return syntaxHighlighter.computeHighlighting(text);
                    }
                };
        task.setOnSucceeded(e -> {
            if (editorArea != null) {
                StyleSpans<java.util.Collection<String>> spans = task.getValue();
                if (spans != null) {
                    editorArea.setStyleSpans(0, spans);
                    System.out.println("[EditorTag] Highlighting aplicado: " + spans.getSpanCount() + " spans.");
                } else {
                    System.out.println("[EditorTag] Nenhum span de highlighting gerado.");
                }
            }
        });
        task.setOnFailed(e -> {
            System.err.println("[EditorTag] Erro no highlighting: " + task.getException().getMessage());
        });
        new Thread(task).start();
    }

    // ─── Métodos auxiliares ──────────────────────────────────────────────

    private boolean parseBoolean(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        return "true".equalsIgnoreCase(value.toString());
    }

    private int parseInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    private String getInitialContent() {
        if (sourceNode.attributes.containsKey("content")) {
            return String.valueOf(sourceNode.attributes.get("content"));
        }
        if (sourceNode.attributes.containsKey("code")) {
            return String.valueOf(sourceNode.attributes.get("code"));
        }
        if (sourceNode.textContent != null && !sourceNode.textContent.trim().isEmpty()) {
            return sourceNode.textContent;
        }
        return "";
    }

    // ─── Reactividade ─────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public void onReactiveAttributeChange(String attrName, Object newValue) {
        super.onReactiveAttributeChange(attrName, newValue);

        javafx.application.Platform.runLater(() -> {
            switch (attrName.toLowerCase()) {
                case "content":
                case "code":
                    String newContent = String.valueOf(newValue);
                    if (!editorArea.getText().equals(newContent)) {
                        isUpdatingFromEngine = true;
                        int caretPosition = editorArea.getCaretPosition();
                        editorArea.replaceText(newContent);
                        if (caretPosition <= editorArea.getLength()) {
                            editorArea.moveTo(caretPosition);
                        }
                        isUpdatingFromEngine = false;
                        applyHighlighting(newContent);
                    }
                    break;
                case "editable":
                    editorArea.setEditable(parseBoolean(newValue, true));
                    break;
                case "wrap-text":
                    editorArea.setWrapText(parseBoolean(newValue, false));
                    break;
                case "syntax":
                    if (newValue instanceof List) {
                        List<Map<String, Object>> rules = (List<Map<String, Object>>) newValue;
                        syntaxHighlighter.compileRules(rules);
                        generateDynamicCss(rules);
                        applyThemeInline();
                        String currentText = editorArea.getText();
                        if (!currentText.isEmpty()) {
                            applyHighlighting(currentText);
                        }
                    }
                    break;
            }
        });
    }

    // =========================================================================
    // ⭐ MENU DE CONTEXTO ESPECÍFICO DO EDITOR (Preserva a Seleção)
    // =========================================================================
    @Override
    protected void bindContextMenu() {
        if (sourceNode.attributes == null) return;
        Object menuIdAttr = sourceNode.attributes.get("context-menu");

        if (menuIdAttr != null) {
            String menuId = menuIdAttr.toString();

            // Anexamos o ouvinte DIRETAMENTE na CodeArea e não no StackPane pai!
            editorArea.setOnContextMenuRequested(event -> {
                event.consume(); // ⭐ Impede que o editor perca a seleção de texto!

                com.dic.xsuper.lang.ui.SuperUiEngine engine = com.dic.xsuper.lang.ui.SuperUiEngine.getInstance();
                if (engine != null && engine.getActiveDom() != null) {
                    com.dic.xsuper.lang.ui.html.XplNode menuVirtualNode = engine.getActiveDom().getElementById(menuId);

                    if (menuVirtualNode != null && menuVirtualNode.nativeNode instanceof javafx.scene.Node dummyNode) {
                        Object rawMenu = dummyNode.getProperties().get("xpl_context_menu");
                        if (rawMenu instanceof javafx.scene.control.ContextMenu ctxMenu) {
                            ctxMenu.show(editorArea, event.getScreenX(), event.getScreenY());
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void applyTagSpecificStyles() {}

    @Override
    protected void addChildren() {}

    @Override
    public boolean isGreedyByDefault() { return true; }
}