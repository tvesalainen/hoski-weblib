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

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Timo Vesalainen
 */
public class RaceAuthFilter implements Filter {
  
  private DatastoreService datastore;
  // The filter configuration object we are associated with.  If
  // this value is null, this filter instance is not currently
  // configured. 
  private FilterConfig filterConfig = null;
  
  public RaceAuthFilter() {
  }

  /**
   *
   * @param request The servlet request we are processing
   * @param response The servlet response we are creating
   * @param chain The filter chain we are processing
   *
   * @exception IOException if an input/output error occurs
   * @exception ServletException if a servlet error occurs
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
    FilterChain chain)
    throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;
    UserService userService = UserServiceFactory.getUserService();
    String thisURL = req.getRequestURI();
    
    if (userService.isUserLoggedIn()) {
      User currentUser = userService.getCurrentUser();
      String email = currentUser.getEmail();
      Query query = new Query("RaceAdmins");
      query.addFilter("Email", Query.FilterOperator.EQUAL, email);
      PreparedQuery prepared = datastore.prepare(query);
      int count = prepared.countEntities(FetchOptions.Builder.withDefaults());
      if (count > 0) {
        chain.doFilter(request, response);
      } else {
        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      }
    } else {
        resp.sendRedirect(userService.createLoginURL(thisURL));
    }
  }

  /**
   * Return the filter configuration object for this filter.
   */
  public FilterConfig getFilterConfig() {
    return (this.filterConfig);
  }

  /**
   * Set the filter configuration object for this filter.
   *
   * @param filterConfig The filter configuration object
   */
  public void setFilterConfig(FilterConfig filterConfig) {
    this.filterConfig = filterConfig;
  }

  /**
   * Destroy method for this filter
   */
  public void destroy() {
    datastore = null;
  }

  /**
   * Init method for this filter
   */
  public void init(FilterConfig filterConfig) {
    this.filterConfig = filterConfig;
    if (filterConfig != null) {
      datastore = DatastoreServiceFactory.getDatastoreService();
    }
  }

  /**
   * Return a String representation of this object.
   */
  @Override
  public String toString() {
    if (filterConfig == null) {
      return ("RaceAuthFilter()");
    }
    StringBuffer sb = new StringBuffer("RaceAuthFilter(");
    sb.append(filterConfig);
    sb.append(")");
    return (sb.toString());
  }
  
  public void log(String msg) {
    filterConfig.getServletContext().log(msg);
  }
}
