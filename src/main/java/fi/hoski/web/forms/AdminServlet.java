/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.hoski.web.forms;

import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Link;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import fi.hoski.datastore.DSUtils;
import fi.hoski.datastore.DSUtilsImpl;
import fi.hoski.datastore.Races;
import fi.hoski.datastore.RacesImpl;
import fi.hoski.datastore.repository.Attachment;
import fi.hoski.datastore.repository.Attachment.Type;
import fi.hoski.datastore.repository.DataObject;
import fi.hoski.datastore.repository.RaceEntry;
import fi.hoski.datastore.repository.RaceSeries;
import fi.hoski.datastore.repository.RaceFleet;
import fi.hoski.datastore.repository.Year;
import fi.hoski.mail.MailService;
import fi.hoski.mail.MailServiceImpl;
import fi.hoski.sailwave.Competitor;
import fi.hoski.sailwave.SailWaveFile;
import fi.hoski.util.Day;
import fi.hoski.util.LogWrapper;
import fi.hoski.web.ServletLog;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author tkv
 */
public class AdminServlet extends HttpServlet
{
    public static final ResourceBundle repositoryBundle = ResourceBundle.getBundle("fi/hoski/datastore/repository/fields");
    private DatastoreService datastore;
    private DSUtils entities;
    private Races races;
    private MailService mailService;

    @Override
    public void init() throws ServletException
    {
        super.init();
        datastore = DatastoreServiceFactory.getDatastoreService();
        entities = new DSUtilsImpl(datastore);
        mailService = new MailServiceImpl();
        LogWrapper log = new ServletLog(this);
        races = new RacesImpl(log, datastore, entities, mailService);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        try
        {
            String referer = req.getHeader("referer");
            boolean isRaceAdmin = isRaceAdmin(req, races);
            resp.setCharacterEncoding("UTF-8");
            String action = req.getParameter("action");
            if (action != null)
            {
                if ("year".equals(action))
                {
                    printYear(resp.getWriter(), isRaceAdmin);
                    return;
                }
                if (!isRaceAdmin)
                {
                    if ("login".equals(action))
                    {
                        printLoginDialog(resp, referer);
                        return;
                    }
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                if ("auth".equals(action))
                {
                    return;
                }
                switch (action)
                {
                    case "login":
                        break;
                    case "logout":
                        printLogoutDialog(resp, referer);
                        break;
                    case "download-sailwave-dialog":
                        printSailWaveDialog(resp.getWriter(), getKey(req));
                        return;
                    case "remove-attachment-dialog":
                        printRemoveAttachmentsDialog(resp.getWriter(), getKey(req), referer);
                        return;
                    case "add-attachment-dialog":
                        printAddAttachmentDialog(resp.getWriter(), getKey(req), referer);
                        return;
                    case "download-sailwave":
                        String downloadType = req.getParameter("download");
                        download(resp, downloadType);
                        break;
                    case "remove-attachments":
                        removeAttachments(req);
                        break;
                    default:
                        throw new ServletException(action+" unknown");
                }
                String redir = req.getParameter("redir");
                if (redir != null)
                {
                    resp.sendRedirect(redir);
                }
                else
                {
                    if (referer != null)
                    {
                        resp.sendRedirect(referer);
                    }
                }
                return;
            }
        }
        catch (EntityNotFoundException ex)
        {
            throw new ServletException(ex);
        }
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    private String text(String key)
    {
        try
        {
            return repositoryBundle.getString(key);
        }
        catch (MissingResourceException ex)
        {
            log(key+" missing");
            return key;
        }
    }
    private String description(Key key) throws EntityNotFoundException, ServletException
    {
        switch (key.getKind())
        {
            case Year.KIND:
                return String.valueOf(key.getId());
            case RaceSeries.KIND:
                Entity rs = datastore.get(key);
                return (String) rs.getProperty(RaceSeries.EVENT);
            case RaceFleet.Kind:
                String raceSer = description(key.getParent());
                Entity rf = datastore.get(key);
                return raceSer+" - "+(String) rf.getProperty(RaceFleet.Fleet);
            default:
                throw new ServletException(key+" unknown");
        }
    }

    private void removeAttachments(HttpServletRequest req) throws EntityNotFoundException, IOException
    {
        String[] vals = req.getParameterValues("delAttachment");
        if (vals != null)
        {
            List<Attachment> attachments = new ArrayList<>();
            for (String val : vals)
            {
                Key key = KeyFactory.stringToKey(val);
                attachments.add((Attachment) entities.newInstance(key));
            }
            entities.removeAttachments(attachments);
        }
    }

    private void printSailWaveDialog(PrintWriter out, Key key) throws ServletException
    {
        try
        {
            if (
                    RaceSeries.KIND.equals(key.getKind()) ||
                    RaceFleet.Kind.equals(key.getKind())
                    )
            {
                out.println("<h1>"+description(key)+"</h1>");
                // downloads
                out.println("<form id=\"downloadSailWaveForm\" method=\"post\" action=\"/admin?action=download-sailwave\">");
                out.println("<fieldset>");
                out.println("<legend>"+text("downloadSailWave")+"</legend>");
                out.println("<input type=\"text\" style=\"display: none\" name=\"download\" value=\"" + KeyFactory.keyToString(key) + "\">");
                out.println("<input type=\"submit\" value=\""+text("download")+"\">");
                out.println("</fieldset>");
                out.println("</form>");
            }
        }
        catch (EntityNotFoundException ex)
        {
            throw new ServletException(ex);
        }
    }
    private void printRemoveAttachmentsDialog(PrintWriter out, Key key, String redir) throws ServletException
    {
        try
        {
            out.println("<h1>"+description(key)+"</h1>");
            // remove attachments
            Query query = new Query(Attachment.KIND, key);
            PreparedQuery pq = datastore.prepare(query);
            Iterator<Entity> it = pq.asIterator();
            if (it.hasNext())
            {
                out.println("<form id=\"removeAttachmentForm\" method=\"post\" action=\"/admin?action=remove-attachments\">");
                out.println("<fieldset>");
                out.println("<legend>"+text("Attachments")+"</legend>");
                while (it.hasNext())
                {
                    Entity e = it.next();
                    String aKey = KeyFactory.keyToString(e.getKey());
                    String title = (String) e.getProperty(Attachment.TITLE);
                    String filename = (String) e.getProperty(Attachment.Filename);
                    out.println("<input type=\"checkbox\" name=\"delAttachment\" value=\""+aKey+"\">"+title+"-"+filename+"<br>");
                }
                out.println("<input type=\"text\" style=\"display: none\" name=\""+redir+"\" value=\"/races/\">");
                out.println("<input type=\"submit\" value=\""+text("remove")+"\">");
                out.println("</fieldset>");
                out.println("</form>");
            }
        }
        catch (EntityNotFoundException ex)
        {
            throw new ServletException(ex);
        }
    }
    private void printAddAttachmentDialog(PrintWriter out, Key key, String redir) throws ServletException
    {
        try
        {
            out.println("<h1>"+description(key)+"</h1>");
            // add attachment
            BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
            out.println("<form id=\"addAttachmentForm\" action=\""+blobstoreService.createUploadUrl("/upload")+"\"  method=\"post\" enctype=\"multipart/form-data\">");
            out.println("<fieldset>");
            out.println("<legend>"+text("addAttachment")+"</legend>");
            for (Type type : Type.values())
            {
                out.println("<input type=\"radio\" name=\"type\" value=\""+type.name()+"\" required>"+text(type.name())+"<br>");
            }
            out.println("<input type=\"text\" style=\"display: none\" name=\"attachTo\" value=\"" + KeyFactory.keyToString(key) + "\">");
            out.println("<input type=\"text\" style=\"display: none\" name=\"redir\" value=\""+redir+"\">");
            out.println("<label for=\"attachmentTitle\">"+text("Title")+"</label>");
            out.println("<input id=\"attachmentTitle\" type=\"text\" name=\"title\" required>");
            out.println("<input id=\"chooseFile\" type=\"button\" value=\""+text("chooseFile")+"\">");
            out.println("<input id=\"attachmentFile\" required style=\"visibility: hidden; position: absolute;\" type=\"file\">");
            out.println("<input id=\"attachmentFilename\" type=\"text\" readonly >");
            out.println("<input type=\"submit\" value=\""+text("add")+"\">");
            out.println("</fieldset>");
            out.println("</form>");
            out.flush();
        }
        catch (EntityNotFoundException ex)
        {
            throw new ServletException(ex);
        }
    }

    private void download(HttpServletResponse resp, String keyStr) throws EntityNotFoundException, IOException, ServletException
    {
        if (keyStr == null)
        {
            throw new ServletException("no key");
        }
        Key key = KeyFactory.stringToKey(keyStr);
        DataObject dataObject = entities.newInstance(key);
        List<RaceEntry> entryList = races.getRaceEntriesFor(dataObject);
        RaceSeries raceSeries = (RaceSeries) entities.getParent(key, RaceSeries.KIND);
        if (raceSeries == null)
        {
            log("no race for "+keyStr);
        }
        Blob swb = (Blob) raceSeries.get(RaceSeries.SAILWAVEFILE);
        SailWaveFile swf = new SailWaveFile(swb.getBytes());
        if (entryList != null)
        {
            for (RaceEntry entry : entryList)
            {
                Competitor competitor = new Competitor();
                competitor.setAll(entry.getAll());
                swf.addCompetitor(competitor);
            }
        }
        String filename = description(key).replace(' ', '_');
        swf.deleteNotNeededFleets(entryList);
        resp.setContentType("application/x-sailwave; name="+filename+".blw");
        resp.setHeader("Content-Disposition", "attachment; filename=\""+filename+".blw\"");
        swf.write(resp.getOutputStream());
    }

    private Key getKey(HttpServletRequest req) throws ServletException
    {
        String keyStr = req.getParameter("key");
        if (keyStr == null)
        {
            throw new ServletException("key missing");
        }
        return KeyFactory.stringToKey(keyStr);
    }

    private void printYear(PrintWriter out, boolean authenticated)
    {
        if (authenticated)
        {
            Key yearKey = entities.getYearKey();
            out.println("<span data-hoski-key=\""+KeyFactory.keyToString(yearKey)+"\">"+new Day().getYear()+"</span>");
        }
        else
        {
            out.println(new Day().getYear());
        }
    }

    public static final boolean isRaceAdmin(HttpServletRequest req, Races races)
    {
        Principal user = req.getUserPrincipal();
        System.err.println("logged="+user);
        if (user != null)
        {
            System.err.println("logged="+user.toString());
            return races.isRaceAdmin(user.getName());
        }
        return false;
    }

    private void printLoginDialog(HttpServletResponse resp, String redir) throws IOException
    {
        UserService userService = UserServiceFactory.getUserService();
        resp.sendRedirect(userService.createLoginURL(redir));
    }

    private void printLogoutDialog(HttpServletResponse resp, String redir) throws IOException
    {
        UserService userService = UserServiceFactory.getUserService();
        resp.sendRedirect(userService.createLogoutURL(redir));
    }

}
