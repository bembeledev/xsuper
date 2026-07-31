package com.dic.xsuper.tests.ui;


import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.render.javafx.tags.TagFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class TestUIRender02 extends Application {

    public static void main(String[] args) {
        // Teste lógico (sem UI) – verifica a resolução de estilos
        testStyleResolution();

        // Teste visual (JavaFX) – renderiza uma árvore complexa
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Cria uma árvore XplNode complexa
        XplNode root = createComplexTree();

        // Converte para NativeTag e constrói o nó JavaFX
        NativeTag rootTag = TagFactory.create(root);
        javafx.scene.Node fxRoot = rootTag.build();

        // Envolve num ScrollPane para permitir visualizar grandes árvores
        ScrollPane scrollPane = new ScrollPane(fxRoot);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);

        // Mostra numa janela
        StackPane pane = new StackPane(scrollPane);
        pane.setStyle("-fx-background-color: #2b2b2b;");
        Scene scene = new Scene(pane, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Teste UI Avançado - HTML para JavaFX");
        primaryStage.show();
    }

    private static void testStyleResolution() {
        System.out.println("=== TESTE DE RESOLUÇÃO DE ESTILOS ===");

        // 1. Teste básico: div com padding, background, border, border-radius
        XplNode node1 = new XplNode("div");
        node1.id = "testDiv";
        node1.className = "container";
        node1.attributes.put("style",
                "padding: 20px; " +
                        "background: #f0f0f0; " +
                        "border: 2px solid #333; " +
                        "border-radius: 10px; " +
                        "color: #ff0000; " +
                        "font-size: 18px; " +
                        "font-weight: bold;"
        );
        node1.textContent = "Olá mundo!";

        NativeTag tag1 = TagFactory.create(node1);
        System.out.println("1. Tag: " + tag1.getClass().getSimpleName());
        System.out.println("   Padding: " + tag1.getResolvedStyles().padding);
        System.out.println("   Margin: " + tag1.getResolvedStyles().margin);
        System.out.println("   Border: " + tag1.getResolvedStyles().border);
        System.out.println("   BorderRadius: " + tag1.getResolvedStyles().borderRadius);
        System.out.println("   BackgroundColor: " + Integer.toHexString(tag1.getResolvedStyles().backgroundColor));
        System.out.println("   TextColor: " + Integer.toHexString(tag1.getResolvedStyles().textColor));
        System.out.println("   FontSize: " + tag1.getResolvedStyles().fontSize);
        System.out.println("   FontWeight: " + tag1.getResolvedStyles().fontWeight);
        System.out.println();

        // 2. Teste com margin e padding assimétricos
        XplNode node2 = new XplNode("div");
        node2.attributes.put("style",
                "margin: 10px 20px 30px 40px; " +
                        "padding: 5px 15px 25px 35px; " +
                        "background: #e0e0e0;"
        );
        NativeTag tag2 = TagFactory.create(node2);
        System.out.println("2. Tag: " + tag2.getClass().getSimpleName());
        System.out.println("   Margin: " + tag2.getResolvedStyles().margin);
        System.out.println("   Padding: " + tag2.getResolvedStyles().padding);
        System.out.println();

        // 3. Teste com box-shadow
        XplNode node3 = new XplNode("div");
        node3.attributes.put("style",
                "box-shadow: 10px 10px 20px rgba(0,0,0,0.5); " +
                        "background: #ffffff;"
        );
        NativeTag tag3 = TagFactory.create(node3);
        System.out.println("3. Tag: " + tag3.getClass().getSimpleName());
        System.out.println("   BoxShadows: " + tag3.getResolvedStyles().boxShadows.size());
        if (!tag3.getResolvedStyles().boxShadows.isEmpty()) {
            System.out.println("   BoxShadow[0]: offsetX=" + tag3.getResolvedStyles().boxShadows.get(0).getOffsetX() +
                    ", offsetY=" + tag3.getResolvedStyles().boxShadows.getFirst().getOffsetY() +
                    ", blur=" + tag3.getResolvedStyles().boxShadows.getFirst().getBlurRadius() +
                    ", spread=" + tag3.getResolvedStyles().boxShadows.getFirst().getSpreadRadius() +
                    ", color=" + Integer.toHexString(tag3.getResolvedStyles().boxShadows.get(0).getColor()));
        }
        System.out.println();

        // 4. Teste com gradiente (linear e radial)
        XplNode node4 = new XplNode("div");
        node4.attributes.put("style",
                "background: linear-gradient(to right, red, blue); " +
                        "width: 200px; " +
                        "height: 100px;"
        );
        NativeTag tag4 = TagFactory.create(node4);
        System.out.println("4. Tag: " + tag4.getClass().getSimpleName());
        System.out.println("   Gradients: " + tag4.getResolvedStyles().gradients.size());
        if (!tag4.getResolvedStyles().gradients.isEmpty()) {
            System.out.println("   Gradient[0] type: " + tag4.getResolvedStyles().gradients.get(0).getType());
            System.out.println("   Gradient[0] stops: " + tag4.getResolvedStyles().gradients.get(0).getStops().size());
        }
        System.out.println();

        System.out.println("=== FIM DO TESTE LÓGICO ===");
        System.out.println();
    }

    private static XplNode createComplexTree() {
        // Raiz: container principal com flexbox e gap
        XplNode root = new XplNode("div");
        root.id = "root";
        root.className = "main-container";
        root.attributes.put("style",
                "padding: 30px; " +
                        "background: #f8f9fa; " +
                        "display: flex; " +
                        "flex-direction: column; " +
                        "gap: 20px; " +
                        "font-family: 'Segoe UI', Arial, sans-serif;"
        );
        root.addChild(createHeader());
        root.addChild(createMainContent());
        root.addChild(createFooter());
        return root;
    }

    private static XplNode createHeader() {
        XplNode header = new XplNode("div");
        header.id = "header";
        header.className = "header";
        header.attributes.put("style",
                "display: flex; " +
                        "justify-content: space-between; " +
                        "align-items: center; " +

                        "padding: 15px 20px; " +
                        "background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); " +
                        "border-radius: 10px; " +
                        "box-shadow: 0 4px 15px rgba(0,0,0,0.2);"
        );

        // Título
        XplNode title = new XplNode("h1");
        title.attributes.put("style",
                "color: white; " +
                        "font-size: 28px; " +
                        "font-weight: bold; " +
                        "margin: 0; " +
                        "text-shadow: 2px 2px 4px rgba(0,0,0,0.3);"
        );
        title.textContent = "🏆 XPL UI - Teste Avançado";

        // Botão de ação
        XplNode btnAction = new XplNode("button");
        btnAction.id = "btnAction";
        btnAction.className = "btn-primary";
        btnAction.attributes.put("style",
                "padding: 10px 25px; " +
                        "background: white; " +
                        "color: #764ba2; " +
                        "border: none; " +
                        "border-radius: 25px; " +
                        "font-size: 16px; " +
                        "font-weight: bold; " +
                        "cursor: pointer; " +
                        "box-shadow: 0 2px 8px rgba(0,0,0,0.2); " +
                        "transition: all 0.3s ease;"
        );
        btnAction.textContent = "Clique aqui!";
        btnAction.events.put("click", "handleClick");

        header.addChild(title);
        header.addChild(btnAction);
        return header;
    }

    private static XplNode createMainContent() {
        XplNode main = new XplNode("div");
        main.id = "mainContent";
        main.attributes.put("style",
                "display: grid; " +
                        "grid-template-columns: 1fr 2fr 1fr; " +
                        "gap: 20px; " +
                        "padding: 10px 0;"
        );

        main.addChild(createSidebar());
        main.addChild(createCentralPanel());
        main.addChild(createRightPanel());
        return main;
    }

    private static XplNode createSidebar() {
        XplNode sidebar = new XplNode("div");
        sidebar.id = "sidebar";
        sidebar.className = "sidebar";
        sidebar.attributes.put("style",
                "background: white; " +
                        "padding: 20px; " +
                        "border-radius: 10px; " +
                        "box-shadow: 0 2px 10px rgba(0,0,0,0.1);"
        );

        XplNode subtitle = new XplNode("h3");
        subtitle.attributes.put("style",
                "color: #333; " +
                        "font-size: 18px; " +
                        "margin-top: 0; " +
                        "border-bottom: 2px solid #eee; " +
                        "padding-bottom: 10px;"
        );
        subtitle.textContent = "📋 Menu";

        XplNode list = new XplNode("ul");
        list.attributes.put("style",
                "list-style: none; " +
                        "padding: 0; " +
                        "margin: 10px 0;"
        );

        String[] items = {"Dashboard", "Relatórios", "Configurações", "Perfil", "Sair"};
        for (String item : items) {
            XplNode li = new XplNode("li");
            li.attributes.put("style",
                    "padding: 10px 15px; " +
                            "margin-bottom: 5px; " +
                            "background: #f8f9fa; " +
                            "border-radius: 5px; " +
                            "cursor: pointer; " +
                            "transition: all 0.2s ease;"
            );
            li.textContent = "• " + item;
            li.events.put("click", "handleMenuItem('" + item + "')");
            list.addChild(li);
        }

        sidebar.addChild(subtitle);
        sidebar.addChild(list);
        return sidebar;
    }

    private static XplNode createCentralPanel() {
        XplNode central = new XplNode("div");
        central.id = "centralPanel";
        central.className = "central-panel";
        central.attributes.put("style",
                "background: white; " +
                        "padding: 20px; " +
                        "border-radius: 10px; " +
                        "box-shadow: 0 2px 10px rgba(0,0,0,0.1);"
        );

        // Título
        XplNode title = new XplNode("h2");
        title.attributes.put("style",
                "color: #2c3e50; " +
                        "font-size: 22px; " +
                        "margin-top: 0; " +
                        "border-bottom: 2px solid #3498db; " +
                        "padding-bottom: 10px;"
        );
        title.textContent = "📊 Painel Central";

        // Descrição
        XplNode desc = new XplNode("p");
        desc.attributes.put("style",
                "color: #555; " +
                        "font-size: 14px; "
                       // + "line-height: 1.6;"
        );
        desc.textContent = "Esta é uma demonstração avançada do motor de UI, convertendo HTML/CSS em componentes JavaFX nativos.";

        // Formulário de exemplo
        XplNode form = new XplNode("div");
        form.attributes.put("style",
                "margin-top: 20px; " +
                        "padding: 15px; " +
                        "background: #f8f9fa; " +
                        "border-radius: 8px; " +
                        "border: 1px solid #e0e0e0;"
        );

        XplNode labelNome = new XplNode("label");
        labelNome.attributes.put("style",
                "display: block; " +
                        "font-weight: bold; " +
                        "color: #333; " +
                        "margin-bottom: 5px;"
        );
        labelNome.textContent = "Nome:";

        XplNode inputNome = new XplNode("input");
        inputNome.id = "inputNome";
        inputNome.attributes.put("style",
                "width: 100%; " +
                        "padding: 8px 12px; " +
                        "border: 2px solid #ced4da; " +
                        "border-radius: 5px; " +
                        "font-size: 14px; " +
                        "box-sizing: border-box;"
        );
        inputNome.attributes.put("placeholder", "Digite seu nome...");
        inputNome.events.put("input", "handleInput");

        XplNode labelEmail = new XplNode("label");
        labelEmail.attributes.put("style",
                "display: block; " +
                        "font-weight: bold; " +
                        "color: #333; " +
                        "margin-top: 15px; " +
                        "margin-bottom: 5px;"
        );
        labelEmail.textContent = "E-mail:";

        XplNode inputEmail = new XplNode("input");
        inputEmail.id = "inputEmail";
        inputEmail.attributes.put("style",
                "width: 100%; " +
                        "padding: 8px 12px; " +
                        "border: 2px solid #ced4da; " +
                        "border-radius: 5px; " +
                        "font-size: 14px; " +
                        "box-sizing: border-box;"
        );
        inputEmail.attributes.put("placeholder", "seu@email.com");
        inputEmail.events.put("input", "handleInput");

        XplNode btnSubmit = new XplNode("button");
        btnSubmit.id = "btnSubmit";
        btnSubmit.attributes.put("style",
                        "margin: 15px; " +
                        "padding: 10px 30px; " +
                        "background: #3498db; " +
                        "color: white; " +
                        "border: none; " +
                        "border-radius: 5px; " +
                        "font-size: 16px; " +
                        "font-weight: bold; " +
                        "cursor: pointer; " +
                        "box-shadow: 0 2px 8px rgba(52,152,219,0.3);"
        );
        btnSubmit.textContent = "Enviar";
        btnSubmit.events.put("click", "handleSubmit");

        form.addChild(labelNome);
        form.addChild(inputNome);
        form.addChild(labelEmail);
        form.addChild(inputEmail);
        form.addChild(btnSubmit);

        central.addChild(title);
        central.addChild(desc);
        central.addChild(form);
        return central;
    }

    private static XplNode createRightPanel() {
        XplNode right = new XplNode("div");
        right.id = "rightPanel";
        right.className = "right-panel";
        right.attributes.put("style",
                "background: white; " +
                        "padding: 20px; " +
                        "border-radius: 10px; " +
                        "box-shadow: 0 2px 10px rgba(0,0,0,0.1);"
        );

        XplNode title = new XplNode("h3");
        title.attributes.put("style",
                "color: #333; " +
                        "font-size: 18px; " +
                        "margin-top: 0; " +
                        "border-bottom: 2px solid #eee; " +
                        "padding-bottom: 10px;"
        );
        title.textContent = "📈 Status";

        // Status visual (simulado)
        String[] statuses = {"🟢 Online", "🟡 Conectando...", "🔴 Offline", "🟢 Ativo"};
        for (String status : statuses) {
            XplNode item = new XplNode("div");
            item.attributes.put("style",
                    "padding: 8px 12px; " +
                            "margin-bottom: 5px; " +
                            "background: #f8f9fa; " +
                            "border-radius: 5px; " +
                            "font-size: 14px; " +
                            "border-left: 4px solid #28a745;"
            );
            item.textContent = status;
            right.addChild(item);
        }

        // Badge exemplo
        XplNode badge = new XplNode("div");
        badge.attributes.put("style",
                "margin-top: 15px; " +
                        "padding: 10px; " +
                        "background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); " +
                        "border-radius: 8px; " +
                        "color: white; " +
                        "text-align: center; " +
                        "font-weight: bold; " +
                        "font-size: 14px; " +
                        "box-shadow: 0 2px 10px rgba(245,87,108,0.3);"
        );
        badge.textContent = "🚀 Versão 2.0.0";

        right.addChild(title);
        right.addChild(badge);
        return right;
    }

    private static XplNode createFooter() {
        XplNode footer = new XplNode("div");
        footer.id = "footer";
        footer.className = "footer";
        footer.attributes.put("style",
                "padding: 15px 20px; " +
                        "background: #2c3e50; " +
                        "border-radius: 10px; " +
                        "color: white; " +
                        "text-align: center; " +
                        "font-size: 14px; " +
                        "margin-top: 10px; " +
                        "box-shadow: 0 -2px 10px rgba(0,0,0,0.1);"
        );

        XplNode text = new XplNode("span");
        text.setAttribute("style","color:#FFFFFF;");
        text.textContent = "© 2026 XPL Engine - Teste Avançado de UI | Desenvolvido com XPL e JavaFX";
        footer.addChild(text);
        return footer;
    }
}