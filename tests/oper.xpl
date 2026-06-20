// 1. TESTE DE CURTO CIRCUITO (O mais importante!)
var nulo = null;
var ignorado = false;

// Se a máquina tentar ler 'nulo.nome', ela vai crashar. Como tem curto-circuito, ela passa ilesa!
if (nulo != null && nulo.nome == "Teste") {
    ignorado = true;
}
println("Curto-circuito sobreviveu? " + !ignorado); // true


// 2. IGUALDADE ESTRITA
var a = 10;
var b = "10";
println("10 == '10':  " + (a == b));   // true (o motor converte ou compara relaxado)
println("10 === '10': " + (a === b));  // false (Tipos diferentes!)


// 3. MATEMÁTICA DE BITS (O nível Rust)
var mascara = 5;       // Em bits: 0101
var flag = 3;          // Em bits: 0011

println("5 & 3 (AND):  " + (mascara & flag));   // Resulta 1  (0001)
println("5 | 3 (OR):   " + (mascara | flag));   // Resulta 7  (0111)
println("1 << 3 (Shl): " + (1 << 3));           // Resulta 8  (1 virou 1000)