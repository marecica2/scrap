package models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;
import utils.DateTimeUtils;

@Entity
public class Rating extends Model
{
    @ManyToOne
    public User user;

    @ManyToOne
    public User ratedUser;

    public String uuid;

    public String description;

    public Date created;

    public Integer stars;

    public String reason;

    public static Rating getUuid(String uuid)
    {
        return Rating.find("byUuid", uuid).first();
    }

    public static List<Rating> getRatingsForUser(User user)
    {
        return Rating.find("from Rating where ratedUser = ? order by created desc", user).fetch(100);
    }

    public String formatDate(Date date)
    {
        return new SimpleDateFormat(DateTimeUtils.TYPE_EUROPE).format(date);
    }
}
