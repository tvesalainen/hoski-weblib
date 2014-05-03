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
package fi.hoski.web.google;

import fi.hoski.web.mail.EmailService;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.UnavailableException;

public class GoogleMailService implements EmailService {

  @Override
  public void send(String from, String subject, String body, String... to)
    throws UnavailableException, IllegalArgumentException {
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);

    try {
      Message msg = new MimeMessage(session);
      msg.setFrom(new InternetAddress(from));
      for (String recipient : to) {
        msg.addRecipient(Message.RecipientType.BCC,
          new InternetAddress(recipient));
      }
      msg.setSubject(subject);
      msg.setText(body);
      Transport.send(msg);

    } catch (AddressException e) {
      throw new IllegalArgumentException(e);
    } catch (MessagingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
