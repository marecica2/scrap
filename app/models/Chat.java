package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;

@Entity
public class Chat extends Model
{
    @ManyToOne
    public User user;

    @ManyToOne
    public User admin;

    public String text;

    public Date created;

    public String uuid;

    public Boolean inverted;

    public static List<Chat> getByUser(User user)
    {
        return Chat.find("from Chat where user = ? order by created desc", user).fetch(200);
    }

    public static List<Chat> getAll(User user)
    {
        return Chat.find("from Chat order by created desc").fetch(500);
    }
}
