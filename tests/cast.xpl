var b = 12.99;
var c = b as int;
println("Decimal original: " + b);
println("Convertido para Inteiro: " + c);

var texto = "500";
var numero = texto as int;
println("Texto + 20: " + (texto + 20));       // Imprime: 50020
println("Numero + 20: " + (numero + 20));     // Imprime: 520

var d = 100 as float;
println("Inteiro para Float: " + d);          // O stringify tratará de mostrar como numérico puro



println("==========================================", "#00FFFF");
println("      SUPER XPL - STRESS TEST: CASTING    ", "#00FFFF");
println("==========================================\n", "#00FFFF");

// ------------------------------------------
println("1. CAST DE PRIMITIVOS EM CASCATA");
// ------------------------------------------
var pi = 3.14159265;
var pi_inteiro = pi as int;
var pi_texto = pi_inteiro as string;

println("Float original: " + pi);
println("Float -> Int:   " + pi_inteiro);
println("Int -> String:  " + pi_texto + " (Matemática com texto: " + (pi_texto + 100) + ")");
println("");

// ------------------------------------------
println("2. CAST DENTRO DE COLEÇÕES E LOOPS");
// ------------------------------------------
// Array com dados "sujos" a simular uma leitura de ficheiro ou API
var dados_sujos = ["10", "20.5", "30.1", "40"];
var soma = 0.0;

for i in (0, dados_sujos.length - 1) {
    // Pega na string, converte para float dinamicamente e acumula!
    soma += dados_sujos[i] as float;
}
println("Dados brutos: " + dados_sujos);
println("Soma exata (Strings convertidas para Float): " + soma);
println("Soma inteira (Float convertido para Int): " + (soma as int));
println("");

// ------------------------------------------
println("3. CAST COM ORIENTAÇÃO A OBJETOS (UPCASTING)");
// ------------------------------------------
declare Funcionario {
    pub nome: string;
    pub salario: float;
}

implement Funcionario {
    default { nome: "Desconhecido", salario: 0.0 }

    pub fun toString(): string {
        return "Funcionario[" + this.nome + " | " + this.salario + " MZN]";
    }
}

// A Herança a entrar em ação
declare Engenheiro extends Funcionario {
    pub linguagem: string;
}

implement Engenheiro {
    default { linguagem: "XPL" }

    pub fun programar(): string {
        return this.nome + " está a codar o compilador em " + this.linguagem;
    }
}

var eng = new Engenheiro();
eng.nome = "Samito";
eng.salario = 150000.75;
eng.linguagem = "Rust e Java";

println("Instância Original (Subclasse): " + eng);
println("Ação nativa: " + eng.programar());

// ⭐ O TESTE DE FOGO: Converter o filho para a classe mãe ⭐
var worker = eng as Funcionario;
println("\nUpcast Seguro (Engenheiro as Funcionario): " + worker);

// Cast aninhado: Propriedade (Float) -> Inteiro -> String
println("Salário limpo pós-cast: " + ((worker.salario as int) as string) + " MT");
println("");

// ------------------------------------------
println("4. CAST E IMUTABILIDADE CRUZADA");
// ------------------------------------------
// Congela o objeto DEPOIS de fazer o cast para Funcionario!
var congelado = (eng as Funcionario).toObject();
println("Memória Congelada: " + congelado);

println("\n==========================================", "#00FFFF");
println("         MOTOR XPL SOBREVIVEU!            ", "#00FFFF");
println("==========================================", "#00FFFF");