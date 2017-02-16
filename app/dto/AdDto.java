package dto;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import models.Ad;
import models.FileUpload;
import models.User;
import utils.DateTimeUtils;

public class AdDto
{
    public User user;
    public String uuid;
    public Boolean active;
    public Long created;
    public Long modified;
    public Long published;
    public Long validTo;
    public String state;
    public String description;
    public String type;
    public String priceType;
    public String buySell;
    public String category;
    public String subCategory;
    public BigDecimal price;
    public BigDecimal amount;

    public List<FileUpload> fileUploads;
    public Double locationX;
    public Double locationY;

    public String locationName;
    public String locationStreetNumber;
    public String locationStreet;
    public String locationCity;
    public String locationCountry;

    public Double distance;
    public Boolean auction;
    public Boolean expired;

    public static AdDto convert(Ad ad)
    {
        AdDto a = new AdDto();
        a.active = ad.active;
        a.amount = ad.amount;
        a.auction = ad.auction;
        a.buySell = ad.buySell;
        a.category = ad.category;
        if (ad.created != null)
            a.created = ad.created.getTime();
        a.description = ad.description;
        a.distance = ad.distance;
        a.expired = ad.isExpired();
        a.fileUploads = ad.fileUploads;
        a.locationCity = ad.locationCity;
        a.locationCountry = ad.locationCountry;
        a.locationName = ad.locationName;
        a.locationStreet = ad.locationStreet;
        a.locationStreetNumber = ad.locationStreetNumber;
        a.locationX = ad.locationX;
        a.locationY = ad.locationY;
        if (ad.modified != null)
            a.modified = ad.modified.getTime();
        a.price = ad.price;
        a.priceType = ad.priceType;
        if (ad.published != null)
            a.published = ad.published.getTime();
        a.state = ad.state;
        a.subCategory = ad.subCategory;
        a.type = ad.type;
        a.user = ad.user;
        a.uuid = ad.uuid;
        if (ad.validTo != null)
            a.validTo = ad.validTo.getTime();
        return a;
    }

    public String formatDate(Long date)
    {
        if (date != null)
            return new SimpleDateFormat(DateTimeUtils.TYPE_EUROPE).format(new Date(date));
        return null;
    }

    public String formatDate(Date date)
    {
        if (date != null)
            return new SimpleDateFormat(DateTimeUtils.TYPE_EUROPE).format(date);
        return null;
    }

}
