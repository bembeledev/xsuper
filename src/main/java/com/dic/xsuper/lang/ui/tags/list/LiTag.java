package com.dic.xsuper.lang.ui.tags.list;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.layout.LayoutEngine;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class LiTag extends NativeTag {

    private Label fxLabel;

    public LiTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        return LayoutEngine.resolveLayout(this).createContainer(this);
    }

    @Override
    protected void applyTagSpecificStyles() {
    }


    public String getText() {
        return sourceNode.textContent != null ? sourceNode.textContent : "";
    }
}