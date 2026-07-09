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

__ui_engine.defineTag("Card", Card);

let html = """
    <html>
        <body>
            <Card>
                <h1 slot="header">Título do Cartão!</h1>
                <p>Conteúdo do corpo</p>
                <div>
                    <p>Mais conteúdo complexo!</p>
                </div>
            </Card>
            <Card></Card>
        </body>
    </html>
""";

__ui_engine.loadView(html);

println("=== ÁRVORE COM SLOTS RESOLVIDOS ===");
println(document.getHTML());