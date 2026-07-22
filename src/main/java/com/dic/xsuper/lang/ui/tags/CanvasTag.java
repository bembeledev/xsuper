package com.dic.xsuper.lang.ui.tags;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public class CanvasTag extends NativeTag {

    private Canvas fxCanvas;

    public CanvasTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        double width = 300;
        double height = 150;

        if (sourceNode.attributes.containsKey("width")) {
            try { width = Double.parseDouble(sourceNode.attributes.get("width").toString()); } catch (Exception ignored) {}
        }
        if (sourceNode.attributes.containsKey("height")) {
            try { height = Double.parseDouble(sourceNode.attributes.get("height").toString()); } catch (Exception ignored) {}
        }

        fxCanvas = new javafx.scene.canvas.Canvas(width, height);

        // ⭐ INVERSÃO DE CONTROLO: Criamos o Contexto XPL aqui na camada UI!
        javafx.scene.canvas.GraphicsContext gc = fxCanvas.getGraphicsContext2D();
        com.dic.xsuper.lang.ui.document.XplCanvasContext2D xplCtx =
                new com.dic.xsuper.lang.ui.document.XplCanvasContext2D(gc, com.dic.xsuper.lang.ui.document.ContextNativeRegistry.CANVAS_CTX_CLASS);

        // ⭐ Injetamos silenciosamente na planta (XplNode) como um atributo privado
        sourceNode.attributes.put("__ctx_2d", xplCtx);

        // Se a árvore viva (XplElement) já existir, injetamos lá também
        if (sourceNode.liveElement != null) {
            sourceNode.liveElement.setAttributeSilently("__ctx_2d", xplCtx);
        }

        applyCommonStyles();
        return fxCanvas;
    }
    @Override
    protected void applyTagSpecificStyles() {
        // Estilos específicos, se aplicável
    }

    @Override
    protected void addChildren() {
        // O Canvas é um elemento de renderização isolado, ignoramos os filhos do DOM
    }
}