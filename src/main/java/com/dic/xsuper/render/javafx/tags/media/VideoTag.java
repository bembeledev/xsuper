package com.dic.xsuper.render.javafx.tags.media;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.io.File;
import java.util.Map;

/**
 * Tag HTML &lt;video&gt; convertida para JavaFX MediaView + MediaPlayer.
 * A UI (controlos, área de visualização) é sempre exibida, mesmo que o ficheiro não carregue.
 * <p>
 * Atributos suportados:
 * - src: URL do vídeo (ou caminho local)
 * - controls: mostra controlos (true/false)
 * - autoplay: inicia automaticamente
 * - loop: repete em ciclo
 * - muted: inicia silenciado
 * - poster: imagem de fundo antes da reprodução
 * - width / height: dimensões do vídeo
 * - preload: "auto", "metadata", "none" (parcialmente suportado)
 * <p>
 * Suporta múltiplas tags &lt;source&gt; como filhos (fallback).
 */
public class VideoTag extends NativeTag {

    // ─── Componentes da UI ──────────────────────────────────────────────────
    private StackPane rootContainer;
    private StackPane videoArea;          // onde o MediaView e o poster são colocados
    private MediaView mediaView;
    private MediaPlayer mediaPlayer;
    private ImageView posterView;

    // ─── Controlos ──────────────────────────────────────────────────────────
    private VBox controlsContainer;
    private Slider progressSlider;
    private Label timeLabel;
    private Button playBtn;
    private Button pauseBtn;
    private Button stopBtn;
    private Button volumeBtn;
    private Slider volumeSlider;
    private Label errorLabel;

    // ─── Estado ─────────────────────────────────────────────────────────────
    private boolean controls = true;
    private boolean autoplay = false;
    private boolean loop = false;
    private boolean muted = false;
    private double volume = 1.0;
    private String posterUrl = null;
    private double videoWidth = -1;
    private double videoHeight = -1;
    private String preload = "metadata";
    private boolean mediaLoaded = false;
    private boolean mediaError = false;

    public VideoTag(XplNode sourceNode) {
        super(sourceNode);
        parseAttributes();
    }

    // ─── Parsing de atributos ──────────────────────────────────────────────

    private void parseAttributes() {
        Map<String, Object> attrs = sourceNode.attributes;

        // controls
        if (attrs.containsKey("controls")) {
            controls = !"false".equalsIgnoreCase(attrs.get("controls").toString());
        }

        // autoplay
        if (attrs.containsKey("autoplay")) {
            autoplay = "true".equalsIgnoreCase(attrs.get("autoplay").toString()) ||
                    attrs.get("autoplay").equals("");
        }

        // loop
        if (attrs.containsKey("loop")) {
            loop = "true".equalsIgnoreCase(attrs.get("loop").toString()) ||
                    attrs.get("loop").equals("");
        }

        // muted
        if (attrs.containsKey("muted")) {
            muted = "true".equalsIgnoreCase(attrs.get("muted").toString()) ||
                    attrs.get("muted").equals("");
        }

        // volume
        if (attrs.containsKey("volume")) {
            try {
                volume = Double.parseDouble(attrs.get("volume").toString());
                volume = Math.max(0, Math.min(1, volume));
            } catch (NumberFormatException ignored) {}
        }

        // poster
        if (attrs.containsKey("poster")) {
            posterUrl = attrs.get("poster").toString();
        }

        // width / height
        if (attrs.containsKey("width")) {
            try { videoWidth = Double.parseDouble(attrs.get("width").toString()); } catch (Exception ignored) {}
        }
        if (attrs.containsKey("height")) {
            try { videoHeight = Double.parseDouble(attrs.get("height").toString()); } catch (Exception ignored) {}
        }

        // preload
        if (attrs.containsKey("preload")) {
            preload = attrs.get("preload").toString().toLowerCase();
        }
    }

    // ─── Construção do nó ──────────────────────────────────────────────────

    @Override
    protected Node createNode() {
        // 1. Container principal (StackPane para sobrepor poster/video/controlos)
        rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: #1a1a2e; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-color: #333; -fx-border-width: 1px;");
        rootContainer.setPrefHeight(360);
        rootContainer.setMinHeight(200);

        // 2. Área de vídeo (onde fica o MediaView + poster)
        videoArea = new StackPane();
        videoArea.setStyle("-fx-background-color: #0d0d1a;");
        videoArea.setAlignment(Pos.CENTER);

        // 3. Poster (imagem de fundo)
        createPoster();

        // 4. MediaView (será adicionado quando o vídeo carregar)
        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);
        mediaView.setSmooth(true);
        if (videoWidth > 0) mediaView.setFitWidth(videoWidth);
        if (videoHeight > 0) mediaView.setFitHeight(videoHeight);

        // 5. Tentar carregar o vídeo
        loadVideo();

        // 6. Controlos (sempre visíveis se controls=true)
        if (controls) {
            createControls();
            rootContainer.getChildren().add(controlsContainer);
            StackPane.setAlignment(controlsContainer, Pos.BOTTOM_CENTER);
        }

        // 7. Aplicar estilos
        applyCommonStyles();
        applyTagSpecificStyles();

        // 8. Autoplay (se definido e o vídeo já estiver carregado)
        if (autoplay && mediaPlayer != null) {
            Platform.runLater(() -> mediaPlayer.play());
        }

        return rootContainer;
    }

    // ─── Poster ────────────────────────────────────────────────────────────

    private void createPoster() {
        if (posterUrl == null || posterUrl.isEmpty()) {
            // Poster vazio: mostra um placeholder com ícone de vídeo
            Label placeholder = new Label("🎬");
            placeholder.setStyle("-fx-font-size: 48px; -fx-text-fill: rgba(255,255,255,0.3);");
            videoArea.getChildren().add(placeholder);
            return;
        }

        try {
            Image posterImage = new Image(posterUrl, true);
            posterView = new ImageView(posterImage);
            posterView.setPreserveRatio(true);
            if (videoWidth > 0) posterView.setFitWidth(videoWidth);
            if (videoHeight > 0) posterView.setFitHeight(videoHeight);
            videoArea.getChildren().add(posterView);
        } catch (Exception e) {
            // Fallback para texto
            Label fallback = new Label("🎬 " + posterUrl);
            fallback.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 14px;");
            videoArea.getChildren().add(fallback);
        }
    }

    // ─── Carregamento do vídeo ─────────────────────────────────────────────

    private void loadVideo() {
        String src = getBestSource();
        if (src == null || src.isEmpty()) {
            showError("Nenhuma fonte de vídeo especificada.");
            return;
        }

        try {
            String mediaUrl = resolveUrl(src);
            Media media = new Media(mediaUrl);
            mediaPlayer = new MediaPlayer(media);

            // Configurações
            mediaPlayer.setVolume(volume);
            mediaPlayer.setMute(muted);
            mediaPlayer.setCycleCount(loop ? MediaPlayer.INDEFINITE : 1);

            // Adiciona o MediaView à área de vídeo
            if (posterView != null) {
                videoArea.getChildren().remove(posterView);
            }
            videoArea.getChildren().add(mediaView);
            mediaView.setMediaPlayer(mediaPlayer);

            // Liga eventos
            bindMediaEvents();

            // Atualiza UI
            mediaLoaded = true;
            mediaError = false;
            updateControlsState();

        } catch (Exception e) {
            showError("Erro ao carregar vídeo: " + e.getMessage());
            mediaError = true;
            mediaLoaded = false;
        }
    }

    private String getBestSource() {
        // Primeiro, verifica se há um atributo src direto
        String src = (String) sourceNode.attributes.get("src");
        if (src != null && !src.isEmpty()) return src;

        // Depois, procura por tags <source> filhas
        for (XplNode child : sourceNode.children) {
            if ("source".equalsIgnoreCase(child.tag)) {
                String childSrc = (String) child.attributes.get("src");
                if (childSrc != null && !childSrc.isEmpty()) {
                    return childSrc;
                }
            }
        }
        return null;
    }

    private String resolveUrl(String src) {
        if (src.startsWith("http://") || src.startsWith("https://") || src.startsWith("file:")) {
            return src;
        }
        File file = new File(src);
        try {
            if (file.exists()) {
                return file.toURI().toURL().toString();
            }
        }catch (Exception ignored){}


        return src;
    }

    // ─── Eventos do MediaPlayer ────────────────────────────────────────────

    private void bindMediaEvents() {
        if (mediaPlayer == null) return;

        // Atualiza progresso (Protegido contra NaN e cliques)
        mediaPlayer.currentTimeProperty().addListener((obs, old, current) -> {
            if (progressSlider != null && !progressSlider.isPressed() && !progressSlider.isValueChanging()) {
                Duration total = mediaPlayer.getTotalDuration();
                if (total != null && !total.isUnknown() && total.toMillis() > 0) {
                    double progress = current.toMillis() / total.toMillis();
                    progressSlider.setValue(progress * 100);
                }
            }
            updateTimeLabel();
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            if (loop) {
                mediaPlayer.play();
            } else {
                updateControlsState();
                progressSlider.setValue(0);
                updateTimeLabel();
            }
        });

        mediaPlayer.setOnReady(() -> {
            mediaLoaded = true;
            updateControlsState();
            if (autoplay) {
                mediaPlayer.play();
            }
        });

        mediaPlayer.setOnError(() -> {
            String errorMsg = mediaPlayer.getError() != null ? mediaPlayer.getError().getMessage() : "Erro desconhecido";
            showError("Erro de reprodução: " + errorMsg);
            mediaError = true;
            mediaLoaded = false;
            updateControlsState();
        });
    }

    // ─── Controlos UI ──────────────────────────────────────────────────────

    private void createControls() {
        controlsContainer = new VBox(6);
        controlsContainer.setMaxWidth(Double.MAX_VALUE);
        controlsContainer.setStyle("-fx-padding: 8px 12px; -fx-background-color: rgba(0,0,0,0.7); -fx-background-radius: 8px; -fx-alignment: center;");
        controlsContainer.setMinHeight(60);

        // Barra de progresso
        progressSlider = new Slider();
        progressSlider.setMaxWidth(Double.MAX_VALUE);
        progressSlider.setMinHeight(18);
        progressSlider.setStyle("-fx-control-inner-background: rgba(255,255,255,0.2); -fx-accent: #3b82f6;");

        // Evento de arraste para seek
        progressSlider.setOnMousePressed(e -> {
            if (mediaPlayer != null) mediaPlayer.pause();
        });
        progressSlider.setOnMouseDragged(e -> {
            // Não fazer seek! Apenas mostra o tempo previsto no arrastar
            if (mediaPlayer != null && mediaLoaded) {
                Duration total = mediaPlayer.getTotalDuration();
                if (total != null && !total.isUnknown()) {
                    Duration previewTime = total.multiply(progressSlider.getValue() / 100.0);
                    if (timeLabel != null) {
                        timeLabel.setText(formatTime(previewTime) + " / " + formatTime(total));
                    }
                }
            }
        });

        progressSlider.setOnMouseReleased(e -> {
            // Executa o salto para o tempo apenas quando larga
            if (mediaPlayer != null && mediaLoaded) {
                Duration total = mediaPlayer.getTotalDuration();
                if (total != null && !total.isUnknown()) {
                    double progress = progressSlider.getValue() / 100.0;
                    mediaPlayer.seek(total.multiply(progress));
                }
            }
        });

        // HBox dos botões
        HBox controlsBox = new HBox(10);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        controlsBox.setStyle("-fx-padding: 2px 0;");

        // Botões
        playBtn = createControlButton("▶");
        pauseBtn = createControlButton("⏸");
        stopBtn = createControlButton("⏹");
        volumeBtn = createControlButton(muted ? "🔇" : "🔊");

        // Slider de volume
        volumeSlider = new Slider(0, 1, volume);
        volumeSlider.setPrefWidth(70);
        volumeSlider.setMinHeight(18);
        volumeSlider.setStyle("-fx-control-inner-background: rgba(255,255,255,0.2); -fx-accent: #3b82f6;");
        volumeSlider.valueProperty().addListener((obs, old, val) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(val.doubleValue());
                volumeBtn.setText(val.doubleValue() == 0 ? "🔇" : (mediaPlayer.isMute() ? "🔇" : "🔊"));
            }
        });

        // Label de tempo
        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-family: monospace;");

        // Label de erro (aparece se houver problema)
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        // ─── Eventos dos botões ──────────────────────────────────────────────

        playBtn.setOnAction(e -> {
            if (mediaPlayer != null && mediaLoaded) {
                mediaPlayer.play();
                updateControlsState();
            }
        });

        pauseBtn.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
                updateControlsState();
            }
        });

        stopBtn.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                progressSlider.setValue(0);
                updateTimeLabel();
                updateControlsState();
            }
        });

        volumeBtn.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.setMute(!mediaPlayer.isMute());
                volumeBtn.setText(mediaPlayer.isMute() ? "🔇" : "🔊");
            } else {
                muted = !muted;
                volumeBtn.setText(muted ? "🔇" : "🔊");
            }
        });

        // Adiciona nós
        controlsBox.getChildren().addAll(playBtn, pauseBtn, stopBtn, volumeBtn, volumeSlider, timeLabel);
        controlsContainer.getChildren().addAll(progressSlider, controlsBox);

        // Estado inicial
        updateControlsState();
        updateTimeLabel();
    }

    private Button createControlButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 4px 8px;");
        btn.setMinSize(30, 30);
        return btn;
    }

    // ─── Atualização do estado dos controlos ──────────────────────────────

    private void updateControlsState() {
        if (playBtn == null || pauseBtn == null) return;

        boolean hasMedia = mediaLoaded && mediaPlayer != null;

        if (hasMedia && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            playBtn.setDisable(true);
            pauseBtn.setDisable(false);
            stopBtn.setDisable(false);
        } else if (hasMedia && (mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED ||
                mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED)) {
            playBtn.setDisable(false);
            pauseBtn.setDisable(true);
            stopBtn.setDisable(false);
        } else {
            // Sem mídia carregada ou em erro
            playBtn.setDisable(true);
            pauseBtn.setDisable(true);
            stopBtn.setDisable(true);
        }

        // Se houver erro, desativa tudo
        if (mediaError) {
            playBtn.setDisable(true);
            pauseBtn.setDisable(true);
            stopBtn.setDisable(true);
        }
    }

    private void updateTimeLabel() {
        if (timeLabel == null) return;
        if (mediaPlayer != null && mediaLoaded) {
            Duration current = mediaPlayer.getCurrentTime();
            Duration total = mediaPlayer.getTotalDuration();
            timeLabel.setText(formatTime(current) + " / " + formatTime(total));
        } else {
            timeLabel.setText("00:00 / 00:00");
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText("⚠️ " + message);
            errorLabel.setVisible(true);
            // Adiciona ao container se não estiver presente
            if (!controlsContainer.getChildren().contains(errorLabel)) {
                controlsContainer.getChildren().add(errorLabel);
            }
        } else {
            // Fallback: mostrar no centro do vídeo
            Label error = new Label("⚠️ " + message);
            error.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 14px; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 10px;");
            videoArea.getChildren().add(error);
        }
        mediaError = true;
        updateControlsState();
    }

    // ─── Utilitários ────────────────────────────────────────────────────────

    private String formatTime(Duration duration) {
        if (duration == null || duration.isUnknown() || Double.isNaN(duration.toMillis())) {
            return "00:00";
        }
        long millis = (long) duration.toMillis();
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // ─── Overrides ──────────────────────────────────────────────────────────

    @Override
    protected void applyTagSpecificStyles() {
        // Podes adicionar estilos CSS específicos para <video>
    }

    @Override
    protected void addChildren() {
        // <video> processa os filhos <source> no parsing, não adiciona como nós visuais
    }

    @Override
    protected void bindEvents() {
        super.bindEvents();
        // Podes adicionar eventos XPL (onplay, onpause, etc.)
    }

    // ─── Getters para o motor ──────────────────────────────────────────────

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public boolean isMediaLoaded() {
        return mediaLoaded;
    }

    public boolean hasError() {
        return mediaError;
    }
}