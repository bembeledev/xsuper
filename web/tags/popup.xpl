/* ========================================================
 * Exemplo prático: popup.xpl
 * Demonstração do componente <popup> com ancoragem e timeout
 * ======================================================== */

// 1. O Estado
var isPopupOpen = false;

// 2. Ação de Abertura
fun abrirPopup() {
    isPopupOpen = true;
    println("A abrir o popup flutuante...");

    // Vai buscar a Tag <popup> ao DOM e aciona o atributo reativo "show"
    let meuPopup = document.getElementById("balao-info");
    meuPopup.setAttribute("show", true);
}

// 3. Ação de Fecho (Invocada pelo botão ou pelo timeout de 3 segundos)
fun fecharPopup() {
    isPopupOpen = false;
    println("O popup foi fechado!");

    let meuPopup = document.getElementById("balao-info");
    meuPopup.setAttribute("show", false);
}

// 4. A Vista (W3C HTML + XPL Bindings)
var view = """
    <div style="padding: 50px; display: flex; flex-direction: column; align-items: center; gap: 30px; font-family: 'Segoe UI', sans-serif; background: #f8fafc; height: 100vh;">

        <h2 style="color: #0f172a; margin: 0;">Demonstração de Popups XPL</h2>
        <p style="color: #64748b; margin: 0;">Clica no botão para revelar um balão flutuante. Ele fechará sozinho após 3 segundos.</p>

        <!-- BOTÃO ÂNCORA -->
        <button id="btn-alvo" (click)="abrirPopup()" style="padding: 12px 24px; background: linear-gradient(to right, #3b82f6, #2563eb); color: white; border-radius: 8px; font-weight: bold; cursor: hand; border: none; box-shadow: 0 4px 6px rgba(37, 99, 235, 0.2);">
            Mostrar Informação 🎯
        </button>

        <!-- O POPUP -->
        <!-- anchor="btn-alvo": O radar local vai colar este balão ao botão -->
        <!-- timeout="3000": O JavaFX vai emitir o evento "close" passado 3s -->
        <!-- (close)="fecharPopup()": O XPL escuta o fecho e limpa o estado -->
        <popup id="balao-info" anchor="btn-alvo" timeout="3000" (close)="fecharPopup()">
            <div style="background: #1e293b; color: #f8fafc; padding: 16px; border-radius: 10px; box-shadow: 0px 10px 25px rgba(0,0,0,0.3); width: 220px; border: 1px solid #334155;">
                <h4 style="margin: 0 0 8px 0; color: #38bdf8;">Dica Rápida 💡</h4>
                <p style="margin: 0 0 12px 0; font-size: 13px; line-height: 1.5; color: #cbd5e1;">
                    Eu sou um popup perfeitamente ancorado! Fui desenhado de forma totalmente assíncrona.
                </p>
                <button (click)="fecharPopup()" style="padding: 6px 12px; background: #ef4444; color: white; border: none; border-radius: 4px; cursor: hand; width: 100%;">
                    Fechar Agora
                </button>
            </div>
        </popup>
        <!-- Vai abrir SOZINHO ao fim de 2 segundos (delay), e FECHAR sozinho 4 segundos depois (timeout) -->
        <popup id="notificacao" delay="2000" timeout="4000">
            <div style="background: #22c55e; color: white; padding: 12px 24px; border-radius: 8px; box-shadow: 0 4px 15px rgba(34,197,94,0.4);">
                <strong>Sucesso!</strong> Dados guardados com sucesso na Base de Dados.
            </div>
        </popup>

    </div>
""";

// 5. Injeção no Motor de Renderização
//document.body.innerHTML = view;
__ui_engine.loadView(view);
__ui_engine.showWindow("Aplicação Desktop Nativa", 800, 600);
