module files.http;
import files.http.RequestHttp.RequestHttp;
import files.http.ResponseHttp.ResponseHttp;

declare Http {
    pub readonly req: RequestHttp;
    pub readonly res: ResponseHttp;
}

implement Http {
    pub fun init(req: RequestHttp, res:ResponseHttp){
      this.req = req;
      this.res = res;
    }
}
export Http;