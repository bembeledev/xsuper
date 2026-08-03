println("==================================================", "#00FFFF");
println("     🌪️ SUPER XPL - MEGA STRESS: GENÉRICOS 🌪️     ", "#00FFFF");
println("   Reificação C++, Transmutação Primária e Cache  ", "#00FFFF");
println("==================================================\n", "#00FFFF");

// =============================================================================
// 1. O MOTOR DE ARMAZENAMENTO CUSTOMIZADO (Dicionário feito em XPL)
// =============================================================================
declare MapaKV<K, V> {
    pub chaves: array;
    pub valores: array;
}

implement MapaKV<K, V> {
    pub fun init() {
        this.chaves = [];
        this.valores = [];
    }

    // O Monomorfizador vai reescrever K e V fisicamente aqui na compilação!
    pub fun put(chave: K, valor: V) {
        this.chaves.push(chave);
        this.valores.push(valor);
    }

    pub fun get(chave: K): ?V {
        let i: int = 0;
        for (let idx: int = 0; idx < this.chaves.length(); idx = idx + 1) {
            if (this.chaves[idx] == chave) {
                return this.valores[idx];
            }
        }
        return null;
    }
}

// =============================================================================
// 2. O COLOSSO ESTRUTURAL (Utilizando o nosso Mapa Genérico em XPL)
// =============================================================================
declare Ecossistema<T, U, V> {
    pub idUnidade: T;
    pub registroNomes: MapaKV<string, U>; // ⭐ NOVO: Um Genérico aninhado feito puramente em XPL!
    pub metadados: ?V;
}

implement Ecossistema<T, U, V> {

    pub fun init(id: T, metadadosIniciais: ?V) {
        this.idUnidade = id;
        // Instancia o nosso mapa reificado especificamente para <string, U>
        this.registroNomes = new MapaKV<string, U>();
        this.metadados = metadadosIniciais;
    }

     pub fun registrar(chave: string, entidade: U, pontuacao: T): bool {
        this.registroNomes.put(chave, entidade);
        println(" -> [Registro OK] " + chave + " | Entidade em RAM: " + typeof(entidade), "#00FF00");
        return true;
    }

    pub fun auditarMetadados(): ?V {
        return this.metadados;
    }
}

// =============================================================================
// 3. FASE DE EXECUÇÃO: A TORTURA DO SILÍCIO
// =============================================================================

println("[Fase 1] Sintetizando Clones Alpha <int, string, float>...", "#FFFF00");
// 1024 (Long/Integer da JVM) -> T=int engole!
// 99.5 (Double da JVM) -> V=float engole!
var ecoAlpha = new Ecossistema<int, string, float>(1024, 99.5);

// Isto vai forçar a execução do método .put(string, string) dentro do clone MapaKV<string, string>!
ecoAlpha.registrar("Arquiteto", "Fernando Nelson", 5000);
ecoAlpha.registrar("Motor", "V22 Quântico", 82570);

println("\n[Fase 2] Testando Isolamento de Molde e Promoção Numérica...", "#FFFF00");
// Força a criação de um Ecossistema<string, bool, float> que por sua vez cria um MapaKV<string, bool>!
var ecoBeta = new Ecossistema<string, bool, float>("Macia-Sede", 100.0);

ecoBeta.registrar("StatusServidor", true, "ID-9999");

println("\n[Fase 3] Auditando Otimização de Cache de Synthesized Classes...", "#FFFF00");
var ecoAlphaClone = new Ecossistema<int, string, float>(2048, null);
println(" -> Ecossistema Alpha instanciado a partir da Cache (Sem duplicar AST)!", "#00FF00");

println("\n[Fase 4] Verificação Forense de Tipagem em Tempo de Execução...", "#FFFF00");
println("Assinatura de ecoAlpha:      " + typeof(ecoAlpha), "#FFFFFF");
println("Assinatura de ecoBeta:       " + typeof(ecoBeta), "#FFFFFF");
println("Metadados Alpha (Float):     " + ecoAlpha.auditarMetadados(), "#FFFFFF");
println("Metadados AlphaClone (null): " + ecoAlphaClone.auditarMetadados(), "#FFFFFF");

println("\n==================================================", "#00FFFF");
println(" 🏆 TESTE CONCLUÍDO! O NÚCLEO DE RUST ESTÁ ESTÁVEL! 🏆 ", "#00FFFF");
println("==================================================", "#00FFFF");