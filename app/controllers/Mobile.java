package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.Ad;
import models.AdWatch;
import models.Auction;
import models.FilterAd;
import models.User;
import utils.JsonUtils;
import utils.NumberUtils;

import com.google.gson.JsonObject;

import dto.NotificationDto;

public class Mobile extends BaseController
{
    public static void notifications(String login)
    {
        User user = User.getUserByLogin(login);
        if (user != null)
        {
            FilterAd filterAd = new FilterAd();
            filterAd.categoryList = user.notificationCategory;
            filterAd.typeList = user.notificationType;
            Date notificationLastMobile = user.notificationLastMobile;
            if (notificationLastMobile == null)
                notificationLastMobile = new Date();

            filterAd.published = new Date(notificationLastMobile.getTime());
            //Logger.error(filterAd.toString());
            List<NotificationDto> nlist = new ArrayList<NotificationDto>();

            List<Ad> ads = Ad.getFilteredAds(filterAd, null, null, null, null);
            for (Ad ad : ads)
            {
                NotificationDto n = new NotificationDto();
                n.uuid = ad.uuid;
                n.price = ad.price;
                n.amount = ad.amount;
                n.type = ad.type;
                n.category = ad.category;
                n.published = n.formatDate(ad.published);
                n.created = n.formatDate(new Date());
                n.createdTime = n.formatTime(new Date());
                n.createdDate = n.formatDateOnly(new Date());
                n.isAuction = false;
                nlist.add(n);
            }

            Date from = new Date(notificationLastMobile.getTime());
            List<Auction> auctions = Auction.getAuctionsAfter(from);
            for (Auction auction : auctions)
            {
                Ad ad = auction.ad;
                // create notification only if the ad is on the watchlist (favorites)
                AdWatch adWatch = AdWatch.getByUserAd(user, ad);
                if (adWatch != null)
                {
                    NotificationDto n = new NotificationDto();
                    n.isAuction = true;
                    n.uuid = ad.uuid;
                    n.price = ad.price;
                    n.amount = ad.amount;
                    n.type = ad.type;
                    n.category = ad.category;
                    n.published = n.formatDate(ad.published);
                    n.created = n.formatDate(new Date());
                    n.createdTime = n.formatTime(new Date());
                    n.createdDate = n.formatDateOnly(new Date());
                    nlist.add(n);
                }
            }

            user.notificationLastMobile = new Date();
            user.save();
            renderJSON(nlist);
        }

        response.status = 404;
        notFound();
    }

    public static void newAd()
    {
        final JsonObject jo = JsonUtils.getJson(request.body);
        final String login = jo.get("login").getAsString();
        final String password = jo.get("password").getAsString();
        final User user = User.getUserByLogin(login);

        if (user != null && user.password.equals(password))
        {
            String type = jo.get("type").getAsString();
            String buySell = jo.get("buySell").getAsString();
            String category = jo.get("category").getAsString();
            String subCategory = jo.get("subCategory").getAsString();
            String price = jo.get("price").getAsString();
            String uuid = jo.get("uuid").getAsString();
            String amount = jo.get("amount").getAsString();
            String priceType = jo.get("priceType").getAsString();
            String description = jo.get("description") != null ? jo.get("description").getAsString() : null;

            Ad ad = Ad.getAdByUUID(uuid);
            if (ad == null)
                ad = new Ad();
            ad.user = user;
            ad.active = false;
            ad.type = type;
            ad.category = category;
            ad.subCategory = subCategory;
            ad.buySell = buySell;
            ad.created = new Date();
            ad.priceType = priceType;
            ad.description = description;
            ad.uuid = uuid;
            ad.state = Ad.STATE_UNPUBLISHED;
            if (amount != null)
                ad.amount = NumberUtils.parseDecimal(amount);
            if (price != null)
                ad.price = NumberUtils.parseDecimal(price);
            ad.save();

            System.err.println("new Ad save success");
            renderJSON("{\"success\":\"" + ad.uuid + "\"}");
        }

        response.status = 404;
        renderJSON("{\"success\":\"false\"}");
    }
}