## 📦 Documentação do Módulo 13 – Sistema de Importações (Super)

O **Volume de Imports e Expoorts** implementa a modularidade da linguagem **Super**, permitindo organizar código em módulos, importar símbolos e controlar a visibilidade entre ficheiros.

---

### 🔤 Palavras‑chave

| Palavra       | Uso                                      |
|---------------|------------------------------------------|
| `module`      | Declara o caminho do módulo actual       |
| `import`      | Importa símbolos de outro módulo         |
| `as`          | Cria um alias (apelido) para um símbolo  |

> **Nota:** A palavra `from` **não é utilizada** na sintaxe actual.

---

### 📁 Estrutura de ficheiros

Os módulos seguem a estrutura de pastas do projecto.  
Um módulo `banco.modelos.Cliente` corresponde ao ficheiro:

```
src/banco/modelos/Cliente.xpl
```

O motor procura pelos seguintes directórios (por ordem):
1. `.` (directório actual)
2. `src/`
3. `lib/`

---

### 📝 Declaração de módulo (`module`)

**Todo ficheiro que pretende ser importado deve declarar o seu caminho na primeira linha.**

```xpl
// ficheiro: src/banco/modelos/Cliente.xpl
module banco.modelos;

export declare Pessoa {
    pub id: int;
    pub nome: string;
}

export declare Cliente {
    pub id: int;
    pub nome: string;
}

```
Ou 
```xpl
// ficheiro: src/banco/modelos/Cliente.xpl
module banco.modelos;

declare Pessoa {
    pub id: int;
    pub nome: string;
}

declare Cliente {
    pub id: int;
    pub nome: string;
}

export Cliente, Pessoa; // não importa onde pode ser colocado mas deve ser depois do module e das importações

```

Ou
```xpl
// ficheiro: src/banco/modelos/Cliente.xpl
module banco.modelos;

declare Pessoa {
    pub id: int;
    pub nome: string;
}

declare Cliente {
    pub id: int;
    pub nome: string;
}

export all; //<-exporta tudo, não importa onde pode ser colocado mas deve ser depois do module e das importações

```

A declaração `module` é obrigatória. Sem ela, o módulo **não pode ser importado** (erro de segurança).

---

### 📥 Sintaxes de importação

#### 1. Importar um único símbolo

```xpl
import banco.modelos.Cliente;

var c = new Cliente(1, "João");
```

#### 2. Importar múltiplos símbolos (com ou sem alias)

```xpl
import banco.modelos.{Cliente, Conta as ContaBancaria};

var c = new Cliente(1, "Ana");
var conta = new ContaBancaria(1001);
```

#### 3. Importar todos os símbolos públicos (`*`)

```xpl
import banco.modelos.*;

var c = new Cliente(2, "Carlos");
var conta = new Conta(2001);   // também pública
```

#### 4. Importar com alias (renomear)

```xpl
import banco.modelos.Cliente as Pessoa;

var p = new Pessoa(3, "Maria");
```

#### 5. Misto (vários símbolos com alias)

```xpl
import banco.modelos.{Cliente as Pessoa, Conta as ContaBancaria};
```

---

### 🔒 Regras de visibilidade

Apenas símbolos com precedencia de **export** são exportados e podem ser importados.

```xpl
module util.Matematica;

export fun somar(a:int, b:int):int { return a + b; }   // ✅ exportado
fun auxiliar() { }                              // ❌ não exportado
var versao = "1.0";                                     // ❌ não exportado (sem pub)

```
---

### ⚙️ Funcionamento interno

 1.** Resolução de caminho**  
   O nome do módulo (`banco.modelos.Cliente`) é convertido para um caminho de ficheiro:  
   `banco/modelos/Cliente.xpl`. A procura ocorre nos directórios configurados.

2.** Carregamento e cache**
    - O módulo é carregado uma única vez.
    - Se for importado novamente, o ambiente em cache é reutilizado.
    - O cabeçalho `module` é validado (deve coincidir com o caminho esperado).

3.** Injecção de símbolos**  
   Os símbolos públicos (ou classes) são copiados para o ambiente de quem fez o `import`, respeitando os alias definidos.

---

### ⚠️ Restrições e boas práticas

- **Caminhos circulares** – O sistema não detecta ciclos. Evite importações mútuas.
- **Alias únicos** – Não é possível ter dois símbolos com o mesmo alias no mesmo escopo.
- **Módulo ≡ ficheiro** – Cada ficheiro `.xpl` define exactamente um módulo.
- **Símbolos com overload** – Apenas a primeira sobrecarga é importada (limitação actual).
- **Recomendação:** Use `import` específico em vez de `*` para evitar poluição do espaço de nomes.

---

### 🧪 Exemplo completo

**Ficheiro `src/geometria/Ponto.xpl`**
```xpl
module geometria;

export declare Ponto {
    pub x: int;
    pub y: int;
}
```

**Ficheiro `src/geometria/Circulo.xpl`**
```xpl
module geometria;

export declare Circulo {
    pub raio: float;
}
```

**Ficheiro `src/main.xpl`**
```xpl
module app;

import geometria.Ponto;
import geometria.{Circulo as Circ};

var p = new Ponto(10, 20);
var c = new Circ(5.5);
println(p.x);   // 10
println(c.raio); // 5.5
```

---

### 🔮 Desenvolvimentos futuros (planeados)

- Detecção de dependências circulares.
- Suporte a `import modulo.* as M` (namespace).
- Resolução de caminhos configurável (ficheiro de projecto).
- Importação de múltiplas sobrecargas de funções.

---

Esta documentação reflecte a implementação **actual** do Volume 13. Para mais detalhes, consulte o código fonte em `src/modules/v13_modularity/`.