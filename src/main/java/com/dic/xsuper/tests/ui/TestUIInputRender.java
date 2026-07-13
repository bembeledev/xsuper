package com.dic.xsuper.tests.ui;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.tags.TagFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

/**
 * Teste Avançado de Renderização de Inputs HTML para JavaFX.
 * Demonstra todos os tipos de input, layouts complexos e estilos variados.
 */
public class TestUIInputRender extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Cria a árvore XplNode com um formulário extremamente completo
        XplNode root = createAdvancedForm();

        // Converte para NativeTag e constrói a UI JavaFX
        NativeTag rootTag = TagFactory.create(root);
        javafx.scene.Node fxRoot = rootTag.build();

        // Envolve num ScrollPane para lidar com formulários longos
        ScrollPane scrollPane = new ScrollPane(fxRoot);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #e8edf2; -fx-padding: 10;");

        Scene scene = new Scene(scrollPane, 800, 900);
        primaryStage.setTitle("🧪 Laboratório de Inputs - Xplorer UI Engine");
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("✅ UI renderizada com sucesso!");
    }

    // ─── CONSTRUTOR DO FORMULÁRIO AVANÇADO ────────────────────────────────

    public static XplNode createAdvancedForm() {
        XplNode container = new XplNode("div");
        container.attributes.put("style", """
                padding: 40px;
                max-width: 800px;
                margin: 0 auto;
                background: white;
                border-radius: 16px;
                box-shadow: 0 8px 30px rgba(0,0,0,0.12);
                font-family: 'Segoe UI', system-ui, sans-serif;
            """);

        // ─── CABEÇALHO ──────────────────────────────────────────────────────
        XplNode header = new XplNode("div");
        header.attributes.put("style", "display: flex; align-items: center; gap: 15px; margin-bottom: 30px; border-bottom: 2px solid #eef2f7; padding-bottom: 20px;");

        XplNode avatar = new XplNode("div");
        avatar.attributes.put("style", """
                width: 60px; height: 60px;
                border-radius: 50%;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                display: flex; align-items: center; justify-content: center;
                color: white; font-size: 28px; font-weight: bold;
            """);
        avatar.textContent = "👤";
        header.addChild(avatar);

        XplNode titleGroup = new XplNode("div");
        titleGroup.attributes.put("style", "display: flex; flex-direction: column;");

        XplNode title = new XplNode("h1");
        title.textContent = "Criar Conta";
        title.attributes.put("style", "margin: 0; font-size: 28px; color: #1a202c; font-weight: 700;");
        titleGroup.addChild(title);

        XplNode subtitle = new XplNode("span");
        subtitle.textContent = "Preencha os dados para criar a sua conta Xplorer";
        subtitle.attributes.put("style", "color: #718096; font-size: 15px;");
        titleGroup.addChild(subtitle);

        header.addChild(titleGroup);
        container.addChild(header);

        // ─── SECÇÃO: DADOS PESSOAIS ──────────────────────────────────────
        container.addChild(createSectionTitle("📋 Dados Pessoais"));

        container.addChild(createFormRow(
                createFormGroup("Nome Completo", "text", "João Silva", "João Silva", "required"),
                createFormGroup("E-mail", "email", "joao@email.com", "joao@email.com", "required")
        ));

        container.addChild(createFormRow(
                createFormGroup("Telefone", "tel", "(11) 99999-9999", "(11) 98765-4321", ""),
                createFormGroup("Data de Nascimento", "date", "", "2000-01-15", "")
        ));

        // ─── SECÇÃO: SEGURANÇA ────────────────────────────────────────────
        container.addChild(createSectionTitle("🔐 Segurança"));

        container.addChild(createFormRow(
                createFormGroup("Palavra-passe", "password", "••••••••", "12345678", "required"),
                createFormGroup("Confirmar Palavra-passe", "password", "••••••••", "12345678", "required")
        ));

        // ─── SECÇÃO: PREFERÊNCIAS ──────────────────────────────────────────
        container.addChild(createSectionTitle("🎨 Preferências"));

        // Linha com Color + Range
        container.addChild(createFormRow(
                createFormGroup("Cor do Tema", "color", "", "#667eea", ""),
                createFormGroup("Nível de Brilho", "range", "", "70", "")
        ));

        // Linha com Number + Checkbox
        container.addChild(createFormRow(
                createFormGroup("Idade", "number", "", "25", "min='18' max='120' step='1'"),
                createCheckboxGroup("Aceito termos de uso", true)
        ));

        // ─── SECÇÃO: GÉNERO (Radio Buttons) ──────────────────────────────
        container.addChild(createSectionTitle("⚥ Género"));

        XplNode radioGroup = new XplNode("div");
        radioGroup.attributes.put("style", "display: flex; gap: 25px; padding: 10px 0; flex-wrap: wrap;");

        radioGroup.addChild(createRadioOption("masculino", "Masculino", false));
        radioGroup.addChild(createRadioOption("feminino", "Feminino", false));
        radioGroup.addChild(createRadioOption("outro", "Outro", true)); // selecionado por defeito

        container.addChild(radioGroup);

        // ─── SECÇÃO: INTERESSES (Checkboxes múltiplos) ────────────────────
        container.addChild(createSectionTitle("🏷️ Interesses"));

        XplNode interestGrid = new XplNode("div");
        interestGrid.attributes.put("style", "display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px; padding: 10px 0;");

        interestGrid.addChild(createCheckbox("Programação", true));
        interestGrid.addChild(createCheckbox("Design", false));
        interestGrid.addChild(createCheckbox("Música", true));
        interestGrid.addChild(createCheckbox("Cinema", false));
        interestGrid.addChild(createCheckbox("Desporto", true));
        interestGrid.addChild(createCheckbox("Leitura", false));

        container.addChild(interestGrid);

        // ─── SECÇÃO: FICHEIRO ─────────────────────────────────────────────
        container.addChild(createSectionTitle("📎 Anexos"));

        XplNode fileGroup = new XplNode("div");
        fileGroup.attributes.put("style", "padding: 12px 0;");

        XplNode fileInput = new XplNode("input");
        fileInput.attributes.put("type", "file");
        fileInput.attributes.put("style", "padding: 10px; border: 2px dashed #cbd5e0; border-radius: 8px; width: 100%;");
        fileGroup.addChild(fileInput);

        container.addChild(fileGroup);

        // ─── SECÇÃO: COMENTÁRIO ────────────────────────────────────────────
        container.addChild(createSectionTitle("💬 Comentário Adicional"));

        XplNode textareaGroup = new XplNode("div");
        textareaGroup.attributes.put("style", "padding: 8px 0;");

        XplNode textarea = new XplNode("textarea");
        textarea.attributes.put("placeholder", "Escreva aqui o seu comentário...");
        textarea.attributes.put("rows", "4");
        textarea.attributes.put("style", """
                width: 100%; padding: 12px;
                border: 2px solid #e2e8f0;
                border-radius: 8px;
                font-size: 14px;
                font-family: inherit;
                resize: vertical;
            """);
        textarea.textContent = "Isto é um comentário de exemplo...";
        textareaGroup.addChild(textarea);

        container.addChild(textareaGroup);

        // ─── BOTÃO DE SUBMISSÃO ───────────────────────────────────────────
        XplNode buttonRow = new XplNode("div");
        buttonRow.attributes.put("style", "display: flex; gap: 15px; margin-top: 30px; justify-content: flex-end;");

        XplNode cancelBtn = new XplNode("button");
        cancelBtn.textContent = "Cancelar";
        cancelBtn.attributes.put("style", """
                padding: 12px 30px;
                border: 2px solid #e2e8f0;
                border-radius: 8px;
                background: white;
                color: #4a5568;
                font-weight: 600;
                cursor: pointer;
                transition: all 0.2s;
            """);
        buttonRow.addChild(cancelBtn);

        XplNode submitBtn = new XplNode("button");
        submitBtn.textContent = "✅ Criar Conta";
        submitBtn.attributes.put("style", """
                padding: 12px 40px;
                border: none;
                border-radius: 8px;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                font-weight: 700;
                font-size: 16px;
                cursor: pointer;
                box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
                transition: all 0.2s;
            """);
        buttonRow.addChild(submitBtn);

        container.addChild(buttonRow);

        return container;
    }

    // ─── UTILITÁRIOS DE CONSTRUÇÃO ────────────────────────────────────────

    private static XplNode createSectionTitle(String text) {
        XplNode section = new XplNode("div");
        section.attributes.put("style", "margin-top: 25px; margin-bottom: 10px;");

        XplNode title = new XplNode("h3");
        title.textContent = text;
        title.attributes.put("style", "font-size: 18px; font-weight: 600; color: #2d3748; margin: 0; padding: 8px 0;");
        section.addChild(title);

        XplNode divider = new XplNode("hr");
        divider.attributes.put("style", "border: none; height: 2px; background: linear-gradient(to right, #e2e8f0, transparent); margin: 4px 0 8px 0;");
        section.addChild(divider);

        return section;
    }

    private static XplNode createFormRow(XplNode... children) {
        XplNode row = new XplNode("div");
        row.attributes.put("style", "display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 12px;");
        for (XplNode child : children) {
            row.addChild(child);
        }
        return row;
    }

    private static XplNode createFormGroup(String label, String type, String placeholder, String value, String extraAttrs) {
        XplNode group = new XplNode("div");
        group.attributes.put("style", "display: flex; flex-direction: column; gap: 6px;");

        XplNode lbl = new XplNode("label");
        lbl.textContent = label;
        lbl.attributes.put("style", "font-size: 14px; font-weight: 500; color: #4a5568;");

        XplNode input = new XplNode("input");
        input.attributes.put("type", type);
        if (!placeholder.isEmpty()) input.attributes.put("placeholder", placeholder);
        if (!value.isEmpty()) input.attributes.put("value", value);
        if (!extraAttrs.isEmpty()) {
            for (String attr : extraAttrs.split(" ")) {
                if (attr.contains("=")) {
                    String[] parts = attr.split("=", 2);
                    input.attributes.put(parts[0], parts[1].replace("\"", ""));
                } else {
                    input.attributes.put(attr, "true");
                }
            }
        }

        // Estilo base para todos os inputs (exceto checkbox, radio, range, color, file)
        if (!type.matches("checkbox|radio|range|color|file|submit|reset|button|image|hidden")) {
            input.attributes.put("style", """
                    width: 100%; padding: 10px 14px;
                    border-radius: 8px;
                    font-size: 14px;
                    transition: border-color 0.2s;
                    background: #f7fafc;
                """);
        }

        group.addChild(lbl);
        group.addChild(input);

        return group;
    }

    private static XplNode createRadioOption(String name, String label, boolean selected) {
        XplNode wrapper = new XplNode("div");
        wrapper.attributes.put("style", "display: flex; align-items: center; gap: 8px;");

        XplNode radio = new XplNode("input");
        radio.attributes.put("type", "radio");
        radio.attributes.put("name", "gender");
        radio.attributes.put("value", name);
        if (selected) radio.attributes.put("checked", "true");

        XplNode lbl = new XplNode("label");
        lbl.textContent = label;
        lbl.attributes.put("style", "font-size: 14px; color: #2d3748; cursor: pointer;");

        wrapper.addChild(radio);
        wrapper.addChild(lbl);

        return wrapper;
    }

    private static XplNode createCheckbox(String label, boolean checked) {
        XplNode wrapper = new XplNode("div");
        wrapper.attributes.put("style", "display: flex; align-items: center; gap: 8px;");

        XplNode cb = new XplNode("input");
        cb.attributes.put("type", "checkbox");
        if (checked) cb.attributes.put("checked", "true");

        XplNode lbl = new XplNode("label");
        lbl.textContent = label;
        lbl.attributes.put("style", "font-size: 14px; color: #2d3748; cursor: pointer;");

        wrapper.addChild(cb);
        wrapper.addChild(lbl);

        return wrapper;
    }

    private static XplNode createCheckboxGroup(String label, boolean checked) {
        XplNode wrapper = new XplNode("div");
        wrapper.attributes.put("style", "display: flex; align-items: center; gap: 10px; padding-top: 8px;");

        XplNode cb = new XplNode("input");
        cb.attributes.put("type", "checkbox");
        if (checked) cb.attributes.put("checked", "true");

        XplNode lbl = new XplNode("label");
        lbl.textContent = label;
        lbl.attributes.put("style", "font-size: 14px; color: #2d3748; cursor: pointer;");

        wrapper.addChild(cb);
        wrapper.addChild(lbl);

        return wrapper;
    }
}