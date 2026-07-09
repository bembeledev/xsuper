declare Cofre {
    pub readonly saldo: float;
    priv password: string;
    pub final MOEDA: string;
}

implement Cofre {
    default {
        saldo: 1000.0,
        password: "123",
        MOEDA: "MZN"
    }

    pub fun depositar(valor: float) {
        // PERMITIDO: A classe tem acesso ao seu próprio readonly e private!
        this.saldo += valor;
        println("Depositaste " + valor + " " + this.MOEDA);
    }
}

var c = new Cofre();

// 1. LER:
println(c.saldo);   // OK! É readonly, o público pode ler!
// println(c.password); // ERRO FATAL: É privada!

// 2. ESCREVER:
c.depositar(500.0);   // OK!
//c.saldo = 50000; // ERRO FATAL: Tentativa de escrita externa em readonly!
c.MOEDA = "USD"; // ERRO FATAL: Propriedade é Final!