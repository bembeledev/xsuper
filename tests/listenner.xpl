listener Validate {}

implement Validate {

    @(Listen.Set)
    pub fun aoAtualizar() {
        println("✏️ Evento " + this.event + " na propriedade '" + this.property + "'");

        // 1. O this.target JÁ É o Reflect.on(objeto)!
        let metodos = this.target.getMethods();

        // ⭐ CORREÇÃO AQUI: Em XPL usa-se '.length' para os Arrays! ⭐
        println("   A classe tem " + metodos.length + " métodos.");

        let nomeDaClasse = this.target.getName();
        println("   O nome do modelo alvo é: " + nomeDaClasse);

        // 2. Manipular memória real sem Loops Infinitos!
        if (this.property == "idade" && this.value < 0) {
            println("   Erro: A classe " + nomeDaClasse + " não aceita idades negativas.");
        }

        // 3. Modifica propriedades à socapa (bypass)!
        if (this.property == "idade") {
            this.target.setFieldValue("nome", "Refletido com MOP Madura!");
        }
    }
}

// O Teste:
&Validate()
declare Pessoa {
    pub nome: string;
    pub idade: int;
}

implement Pessoa {
    default { nome: "XPL", idade: 10 }
}

var p = new Pessoa();

// Dispara o @(Listen.Set)
p.idade = 25;

println("Nome final da pessoa real: " + p.nome);