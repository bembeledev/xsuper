declare Animal {
    pub nome: string;
    pub static INSTANCIAS: int;
}

implement Animal {
    default {
        nome: "ANA",
        INSTANCIAS: 100
    }

    pub fun init(){
     //Animal.INSTANCIAS++;
    }

    pub static fun verContador() {
        println("Animais Totals: " + Animal.INSTANCIAS);
    }
}

Animal.verContador(); // Imprime: Animais Totais: 100
Animal.INSTANCIAS = 5;

var obj = new Animal();
Animal.verContador(); // Imprime: Animais Totais: 100
println(obj.nome); // Imprime: ANA