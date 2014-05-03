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
import fi.hoski.datastore.repository.*;
import fi.hoski.web.ServletLog;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Timo Vesalainen
 */
public class MenuServlet extends HttpServlet {

  public static final String MENUKIND = "menuKind";
  public static final String PROPERTIES = "properties";
  public static final String FILTER = "filter";
  public static final String ALL = "all";
  public static final String REV = "rev";
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
    boolean authenticated = request.isUserInRole("member");
    try {
      ParameterContext c = new ParameterContext(this, request);
      log(c.toString());
      printMenu(out, c, authenticated);
    } catch (EntityNotFoundException ex) {
      throw new ServletException(ex);
    } finally {
      out.close();
    }
  }

  private void printMenu(PrintWriter out, ParameterContext c, boolean authenticated) throws EntityNotFoundException, ServletException {
    NavigableMap<Key, NavigableMap> root = new TreeMap<>();
    Deque<Key> stack = new ArrayDeque<>();
    String kind = c.getParameter(MENUKIND);
    if (kind == null) {
      throw new ServletException(MENUKIND + " not set");
    }
    boolean rev = c.getBooleanParameter(REV);
    Query query = new Query(kind);
    boolean all = c.getBooleanParameter(ALL);
    if (!all) {
      Key yearKey = entities.getYearKey();
      query.setAncestor(yearKey);
    }
    query.setKeysOnly();
    setFilter(query, c);
    //query.addFilter(Event.EVENTDATE, Query.FilterOperator.GREATER_THAN_OR_EQUAL, new Day().getValue());
    log(query.toString());
    PreparedQuery preparedQuery = datastore.prepare(query);
    int maxDepth = 0;
    for (Entity entity : preparedQuery.asIterable()) {
      Key key = entity.getKey();
      // reverse the key
      while (key != null) {
        stack.push(key);
        key = key.getParent();
      }
      maxDepth = Math.max(maxDepth, stack.size());
      NavigableMap<Key, NavigableMap> tree = root;
      // create the tree
      while (!stack.isEmpty()) {
        Key k = stack.pop();
        NavigableMap<Key, NavigableMap> st = tree.get(k);
        if (st == null) {
          st = new TreeMap<Key, NavigableMap>();
          tree.put(k, st);
        }
        tree = st;
      }
    }
    int depth = maxDepth;
    // skip single choises
    while (root.size() == 1 && depth > 2) {
      Set<NavigableMap.Entry<Key, NavigableMap>> entrySet = root.entrySet();
      NavigableMap.Entry<Key, NavigableMap> entry = entrySet.iterator().next();
      root = entry.getValue();
      depth--;
    }
    printTree(out, root, c, kind, 1, authenticated, rev);
  }

  private void printTree(PrintWriter out, NavigableMap<Key, NavigableMap> t, ParameterContext c, String kind, int level, boolean authenticated, boolean rev) throws EntityNotFoundException {
    if (!t.isEmpty()) {
      out.println("<ul class=\"l" + level + "\">");
      for (Key key : rev ? t.descendingKeySet() : t.navigableKeySet()) {
        out.println("<li><p class=\"l" + level + "\">" + keyToString(key, c, kind, authenticated) + "</p>");
        printTree(out, t.get(key), c, kind, level + 1, authenticated, rev);
        out.println("</li>");
      }
      out.println("</ul>");
    }
  }

  private String keyToString(Key key, ParameterContext c, String kind, boolean authenticated) throws EntityNotFoundException {
    KeyInfo keyInfo = new KeyInfo(entities, events, races, c.toString(), key, authenticated);
    if (key.getKind().equals(kind)) {
      if (c.getParameter(PROPERTIES) == null) {
        return keyInfo.getEditLink();
      } else {
        return keyInfo.getMenuLink();
      }
    }
    return keyInfo.getLabel();
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
  private static final Pattern CONSTRAINT = Pattern.compile("(\\$lt;)|(\\$le;)|(\\$equals;)|(\\$ne;)|(\\$ge;)|(\\$gt;)");

  private void setFilter(Query query, ParameterContext c) throws ServletException {
    String[] filters = c.getArrayParameter(FILTER);
    if (filters != null) {
      DataObjectModel model = entities.getModel(query.getKind());
      for (String filter : filters) {
        String[] split = CONSTRAINT.split(filter);
        if (split.length != 2) {
          throw new ServletException("illegal filter " + filter);
        }
        Matcher matcher = CONSTRAINT.matcher(filter);
        if (!matcher.find()) {
          throw new ServletException("illegal filter " + filter);
        }
        String constraint = matcher.group();
        Object value = model.convert(split[0], split[1]);
        if ("$lt;".equals(constraint)) {
          query.addFilter(split[0], Query.FilterOperator.LESS_THAN, value);
        } else {
          if ("$le;".equals(constraint)) {
            query.addFilter(split[0], Query.FilterOperator.LESS_THAN_OR_EQUAL, value);
          } else {
            if ("$gt;".equals(constraint)) {
              query.addFilter(split[0], Query.FilterOperator.GREATER_THAN, value);
            } else {
              if ("$ge;".equals(constraint)) {
                query.addFilter(split[0], Query.FilterOperator.GREATER_THAN_OR_EQUAL, value);
              } else {
                if ("$equals;".equals(constraint)) {
                  query.addFilter(split[0], Query.FilterOperator.EQUAL, value);
                } else {
                  if ("$ne;".equals(constraint)) {
                    query.addFilter(split[0], Query.FilterOperator.NOT_EQUAL, value);
                  } else {
                    throw new ServletException(constraint);
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
