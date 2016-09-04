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
package racetrack.kb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import racetrack.framework.BundlesDT;

import racetrack.gui.RT;

import racetrack.util.Entity;
import racetrack.util.EntityExtractor;
import racetrack.util.Interval;
import racetrack.util.SubText;
import racetrack.util.TimeStamp;
import racetrack.util.Utils;

/**
 * Class representing a textual comment about the bundles (records).  Originally,
 * the comment stored all of the related bundle UUID to recreate views.  However,
 * the storage costs were excessive.  Ideally, we should be able to recreate
 * the views for when a comment is added and include annotations.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class RTComment {
  /**
   * Flag to indicate that the comment has been modified
   */
  boolean modified = false;

  /**
   * Return true if the comment has been modfied.
   *
   *@return true, if modified
   */
  public boolean commentModified() { return modified;  }

  /**
   * Write the current comment to disk (not implemeneted).
   */
  public void    storedToDisk()    { modified = false; }

  /**
   * Minimum timestamp found in the comment (or related bundles, not fully implemented)
   */
  long         ts_min            = Long.MAX_VALUE, 
  /**
   * Maximum timestamp found int he comment (or related bundles, not fully implemented)
   */
               ts_max            = 0L;

  /**
   * Title of comment
   */
  String       title             = "Title",
  /**
   * Tags - for searching for instance (not implemented)
   */
               tags              = "", 
  /**
   * Text of comments
   */
	       text              = "",      // 1.2.3.4 www.something.com http://this.test.com/index.html 5.6.7.8 1.2.3.4";
  /**
   * Location of the comment on the network
   */
	       url               = "";      // How to get to this comment
  /**
   * Unique ID for the comment (useful for updating, de-duping)
   */
  UUID         uuid;
  /**
   * Parent UUID (if comment is part of a thread) (not implemented)
   */
  UUID         parent_uuid       = null;

  /**
   * Enumeration for the level of detail for a comment
   */
  enum Level { OVERVIEW, GENERAL, DETAIL };

  /**
   * Level of detail for the comment
   */
  Level        level             = Level.DETAIL;

  /**
   * Source of comment - most likely a user name but could be a webpage
   */
  String       source            = RT.getUserName();

  /**
   * Creation time of comment
   */
  long         created_ts, 

  /**
   * Last modification time of comment
   */
               last_modified_ts, 
  /**
   * Last access time of comment
   */
	       last_accessed_ts;


  /**
   * Validity flag - used for parsing comments from file lines.  If 
   * a parse error occurs, then the line is not valid.
   */
  boolean      valid_flag        = true; 

  /**
   * Return true if the comment is valid (i.e., parsed correctly).
   *
   *@return true if valid
   */
  public boolean valid() { return valid_flag; }

  /**
   * Construct a new comment from a file line.  If the comment does
   * not parse correctly, set the valid flag to false.
   *
   *@param line file line containing encoded comment
   */
  public        RTComment(String line) {
    valid_flag = false;
    try {
      String tokens[] = Utils.tokenize(line,",");
        created_ts       = Utils.parseTimeStamp(tokens[0]);
	last_modified_ts = Utils.parseTimeStamp(tokens[1]); modified = false;
	last_accessed_ts = Utils.parseTimeStamp(tokens[2]);
	title            = Utils.decFmURL(tokens[3]);
	tags             = Utils.decFmURL(tokens[4]);
	source           = Utils.decFmURL(tokens[5]);
	uuid             = UUID.fromString(Utils.decFmURL(tokens[6]));
	tokens[7]        = Utils.decFmURL(tokens[7]);
        if      (tokens[7].equals("" + Level.OVERVIEW)) level = Level.OVERVIEW;
	else if (tokens[7].equals("" + Level.GENERAL))  level = Level.GENERAL;
	else if (tokens[7].equals("" + Level.DETAIL))   level = Level.DETAIL;
	else throw new RuntimeException("Don't Understand Level \"" + tokens[7] + "\"");
	url              = Utils.decFmURL(tokens[8]);
	tokens[9]        = Utils.decFmURL(tokens[9]);
        if      (tokens[9].equals(""))                  parent_uuid = null;
	else                                            parent_uuid = UUID.fromString(tokens[9]);
	text             = Utils.decFmURL(tokens[10]);
      valid_flag = true;
    } catch (Throwable t) {
      System.err.println("Error Parsing Token File Line \"" + t + "\"");
    }
  }

  /**
   * Load a textfile as a comment.  Do something reasonable with the fields in the comment object.
   *
   *@param file file to load
   */
  public RTComment(File file) throws IOException {
    valid_flag = false;

    // Load the file in
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(file)); String line = null, first_line = null;
      StringBuffer   sb = new StringBuffer(); while ((line = in.readLine()) != null) { if (first_line == null) first_line = line; sb.append(line); sb.append("\r\n"); }

      created_ts  = last_accessed_ts = last_modified_ts = file.lastModified();
      source      = "File From Disk";
      tags        = BundlesDT.NOTSET;
      uuid        = UUID.randomUUID();
      url         = BundlesDT.NOTSET;
      parent_uuid = null;
      text        = sb.toString();
      level       = Level.GENERAL;
  
      if (first_line != null && first_line.length() < 128) title = first_line; else title = "File \"" + file.getName() + "\"";
  
      valid_flag = true;
    } finally { if (in != null) in.close(); }
  }

  /**
   * Return the file header associated with a file of comments.  This uses CSV formatting.
   *
   *@return file header line for comment storage
   */
  public static String getFileHeader() { return "beg,end,accessed,title,tags,source,uuid,level,commenturl,parentuuid,text"; }

  /**
   * Return the comment as a file line.  This encodes the comment using a pseudo-URL encoding
   * scheme to handle binary and delimiters.
   *
   *@return comment as a file line
   */
  public        String asFileLine()    { return Utils.exactDate(created_ts)          + "," +
                                                Utils.exactDate(last_modified_ts)    + "," +
						Utils.exactDate(last_accessed_ts)    + "," +
						Utils.encToURL(getTitle())           + "," +
						Utils.encToURL(getTags())            + "," +
						Utils.encToURL(getSource())          + "," + 
						Utils.encToURL("" + getUUID())       + "," +
						Utils.encToURL("" + getLevel())      + "," +
						Utils.encToURL(getURL())             + "," +
						(getParentUUID() != null ? "" + getParentUUID() : "") + "," +
						Utils.encToURL(getText()); }

  /**
   * Return the UUID of the comment.
   *
   *@return UUID
   */
  public UUID getUUID()       { return uuid; }

  /**
   * Return the parent UUID of the comment if it exists (comment is part of a thread), null otherwise.
   *
   *@return UUID of parent comment
   */
  public UUID getParentUUID() { return parent_uuid; }

  /**
   * Get the title of the comment.
   *
   *@return title of comment
   */
  public String getTitle()           { last_accessed_ts = System.currentTimeMillis(); return title; } 

  /**
   * Set the title of the comment.
   *
   *@param str new title of comment
   */
  public void   setTitle(String str) { 
    if (str.equals(title) == false) {
      last_modified_ts = System.currentTimeMillis();
      modified = true;
      title = str; 
    }
  }

  /**
   * Get the tags for the comment.  See the Utils.java class for a description
   * of the formatting/delimiting of multiple tags.
   *
   *@return tags
   */
  public String getTags()           { last_accessed_ts = System.currentTimeMillis(); return tags;  } 

  /**
   * Set the tags for the comment.
   *
   *@param str new tags
   */
  public void   setTags(String str) { 
    if (str.equals(tags) == false) {
      last_modified_ts = System.currentTimeMillis();
      modified = true;
      tags  = str; 
    }
  }

  /**
   * Get the text body of the comment.
   *
   *@return text body (the actual comment)
   */
  public String getText()           { last_accessed_ts = System.currentTimeMillis(); return text;  } 

  /**
   * Set the text body of the comment.
   *
   *@param str new text body
   */
  public void   setText(String str) { 
    if (str.equals(text) == false) {
      last_modified_ts = System.currentTimeMillis();
      modified = true;
      text  = str; 
    }
  }

  /**
   * Return the level of detail for this comment.
   *
   *@return level of detail
   */
  public Level     getLevel()          { last_accessed_ts = System.currentTimeMillis(); return level; } 

  /**
   * Set the level of detail for this comment.
   *
   *@param lvl new level of detail
   */
  public void      setLevel(Level lvl) { 
    if (level != lvl) {
      last_modified_ts = System.currentTimeMillis();
      modified = true;
      level = lvl;  
    }
  }

  /**
   * Return the URL for accessing this comment (not implemented).
   *
   *@return URL for comment
   */
  public String    getURL()              { return url;              }

  /**
   * Return the source of the comment - usually the username.
   *
   *@return source of comment (username)
   */
  public String    getSource()           { return source;           }

  /**
   * Return the creation timestamp in the comment.
   *
   *@return creation timestamp
   */
  public long      getCreationTime()     { return created_ts;       }

  /**
   * Return the last modification time of the comment.
   *
   *@return last modification time of the comment
   */
  public long      getModificationTime() { return last_modified_ts; }

  /**
   * Return the last access time for the comment (not guaranteed unless the user has resaved the comments).
   *
   *@return last access time
   */
  public long      getAccessTime()       { return last_accessed_ts; } // ISSUE: Not guaranteed to get written to disk...

  /**
   * Return the minimum timestamp in the comment.
   *
   *@return minimum timestamp
   */
  public long      getMinTimeStamp()     { return ts_min;           }

  /**
   * Return the maximum timestamp in the comment.
   *
   *@return maximum timestamp
   */
  public long      getMaxTimeStamp()     { return ts_max;           }

  /**
   * Cache the last extraction
   */
  private List<SubText> last_extracted_list = null;

  /**
   * Cache the last extracted text
   */
  private String        last_extracted_text = null;

  /**
   * Extract entities from the document and store them as {@link SubText} elements
   * that are returned as a list.
   *
   *@return list of extracted entities
   */
  public List<SubText> listEntitiesAndRelationships() {
    long ts0 = System.currentTimeMillis();
    // Return variable -- check for a cache match
    List<SubText> al;
    if (getText().equals(last_extracted_text)) { al = last_extracted_list; } else { al = EntityExtractor.list(getText()); }
    last_extracted_text = getText();
    last_extracted_list = al;
    // Reset the timestamps 
    long ts_min = Long.MAX_VALUE, ts_max = 0L;
    // Go through the elements
    for (int i=0;i<al.size();i++) {
      if        (al.get(i) instanceof Interval)  {
        Interval interval = (Interval) al.get(i);
	if (interval.getMinTimeStamp() < ts_min) ts_min = interval.getMinTimeStamp();
	if (interval.getMaxTimeStamp() > ts_max) ts_max = interval.getMaxTimeStamp();
      } else if (al.get(i) instanceof TimeStamp) {
        TimeStamp timestamp = (TimeStamp) al.get(i);
	if (timestamp.getTimeStamp() < ts_min) ts_min = timestamp.getTimeStamp();
	if (timestamp.getTimeStamp() > ts_max) ts_max = timestamp.getTimeStamp();
      }
    }
    long ts1 = System.currentTimeMillis();
    if ((ts1 - ts0) > 500L) System.err.println("  RTComment.listEntitiesAndRelationships():  Slow Execution Time: " + Utils.humanReadableDuration(ts1 - ts0) + " ... Results=" + al.size());
    return al;
  }

  /**
   * Default constructor for a comment.
   */
  public RTComment() { 
    created_ts = last_modified_ts = last_accessed_ts = System.currentTimeMillis();
    long hi = Double.doubleToLongBits(Math.random()) & 0xffffffffffff0fffL; hi = hi | 0x0000000000004000L;
    long lo = created_ts                             & 0x0fffffffffffffffL; lo = lo | 0xa000000000000000L;
    uuid = new UUID(hi,lo);
  }

  /**
   * More specific constructor.  Includes a few options.
   *
   *@param title  title of comment
   *@param text   body of comment
   *@param source source name of comment
   *@param ts     creation time of comment
   *@param uuid   unique ID for the comment
   */
  public RTComment(String title, String text, String source, long ts, UUID uuid) {
    this.title = title;
    this.text  = text;
    created_ts = last_modified_ts = last_accessed_ts = ts;
    this.uuid  = uuid;
  }

  /**
   * Append a timestamp string to the comment.  Useful for building the comment overtime from 
   * interaction with the GUI.
   *
   *@param ts timestamp to add
   */
  public void addTime    (long ts) {
    appendToText("\n" + Utils.humanReadableDate(ts));
    if (ts_min > ts) ts_min = ts; if (ts_max < ts) ts_max = ts;
  }

  /**
   * Append a durationg string to the comment.  Useful for building the comment over time from
   * interaction with the GUI.
   *
   *@param ts_from beginning of the interval
   *@param ts_to   ending of the interval
   */
  public void addDuration(long ts_from, long ts_to) {
    appendToText("\n" + Utils.humanReadableDate(ts_from) + " thru " + Utils.humanReadableDate(ts_to));
    if (ts_min > ts_from) ts_min = ts_from; if (ts_max < ts_to) ts_max = ts_to;
  }

  /**
   * Append a list of entities to the comment.  Useful for building the comment over time from
   * interaction with the GUI.
   *
   *@param ents entities to add
   */
  public void addEntities(String ents[]) {
    for (int i=0;i<ents.length;i++) {
      appendToText("\n" + ents[i]);
    }
  }

  /**
   * Append text to the end of the comment body.
   *
   *@param str text to append
   */
  protected void appendToText(String str) { text += str; }

  /**
   * Calculate the min and max times embedded within a comment.  Not implemented - should
   * probably use the entity extraction and then go through the time-based subtexts...
   */
  public void calculateTime() {
    System.err.println("calculateTime() - not implemented...");
    System.err.println("calculateTime() - not implemented...");
    System.err.println("calculateTime() - not implemented...");
  }

  /**
   * Return the minimum timestamp in the comment.
   *
   *@return minimum timestamp in comment
   */
  public long ts0() { return ts_min; }

  /**
   * Return the maximum timestamp in the comment.
   *
   *@return maximum timestamp in comment
   */
  public long ts1() { return ts_max; }
}

