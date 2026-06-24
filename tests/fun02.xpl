// Define uma função solta
fun gritar(mensagem: string) {
    println(mensagem + "!!!");
}

// Uma função que recebe a tua função como argumento
fun processarEvento( id: int, callback: (string) -> void ) {
    if (id == 1){
        callback("O evento 1 disparou"); // Invoca a função recebida!
    }
}

// Passando a função sem a executar
processarEvento(1, gritar);

// 1. Uma função normal de matemática
fun somar(a: int, b: int): int {
    return a + b;
}
// 2. ⭐ O PODER DA FUNÇÃO COMO ARGUMENTO ⭐
// O parâmetro 'operacao' exige receber uma função que tome dois 'int' e devolva 'int'
fun calcularTudo(x: int, y: int, operacao: (int, int) -> int) {
    println("A iniciar cálculo complexo...");

    // Invocamos a função que viajou pela variável!
    let resultado = operacao(x, y);
    println("Resultado final: " + resultado);
}


// 3. A Execução
// Passamos a REFERÊNCIA da função 'somar' (sem os parêntesis, não a estamos a invocar!)
calcularTudo(10, 20, somar);