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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import racetrack.gui.RT;
import racetrack.gui.TimeMarker;
import racetrack.kb.EntityTag;
import racetrack.kb.RTComment;
import racetrack.util.CSVTokenConsumer;
import racetrack.util.Utils;
import racetrack.visualization.StatsOverlay;

/**
 * Simple class to parse a CSV file.
 *
 * @author  D. Trimm
 * @version 1.0
 */
class CSVParser implements CSVTokenConsumer {
  /**
   * First line flag - indicates that the next line will be a first
   */
  boolean               first_line   = true, 
  /**
   * Indicates that this data is a lookup (transform) table
   */
                        lookup_table = false; 
  /**
   * Overarching {@link Bundles} instance to load this data into
   */
  Bundles               bundles; 
  /**
   * Application framework (needed to {@link EntityTags} and {@link TimeMarkers})
   */
  RT                    rt;
  /**
   * Tablet to add the current bundle records to
   */
  Tablet                tablet; 
  /**
   * Set of {@link Bundle} that have been loaded so far by the reader
   */
  Set<Bundle>           set; 
  /**
   * Index (within file) of the timestamp (-1 indicates no timestamp)
   */
  int                   time_i     = -1; 
  /**
   * Index (within file) of the end timestamp (-1 indicates no timestamp end)
   */
  int                   time_end_i = -1; 
  /**
   * Lookup table for local to global (I think)
   */
  int                   lu[]; 
  /**
   * Lookup table to identify the bundle (record) index from the file (token) index
   */
  Map<Integer,Integer>  index_map = new HashMap<Integer,Integer>();
  /**
   * Header names from the csv file
   */
  String                hdr[];
  /**
   * {@link TimeMarker} loaded so far - needed to flush to the application
   * when a blank line/eof is found
   */
  List<TimeMarker> time_markers = null;
  /**
   * {@link EntityTag} loaded so far - needed to flush to the application
   * when a blank line/eof is found
   */
  List<EntityTag>  entity_tags  = null;
  /**
   * {@link RTComment} loaded so far - needed to flush to the application
   * when a blank line/eof is found
   */
  List<RTComment>  comments     = null;

  /**
   * Used for  lookup (transform) tables - index of the datatype
   */
  int          dt_lu_index    = -1;

  /**
   * Used for  lookup (transform) tables - datatype
   */
  BundlesDT.DT dt_lu_type     = null;

  /**
   * Used for  lookup (transform) tables - fields to convert
   */
  String       dt_lu_fields[] = null;

  /**
   * List of comment lines in the loaded files -- these lines will typically be application configuration
   * settings (e.g., windows up, positions, settings)
   */
  List<String> appconfs = new ArrayList<String>();

  /**
   * Constructor - just capture the inital variables.
   *
   * @param bundles global data structure
   * @param rt      application control
   * @param set     bundles (records) that get parsed
   */
  public CSVParser(Bundles bundles, RT rt, Set<Bundle> set) { this.bundles = bundles; this.rt = rt; this.set = set; }

  /**
   * Consume comment lines.  For this implmentation, comments will include information about the application state.
   * To provide them back to the application, they will be maintained in a list of comment lines.
   *
   *@param line comment line
   */
  public void commentLine(String line) { appconfs.add(line); }

  /**
   * Return the lines associated with application configuration.
   *
   *@return list of comment lines
   */
  public List<String> getAppConfigs() {
    return appconfs;
  }
  
  /**
   * Consume a line of tokens from the {@link CSVParser}.
   *
   * @param tokens  array of the tokens from the line
   * @param line    original line in file (helps with debugging)
   * @param line_no line number from file (helps to find problems with data)
   */
  public boolean consume(String tokens[], String line, int line_no) {
   // System.err.println("Consuming Line \"" + line + "\"");
   try {
    if (first_line)                { // Create a new tablet, assign the headers
      if        (tokens.length == 0)                      { return true; // Wait til we have something
      } else if (line.equals(TimeMarker.getFileHeader())) { time_markers = new ArrayList<TimeMarker>();
      } else if (line.equals(EntityTag.getFileHeader()))  { entity_tags  = new ArrayList<EntityTag>();
      } else if (line.equals(RTComment.getFileHeader()))  { comments     = new ArrayList<RTComment>();
      } else {
	// Figure out if it's a lookup table or just a regular tablet
	// - Lookup tables will have a column that matches (String-wise) a datatype
	int dt_header_count = 0;
	for (int i=0;i<tokens.length;i++) 
	  if (BundlesDT.parseDataType(tokens[i]) != null) {
	    dt_lu_index = i;
	    dt_lu_type  = BundlesDT.parseDataType(tokens[i]);
	    dt_header_count++;
          }
        // If there's exactly one datatype in the header, it's a lookup table
        if (dt_header_count == 1) {
          lookup_table = true;
	  dt_lu_fields = tokens;
	} else {
	  if (dt_header_count != 0) System.err.println("Table Header Has Multiple DataType Names!");
          // See if we need a new tablet or if we can match an existing tablet
          tablet = bundles.createTablet();
          /* Figure out the index mapping */
          lu = new int[tokens.length]; hdr = tokens;
          int j = 0;
          for (int i=0;i<tokens.length;i++) {
            if        (tokens[i].equals("beg") || tokens[i].equals("timestamp"))     { time_i     = i; tablet.setHasTimeStamps();
	    } else if (tokens[i].equals("end") || tokens[i].equals("timestamp_end")) { time_end_i = i; tablet.setHasTimeStamps(); tablet.setHasDurations();
	    } else {
	      // isAllUpper indicates a scalar field (integer)
              int fld_i = bundles.getGlobals().getOrCreateField(tokens[i], Utils.isAllUpper(tokens[i]));
	      lu[j] = fld_i;
              tablet.setFieldIndex(fld_i, j);
              index_map.put(i, j);
	      j++;
            }
          }
          /* Check to see if an identical tablet already exists, if so use that instead */
          Iterator<Tablet> it = bundles.tabletIterator(); boolean found_match = false;
          while (it.hasNext() && found_match == false) {
            Tablet compare = it.next();
            if (compare.equals(tablet)) { found_match = true; tablet = compare; }
          }

          /* Give some stats to user */
          System.err.println("Using Tablet \"" + tablet + "\" For Hdr " + line.hashCode());

          /* Otherwise, add the new tablet to the bundles */
          if (found_match == false) bundles.add(tablet); else {
            j = 0;
            for (int i=0;i<tokens.length;i++) {
              if        (tokens[i].equals("beg") || tokens[i].equals("timestamp"))     { time_i     = i; tablet.setHasTimeStamps();
	      } else if (tokens[i].equals("end") || tokens[i].equals("timestamp_end")) { time_end_i = i; tablet.setHasTimeStamps(); tablet.setHasDurations();
	      } else {
	        // isAllUpper indicates a scalar field (integer)
                int fld_i = bundles.getGlobals().getOrCreateField(tokens[i], Utils.isAllUpper(tokens[i]));
	        lu[j] = fld_i;
                index_map.put(i, (tablet.getFields())[fld_i]);
	        j++;
              }
            }
          }
        }
      }
      first_line = false;
    } else if (tokens.length == 0)   { 
      addListsToRT();
      first_line = true; time_i = -1; time_end_i = -1; tablet = null; lu = null; lookup_table = false;
      bundles.getGlobals().resetTransforms();
    } else if (entity_tags  != null) {
      EntityTag  entity_tag  = new EntityTag(line);   if (entity_tag.valid())  entity_tags.add(entity_tag);
    } else if (time_markers != null) {
      TimeMarker time_marker = new TimeMarker(line);  if (time_marker.valid()) time_markers.add(time_marker);
    } else if (comments     != null) {
      RTComment  comment     = new RTComment(line);   if (comment.valid())     comments.add(comment);
    } else if (lookup_table)         {
      // If the dt_lu_type is a IPv4CIDR, let's accept IPv4 and just append a /32
      if (dt_lu_type == BundlesDT.DT.IPv4CIDR && BundlesDT.getEntityDataType(tokens[dt_lu_index]) == BundlesDT.DT.IPv4) {
        tokens[dt_lu_index] += "/32";
      }
      // Make sure the token datatype matches the transform
      if (BundlesDT.getEntityDataType(tokens[dt_lu_index]) == dt_lu_type) {
        if (tokens.length == dt_lu_fields.length) {
          for (int i=0;i<tokens.length;i++) { 
	    if (i != dt_lu_index) bundles.getGlobals().addTransform(dt_lu_type, dt_lu_fields[i], tokens[dt_lu_index], tokens[i]);
          }
        } else System.err.println("Lookup Table Error @ \"" + line + "\" (Line " + line_no + ") - Incorrect Token Count");
      } else System.err.println("Lookup Table Error @ \"" + line + "\" (Line " + line_no + ") - DataType Mismatch");
    } else                           {
      String ts0_str = (time_i     == -1) ? null : tokens[time_i],
             ts1_str = (time_end_i == -1) ? null : tokens[time_end_i];

      Map<String,String> attr = new HashMap<String,String>(); int len = (hdr.length < tokens.length) ? hdr.length : tokens.length;
      for (int i=0;i<tokens.length;i++) {
        if (hdr[i].equals("beg") || hdr[i].equals("timestamp")) { } else if (hdr[i].equals("end") || hdr[i].equals("timestamp_end")) { } else { attr.put(hdr[i], tokens[i]); }
      }

      Bundle bundle;
      if      (ts0_str != null && ts1_str != null) bundle = tablet.addBundle(attr, ts0_str, ts1_str);
      else if (ts0_str != null)                    bundle = tablet.addBundle(attr, ts0_str);
      else if (ts1_str != null)                    bundle = tablet.addBundle(attr, ts1_str);
      else                                         bundle = tablet.addBundle(attr);
      if (bundle != null) set.add(bundle);

/*
      Bundle bundle = tablet.createBundle();

      // Allocate the strings 
      if      (time_end_i != -1) bundle.setStrs(new String[tokens.length-2]);
      else if (time_i     != -1) bundle.setStrs(new String[tokens.length-1]);
      else                       bundle.setStrs(new String[tokens.length-0]);
      // Check the header alignment
      if (tokens.length != hdr.length) {
        System.err.println("Header Length / Tokens Length Mismatch");
	for (int i=0;i<(tokens.length > hdr.length ? tokens.length : hdr.length);i++) {
	  if      (i < tokens.length && i < hdr.length) System.err.println(hdr[i] + " : " + tokens[i]);
	  else if (i < hdr.length)                      System.err.println(hdr[i] + " : " + null);
	  else                                          System.err.println(null   + " : " + tokens[i]);
	}
      }
      // Parse the fields
      int j = 0;
      for (int i=0;i<tokens.length;i++) {
        // System.err.println("tokens[" + i + "] = \"" + tokens[i] + "\"");
        if      (hdr[i].equals("beg") || hdr[i].equals("timestamp"))     bundle.setTS0(Utils.parseTimeStamp(tokens[i]));
	else if (hdr[i].equals("end") || hdr[i].equals("timestamp_end")) bundle.setTS1(Utils.parseTimeStamp(tokens[i]));
	else {
          // Normalize the tags field if appropriate
          if (hdr[i].equals(BundlesDT.TAGS)) tokens[i] = Utils.normalizeTag(tokens[i]);
          // Set the actual bundle
	  bundle.getStrs()[index_map.get(i)] = tokens[i];
          bundles.getGlobals().addFieldEntity(lu[j], tokens[i]);
	  j++;
	}
      }

      // Add duration entity lookups
      if (bundle.hasDuration()) {
        String blanks[] = KeyMaker.blanks(bundles.getGlobals(), false, false, false, true);
        for (int i=0;i<blanks.length;i++) {
          if (blanks[i].startsWith("|Dur|")) {
            KeyMaker km = new KeyMaker(tablet, blanks[i]);
            bundles.getGlobals().addFieldEntity(-1, (km.stringKeys(bundle))[0]);
          }
        }
      }

      // Finally, add the bundle to the application
      bundles.add(bundle); tablet.add(bundle); set.add(bundle);
*/
    }
   } catch (Throwable t) { System.err.println("Throwable: " + t + " @ Line No " + line_no); t.printStackTrace(System.err); }
   return true;
  }

  /**
   * Flush data elements to the main applications.  Elements flushed include 
   * {@link EntityTag}, {@link TimeMarker}, and {@link RTComment}.
   */
  public void addListsToRT() {
    if (rt           == null) return;
    if (entity_tags  != null) { rt.addEntityTags(entity_tags);   entity_tags  = null; }
    if (time_markers != null) { rt.addTimeMarkers(time_markers); time_markers = null; }
    if (comments     != null) { rt.addRTComments(comments);      comments     = null; }
  }
}
