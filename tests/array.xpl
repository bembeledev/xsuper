println("==========================================", "#00FFFF");
println("      SUPER XPL - TEST SUITE: ARRAYS      ", "#00FFFF");
println("==========================================\n", "#00FFFF");

// ------------------------------------------
println("1. PROPRIEDADES DIRETAS");
// ------------------------------------------
let p = [10, 20, 30];
println("Array base: " + p.join(", "));
println(" -> length:  " + p.length);      // 3
println(" -> isEmpty: " + p.isEmpty);     // false
println(" -> first:   " + p.first);       // 10
println(" -> last:    " + p.last);        // 30
println("");

// ------------------------------------------
println("2. ADIÇÃO E REMOÇÃO");
// ------------------------------------------
let mut = [1, 2];
mut.push(3, 4);
println(" -> push(3, 4): " + mut.join(", ")); // 1, 2, 3, 4

mut.pop();
println(" -> pop():      " + mut.join(", ")); // 1, 2, 3

mut.unshift(0);
println(" -> unshift(0): " + mut.join(", ")); // 0, 1, 2, 3

mut.shift();
println(" -> shift():    " + mut.join(", ")); // 1, 2, 3

mut.remove(1);
println(" -> remove(1):  " + mut.join(", ")); // 1, 3

let sp = [10, 20, 30, 40];
let removidos = sp.splice(1, 2, 99, 100);
println(" -> splice original:  " + sp.join(", "));      // 10, 99, 100, 40
println(" -> splice removidos: " + removidos.join(", "));// 20, 30
println("");

// ------------------------------------------
println("3. TRANSFORMAÇÃO E PREENCHIMENTO");
// ------------------------------------------
let base = [1, 2];
let outro = [3, 4];
println(" -> concat:     " + base.concat(outro, 5).join(", ")); // 1, 2, 3, 4, 5

let f = [1, 2, 3, 4, 5];
f.fill(0, 1, 4);
println(" -> fill(0,1,4):" + f.join(", ")); // 1, 0, 0, 0, 5

let c = [1, 2, 3, 4, 5];
c.copyWithin(0, 3, 5);
println(" -> copyWithin: " + c.join(", ")); // 4, 5, 3, 4, 5

let sl = [10, 20, 30, 40];
println(" -> slice(1,3): " + sl.slice(1, 3).join(", ")); // 20, 30

let achatar = [1, [2, 3]];
println(" -> flat(1):    " + achatar.flat(1).join(", ")); // 1, 2, 3
println("");

// ------------------------------------------
println("4. PESQUISA E LOCALIZAÇÃO");
// ------------------------------------------
let busca = [10, 20, 30, 20, 40];
println(" -> indexOf(20):     " + busca.indexOf(20));     // 1
println(" -> lastIndexOf(20): " + busca.lastIndexOf(20)); // 3
println(" -> includes(30):    " + busca.includes(30));    // true
println(" -> at(-1):          " + busca.at(-1));          // 40
println("");

// ------------------------------------------
println("5. ORDENAÇÃO E REVERSÃO");
// ------------------------------------------
let ordem = [3, 1, 4, 2];
println(" -> toReversed: " + ordem.toReversed().join(", ")); // 2, 4, 1, 3
ordem.reverse();
println(" -> reverse:    " + ordem.join(", "));              // 2, 4, 1, 3

let sortArr = [30, 10, 40, 20];
println(" -> toSorted:   " + sortArr.toSorted().join(", ")); // 10, 20, 30, 40
sortArr.sort();
println(" -> sort:       " + sortArr.join(", "));            // 10, 20, 30, 40
println("");

// ------------------------------------------
println("6. CALLBACKS (HIGHER-ORDER FUNCTIONS)");
// ------------------------------------------
let num = [1, 2, 3, 4, 5];

println(" -> map (*2):     " + num.map(x => x * 2).join(", "));
println(" -> filter (>2):  " + num.filter(x => x > 2).join(", "));
println(" -> find (>3):    " + num.find(x => x > 3));
println(" -> findIndex:    " + num.findIndex(x => x > 3));
println(" -> some (==5):   " + num.some(x => x == 5));
println(" -> every (>0):   " + num.every(x => x > 0));

// Funções nomeadas para métodos que enviam mais de 1 parâmetro:
fun Redutor(acc:int, atual:int, indice:int):int {
    return acc + atual;
}
println(" -> reduce(soma): " + num.reduce(Redutor, 0)); // 15

println(" -> forEach:");
fun ImprimirIndice(valor:int, idx:int) {
    println("      Item[" + idx + "] = " + valor);
}
num.forEach(ImprimirIndice);

println("\n==========================================", "#00FFFF");
println("             TESTES CONCLUIDOS              ", "#00FFFF");
println("============================================", "#00FFFF");