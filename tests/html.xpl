// html.xpl

// 1. Definição das funções no Escopo Global
fun login() { 
    println("Entrou no sistema"); 
}

fun logout() { 
    println("Saiu no sistema"); 
}


var isLogado = true;
var nivelAcesso = "admin";


// 2. A UI Declarativa
var html = """
<html>
    <body>
        <button id="12" (click)="login();logout();"></button>
        // Exemplo de como o motor deve processar o teu código:
        @if (isLogado) {
            <div id="painel-restrito">
                <h1>Bem-vindo!</h1>

                @switch (nivelAcesso) {
                    @case ("admin") {
                        <p>Acesso Total.</p>
                    }
                    @default {
                        <p>Visitante.</p>
                    }
                }
            </div>
        } @else {
            <div>Acesso Negado</div>
        }
        <div>OLa</div>
    </body>
</html>
""";

// 3. Processamento via Motor
__ui_engine.loadView(html);

// 4. Captura do objeto nativo instanciado
var btn = document.getElementById("12");
var divs = document.getElementsByTagName("div");

var elemento = divs[0];

println(elemento.innerHTML);


// 5. O gatilho! O botão é acionado, as funções são resolvidas e executadas.
btn.callAction("click"); // Ou btn.callAction(Action.CLICK) se tiveres o enum global

