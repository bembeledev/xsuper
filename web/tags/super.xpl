__ui_engine.loadStyles("""
        /* ===== RESET & VARIÁVEIS ===== */
        *, *::before, *::after {
            margin: 0; padding: 0; box-sizing: border-box;
        }

        :root {
            --bg-primary: #f6f8fc;
            --bg-card: #ffffff;
            --bg-sidebar: #0f172a;
            --bg-sidebar-hover: #1e293b;
            --text-primary: #0f172a;
            --text-secondary: #475569;
            --text-muted: #94a3b8;
            --text-sidebar: #e2e8f0;
            --text-sidebar-muted: #94a3b8;
            --accent: #6366f1;
            --accent-light: #eef2ff;
            --accent-dark: #4f46e5;
            --green: #22c55e;
            --green-light: #dcfce7;
            --orange: #f59e0b;
            --orange-light: #fef3c7;
            --red: #ef4444;
            --red-light: #fee2e2;
            --border: #e2e8f0;
            --shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
            --radius: 16px;
            --radius-sm: 10px;
            --transition: 0.25s ease;
            --sidebar-width: 260px;
            --header-height: 72px;
        }

        /* ===== BODY (HBox Base) ===== */
        body {
            font-family: 'Segoe UI', system-ui, sans-serif; /* Fontes seguras do JavaFX */
            background: var(--bg-primary);
            color: var(--text-primary);

            /* ⭐ Motor XPL: Converte para HBox ocupando todo o ecrã */
            display: flex;
            flex-direction: row;
            width: 100vw;
            height: 100vh;
            overflow: hidden; /* Impede scroll global, delegando para os filhos */
        }

        /* ===== SIDEBAR (VBox com Scroll Customizado) ===== */
        .sidebar {
            width: var(--sidebar-width);
            min-width: var(--sidebar-width);
            height: 100vh;
            background: var(--bg-sidebar);
            color: var(--text-sidebar);
            padding: 24px 16px;

            /* ⭐ Motor XPL: Converte para VBox */
            display: flex;
            flex-direction: column;

            /* ⭐ Motor XPL: Ativa ScrollPane com estilos customizados da NativeTag */
            overflow-y: auto;
            scrollbar-width: 6px;
            scrollbar-thumb: #475569;
            scrollbar-hover: #94a3b8;
            scrollbar-track: transparent;
        }

        .sidebar-brand {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 0 8px 32px 8px;
            border-bottom: 1px solid rgba(255, 255, 255, 0.06);
            margin-bottom: 24px;
        }

        .sidebar-brand .logo-icon {
            width: 40px; height: 40px;
            background: var(--accent);
            border-radius: var(--radius-sm);
            display: flex; align-items: center; justify-content: center;
            font-weight: bold; font-size: 20px; color: #ffffff;
        }

        .sidebar-brand h1 {
            font-size: 20px; font-weight: bold; color: #ffffff; margin: 0;
        }

        .sidebar-nav {
            flex: 1; /* Cresce verticalmente */
            display: flex; flex-direction: column; gap: 4px;
        }

        .sidebar-nav .nav-label {
            font-size: 12px; font-weight: bold; color: var(--text-sidebar-muted);
            padding: 16px 8px 8px 8px;
        }

        .sidebar-nav a {
            display: flex; align-items: center; gap: 14px;
            padding: 10px 14px; border-radius: var(--radius-sm);
            color: var(--text-sidebar-muted); font-size: 14px; font-weight: bold;
            transition: all var(--transition);
        }

        .sidebar-nav a:hover {
            background: var(--bg-sidebar-hover); color: #ffffff;
        }

        .sidebar-nav a.active {
            background: var(--accent); color: #ffffff;
            box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35); /* Traduzido p/ DropShadow */
        }

        .sidebar-footer {
            margin-top: auto; padding-top: 20px;
            border-top: 1px solid rgba(255, 255, 255, 0.06);
        }

        .sidebar-footer .user-card {
            display: flex; align-items: center; gap: 12px;
            padding: 8px 10px; border-radius: var(--radius-sm);
        }

        .sidebar-footer .avatar {
            width: 40px; height: 40px; border-radius: 20px;
            background: var(--accent); color: #ffffff;
            display: flex; align-items: center; justify-content: center;
            font-weight: bold;
        }

        /* ===== MAIN CONTENT (VBox Flexível) ===== */
        .main {
            /* ⭐ Motor XPL: Ativa HBox.setHgrow(ALWAYS) no body */
            flex: 1;
            height: 100vh;
            display: flex; flex-direction: column;
            background: var(--bg-primary);

            /* ⭐ Motor XPL: ScrollPane da área principal */
            overflow-y: auto;
            scrollbar-width: 8px;
            scrollbar-thumb: #cbd5e1;
            scrollbar-hover: #94a3b8;
            scrollbar-track: transparent;
        }

        /* ===== HEADER ===== */
        .header {
            height: var(--header-height); min-height: var(--header-height);
            background: rgba(255, 255, 255, 0.85);
            border-bottom: 1px solid var(--border);
            padding: 0 32px;
            display: flex; flex-direction: row; align-items: center; justify-content: space-between;

            /* ⭐ Motor XPL: Converte para javafx.scene.effect.BoxBlur nativo */
            backdrop-filter: blur(8px);
        }

        .header-left { display: flex; align-items: center; gap: 16px; }
        .header-left h2 { font-size: 18px; font-weight: bold; margin: 0; color: var(--text-primary); }
        .header-left h2 span { color: var(--text-muted); font-weight: normal; }

        .header-right { display: flex; align-items: center; gap: 12px; }
        .header-right .search-box {
            display: flex; align-items: center; gap: 10px;
            background: var(--bg-primary); border-radius: 20px;
            padding: 6px 16px; border: 1px solid var(--border);
        }
        .header-right .search-box input {
            border: none; background: transparent; padding: 6px 0; width: 180px;
        }

        /* ===== CONTENT & GRIDS ===== */
        .content { padding: 28px 32px 40px; flex: 1; display: flex; flex-direction: column; }

        /* ⭐ Motor XPL: Converte nativamente para GridPane */
        .stats-grid {
            display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 28px;
        }

        .stat-card {
            background: var(--bg-card); border-radius: var(--radius);
            padding: 20px 24px; border: 1px solid var(--border);
            box-shadow: var(--shadow);
            display: flex; flex-direction: column;
        }

        .stat-card .stat-label { font-size: 13px; font-weight: bold; color: var(--text-muted); }
        .stat-card .stat-value { font-size: 30px; font-weight: bold; margin-top: 4px; color: var(--text-primary); }

        .stat-footer { display: flex; align-items: center; gap: 8px; margin-top: 10px; }
        .stat-footer .trend-up { color: var(--green); background: var(--green-light); padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: bold; }
        .stat-footer .trend-down { color: var(--red); background: var(--red-light); padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: bold; }
        .stat-footer .trend-neutral { color: var(--orange); background: var(--orange-light); padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: bold; }
        .stat-footer .period { color: var(--text-muted); font-size: 12px; }

        .row-two { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; margin-bottom: 28px; }

        .card {
            background: var(--bg-card); border-radius: var(--radius); padding: 24px;
            box-shadow: var(--shadow); border: 1px solid var(--border);
            display: flex; flex-direction: column;
        }

        .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
        .card-header h3 { font-size: 16px; font-weight: bold; margin: 0; }
        .card-header .card-action { color: var(--accent); font-size: 13px; font-weight: bold; padding: 4px 12px; border-radius: 12px; }

        /* ===== CHART FAKE CSS ===== */
        .chart-bars { display: flex; align-items: flex-end; justify-content: space-between; height: 180px; gap: 8px; }
        .chart-bar-wrapper { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6px; height: 100%; justify-content: flex-end; }

        /* O JavaFX Region entende perfeitamente percentagens de altura! */
        .chart-bar { width: 100%; max-width: 40px; border-radius: 6px 6px 0 0; background: var(--accent); min-height: 8px; }
        .chart-bar-wrapper:nth-child(1) .chart-bar { height: 55%; background: #818cf8; }
        .chart-bar-wrapper:nth-child(2) .chart-bar { height: 78%; background: #6366f1; }
        .chart-bar-wrapper:nth-child(3) .chart-bar { height: 45%; background: #a5b4fc; }
        .chart-bar-wrapper:nth-child(4) .chart-bar { height: 92%; background: #4f46e5; }
        .chart-bar-wrapper:nth-child(5) .chart-bar { height: 63%; background: #818cf8; }
        .chart-label { font-size: 11px; color: var(--text-muted); font-weight: bold; }

        /* ===== ACTIVITY LIST ===== */
        .activity-list { display: flex; flex-direction: column; gap: 16px; }
        .activity-item { display: flex; align-items: flex-start; gap: 14px; }
        .activity-icon { width: 36px; height: 36px; border-radius: 18px; display: flex; align-items: center; justify-content: center; font-size: 16px; }
        .activity-icon.blue { background: var(--accent-light); color: var(--accent); }
        .activity-icon.green { background: var(--green-light); color: var(--green); }

        .activity-content { flex: 1; display: flex; flex-direction: column; }
        .activity-content .title { font-size: 14px; font-weight: bold; }
        .activity-content .desc { font-size: 13px; color: var(--text-muted); }
        .activity-time { font-size: 12px; color: var(--text-muted); }

        .row-bottom { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
""");

var view = """
<html lang="pt-BR">
<body>
    <!-- ===== SIDEBAR ===== -->
    <aside class="sidebar">
        <div class="sidebar-brand">
            <div class="logo-icon">◆</div>
            <h1>Nexus<span>.</span></h1>
        </div>

        <nav class="sidebar-nav">
            <div class="nav-label">Principal</div>
            <a href="#" class="active">
                <span class="nav-icon">📊</span> <span>Dashboard</span>
            </a>
            <a href="#">
                <span class="nav-icon">📈</span> <span>Analytics</span>
                <span class="badge">Novo</span>
            </a>
            <a href="#">
                <span class="nav-icon">📋</span> <span>Projetos</span>
            </a>
        </nav>

        <div class="sidebar-footer">
            <div class="user-card">
                <div class="avatar">AR</div>
                <div class="user-info">
                    <div class="name">Ana Ribeiro</div>
                    <div class="role">Administradora</div>
                </div>
            </div>
        </div>
    </aside>

    <!-- ===== MAIN ===== -->
    <div class="main">
        <header class="header">
            <div class="header-left">
                <h2>Dashboard <span>/ visão geral</span></h2>
            </div>
            <div class="header-right">
                <div class="search-box">
                    <span>🔍</span>
                    <input type="text" placeholder="Pesquisar..." />
                </div>
            </div>
        </header>

        <div class="content">
            <!-- STATS -->
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-label">Receita total</div>
                    <div class="stat-value">R$ 48.250</div>
                    <div class="stat-footer">
                        <span class="trend-up">▲ +12.5%</span>
                        <span class="period">vs. mês passado</span>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">Novos clientes</div>
                    <div class="stat-value">+284</div>
                    <div class="stat-footer">
                        <span class="trend-up">▲ +8.2%</span>
                        <span class="period">vs. mês passado</span>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">Taxa conversão</div>
                    <div class="stat-value">3.42%</div>
                    <div class="stat-footer">
                        <span class="trend-neutral">— 0.4%</span>
                        <span class="period">estável</span>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">Ticket médio</div>
                    <div class="stat-value">R$ 1.240</div>
                    <div class="stat-footer">
                        <span class="trend-down">▼ -2.1%</span>
                        <span class="period">vs. mês passado</span>
                    </div>
                </div>
            </div>

            <!-- ROW: CHART + ACTIVITY -->
            <div class="row-two">
                <div class="card">
                                    <div class="card-header">
                                        <h3>📊 Vendas (últimos dias)</h3>
                                    </div>
                                    <!-- ⭐ Aqui entra a superpotência da tua Engine! -->
                                    <div style="height: 180px; padding-top: 10px; width: 100%;">
                                        <chart type="bar"
                                               labels="01, 02, 03, 04, 05"
                                               data="{}"
                                               color="var(--accent)"
                                               style="width: 100%; height: 100%;">
                                        </chart>
                                    </div>
                                </div>

                <div class="card">
                    <div class="card-header">
                        <h3>🕒 Atividade recente</h3>
                    </div>
                    <div class="activity-list">
                        <div class="activity-item">
                            <div class="activity-icon blue">📄</div>
                            <div class="activity-content">
                                <span class="title">Novo projeto "EcoLabs"</span>
                                <span class="desc">Criado por Lucas M.</span>
                            </div>
                            <span class="activity-time">12 min</span>
                        </div>
                        <div class="activity-item">
                            <div class="activity-icon green">💰</div>
                            <div class="activity-content">
                                <span class="title">Pagamento recebido</span>
                                <span class="desc">R$ 3.200 — Cliente XPTO</span>
                            </div>
                            <span class="activity-time">1h atrás</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
""";

__ui_engine.loadView(view);
__ui_engine.showWindow("Dashboard Xplorer Native", 1200, 800);