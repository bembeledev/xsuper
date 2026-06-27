println("=== TESTE DO SISTEMA DE FICHEIROS XPL ===");

var path = "teste_log.txt";

// 1. Escrever
println("A criar ficheiro...");
file_write(path, "Linha 1: Motor XPL Operacional.\n");

// 2. Anexar
file_append(path, "Linha 2: Ficheiros dominados.\n");

// 3. Informações
println("O ficheiro existe? " + file_exists(path));
println("Tamanho do ficheiro: " + file_size(path) + " bytes");
println("Extensão do ficheiro: " + file_extension(path));

// 4. Ler em Bloco
println("\n-> Conteúdo lido (Tudo):");
println(file_read(path));

// 5. Ler por Linhas (Devolve um Array)
println("-> Conteúdo lido (Array de Linhas):");
var linhas = file_read_lines(path);
println(linhas); // O motor deve formatar como [Linha 1: ..., Linha 2: ...]

// 6. Limpeza
println("\nA eliminar ficheiro de teste...");
file_delete(path);
println("Eliminado com sucesso? " + !file_exists(path));