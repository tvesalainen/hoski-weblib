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

import fi.hoski.datastore.EmailNotUniqueException;
import fi.hoski.datastore.DSUtils;
import fi.hoski.datastore.DSUtilsImpl;
import fi.hoski.datastore.repository.Messages;
import fi.hoski.web.mail.EmailService;
import fi.hoski.web.google.GoogleMailService;
import fi.hoski.web.google.DatastoreUserDirectory;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.text.MessageFormat;
import java.util.Random;
import java.util.ResourceBundle;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.ServletException;

/**
 * ActivationServlet creates a password for the user and sends the password to
 * the user's e-mail address. The user must already exist in the user directory.
 *
 * The ActivationServlet is designed to be used trough AJAX.
 */
public class ActivationServlet extends HttpServlet {

  public static final long serialVersionUID = -1;
  private static final String PASSWORD_CHARS =
    // "only" 2821109907456 combinations with 8 char password
    "1234567890abcdefghijklmnopqrstuvwxyz";
  // 7326680472586200649 combinations with 10 char password
  //"1234567890+!#%&/()=qwertyuiopasdfghjklzxcvbnm,.-QWERTYUIOPASDFGHJKLZXCVBNM;:_";
  private static final int ACTIVATION_INTERVAL_MIN = 1; //60*60*1000; // TODO: put back to 1h
  private static final int PASSWORD_LENGTH = 8;
  private Random random = new Random();
  private UserDirectory userDirectory;
  private EmailService emailService;
  private DSUtils entities;

  @Override
  public void init() {
    userDirectory = new DatastoreUserDirectory();
    emailService = new GoogleMailService();
    entities = new DSUtilsImpl();
  }

  @Override
  public void doPost(HttpServletRequest request,
    HttpServletResponse response)
    throws ServletException, IOException {
    response.setCharacterEncoding("UTF-8");

    String email = request.getParameter("email");
    email = (email != null) ? email.trim() : null;

    try {
      // 1. check params
      if (email == null || email.isEmpty()) {
        log("Missing parameter: email");
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Missing parameter: email");
      } else {
        // 2. check user exists
        Map<String, Object> user = userDirectory.findUser(email);
        if (user == null) {
          log("Unknown user");
          response.sendError(HttpServletResponse.SC_FORBIDDEN,
            "Unknown user");
        } else {
          // 3. check last activation
          Date latestActivation = null; // (Date) user.get(LATEST_ACTIVATION); TODO fixme
          if (latestActivation != null
            && latestActivation.after(new Date(System.currentTimeMillis()
            - ACTIVATION_INTERVAL_MIN))) {
            log("Too many activations");
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
              "Too many activations");
          } else {
            // finally everything is ok and we can activate
            String password = randomPassword();
            String activationKey = UUID.randomUUID().toString();
            userDirectory.setUserPassword(email, password, activationKey);
            URL base = new URL(request.getRequestURL().toString());
            URL activationUrl =
              new URL(base, "/login" + // TODO: make configurable
              "?email=" + URLEncoder.encode(email, "UTF-8")
              + "&activationKey=" + activationKey);
            emailPassword(email, password, request.getLocale(),
              activationUrl.toString());


            response.getWriter().write("Odota sinulle lähetettyä sähköpostia, jossa on uusi salasanasi.");
          }
        }
      }
    } catch (EmailNotUniqueException ex) {
      log(ex.getMessage(), ex);
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
        "E-mail cannot be sent, address not unique");
    } catch (IllegalArgumentException ex) {
      log(ex.getMessage(), ex);
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
        "E-mail cannot be sent");
    }
  }

  private String randomPassword() {
    StringBuilder sb = new StringBuilder();
    while (sb.length() < PASSWORD_LENGTH) {
      sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
    }
    return sb.toString();
  }

  private void emailPassword(String email, String password,
    Locale locale, String activationUrl)
    throws UnavailableException, IllegalArgumentException {
    Messages messages = entities.getMessages();

    String from = messages.getString("passwordFromAddress");
    String subject = messages.getString("passwordMessageSubject");
    String body = messages.getString("passwordMessageBody");

    emailService.send(from, subject,
      MessageFormat.format(body, email, password, activationUrl), email);
    log("sent message to " + email);
  }
}
