package dto;

import models.User;

public class UserDto
{
    public String uuid;
    public String name;

    public static UserDto convert(User user)
    {
        UserDto a = new UserDto();
        a.name = user.getFullName();
        a.uuid = user.uuid;
        return a;
    }

    @Override
    public String toString()
    {
        return "UserDto [name=" + name + "]";
    }

}
