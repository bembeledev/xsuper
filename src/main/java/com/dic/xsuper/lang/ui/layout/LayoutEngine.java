package com.dic.xsuper.lang.ui.layout;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import java.util.Map;

public class LayoutEngine {

    public static LayoutManager resolveLayout(NativeTag tag) {
        String display = tag.getResolvedStyles().display.toLowerCase().trim();
        Map<String, String> rawStyles = tag.getRawStyles();
        String position = rawStyles.getOrDefault("position", "static").toLowerCase().trim();

        // 1. Z-Index / Sobreposição ganha sempre (Absolute/Relative)
        if ("absolute".equals(position) || "relative".equals(position)) {
            return new StackLayout(); // Mapeia para o teu Layer/Stack
        }

        // 2. O Roteador Universal de Display
        return switch (display) {
            // -- Inivísivel --
            case "none" -> new NoneLayout();

            // -- Flexbox --
            case "flex", "inline-flex" -> {
                if ("wrap".equals(rawStyles.getOrDefault("flex-wrap", "nowrap"))) {
                    yield new FlowLayout();
                }
                yield new FlexLayout();
            }

            // -- Grid --
            case "grid" -> new GridLayout();
            case "inline-grid" -> new InlineGridLayout(); // ⭐ Agora tem identidade própria!


            // -- Tabular --
            case "table" -> new TableLayout();

            // As linhas funcionam como blocos se usadas fora de uma tabela.
            // Dentro de uma tabela, o TableContainerPane intercepta e extrai as suas células!
            case "table-row", "table-cell" -> new BlockLayout();

            // -- Texto e Elementos em Linha --
            case "inline" -> new InlineLayout(); // TextFlow (Mistura texto com links na mesma linha)
            case "inline-block" -> new InlineBlockLayout(); // Bloco rígido mas que encolhe a si próprio
            case "contents" -> new ContentsLayout(); // Caixa fantasma (Pass-through)

            // -- Padrão (Bloco) --
            case "block", "flow-root" -> new BlockLayout();

            // -- Modos Especiais Xplorer --
            case "layer" -> new LayerLayout(); // ⭐ Motor avançado de camadas!
            case "border" -> new BorderLayout();

            default -> new BlockLayout();
        };
    }
}