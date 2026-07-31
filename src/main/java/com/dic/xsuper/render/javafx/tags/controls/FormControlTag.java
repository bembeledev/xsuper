package com.dic.xsuper.render.javafx.tags.controls;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.render.javafx.event.UiEventPublisher;
import javafx.scene.control.Control;

import java.util.Map;

/**
 * Classe base para todos os elementos de formulário (inputs, selects, textareas).
 * Centraliza atributos comuns: disabled, readonly, required, placeholder, value, name, etc.
 */
public abstract class FormControlTag extends NativeTag {

    // ─── Atributos HTML comuns ──────────────────────────────────────────────
    protected String name = "";
    protected String value = "";
    protected String placeholder = "";
    protected boolean disabled = false;
    protected boolean readonly = false;
    protected boolean required = false;
    protected String autocomplete = "";
    protected String autofocus = "";
    protected String form = "";
    protected String formaction = "";
    protected String formenctype = "";
    protected String formmethod = "";
    protected boolean formnovalidate = false;
    protected String formtarget = "";
    protected String inputmode = "";
    protected String list = "";          // datalist
    protected String max = "";
    protected String maxlength = "";
    protected String min = "";
    protected String minlength = "";
    protected String multiple = "";
    protected String pattern = "";
    protected String size = "";
    protected String spellcheck = "";
    protected String step = "";

    public FormControlTag(XplNode sourceNode) {
        super(sourceNode);
        parseCommonAttributes();
    }

    /**
     * Extrai todos os atributos comuns do nó XPL e guarda nos campos.
     */
    private void parseCommonAttributes() {
        Map<String, Object> attrs = sourceNode.attributes;

        name = getAttrAsString(attrs, "name", "");
        value = getAttrAsString(attrs, "value", "");
        placeholder = getAttrAsString(attrs, "placeholder", "");
        disabled = attrs.containsKey("disabled") && !"false".equalsIgnoreCase(getAttrAsString(attrs, "disabled", "false"));
        readonly = attrs.containsKey("readonly") && !"false".equalsIgnoreCase(getAttrAsString(attrs, "readonly", "false"));
        required = attrs.containsKey("required") && !"false".equalsIgnoreCase(getAttrAsString(attrs, "required", "false"));
        autocomplete = getAttrAsString(attrs, "autocomplete", "");
        autofocus = getAttrAsString(attrs, "autofocus", "");
        form = getAttrAsString(attrs, "form", "");
        formaction = getAttrAsString(attrs, "formaction", "");
        formenctype = getAttrAsString(attrs, "formenctype", "");
        formmethod = getAttrAsString(attrs, "formmethod", "");
        formnovalidate = attrs.containsKey("formnovalidate") && !"false".equalsIgnoreCase(getAttrAsString(attrs, "formnovalidate", "false"));
        formtarget = getAttrAsString(attrs, "formtarget", "");
        inputmode = getAttrAsString(attrs, "inputmode", "");
        list = getAttrAsString(attrs, "list", "");
        max = getAttrAsString(attrs, "max", "");
        maxlength = getAttrAsString(attrs, "maxlength", "");
        min = getAttrAsString(attrs, "min", "");
        minlength = getAttrAsString(attrs, "minlength", "");
        multiple = getAttrAsString(attrs, "multiple", "");
        pattern = getAttrAsString(attrs, "pattern", "");
        size = getAttrAsString(attrs, "size", "");
        spellcheck = getAttrAsString(attrs, "spellcheck", "");
        step = getAttrAsString(attrs, "step", "");
    }

    /**
     * Motor Rígido de Two-Way Data Binding.
     * Liga qualquer propriedade JavaFX (Property<T>) a um atributo do XPL.
     *
     * @param fxProperty A propriedade do JavaFX (ex: textField.textProperty())
     * @param domAttribute O nome do atributo no DOM (ex: "value", "checked")
     */
    protected <T> void bindTwoWayProperty(javafx.beans.property.Property<T> fxProperty, String domAttribute) {
        fxProperty.addListener((obs, oldVal, newVal) -> {
            Object currentValue = sourceNode.attributes.get(domAttribute);
            if (newVal != null && !newVal.equals(currentValue)) {

                // Atualiza Planta Base
                sourceNode.attributes.put(domAttribute, newVal);

                // Publica no BUS que o JavaFX interagiu! (O UiEventHandlers fará o resto)
                if (sourceNode.id != null && !sourceNode.id.isEmpty()) {
                    UiEventPublisher
                            .publishUiInteracted(sourceNode.id, domAttribute, newVal);
                }
            }
        });
    }


    private String getAttrAsString(Map<String, Object> attrs, String key, String defaultValue) {
        Object val = attrs.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    /**
     * Aplica os atributos comuns a um controlo JavaFX.
     * Deve ser chamado por todas as subclasses após criar o controlo.
     */
    protected void applyCommonAttributes(Control control) {
        if (control == null) return;

        // Disabled
        if (disabled) {
            control.setDisable(true);
        }

        // Readonly (apenas para TextField, TextArea, etc. – subclasses devem aplicar)
        // Não há um método genérico para readonly em todos os controls.

        // Placeholder – subclasses aplicam conforme o tipo.

        // Value – subclasses aplicam.

        // Name – guardamos no userData para referência futura.
        control.getProperties().put("xpl_control_name", name);

        // Required – também guardamos no userData para validação.
        if (required) {
            control.getProperties().put("xpl_required", true);
        }

        // Autofocus – aplicamos se presente.
        if (!autofocus.isEmpty() && "true".equalsIgnoreCase(autofocus)) {
            control.requestFocus();
        }

        // Estilos adicionais por atributo (p.ex. se o control for um TextField)
        applyAttributeStyles(control);
    }

    /**
     * Aplica estilos derivados de atributos específicos (ex: size, maxlength, etc.)
     * Pode ser sobrescrito por subclasses para comportamento personalizado.
     */
    protected void applyAttributeStyles(Control control) {
        // Este método pode ser expandido por subclasses
    }

    // ─── Getters para uso em subclasses ──────────────────────────────────────
    public String getName() { return name; }
    public String getValue() { return value; }
    public String getPlaceholder() { return placeholder; }
    public boolean isDisabled() { return disabled; }
    public boolean isReadonly() { return readonly; }
    public boolean isRequired() { return required; }

    // ─── Construtores e métodos herdados ──────────────────────────────────
    @Override
    protected void addChildren() {
        // Inputs são void elements – não têm filhos
    }
}