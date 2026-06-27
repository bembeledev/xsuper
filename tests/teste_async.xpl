println("=========================================");
println(" 🚀 INICIANDO TESTE DE CONCORRÊNCIA XPL ");
println("=========================================");

println("[Main] A lançar 3 pedidos HTTP em Background (Paralelismo)...");

// Dispara 3 requisições ao GitHub simultaneamente!
let t1 = task_run(():string => {
    println("   -> [Thread 1] A buscar o teu perfil...");
    let res = http_request("https://api.github.com/users/fernando", HTTP.GET, null);
    return "T1 (Perfil): " + res.status;
});

let t2 = task_run(():string => {
    println("   -> [Thread 2] A buscar dados do Google...");
    let res = http_request("https://www.google.com", HTTP.GET, null);
    return "T2 (Google): " + res.status;
});

let t3 = task_run(():string => {
    println("   -> [Thread 3] A calcular uma matemática pesada...");
    task_sleep(2000); // Finge que demorou 2 segundos a processar
    return "T3 (Math): Sucesso!";
});

println("[Main] Tasks lançadas! O sistema NÃO está bloqueado. Posso fazer outras coisas...");

// O `task_all` vai sincronizar todas as threads e devolver os resultados num Array
var resultados = task_all([t1, t2, t3]);

println("\n[Main] Todas as Tasks terminaram! Resultados:");
println(resultados);