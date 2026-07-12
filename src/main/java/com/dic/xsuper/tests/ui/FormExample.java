package com.dic.xsuper.tests.ui;

import com.dic.xsuper.lang.ui.html.XplNode;

/**
 * Exemplo de formulário XPL com validação, convertido para XplNode.
 * <p>
 * Equivalente ao HTML/XPL:
 * <pre>
 * &lt;form action="/api/registar" method="post" onsubmit="validarFormulario"&gt;
 *     &lt;input type="text" name="nome" required minlength="3" placeholder="Nome"&gt;
 *     &lt;input type="email" name="email" required pattern="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"&gt;
 *     &lt;input type="checkbox" name="interesses" value="codar"&gt; Codar
 *     &lt;input type="checkbox" name="interesses" value="musica"&gt; Música
 *     &lt;button type="submit"&gt;Enviar&lt;/button&gt;
 * &lt;/form&gt;
 *
 * fun validarFormulario(dados) {
 *     println("A validar: " + dados);
 *     return true;
 * }
 * </pre>
 */
public class FormExample {

    /**
     * Cria a árvore XplNode para o formulário.
     * @return nó raiz (&lt;form&gt;)
     */
    public static XplNode createFormExample() {
        // ─── Nó <form> ──────────────────────────────────────────────────────
        XplNode form = new XplNode("form");
        form.attributes.put("action", "/api/registar");
        form.attributes.put("method", "post");
        form.attributes.put("onsubmit", "validarFormulario");
        form.attributes.put("style",
                "display: flex; flex-direction: column; gap: 12px; " +
                        "padding: 20px; background: #1e293b; border-radius: 8px; " +
                        "max-width: 400px; margin: 20px auto;"
        );

        // ─── Input: nome ────────────────────────────────────────────────────
        XplNode inputNome = new XplNode("input");
        inputNome.attributes.put("type", "text");
        inputNome.attributes.put("name", "nome");
        inputNome.attributes.put("required", "true");
        inputNome.attributes.put("minlength", "3");
        inputNome.attributes.put("placeholder", "Nome");
        inputNome.attributes.put("style",
                "padding: 10px; border-radius: 4px; border: 1px solid #475569; " +
                        "background: #0f172a; color: #e2e8f0; font-size: 14px;"
        );
        form.addChild(inputNome);

        // ─── Input: email ──────────────────────────────────────────────────
        XplNode inputEmail = new XplNode("input");
        inputEmail.attributes.put("type", "email");
        inputEmail.attributes.put("name", "email");
        inputEmail.attributes.put("required", "true");
        inputEmail.attributes.put("pattern", "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        inputEmail.attributes.put("style",
                "padding: 10px; border-radius: 4px; border: 1px solid #475569; " +
                        "background: #0f172a; color: #e2e8f0; font-size: 14px;"
        );
        form.addChild(inputEmail);

        // ─── Checkbox: Codar ───────────────────────────────────────────────
        XplNode wrapper1 = createCheckboxWrapper("interesses", "codar", "Codar", false);
        form.addChild(wrapper1);

        // ─── Checkbox: Música ──────────────────────────────────────────────
        XplNode wrapper2 = createCheckboxWrapper("interesses", "musica", "Música", false);
        form.addChild(wrapper2);

        // ─── Botão submit ──────────────────────────────────────────────────
        XplNode button = new XplNode("button");
        button.attributes.put("type", "submit");
        button.textContent = "Enviar";
        button.attributes.put("style",
                "padding: 10px 20px; background: #3b82f6; color: white; " +
                        "border: none; border-radius: 4px; font-weight: 600; " +
                        "cursor: pointer; transition: background 0.2s;"
        );
        form.addChild(button);

        return form;
    }

    /**
     * Cria um wrapper para checkbox com label.
     */
    private static XplNode createCheckboxWrapper(String name, String value, String label, boolean checked) {
        XplNode wrapper = new XplNode("div");
        wrapper.attributes.put("style", "display: flex; align-items: center; gap: 8px;");

        XplNode checkbox = new XplNode("input");
        checkbox.attributes.put("type", "checkbox");
        checkbox.attributes.put("name", name);
        checkbox.attributes.put("value", value);
        if (checked) checkbox.attributes.put("checked", "true");
        checkbox.attributes.put("style", "width: 18px; height: 18px; accent-color: #3b82f6;");
        wrapper.addChild(checkbox);

        XplNode lbl = new XplNode("label");
        lbl.textContent = label;
        lbl.attributes.put("style", "color: #e2e8f0; font-size: 14px; cursor: pointer;");
        wrapper.addChild(lbl);

        return wrapper;
    }

    /**
     * Exemplo de função XPL associada (em comentário, para referência).
     * <pre>
     * fun validarFormulario(dados) {
     *     println("A validar: " + dados);
     *     return true; // permite submissão
     * }
     * </pre>
     */
    public static String getValidationFunction() {
        return "fun validarFormulario(dados) {\n" +
                "    println(\"A validar: \" + dados);\n" +
                "    return true;\n" +
                "}";
    }

    /**
     * Exemplo de uso no motor UI.
     */
    public static void demo() {
        XplNode form = createFormExample();
        String validationFn = getValidationFunction();
        // Para registar a função no interpretador XPL, usa:
        // interpreter.interpret(validationFn);
        // E depois renderiza o formulário:
        // __ui_engine.loadView(form);
    }
}