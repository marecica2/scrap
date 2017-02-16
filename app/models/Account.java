package models;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Table;

import play.db.jpa.Model;

@Entity
@Table(name = "Account")
public class Account extends Model
{
    public static final String PERSONAL = "personal";
    public static final String BUSINESS = "business";

    public String uuid;

    public BigDecimal credit;

    public String type;

    public String phone;

    public String cellPhone;

    public String fax;

    public String email;

    public String companyName;

    public String companyTaxId;

    public String companyId;

    public String billingAddressStreet;

    public String billingAddressCity;

    public String billingAddressState;

    public Date modified;

    public Date lastPayment;

    public static Account getUserByUUID(String uuid)
    {
        return Account.find("byUuid", uuid).first();
    }

    @Override
    public String toString()
    {
        return "Account [uuid=" + uuid + ", credit=" + credit + ", type=" + type + ", email=" + email + "]";
    }

}
