println(">>> A INICIAR MOTOR DE DADOS <<<", "#00FFFF");

// ==========================================
// 1. O CONTRATO (A Lei)
// ==========================================
interface CRUD {
    pub fun add(nome: string): int;
    pub fun delete(id: int): string;
    pub fun getId(); // Vazio (void)
}

// ==========================================
// 2. A MEMÓRIA (O Molde Genético Puro)
// ==========================================
declare Animal {
    pub nome: string;
    pub especie: string;
    pub id: int; // Mudei para 'pub' para facilitar o teste de execução
}

// ==========================================
// 3. A ALMA (O Comportamento Base e Abstracto)
// ==========================================
abstract implement Animal {

     pub fun init(nome: string, id: int) {
       this.nome = nome;
       this.id = id;
     }

    // Método concreto herdado por todos os filhos
    pub fun verIdade(): int {
        println("Tenho " + this.id + " anos");
        return this.id;
    }

    // Regra Implacável: Todos os filhos TÊM de saber acasalar devolvendo uma string
    pub abstract fun acasalamento(): string;
}

// ==========================================
// 4. A VARIANTE DIRECTA (Cumpre a Alma)
// ==========================================
abstract implement Animal as Fish {
    // Sobrescreve o método (Opcional, mas permitido na POO)
    pub fun verIdade(): int {
        return this.id * 2; // Peixes envelhecem diferente!
    }

    // ⭐ OBRIGATÓRIO: Cumpre a regra abstracta do Pai
    pub fun acasalamento(): string {
        return "O peixe liberta ovos na água.";
    }
}

// ==========================================
// 5. A HERANÇA DE MEMÓRIA
// ==========================================
declare Mamifero extends Animal {
    pub localizacao: string;
}

implement Mamifero {
    pub fun init(nome: string, id: int, local: string) {
        super.init(nome, id); // O pai trata da genética base!
        this.localizacao = local; // O filho foca-se na evolução!
    }
}


// ==========================================
// 6. O CAMARADA COMPLETO (Cumpre a Alma e a Lei)
// ==========================================
implement Mamifero as Mam1 for CRUD {

    // ⭐ OBRIGATÓRIO 1: Cumpre a regra abstracta herdada do Animal
    @Override
    pub fun acasalamento(): string {
        return "O mamífero reproduz-se de forma vivípara na " + this.localizacao;
    }

    // ⭐ OBRIGATÓRIO 2: Cumpre a interface CRUD à risca (Tipos e Parâmetros)
    @Override
    pub fun add(nome: string): int {
        this.nome = nome;
        println("A guardar o animal [" + this.nome + "] na Base de Dados A...");
        return 200; // Retorna int!
    }

    @Override
    pub fun delete(id: int): string {
        println("A eliminar " + this.nome + " com o id: " + id);
        return "Sucesso na eliminação"; // Retorna string!
    }

    @Override
    pub fun getId() { // Não tem retorno (void), exactamente como a interface
        println("O ID actual é: " + this.id);
    }
}

// ==========================================
// 7. O NASCIMENTO E EXECUÇÃO
// ==========================================
println("--- TESTE DE EXECUÇÃO ---", "#FFFF00");

//var erro = new Fish(); // A GUILHOTINA IA CORTAR ISTO! (Base abstracta)
//println(erro);

var leao = new Mam1("",12);
leao.id = 5;
leao.localizacao = "Savana Africana";

// Executa métodos do Contrato CRUD
leao.add("Leão Rei");
leao.getId();

// Executa método exigido pela Abstracção do Animal
var reprod = leao.acasalamento();
println(reprod);

// Executa método herdado da Implementação Base do Animal
leao.verIdade();

var mam = new Mamifero("Tacto",21,"Oil");
mam.verIdade();

println(">> Execução concluída com sucesso da Arquitectura XPL!", "#00FF00");
