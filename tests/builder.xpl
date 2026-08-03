println("--- TESTE DE GERAÇÃO DINÂMICA DE CLASSES ---");

// 1. Criamos a classe do Vazio! (Como o CGLib do Java)
var construtorVirtual = Reflect.defineClass("SessaoMagica");

// 2. Injetamos Variáveis (RAM)
construtorVirtual.injectField("id", "int");
construtorVirtual.injectField("token", "string");

// 3. Criamos a Lógica e Injetamos na Classe com um Nome Fixo!
let logicaSalvar = () => {
    println("💾 A guardar o token [" + this.token + "] na Base de Dados...");
};
construtorVirtual.injectMethod(logicaSalvar, "salvarDB");

// 4. Instanciamos a classe que criámos há milissegundos atrás!
var sessaoDin = construtorVirtual.newInstance();

// 5. Manipulamos e Invocamos!
var metaSessao = Reflect.on(sessaoDin);

metaSessao.setFieldValue("id", 100);
metaSessao.setFieldValue("token", "XYZ-999-ABC");

// Chama o comportamento dinâmico que inserimos!
metaSessao.CallMethod("salvarDB");