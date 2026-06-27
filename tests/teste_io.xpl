println("=========================================");
println(" 🖥️ SISTEMA DE REGISTO DE UTILIZADORES 🖥️ ");
println("=========================================");

// 1. Usar o Prompt direto
var nome = prompt("Qual é o teu nome Mestre? ");

// 2. Usar o Prompt com conversão automática de tipo
var idade = prompt_int("Quantos anos tens? ");

// 3. Print e Read separados
print("És um programador (sim/nao)? ");
var isDev = read_bool();

println("\n--- PERFIL SALVO ---");
println("Nome: " + nome);
println("Idade: " + idade + " anos (Faltam " + (100 - idade) + " para os 100!)");
println("Dev: " + isDev);