module files.controller;

import files.http.Http.Http;
import files.listeners.WebFilter.WebFilter;

&WebFilter()
declare Controller {
    pub http: Http;
}

implement Controller {
    pub fun init(http: Http) {
        this.http = http;
    }
}

export Controller;