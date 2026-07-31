package com.dic.xsuper.render.javafx.tags.table;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.layout.LayoutEngine;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;

public class TableCellTag extends NativeTag {

    public TableCellTag(XplNode sourceNode) {
        super(sourceNode);

        // ⭐ A CURA DO THEAD INVISÍVEL
        if ("th".equalsIgnoreCase(sourceNode.tag)) {
            // Em vez de 'table-cell' (que colapsa em cabeçalhos nativos),
            // usamos Flexbox para garantir que o conteúdo se expande e centra!
            if (!this.style.containsKey("display")) this.style.put("display", "flex");
            if (!this.style.containsKey("justify-content")) this.style.put("justify-content", "center");
            if (!this.style.containsKey("align-items")) this.style.put("align-items", "center");
            if (!this.style.containsKey("font-weight")) this.style.put("font-weight", "bold");
        } else {
            if (!this.style.containsKey("display")) this.style.put("display", "table-cell");
        }
    }

    @Override
    protected Node createNode() {
        return LayoutEngine.resolveLayout(this).createContainer(this);
    }

    @Override
    protected void applyTagSpecificStyles() {}
}