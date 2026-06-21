println("==================================================");
println("     🛡️ TESTE CANÓNICO: VOLUME 20 (ALIASES) 🛡️     ");
println("==================================================");

// 1. Aliases Primitivos e Encadeados:
type Inteiro = int;
type NumeroByte = Inteiro; // Encadeamento de 2º grau!
type Texto = string;

var idade: NumeroByte = 25;
var apelido: Texto = "Macassa";

println("[1/4] Aliases Simples: " + apelido + " tem " + idade + " anos.");


// 2. Apelidando Modelos de Domínio (OOP):
declare FichaTecnica {
    pub peso: int;
}

implement FichaTecnica {

}

type Documento = FichaTecnica;

var doc: Documento = new FichaTecnica();
doc.peso = 88;

println("[2/4] Alias de POO instanciado com peso: " + doc.peso);


// 3. O Teste de Colisão Quântica (Alias + Opcional V21):
type Telefone = ?string; // O alias já carrega a permissão de ser nulo!

var celCasa: Telefone = "+258840000000";
var celTrabalho: Telefone = null; // Passa na alfândega de forma imaculada

println("[3/4] Coalescência sobre Alias Opcional: " + (celTrabalho ?? "Sem Telefone"));


// 4. Testando a Defesa contra Ciclos:
var logCiclo = "Nenhum erro pego.";
try {
    println("[4/4] Tentando armadilha de referência circular...");

    // O crime: tentar compilar um sinónimo paradoxal
    // (Descomenta no ficheiro real para testar a detonação do compilador!)
     type Alfa = Beta;
     type Beta = Alfa;

    logCiclo = "Armadilha evitada (Código comentado).";
} catch (e: Error) {
    logCiclo = "Pegou o motor: " + e.message;
}

println(" -> Status do Escudo: " + logCiclo);
println("==================================================");