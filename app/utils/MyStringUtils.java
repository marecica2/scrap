package utils;

import java.net.URLDecoder;
import java.util.Random;

public class MyStringUtils
{
    private static final String charset = "0123456789abcdefghijklmnopqrstuvwxyz";

    public static String getRandomPassword(int length)
    {
        Random rand = new Random(System.currentTimeMillis());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++)
        {
            int pos = rand.nextInt(charset.length());
            sb.append(charset.charAt(pos));
        }
        return sb.toString();
    }

    public static String getStringNotNull(String value)
    {
        if (value == null)
            return "";
        else
            return value;
    }

    public static String getStringNotNullMaxLen(String value, int len)
    {
        if (value == null)
            return "";
        else
        {
            if (value.length() > len)
                value = value.substring(0, len);
            return value;
        }
    }

    public static String getStringNotNullDecode(String value)
    {
        if (value == null)
            return "";
        else
            try
            {
                return URLDecoder.decode(value, "UTF-8");
            } catch (Exception e)
            {
                return "";
            }
    }

    public static boolean isValidEmailAddress(String email)
    {
        boolean stricterFilter = true;
        String stricterFilterString = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
        String laxString = ".+@.+\\.[A-Za-z]{2}[A-Za-z]*";
        String emailRegex = stricterFilter ? stricterFilterString : laxString;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(emailRegex);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

    public static String htmlEscape(String value)
    {
        if (value != null)
        {
            value = value.replaceAll("<", "&lt;");
            value = value.replaceAll(">", "&gt;");
            return value;
        }
        return null;
    }

}
