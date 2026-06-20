declare Animal {
    pub nome: string;
    pub static INSTANCE: int;
}

implement Animal {
    default {
        nome: "ANA",
        INSTANCE: 100
    }

    pub fun init(){
      Animal.INSTANCE++;
    }

    pub static fun verContador() {
        println("Animais Totals: " + Animal.INSTANCE);
    }
}

Animal.verContador(); // Imprime: Animais Totais: 100
Animal.INSTANCE += 5;

var obj = new Animal();
Animal.verContador(); // Imprime: Animais Totais: 100
println(Animal.INSTANCE); // Imprime: ANA