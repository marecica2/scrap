package quartz;

import models.Ad;

public class RunUnscheduleNotification extends play.jobs.Job
{
    private final Ad ad;

    public RunUnscheduleNotification(Ad ad)
    {
        this.ad = ad;
    }

    @Override
    public void doJob() throws Exception
    {
        QuartzAdServise.scheduleNotification(ad, true);
    }
}
