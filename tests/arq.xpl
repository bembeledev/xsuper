
let resultado:string = shell("ps");
var dados = resultado.split("\n");

println("Resultado: "+typeof(resultado));
let i:int =0;
for a in dados {
   if(i%2===0){
      println(a.split("|").last.trim(),"#FF224F");
   }else{
      println(a.split("|").last.trim(),"#22FFF4");
   }
   i++;
}