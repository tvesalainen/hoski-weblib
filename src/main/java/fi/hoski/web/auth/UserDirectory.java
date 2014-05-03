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
import java.util.Map;
import javax.servlet.UnavailableException;

public interface UserDirectory {
 
  /**
   * Key of the latest activation.
   *
   * Latest activation is the last time the user was activated in the
   * form of java.util.Date object. Used to restrict the number of
   * activations per timespan.
   */
  public static final String LATEST_ACTIVATION = "latestActivation";
  /**
   * On-time activation key the user can use to log-in from e-mail.
   */
  public static final String ACTIVATION_KEY = "activationKey";
  /**
   * Key of the user's password digest.
   */
  public static final String PASSWORD = "password";

  /**
   * Key of the user's e-mail address.
   */
  public static final String EMAIL = "Jasenet.Email";

  /**
   * Finds a user using e-mail address.
   *
   * @param email the e-mail address of the user
   * @return the user data as a key-value map or null if the user
   * was not found.
   */
  Map<String,Object> findUser(String email) throws UnavailableException, EmailNotUniqueException;

  /**
   * Finds and authenticates the user using e-mail address and password.
   *
   * @param email the e-mail address of the user
   * @param password of the user (in clear text)
   * @return the user data as a key-value map or null if the user
   * was not found or the password incorrect.
   */
  Map<String,Object> authenticateUser(String email, String password) throws UnavailableException, EmailNotUniqueException;
    
  /**
   * Sets the user's password. Also updates the user's
   * LATEST_ACTIVATION field to the value of new Date();
   */
  void setUserPassword(String email, String password, String activationKey) throws UnavailableException, EmailNotUniqueException;

  /**
   * Clears the users activation key.
   */
  Map<String,Object>  useActivationKey(String email, String activationKey) throws UnavailableException, EmailNotUniqueException;

}
