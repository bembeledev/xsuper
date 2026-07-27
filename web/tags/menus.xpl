// ============================================================
// 1. DADOS XPL (Sintaxe e Funções)
// ============================================================
let regrasSintaxe = [
    { "classe": "keyword", "regex": "\\b(fun|let|if|else|return)\\b", "color": "#c678dd", "bold": true },
    { "classe": "string",  "regex": "\"([^\"\\\\]|\\\\.)*\"", "color": "#98c379" },
    { "classe": "comment", "regex": "//[^\n]*", "color": "#5c6370", "italic": true }
];

let codigoDemo = """
// ==========================================
// TESTE DE MENUS DE CONTEXTO E FLEXBOX
// ==========================================
fun main() {
    let mensagem = "Clique direito em qualquer lugar!";
    println(mensagem);
}
""";

fun acaoMenu(nomeAcao:string) {
    println("[AÇÃO EXECUTADA] O utilizador clicou em: " + nomeAcao);
}

// ============================================================
// 2. CSS BASE (O nosso motor agora converte isto para Flexbox nativo)
// ============================================================
__ui_engine.loadStyles("""
    body {
        background-color: #0f172a; /* Fundo principal escuro */
        color: #f8fafc;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        height: 100vh;
        font-family: "Segoe UI", sans-serif;
    }
    .painel-lateral {
        background-color: #1e293b;
        border-right: 1px solid #334155;
        width: 250px;
        padding: 15px;
        display: flex;
        flex-direction: column;
        gap: 10px;
    }
    .item-lista {
        background-color: #334155;
        padding: 10px 15px;
        border-radius: 6px;
        cursor: hand;
        transition: background-color 0.2s;
    }
    .item-lista:hover {
        background-color: #475569;
    }
""");

// ============================================================
// 3. A INTERFACE GRÁFICA (VDOM)
// ============================================================
let htmlView = """
<html>
    <head></head>

    <!--
      ⭐ O BODY TEM O MENU "menu-workspace"
      Se clicares no fundo escuro, abre o menu geral!
    -->
    <body context-menu="menu-workspace">

        <!-- ⭐ 1. BANCO DE MENUS (Invisíveis na tela) ⭐ -->
        <contextmenu id="menu-workspace">
            <menuitem label="Novo Ficheiro..." icon="📄" shortcut="Ctrl+N" (click)="acaoMenu('Novo Ficheiro');" />
            <menuitem label="Nova Pasta..." icon="📁" (click)="acaoMenu('Nova Pasta');" />
            <menuitem separator="true" />
            <menuitem label="Recarregar Janela" icon="🔄" shortcut="F5" (click)="acaoMenu('Recarregar');" />
        </contextmenu>

        <contextmenu id="menu-editor">
            <menuitem label="Copiar Código" icon="📋" shortcut="Ctrl+C" (click)="acaoMenu('Copiar');" />
            <menuitem label="Colar" icon="📝" shortcut="Ctrl+V" (click)="acaoMenu('Colar');" />
            <menuitem separator="true" />
            <menuitem label="Formatar Documento" icon="✨" shortcut="Alt+Shift+F" (click)="acaoMenu('Formatar');" />
        </contextmenu>

        <contextmenu id="menu-ficheiro">
            <menuitem label="Renomear" icon="✏️" shortcut="F2" (click)="acaoMenu('Renomear Ficheiro');" />
            <menuitem label="Mover para o Lixo" icon="🗑️" shortcut="Del" style="color: #ef4444;" (click)="acaoMenu('Apagar Ficheiro');" />
        </contextmenu>

        <contextmenu id="menu-avancado">

            <menuitem label="Copiar" icon="📋" shortcut="Ctrl+C" />
            <menuitem label="Colar" icon="📝" shortcut="Ctrl+V" />

            <menuitem separator="true" />

            <!-- ⭐ SUB-MENU 1 (Multi-menu) -->
            <menu label="Partilhar" icon="🔗">
                <menuitem label="Email" icon="📧" />
                <menuitem label="Redes Sociais" icon="🌐" />
                <menuitem label="Copiar Link Direto" icon="🔗" />
            </menu>

            <!-- ⭐ SUB-MENU 2 (Ainda mais profundo!) -->
            <menu label="Exportar Como..." icon="📤">
                <menuitem label="PDF (.pdf)" icon="📕" />
                <menuitem label="Imagem (.png)" icon="🖼️" />
                <menu label="Código Fonte">
                    <menuitem label="JavaScript" icon="🟨" />
                    <menuitem label="Java" icon="☕" />
                </menu>
            </menu>

            <menuitem separator="true" />
            <menuitem label="Apagar Tudo" icon="🗑️" shortcut="Del" />

        </contextmenu>

        <!-- ⭐ 2. LAYOUT VISUAL ⭐ -->

        <!-- Cabeçalho -->
        <div style="background-color: #1e1e1e; padding: 12px 20px; border-bottom: 1px solid #333;">
            <span style="font-weight: bold; font-size: 16px; color: #38bdf8;">SuperUI Code </span>
            <span style="color: #94a3b8; font-size: 14px;"> - Área de Testes</span>
        </div>

        <!-- Área Central (Painel Lateral + Editor) -->
        <div style="display: flex; flex-direction: row; flex-grow: 1;">

            <!-- PAINEL LATERAL -->
            <div class="painel-lateral">
                <span style="font-size: 12px; font-weight: bold; color: #94a3b8; letter-spacing: 1px;">EXPLORADOR</span>

                <!-- ⭐ LISTA DE FICHEIROS (A usar o 'menu-ficheiro') ⭐ -->
                <div class="item-lista" context-menu="menu-ficheiro">📄 main.xpl</div>
                <div class="item-lista" context-menu="menu-ficheiro">📄 index.html</div>
                <div class="item-lista" context-menu="menu-ficheiro">🎨 styles.css</div>
            </div>

            <!-- ÁREA DO EDITOR (A usar o 'menu-editor') -->
            <!-- Repara no flex-grow: 1 -> Agora vai esticar perfeitamente! -->
            <div style="display: flex; flex-direction: column; flex-grow: 1; padding: 15px;" context-menu="menu-editor">

                <tabs tab-active-color="#c678dd" tab-inactive-bg="#282c34" style="flex-grow: 1;">
                    <tab title="main.xpl" active="true">

                        <editor id="codigo-principal"
                                content="{codigoDemo}"
                                syntax="{regrasSintaxe}"
                                line-numbers="true"
                                style="background-color: #282c34; flex-grow: 1;" />

                    </tab>
                </tabs>

            </div>
        </div>

        <!-- O ALVO ONDE VAIS CLICAR -->
        <div context-menu="menu-avancado" style="padding: 50px; background: #333; color: white;">
            Clica com o botão direito para ver o Multi-Menu!
        </div>

        <!-- Rodapé -->
        <div style="background-color: #007acc; padding: 4px 15px; font-size: 12px; font-weight: bold;">
            PRONTO
        </div>

    </body>
</html>
""";

// ============================================================
// 4. BOOT DA ENGINE
// ============================================================
__ui_engine.loadView(htmlView);
__ui_engine.renderCycle();
__ui_engine.showWindow("Teste de Context Menu e Layout W3C", 1100, 700);