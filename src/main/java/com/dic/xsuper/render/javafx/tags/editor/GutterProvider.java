package com.dic.xsuper.render.javafx.tags.editor;

import javafx.scene.Node;

public interface GutterProvider {
    Node createGraphic(int lineIndex, String text);
}