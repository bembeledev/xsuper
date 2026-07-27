package com.dic.xsuper.lang.ui.tags.editor;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

// --- Bus ---
public class EditorEventBus {
    private static final EditorEventBus INSTANCE = new EditorEventBus();
    private final ObjectProperty<Integer> metadataChangedEvent = new SimpleObjectProperty<>(-1);
    private EditorEventBus() {}
    public static EditorEventBus getInstance() { return INSTANCE; }
    public ObjectProperty<Integer> getMetadataChangedEvent() { return metadataChangedEvent; }
    public void fireMetadataChanged(int lineIndex) { metadataChangedEvent.set(lineIndex); }
}
