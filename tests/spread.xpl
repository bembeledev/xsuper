fun somar(multiplicador:int, ...numeros:array):float{
    // numeros vira um Array XPL automaticamente!
    let total = 0;
    for n in numeros { total += n; }
    return total * multiplicador;
}

let arrayDeNumeros = [10, 20, 30];
println(somar(2, ...arrayDeNumeros)); // Desempacota na chamada!


let user = { nome: "XPL", nivel: 99 };
let admin = { ...user, cargo: "chefe", nivel: 100 }; // { nome: "XPL", cargo: "chefe", nivel: 100 }
println("Object:  " + admin);

let pares = [2, 4, 6];
let impares = [1, 3, 5];
let todos = [...impares, ...pares, 7, 8]; // [1, 3, 5, 2, 4, 6, 7, 8]

println("Array:  " + todos);
