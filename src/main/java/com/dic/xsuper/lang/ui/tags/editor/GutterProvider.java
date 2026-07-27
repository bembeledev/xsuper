package com.dic.xsuper.lang.ui.tags.editor;

import javafx.scene.Node;

public interface GutterProvider {
    Node createGraphic(int lineIndex, String text);
}