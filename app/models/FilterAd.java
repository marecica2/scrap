package models;

import java.math.BigDecimal;

public class FilterAd extends Ad
{
    public BigDecimal priceFrom;

    public BigDecimal priceTo;

    public BigDecimal amountFrom;

    public BigDecimal amountTo;

    public String keyword;

    public String typeList;

    public String categoryList;

    public String sorting;

    @Override
    public String toString()
    {
        return "FilterAd [published=" + published + ", priceFrom=" + priceFrom + ", priceTo=" + priceTo + ", amountFrom=" + amountFrom + ", amountTo=" + amountTo + ", keyword=" + keyword
                + ", typeList=" + typeList
                + ", categoryList=" + categoryList + "]";
    }

}
