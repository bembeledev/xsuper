package com.dic.xsuper.lang.ui.properties.gradient;

import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;
import com.dic.xsuper.lang.ui.properties.cssunit.CssValue;
import com.sun.prism.ps.Shader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Gradient {
    private final GradientType type;
    private final List<GradientStop> stops;
    private final float angle;           // para linear (graus)
    private final String direction;      // "to right", "to bottom left", etc.
    private final float centerX, centerY; // centro para radial/conic
    private final float radiusX, radiusY; // raio para radial (ellipse)
    private final boolean isCircle;      // se radial é circle
    private final float startOffset;     // para repeating
    private float softness; // 0.0 a 1.0 (default 0.5)

    // ─── Construtor privado (usar builder) ────────────────────────────

    private Gradient(GradientType type, List<GradientStop> stops,
                     float angle, String direction,
                     float centerX, float centerY,
                     float radiusX, float radiusY, boolean isCircle,
                     float startOffset, float softness) {
        this.type = type;
        this.stops = stops != null ? new ArrayList<>(stops) : Collections.emptyList();
        this.angle = angle;
        this.direction = direction;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.isCircle = isCircle;
        this.startOffset = startOffset;
        this.softness = softness;
    }

    // ─── Getters ──────────────────────────────────────────────────────

    public GradientType getType() { return type; }
    public List<GradientStop> getStops() { return stops; }
    public float getAngle() { return angle; }
    public String getDirection() { return direction; }
    public float getCenterX() { return centerX; }
    public float getCenterY() { return centerY; }
    public float getRadiusX() { return radiusX; }
    public float getRadiusY() { return radiusY; }
    public boolean isCircle() { return isCircle; }
    public float getStartOffset() { return startOffset; }

    // ─── Builder fluente ──────────────────────────────────────────────

    public static class Builder {
        private GradientType type = GradientType.LINEAR;
        private List<GradientStop> stops = new ArrayList<>();
        private float angle = 0;
        private String direction = "to bottom";
        private float centerX = 50, centerY = 50;
        private float radiusX = 50, radiusY = 50;
        private boolean isCircle = false;
        private float startOffset = 0;

        public Builder type(GradientType type) { this.type = type; return this; }
        public Builder stops(List<GradientStop> stops) { this.stops = stops; return this; }
        public Builder stop(GradientStop stop) { this.stops.add(stop); return this; }
        public Builder angle(float angle) { this.angle = angle; return this; }
        public Builder direction(String direction) { this.direction = direction; return this; }
        public Builder center(float x, float y) { this.centerX = x; this.centerY = y; return this; }
        public Builder radius(float rx, float ry) { this.radiusX = rx; this.radiusY = ry; return this; }
        public Builder circle() { this.isCircle = true; this.radiusX = this.radiusY = 50; return this; }
        public Builder ellipse() { this.isCircle = false; return this; }
        public Builder startOffset(float offset) { this.startOffset = offset; return this; }

        public Gradient build() {
            if (stops.isEmpty()) {
                stops.add(new GradientStop(0xFFFFFFFF, CssValue.percent(0)));
                stops.add(new GradientStop(0xFF000000, CssValue.percent(100)));
            }
            return new Gradient(type, stops, angle, direction, centerX, centerY,
                    radiusX, radiusY, isCircle, startOffset, 0.5f);
        }
    }

    public static Builder builder() { return new Builder(); }

    // ─── Fábricas rápidas ──────────────────────────────────────────────

    public static Gradient linear(float angle, GradientStop... stops) {
        Builder b = builder().type(GradientType.LINEAR).angle(angle);
        for (GradientStop s : stops) b.stop(s);
        return b.build();
    }

    public static Gradient radial(float cx, float cy, float radius, GradientStop... stops) {
        Builder b = builder().type(GradientType.RADIAL).center(cx, cy).radius(radius, radius).circle();
        for (GradientStop s : stops) b.stop(s);
        return b.build();
    }

    public Gradient softness(float softness) {
        this.softness = Math.max(0, Math.min(1, softness));
        return this;
    }

    public Gradient softnessPercent(float percent) {
        return softness(percent / 100f);
    }

    // ─── Método para criar Shader Skia ────────────────────────────────
    public Shader createShader(CssContext context, float x, float y, float w, float h) {
        List<GradientStop> stops = this.stops;
        if (stops == null || stops.size() < 2) return null;

        int[] colors = new int[stops.size()];
        float[] positions = new float[stops.size()];

        float length = 1; // fallback

        // Determina o comprimento de referência para cada tipo
        switch (type) {
            case LINEAR:
            case REPEATING_LINEAR: {
                float[] pts = calculateLinearPoints(x, y, w, h);
                float dx = pts[2] - pts[0];
                float dy = pts[3] - pts[1];
                length = (float) Math.sqrt(dx*dx + dy*dy);
                break;
            }
            case RADIAL:
            case REPEATING_RADIAL: {
                length = isCircle ? Math.min(w, h) * (radiusX / 100f) :
                        Math.max(w, h) * (Math.max(radiusX, radiusY) / 100f);
                break;
            }
            case CONIC:
            case CUSTOM:
            default:
                length = Math.max(w, h);
                break;
        }

        if (length <= 0) length = 1;

        for (int i = 0; i < stops.size(); i++) {
            GradientStop s = stops.get(i);
            colors[i] = s.getColorWithOpacity();
            // Usa o comprimento real do gradiente para converter posição
            float pos = s.getPositionPixels(context, length);
            float normalized = Math.max(0, Math.min(1f, pos / length));
            positions[i] = normalized;
        }

        // Cria o shader sem recursão
        /*switch (type) {
            case LINEAR:
            case REPEATING_LINEAR: {
                float[] pts = calculateLinearPoints(x, y, w, h);
                return Shader.makeLinearGradient(pts[0], pts[1], pts[2], pts[3], colors, positions);
            }
            case RADIAL:
            case REPEATING_RADIAL: {
                float cx = x + (centerX / 100f) * w;
                float cy = y + (centerY / 100f) * h;
                float rx = (radiusX / 100f) * (isCircle ? Math.min(w, h) : w);
                float ry = (radiusY / 100f) * (isCircle ? Math.min(w, h) : h);
                // Se for circle, usa o menor dos dois
                float radius = isCircle ? Math.min(rx, ry) : Math.max(rx, ry);
                return Shader.makeRadialGradient(cx, cy, radius, colors, positions);
            }
            case CONIC: {
                // Fallback para radial
                float cx = x + (centerX / 100f) * w;
                float cy = y + (centerY / 100f) * h;
                float radius = Math.max(w, h) / 2;
                return Shader.makeRadialGradient(cx, cy, radius, colors, positions);
            }
            case CUSTOM: {
                // Para custom, usa uma interpolação radial básica
                float cx = x + (centerX / 100f) * w;
                float cy = y + (centerY / 100f) * h;
                float radius = Math.max(w, h) / 2;
                return Shader.makeRadialGradient(cx, cy, radius, colors, positions);
            }
            default:
                return null;
        }*/

        return null;
    }
    // Método auxiliar para calcular pontos do gradiente linear
    private float[] calculateLinearPoints(float x, float y, float w, float h) {
        float startX, startY, endX, endY;
        if (direction != null) {
            return directionToPoints(direction, x, y, w, h);
        } else {
            float rad = (float) Math.toRadians(angle);
            float dx = (float) Math.cos(rad) * w;
            float dy = (float) Math.sin(rad) * h;
            startX = x + (w - dx) / 2;
            startY = y + (h - dy) / 2;
            endX = x + (w + dx) / 2;
            endY = y + (h + dy) / 2;
        }
        return new float[]{startX, startY, endX, endY};
    }

    private float[] directionToPoints(String dir, float x, float y, float w, float h) {
        float startX = x, startY = y + h;
        float endX = x + w, endY = y;
        // Lógica básica: mapeia "to right", "to left", "to top", "to bottom"
        switch (dir) {
            case "to right":   startX = x; startY = y + h/2; endX = x + w; endY = y + h/2; break;
            case "to left":    startX = x + w; startY = y + h/2; endX = x; endY = y + h/2; break;
            case "to top":     startX = x + w/2; startY = y + h; endX = x + w/2; endY = y; break;
            case "to bottom":  startX = x + w/2; startY = y; endX = x + w/2; endY = y + h; break;
            case "to top right": startX = x; startY = y + h; endX = x + w; endY = y; break;
            case "to top left":  startX = x + w; startY = y + h; endX = x; endY = y; break;
            case "to bottom right": startX = x; startY = y; endX = x + w; endY = y + h; break;
            case "to bottom left":  startX = x + w; startY = y; endX = x; endY = y + h; break;
            default: // fallback para ângulo
                float rad = (float) Math.toRadians(angle);
                float dx = (float) Math.cos(rad) * w;
                float dy = (float) Math.sin(rad) * h;
                startX = x + (w - dx) / 2;
                startY = y + (h - dy) / 2;
                endX = x + (w + dx) / 2;
                endY = y + (h + dy) / 2;
                break;
        }
        return new float[]{startX, startY, endX, endY};
    }
}