package com.dic.xsuper.lang.ui.tags.navigation;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.Region;

public class ContextMenuTag extends NativeTag {

    private String targetId = null;

    public ContextMenuTag(XplNode sourceNode) {
        super(sourceNode);
        if (sourceNode.attributes.containsKey("target")) {
            targetId = sourceNode.attributes.get("target").toString();
        }
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
        // O motor superior pode ler isto mais tarde e aplicar ao verdadeiro alvo (targetId).
        dummy.getProperties().put("xpl_context_menu", contextMenu);
        if (targetId != null) {
            dummy.getProperties().put("xpl_context_target", targetId);
        }

        return dummy;
    }

    @Override
    protected void applyTagSpecificStyles() {}

    @Override
    protected void addChildren() {}
}