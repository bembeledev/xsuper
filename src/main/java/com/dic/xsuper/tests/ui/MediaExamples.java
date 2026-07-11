package com.dic.xsuper.tests.ui;

import com.dic.xsuper.lang.ui.html.XplNode;

import java.io.File;

public class MediaExamples {

    /**
     * Cria um nó <audio> com controles, autoplay, loop e uma fonte.
     */
    public static XplNode createAudioNode() {
        // 1. Nó principal <audio>
        XplNode audio = new XplNode("audio");
        audio.attributes.put("controls", "true");
        audio.attributes.put("autoplay", "true");
        audio.attributes.put("loop", "true");

        // 2. Nó <source> (filho)
        XplNode source = new XplNode("source");
        String caminho = "E:/Adoraca/Data/13_Ir-Manuel-Ferramenta-Sem-Deus-o-homem-não-vale-nada.mp3";
        File file = new File(caminho);
        System.out.println(file.isFile()); // deve imprimir true

        try {
            String mediaUrl = file.toURI().toURL().toString();
            source.attributes.put("src", mediaUrl);
            audio.addChild(source);
        }catch (Exception ignored){ }
  return audio;
    }

    /**
     * Cria um nó <video> com controles, poster, dimensões e uma fonte.
     */
    public static XplNode createVideoNode() {
        // 1. Nó principal <video>
        XplNode video = new XplNode("video");
        video.attributes.put("controls", "true");
        video.attributes.put("poster", "https://example.com/poster.jpg");
        video.attributes.put("width", "640");
        video.attributes.put("height", "360");

        // 2. Nó <source> (filho)
        XplNode source = new XplNode("source");
        source.attributes.put("src", "https://www.w3schools.com/html/mov_bbb.mp4");
        video.addChild(source);

        return video;
    }

    /**
     * Exemplo de como usar no motor (se estiveres a usar a UI).
     */
    public static XplNode demo() {
        // Criar os nós
        XplNode audioNode = createAudioNode();
        XplNode videoNode = createVideoNode();

        // Opcional: adicionar ambos a um contentor para renderizar na mesma janela
        XplNode container = new XplNode("div");
        container.attributes.put("style", "display: flex; flex-direction: column; gap: 20px; padding: 20px;");
        container.addChild(audioNode);
        container.addChild(videoNode);

        // Para renderizar, basta passar o container para o motor:
        // __ui_engine.loadView(container);  (se o teu motor aceitar XplNode diretamente)
        // Ou converter para HTML e carregar: 
        // String html = generateHTML(container);
        // __ui_engine.loadView(html);

        return container;
    }
}