module files.controller;

import files.http.Http.Http;
import files.controller.Controller.Controller;
import files.views.view.View;

declare HomeController extends Controller {}


implement HomeController {

    pub fun init(http: Http) {
        super.init(http);

        let method: ?string = http.req.request.method;
        http.res.response.body = switch (method) {
            case "GET": View.load("home");
            default: {
                http.res.response.status = 404;
                View.load("notfound");
            }
        };
    }
}

export HomeController;