let herois = ["Batman", "Superman", "Flash"];

println("O primeiro herói é: " + herois[0]);

// Modificando um valor no Array!
herois[1] = "Mulher Maravilha";

println("A nova equipa é:");
for h in herois {
    println("- " + h, "#00FFFF");
}


// 1. O CHAINING A FUNCIONAR (push devolve a lista!)
herois.push("ANA").push("Olga");
println(herois);
println("Tamanho atual: " + herois.length());

// 2. PREPARANDO A FUNÇÃO PARA O MAP
fun DecorarHeroi(nome) {
    return "[ " + nome + " ]";
}

// 3. O MAP E O JOIN EM CADEIA!
// Passamos o nome da função sem parêntesis!
let resultado = herois.map(DecorarHeroi).join(" -> ");

println("Heróis processados:");
println(resultado, "#00FFFF");

// Vamos imaginar que tens um toUpperCase() nativo, ou usamos matemática pura para testar:
let pontuacoes = [10, 20, 30];

// Uma linha majestosa de Arrow Function com Method Chaining!
let resultado = pontuacoes.map(nota => nota * 2).join(" | ");

println("Notas dobradas: " + resultado);