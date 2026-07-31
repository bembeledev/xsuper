package com.dic.xsuper.render.javafx.tags.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;

public class SpawnerTag extends GameTagBase {

    private double x = 0, y = 0;
    private double interval = 2.0;
    private int maxEntities = 5;
    private EntityType entityType = EntityType.ENEMY;

    public SpawnerTag(XplNode sourceNode) {
        super(sourceNode);
        x = parseDouble(sourceNode.attributes.get("x"), 0);
        y = parseDouble(sourceNode.attributes.get("y"), 0);
        interval = parseDouble(sourceNode.attributes.get("interval"), 2.0);
        maxEntities = parseInt(sourceNode.attributes.get("max-entities"), 5);
        if (sourceNode.attributes.containsKey("entity-type")) {
            entityType = parseEntityType(sourceNode.attributes.get("entity-type"));
        }
    }

    @Override
    protected void onGameReady() {
        Entity spawner = FXGL.entityBuilder()
                .at(x, y)
                .type(EntityType.SPAWNER)
                .with(new SpawnerComponent(interval, maxEntities,entityType))
                .buildAndAttach();
        System.out.println("[SpawnerTag] Spawner criado em (" + x + ", " + y + ")");
    }

    @Override
    protected Node createNode() {
        return createPlaceholder();
    }

    private static class SpawnerComponent extends Component {
        private final double interval;
        private final int maxEntities;
        private final EntityType entityType;
        private double timer = 0;

        public SpawnerComponent(double interval, int maxEntities, EntityType entityType) {
            this.interval = interval;
            this.maxEntities = maxEntities;
            this.entityType = entityType;
        }

        @Override
        public void onUpdate(double tpf) {
            // Verifica quantas entidades do tipo existem
            long count = FXGL.getGameWorld().getEntitiesByType(entityType).size();
            if (count >= maxEntities) return;

            timer += tpf;
            if (timer >= interval) {
                timer = 0;
                // Spawn com ligeira aleatoriedade na posição
                double spawnX = entity.getX() + (Math.random() - 0.5) * 100;
                double spawnY = entity.getY() + (Math.random() - 0.5) * 100;
                FXGL.spawn(entityType.name(), spawnX, spawnY);
            }
        }
    }
}