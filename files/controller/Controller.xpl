module files.controller;

import files.http.Http.Http;

declare Controller {
    pub http: Http;
}

implement Controller {
    pub fun init(http: Http) {
        this.http = http;
    }
}

export Controller;