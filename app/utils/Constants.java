package utils;

import play.Play;
import controllers.BaseController;

public class Constants
{
    public static final String CONFIG_BASE_URL = "scrap.server.url";

    public static final String MAIL_PROTOCOL_SMTP = "smtp";
    public static final String MAIL_PROTOCOL_SMTPS = "smtps";

    public static final String MAIL_ACCOUNT = BaseController.getProperty("scrap.mail.user");
    public static final String MAIL_PASSWORD = BaseController.getProperty("scrap.mail.password");
    public static final String MAIL_HOST = BaseController.getProperty("scrap.mail.host");
    public static final String MAIL_PROTOCOL = BaseController.getProperty("scrap.mail.protocol");
    public static final String MAIL_PORT = BaseController.getProperty("scrap.mail.port");

    public static String getProperty(String key)
    {
        return Play.configuration.getProperty(key);
    }

    public static String getBaseUrl()
    {
        return getProperty(CONFIG_BASE_URL);
    }

}
