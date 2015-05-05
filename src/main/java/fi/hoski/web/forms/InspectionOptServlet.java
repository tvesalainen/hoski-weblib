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
import fi.hoski.datastore.repository.Event;
import fi.hoski.datastore.repository.KeyInfo;
import fi.hoski.util.Day;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Timo Vesalainen
 */
public class InspectionOptServlet extends HttpServlet {

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
      response.setContentType("text/html;charset=UTF-8");
      PrintWriter out = response.getWriter();
      boolean authenticated = request.isUserInRole("member");

      String referer = request.getHeader("referer");
      log("referer=" + referer);
      if (referer != null) {
        int i1 = referer.indexOf("ancestor=");
        if (i1 != -1) {
          String ancestor = null;
          int i2 = referer.indexOf("&", i1);
          if (i2 != -1) {
            ancestor = referer.substring(i1 + 9, i2);
          } else {
            ancestor = referer.substring(i1 + 9);
          }
          Key parent = KeyFactory.stringToKey(ancestor);
          if (!"Event".equals(parent.getKind())) {
            throw new ServletException(parent + " not event key");
          }
          Entity eventEntity = datastore.get(parent);
          Event event = (Event) entities.newInstance(eventEntity);
          Day day = (Day) event.get(Event.EventDate);
          List<Event> inspectionEvents = events.getEvents(Event.EventType.INSPECTION, day, 3);
          boolean selected = true;
          for (Event inspectionEvent : inspectionEvents) {
            Key inspectionKey = inspectionEvent.createKey();
            String keyString = KeyFactory.keyToString(inspectionKey);
            KeyInfo keyInfo = new KeyInfo(entities, events, null, "", inspectionKey, authenticated);
            if (selected) {
              out.println("<option selected='true' value='" + keyString + "'>" + keyInfo.getLabel() + "</option>");
            } else {
              out.println("<option value='" + keyString + "'>" + keyInfo.getLabel() + "</option>");
            }
            selected = false;
          }
          out.println("<option value=''>" + "Varaan myöhemmin" + "</option>");
        }
      }
    } catch (EntityNotFoundException ex) {
      log(ex.getMessage(), ex);
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
