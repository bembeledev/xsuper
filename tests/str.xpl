println("==========================================", "#00FFFF");
println("     SUPER XPL - TEST SUITE: STRINGS      ", "#00FFFF");
println("==========================================\n", "#00FFFF");

let texto = "  ola Super Dev 2026  ";

// ------------------------------------------
println("1. PROPRIEDADES E INFORMAÇÃO BÁSICA");
// ------------------------------------------
println("Original:   [" + texto + "]");
println(" -> length:  " + texto.length);        // 22
println(" -> isEmpty: " + texto.isEmpty);       // false
println(" -> charAt:  " + texto.charAt(6));     // 'S'
println(" -> at(-3):  " + texto.at(-3));        // '2'
println("");

// ------------------------------------------
println("2. TRANSFORMAÇÃO E LIMPEZA");
// ------------------------------------------
let limpo = texto.trim();
println(" -> trim:        [" + limpo + "]");
println(" -> toUpperCase: " + limpo.toUpperCase());
println(" -> toLowerCase: " + limpo.toLowerCase());
println(" -> trimLeft:    [" + texto.trimLeft() + "]");
println("");

// ------------------------------------------
println("3. SUBSTRINGS E EXTRAÇÃO");
// ------------------------------------------
let alvo = "Linguagem XPL";
println("Alvo: " + alvo);
println(" -> slice(0, 9):    " + alvo.slice(0, 9));       // "Linguagem"
println(" -> substring(10):  " + alvo.substring(10));     // "XPL"
println(" -> slice(-3):      " + alvo.slice(-3));         // "XPL" (Índice negativo!)
println("");

// ------------------------------------------
println("4. PESQUISA E VALIDAÇÃO");
// ------------------------------------------
let ficheiro = "script.min.xpl";
println("Ficheiro: " + ficheiro);
println(" -> startsWith: " + ficheiro.startsWith("script")); // true
println(" -> endsWith:   " + ficheiro.endsWith(".xpl"));    // true
println(" -> includes:   " + ficheiro.includes("min"));     // true
println(" -> indexOf:    " + ficheiro.indexOf("."));        // 6
println("");

// ------------------------------------------
println("5. FORMATAÇÃO E SUBSTITUIÇÃO");
// ------------------------------------------
let num = "42";
println(" -> padStart:   " + num.padStart(5, "0"));         // "00042"
println(" -> padEnd:     " + num.padEnd(5, "-"));           // "42---"
println(" -> repeat:     " + "XPL!".repeat(3));             // "XPL!XPL!XPL!"

let bug = "erro no sistema, erro grave";
println(" -> replace:    " + bug.replace("erro", "aviso")); // Substitui o primeiro ou todos dependendo do Java
println(" -> replaceAll: " + bug.replaceAll("erro", "ok")); // "ok no sistema, ok grave"
println("");

// ------------------------------------------
println("6. CONVERSÃO E O PODER HÍBRIDO (CHAINING)");
// ------------------------------------------
let boolTexto = "YES";
println(" -> toBoolean:  " + boolTexto.toBoolean());        // true
println(" -> toNumber:   " + "3.1415".toNumber());          // 3.1415

// A MAGIA SUPREMA: String -> Array -> Array -> String
println("\n>>> TESTE DE CHAINING HÍBRIDO <<<", "#FFFF00");
let csv = "batman,superman,flash";

let resultado = csv.split(",")                   // String vira Array!
                   .map(h => h.toUpperCase())    // Array recebe Arrow Function!
                   .reverse()                    // Array é invertido!
                   .join(" | ");                 // Array vira String!

println("Resultado Final: " + resultado, "#00FF00"); // FLASH | SUPERMAN | BATMAN

println("\n==========================================", "#00FFFF");
println("             TESTES CONCLUIDOS            ", "#00FFFF");
println("==========================================", "#00FFFF");