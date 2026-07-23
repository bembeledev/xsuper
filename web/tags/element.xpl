declare Element extends XplElement {
    priv template: string;
    priv style: string;
}

implement Element {
    pub fun init(props:object) {
        // Inicializa com props, se necessário
        if (props.template!=null) {
            this.template = props.template;
        }
        if (props.style!=null) {
            this.style = props.style;
        }
    }

    pub fun setTemplate(template: string) {
        this.template = template;
        // Não chama loadView aqui
    }

    pub fun setStyle(style: string) {
        this.style = style;
    }

    pub fun render(): string {
        // Retorna o template (com suporte a style inline se quiser)
        return this.template;
    }
}

// Registrar o componente
__ui_engine.defineTag("Element", Element);

// Usar o componente numa view

var template = """
<div>Olá Mundo!</div>
""";

var temp = """<Element template={template} style="color:red;"/>""";

let html = """
    <html>
        <body>
            <Element template={template} style="color:red;"/>
            <Element template={template} style="color:red;"/>
        </body>
    </html>
""";
__ui_engine.loadView(html);

// BAM! A janela aparece aqui.
// O JavaFX arranca, o renderizador é injetado, e a vista já mastigada é pintada.
__ui_engine.showWindow("Aplicação Desktop Nativa", 800, 600);
println(document.getInnerHTML());