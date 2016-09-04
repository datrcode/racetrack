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
package racetrack.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import racetrack.util.Utils;

/**
 * Class for storing and retrieving user preferences and other
 * values across application instantiations.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class RTPrefs {
  /**
   * Store named properties to the RTPreference store.  Values
   * will be encoded by default to storage so that the delimiters
   * do not get corrupted.
   *
   * Recommend name equal to major.minor -- eg. "RTGraph.Relationships"
   *
   *@param name fully qualified name for storage.  Recommend major.minor.
   *@param i    integer to store
   */
  public static void store(String name, int i)         { store(name,"" + i); }

  /**
   * Store named properties to the RTPreference store.
   *
   *@param name fully qualified name for storage.  Recommend major.minor.
   *@param l    long to store
   */
  public static void store(String name, long l)        { store(name,"" + l); }

  /**
   * Store named properties to the RTPreference store.
   *
   *@param name fully qualified name for storage.  Recommend major.minor.
   *@param str  string to store
   */
  public static void store(String name, String str)    { 
    String strs[] = new String[1]; strs[0] = str; store(name, strs);
  }

  /**
   * Store named properties to the RTPreference store.
   *
   *@param name fully qualified name for storage.  Recommend major.minor.
   *@param strs array of strings to store
   */
  public static void store(String name, String strs[]) { 
    try {
      name = shorten(name);
      File file = RTDir.createOrGetRTFile(name + ".pref");
      PrintStream out = new PrintStream(new FileOutputStream(file));
      for (int i=0;i<strs.length;i++) out.println(Utils.encToURL(strs[i]));
      out.close();
    } catch (IOException ioe) {
      System.err.println("Problem Storing Preference \"" + name + "\" - IOE: " + ioe);
    }
  }

  /**
   * Retrieve different named properties from the RTPrefernce store.
   *
   *@param   name name to retrieve
   *@return       retrieved integer
   */
  public static int      retrieveInt    (String name) { 
    File file = RTDir.createOrGetRTFile(name + ".pref"); if (file.exists() == false) return 0;
    return Integer.parseInt(retrieveString(name)); }

  /**
   * Retrieve different named properties from the RTPrefernce store.
   *
   *@param   name name to retrieve
   *@return       retrieved long value
   */
  public static long     retrieveLong   (String name) { 
    File file = RTDir.createOrGetRTFile(name + ".pref"); if (file.exists() == false) return 0L;
    return Long.parseLong(retrieveString(name)); }

  /**
   * Retrieve different named properties from the RTPrefernce store.
   *
   *@param   name name to retrieve
   *@return       retrieved string
   */
  public static String   retrieveString (String name) { 
    String strs[] = retrieveStrings(name); if (strs == null || strs.length == 0) return null; else return strs[0]; }

  /**
   * Retrieve different named properties from the RTPrefernce store.
   *
   *@param   name name to retrieve
   *@return       retrieved array of strings
   */
  public static String[] retrieveStrings(String name) { 
    try {
      name = shorten(name);
      // Create file ref and check for existance
      File file = RTDir.createOrGetRTFile(name + ".pref");
      if (file.exists() == false) return new String[0];
      // Retrieve strings
      List<String> list = new ArrayList<String>();
      BufferedReader in = new BufferedReader(new FileReader(file));
      String line; while ((line = in.readLine()) != null) { list.add(Utils.decFmURL(line)); }
      in.close();
      // Transfer to array and return
      String strs[] = new String[list.size()];
      for (int i=0;i<strs.length;i++) strs[i] = list.get(i);
      return strs;
    } catch (IOException ioe) {
      System.err.println("Problem Retrieving Preference \"" + name + "\"");
      return new String[0]; 
    }
  }

  /**
   * Shorten the supplied string to make it smaller and to only use characters
   * supported for filenames.
   *
   *@param str string to shorten
   *
   *@return shortened version of the string
   */
  private static String shorten(String str) {
    if (str.length() > 64) { String hex = Long.toString(str.hashCode()&0x00ffffffffL, 16);
                             str = str.substring(0,16)                                   + "_" + 
                                   str.substring(str.length()/2 - 8, str.length()/2 + 8) + "_" +
                                   str.substring(str.length()-16,str.length())           + "_" + hex; }
    return str;
  }
}

/**
 * Handle/manage the RTDirectory within the the user's home directory
 */
class RTDir {
  /**
   * Get the file that represents the racetrack directory within the users
   * directory.
   *
   *@return directory for long term storage of the application data
   */
  public static File createOrGetRTDir() {
    File rtdir = new File(System.getProperty("user.home") + System.getProperty("file.separator") + ".racetdir");
    if (rtdir.exists() == false) rtdir.mkdir();
    return rtdir;
  }
  
  /**
   * Get the file that represents a specific resource name within the
   * racetrack directory.
   *
   *@param  name qualifier for the filename
   *
   *@return      file object
   */
  public static File createOrGetRTFile(String name) {
    if (name.indexOf(System.getProperty("file.separator")) >= 0) {
      System.err.println("RTDir.createOrGetRTFile() - name \"" + name + "\" contains file separator...  encoding...");
      name = Utils.encToURL(name);
    }
    return new File(createOrGetRTDir() + System.getProperty("file.separator") + name);
  }
}

