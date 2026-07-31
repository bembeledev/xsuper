package com.dic.xsuper.render.javafx.event;

import com.dic.xsuper.dom.event.XplEvent;
import com.dic.xsuper.dom.event.XplEventType;
import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.engine.natives.UINativeRegistry;
import com.dic.xsuper.render.javafx.core.SuperUiEngine;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.media.MediaPlayer;
import javafx.scene.web.WebView;

import java.util.Map;

public class AttachJavaFxListener {
    /**
     * Mapeia os eventos do enum W3C para os Listeners reais do JavaFX.
     */
    public static void attachJavaFxListener(XplNode sourceNode, Node fxNode, XplEventType type, String scriptCallback) {
        switch (type) {
            // ─── 🖱️ EVENTOS DE RATO (MOUSE) ──────────────────────────────────
            case CLICK, DBLCLICK -> {
                // ⭐ Proteção contra Botões: Eles exigem 'setOnAction' em vez de MouseClicked
                if (fxNode instanceof ButtonBase btn) {
                    if (type == XplEventType.CLICK) btn.setOnAction(e -> fireXplEvent(sourceNode, type, e, scriptCallback));
                } else {
                    fxNode.setOnMouseClicked(e -> {
                        if (type == XplEventType.CLICK && e.getClickCount() == 1) {
                            fireXplEvent(sourceNode, type, e, scriptCallback);
                        } else if (type == XplEventType.DBLCLICK && e.getClickCount() == 2) {
                            fireXplEvent(sourceNode, type, e, scriptCallback);
                        }
                    });
                }
            }
            case MOUSEDOWN -> fxNode.setOnMousePressed(e -> fireXplEvent( sourceNode,type, e, scriptCallback));
            case MOUSEUP -> fxNode.setOnMouseReleased(e -> fireXplEvent( sourceNode,type, e, scriptCallback));
            case MOUSEENTER -> fxNode.setOnMouseEntered(e -> fireXplEvent( sourceNode,type, e, scriptCallback));
            case MOUSELEAVE -> fxNode.setOnMouseExited(e -> fireXplEvent( sourceNode,type, e, scriptCallback));
            case MOUSEMOVE -> {
                // ⭐ MAGIA W3C: Na Web, o mousemove dispara mesmo quando o rato está premido a desenhar.
                // No JavaFX, temos de escutar os dois comportamentos para imitar o browser perfeitamente!
                fxNode.setOnMouseMoved(e -> fireXplEvent(sourceNode, type, e, scriptCallback));
                fxNode.setOnMouseDragged(e -> fireXplEvent(sourceNode, type, e, scriptCallback));
            }
            case MOUSEOVER -> fxNode.setOnMouseEntered(e -> fireXplEvent( sourceNode,type, e, scriptCallback)); // alias
            case MOUSEOUT -> fxNode.setOnMouseExited(e -> fireXplEvent( sourceNode,type, e, scriptCallback));   // alias
            case DRAG -> fxNode.setOnMouseDragged(e -> fireXplEvent( sourceNode,type, e, scriptCallback));
            case CONTEXTMENU -> fxNode.setOnContextMenuRequested(e -> fireXplEvent( sourceNode,type, e, scriptCallback));
            case WHEEL -> fxNode.setOnScroll(e -> fireXplEvent( sourceNode,type, e, scriptCallback));

            // ─── ⌨️ EVENTOS DE TECLADO (KEYBOARD) ────────────────────────────
            case KEYDOWN -> fxNode.setOnKeyPressed(e -> fireXplEvent( sourceNode,type, e, scriptCallback));
            case KEYUP -> fxNode.setOnKeyReleased(e -> fireXplEvent( sourceNode,type, e, scriptCallback));
            case KEYPRESS -> fxNode.setOnKeyTyped(e -> fireXplEvent( sourceNode,type, e, scriptCallback));

            // ─── 🎯 EVENTOS DE FOCO (FOCUS) ──────────────────────────────────
            case FOCUS -> fxNode.focusedProperty().addListener((obs, old, isFocused) -> {
                if (isFocused) fireXplEvent( sourceNode,type, null, scriptCallback);
            });
            case BLUR -> fxNode.focusedProperty().addListener((obs, old, isFocused) -> {
                if (!isFocused) fireXplEvent( sourceNode,type, null, scriptCallback);
            });

            // ─── 📦 EVENTOS DE ARRASTAR E LARGAR (DRAG & DROP) ──────────────
            case DRAGSTART -> fxNode.setOnDragDetected(e -> fireXplEvent( sourceNode,type, e, scriptCallback));
            case DRAGENTER -> fxNode.setOnDragEntered(e -> fireXplEvent( sourceNode,type, e, scriptCallback));
            case DRAGOVER -> fxNode.setOnDragOver(e -> fireXplEvent( sourceNode,type, e, scriptCallback));
            case DRAGLEAVE -> fxNode.setOnDragExited(e -> fireXplEvent( sourceNode,type, e, scriptCallback));
            case DROP -> fxNode.setOnDragDropped(e -> fireXplEvent( sourceNode,type, e, scriptCallback));
            case DRAGEND -> fxNode.setOnDragDone(e -> fireXplEvent( sourceNode,type, e, scriptCallback));

            // ─── 📝 EVENTOS DE FORMULÁRIO (INPUT/CHANGE/SELECT) ──────────────
            case INPUT -> {
                if (fxNode instanceof TextInputControl text) {
                    text.textProperty().addListener((obs, old, val) -> {
                        fireXplEvent( sourceNode,type, val, scriptCallback);
                    });
                } else if (fxNode instanceof ComboBox<?> combo) {
                    combo.valueProperty().addListener((obs, old, val) -> {
                        fireXplEvent(sourceNode,type, val, scriptCallback);
                    });
                } else if (fxNode instanceof Slider slider) {
                    slider.valueProperty().addListener((obs, old, val) -> {
                        fireXplEvent(sourceNode,type, val, scriptCallback);
                    });
                }
            }
            case CHANGE -> {
                if (fxNode instanceof CheckBox cb) {
                    cb.selectedProperty().addListener((obs, old, val) -> {
                        fireXplEvent(sourceNode,type, val, scriptCallback);
                    });
                } else if (fxNode instanceof RadioButton rb) {
                    rb.selectedProperty().addListener((obs, old, val) -> {
                        fireXplEvent(sourceNode,type, val, scriptCallback);
                    });
                } else if (fxNode instanceof ComboBox<?> combo) {
                    combo.valueProperty().addListener((obs, old, val) -> {
                        fireXplEvent(sourceNode,type, val, scriptCallback);
                    });
                } else if (fxNode instanceof ListView<?> lv) {
                    lv.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
                        fireXplEvent(sourceNode,type, val, scriptCallback);
                    });
                }
            }
            case SELECT -> {
                if (fxNode instanceof TextInputControl text) {
                    text.selectedTextProperty().addListener((obs, old, val) -> {
                        fireXplEvent(sourceNode,type, val, scriptCallback);
                    });
                }
            }

            // ─── 🎬 EVENTOS DE MÍDIA (ÁUDIO/VIDEO) ──────────────────────────
            case PLAY, PAUSE, ENDED, TIMEUPDATE, VOLUMECHANGE, LOADEDMETADATA, LOADEDDATA, PROGRESS -> {
                // Estes eventos são tratados diretamente no MediaControlBase
                // A lógica está em AudioTag / VideoTag / MediaControlBase
                // Para suporte genérico, usamos um listener de propriedades
                if (fxNode.getProperties().containsKey("xpl_media_player")) {
                    MediaPlayer mediaPlayer = (MediaPlayer) fxNode.getProperties().get("xpl_media_player");
                    bindMediaEvents(sourceNode,mediaPlayer, type, scriptCallback);
                }
            }


            case SCROLL -> {
                if (fxNode instanceof ScrollPane sp) {
                    sp.vvalueProperty().addListener((obs, old, val) -> {
                        fireXplEvent(sourceNode,type, Map.of("vvalue", val), scriptCallback);
                    });
                    sp.hvalueProperty().addListener((obs, old, val) -> {
                        fireXplEvent(sourceNode,type, Map.of("hvalue", val), scriptCallback);
                    });
                }
            }
            case LOAD -> {
                if (fxNode instanceof WebView webView) {
                    webView.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                        if (state == Worker.State.SUCCEEDED) {
                            fireXplEvent(sourceNode,type, webView.getEngine().getLocation(), scriptCallback);
                        }
                    });
                }
            }

            // ─── 📄 EVENTOS DE DOCUMENTO/JANELA ──────────────────────────────

            case BEFOREUNLOAD, VISIBILITYCHANGE -> {
                // Temos de esperar que o fxNode seja adicionado a uma Scene e a uma Window (Stage)
                fxNode.sceneProperty().addListener((obsScene, oldScene, newScene) -> {
                    if (newScene != null) {
                        newScene.windowProperty().addListener((obsWin, oldWin, newWin) -> {
                            if (newWin instanceof javafx.stage.Stage stage) {
                                if (type == XplEventType.BEFOREUNLOAD) {
                                    stage.setOnCloseRequest(e -> fireXplEvent(sourceNode,type, null, scriptCallback));
                                }
                                if (type == XplEventType.VISIBILITYCHANGE) {
                                    stage.iconifiedProperty().addListener((o, old, val) -> {
                                        fireXplEvent(sourceNode,type, java.util.Map.of("iconified", val), scriptCallback);
                                    });
                                    stage.showingProperty().addListener((o, old, val) -> {
                                        fireXplEvent(sourceNode,type, java.util.Map.of("showing", val), scriptCallback);
                                    });
                                }
                            }
                        });
                    }
                });
            }

            case RESIZE -> {
                // 1. Suporte moderno (ResizeObserver): O próprio elemento (div, botão, etc.) muda de tamanho
                if (fxNode instanceof javafx.scene.layout.Region region) {
                    region.widthProperty().addListener((obs, old, val) -> {
                        fireXplEvent(sourceNode,type, java.util.Map.of("width", val), scriptCallback);
                    });
                    region.heightProperty().addListener((obs, old, val) -> {
                        fireXplEvent(sourceNode,type, java.util.Map.of("height", val), scriptCallback);
                    });
                }

                // 2. Suporte clássico (Window Resize): Escutamos a alteração de tamanho da Scene global
                fxNode.sceneProperty().addListener((obsScene, oldScene, newScene) -> {
                    if (newScene != null) {
                        newScene.widthProperty().addListener((obs, old, val) -> {
                            fireXplEvent(sourceNode,type, java.util.Map.of("width", val), scriptCallback);
                        });
                        newScene.heightProperty().addListener((obs, old, val) -> {
                            fireXplEvent(sourceNode,type, java.util.Map.of("height", val), scriptCallback);
                        });
                    }
                });
            }

            // ─── 📋 EVENTOS DE CLIPBOARD ──────────────────────────────────────
            case CUT, COPY, PASTE -> {
                // Atalhos de teclado são tratados globalmente na Scene
                // Registamos o evento na Scene do nó
                Node sceneNode = fxNode.getScene() != null ? fxNode.getScene().getRoot() : fxNode;
                sceneNode.setOnKeyPressed(e -> {
                    if (e.isShortcutDown()) {
                        switch (type) {
                            case CUT -> { if (e.getCode() == KeyCode.X) fireXplEvent(sourceNode,type, e, scriptCallback); }
                            case COPY -> { if (e.getCode() == KeyCode.C) fireXplEvent(sourceNode,type, e, scriptCallback); }
                            case PASTE -> { if (e.getCode() == KeyCode.V) fireXplEvent(sourceNode,type, e, scriptCallback); }
                        }
                    }
                });
            }

            case CLOSE -> {

            }

            // ─── 🎯 EVENTOS DE TOQUE (TOUCH) ──────────────────────────────────
            case TOUCHSTART -> fxNode.setOnTouchPressed(e -> fireXplEvent(sourceNode,type, e, scriptCallback));
            case TOUCHMOVE -> fxNode.setOnTouchMoved(e -> fireXplEvent(sourceNode,type, e, scriptCallback));
            case TOUCHEND -> fxNode.setOnTouchReleased(e -> fireXplEvent(sourceNode,type, e, scriptCallback));
            case TOUCHCANCEL -> fxNode.setOnTouchStationary(e -> fireXplEvent(sourceNode,type, e, scriptCallback));

            // ─── 🌐 EVENTOS DE REDE E ESTADO ──────────────────────────────────
            case ONLINE, OFFLINE -> {
                // Monitoriza o estado da rede (não nativo no JavaFX)
                // Pode ser implementado com NetworkInterface ou lib externa
                // Placeholder
            }
            case ERROR -> {
                if (fxNode instanceof WebView webView) {
                    webView.getEngine().getLoadWorker().exceptionProperty().addListener((obs, old, ex) -> {
                        if (ex != null) fireXplEvent(sourceNode,type, ex.getMessage(), scriptCallback);
                    });
                } else if (fxNode.getProperties().containsKey("xpl_media_player")) {
                    MediaPlayer mediaPlayer = (MediaPlayer) fxNode.getProperties().get("xpl_media_player");
                    mediaPlayer.setOnError(() -> {
                        fireXplEvent(sourceNode,type, mediaPlayer.getError().getMessage(), scriptCallback);
                    });
                }
            }
            case ABORT -> {
                // Carregamento abortado (ex: WebView)
                if (fxNode instanceof WebView webView) {
                    webView.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                        if (state == Worker.State.CANCELLED) {
                            fireXplEvent(sourceNode,type, null, scriptCallback);
                        }
                    });
                }
            }
            case TOGGLE -> {
                // Para <details>/<summary>
                if (fxNode instanceof ToggleButton tb) {
                    tb.selectedProperty().addListener((obs, old, val) -> {
                        fireXplEvent(sourceNode,type, val, scriptCallback);
                    });
                }
            }

            // ─── ⚙️ EVENTOS DE CARREGAMENTO (GENÉRICO) ──────────────────────
            case DOMCONTENTLOADED -> {
                if (fxNode instanceof WebView webView) {
                    webView.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                        if (state == Worker.State.RUNNING) {
                            fireXplEvent(sourceNode,type, null, scriptCallback);
                        }
                    });
                }
            }

            default -> {
                // Evento não suportado
                System.err.println("[XplUI] Evento não suportado: " + type);
            }
        }
    }

    /**
     * Método auxiliar para ligar eventos de mídia.
     */
    private static void bindMediaEvents(XplNode sourceNode, MediaPlayer mediaPlayer, XplEventType type, String scriptCallback) {
        switch (type) {
            case PLAY -> mediaPlayer.statusProperty().addListener((obs, old, status) -> {
                if (status == MediaPlayer.Status.PLAYING) fireXplEvent(sourceNode,type, null, scriptCallback);
            });
            case PAUSE -> mediaPlayer.statusProperty().addListener((obs, old, status) -> {
                if (status == MediaPlayer.Status.PAUSED) fireXplEvent(sourceNode,type, null, scriptCallback);
            });
            case ENDED -> mediaPlayer.setOnEndOfMedia(() -> fireXplEvent(sourceNode,type, null, scriptCallback));
            case TIMEUPDATE -> mediaPlayer.currentTimeProperty().addListener((obs, old, cur) -> {
                fireXplEvent(sourceNode,type, cur.toMillis(), scriptCallback);
            });
            case VOLUMECHANGE -> mediaPlayer.volumeProperty().addListener((obs, old, val) -> {
                fireXplEvent(sourceNode,type, val, scriptCallback);
            });
            case LOADEDMETADATA, LOADEDDATA -> mediaPlayer.setOnReady(() -> {
                fireXplEvent(sourceNode,type, null, scriptCallback);
            });
            case PROGRESS -> mediaPlayer.bufferProgressTimeProperty().addListener((obs, old, val) -> {
                if (val != null) fireXplEvent(sourceNode,type, val.toMillis(), scriptCallback);
            });
            default -> {}
        }
    }


    /**
     * A PONTE UNIVERSAL: Recebe qualquer tipo de dados (Eventos JavaFX, Mapas ou Valores puros)
     * e empacota-os num XplEvent para o Interpretador.
     */
    private static void fireXplEvent(XplNode sourceNode, XplEventType type, Object eventData, String callbackString) {

        // ⭐ CORREÇÃO 2: Passar o LiveElement (O objeto real do DOM) e não o Virtual Node!
        // A avaliação é "lazy" (tardia), por isso quando o utilizador clica, o liveElement já não é nulo.
        Object targetElement = sourceNode.liveElement != null ? sourceNode.liveElement : sourceNode;

        // 1. Cria a instância nativa do evento XPL com o Alvo Real
        XplEvent xplEvent = new XplEvent(type.getWebName(), targetElement, targetElement);

        // ⭐ A CURA DO ERRO DE TIPO: Carimbar a instância física com a Classe da Máquina Virtual!
        if (UINativeRegistry.EVENT_CLASS != null) {
            xplEvent.klass = UINativeRegistry.EVENT_CLASS;
        }

        // 2. Tira os dados à medida do tipo de objecto recebido
        if (eventData != null) {
            if (eventData instanceof javafx.scene.input.MouseEvent me) {
                xplEvent.setDetail("x", me.getX());
                xplEvent.setDetail("y", me.getY());
                xplEvent.setDetail("clientX", me.getSceneX());
                xplEvent.setDetail("clientY", me.getSceneY());
                xplEvent.setDetail("screenX", me.getScreenX());
                xplEvent.setDetail("screenY", me.getScreenY());
                xplEvent.setDetail("button", me.getButton().name());
            }
            else if (eventData instanceof javafx.scene.input.KeyEvent ke) {
                xplEvent.setDetail("key", ke.getText());
                xplEvent.setDetail("code", ke.getCode().name());
            }
            else if (eventData instanceof javafx.scene.input.ScrollEvent se) {
                xplEvent.setDetail("deltaX", se.getDeltaX());
                xplEvent.setDetail("deltaY", se.getDeltaY());
            }
            else if (eventData instanceof javafx.scene.input.DragEvent de) {
                xplEvent.setDetail("sceneX", de.getSceneX());
                xplEvent.setDetail("sceneY", de.getSceneY());
            }
            else if (eventData instanceof java.util.Map<?, ?> map) {
                for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                    xplEvent.setDetail(entry.getKey().toString(), entry.getValue());
                }
            }
            else if (!(eventData instanceof javafx.event.Event)) {
                xplEvent.setDetail("value", eventData);
            }
        }

        // ⭐ CORREÇÃO 3: Execução Directa (Bypass do EventBus para interações de utilizador rápidas)
        SuperUiEngine.getInstance().executeInlineScript(callbackString, xplEvent);

        // 4. Bloqueia o comportamento do JavaFX se o script XPL fez 'event.preventDefault()'
        if (xplEvent.defaultPrevented && eventData instanceof javafx.event.Event fxEvent) {
            fxEvent.consume();
        }
    }
}
