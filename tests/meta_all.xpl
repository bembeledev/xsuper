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

println("=================================================", "#00FFFF");
println(" 🚀 INICIANDO TESTE UNIVERSAL DO MOTOR JIT XPL", "#00FFFF");
println("=================================================", "#00FFFF");

// ==========================================================
// 1. TESTE: IF / ELSE IF / ELSE
// ==========================================================
println("\n[1] A testar If Fluido...", "#FFFF00");
var checkEnergia = If(() => { return bot.energia < 30; }, () => { bot.status = "CRITICO"; })
                  .ElseIf(() => { return bot.energia < 70; }, () => { bot.status = "OK"; })
                  .Else(() => { bot.status = "FULL"; });

checkEnergia.execute();
println("    -> Status do Bot: " + bot.status); // Deve ser OK


// ==========================================================
// 2. TESTE: SWITCH COM CONTROL.BREAK
// ==========================================================
println("\n[2] A testar Switch dinâmico...", "#FFFF00");
var comando = "reparar";
Switch(comando)
    .Case(["reparar", "fix"], () => { bot.energia = 100; }, CONTROL.BREAK)
    .Case("desligar", () => { bot.status = "OFF"; }, CONTROL.BREAK)
    .Default(() => { println("Comando desconhecido!"); })
    .execute(); // O .execute() encadeado é elegância pura!

println("    -> Energia após reparação: " + bot.energia); // Deve ser 100


// ==========================================================
// 3. TESTE: WHILE LOOP
// ==========================================================
println("\n[3] A testar While Loop JIT...", "#FFFF00");
var w = 0;
While(() => { return w < 3; }, () => {
    println("    -> While Tick: " + w);
    w++;
}).execute();


// ==========================================================
// 4. TESTE: DO-WHILE LOOP
// ==========================================================
println("\n[4] A testar Do-While Loop JIT...", "#FFFF00");
var dw = 0;
Do(() => {
    println("    -> Do-While Tick: " + dw);
    dw++;
}).While(() => { return dw < 2; }).execute();


// ==========================================================
// 5. TESTE: MATCH COM GUARDAS MATEMÁTICAS
// ==========================================================
println("\n[5] A testar Match Pattern (Com Guardas)...", "#FFFF00");
var sinal = "ALERTA";
var gravidade = 5;

// O Arm recebe: (Valor, Guarda_Opcional, Bloco)
Match(sinal)
    .Arm("ALERTA", () => { return gravidade > 3; }, () => {
        println("    -> Match detetou: Alerta Máximo! Evacuar!");
    })
    .Arm("ALERTA", () => { return gravidade <= 3; }, () => {
        println("    -> Match detetou: Alerta Leve.");
    })
    .Default(() => {
        println("    -> Match detetou: Tudo normal.");
    })
    .execute();


// ==========================================================
// 6. TESTE: INJEÇÃO JIT DE INSTÂNCIA (Singleton Behavior)
// ==========================================================
println("\n[6] A testar JIT Method Injection (bot::injectMethod)...", "#FFFF00");
var paramQtd = Param("qtd", TYPES.INT);

var logicaConsumo = () => {
    println("    -> A consumir " + qtd + " de energia...");
    this.energia = this.energia - qtd;
};

var metodoJit = Func("consumirEnergia", VISIBILITY.PUB, [paramQtd], TYPES.VOID, logicaConsumo);

// Injetamos apenas na instância 'bot', provando que o motor isola a memória!
bot::injectMethod(metodoJit);
bot.consumirEnergia(40);
println("    -> Energia pós-consumo: " + bot.energia); // Deve ser 60


// ==========================================================
// 7. TESTE: FOR LOOP CLÁSSICO
// ==========================================================
println("\n[7] A testar For Loop JIT...", "#FFFF00");
For(
    () => { let i = 0; },     // Init
    () => { return i < 3; },  // Condição
    () => { i++; },           // Incremento
    () => { println("    -> For Tick: " + i); } // Corpo
).execute();

println("\n=================================================", "#00FF00");
println(" 🏆 TODOS OS MÓDULOS PASSARAM COM SUCESSO!", "#00FF00");
println("=================================================", "#00FF00");