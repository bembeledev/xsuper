module files.listeners;

listener WebFilter {}

abstract  implement WebFilter {

    @(Listen.Set)
    pub fun interceptarRequisicao() {
        // Ocultamos a lógica até a propriedade 'http' ser injetada no Controller
        if (this.property == "http") {

            let httpObj = this.value; // O objeto Http real que está a entrar
            let req = httpObj.req.request; // A requisição bruta
            let res = httpObj.res.response; // A resposta bruta

            // 1. Log Elegante (Estilo Spring Boot Logger)
            println(" ");
            println("==================================================");
            println("🟢 [WebFilter] Nova Requisição Intercetada!");
            println("📍 Rota:    " + req.path);
            println("🛠️ Método:  " + req.method);

            // ⭐ MAGIA DA REFLEXÃO MOP: Descobre dinamicamente qual Controller atendeu!
            println("🎯 Destino: " + this.target.getName());
            println("==================================================");

            // 2. Podemos injetar Cabeçalhos de Segurança Dinamicamente em TODOS os controllers!
            res.headers["X-Powered-By"] = "Xsuper MOP Engine";
            res.headers["X-Frame-Options"] = "SAMEORIGIN";

            // 3. (Futuro) Aqui poderias fazer validações de Autenticação (Tokens, Sessões)
            // Se falhasse, podias fazer throw Error("Não Autorizado");
        }
    }
}

export WebFilter;