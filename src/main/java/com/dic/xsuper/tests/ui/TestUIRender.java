package com.dic.xsuper.tests.ui;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.tags.TagFactory;


public class TestUIRender  {

    public static void testStyleResolution() {
        System.out.println("--- Teste de Resolução de Estilos ---");

        // Simula um nó div com estilo inline
        XplNode node = new XplNode("div");
        node.id = "testDiv";
        node.className = "container";
        node.attributes.put("style", "padding: 20px; background: #f0f0f0; border: 2px solid #333; border-radius: 10px;");
        node.textContent = "Olá mundo!";

        NativeTag tag = TagFactory.create(node);
        System.out.println("Tag criada: " + tag.getClass().getSimpleName());
        System.out.println("ID Atribuído: " + tag.getId());
    }

    public static XplNode createSampleTree() {
        XplNode root = new XplNode("div");
        root.attributes.put("style", "padding: 20px; display: flex; gap: 10px; border: 2px solid #ff0000;");

        // Botão com evento de clique na gaveta correta 'events'
        XplNode child1 = new XplNode("button");
        child1.attributes.put("style", "padding: 10px; background: #007bff; color: white; border-radius: 5px;");



        child1.events.put("click", "println(event.detail);"); // ⭐ Usar .events.put com a chave limpa "click"
        child1.textContent = "Clique com Evento";

        // Input de texto com evento de teclado na gaveta 'events'
        XplNode child2 = new XplNode("input");
        child2.attributes.put("style", "padding: 1px; border: 6px solid #ccc;");
        child2.attributes.put("placeholder", "Digite algo...");
        child2.events.put("input", "println(event.detail.value);"); // ⭐ Usar .events.put com a chave limpa "keydown"
        child2.events.put("resize", "println(event.detail);"); // ⭐ Usar .events.put com a chave limpa "keydown"
        //child2.events.put("mouseenter", "println(event.currentTarget);"); // ⭐ Usar .events.put com a chave limpa "keydown"
        //child2.events.put("focus", "println(event);"); // ⭐ Usar .events.put com a chave limpa "keydown"

        root.addChild(child2);
        root.addChild(child1);
        return root;
    }
}