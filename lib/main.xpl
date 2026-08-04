module app.main;
//import com.pdf.convert.globals.* prefix "PDFCONV";
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

var c1 = new Circe(12.23,PI);
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

fun executar() {
    // 0 imports aqui dentro! O motor buscou na cadeia de ambientes!
    println("Biblioteca PDF ativa na versão: " + PDFCONV_VERSION);
    println("HASH: " + PDFCONV_PDF_SECRET_HASH);
}

executar();

implement Circle as Circe02 { // Tenta adivinhar o nome do declare!
    pub fun init(raio:int, pi:float=3.132343543){
    this.raio = raio;
    this.pi = pi;
    }

    pub fun getArea():float{
        return this.raio * this.raio * this.pi;
    }
}

var v2 = new Circe02(12);
println("Bom dia: "+ v2.getArea());