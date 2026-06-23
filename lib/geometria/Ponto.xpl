module geometria;

export declare Ponto {
    pub x: int;
    pub y: int;
}

implement Ponto {
    pub fun init(x: int, y: int) {
        this.x = x;
        this.y = y;
    }
}