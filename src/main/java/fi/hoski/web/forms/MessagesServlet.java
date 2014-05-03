/*
 * Copyright (C) 2013 Helsingfors Segelklubb ry
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

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import fi.hoski.datastore.CachingDatastoreService;
import fi.hoski.datastore.DSUtilsImpl;
import fi.hoski.datastore.repository.Keys;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Timo Vesalainen
 */
public class MessagesServlet extends HttpServlet {

  private CachingDatastoreService datastore;
  private DSUtilsImpl entities;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    datastore = new CachingDatastoreService(DatastoreServiceFactory.getDatastoreService(), Keys.getRootKey());
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
    String key = request.getParameter("key");
    if (key == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "key parameter missing");
    } else {
      response.setContentType("text/html;charset=UTF-8");
      PrintWriter out = response.getWriter();
      String message = entities.getMessage(key);
      out.print(message);
      out.close();
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
  }// </editor-fold>
}
