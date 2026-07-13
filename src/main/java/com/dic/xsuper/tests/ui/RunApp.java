package com.dic.xsuper.tests.ui;

import com.dic.xsuper.core.CommandRegistry;
import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.tags.TagFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

public class RunApp extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Inicializa o CommandRegistry e o Path para o construtor real do Interpreter
        CommandRegistry registry = new CommandRegistry();
        java.nio.file.Path currentDir = java.nio.file.Paths.get(".").toAbsolutePath();

        com.dic.xsuper.lang.Interpreter interpreter = new com.dic.xsuper.lang.Interpreter(registry, currentDir);
        com.dic.xsuper.lang.ui.SuperUiEngine engine = new com.dic.xsuper.lang.ui.SuperUiEngine(interpreter, null);

        XplNode root = Windows();

        NativeTag rootTag = TagFactory.create(root);
        javafx.scene.Node fxRoot = rootTag.build();

        ScrollPane scroll = new ScrollPane(fxRoot);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #f0f2f5; -fx-padding: 20;");

        Scene scene = new Scene(scroll, 700, 850);
        primaryStage.setTitle("🧪 Teste: Select com Optgroup");
        /*UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();*/
        //Application.setUserAgentStylesheet(new TransitTheme().getStyle().getStyleStylesheetURL());
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("✅ Select com optgroup renderizado com sucesso!");
    }

    private static XplNode Windows() {
        //return TestLists.createListTestTree();
        //return ListExamples.create();
        //return MediaExamples.demo();
        // return DetailsExample.createDetailsExample();
        // TabExample.createTabsExample();
        //return TabExampleModern.createModernTabs();
        //return OverflowExample.createScrollExample2();
        //return ComprehensiveEngineTest.createExample();
        //return FormExample.createFormExample();
        //return MenuExample.createNavExample();
        //return TestMenus.createFullMenuExample();
        //return TestUIRender.createSampleTree();
        //return StyleTransformTest.createTransformTestTree();
        //return AnimationTest.createTransformDemo();
        return TestUIInputRender.createAdvancedForm();
    }
}
