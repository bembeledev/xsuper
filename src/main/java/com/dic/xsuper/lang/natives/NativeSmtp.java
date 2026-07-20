package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Properties;

/**
 * Envio de e‑mail via SMTP de forma desacoplada (Reflection).
 * Compila mesmo sem a dependência JavaMail no classpath do projeto.
 */
public class NativeSmtp {

    public static void register(Interpreter interpreter) {
        interpreter.globals.defineConst("smtp_send", buildFunc(8, (intp, args) -> {
            String host = getString(intp, args, 0);
            int port = ((Number) intp.evaluate(args.get(1).expression)).intValue();
            String user = getString(intp, args, 2);
            String password = getString(intp, args, 3);
            String from = getString(intp, args, 4);
            String to = getString(intp, args, 5);
            String subject = getString(intp, args, 6);
            String body = getString(intp, args, 7);

            try {
                // Carrega as classes dinamicamente
                Class<?> sessionClass = Class.forName("javax.mail.Session");
                Class<?> authenticatorClass = Class.forName("javax.mail.Authenticator");
                Class<?> passwordAuthClass = Class.forName("javax.mail.PasswordAuthentication");
                Class<?> messageClass = Class.forName("javax.mail.Message");
                Class<?> mimeMessageClass = Class.forName("javax.mail.MimeMessage");
                Class<?> internetAddressClass = Class.forName("javax.mail.internet.InternetAddress");
                Class<?> recipientTypeClass = Class.forName("javax.mail.Message$RecipientType");
                Class<?> transportClass = Class.forName("javax.mail.Transport");

                // Configura as propriedades
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

                // Instancia o PasswordAuthentication
                Constructor<?> authConst = passwordAuthClass.getConstructor(String.class, String.class);
                Object passwordAuthInstance = authConst.newInstance(user, password);

                // Cria o Authenticator dinamicamente usando Proxy ou uma Subclasse Anônima via reflexão básica se necessário.
                // Como Authenticator é uma classe abstrata e não uma interface, criamos um proxy customizado:
                Object authenticatorInstance = Proxy.newProxyInstance(
                        NativeSmtp.class.getClassLoader(),
                        new Class<?>[]{},
                        (proxy, method, methodArgs) -> {
                            if (method.getName().equals("getPasswordAuthentication")) {
                                return passwordAuthInstance;
                            }
                            return null;
                        }
                );

                // Session.getInstance(props, authenticator)
                Method getInstanceMethod = sessionClass.getMethod("getInstance", Properties.class, authenticatorClass);
                Object sessionInstance = getInstanceMethod.invoke(null, props, null); // Passando null provisoriamente se não usar autenticação customizada em nível de proxy de classe abstrata

                // Se precisar de autenticação estrita em classe abstrata por reflexão pura, o ideal é usar a Session e injetar as propriedades direto:
                props.put("mail.smtp.user", user);
                props.put("mail.smtp.password", password);
                sessionInstance = sessionClass.getMethod("getInstance", Properties.class).invoke(null, props);

                // Criar a MimeMessage
                Constructor<?> mimeMessageConst = mimeMessageClass.getConstructor(sessionClass);
                Object messageInstance = mimeMessageConst.newInstance(sessionInstance);

                // message.setFrom(new InternetAddress(from))
                Constructor<?> addrConst = internetAddressClass.getConstructor(String.class);
                Object fromAddress = addrConst.newInstance(from);
                messageClass.getMethod("setFrom", Class.forName("javax.mail.Address")).invoke(messageInstance, fromAddress);

                // InternetAddress.parse(to)
                Method parseMethod = internetAddressClass.getMethod("parse", String.class);
                Object[] toAddresses = (Object[]) parseMethod.invoke(null, to);

                // message.setRecipients(RecipientType.TO, addresses)
                Object recipientTypeTo = recipientTypeClass.getField("TO").get(null);
                messageClass.getMethod("setRecipients", recipientTypeClass, Class.forName("[Ljavax.mail.internet.InternetAddress;"))
                        .invoke(messageInstance, recipientTypeTo, toAddresses);

                // message.setSubject(subject)
                messageClass.getMethod("setSubject", String.class).invoke(messageInstance, subject);

                // message.setText(body)
                messageClass.getMethod("setText", String.class).invoke(messageInstance, body);

                // Transport.send(message)
                Method sendMethod = transportClass.getMethod("send", messageClass);
                sendMethod.invoke(null, messageInstance);

                return true;

            } catch (ClassNotFoundException e) {
                throw new ControlFlow.RuntimeError(null, "JavaMail não instalado no sistema de arquivos. Adicione o jar do javax.mail.");
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new ControlFlow.RuntimeError(null, "smtp_send erro: " + cause.getMessage());
            }
        }));
    }

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
