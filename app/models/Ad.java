package models;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import play.db.jpa.Model;
import utils.DateTimeUtils;

@Entity
public class Ad extends Model
{
    public static final long AD_DURATION_DAYS = (1000 * 60 * 60 * 24 * 7);
    public static final long AUCTION_DURATION_DAYS = (1000 * 60 * 60 * 24 * 2);

    public static final String STATE_UNPUBLISHED = "001";
    public static final String STATE_PUBLISHED = "002";
    public static final String STATE_EXPIRED = "003";

    public static final String TYPE_SCRAP = "001";
    public static final String TYPE_MIXED = "002";
    public static final String TYPE_DEMOLITION = "003";
    public static final String TYPE_USED = "004";
    public static final String TYPE_TRANSIT = "005";
    public static final String TYPE_JOB = "006";

    public static final String TYPE_FIXED_PRICE = "001";
    public static final String TYPE_AUCTION = "002";

    @ManyToOne
    public User user;

    public String uuid;

    public Boolean active;

    public Date created;

    public Date modified;

    public Date published;

    public Date validTo;

    public String state;

    public String description;

    public String type;

    public String priceType;

    public String buySell;

    public String category;

    public String subCategory;

    public BigDecimal price;

    public BigDecimal maxPrice;

    public Integer offers;

    public Date lastOffer;

    public BigDecimal amount;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "ad")
    public List<FileUpload> fileUploads;

    /**
     * longitude - poludnik
     */
    public Double locationX;

    /**
     * latitude - rovnobezka
     */
    public Double locationY;

    public String locationName;
    public String locationStreetNumber;
    public String locationStreet;
    public String locationCity;
    public String locationCountry;

    @Transient
    public Double distance;

    @Transient
    public Boolean auction;

    public Boolean isAuction()
    {
        return TYPE_AUCTION.equals(this.priceType);
    }

    public boolean isExpired()
    {
        if (this.published == null)
            return false;

        long expiry = this.published.getTime() + AD_DURATION_DAYS;
        if (TYPE_AUCTION.equals(this.priceType))
            expiry = this.published.getTime() + AUCTION_DURATION_DAYS;

        final long now = System.currentTimeMillis();

        if (this.state.equals(STATE_PUBLISHED) && now >= expiry)
            return true;

        if (this.state.equals(STATE_UNPUBLISHED))
            return true;

        if (this.state.equals(STATE_EXPIRED))
            return true;

        return false;
    }

    public String getAddress()
    {
        return (locationStreet != null ? locationStreet : "") +
                (locationStreetNumber != null ? " " + locationStreetNumber : "") +
                (locationCity != null ? ", " + locationCity : "") +
                (locationCountry != null ? ", " + locationCountry : "");
    }

    public static Ad getAdByUUID(String uuid)
    {
        return Ad.find("byUuid", uuid).first();
    }

    public static List<Ad> getAdByUUID2(String uuid)
    {
        return Ad.em().createQuery("from Ad where uuid = ? ").setParameter(1, uuid).getResultList();
    }

    public static List<Ad> getAdsByUser(User user)
    {
        return Ad.find("byUser", user).fetch();
    }

    public static List<Ad> getAdsAfter(Date after)
    {
        return Ad.find("state = ? and published >= ?", STATE_PUBLISHED, after).fetch();
    }

    public static List<Ad> getAdsByFilter(Ad ad)
    {
        return Ad.findAll();
    }

    public static List<Ad> getAuctionAds()
    {
        return Ad.find("state = ? and priceType = ? order by lastOffer desc ", Ad.STATE_PUBLISHED, Ad.TYPE_AUCTION).fetch();
    }

    public static List<Ad> getFilteredAds(FilterAd filterAd, Integer first, Integer count, Double startlat, Double startlng)
    {
        // SELECT latitude, longitude, SQRT(POW(69.1 * (latitude - [startlat]), 2) + POW(69.1 * ([startlng] - longitude) * COS(latitude / 57.3), 2)) AS distance FROM TableName HAVING distance < 25 ORDER BY distance;

        String query = "select a from Ad a where 1 = 1 and created > :created ";
        if (filterAd.distance != null)
            query = "select a, SQRT(POW(69.1 * (a.locationY - :startlat), 2) + POW(69.1 * (:startlng - a.locationX) * COS(a.locationY / 57.3), 2)) AS distance from Ad a where 1 = 1 and created > :created ";

        if (filterAd.validTo != null)
            query += " and validTo >= :validTo ";

        if (filterAd.type != null)
            query += " and type = :type ";

        if (filterAd.buySell != null)
            query += " and buySell = :buySell ";

        if (filterAd.priceType != null)
            query += " and priceType = :priceType ";

        if (filterAd.state != null)
            query += " and state = :state ";

        if (filterAd.category != null)
            query += " and category = :category ";

        if (filterAd.subCategory != null)
            query += " and subCategory = :subCategory ";

        query += " and (";
        if (filterAd.categoryList != null)
            query += " :categoryList like CONCAT('%',category,'%') ";

        if (filterAd.typeList != null)
            query += (filterAd.categoryList != null ? " or " : "") + " :typeList like CONCAT('%',type,'%') ";

        if (filterAd.typeList == null && filterAd.categoryList == null)
            query += " 1 = 1 ";
        query += " )";

        if (filterAd.user != null)
            query += " and user = :user ";

        if (filterAd.priceFrom != null)
            query += " and price >= :priceFrom ";

        if (filterAd.priceTo != null)
            query += " and price <= :priceTo ";

        if (filterAd.amountFrom != null)
            query += " and amount >= :amountFrom ";

        if (filterAd.amountTo != null)
            query += " and amount <= :amountTo ";

        if (filterAd.published != null)
            query += " and published >= :published ";

        if (filterAd.keyword != null)
            query += " and CONCAT(user.login,user.uuid, user.firstName, user.lastName, user.account.companyName) like :keyword ";

        if (filterAd.distance != null)
        {
            System.err.println(filterAd.distance);
            filterAd.distance = filterAd.distance * 0.62137;
            query += " and SQRT(POW(69.1 * (locationY - :startlat), 2) + POW(69.1 * (:startlng - locationX) * COS(locationY / 57.3), 2)) < :maxDist ";
        }

        if (filterAd.sorting != null && "timeDesc".equals(filterAd.sorting))
            query += " order by created desc ";
        if (filterAd.sorting != null && "timeAsc".equals(filterAd.sorting))
            query += " order by created asc ";
        if (filterAd.sorting != null && "priceDesc".equals(filterAd.sorting))
            query += " order by price desc ";
        if (filterAd.sorting != null && "priceAsc".equals(filterAd.sorting))
            query += " order by price asc ";
        if (filterAd.sorting != null && "amountDesc".equals(filterAd.sorting))
            query += " order by amount desc ";
        if (filterAd.sorting != null && "amountAsc".equals(filterAd.sorting))
            query += " order by amount asc ";
        if (filterAd.sorting == null)
            query += " order by created desc ";

        javax.persistence.Query q = Ad.em().createQuery(query);
        //Logger.error("Ad.getFilteredAds " + query);

        final Date beforeXDays = new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 60L));
        q.setParameter("created", beforeXDays);

        if (filterAd.validTo != null)
            q.setParameter("validTo", new Date());

        if (filterAd.type != null)
            q.setParameter("type", filterAd.type);

        if (filterAd.buySell != null)
            q.setParameter("buySell", filterAd.buySell);

        if (filterAd.priceType != null)
            q.setParameter("priceType", filterAd.priceType);

        if (filterAd.state != null)
            q.setParameter("state", filterAd.state);

        if (filterAd.category != null)
            q.setParameter("category", filterAd.category);

        if (filterAd.subCategory != null)
            q.setParameter("subCategory", filterAd.subCategory);

        if (filterAd.categoryList != null)
            q.setParameter("categoryList", filterAd.categoryList);

        if (filterAd.typeList != null)
            q.setParameter("typeList", filterAd.typeList);

        if (filterAd.user != null)
            q.setParameter("user", filterAd.user);

        if (filterAd.priceFrom != null)
            q.setParameter("priceFrom", filterAd.priceFrom);

        if (filterAd.priceTo != null)
            q.setParameter("priceTo", filterAd.priceTo);

        if (filterAd.amountFrom != null)
            q.setParameter("amountFrom", filterAd.amountFrom);

        if (filterAd.amountTo != null)
            q.setParameter("amountTo", filterAd.amountTo);

        if (filterAd.published != null)
            q.setParameter("published", filterAd.published);

        if (filterAd.keyword != null)
            q.setParameter("keyword", "%" + filterAd.keyword + "%");

        if (filterAd.distance != null)
        {
            q.setParameter("startlat", startlat);
            q.setParameter("startlng", startlng);
            q.setParameter("maxDist", filterAd.distance);
        }

        // pagination
        if (first != null)
            q.setFirstResult(first);
        if (count != null)
            q.setMaxResults(count);

        if (filterAd.distance != null)
        {
            final List resultList = q.getResultList();
            List<Ad> adList = new ArrayList<Ad>();
            for (Object object : resultList)
            {
                Object[] items = (Object[]) object;
                Ad ad = (Ad) items[0];
                ad.distance = (Double) items[1];
                if (ad.distance != null)
                    ad.distance = ad.distance * 1.60934;
                adList.add(ad);
            }
            return adList;
        } else
        {
            List<Ad> adList = q.getResultList();
            return adList;
        }
    }

    public String formatDate(Date date)
    {
        if (date != null)
            return new SimpleDateFormat(DateTimeUtils.TYPE_EUROPE).format(date);
        return null;
    }

    @Override
    public String toString()
    {
        return "Ad [user=" + user + ", uuid=" + uuid + ", active=" + active + ", created=" + created + ", modified=" + modified + ", published=" + published + ", validTo=" + validTo
                + ", state=" + state + ", description=" + description + ", type=" + type + ", priceType=" + priceType + ", buySell=" + buySell + ", category=" + category + ", subCategory="
                + subCategory + ", price=" + price + ", maxPrice=" + maxPrice + ", offers=" + offers + ", lastOffer=" + lastOffer + ", amount=" + amount + ", fileUploads=" + fileUploads
                + ", locationX=" + locationX + ", locationY=" + locationY + ", locationName=" + locationName + ", locationStreetNumber=" + locationStreetNumber + ", locationStreet="
                + locationStreet + ", locationCity=" + locationCity + ", locationCountry=" + locationCountry + ", distance=" + distance + ", auction=" + auction + "]";
    }
}
