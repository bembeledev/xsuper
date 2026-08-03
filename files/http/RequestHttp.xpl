module files.http;

declare RequestHttp {
    pub readonly request: object;
}

implement RequestHttp {
    pub fun init(request: object){
        this.request = request;
    }
}

export RequestHttp;