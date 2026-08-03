module files.views;

declare View {
    // 1. Tornamos a variável estática para pertencer à Classe!
    pub static view: string;
}

implement View {
    default {
        // Inicializa o valor estático
        view: "files/views/templates/"
    }

    pub static fun load(templateName: string): string {
        try {
            // 2. Substituímos 'this' pelo nome da classe 'View'!
            return File.read(View.view + templateName + ".html");
        } catch(e: Error) {
            return File.read("files/views/templates/notfound.html");
        }
    }
}

export View;