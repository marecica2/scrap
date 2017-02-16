package controllers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.Account;
import models.Ad;
import models.AdWatch;
import models.Event;
import models.Rating;
import models.User;

import org.apache.commons.lang.StringUtils;

import play.cache.Cache;
import play.i18n.Messages;
import play.mvc.With;
import utils.Credits;
import utils.MyStringUtils;
import utils.NumberUtils;
import utils.RandomUtil;
import email.EmailProvider;

@With(Secure.class)
public class UserController extends BaseController
{
    public static void profile()
    {
        User user = getCachedUser();
        if (user.email.isEmpty() || user.phone.isEmpty())
            flash.put("info", Messages.get("system.profile"));

        List<Ad> ads = Ad.getAdsByFilter(null);
        List<Rating> ratings = Rating.getRatingsForUser(user);
        Integer avg = Ads.calculateAverageRating(ratings);
        user.avg = avg;
        user.locationName = user.getAddress();
        render(user, ads, ratings);
    }

    public static void events()
    {
        User user = getCachedUser();
        List<Event> events = Event.getEventsByUser(user.uuid);
        render(user, events);
    }

    public static void block(String uuid, String url)
    {
        User user = getCachedUser();
        User usr = User.getUserByUUID(uuid);
        usr.active = false;
        usr.save();
        redirect(url);
    }

    public static void unblock(String uuid, String url)
    {
        User user = getCachedUser();
        User usr = User.getUserByUUID(uuid);
        usr.active = true;
        usr.save();
        redirect(url);
    }

    public static void enableNotification(String url)
    {
        User user = getNotCachedUser();
        user.notificationEnabled = true;
        user.save();
        clearCachedUser();
        redirect(url);
    }

    public static void disableNotification(String url)
    {
        User user = getNotCachedUser();
        user.notificationEnabled = false;
        user.save();
        clearCachedUser();
        redirect(url);
    }

    public static void favorites()
    {
        User user = getCachedUser();
        List<AdWatch> favorites = AdWatch.getByUser(user);
        render(user, favorites);
    }

    public static void favoritesRemove(String uuid, String url)
    {
        User user = getCachedUser();
        AdWatch a = AdWatch.getByUuid(uuid);
        a.delete();
        redirect(url);
    }

    public static void favoritesWatch(String uuid, String url, String watch)
    {
        User user = getCachedUser();
        if (watch != null)
        {
            AdWatch a = AdWatch.getByUuid(watch);
            a.delete();
        } else
        {
            AdWatch a = new AdWatch();
            a.ad = Ad.getAdByUUID(uuid);
            a.user = user;
            a.active = true;
            a.created = new Date();
            a.uuid = RandomUtil.getUUID();
            a.save();
        }
        redirect(url);
    }

    public static void publicProfile(String uuid)
    {
        final boolean publicProfile = true;
        final boolean adDetail = true;
        final User user = getCachedUser();
        final User usr = User.getUserByUUID(uuid);
        final List<Rating> ratings = Rating.getRatingsForUser(usr);
        final List<Event> events = Event.getEventsByUser(usr.uuid);
        final Integer avg = Ads.calculateAverageRating(ratings);

        usr.avg = avg;
        usr.locationName = user.getAddress();
        Ad ad = new Ad();
        ad.user = usr;
        BigDecimal conversionRate = Credits.EURO_CREDIT_CONVERSION;
        render("UserController/profile.html", adDetail, publicProfile, user, ad, ratings, events, conversionRate);
    }

    public static void addCredit(String credit, String uuid, String url)
    {
        User user = getCachedUser();
        User usr = User.getUserByUUID(uuid);
        final BigDecimal creditValue = NumberUtils.parseDecimal(credit);
        if (creditValue != null && creditValue.compareTo(new BigDecimal(0)) > 0)
        {
            if (usr.account.credit == null)
                usr.account.credit = new BigDecimal(0);
            usr.account.credit = usr.account.credit.add(creditValue);
            usr.account.lastPayment = new Date();
            usr.account.save();
            Cache.delete(usr.login);

            Event e = new Event();
            e.type = Event.TYPE_USER_AD_CREDIT;
            e.created = new Date();
            e.credits = creditValue;
            e.user = usr.uuid;
            e.uuid = RandomUtil.getUUID();
            e.save();

            try
            {
                new EmailProvider().sendEmail(Messages.getMessage(usr.locale, "credit.added"), usr.login,
                        Messages.getMessage(usr.locale, "credit.added.msg", creditValue, usr.account.credit));
            } catch (Exception e1)
            {
                e1.printStackTrace();
            }

            flash.success(Messages.get("system.addcredit", credit, usr.getFullName()));
        } else
        {
            flash.error(Messages.get("system.invalidcredit"));
        }
        redirect(url);
    }

    public static void notifications()
    {
        User user = getCachedUser();

        Set<String> type = new HashSet<String>();
        Set<String> category = new HashSet<String>();
        Set<String> day = new HashSet<String>();
        Set<String> hour = new HashSet<String>();
        if (user.notificationType != null)
        {
            type = new HashSet<String>(Arrays.asList(user.notificationType.split(",")));
        }
        if (user.notificationCategory != null)
        {
            category = new HashSet<String>(Arrays.asList(user.notificationCategory.split(",")));
        }
        if (user.notificationDay != null)
        {
            day = new HashSet<String>(Arrays.asList(user.notificationDay.split(",")));
        }
        if (user.notificationHour != null)
        {
            hour = new HashSet<String>(Arrays.asList(user.notificationHour.split(",")));
        }
        render(user, day, hour, category, type);
    }

    public static void notificationsPost()
    {
        User user = getNotCachedUser();
        String[] types = request.params.getAll("type");
        String[] categories = request.params.getAll("category");
        String[] days = request.params.getAll("day");
        String[] hours = request.params.getAll("hour");

        user.notificationType = StringUtils.join(types, ",");
        user.notificationCategory = StringUtils.join(categories, ",");
        user.notificationDay = StringUtils.join(days, ",");
        user.notificationHour = StringUtils.join(hours, ",");
        user.save();
        clearCachedUser();
        flash("success", Messages.get("system.settings"));
        notifications();
    }

    public static void editProfile()
    {
        User user = getCachedUser();
        params.put("login", user.login);
        params.put("firstName", user.firstName);
        params.put("lastName", user.lastName);
        params.put("password", user.password);
        params.put("passwordRepeat", user.password);
        params.put("usrPhone", user.phone);
        params.put("usrCellPhone", user.cellPhone);
        params.put("usrFax", user.fax);
        params.put("usrEmail", user.email);

        params.put("type", user.account.type);
        params.put("phone", user.account.phone);
        params.put("cellPhone", user.account.cellPhone);
        params.put("fax", user.account.fax);
        params.put("email", user.account.email);
        params.put("companyName", user.account.companyName);
        params.put("companyId", user.account.companyId);
        params.put("companyTaxId", user.account.companyTaxId);
        params.put("billingAddressStreet", user.account.billingAddressStreet);
        params.put("billingAddressCity", user.account.billingAddressCity);
        params.put("billingAddressState", user.account.billingAddressState);

        user.locationName = user.getAddress();
        params.put("location", user.locationName);

        if (user.locationX != null)
            params.put("x", user.locationX.toString());
        if (user.locationY != null)
            params.put("y", user.locationY.toString());

        render(user);
    }

    public static void editProfilePost(
        String type,

        String login,
        String firstName,
        String lastName,
        String password,
        String passwordRepeat,
        String usrPhone,
        String usrCellPhone,
        String usrFax,
        String usrEmail,

        String phone,
        String cellPhone,
        String fax,
        String email,
        String companyName,
        String companyId,
        String companyTaxId,
        String billingAddressStreet,
        String billingAddressCity,
        String billingAddressState,

        String x,
        String y,
        String location,
        String streetNumber,
        String street,
        String city,
        String country
        )
    {
        validation.required(type);
        validation.required(firstName);
        validation.required(lastName);
        //validation.required(password);
        //validation.equals(password, passwordRepeat).message("validation.passwordMatch");

        if (type.equals(Account.BUSINESS))
        {
            validation.required(phone);
            validation.required(cellPhone);
            validation.required(email);
            validation.required(companyName);
            validation.required(companyId);
            validation.required(companyTaxId);
            validation.required(billingAddressStreet);
            validation.required(billingAddressCity);
            validation.required(billingAddressState);
        }

        User user = getNotCachedUser();
        if (!validation.hasErrors())
        {
            Account account = user.account;
            account.type = MyStringUtils.htmlEscape(type);
            account.phone = MyStringUtils.htmlEscape(phone);
            account.cellPhone = MyStringUtils.htmlEscape(cellPhone);
            account.fax = MyStringUtils.htmlEscape(fax);
            account.email = MyStringUtils.htmlEscape(email);
            account.companyId = MyStringUtils.htmlEscape(companyId);
            account.companyName = MyStringUtils.htmlEscape(companyName);
            account.companyTaxId = MyStringUtils.htmlEscape(companyTaxId);
            account.billingAddressStreet = MyStringUtils.htmlEscape(billingAddressStreet);
            account.billingAddressCity = MyStringUtils.htmlEscape(billingAddressCity);
            account.billingAddressState = MyStringUtils.htmlEscape(billingAddressState);
            account.modified = new Date();
            account.save();

            user.modified = new Date();
            user.firstName = MyStringUtils.htmlEscape(firstName);
            user.lastName = MyStringUtils.htmlEscape(lastName);
            user.phone = usrPhone;
            user.cellPhone = usrCellPhone;
            user.email = usrEmail;
            user.fax = usrFax;

            if (x != null && !x.isEmpty())
                user.locationX = Double.parseDouble(x);
            if (y != null && !y.isEmpty())
                user.locationY = Double.parseDouble(y);
            if (location != null && location.length() > 0)
                user.locationName = location;
            if (street != null && street.length() > 0)
                user.locationStreet = street;
            if (streetNumber != null && streetNumber.length() > 0)
                user.locationStreetNumber = streetNumber;
            if (location != city && city.length() > 0)
                user.locationCity = city;
            if (location != country && country.length() > 0)
                user.locationCountry = country;

            user.save();
            clearCachedUser();
            redirect("/profile");
        } else
        {
            params.flash();
            render("UserController/editProfile.html", user);
        }
    }

    public static void addRate(String ratedUser, Integer rating, String description, String reason, String redirectUrl, String adUuid)
    {
        validation.required(rating);
        validation.required(reason);
        validation.required(ratedUser);

        User user = getNotCachedUser();
        if (!validation.hasErrors())
        {
            Rating r = new Rating();
            r.created = new Date();
            r.description = description;
            r.ratedUser = User.getUserByUUID(ratedUser);
            r.user = user;
            r.reason = reason;
            r.stars = rating;
            r.uuid = RandomUtil.getUUID();
            r.save();
            redirect(redirectUrl);
        } else
        {
            params.flash();
            validation.keep();
            Ads.ad(adUuid);
        }

    }

    public static void deleteRate(String uuid, String url)
    {
        User user = getCachedUser();
        if (user.isAdmin())
        {
            Rating r = Rating.getUuid(uuid);
            if (r != null)
                r.delete();
            flash.success(Messages.get("system.rating"));
            redirect(url);
        }
        response.status = 403;
        redirect(url);
    }
}