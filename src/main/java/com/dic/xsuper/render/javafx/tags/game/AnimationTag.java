package com.dic.xsuper.render.javafx.tags.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class AnimationTag extends GameTagBase {

    private String id;
    private String spriteSheet;
    private int frameWidth = 0;
    private int frameHeight = 0;
    private int columns = 1;
    private int rows = 1;
    private int startFrame = 0;
    private int endFrame = -1;
    private double durationSeconds = 1.0;
    private boolean loop = true;
    private boolean autoPlay = true;
    private String onCompleteScript;

    private List<String> imagePaths = new ArrayList<>();
    private AnimatedTexture animatedTexture;

    public AnimationTag(XplNode sourceNode) {
        super(sourceNode);
        parseAttributes();
    }

    private void parseAttributes() {
        if (sourceNode.attributes.containsKey("id")) {
            id = sourceNode.attributes.get("id").toString();
        }
        if (sourceNode.attributes.containsKey("sprite")) {
            spriteSheet = sourceNode.attributes.get("sprite").toString();
        }
        if (sourceNode.attributes.containsKey("frames")) {
            String raw = sourceNode.attributes.get("frames").toString();
            for (String f : raw.split(",")) {
                imagePaths.add(f.trim());
            }
        }
        if (sourceNode.attributes.containsKey("frame-width")) {
            frameWidth = parseInt(sourceNode.attributes.get("frame-width"), 0);
        }
        if (sourceNode.attributes.containsKey("frame-height")) {
            frameHeight = parseInt(sourceNode.attributes.get("frame-height"), 0);
        }
        if (sourceNode.attributes.containsKey("columns")) {
            columns = parseInt(sourceNode.attributes.get("columns"), 1);
        }
        if (sourceNode.attributes.containsKey("rows")) {
            rows = parseInt(sourceNode.attributes.get("rows"), 1);
        }
        if (sourceNode.attributes.containsKey("start-frame")) {
            startFrame = parseInt(sourceNode.attributes.get("start-frame"), 0);
        }
        if (sourceNode.attributes.containsKey("end-frame")) {
            endFrame = parseInt(sourceNode.attributes.get("end-frame"), -1);
        }
        if (sourceNode.attributes.containsKey("duration")) {
            durationSeconds = parseDuration(sourceNode.attributes.get("duration").toString());
        }
        if (sourceNode.attributes.containsKey("loop")) {
            loop = parseBoolean(sourceNode.attributes.get("loop"), true);
        }
        if (sourceNode.attributes.containsKey("auto-play")) {
            autoPlay = parseBoolean(sourceNode.attributes.get("auto-play"), true);
        }
        if (sourceNode.attributes.containsKey("on-complete")) {
            onCompleteScript = sourceNode.attributes.get("on-complete").toString();
        }
    }

    @Override
    protected void onGameReady() {
        buildAnimation();
    }

    private void buildAnimation() {
        Duration duration = Duration.seconds(durationSeconds);

        // CASO 1: Lista de imagens
        if (!imagePaths.isEmpty()) {
            List<Image> images = new ArrayList<>();
            for (String path : imagePaths) {
                try {
                    images.add(FXGL.image(path));
                } catch (Exception e) {
                    System.err.println("[AnimationTag] Erro ao carregar imagem: " + path);
                }
            }
            if (images.isEmpty()) {
                System.err.println("[AnimationTag] Nenhuma imagem carregada.");
                return;
            }
            AnimationChannel channel = new AnimationChannel(images, duration);
            animatedTexture = new AnimatedTexture(channel);
            configureTexture();
            return;
        }

        // CASO 2: Spritesheet com grid
        if (spriteSheet != null && frameWidth > 0 && frameHeight > 0) {
            Image sheet = FXGL.image(spriteSheet);
            int totalFrames = columns * rows;
            if (endFrame == -1 || endFrame > totalFrames) {
                endFrame = totalFrames;
            }
            // Certifica que startFrame < endFrame
            if (startFrame >= endFrame) {
                startFrame = 0;
                endFrame = totalFrames;
            }
            AnimationChannel channel = new AnimationChannel(
                    sheet,
                    columns,      // framesPerRow
                    frameWidth,
                    frameHeight,
                    duration,
                    startFrame,
                    endFrame
            );
            animatedTexture = new AnimatedTexture(channel);
            configureTexture();
            return;
        }

        // CASO 3: Imagem estática (única)
        if (spriteSheet != null) {
            try {
                Image img = FXGL.image(spriteSheet);
                // Cria um canal com uma única imagem
                AnimationChannel channel = new AnimationChannel(List.of(img), Duration.seconds(1.0));
                animatedTexture = new AnimatedTexture(channel);
                // Para garantir que não anima, paramos
                animatedTexture.stop();
                configureTexture();
            } catch (Exception e) {
                System.err.println("[AnimationTag] Erro ao carregar imagem estática: " + spriteSheet);
            }
        } else {
            System.err.println("[AnimationTag] Nenhum recurso de animação especificado.");
        }
    }

    private void configureTexture() {
        if (animatedTexture == null) return;

        // Controlo de loop
        if (loop) {
            animatedTexture.loop();  // Ativa loop infinito
        } else {
            // Apenas toca uma vez (play() já faz isso)
            if (autoPlay) {
                animatedTexture.play();
            }
        }

        // Se não for loop e tiver script de conclusão
        if (!loop && onCompleteScript != null) {
            animatedTexture.setOnCycleFinished(() -> executeScript(onCompleteScript));
        }

        // Regista a animação globalmente (para ser referenciada por outras tags)
        if (id != null && !id.isEmpty()) {
            FXGL.getWorldProperties().setValue("anim_" + id, this);
            System.out.println("[AnimationTag] Animação '" + id + "' registada.");
        }
    }

    public AnimatedTexture getAnimatedTexture() {
        return animatedTexture;
    }

    private void executeScript(String script) {
        // Chamar motor XPL
        System.out.println("[AnimationTag] Executando script: " + script);
        // SuperUiEngine.getInstance().executeInlineScript(script, null);
    }

    private double parseDuration(String value) {
        if (value.endsWith("ms")) {
            return Double.parseDouble(value.replace("ms", "")) / 1000.0;
        } else if (value.endsWith("s")) {
            return Double.parseDouble(value.replace("s", ""));
        }
        return Double.parseDouble(value);
    }

    @Override
    protected Node createNode() {
        return createPlaceholder();
    }

    @Override
    protected void addChildren() {
        // Nenhum filho
    }
}