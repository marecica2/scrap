package email;

import models.Ad;
import models.Auction;
import models.User;

import org.apache.commons.lang.StringUtils;

import play.i18n.Messages;
import utils.Constants;

public class Templates
{
    public static String adEmailTemplateSubject(User user)
    {
        final String locale = StringUtils.defaultIfEmpty(user.locale, "en");
        return Messages.getMessage(locale, "ad.email.template.subject");
    }

    public static String adEmailTemplateHeader(User user)
    {
        final String locale = StringUtils.defaultIfEmpty(user.locale, "en");
        StringBuilder sb = new StringBuilder();
        sb.append(Messages.getMessage(locale, "ad.email.header") + "<br/><br/>");
        return sb.toString();
    }

    private static String adToHtml(String locale, StringBuilder sb, String server, Ad ad)
    {
        sb.append("<b>" + Messages.getMessage(locale, "ad.email.newAd") + "</b><br/>");
        sb.append(Messages.getMessage(locale, "ad.email.intime") + ad.formatDate(ad.created) + "<br/>");
        sb.append(Messages.getMessage(locale, "ad.type") + ": " + Messages.getMessage(locale, "ad.type." + ad.type) + "<br/>");
        if (ad.category != null)
            sb.append(Messages.getMessage(locale, "ad.category") + ": " + Messages.getMessage(locale, "ad.category." + ad.category) + "<br/>");
        if (ad.subCategory != null)
            sb.append(Messages.getMessage(locale, "ad.subcategory") + ": " + Messages.getMessage(locale, "ad.subcategory." + ad.subCategory) + "<br/>");
        if (ad.price != null)
            sb.append(Messages.getMessage(locale, "ad.price") + ": " + ad.price + " &euro; <br/>");
        if (ad.amount != null)
            sb.append(Messages.getMessage(locale, "ad.amount") + ": " + ad.amount + " kg; <br/>");
        sb.append("<a href='" + server + "/ad?uuid=" + ad.uuid + "'>" + Messages.getMessage(locale, "ad.url") + "</a>" + "<br/>");
        sb.append("<br/>");
        return sb.toString();
    }

    public static String renderHtmlAd(Ad ad, Auction auction, User user)
    {
        final String locale = StringUtils.defaultIfEmpty(user.locale, "en");
        final StringBuilder sb = new StringBuilder();
        return adToHtml(locale, sb, Constants.getBaseUrl(), ad);
    }

}
