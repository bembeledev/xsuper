declare Card extends XplElement {}

implement Card {
    pub fun init() {}

    pub fun render(): string {
        // O Template do Card define a estrutura e os Slots!
        return """
            <div class="meu-cartao">
                <header class="cabecalho">
                    <slot name="header">Título Padrão (Caso falhe)</slot>
                </header>

                <main class="corpo">
                    <slot></slot>
                </main>

                <footer class="rodape">
                    <slot name="footer">Sem rodapé.</slot>
                </footer>
            </div>
        """;
    }
}

__ui_engine.loadStyles("""

    .meu-cartao{
        margin:2px;
        padding:4px;
        border: 1px solid #ff43de;
        width: 200px;
    }

    #body{
        display: grid;
        grid-template-columns: 1fr 1fr;
    }

""");

__ui_engine.defineTag("Card", Card);

let html = """
    <html>
        <body >
            <div  id="body">
                <Card>
                    <h1 slot="header">Título do Cartão!</h1>
                    <p>Conteúdo do corpo</p>
                    <div>
                        <p>Mais conteúdo complexo!</p>
                    </div>
                </Card>
                <Card></Card>
            </div>
            <button (click)="adicionar();" >Add Card </button>
        </body>
    </html>
""";

fun adicionar() {
    //println(card.innerHTML);
    let novoCard = document.createElement("Card");
    card.appendChild(novoCard);
    println(document.getHTML());
    __ui_engine.renderCycle();
}

__ui_engine.loadView(html);
__ui_engine.showWindow("Aplicação Desktop Nativa", 800, 600);

var card = document.getElementById("body");

println("=== ÁRVORE COM SLOTS RESOLVIDOS ===");