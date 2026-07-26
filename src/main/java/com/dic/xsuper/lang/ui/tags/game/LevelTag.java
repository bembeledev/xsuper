package com.dic.xsuper.lang.ui.tags.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.level.tiled.TMXLevelLoader;
import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import java.net.URL;

public class LevelTag extends GameTagBase {

    private String src;
    private String background = "#000000";

    public LevelTag(XplNode sourceNode) {
        super(sourceNode);
        if (sourceNode.attributes.containsKey("src")) {
            src = sourceNode.attributes.get("src").toString();
        }
        if (sourceNode.attributes.containsKey("background")) {
            background = sourceNode.attributes.get("background").toString();
        }
    }

    @Override
    protected void onGameReady() {
        if (src != null && !src.isEmpty()) {
            try {
                // ⭐ Converte a String para URL usando getClass().getResource()
                URL url = getClass().getResource(src);
                if (url == null) {
                    // Fallback: tentar como ficheiro do sistema
                    url = new java.io.File(src).toURI().toURL();
                }

                // ⭐ Carrega o mapa
                var level = new TMXLevelLoader().load(url, FXGL.getGameWorld());
                FXGL.getGameWorld().setLevel(level);
                FXGL.getGameScene().setBackgroundColor(Color.web(background));
                System.out.println("[LevelTag] Mapa carregado: " + src);
            } catch (Exception e) {
                System.err.println("[LevelTag] Erro ao carregar mapa: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    protected Node createNode() {
        return createPlaceholder();
    }
}