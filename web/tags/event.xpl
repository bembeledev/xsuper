// =====================================================================
// SuperUI - Aplicação Paint (Desenho Vetorial a 60FPS)
// =====================================================================

// ⭐ APAGAR A VARIÁVEL GLOBAL "var isDrawing = false;" !
// Escondemos TUDO dentro do dicionário "pincel" para não acionar o renderCycle!
let pincel = {
    isDrawing: false,
    lastX: 0.0,
    lastY: 0.0
};

// ⭐ Declaramos o 'ctx' global, mas começamos com 'null'
var ctx = null;

__ui_engine.loadStyles("""
    body {
        background: #0f172a;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100vh;
        font-family: 'Segoe UI', sans-serif;
        margin: 0;
    }
    canvas {
        background: #ffffff;
        border-radius: 12px;
        box-shadow: 0 10px 40px rgba(0,0,0,0.6);
        border: 2px solid #334155;
        cursor: crosshair;
    }
    .tools {
        margin-top: 20px;
        display: flex;
        gap: 10px;
    }
    button {
        padding: 10px 20px;
        border-radius: 8px;
        border: none;
        font-weight: bold;
        cursor: pointer;
        color: white;
    }
""");

let view = """
    <html>
        <body>
            <h2 style="color: #f8fafc; margin-bottom: 20px;">🎨 Quadro Interativo XPL</h2>

            <canvas id="quadro" width="700" height="450"
                    (mousedown)="comecarDesenho(event);"
                    (mousemove)="desenhar(event);"
                    (mouseup)="pararDesenho();"
                    (mouseout)="pararDesenho();">
            </canvas>

            <div class="tools">
                <button style="background: #ef4444;" (click)="limparTela();">Limpar Tela</button>
            </div>
        </body>
    </html>
""";

fun comecarDesenho(e: XplEvent) {
    pincel.isDrawing = true;
    pincel.lastX = e.detail.x;
    pincel.lastY = e.detail.y;
}

fun desenhar(e: XplEvent) {
    if (!pincel.isDrawing) {
        return;
    }

    // ⭐ FIM DO DESPERDÍCIO!
    // Já não fazemos getElementById nem getContext aqui a 60FPS.
    // Usamos diretamente o 'ctx' que já foi guardado globalmente!

    ctx.beginPath();
    ctx.setStrokeStyle("#3b82f6");
    ctx.setLineWidth(5);
    ctx.setLineCap("round");

    ctx.moveTo(pincel.lastX, pincel.lastY);

    let atualX = e.detail.x;
    let atualY = e.detail.y;

    ctx.lineTo(atualX, atualY);
    ctx.stroke();
    ctx.closePath();

    pincel.lastX = atualX;
    pincel.lastY = atualY;
}

fun pararDesenho() {
    pincel.isDrawing = false;
}

fun limparTela() {
    // Usamos o ctx global aqui também!
    ctx.clearRect(0, 0, 700, 450);
}

// ─── INICIALIZAÇÃO ───────────────────────────────────────────────────
__ui_engine.loadView(view);

// ⭐ O TRUQUE DE MESTRE DA OTIMIZAÇÃO ⭐
// Agora que o 'loadView' já correu, o DOM já existe!
// Podemos capturar o Canvas UMA ÚNICA VEZ e guardar na variável global 'ctx':
ctx = document.getElementById("quadro").getContext("2d");

__ui_engine.showWindow("SuperUI - Paint", 900, 700);