global stop = false;

println("=== MÚLTIPLAS TAREFAS E CANAIS ===");

// Criar canal
var canal = channel_create();

// Produtor 1
var prod1 = task_run(() => {
    for (let i:int = 1; i <= 3; i++) {
        canal.send("Prod1-msg" + i);
        task_sleep(200);
    }
    canal.send("EOF");
});

// Produtor 2
var prod2 = task_run(() => {
    for (let i:int = 1; i <= 3; i++) {
        canal.send("Prod2-msg" + i);
        task_sleep(300);
    }
    canal.send("EOF");
});

// Consumidor
var consumidor = task_run(() => {
    let msgs = 0;
    let eofs = 0;
    while (eofs < 2) {
        let dado = canal.receive();
        if (dado == "EOF") {
            eofs++;
            println("Consumidor: recebeu EOF (" + eofs + "/2)");
        } else {
            msgs++;
            println("Consumidor: " + dado);
        }
    }
    println("Consumidor: terminou. Total mensagens: " + msgs);
});

// Aguarda todas
task_all([prod1, prod2, consumidor]);
println("Todas as tarefas concluídas.");