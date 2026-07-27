package com.dic.xsuper.lang.ui.tags.texts;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.text.Text;

public class TextNodeTag extends NativeTag {

    public TextNodeTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        // Cria fisicamente a string na GPU
        return new Text(sourceNode.textContent != null ? sourceNode.textContent : "");
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Como o Text não é um "Region" (caixa W3C), aplicamos os estilos inline do teu css W3C.
        // A NativeTag já chamou o applyCommonStyles() que injeta a cor, tamanho e fontweight na perfeição!
    }

    @Override
    protected void addChildren() {
        // Os nós #text são Folhas (Leaf Nodes). Nunca têm filhos!
    }
}