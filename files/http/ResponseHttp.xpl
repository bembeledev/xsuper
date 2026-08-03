module files.http;

declare ResponseHttp {
    pub readonly response: object;
}

implement ResponseHttp{

    pub fun init(response: object){
        this.response = response;
    }
}

export ResponseHttp;