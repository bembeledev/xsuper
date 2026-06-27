println("\n=== TESTE DE TASK_TIMEOUT ===");

var tarefaLenta = task_run(() => {
    while (1==1) {
        println("Tarefa lenta a trabalhar...");
        task_sleep(500);
    }
});

// Dá timeout após 2 segundos
task_timeout(tarefaLenta, 2000);

// Aguarda 3 segundos para ver o efeito
task_sleep(3000);
println("Tarefa cancelada? " + task_is_cancelled(tarefaLenta));
println("Tarefa terminou? " + task_is_done(tarefaLenta));