package controllers;

import java.math.BigDecimal;
import java.util.Date;

import models.Account;
import models.User;
import play.cache.Cache;
import play.i18n.Messages;
import utils.MyStringUtils;
import utils.RandomUtil;
import email.EmailProvider;

//@With(controllers.Secure.class)
public class Registration extends BaseController
{
    public static void registration()
    {
        String uuid = RandomUtil.getUUID();
        params.put("uuid", uuid);
        params.flash();
        render(uuid);
    }

    public static void registrationPost(
        String type,
        String captcha,
        String uuid,

        String login,
        String firstName,
        String lastName,
        String password,
        String passwordRepeat,

        String phone,
        String cellPhone,
        String fax,
        String email,
        String companyName,
        String companyId,
        String companyTaxId,
        String billingAddressStreet,
        String billingAddressCity,
        String billingAddressState
        )
    {
        validation.required(type);
        validation.required(firstName);
        validation.required(lastName);
        validation.email(login).message("validation.login");
        validation.required(login);
        validation.required(password);
        validation.equals(password, passwordRepeat).message("validation.passwordMatch");
        validation.required(captcha);
        final Object cap = Cache.get("captcha." + uuid);
        if (captcha != null && cap != null)
        {
            validation.equals(captcha, cap).message("invalid.captcha");
        }

        if (type.equals(Account.BUSINESS))
        {
            validation.required(phone);
            validation.required(cellPhone);
            validation.required(email);
            validation.required(companyName);
            validation.required(companyId);
            validation.required(companyTaxId);
            validation.required(billingAddressStreet);
            validation.required(billingAddressCity);
            validation.required(billingAddressState);
        }

        User checkUser = User.getUserByLogin(login);
        if (checkUser != null)
            validation.addError("login", "validation.account");

        if (!validation.hasErrors())
        {
            Account account = new Account();
            account.type = MyStringUtils.htmlEscape(type);
            account.phone = MyStringUtils.htmlEscape(phone);
            account.cellPhone = MyStringUtils.htmlEscape(cellPhone);
            account.fax = MyStringUtils.htmlEscape(fax);
            account.email = MyStringUtils.htmlEscape(email);
            account.companyId = MyStringUtils.htmlEscape(companyId);
            account.companyName = MyStringUtils.htmlEscape(companyName);
            account.companyTaxId = MyStringUtils.htmlEscape(companyTaxId);
            account.billingAddressStreet = MyStringUtils.htmlEscape(billingAddressStreet);
            account.billingAddressCity = MyStringUtils.htmlEscape(billingAddressCity);
            account.billingAddressState = MyStringUtils.htmlEscape(billingAddressState);
            account.uuid = RandomUtil.getUUID();
            account.credit = new BigDecimal("0");
            account.modified = new Date();
            account.save();

            User user = new User();
            user.modified = new Date();
            user.account = account;
            user.uuid = RandomUtil.getUUID();
            user.active = true;
            user.role = User.ROLE_ADMIN;
            user.login = login;
            user.password = password;
            user.firstName = MyStringUtils.htmlEscape(firstName);
            user.lastName = MyStringUtils.htmlEscape(lastName);
            user.cellPhone = MyStringUtils.htmlEscape(cellPhone);
            user.phone = MyStringUtils.htmlEscape(phone);
            user.fax = MyStringUtils.htmlEscape(fax);
            user.email = MyStringUtils.htmlEscape(email);
            user.save();

            flash.success(Messages.get("registration-msg"));
            try
            {
                new EmailProvider().sendEmail(Messages.get("registration-subject"), user.login, Messages.get("registration-email", user.login, user.password));
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            redirect("/login");
        } else
        {
            params.flash();
            render("Registration/registration.html");
        }
    }

    public static void password()
    {
        final User user = getCachedUser();
        render(user);
    }

    public static void passwordPost(String oldPassword, String password, String passwordRepeat)
    {
        final User user = getNotCachedUser();
        validation.required(oldPassword);
        validation.required(password);
        validation.equals(password, passwordRepeat).message("validation.passwordMatch");

        if (!oldPassword.equals(user.password))
            validation.addError("oldPassword", "validation.invalidPassword");

        if (!validation.hasErrors())
        {
            user.password = password;
            user.save();
            redirect("/login");
        } else
        {
            params.flash();
            render("Registration/password.html", user);
        }
    }

}