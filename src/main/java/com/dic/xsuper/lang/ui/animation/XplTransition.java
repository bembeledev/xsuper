package com.dic.xsuper.lang.ui.animation;

import javafx.util.Duration;

import java.util.*;

/**
 * Representa uma transição CSS, com lista de propriedades,
 * duração, delay e easing.
 */
public class XplTransition {
    private final Set<String> properties = new HashSet<>();
    private Duration duration = Duration.millis(300);
    private Duration delay = Duration.ZERO;
    private XplEasing easing = XplEasing.EASE;

    // Estado interno para o motor
    private boolean running = false;

    // Construtor para criar a partir de uma string CSS
    public XplTransition(String cssValue) {
        parse(cssValue);
    }

    public XplTransition(Set<String> props, Duration duration, Duration delay, XplEasing easing) {
        this.properties.addAll(props);
        this.duration = duration;
        this.delay = delay;
        this.easing = easing;
    }

    /**
     * Parse de uma string CSS de transição.
     * Ex: "all 0.3s ease, transform 0.5s ease-in-out"
     */
    private void parse(String cssValue) {
        if (cssValue == null || cssValue.trim().isEmpty()) return;
        // Dividir por vírgula para múltiplas declarações
        String[] parts = cssValue.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            // Cada parte tem propriedade, duração, delay, easing (opcionais)
            String[] tokens = part.split("\\s+");
            // tokens[0] = propriedade (ex: "all", "opacity", "transform")
            String prop = tokens[0];
            properties.add(prop);
            // tokens[1] = duração (ex: "0.3s", "300ms")
            Duration dur = Duration.millis(300);
            Duration del = Duration.ZERO;
            XplEasing eas = XplEasing.EASE;
            for (int i = 1; i < tokens.length; i++) {
                String token = tokens[i];
                if (token.endsWith("s") || token.endsWith("ms")) {
                    // é duração ou delay?
                    // Se já tiver duração, será delay
                    double time = parseTime(token);
                    if (dur == Duration.millis(300) && !token.contains("delay")) {
                        dur = Duration.millis(time);
                    } else {
                        del = Duration.millis(time);
                    }
                } else {
                    // é um easing
                    eas = XplEasing.fromString(token);
                }
            }
            this.duration = dur;
            this.delay = del;
            this.easing = eas;
            // Como estamos a processar a primeira parte, assumimos que todas têm a mesma duração/delay/easing.
            // Se forem diferentes, o parse deve ser mais complexo. Para simplificar, usamos a primeira.
            // Poderíamos guardar uma lista de transições separadas.
            // Nesta implementação, vamos usar a primeira definida.
            break;
        }
    }

    // Getters
    public Set<String> getProperties() { return properties; }
    public Duration getDuration() { return duration; }
    public Duration getDelay() { return delay; }
    public XplEasing getEasing() { return easing; }
    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }

    // Método auxiliar
    private double parseTime(String token) {
        token = token.replace("s", "").replace("ms", "");
        double val = Double.parseDouble(token);
        if (token.endsWith("ms") || token.contains("ms")) {
            return val; // já em ms
        } else {
            return val * 1000; // converte segundos para ms
        }
    }
}