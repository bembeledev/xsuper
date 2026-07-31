__ui_engine.loadStyles("""
    body {
        background-color: #f8fafc;
    }

    table {
        border-collapse: separate;
        border-spacing: 0;
        width: 100%;
        font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
        font-size: 14px;
        margin: 24px 0;
        border-radius: 12px;
        overflow: hidden;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
        background: white;
    }

    /* As células obedecem naturalmente ao padding e border W3C */
    th, td {
        padding: 14px 20px;
        text-align: left;
        border-bottom: 1px solid #f1f5f9;
        transition: background 0.2s ease;
    }

    th {
        background: linear-gradient(135deg, #6366f1, #8b5cf6);
        color: white;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.03em;
        font-size: 0.8rem;
        border-bottom: 2px solid #4f46e5;
        text-align: center;
    }

    /* O motor Sizzle deteta isto e o TableTag traduz para a TableRow! */
    tbody tr:nth-child(even) {
        background-color: #f8fafc;
    }

    tbody tr:hover {
        background-color: #eef2ff;
        cursor: pointer;
    }

    tfoot td {
        background-color: #f1f5f9;
        font-weight: 600;
        text-align: center;
        color: #1e293b;
        border-top: 2px solid #e2e8f0;
    }
""");

let view = """
    <html>
        <body>
            <h1 style="color: #0f172a; font-weight: 700; margin-bottom: 0.25rem;">📊 Tabela Moderna</h1>
            <p style="color: #64748b; margin-bottom: 32px;">Com gradiente, sombra e efeitos suaves</p>

            <table id="table-data" sortable="true" columns-menu="true" selection="multiple" draggable="true">
                <thead>
                    <tr>
                        <th>Nome</th>
                        <th>Idade</th>
                        <th>País</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td style="font-weight: bold;">João Silva</td>
                        <td>25</td>
                        <td>🇧🇷 Brasil</td>
                    </tr>
                    <tr>
                        <td style="font-weight: bold;">Maria Santos</td>
                        <td>30</td>
                        <td>🇵🇹 Portugal</td>
                    </tr>
                    <tr>
                        <td style="font-weight: bold;">Pedro Costa</td>
                        <td>42</td>
                        <td>🇦🇴 Angola</td>
                    </tr>
                </tbody>
                <tfoot>
                    <tr>
                        <td colspan="3">Total de registos: 3 pessoas</td>
                    </tr>
                </tfoot>
            </table>
        </body>
    </html>
""";



var table = document.getElementById("table-data");
println(table);

__ui_engine.loadView(view);
__ui_engine.showWindow("SuperUI - Tabela Moderna", 800, 600);



