var v = 25;

// 1. O PYTHONIC INLINE
let taxa = 0.5 if v > 100 else 0.1;
println("Taxa aplicada: " + taxa); // Imprime 0.1

// 2. O RUST BLOCK EXPRESSION
let calculo = if (v >= 10 || v < 14) {
    println("Entrou na rota A...");
    v ** 2; // ⭐ Sem 'return', sem '='. O resultado vai direto para a variável 'calculo'!
} else {
    v + 100;
};

println("Resultado do bloco: " + calculo); // Imprime 625

// 3. O IF CLÁSSICO (Sobrevivente)
if (v == 25) {
    println("O If clássico contínua a funcionar perfeitamente como declaração!");
}