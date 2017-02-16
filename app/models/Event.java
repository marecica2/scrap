package models;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;

import play.db.jpa.Model;
import utils.DateTimeUtils;

@Entity
public class Event extends Model
{
    public static final String TYPE_AD_WATCH = "001";
    public static final String TYPE_LME_WATCH = "002";
    public static final String TYPE_AUCTION_PLACE_BID = "003";
    public static final String TYPE_AUCTION_REVEAL = "005";
    public static final String TYPE_USER_AD_CREDIT = "004";

    @Column(name = "userUuid")
    public String user;

    public String ad;

    public String type;

    public BigDecimal credits;

    public String uuid;

    public Date created;

    public static Event getPaidAdForUser(String user, String ad)
    {
        return Event.find("byUserAndAd", user, ad).first();
    }

    public static Event getByUuid(String uuid)
    {
        return Event.find("byUuid", uuid).first();
    }

    public static List<Event> getEventsByUser(String user)
    {
        return Event.find("from Event where user = ? order by created desc", user).fetch(200);
    }

    public String formatDate(Date date)
    {
        if (date != null)
            return new SimpleDateFormat(DateTimeUtils.TYPE_EUROPE).format(date);
        return null;
    }
}
