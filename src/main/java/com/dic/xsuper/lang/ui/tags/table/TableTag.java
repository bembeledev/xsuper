package com.dic.xsuper.lang.ui.tags.table;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.tags.TagFactory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.HashMap;
import java.util.Map;

public class TableTag extends NativeTag {

    private TableView<Map<Integer, XplNode>> tableView;

    // ─── Propriedades Semânticas W3C ───
    private boolean sortable = true;
    private boolean columnsMenu = false;
    private boolean multiSelect = false;

    public TableTag(XplNode sourceNode) {
        super(sourceNode);
        parseTableAttributes();
    }

    private void parseTableAttributes() {
        if (sourceNode.attributes.containsKey("sortable")) {
            sortable = !"false".equalsIgnoreCase(sourceNode.attributes.get("sortable").toString());
        }
        if (sourceNode.attributes.containsKey("columns-menu")) {
            columnsMenu = "true".equalsIgnoreCase(sourceNode.attributes.get("columns-menu").toString());
        }
        if (sourceNode.attributes.containsKey("selection")) {
            multiSelect = "multiple".equalsIgnoreCase(sourceNode.attributes.get("selection").toString());
        }
    }

    @Override
    protected Node createNode() {
        tableView = new TableView<>();

        // Configurações HTML nativas
        tableView.setTableMenuButtonVisible(columnsMenu);
        tableView.getSelectionModel().setSelectionMode(multiSelect ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ⭐ O RESET CSS (Despe o JavaFX para o W3C brilhar) ⭐
        String resetCss = """
            .table-view { -fx-background-color: transparent; -fx-padding: 0; }
            .column-header-background { -fx-background-color: transparent; }
            .column-header { -fx-background-color: transparent; -fx-padding: 0; }
            .column-header .label { -fx-padding: 0; }
            .table-cell { -fx-padding: 0; -fx-border-width: 0; }
            .table-row-cell { -fx-background-color: transparent; -fx-padding: 0; -fx-border-width: 0; }
            .table-row-cell:empty { -fx-background-color: transparent; }
        """;
        String uri = "data:text/css;charset=utf-8;base64," + java.util.Base64.getEncoder().encodeToString(resetCss.getBytes());
        tableView.getStylesheets().add(uri);

        return tableView;
    }

    @Override
    protected void addChildren() {
        XplNode thead = findChildByTag(sourceNode, "thead");
        XplNode tbody = findChildByTag(sourceNode, "tbody");

        // --- A. CONSTRUIR AS COLUNAS (THEAD) ---
        if (thead != null && !thead.children.isEmpty()) {
            XplNode trHeader = findChildByTag(thead, "tr");
            if (trHeader != null) {
                int colIndex = 0;
                for (XplNode th : trHeader.children) {
                    if (!"th".equalsIgnoreCase(th.tag) && !"td".equalsIgnoreCase(th.tag)) continue;

                    TableColumn<Map<Integer, XplNode>, XplNode> column = new TableColumn<>();
                    final int currentIdx = colIndex;

                    column.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().get(currentIdx)));
                    column.setSortable(sortable);

                    if (sortable) {
                        column.setComparator((node1, node2) -> {
                            String t1 = node1 != null && node1.textContent != null ? node1.textContent : "";
                            String t2 = node2 != null && node2.textContent != null ? node2.textContent : "";
                            try { return Double.compare(Double.parseDouble(t1), Double.parseDouble(t2)); }
                            catch (NumberFormatException e) { return t1.compareToIgnoreCase(t2); }
                        });
                    }

                    // ⭐ RENDERIZADOR W3C DO CABEÇALHO (O TH assume o teu CSS: background: linear-gradient...)
                    Node headerVisuals = TagFactory.create(th).build();
                    column.setGraphic(headerVisuals);

                    // ⭐ RENDERIZADOR W3C DAS CÉLULAS (O TD assume o teu padding, color, borders...)
                    column.setCellFactory(col -> new TableCell<>() {
                        @Override
                        protected void updateItem(XplNode item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setGraphic(null);
                            } else {
                                Node cellVisuals = TagFactory.create(item).build();
                                setGraphic(cellVisuals);
                            }
                        }
                    });

                    tableView.getColumns().add(column);
                    colIndex++;
                }
            }
        }

        // --- B. RENDERIZADOR DE LINHAS (TR) ---
        tableView.setRowFactory(tv -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(Map<Integer, XplNode> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("-fx-background-color: transparent;");
                } else {
                    XplNode firstTd = item.get(0);
                    if (firstTd != null && firstTd.parent != null) {
                        XplNode tr = firstTd.parent;

                        // ⭐ TRADUTOR DO TR W3C: Passa o teu `tr:nth-child(even)` para o JavaFX nativo
                        StringBuilder fxCss = new StringBuilder();
                        com.dic.xsuper.lang.ui.css.W3cCssAdapter.applyW3cToNative(this, tr.style, fxCss);

                        // Garante um efeito hover nativo para substituir o hover lento
                        if (fxCss.isEmpty()) {
                            fxCss.append("-fx-background-color: transparent;");
                        }
                        setStyle(fxCss.toString());
                    }
                }
            }
        });

        // --- C. PREENCHER OS DADOS VIRTUAIS ---
        if (tbody != null) {
            ObservableList<Map<Integer, XplNode>> tableData = FXCollections.observableArrayList();

            for (XplNode tr : tbody.children) {
                if (!"tr".equalsIgnoreCase(tr.tag)) continue;
                Map<Integer, XplNode> rowData = new HashMap<>();
                int colIndex = 0;
                for (XplNode td : tr.children) {
                    if (!"td".equalsIgnoreCase(td.tag) && !"th".equalsIgnoreCase(td.tag)) continue;
                    rowData.put(colIndex, td);
                    colIndex++;
                }
                tableData.add(rowData);
            }
            tableView.setItems(tableData);
        }
    }

    private XplNode findChildByTag(XplNode parent, String tag) {
        for (XplNode child : parent.children) {
            if (child.tag.equalsIgnoreCase(tag)) return child;
        }
        return null;
    }

    @Override
    protected void applyTagSpecificStyles() {}
}