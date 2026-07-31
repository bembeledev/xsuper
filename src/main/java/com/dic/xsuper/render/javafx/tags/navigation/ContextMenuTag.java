package com.dic.xsuper.render.javafx.tags.navigation;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.Region;

public class ContextMenuTag extends NativeTag {

    public ContextMenuTag(XplNode sourceNode) {
        super(sourceNode);
        // O atributo 'target' foi removido! A ligação é agora gerida globalmente.
    }

    @Override
    protected Node createNode() {
        ContextMenu contextMenu = new ContextMenu();

        // Constrói os itens usando o nosso Helper central
        for (XplNode childNode : sourceNode.children) {
            if ("menuitem".equalsIgnoreCase(childNode.tag)) {
                contextMenu.getItems().add(MenuHelper.buildMenuItem(childNode));
            } else if ("menu".equalsIgnoreCase(childNode.tag)) {
                contextMenu.getItems().add(MenuHelper.buildMenu(childNode));
            }
        }

        // TRUQUE DE MESTRE: Devolvemos um nó vazio (dummy) de 0 pixels para não quebrar a UI
        Region dummy = new Region();
        dummy.setManaged(false); // Não ocupa espaço no ecrã
        dummy.setVisible(false);

        // Guardamos o ContextMenu dentro das propriedades secretas do nó fantasma!
        dummy.getProperties().put("xpl_context_menu", contextMenu);

        return dummy;
    }

    @Override
    protected void applyTagSpecificStyles() {}

    @Override
    protected void addChildren() {}
}