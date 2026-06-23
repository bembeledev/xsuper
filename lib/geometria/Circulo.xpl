module geometria;

declare Circulo {
    pub raio: float;
    pub pi: float;
}

implement Circulo {
    pub fun init(r: float) {
        this.raio = r;
    }

    pub fun area(): float {
        return 3.1415 * this.raio * this.raio;
    }
}

implement Circulo as Circe {
    pub fun init(r: float, pi: float) {
        this.raio = r;
        this.pi = pi;
    }

    pub fun area(): float {
        return this.pi * this.raio * this.raio;
    }
}

// Exporta absolutamente tudo o que estiver neste ficheiro!
export all;