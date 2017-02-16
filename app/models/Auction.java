package models;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;
import utils.DateTimeUtils;

@Entity
public class Auction extends Model
{
    @ManyToOne
    public User user;

    @ManyToOne
    public Ad ad;

    public BigDecimal offer;

    public String uuid;

    public Date created;

    public Boolean show;

    public static Auction getPaidAdForUser(String user, String ad)
    {
        return Auction.find("byUserAndAd", user, ad).first();
    }

    public static Auction getByUuid(String uuid)
    {
        return Auction.find("byUuid", uuid).first();
    }

    public static List<Auction> getAuctionByAd(Ad ad)
    {
        return Auction.find("from Auction where ad = ? order by created desc", ad).fetch();
    }

    public static List<Auction> getAuctionsAfter(Date date)
    {
        return Auction.find("from Auction where created >= ? order by created asc", date).fetch();
    }

    public String formatDate(Date date)
    {
        return new SimpleDateFormat(DateTimeUtils.TYPE_EUROPE).format(date);
    }
}
