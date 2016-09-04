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

package racetrack.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import racetrack.gui.RT;
import racetrack.util.CSVReader;

/**
 * Utilities for the classes in this file.
 *
 * @author  D. Trimm 
 * @version 1.0
 */
public class BundlesUtils {
  /**
   * Parse a data file and load it into the application.
   *
   * @param  bundles    application data (output)
   * @param  rt         application class
   * @param  file       file to parse
   * @param  appconfigs lines from the parsed file that may indicate application configuration information (output)
   *
   * @return         set of the bundles (records) that were loaded
   */
  public static Set<Bundle> parse(Bundles bundles, RT rt, File file, List<String> appconfs) {
    // Determine if the delimiter is commas, tabs, or pipes
    BufferedReader in = null; Map<String,Map<Integer,Integer>> map = new HashMap<String,Map<Integer,Integer>>();
    map.put(",",  new HashMap<Integer,Integer>()); map.put("|",  new HashMap<Integer,Integer>()); map.put("\t", new HashMap<Integer,Integer>());
    try {
      if (file.getName().toLowerCase().endsWith(".gz")) in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)))); 
      else                                              in = new BufferedReader(new FileReader(file)); 
      
      // Parse the first ten lines or so to determine the delimiter
      String line; int i = 0;
      while ((line = in.readLine()) != null && line.equals("") == false && i < 10) {
        i++;
        int commas = count(line, ','), pipes  = count(line, '|'), tabs   = count(line, '\t');
        // System.err.println("commas=" + commas + " pipes=" + pipes + " tabs=" + tabs + " \"" + line + "\"");
        if (commas > 0) { if (!map.get(",").containsKey(commas))  map.get(",").put(commas, 0);  map.get(",").put(commas,  map.get(",").get(commas)  + 1); }
        if (pipes  > 0) { if (!map.get("|").containsKey(pipes))   map.get("|").put(pipes,  0);  map.get("|").put(pipes,   map.get("|").get(pipes)   + 1); }
        if (tabs   > 0) { if (!map.get("\t").containsKey(tabs))   map.get("\t").put(tabs,  0);  map.get("\t").put(tabs,   map.get("\t").get(tabs)   + 1); }
      }
    } catch (IOException ioe) {
      System.err.println("IOException: " + ioe);
      ioe.printStackTrace(System.err);
    } finally {
      if (in != null) try { in.close(); } catch (IOException ioe) { } 
    }

    // Determine the strongest pattern for the delimiter -- checks for comma, pipe, and tab
    String delims = ","; int c_strength, p_strength, t_strength;
    if (map.get(","). keySet().size() == 1) { c_strength = map.get(","). get(map.get(","). keySet().iterator().next()); } else c_strength = -1;
    if (map.get("|"). keySet().size() == 1) { p_strength = map.get("|"). get(map.get("|"). keySet().iterator().next()); } else p_strength = -1;
    if (map.get("\t").keySet().size() == 1) { t_strength = map.get("\t").get(map.get("\t").keySet().iterator().next()); } else t_strength = -1;

    if      (c_strength > p_strength && c_strength > t_strength) delims = ",";
    else if (p_strength > c_strength && p_strength > c_strength) delims = "|";
    else if (t_strength > p_strength && t_strength > c_strength) delims = "\t";
    // System.err.println("c=" + c_strength + " | p=" + p_strength + " | t=" + t_strength); // DEBUG

    // Create the set for storage
    Set<Bundle> set = new HashSet<Bundle>();
    // Execute the parser
    CSVParser csv_parser = null;
    try { 
      // System.err.println("Creating CSVReader..."); // DEBUG
      CSVReader reader = new CSVReader(file, csv_parser = new CSVParser(bundles, rt, set), delims, true); 
      // System.err.println("  CSVReader Complete!"); // DEBUG
    } catch (IOException ioe) { 
      System.err.println("IOException: " + ioe); ioe.printStackTrace(System.err); 
    }
    // Make sure that the class had a chance to add the lists to main class
    if (csv_parser != null) { csv_parser.addListsToRT(); if (appconfs != null) appconfs.addAll(csv_parser.getAppConfigs()); }
    // Re-run the transforms for the fast lookup tables
    bundles.getGlobals().resetTransforms();
    return set;
  }

  /**
   * Count the number of character occurences within a string.
   *
   *@param str string to examine
   *@param c   charater to count
   *
   *@return number of times character occurs in string
   */
  public static int count(String str, char c) {
    int sum = 0;
    for (int i=0;i<str.length();i++) if (str.charAt(i) == c) sum++;
    return sum;
  }
}

