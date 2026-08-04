sealed declare MotorAPI {
    pub cavalos: int;
    pub modelo: string;
}

implement MotorAPI {
    pub fun ligar() { println("Vrumm..."); }
    pub fun desligar() { println("Silêncio."); }
}

declare Motor {
    pub ola: int;
}

implement Motor {
    pub fun saudar() { println("Vrumm..."); }
}


MotorAPI::getDeclareName();
MotorAPI::getMethods();
MotorAPI::getFields();
MotorAPI::CallMethod();
MotorAPI::getImplements();
MotorAPI::getInterfaces();
MotorAPI::getDecorators();
MotorAPI::getDecorators();
MotorAPI::getDeclareToObject(); // rotornar esse objecto:
MotorAPI::getImplementsAliasNames();
//mais propriedades...

var meuMotor = new MotorAPI();

var method = new Method();
method.name ="power";

method.visibility = VISIBILITY.PUB;
method.returnType =RETURN.INT;
var par = new Param<int>();
par.name = "id";
par.typeParam = PARAM.INT;
par.defaultValue = Param.ParamValue(12);
method.injectParam(par);

var bloc: Block = {if (this.modelo == id) return id**8;}

method.block(block);
meuMotor::injectMethod(method);
meuMotor.power(12);
meuMotor::injectMethod(Motor::getMethod("saudar"));
meuMotor.saudar();

meuMotor::CallMethod(/*coisas assim*/);
meuMotor::getDecorators();
MotorAPI::getDeclareName();

If(a instance Peixe,bloc).Else(block);



