// ============================================================
// 1. CSS BASE (Padrão W3C suportado pelo teu Adapter)
// ============================================================
__ui_engine.loadStyles("""
    body {
        background-color: #f8fafc;
        color: #1e293b;
        font-family: "Segoe UI", "Helvetica Neue", sans-serif;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;

    }

    /* Cabeçalho da Página - Bloqueado para seleção */
    header {
        background-color: #22c55e; /* Verde Frutado */
        color: #ffffff;
        padding: 30px;
        text-align: center;
        user-select: none; /* ⭐ O utilizador não consegue sublinhar isto! */
        box-shadow: 0px 4px 10px rgba(0,0,0,0.1);
    }

    header h1 {
        margin: 0 0 10px 0;
        font-size: 32px;
    }

    header p {
        margin: 0;
        font-size: 18px;
        opacity: 0.9;
    }

    /* Área Principal com Flexbox */
    main {
        display: flex;
        flex-direction: row;
        flex-grow: 1;
        padding: 30px;
        gap: 30px;
    }

    /* O Artigo Central - Seleção Permitida */
    article {
        flex-grow: 1;
        background-color: #ffffff;
        padding: 40px;
        border-radius: 12px;
        border: 1px solid #e2e8f0;
        box-shadow: 0px 8px 25px rgba(0,0,0,0.05);
        user-select: text; /* ⭐ Permite a cópia de texto naturalmente! */
    }

    article h2 {
        color: #15803d;
        border-bottom: 2px solid #f1f5f9;
        padding-bottom: 10px;
        margin-top: 0;
    }

    article p {
        font-size: 16px;
        line-height: 1.6;
        color: #334155;
        margin-bottom: 20px;
    }

    /* Estilização de Links */
    a {
        color: #2563eb;
        text-decoration: underline;
        cursor: hand;
    }

    a:hover {
        color: #1d4ed8;
        background-color: #eff6ff;
    }

    /* Cartões de Fruta dentro do artigo */
    .fruta-card {
        display: flex;
        flex-direction: row;
        align-items: center;
        gap: 20px;
        background-color: #f8fafc;
        border: 1px solid #cbd5e1;
        border-radius: 8px;
        padding: 20px;
        margin-bottom: 20px;
    }

    .fruta-icone {
        font-size: 45px;
    }

    /* Barra Lateral (Sidebar) - Bloqueada para seleção */
    aside {
        width: 280px;
        background-color: #ffffff;
        padding: 25px;
        border-radius: 12px;
        border: 1px solid #e2e8f0;
        user-select: none; /* ⭐ Protege a navegação contra cliques acidentais */
    }

    .menu-link {
        display: block;
        padding: 10px 15px;
        background-color: #f1f5f9;
        color: #0f172a;
        text-decoration: none;
        border-radius: 6px;
        margin-bottom: 10px;
        font-weight: bold;
    }

    .menu-link:hover {
        background-color: #e2e8f0;
    }
""");

// ============================================================
// 2. FUNÇÕES DE SUPORTE (XPL)
// ============================================================
fun notificarAcao(mensagem:string) {
    println("[AÇÃO] " + mensagem);
}

// ============================================================
// 3. A INTERFACE GRÁFICA (VDOM)
// ============================================================
let htmlView = """
<html>
    <head></head>

    <body context-menu="menu-leitor">
        <!-- ⭐ BANCO DE MENUS ⭐ -->
        <contextmenu id="menu-leitor">
            <menuitem label="Copiar Texto" icon="📋" shortcut="Ctrl+C" onclick="notificarAcao('Cópia solicitada');" />
            <menuitem separator="true" />
            <menuitem label="Adicionar aos Favoritos" icon="⭐" shortcut="Ctrl+D" />
            <menuitem label="Imprimir Artigo..." icon="🖨️" shortcut="Ctrl+P" />
        </contextmenu>

        <!-- ⭐ CABEÇALHO ⭐ -->
        <header>
            <h1>O Maravilhoso Mundo das Frutas</h1>
            <p>Um guia saudável, fresco e cheio de vitaminas.</p>
        </header>

        <!-- ⭐ CONTEÚDO PRINCIPAL ⭐ -->
        <main>

            <!-- ARTIGO (Com seleção ativada e Links) -->
            <article>
                <h2>A Importância das Cores Naturais</h2>
                <p>O consumo regular de frutas é um dos pilares para uma vida saudável. Segundo especialistas, devemos procurar consumir entre 3 a 5 porções diárias. Pode ler o relatório completo no <a href="https://www.who.int/dietphysicalactivity/fruit/en/" onclick="notificarAcao('Navegando para a OMS...');">site da Organização Mundial de Saúde (OMS)</a>.</p>

                <p>Abaixo, destacamos algumas das frutas mais populares e as suas propriedades únicas.</p>

                <!-- Cartão da Maçã -->
                <div class="fruta-card">
                    <div class="fruta-icone">🍎</div>
                    <div style="flex-grow: 1;">
                        <h3 style="margin: 0 0 5px 0; color: #b91c1c;">Maçã</h3>
                        <p style="margin: 0; font-size: 14px;">A maçã é incrivelmente rica em pectina, uma fibra solúvel que auxilia a digestão e protege o sistema cardiovascular. <a href="#maca-receitas">Ver receitas com maçã</a>.</p>
                    </div>
                </div>

                <!-- Cartão da Banana -->
                <div class="fruta-card">
                    <div class="fruta-icone">🍌</div>
                    <div style="flex-grow: 1;">
                        <h3 style="margin: 0 0 5px 0; color: #a16207;">Banana</h3>
                        <p style="margin: 0; font-size: 14px;">A fruta dos desportistas. Carregada de potássio, a banana atua de forma crucial na prevenção de cãibras musculares e na regulação da pressão arterial.</p>
                    </div>
                </div>

                <!-- Cartão do Mirtilo -->
                <div class="fruta-card">
                    <div class="fruta-icone">🫐</div>
                    <div style="flex-grow: 1;">
                        <h3 style="margin: 0 0 5px 0; color: #1d4ed8;">Mirtilo</h3>
                        <p style="margin: 0; font-size: 14px;">Um autêntico superalimento! Os mirtilos são densos em antioxidantes naturais que ajudam na regeneração celular e protegem o cérebro.</p>
                    </div>
                </div>
            </article>

            <!-- BARRA LATERAL (Bloqueada para seleção) -->
            <aside>
                <h3 style="color: #475569; margin-top: 0; border-bottom: 1px solid #cbd5e1; padding-bottom: 10px;">Categorias</h3>

                <a href="#citricas" class="menu-link">🍊 Cítricas</a>
                <a href="#vermelhas" class="menu-link">🍓 Frutas Vermelhas</a>
                <a href="#tropicais" class="menu-link">🍍 Tropicais</a>
                <a href="#secas" class="menu-link">🥜 Frutos Secos</a>

                <div style="margin-top: 30px; padding: 15px; background-color: #f8fafc; border: 1px dashed #94a3b8; border-radius: 8px;">
                    <span style="font-size: 24px; display: block; margin-bottom: 10px;">💡</span>
                    <p style="font-size: 12px; color: #64748b; margin: 0; line-height: 1.4;">
                        <strong>Nota do Motor:</strong> Tente selecionar o texto deste painel lateral. O <i>user-select: none</i> impede a seleção, forçando o comportamento nativo de interfaces de aplicações!
                    </p>
                </div>
            </aside>

        </main>
    </body>
</html>
""";

// ============================================================
// 4. INICIALIZAÇÃO DA SUPERUI
// ============================================================
__ui_engine.loadView(htmlView);
__ui_engine.showWindow("Artigo Interativo - Mundo das Frutas", 1100, 750);