/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.hoski.web.forms;

import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import fi.hoski.datastore.repository.Attachment;
import fi.hoski.datastore.repository.Attachment.Type;
import fi.hoski.datastore.repository.RaceSeries;
import fi.hoski.datastore.repository.RaceFleet;
import fi.hoski.util.FormPoster;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    @Override
    public void init() throws ServletException
    {
        super.init();
        datastore = DatastoreServiceFactory.getDatastoreService();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String keyStr = req.getParameter("key");
        if (keyStr != null)
        {
            try {
                log(keyStr);
                Key key = KeyFactory.stringToKey(keyStr);
                PrintWriter out = resp.getWriter();
                out.println("<h1>"+description(key)+"</h1>");
                Query query = new Query(Attachment.KIND, key);
                PreparedQuery pq = datastore.prepare(query);
                Iterator<Entity> it = pq.asIterator();
                if (it.hasNext())
                {
                    out.println("<form id=\"removeAttachmentForm\" action=\"/admin\">");
                    while (it.hasNext())
                    {
                        Entity e = it.next();
                        String aKey = KeyFactory.keyToString(e.getKey());
                        String title = (String) e.getProperty("Title");
                        out.println("<input type=\"checkbox\" name=\"delAttachment\" value=\""+aKey+"\">"+title+"<br>");
                    }
                    out.println("<input type=\"submit\" value=\"remove\">");
                    out.println("</form>");
                }
                BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
                out.println("<form id=\"addAttachmentForm\" action=\""+blobstoreService.createUploadUrl("/upload")+"\"  method=\"post\" enctype=\"multipart/form-data\">");
                out.println("<select name=\"type\">");
                for (Type type : Type.values())
                {
                    out.println("<option value=\""+type.name()+"\">"+repositoryBundle.getString(type.name())+"</option>");
                }
                out.println("</select>");
                out.println("<input type=\"text\" style=\"display: none\" name=\"attachTo\" value=\"" + keyStr + "\">");
                out.println("<input type=\"text\" style=\"display: none\" name=\"redir\" value=\"/races\">");
                out.println("<label for=\"attachmentTitle\">title</label>");
                out.println("<input id=\"attachmentTitle\" type=\"text\" name=\"title\">");
                out.println("<label for=\"attachmentFile\">file</label>");
                out.println("<input id=\"attachmentFile\" type=\"file\" name=\"file\">");
                out.println("<input type=\"submit\" value=\"add\">");
                out.println("</form>");
                out.flush();
                return;
            }
            catch (EntityNotFoundException ex) {
                throw new ServletException(ex);
            }
        }
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
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

}
