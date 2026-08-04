println("=== INTEROP MADURO - DEMONSTRAÇÃO ===");

// 1. EVAL com Python
println("\n--- Eval Python ---");
let result = Interop.eval("python", "print('Olá do Python!'); print(2+2)");
println("Resultado: " + result);

// 2. SPAWN com controlo total
println("\n--- Spawn com opções ---");
let opts = {
    cwd: ".",
    env: { "MY_VAR": "valor_secreto" },
    charset: "UTF-8",
    timeout: 5000,
    stream: false
};
let proc = Interop.spawn("cmd.exe", ["/c", "echo", "Olá do XPL!"], opts);
let saida = proc.readAll();
proc.wait(2000);
println("Saída: " + saida);
println("PID: " + proc.pid);
println("Código de saída: " + proc.exitCode);

// 3. STREAMING ASSÍNCRONO (com canais)
println("\n--- Streaming assíncrono ---");
var canal = channel_create();
let optsStream = {
    "stream": true,
    "stdoutChannel": "canal",
    "timeout": 3000,
    "charset": "Windows-1252" // Resolve os acentos do Ping no Windows!
};

// No Windows, o ping usa -n em vez de -c para o limite de saltos!
let proc2 = Interop.spawn("ping", ["-n", "4", "8.8.8.8"], optsStream);

task_sleep(100);
println("A ler do canal...");

while (true) {
    let linha = canal.receive();

    // O escudo: Sai do loop se a mensagem for EOF!
    if (linha == null || linha == "EOF") {
        break;
    }

    println("> " + linha);
}

proc2.wait(3000);
println("Processo terminado.");

// 4. GESTÃO DE CICLO DE VIDA
println("\n--- Ciclo de vida ---");
// Usamos o timeout do Windows em vez de sleep para simular um processo duradouro
let proc3 = Interop.spawn("timeout", ["30"]);
println("PID: " + proc3.pid);
println("Está vivo? " + proc3.isAlive);
proc3.terminate(); // SIGTERM
task_sleep(100);
println("Está vivo? " + proc3.isAlive);
println("Código de saída: " + proc3.exitCode);