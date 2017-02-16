package email;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import play.Logger;
import utils.Constants;

public class EmailProvider
{
    public static final String EMAIL_SENDER = "info@wid.gr";
    public static final String PROTOCOL_SMTP = "smtp";
    public static final String PROTOCOL_SMTPS = "smtps";

    private final String host;
    private final String port;
    private final String username;
    private final String password;
    private final String protocol;
    private boolean tls = false;
    private final Properties props = System.getProperties();
    private Session session = null;

    public EmailProvider(String host, String port, String email, String password, String timeout, String protocol, boolean tls)
    {
        this.host = Constants.MAIL_HOST;
        this.port = Constants.MAIL_PORT;
        this.username = Constants.MAIL_ACCOUNT;
        this.password = Constants.MAIL_PASSWORD;
        this.protocol = Constants.MAIL_PROTOCOL;
        this.tls = true;
        emailSettings();
        createSession();
    }

    public EmailProvider()
    {
        this.host = Constants.MAIL_HOST;
        this.port = Constants.MAIL_PORT;
        this.username = Constants.MAIL_ACCOUNT;
        this.password = Constants.MAIL_PASSWORD;
        this.protocol = Constants.MAIL_PROTOCOL_SMTP;
        this.tls = true;
        emailSettings();
        createSession();
    }

    private void emailSettings()
    {
        props.put("mail.transport.protocol", protocol);
        props.put("mail." + protocol + ".host", host);
        props.put("mail." + protocol + ".starttls.enable", tls);
        props.put("mail." + protocol + ".port", port);
        props.put("mail." + protocol + ".auth", "true");
        props.put("mail." + protocol + ".socketFactory.class", "email.DefaultSSLSocketFactory");
    }

    private void createSession()
    {
        session = Session.getInstance(props, new javax.mail.Authenticator()
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication(username, password);
            }
        });
        //session.setDebug(true);
    }

    public void sendEmail(String subject, String recipient, String body) throws Exception
    {
        EmailContainer ec = new EmailContainer();
        ec.subject = "STI - " + subject;
        ec.recipient = recipient;
        ec.body = body;
        List<EmailContainer> list = new ArrayList<EmailContainer>();
        list.add(ec);
        sendEmails(list);
    }

    public void sendEmails(List<EmailContainer> emails) throws Exception
    {
        Transport transport = session.getTransport(protocol);
        transport.connect(username, password);
        for (EmailContainer email : emails)
        {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_SENDER));
            message.setSubject(email.subject);
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email.recipient));

            Multipart multipart = new MimeMultipart("alternative");
            BodyPart messageBodyPart = buildHtmlTextPart(email.body);
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);

            transport.sendMessage(message, message.getAllRecipients());
            Logger.info("Email sent to " + email.recipient);
        }
        transport.close();
    }

    private BodyPart buildHtmlTextPart(String htmlPart) throws MessagingException
    {
        MimeBodyPart descriptionPart = new MimeBodyPart();
        descriptionPart.setContent(htmlPart, "text/html; charset=utf-8");
        return descriptionPart;
    }
}
