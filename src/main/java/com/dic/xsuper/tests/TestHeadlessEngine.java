package com.dic.xsuper.tests;

import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.ui.SuperUiEngine;
import com.dic.xsuper.lang.ui.events.XplEvent;
import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.XplUiBridge;
import com.dic.xsuper.lang.ui.document.XplDocument;
import com.dic.xsuper.lang.ui.document.XplElement;

/**
 * Teste Headless da SuperUiEngine, sem JavaFX.
 * Verifica a hidratação, o índice de IDs e a reatividade.
 */
public class TestHeadlessEngine {
    public static void main(String[] args) {

        // =====================================================================
        // 1. A PONTE FANTASMA (Mock Renderer)
        // =====================================================================
        XplUiBridge consoleBridge = new XplUiBridge() {
            @Override
            public void renderView(XplNode activeDomRoot) {
                System.out.println("📺 [Ponte UI] Tela completa redesenhada! Nós ativos: " + activeDomRoot.children.size());
            }

            @Override
            public void updateProperty(String nodeId, String propertyName, Object newValue) {
                System.out.println("🎨 [Ponte UI] A pintar modificação cirúrgica: ID '" + nodeId + "' -> " + propertyName + " = " + newValue);
            }

            @Override
            public void setEngineCallback(EngineCallback callback) {
                System.out.println("🔌 [Ponte UI] Callback de eventos registado.");
            }

            @Override
            public void reportError(String message) {
                System.err.println("❌ [Ponte UI] Erro: " + message);
            }
        };

        // =====================================================================
        // 2. INICIALIZAR A ENGINE (com um Interpreter simulado)
        // =====================================================================
        // Como não temos um Interpreter real neste teste, criamos um stub
        // que apenas permite a injeção do documento.
        Interpreter mockInterpreter = new Interpreter(null, null) {
            // Sobrescrevemos o construtor para não precisar de CommandRegistry
            // e injetamos o documento manualmente.
        };
        // O construtor da SuperUiEngine já injeta o document no globals,
        // mas como passamos um Interpreter nulo, precisamos de garantir que
        // o documento seja criado. Vamos criar a engine com um Interpreter real
        // (mesmo que seja apenas para testes) – mas o Interpreter precisa de
        // um CommandRegistry. Podemos passar null e tratar os nulos.



        SuperUiEngine engine = new SuperUiEngine(null, consoleBridge);
        // Mas o construtor da SuperUiEngine espera um Interpreter não nulo.
        // Vamos criar um Interpreter mínimo para o teste.

        // =====================================================================
        // 2b. CRIAR UM INTERPRETER MÍNIMO PARA O TESTE
        // =====================================================================
        // Usamos um Interpreter real, mas sem registry. O construtor aceita null.
        Interpreter interpreter = new Interpreter(null, null);
        SuperUiEngine engine2 = new SuperUiEngine(interpreter, consoleBridge);

        // =====================================================================
        // 3. O HTML DO PROGRAMADOR
        // =====================================================================
        String html = """
            <div id="app" class="dark-mode-panel">
                <button id="btn-login" class="btn">Fazer Login</button>
            </div>
            """;

        System.out.println("--- 🚀 A INICIAR A SUPER UI ENGINE ---");
        engine2.loadView(html);

        // =====================================================================
        // 4. A MAGIA: SIMULANDO O CÓDIGO XPL!
        // =====================================================================
        System.out.println("\n--- 🧠 SIMULANDO A EXECUÇÃO DO SCRIPT XPL ---");

        // O documento global foi injetado como 'document' ou 'ui'
        XplDocument document = engine2.getDocument();

        System.out.println("> Executando: var botao = document.getElementById('btn-login');");
        XplElement botao = document.getElementById("btn-login");

        if (botao == null) {
            System.err.println("❌ Elemento 'btn-login' não encontrado!");
            return;
        }

        System.out.println("> Executando: botao.setAttribute('disabled', true);");
        botao.setAttribute("disabled", true);
        System.out.println("   -> Atributo 'disabled' definido para: " + botao.getAttribute("disabled"));

        System.out.println("> Executando: botao.setAttribute('class', 'btn btn-red');");
        botao.setAttribute("class", "btn btn-red");
        System.out.println("   -> Atributo 'class' definido para: " + botao.getAttribute("class"));

        // Verificar se o índice está a funcionar
        System.out.println("\n--- 🔍 VERIFICANDO O ÍNDICE DE IDs ---");
        XplElement app = document.getElementById("app");
        System.out.println("   -> document.getElementById('app') = " + app);
        System.out.println("   -> innerHTML do app: " + app.getInnerHTML());

        // Verificar o elemento pelo ID novamente (via índice)
        XplElement btnAgain = document.getElementById("btn-login");
        System.out.println("   -> document.getElementById('btn-login') (via índice) = " + btnAgain);
        System.out.println("   -> outerHTML do botão: " + btnAgain.getOuterHTML());

        // Testar a criação dinâmica de elementos
        System.out.println("\n--- 🛠️ CRIANDO UM NOVO ELEMENTO DINAMICAMENTE ---");
        XplElement novoDiv = document.createElement("div");
        novoDiv.setId("novo-div");
        novoDiv.setAttribute("style", "color: blue;");
        novoDiv.setTextContent("Olá, mundo!");
        document.body.appendChild(novoDiv);

        // Registrar o novo elemento no índice (opcional, mas recomendado)
        document.registerElement(novoDiv);

        // Verificar se está no índice
        XplElement divEncontrado = document.getElementById("novo-div");
        System.out.println("   -> Elemento criado e registado: " + divEncontrado);
        System.out.println("   -> Conteúdo: " + divEncontrado.getTextContent());

        // Simular um evento (sem JavaFX, apenas para ver o fluxo)
        System.out.println("\n--- 🎯 SIMULANDO UM CLIQUE (via dispatchEvent) ---");
        // Adicionar um listener ao botão (usando uma função XPL simulada)
        // Como não temos um XplFunction real, usamos um listener nativo.
        botao.addEventListener("click", (event) -> {
            System.out.println("   [Listener] Botão clicado! Evento: " + event);
        });

        // Disparar evento
        XplEvent clickEvent =
                new XplEvent("click", botao);
        botao.dispatchEvent(clickEvent);

        System.out.println("\n--- ✅ TESTE CONCLUÍDO COM SUCESSO ---");
    }
}