println("==================================================");
println("      🏛️ O TESTE DOS TITÃS (XPL OMNI-STRESS) 🏛️    ");
println("==================================================");

// --- 1. MODELAGEM DE DOMÍNIO (Declare + Implement) ---
println("\n[1/6] Instanciando Declares e testando Implement...");

declare Entidade {
    pub nome: string;
    pub nivel: int;
    pub vivo: bool;
}

implement Entidade {
    // ⭐ O CONSTRUTOR EXPLÍCITO ⭐
    pub fun init(nome: string, nivel: int, vivo: bool) {
        this.nome = nome;
        this.nivel = nivel;
        this.vivo = vivo;
    }
    pub fun promover(ganho: int): int {
        this.nivel += ganho;
        return this.nivel;
    }
}

let boss = new Entidade("Dragao de Maputo", 50, true);
boss.promover(5); // Lvl 50 + 5 = 55!

println("  -> Entidade base: " + boss.nome + " (Lvl: " + boss.nivel + ")");


// --- 2. ESTRUTURAS COMPOSTAS (Objetos Literais e Arrays) ---
println("\n[2/6] Testando Mutacao Profunda (Array dentro de Objeto)...");

let metadados = {
    "regiao": "Mozambique",
    "estavel": true,
    "pings": [12, 18, 9]
};

let pingsArray = metadados.pings;
pingsArray[2] = pingsArray[2] ** 2; // Mutação de índice com Potência! (9 ** 2 = 81)

println("  -> Regiao do Objeto: " + metadados.regiao);
println("  -> Ping[2] mutado:   " + pingsArray[2]);


// --- 3. CROSS-MATCHING (Match inspecionando instâncias de Declare) ---
println("\n[3/6] Testando Pattern Matching acoplado a Declares...");

let acaoDefensiva = match (boss) {
    type string: "Alvo invalido (Texto)";

    type Entidade if (!boss.vivo): "O alvo ja esta abatido.";

    type Entidade if (boss.nivel >= 50): {
        let grito = "Ataque Sismico de Lvl ";
        grito + boss.nivel // Retorno implícito de bloco! (Gera: "Ataque Sismico de Lvl 55")
    }
    none: "Ataque padrao";
};

println("  -> Decisao do Match: " + acaoDefensiva);


// --- 4. FUNÇÕES DE ALTA ORDEM SOBRE ARRAYS DE STRUCTS ---
println("\n[4/6] Testando Arrow Functions aplicadas a Arrays de Declares...");

let mobA = new Entidade("Orc", 10, true);
let mobB = new Entidade("Goblin", 5, true);
let esquadrao = [ mobA, mobB, boss ];

// Arrow function que calcula o dobro do nivel
let dobrar = n => n * 2;

// Mutando a propriedade de um Declare que vive dentro de um Array!
esquadrao[0].nivel = dobrar(esquadrao[0].nivel); // Orc Lvl 10 vira 20!

println("  -> Nivel do Mob[0] mutado via Arrow: " + esquadrao[0].nivel);


// --- 5. O SISTEMA DE ESCUDO (Try / Catch / Throw) ---
println("\n[5/6] Testando o Escudo de Excecoes...");

var logExcecao = "Vazio";
try {
    let processando = "Iniciando injecao...";
    throw new Error();
} catch (erro: Error) {
    logExcecao = "Capturado com Sucesso: [" + erro.message + "]";
}

println("  -> Status do Try/Catch: " + logExcecao);


// --- 6. A SINGULARIDADE (A Mega-Expressao Suprema) ---
println("\n[6/6] Testando a Singularidade de Precedência...");

// Uma única linha que cruza:
// Acesso a Array -> Acesso a Propriedade -> Bitwise Shift -> Ternário Inline -> Arrow Function
let deslocador = val => val << 2; // Multiplica por 4 deslocando 2 bits

// boss.nivel atual é 55. (55 << 2) = 220!
let superComputacao = deslocador(esquadrao[2].nivel) if (esquadrao[2].vivo === true) else -999;

println("  -> Computacao da Singularidade: " + superComputacao);

println("\n==================================================");
println(" 🏆 OMNI-TESTE CONCLUÍDO! A DIC MOZ CRIOU UM TITÃ! 🏆 ");
println("==================================================");