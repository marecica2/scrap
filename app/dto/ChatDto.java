package dto;

import models.Chat;
import utils.DateTimeUtils;

public class ChatDto
{
    public String uuid;
    public String userUuid;
    public String adminUuid;
    public String name;
    public String text;
    public Boolean inverted;
    public String adminName;
    public boolean adminOnline;
    public String created;

    public static ChatDto convert(Chat chat, Boolean adminOnline)
    {
        ChatDto a = new ChatDto();
        if (chat.user != null)
        {
            a.userUuid = chat.user.uuid;
            a.name = chat.user.getFullName();
        }
        if (chat.admin != null)
        {
            a.adminName = chat.admin.getFullName();
            a.adminUuid = chat.admin.uuid;
        }
        a.text = chat.text;
        a.inverted = chat.inverted;
        a.uuid = chat.uuid;
        a.adminOnline = adminOnline;
        a.created = DateTimeUtils.formatDate(chat.created);
        return a;
    }

}
