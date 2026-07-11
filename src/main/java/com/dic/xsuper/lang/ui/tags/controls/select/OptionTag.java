package com.dic.xsuper.lang.ui.tags.controls.select;

import com.dic.xsuper.lang.ui.html.XplNode;

/**
 * Representa uma tag HTML <option>.
 * Não é um nó visual, apenas um contentor de dados para o <select>.
 */
public class OptionTag {

    private final String value;
    private final String label;
    private final boolean selected;
    private final boolean disabled;

    public OptionTag(XplNode node) {
        this.value = (String) node.attributes.getOrDefault("value", node.textContent);
        this.label = node.textContent;
        this.selected = node.attributes.containsKey("selected");
        this.disabled = node.attributes.containsKey("disabled");
    }

    public String getValue() { return value; }
    public String getLabel() { return label; }
    public boolean isSelected() { return selected; }
    public boolean isDisabled() { return disabled; }
}