println("==================================================", "#00FFFF");
println("     🛡️ SUPER XPL - MEGA STRESS: DECORADORES 🛡️   ", "#00FFFF");
println("       Alocação, Context Proxy e Delegação        ", "#00FFFF");
println("==================================================\n", "#00FFFF");

// --- 0. CLASSE ALVO DE TESTE ---
declare Servidor {
    pub host: string;
}

implement Servidor {
    pub fun init(host: string) {
        this.host = host;
    }
    pub fun boot(): string {
        return "Servidor [" + this.host + "] inicializado a 10.000 RPM!";
    }
}

// --- 1. A ESTRUTURA DO DECORADOR (Layout de RAM) ---
decorator Auditoria {
    pub idOperacao: int;
    pub responsavel: string;
    pub nivel: string;
    pub ctx: object; // O interpretador injeta o Contexto nativo aqui dentro!
}

// --- 2. A METAPROGRAMAÇÃO (Os Gatilhos) ---
abstract implement Auditoria {
    
    // ⭐ A ALFÂNDEGA DE ENTRADA (Valida os argumentos do @Auditoria)
    pub fun init(idOperacao: int, responsavel: string, nivel: string = "INFO") {
        this.idOperacao = idOperacao;
        this.responsavel = responsavel;
        this.nivel = nivel;
    }

    @(Context.Init)
    pub fun aoNascer() {
        println("[SUPER-HOOK: Init] Detetada nova alocação de memória!");
        println("  -> Alvo capturado:   " + this.ctx.targetName);
        println("  -> Tipo Estrutural:  " + this.ctx.targetType);
        println("  -> Responsável:      " + this.responsavel + " (Op ID: " + this.idOperacao + ")");
        println("  -> Nível de Risco:   " + this.nivel);
        
        // Verificação quântica de integridade:
        if (this.ctx.get() == null) {
            throw new Error("Interceção Fatal: O objeto decorado nasceu nulo!");
        }
        println("  [Status] Objeto embrulhado no Contexto com sucesso.\n", "#00FF00");
    }
}

println("[Fase 1] Aplicando o decorador na variável global...");

// ⭐ A APLICAÇÃO DO AUTOCOLANTE (Sem ponto e vírgula no final!)
@Auditoria(idOperacao: 808, responsavel: "Fernando Nelson Bembele", nivel: "ALERTA_VERMELHO")
var srvProd = new Servidor("maputo.datacenter.mz");

println("[Fase 2] Invocando método através da blindagem do Proxy...");

// O momento da verdade: a variável 'srvProd' agora é uma caixa proxy do Contexto.
// Quando chamamos .boot(), a XplInstance tem de detetar o buraco negro,
// delegar a chamada para dentro do Servidor real e devolver a string!
let resposta = srvProd.boot();

println(" -> Resposta capturada: " + resposta, "#FFFF00");

println("\n==================================================", "#00FFFF");
println(" 🏆 TESTE CONCLUÍDO! O BURACO NEGRO ESTÁ ESTÁVEL! 🏆 ", "#00FFFF");
println("==================================================", "#00FFFF");


listener Monitor {
    pub value: int;
}

abstract implement Monitor {
    // ⭐ A ALFÂNDEGA DE ENTRADA (Valida os argumentos do @Auditoria)
    pub fun init(value: int) {
        this.value = value;
    }
    @(Listen.Init) pub fun aoNascer() { println("🚀 INIT: Monitor ligado! - " +this.value); }
    @(Listen.Get)  pub fun onGet()  { println("👁️ GET: Propriedade lida. - " + this.value); }
    @(Listen.Set)  pub fun onSet()  { println("✍️ SET: Propriedade alterada! - " + this.value); }
    @(Listen.End)  pub fun aoMorrer() { println("⚰️ END: Monitor desligado. - " + this.value); }
}

declare TV {pub value: int;}
implement TV {
    pub fun init() {}
}

&Monitor(value: 10)
var obj = new TV();

println("--- Iniciando Ciclo de Vida ---");
let v = obj.value; // Dispara GET
obj.value = 20;    // Dispara SET
println("Valor final: " + obj.value); // GET
println("--- Fim do Teste ---");