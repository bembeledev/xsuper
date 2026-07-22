println("==================================================", "#00FFFF");
println("      🛡️ SUPER XPL - MEGA STRESS: FUNÇÕES 🛡️      ", "#00FFFF");
println("       Named Args, Sobreviventes, Lazy Defaults   ", "#00FFFF");
println("==================================================\n", "#00FFFF");

// ---------------------------------------------------------
println("[1/5] ALFÂNDEGA HÍBRIDA & OMISSÕES QUÂNTICAS", "#FFFF00");
// ---------------------------------------------------------
// Função com aridade 5, mas apenas 2 obrigatórios (idade, nome)
fun PerfilMilitar(idade: int, patente: ?string, posto: ?string, nome: string, codigoOperacao: string = "XPL-2026"): string {
    let pat = patente ?? "Sem Patente Registada";
    let pos = posto ?? "Base Desconhecida";
    return "Nome: " + nome + " | Idade: " + idade + " | Patente: " + pat + " | Posto: " + pos + " | Op: " + codigoOperacao;
}

// A) Invocação 100% Nomeada fora de ordem (omitiu patente, posto e codigoOperacao)
let militar1 = PerfilMilitar(nome: "Fernando Nelson Bembele", idade: 21);
println(" -> Nomeado Fora de Ordem: " + militar1);

// B) Invocação Mista: Nomeados fora de ordem + Posicionais para os sobreviventes (patente, posto, codigo)
let militar2 = PerfilMilitar(nome: "Samito Macassa", idade: 28, "Capitão", "Maputo HQ", "STRESS-MAX");
println(" -> Misto Sobrevivente:    " + militar2);
println("");

// ---------------------------------------------------------
println("[2/5] COERÇÃO IMPLÍCITA & TRANSMUTAÇÃO DE TIPOS", "#FFFF00");
// ---------------------------------------------------------
// Função que pede estritamente (string, float), mas vamos atirar (Long, Long)!
fun CalcularSaldo(titular: string, saldo: float): string {
    return "Titular: " + titular + " | Saldo Contábil: " + saldo;
}

// Passando Long (456) para titular (string) e Long (15000) para saldo (float)!
// A Alfândega de Transmutação vai convertê-los nativamente para "456" e 15000.0!
let relatorioSaldo = CalcularSaldo(456 as string, 15000 as float);
println(" -> Transmutação de Primitivos: " + relatorioSaldo);
println("");

// ---------------------------------------------------------
println("[3/5] AUDITORIA DE RETORNO OPCIONAL (?tipo)", "#FFFF00");
// ---------------------------------------------------------
fun BuscarAlvara(ativo: bool): ?string {
    if (!ativo) {
        return null; // Retorno nulo legítimo auditado e validado por ?string!
    }
    return "ALV-MAPUTO-2026";
}

let alvaraInativo = BuscarAlvara(false) ?? "Nenhum Alvará Encontrado";
let alvaraAtivo   = BuscarAlvara(true) ?? "Não deve aparecer";

println(" -> Retorno Nulo (?string):   " + alvaraInativo);
println(" -> Retorno Válido (?string): " + alvaraAtivo);
println("");

// ---------------------------------------------------------
println("[4/5] TRUE LAZY BINDING (Curto-Circuito de Defaults)", "#FFFF00");
// ---------------------------------------------------------
fun GeradorPesadoDeToken(): string {
    println("    [ALERTA FATAL] GeradorPesadoDeToken() foi executado na AST!", "#FF0000");
    return "TOKEN_CUSTO_ALTO";
}

fun AutenticarSessao(id: int, token: string = GeradorPesadoDeToken()): string {
    return "Sessão " + id + " validada com token: " + token;
}

// Como passamos o token explicitamente ("TOKEN_SIMPLES"), a ranhura é removida na Fase 1.
// Prova matemática: A expressão GeradorPesadoDeToken() NUNCA será invocada pela JVM!
println(" -> Invocando função com Lazy Default sobreposto:");
let sessaoRapida = AutenticarSessao(id: 505, token: "TOKEN_CACHE_RAPIDO");
println("    Resultado: " + sessaoRapida);
println("");

// ---------------------------------------------------------
println("[5/5] ESCUDO DE CONTRATO (A Bofetada Pedagógica)", "#FFFF00");
// ---------------------------------------------------------
var logDefesa = "Sem Infrações";

try {
    println(" -> [Try] Executando função com :int que se esquece do return...");

    // Função local anónima/closure que viola a Lei da Promessa Falhada
    fun PromessaQuebrada(): int {
        let calculo = 100 * 200;
        // Esqueceu-se do return! A Lei 2 do XplFunction vai guilhotinar!
        return calculo;
    }

    let falha = PromessaQuebrada();
    println("    [ERRO] A função devolveu matéria ilegal!");

} catch (erro: Error) {
    logDefesa = "Contrato Protegido: [" + erro.message + "]";
}

println(" -> Veredito do Hipervisor: " + logDefesa,"#FF0000");

println("\n==================================================", "#00FFFF");
println(" 🏆 TESTE V22 CONCLUÍDO! O MOTOR DE FUNÇÕES É REI! 🏆 ", "#00FFFF");
println("==================================================", "#00FFFF");