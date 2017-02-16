package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;

@Entity
public class AdWatch extends Model
{
    @ManyToOne
    public User user;

    @ManyToOne
    public Ad ad;

    public Date created;

    public String uuid;

    public Boolean active;

    public static AdWatch getByUuid(String uuid)
    {
        return AdWatch.find("from AdWatch where uuid = ? ", uuid).first();
    }

    public static AdWatch getByUserAd(User user, Ad ad)
    {
        return AdWatch.find("from AdWatch where user = ? and ad = ? order by created desc", user, ad).first();
    }

    public static List<AdWatch> getByAd(Ad ad)
    {
        return AdWatch.find("from AdWatch where ad = ? order by created desc", ad).fetch();
    }

    public static List<AdWatch> getByUser(User user)
    {
        return AdWatch.find("from AdWatch where user = ? order by created desc", user).fetch();
    }

}
