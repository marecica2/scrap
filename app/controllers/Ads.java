package controllers;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.Ad;
import models.AdWatch;
import models.Auction;
import models.Event;
import models.FileUpload;
import models.FilterAd;
import models.Rating;
import models.User;
import play.Logger;
import play.i18n.Messages;
import quartz.RunScheduleNotification;
import quartz.RunUnscheduleNotification;
import utils.Credits;
import utils.GeoDistance;
import utils.MyStringUtils;
import utils.NumberUtils;
import utils.RandomUtil;
import dto.AdDto;

public class Ads extends BaseController
{
    public static void ad(String uuid)
    {
        try
        {
            User user = getCachedUser();
            if (user == null)
                user = getUserFromAuthorizationHeader();

            Ad adObj = Ad.getAdByUUID(uuid);
            adObj.locationName = adObj.getAddress();
            AdWatch adWatch = user != null ? AdWatch.getByUserAd(user, adObj) : null;
            Boolean paid = checkPayment(user, adObj);
            Boolean isOwner = false;
            if (user != null && user.equals(adObj.user))
                isOwner = true;

            // get distance
            Double locationX = user != null ? user.locationX : null;
            Double locationY = user != null ? user.locationY : null;
            adObj.distance = GeoDistance.distance(adObj.locationX, adObj.locationY, locationX, locationY, "K");

            // get auctions and offer value
            List<Auction> auctions = Auction.getAuctionByAd(adObj);
            BigDecimal maxOffer = auctions != null && auctions.size() > 0 ? auctions.get(0).offer : adObj.price;
            BigDecimal newOffer = maxOffer.add(adObj.price.multiply(Credits.AUCTION_PERCENTAGE));
            newOffer = newOffer.setScale(1, BigDecimal.ROUND_UP);

            adObj.auction = adObj.isAuction();

            // get ratings, average rating
            List<Rating> ratings = Rating.getRatingsForUser(adObj.user);
            boolean rated = false;
            for (Rating rating : ratings)
            {
                if (rating.user.equals(user))
                {
                    rated = true;
                    break;
                }
            }
            Integer avg = calculateAverageRating(ratings);
            adObj.user.avg = avg;

            // get expiration
            if (adObj.validTo != null)
            {
                adObj.active = System.currentTimeMillis() >= adObj.validTo.getTime() ? false : true;
                if (adObj.active == false)
                    adObj.state = Ad.STATE_EXPIRED;
            }

            AdDto ad = AdDto.convert(adObj);
            render(user, ad, paid, isOwner, ratings, rated, auctions, maxOffer, newOffer, adWatch);
        } catch (Exception e)
        {
            Logger.error(e, e.getMessage());
            notFound();
        }
    }

    private static Boolean checkPayment(User user, Ad ad)
    {
        Boolean paid = false;
        if (ad.type.equals(Ad.TYPE_JOB) || ad.type.equals(Ad.TYPE_TRANSIT) || ad.amount.compareTo(new BigDecimal("20")) < 0)
        {
            paid = true;
        } else if (user != null && user.equals(ad.user))
        {
            paid = true;
        } else
        {
            if (user != null)
            {
                Event event = Event.getPaidAdForUser(user.uuid, ad.uuid);
                if (event != null)
                    paid = true;
            }
        }
        return paid;
    }

    public static void adList()
    {
        User user = getCachedUser();
        render(user);
    }

    public static void adListUser() throws InterruptedException
    {
        User user = getCachedUser();
        Integer first = request.params.get("first") != null ? Integer.parseInt(request.params.get("first")) : 0;
        Integer count = request.params.get("count") != null ? Integer.parseInt(request.params.get("count")) : 10;

        FilterAd filterAd = new FilterAd();
        filterAd.user = user;
        filterAd.sorting = "timeDesc";
        List<Ad> adsList = Ad.getFilteredAds(filterAd, first, count, null, null);
        List<AdDto> ads = new ArrayList<AdDto>();
        for (Ad ad : adsList)
        {
            final AdDto aDto = AdDto.convert(ad);
            ads.add(aDto);
        }
        renderJSON(ads);
    }

    public static void adPublish(String uuid, String url)
    {
        User user = getCachedUser();
        Ad ad = Ad.getAdByUUID(uuid);
        if (!ad.user.equals(user))
            unauthorized();

        validation.required(ad.price);
        validation.required(ad.amount);
        validation.required(ad.priceType);
        validation.required(ad.buySell);

        if (!validation.hasErrors())
        {
            ad.state = Ad.STATE_PUBLISHED;
            ad.published = new Date();

            // default validity is 7 days
            if (Ad.TYPE_AUCTION.equals(ad.priceType))
            {
                ad.validTo = new Date(System.currentTimeMillis() + Ad.AUCTION_DURATION_DAYS);
            } else
                ad.validTo = new Date(System.currentTimeMillis() + Ad.AD_DURATION_DAYS);
            ad.save();

            List<Auction> auctions = Auction.getAuctionByAd(ad);
            for (Auction auction : auctions)
                auction.delete();

            flash.success(Messages.get("system.adpublished"));

            // schedule asynchronously
            new RunScheduleNotification(ad).now();

            redirect(url);
        } else
        {
            flash.error(Messages.get("system.invalidAd"));
            redirect(url);
        }

    }

    public static void adUnpublish(String uuid, String url)
    {
        User user = getCachedUser();
        Ad ad = Ad.getAdByUUID(uuid);
        if (!ad.user.equals(user))
            unauthorized();

        ad.state = Ad.STATE_UNPUBLISHED;
        ad.validTo = null;
        ad.save();

        // unschedule asynchronously
        new RunUnscheduleNotification(ad).now();

        redirect(url);
    }

    public static void auctionReveal(String uuid, String url)
    {
        User user = getNotCachedUser();
        clearCachedUser();
        Auction auction = Auction.getByUuid(uuid);
        // if enough credit then substract
        if (user.account.credit.compareTo(Credits.CREDIT_FOR_AD) > 0)
        {
            user.account.credit = user.account.credit.subtract(Credits.CREDIT_FOR_AD_REVEAL);
            user.account.save();

            auction.show = true;
            auction.save();

            Event e = new Event();
            e.type = Event.TYPE_AUCTION_REVEAL;
            e.created = new Date();
            e.credits = Credits.CREDIT_FOR_AD_REVEAL;
            e.user = user.uuid;
            e.ad = auction.ad.uuid;
            e.uuid = RandomUtil.getUUID();
            e.save();
            flash.success(Messages.get("system.subcredit", Credits.CREDIT_FOR_AD_REVEAL));
        } else
        {
            flash.error(Messages.get("system.nocredit"));
        }
        redirect(url);
    }

    public static void auctionAdd(String uuid, String url, String offer)
    {
        User user = getNotCachedUser();
        Ad ad = Ad.getAdByUUID(uuid);
        if (ad.user.equals(user))
            unauthorized();

        BigDecimal auctionOffer = NumberUtils.parseDecimal(offer);
        List<Auction> auctions = Auction.getAuctionByAd(ad);
        BigDecimal maxOffer = auctions != null && auctions.size() > 0 ? auctions.get(0).offer : ad.price;
        BigDecimal newOffer = maxOffer.add(ad.price.multiply(Credits.AUCTION_PERCENTAGE));
        newOffer = newOffer.setScale(1, BigDecimal.ROUND_UP);

        if (auctionOffer.compareTo(newOffer) < 0)
        {
            validation.addError("", Messages.get("system.invalid"));
            flash.error(Messages.get("system.invalid"));
        }

        if (user.account.credit.compareTo(Credits.CREDIT_LIMIT_FOR_AUCTION) <= 0)
        {
            validation.addError("", Messages.get("system.nocredit.limit", Credits.CREDIT_LIMIT_FOR_AUCTION));
            flash.error(Messages.get("system.nocredit.limit", Credits.CREDIT_LIMIT_FOR_AUCTION));
        }

        if (!validation.hasErrors())
        {
            Auction a = new Auction();
            a.ad = ad;
            a.user = user;
            a.offer = auctionOffer;
            a.uuid = RandomUtil.getUUID();
            a.created = new Date();
            a.save();

            // ad auction to watched if it is not
            AdWatch watch = AdWatch.getByUserAd(user, ad);
            if (watch == null)
            {
                watch = new AdWatch();
                watch.uuid = RandomUtil.getUUID();
                watch.user = user;
                watch.ad = ad;
                watch.active = true;
                watch.created = new Date();
                watch.save();
            }

            // save max offer
            ad.lastOffer = new Date();
            ad.modified = new Date();
            ad.maxPrice = auctionOffer;
            ad.validTo = new Date(System.currentTimeMillis() + Ad.AUCTION_DURATION_DAYS);
            ad.offers = ad.offers == null ? 1 : (ad.offers + 1);
            ad.save();

            // if enough credit then substract
            user.account.credit = user.account.credit.subtract(Credits.CREDIT_FOR_AD);
            user.account.save();
            clearCachedUser();

            Event e = new Event();
            e.type = Event.TYPE_AUCTION_PLACE_BID;
            e.created = new Date();
            e.credits = Credits.CREDIT_FOR_AUCTION;
            e.user = user.uuid;
            e.ad = uuid;
            e.uuid = RandomUtil.getUUID();
            e.save();
            flash.success(Messages.get("system.bidsuccess"));
        }

        redirect(url);
    }

    public static void payForAd(String ad, String url)
    {
        User user = getNotCachedUser();
        // if enough credit then substract
        if (user.account.credit.compareTo(Credits.CREDIT_FOR_AD) > 0)
        {
            user.account.credit = user.account.credit.subtract(Credits.CREDIT_FOR_AD);
            user.account.save();

            Event e = new Event();
            e.type = Event.TYPE_AD_WATCH;
            e.created = new Date();
            e.credits = Credits.CREDIT_FOR_AD;
            e.user = user.uuid;
            e.ad = ad;
            e.uuid = RandomUtil.getUUID();
            e.save();
            flash.success(Messages.get("system.adsuccess"));
        } else
        {
            flash.error(Messages.get("system.nocredit"));
        }

        BaseController.clearCachedUser();
        redirect(url);
    }

    public static void getAds(
        String type,
        String category,
        String subCategory,
        String buySell,
        String sorting,
        String priceType,
        Boolean profile,
        BigDecimal filterPriceFrom,
        BigDecimal filterPriceTo,
        BigDecimal filterAmountFrom,
        BigDecimal filterAmountTo,
        Double distance,
        String x,
        String y,
        String transit,
        String keyword)
        throws InterruptedException, UnsupportedEncodingException
    {
        Integer first = request.params.get("first") != null ? Integer.parseInt(request.params.get("first")) : 0;
        Integer count = request.params.get("count") != null ? Integer.parseInt(request.params.get("count")) : 10;
        Thread.sleep(100);

        User user = getCachedUser();
        Double locationY = user != null ? user.locationY : null;
        Double locationX = user != null ? user.locationX : null;
        if (transit != null)
        {
            locationX = NumberUtils.parseDouble(x);
            locationY = NumberUtils.parseDouble(y);
        }

        FilterAd filterAd = new FilterAd();
        filterAd.type = "".equals(type) ? null : type;
        filterAd.priceType = priceType;
        filterAd.buySell = buySell;
        filterAd.category = "".equals(category) ? null : category;
        filterAd.subCategory = "".equals(subCategory) ? null : subCategory;
        filterAd.state = Ad.STATE_PUBLISHED;
        filterAd.priceFrom = filterPriceFrom;
        filterAd.priceTo = filterPriceTo;
        filterAd.amountFrom = filterAmountFrom;
        filterAd.sorting = sorting;
        filterAd.amountTo = filterAmountTo;
        filterAd.distance = distance;
        if (keyword != null)
            filterAd.keyword = URLDecoder.decode(keyword, "UTF-8").replaceFirst("\\s", "");

        List<Ad> adsList = Ad.getFilteredAds(filterAd, first, count, locationY, locationX);
        List<AdDto> ads = new ArrayList<AdDto>();
        for (Ad ad : adsList)
        {
            if (!ad.isExpired())
            {
                ad.locationName = ad.getAddress();
                final AdDto aDto = AdDto.convert(ad);
                aDto.user = null;
                aDto.locationX = null;
                aDto.locationY = null;
                aDto.locationCity = null;
                aDto.locationStreet = null;
                aDto.locationStreetNumber = null;
                aDto.locationName = null;
                ads.add(aDto);
            }
        }

        renderJSON(ads);
    }

    public static void map()
    {
        render();
    }

    public static void storeLocation(String uuid)
    {
        String xCoord = request.params.get("x");
        String yCoord = request.params.get("y");
        String location = request.params.get("location");
        Ad ad = Ad.getAdByUUID(uuid);
        if (ad != null)
        {
            ad.locationX = Double.parseDouble(xCoord);
            ad.locationY = Double.parseDouble(yCoord);
            ad.locationName = location;
            ad.save();
        }
    }

    public static void editAd(String uuid)
    {
        User user = getCachedUser();
        Ad ad = Ad.getAdByUUID(uuid);
        ad.locationName = ad.getAddress();
        String edit = uuid;

        params.put("description", ad.description);
        params.put("type", ad.type);
        params.put("buySell", ad.buySell);
        params.put("category", ad.category);
        params.put("subCategory", ad.subCategory);
        if (ad.amount != null)
            params.put("amount", ad.amount.toString());
        if (ad.price != null)
            params.put("price", ad.price.toString());
        if (ad.locationX != null)
            params.put("x", ad.locationX.toString());
        if (ad.locationY != null)
            params.put("y", ad.locationY.toString());
        params.put("location", ad.getAddress());
        params.put("buySell", ad.buySell);
        params.put("priceType", ad.priceType);

        final List<FileUpload> files = ad.fileUploads;
        params.flash();
        render("Ads/newAd.html", user, edit, ad, files);
    }

    public static void deleteAd(String uuid)
    {
        Ad ad = Ad.getAdByUUID(uuid);

        List<FileUpload> fileUploads = ad.fileUploads;
        for (FileUpload fileUpload : fileUploads)
            fileUpload.delete();

        List<Auction> auctions = Auction.getAuctionByAd(ad);
        for (Auction auction : auctions)
            auction.delete();

        List<AdWatch> watches = AdWatch.getByAd(ad);
        for (AdWatch adWatch : watches)
            adWatch.delete();

        ad.delete();
        redirect("/profile");
    }

    public static void newAd()
    {
        params.put("buySell", "sell");
        params.put("priceType", "001");
        params.flash();
        String temp = RandomUtil.getUUID();
        User user = getCachedUser();
        render(user, temp);
    }

    public static void newAdPost(
        String type,
        String category,
        String subCategory,
        String buySell,
        String priceType,
        String amount,
        String price,
        String description,
        String temp,
        String edit,
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
        validation.required(buySell);
        validation.required(description);
        if (Ad.TYPE_SCRAP.equals(type))
        {
            validation.required(category);
            validation.required(subCategory);
        }

        if (Ad.TYPE_SCRAP.equals(type) || Ad.TYPE_MIXED.equals(type) || Ad.TYPE_SCRAP.equals(type))
            validation.required(amount);

        if (Ad.TYPE_SCRAP.equals(type) || Ad.TYPE_MIXED.equals(type) || Ad.TYPE_SCRAP.equals(type) || Ad.TYPE_AUCTION.equals(priceType))
            validation.required(price);

        validation.required(location);

        User user = getCachedUser();
        if (!validation.hasErrors())
        {
            Ad ad = new Ad();
            ad.state = Ad.STATE_UNPUBLISHED;
            ad.uuid = RandomUtil.getUUID();
            ad.user = user;
            ad.active = false;
            ad.created = new Date();
            ad.modified = new Date();

            if (edit != null)
            {
                ad = Ad.getAdByUUID(edit);
            }
            ad.type = MyStringUtils.htmlEscape(type);
            ad.buySell = MyStringUtils.htmlEscape(buySell);
            ad.priceType = priceType;
            if (ad.type.equals(Ad.TYPE_SCRAP))
            {
                ad.category = MyStringUtils.htmlEscape(category);
                ad.subCategory = MyStringUtils.htmlEscape(subCategory);
            } else
            {
                ad.category = null;
                ad.subCategory = null;
            }
            ad.amount = NumberUtils.parseDecimal(amount);
            ad.price = NumberUtils.parseDecimal(price);
            ad.description = MyStringUtils.htmlEscape(description);
            if (x != null && !"".equals(x))
                ad.locationX = Double.parseDouble(x);
            if (y != null && !"".equals(y))
                ad.locationY = Double.parseDouble(y);

            if (location != null && location.length() > 0)
                ad.locationName = location;

            if (street != null && street.length() > 0)
                ad.locationStreet = street;

            if (streetNumber != null && streetNumber.length() > 0)
                ad.locationStreetNumber = streetNumber;

            if (location != city && city.length() > 0)
                ad.locationCity = city;

            if (location != country && country.length() > 0)
                ad.locationCountry = country;

            ad.save();

            // append temp files to new Ad
            if (temp != null)
            {
                final List<FileUpload> files = FileUpload.getByTemp(temp);
                for (FileUpload file : files)
                {
                    FileUpload f = FileUpload.getByUuid(file.uuid);
                    if (f != null)
                    {
                        f.ad = ad.getId();
                        f.temp = null;
                        f.save();
                    }
                }
            }
            redirect("/profile");
        } else
        {
            List<FileUpload> files = null;
            if (edit != null)
            {
                Ad ad = Ad.getAdByUUID(edit);
                files = ad.fileUploads;
            } else
            {
                files = FileUpload.getByTemp(temp);
            }
            params.flash();
            render("Ads/newAd.html", user, temp, files, edit);
        }
    }

    public static Integer calculateAverageRating(List<Rating> ratings)
    {
        Integer avg = 0;
        for (Rating rating : ratings)
            avg += rating.stars == null ? 0 : rating.stars;
        if (ratings.size() > 0)
            avg = avg / ratings.size();
        return avg;
    }
}