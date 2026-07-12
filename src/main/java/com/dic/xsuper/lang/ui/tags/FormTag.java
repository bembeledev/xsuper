package com.dic.xsuper.lang.ui.tags;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.layout.LayoutEngine;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tag HTML &lt;form&gt; – Orquestrador de submissão, serialização e validação de dados.
 * <p>
 * Atributos suportados:
 * - action: URL para onde os dados são enviados
 * - method: GET | POST (padrão GET)
 * - enctype: application/x-www-form-urlencoded | multipart/form-data | text/plain
 * - target: _blank | _self | _parent | _top | (nome)
 * - novalidate: desativa a validação do formulário
 * - onsubmit: nome da função XPL a invocar antes da submissão
 * - onreset: nome da função XPL a invocar após reset
 */
public class FormTag extends NativeTag {

    // ─── Atributos do formulário ────────────────────────────────────────────
    private String method = "get";
    private String action = "";
    private String enctype = "application/x-www-form-urlencoded";
    private String target = "_self";
    private boolean novalidate = false;
    private String onsubmit = null;
    private String onreset = null;

    // ─── Callback para integração com motor XPL ────────────────────────────
    private FormSubmissionCallback submissionCallback;

    public FormTag(XplNode node) {
        super(node);
        parseAttributes();
    }

    private void parseAttributes() {
        if (sourceNode.attributes == null) return;
        method = sourceNode.attributes.getOrDefault("method", "get").toString().toLowerCase();
        action = sourceNode.attributes.getOrDefault("action", "").toString();
        enctype = sourceNode.attributes.getOrDefault("enctype", "application/x-www-form-urlencoded").toString();
        target = sourceNode.attributes.getOrDefault("target", "_self").toString();
        novalidate = sourceNode.attributes.containsKey("novalidate");
        onsubmit = sourceNode.attributes.containsKey("onsubmit") ? sourceNode.attributes.get("onsubmit").toString() : null;
        onreset = sourceNode.attributes.containsKey("onreset") ? sourceNode.attributes.get("onreset").toString() : null;
    }

    // ─── Ciclo de vida ──────────────────────────────────────────────────────

    @Override
    protected Node createNode() {
        return LayoutEngine.resolveLayout(this).createContainer(this);
    }

    @Override
    protected void applyTagSpecificStyles() {}

    @Override
    protected void bindEvents() {
        super.bindEvents();
        if (fxNode != null) {
            hookSubmitButtons(fxNode);
        }
    }

    // ─── Deteção de botões de submissão ────────────────────────────────────

    private void hookSubmitButtons(Node root) {
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (child instanceof Button btn) {
                    String eventType = (String) btn.getProperties().get("xpl_event_type");
                    String text = btn.getText() != null ? btn.getText().toLowerCase() : "";

                    // Deteta botão submit (explícito ou por texto)
                    if ("submit".equals(eventType) || text.contains("submeter") || text.contains("submit")) {
                        String formAction = (String) btn.getProperties().get("xpl_formaction");
                        String formMethod = (String) btn.getProperties().get("xpl_formmethod");
                        btn.setOnAction(e -> triggerSubmit(formAction, formMethod));
                    } else if ("reset".equals(eventType) || text.contains("reset")) {
                        btn.setOnAction(e -> triggerReset());
                    }
                }
                hookSubmitButtons(child);
            }
        }
    }

    // ─── Submissão ──────────────────────────────────────────────────────────

    private void triggerSubmit() {
        triggerSubmit(null, null);
    }

    private void triggerSubmit(String overrideAction, String overrideMethod) {
        String finalAction = overrideAction != null ? overrideAction : action;
        String finalMethod = overrideMethod != null ? overrideMethod.toLowerCase() : method;

        // 1. Validação (se não estiver desativada)
        if (!novalidate) {
            List<String> errors = validateForm();
            if (!errors.isEmpty()) {
                String msg = String.join("\n", errors);
                dispatchEvent("invalid", sourceNode.id, msg);
                // Podes mostrar um alerta JavaFX ou notificar o XPL
                return;
            }
        }

        // 2. Coleta de dados
        Map<String, Object> formData = collectFormData();

        // 3. Executa callback onsubmit (se definido)
        if (onsubmit != null && !onsubmit.isEmpty()) {
            // Dispara evento para o motor XPL – a função pode cancelar a submissão
            boolean shouldSubmit = (boolean) dispatchEventAndWait("submit", sourceNode.id, formData);
            if (!shouldSubmit) return;
        }

        // 4. Submissão propriamente dita
        submitData(finalAction, finalMethod, formData);
    }

    private void submitData(String action, String method, Map<String, Object> data) {
        if (action == null || action.isEmpty()) {
            System.out.println("[FormTag] Submissão: sem action definida, apenas log dos dados.");
            System.out.println("  Dados: " + data);
            return;
        }

        switch (method) {
            case "get" -> submitGet(action, data);
            case "post" -> submitPost(action, data);
            default -> System.err.println("[FormTag] Método HTTP não suportado: " + method);
        }
    }

    private void submitGet(String action, Map<String, Object> data) {
        String queryString = serializeToQueryString(data);
        String url = action + (action.contains("?") ? "&" : "?") + queryString;
        // Abre a URL no navegador padrão (ou WebView)
        launchUrl(url);
    }

    private void submitPost(String action, Map<String, Object> data) {
        // Usa NativeHttp se estiver disponível, senão fallback para browser
        try {
            String payload = serializeToFormData(data);
            // Tenta usar NativeHttp (se registado)
            // Se não, fallback para abertura de URL com POST (mais complexo)
            // Por simplicidade, apenas log e abrir URL com os dados codificados
            System.out.println("[FormTag] POST para " + action + " com payload: " + payload);
            launchUrl(action);
        } catch (Exception e) {
            System.err.println("[FormTag] Erro na submissão POST: " + e.getMessage());
        }
    }

    private void launchUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            // Fallback: abrir WebView se disponível
            dispatchEvent("open", sourceNode.id, url);
        }
    }

    // ─── Serialização ───────────────────────────────────────────────────────

    private String serializeToQueryString(Map<String, Object> data) {
        return data.entrySet().stream()
                .map(e -> {
                    String key = encode(e.getKey());
                    Object val = e.getValue();
                    if (val instanceof List<?> list) {
                        return list.stream()
                                .map(item -> key + "=" + encode(String.valueOf(item)))
                                .collect(Collectors.joining("&"));
                    }
                    return key + "=" + encode(String.valueOf(val));
                })
                .collect(Collectors.joining("&"));
    }

    private String serializeToFormData(Map<String, Object> data) {
        // Para multipart/form-data seria mais complexo; aqui simplificamos para URL-encoded
        return serializeToQueryString(data);
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    // ─── Reset ──────────────────────────────────────────────────────────────

    private void triggerReset() {
        resetFormFields(fxNode);
        if (onreset != null && !onreset.isEmpty()) {
            dispatchEvent("reset", sourceNode.id, null);
        }
    }

    private void resetFormFields(Node node) {
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (child instanceof TextField tf) tf.clear();
                else if (child instanceof TextArea ta) ta.clear();
                else if (child instanceof CheckBox cb) cb.setSelected(false);
                else if (child instanceof RadioButton rb) rb.setSelected(false);
                else if (child instanceof Slider slider) slider.setValue(slider.getMin());
                else if (child instanceof ComboBox<?> combo) combo.getSelectionModel().clearSelection();
                else if (child instanceof ListView<?> lv) lv.getSelectionModel().clearSelection();
                resetFormFields(child);
            }
        }
    }

    // ─── Coleta de dados ────────────────────────────────────────────────────

    public Map<String, Object> collectFormData() {
        Map<String, Object> data = new LinkedHashMap<>();
        if (fxNode == null) return data;
        scanNodesForData(fxNode, data);
        return data;
    }

    private void scanNodesForData(Node node, Map<String, Object> data) {
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                extractValueFromControl(child, data);
                scanNodesForData(child, data);
            }
        }
    }

    private void extractValueFromControl(Node node, Map<String, Object> data) {
        Object nameObj = node.getProperties().get("xpl_control_name");
        if (nameObj == null) return;
        String name = nameObj.toString();
        if (name.isEmpty()) return;

        // Ignora campos disabled
        if (node.isDisabled()) return;

        if (node instanceof TextField tf) {
            data.put(name, tf.getText());
        } else {
            if (node instanceof TextArea ta) {
                data.put(name, ta.getText());
            } else if (node instanceof CheckBox cb) {
                // Se já existir uma lista para este nome, adiciona; senão, cria
                if (cb.isSelected()) {
                    mergeValue(data, name, node.getProperties().getOrDefault("xpl_input_value", "on"));
                }
            } else if (node instanceof RadioButton rb) {
                if (rb.isSelected()) {
                    // Para radio, o valor pode ser o texto ou o atributo value
                    Object val = node.getProperties().getOrDefault("xpl_input_value", rb.getText());
                    data.put(name, val);
                }
            } else if (node instanceof ComboBox<?> combo) {
                Object val = combo.getValue();
                if (val != null) data.put(name, val);
            } else if (node instanceof ListView<?> lv) {
                ObservableList<?> selected = lv.getSelectionModel().getSelectedItems();
                if (!selected.isEmpty()) {
                    if (lv.getSelectionModel().getSelectionMode() == SelectionMode.MULTIPLE) {
                        data.put(name, new ArrayList<>(selected));
                    } else {
                        data.put(name, selected.getFirst());
                    }
                }
            } else if (node instanceof Slider slider) {
                data.put(name, slider.getValue());
            } else if (node.getProperties().containsKey("xpl_file_path")) {
                data.put(name, node.getProperties().get("xpl_file_path"));
            } else if (node instanceof DatePicker dp) {
                data.put(name, dp.getValue() != null ? dp.getValue().toString() : null);
            } else if (node instanceof ColorPicker cp) {
                data.put(name, cp.getValue() != null ? cp.getValue().toString() : null);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeValue(Map<String, Object> data, String key, Object value) {
        if (data.containsKey(key)) {
            Object existing = data.get(key);
            if (existing instanceof List) {
                ((List<Object>) existing).add(value);
            } else {
                List<Object> list = new ArrayList<>();
                list.add(existing);
                list.add(value);
                data.put(key, list);
            }
        } else {
            data.put(key, value);
        }
    }

    // ─── Validação ──────────────────────────────────────────────────────────

    private List<String> validateForm() {
        List<String> errors = new ArrayList<>();
        if (fxNode == null) return errors;
        validateNode(fxNode, errors);
        return errors;
    }

    private void validateNode(Node node, List<String> errors) {
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                validateControl(child, errors);
                validateNode(child, errors);
            }
        }
    }

    private void validateControl(Node node, List<String> errors) {
        // Required
        if (Boolean.TRUE.equals(node.getProperties().get("xpl_required"))) {
            Object nameObj = node.getProperties().get("xpl_control_name");
            String name = nameObj != null ? nameObj.toString() : "campo";
            boolean hasValue = false;

            if (node instanceof TextInputControl text) {
                hasValue = !text.getText().isEmpty();
            } else if (node instanceof CheckBox cb) {
                hasValue = cb.isSelected();
            } else if (node instanceof ComboBox<?> combo) {
                hasValue = combo.getValue() != null;
            } else if (node instanceof ListView<?> lv) {
                hasValue = !lv.getSelectionModel().getSelectedItems().isEmpty();
            } else if (node instanceof Slider) {
                hasValue = true; // Slider tem sempre valor
            }

            if (!hasValue) {
                errors.add("O campo '" + name + "' é obrigatório.");
            }
        }

        // Pattern (para TextField)
        if (node instanceof TextField tf) {
            String pattern = (String) node.getProperties().get("xpl_pattern");
            if (pattern != null && !pattern.isEmpty()) {
                String text = tf.getText();
                if (!text.matches(pattern)) {
                    String name = (String) node.getProperties().getOrDefault("xpl_control_name", "campo");
                    errors.add("O campo '" + name + "' não corresponde ao formato esperado.");
                }
            }
            // Minlength / Maxlength
            Integer minLen = (Integer) node.getProperties().get("xpl_minlength");
            Integer maxLen = (Integer) node.getProperties().get("xpl_maxlength");
            String text = tf.getText();
            if (minLen != null && text.length() < minLen) {
                String name = (String) node.getProperties().getOrDefault("xpl_control_name", "campo");
                errors.add("O campo '" + name + "' deve ter pelo menos " + minLen + " caracteres.");
            }
            if (maxLen != null && text.length() > maxLen) {
                String name = (String) node.getProperties().getOrDefault("xpl_control_name", "campo");
                errors.add("O campo '" + name + "' deve ter no máximo " + maxLen + " caracteres.");
            }
        }

        // Min / Max (para NumberInput)
        if (node instanceof Spinner<?> spinner) {
            // Casting para Number
            // ...
        }
    }

    // ─── Eventos ────────────────────────────────────────────────────────────

    private void dispatchEvent(String eventName, String targetId, Object payload) {
        // Chama o callback da bridge se existir
        System.out.println("[FormTag] Evento: " + eventName + " | ID: " + targetId + " | Payload: " + payload);
        // if (engineCallback != null) engineCallback.onEvent(eventName, targetId, payload);
    }

    private Object dispatchEventAndWait(String eventName, String targetId, Object payload) {
        // Para eventos síncronos (ex: onsubmit pode cancelar)
        // Implementar com CompletableFuture ou callback síncrono
        dispatchEvent(eventName, targetId, payload);
        return true; // default: continuar submissão
    }

    // ─── Interface para callback ───────────────────────────────────────────

    public void setSubmissionCallback(FormSubmissionCallback callback) {
        this.submissionCallback = callback;
    }

    @FunctionalInterface
    public interface FormSubmissionCallback {
        void onSubmit(String action, String method, Map<String, Object> data);
    }
}