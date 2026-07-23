package com.dic.xsuper.lang.ui.layout.scroll;

public class ScrollBar {
    public enum Orientation { VERTICAL, HORIZONTAL }
    public enum State { IDLE, HOVER, PRESSED, DRAGGING, DISABLED }

    private final Orientation orientation;
    private final String elementId;

    // Geometria (coordenadas locais)
    private double trackX, trackY, trackWidth, trackHeight;
    private double thumbX, thumbY, thumbWidth, thumbHeight;

    // Valores
    private double minValue = 0;
    private double maxValue = 100;
    private double currentValue = 0;
    private double visibleSize = 50;
    private double totalSize = 100;

    // Estado
    private State state = State.IDLE;
    private boolean visible = true;
    private double opacity = 1.0;
    private ScrollTheme theme;

    public ScrollBar(Orientation orientation, String elementId) {
        this.orientation = orientation;
        this.elementId = elementId;
        this.theme = ScrollTheme.darkModern();
    }

    // Getters/Setters
    public Orientation getOrientation() { return orientation; }
    public String getElementId() { return elementId; }
    public double getTrackX() { return trackX; }
    public double getTrackY() { return trackY; }
    public double getTrackWidth() { return trackWidth; }
    public double getTrackHeight() { return trackHeight; }
    public void setTrack(double x, double y, double w, double h) {
        this.trackX = x; this.trackY = y; this.trackWidth = w; this.trackHeight = h;
    }
    public double getThumbX() { return thumbX; }
    public double getThumbY() { return thumbY; }
    public double getThumbWidth() { return thumbWidth; }
    public double getThumbHeight() { return thumbHeight; }
    public void setThumb(double x, double y, double w, double h) {
        this.thumbX = x; this.thumbY = y; this.thumbWidth = w; this.thumbHeight = h;
    }
    public double getMinValue() { return minValue; }
    public void setMinValue(double minValue) { this.minValue = minValue; }
    public double getMaxValue() { return maxValue; }
    public void setMaxValue(double maxValue) { this.maxValue = maxValue; }
    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) {
        this.currentValue = Math.max(minValue, Math.min(maxValue, currentValue));
    }
    public double getVisibleSize() { return visibleSize; }
    public void setVisibleSize(double visibleSize) { this.visibleSize = visibleSize; }
    public double getTotalSize() { return totalSize; }
    public void setTotalSize(double totalSize) { this.totalSize = totalSize; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public double getOpacity() { return opacity; }
    public void setOpacity(double opacity) { this.opacity = opacity; }
    public ScrollTheme getTheme() { return theme; }
    public void setTheme(ScrollTheme theme) { if (theme != null) this.theme = theme; }

    public boolean isVertical() { return orientation == Orientation.VERTICAL; }
    public boolean isHorizontal() { return orientation == Orientation.HORIZONTAL; }

    // Hit testing
    public boolean isPointOnTrack(double x, double y) {
        return x >= trackX && x <= trackX + trackWidth &&
                y >= trackY && y <= trackY + trackHeight;
    }

    public boolean isPointOnThumb(double x, double y) {
        return x >= thumbX && x <= thumbX + thumbWidth &&
                y >= thumbY && y <= thumbY + thumbHeight;
    }

    // Cálculo da posição do thumb
    public double getThumbPosition() {
        double trackLen = isVertical() ? trackHeight : trackWidth;
        double thumbLen = Math.max(theme.getThumbMinSize(),
                Math.min(trackLen, trackLen * (visibleSize / totalSize)));
        double range = trackLen - thumbLen;
        if (range <= 0) return 0;
        double ratio = (currentValue - minValue) / (maxValue - minValue);
        return ratio * range;
    }

    public double getThumbLength() {
        double trackLen = isVertical() ? trackHeight : trackWidth;
        return Math.max(theme.getThumbMinSize(),
                Math.min(trackLen, trackLen * (visibleSize / totalSize)));
    }

    // Valor a partir de posição no track
    public double valueFromPosition(double x, double y) {
        double pos;
        if (isVertical()) {
            pos = (y - trackY) / trackHeight;
        } else {
            pos = (x - trackX) / trackWidth;
        }
        pos = Math.max(0, Math.min(1, pos));
        return minValue + pos * (maxValue - minValue);
    }

    @Override
    public String toString() {
        return String.format("ScrollBar{%s, id='%s', value=%.2f, state=%s}",
                orientation, elementId, currentValue, state);
    }
}