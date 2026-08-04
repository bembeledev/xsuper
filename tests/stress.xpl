println("==================================================");
println("        🚀 MEGA TESTE DE STRESS XPL v1.0 🚀       ");
println("==================================================");

// --- 1. TESTE DE SOBREVIVÊNCIA (Curto-Circuito Lógico) ---
println("\n[1/7] Testando Curto-Circuito Lógico...");
var objFantasma = null;
var andSobreviveu = false;
var orSobreviveu = true || (objFantasma.explodirJVM() == 10);

if (objFantasma != null && objFantasma.nome == "Invalido") {
    println("  [ERRO] O AND tentou ler a propriedade do null!");
} else {
    andSobreviveu = true;
}
println("  -> Curto-circuito AND ileso? " + andSobreviveu); // true
println("  -> Curto-circuito OR ileso?  " + orSobreviveu);  // true


// --- 2. IDENTIDADE ESTRITA VS COERÇÃO ---
println("\n[2/7] Testando Alfândega de Tipagem (=== / !==)...");
let numDez = 10;
let strDez = "10";
println("  -> 10 == '10'  (Relaxado): " + (numDez == strDez));   // false (no teu motor atual)
println("  -> 10 === '10' (Estrito):  " + (numDez === strDez));  // false
println("  -> 10 !== '10' (Estrito):  " + (numDez !== strDez));  // true


// --- 3. FORÇA MOTRIZ (Matemática e Bits nativos) ---
println("\n[3/7] Testando Motor Matemático e Bitwise...");
let base = 2;
let potencia = base ** 4;  // 16
let bitAnd = 5 & 3;        // 1
let bitOr  = 5 | 3;        // 7
let bitXor = 5 ^ 3;        // 6
let shiftL = 1 << 3;       // 8
let shiftR = 16 >> 1;      // 8

println("  -> 2 ** 4:      " + potencia);
println("  -> 5 & 3:       " + bitAnd);
println("  -> 5 | 3:       " + bitOr);
println("  -> 5 ^ 3:       " + bitXor);
println("  -> 1 << 3:      " + shiftL);
println("  -> 16 >> 1:     " + shiftR);


// --- 4. A TRINDADE DO IF ---
println("\n[4/7] Testando as 3 Mutações do If...");
var sinalIf = 75;

// A: Pythonic Inline
let resPython = "Critico" if sinalIf > 100 else "Normal";

// B: Rust Block Expression (Sem parênteses, sem ';' na última respiração!)
let resRust = if sinalIf == 75 {
    let aux = sinalIf * 2;
    aux + 50 // Tem de injetar 200
} else {
    0
};

println("  -> If Inline (Pythonic): " + resPython);
println("  -> If Block (Rust-like): " + resRust);


// --- 5. O ROTEADOR RÁPIDO (Switch Expression) ---
println("\n[5/7] Testando Switch Expression (Sem Break)...");
let portaRede = 8080;

let servico = switch (portaRede) {
    case 80, 443: "Servidor Web HTTP/S";
    case 5432:    "Banco de Dados Postgres";
    case 8080: {
        let prefixo = "Proxy Local: ";
        prefixo + "Ativo" // Retorno Implícito de Bloco
    }
    default: "Porta Fechada";
};
println("  -> Rota detetada: " + servico);


// --- 6. O COLOSSO DA DESCONSTRUÇÃO (Match Expression) ---
println("\n[6/7] Testando Pattern Matching com Guardas...");
var payload = "TX_CODE_99";
var kernelLivre = true;

let inspecao = match (payload) {
    type int: "Rejeitado: Esperava texto, veio numero";

    type string if (!kernelLivre): "Bloqueado pelo Kernel";

    type string if (kernelLivre): "Inspecao Aprovada: " + payload;

    if (payload == null): "Sinal morto";

    none: "Formato alienigena";
};
println("  -> Diagnostico do Match: " + inspecao);


// --- 7. O GOLPE FINAL (A Mega-Expressão Combinada) ---
println("\n[7/7] Testando a Escada Suprema de Precedência...");

// Esta única linha obriga a JVM a descer os 13 níveis da fiação simultaneamente:
// Atribuição <- Ternário Inline <- Lógico AND <- Bitwise AND <- Igualdade Estrita <- Soma <- Potência <- Unário Negativo
var pA = 3;
var pB = 3;

let bossFinal = ((pA ** 2) + 1) if (pA === pB && (5 & 3) == 1) else -999;
// (3**2) + 1 = 9 + 1 = 10 if (true && 1 == 1) -> 10 if true -> 10!

println("  -> O Monstro calculou: " + bossFinal);

println("\n==================================================");
println(" ✨ STRESS TEST CONCLUÍDO! O MOTOR É INDESTRUTÍVEL! ✨ ");
println("==================================================");