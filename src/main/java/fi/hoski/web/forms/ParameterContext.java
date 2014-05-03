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

import fi.hoski.util.Day;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

/**
 * @author Timo Vesalainen
 */
public class ParameterContext {

  private static final Pattern REF = Pattern.compile("\\$\\{([^\\}]+)\\}");
  private static final String TODAY = "_Today_";
  private ServletContext context;
  private ServletConfig config;
  private ServletRequest request;

  public ParameterContext(ServletConfig config, ServletRequest request) {
    this.context = config.getServletContext();
    this.config = config;
    this.request = request;
  }

  public boolean getBooleanParameter(String name) {
    String value = getParameter(name);
    if (value != null) {
      return Boolean.parseBoolean(value);
    } else {
      return false;
    }
  }

  public String[] getArrayParameter(String name) {
    String value = getParameter(name);
    if (value != null) {
      return value.split(",");
    } else {
      return null;
    }
  }

  /**
   * Returns true if parameter value comes directly from request
   *
   * @param name
   * @return
   */
  public boolean fromRequest(String name) {
    String value = request.getParameter(name);
    if (value != null) {
      Matcher m = REF.matcher(value);
      return !m.matches();
    }
    return false;
  }

  public String getParameter(String name) {
    String value = getParam(name);
    if (value != null) {
      Matcher m = REF.matcher(value);
      StringBuffer sb = new StringBuffer();
      while (m.find()) {
        String v = getConfigParam(m.group(1));
        if (v != null) {
          m.appendReplacement(sb, v);
        } else {
          m.appendReplacement(sb, "$0");
        }
      }
      m.appendTail(sb);
      return sb.toString();
    } else {
      return value;
    }
  }

  private String getParam(String name) {
    String value = request.getParameter(name);
    if (value == null) {
      return getConfigParam(name);
    }
    return value;
  }

  private String getConfigParam(String name) {
    if (TODAY.equals(name)) {
      return new Day().toString();
    }
    String value = config.getInitParameter(name);
    if (value == null) {
      return context.getInitParameter(name);
    }
    return value;
  }

  public Set<String> getParameterNames() {
    Set<String> set = new HashSet<String>();
    set.addAll(request.getParameterMap().keySet());
    Enumeration<String> initParameterNames = config.getInitParameterNames();
    while (initParameterNames.hasMoreElements()) {
      set.add(initParameterNames.nextElement());
    }
    return set;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (String name : getParameterNames()) {
      if (sb.length() > 0) {
        sb.append('&');
      }
      sb.append(name);
      sb.append('=');
      sb.append(getParameter(name));
    }
    return sb.toString();
  }
}
