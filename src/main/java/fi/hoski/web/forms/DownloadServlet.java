/*
 * Copyright (C) 2012 Helsingfors Segelklubb ry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package fi.hoski.web.forms;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.*;
import fi.hoski.datastore.DSUtils;
import fi.hoski.datastore.DSUtilsImpl;
import fi.hoski.datastore.repository.Attachment;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Timo Vesalainen
 */
public class DownloadServlet extends HttpServlet {

  private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
  private DatastoreService datastore;
  private DSUtils entities;

  @Override
  public void init() throws ServletException {
    super.init();
    datastore = DatastoreServiceFactory.getDatastoreService();
    entities = new DSUtilsImpl(datastore);
  }

  /**
   * Handles the HTTP
   * <code>GET</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    try {
        String blobKeyStr = null;
        String attachmentKeyStr = request.getParameter("attachment-key");
        if (attachmentKeyStr != null){
            Key attachmentKey = KeyFactory.stringToKey(attachmentKeyStr);
            Entity attachment = datastore.get(attachmentKey);
            Link link = (Link) attachment.getProperty(Attachment.URL);
            String url = link.getValue();
            int idx = url.indexOf('=');
            if (idx == -1){
                throw new ServletException("idx == -1");
            }
            log(url);
            blobKeyStr = url.substring(idx+1);
            log(blobKeyStr);
            String filename = (String) attachment.getProperty(Attachment.Filename);
            log(filename);
            response.setHeader("Content-Disposition", "attachment; filename=\""+filename+"\"");
        } else {
          blobKeyStr = request.getParameter("blob-key");
        }
        if (blobKeyStr != null){
          BlobKey blobKey = new BlobKey(blobKeyStr);
          blobstoreService.serve(blobKey, response);
        } else {

        }
    }
    catch (EntityNotFoundException ex){
        throw new ServletException(ex);
    }
  }

  /**
   * Handles the HTTP
   * <code>POST</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  /**
   * Returns a short description of the servlet.
   *
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "Short description";
  }
}
