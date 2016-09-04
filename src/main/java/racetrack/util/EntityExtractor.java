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
package racetrack.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import racetrack.framework.BundlesDT;

/**
 * Extract entities from a text block.  Encode them as subtexts for
 * overlay into the visualization.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class EntityExtractor {
  /**
   * (Pre-defined) Entity patterns
   */
  enum FindType { ENTITY, 
  /**
   * Relatinoship subtext (not implemented)
   */
                  RELATIONSHIP, 
  /**
   * Timestamp subtext
   */
		  TIMESTAMP,
  /**
   * Interval (duration, begin/end time) subtext
   */
		  INTERVAL };

  /**
   * Find the specific regex in the text and add the subtext to the list.
   *
   *@param all_text text block
   *@param regex    pattern to find
   *@param type     type of subtext to look for (have to ordered so that more general types are found first)
   *@param detected already extracted entity positions
   *@param al       list to add the subtexts to
   */
  private static void find(String all_text, String regex, FindType type,                        boolean detected[], List<SubText> al) {
    find(all_text, regex, type, null, detected, al);
  }

  /**
   * Find the specific regex in the text and add the subtext to the list.
   *
   *@param all_text text block
   *@param regex    pattern to find
   *@param type     type of subtext to look for (have to ordered so that more general types are found first)
   *@param datatype application-level data type
   *@param detected already extracted entity positions
   *@param al       list to add the subtexts to
   */
  private static void find(String all_text, String regex, FindType type, BundlesDT.DT datatype, boolean detected[], List<SubText> al) {
    Pattern pattern = Pattern.compile(regex); Matcher matcher = pattern.matcher(all_text);
    while (matcher.find()) {
      int i0 = matcher.start(), i1 = matcher.end(); String toplevel = Utils.stripSpaces(matcher.group());
      if (overlaps(i0,i1,detected) == false) {
        switch (type) {
          case ENTITY:       al.add(new Entity      (all_text, toplevel, datatype, i0, i1)); break;
	  case RELATIONSHIP: al.add(new Relationship(all_text, toplevel,           i0, i1)); break;
	  case TIMESTAMP:    al.add(new TimeStamp   (all_text, toplevel,           i0, i1)); break;
	  case INTERVAL:     al.add(new Interval    (all_text, toplevel,           i0, i1)); break;
	}
      }
    }
  }
  
  /**
   * Check for overlaps with a set of indices and modify the overlap array
   * to cover these for future comparisons.
   */
  private static boolean overlaps(int i0, int i1, boolean array[]) {
    boolean overlaps = false; 
    for (int i=i0;i<=i1;i++) { 
      if (i >= array.length) continue;
      if (array[i]) overlaps = true; array[i] = true; 
    } 
    return overlaps; 
  }

  /**
   * Find entities in the text block.
   * - 2013-06-01:  Removed several "too-generic" data types from the extractor
   *
   *@param all_text text block
   *@param detected already extracted subtexts
   *@param al       list to place discovered subtexts
   */
  private static void findEntities(String all_text, boolean detected[], List<SubText> al) {
    for (int i=0;i<BundlesDT.getNumberOfDataTypes();i++) {
      BundlesDT.DT datatype = BundlesDT.getDataType(i);

      // For entity extraction, several data types don't make sense to pull from the text
      if (datatype == BundlesDT.DT.INTEGER || datatype == BundlesDT.DT.FLAG || datatype == BundlesDT.DT.TAGS) continue;

      String       regex    = BundlesDT.getDataTypeRegex(i);
      if (regex != null) find(all_text, regex, FindType.ENTITY, datatype, detected, al);
    }
  }

  /**
   * Find relationships in the text block. Not implemented.
   *
   *@param all_text text block
   *@param detected already extracted subtexts
   *@param al       list to place discovered subtexts
   */
  private static void findRelationships(String all_text, boolean detected[], List<SubText> al) {
    String regex = Utils.getRelationshipRegex();
    find(all_text, regex, FindType.RELATIONSHIP, null, detected, al);
  }

  /**
   * Find timestamps in the text block.
   *
   *@param all_text text block
   *@param detected already extracted subtexts
   *@param al       list to place discovered subtexts
   */
  private static void findTimeStamps(String all_text, boolean detected[], List<SubText> al) {
    String  regex = Utils.getTimeStampRegex();
    if (regex != null) find(all_text, regex, FindType.TIMESTAMP, detected, al);
  }

  /**
   * Find intervals in the text block.
   *
   *@param all_text text block
   *@param detected already extracted subtexts
   *@param al       list to place discovered subtexts
   */
  private static void findIntervals(String all_text, boolean detected[], List<SubText> al) {
    find(all_text,              Utils.getTimeStampRegex() + " thru "    + Utils.getTimeStampRegex(), FindType.INTERVAL, detected, al);
    find(all_text,              Utils.getTimeStampRegex() + " through " + Utils.getTimeStampRegex(), FindType.INTERVAL, detected, al);
    find(all_text, "between " + Utils.getTimeStampRegex() + " and "     + Utils.getTimeStampRegex(), FindType.INTERVAL, detected, al);
  }

  /**
   * For a block of text, extract all of the known subtext types and return
   * them as a list.
   *
   *@param  str text block
   *
   *@return list of extracted subtexts
   */
  public static List<SubText> list(String str) {
    // Return variable
    List<SubText> al = new ArrayList<SubText>();
    // State to ensure that something isn't double counted...  need to order checks from most specific to least
    boolean detected[] = new boolean[str.length()];
    // Start checking -- most specific to least...
    findIntervals     (str, detected, al);
    findTimeStamps    (str, detected, al);
    findRelationships (str, detected, al);
    findEntities      (str, detected, al);
    return al;
  }

  /**
   * Extract the entities but return the substrings as a set.
   *
   *@param  str text block
   *@return set of extracted entity strings
   */
  public static Set<String> stringSet(String str) {
    List<SubText> al = list(str);
    Set<String> set = new HashSet<String>();
    Iterator<SubText> it = al.iterator();
    while (it.hasNext()) set.add(it.next().toString());
    return set;
  }
}
