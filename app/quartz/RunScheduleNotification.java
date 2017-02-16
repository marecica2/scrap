package quartz;

import models.Ad;

public class RunScheduleNotification extends play.jobs.Job
{
    private final Ad ad;

    public RunScheduleNotification(Ad ad)
    {
        this.ad = ad;
    }

    @Override
    public void doJob() throws Exception
    {
        QuartzAdServise.scheduleNotification(ad, null);
    }
}
