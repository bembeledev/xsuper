package com.dic.xsuper.render.javafx.layout.scroll;

import com.dic.xsuper.dom.node.XplElement;
import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

public class ScrollIntegration {
    private final ScrollEngine engine;

    public ScrollIntegration(ScrollEngine engine) {
        this.engine = engine;
    }

    public Node wrapWithScrollIfNeeded(XplNode node, Node contentNode) {
        if (node == null || contentNode == null) return contentNode;
        if (node.liveElement == null) return contentNode;

        XplElement element = node.liveElement;
        String overflowX = element.getOverflowX();
        String overflowY = element.getOverflowY();

        boolean needsV = "scroll".equals(overflowY) || "auto".equals(overflowY);
        boolean needsH = "scroll".equals(overflowX) || "auto".equals(overflowX);



        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(contentNode);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(needsH ? ScrollPane.ScrollBarPolicy.AS_NEEDED : ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(needsV ? ScrollPane.ScrollBarPolicy.AS_NEEDED : ScrollPane.ScrollBarPolicy.NEVER);

        applyTheme(scrollPane, node);
        bindScrollManager(element, scrollPane);



        // Atualizar contexto
        ScrollContext ctx = engine.getContext(element);
        ctx.setViewportSize(scrollPane.getViewportBounds().getWidth(), scrollPane.getViewportBounds().getHeight());

        return scrollPane;
    }

    private void applyTheme(ScrollPane scrollPane, XplNode node) {
        String themeName = (String) node.attributes.getOrDefault("scroll-theme", "dark");
        ScrollTheme theme;
        switch (themeName) {
            case "light": theme = ScrollTheme.lightModern(); break;
            case "minimal": theme = ScrollTheme.minimal(); break;
            case "gradient": theme = ScrollTheme.gradientTheme(
                    javafx.scene.paint.Color.web("#667eea"),
                    javafx.scene.paint.Color.web("#764ba2")); break;
            default: theme = ScrollTheme.darkModern(); break;
        }
        String css = generateScrollPaneCss(theme);
        scrollPane.setStyle(css);
    }

    private String generateScrollPaneCss(ScrollTheme theme) {
        StringBuilder sb = new StringBuilder();
        sb.append("-fx-background-color: transparent;");
        sb.append("-fx-background: transparent;");
        double w = theme.getWidth();
        sb.append(".scroll-bar:vertical { -fx-pref-width: ").append(w).append("px; }");
        sb.append(".scroll-bar:horizontal { -fx-pref-height: ").append(w).append("px; }");

        String thumbBg = paintToCss(theme.getThumbBackground());
        sb.append(".scroll-bar .thumb {");
        sb.append("  -fx-background-color: ").append(thumbBg).append(";");
        sb.append("  -fx-background-radius: ").append(theme.getThumbCornerRadius()).append("px;");
        sb.append("}");
        if (theme.getThumbHoverBackground() != null) {
            sb.append(".scroll-bar .thumb:hover {");
            sb.append("  -fx-background-color: ").append(paintToCss(theme.getThumbHoverBackground())).append(";");
            sb.append("}");
        }
        sb.append(".scroll-bar .track { -fx-background-color: transparent; }");
        sb.append(".scroll-bar .increment-button, .scroll-bar .decrement-button {");
        sb.append("  -fx-background-color: transparent;");
        sb.append("  -fx-pref-width: 0; -fx-pref-height: 0; -fx-padding: 0;");
        sb.append("}");
        sb.append(".scroll-bar .increment-arrow, .scroll-bar .decrement-arrow {");
        sb.append("  -fx-shape: \" \"; -fx-padding: 0;");
        sb.append("}");
        return sb.toString();
    }

    private String paintToCss(javafx.scene.paint.Paint paint) {
        if (paint instanceof javafx.scene.paint.Color c) {
            return String.format("rgba(%d,%d,%d,%.2f)",
                    (int)(c.getRed()*255), (int)(c.getGreen()*255),
                    (int)(c.getBlue()*255), c.getOpacity());
        } else if (paint instanceof javafx.scene.paint.LinearGradient lg) {
            return "linear-gradient(to bottom right, " +
                    paintToCss(lg.getStops().get(0).getColor()) + ", " +
                    paintToCss(lg.getStops().get(1).getColor()) + ")";
        }
        return "transparent";
    }

    private void bindScrollManager(XplElement element, ScrollPane scrollPane) {
        scrollPane.vvalueProperty().addListener((obs, old, val) -> {
            double total = scrollPane.getContent().getBoundsInLocal().getHeight() - scrollPane.getViewportBounds().getHeight();
            if (total > 0) {
                element.setScrollTop((float) (val.doubleValue() * total));
            }
        });
        scrollPane.hvalueProperty().addListener((obs, old, val) -> {
            double total = scrollPane.getContent().getBoundsInLocal().getWidth() - scrollPane.getViewportBounds().getWidth();
            if (total > 0) {
                element.setScrollLeft((float) (val.doubleValue() * total));
            }
        });
    }
}