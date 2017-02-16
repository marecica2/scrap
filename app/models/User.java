package models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;

import play.db.jpa.Model;
import utils.DateTimeUtils;

@Entity
@Table(name = "\"user\"")
public class User extends Model
{
    public static final String ROLE_SUPERADMIN = "superadmin";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    @ManyToOne
    public Account account;

    public String uuid;

    public Boolean active;

    public String login;

    public String password;

    public String role;

    public String firstName;

    public String lastName;

    public String phone;

    public String cellPhone;

    public String fax;

    public String email;

    public String locale;

    public String timeZone;

    public String avatarUrl;

    public String avatarUrlSmall;

    public Date modified;

    public Integer lmePeriod;

    public String notificationHour;

    public String notificationDay;

    public String notificationType;

    public String notificationCategory;

    public Date notificationLastMobile;

    public Boolean notificationEnabled;

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
    public Integer avg;

    public Boolean online;

    public boolean isAdmin()
    {
        if (this.role != null && (this.role.equals(ROLE_SUPERADMIN)))
            return true;
        return false;
    }

    public String getFullName()
    {
        return firstName + " " + lastName;
    }

    public static User getUserByLogin(String login)
    {
        return User.find("byLogin", login).first();
    }

    public static List<Object> getUserByLogin2(String login)
    {
        return User.em().createQuery("from User where login = ? ").setParameter(1, login).getResultList();
    }

    public static List<User> getNotifiedUsers(Ad ad)
    {
        String sql = "from User where notificationEnabled = true and (notificationType like :type or notificationCategory like :category)";
        Query query = User.em().createQuery(sql);
        query = query.setParameter("type", "%" + ad.type + "%");
        query = query.setParameter("category", "%" + ad.category + "%");
        return query.getResultList();
    }

    public static User getUserByUUID(String uuid)
    {
        return User.find("byUuid", uuid).first();
    }

    public static User getAdminUser()
    {
        return User.find("byRole", ROLE_SUPERADMIN).first();
    }

    public String getAddress()
    {
        return (locationStreet != null ? locationStreet : "") +
                (locationStreetNumber != null ? " " + locationStreetNumber : "") +
                (locationCity != null ? ", " + locationCity : "") +
                (locationCountry != null ? ", " + locationCountry : "");
    }

    @Override
    public String toString()
    {
        return "User [login=" + login + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (uuid == null)
        {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

    public String formatDate(Date date)
    {
        if (date != null)
            return new SimpleDateFormat(DateTimeUtils.TYPE_EUROPE).format(date);
        return null;
    }

    public static List<User> findAllUsers()
    {
        return User.find("from User where 1 = 1 order by Id asc").fetch();
    }

    public String getUserEmail()
    {
        if (this.email != null)
            return this.email;
        return this.login;
    }

}
