import tests.listen.*;



// ============================================================================
// 1. O GUARDA DE NÚMEROS (Limites de Bits)
// ============================================================================
listener Validate {}

implement Validate {
    // 1. Escuta quando alguém faz: new Pessoa() ou nasce uma variável
    @(Listen.Init)
    pub fun aoCriar() {
        println("🔥 Ah! Alguém instanciou ou declarou o nosso objeto: ");
    }

    // 2. Escuta quando alguém faz: pessoa.setNome() ou pessoa.idade
    @(Listen.Get)
    pub fun aoAceder() {
        println("👀 Estão a tentar ler ou aceder ao método/propriedade: ");
    }

    // 3. Escuta quando alguém faz: pessoa.idade = 20
    @(Listen.Set)
    pub fun aoAtualizar() {
    let propriedade ="";
        println("✏️ Alguém está a atualizar '" + propriedade + "' para o novo valor: " + propriedade);

        // Exemplo de Reflexão Madura: Validação dinâmica!
        if (propriedade == "idade" ) {
              println("Erro: A idade não pode ser negativa!");
        }
    }
}

// O Teste Supremo:
&Validate()
declare Pessoa {
    pub nome: string;
    pub idade: int;
}

implement Pessoa {
    pub fun setNome(n: string) {
        this.nome = n;
    }
}

println("--- INICIAR TESTE ---");

// Vai disparar o aoCriar()
&Validate()
var p = new Pessoa();

// Vai disparar o aoAceder("setNome")
p.setNome("Fernão");

// Vai disparar o aoAtualizar("idade", 25)
p.idade = 25;


var email: Email = ""; //ele lanca erro aqui....:

