package quartz;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import models.Ad;
import models.User;

import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import play.Logger;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import email.Templates;

@OnApplicationStart
public class QuartzAdServise extends Job
{
    public static final String AD_NOTIFICATION_GROUP = "adNotification";
    public static Scheduler scheduler = null;

    @Override
    public void doJob() throws Exception
    {
        try
        {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            listJobs();
            Logger.info("Quartz has been initialized");
        } catch (SchedulerException se)
        {
            Logger.error(se, "Error in Quartz");
        }
    }

    public static List<String> listJobs() throws SchedulerException
    {
        List<String> jobs = new ArrayList<String>();
        Logger.info("Quartz jobs:");

        for (String group : scheduler.getJobGroupNames())
        {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group)))
            {
                Trigger oldTrigger = scheduler.getTrigger(new TriggerKey(jobKey.getName()));
                if (oldTrigger != null)
                {
                    Logger.info("Found scheduled job: " + jobKey + " " + oldTrigger.getNextFireTime());
                    printMap(scheduler.getJobDetail(jobKey).getJobDataMap());
                }
                jobs.add(jobKey.getName() + " " + jobKey.getGroup() + " " + oldTrigger.getNextFireTime());
            }
        }
        return jobs;
    }

    public static void clearJobs() throws SchedulerException
    {
        Logger.info("Clearing jobs:");
        scheduler.clear();
    }

    public static void scheduleNotification(Ad ad, Boolean unschedule)
    {
        final List<User> users = User.getNotifiedUsers(ad);
        for (User user : users)
        {
            final String scheduleKey = user.login;

            // validation
            if (user.notificationEnabled == null || !user.notificationEnabled)
                return;
            if (user.notificationHour == null || user.notificationHour.length() == 0)
                return;
            if (user.notificationDay == null || user.notificationDay.length() == 0)
                return;

            try
            {
                // get existing trigger
                final JobDetail oldJobDetail = scheduler.getJobDetail(new JobKey(scheduleKey));

                // set data
                JobDataMap data = new JobDataMap();
                if (oldJobDetail != null)
                    data = oldJobDetail.getJobDataMap();

                if (unschedule != null && unschedule)
                {
                    unschedule(ad, user, scheduleKey, data);
                }
                else
                {
                    schedule(ad, user, scheduleKey, oldJobDetail, data);
                }
            } catch (SchedulerException se)
            {
                Logger.error(se, "Error in Quartz");
            }
        }
    }

    private static void schedule(final Ad ad, final User user, final String scheduleKey, final JobDetail oldJobDetail, JobDataMap data) throws SchedulerException
    {
        data.put("ad-" + ad.uuid, Templates.renderHtmlAd(ad, null, user));
        JobDetail jobDetail = JobBuilder.newJob(QuartzAdNotificationJob.class)
                .withIdentity(scheduleKey)
                .usingJobData(data)
                .storeDurably(true)
                .build();

        // update existing
        if (oldJobDetail != null)
        {
            scheduler.addJob(jobDetail, true);
            Logger.info("Adding to existing notification for " + user.login);
        } else
        {
            //String cron = "*/30 * * * * ?";
            String cron = "0 0 " + user.notificationHour + " ? * " + user.notificationDay + " *";
            CronTrigger newTrigger = newTrigger()
                    .withIdentity(scheduleKey)
                    .withSchedule(cronSchedule(cron))
                    .forJob(jobDetail.getKey())
                    .build();

            scheduler.scheduleJob(jobDetail, newTrigger);
            Logger.info("Creating new notification for " + user.login + " time: " + newTrigger.getNextFireTime());
        }
    }

    private static void unschedule(final Ad ad, final User user, final String scheduleKey, JobDataMap data) throws SchedulerException
    {
        data.remove("ad-" + ad.uuid);
        if (data.size() == 0)
        {
            scheduler.deleteJob(new JobKey(scheduleKey));
            Logger.info("Cancelling notification for " + user.login);
            printMap(data);
        } else
        {
            JobDetail jobDetail = JobBuilder.newJob(QuartzAdNotificationJob.class)
                    .withIdentity(scheduleKey)
                    .usingJobData(data)
                    .storeDurably(true)
                    .build();

            scheduler.addJob(jobDetail, true);
            Logger.info("Deleting from notification for " + user.login);
            printMap(data);
        }
    }

    private static void printMap(final JobDataMap map)
    {
        for (Entry<String, Object> entry : map.entrySet())
        {
            Logger.info(entry.getKey());
            //Logger.info(entry.getKey() + ":" + entry.getValue());
        }
    }
}
