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

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.ShortBlob;
import fi.hoski.datastore.EmailNotUniqueException;
import fi.hoski.datastore.UsersImpl;
import fi.hoski.web.auth.UserDirectory;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.UnavailableException;

/**
 *
 * @author Timo Vesalainen
 */
public class DatastoreUserDirectory extends UsersImpl implements UserDirectory {


  public DatastoreUserDirectory() {
    super(DatastoreServiceFactory.getDatastoreService());
  }

  /**
   * Simple method for finding if user for email address exists. Returns a Map
   * containing properties for user. Property names don't have kind prefix!
   *
   * @param email
   * @return
   * @throws UnavailableException
   */
  @Override
  public Map<String, Object> findUser(String email) throws UnavailableException, EmailNotUniqueException {
    Entity user = retrieveUser(email);
    if (user != null) {
      return getUserData(user);
    } else {
      return null;
    }
  }

  @Override
  public void setUserPassword(String email, String password, String activationKey)
    throws UnavailableException, EmailNotUniqueException {
    byte[] passwordDigest = digest(password);
    Date latestActivation = new Date();
    Entity user = retrieveUser(email);
    if (user != null) {
      Key credKey = KeyFactory.createKey(CREDENTIALS, user.getKey().getId());
      Entity credentials = new Entity(credKey);
      
      credentials.setUnindexedProperty(PASSWORD, new ShortBlob(passwordDigest));
      credentials.setUnindexedProperty(LATEST_ACTIVATION, latestActivation);
      credentials.setUnindexedProperty(ACTIVATION_KEY, activationKey);
      datastore.put(credentials);
    } else {
      // what should we do here?
    }
  }

  @Override
  public Map<String, Object> useActivationKey(String email, String activationKey)
    throws UnavailableException, EmailNotUniqueException {
    Entity credentials = retrieveCredentials(email);
    if (credentials != null && 
        activationKey.equals(credentials.getProperty(ACTIVATION_KEY))) {
      credentials.removeProperty(ACTIVATION_KEY);
      datastore.put(credentials);
      return findUser(email);
    } else {
      return null;
    }
  }

  @Override
  public Map<String, Object> authenticateUser(String email, String password)
    throws UnavailableException, EmailNotUniqueException {
    byte[] passwordDigest = digest(password);
    Entity credentials = retrieveCredentials(email);

    if (credentials != null) {
      ShortBlob userPasswordDigest = (ShortBlob) credentials.getProperty(PASSWORD);
      if (userPasswordDigest != null
        && Arrays.equals(userPasswordDigest.getBytes(), passwordDigest)) {

        return findUser(email);
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  private byte[] digest(String password) throws UnavailableException {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      md.update("fi.hoski".getBytes()); // a grain of salt     
      return md.digest(password.getBytes());
    } catch (NoSuchAlgorithmException e) {
      throw new UnavailableException("Requirements failed");
    }
  }

}
