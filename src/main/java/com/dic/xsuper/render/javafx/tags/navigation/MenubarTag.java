package com.dic.xsuper.render.javafx.tags.navigation;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.control.MenuBar;

public class MenubarTag extends NativeTag {

    public MenubarTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1px 0;");

        // Lemos os XplNodes puros! (Ignoramos o método addChildren() normal)
        for (XplNode childNode : sourceNode.children) {
            if ("menu".equalsIgnoreCase(childNode.tag)) {
                menuBar.getMenus().add(MenuHelper.buildMenu(childNode));
            }
        }

        applyCommonStyles();
        return menuBar;
    }

    @Override
    protected void applyTagSpecificStyles() {}

    @Override
    protected void addChildren() {
        // Bloqueado! Os filhos já foram construídos dentro do createNode().
    }
}