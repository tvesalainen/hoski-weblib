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
import fi.hoski.datastore.repository.DataObject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Timo Vesalainen
 */
public class UploadServlet extends HttpServlet {

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
    BlobKey blobKey = new BlobKey(request.getParameter("blob-key"));
    blobstoreService.serve(blobKey, response);
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
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    log(blobs.toString());
    String title = request.getParameter("title");
    String typeStr = request.getParameter("type");
    if (typeStr == null) {
      throw new ServletException("type not found");
    }
    String attachStr = request.getParameter("attachTo");
    if (attachStr == null) {
      throw new ServletException("attachTo not found");
    }
    Attachment.Type type = Attachment.Type.valueOf(typeStr.toUpperCase());
    Key key = KeyFactory.stringToKey(attachStr);
    try {
      DataObject parent = entities.newInstance(key);
      for (Entry<String, List<BlobKey>> entry : blobs.entrySet()) {
        String filename = entry.getKey();
        for (BlobKey bk : entry.getValue()) {
          Attachment attachment = new Attachment(parent);
          attachment.set(Attachment.TYPE, type.ordinal());
          URI reqUri = new URI(request.getScheme(), request.getServerName(), "", "");
          URI uri = reqUri.resolve("/download?blob-key="+bk.getKeyString());
          attachment.set(Attachment.URL, uri.toASCIIString());
          if (title != null) {
            attachment.set(Attachment.TITLE, title);
          } else {
            attachment.set(Attachment.TITLE, filename);
          }
          attachment.set(Attachment.Filename, filename);
          entities.put(attachment);
        }
      }
    } catch (URISyntaxException ex) {
      throw new ServletException(ex);
    } catch (EntityNotFoundException ex) {
      throw new ServletException(ex);
    }
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
