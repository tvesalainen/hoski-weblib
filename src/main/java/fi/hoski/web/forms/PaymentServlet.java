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
import fi.hoski.datastore.DSUtils;
import fi.hoski.datastore.DSUtilsImpl;
import fi.hoski.datastore.Races;
import fi.hoski.datastore.RacesImpl;
import fi.hoski.mail.MailService;
import fi.hoski.mail.MailServiceImpl;
import fi.hoski.util.BankingBarcode;
import fi.hoski.util.code128.CharNotInCodesetException;
import fi.hoski.util.code128.Code128;
import fi.hoski.web.ServletLog;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Experimental
 *
 * @author Timo Vesalainen
 */
public class PaymentServlet extends HttpServlet {

  private DatastoreService datastore;
  private DSUtils entities;
  private Races races;
  private MailService mailService;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    datastore = DatastoreServiceFactory.getDatastoreService();
    entities = new DSUtilsImpl(datastore);
    mailService = new MailServiceImpl();
    races = new RacesImpl(new ServletLog(this), datastore, entities, mailService);
  }

  /**
   * Processes requests for both HTTP
   * <code>GET</code> and
   * <code>POST</code> methods.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    response.setHeader("Cache-Control", "no-cache");  // input comes from referrer
    response.setContentType("application/json");
    JSONObject json = new JSONObject();
    JSONArray jsonArray = new JSONArray();
    try {
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
          Key raceEntryKey = KeyFactory.stringToKey(ancestor);
          BankingBarcode barcode = races.getBarcode(raceEntryKey);
          if (barcode != null) {
            for (Map.Entry<String, Object> e : barcode.getMetaData().entrySet()) {
              json.put(e.getKey(), e.getValue());
            }
            char[] bars = Code128.encode(barcode.toString());
            int width = 0;
            for (int cc : bars) {
              width += cc;
              jsonArray.put(cc);
            }
            json.put("barwidth", width);
            json.put("bars", jsonArray);
          }
        }
      }
      json.write(response.getWriter());
    } catch (EntityNotFoundException ex) {
      throw new ServletException(ex);
    } catch (JSONException ex) {
      throw new ServletException(ex);
    } catch (CharNotInCodesetException ex) {
      throw new ServletException(ex);
    } finally {
    }
  }

  // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
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
    processRequest(request, response);
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
    processRequest(request, response);
  }

  /**
   * Returns a short description of the servlet.
   *
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "Short description";
  }// </editor-fold>
}
