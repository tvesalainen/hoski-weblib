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
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
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
import fi.hoski.mail.MailService;
import fi.hoski.mail.MailServiceImpl;
import fi.hoski.sailwave.Competitor;
import fi.hoski.sailwave.SailWaveFile;
import fi.hoski.util.LogWrapper;
import fi.hoski.web.ServletLog;
import java.io.IOException;
import java.io.PrintWriter;
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
            resp.setCharacterEncoding("UTF-8");
            removeAttachments(req);
            String keyStr = req.getParameter("key");
            if (keyStr != null)
            {
                printDialog(resp, keyStr);
                return;
            }
            String downloadType = req.getParameter("download");
            if (downloadType != null)
            {
                download(resp, downloadType);
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

    private void removeAttachments(HttpServletRequest req)
    {
        String[] vals = req.getParameterValues("delAttachment");
        if (vals != null)
        {
            List<Key> keys = new ArrayList<>();
            for (String val : vals)
            {
                keys.add(KeyFactory.stringToKey(val));
            }
            datastore.delete(keys);
        }
    }

    private void printDialog(HttpServletResponse resp, String keyStr) throws ServletException, IOException
    {
        try {
            log(keyStr);
            Key key = KeyFactory.stringToKey(keyStr);
            PrintWriter out = resp.getWriter();
            out.println("<h1>"+description(key)+"</h1>");
            // downloads
            out.println("<form id=\"downloadSailWaveForm\" method=\"post\" action=\"/admin\">");
            out.println("<fieldset>");
            out.println("<legend>"+text("downloadSailWave")+"</legend>");
            out.println("<input type=\"text\" style=\"display: none\" name=\"download\" value=\"" + keyStr + "\">");
            out.println("<input type=\"submit\" value=\""+text("download")+"\">");
            out.println("</fieldset>");
            out.println("</form>");
            // remove attachments
            Query query = new Query(Attachment.KIND, key);
            PreparedQuery pq = datastore.prepare(query);
            Iterator<Entity> it = pq.asIterator();
            if (it.hasNext())
            {
                out.println("<form id=\"removeAttachmentForm\" method=\"post\" action=\"/admin\">");
                out.println("<fieldset>");
                out.println("<legend>"+text("Attachments")+"</legend>");
                while (it.hasNext())
                {
                    Entity e = it.next();
                    String aKey = KeyFactory.keyToString(e.getKey());
                    String title = (String) e.getProperty("Title");
                    out.println("<input type=\"checkbox\" name=\"delAttachment\" value=\""+aKey+"\">"+title+"<br>");
                }
                out.println("<input type=\"submit\" value=\""+text("remove")+"\">");
                out.println("</fieldset>");
                out.println("</form>");
            }
            // add attachment
            BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
            out.println("<form id=\"addAttachmentForm\" action=\""+blobstoreService.createUploadUrl("/upload")+"\"  method=\"post\" enctype=\"multipart/form-data\">");
            out.println("<fieldset>");
            out.println("<legend>"+text("addAttachment")+"</legend>");
            out.println("<select name=\"type\">");
            for (Type type : Type.values())
            {
                out.println("<option value=\""+type.name()+"\">"+text(type.name())+"</option>");
            }
            out.println("</select>");
            out.println("<input type=\"text\" style=\"display: none\" name=\"attachTo\" value=\"" + keyStr + "\">");
            out.println("<input type=\"text\" style=\"display: none\" name=\"redir\" value=\"/races/\">");
            out.println("<label for=\"attachmentTitle\">"+text("Title")+"</label>");
            out.println("<input id=\"attachmentTitle\" type=\"text\" name=\"title\">");
            out.println("<input id=\"chooseFile\" type=\"button\" value=\""+text("chooseFile")+"\">");
            out.println("<input id=\"attachmentFile\" required style=\"visibility: hidden; position: absolute;\" type=\"file\" name=\"file\">");
            out.println("<input id=\"attachmentFilename\" type=\"text\" readonly >");
            out.println("<input type=\"submit\" value=\""+text("add")+"\">");
            out.println("</fieldset>");
            out.println("</form>");
            out.flush();
        }
        catch (EntityNotFoundException ex) {
            throw new ServletException(ex);
        }
    }

    private void download(HttpServletResponse resp, String keyStr) throws EntityNotFoundException, IOException
    {
        Key key = KeyFactory.stringToKey(keyStr);
        DataObject dataObject = entities.newInstance(key);
        List<RaceEntry> entryList = races.getRaceEntriesFor(dataObject);
        RaceSeries raceSeries = (RaceSeries) entities.getParent(key, RaceSeries.KIND);
        if (raceSeries == null)
        {
            log("no race for "+keyStr);
        }
        Blob swb = (Blob) raceSeries.get(RaceSeries.SAILWAVEFILE);
        String event = (String) raceSeries.get(RaceSeries.EVENT);
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
        log(event);
        swf.deleteNotNeededFleets(entryList);
        resp.setContentType("application/x-sailwave; name="+event+".blw");
        resp.setHeader("Content-Disposition", "attachment; filename=\""+event+".blw\"");
        swf.write(resp.getOutputStream());
    }

}
