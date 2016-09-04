/* 

Copyright 2013 David Trimm

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package racetrack.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import racetrack.framework.BundlesDT;
import racetrack.gui.RTGraphPanel;
import racetrack.util.CacheManager;
import cz.mallat.uasparser.UASparser;
import cz.mallat.uasparser.UserAgentInfo;

/**
 * Class for transforming user agent strings into the various components.  Primarily
 * a wrapper class for an external library that specializes in user agent transformations.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class UserAgents {
  /**
   * The actual parser for this wrapper.
   */
  UASparser parser;

  /**
   * Construct a new UserAgent transformer.  Create the external interface for the
   * parsing calls.
   */
  private UserAgents() {
   try { 
     File dir         = new File(RTGraphPanel.class.getProtectionDomain().getCodeSource().getLocation().toURI());
     File ua_ini_file = new File(dir, "uas.ini");
     parser           = new UASparser(new FileInputStream(ua_ini_file));
     CacheManager.registerCache("User Agent Transform", cache);
   } catch (URISyntaxException urise) {
     System.err.println("URISyntaxException : " + urise);
   } catch (IOException ioe) {
     System.err.println("IOException: " + ioe);
   }
  }
  /**
   * Instance - Provides Singleton
   */
  private static UserAgents instance;

  /**
   * Return the singleton for this class.
   */
  public static  synchronized UserAgents getInstance() {
    if (instance == null) instance = new UserAgents();
    return instance;
  }

  /**
   * Enumeration for the components of the user agent string
   */
  public enum Component { TYPE, NAME, FAMILY, COMPANY, OSNAME, OSFAMILY, OSCOMPANY };

  /**
   * Caches the previously calculated transformation for faster use.
   */
  Map<String,Map<Component,String>> cache = new HashMap<String,Map<Component,String>>();

  /**
   * Lookup the specified component in the included user agent string.
   *
   *@param ua_str    user agent string to decode
   *@param component transformations
   *
   *@return user agent decoded result
   */
  public String lookup(String ua_str, Component component) { 
    if (cache.containsKey(ua_str) == false) {
      cache.put(ua_str, new HashMap<Component,String>());
      UserAgentInfo info = null; String str = null;
      try { info = parser.parse(ua_str); } catch (IOException ioe) { System.err.println("IOE: " + ioe); }

      // TYPE
      if (info == null || str == null || str.equals("")) str = BundlesDT.NOTSET;
      cache.get(ua_str).put(Component.TYPE,str); str = null;

      // NAME
      if (info != null) str = info.getUaName();
      if (info == null || str == null || str.equals("")) str = BundlesDT.NOTSET;
      cache.get(ua_str).put(Component.NAME,str); str = null;

      // FAMILY
      if (info != null) str = info.getUaFamily();
      if (info == null || str == null || str.equals("")) str = BundlesDT.NOTSET;
      cache.get(ua_str).put(Component.FAMILY,str); str = null;

      // COMPANY
      if (info != null) str = info.getUaCompany();
      if (info == null || str == null || str.equals("")) str = BundlesDT.NOTSET;
      cache.get(ua_str).put(Component.COMPANY,str); str = null;

      // OSNAME
      if (info != null) str = info.getOsName();
      if (info == null || str == null || str.equals("")) str = BundlesDT.NOTSET;
      cache.get(ua_str).put(Component.OSNAME,str); str = null;

      // OSFAMILY
      if (info != null) str = info.getOsFamily();
      if (info == null || str == null || str.equals("")) str = BundlesDT.NOTSET;
      cache.get(ua_str).put(Component.OSFAMILY,str); str = null;

      // OSCOMPANY
      if (info != null) str = info.getOsCompany();
      if (info == null || str == null || str.equals("")) str = BundlesDT.NOTSET;
      cache.get(ua_str).put(Component.OSCOMPANY,str); str = null;
    }
    return cache.get(ua_str).get(component);
  }
}

