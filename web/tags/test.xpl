declare Perfil extends XplElement {
    priv pessoa: object;
}
implement Perfil {
    pub fun init(props: object) {
        this.pessoa = props.pessoa;
    }
    pub fun render(): string {
        return "<div>Nome: " + this.pessoa.nome + ", Idade: " + this.pessoa.idade + "</div>";
    }
}

declare Avatar extends XplElement {
    pub imagemUrl: string;
    pub mensagem: string;
}
implement Avatar {
    pub fun init() {
        this.mensagem = "Bem-vindo ao sistema!";
    }

    pub fun render(): string {
        // ⭐ PERFEIÇÃO: Chamamos apenas getProps(), tal como no Angular ou Vue!
        // E passamos a variável mágica $event que o nosso Java injetou!
        return "<img id='img' (click)='getProps(event);processarClique(event, \"Admin\", 42, mensagem);' src='" + this.imagemUrl + "' />";
    }

    // Recebe o evento e usa as suas propriedades
    pub fun getProps(event: XplEvent){
        println("A imagem " + this.imagemUrl + " foi clicada!");
    }

    // ⭐ ASSINATURA FORTE: O XPL garante que todos os tipos batem certo!
        pub fun processarClique(ev: XplEvent, nivelAcesso: string, codigo: number, msg: string) {
            println("=== MÚLTIPLOS PARÂMETROS RECEBIDOS ===");
            println("1. Evento do Tipo: " + ev.getType());
            println("2. Nível de Acesso: " + nivelAcesso);
            println("3. Código Secreto: " + codigo);
            println("4. Mensagem da Classe: " + msg);
        }
}

let utilizador = { "nome": "Fernando", "idade": 25 };

let html = """
    <html>
        <body>
            <Perfil pessoa="{{utilizador}}"></Perfil>
            <Avatar imagemUrl="foto.png"></Avatar>
        </body>
    </html>
""";

__ui_engine.defineTag("Perfil", Perfil);
__ui_engine.defineTag("Avatar", Avatar);
__ui_engine.loadView(html);
println(document.getInnerHTML());
__ui_engine.renderCycle();
    // BAM! A janela aparece aqui.
    // O JavaFX arranca, o renderizador é injetado, e a vista já mastigada é pintada.
__ui_engine.showWindow("Aplicação Desktop Nativa", 800, 600);

// Dispara o clique no Avatar para testar!
var act = document.getElementById("img").toObject();
act.callAction("click");