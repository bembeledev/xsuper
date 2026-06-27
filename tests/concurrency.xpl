println("=========================================");
println(" 🧬 PIPELINE DE CONCORRÊNCIA VIA CANAIS ");
println("=========================================");

// Criamos um canal de comunicação seguro para tráfego de dados
var canal = channel_create();
var tranca = mutex_create();

var contadorGlobal = 0;

// TASK 1: O Produtor (Roda em background a gerar pacotes)
var produtor = task_run(() => {
    for (let i:int = 1; i <= 3; i++) {
        task_sleep(500); // Simula processamento de I/O
        println("   [Produtor] A enviar pacote #" + i + " para o canal...");
        canal.send("Ficheiro_Dados_Parte_" + i + ".bin");
    }
    // Envia sinal de encerramento
    canal.send("EOF");
});

// TASK 2: O Consumidor (Espera passivamente pelos dados sem travar o motor)
var consumidor = task_run(() => {
    let continuar = true;
    while (continuar) {
        // O receive() adormece a thread até haver dados reais no buffer!
        let dado = canal.receive();
        if (dado == "EOF") {
            continuar = false;
        } else {
            println("   [Consumidor] -> Processando dados críticos: " + dado);
            // Protegemos a região crítica de escrita partilhada com Mutex!
            tranca.lock();
            contadorGlobal++;
            tranca.unlock();
        }
    }
});

// Aguardamos que ambas as tarefas concluam
task_all([produtor, consumidor]);

println("\n[Main] Sincronização total concluída!");
println("Total de ficheiros processados na pipeline: " + contadorGlobal);

