
module files;

// 1. Ligar ao Banco de Dados (Vamos usar SQLite para simplificar o exemplo)
var db = Database.connect("jdbc:sqlite:metadados.db");

// 2. Criar a Tabela de Pastas (Para controlar o limite de 50MB e 50 arquivos)
db.execute("""
    CREATE TABLE IF NOT EXISTS pastas (
        id TEXT PRIMARY KEY,
        nome TEXT,
        tamanho_total INTEGER DEFAULT 0,
        qtd_arquivos INTEGER DEFAULT 0
    )
""");

// 3. Criar a Tabela de Arquivos
db.execute("""
    CREATE TABLE IF NOT EXISTS arquivos (
        id TEXT PRIMARY KEY,
        pasta_id TEXT,
        nome TEXT,
        url TEXT,
        size INTEGER,
        tipo TEXT,
        status TEXT,         -- 'PENDENTE', 'UPLOADING', 'CONCLUIDO'
        bytes_recebidos INTEGER DEFAULT 0,
        FOREIGN KEY(pasta_id) REFERENCES pastas(id)
    )
""");

println("✅ Banco de Dados inicializado com sucesso!");

export db;