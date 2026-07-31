package com.dic.xsuper.render.javafx.layout.scroll;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.Map;
import java.util.WeakHashMap;

public class DefaultScrollBarRenderer implements ScrollBarRenderer {
    private final Map<ScrollBar, ScrollBarUI> uiMap = new WeakHashMap<>();

    @Override
    public Node renderScrollBar(ScrollBar scrollBar, Pane parent) {
        ScrollBarUI ui = new ScrollBarUI(scrollBar, parent);
        uiMap.put(scrollBar, ui);
        return ui.root;
    }

    @Override
    public void updateScrollBar(ScrollBar scrollBar, Node scrollBarNode) {
        ScrollBarUI ui = uiMap.get(scrollBar);
        if (ui != null) ui.update();
    }

    @Override
    public void removeScrollBar(ScrollBar scrollBar, Node scrollBarNode) {
        ScrollBarUI ui = uiMap.remove(scrollBar);
        if (ui != null) ui.destroy();
    }

    @Override
    public Pane createScrollContainer() {
        Pane container = new Pane();
        container.setClip(new Rectangle());
        container.setStyle("-fx-background-color: transparent;");
        return container;
    }

    @Override
    public void applyClip(Pane container, double width, double height) {
        Rectangle clip = (Rectangle) container.getClip();
        if (clip == null) {
            clip = new Rectangle(width, height);
            container.setClip(clip);
        } else {
            clip.setWidth(width);
            clip.setHeight(height);
        }
    }

    private class ScrollBarUI {
        final ScrollBar scrollBar;
        final Pane parent;
        final Region root;
        final Region track;
        final Region thumb;
        FadeTransition fadeIn, fadeOut;
        boolean isHovered = false;

        ScrollBarUI(ScrollBar scrollBar, Pane parent) {
            this.scrollBar = scrollBar;
            this.parent = parent;
            root = new Region();
            root.setMouseTransparent(false);
            track = new Region();
            thumb = new Region();
            //root.addaddAll(track, thumb);
            parent.getChildren().add(root);
            applyTheme();
            setupAutoHide();
            update();
        }

        private void applyTheme() {
            ScrollTheme theme = scrollBar.getTheme();
            track.setStyle(buildBackground(theme.getTrackBackground()) +
                    buildCornerRadius(theme.getTrackCornerRadius()));
            updateThumbStyle(ScrollBar.State.IDLE);
        }

        private void updateThumbStyle(ScrollBar.State state) {
            ScrollTheme theme = scrollBar.getTheme();
            Paint bg = switch (state) {
                case HOVER, DRAGGING -> theme.getThumbHoverBackground();
                case PRESSED -> theme.getThumbPressedBackground();
                default -> theme.getThumbBackground();
            };
            thumb.setStyle(buildBackground(bg) +
                    buildCornerRadius(theme.getThumbCornerRadius()));
        }

        private String buildBackground(Paint paint) {
            if (paint == null) return "-fx-background-color: transparent;";
            return "-fx-background-color: " + paintToString(paint) + ";";
        }

        private String paintToString(Paint paint) {
            if (paint instanceof javafx.scene.paint.Color c) {
                return String.format("rgba(%d,%d,%d,%.2f)",
                        (int)(c.getRed()*255), (int)(c.getGreen()*255),
                        (int)(c.getBlue()*255), c.getOpacity());
            } else if (paint instanceof javafx.scene.paint.LinearGradient lg) {
                return "linear-gradient(to bottom right, " +
                        paintToString(lg.getStops().get(0).getColor()) + ", " +
                        paintToString(lg.getStops().get(1).getColor()) + ")";
            }
            return "transparent";
        }

        private String buildCornerRadius(double radius) {
            return radius > 0 ? "-fx-background-radius: " + radius + "px;" : "";
        }

        private void setupAutoHide() {
            ScrollTheme theme = scrollBar.getTheme();
            if (theme.isAutoHide()) {
                root.setOpacity(0);
                fadeIn = new FadeTransition(Duration.millis(theme.getFadeInDuration()), root);
                fadeIn.setToValue(1.0);
                fadeOut = new FadeTransition(Duration.millis(theme.getFadeOutDuration()), root);
                fadeOut.setToValue(0.0);
                fadeOut.setDelay(Duration.millis(theme.getAutoHideDelay()));
                root.setOnMouseEntered(e -> { show(); cancelAutoHide(); });
                root.setOnMouseExited(e -> scheduleAutoHide());
                scheduleAutoHide();
            } else {
                root.setOpacity(1.0);
            }
        }

        void show() {
            if (fadeIn != null) { fadeOut.stop(); fadeIn.playFromStart(); }
            else root.setOpacity(1.0);
            scrollBar.setVisible(true);
        }

        void scheduleAutoHide() {
            if (fadeOut != null && !isHovered) {
                fadeIn.stop();
                fadeOut.playFromStart();
                scrollBar.setVisible(false);
            }
        }

        void cancelAutoHide() {
            if (fadeOut != null) fadeOut.stop();
        }

        void update() {
            ScrollTheme theme = scrollBar.getTheme();
            double width = theme.getWidth();
            if (scrollBar.isVertical()) {
                root.setPrefWidth(width);
                root.setPrefHeight(parent.getHeight());
                track.setLayoutX(0); track.setLayoutY(0);
                track.setPrefWidth(width); track.setPrefHeight(parent.getHeight());
                double thumbPos = scrollBar.getThumbPosition();
                double thumbLen = scrollBar.getThumbLength();
                thumb.setLayoutX(0); thumb.setLayoutY(thumbPos);
                thumb.setPrefWidth(width); thumb.setPrefHeight(thumbLen);
            } else {
                root.setPrefHeight(width);
                root.setPrefWidth(parent.getWidth());
                track.setLayoutX(0); track.setLayoutY(0);
                track.setPrefWidth(parent.getWidth()); track.setPrefHeight(width);
                double thumbPos = scrollBar.getThumbPosition();
                double thumbLen = scrollBar.getThumbLength();
                thumb.setLayoutX(thumbPos); thumb.setLayoutY(0);
                thumb.setPrefWidth(thumbLen); thumb.setPrefHeight(width);
            }
            updateThumbStyle(scrollBar.getState());
            root.setVisible(scrollBar.isVisible());
            root.setOpacity(scrollBar.getOpacity());
        }

        void destroy() {
            parent.getChildren().remove(root);
            if (fadeIn != null) fadeIn.stop();
            if (fadeOut != null) fadeOut.stop();
        }
    }
}