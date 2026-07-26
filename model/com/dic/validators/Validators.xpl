module model.com.dic.validators;

// ============================================================================
// 1. O GUARDA DE NÚMEROS (Limites de Bits)
// ============================================================================
listener ValidNumber {
    pub min: int;
    pub max: int;
}

abstract implement ValidNumber {
    pub fun init(min: int, max: int) {
        this.min = min;
        this.max = max;
    }

    @(Listen.Set)
    pub fun avaliar(novoValor: int) {
        if (novoValor < this.min || novoValor > this.max) {
            let err = new Error();
            err.message = "Erro de Estouro: O valor " + novoValor + " excede os limites [" + this.min + " a " + this.max + "] do tipo " + this.targetName;
            throw err;
        }
    }
}

// ============================================================================
// 2. O GUARDA DE STRINGS (Regex e Tamanhos)
// ============================================================================
listener ValidEmail {}

abstract implement ValidEmail {
    pub fun init() {}

    @(Listen.Set)
    pub fun avaliar(novoValor: string) {
        if (!novoValor.contains("@") || !novoValor.contains(".")) {
            let err = new Error();
            err.message = "Formato Inválido: '" + novoValor + "' não é um email válido.";
            throw err;
        }
    }
}

listener ValidPin {}
abstract implement ValidPin {
    pub fun init() {}
    @(Listen.Set)
    pub fun avaliar(novoValor: string) {
        // Usa o novo método isNumeric e o length que já tinhas!
        if (!novoValor.isNumeric() || (novoValor.length != 4 && novoValor.length != 6)) {
            let err = new Error();
            err.message = "PIN inválido: Deve conter apenas números e ter 4 ou 6 dígitos.";
            throw err;
        }
    }
}

listener ValidPassword {}

abstract implement ValidPassword {
    pub fun init() {}

    @(Listen.Set)
    pub fun avaliar(novoValor: string) {
        if (novoValor.length < 8) {
            let err = new Error();
            err.message = "Senha Fraca: Mínimo de 8 caracteres exigidos.";
            throw err;
        }
    }
}

// ============================================================================
// 3. A IMPRESSORA 3D DE TIPOS (Type Aliases)
// ============================================================================

// Tipos Numéricos Seguros
&ValidNumber(min: -128, max: 127)
type Byte = int;

&ValidNumber(min: -32768, max: 32767)
type Short = int;

&ValidNumber(min: -2147483648, max: 2147483647)
type Integer = int;

// Tipos de Texto Seguros
&ValidEmail()
type Email = string;

&ValidPin()
type Pin = string;

&ValidPassword()
type Password = string;

println("🛡️ [com.dic.validators] Módulo carregado e matriz de segurança ativada!");

// Exigimos a exportação completa de tudo o que for aqui definido
export all;