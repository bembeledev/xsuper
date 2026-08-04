module tests.meta_complete_test;

declare Robot {
    pub nome: string;
    pub energia: int;
    pub status: string;
}

implement Robot {
    pub fun init(n: string) {
        this.nome = n;
        this.energia = 50;
        this.status = "OFF";
    }
}

var bot = new Robot("T-800");

println("=== INICIANDO TESTE FINAL DE STRESS DO MOTOR JIT ===", "#00FFFF");

// ==========================================================
// 1. TESTE DA API FLUIDA (IF / SWITCH)
// ==========================================================
var checkEnergia = If(() => { return bot.energia < 30; }, () => { bot.status = "CRITICO"; })
                  .ElseIf(() => { return bot.energia < 70; }, () => { bot.status = "OK"; })
                  .Else(() => { bot.status = "FULL"; });

checkEnergia.execute();
println("Status pós-verificação: " + bot.status); // Deve ser OK

// Simulação de switch
var comando = "reparar";

// Teste de Switch
var seletor =
     Switch(comando)
    .Case(["reparar", "fix"], () => { bot.energia += 100; println("Energia:" +bot.energia ); }, CONTROL.BREAK)
    .Case("desligar", () => { bot.status = "OFF"; }, CONTROL.BREAK)
    .Default(() => { println("Comando desconhecido!"); });


seletor.execute();

// ==========================================================
// 2. TESTE DE JIT INJECTION (FÁBRICA FUNCIONAL)
// ==========================================================
// Vamos injetar um novo método 'consumirEnergia' dinamicamente
var paramQtd = Param("qtd", TYPES.INT);

// ⭐ A MÁGICA: Declaramos o (qtd: int) para o Linter analisar e validar a matemática!
// O JIT vai engolir esta função, ignorar o parâmetro do wrapper, e extrair apenas o bloco!
var logicaConsumo = (qtd: int) => {
    println("Consumindo " + qtd + " de energia...");
    this.energia = this.energia - qtd;
};

// Composição: Criamos a função e injetamos no modelo Robot
var metodoJit = Func("consumirEnergia", VISIBILITY.PUB, [paramQtd], TYPES.VOID, logicaConsumo);
bot::injectMethod(metodoJit);

// Executamos o método injetado
bot.consumirEnergia(20);
println("Energia atual: " + bot.energia); // Deve ser 30 (50 - 20)

// ==========================================================
// 3. TESTE DE LOOPS DINÂMICOS (FOR)
// ==========================================================
// ⭐ Usamos o mesmo truque: Tipamos o 'i' para a AST ficar estruturalmente válida!
var corpoLoop = (i: int) => { println("Tick: " + i); };

var loop = For(
    () => { let i = 0; },           // Init (Linter aprova porque estamos a declarar o 'i')
    (i: int) => { return i < 3; },  // Cond (Linter aprova porque recebe 'i' como int)
    (i: int) => { i++; },           // Inc  (Linter aprova o incremento matemático!)
    corpoLoop                       // Corpo
);

loop.execute(); // O JIT junta as peças num Stmt.ForCStyle perfeito e roda!

println("=== TESTE FINALIZADO COM SUCESSO! ===", "#00FF00");