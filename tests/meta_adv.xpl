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

var logicaConsumo = () => {
    println("Consumindo " + qtd + " de energia...");
    this.energia = this.energia - qtd;
};

// Composição: Criamos a função e injetamos no modelo Robot
var metodoJit = Func("consumirEnergia", VISIBILITY.PUB, [paramQtd], TYPES.VOID, logicaConsumo);
bot::injectMethod(metodoJit);

// Executamos o método injetado
bot.consumirEnergia(20);
println("Energia atual: " + bot.energia); // Deve ser 80

// ==========================================================
// 3. TESTE DE LOOPS DINÂMICOS (FOR)
// ==========================================================
var corpoLoop = () => { println("Tick: " + i); };


println(typeof(corpoLoop));

var loop = For(
    () => { let i = 0; },     // Init
    () => { return i < 3; },  // Cond
    () => { i++; },           // Inc
    corpoLoop                 // Corpo
);

loop.execute();

println("=== TESTE FINALIZADO COM SUCESSO! ===", "#00FF00");