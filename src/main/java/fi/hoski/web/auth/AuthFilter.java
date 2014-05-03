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
package fi.hoski.web.auth;

import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import static fi.hoski.web.auth.UserDirectory.EMAIL;

/**
 * An authorization filter that authorizes access based on the existence of a
 * HttpSession and a user object in it.
 */
public class AuthFilter implements Filter {

  private FilterConfig filterConfig;

  public void init(FilterConfig filterConfig) {
    this.filterConfig = filterConfig;
  }

  public void destroy() {
  }

  public void doFilter(ServletRequest request, ServletResponse response,
    FilterChain chain)
    throws ServletException, IOException {

    if (request instanceof HttpServletRequest) {
      HttpServletRequest req = (HttpServletRequest) request;
      HttpServletResponse res = (HttpServletResponse) response;

      res.setHeader("Cache-Control", "private, max-age=0, must-revalidate");

      HttpSession session = req.getSession(false);
      // 1. check session and user exist
      @SuppressWarnings("unchecked")
      final Map<String, Object> user = (session != null)
        ? (Map<String, Object>) session.getAttribute(LoginServlet.USER) : null;

      if (user == null) {
        // Clear cookie so that 'Vary: Cookie' works
        Cookie c = new Cookie("JSESSIONID", null);
        c.setMaxAge(0);
        res.addCookie(c);

        if (req.getMethod().equals("GET")) {
          res.setStatus(HttpServletResponse.SC_FORBIDDEN);

          // bug fix for jetty, which responds NOT_MODIFIED even if status
          // has already been set to error. 
          ServletRequest wrapper = new HttpServletRequestWrapper(req) {
            // don't return If-* headers (or any other headers for that
            // matter)

            public String getHeader(String name) {
              return name.startsWith("If-") ? null : super.getHeader(name);
            }
          };

          filterConfig.getServletContext().getRequestDispatcher("/login.html").forward(wrapper, response);
        } else {
          res.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
      } else {
        // 2. create request wrapper
        ServletRequest wrapper = new HttpServletRequestWrapper(req) {

          @Override
          public String getRemoteUser() {
            Object email = user.get(EMAIL);
            return (email != null) ? email.toString() : null;
          }

          @Override
          public boolean isUserInRole(String role) {
            return "member".equals(role);
          }
        };
        chain.doFilter(wrapper, res);
      }
    } else {
      throw new ServletException("Unknown request type");
    }
  }
}
