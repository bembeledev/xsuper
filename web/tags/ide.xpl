// ============================================================
// 1. DADOS DOS FICHEIROS E SINTAXE DINÂMICA
// ============================================================
let regrasXpl = [
    { "classe": "keyword", "regex": "\\b(fun|let|var|if|else|return|true|false)\\b", "color": "#569cd6", "bold": true },
    { "classe": "string",  "regex": "\"([^\"\\\\]|\\\\.)*\"", "color": "#ce9178" },
    { "classe": "comment", "regex": "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/", "color": "#6a9955", "italic": true },
    { "classe": "number",  "regex": "\\b\\d+(\\.\\d+)?\\b", "color": "#b5cea8" }
];

let codigoController = """
// =========================
// Controlador Principal
// =========================
fun iniciarSistema() {
    let status = true;

    if (status) {
        println("Sistema online e a usar o ecrã inteiro!");
    }
}
""";

let codigoEstilos = """
/* Ficheiro de Estilos W3C */
.alerta-critico {
    background-color: #ef4444;
    color: #ffffff;
}
""";

// ============================================================
// 2. ESTILOS GLOBAIS DA APLICAÇÃO (CSS Base Simplificado)
// ============================================================
__ui_engine.loadStyles("""
    body {
        font-family: "Segoe UI", sans-serif;
        background-color: #1e1e1e;
        color: #f8fafc;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        height: 100vh; /* Ocupa 100% da janela nativa */
    }

    .header {
        padding: 15px 20px;
        background-color: #252526;
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid #333333;
    }

    .footer {
        background-color: #007acc;
        color: white;
        padding: 5px 15px;
        font-size: 12px;
        display: flex;
        justify-content: space-between;
    }

    .btn-danger {
        background: #ef4444;
        color: white;
        border: none;
        border-radius: 4px;
        padding: 8px 16px;
        cursor: pointer;
        font-weight: bold;
    }

    .btn-salvar {
        background: #10b981;
        color: white;
        border: none;
        border-radius: 4px;
        padding: 8px 16px;
        cursor: pointer;
        font-weight: bold;
    }
""");

// ============================================================
// 3. FUNÇÕES DE AÇÃO DO XPL
// ============================================================
fun imprimirArquivos() {
    let conteudoMain = document.getElementById("editor-main").value;
    let conteudoCss = document.getElementById("editor-css").value;

    println("=== main.xpl ===\n" + conteudoMain);
    println("=== styles.css ===\n" + conteudoCss);
}

// ============================================================
// 4. VIEW PRINCIPAL (Layout Limpo)
// ============================================================
let htmlView = """
<html>
    <head></head>
    <body>

        <!-- 1. CABEÇALHO -->
        <div class="header">
            <div style="font-weight: bold; font-size: 16px;">SuperUI IDE Workspace</div>
            <div style="display: flex; gap: 10px;">
                <button class="btn-salvar" (click)="imprimirArquivos();">Imprimir Código</button>
                <button class="btn-danger" (click)="println('Encerrando...');">Encerrar</button>
            </div>
        </div>

        <!-- 2. SISTEMA DE ABAS (Preenche o espaço do meio com flex-grow: 1) -->
        <tabs tab-active-color="#38bdf8"
              tab-inactive-color="#94a3b8"
              tab-active-bg="#1e1e1e"
              tab-inactive-bg="#2d2d2d"
              tab-hover-bg="#333333"
              style="display: flex; flex-direction: column; flex-grow: 1;height:100%">

            <tab title="main.xpl" active="true" style="display: flex; flex-grow: 1; flex-direction: column; height:100%;">
                <editor id="editor-main"
                        content="{codigoController}"
                        syntax="{regrasXpl}"
                        line-numbers="true"
                        tab-size="4"
                        soft-tabs="true"
                        style="background-color: #1e1e1e; flex-grow: 1;height:100%;" />
            </tab>

            <tab title="styles.css" style="display: flex; flex-grow: 1; flex-direction: column; height:100%;">
                 <editor id="editor-css"
                        content="{codigoEstilos}"
                        syntax="{regrasXpl}"
                        line-numbers="true"
                        tab-size="4"
                        soft-tabs="true"
                        style="background-color: #1e1e1e; flex-grow: 1;height:100%;" />
            </tab>

        </tabs>

        <!-- 3. RODAPÉ -->
        <div class="footer">
            <span>Pronto - Sistema Estável</span>
            <span>UTF-8 | Espaços: 4</span>
        </div>

    </body>
</html>
""";

// ============================================================
// 5. INICIALIZAÇÃO DA SUPERUI
// ============================================================
__ui_engine.loadView(htmlView);
__ui_engine.renderCycle();
__ui_engine.showWindow("SuperUI - Code Editor", 1200, 850);