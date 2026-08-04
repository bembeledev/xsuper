println(">>> A INICIAR TESTE DE CLASSES ANÓNIMAS E POLIMORFISMO <<<", "#00FFFF");

// 1. O nosso Contrato
interface CRUD {
    pub fun add(nome: string);
    pub fun delete(id: int);
    //pub fun getIda(): int;
    pub fun getId(): int;
}

// 2. O nosso Modelo Base
declare Pessoa {
    pub nome: string;
    pub id: int;
}

implement Pessoa {
    pub fun init(nome: string, id: int) {
        this.nome = nome;
        this.id = id;
    }
}

// =======================================================================
// ⭐ 3. A TUA FUNÇÃO MÁGICA: Retorna uma Classe Anónima on-the-fly!
// =======================================================================
fun criarGestorCrud(alvo: Pessoa): CRUD {
    println("A gerar contrato anónimo para: " + alvo.nome, "#FFFF00");

    // A classe nasce aqui e assina o contrato CRUD instantaneamente!
    return new CRUD() {
        @Override
        pub fun add(nome: string) {
            // Nota: O alvo.nome é capturado magicamente pela Closure do motor!
            println("[Anónimo] A guardar o item '" + nome + "' para a pessoa: " + alvo.nome);
        }

        @Override
        pub fun delete(id: int) {
            println("[Anónimo] A apagar registo " + id + " do sistema.");
        }

        @Override
        pub fun getId(): int {
            return alvo.id;
        }
    };
}

// =======================================================================
// 4. TESTE DE EXECUÇÃO
// =======================================================================

var programador = new Pessoa("Fernando", 777);

// A variável está tipada estritamente como 'CRUD', e recebe a classe anónima!
var meuGestor: CRUD = criarGestorCrud(programador);

println("\n--- A testar métodos da Classe Anónima ---", "#00FF00");
meuGestor.add("Projeto XPL");
meuGestor.delete(404);
println("O ID retornado foi: " + meuGestor.getId());


// =======================================================================
// 5. TESTE DE INJEÇÃO DE DEPENDÊNCIAS (POLIMORFISMO)
// =======================================================================
println("\n--- A testar Polimorfismo Real ---", "#00FF00");

// Esta função não sabe o que é uma Pessoa ou uma Classe Anónima. Só exige um CRUD!
fun executarTarefa(worker: CRUD) {
    worker.add("Tarefa Injetada Automática");
}

// Passamos o nosso gestor anónimo, e a Alfândega deixa passar!
executarTarefa(meuGestor);

println("\n>>> TESTE CONCLUÍDO COM SUCESSO <<<", "#00FFFF");