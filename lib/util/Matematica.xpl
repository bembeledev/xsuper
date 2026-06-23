module util;

fun somar(a: int, b: int): int {
    return a + b;
}

fun multiplicar(a: int, b: int): int {
    return a * b;
}

var PI = 3.14159;

// O motor quântico tem de empacotar exatamente estes 3 símbolos:
export somar, multiplicar, PI;