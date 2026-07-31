package com.dic.xsuper.render.javafx.tags.controls;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.control.Slider;

import java.util.Map;

/**
 * Tag HTML &lt;input type="range"&gt; convertida para JavaFX Slider.
 */
public class RangeInputTag extends FormControlTag {

    private Slider fxSlider;

    public RangeInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        // 1. Cria o Slider
        fxSlider = new Slider();

        // 2. Atributos HTML específicos do range
        double min = parseDouble(sourceNode.attributes.get("min"), 0);
        double max = parseDouble(sourceNode.attributes.get("max"), 100);
        double val = parseDouble(sourceNode.attributes.get("value"), 50);
        double step = parseDouble(sourceNode.attributes.get("step"), 1);

        fxSlider.setMin(min);
        fxSlider.setMax(max);
        fxSlider.setValue(val);
        fxSlider.setBlockIncrement(step);
        fxSlider.setMajorTickUnit(step * 10);
        fxSlider.setMinorTickCount(0);

        // 3. Aplica os atributos comuns (disabled, required, etc.)
        applyCommonAttributes(fxSlider);

        // 4. Aplica estilos específicos (ex: thumb-color)
        applyRangeStyles();


        return fxSlider;
    }

    @Override
    protected void applyTagSpecificStyles() {

    }

    /**
     * Aplica estilos CSS avançados para o slider (thumb, track, etc.)
     */
    private void applyRangeStyles() {
        if (fxSlider == null) return;

        Map<String, String> styles = getRawStyles();
        StringBuilder css = new StringBuilder(fxSlider.getStyle() != null ? fxSlider.getStyle() : "");

        // Cor do thumb (bolinha)
        if (styles.containsKey("thumb-color")) {
            String color = styles.get("thumb-color");
            css.append("-fx-thumb-color: ").append(color).append("; ");
        }

        // Cor do track (barra)
        if (styles.containsKey("track-color")) {
            String color = styles.get("track-color");
            css.append("-fx-track-color: ").append(color).append("; ");
        }

        // Cor da parte preenchida (progresso)
        if (styles.containsKey("filled-track-color")) {
            String color = styles.get("filled-track-color");
            css.append("-fx-filled-track-color: ").append(color).append("; ");
        }

        fxSlider.setStyle(css.toString());
    }

    private double parseDouble(Object val, double def) {
        if (val == null) return def;
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ⚠️ O método dispatchEvent já existe em NativeTag (ou podes usar o da bridge)
    private void dispatchEvent(String eventName, String targetId, Object payload) {
        // Este método pode ser implementado na classe base NativeTag
        // ou podes chamar o callback da bridge diretamente.
        // Por exemplo:
        // if (eventCallback != null) eventCallback.onEvent(eventName, targetId, payload);
    }

    @Override
    protected void addChildren() {
        // <input> é void element – não tem filhos
    }

    @Override
    protected void bindEvents() {
        super.bindEvents();
        if (fxSlider != null) {
            // Liga o arrasto do slider ao atributo 'value'
            bindTwoWayProperty(fxSlider.valueProperty(), "value");
        }
    }
}