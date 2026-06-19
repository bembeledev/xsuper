println(">>> A INICIAR MOTOR DE DADOS <<<", "#00FFFF");

interface CRUD {
    add(nome: string);
    delete(id: int);
    getId();
}


declare Animal {
    pub nome: string;
    pub especie: string;
    priv id: int;
}

declare Mamifero extends Animal {
    pub fun localizacao: string;
}


abstract implement Animal {
    pub fun verIdade():int {
        println("Tenho "+ this.id+" anos");
        return this.id;
    }
    pub abstract fun acasalamento(): string;
}

implement Animal as Fish {
    pub verIdade():int {
        println("Tenho "+ this.id+" anos");
        return this.id;
    }
    pub abstract fun acasalamento(): string;
}

implement Mamifero as Mam1 for CRUD {
    pub fun add(nome: string) {
        // Agora o objeto sabe o seu próprio nome!
        println("A guardar o animal [" + this.nome + "] na Base de Dados A...");
    }

    pub fun delete(id: int) {
        println("A eliminar " + this.nome + " com o id: " + id);
    }
    pub fun getId():int{return this.verIdade();}
}

implement Mamifero as Mam2 for CRUD {
     fun add() { println("A guardar Mam2 na Base de Dados B..."); }
     fun delete(id: int) { println("A eliminar Mam1 com id: "+ id); }
     fun getId():int{return this.id;}
}

var m = new Mam1();

var m2 = new Mam2();

// O visitSetExpr entra em ac    ção!
m.nome = "Leão";
m.especie = "Panthera leo";
m.localizacao = "Savana Africana";
m.id = 12;
// O visitGetExpr entra em ação!
println("Nome: "+ m.nome);
println("Local: "+ m.localizacao);
m2.id = 21;
println(m.getId()*m2.getID());
m.add(12);
m.delete(100);
// O visitGetExpr também vai encontrar o método add(), mas a Invocação... falaremos a seguir!