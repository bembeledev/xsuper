declare Fatura {
    pub id: int;
    pub cliente: string;
    pub valor: float;
}

implement Fatura {
    default {
        id: 999,
        cliente: "DIC Moz",
        valor: 1500.50
    }

    // ⭐ O método cósmico!
    pub fun toString(): string {
        return "Fatura[ID: " + this.id + " | Cliente: " + this.cliente + " | Total: " + this.valor + "MZN]";
    }
}

var f = new Fatura();

// 1. O toString() executa invisivelmente!
println(f);
println("Log do Sistema: " + f);

// 2. O congelamento imutável!
var estatico = f.toObject();
println(estatico.cliente); // Lê perfeitamente!

estatico.cliente = "Hacker"; // ERRO FATAL: Este objeto é estritamente imutável!