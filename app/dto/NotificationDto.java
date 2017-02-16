package dto;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import utils.DateTimeUtils;

public class NotificationDto
{

    public String locationName;
    public String category;
    public String type;
    public String uuid;
    public String published;
    public BigDecimal price;
    public BigDecimal amount;
    public String created;
    public String createdTime;
    public String createdDate;
    public Boolean isAuction;

    public String formatDate(Long date)
    {
        if (date != null)
            return new SimpleDateFormat(DateTimeUtils.TYPE_EUROPE_SEC).format(new Date(date));
        return null;
    }

    public String formatDate(Date date)
    {
        if (date != null)
            return new SimpleDateFormat(DateTimeUtils.TYPE_EUROPE_SEC).format(date);
        return null;
    }

    public String formatTime(Date date)
    {
        if (date != null)
            return new SimpleDateFormat(DateTimeUtils.TYPE_TIME_ONLY_EUROPE).format(date);
        return null;
    }

    public String formatDateOnly(Date date)
    {
        if (date != null)
            return new SimpleDateFormat(DateTimeUtils.TYPE_DATE_ONLY_EUROPE).format(date);
        return null;
    }

}
