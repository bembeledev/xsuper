package com.dic.xsuper.lang.ui.layout.panes;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.List;

public class InlineBlockContainerPane extends VBox implements CustomLayoutPane {

    public InlineBlockContainerPane() {
        // ⭐ A MAGIA: Diferente de um Bloco normal que tenta ocupar 100% da largura,
        // o Inline-Block "encolhe-se" (shrink-wrap) para abraçar apenas o seu conteúdo!
        setMaxWidth(Region.USE_PREF_SIZE);
        setMaxHeight(Region.USE_PREF_SIZE);
    }

    @Override
    public void populateChildren(List<NativeTag> children, CssContext context) {
        for (NativeTag child : children) {
            Node fxChild = child.build();
            applyMargins(fxChild, child, context);
            getChildren().add(fxChild);
        }
    }

    private void applyMargins(Node targetNode, NativeTag child, CssContext context) {
        if (!child.getResolvedStyles().margin.isZero()) {
            Insets m = new Insets(
                    child.getResolvedStyles().margin.getTopPixels(context),
                    child.getResolvedStyles().margin.getRightPixels(context),
                    child.getResolvedStyles().margin.getBottomPixels(context),
                    child.getResolvedStyles().margin.getLeftPixels(context)
            );
            VBox.setMargin(targetNode, m);
        }
    }
}