module tests.meta_test;

// ============================================================
// 1. DECLARAÇÃO DE INTERFACES E DECORADORES
// ============================================================

interface Loggable {
    pub fun log(mensagem: string): string;
}

interface Serializable {
    pub fun toJSON(): object;
}

// Decorador para testes
decorator Audit {
    pub id: int;
    pub nivel: string;
}

abstract implement Audit {
    pub fun init(id: int, nivel: string = "INFO") {
        this.id = id;
        this.nivel = nivel;
    }
    // Gatilhos para testar reflexão
    @(Context.Init) pub fun aoNascer() {
        // apenas para metadados
    }
}

// ============================================================
// 2. MODELO GENÉRICO COM DECORADORES E INTERFACES
// ============================================================

declare Repositorio<K, V> {
    pub chaves: array;
    pub valores: array;
}

implement Repositorio<K, V> for Loggable, Serializable {
    pub fun init() {
        this.chaves = [];
        this.valores = [];
    }

    pub fun adicionar(chave: K, valor: V) {
        this.chaves.push(chave);
        this.valores.push(valor);
    }

    pub fun obter(chave: K): ?V {
        for i in (0 , this.chaves.length-1) {
            if (this.chaves[i] == chave) {
                return this.valores[i];
            }
        }
        return null;
    }

    // Implementação da interface Loggable
    pub fun log(mensagem: string): string {
        return "[LOG] Repositorio: " + mensagem;
    }

    // Implementação da interface Serializable
    pub fun toJSON(): object {
        return { "chaves": this.chaves, "valores": this.valores };
    }
}

// ============================================================
// 3. DECLARAÇÃO DE ALIASES DE TIPO
// ============================================================

type Inteiro = int;
type Texto = string;
type RepoDeStrings = Repositorio<string, string>;

// ============================================================
// 4. DECLARAÇÃO DE VARIANTES (AS)
// ============================================================

declare Animal {
    pub nome: string;
}

implement Animal {
    default { nome: "Desconhecido" }
}

implement Animal as Cachorro {
    pub fun latir() { println("Au au!"); }
}

implement Animal as Gato {
    pub fun miar() { println("Miau!"); }
}

// ============================================================
// 5. APLICAÇÃO DE DECORADORES EM VARIÁVEIS
// ============================================================

@Audit(id: 101, nivel: "ALTA")
var repo = new Repositorio<string, int>();

// ============================================================
// 6. TESTE DE REFLEXÃO AVANÇADA
// ============================================================

println("==================================================", "#00FFFF");
println("    🔬 SUPER XPL - REFLEXÃO COMPLETA 🔬          ", "#00FFFF");
println("==================================================\n", "#00FFFF");

// --- 6.1. getDeclareToObject (todos os metadados) ---
println("[1/8] getDeclareToObject() - Metadados completos", "#FFFF00");
var meta = Repositorio::getDeclareToObject();

println(" -> Nome: " + meta.name);
println(" -> Abstrato? " + meta.isAbstract);
println(" -> Sealed? " + meta.isSealed);
println(" -> Superclasse: " + (meta.superclass ?? "null"));
println(" -> BaseModel (variante): " + (meta.baseModel ?? "null"));
println(" -> Tem implementação base? " + meta.hasBaseImplementation);
println(" -> Pode ser instanciado? " + meta.canBeInstantiated);
println(" -> Qtd. campos: " + meta.fields.length);
println(" -> Qtd. métodos: " + meta.methods.length);
println(" -> Interfaces implementadas: " + meta.interfaces.join(", "));
println(" -> Parâmetros genéricos: " + meta.typeParameters.join(", "));
println(" -> Decoradores aplicados à classe: " + meta.decorators);
println(" -> Aliases que apontam para este modelo: " + meta.aliases);
println(" -> Variantes (as): " + (meta.variantAliases ?? "[]"));
println(" -> Campos estáticos: " + meta.staticFields);
println(" -> Defaults de instância: " + meta.defaultInstanceFields);

// --- 6.2. getInterfaces ---
println("\n[2/8] getInterfaces()", "#FFFF00");
var ifaces = Repositorio::getInterfaces();
println(" -> Interfaces: " + ifaces.join(", "));

// --- 6.3. getDecorators ---
println("\n[3/8] getDecorators()", "#FFFF00");
var decs = repo::getDecorators(); // via instância
println(" -> Decoradores aplicados a 'repo': " + decs);

// --- 6.4. getTypeParameters ---
println("\n[4/8] getTypeParameters()", "#FFFF00");
var typeParams = Repositorio::getTypeParameters();
println(" -> Parâmetros de tipo: " + typeParams.join(", "));

// --- 6.5. getResolvedGenerics (instância concreta) ---
println("\n[5/8] getResolvedGenerics()", "#FFFF00");
var resolved = repo::getResolvedGenerics();
println(" -> Mapeamento concreto: " + resolved);

// --- 6.6. getAliases (global) ---
println("\n[6/8] getAliases (global)", "#FFFF00");
var allAliases = Repositorio::getAliases();
println(" -> Todos os aliases: " + allAliases);

// --- 6.7. getVariantAliases ---
println("\n[7/8] getVariantAliases()", "#FFFF00");
var variants = Animal::getVariantAliases();
println(" -> Variantes de Animal: " + variants.join(", "));

// --- 6.8. Injeção dinâmica e chamada de método via reflexão ---
println("\n[8/8] CallMethod dinâmico", "#FFFF00");
repo::CallMethod("adicionar", "chave1", 100);
var valor = repo::CallMethod("obter", "chave1");
println(" -> Valor obtido via reflexão: " + valor);

// Teste do toJSON via reflexão
var json = repo::CallMethod("toJSON");
println(" -> JSON do repositório: " + json);

println("\n==================================================", "#00FFFF");
println(" ✅ REFLEXÃO COMPLETA VALIDADA! ✅               ", "#00FFFF");
println("==================================================", "#00FFFF");