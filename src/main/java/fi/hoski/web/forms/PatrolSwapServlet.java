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

import com.google.appengine.api.datastore.EntityNotFoundException;
import fi.hoski.datastore.PatrolShifts;
import fi.hoski.datastore.PatrolShiftsImpl;
import fi.hoski.datastore.Repository;
import fi.hoski.datastore.SMSNotConfiguredException;
import fi.hoski.datastore.repository.Options;
import fi.hoski.datastore.repository.SwapRequest;
import fi.hoski.sms.SMSException;
import fi.hoski.util.CalPrinter;
import fi.hoski.util.Day;
import fi.hoski.web.ServletLog;
import fi.hoski.web.auth.LoginServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ConcurrentModificationException;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javax.mail.internet.AddressException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Timo Vesalainen
 */
public class PatrolSwapServlet extends HttpServlet {

  private ResourceBundle repositoryBundle;
  private PatrolShifts patrolShifts;
  private int margin = 5;

  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      super.init(config);
      String marginString = config.getInitParameter("dayMargin");
      if (marginString != null) {
        margin = Integer.parseInt(marginString);
      }
      repositoryBundle = ResourceBundle.getBundle("fi/hoski/datastore/repository/fields");
      patrolShifts = new PatrolShiftsImpl(new ServletLog(this), margin);
    } catch (SMSNotConfiguredException | EntityNotFoundException ex) {
      throw new ServletException(ex);
    }
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
    response.setContentType("text/html;charset=UTF-8");
    PrintWriter out = response.getWriter();
    try {
      String queryString = request.getQueryString();
      log(""+queryString);
      if ("shifts".equals(queryString)) {
        Map<String, Object> user = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
          user = (Map<String, Object>) session.getAttribute(LoginServlet.USER);
        }
        if (user == null) {
          throw new ServletException("user not found");
        }
        Options<String> allShifts = patrolShifts.getShiftOptions((String) user.get(Repository.JASENET_KEY));
        if (allShifts == null) {
          log(user.toString());
          throw new ServletException("shifts not found");
        }
        out.println(allShifts.html());
      }
      if ("calendar".equals(queryString)) {
        body(out, request.getLocale());
      }
      if ("pending".equals(queryString)) {
        Map<String, Object> user = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
          user = (Map<String, Object>) session.getAttribute(LoginServlet.USER);
        }
        if (user == null) {
          throw new ServletException("user not found");
        }
        pending(out, user);
      }
    } catch (EntityNotFoundException ex) {
      log(ex.getMessage(), ex);
      throw new ServletException(ex);
    } finally {
      out.close();
    }
  }

  private void sendError(HttpServletResponse response,
    int statusCode,
    String htmlMessage) throws IOException {
    response.setStatus(statusCode);
    response.setContentType("text/html");
    response.setCharacterEncoding("utf-8");
    response.getWriter().write(htmlMessage);
  }

  private void body(PrintWriter out, Locale locale) throws EntityNotFoundException {
    Day[] limits = patrolShifts.firstAndLastShift();
    CalPrinter cp = new CalPrinter(locale);
    cp.print(out, margin, limits[0], limits[1]);
  }

  private void pending(PrintWriter out, Map<String, Object> user) throws EntityNotFoundException, IOException {
    for (SwapRequest pendingSwapRequest : patrolShifts.pendingSwapRequests(user)) {
      out.append("<tr>");
      out.append("<td>");
      Day day = (Day) pendingSwapRequest.get(Repository.PAIVA);
      out.append(day.toString());
      out.append("</td>");
      out.append("<td>");
      out.append("<form action='/member/swap?remove=" + pendingSwapRequest.createKeyString() + "' method='post'>");
      out.append("<input type='submit' name='submit' value='" + repositoryBundle.getString("RemovePatrolShiftSwap") + "'/>");
      out.append("</form>");
      out.append("</td>");
      out.append("</tr>");
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
    Map<String, Object> user = null;
    HttpSession session = request.getSession(false);
    if (session != null) {
      user = (Map<String, Object>) session.getAttribute(LoginServlet.USER);
    }
    if (user == null) {
      throw new ServletException("user not found");
    }
    String swap = request.getParameter("swap");
    String remove = request.getParameter("remove");
    try {
      if (swap != null) {
        String shift = request.getParameter("shift");
        if (shift == null) {
          sendError(response, HttpServletResponse.SC_NOT_FOUND,
            "<div id=\"eShiftNotFound\">Shift not found.</div>");
          return;
        }
        String[] excl = request.getParameterValues("exclude");
        boolean ok = patrolShifts.swapShift(user, shift, excl);
        log("swap=" + ok);
        if (ok) {
          sendError(response, HttpServletResponse.SC_OK,
            "<div id=\"eDone\">Ok</div>");
        } else {
          sendError(response, HttpServletResponse.SC_OK,
            "<div id=\"ePending\">Queued</div>");
        }
      }
      if (remove != null) {
        patrolShifts.removeSwapShift(user, remove);
        sendError(response, HttpServletResponse.SC_OK,
          "<div id=\"eDone\">Ok</div>");
      }
    } catch (EntityNotFoundException ex) {
      log(ex.getMessage(), ex);
      sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
        "<div id=\"eEntityNotFound\">Internal error.</div>");
    } catch (SMSException ex) {
      log(ex.getMessage(), ex);
      sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
        "<div id=\"eSMS\">Internal error.</div>");
    } catch (AddressException ex) {
      log(ex.getMessage(), ex);
      sendError(response, HttpServletResponse.SC_CONFLICT,
        "<div id=\"eAddress\">Address error.</div>");
    } catch (ConcurrentModificationException ex) {
      log(ex.getMessage(), ex);
      sendError(response, HttpServletResponse.SC_CONFLICT,
        "<div id=\"eConcurrentModification\">Concurrent modification.</div>");
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
