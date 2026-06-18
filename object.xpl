println("==========================================", "#00FFFF");
println("    SUPER XPL - TEST SUITE: DICIONÁRIOS   ", "#00FFFF");
println("==========================================\n", "#00FFFF");

// 1. Dicionário Aninhado Complexo
let db = {
    "empresa": "DIC Moz Solutions",
    "ano": 2026,
    "tech": ["Java", "Rust", "XPL"],
    "ceo": {
        "nome": "Fernando",
        "cidade": "Maputo"
    }
};

// ------------------------------------------
println("1. ACESSO E PROPRIEDADES");
// ------------------------------------------
println(" -> size:    " + db.size);              // 4
println(" -> isEmpty: " + db.isEmpty);           // false
println(" -> dot:     " + db.empresa);           // DIC Moz Solutions
println(" -> index:   " + db["ano"]);            // 2026
println("");

// ------------------------------------------
println("2. MANIPULAÇÃO BASE");
// ------------------------------------------
db.set("status", "Ativo");
println(" -> set():        " + db.status);        // Ativo
println(" -> has(status):  " + db.has("status")); // true
println(" -> keys():       " + db.keys().join(", "));
db.remove("status");
println(" -> has(status):  " + db.has("status")); // false
println("");

// ------------------------------------------
println("3. TRANSFORMAÇÕES AVANÇADAS");
// ------------------------------------------
let conf = { "tema": "dark", "versao": 1.0, "beta": true };

let filtrado = conf.pick("tema", "versao");
println(" -> pick: " + filtrado.keys().join(" & ")); // tema & versao

let semBeta = conf.omit("beta");
println(" -> omit: " + semBeta.keys().join(" & "));  // tema & versao

let base = { "a": 1, "b": 2 };
let extra = { "b": 99, "c": 3 };
let fundido = base.merge(extra);
println(" -> merge: a=" + fundido.a + ", b=" + fundido.b + ", c=" + fundido.c); // a=1, b=99, c=3
println("");

// ------------------------------------------
println("4. O MOTOR DE GRAFOS (SEARCH & PATHS)");
// ------------------------------------------
println(" -> hasPath('ceo.nome'):      " + db.hasPath("ceo.nome"));       // true
println(" -> hasPath('tech[1]'):       " + db.hasPath("tech[1]"));        // true
println(" -> hasPath('ceo.idade'):     " + db.hasPath("ceo.idade"));      // false

println("\n>>> TESTE DE FLATTEN <<<", "#FFFF00");
let plano = db.flatten();
println(" -> ceo.cidade virou: " + plano["ceo.cidade"]); // Maputo
println(" -> tech[2] virou:    " + plano["tech[2]"]);    // XPL

println("\n>>> TESTE DE PESQUISA (SEARCH) <<<", "#FFFF00");
// Procura em todo o objeto e sub-objetos onde está a palavra "Fernando"
let busca = db.searchByValue("Fernando");

// Como search devolve um Array de Objetos, podemos iterar!
for resultado in busca {
    println(" -> Encontrado '" + resultado.value + "' no caminho: " + resultado.path, "#00FF00");
}

println("\n==========================================", "#00FFFF");
println("             TESTES CONCLUIDOS            ", "#00FFFF");
println("==========================================", "#00FFFF");