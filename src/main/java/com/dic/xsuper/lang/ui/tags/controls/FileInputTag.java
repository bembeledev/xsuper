package com.dic.xsuper.lang.ui.tags.controls;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Tag HTML &lt;input type="file"&gt; convertida para JavaFX FileChooser.
 * Exibe um botão que, ao ser clicado, abre o diálogo de seleção de ficheiros.
 */
public class FileInputTag extends FormControlTag {

    private Button fxButton;

    public FileInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        // 1. Cria o botão
        fxButton = new Button("Escolher ficheiro...");
        fxButton.setStyle("-fx-cursor: hand;");

        // 2. Aplica atributos comuns (disabled, required, etc.)
        applyCommonAttributes(fxButton);

        // 3. Configura a ação de seleção de ficheiro
        fxButton.setOnAction(e -> openFileChooser());

        return fxButton;
    }

    /**
     * Abre o seletor de ficheiros e processa a seleção.
     */
    private void openFileChooser() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar Ficheiro");

        // Suporte ao atributo 'accept' (extensões permitidas)
        if (sourceNode.attributes.containsKey("accept")) {
            String acceptAttr = (String) sourceNode.attributes.get("accept");
            String[] formats = acceptAttr.replace(" ", "").split(",");
            for (int i = 0; i < formats.length; i++) {
                if (formats[i].startsWith(".")) {
                    formats[i] = "*" + formats[i];
                }
            }
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Ficheiros Permitidos", formats)
            );
        }

        // Mostra o diálogo nativo do sistema
        File file = fc.showOpenDialog(null);

        if (file != null) {
            // Atualiza o texto do botão com o nome do ficheiro
            fxButton.setText(file.getName());
            // Guarda o caminho absoluto no userData do botão para acesso posterior
            fxButton.getProperties().put("xpl_file_path", file.getAbsolutePath());
            // Dispara evento para o motor XPL (ex: "change" ou "input")
            dispatchEvent("change", sourceNode.id, file.getAbsolutePath());
            dispatchEvent("input", sourceNode.id, file.getAbsolutePath());
        }
    }

    /**
     * Dispara um evento para o motor XPL.
     * Este método pode ser sobrescrito ou substituído pela ponte de eventos.
     */
    private void dispatchEvent(String eventName, String targetId, Object payload) {
        // Se a classe base NativeTag tiver um método dispatchEvent, usa-o.
        // Caso contrário, podes chamar o callback da bridge:
        // if (eventCallback != null) eventCallback.onEvent(eventName, targetId, payload);
        // Para já, apenas imprimimos no console para debug.
        System.out.println("[FileInputTag] Evento: " + eventName + " | ID: " + targetId + " | Payload: " + payload);
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Estilos específicos podem ser aplicados aqui, se necessário.
    }

    @Override
    protected void addChildren() {
        // <input type="file"> é void element – não tem filhos.
    }
}