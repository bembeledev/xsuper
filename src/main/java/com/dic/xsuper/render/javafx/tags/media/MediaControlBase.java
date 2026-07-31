package com.dic.xsuper.render.javafx.tags.media;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Classe base para controlos de mídia (áudio/vídeo).
 * Fornece play/pause, stop, volume, mute, progresso, tempo, loop, velocidade.
 * Pode ser personalizada via CSS ou programaticamente.
 */
public class MediaControlBase {

    protected MediaPlayer mediaPlayer;
    protected Runnable onPlay;
    protected Runnable onPause;
    protected Runnable onStop;
    protected Runnable onEnd;
    protected Consumer<Double> onVolumeChange;

    // Componentes UI
    protected VBox container;
    protected Slider progressSlider;
    protected Label timeLabel;
    protected Button playBtn;
    protected Button pauseBtn;
    protected Button stopBtn;
    protected Button muteBtn;
    protected Slider volumeSlider;
    protected Button loopBtn;
    protected ComboBox<String> speedCombo;
    protected Button fullscreenBtn; // para vídeo

    protected boolean loop = false;
    protected double volume = 1.0;
    protected boolean muted = false;
    protected double speed = 1.0;

    // Controla se a barra de progresso está a ser arrastada
    private boolean isDragging = false;

    public MediaControlBase(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
        if (mediaPlayer != null) {
            this.volume = mediaPlayer.getVolume();
            this.muted = mediaPlayer.isMute();
        }
        buildUI();
        bindEvents();
    }

    protected void buildUI() {
        container = new VBox(6);
        container.setPadding(new Insets(8, 12, 8, 12));
        container.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-background-radius: 8px; -fx-alignment: center;");
        container.setMinHeight(70);

        // Barra de progresso
        progressSlider = new Slider();
        progressSlider.setMaxWidth(Double.MAX_VALUE);
        progressSlider.setMinHeight(20);
        progressSlider.setStyle("-fx-control-inner-background: rgba(255,255,255,0.2); -fx-accent: #3b82f6;");

        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        // HBox dos botões
        HBox buttonRow = new HBox(8);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        buttonRow.setStyle("-fx-padding: 4px 0;");

        // Botões
        playBtn = createButton("▶", "Play");
        pauseBtn = createButton("⏸", "Pause");
        stopBtn = createButton("⏹", "Stop");
        muteBtn = createButton(muted ? "🔇" : "🔊", "Mute");
        loopBtn = createButton("🔁", "Loop");
        speedCombo = new ComboBox<>();
        speedCombo.getItems().addAll("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x");
        speedCombo.setValue("1.0x");
        speedCombo.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 12px;");

        // Slider de volume
        volumeSlider = new Slider(0, 1, volume);
        volumeSlider.setPrefWidth(70);
        volumeSlider.setMinHeight(16);
        volumeSlider.setStyle("-fx-control-inner-background: rgba(255,255,255,0.2); -fx-accent: #3b82f6;");

        // Label de tempo
        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-family: monospace; -fx-padding: 0 8px;");

        // Fullscreen (apenas para vídeo, mas adicionamos)
        fullscreenBtn = createButton("⛶", "Fullscreen");
        fullscreenBtn.setVisible(false);

        // Adiciona ao HBox
        buttonRow.getChildren().addAll(playBtn, pauseBtn, stopBtn, muteBtn, volumeSlider, timeLabel, loopBtn, speedCombo, fullscreenBtn);
        // Espaçador para alinhar à direita (se necessário)
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonRow.getChildren().add(spacer);

        container.getChildren().addAll(progressSlider, buttonRow);

        // Estado inicial
        updateState();
    }

    protected Button createButton(String text, String tooltip) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 4px 6px;");
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }

    protected void bindEvents() {
        if (mediaPlayer == null) return;

        // Play/Pause/Stop
        playBtn.setOnAction(e -> {
            mediaPlayer.play();
            updateState();
            if (onPlay != null) onPlay.run();
        });
        pauseBtn.setOnAction(e -> {
            mediaPlayer.pause();
            updateState();
            if (onPause != null) onPause.run();
        });
        stopBtn.setOnAction(e -> {
            mediaPlayer.stop();
            progressSlider.setValue(0);
            updateTimeLabel(Duration.ZERO);
            updateState();
            if (onStop != null) onStop.run();
        });

        // Mute
        muteBtn.setOnAction(e -> {
            mediaPlayer.setMute(!mediaPlayer.isMute());
            muted = mediaPlayer.isMute();
            muteBtn.setText(muted ? "🔇" : "🔊");
        });

        // Volume
        volumeSlider.valueProperty().addListener((obs, old, val) -> {
            mediaPlayer.setVolume(val.doubleValue());
            if (onVolumeChange != null) onVolumeChange.accept(val.doubleValue());
            muted = val.doubleValue() == 0;
            muteBtn.setText(muted ? "🔇" : "🔊");
        });

        // Loop e Velocidade (Mantém-se igual)
        loopBtn.setOnAction(e -> {
            loop = !loop;
            loopBtn.setStyle(loop ? "-fx-background-color: rgba(59,130,246,0.3); -fx-text-fill: #3b82f6;" : "-fx-background-color: transparent; -fx-text-fill: white;");
            mediaPlayer.setCycleCount(loop ? MediaPlayer.INDEFINITE : 1);
        });
        speedCombo.setOnAction(e -> {
            String selected = speedCombo.getValue();
            if (selected != null) {
                speed = Double.parseDouble(selected.replace("x", ""));
                mediaPlayer.setRate(speed);
            }
        });

        // ⭐ CORREÇÃO 1: Progresso (Protegido contra NaN e conflitos de Rato)
        mediaPlayer.currentTimeProperty().addListener((obs, old, current) -> {
            if (progressSlider != null && !progressSlider.isPressed() && !progressSlider.isValueChanging()) {
                Duration total = mediaPlayer.getTotalDuration();

                // ⭐ A NOVA PROTEÇÃO: Rejeitar também o isIndefinite()
                if (total != null && !total.isUnknown() && !total.isIndefinite() && total.toMillis() > 0) {
                    double progress = (current.toMillis() / total.toMillis()) * 100.0;
                    progressSlider.setValue(progress);
                }
                updateTimeLabel(current); // Se current for Indefinite, o formatTime tratará disso!
            }
        });

        // ⭐ CORREÇÃO 2: Arraste Inteligente (Sem encravar o motor)
        progressSlider.setOnMousePressed(e -> {
            // Opcional: pausar o áudio enquanto o utilizador arrasta a barra
        });

        progressSlider.setOnMouseDragged(e -> {
            if (mediaPlayer != null) {
                Duration total = mediaPlayer.getTotalDuration();
                // ⭐ Proteção
                if (total != null && !total.isUnknown() && !total.isIndefinite()) {
                    Duration previewTime = total.multiply(progressSlider.getValue() / 100.0);
                    if (timeLabel != null) {
                        timeLabel.setText(formatTime(previewTime) + " / " + formatTime(total));
                    }
                }
            }
        });

        progressSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null) {
                Duration total = mediaPlayer.getTotalDuration();
                // ⭐ Proteção antes de fazer o "Seek"
                if (total != null && !total.isUnknown() && !total.isIndefinite()) {
                    double progress = progressSlider.getValue() / 100.0;
                    mediaPlayer.seek(total.multiply(progress));
                }
            }
        });

        // Quando termina
        mediaPlayer.setOnEndOfMedia(() -> {
            if (loop) {
                mediaPlayer.play();
            } else {
                progressSlider.setValue(0);
                updateTimeLabel(Duration.ZERO);
                updateState();
                if (onEnd != null) onEnd.run();
            }
        });

        // Quando fica pronto
        mediaPlayer.setOnReady(() -> {
            updateState();
            updateTimeLabel(mediaPlayer.getCurrentTime());
        });
    }

    // ⭐ CORREÇÃO 3: Blindagem contra durações desconhecidas
    protected String formatTime(Duration duration) {
        if (duration == null || duration.isUnknown() || Double.isNaN(duration.toMillis())) {
            return "00:00";
        }
        long millis = (long) duration.toMillis();
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    protected void updateState() {
        if (mediaPlayer == null) return;
        boolean isPlaying = mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
        playBtn.setDisable(isPlaying);
        pauseBtn.setDisable(!isPlaying);
        stopBtn.setDisable(false);
    }

    protected void updateTimeLabel(Duration current) {
        if (timeLabel == null) return;
        Duration total = mediaPlayer != null ? mediaPlayer.getTotalDuration() : null;
        String currentStr = current != null ? formatTime(current) : "00:00";
        System.out.println(total);
        String totalStr = total != null ? formatTime(total) : "00:00";
        timeLabel.setText(currentStr + " / " + totalStr);
    }



    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
        // Rebind events
    }

    public VBox getContainer() {
        return container;
    }

    public Slider getProgressSlider() {
        return progressSlider;
    }

    public void setOnPlay(Runnable onPlay) {
        this.onPlay = onPlay;
    }

    public void setOnPause(Runnable onPause) {
        this.onPause = onPause;
    }

    public void setOnStop(Runnable onStop) {
        this.onStop = onStop;
    }

    public void setOnEnd(Runnable onEnd) {
        this.onEnd = onEnd;
    }

    public void setOnVolumeChange(Consumer<Double> onVolumeChange) {
        this.onVolumeChange = onVolumeChange;
    }

    public void showFullscreen(boolean show) {
        fullscreenBtn.setVisible(show);
    }
}