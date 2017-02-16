package controllers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.Ad;
import models.Chat;
import models.FilterAd;
import models.User;

import org.apache.commons.lang.RandomStringUtils;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.i18n.Lang;
import play.i18n.Messages;
import play.libs.Images;
import utils.Constants;
import utils.Credits;
import utils.MyStringUtils;
import utils.NumberUtils;
import utils.RandomUtil;
import dto.ChatDto;
import email.EmailProvider;

public class Application extends BaseController
{
    public static void captcha(String uuid, String exp)
    {
        Images.Captcha captcha = Images.captcha();
        String code = captcha.getText(5);
        String expiration = "10mn";
        if (exp != null)
            expiration = exp + "s";
        Cache.set("captcha." + uuid, code, expiration);
        renderBinary(captcha);
    }

    public static void lme() throws IOException
    {
        User user = getNotCachedUser();
        render(user);
    }

    public static void postChat(String message, String to) throws IOException
    {
        User user = getCachedUser();
        Chat chat = new Chat();
        chat.user = user;
        chat.created = new Date();
        chat.text = MyStringUtils.htmlEscape(message);
        chat.uuid = RandomUtil.getUUID();

        if (user.isAdmin())
        {
            User usr = User.getUserByUUID(to);
            chat.user = usr;
            chat.inverted = true;
            chat.admin = user;
        }
        chat.save();
        renderText("ok");
    }

    public static void myChats() throws IOException
    {
        User user = getCachedUser();
        User admin = User.getAdminUser();
        boolean adminOnline = false;
        if (admin != null && admin.online != null && admin.online)
            adminOnline = true;

        List<Chat> chatsList = null;
        if (user.isAdmin())
            chatsList = Chat.getAll(user);
        else
            chatsList = Chat.getByUser(user);
        List<ChatDto> chats = new ArrayList<ChatDto>();
        for (Chat chat : chatsList)
            chats.add(ChatDto.convert(chat, adminOnline));
        renderJSON(chats);
    }

    public static void lmeImage(String image) throws IOException
    {
        User user = getNotCachedUser();
        clearCachedUser();

        final File file = new File(Play.applicationPath + "/public/lme/" + image);
        final long now = System.currentTimeMillis();
        if (file.lastModified() <= (now - 160000) && false)
        {
            Logger.error("LmeDownloader not working, disabling auto update");
            notFound();
        }

        if (user != null)
        {
            if (user.account.credit.compareTo(Credits.CREDIT_FOR_LME) <= 0)
            {
                Logger.error("Not enough credit " + user.getFullName());
                error();
            } else
            {
                user.account.credit = user.account.credit.subtract(Credits.CREDIT_FOR_LME);
                user.account.save();

                Logger.info("credit substracted " + user.account.credit + " user " + user.getFullName());
                renderBinary(file);
            }
        }
        notFound();
    }

    public static void locale(String locale, String url)
    {
        User user = getNotCachedUser();
        if (user != null)
        {
            user.locale = locale;
            user.save();
            clearCachedUser();
        }
        Lang.change(locale);
        redirect(url);
    }

    public static void lmePeriod(String period, String url)
    {
        User user = getNotCachedUser();
        if (user != null)
        {
            user.lmePeriod = NumberUtils.parseInt(period);
            user.save();
            clearCachedUser();
        }
        redirect(url);
    }

    public static void index()
    {
        User user = getCachedUser();
        if (user == null)
            user = getUserFromAuthorizationHeader();

        FilterAd filterAd = new FilterAd();
        filterAd.state = Ad.STATE_PUBLISHED;
        List<Ad> adList1 = Ad.getFilteredAds(filterAd, 0, 10, null, null);
        List<Ad> adList = new ArrayList<Ad>();

        for (Ad ad : adList1)
        {
            ad.locationName = ad.getAddress();
            if (!ad.isExpired())
                adList.add(ad);
        }

        List<Integer> repeats = new ArrayList<Integer>();
        List<List<Ad>> ads = new ArrayList<List<Ad>>();

        // process ads for carousel
        if (adList.size() >= 3)
        {
            ads.add(adList.subList(0, 3));
            repeats.add(0);
        }
        if (adList.size() >= 6)
        {
            ads.add(adList.subList(3, 6));
            repeats.add(1);
        } else
        {
            ads.add(new ArrayList<Ad>());
        }
        if (adList.size() >= 9)
        {
            ads.add(adList.subList(6, 9));
            repeats.add(2);
        } else
        {
            ads.add(new ArrayList<Ad>());
        }

        List<Ad> auctionsList = Ad.getAuctionAds();
        List<Ad> auctions = new ArrayList<Ad>();
        for (Ad ad : auctionsList)
        {
            if (!ad.isExpired())
                auctions.add(ad);
        }
        render(user, ads, auctions, repeats);
    }

    public static void forgotPassword()
    {
        render("Secure/password.html");
    }

    public static void forgotPasswordPost(String uuid, String captcha, String username)
    {
        final Object cap = Cache.get("captcha." + uuid);
        if (captcha != null && cap != null)
        {
            validation.equals(captcha, cap).message("invalid.captcha");
            if (!validation.hasErrors())
            {
                EmailProvider ep = new EmailProvider();
                try
                {
                    String password = RandomStringUtils.randomAlphanumeric(8);
                    User user = User.getUserByLogin(username);
                    if (user != null)
                    {
                        user.password = password;
                        user.save();
                    }
                    ep.sendEmail(Messages.get("system.password.subject"), username, Messages.get("system.password.body", password));
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
                flash("success", Messages.get("system.newpassword"));
                redirect("/login");
            }
        }
        params.flash();
        render("Secure/password.html");
    }

    public static void contact()
    {
        final String url = request.params.get("url");
        final User user = getCachedUser();
        final String uuid = RandomUtil.getUUID();
        params.put("uuid", uuid);
        params.flash();
        render(user, uuid, url);
    }

    public static void contactPost(String uuid, String name, String email, String subject, String message, String captcha, String id, String url)
    {
        final User user = getCachedUser();
        final Object cap = Cache.get("captcha." + uuid);

        if (user == null)
        {
            validation.required("email", email);
            validation.required("name", name);
            validation.email("email", email).message("validation.login");
            validation.required("captcha", captcha);
        }

        validation.required("subject", subject);
        validation.required("message", message);

        if (captcha != null && cap != null)
            validation.equals(captcha, cap).message("invalid.captcha");

        if (!validation.hasErrors())
        {
            flash.success(Messages.get("message-sent-successfully"));
            flash.keep();

            String body = "Sender: " + name + " (" + email + ") <br/>";
            if (user != null)
                body = "Odosielatel: <a href='" + Constants.getBaseUrl() + "/public-profile?uuid=" + user.uuid + "'>" + user.getFullName() + "</a> (" + user.login + ")";
            body += "<h3>" + subject + " <small><a href='" + Constants.getBaseUrl() + url + "'>Url odkaz</a></small></h3>";
            body += message;

            try
            {
                new EmailProvider().sendEmail("Spravy od uzivatelov", "info@wid.gr", body);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            contact();
        } else
        {
            uuid = RandomUtil.getUUID();
            flash.error(Messages.get("message-sent-error"));
            params.put("uuid", uuid);
            params.flash();
            final User usr = id != null ? User.getUserByLogin(id) : null;
            renderTemplate("Application/contact.html", user, uuid, usr);
        }

    }

    public static void about()
    {
        User user = getCachedUser();
        renderTemplate("Application/about_" + Lang.getLocale() + ".html", user);
    }

    public static void pricing()
    {
        User user = getCachedUser();
        renderTemplate("Application/pricing_" + Lang.getLocale() + ".html", user);
    }

    public static void terms()
    {
        User user = getCachedUser();
        renderTemplate("Application/terms_" + Lang.getLocale() + ".html", user);
    }
}