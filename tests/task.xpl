let cancelar = false;   // <-- AGORA global!

var loopInfinito = task_run(() => {
    while(!cancelar) {
        println("A trabalhar...");
        task_sleep(1000);
    }
    println("Tarefa terminou.");
});

task_sleep(3000);
println("Chega! A cancelar a tarefa...");
cancelar = true;
task_sleep(500);
println("Tarefa cancelada? " + task_is_done(loopInfinito));