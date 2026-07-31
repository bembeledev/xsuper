package com.dic.xsuper.render.javafx.tags.controls.select;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.render.javafx.core.SuperUiEngine;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Tag HTML &lt;select&gt; convertida para JavaFX ComboBox (single) ou ListView (multiple).
 * Suporta <option> e <optgroup>.
 */
public class SelectTag extends NativeTag {

    // Controlo principal
    private javafx.scene.control.Control fxControl;

    // Estrutura: lista de grupos (cada grupo tem label e lista de opções)
    private final List<OptgroupTag> groups = new ArrayList<>();
    private final List<OptionTag> flatOptions = new ArrayList<>(); // todas as opções em sequência

    private final boolean multiple;

    public SelectTag(XplNode sourceNode) {
        super(sourceNode);
        this.multiple = sourceNode.attributes.containsKey("multiple");
        parseOptionsAndGroups();
    }

    // ─── PARSING ──────────────────────────────────────────────────────────────

    private void parseOptionsAndGroups() {
        for (XplNode child : sourceNode.children) {
            if ("option".equalsIgnoreCase(child.tag)) {
                // Opção solta (fora de optgroup)
                OptionTag opt = new OptionTag(child);
                flatOptions.add(opt);
                // Cria um grupo "fantasma" sem label para opções soltas
                if (groups.isEmpty() || !groups.getLast().getLabel().isEmpty()) {
                    // Se o último grupo tem label, cria um novo grupo sem label
                    groups.add(new OptgroupTag(createOptgroupNode("")));
                }
                groups.getLast().getOptions().add(opt);
            } else if ("optgroup".equalsIgnoreCase(child.tag)) {
                OptgroupTag group = new OptgroupTag(child);
                groups.add(group);
                flatOptions.addAll(group.getOptions());
            }
        }
        // Se não houver nenhum grupo, cria um vazio para evitar NPE
        if (groups.isEmpty()) {
            groups.add(new OptgroupTag(createOptgroupNode("")));
        }
    }

    private XplNode createOptgroupNode(String label) {
        XplNode node = new XplNode("optgroup");
        node.attributes.put("label", label);
        return node;
    }

    // ─── CRIAÇÃO DO NÓ ──────────────────────────────────────────────────────

    @Override
    protected Node createNode() {
        int size = parseSize();
        if (multiple || size > 1) {
            fxControl = createListView(size);
        } else {
            fxControl = createComboBox();
        }

        // Aplica atributos comuns (disabled, required, etc.) – herdados de NativeTag/FormControlTag
        // Se a tua classe base tiver applyCommonAttributes, chama aqui.
        // applyCommonAttributes(fxControl);

        bindEvents();
        return fxControl;
    }

    // ─── COMBOBOX (seleção única) ───────────────────────────────────────────

    private ComboBox<String> createComboBox() {
        ComboBox<String> combo = new ComboBox<>();
        ObservableList<String> items = buildDisplayItems();
        combo.setItems(items);

        // Seleciona a primeira opção marcada (ou a primeira não-cabeçalho)
        int selectedIdx = findSelectedIndex();
        if (selectedIdx >= 0 && selectedIdx < items.size()) {
            combo.getSelectionModel().select(selectedIdx);
        } else {
            // Seleciona o primeiro item que não seja cabeçalho
            for (int i = 0; i < items.size(); i++) {
                if (!items.get(i).startsWith("──")) {
                    combo.getSelectionModel().select(i);
                    break;
                }
            }
        }

        // Estiliza células: cabeçalhos desativados e negrito
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("──")) {
                        setDisable(true);
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #718096; -fx-opacity: 0.8;");
                    } else {
                        setDisable(false);
                        setStyle("");
                    }
                }
            }
        });

        return combo;
    }

    // ─── LISTVIEW (seleção múltipla) ────────────────────────────────────────

    private ListView<String> createListView(int size) {
        ListView<String> listView = new ListView<>();
        ObservableList<String> items = buildDisplayItems();
        listView.setItems(items);

        if (multiple) {
            listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        } else {
            listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        }

        if (size > 1) {
            listView.setPrefHeight(size * 24.0);
        }

        // Seleciona opções marcadas
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            if (item.startsWith("──")) continue;
            for (OptionTag opt : flatOptions) {
                if (opt.getLabel().equals(item) && opt.isSelected()) {
                    listView.getSelectionModel().select(i);
                    break;
                }
            }
        }

        // Estiliza cabeçalhos (desativados)
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("──")) {
                        setDisable(true);
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #718096; -fx-opacity: 0.8;");
                    } else {
                        setDisable(false);
                        setStyle("");
                    }
                }
            }
        });

        return listView;
    }

    // ─── CONSTRUÇÃO DA LISTA DE ITENS EXIBIDOS ─────────────────────────────

    private ObservableList<String> buildDisplayItems() {
        ObservableList<String> items = FXCollections.observableArrayList();
        for (OptgroupTag group : groups) {
            if (!group.getLabel().isEmpty()) {
                items.add("── " + group.getLabel() + " ──");
            }
            for (OptionTag opt : group.getOptions()) {
                items.add(opt.getLabel());
            }
        }
        return items;
    }

    // ─── EVENTOS ─────────────────────────────────────────────────────────────

    @Override
    protected void bindEvents() {
        super.bindEvents(); // Garante herança da NativeTag

        if (fxControl instanceof javafx.scene.control.ComboBox<?> combo) {
            combo.valueProperty().addListener((obs, oldVal, newVal) -> {
                syncSelectValueToDom();
            });
        } else if (fxControl instanceof javafx.scene.control.ListView<?> listView) {
            listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                syncSelectValueToDom();
            });
        }
    }

    private void syncSelectValueToDom() {
        Object selectedValue = getSelectedValue();
        Object currentValue = sourceNode.attributes.get("value");

        if (selectedValue != null && !selectedValue.equals(currentValue)) {
            sourceNode.attributes.put("value", selectedValue);
            if (sourceNode.liveElement != null) {
                sourceNode.liveElement.setAttributeSilently("value", selectedValue);
            }
            if (sourceNode.id != null && !sourceNode.id.isEmpty()) {
                SuperUiEngine.getInstance().dispatchEvent("change", selectedValue, sourceNode.id);
            }
        }
    }
    // ─── OBTENÇÃO DO VALOR SELECIONADO ─────────────────────────────────────

    /**
     * Retorna o valor selecionado.
     * Se for multiple, retorna uma List<String> com os valores.
     * Se for single, retorna uma String (ou null).
     */
    private Object getSelectedValue() {
        if (fxControl instanceof ComboBox<?> combo) {
            String selectedLabel = (String) combo.getValue();
            if (selectedLabel == null || selectedLabel.startsWith("──")) return null;
            for (OptionTag opt : flatOptions) {
                if (opt.getLabel().equals(selectedLabel)) {
                    return opt.getValue();
                }
            }
            return selectedLabel;
        } else if (fxControl instanceof ListView<?> listView) {
            ObservableList<?> selectedItems = listView.getSelectionModel().getSelectedItems();
            if (selectedItems.isEmpty()) return multiple ? new ArrayList<>() : null;

            if (multiple) {
                List<String> result = new ArrayList<>();
                for (Object item : selectedItems) {
                    String label = (String) item;
                    if (label.startsWith("──")) continue;
                    for (OptionTag opt : flatOptions) {
                        if (opt.getLabel().equals(label)) {
                            result.add(opt.getValue());
                            break;
                        }
                    }
                }
                return result;
            } else {
                String selectedLabel = (String) selectedItems.get(0);
                if (selectedLabel == null || selectedLabel.startsWith("──")) return null;
                for (OptionTag opt : flatOptions) {
                    if (opt.getLabel().equals(selectedLabel)) {
                        return opt.getValue();
                    }
                }
                return selectedLabel;
            }
        }
        return null;
    }

    // ─── MÉTODOS AUXILIARES ──────────────────────────────────────────────────

    private int parseSize() {
        if (sourceNode.attributes.containsKey("size")) {
            try {
                return Integer.parseInt(sourceNode.attributes.get("size").toString());
            } catch (NumberFormatException ignored) {}
        }
        return 1;
    }

    private int findSelectedIndex() {
        int idx = 0;
        for (OptgroupTag group : groups) {
            if (!group.getLabel().isEmpty()) idx++; // cabeçalho
            for (OptionTag opt : group.getOptions()) {
                if (opt.isSelected()) return idx;
                idx++;
            }
        }
        return -1;
    }

    // ─── GETTERS PARA O XPL ─────────────────────────────────────────────────

    public Object getValue() {
        return getSelectedValue();
    }

    // ─── OVERRIDES ──────────────────────────────────────────────────────────

    @Override
    protected void applyTagSpecificStyles() {
        // Podes adicionar estilos específicos
    }

    @Override
    protected void addChildren() {
        // Os filhos são processados no parser, não adicionados como nós visuais.
    }

    // ─── DISPACHO DE EVENTOS (se não existir na classe base) ──────────────

    private void dispatchEvent(String eventName, String targetId, Object payload) {
        // Chama o callback da bridge (se existir) ou imprime para debug.
        System.out.println("[SelectTag] Evento: " + eventName + " | ID: " + targetId + " | Payload: " + payload);
        // Se tiveres um EngineCallback, usa-o:
        // if (engineCallback != null) engineCallback.onEvent(eventName, targetId, payload);
    }
}