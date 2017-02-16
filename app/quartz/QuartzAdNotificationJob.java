package quartz;

import java.util.Map.Entry;
import java.util.Set;

import models.User;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import play.Logger;
import play.i18n.Messages;
import email.EmailProvider;
import email.Templates;

public class QuartzAdNotificationJob extends play.jobs.Job implements Job
{
    private String key = null;
    private String userLogin = null;
    private JobDataMap jobDataMap = null;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        key = context.getJobDetail().getKey().getName();
        userLogin = key;
        jobDataMap = context.getJobDetail().getJobDataMap();

        // unschedule
        try
        {
            context.getScheduler().deleteJob(context.getJobDetail().getKey());
            //Logger.info("unscheduled successfully for" + key);
        } catch (SchedulerException e)
        {
            Logger.error(e, "Error occured during unscheduling job");
        }

        // will trigger doJob
        now();
    }

    @Override
    public void doJob() throws Exception
    {
        // get user
        final Object obj = User.getUserByLogin2(userLogin).get(0);
        final User user = (User) obj;

        // build email
        final Set<Entry<String, Object>> entries = jobDataMap.entrySet();
        final StringBuilder bodySb = new StringBuilder();
        bodySb.append(Templates.adEmailTemplateHeader(user));
        for (Entry<String, Object> entry : entries)
            bodySb.append(entry.getValue());

        // send email
        new EmailProvider().sendEmail(Messages.get("ad.email.header"), user.login, bodySb.toString());
        super.doJob();
    }
}
