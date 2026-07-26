package com.dic.xsuper.lang.ui;

import com.dic.xsuper.lang.ui.animation.XplTransition;
import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.tags.TagFactory;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * O Renderizador Oficial da SuperUI com VIRTUAL DOM DIFFING.
 */
public class JavaFxRenderer implements XplUiBridge {

    private final Pane windowRoot;
    private EngineCallback engineCallback;
    private final Map<String, Node> fxNodeRegistry = new HashMap<>();

    // ⭐ O ESTADO DA ÁRVORE ANTERIOR (Memória VDOM)
    private XplNode currentDomRoot = null;

    // No topo do JavaFxRenderer.java
    private final Map<String, NativeTag> tagRegistry = new HashMap<>();

    public JavaFxRenderer(Pane windowRoot) {
        this.windowRoot = windowRoot;
    }

    @Override
    public void setEngineCallback(EngineCallback callback) {
        this.engineCallback = callback;
    }

    // Em JavaFxRenderer.java
    @Override
    public void rebuildFullView(String targetUid, XplNode virtualNode) {
        Platform.runLater(() -> {
            // 1. Encontra a caixa antiga na tela
            Node oldFxNode = fxNodeRegistry.get(targetUid);
            if (oldFxNode == null) return;

            javafx.scene.Parent parent = oldFxNode.getParent();

            if (parent instanceof Pane parentPane) {
                int index = parentPane.getChildren().indexOf(oldFxNode);
                if (index == -1) return;

                // 2. Limpa a memória velha
                fxNodeRegistry.remove(targetUid);

                // ⭐ 3. CRIA A CAIXA NOVA INTEIRA (Com todas as regras do TagFactory, Margens e Textos!)
                NativeTag newTag = TagFactory.create(virtualNode);
                if (newTag != null) {
                    Node newFxNode = newTag.build();
                    virtualNode.nativeNode = newFxNode;

                    registerNodeRecursively(newTag);

                    // 4. Substituição cirúrgica da caixa no pai
                    parentPane.getChildren().set(index, newFxNode);
                    parentPane.requestLayout(); // Garante o alinhamento instantâneo
                }
            }
        });
    }

    @Override
    public void invokeMethodOnNode(String targetId, String methodName, Object... args) {
        Platform.runLater(() -> {
            NativeTag tag = tagRegistry.get(targetId);
            if (tag != null) {
                tag.invokeMethod(methodName, args); // O Polimorfismo faz o resto!
            } else {
                System.err.println("[Renderer] Elemento não encontrado para invocar o método: " + targetId);
            }
        });
    }

    // =====================================================================
    // 1. O MOTOR DE RENDERIZAÇÃO E RECONCILIAÇÃO
    // =====================================================================
    @Override
    public void renderView(XplNode newDomRoot) {

        Platform.runLater(() -> {
            try {

                if (currentDomRoot == null) {

                    windowRoot.getChildren().clear();
                    fxNodeRegistry.clear();

                    for (XplNode xplChild : newDomRoot.children) {
                        NativeTag tag = TagFactory.create(xplChild);
                        if (tag != null) {
                            Node fxNode = tag.build();
                            xplChild.nativeNode = fxNode;
                            registerNodeRecursively(tag);
                            windowRoot.getChildren().add(fxNode);
                        }
                    }
                } else {
                    reconcileChildren(currentDomRoot, newDomRoot, windowRoot);
                }

                // Atualiza a árvore em memória para o próximo ciclo
                currentDomRoot = newDomRoot;

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }



    /**
     * HARD REFLOW: Usado EXCLUSIVAMENTE para Media Queries e Responsividade.
     * Destrói a árvore física e recria as instâncias nativas corretas (HBox <-> VBox).
     */
    public void rebuildFullView(XplNode updatedDom) {
        Platform.runLater(() -> {
            // 1. Incinera a árvore visual antiga do JavaFX e limpa a memória!
            this.windowRoot.getChildren().clear();
            this.fxNodeRegistry.clear();
            this.currentDomRoot = updatedDom;

            // 2. RECRIACÃO TOTAL!
            for (XplNode child : updatedDom.children) {
                NativeTag tag = TagFactory.create(child);
                if (tag != null) {
                    Node fxNode = tag.build();
                    child.nativeNode = fxNode;

                    // Fundamental para não perderes os eventos (cliques, inputs) após o reflow!
                    registerNodeRecursively(tag);

                    this.windowRoot.getChildren().add(fxNode);
                }
            }

            System.out.println("[Renderer] 🏗️ Hard Reflow (Responsividade) concluído com sucesso!");
        });
    }

    // =====================================================================
    // 2. ALGORITMO DE DIFFING (O Coração do React/Vue)
    // =====================================================================
    private void reconcileChildren(XplNode oldParent, XplNode newParent, Pane fxParentContainer) {
        int oldSize = oldParent.children != null ? oldParent.children.size() : 0;
        int newSize = newParent.children != null ? newParent.children.size() : 0;
        int minSize = Math.min(oldSize, newSize);

        // 1. ACTUALIZAR nós que existem em ambas as árvores
        for (int i = 0; i < minSize; i++) {
            XplNode oldChild = oldParent.children.get(i);
            XplNode newChild = newParent.children.get(i);

            if (!oldChild.tag.equalsIgnoreCase(newChild.tag)) {
                // A Tag mudou (ex: de <span> para <div>). Destruir o antigo e criar novo!
                NativeTag tag = TagFactory.create(newChild);
                if (tag != null) {
                    Node newFxNode = tag.build();
                    newChild.nativeNode = newFxNode;
                    registerNodeRecursively(tag);
                    fxParentContainer.getChildren().set(i, newFxNode); // Substitui cirurgicamente
                }
            } else {
                // ⭐ A TAG É A MESMA: Fazer o PATCH das diferenças (Sem destruir nada!)
                Node fxNode = (Node) oldChild.nativeNode;
                newChild.nativeNode = fxNode; // Passar a âncora para a nova geração

                patchNodeProperties(oldChild, newChild, fxNode);

                // Mergulhar na árvore e reconciliar os filhos
                if (fxNode instanceof Pane childContainer) {
                    reconcileChildren(oldChild, newChild, childContainer);
                }
            }
        }

        // 2. ADICIONAR novos nós que surgiram (ex: novos itens num @for)
        if (newSize > oldSize) {
            for (int i = oldSize; i < newSize; i++) {
                XplNode newChild = newParent.children.get(i);
                NativeTag tag = TagFactory.create(newChild);
                if (tag != null) {
                    Node fxNode = tag.build();
                    newChild.nativeNode = fxNode;
                    registerNodeRecursively(tag);
                    fxParentContainer.getChildren().add(fxNode);
                }
            }
        }

        // 3. REMOVER nós que desapareceram
        if (oldSize > newSize) {
            // Removemos do fim para o início para não desalinhar os índices da lista
            for (int i = oldSize - 1; i >= newSize; i--) {
                fxParentContainer.getChildren().remove(i);

            }
        }
    }

    private void patchNodeProperties(XplNode oldNode, XplNode newNode, Node fxNode) {
        if (fxNode == null) return;

        // A. Diff de Texto
        if (!Objects.equals(oldNode.textContent, newNode.textContent)) {
            if (fxNode instanceof Labeled labeled) {
                labeled.setText(newNode.textContent != null ? newNode.textContent : "");
            } else if (fxNode instanceof TextInputControl input) {
                input.setText(newNode.textContent != null ? newNode.textContent : "");
            }
        }

        // B. Diff de Classes
        String oldClass = oldNode.attributes.getOrDefault("class", "").toString();
        String newClass = newNode.attributes.getOrDefault("class", "").toString();

        if (!oldClass.equals(newClass)) {
            applyRawStyleToFxNode(fxNode, "class", newClass, newClass);
        }

        // =========================================================================
        // ⭐ C. DIFF DE ESTILOS COMPUTADOS E TRANSIÇÕES (A peça em falta!)
        // =========================================================================
        String transitionConfig = newNode.style.get("transition");
        if (transitionConfig == null) {
            transitionConfig = (String) fxNode.getProperties().get("transition");
        }

        // 🧹 1. O EXTERMINADOR DE ESTILOS: Remove CSS que o nó antigo tinha mas o novo perdeu
        for (String oldProp : oldNode.style.keySet()) {
            if (!newNode.style.containsKey(oldProp)) {
                // Passar uma string vazia força os teus switchs a reverterem a propriedade!
                applyRawStyleToFxNode(fxNode, oldProp, "", null);
            }
        }

        // 🎨 2. APLICAÇÃO CIRÚRGICA: Varre os estilos novos e aplica as diferenças
        for (Map.Entry<String, String> newEntry : newNode.style.entrySet()) {
            String prop = newEntry.getKey().toLowerCase();
            String newVal = newEntry.getValue();
            String oldVal = oldNode.style.get(prop);

            if (!java.util.Objects.equals(oldVal, newVal)) {
                // A propriedade visual mudou (Ex: background-color red -> blue)
                if (transitionConfig != null && (transitionConfig.contains(prop) || transitionConfig.contains("all"))) {
                    String physicalOldVal = oldVal != null ? oldVal : getCurrentFxPropertyValue(fxNode, prop);
                    com.dic.xsuper.lang.ui.animation.XplAnimationEngine.applyTransition(
                            fxNode, prop, physicalOldVal, newVal,
                            new com.dic.xsuper.lang.ui.animation.XplTransition(transitionConfig)
                    );
                } else {
                    // Se não tiver transição, aplica diretamente
                    applyRawStyleToFxNode(fxNode, prop, newVal, newVal);
                }
            }
        }


        // 1. Recupera a tag inteligente DIRETAMENTE da memória física do ecrã
        NativeTag targetTag = (NativeTag) fxNode.getProperties().get("xpl_native_tag");
        // D. DIFF GENÉRICO DE ATRIBUTOS
        for (Map.Entry<String, Object> newAttr : newNode.attributes.entrySet()) {
            String attrName = newAttr.getKey().toLowerCase();
            Object newVal = newAttr.getValue();
            Object oldVal = oldNode.attributes.get(attrName);

            if (!java.util.Objects.equals(oldVal, newVal)) {

                // 2. AVISA A TAG IMEDIATAMENTE (Isto vai acordar o DialogTag!)
                if (targetTag != null) {
                    targetTag.onReactiveAttributeChange(attrName, newVal);
                }

                String valStr = newVal != null ? newVal.toString() : "";
                switch (attrName) {
                    case "disabled" -> fxNode.setDisable(Boolean.parseBoolean(valStr) || valStr.isEmpty());
                    case "value" -> applyRawStyleToFxNode(fxNode, "value", valStr, newVal);
                    case "checked" -> applyRawStyleToFxNode(fxNode, "checked", valStr, newVal);
                }
            }
        }
    }

    private void unregisterNodeRecursively(XplNode node) {
        if (node == null) return;
        fxNodeRegistry.remove(node.id);
        fxNodeRegistry.remove(node._internalUid);
        for (XplNode child : node.children) {
            unregisterNodeRecursively(child);
        }
    }

    // =====================================================================
    // 4. REGISTO RECURSIVO DE IDs E EVENTOS (O Teu Código Original)
    // =====================================================================
    private void registerNodeRecursively(NativeTag tag) {
        Node fxNode = tag.getFxNode();
        if (fxNode != null) {
            String id = tag.getId();
            if (id != null && !id.isEmpty()) {
                fxNodeRegistry.put(id, fxNode);
                tagRegistry.put(id, tag);
            }

            // ⭐ Lê a matrícula secreta gerada no XplNode/XplElement
            String secretUid = tag.getSourceNode()._internalUid;

            if (secretUid != null) {
                // A caixa gráfica fica registada no dicionário com a chave "node_X"
                fxNodeRegistry.put(secretUid, fxNode);
                tagRegistry.put(secretUid, tag);
            }

        }
        for (NativeTag child : tag.getChildren()) {
            registerNodeRecursively(child);
        }
    }

    // =====================================================================
    // 3. ATUALIZAÇÃO CIRÚRGICA (Alta Performance - Canal Expresso)
    // =====================================================================
    @Override
    public void updateProperty(String nodeId, String propertyName, Object newValue) {
        Platform.runLater(() -> {
            Node fxNode = fxNodeRegistry.get(nodeId);
            if (fxNode == null) return;

            // ⭐ 1. AVISAR A TAG INTELIGENTE (SRP Aplicado!)
            NativeTag tag = tagRegistry.get(nodeId);
            if (tag != null) {
                tag.onReactiveAttributeChange(propertyName, newValue);
            }

            String valStr = newValue != null ? newValue.toString() : "";
            String prop = propertyName.toLowerCase();

            // 1. Vai buscar a transição guardada (Se existir)
            String transitionConfig = (String) fxNode.getProperties().get("transition");

            // 2. ⭐ INTERCETOR DE ANIMAÇÕES
            if (transitionConfig != null && (transitionConfig.contains(prop) || transitionConfig.contains("all"))) {
                // TEM TRANSIÇÃO!
                XplTransition trans = new XplTransition(transitionConfig);

                // Lê o valor físico exato do nó no ecrã antes de mudar
                String oldValue = getCurrentFxPropertyValue(fxNode, prop);

                // Chama o motor de animações para fazer a interpolação!
                com.dic.xsuper.lang.ui.animation.XplAnimationEngine.applyTransition(
                        fxNode, prop, oldValue, valStr, trans
                );

            } else {
                // NÃO TEM TRANSIÇÃO. Aplica a mudança instantânea.
                applyRawStyleToFxNode(fxNode, prop, valStr, newValue);
            }
        });
    }

    // =====================================================================
    // 🛠️ HELPER: LER O VALOR FÍSICO ATUAL DO JAVAFX
    // =====================================================================
    private String getCurrentFxPropertyValue(Node fxNode, String prop) {
        return switch (prop.toLowerCase()) {
            case "opacity" -> String.valueOf(fxNode.getOpacity());
            case "translatex" -> String.valueOf(fxNode.getTranslateX());
            case "translatey" -> String.valueOf(fxNode.getTranslateY());
            case "scalex" -> String.valueOf(fxNode.getScaleX());
            case "scaley" -> String.valueOf(fxNode.getScaleY());
            case "rotate" -> String.valueOf(fxNode.getRotate());
            default -> "0"; // Valor seguro de fallback
        };
    }

    // =====================================================================
    // 🛠️ HELPER: APLICADOR BRUTO (O teu antigo switch gigante)
    // =====================================================================
    private void applyRawStyleToFxNode(Node fxNode, String prop, String valStr, Object newValue) {
        switch (prop) {
            // ─── ESTADOS BOOLEANOS ──────────────────────────────────────
            case "disabled" -> fxNode.setDisable(Boolean.parseBoolean(valStr) || valStr.equals(""));

            case "checked" -> {
                boolean isChecked = Boolean.parseBoolean(valStr) || valStr.equals("");
                if (fxNode instanceof javafx.scene.control.CheckBox cb) cb.setSelected(isChecked);
                else if (fxNode instanceof javafx.scene.control.RadioButton rb) rb.setSelected(isChecked);
                else if (fxNode instanceof javafx.scene.control.ToggleButton tb) tb.setSelected(isChecked);
            }

            case "visible" -> {
                boolean isVisible = Boolean.parseBoolean(valStr);
                fxNode.setVisible(isVisible);
                fxNode.setManaged(isVisible);
            }

            // ─── CONTEÚDO E VALORES ─────────────────────────────────────
            case "value" -> {
                if (fxNode instanceof javafx.scene.control.TextInputControl input) {
                    // ⭐ SÓ ATUALIZA SE O TEXTO FOR DIFERENTE (Protege o cursor!)
                    if (!input.getText().equals(valStr)) {
                        input.setText(valStr);
                    }
                } else if (fxNode instanceof javafx.scene.control.ComboBox combo) {
                    if (combo.getValue() == null || !combo.getValue().toString().equals(valStr)) {
                        combo.setValue(newValue);
                    }
                } else if (fxNode instanceof javafx.scene.control.Slider slider) {
                    try { slider.setValue(Double.parseDouble(valStr)); } catch (Exception ignored) {}
                } else if (fxNode instanceof javafx.scene.control.Spinner spinner) {
                    // ⭐ A CURA DO SPINNER (INPUT NUMBER) ⭐
                    try {
                        double parsed = Double.parseDouble(valStr);
                        if (spinner.getValue() == null || !spinner.getValue().toString().equals(String.valueOf(parsed))) {
                            spinner.getValueFactory().setValue(parsed);
                        }

                        // Só atualiza o Editor se for NUMERICAMENTE diferente!
                        // (Impede de apagar "10." e voltar para "10.0" enquanto o user digita decimais)
                        String editorText = spinner.getEditor().getText();
                        boolean shouldUpdateText = true;
                        try {
                            if (Double.parseDouble(editorText) == parsed) {
                                shouldUpdateText = false; // Já é igual, deixa o cursor em paz!
                            }
                        } catch(Exception e) {}

                        if (shouldUpdateText && !editorText.equals(valStr)) {
                            spinner.getEditor().setText(valStr);
                        }
                    } catch (Exception ignored) {}
                }
            }

            case "text", "textcontent", "innerhtml" -> {
                if (fxNode instanceof javafx.scene.control.Labeled labeled) {
                    labeled.setText(valStr);
                } else if (fxNode instanceof javafx.scene.control.TextInputControl input) {
                    input.setText(valStr);
                } else if (fxNode instanceof javafx.scene.layout.Pane pane) {
                    // 👻 CAÇA AO FANTASMA: Procura o Label escondido e atualiza-o!
                    for (Node child : pane.getChildren()) {
                        if (Boolean.TRUE.equals(child.getProperties().get("xpl_ghost_text"))) {
                            ((javafx.scene.control.Label) child).setText(valStr);
                            break;
                        }
                    }
                }
            }

            // ─── ESTILO E APARÊNCIA ─────────────────────────────────────
            case "style" -> fxNode.setStyle(valStr);

            case "class", "classname" -> {
                fxNode.getStyleClass().clear();
                if (!valStr.isEmpty()) {
                    fxNode.getStyleClass().addAll(valStr.split("\\s+"));
                }
            }

            // ─── PROPRIEDADES CSS COMUNS ────────────────────────────────
            case "width" -> setSize(fxNode, "width", valStr);
            case "height" -> setSize(fxNode, "height", valStr);
            case "maxwidth" -> setSize(fxNode, "maxWidth", valStr);
            case "maxheight" -> setSize(fxNode, "maxHeight", valStr);
            case "minwidth" -> setSize(fxNode, "minWidth", valStr);
            case "minheight" -> setSize(fxNode, "minHeight", valStr);
            case "prefwidth" -> setSize(fxNode, "prefWidth", valStr);
            case "prefheight" -> setSize(fxNode, "prefHeight", valStr);

            case "margin", "padding" -> applyInsets(fxNode, prop, valStr);
            case "background", "background-color" -> applyBackground(fxNode, valStr);
            case "color", "text-fill" -> applyTextFill(fxNode, valStr);

            case "font-size" -> applyFontSize(fxNode, valStr);
            case "font-family" -> applyFontFamily(fxNode, valStr);
            case "font-weight" -> applyFontWeight(fxNode, valStr);
            case "font-style" -> applyFontStyle(fxNode, valStr);
            case "text-align" -> applyTextAlign(fxNode, valStr);
            case "text-decoration" -> applyTextDecoration(fxNode, valStr);
            case "border-radius" -> applyBorderRadius(fxNode, valStr);

            case "transform" -> applyTransform(fxNode, valStr);
            case "opacity" -> {
                try { fxNode.setOpacity(Double.parseDouble(valStr)); } catch (Exception ignored) {}
            }

            case "display" -> applyDisplay(fxNode, valStr);
            case "flex-direction" -> applyFlexDirection(fxNode, valStr);
            case "align-items" -> applyAlignment(fxNode, "align-items", valStr);
            case "justify-content" -> applyAlignment(fxNode, "justify-content", valStr);
            case "gap" -> applyGap(fxNode, valStr);

            case "src" -> {
                if (fxNode instanceof javafx.scene.image.ImageView iv) {
                    try {
                        iv.setImage(new javafx.scene.image.Image(valStr, true));
                    } catch (Exception e) {
                        System.err.println("[JavaFxRenderer] Erro ao carregar src: " + valStr);
                    }
                }
            }

            default -> {
                fxNode.getProperties().put(prop, newValue);
            }
        }
    }
    // ─── MÉTODOS AUXILIARES PARA APLICAÇÃO DE ESTILOS ──────────────────────

    private void setSize(Node node, String sizeType, String value) {
        if (!(node instanceof javafx.scene.layout.Region region)) return;
        double val = parseSize(value);
        if (Double.isNaN(val)) return;
        switch (sizeType) {
            case "width" -> region.setPrefWidth(val);
            case "height" -> region.setPrefHeight(val);
            case "maxWidth" -> region.setMaxWidth(val);
            case "maxHeight" -> region.setMaxHeight(val);
            case "minWidth" -> region.setMinWidth(val);
            case "minHeight" -> region.setMinHeight(val);
            case "prefWidth" -> region.setPrefWidth(val);
            case "prefHeight" -> region.setPrefHeight(val);
        }
    }

    private double parseSize(String value) {
        if (value == null || value.isEmpty()) return Double.NaN;
        value = value.trim();
        // Remove unidades (px, em, %) e converte para double
        value = value.replaceAll("[^0-9.\\-]", "");
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    // ─── GESTOR INTELIGENTE DE ESTILOS JAVAFX ───────────────────────────────
    private void updateFxStyle(Node node, String fxProperty, String value) {
        String currentStyle = node.getStyle();
        if (currentStyle == null) currentStyle = "";

        // 1. Converte o estilo atual num mapa
        Map<String, String> styleMap = new java.util.LinkedHashMap<>();
        for (String rule : currentStyle.split(";")) {
            if (rule.trim().isEmpty()) continue;
            String[] kv = rule.split(":", 2);
            if (kv.length == 2) {
                styleMap.put(kv[0].trim(), kv[1].trim());
            }
        }

        // 2. Atualiza ou remove a propriedade
        if (value == null || value.isEmpty() || value.equals("none")) {
            styleMap.remove(fxProperty);
        } else {
            styleMap.put(fxProperty, value);
        }

        // 3. Reconstrói a string de estilo limpa
        StringBuilder newStyle = new StringBuilder();
        for (Map.Entry<String, String> entry : styleMap.entrySet()) {
            newStyle.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
        }

        node.setStyle(newStyle.toString());
    }

    private void applyInsets(Node node, String type, String value) {
        if (!(node instanceof javafx.scene.layout.Region region)) return;

        String[] parts = value.trim().split("\\s+");
        double top = 0, right = 0, bottom = 0, left = 0;

        // Lógica de descompactação de shorthand (1, 2, 3 ou 4 valores)
        if (parts.length == 1) {
            top = right = bottom = left = parseSize(parts[0]);
        } else if (parts.length == 2) {
            top = bottom = parseSize(parts[0]);
            right = left = parseSize(parts[1]);
        } else if (parts.length == 3) {
            top = parseSize(parts[0]);
            right = left = parseSize(parts[1]);
            bottom = parseSize(parts[2]);
        } else if (parts.length >= 4) {
            top = parseSize(parts[0]);
            right = parseSize(parts[1]);
            bottom = parseSize(parts[2]);
            left = parseSize(parts[3]);
        }

        if (Double.isNaN(top)) return;
        javafx.geometry.Insets insets = new javafx.geometry.Insets(top, right, bottom, left);

        if ("margin".equals(type)) {
            // A Margem aplica-se através do PAI do nó!
            javafx.scene.Parent parent = node.getParent();
            if (parent instanceof javafx.scene.layout.VBox) javafx.scene.layout.VBox.setMargin(node, insets);
            else if (parent instanceof javafx.scene.layout.HBox) javafx.scene.layout.HBox.setMargin(node, insets);
            else if (parent instanceof javafx.scene.layout.StackPane) javafx.scene.layout.StackPane.setMargin(node, insets);
            else if (parent instanceof javafx.scene.layout.GridPane) javafx.scene.layout.GridPane.setMargin(node, insets);
        } else {
            // Padding aplica-se diretamente na Region
            region.setPadding(insets);
        }
    }


    private void applyBackground(Node node, String value) {
        updateFxStyle(node, "-fx-background-color", value);
    }

    private void applyTextFill(Node node, String value) {
        // Para Labeled (Botões, Labels) usamos o método nativo se possível, ou CSS
        if (node instanceof javafx.scene.control.Labeled labeled) {
            try {
                labeled.setTextFill(javafx.scene.paint.Color.web(value));
            } catch (Exception e) {
                updateFxStyle(node, "-fx-text-fill", value);
            }
        } else {
            updateFxStyle(node, "-fx-text-fill", value);
        }
    }

    private void applyFontSize(Node node, String value) {
        updateFxStyle(node, "-fx-font-size", value);
    }

    private void applyFontFamily(Node node, String value) {
        updateFxStyle(node, "-fx-font-family", "\"" + value.replace("\"", "") + "\"");
    }

    private void applyFontWeight(Node node, String value) {
        updateFxStyle(node, "-fx-font-weight", value);
    }

    private void applyFontStyle(Node node, String value) {
        updateFxStyle(node, "-fx-font-style", value);
    }

    private void applyTextAlign(Node node, String value) {
        updateFxStyle(node, "-fx-text-alignment", value);
        updateFxStyle(node, "-fx-alignment", value); // No JavaFX, alignment frequentemente cuida do layout interno
    }

    private void applyTextDecoration(Node node, String value) {
        if (node instanceof javafx.scene.control.Labeled labeled) {
            if ("underline".equals(value)) {
                labeled.setUnderline(true);
                updateFxStyle(node, "-fx-strikethrough", "false");
            } else if ("line-through".equals(value)) {
                labeled.setUnderline(false);
                updateFxStyle(node, "-fx-strikethrough", "true");
            } else {
                labeled.setUnderline(false);
                updateFxStyle(node, "-fx-strikethrough", "false");
            }
        }
    }

    private void applyBorderRadius(Node node, String value) {
        updateFxStyle(node, "-fx-background-radius", value);
        updateFxStyle(node, "-fx-border-radius", value);
    }

    private void applyTransform(Node node, String value) {
        // Pode ser delegado à tua class StyleTransformUtils futuramente.
        // Mas por via CSS puro do JavaFX (parcialmente suportado):
        updateFxStyle(node, "-fx-transform", value);
    }

    private void applyDisplay(Node node, String value) {
        if ("none".equals(value)) {
            node.setVisible(false);
            node.setManaged(false);
        } else {
            node.setVisible(true);
            node.setManaged(true);
        }
    }

    private void applyAlignment(Node node, String type, String value) {
        updateFxStyle(node, "-fx-alignment", value);
    }

    private void applyFlexDirection(Node node, String value) {
        if (node instanceof javafx.scene.layout.VBox vbox) {
            // row/column
            if ("row".equals(value)) {
                // Não suportado diretamente no VBox; converter para HBox seria complexo
                // Usamos CSS
                vbox.setStyle(vbox.getStyle() + "; -fx-alignment: " + value + ";");
            }
        }
    }

    private void applyGap(Node node, String value) {
        if (node instanceof javafx.scene.layout.VBox vbox) {
            double gap = parseSize(value);
            if (!Double.isNaN(gap)) vbox.setSpacing(gap);
        } else if (node instanceof javafx.scene.layout.HBox hbox) {
            double gap = parseSize(value);
            if (!Double.isNaN(gap)) hbox.setSpacing(gap);
        }
    }

    private String extractOffsetX(String css) {
        return css.replaceAll(".*?(\\d+)px?\\s+(-?\\d+)px?.*", "$1");
    }

    private String extractOffsetY(String css) {
        return css.replaceAll(".*?(\\d+)px?\\s+(-?\\d+)px?.*", "$2");
    }


    @Override
    public void reportError(String message) {
        System.err.println("[JavaFxRenderer ERROR]: " + message);
    }
}