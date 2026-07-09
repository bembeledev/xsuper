var frutas = ["Maçã", "Banana", "Manga"];

var html = """
<html>
    <body>
        <ul>
            @for (let item of frutas) {
                <li>{{item}}</li>
            } @empty {
                <li>Sem frutas na cesta.</li>
            }
        </ul>
    </body>
</html>
""";

// 3. Processamento via Motor
__ui_engine.loadView(html);

// Vamos imprimir o corpo do documento para ver como a Engine o reescreveu!
println("\n=== ESTADO FINAL DO VIRTUAL DOM ===");
println(document.body.toObject());