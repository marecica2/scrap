package utils;

public class GeoDistance
{

    // System.err.println(GeoDistance.distance(48.6885765, 21.3497424, 
    // ad.locationY, ad.locationX, "K") + " Kilometers\n");

    // locY = rovnobezka pre nas 48 latitude
    // locX = poludnik 21 longitude
    /**
     * @param lat1 = x1
     * @param lon1 = y1
     * @param lat2 = x2
     * @param lon2 = y2
     * @param unit = K
     * @return
     */
    public static Double distance(Double lat1, Double lon1, Double lat2, Double lon2, String unit)
    {
        if (lat1 != null && lat2 != null && lon1 != null && lon2 != null)
        {
            double theta = lon1 - lon2;
            double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
            dist = Math.acos(dist);
            dist = rad2deg(dist);
            dist = dist * 60 * 1.1515;
            if (unit.equals("K"))
            {
                dist = dist * 1.609344;
            } else if (unit.equals("N"))
            {
                dist = dist * 0.8684;
            }
            return round(dist, 2);
        }
        return null;
    }

    private static double round(double value, int places)
    {
        if (places < 0)
            throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    private static double deg2rad(double deg)
    {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad)
    {
        return (rad * 180 / Math.PI);
    }
}
