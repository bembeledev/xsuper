println("==================================================");
println("     🛡️ MEGA TESTE DE STRESS: VOLUME 21 (V21) 🛡️   ");
println("        Opcionais, Coalescência e Encadeamento    ");
println("==================================================");

// --- 1. DECLARAÇÃO COM TIPOS OPCIONAIS E COALESCÊNCIA (??) ---
println("\n[1/6] Testando Tipos Opcionais (?tipo) e Coalescência (??)...");

var textNulo: ?string = null;
var textValido: ?string = "SuperXPL";
var numeroOpcional: ?int = null;

let fallbackAtivado = textNulo ?? "Anônimo";
let fallbackIgnorado = textValido ?? "NaoDeveriaAparecer";
let calculoFallback = (numeroOpcional ?? 50) * 2; // (null ?? 50) * 2 = 100!

println("  -> Fallback (nulo ?? val):     " + fallbackAtivado);
println("  -> Fallback (val ?? fallback): " + fallbackIgnorado);
println("  -> Matemática com Fallback:    " + calculoFallback);


// --- 2. ENCADEAMENTO OPCIONAL EM OBJETOS LITERAIS (?.) ---
println("\n[2/6] Testando Encadeamento Opcional Seguro (?.)...");

let servidorOnline = {
    "status": "ATIVO",
    "sessao": {
        "usuario": "Arquiteto",
        "permissoes": [1, 7, 8]
    }
};

var servidorOffline: ?object = null;

// Acesso seguro profundo em cascata:
let userOnline  = servidorOnline?.sessao?.usuario;
let userOffline = servidorOffline?.sessao?.usuario; // Não pode crashar! Devolve null silenciosamente.
let comboChain  = servidorOffline?.sessao?.usuario ?? "Sessao Expirada";

println("  -> Usuario Online:       " + userOnline);
println("  -> Usuario Offline:      " + userOffline);
println("  -> Chain + Coalescência: " + comboChain);


// --- 3. MODELAGEM DE DOMÍNIO COM OPCIONAIS (Declare + Implement) ---
println("\n[3/6] Testando Declares com Atributos Opcionais e Chamadas Seguras ?.()...");

declare Conexao {
    pub host: string;
    pub porta: ?int; // Campo estritamente opcional!
}

implement Conexao {
    pub fun init(host:string, porta:int){
        this.host = host;
        this.porta = porta;
    }

    pub fun ping(): string {
        return "Pong do host: " + this.host;
    }
}

var dbPrimario: ?Conexao = new Conexao("localhost", 5432);
var dbReplicacao: ?Conexao = null;

// Testando a delegação da chamada opcional de método ?.()
let pingPrimario = dbPrimario?.ping();
let pingReplica  = dbReplicacao?.ping() ?? "Replicacao Inativa";

let portaAtiva   = dbPrimario?.porta ?? 8080;
let portaInativa = dbReplicacao?.porta ?? 9999;

println("  -> Ping Primario:  " + pingPrimario);
println("  -> Ping Replica:   " + pingReplica);
println("  -> Porta Primaria: " + portaAtiva);
println("  -> Porta Replica:  " + portaInativa);


// --- 4. O DESFIBRILADOR (Unwrap Forçado !) + ESCUDO TRY/CATCH ---
println("\n[4/6] Testando Unwrap Forçado (!) e Interceptação de Exceção...");

var payloadCritico: ?string = "Sinal Estavel";
var payloadCorrompido: ?string = null;

let unwrapSeguro = payloadCritico!; // Tem de extrair a string limpa
println("  -> Unwrap Seguro (!): " + unwrapSeguro);

var logUnwrap = "Falha ao capturar";
try {
    println("  [Try] Tentando abrir opcional vazio com '!'...");

    // O crime: Forçar a abertura de um null. Dispara RuntimeError do V21!
    let explosao = payloadCorrompido!;

    println("  [ERRO] O unwrap forçado ignorou o null!");
} catch (erro: Error) {
    // ⭐ Capturado limpo pela nossa ponte nativa de erros!
    logUnwrap = "Excecao Capturada: [" + erro.message + "]";
}

println("  -> Resultado do Escudo: " + logUnwrap);


// --- 5. CURTO-CIRCUITO DE COALESCÊNCIA (Avaliação Preguiçosa) ---
println("\n[5/6] Testando Curto-Circuito no Operador ??...");

// Regra de Ouro: Se a esquerda não é nula, o lado direito NUNCA pode ser avaliado!
var statusMotor = "Motor Ileso";
let testeCurtoCircuito = "Esquerda Viva" ?? (payloadCorrompido!);

println("  -> Lado direito ignorado? " + testeCurtoCircuito);


// --- 6. A SINGULARIDADE QUÂNTICA (A Mega-Expressão Suprema do V21) ---
println("\n[6/6] Testando a Singularidade Quântica de Precedência...");

// Cruzando na mesma linha: Encadeamento Opcional -> Chamada Opcional -> Coalescência -> Potência -> Ternário Inline
var sistemaAlvo: ?object = {
    "nucleo": new Conexao("maputo.dicmoz.com", 2)
};

// Se o ping responder, calcula: ((porta ?? 10) ** 4). Como porta é 2 -> (2 ** 4) = 16!
let singularidade = ((sistemaAlvo?.nucleo?.porta ?? 10) ** 4) if (sistemaAlvo?.nucleo?.ping() !== null) else -1;

println("  -> Cálculo da Singularidade: " + singularidade);

println("\n==================================================");
println(" 🏆 TESTE V21 CONCLUÍDO! O MOTOR QUÂNTICO ESTÁ VIVO! 🏆 ");
println("==================================================");