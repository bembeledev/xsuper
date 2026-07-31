package com.dic.xsuper.render.javafx.animation;

import javafx.animation.Interpolator;

/**
 * Funções de easing (curvas de aceleração) para animações.
 */
public enum XplEasing {
    LINEAR(Interpolator.LINEAR),
    EASE(Interpolator.EASE_BOTH),
    EASE_IN(Interpolator.EASE_IN),
    EASE_OUT(Interpolator.EASE_OUT),
    EASE_IN_OUT(Interpolator.EASE_BOTH),
    // Extras comuns
    STEP_START(Interpolator.DISCRETE),
    STEP_END(Interpolator.DISCRETE);

    private final Interpolator interpolator;

    XplEasing(Interpolator interpolator) {
        this.interpolator = interpolator;
    }

    public Interpolator getInterpolator() {
        return interpolator;
    }

    /**
     * Converte uma string (ex: "ease-in-out") para o enum correspondente.
     * Caso não encontre, retorna EASE.
     */
    public static XplEasing fromString(String name) {
        if (name == null) return EASE;
        String normalized = name.trim().toLowerCase()
                .replace("-", "_")
                .replace(" ", "_");
        try {
            return valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException e) {
            return EASE;
        }
    }
}