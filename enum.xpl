println("==========================================", "#00FFFF");
println("       SUPER XPL - TEST SUITE: ENUMS      ", "#00FFFF");
println("==========================================\n", "#00FFFF");

// 1. Declaração do Enum
enum Estado {
    PENDENTE,
    EM_PROCESSAMENTO,
    CONCLUIDO,
    ERRO
}

// 2. Uso clássico via acesso por ponto
let estadoAtual = Estado.EM_PROCESSAMENTO;

println("Estado Atual: " + estadoAtual);

// 3. Lógica de controlo com Enums
if (estadoAtual == Estado.CONCLUIDO) {
    println("A tarefa terminou com sucesso!");
} else {
    println("A tarefa ainda não está concluída.");
}

// 4. A MAGIA: Os métodos de Objeto/Map funcionam no Enum!
println("\n>>> INSPEÇÃO DO ENUM <<<", "#FFFF00");
println("Estados disponíveis: " + Estado.keys().join(" | "));
println("O Enum tem 'ERRO'? " + Estado.has("ERRO"));
println("Total de estados: " + Estado.size);

println("\n==========================================", "#00FFFF");
println("             TESTES CONCLUIDOS            ", "#00FFFF");
println("==========================================", "#00FFFF");