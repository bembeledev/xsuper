package com.dic.xsuper.lang.ui.layout.panes;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;
import com.dic.xsuper.lang.ui.properties.cssunit.CssValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

import java.util.*;

public class TableContainerPane extends GridPane implements CustomLayoutPane {

    private final Map<String, String> style;
    private boolean borderCollapse = false;

    // Armazena a largura máxima de cada coluna (para calcular colunas)
    private int maxCols = 0;

    public TableContainerPane(Map<String, String> style) {
        this.style = style != null ? style : Map.of();
        parseBorderCollapse();
        // Remove o espaçamento padrão do GridPane
        setHgap(0);
        setVgap(0);
        // Para border-collapse: collapse, as células não devem ter padding extra
        if (borderCollapse) {
            setStyle("-fx-border-width: 0; -fx-border-insets: 0;");
        }
    }

    private void parseBorderCollapse() {
        String bc = style.getOrDefault("border-collapse", "separate").toLowerCase().trim();
        borderCollapse = "collapse".equals(bc);
    }

    @Override
    public void populateChildren(List<NativeTag> children, CssContext context) {
        // Primeiro, vamos extrair a estrutura completa: linhas e células com seus spans
        TableStructure structure = extractStructure(children, context);
        // Agora, construir o GridPane com base nessa estrutura
        buildGrid(structure, context);
    }

    /**
     * Extrai a estrutura da tabela (linhas, células, colspan, rowspan, alinhamento)
     * e também guarda referências para os nós JavaFX.
     */
    private TableStructure extractStructure(List<NativeTag> nodes, CssContext context) {
        TableStructure structure = new TableStructure();
        int currentRow = 0;

        for (NativeTag node : nodes) {
            String tag = node.getSourceNode().tag.toLowerCase();
            String display = node.getResolvedStyles().display.toLowerCase().trim();

            // Ignora caption aqui (será tratado separadamente)
            if ("caption".equals(tag)) {
                structure.captionNode = node;
                continue;
            }

            // Grupos (thead, tbody, tfoot) – mergulha nos filhos
            if ("thead".equals(tag) || "tbody".equals(tag) || "tfoot".equals(tag) || "contents".equals(display)) {
                TableStructure sub = extractStructure(node.getChildren(), context);
                structure.merge(sub, currentRow);
                currentRow += sub.rowCount;
                continue;
            }

            // Linha (tr)
            if ("table-row".equals(display) || "tr".equals(tag)) {
                TableRow row = new TableRow();
                int colIndex = 0;
                for (NativeTag cellNode : node.getChildren()) {
                    String cellTag = cellNode.getSourceNode().tag.toLowerCase();
                    if (!"td".equals(cellTag) && !"th".equals(cellTag)) continue;

                    // Lê colspan e rowspan
                    int colspan = 1;
                    int rowspan = 1;
                    try {
                        String colSpanAttr = cellNode.getSourceNode().attributes.getOrDefault("colspan", "1").toString();
                        colspan = Integer.parseInt(colSpanAttr);
                    } catch (NumberFormatException ignored) {}
                    try {
                        String rowSpanAttr = cellNode.getSourceNode().attributes.getOrDefault("rowspan", "1").toString();
                        rowspan = Integer.parseInt(rowSpanAttr);
                    } catch (NumberFormatException ignored) {}

                    // Lê alinhamentos
                    HPos hAlign = parseHorizontalAlignment(cellNode);
                    VPos vAlign = parseVerticalAlignment(cellNode);

                    // Constrói o nó JavaFX
                    Node fxNode = cellNode.build();

                    // Aplica estilos de borda (se collapse, remove bordas duplas)
                    if (borderCollapse) {
                        fxNode.setStyle((fxNode.getStyle() != null ? fxNode.getStyle() : "") +
                                "; -fx-border-insets: 0; -fx-background-insets: 0;");
                    }

                    TableCell cell = new TableCell(fxNode, colIndex, colspan, rowspan, hAlign, vAlign);
                    row.cells.add(cell);

                    // Avança o índice da coluna, considerando colunas ocupadas
                    // Para simplificar, usamos um mapa de ocupação para rowspan
                    colIndex += colspan;
                }
                structure.rows.add(row);
                currentRow++;
                // Atualiza o número máximo de colunas
                maxCols = Math.max(maxCols, row.cells.size());
            } else {
                // Qualquer outro elemento (ex: caption) é tratado como uma célula de largura total
                Node fxNode = node.build();
                TableCell cell = new TableCell(fxNode, 0, 1, 1, HPos.CENTER, VPos.CENTER);
                TableRow row = new TableRow();
                row.cells.add(cell);
                structure.rows.add(row);
                currentRow++;
            }
        }
        return structure;
    }

    /**
     * Constrói o GridPane a partir da estrutura extraída, respeitando spans.
     */
    private void buildGrid(TableStructure structure, CssContext context) {
        // Limpa constraints anteriores
        getColumnConstraints().clear();
        getRowConstraints().clear();

        // Se houver caption, adiciona como primeira linha
        if (structure.captionNode != null) {
            Node captionNode = structure.captionNode.build();
            // A caption ocupa toda a largura
            this.add(captionNode, 0, 0);
            GridPane.setColumnSpan(captionNode, GridPane.REMAINING);
            // Posiciona no topo (ou bottom, conforme atributo)
            String captionSide = structure.captionNode.getSourceNode().attributes.getOrDefault("caption-side", "top").toString();
            if ("bottom".equalsIgnoreCase(captionSide)) {
                // Adicionamos no final, mas precisamos saber o total de linhas depois
                // Por simplicidade, adicionamos depois de todas as linhas
                // Vamos guardar para adicionar no final.
                structure.captionNode = null; // já foi processado, mas guardamos referência
            }
            // Na verdade, vamos adicionar a caption no final do método.
        }

        // Calcula o número total de linhas (incluindo rowspans)
        int rowCount = structure.rows.size();

        // Cria constraints para colunas (baseado em larguras definidas via CSS)
        applyColumnConstraints(structure, context);

        // Cria constraints para linhas (alturas)
        applyRowConstraints(structure, context);

        // Posiciona as células
        int rowIndex = 0;
        for (TableRow row : structure.rows) {
            int colIndex = 0;
            for (TableCell cell : row.cells) {
                // Ajusta a posição se houver células com rowspan ocupando esta posição
                // Para simplificar, usamos um mapa de ocupação (grid[row][col] = ocupado)
                // Vamos implementar um método auxiliar para encontrar a próxima posição livre.
                int[] pos = findNextFreePosition(structure, rowIndex, colIndex);
                colIndex = pos[1];

                // Adiciona a célula
                this.add(cell.node, colIndex, rowIndex, cell.colspan, cell.rowspan);
                GridPane.setHalignment(cell.node, cell.hAlign);
                GridPane.setValignment(cell.node, cell.vAlign);

                // Marca a área ocupada (para rowspan)
                markOccupied(structure, rowIndex, colIndex, cell.rowspan, cell.colspan);

                colIndex += cell.colspan;
            }
            rowIndex++;
        }

        // Adiciona a caption no final (se existir)
        if (structure.captionNode != null) {
            Node captionNode = structure.captionNode.build();
            this.add(captionNode, 0, rowCount);
            GridPane.setColumnSpan(captionNode, GridPane.REMAINING);
            GridPane.setHalignment(captionNode, HPos.CENTER);
        }
    }

    // ─── Métodos auxiliares para posicionamento ─────────────────────────────

    private int[] findNextFreePosition(TableStructure structure, int row, int col) {
        // Verifica se a posição (row, col) está ocupada por um rowspan de uma célula anterior
        while (structure.isOccupied(row, col)) {
            col++;
        }
        return new int[]{row, col};
    }

    private void markOccupied(TableStructure structure, int row, int col, int rowspan, int colspan) {
        for (int r = row; r < row + rowspan; r++) {
            for (int c = col; c < col + colspan; c++) {
                structure.occupied.put(r + "," + c, true);
            }
        }
    }

    // ─── Aplicação de constraints ────────────────────────────────────────────

    private void applyColumnConstraints(TableStructure structure, CssContext context) {
        // Se houver definição de larguras via CSS (grid-template-columns ou width)
        // Podemos extrair de style ou de colgroup
        // Por simplicidade, criamos constraints baseadas no número máximo de colunas
        int colCount = maxCols;
        for (int i = 0; i < colCount; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            // Define largura mínima e preferida
            cc.setFillWidth(true);
            // Se houver definição de largura, aplica
            // Exemplo: style.get("grid-template-columns") ou width nas colunas
            // Aqui podemos ler de style e aplicar
            getColumnConstraints().add(cc);
        }
    }

    private void applyRowConstraints(TableStructure structure, CssContext context) {
        int rowCount = structure.rows.size();
        for (int i = 0; i < rowCount; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setFillHeight(true);
            getRowConstraints().add(rc);
        }
    }

    // ─── Parsing de alinhamentos ────────────────────────────────────────────

    private HPos parseHorizontalAlignment(NativeTag cell) {
        String align = cell.getSourceNode().attributes.getOrDefault("align", "left").toString().toLowerCase();
        String textAlign = cell.getRawStyles().getOrDefault("text-align", align);
        return switch (textAlign) {
            case "right" -> HPos.RIGHT;
            case "center" -> HPos.CENTER;
            case "justify" -> HPos.CENTER; // fallback
            default -> HPos.LEFT;
        };
    }

    private VPos parseVerticalAlignment(NativeTag cell) {
        String valign = cell.getSourceNode().attributes.getOrDefault("valign", "middle").toString().toLowerCase();
        String verticalAlign = cell.getRawStyles().getOrDefault("vertical-align", valign);
        return switch (verticalAlign) {
            case "top" -> VPos.TOP;
            case "bottom" -> VPos.BOTTOM;
            case "baseline" -> VPos.BASELINE;
            default -> VPos.CENTER;
        };
    }

    // ─── Estruturas internas ────────────────────────────────────────────────

    private static class TableStructure {
        List<TableRow> rows = new ArrayList<>();
        Map<String, Boolean> occupied = new HashMap<>();
        int rowCount = 0;
        NativeTag captionNode = null;

        void merge(TableStructure other, int offsetRow) {
            for (TableRow row : other.rows) {
                this.rows.add(row);
            }
            this.rowCount += other.rowCount;
        }

        boolean isOccupied(int row, int col) {
            return occupied.containsKey(row + "," + col);
        }
    }

    private static class TableRow {
        List<TableCell> cells = new ArrayList<>();
    }

    private static class TableCell {
        Node node;
        int colIndex;
        int colspan;
        int rowspan;
        HPos hAlign;
        VPos vAlign;

        TableCell(Node node, int colIndex, int colspan, int rowspan, HPos hAlign, VPos vAlign) {
            this.node = node;
            this.colIndex = colIndex;
            this.colspan = colspan;
            this.rowspan = rowspan;
            this.hAlign = hAlign;
            this.vAlign = vAlign;
        }
    }
}