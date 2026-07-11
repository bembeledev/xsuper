package com.dic.xsuper.lang.ui.tags.controls;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.Node;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

public class NumberInputTag extends TextInputTag {

    public NumberInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        double min = parseDouble(sourceNode.attributes.get("min"), Double.MIN_VALUE);
        double max = parseDouble(sourceNode.attributes.get("max"), Double.MAX_VALUE);
        double step = parseDouble(sourceNode.attributes.get("step"), 1);
        double val = parseDouble(sourceNode.attributes.get("value"), 0);

        Spinner<Double> spinner = new Spinner<>();
        SpinnerValueFactory<Double> factory = new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, val, step);
        spinner.setValueFactory(factory);
        spinner.setEditable(true);

        fxControl = spinner;
        applyCommonAttributes(fxControl);
        applyAttributeStyles(fxControl);


        return fxControl;
    }

    private double parseDouble(Object val, double def) {
        if (val == null) return def;
        try { return Double.parseDouble(val.toString()); } catch (NumberFormatException e) { return def; }
    }
}