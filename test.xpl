for i in(1,12,12){}

// Loop normal de 1 a 5 (assume jump = 1)
for i in (1, 5) {
    println("Normal: " + i);
}

var a = 12;
var b = a + " Ana";
println(b);

// Loop invertido automático (assume jump = -1)
for i in (10, 1) {
    println("Contagem decrescente: " + i);
}

// Loop com salto customizado
for i in (0, 100, 20) {
   for j in (10, 1) {
       println(J+"Contagem decrescente: " + i);
   }
}

// Loop com expressões matemáticas dinâmicas (!!!)
let inicio = 2;
for i in (inicio * 2, 50 / 2) {
    println("Matemática pura: " + i);
}

// Função que processa dados e retorna o resultado
fun ProcessarFicheiros(extensao) {
    println("A procurar ficheiros com a extensão: " + extensao, "#00FF00");

    // Executa comando nativo do teu terminal Xplorer!
    let resultado = shell("ps");

    return "Processamento terminado!";
}

// Uma função matemática pura
fun Somar(a, b): int {
    return a + b;
}

for i in(0,10){}

// Invocação moderna (sem o 'call'!)
let mensagem = ProcessarFicheiros(".txt");
println(mensagem, "#FFFF00");

let calculo = Somar(50, 10);
println("Resultado da soma: " + calculo);