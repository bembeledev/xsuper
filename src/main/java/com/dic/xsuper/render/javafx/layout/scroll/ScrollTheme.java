package com.dic.xsuper.render.javafx.layout.scroll;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.image.Image;

public class ScrollTheme {
    private Paint trackBackground = Color.TRANSPARENT;
    private double trackCornerRadius = 0;
    private Paint thumbBackground = Color.rgb(71, 85, 105, 0.7);
    private Paint thumbHoverBackground = Color.rgb(148, 163, 184, 0.8);
    private Paint thumbPressedBackground = Color.rgb(100, 116, 139, 0.9);
    private double thumbCornerRadius = 6;
    private double thumbMinSize = 20;
    private double width = 10;
    private double minWidth = 6;
    private double maxWidth = 20;
    private boolean autoHide = true;
    private double fadeInDuration = 150;
    private double fadeOutDuration = 1000;
    private double autoHideDelay = 2000;
    private boolean showButtons = false;
    private double buttonSize = 14;
    private Paint buttonColor = Color.rgb(100, 100, 100, 0.5);
    private Paint buttonHoverColor = Color.rgb(150, 150, 150, 0.7);

    public ScrollTheme() {}

    public static ScrollTheme darkModern() {
        ScrollTheme t = new ScrollTheme();
        t.thumbBackground = Color.rgb(71, 85, 105, 0.7);
        t.thumbHoverBackground = Color.rgb(148, 163, 184, 0.8);
        t.thumbCornerRadius = 6;
        t.width = 10;
        t.autoHide = true;
        return t;
    }

    public static ScrollTheme lightModern() {
        ScrollTheme t = new ScrollTheme();
        t.thumbBackground = Color.rgb(200, 200, 200, 0.7);
        t.thumbHoverBackground = Color.rgb(180, 180, 180, 0.8);
        t.thumbCornerRadius = 6;
        t.width = 10;
        t.autoHide = true;
        return t;
    }

    public static ScrollTheme gradientTheme(Color c1, Color c2) {
        ScrollTheme t = new ScrollTheme();
        LinearGradient grad = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, c1), new Stop(1, c2));
        t.thumbBackground = grad;
        t.thumbHoverBackground = grad;
        t.thumbCornerRadius = 6;
        t.width = 12;
        t.autoHide = true;
        return t;
    }

    public static ScrollTheme minimal() {
        ScrollTheme t = new ScrollTheme();
        t.thumbBackground = Color.rgb(150, 150, 150, 0.3);
        t.thumbCornerRadius = 3;
        t.width = 6;
        t.autoHide = true;
        t.fadeInDuration = 100;
        t.fadeOutDuration = 800;
        return t;
    }

    // Getters
    public Paint getTrackBackground() { return trackBackground; }
    public double getTrackCornerRadius() { return trackCornerRadius; }
    public Paint getThumbBackground() { return thumbBackground; }
    public Paint getThumbHoverBackground() { return thumbHoverBackground; }
    public Paint getThumbPressedBackground() { return thumbPressedBackground; }
    public double getThumbCornerRadius() { return thumbCornerRadius; }
    public double getThumbMinSize() { return thumbMinSize; }
    public double getWidth() { return width; }
    public double getMinWidth() { return minWidth; }
    public double getMaxWidth() { return maxWidth; }
    public boolean isAutoHide() { return autoHide; }
    public double getFadeInDuration() { return fadeInDuration; }
    public double getFadeOutDuration() { return fadeOutDuration; }
    public double getAutoHideDelay() { return autoHideDelay; }
    public boolean isShowButtons() { return showButtons; }
    public double getButtonSize() { return buttonSize; }
    public Paint getButtonColor() { return buttonColor; }
    public Paint getButtonHoverColor() { return buttonHoverColor; }

    // Setters (fluent)
    public ScrollTheme withThumbBackground(Paint paint) { this.thumbBackground = paint; return this; }
    public ScrollTheme withThumbHoverBackground(Paint paint) { this.thumbHoverBackground = paint; return this; }
    public ScrollTheme withWidth(double width) { this.width = Math.max(minWidth, Math.min(maxWidth, width)); return this; }
    public ScrollTheme withAutoHide(boolean autoHide) { this.autoHide = autoHide; return this; }
    public ScrollTheme withThumbCornerRadius(double radius) { this.thumbCornerRadius = radius; return this; }
    public ScrollTheme withGradient(Color c1, Color c2) {
        this.thumbBackground = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, c1), new Stop(1, c2));
        return this;
    }
    public ScrollTheme withImage(String url) {
        try {
            this.thumbBackground = new ImagePattern(new Image(url));
        } catch (Exception ignored) {}
        return this;
    }
}