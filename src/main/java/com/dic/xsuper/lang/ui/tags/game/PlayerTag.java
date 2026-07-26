package com.dic.xsuper.lang.ui.tags.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

public class PlayerTag extends GameTagBase {

    private double x = 0, y = 0;
    private double speed = 200;
    private double jumpForce = 400;
    private String sprite = "player.png";
    private String controls = "WASD";

    public PlayerTag(XplNode sourceNode) {
        super(sourceNode);
        x = parseDouble(sourceNode.attributes.get("x"), 0);
        y = parseDouble(sourceNode.attributes.get("y"), 0);
        speed = parseDouble(sourceNode.attributes.get("speed"), 200);
        jumpForce = parseDouble(sourceNode.attributes.get("jump-force"), 400);
        if (sourceNode.attributes.containsKey("sprite")) {
            sprite = sourceNode.attributes.get("sprite").toString();
        }
        if (sourceNode.attributes.containsKey("controls")) {
            controls = sourceNode.attributes.get("controls").toString().toUpperCase();
        }
    }

    @Override
    protected void onGameReady() {
        HitBox hitBox = new HitBox(BoundingShape.box(32, 32));

        Entity player = FXGL.entityBuilder()
                .at(x, y)
                .view(createFallbackTexture(sprite, Color.BLUE, 32, 32))
                .bbox(hitBox)
                .type(EntityType.PLAYER)
                .with(new PlayerControl(speed, jumpForce, controls, this.hashCode()))
                .buildAndAttach();

        FXGL.getGameScene().getViewport().bindToEntity(player, 400, 300);
        System.out.println("[PlayerTag] Jogador criado em (" + x + ", " + y + ")");
    }

    @Override
    protected Node createNode() {
        return createPlaceholder();
    }

    private static class PlayerControl extends Component {
        private final double speed;
        private final double jumpForce;
        private final String controls;
        private final int instanceId;
        private boolean moveLeft = false;
        private boolean moveRight = false;
        private boolean moveUp = false;
        private boolean jump = false;
        private boolean inputRegistered = false;

        public PlayerControl(double speed, double jumpForce, String controls, int instanceId) {
            this.speed = speed;
            this.jumpForce = jumpForce;
            this.controls = controls;
            this.instanceId = instanceId;
        }

        @Override
        public void onAdded() {
            if (!inputRegistered) {
                setupInput();
                inputRegistered = true;
            }
        }

        private void setupInput() {
            Input input = FXGL.getInput();
            KeyCode leftKey, rightKey, upKey, jumpKey;
            if (controls.contains("WASD")) {
                leftKey = KeyCode.A;
                rightKey = KeyCode.D;
                upKey = KeyCode.W;
                jumpKey = KeyCode.SPACE;
            } else {
                leftKey = KeyCode.LEFT;
                rightKey = KeyCode.RIGHT;
                upKey = KeyCode.UP;
                jumpKey = KeyCode.SPACE;
            }

            // Nomes únicos baseados no ID da instância
            String leftName = "Move Left_" + instanceId;
            String rightName = "Move Right_" + instanceId;
            String upName = "Move Up_" + instanceId;
            String jumpName = "Jump_" + instanceId;

            // ⭐ CORREÇÃO DE NÍVEL INDUSTRIAL:
            // Como a FXGL lança exceção se a ação não existir, usamos o try-catch para testar a existência
            boolean alreadyRegistered;
            try {
                input.getActionByName(leftName);
                alreadyRegistered = true;
            } catch (IllegalArgumentException e) {
                alreadyRegistered = false;
            }

            if (alreadyRegistered) {
                System.out.println("[PlayerTag] Ações já registadas para a instância " + instanceId);
                return;
            }

            // Registo seguro das ações
            try {
                input.addAction(new UserAction(leftName) {
                    @Override protected void onActionBegin() { moveLeft = true; }
                    @Override protected void onActionEnd() { moveLeft = false; }
                }, leftKey);
            } catch (IllegalArgumentException e) {
                System.err.println("[PlayerTag] Aviso: A tecla para a ação " + leftName + " já está ocupada.");
            }

            try {
                input.addAction(new UserAction(rightName) {
                    @Override protected void onActionBegin() { moveRight = true; }
                    @Override protected void onActionEnd() { moveRight = false; }
                }, rightKey);
            } catch (IllegalArgumentException e) {
                System.err.println("[PlayerTag] Aviso: A tecla para a ação " + rightName + " já está ocupada.");
            }

            try {
                input.addAction(new UserAction(upName) {
                    @Override protected void onActionBegin() { moveUp = true; }
                    @Override protected void onActionEnd() { moveUp = false; }
                }, upKey);
            } catch (IllegalArgumentException e) {
                System.err.println("[PlayerTag] Aviso: A tecla para a ação " + upName + " já está ocupada.");
            }

            try {
                input.addAction(new UserAction(jumpName) {
                    @Override protected void onActionBegin() { jump = true; }
                    @Override protected void onActionEnd() { jump = false; }
                }, jumpKey);
            } catch (IllegalArgumentException e) {
                System.err.println("[PlayerTag] Aviso: A tecla para a ação " + jumpName + " já está ocupada.");
            }

            System.out.println("[PlayerTag] Ações registadas com sucesso para a instância " + instanceId);
        }

        @Override
        public void onUpdate(double tpf) {
            double vx = 0, vy = 0;
            if (moveLeft) vx = -speed;
            else if (moveRight) vx = speed;
            if (moveUp) vy = -speed;
            entity.translateX(vx * tpf);
            entity.translateY(vy * tpf);
            if (jump) {
                entity.translateY(-jumpForce * tpf);
            }
        }
    }
}