package com.dic.xsuper.render.javafx.tags.controls.select;

import com.dic.xsuper.dom.node.XplNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma tag HTML <optgroup>.
 * Agrupa opções com um rótulo (label).
 */
public class OptgroupTag {

    private final String label;
    private final List<OptionTag> options = new ArrayList<>();

    public OptgroupTag(XplNode node) {
        this.label = (String) node.attributes.getOrDefault("label", "");
        for (XplNode child : node.children) {
            if ("option".equalsIgnoreCase(child.tag)) {
                options.add(new OptionTag(child));
            }
        }
    }

    public String getLabel() { return label; }
    public List<OptionTag> getOptions() { return options; }
}