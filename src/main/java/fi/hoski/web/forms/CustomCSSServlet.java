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

import com.google.appengine.api.datastore.*;
import fi.hoski.datastore.*;
import fi.hoski.datastore.repository.KeyInfo;
import fi.hoski.datastore.repository.Keys;
import fi.hoski.web.ServletLog;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Timo Vesalainen
 */
public class CustomCSSServlet extends HttpServlet {

  private DatastoreService datastore;
  private DSUtils entities;
  private Events events;
  private Races races;

  @Override
  public void init() throws ServletException {
    super.init();
    datastore = DatastoreServiceFactory.getDatastoreService();
    entities = new DSUtilsImpl(datastore);
    events = new EventsImpl();
    races = new RacesImpl(new ServletLog(this), datastore, entities, null);
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
      response.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate");  // input comes from referrer
      Key parent = getAncestor(request);
      if (parent != null) {
        KeyInfo keyInfo = new KeyInfo(entities, events, races, "", parent, false);
        Map<String, Object> m = keyInfo.getMap();
        String css = (String) m.get("RaceSeries.SponsorStyle");
        if (css != null) {
          response.sendRedirect(css);
          return;
        }
      }
      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } catch (EntityNotFoundException ex) {
      log(ex.getMessage(), ex);
      throw new ServletException(ex);
    }
  }

  private Key getAncestor(HttpServletRequest request) throws EntityNotFoundException {
    String attachment = request.getParameter("attachment");
    if (attachment != null) {
      return Keys.getAttachmentRootKey(attachment);
    }
    String ancestor = request.getParameter("ancestor");
    if (ancestor == null) {
      String referer = request.getHeader("referer");
      if (referer != null) {
        int i1 = referer.indexOf("ancestor=");
        if (i1 != -1) {
          int i2 = referer.indexOf("&", i1);
          if (i2 != -1) {
            ancestor = referer.substring(i1 + 9, i2);
          } else {
            ancestor = referer.substring(i1 + 9);
          }
        }
      }
    }
    if (ancestor != null) {
      return KeyFactory.stringToKey(ancestor);
    }
    return null;
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
