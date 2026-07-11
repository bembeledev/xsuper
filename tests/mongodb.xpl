println("=== TESTE MONGODB ===");

// Liga ao MongoDB (localhost)
var conn = mongo_connect("mongodb://localhost:27017");
var db = conn.getDatabase("teste_xpl");
var coll = db.getCollection("users");

// Insere um documento
var result = coll.insertOne({ "name": "Alice", "age": 30, "city": "Maputo" });
println("Inserido ID: " + result.insertedId);

// Insere vários
coll.insertMany([
    { "name": "Bob", "age": 25, "city": "Nampula" },
    { "name": "Carlos", "age": 35, "city": "Beira" }
]);

// Consulta com filtro
var users = coll.find({ "age": { "$gt": 28 } });
println("\nUtilizadores com idade > 28:");
while (users.hasNext()) {
    let u = users.next();
    println("  " + u.name + " (" + u.age + " anos)");
}
users.close();

// Update
var updated = coll.updateOne(
    { "name": "Alice" },
    { "$set": { "age": 31 } }
);
println("\nAtualizados: " + updated.matchedCount + " documento(s)");

// Delete
var deleted = coll.deleteOne({ "name": "Bob" });
println("Eliminados: " + deleted + " documento(s)");

// Agregação
var pipeline = [
    { "$match": { "age": { "$gt": 25 } } },
    { "$group": { "_id": "$city", "total": { "$sum": 1 } } }
];
var results = coll.aggregate(pipeline);
println("\nAgregação (cidades com > 25 anos):");
while (results.hasNext()) {
    let r = results.next();
    println("  " + r._id + ": " + r.total);
}
results.close();

// Fecha ligação
conn.close();
println("\nFim.");