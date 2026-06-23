module app.main;

println("==================================================", "#00FFFF");
println("       📦 SUPER XPL - TESTE DE MODULARIDADE 📦    ", "#00FFFF");
println("==================================================\n", "#00FFFF");

// 1. Importação Simples
import geometria.Ponto.Ponto; // src/geometria/Ponto.xpl/Símbolo

// 2. Importação com Alias (O Circulo vai disfarçar-se de Circle)
import geometria.Circulo.{Circulo as Circle,Circe};// src/geometria/Circulo.xpl/Símbolo

// 3. Importação Wildcard (Puxa o somar, o multiplicar e o PI)
import util.Matematica.*;// src/util/Matematica.xpl/Símbolo
// ou import util.Matematica.{somar, multiplicar, PI};

println("[Teste 1] Instanciando Ponto (Import Simples)...", "#FFFF00");
var p = new Ponto(10, 20);
println(" -> Ponto criado na RAM: X=" + p.x + ", Y=" + p.y, "#00FF00");

println("\n[Teste 2] Instanciando Circulo (Import com Alias)...", "#FFFF00");
var c = new Circle(5.5);
println(" -> Circulo criado com raio: " + c.raio, "#00FF00");
println(" -> Área calculada: " + c.area(), "#00FF00");

var c1 = new Circe(12.23);
println(" -> Circulo criado com raio: " + c1.raio, "#00FF00");
println(" -> Área calculada: " + c1.area(), "#00FF00");

println("\n[Teste 3] Usando Matemática (Import Wildcard e Funções)...", "#FFFF00");
var soma = somar(50, 50);
var prod = multiplicar(10, 2);
println(" -> Soma (50+50): " + soma, "#00FF00");
println(" -> Produto (10*2): " + prod, "#00FF00");
println(" -> Constante PI isolada: " + PI, "#00FF00");

println("\n==================================================", "#00FFFF");
println(" 🏆 MÓDULOS CARREGADOS E ISOLADOS COM SUCESSO! 🏆 ", "#00FFFF");
println("==================================================", "#00FFFF");
