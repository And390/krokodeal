package util;

import utils.Console;
import utils.Util;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

/**
 * User: And390
 * Date: 23.12.11
 * Time: 11:46
 */
public class MailUtil
{
    //                --------    smtp part    --------

    public static final String encoding = "UTF-8";

    public static final String SMTP_HOST_CFG = "mail.smtp.host";
    public static final String SMTP_PORT_CFG = "mail.smtp.port";
    public static final String SMTP_USER_CFG = "mail.smtp.user";
    public static final String SMTP_PASSWORD_CFG = "mail.smtp.password";
    public static final String SMTP_FROM_CFG = "mail.smtp.from";


    public static Multipart createMessage(String text) throws MessagingException
    {
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(text, "UTF-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);

        return multipart;
    }

    public static Multipart attachFile(Multipart multipart, String name, byte[] data) throws MessagingException
    {
        MimeBodyPart filePart = new MimeBodyPart();
        filePart.setContent(data, "application/octet-stream");
        filePart.setFileName(name);
        filePart.setHeader("Content-Transfer-Encoding", "base64");
        multipart.addBodyPart(filePart);
        return multipart;
    }

    public static Properties properties(String host, String user, String password, String from)
    {
        Properties properties = new Properties ();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.user", user);
        properties.setProperty("mail.smtp.password", password);
        properties.setProperty("mail.smtp.from", from);
        return properties;
    }

    public static Properties properties(String host, int port, String user, String password, String from)
    {
        Properties properties = properties(host, user, password, from);
        properties.setProperty("mail.smtp.port", ""+port);
        return properties;
    }

    private static Transport open(Session session, Properties properties) throws MessagingException
    {
        String host = properties.getProperty("mail.smtp.host");
        String port = properties.getProperty("mail.smtp.port");
        String user = properties.getProperty("mail.smtp.user");
        String password = properties.getProperty("mail.smtp.password");

        Transport transport = session.getTransport("smtp");

        try  {
            if (user!=null && password!=null)
                if (host!=null)
                    if (port!=null)  transport.connect(host, Integer.parseInt(port), user, password);
                    else  transport.connect(host, user, password);
                else  transport.connect(user, password);
            return transport;
        }
        catch (Throwable e) {
            transport.close();
            throw e;
        }
    }

    public static void verifySmtp(Properties properties) throws MessagingException
    {
        Session session = Session.getInstance(properties);
        Transport transport = open(session, properties);
        transport.close();
    }

    public static void send(Properties properties, String[] recipients, String subject, Multipart multipart) throws MessagingException
    {
        Session session = Session.getInstance(properties);

        String senderName = properties.getProperty("mail.smtp.from.name");
        String sender = properties.getProperty("mail.smtp.from");
        if (sender==null)  {
            String host = properties.getProperty("mail.smtp.host");
            String user = properties.getProperty("mail.smtp.user");
            if (user.indexOf('@')!=-1)  sender = user;
            else  {
                int i = host.lastIndexOf('.');
                if (i==-1)  throw new IllegalArgumentException ("sender is not set and it can't be obtained from host");
                i = host.lastIndexOf('.', i-1);
                sender = user + '@' + host.substring(i+1);
            }
        }

        Transport transport = open(session, properties);
        try {
            InternetAddress senderAddress = senderName!=null ? new InternetAddress(sender, senderName) : new InternetAddress(sender);
            InternetAddress[] recipientAddresses = new InternetAddress [recipients.length];
            for (int i=0; i<recipients.length; i++)  recipientAddresses[i] = new InternetAddress (recipients[i]);

            MimeMessage message = new MimeMessage(session);
			message.setFrom(senderAddress);
			message.setReplyTo(new InternetAddress[] {senderAddress});
            message.setRecipients(Message.RecipientType.TO, recipientAddresses);
            message.setSubject(subject, encoding);
			message.setContent(multipart);
			message.setSentDate(new Date());
			message.saveChanges();

            transport.sendMessage(message, recipientAddresses);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } finally {
			transport.close();
		}
    }

    public static void send(Properties properties, String recipients, String subject, Multipart multipart) throws MessagingException  {
        send(properties, new String[] {recipients}, subject, multipart);
    }

    public static void send(Properties properties, String[] recipients, String subject, String text) throws MessagingException {
        send(properties, recipients, subject, createMessage(text));
    }

    public static void send(Properties properties, String recipient, String subject, String text) throws MessagingException {
        send(properties, recipient, subject, createMessage(text));
    }

    public static void send(Properties properties, String recipient, String subject, String text, String attachmentName, byte[] attachmentContent) throws Exception {
        send(properties, recipient, subject, attachFile(createMessage(text), attachmentName, attachmentContent));
    }


    public static class Send
    {
        public static void main(String[] args) throws Exception
        {
            Console.setEncoding();
            run(args);
        }

        public static void run(String[] args) throws Exception
        {
            //    command line arguments
            if (args.length<3)  throw new Exception ("command line parameters: <email> <subject> <message>");
            String[] recipients = Util.slice(Util.checkNotEmpty(args[0], "аргумента 'email'"), ',');
            String subject = Util.checkNotEmpty(args[1], "аргумента 'subject'");
            String message =  Util.checkNotEmpty(args[2], "аргумента 'message'");

            //    locad properties
            Properties properties = new Properties ();
            properties.setProperty("mail.smtp.socketFactory.port", "465");
            properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            Util.getNotEmpty(properties, "mail.smtp.host");
            Util.get(properties, "mail.smtp.user");
            Util.get(properties, "mail.smtp.password");

            send(properties, recipients, subject, message);
        }
    }

    public static class TestMail  {
        public static void main(String[] args) throws Exception {
            Properties properties = new Properties();
            properties.setProperty("mail.smtp.host", "smtp.mail.ru");
            properties.setProperty("mail.smtp.port", "465");
            properties.setProperty("mail.smtp.from", "test.nbki.2@mail.ru");
            properties.setProperty("mail.smtp.user", "test.nbki.2@mail.ru");
            properties.setProperty("mail.smtp.password", "testnbki");
            properties.setProperty("mail.smtp.socketFactory.port", "465");
            properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            send(properties, "night-drifter@yandex.ru", "test mail",
                    attachFile(createMessage("text"), "attachment.txt", "bla bla bla".getBytes("cp1251")));
        }
    }

    public static class TestYandex  {
        public static void main(String[] args) throws Exception {
            Properties properties = new Properties();
            properties.setProperty("mail.smtp.host", "smtp.yandex.ru");
            properties.setProperty("mail.smtp.port", "465");
            properties.setProperty("mail.smtp.from", "eda-nado@yandex.ru");
            properties.setProperty("mail.smtp.user", "eda-nado");
            properties.setProperty("mail.smtp.password", "pain715");
            properties.setProperty("mail.smtp.socketFactory.port", "465");
            properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            send(properties, "night-drifter@yandex.ru", "test mail",
                    attachFile(createMessage("text"), "attachment.txt", "bla bla bla".getBytes("cp1251")));
        }
    }

    public static class TestAgava  {
        public static void main(String[] args) throws Exception {
            Properties properties = new Properties();
            properties.setProperty("mail.debug", "true");
            properties.setProperty("mail.smtp.host", "mobzver.ru");  //"vm9711.vps.agava.net");
            properties.setProperty("mail.smtp.port", "25");
            properties.setProperty("mail.smtp.user", "info");
            properties.setProperty("mail.smtp.password", "warm7908");
            properties.setProperty("mail.smtp.from", "info@mobzver.ru");  //"root@vm9711.vps.agava.net");
            properties.setProperty("mail.smtp.from.name", "MobZver");
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.smtp.ssl.trust", "mobzver.ru");
            properties.setProperty("mail.smtp.starttls.enable", "true");
//            properties.setProperty("mail.smtp.socketFactory.port", "465");
//            properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            send(properties, "night-drifter@yandex.ru", "test mail",
                    attachFile(createMessage("text"), "attachment.txt", "bla bla bla".getBytes("cp1251")));
        }
    }

    public static class TestAgavaRead  {
        public static void main(String[] args) throws Exception {
            Properties props = new Properties();
            props.setProperty("mail.debug", "true");
            props.setProperty("mail.store.protocol", "pop3");
            props.setProperty("mail.pop3.ssl.enable", "true");
            props.setProperty("mail.pop3.ssl.trust", "*");
            props.setProperty("mail.imap.ssl.enable", "true");
            props.setProperty("mail.imap.ssl.trust", "*");
            try {
                Session session = Session.getInstance(props, null);
                Store store = session.getStore();
                store.connect("mobzver.ru", "info", "warm7908");
                Folder inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_ONLY);
                System.out.println(inbox.getMessageCount());
                if (inbox.getMessageCount() == 0)  return;
                Message msg = inbox.getMessage(inbox.getMessageCount());
                Address[] in = msg.getFrom();
                for (Address address : in) {
                    System.out.println("FROM:" + address.toString());
                }
                Multipart mp = (Multipart) msg.getContent();
                BodyPart bp = mp.getBodyPart(0);
                System.out.println("SENT DATE:" + msg.getSentDate());
                System.out.println("SUBJECT:" + msg.getSubject());
                System.out.println("CONTENT:" + bp.getContent());
            } catch (Exception mex) {
                mex.printStackTrace();
            }
        }
    }

}
