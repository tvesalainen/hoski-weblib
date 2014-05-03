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
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Timo Vesalainen
 */
public class KeyInfoServlet extends HttpServlet {

  private DatastoreService datastore;
  private DSUtils entities;
  private Events events;
  private Races races;

  @Override
  public void init() throws ServletException {
    super.init();
    datastore = new CachingDatastoreService(DatastoreServiceFactory.getDatastoreService(), Keys.getRootKey());
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
      response.setHeader("Cache-Control", "no-cache");  // input comes from referrer
      response.setContentType("application/json");
      boolean authenticated = request.isUserInRole("member");

      JSONObject json = new JSONObject();
      Key parent = getAncestor(request);
      if (parent != null) {
        KeyInfo keyInfo = new KeyInfo(entities, events, races, "", parent, authenticated);
        Map<String, Object> m = keyInfo.getMap();
        String clubDiscount = (String) m.get("RaceSeries.ClubDiscount");
        String club = (String) m.get("Club");
        if (Boolean.parseBoolean(clubDiscount) && "HSK".equalsIgnoreCase(club)) {
          m.put("isClubDiscountGranted", true);
        } else {
          m.put("isClubDiscountGranted", false);
        }
        for (Map.Entry<String, Object> e : m.entrySet()) {
          if (e.getValue() instanceof List) {
            JSONArray a = new JSONArray();
            json.put(e.getKey(), a);
            List<String> l = (List<String>) e.getValue();
            for (String s : l) {
              a.put(s);
            }
          } else {
            if (e.getValue() instanceof char[]) {
              JSONArray a = new JSONArray();
              json.put(e.getKey(), a);
              char[] ar = (char[]) e.getValue();
              for (char c : ar) {
                a.put((int) c);
              }
            } else {
              json.put(e.getKey(), e.getValue());
            }
          }
        }
        json.write(response.getWriter());
      }
    } catch (EntityNotFoundException ex) {
      log(ex.getMessage(), ex);
      throw new ServletException(ex);
    } catch (JSONException ex) {
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
