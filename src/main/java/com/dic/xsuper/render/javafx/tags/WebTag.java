package com.dic.xsuper.render.javafx.tags;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Tag <web> – Renderiza uma página web inteira ou código HTML nativo.
 * Suporta 'src' (URLs absolutos) e 'srcdoc' (HTML embutido).
 */
public class WebTag extends NativeTag {

    private WebView webView;

    public WebTag(XplNode node) {
        super(node);
    }

    @Override
    protected Node createNode() {
        webView = new WebView();
        WebEngine engine = webView.getEngine();

        String src = (String) sourceNode.attributes.get("src");
        String srcdoc = (String) sourceNode.attributes.get("srcdoc");

        // Regra do HTML5: srcdoc tem prioridade sobre src
        if (srcdoc != null && !srcdoc.trim().isEmpty()) {
            engine.loadContent(srcdoc);
        }
        else if (src != null && !src.trim().isEmpty()) {
            // Garante o protocolo http se o programador se esquecer
            if (!src.startsWith("http://") && !src.startsWith("https://") && !src.startsWith("file://")) {
                src = "https://" + src;
            }
            engine.load(src);
        }
        else {
            // Fallback elegante se a tag estiver vazia
            engine.loadContent("<html><body style='font-family: sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; color: #94a3b8; background: #f8fafc;'>Nenhum recurso especificado.</body></html>");
        }

        applyCommonStyles();
        return webView;
    }

    @Override
    protected void applyTagSpecificStyles() {}

    @Override
    protected void addChildren() {
        // A tag <web> não tem filhos XPL nativos.
    }
}