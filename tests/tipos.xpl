declare Animal { pub nome: string; } implement Animal {}
declare Cao extends Animal { pub raca: string; } implement Cao {}

var c = new Cao();
var n = 12.5;

//debugger;

// 1. O TYPEOF
println("typeof n: " + typeof(n)); // float
println("typeof c: " + typeof(c)); // Cao

// 2. O TYPE (Flexível - ADN)
println("c type Animal? " + (c type Animal)); // true (Herança!)
println("n type float?  " + (n type float));  // true

// 3. O INSTANCE (Restrito - Exacto)
println("c instance Animal? " + (c instance Animal)); // false (Não é um Animal directo, é um Cão!)
println("c instance Cao?    " + (c instance Cao));    // true

// 4. O CAST SEGURO (as)
var numero_seguro = "texto_sujo" as int;
if (numero_seguro == null) {
    println("Cast seguro falhou e devolveu null! Não crashou o motor.");
}

// 5. O CAST FORÇADO (as!)
try {
    let explosao:string = "texto_sujo" as! int;
} catch(e: Error) {
    println("O Cast Forçado explodiu como prometido e foi apanhado pelo try/catch!");
}