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
import fi.hoski.util.EntityReferences;
import fi.hoski.web.ServletLog;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Timo Vesalainen
 */
public class EntityListServlet extends HttpServlet {

  private static final long serialVersionUID = 1;
  public static final String ANCESTOR = "ancestor"; // ancestor key in string format
  public static final String KIND = "kind"; // entity kind
  public static final String SORTPROPERTY = "sortProperty"; // entity property used in sorting
  public static final String DESCENDING = "descending"; // sorts descending
  public static final String PROPERTIES = "properties"; // sorts ascending (default)
  private ResourceBundle repositoryBundle;
  private DatastoreService datastore;
  private DSUtils entities;
  private Events events;
  private Races races;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    repositoryBundle = ResourceBundle.getBundle("fi/hoski/datastore/repository/fields");
    datastore = new CachingDatastoreService(DatastoreServiceFactory.getDatastoreService(), Keys.getRootKey());
    entities = new DSUtilsImpl(datastore);
    events = new EventsImpl();
    races = new RacesImpl(new ServletLog(this), datastore, entities, null);
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
    response.setContentType("text/html;charset=UTF-8");
    PrintWriter out = response.getWriter();
    try {
      boolean authenticated = request.isUserInRole("member");
      ParameterContext c = new ParameterContext(this, request);
      log(c.toString());
      if (c.fromRequest(KIND) || c.fromRequest(PROPERTIES)) {
        log("rejecting params=" + c);
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      String title = "";
      String ancestor = c.getParameter(ANCESTOR);
      if (ancestor != null) {
        Key parent = KeyFactory.stringToKey(ancestor);
        KeyInfo keyInfo = new KeyInfo(entities, events, races, "", parent, authenticated);
        title = keyInfo.getLabel();
      }

      out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
      out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
      out.println("<title>" + title + "</title>");
      out.println("</head>");
      out.println("<body>");
      out.println("<h1>" + title + "</h1>");

      printTable(out, c);

      out.println("</body>");
      out.println("</html>");
    } catch (EntityNotFoundException ex) {
      log(ex.getMessage(), ex);
      throw new ServletException(ex);
    } finally {
      out.close();
    }
  }

  private void printTable(PrintWriter out, ParameterContext c) {
    String kind = c.getParameter(KIND);
    Query query = new Query(kind);
    String ancestor = c.getParameter(ANCESTOR);
    if (ancestor != null) {
      query.setAncestor(KeyFactory.stringToKey(ancestor));
    }
    String sortProperty = c.getParameter(SORTPROPERTY);
    String[] properties = c.getArrayParameter(PROPERTIES);
    if (properties == null) {
      throw new IllegalArgumentException("missing " + PROPERTIES);
    }
    boolean descending = c.getBooleanParameter(DESCENDING);
    if (sortProperty != null) {
      if (descending) {
        query.addSort(sortProperty, Query.SortDirection.DESCENDING);
      } else {
        query.addSort(sortProperty);
      }
    }
    log(query.toString());
    PreparedQuery preparedQuery = datastore.prepare(query);

    out.println("<table><thead><tr>");

    for (String property : properties) {
      out.print("<th>");
      out.print(repositoryBundle.getString(property));
      out.print("</th>");
    }

    out.println("</tr></thead><tbody>");

    for (Entity entity : preparedQuery.asIterable()) {
      out.println("<tr>");
      for (String property : properties) {
        out.print("<td>");
        Object ob = entity.getProperty(property);
        if (ob != null) {
          out.print(EntityReferences.encode(ob.toString()));
        }
        out.print("</td>");
      }
      out.println("</tr>");
    }

    out.println("</tbody></table>");
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
  }
}
