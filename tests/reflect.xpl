sealed declare MotorAPI {
    pub cavalos: int;
    pub modelo: string;
}

implement MotorAPI {
    pub fun ligar() { println("Vrumm..."); }
    pub fun desligar() { println("Silêncio."); }
}

var meuMotor = new MotorAPI();

// ========================================================
// A MÁGICA DA REFLEXÃO XPL
// ========================================================
println("1. Nome do Modelo: " + reflect_name(meuMotor));
println("2. É um modelo blindado? " + reflect_isSealed(meuMotor));

var variaveis = reflect_fields(meuMotor);
println("3. Propriedades detetadas: " + variaveis);

var funcoes = reflect_methods(meuMotor);
println("4. Funções detetadas: " + funcoes);

declare Calculadora {
    pub nome: string;
}

implement Calculadora {
    pub fun init(nome: string) {
        this.nome = nome;
    }
    pub fun somar(a: int, b: int): int {
        println("[" + this.nome + "] A calcular " + a + " + " + b + "...");
        return a + b;
    }
}

var calc = new Calculadora("Casio Quântica");

// 1. Chamada Normal (Hardcoded)
var res1 = calc.somar(10, 20);

// ========================================================
// 2. A MÁGICA DA INTERCESSÃO DINÂMICA (Metaprogramação)
// Estilo JavaScript: Reflect.apply(calc, "somar", [50, 50])
// ========================================================
var nomeDoMetodo = "somar";
var argumentos = [50, 50];

// O motor pega na string, encontra o método, injeta o 'this' (bind) e dispara os argumentos!
var res2 = reflect_invoke(calc, nomeDoMetodo, argumentos);

println("Resultado Reflexivo: " + res2);