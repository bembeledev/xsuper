module files.controller;

import files.http.Http.Http;
import files.controller.Controller.Controller;
import files.views.view.View;

declare UploadViewController extends Controller {}

implement UploadViewController {

    @Override
    pub fun init(http: Http) {
        super.init(http);

        let method: ?string = http.req.request.method;

        http.res.response.body = switch (method) {
            case "GET": View.load("upload");
            default: {
                http.res.response.status = 404;
                View.load("notfound");
            }
        };
    }
}

export UploadViewController;