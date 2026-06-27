var loopInfinito = task_run(():void => {
    while(true) {
        println("A trabalhar...");
        task_sleep(1000);
    }
});

task_sleep(3000); // Espera 3 segundos
println("Chega! A cancelar a tarefa...");
task_cancel(loopInfinito);
println("Tarefa cancelada? " + task_is_cancelled(loopInfinito));