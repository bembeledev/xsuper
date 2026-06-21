println(">>> TESTE DE MAPAS FORTEMENTE TIPADOS <<<", "#00FFFF");

// A sintaxe gloriosa do Arquiteto:
let registos= new Map<String, Integer> ();

// Como devolve um Map nativo, os teus ObjectMethods assumem o controlo automaticamente!
registos.set("users", 150);
registos.set("admins", 5);

println("Total de Users: " + registos.get("users"));
println("Chaves do Mapa: " + registos.keys().join(", "));
println("O mapa está vazio? " + registos.isEmpty);

// O XPL aceita a sintaxe clássica do Java/TypeScript!
let registos = new Map<String, Integer>();

registos.set("users", 150);
registos.set("admins", 5);

println("Map criado com generics!");
println("Users: " + registos.get("users"));