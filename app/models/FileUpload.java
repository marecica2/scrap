package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class FileUpload extends Model
{
    public String name;

    public Long size;

    public Long ad;

    public String contentType;

    public String url;

    public Date created;

    public String uuid;

    public String temp;

    public static FileUpload getByUuid(String uuid)
    {
        if (uuid != null)
        {
            return FileUpload.find("byUuid", uuid).first();
        }
        return null;
    }

    public static List<FileUpload> getByTemp(String uuid)
    {
        if (uuid != null)
        {
            return FileUpload.find("byTemp", uuid).fetch();
        }
        return null;
    }
}
