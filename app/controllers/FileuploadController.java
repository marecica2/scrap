package controllers;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import models.Ad;
import models.FileUpload;
import models.User;

import org.apache.commons.io.FileUtils;

import play.Play;
import play.libs.Images;
import utils.NumberUtils;
import utils.RandomUtil;

public class FileuploadController extends BaseController
{
    public static final String PATH_TO_UPLOADS = Play.applicationPath + "/public/uploads/";
    public static final Integer FILESIZE = 2500000;

    public static void removeFile(String uuid, String url) throws IOException
    {
        FileUpload fu = FileUpload.getByUuid(uuid);
        if (fu != null)
        {
            String filename = fu.url;
            File f = new File(PATH_TO_UPLOADS + filename);
            f.delete();
            File ft = new File(PATH_TO_UPLOADS + filename + "_thumb");
            ft.delete();
            fu.delete();
        }
        redirect(url);
    }

    public static void uploadFile(String item, String temp, File attachment, String contentType, String size) throws IOException
    {
        int fileSize = NumberUtils.parseInt(size);
        if (fileSize < FILESIZE && contentType.contains("image"))
        {
            FileUpload fu = createFileAttachment(item, temp, attachment, contentType, size);
            renderText("{\"url\":\"" + fu.url + "\", \"uuid\":\"" + fu.uuid + "\"}");
        } else
        {
            response.status = 400;
            renderText("{\"exception\":\"Invalid file size or content type\"}");
        }
    }

    public static void uploadMobile(File file, String size) throws IOException
    {
        System.err.println("aaa");
        Integer fileSize = NumberUtils.parseInt(size);
        if (fileSize == null)
            fileSize = 1000;
        if (fileSize < FILESIZE)
        {
            System.err.println("photo upload");
            System.err.println(file.getName() + " " + size);

            final String id = request.params.get("id");
            final String login = request.params.get("login");
            final String password = request.params.get("password");
            final User user = User.getUserByLogin(login);

            if (user != null && user.password.equals(password))
            {
                createFileAttachment(id, null, file, "image/jpeg", size);
            }

            renderJSON("{\"success\":\"false\"}");
        } else
        {
            response.status = 400;
            renderText("{\"exception\":\"Invalid file size or content type\"}");
        }
    }

    private static FileUpload createFileAttachment(String item, String temp, File attachment, String contentType, String size) throws IOException
    {
        final String uuid = RandomUtil.getUUID();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        String folder = cal.get(Calendar.YEAR) + "" +
                ((cal.get(Calendar.MONTH) + 1) < 10 ? "0" : "") + (cal.get(Calendar.MONTH) + 1) + "" +
                (cal.get(Calendar.DAY_OF_MONTH) < 10 ? "0" : "") + cal.get(Calendar.DAY_OF_MONTH) + "";

        File copyTo = new File(PATH_TO_UPLOADS + folder);
        if (!copyTo.exists())
            copyTo.mkdir();

        String filename = folder + "/" + uuid;
        final File destination = new File(PATH_TO_UPLOADS + filename);
        FileUtils.copyFile(attachment, destination);

        File destinationThumb = new File(PATH_TO_UPLOADS + filename + "_thumb");
        Images.resize(destination, destinationThumb, 300, 300, true);

        FileUpload fu = new FileUpload();
        fu.url = filename;
        fu.name = attachment.getName();
        fu.size = attachment.length();
        fu.contentType = contentType;
        fu.uuid = uuid;

        // if editing new ad set temporary id of the fileupload
        if (temp != null)
        {
            fu.temp = temp;
        }

        // if editing existing ad, set the ad of the fileupload
        if (item != null)
        {
            Ad ad = Ad.getAdByUUID(item);
            fu.ad = ad.id;
        }

        fu.save();
        return fu;
    }

    public static void uploadPhoto(String item, String temp, File attachment, String contentType, String size) throws IOException
    {
        int fileSize = NumberUtils.parseInt(size);
        if (fileSize < FILESIZE && contentType.contains("image"))
        {
            final User user = getNotCachedUser();

            String filename = "/" + user.uuid;
            final File destination = new File(PATH_TO_UPLOADS + filename);
            FileUtils.copyFile(attachment, destination);

            final String path = PATH_TO_UPLOADS + filename + "_thumb";
            File destinationThumb = new File(path);
            Images.resize(destination, destinationThumb, 500, 500, true);

            user.avatarUrl = filename;
            user.avatarUrlSmall = filename + "_thumb";
            user.save();

            clearCachedUser();
            renderText("{\"url\":\"" + filename + "\"}");
        } else
        {
            response.status = 400;
            renderText("{\"exception\":\"Invalid file size or content type\"}");
        }

    }

    public static void deleteFile(String uuid)
    {
        FileUpload fu = FileUpload.find("byUuid", uuid).first();
        if (fu != null)
        {
            File f = new File(PATH_TO_UPLOADS + fu.url);
            if (f.exists())
                f.delete();
            fu.delete();
        }
    }

    public static void deleteTmpFiles()
    {
        File[] listOfFiles = Play.tmpDir.listFiles();
        for (int i = 0; i < listOfFiles.length; i++)
        {
            final File file = listOfFiles[i];
            if (file.isFile())
            {
                file.delete();
            }
        }
    }

}
