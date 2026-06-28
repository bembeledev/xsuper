
var html = """
<html>
    <body>
        <button id="12" (click)="login();logout();"></button>
    </body>
</html>
""";


var btn = document.getElementById("12");
fun login(){println("Entrou no sistema");}
fun logout(){println("Saiu no sistema");}
btn.callAction(Action.CLICK);