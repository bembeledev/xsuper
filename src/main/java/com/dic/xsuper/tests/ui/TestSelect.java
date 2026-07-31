package com.dic.xsuper.tests.ui;
import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.render.javafx.tags.TagFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

/**
 * Teste completo para <select> com <optgroup> e <select multiple>.
 * Inclui também outros inputs para demonstrar o ecossistema.
 */
public class TestSelect extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        XplNode root = createFormWithSelects();

        NativeTag rootTag = TagFactory.create(root);
        javafx.scene.Node fxRoot = rootTag.build();

        ScrollPane scroll = new ScrollPane(fxRoot);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #f0f2f5; -fx-padding: 20;");

        Scene scene = new Scene(scroll, 700, 850);
        primaryStage.setTitle("🧪 Teste: Select com Optgroup");
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("✅ Select com optgroup renderizado com sucesso!");
    }

    // ─── CONSTRUTOR DO FORMULÁRIO ──────────────────────────────────────────

    private static XplNode createFormWithSelects() {
        XplNode container = new XplNode("div");
        container.attributes.put("style", """
                padding: 40px;
                max-width: 600px;
                margin: 0 auto;
                background: white;
                border-radius: 16px;
                box-shadow: 0 8px 30px rgba(0,0,0,0.10);
                font-family: 'Segoe UI', system-ui, sans-serif;
            """);

        // ─── TÍTULO ──────────────────────────────────────────────────────────
        XplNode title = new XplNode("h1");
        title.textContent = "🌍 Select com Optgroup";
        title.attributes.put("style", "text-align: center; color: #2d3748; font-size: 26px; margin: 0 0 25px 0;");
        container.addChild(title);

        // ─── CAMPO 1: SELECT COM OPTGROUP ──────────────────────────────────
        container.addChild(createSelectGroup());

        // ─── CAMPO 2: SELECT MÚLTIPLO ──────────────────────────────────────
        container.addChild(createMultiSelectGroup());

        // ─── CAMPO 3: SELECT SIMPLES (sem optgroup) ───────────────────────
        container.addChild(createSimpleSelectGroup());

        // ─── CAMPO 4: DATA ─────────────────────────────────────────────────
        container.addChild(createField("Data de Nascimento", "date", "2000-01-01", ""));

        // ─── CAMPO 5: CHECKBOX ─────────────────────────────────────────────
        container.addChild(createCheckboxGroup());

        // ─── BOTÃO DE SUBMISSÃO ────────────────────────────────────────────
        container.addChild(createSubmitButton());

        return container;
    }

    // ─── COMPONENTES ────────────────────────────────────────────────────────

    private static XplNode createSelectGroup() {
        XplNode group = new XplNode("div");
        group.attributes.put("style", "display: flex; flex-direction: column; gap: 4px; margin-bottom: 20px;");

        // Label
        XplNode label = new XplNode("label");
        label.textContent = "País de Origem:";
        label.attributes.put("style", "font-weight: 600; font-size: 14px; color: #4a5568;");
        group.addChild(label);

        // Select com optgroup
        XplNode select = new XplNode("select");
        select.attributes.put("id", "pais");
        select.attributes.put("style", """
                padding: 10px 14px;
                border: 2px solid #e2e8f0;
                border-radius: 8px;
                font-size: 14px;
                margin-bottom: 14px;
                background: #f7fafc;
            """);

        // Optgroup: Europa
        XplNode eu = new XplNode("optgroup");
        eu.attributes.put("label", "🌍 Europa");
        eu.addChild(createOption("pt", "Portugal"));
        eu.addChild(createOption("es", "Espanha"));
        eu.addChild(createOption("fr", "França", true)); // França selecionada
        eu.addChild(createOption("it", "Itália"));
        select.addChild(eu);

        // Optgroup: África
        XplNode af = new XplNode("optgroup");
        af.attributes.put("label", "🌍 África");
        af.addChild(createOption("mz", "Moçambique"));
        af.addChild(createOption("ao", "Angola"));
        af.addChild(createOption("za", "África do Sul"));
        af.addChild(createOption("ke", "Quénia"));
        select.addChild(af);

        // Optgroup: Ásia
        XplNode asia = new XplNode("optgroup");
        asia.attributes.put("label", "🌍 Ásia");
        asia.addChild(createOption("jp", "Japão"));
        asia.addChild(createOption("cn", "China"));
        asia.addChild(createOption("in", "Índia"));
        select.addChild(asia);

        group.addChild(select);
        return group;
    }

    private static XplNode createMultiSelectGroup() {
        XplNode group = new XplNode("div");
        group.attributes.put("style", "display: flex; flex-direction: column; gap: 4px; margin-bottom: 20px;");

        XplNode label = new XplNode("label");
        label.textContent = "Interesses (seleção múltipla):";
        label.attributes.put("style", "font-weight: 600; font-size: 14px; color: #4a5568;");
        group.addChild(label);

        XplNode select = new XplNode("select");
        select.attributes.put("id", "interesses");
        select.attributes.put("multiple", "true");
        select.attributes.put("size", "4");
        select.attributes.put("style", """
                padding: 8px;
                border: 2px solid #e2e8f0;
                border-radius: 8px;
                font-size: 14px;
                background: #f7fafc;
                min-height: 100px;
            """);

        // Opções sem optgroup (vão para um grupo "fantasma" sem label)
        select.addChild(createOption("prog", "Programação", true));
        select.addChild(createOption("design", "Design", false));
        select.addChild(createOption("musica", "Música", true));
        select.addChild(createOption("cinema", "Cinema", false));
        select.addChild(createOption("desporto", "Desporto", false));
        select.addChild(createOption("leitura", "Leitura", true));

        group.addChild(select);
        return group;
    }

    private static XplNode createSimpleSelectGroup() {
        XplNode group = new XplNode("div");
        group.attributes.put("style", "display: flex; flex-direction: column; gap: 4px; margin-bottom: 20px;");

        XplNode label = new XplNode("label");
        label.textContent = "Género:";
        label.attributes.put("style", "font-weight: 600; font-size: 14px; color: #4a5568;");
        group.addChild(label);

        XplNode select = new XplNode("select");
        select.attributes.put("id", "genero");
        select.attributes.put("style", """
                padding: 10px 14px;
                border: 2px solid #e2e8f0;
                border-radius: 8px;
                font-size: 14px;
                background: #f7fafc;
            """);

        select.addChild(createOption("m", "Masculino"));
        select.addChild(createOption("f", "Feminino"));
        select.addChild(createOption("o", "Outro", true));

        group.addChild(select);
        return group;
    }

    private static XplNode createField(String label, String type, String value, String placeholder) {
        XplNode group = new XplNode("div");
        group.attributes.put("style", "display: flex; flex-direction: column; gap: 4px; margin-bottom: 20px;");

        XplNode lbl = new XplNode("label");
        lbl.textContent = label;
        lbl.attributes.put("style", "font-weight: 600; font-size: 14px; color: #4a5568;");
        group.addChild(lbl);

        XplNode input = new XplNode("input");
        input.attributes.put("type", type);
        if (!value.isEmpty()) input.attributes.put("value", value);
        if (!placeholder.isEmpty()) input.attributes.put("placeholder", placeholder);
        input.attributes.put("style", """
                padding: 10px 14px;
                border: 2px solid #e2e8f0;
                border-radius: 8px;
                font-size: 14px;
                background: #f7fafc;
            """);

        group.addChild(input);
        return group;
    }

    private static XplNode createCheckboxGroup() {
        XplNode group = new XplNode("div");
        group.attributes.put("style", "display: flex; align-items: center; gap: 10px; margin-bottom: 25px;");

        XplNode cb = new XplNode("input");
        cb.attributes.put("type", "checkbox");
        cb.attributes.put("checked", "true");
        cb.attributes.put("style", "width: 18px; height: 18px;");

        XplNode label = new XplNode("label");
        label.textContent = "Aceito os termos de uso.";
        label.attributes.put("style", "font-size: 14px; color: #4a5568; cursor: pointer;");

        group.addChild(cb);
        group.addChild(label);
        return group;
    }

    private static XplNode createSubmitButton() {
        XplNode btn = new XplNode("input");
        btn.attributes.put("type", "submit");
        btn.attributes.put("value", "✅ Submeter");
        btn.attributes.put("style", """
                width: 100%;
                padding: 14px;
                border: none;
                margin-top: 10px;
                border-radius: 8px;
                background: linear-gradient(135deg, #667eea, #764ba2);
                color: white;
                font-weight: 700;
                font-size: 16px;
                cursor: pointer;
                transition: 0.2s;
                box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
            """);
        return btn;
    }

    // ─── UTILITÁRIO PARA CRIAR OPTION ──────────────────────────────────────

    private static XplNode createOption(String value, String label) {
        return createOption(value, label, false);
    }

    private static XplNode createOption(String value, String label, boolean selected) {
        XplNode opt = new XplNode("option");
        opt.attributes.put("value", value);
        if (selected) opt.attributes.put("selected", "true");
        opt.textContent = label;
        return opt;
    }
}