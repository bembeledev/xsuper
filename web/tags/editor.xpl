// =====================================================================
// 1. ESTADO REATIVO DO EDITOR
// =====================================================================

let currentFileName = "main.xpl";
let isEditable = true;

// O código inicial que vai ser injetado nativamente no RichTextFX!
let fileContent = """// Bem-vindo ao teu novo IDE construído em SuperUI!
fun main() {
    println("Olá, Mundo Nativo!");

    // O editor suporta dezenas de milhares de linhas
    // sem engasgar, graças à tua arquitetura JavaFX.
    let x = 10;
    let y = 20;

    return x + y;
}
""";

// O programador define as regras e o estilo no XPL!
let regrasSintaxe = [
    { "classe": "keyword", "regex": "\\b(fun|let|var|if|else|return)\\b", "color": "#569cd6", "bold": true },
    { "classe": "string",  "regex": "\"([^\"\\\\]|\\\\.)*\"", "color": "#ce9178" },
    { "classe": "comment", "regex": "//[^\n]*", "color": "#6a9955", "italic": true },
    { "classe": "number",  "regex": "\\b\\d+(\\.\\d+)?\\b", "color": "#b5cea8" }
];

// =====================================================================
// 2. MOTOR DE ESTILOS CSS (Tema VS Code Dark)
// =====================================================================

__ui_engine.loadStyles("""
    * { box-sizing: border-box; }

    body {
        background-color: #1e1e1e; /* Fundo do VS Code */
        color: #cccccc;
        margin: 0;
        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    }

    /* Layout Principal (Flexbox) */
    .ide-container {
        display: flex;
        flex-direction: column;
        height: 100vh; /* Ocupa a janela toda */
    }

    /* Barra de Topo */
    .header {
        background-color: #333333;
        padding: 8px 15px;
        font-size: 13px;
        border-bottom: 1px solid #111111;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    .header-title { font-weight: bold; color: #ffffff; }

    /* Área Central (Menu + Editor) */
    .main-area {
        display: flex;
        flex: 1; /* Preenche o resto da altura */
        overflow: hidden;
    }

    /* Barra Lateral (Explorador de Ficheiros) */
    .sidebar {
        width: 250px;
        background-color: #252526;
        border-right: 1px solid #111111;
        display: flex;
        flex-direction: column;
    }

    .sidebar-title {
        font-size: 11px;
        text-transform: uppercase;
        padding: 10px 15px;
        color: #888888;
        letter-spacing: 1px;
    }

    .file-item {
        padding: 6px 15px;
        font-size: 13px;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 8px;
    }

    .file-item:hover { background-color: #2a2d2e; color: #ffffff; }
    .file-item.active { background-color: #37373d; color: #ffffff; }

    /* Zona do Editor e Tabs */
    .editor-wrapper {
        flex: 1; /* Preenche a largura restante */
        display: flex;
        flex-direction: column;
        background-color: #1e1e1e;
    }

    .tabs-bar {
        background-color: #2d2d2d;
        display: flex;
        padding-top: 5px;
    }

    .tab {
        padding: 8px 20px;
        background-color: #1e1e1e;
        border-top: 2px solid #007acc; /* Destaque azul do ficheiro ativo */
        color: #ffffff;
        font-size: 13px;
        cursor: pointer;
    }

    /* ⭐ A NOSSA TAG NATIVA ⭐ */
    /* Garantimos que a StackPane JavaFX vai ocupar 100% da área disponível */
    editor {
        flex: 1;
        width: 100%;
        height: 100%;
    }
""");

// =====================================================================
// 3. ÁRVORE HTML VIRTUAL (DOM)
// =====================================================================

let htmlView = """
<html>
    <head></head>
    <body>
        <div class="ide-container">

            <!-- BARRA SUPERIOR -->
            <div class="header">
                <div class="header-title">SuperUI IDE • Workspace Alpha</div>
                <div style="color: #007acc; font-weight: bold;">Status: Online</div>
            </div>

            <!-- CORPO DO IDE -->
            <div class="main-area">

                <!-- BARRA LATERAL -->
                <div class="sidebar">
                    <div class="sidebar-title">Explorador de Ficheiros</div>
                    <div class="file-item active">📄 {{currentFileName}}</div>
                    <div class="file-item">📄 config.json</div>
                    <div class="file-item">📄 styles.css</div>
                </div>

                <!-- ZONA DE EDIÇÃO -->
                <div class="editor-wrapper">
                    <div class="tabs-bar">
                        <div class="tab">⚙️ {{currentFileName}}</div>
                    </div>

                    <!--
                         AQUI ESTÁ A TUA TAG!
                         O CSS vai esticá-la, e as variáveis injetam os dados.
                    -->
                    <editor
                        content="{fileContent}"
                        line-numbers="true"
                        editable="{isEditable}"
                        wrap-text="false"
                        syntax="{regrasSintaxe}"
                        tab-size="4">
                    </editor>

                </div>
            </div>

        </div>
    </body>
</html>
""";

// =====================================================================
// 4. INICIALIZAÇÃO DA SUPERUI
// =====================================================================

__ui_engine.loadView(htmlView);
__ui_engine.renderCycle();
__ui_engine.showWindow("SuperUI Code Editor", 1200, 800);