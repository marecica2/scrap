package controllers;

import models.Account;
import models.User;
import play.Play;
import play.cache.Cache;
import play.mvc.Controller;
import utils.UriUtils;

import com.ning.http.util.Base64;

public class BaseController extends Controller
{
    public static final String CONFIG_SERVER_IP = "scrap.configuration.ip";

    public static void redirectTo(String url)
    {
        redirect(UriUtils.redirectStr(url));
    }

    public static User getUserFromAuthorizationHeader()
    {
        String base64encodedLogin = request.params.get("Authorization");
        if (base64encodedLogin != null)
        {
            String[] credentials = new String(Base64.decode(base64encodedLogin)).split(":");
            String login = credentials[0];
            String password = credentials[1];
            User user = User.getUserByLogin(login);
            if (user.password.equals(password))
            {
                session.put("username", user.login);
                return user;
            }
        }
        return null;
    }

    public static User getCachedUser()
    {
        final String userLogin = getUserLogin();
        User u = (User) Cache.get(userLogin);
        if (u != null)
        {
            return u;
        } else
        {
            u = User.getUserByLogin(userLogin);
            Cache.set(userLogin, u);
        }
        return u;
    }

    protected static boolean clearCachedUser()
    {
        final String userLogin = getUserLogin();
        Cache.delete(userLogin);
        return true;
    }

    public static User getNotCachedUser()
    {
        final String userLogin = getUserLogin();
        return User.getUserByLogin(userLogin);
    }

    public static Account getAccountByUser()
    {
        User user = getCachedUser();
        return user.account;
    }

    private static String getUserLogin()
    {
        return Secure.Security.connected();
    }

    public static String getProperty(String key)
    {
        return Play.configuration.getProperty(key);
    }

    public static String getIP()
    {
        return getProperty(CONFIG_SERVER_IP);
    }

    public static boolean isProd()
    {
        return Play.mode.isProd();
    }

    public static boolean isUserLogged()
    {
        return Secure.Security.isConnected();
    }

    static void checkAuthorizedAccess() throws Throwable
    {
        if (Secure.Security.isConnected())
        {
        } else
        {
            unauthorized();
        }
    }

}