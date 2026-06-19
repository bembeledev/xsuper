println(">>> TESTE DE TRATAMENTO DE ERROS <<<", "#00FFFF");

// 1. Testar Erro Manual
try {
    println("A verificar o sistema...");
    throw "A base de dados explodiu!"; // Corta a execução aqui e salta para o catch!
    println("Isto nunca vai ser impresso.");
} catch (erro) {
    println("Erro Capturado: " + erro, "#FF0000"); // Imprime a vermelho
} finally {
    println("Limpeza de memória concluída (Finally).", "#00FF00");
}

println("------------------------");

// 2. Testar Proteção contra Motor Nativo (RuntimeError)
try {
    let conta = 10 / 0; // O motor XPL atira um RuntimeError de Divisão por zero!
} catch (e) {
    println("O motor foi salvo de um crash nativo! Motivo: " + e, "#FF0000");
}

println(">>> O XPL CONTINUA A RODAR PERFEITAMENTE! <<<", "#00FFFF");