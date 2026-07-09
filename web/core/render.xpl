module core;

export fun render(html:string){
    try {
        __ui_engine.loadView(html);
    }
    catch(e:Error){
        throw new Error();
    }
}