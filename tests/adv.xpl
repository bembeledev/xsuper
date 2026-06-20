declare Pessoa{
pub nome: string;
pub idade: int;
}

implement Pessoa {
    pub fun init(nome:string, idade:int){
        this.nome = nome;
        this.idade = idade;
    }

    pub fun getNome():string{ return this.nome;}
    pub fun getIdade():int{return this.idade;}

    pub fun setNome(nome:string){this.nome = nome;}
    pub fun setIdade(idade:int){this.idade = idade;}

    pub fun toString():string{
        return "Pessoa{nome:"+this.nome+", idade:"+this.idade+" }";
    }
}

var pessoas = [];

for i in (1,10){
    let p = new Pessoa("Nome_" + i, i*2);
    println(p);
    pessoas.push(p);
}


println(pessoas);

