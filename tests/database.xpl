// SQLite (sem user/pass)
let conn = db_connect("jdbc:sqlite:meu_banco.db");

conn.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, age INTEGER)");

conn.execute("INSERT INTO users (name, age) VALUES (?, ?)", ["Alice", 30]);
conn.execute("INSERT INTO users (name, age) VALUES (?, ?)", ["Bob", 25]);

let users = conn.query("SELECT * FROM users WHERE age > ?", [20]);
for u in users {
    println(u.id + ": " + u.name + " (" + u.age + " anos)");
}

// Transação
conn.transaction(() => {
    conn.execute("UPDATE users SET age = age + 1 WHERE name = ?", ["Alice"]);
    conn.execute("DELETE FROM users WHERE name = ?", ["Bob"]);
});

conn.close();