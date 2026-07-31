package com.dic.xsuper.render.javafx.tags.navigation;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import java.util.Map;

/**
 * Tag HTML <menu> – Representa um sub-menu dentro de uma barra de menu ou contexto.
 * Atua como um contentor estrutural seguro que não quebra as restrições gráficas do JavaFX.
 */
public class MenuTag extends NativeTag {

    private String label = "";
    private String icon = null;
    private boolean disabled = false;

    public MenuTag(XplNode sourceNode) {
        super(sourceNode);
        parseAttributes();
        if (sourceNode.textContent != null && !sourceNode.textContent.trim().isEmpty()) {
            label = sourceNode.textContent.trim();
        }
    }

    private void parseAttributes() {
        Map<String, Object> attrs = sourceNode.attributes;
        if (attrs != null) {
            if (attrs.containsKey("label")) label = attrs.get("label").toString();
            if (attrs.containsKey("icon")) icon = attrs.get("icon").toString();
            if (attrs.containsKey("disabled")) disabled = "true".equalsIgnoreCase(attrs.get("disabled").toString());
        }
    }

    // Getters limpos para o MenubarTag ou ContextMenuTag consultarem os metadados
    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public boolean isDisabled() { return disabled; }

    @Override
    protected Node createNode() {
        // O Menu do JavaFX NÃO herda de Node. Para evitar erros de compilação,
        // devolvemos um elemento fantasma (dummy) de 0px que fica oculto na UI.
        Region dummy = new Region();
        dummy.setManaged(false);
        dummy.setVisible(false);
        return dummy;
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Menus suspensos e popups herdam o visual do sistema ou folhas de estilo globais
    }

    @Override
    protected void addChildren() {
        // Deixamos vazio! Não queremos injetar os componentes filhos dentro do dummy Region.
        // A árvore de sub-menus e itens será lida diretamente da lista de 'children' pelas tags superiores.
    }
}