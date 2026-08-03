declare Route{
    priv method: string;
    priv path: string;
}

implement Route {
    pub fun init(method: "get",path: "/"){
         this.method = method;
         this.path = path;
    }

    pub fun setMethod(method:string){
        this.method = method;
    }

    pub fun setPath(path:string){
        this.path = path;
    }

    pub fun getMethod(){
        return this.method;
    }

    pub fun getPath(){
        return this.path;
    }
}
