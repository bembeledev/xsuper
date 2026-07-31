// ============================================================
// 1. ESTADO GLOBAL
// ============================================================
var currentUser = null;
var currentView = "login";

// ============================================================
// 2. ESTILOS GLOBAIS DA APLICAÇÃO
// ============================================================
__ui_engine.loadStyles("""
    body {
        font-family: "Segoe UI", sans-serif;
        background: #f0f2f5;
        padding: 20px;
        color: #1e293b;
    }
    #app {
        max-width: 800px;
        margin: 0 auto;
    }
    header {
        background: white;
        padding: 15px 20px;
        border-radius: 8px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        margin-bottom: 20px;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    #user-info {
        font-weight: bold;
    }
    .card {
        max-width: 300px;
        margin: 0 auto;
        padding: 20px;
        background: white;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        box-shadow: 0 4px 6px rgba(0,0,0,0.05);
    }
    .card-wide {
        max-width: 600px;
        margin: 0 auto;
        padding: 20px;
        background: white;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        box-shadow: 0 4px 6px rgba(0,0,0,0.05);
    }
    input {
        width: 100%;
        padding: 8px;
        margin: 5px 0;
        border: 1px solid #cbd5e1;
        border-radius: 4px;
        box-sizing: border-box;
    }
    button {
        width: 100%;
        padding: 10px;
        background: #3b82f6;
        color: white;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        font-weight: bold;
    }
    button:hover {
        background: #2563eb;
    }
    .btn-danger {
        background: #ef4444;
    }
    .btn-danger:hover {
        background: #dc2626;
    }
    .error-msg {
        color: #ef4444;
        text-align: center;
        font-size: 14px;
        margin-top: 5px;
    }
""");

// ============================================================
// 3. VIEWS (CONTEÚDO PRINCIPAL)
// ============================================================
fun loginContent():string {
    return """
    <div class="card">
        <h1 style="text-align: center; margin-top: 0;">🔐 Login</h1>
        <input id="username" type="text" placeholder="Usuário" />
        <input id="password" type="password" placeholder="Senha" />
        <button (click)="doLogin();">Entrar</button>
        <p id="msg" class="error-msg"></p>
        <hr style="border: 0; border-top: 1px solid #e2e8f0; margin: 15px 0;" />
        <p style="font-size: 12px; text-align: center; color: #64748b; margin-bottom: 0;">
            admin / 123 &nbsp;|&nbsp; usuario / 456 &nbsp;|&nbsp; convidado / 789
        </p>
    </div>
    """;
}

fun adminContent():string {
    return """
    <div class="card-wide">
        <h1>🔐 Painel Administrativo</h1>
        <p>Bem-vindo, <strong>Admin</strong>!</p>
        <p>Esta área contém dados sensíveis e controlo total do sistema.</p>
        <button class="btn-danger" (click)="logout();" style="width: auto; padding: 10px 20px;">Sair</button>
    </div>
    """;
}

fun userContent():string {
    return """
    <div class="card-wide">
        <h1>👤 Painel do Usuário</h1>
        <p>Bem-vindo, <strong>Usuário</strong>!</p>
        <p>Esta é a sua área pessoal de trabalho.</p>
        <button class="btn-danger" (click)="logout();" style="width: auto; padding: 10px 20px;">Sair</button>
    </div>
    """;
}

fun guestContent():string{
    return """
    <div class="card-wide">
        <h1>👋 Painel Convidado</h1>
        <p>Bem-vindo, <strong>Convidado</strong>!</p>
        <p>Acesso limitado a conteúdos públicos.</p>
        <button class="btn-danger" (click)="logout();" style="width: auto; padding: 10px 20px;">Sair</button>
    </div>
    """;
}

// ============================================================
// 4. ATUALIZAÇÃO DO CABEÇALHO
// ============================================================
fun updateHeader() {
    let userInfo = document.getElementById("user-info");
    if (userInfo != null) {
        userInfo.textContent = (currentUser != null) ? "👤 " + currentUser : "Desconectado";
    }
}

// ============================================================
// 5. LÓGICA DE NAVEGAÇÃO
// ============================================================
// ============================================================
// 5. LÓGICA DE NAVEGAÇÃO
// ============================================================
fun navigateTo(viewName: string) {
    currentView = viewName;

    let content = "";
    if (viewName == "login") {
        content = loginContent();
    } else if (viewName == "admin") {
        content = adminContent();
    } else if (viewName == "user") {
        content = userContent();
    } else if (viewName == "guest") {
        content = guestContent();
    } else {
        println("View desconhecida: " + viewName);
        return;
    }

    let mainElement = document.getElementById("main-content");

    if (mainElement != null) {
        // ⭐ MAGIA CIRÚRGICA: O EventBus e o JavaFxRenderer tratam de tudo!
        mainElement.innerHTML = content;
        println(document.getInnerHTML());
        updateHeader();
    } else {
        // ⭐ FALLBACK: Só recarrega tudo do zero se a tela estiver corrompida/vazia
        __ui_engine.loadView(getFullPage(content));
        __ui_engine.renderCycle();
    }
}

// ============================================================
// 6. AUTENTICAÇÃO
// ============================================================
fun doLogin() {
    let username = document.getElementById("username").value;
    let password = document.getElementById("password").value;
    let msg = document.getElementById("msg");

    if (username == "admin" && password == "123") {
        currentUser = "admin";
        msg.textContent = "";
        navigateTo("admin");
    } else if (username == "usuario" && password == "456") {
        currentUser = "usuario";
        msg.textContent = "";
        navigateTo("user");
    } else if (username == "convidado" && password == "789") {
        currentUser = "convidado";
        msg.textContent = "";
        navigateTo("guest");
    } else {
        msg.textContent = "❌ Credenciais inválidas!";
    }
}

// ============================================================
// 7. LOGOUT
// ============================================================
fun logout() {
    currentUser = null;
    navigateTo("login");
}

// ============================================================
// 8. GERADOR DA PÁGINA COMPLETA
// ============================================================
fun getFullPage(contentHtml: string):string {
    return """
    <html>
        <head></head>
        <body>
            <div id="app">
                <header>
                    <span>🏠 Sistema de Login Nativo</span>
                    <span id="user-info">Desconectado</span>
                </header>
                <main id="main-content">
                   """+contentHtml+"""
                </main>
            </div>
        </body>
    </html>
    """;
}

// ============================================================
// 9. INICIALIZAÇÃO DO SISTEMA
// ============================================================
__ui_engine.loadView(getFullPage(loginContent()));
__ui_engine.showWindow("Sistema de Autenticação XPL", 800, 600);