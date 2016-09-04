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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;

import javax.imageio.ImageIO;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.kb.EntityTag;
import racetrack.kb.RTComment;
import racetrack.util.BloomFilter;
import racetrack.util.UpdateMonitor;
import racetrack.util.Utils;

public class RTStore {
  /**
   * Determine if the sqllite database is available.
   *
   *@return true if db is available
   */
  public static boolean sqlAvailable() {
    try { Class.forName("org.sqlite.JDBC");
          return true; } catch (ClassNotFoundException cnfe) { }
    return false;
  }

  //
  // ==============================================================================================
  // ==============================================================================================
  //
  // Time Markers To SQLLite
  //
  /**
   * Insert or update the time markers into the database.  Inserts occur
   * when the UUID is not found in the sqllite table.  Otherwise, the table
   * entry is updated with the new value.
   *
   *@param markers list of time markers to insert/update in the local database
   *
   *@return true if update/insert successful
   */
  static boolean updateTimeMarkers(Collection<TimeMarker> markers) {
    Connection connection = null; boolean result = false; long timer0, timer1;
    timer0 = System.currentTimeMillis();
    try {
      // Get the connection
      Class.forName("org.sqlite.JDBC"); boolean create_table = false;
      File      file       = RTDir.createOrGetRTFile("timemarkers.db"); 
      if (file.exists() == false) create_table = true;
      connection = DriverManager.getConnection("jdbc:sqlite:" + file.toString());
      // create the statement construct
      Statement statement  = connection.createStatement(); statement.setQueryTimeout(10);
      // Create the table if it doesn't exist
      if (create_table) { 
	// Create the table
        statement.executeUpdate("CREATE TABLE timemarkers (id          VARBINARY(16), " +
	                                                  "description STRING, "  +
							  "tstart      BIGINT, "  +
							  "tend        BIGINT, "  +
							  "tscreate    BIGINT, "  +
							  "source      STRING"    + ")"); 
	// Insert the existing markers
        Iterator<TimeMarker> it = markers.iterator();
	while (it.hasNext()) {
	  TimeMarker tm = it.next();
	  if (tm.isUpdated()) {
	    insert(statement, tm);
	    tm.clearUpdatedFlag();
          }
	}
	result = true;
      } else { // Otherwise, add/overwrite the existing data
        Iterator<TimeMarker> it = markers.iterator();
	while (it.hasNext()) {
          TimeMarker tm = it.next();
	  if (tm.isUpdated()) {
	    ResultSet rs = statement.executeQuery("SELECT * FROM timemarkers WHERE id='0x" + Utils.hexUUID(tm.getUUID()) + "'");
	    if (rs.next()) {
              if (rs.getString("description").equals(tm.getDescription()) == false    ||
	          rs.getLong("tstart") != tm.ts0() || rs.getLong("tend") != tm.ts1()) {
	        int rslt = statement.executeUpdate("DELETE FROM timemarkers WHERE id='0x" + Utils.hexUUID(tm.getUUID()) + "'");
	        System.err.println("  RTStore.updateTimeMarkers() : Deleting " + tm.getUUID() + " (Result = " + rslt + ")");
	        insert(statement, tm);
		tm.clearUpdatedFlag();
	      }
	      // Check for more...  shouldn't be there...
	      // if (rs.next()) { System.err.println("Multiple TimeMarker w/ Identical UUIDs: " + tm.getUUID()); }
	    } else         {
              insert(statement, tm);
	      tm.clearUpdatedFlag();
	    }
          }
	}
      }
      result = true;
    } catch (SQLException e) { System.err.println("SQLException: " + e);
                               e.printStackTrace(System.err);
    } finally                {
      try { if (connection != null) connection.close(); } catch (SQLException e) { }
      timer1 = System.currentTimeMillis();
      System.err.println("RTStore.updateTimeMarkers() : run time : " + Utils.humanReadableDuration(timer1 - timer0));
      return result; }
  }

  /**
   * Low-level insert activity for the time marker database.
   *
   *@param statement sql statement instance
   *@param tm        time marker to insert
   */
  private static void insert(Statement statement, TimeMarker tm) throws SQLException {
    System.err.println("  RTStore.insert(" + tm.getDescription() + " | " + tm.getUUID() + ")");
    statement.executeUpdate("INSERT INTO timemarkers values('0x" + Utils.hexUUID(tm.getUUID())          + "', " +
	                                                   "'"   + Utils.sqlString(tm.getDescription()) + "', " +
				                                 + tm.ts0()                             + ", " +
				                                 + tm.ts1()                             + ", " +
				                                 + tm.getCreateTime()                   + ", " +
				                           "'"   + Utils.sqlString(tm.getSource())      + "')");
  }
  
  /**
   * Delete the time markers from the database.  Search for the time markers
   * by their UUID and delete if they exist.
   *
   *@param  markers set of markers to delete
   *
   *@return true if successfully deleted
   */
  static boolean deleteTimeMarkers(Collection<TimeMarker> markers) {
    Connection connection = null; boolean result = false; long timer0, timer1;
    timer0 = System.currentTimeMillis();
    try {
      // Get the connection
      Class.forName("org.sqlite.JDBC");
      File      file       = RTDir.createOrGetRTFile("timemarkers.db"); 
      if (file.exists() == false) return true;
      connection = DriverManager.getConnection("jdbc:sqlite:" + file.toString());
      // create the statement construct
      Statement statement  = connection.createStatement(); statement.setQueryTimeout(10);
      // Go through each list and delete
      Iterator<TimeMarker> it = markers.iterator();
      while (it.hasNext()) {
        TimeMarker tm = it.next();
	int rslt = statement.executeUpdate("DELETE FROM timemarkers WHERE id='0x" + Utils.hexUUID(tm.getUUID()) + "'");
	System.err.println("  RTStore.deleteTimeMarkers() : Deleting " + tm.getUUID() + " (Result = " + rslt + ")");
      }
      result = true;
    } catch (SQLException e) { System.err.println("SQLException: " + e);
                               e.printStackTrace(System.err);
    } finally                {
      try { if (connection != null) connection.close(); } catch (SQLException e) { }
      timer1 = System.currentTimeMillis();
      System.err.println("RTStore.deleteTimeMarkers() : run time : " + Utils.humanReadableDuration(timer1 - timer0));
      return result; }
  }
  
  /**
   * Retrieve time markers that are within the specified time frame.
   *
   *@param  markers list to add the markers to
   *@param  ts0     minimum timestamp to add
   *@param  ts1     maximum timestamp to add
   *
   *@return true if successfully retrieved
   */
  static boolean retrieveRelatedTimeMarkers(List<TimeMarker> markers, long ts0, long ts1) {
    Connection connection = null; boolean result = false; long timer0, timer1;
    timer0 = System.currentTimeMillis();
    try {
      Class.forName("org.sqlite.JDBC"); boolean create_table = false;
      File      file       = RTDir.createOrGetRTFile("timemarkers.db"); 
      if (file.exists() == false) {
        System.err.println("retrieveRelatedTimeMarkers():  None exist in user directory");
        return true;
      }
                connection = DriverManager.getConnection("jdbc:sqlite:" + file.toString());
      Statement statement  = connection.createStatement(); statement.setQueryTimeout(10);
      ResultSet rs = statement.executeQuery("SELECT * FROM timemarkers WHERE " +
                                            "(tstart >= " + ts0 + " AND tstart <= " + ts1 + ") OR " +
					    "(tend   >= " + ts0 + " AND tend   <= " + ts1 + ") OR " +
					    "(tstart <  " + ts0 + " AND tend   >  " + ts1 + ")");
      // Re-encode the results
      while (rs.next()) {
        String     description = rs.getString("description"),
	           source      = rs.getString("source");
        long       tstart      = rs.getLong("tstart"),
	           tend        = rs.getLong("tend"),
	           tscreate    = rs.getLong("tscreate");
        String     uuid_str    = uuidString(rs.getBytes("id"));
	TimeMarker tm = new TimeMarker(uuid_str, description, tstart, tend, source, tscreate);
	markers.add(tm);
      }
      result = true;
    } catch (SQLException e) { System.err.println("SQLException: " + e);
                               e.printStackTrace(System.err);
    } catch (Throwable    t) { System.err.println("Throwable: " + t);
                               t.printStackTrace(System.err);
    } finally                {
      try { if (connection != null) connection.close(); } catch (SQLException e) { }
      timer1 = System.currentTimeMillis();
      System.err.println("RTStore.retrieveRelatedTimeMarkers() : run time : " + Utils.humanReadableDuration(timer1 - timer0));
      return result; }
  }

  //
  // ==============================================================================================
  // ==============================================================================================
  //
  // Entity Tags To SQLLite
  //
  /**
   * Insert or update the entity tags into the database.  UUIDs are used to
   * determine if the tag already exists in the database.
   *
   *@param  etags tags to insert/update in database
   *
   *@return true if insertion/update successful
   */
  static boolean updateEntityTags(Collection<EntityTag> etags) {
    Connection connection = null; boolean result = false; long timer0, timer1;
    timer0 = System.currentTimeMillis();
    try {
      // Get the connection
      Class.forName("org.sqlite.JDBC"); boolean create_table = false;
      File      file       = RTDir.createOrGetRTFile("entitytags.db"); 
      if (file.exists() == false) create_table = true;
      connection = DriverManager.getConnection("jdbc:sqlite:" + file.toString());
      // create the statement construct
      Statement statement  = connection.createStatement(); statement.setQueryTimeout(10);
      // Create the table if it doesn't exist
      if (create_table) { 
	// Create the table
        statement.executeUpdate("CREATE TABLE entitytags  (id          VARBINARY(16), " +
	                                                  "entity      STRING, "  +
	                                                  "tag         STRING, "  +
							  "tstart      BIGINT, "  +
							  "tend        BIGINT, "  +
							  "tscreate    BIGINT, "  +
							  "source      STRING"    + ")"); 
	// Insert the existing markers
        Iterator<EntityTag> it = etags.iterator();
	while (it.hasNext()) {
	  EntityTag etag = it.next();
	  if (etag.isUpdated()) {
	    insert(statement, etag);
	    etag.clearUpdatedFlag();
          }
	}
	result = true;
      } else { // Otherwise, add/overwrite the existing data
        Iterator<EntityTag> it = etags.iterator();
	while (it.hasNext()) {
          EntityTag etag = it.next();
	  if (etag.isUpdated()) {
	    ResultSet rs = statement.executeQuery("SELECT * FROM entitytags WHERE id='0x" + Utils.hexUUID(etag.getUUID()) + "'");
	    if (rs.next()) {
              if (rs.getString("entity").equals(etag.getEntity()) == false ||
	          rs.getString("tag").equals(etag.getTag())       == false ||
	          rs.getLong("tstart") != etag.ts0() || rs.getLong("tend") != etag.ts1()) {
	        int rslt = statement.executeUpdate("DELETE FROM entitytags WHERE id='0x" + Utils.hexUUID(etag.getUUID()) + "'");
	        System.err.println("  RTStore.updateEntityTags() : Deleting " + etag.getUUID() + " (Result = " + rslt + ")");
	        insert(statement, etag);
	        etag.clearUpdatedFlag();
	      }
	    } else         {
              insert(statement, etag);
	      etag.clearUpdatedFlag();
	    }
          }
	}
      }
      result = true;
    } catch (SQLException e) { System.err.println("SQLException: " + e);
                               e.printStackTrace(System.err);
    } finally                {
      try { if (connection != null) connection.close(); } catch (SQLException e) { }
      timer1 = System.currentTimeMillis();
      System.err.println("RTStore.updateEntityTags() : run time : " + Utils.humanReadableDuration(timer1 - timer0));
      return result; }
  }

  /**
   * Low-level insertion method for submitting tag into database.
   *
   *@param statement sql statement for local db
   *@param etag      entity tag to insert
   */
  private static void insert(Statement statement, EntityTag etag) throws SQLException {
    System.err.println("  RTStore.insert(" + etag.getEntity() + " | " + etag.getTag() + " | " + etag.getUUID() + ")");
    statement.executeUpdate("INSERT INTO entitytags values('0x" + Utils.hexUUID(etag.getUUID())     + "', " +
	                                                  "'"   + Utils.sqlString(etag.getEntity()) + "', " +
	                                                  "'"   + Utils.sqlString(etag.getTag())    + "', " +
				                                + etag.ts0()                        + ", " +
				                                + etag.ts1()                        + ", " +
				                                + etag.getCreateTime()              + ", " +
				                          "'"   + Utils.sqlString(etag.getSource()) + "')");
  }
  
  /**
   * Delete the entity tags from the database.  UUIDs are the primary key
   * for finding and removing the database rows.
   *
   *@param  etags entity tags to delete
   *
   *@return true if successfully deleted
   */
  static boolean deleteEntityTags(Collection<EntityTag> etags) {
    Connection connection = null; boolean result = false; long timer0, timer1;
    timer0 = System.currentTimeMillis();
    try {
      // Get the connection
      Class.forName("org.sqlite.JDBC");
      File      file       = RTDir.createOrGetRTFile("entitytags.db"); 
      if (file.exists() == false) return true;
      connection = DriverManager.getConnection("jdbc:sqlite:" + file.toString());
      // create the statement construct
      Statement statement  = connection.createStatement(); statement.setQueryTimeout(10);
      // Go through each list and delete
      Iterator<EntityTag> it = etags.iterator();
      while (it.hasNext()) {
        EntityTag etag = it.next();
	int rslt = statement.executeUpdate("DELETE FROM entitytags WHERE id='0x" + Utils.hexUUID(etag.getUUID()) + "'");
	System.err.println("  RTStore.deleteEntityTags() : Deleting " + etag.getUUID() + " (Result = " + rslt + ")");
      }
      result = true;
    } catch (SQLException e) { System.err.println("SQLException: " + e);
                               e.printStackTrace(System.err);
    } finally                {
      try { if (connection != null) connection.close(); } catch (SQLException e) { }
      timer1 = System.currentTimeMillis();
      System.err.println("RTStore.deleteEntityTags() : run time : " + Utils.humanReadableDuration(timer1 - timer0));
      return result; }
  }
  
  /**
   * Retrieve entity tags that are within the specified time frame.  Probably need
   * to limit retrieval to just those entities that exist.
   *
   *@param  etags list to add the loaded entity tags to
   *@param  ts0   minimum timestamp to consider
   *@param  ts1   maximum timestamp to consider
   *
   *@return true if entity tags successfully loaded
   *
   */
  static boolean retrieveRelatedEntityTags(List<EntityTag> etags, long ts0, long ts1) {
    Connection connection = null; boolean result = false; long timer0, timer1;
    timer0 = System.currentTimeMillis();
    try {
      Class.forName("org.sqlite.JDBC"); boolean create_table = false;
      File      file       = RTDir.createOrGetRTFile("entitytags.db"); 
      if (file.exists() == false) {
        System.err.println("retrieveRelatedEntityTags():  None exist in user directory");
        return true;
      }
                connection = DriverManager.getConnection("jdbc:sqlite:" + file.toString());
      Statement statement  = connection.createStatement(); statement.setQueryTimeout(10);
      ResultSet rs = statement.executeQuery("SELECT * FROM entitytags WHERE " +
                                            "(tstart >= " + ts0 + " AND tstart <= " + ts1 + ") OR " +
					    "(tend   >= " + ts0 + " AND tend   <= " + ts1 + ") OR " +
					    "(tstart <  " + ts0 + " AND tend   >  " + ts1 + ")");
      // Re-encode the results
      while (rs.next()) {
        String     entity      = rs.getString("entity"),
	           tag         = rs.getString("tag"),
	           source      = rs.getString("source");
        long       tstart      = rs.getLong("tstart"),
	           tend        = rs.getLong("tend"),
	           tscreate    = rs.getLong("tscreate");
        String     uuid_str    = uuidString(rs.getBytes("id"));
	EntityTag  etag        = new EntityTag(uuid_str, entity, tag, tstart, tend, source, tscreate);
	etags.add(etag);
      }
      result = true;
    } catch (SQLException e) { System.err.println("SQLException: " + e);
                               e.printStackTrace(System.err);
    } catch (Throwable    t) { System.err.println("Throwable: " + t);
                               t.printStackTrace(System.err);
    } finally                {
      try { if (connection != null) connection.close(); } catch (SQLException e) { }
      timer1 = System.currentTimeMillis();
      System.err.println("RTStore.retrieveRelatedEntityTags() : run time : " + Utils.humanReadableDuration(timer1 - timer0));
      return result; }
  }

  /**
   * Convert the byte representation of a UUID into a string representation
   * so that it may be used to create a UUID object.
   *
   *@param  chars characters to convert to a string representation of the UUID
   *
   *@return string rep of UUID
   */
  public static String uuidString(byte chars[]) {
    byte bytes[] = hexStrToBytes(chars, 16);
    StringBuffer sb = new StringBuffer();
    for (int i=0;i<bytes.length;i++) {
      String str = Integer.toString(bytes[i] & 0x00ff, 16); if (str.length() == 1) str = "0" + str;
      sb.append(str);
    }
    sb.insert(20, '-'); sb.insert(16, '-'); sb.insert(12, '-'); sb.insert(8,  '-'); // Put dashes in correct places
    return sb.toString();
  }

  /**
   * Convert a sequence of hex characters to a byte array.
   *
   *@param chars      hex characters to convert
   *@param return_len expected return length
   *
   *@param byte representation of hex characters
   */
  public static byte[] hexStrToBytes(byte chars[], int return_len) {
    // Adjust if string started with 0x
    int start = 0; if (chars.length >= 2 && chars[0] == '0' && chars[1] == 'x') start = 2;
    // make sure the string has the right number of characters
    StringBuffer sb = new StringBuffer();
    for (int i=0;i<2*return_len-(chars.length-start);i++) sb.append('0');
    for (int i=start;i<chars.length;i++)                  sb.append((char) (chars[i] & 0x00ff));
    // Check for string too long
    if (sb.length() > 2*return_len) {
      System.err.println("hexStrToBytes() - too long: \"" + sb.toString() + "\"");
    }
    // Convert
    byte ret[] = new byte[return_len];
    for (int i=0;i<ret.length;i++) {
      ret[i] = (byte) Integer.parseInt(sb.substring(i*2,i*2+2),16);
    }
    return ret;
  }

  /**
   * Add a note to the notation directory.  Include the appropriate information as
   * specified by the parameters.
   *
   *@param rt                 application handle
   *@param description        overall description of the note 
   *@param note               the note's text
   *@param save_visible_data  flag indicating that visible bundles should be saved
   *@param save_layout_info   flat indicating that link-node layouts should be stored
   */
  public static void addNote(RT      rt,
                             String  description,
                             String  note,
                             boolean save_visible_data,
                             boolean save_layout_info) throws IOException {
    //
    // Create the notation directory if it is null
    //
    if (notation_dir == null) {
      File file = new File(Utils.fileDateStr() + "_rtnotes");
      while (file.exists()) {
        try { Thread.sleep(1000); } catch (InterruptedException ie) { }
        file = new File(notation_dir, Utils.fileDateStr() + "_notes");
      }
      file.mkdir();
      notation_dir = file;
    }

    //
    // Create the director for this note itself
    // - Updated on 2015-11-06 so that the directory name reflects the note name.
    //
    int file_count = notation_dir.listFiles().length; 
    String file_count_str = "" + file_count; while (file_count_str.length() < 4) file_count_str = "0" + file_count_str;
    String this_note_dir_str = file_count_str + "_" + Utils.makeSafeForFilename(description);
    if (this_note_dir_str.length() > 40) this_note_dir_str = this_note_dir_str.substring(0,40);
    File this_note_dir = new File(notation_dir, this_note_dir_str); int safety = 10;

    while (this_note_dir.exists() && safety > 0) {
      safety--; file_count++;
      file_count_str = "" + file_count; while (file_count_str.length() < 4) file_count_str = "0" + file_count_str;
      this_note_dir_str = file_count_str + "_" + Utils.makeSafeForFilename(description);
      if (this_note_dir_str.length() > 40) this_note_dir_str = this_note_dir_str.substring(0,40);
      this_note_dir = new File(notation_dir, this_note_dir_str);
    }
    if (this_note_dir.exists()) this_note_dir = new File(notation_dir, this_note_dir_str = Utils.fileDateStr() + "_note");
    this_note_dir.mkdir();

    Map<String,BufferedImage> image_map = new HashMap<String,BufferedImage>();

    //
    // First, save off the screen shots
    //
    Iterator<RTPanel> it = rt.rtPanelIterator();
    while (it.hasNext()) {
      RTPanel panel = it.next();
      // Make a unique prefix for this panel
      String prefix_base = panel.getPrefix();
      String prefix      = prefix_base;
      File   file        = new File(this_note_dir, prefix + ".png");
      int    file_no      = 1;
      while (file.exists()) { 
        prefix = prefix_base + "_" + file_no;
        file = new File(this_note_dir, prefix + ".png"); 
	file_no++; 
      }
      // Save off the image
      RTPanel.RTComponent.RTRenderContext rc = panel.getRTComponent().getRTRenderContext();
      if (rc != null) { BufferedImage bi = rc.getBase(); if (bi != null) {
        Graphics2D g2d = (Graphics2D) bi.getGraphics(); panel.getRTComponent().paintComponent(g2d); g2d.dispose();
        ImageIO.write(bi, "PNG", new FileOutputStream(new File(this_note_dir, prefix + ".png")));
	image_map.put(prefix + ".png", bi);
      } }
      // If appropriate, save out the layout information
      if (panel instanceof RTGraphPanel && save_layout_info) {
        ((RTGraphPanel) panel).saveLayout(false, new File(this_note_dir, prefix + ".layout"));
      }
    }

    //
    // Next, save off the visible data (if selected)
    //
    Bundles bs = rt.getVisibleBundles();
    if (save_visible_data) {
      rt.getControlPanel().saveFile(bs, new File(this_note_dir, "visible"), true);
    }

    //
    // Lastly, write-out the note as an html file linking in the images/data
    //
    File index_file = new File(notation_dir, "index.html"); PrintStream out;
    if (index_file.exists()) { 
      out = new PrintStream(new FileOutputStream(index_file, true));
    } else {
      out = new PrintStream(new FileOutputStream(index_file, true));
      out.println("<html> <head> <title> RACETrack Visual Notation Log </title> </head> <body>");
      out.println("<h1> RACETrack Visual Notation Log </h1>");
      out.println("");
    }

      out.println("<h2> " + description + " </h2> <br> ");
      out.println("Number of Records: " + bs.size() + " <br>");
      out.println("<pre>");
      out.println("=== %< === %< === %< ==="); out.println("");
      out.println(note); out.println("");
      out.println("=== %< === %< === %< ===");
      out.println("</pre>");
      Iterator<String> it_str = image_map.keySet().iterator();
      while (it_str.hasNext()) {
        String str = it_str.next();  BufferedImage bi = image_map.get(str); int w,h; double ratio;
	if (bi.getWidth() > bi.getHeight()) { ratio = ((double) bi.getHeight())/bi.getWidth();  w = 200; h = (int) (200*ratio); }
	else                                { ratio = ((double) bi.getWidth()) /bi.getHeight(); h = 200; w = (int) (200*ratio); }
	out.println("<a href=\"" + this_note_dir_str + "/" + str + "\"> <img src=\"" + this_note_dir_str + "/" + str + "\" width=\"" + w + "\" height=\"" + h + "\" border=\"0\"/> </a>");
      }
      out.println(""); out.println("");

    out.close();

    //
    // Really lastly, update the index file for the entire visualization session
    //

  }

  /**
   *
   */
  private static File notation_dir = null;

  /**
   *
   */

  /**
   * Test stub for testing the conversion routines.
   *
   *@param args characters for uuid string
   */
  public static void main(String args[]) {
    byte chars[]  = new byte[args[0].length()];
    for (int i=0;i<chars.length;i++) chars[i] = (byte) (0x00ff & args[0].charAt(i));

    UUID random = UUID.randomUUID();
    System.err.println("random   = " + random);
    String uuid_str = uuidString(chars);
    System.err.println("uuid_str = " + uuid_str);

    UUID from_str = UUID.fromString(uuid_str);
    System.err.println("uuid     = " + from_str);

  }

  //
  // ==============================================================================================
  // ==============================================================================================
  //
  // On initialization, gather up the bloom filters and keep track of which files they relate to
  //

  /**
   * Empty holder to maintain information about which bundles are in
   * which files.  Not implemented.
   */
  static Map<File,BloomFilter> file_to_bloom = new HashMap<File,BloomFilter>();
  static {
  }
  //
  // store();
  // - Put a set of bundles to local disk
  //     needs to be stored in the rt directory
  // - format
  //     bloom filter size                     // long
  //     bloom filter                          // bit array stored as bytes
  //     number of UUIDs                       // long
  //     list of UUID/file-offset-to-bundles   // 2xlong x long
  //     bundles                               // stored in toString rep... there's probably a better way but not
  //                                           //                           one that has deterministic file offsets (i.e.
  //                                           //                           would need to have state throughout file...)

  /**
   * Empty method to store bundles to disk.  Not implemented.
   *
   *@param bundles bundles to store
   */
  public static void        storeBundles(Collection<Bundle> bundles) {
  }

  /**
   * Empty method to store bundles to disk.  Not implemented.
   *
   *@param bundles bundles to store
   */
  public static void        storeBundles(Bundles            bundles) {
  }

  /**
   * Empty method to load bundles (records) from local storage.  Not implemented.
   *
   *@param  bundle_uuid    bundle to load
   *@param  bundles        data to put the loaded bundle into
   *@param  update_monitor progress dialog
   *
   *@return loaded bundle
   */
  public static Bundle      retrieveBundle(UUID bundle_uuid,    Bundles bundles, UpdateMonitor update_monitor) {
    UUID as_array[] = new UUID[1]; as_array[0] = bundle_uuid;
    Set<Bundle> set = retrieveBundles(as_array, bundles, update_monitor);
    return (set == null || set.size() == 0) ? null : set.iterator().next();
  }

  /**
   * Empty method to load bundles (records) from local storage.  Not implemented.
   *
   *@param  bundle_uuids   uuids for the needed bundles
   *@param  bundles        dataset to put the loaded bundles into
   *@param  update_monitor progress dialog
   *
   *@return loaded bundles
   */
  public static Set<Bundle> retrieveBundles(UUID bundle_uuids[], Bundles bundles, UpdateMonitor update_monitor) {
    System.err.println("RTStore.retrieve() - not implemented");
    return new HashSet<Bundle>();
  }

  /**
   * Empty method to load bundles (records) from local storage.  Not implemented.
   *
   *@param  bundle_uuids   uuids for the needed bundles
   *@param  bundles        dataset to put the loaded bundles into
   *@param  update_monitor progress dialog
   *
   *@return loaded bundles
   */
  public static Set<Bundle> retrieveBundles(Collection<UUID> bundle_uuids, Bundles bundles, UpdateMonitor update_monitor) {
    Iterator<UUID> it = bundle_uuids.iterator(); UUID as_array[] = new UUID[bundle_uuids.size()];
    for (int i=0;i<as_array.length;i++) as_array[i] = it.next();
    return retrieveBundles(as_array, bundles, update_monitor);
  }
}

