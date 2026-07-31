package com.dic.xsuper.render.javafx.tags.table;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.render.javafx.tags.TagFactory;
import com.dic.xsuper.render.javafx.css.W3cCssAdapter;
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
    private boolean sortable = true;
    private boolean columnsMenu = false;
    private boolean multiSelect = false;
    private boolean draggableRows = false; // ⭐ Nova propriedade de controlo

    public TableTag(XplNode sourceNode) {
        super(sourceNode);
        parseTableAttributes();
    }

    private void parseTableAttributes() {
        if (sourceNode.attributes.containsKey("sortable")) sortable = !"false".equalsIgnoreCase(sourceNode.attributes.get("sortable").toString());
        if (sourceNode.attributes.containsKey("columns-menu")) columnsMenu = "true".equalsIgnoreCase(sourceNode.attributes.get("columns-menu").toString());
        if (sourceNode.attributes.containsKey("selection")) multiSelect = "multiple".equalsIgnoreCase(sourceNode.attributes.get("selection").toString());
        // Lógica para ativar o Drag & Drop através do HTML
        if (sourceNode.attributes.containsKey("draggable")) draggableRows = "true".equalsIgnoreCase(sourceNode.attributes.get("draggable").toString());
    }

    @Override
    protected Node createNode() {
        tableView = new TableView<>();
        tableView.setTableMenuButtonVisible(columnsMenu);
        tableView.getSelectionModel().setSelectionMode(multiSelect ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        String resetCss = """
            .table-view { -fx-background-color: transparent; -fx-padding: 0; }
            .column-header-background { -fx-background-color: transparent; }
            .column-header { -fx-background-color: transparent; -fx-padding: 0; }
            .column-header .label { -fx-padding: 0; -fx-content-display: graphic-only; -fx-alignment: center; }
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

                    String headerText = th.textContent != null ? th.textContent : "";
                    if (headerText.isEmpty() && !th.children.isEmpty()) {
                        for (XplNode c : th.children) {
                            if ("#text".equals(c.tag) && c.textContent != null) headerText += c.textContent;
                        }
                    }
                    column.setText(headerText.trim());

                    if (sortable) {
                        column.setComparator((node1, node2) -> {
                            String t1 = node1 != null && node1.textContent != null ? node1.textContent : "";
                            if (t1.isEmpty() && node1 != null && !node1.children.isEmpty() && "#text".equals(node1.children.get(0).tag)) t1 = node1.children.get(0).textContent;

                            String t2 = node2 != null && node2.textContent != null ? node2.textContent : "";
                            if (t2.isEmpty() && node2 != null && !node2.children.isEmpty() && "#text".equals(node2.children.get(0).tag)) t2 = node2.children.get(0).textContent;

                            try { return Double.compare(Double.parseDouble(t1), Double.parseDouble(t2)); }
                            catch (NumberFormatException e) { return t1.compareToIgnoreCase(t2); }
                        });
                    }

                    Node headerVisuals = TagFactory.create(th).build();

                    if (headerVisuals instanceof javafx.scene.layout.Region r) {
                        r.setMaxWidth(Double.MAX_VALUE);
                        r.setMinHeight(45);
                        r.prefWidthProperty().bind(column.widthProperty());
                    }

                    column.setGraphic(headerVisuals);

                    column.setCellFactory(col -> new TableCell<>() {
                        @Override
                        protected void updateItem(XplNode item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setGraphic(null);
                                setText(null);
                            } else {
                                Node cellVisuals = TagFactory.create(item).build();
                                setGraphic(cellVisuals);
                                setText(null);
                            }
                        }
                    });

                    tableView.getColumns().add(column);
                    colIndex++;
                }
            }
        }

        // --- B. RENDERIZADOR DE LINHAS (TR) COM CONTROLO HTML ---
        tableView.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Map<Integer, XplNode>> row = new javafx.scene.control.TableRow<>() {
                @Override
                protected void updateItem(Map<Integer, XplNode> item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        XplNode firstTd = item.get(0);
                        if (firstTd != null && firstTd.parent != null) {
                            XplNode tr = firstTd.parent;
                            StringBuilder fxCss = new StringBuilder();
                            W3cCssAdapter.applyW3cToNative(this, tr.style, fxCss);

                            if (fxCss.isEmpty()) {
                                fxCss.append("-fx-background-color: transparent;");
                            }
                            setStyle(fxCss.toString());
                        }
                    }
                }
            };

            // ⭐ O Drag & Drop agora só é instanciado se o HTML permitir!
            if (draggableRows) {
                row.setOnDragDetected(event -> {
                    if (!row.isEmpty()) {
                        Integer index = row.getIndex();
                        javafx.scene.input.Dragboard db = row.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
                        db.setDragView(row.snapshot(null, null));
                        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                        cc.putString(String.valueOf(index));
                        db.setContent(cc);
                        event.consume();
                    }
                });

                row.setOnDragOver(event -> {
                    if (event.getGestureSource() != row && event.getDragboard().hasString()) {
                        event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
                    }
                    event.consume();
                });

                row.setOnDragDropped(event -> {
                    javafx.scene.input.Dragboard db = event.getDragboard();
                    if (db.hasString()) {
                        int draggedIndex = Integer.parseInt(db.getString());
                        int dropIndex = row.isEmpty() ? tv.getItems().size() : row.getIndex();

                        if (draggedIndex != dropIndex) {
                            Map<Integer, XplNode> draggedItem = tv.getItems().get(draggedIndex);
                            tv.getItems().remove(draggedIndex);

                            if (draggedIndex < dropIndex && dropIndex < tv.getItems().size()) {
                                dropIndex--;
                            }

                            tv.getItems().add(dropIndex, draggedItem);
                            tv.getSelectionModel().select(dropIndex);
                        }
                        event.setDropCompleted(true);
                    } else {
                        event.setDropCompleted(false);
                    }
                    event.consume();
                });
            }

            return row;
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