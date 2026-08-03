println(">>> A INICIAR MOTOR DE DADOS <<<", "#00FFFF");

declare Animal {
    pub nome: string;
    pub id: int;
}

abstract implement Animal {
    // Construtor Base: Todos os animais precisam de nome e id ao nascer!
    pub fun init(nome: string, id: int) {
        this.nome = nome;
        this.id = id;
    }

    pub fun verDados() {
        println("Animal: " + this.nome + " | ID: " + this.id);
    }
}

declare Mamifero extends Animal {
    pub localizacao: string;
}

implement Mamifero {
    // Sobrescrita do Construtor (Polimorfismo de Inicialização!)
    pub fun init(nome: string, id: int, local: string) {
        // Como o 'super' ainda está a caminho, populamos diretamente a memória herdada
        this.nome = nome;
        this.id = id;
        this.localizacao = local;
    }

    pub fun rugir() {
        this.verDados();
        println(this.nome + " rugiu forte na " + this.localizacao + "!");
    }
}

println("--- TESTE DE INICIALIZAÇÃO AUTOMÁTICA ---", "#FFFF00");

//var animal = new Animal("Eu",23);

// ⭐ O construtor entra em ação aqui enviando os 3 argumentos diretamente!
var leao = new Mamifero("Leão Africano", 42, "Savana");

// Executa os métodos e prova a consistência do estado interno do objeto
leao.verDados();
leao.rugir();

println(">> Construtores validados com sucesso absoluto!", "#00FF00");