package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.ast.TypeNode;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.core.TokenType;
import com.dic.xsuper.engine.exceptions.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;
import com.dic.xsuper.engine.poo.XplInstance;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

public class SmtpNativeModel {

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Smtp", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false; // Estático

        // =========================================================
        // ⭐ MOLDE DA INSTÂNCIA DO CLIENTE (SmtpClient) ⭐
        // =========================================================
        XPLModel clientModel = new XPLModel("SmtpClient", null);
        clientModel.hasBaseImplementation = true;
        clientModel.canBeInstantiated = false;

        Token pubToken = new Token(TokenType.PUBLIC, "pub", null, 0, 0);
        clientModel.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "host", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_STRING, "string", null, 0, 0))));
        clientModel.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "port", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_INT, "int", null, 0, 0))));

        XplClass clientClass = new XplClass(clientModel, interpreter.globals);

        // =========================================================
        // Smtp.client(host, port, user, password)
        // =========================================================
        model.staticFields.put("client", buildFunc(4, (intp, args) -> {
            String host = getString(intp, args, 0);
            int port = ((Number) intp.evaluate(args.get(1).expression)).intValue();
            String user = getString(intp, args, 2);
            String password = getString(intp, args, 3);

            // Fabricamos o cliente SMTP
            XplInstance instance = new XplInstance(clientClass);
            instance.fields.put("host", host);
            instance.fields.put("port", (long) port);

            // Variáveis privadas na instância (invisíveis por padrão sem o 'pub')
            instance.fields.put("_user", user);
            instance.fields.put("_pass", password);

            // =========================================================
            // smtpCliente.send(from, to, subject, body)
            // =========================================================
            instance.fields.put("send", buildFunc(4, (i, a) -> {
                String from = getString(i, a, 0);
                String to = getString(i, a, 1);
                String subject = getString(i, a, 2);
                String body = getString(i, a, 3);

                try {
                    // Carrega as classes dinamicamente (Reflexão Injetada)
                    Class<?> sessionClass = Class.forName("javax.mail.Session");
                    Class<?> messageClass = Class.forName("javax.mail.Message");
                    Class<?> mimeMessageClass = Class.forName("javax.mail.MimeMessage");
                    Class<?> internetAddressClass = Class.forName("javax.mail.internet.InternetAddress");
                    Class<?> recipientTypeClass = Class.forName("javax.mail.Message$RecipientType");
                    Class<?> transportClass = Class.forName("javax.mail.Transport");

                    // Configura as propriedades de segurança e portas
                    Properties props = new Properties();
                    props.put("mail.smtp.host", host);
                    props.put("mail.smtp.port", String.valueOf(port));
                    props.put("mail.smtp.auth", "true");

                    if (port == 465) {
                        props.put("mail.smtp.ssl.enable", "true");
                        props.put("mail.smtp.socketFactory.port", String.valueOf(port));
                        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    } else if (port == 587) {
                        props.put("mail.smtp.starttls.enable", "true");
                        props.put("mail.smtp.starttls.required", "true");
                    }
                    props.put("mail.smtp.connectiontimeout", "10000");
                    props.put("mail.smtp.timeout", "10000");

                    // Autenticação direta pela Sessão
                    props.put("mail.smtp.user", user);
                    props.put("mail.smtp.password", password);
                    Object sessionInstance = sessionClass.getMethod("getInstance", Properties.class).invoke(null, props);

                    // Criação do corpo do Email
                    Constructor<?> mimeMessageConst = mimeMessageClass.getConstructor(sessionClass);
                    Object messageInstance = mimeMessageConst.newInstance(sessionInstance);

                    // De:
                    Constructor<?> addrConst = internetAddressClass.getConstructor(String.class);
                    Object fromAddress = addrConst.newInstance(from);
                    messageClass.getMethod("setFrom", Class.forName("javax.mail.Address")).invoke(messageInstance, fromAddress);

                    // Para:
                    Method parseMethod = internetAddressClass.getMethod("parse", String.class);
                    Object[] toAddresses = (Object[]) parseMethod.invoke(null, to);
                    Object recipientTypeTo = recipientTypeClass.getField("TO").get(null);
                    messageClass.getMethod("setRecipients", recipientTypeClass, Class.forName("[Ljavax.mail.internet.InternetAddress;"))
                            .invoke(messageInstance, recipientTypeTo, toAddresses);

                    // Assunto e Texto:
                    messageClass.getMethod("setSubject", String.class).invoke(messageInstance, subject);
                    messageClass.getMethod("setText", String.class).invoke(messageInstance, body);

                    // Disparo!
                    Method sendMethod = transportClass.getMethod("send", messageClass);
                    sendMethod.invoke(null, messageInstance);

                    return true;

                } catch (ClassNotFoundException e) {
                    throw new ControlFlow.RuntimeError(null, "Dependência em falta: O JavaMail (javax.mail) não está instalado no sistema.");
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    throw new ControlFlow.RuntimeError(null, "Falha ao enviar email via SMTP: " + cause.getMessage());
                }
            }));

            return instance;
        }));

        interpreter.registry_model.put("Smtp", model);
        interpreter.environment.defineConst("Smtp", new XplClass(model, interpreter.globals));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
    }

    @FunctionalInterface
    private interface NativeAction {
        Object execute(Interpreter interpreter, List<Expr.CallArg> args);
    }

    private static XplCallable buildFunc(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return action.execute(intp, args); }
        };
    }
}