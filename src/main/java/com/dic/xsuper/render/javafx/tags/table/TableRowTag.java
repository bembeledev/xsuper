package com.dic.xsuper.render.javafx.tags.table;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.layout.LayoutEngine;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;

public class TableRowTag extends NativeTag {
    public TableRowTag(XplNode sourceNode) {
        super(sourceNode);
        injectDefaultStyle("display", "table-row");
    }

    @Override
    protected Node createNode() {
        return LayoutEngine.resolveLayout(this).createContainer(this);
    }

    @Override
    protected void applyTagSpecificStyles() {

    }

    private void injectDefaultStyle(String property, String value) {
        String currentStyle = (String) sourceNode.attributes.getOrDefault("style", "");
        if (!currentStyle.toLowerCase().contains(property + ":")) {
            sourceNode.attributes.put("style", property + ": " + value + "; " + currentStyle);
        }
    }
}