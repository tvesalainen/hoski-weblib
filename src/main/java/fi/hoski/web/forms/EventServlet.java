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
import fi.hoski.datastore.BoatNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import fi.hoski.datastore.*;
import fi.hoski.datastore.repository.Event;
import fi.hoski.datastore.repository.Reservation;
import fi.hoski.datastore.repository.Event.EventType;
import fi.hoski.util.Day;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Map;
import java.util.List;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * EventServlet handles storing of event forms as well as form data for the
 * forms.
 */
public class EventServlet extends HttpServlet {

  public static final long serialVersionUID = -1;
  public static final String USER = "fi.hoski.web.user";
  Events eventManager;

  @Override
  public void init() {
    eventManager = new EventsImpl();
  }

  private void sendError(HttpServletResponse response,
    int statusCode,
    String htmlMessage) throws IOException {
    response.setStatus(statusCode);
    response.setContentType("text/html");
    response.setCharacterEncoding("utf-8");
    response.getWriter().write(htmlMessage);
  }

  @Override
  public void doPost(HttpServletRequest request,
    HttpServletResponse response)
    throws ServletException, IOException {
    response.setCharacterEncoding("UTF-8");

    Event event;
    String[] eventKeys = request.getParameterValues("event");
    if (eventKeys == null) {
      log("Event parameter missing");
      sendError(response, HttpServletResponse.SC_BAD_REQUEST,
        "<div id=\"eNoEvent\">Event parameter missing</div>");
      return;
    }
    int count = 1;
    try {
      for (String eventKey : eventKeys) {
        if (!eventKey.isEmpty()) {
          try {
            event = eventManager.getEvent(eventKey);
          } catch (Exception e) {
            log(eventKey);
            log(e.getMessage(), e);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
              "<div id=\"eNoEvent\">Event not found</div>");
            return;
          }

          Reservation reservation = new Reservation(event);

          @SuppressWarnings("unchecked")
          Map<String, String[]> params =
            (Map<String, String[]>) request.getParameterMap();

          reservation.set(Reservation.CREATOR, request.getRemoteUser());
          reservation.populate(params);
          String[] bk = params.get(Repository.VENEET_KEY);
          if (bk != null) {
            Key boatKey = KeyFactory.stringToKey(bk[0]);
            reservation.set(Repository.VENEID, boatKey);
          }

          eventManager.createReservation(reservation, false);
        } else {
          if (count == 1) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
              "<div id=\"eNoEvent\">Event key not found</div>");
            return;
          }
        }
        count++;
      }
    } catch (EntityNotFoundException ex) {
      throw new ServletException(ex);
    } catch (DoubleBookingException ex) {
      if (count == 1) {
        log(ex.getMessage(), ex);
        sendError(response, HttpServletResponse.SC_CONFLICT,
          "<div id=\"eDoubleBooking\">Double booking.</div>");
      }
    } catch (EventFullException e) {
      if (count == 1) {
        log(e.getMessage(), e);
        sendError(response, HttpServletResponse.SC_CONFLICT,
          "<div id=\"eEventFull\">Event full.</div>");
      }
    } catch (BoatNotFoundException e) {
      log(e.getMessage(), e);
      sendError(response, HttpServletResponse.SC_CONFLICT,
        "<div id=\"eBoatNotFound\">Boat not found.</div>");

    } catch (MandatoryPropertyMissingException e) {
      log(e.getMessage(), e);
      sendError(response, HttpServletResponse.SC_CONFLICT,
        "<div id=\"eMandatoryPropertyMissing\">"+e.getMessage()+" mandatory property missing.</div>");

    } catch (ConcurrentModificationException ex) {
      log(ex.getMessage(), ex);
      sendError(response, HttpServletResponse.SC_CONFLICT,
        "<div id=\"eConcurrentModification\">Concurrent modification.</div>");
    }
    response.setContentType("UTF-8");
    response.getWriter().write("Ilmoittautumisesi on vastaanotettu.");
  }

  @Override
  public void doGet(HttpServletRequest request,
    HttpServletResponse response)
    throws ServletException, IOException {
    response.setCharacterEncoding("UTF-8");

    // 1. event type
    EventType eventType;
    try {
      eventType = EventType.valueOf(request.getParameter("eventType"));
    } catch (Exception e) {
      sendError(response, HttpServletResponse.SC_BAD_REQUEST,
        "Bad parameter: eventType");
      return;
    }

    // 2. earliest date
    Date startDate;
    try {
      String evd = request.getParameter("startDate");
      startDate = evd != null ? new Date(Long.parseLong(evd)) : null;
    } catch (Exception e) {
      sendError(response, HttpServletResponse.SC_BAD_REQUEST,
        "Bad parameter: eventDate");
      return;
    }

    // 3. max count    
    Integer maxCount;
    try {
      String maxc = request.getParameter("maxCount");
      maxCount = maxc != null ? new Integer(maxc) : null;
    } catch (Exception e) {
      sendError(response, HttpServletResponse.SC_BAD_REQUEST,
        "Bad parameter: maxCount");
      return;
    }

    // 4. get events of said type
    List<Event> events = eventManager.getEvents(eventType, new Day(startDate), maxCount);

    // 5. write json
    response.setContentType("application/json");

    try {
      JSONArray jsonEvents = new JSONArray();
      for (Event event : events) {
        JSONObject e = new JSONObject(event.getAll());
        e.put("reservations", event.getChildCount());
        e.put("isFull", event.isFull());
        e.put("key", event.createKeyString());
        Day d = (Day) event.get(Event.EventDate);
        e.put("eventDate", d.getDate().getTime());
        e.put("eventName", d);
        jsonEvents.put(e);
      }

      JSONObject json = new JSONObject();

      json.put("events", jsonEvents);
      json.write(response.getWriter());
    } catch (JSONException e) {
      throw new ServletException(e);
    }
  }
}
