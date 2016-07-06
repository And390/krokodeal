package service;


import util.Config;
import util.MailUtil;

import javax.mail.MessagingException;

public class MailSender
{
    public MailSender() throws MessagingException
    {
        Config.getNotEmpty(MailUtil.SMTP_HOST_CFG);
        Config.getNotEmpty(MailUtil.SMTP_USER_CFG);
        Config.get(MailUtil.SMTP_PASSWORD_CFG);
        MailUtil.verifySmtp(Config.getProperties());
    }

    public void send(String email, String subject, String text) throws MessagingException
    {
        MailUtil.send(Config.getProperties(), email, subject, text);
    }

    public void send(String[] emails, String subject, String text) throws MessagingException
    {
        MailUtil.send(Config.getProperties(), emails, subject, text);
    }

}
