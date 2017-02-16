package controllers;

import java.util.ArrayList;
import java.util.List;

import models.User;

import org.quartz.SchedulerException;

import quartz.QuartzAdServise;
import dto.UserDto;

public class Administration extends BaseController
{
    public static void users()
    {
        User user = getCachedUser();
        if (!user.isAdmin())
            forbidden();

        List<User> users = User.findAll();
        render(user, users);
    }

    public static void chat()
    {
        User user = getCachedUser();
        if (!user.isAdmin())
            forbidden();
        render(user);
    }

    public static void online(String url)
    {
        User user = getNotCachedUser();
        if (!user.isAdmin())
            forbidden();
        user.online = true;
        user.save();
        clearCachedUser();
        redirectTo(url);
    }

    public static void offline(String url)
    {
        User user = getNotCachedUser();
        if (!user.isAdmin())
            forbidden();
        user.online = null;
        user.save();
        clearCachedUser();
        redirectTo(url);
    }

    public static void userList()
    {
        User user = getCachedUser();
        if (!user.isAdmin())
            forbidden();

        List<User> usersList = User.findAllUsers();
        List<UserDto> users = new ArrayList<UserDto>();
        for (User usr : usersList)
        {
            users.add(UserDto.convert(usr));
        }
        renderJSON(users);
    }

    public static void cronJobs()
    {
        User user = getCachedUser();
        if (!user.isAdmin())
            forbidden();

        List<String> jobs = null;
        try
        {
            jobs = QuartzAdServise.listJobs();
        } catch (SchedulerException e)
        {
            e.printStackTrace();
        }
        render(user, jobs);
    }

    public static void cronJobsDelete(String url)
    {
        User user = getCachedUser();
        if (!user.isAdmin())
            forbidden();

        try
        {
            QuartzAdServise.clearJobs();
        } catch (SchedulerException e)
        {
            e.printStackTrace();
        }
        redirectTo(url);
    }
}