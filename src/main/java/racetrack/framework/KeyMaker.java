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

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import racetrack.util.Utils;

/**
 * This class is responsible for abstracting away the actual data fields
 * from the GUI components.  It provides "blanks" for keying into the
 * data structure.  The blanks can then be used to instantiate a keymaker
 * that works for a specific {@link Tablet}.  
 *
 * @author  D. Trimm
 * @version 1.0
 */
public class KeyMaker {
  /**
   * Return the blanks associated with the data that's been loaded.
   *
   * @param  globals global data structure
   * @return         array of strings representing different keys for the data
   */
  public  static String[] blanks      (BundlesG globals)    { return blanks(globals, false, true,  true);  }

  /**
   * Return the blanks associated with the data that's been loaded that are entity-based (categorical, not scalar).
   *
   * @param  globals global data structure
   * @return         array of strings representing different keys for the data
   */
  public  static String[] entityBlanks(BundlesG globals)    { return blanks(globals, false, true,  false); }

  /**
   * Return the blanks associated with the data that's been loaded that are scalar-based (not categorical).
   *
   * @param  globals global data structure
   * @return         array of strings representing different keys for the data
   */
  public  static String[] scalarBlanks(BundlesG globals)    { return blanks(globals, false, false, true);  }

  /**
   * Return the blanks associated with the data that's been loaded.
   *
   * @param  globals     global data structure
   * @param  inc_bundles include special string to indicate the records themselves
   *                     useful for "countyby" records
   * @return             array of strings representing different keys for the data
   */
  public  static String[] blanks      (BundlesG globals, 
                                       boolean inc_bundles) { return blanks(globals, true,  true,  true);  }

  /**
   * Return the blanks associated with the data that's been loaded.
   *
   * @param  globals      global data structure
   * @param  inc_bundles  include special string to indicate the records themselves
   *                      useful for "countyby" records
   * @param  inc_entities include categorical keys
   * @param  inc_scalars  include scalar keys
   * @return              array of strings representing different keys for the data
   */
  public  static String[] blanks      (BundlesG globals, 
                                       boolean inc_bundles, 
				       boolean inc_entities, 
				       boolean inc_scalars) {
    return blanks(globals, inc_bundles, inc_entities, inc_scalars, false);
  }

  /**
   * Return the blanks associated with the data that's been loaded.
   *
   * @param  globals      global data structure
   * @param  inc_bundles  include special string to indicate the records themselves
   *                      useful for "countyby" records
   * @param  inc_entities include categorical keys
   * @param  inc_scalars  include scalar keys
   * @param  inc_times    include time keys
   * @return              array of strings representing different keys for the data
   */
  public  static String[] blanks      (BundlesG globals, 
                                       boolean inc_bundles, 
				       boolean inc_entities, 
				       boolean inc_scalars,
				       boolean inc_times) {
    List<String>              al        = new ArrayList<String>();
    Map<BundlesDT.DT,Integer> dts_count = new HashMap<BundlesDT.DT,Integer>(); // Counts the number of fields for datatype key
    // Add time-based versions
    // if (inc_entities) { al.add(yyyymmdd_str); }
    // Figure out what to add to the blanks list...
    Iterator<String> it = globals.fieldIterator();
    while (it.hasNext()) {
      String hdr = it.next(); 
      if      (inc_entities && inc_scalars) { } 
      else if (inc_entities && globals.isScalar(globals.fieldIndex(hdr)) == true)  continue;
      else if (inc_scalars  && globals.isScalar(globals.fieldIndex(hdr)) == false) continue;

      al.add(hdr);
      Iterator<BundlesDT.DT> it_dt = BundlesDT.dataTypesIterator();
      while (it_dt.hasNext()) {
        BundlesDT.DT datatype = it_dt.next();
	// if (globals.getFieldDataType(globals.fieldIndex(hdr)) == datatype) { // This line limits transforms to exactly one data type in a field
	if (globals.getFieldDataTypes(globals.fieldIndex(hdr)).contains(datatype)) { // This line opens up the transforms as long as a field has at least one of those datatypes (testing)
	  if (datatype == BundlesDT.DT.INTEGER && globals.isScalar(globals.fieldIndex(hdr)) == false) continue; // Don't add conversions for non-scalar integer fields
	  String appends[] = BundlesDT.dataTypeVariations(datatype, globals);
	  for (int i=0;i<appends.length;i++) al.add(hdr + BundlesDT.DELIM + appends[i]);
	  if (dts_count.containsKey(datatype) == false) dts_count.put(datatype,0);
	  dts_count.put(datatype,dts_count.get(datatype)+1);
	}
      }
    }
    // Add the multis...
    Iterator<BundlesDT.DT> it_dt = dts_count.keySet().iterator();
    while (it_dt.hasNext()) {
      BundlesDT.DT datatype = it_dt.next();
      if (dts_count.get(datatype) <= 1) continue; // Only allow multis where more than one field is viable
      String strs[] = BundlesDT.getMultis(datatype, globals);
      for (int i=0;i<strs.length;i++) al.add(strs[i]);
    }
    // If the tag field is present, add those
    if (globals.fieldIndex(BundlesDT.TAGS) >= 0) al.add(BundlesDT.TAGS + BundlesDT.MULTI);
    Iterator<String> it_str = globals.tagTypeIterator();
    while (it_str.hasNext()) al.add(BundlesDT.TAGS + BundlesDT.MULTI + BundlesDT.DELIM + it_str.next());
    // Convert it to a string array
    String strs[] = new String[al.size()];
    for (int i=0;i<strs.length;i++) strs[i] = al.get(i);
    // Sort it - put the post processors after the regulars - sort them alphabetically...
    Arrays.sort(strs, new Comparator<String>() {
      public int     compare(String s1, String s2) { 
        if ((s1.indexOf(BundlesDT.DELIM) >= 0 && s2.indexOf(BundlesDT.DELIM) >= 0) ||
            (s1.indexOf(BundlesDT.DELIM) <  0 && s2.indexOf(BundlesDT.DELIM) <  0)) {
          return s1.toLowerCase().compareTo(s2.toLowerCase());
        } else if (s1.indexOf(BundlesDT.DELIM) >= 0) { return  1;
	} else                                       { return -1; }
      }
      public boolean equals(Object obj)            { return false; } } );
    // Include time-based counters
    if (inc_times) {
      String new_strs[] = new String[strs.length + BY_TIME_STRS.length];
      System.arraycopy(strs, 0, new_strs, 0, strs.length);
      System.arraycopy(BY_TIME_STRS, 0, new_strs, strs.length, BY_TIME_STRS.length);
      strs = new_strs;
    }

    // Include the bundles counter
    if (inc_bundles) {
      String new_strs[] = new String[strs.length + 1];
      new_strs[0] = RECORD_COUNT_STR;
      System.arraycopy(strs, 0, new_strs, 1, strs.length);
      strs = new_strs;
    }

    // Include the tablet counter / categorical separator
    if (inc_entities) {
      // Want it to be the second option after the RECORD_COUNT_STR... or the first option overall
      if        (strs.length == 0) {
      } else if (strs[0].equals(RECORD_COUNT_STR)) {
        String new_strs[] = new String[strs.length + 2];
        new_strs[0] = RECORD_COUNT_STR;
	new_strs[1] = TABLET_SEP_STR;
        new_strs[2] = ALL_ENTITIES_STR;
        System.arraycopy(strs, 1, new_strs, 3, strs.length-1);
        strs = new_strs;
      } else {
        String new_strs[] = new String[strs.length + 2];
        new_strs[0] = TABLET_SEP_STR;
        new_strs[1] = ALL_ENTITIES_STR;
        System.arraycopy(strs, 0, new_strs, 2, strs.length);
        strs = new_strs;
      }
    }

    return strs;
  }

  /**
   * String for record counting
   */
  public static final String RECORD_COUNT_STR               = BundlesDT.COUNT_BY_BUNS;

  /**
   * String for tablet separation (and counting)
   */
  public static final String TABLET_SEP_STR                 = "|tablet|";

  /**
   * String for all entities
   */
  public static final String ALL_ENTITIES_STR               = "|all ents|"; 

  /**
   * Strings for time-based keys
   */
  public static final String BY_SECOND_STR                      = "|Tm|Sec|",
                             BY_MILLIS_STR                      = "|Tm|Millis|",
                             BY_MINUTE_SECOND_STR               = "|Tm|Min/Sec|",
                             BY_MINUTE_STR                      = "|Tm|Min|",
                             BY_HOUR_MINUTE_STR                 = "|Tm|Hr/Min|",
                             BY_HOUR_STR                        = "|Tm|Hr|",
                             BY_DAYOFWEEK_HOUR_STR              = "|Tm|DofW/Hr|",
                             BY_DAYOFWEEK_STR                   = "|Tm|DofW|",
                             BY_MONTH_DAY_STR                   = "|Tm|Mo/Day|",
                             BY_MONTH_STR                       = "|Tm|Mo|",
                             BY_YEAR_MONTH_DAY_HOUR_MIN_SEC_STR = "|Tm|Yr/Mo/Day/Hour/Min/Sec|", 
                             BY_YEAR_MONTH_DAY_HOUR_MIN_STR     = "|Tm|Yr/Mo/Day/Hour/Min|", 
                             BY_YEAR_MONTH_DAY_HOUR_STR         = "|Tm|Yr/Mo/Day/Hour|",
                             BY_YEAR_MONTH_DAY_STR              = "|Tm|Yr/Mo/Day|",
                             BY_YEAR_MONTH_STR                  = "|Tm|Yr/Mo|",
                             BY_YEAR_STR                        = "|Tm|Yr|",
                             BY_STRAIGHT_STR                    = "|Tm|Straight|",
			     BY_AUTOTIME_STR                    = "|Tm|Auto|",
                             BY_DUR_SEC_STR                     = "|Dur|Sec|",
                             BY_DUR_HOUR_STR                    = "|Dur|Hr|";

  public static final String BY_TIME_STRS[] = { 
    BY_AUTOTIME_STR,
    BY_STRAIGHT_STR,
    BY_YEAR_STR,
    BY_YEAR_MONTH_STR, 
    BY_YEAR_MONTH_DAY_STR, 
    BY_YEAR_MONTH_DAY_HOUR_STR, 
    BY_YEAR_MONTH_DAY_HOUR_MIN_STR, 
    // BY_YEAR_MONTH_DAY_HOUR_MIN_SEC_STR,
    BY_MONTH_STR, 
    BY_MONTH_DAY_STR,
    BY_DAYOFWEEK_STR, 
    BY_DAYOFWEEK_HOUR_STR, 
    BY_HOUR_STR, 
    BY_HOUR_MINUTE_STR,
    BY_MINUTE_STR, 
    BY_MINUTE_SECOND_STR, 
    BY_SECOND_STR,
    BY_MILLIS_STR,
    BY_DUR_SEC_STR,
    BY_DUR_HOUR_STR
  };

  /**
   * Determine if a tablet can be used to complete a blank.
   *
   * @param  tablet tablet to check against
   * @param  blank  string describing the blank needed to complete
   * @return        true if the tablet can be used to create keys for this blank
   */
  public static boolean  tabletCompletesBlank(Tablet tablet, String blank) { 
    if        (blank.equals(RECORD_COUNT_STR) ||
               blank.equals(TABLET_SEP_STR)   ||
               blank.equals(ALL_ENTITIES_STR))      {
      return true;
    } else if (blank.startsWith("|Tm|"))            {
      return tablet.hasTimeStamps();
    } else if (blank.startsWith("|Dur|"))           {
      return tablet.hasDurations();
    } else if (blank.startsWith(BundlesDT.TAGS))    {
      return tablet.hasField(tablet.getBundles().getGlobals().fieldIndex(BundlesDT.TAGS));
    } else if (blank.indexOf(BundlesDT.MULTI) >= 0) {
      BundlesDT.DT datatype = BundlesDT.parseDataType(blank.substring(0,blank.indexOf(BundlesDT.DELIM)));
      BundlesG     globals  = tablet.getBundles().getGlobals();
      Iterator<String> it = globals.fieldIterator();
      while (it.hasNext()) { String fld = it.next(); int fld_i = globals.fieldIndex(fld);
                             if (tablet.hasField(fld_i) && globals.getFieldDataTypes(fld_i).contains(datatype)) return true; }
      return false;
    } else if (blank.indexOf(BundlesDT.DELIM) >= 0) {
      return tablet.hasField(tablet.getBundles().getGlobals().fieldIndex(blank.substring(0,blank.indexOf(BundlesDT.DELIM))));
    } else {
      return tablet.hasField(tablet.getBundles().getGlobals().fieldIndex(blank));
    }
  }

  /**
   * Interface that converts a bundle (record) into the specified keys based
   * on the blank created.
   */
  interface Maker { 
    /**
     * Create the keys for a bundle/record.
     *
     * @param  bundle record (bundle) to create
     * @return        array of the strings once they've been created
     */
    public String[] stringKeys(Bundle bundle); 
  }

  /**
   * Very simple maker to count bundles.
   */
  class RecordCountMaker implements Maker {
    public String[] stringKeys(Bundle bundle) { 
      String[] strs = new String[1];
      strs[0] = "" + ((Object) bundle);
      return strs; 
    } 
  }

  /**
   * Mapping for tablet headers to integers
   */
  private static Map<String,Integer> tablethdr_to_i = new HashMap<String,Integer>();

  /**
   * Simple maker that just returns the tablet header.
   */
  class TabletSepMaker implements Maker {
    Tablet tablet; String hdr; int hdr_i;
    public TabletSepMaker      (Tablet tablet) { this.tablet = tablet; 
                                                 this.hdr    = tablet.fileHeader(); 
						 if (tablethdr_to_i.containsKey(this.hdr) == false) tablethdr_to_i.put(this.hdr, tablethdr_to_i.keySet().size()+1); 
						 this.hdr_i  = tablethdr_to_i.get(this.hdr); 
                                                 try { tablet.getBundles().getGlobals().toInt(this.hdr); } catch (NullPointerException npe) {
                                                   tablet.getBundles().getGlobals().overrideEntityIndex(this.hdr, this.hdr_i); }
                                               }
    public String[] stringKeys (Bundle bundle) { String strs[] = new String[1]; strs[0] = hdr;   return strs; }
    public int[]    intKeys    (Bundle bundle) { int    is[]   = new int[1];    is[0]   = hdr_i; return is; }
  }

  /**
   * Simple maker that breaks a bundle into all of its entities
   */
  class AllEntitiesMaker implements Maker {
    Tablet tablet; List<Integer> fld_is = new ArrayList<Integer>();
    public          AllEntitiesMaker(Tablet tablet) { 
      this.tablet = tablet; BundlesG globals = tablet.getBundles().getGlobals();
      int flds[] = tablet.getFields(); 
      for (int fld_i=0;fld_i<flds.length;fld_i++) {
        if (flds[fld_i] >= 0 && globals.isScalar(fld_i) == false) fld_is.add(fld_i);
      }
    }
    public String[] stringKeys(Bundle bundle) {
      String results[] = new String[fld_is.size()];
      for (int i=0;i<results.length;i++) results[i] = bundle.toString(fld_is.get(i));
      return results;
    }
    public int[]    intKeys   (Bundle bundle) {
      int results[] = new int[fld_is.size()];
      for (int i=0;i<results.length;i++) results[i] = bundle.toValue(fld_is.get(i));
      return results;
    }
  }

  /**
   * Simple {@link Maker} based on a straight field in the data.
   */
  class SimpleMaker implements Maker {
    /**
     * Global field index
     */
    int fld_i;

    /**
     * Constructor
     *
     * @param fld string-representation of the field
     */
    public SimpleMaker(String fld)            { fld_i = tablet.getBundles().getGlobals().fieldIndex(fld); }
    public String[] stringKeys(Bundle bundle) { String strs[] = new String[1]; strs[0] = bundle.toString(fld_i); return strs; }
  }

  /**
   * Simple {@link Maker} that incorporates a post processor {@link PostProc}.
   */
  class SimplePostMaker implements Maker {
    /**
     * Simple maker used to convert the keys as a first stage
     */
    SimpleMaker simple_maker; 

    /**
     * The post process to use as a second stage
     */
    PostProc    post_proc;

    /**
     * Flag to indicate that additional type checking is needed before post processing
     */
    boolean     needs_check = false;

    /**
     * Constructor
     *
     * @param fld  global field name for simple conversion
     * @param post post process to convert the intermediates to finals
     */
    public SimplePostMaker(String fld, String post) { 
      simple_maker = new SimpleMaker(fld);
      post_proc    = BundlesDT.createPostProcessor(post, tablet.getBundles().getGlobals()); 
      int fld_i = tablet.getBundles().getGlobals().fieldIndex(fld);
      if (tablet.getBundles().getGlobals().getFieldDataType(fld_i) == null) needs_check = true;
    }

    /**
     * 
     */
    public String[] stringKeys(Bundle bundle) {
      String strs[] = simple_maker.stringKeys(bundle); List<String> al = new ArrayList<String>();
      for (int i=0;i<strs.length;i++) {
        if (needs_check && BundlesDT.getEntityDataType(strs[i]) != post_proc.type()) {
          al.add(BundlesDT.NOTSET);
        } else {
          String post[] = post_proc.postProcess(strs[i]);
	  for (int j=0;j<post.length;j++) al.add(post[j]);
        }
      }
      strs = new String[al.size()]; for (int i=0;i<al.size();i++) strs[i] = al.get(i);
      return strs;
    }
  }

  /**
   * Maker that converts a tag field into its components.
   */
  class MultiTagMaker implements Maker {
    /**
     * Global field index of tag field
     */
    int fld_i;

    /**
     * Constructor
     */
    public MultiTagMaker()                    { fld_i = tablet.getBundles().getGlobals().fieldIndex(BundlesDT.TAGS); }
    public String[] stringKeys(Bundle bundle) { 
      List<String> al = Utils.tokenizeTags(bundle.toString(fld_i));
      // Breakdown the hierarhical tags
      int i, sz=al.size();
      for (i=0;i<sz;i++) {
        if (Utils.tagIsHierarchical(al.get(i))) {
          String decomposed[] = Utils.tagDecomposeHierarchical(al.get(i));
          for (int j=0;j<decomposed.length;j++) {
	    if (decomposed[j].equals(al.get(i)) == false) {
	      al.add(decomposed[j]);
	    }
	  }
        }
      }
      // Convert to an array
      String strs[] = new String[al.size()]; 
      for (i=0;i<strs.length;i++) strs[i] = al.get(i);
      return strs;
    }
  }

  /**
   * Maker that finds specific tag type-value pairs and returns only those
   * that match the specified type string.
   */
  class TypeTagMaker implements Maker {
    /**
     * Global field index
     */
    int fld_i; 

    /**
     * Specific type field to use to match type-value tags
     */
    String type;

    /**
     * Constructor
     *
     * @param type specific type field to use to match type-value tags
     */
    public TypeTagMaker(String type)          { fld_i = tablet.getBundles().getGlobals().fieldIndex(BundlesDT.TAGS); this.type = type; }
    public String[] stringKeys(Bundle bundle) { 
      List<String> al = Utils.tokenizeTags(bundle.toString(fld_i)), al2 = new ArrayList<String>();
      for (int i=0;i<al.size();i++) {
        if (Utils.tagIsTypeValue(al.get(i))) {
          // System.err.println("type=\"" + type + "\" - Tag = \"" + al.get(i) + "\"");
	  String sep[] = Utils.separateTypeValueTag(al.get(i));
	  if (sep[0].equals(type)) {
            // System.err.println("  Type Matches");
	    al2.add(sep[1]);
          }
	}
      }
      String strs[] = new String[al2.size()]; 
      for (int i=0;i<strs.length;i++) strs[i] = al2.get(i);
      return strs;
    }
  }

  /**
   * Specialty {@link Maker} that finds all fields of
   * a specific type then uses all to make keys.  Useful when
   * you don't want to distinguish between srcip and dstip
   * for example.
   */
  class MultiMaker implements Maker {
    /**
     * Specific datatype to search for across global fields.
     */
    BundlesDT.DT datatype; 

    /**
     * Global field indices with that type.
     */
    int          fld_is[]; 

    /**
     * Determines if the data needs to be double checked for
     * each field.  For example, if the data contains notset values.
     */
    boolean      needs_check[]; 

    /**
     * Determines if any checks are needed to speed up the 
     * performance.
     */
    boolean      checks_needed = false;

    /**
     * Constructor
     *
     * @param dt_str string representation of the datatype for this multimaker.
     */
    public MultiMaker(String dt_str) {
      BundlesG         globals  = tablet.getBundles().getGlobals();
                       datatype = BundlesDT.parseDataType(dt_str);
      // Allocate expandable storage
      List<Integer> ints   = new ArrayList<Integer>();
      List<Boolean> checks = new ArrayList<Boolean>();
      // Figure out which fields match and which need checks
      Iterator<String> it       = globals.fieldIterator();
      while (it.hasNext()) {
        String fld = it.next(); int fld_i = globals.fieldIndex(fld);
	if (tablet.hasField(fld_i) && globals.getFieldDataTypes(fld_i).contains(datatype)) {
          ints.add(fld_i);
	  if (globals.getFieldDataTypes(fld_i).size() > 1) { checks.add(true);  checks_needed = true; }
	  else                                             { checks.add(false); }
	}
      }
      // Convert to arrays for speed
      fld_is = new int[ints.size()]; needs_check = new boolean[checks.size()];
      for (int i=0;i<fld_is.length;i++) { fld_is[i] = ints.get(i); needs_check[i] = checks.get(i); }
    }
    public String[] stringKeys(Bundle bundle) {
      // Optimize the case where no checks are needed
      if (checks_needed == false) {
        String strs[] = new String[fld_is.length];
	for (int i=0;i<strs.length;i++) strs[i] = bundle.toString(fld_is[i]);
	return strs;
      } else             {
        List<String> al = new ArrayList<String>();
	for (int i=0;i<fld_is.length;i++) {
          String str = bundle.toString(fld_is[i]);
	  if (needs_check[i] && BundlesDT.stringIsType(str,datatype)) {
	    al.add(str);
          } else if (needs_check[i] == false) al.add(str);
	}
	// Convert to an array
	String strs[] = new String[al.size()];
	for (int i=0;i<strs.length;i++) strs[i] = al.get(i);
	return strs;
      }
    }
  }

  /**
   * A multi-maker {@link MultiMaker} that incorporates a post processor
   */
  class MultiPostMaker implements Maker {
    /**
     * {@link MultiMaker} used as a first stage
     */
    MultiMaker multi_maker; 

    /**
     * Second stage {@link PostProc} (post processor)
     */
    PostProc post_proc;

    /**
     * Constructor
     *
     * @param fld  global field header name
     * @param post string name of post processor
     */
    public MultiPostMaker(String fld, String post) { multi_maker = new MultiMaker(fld);
                                                     post_proc   = BundlesDT.createPostProcessor(post, tablet.getBundles().getGlobals()); }
    public String[] stringKeys(Bundle bundle) {
      String strs[] = multi_maker.stringKeys(bundle); List<String> al = new ArrayList<String>();
      for (int i=0;i<strs.length;i++) {
        String post[] = post_proc.postProcess(strs[i]);
	for (int j=0;j<post.length;j++) al.add(post[j]);
      }
      strs = new String[al.size()]; for (int i=0;i<strs.length;i++) strs[i] = al.get(i);
      return strs;
    }
  }

  /**
   * {@link Maker} used to convert intervals into duations
   */
  public class DurationMaker implements Maker {
    long dur;
    public DurationMaker(long dur) { this.dur = dur; }
    public String[] stringKeys(Bundle bundle) {
      String strs[] = new String[1];
      long diff = (bundle.ts1() - bundle.ts0());             // Different between start and end of event
      if (diff == 0L) { strs[0] = "0"; return strs; }        // If difference is 0, return "0"
      int  div  = (int) (diff/dur), mod  = (int) (diff%dur); // Divide and look for left overs
      if (mod > dur/2) div++;                                // Apply left overs if they're more than half of the duration
      strs[0] = ""+div;                                      // Embed as a string for return
      return strs;
    }
  }

  final int MILLIS  = 1;
  final int SECONDS = 1000;
  final int MINUTES = 60*SECONDS;
  final int HOURS   = 60*MINUTES;
  final int DAYS    = 24*HOURS;
  final int WEEKS   = 7*DAYS;

  /**
   * {@link Maker} used to convert to a date representation.
   */
  public class TimeMaker implements Maker {
    /**
     * Format to convert into
     */
    String format;

    /**
     * Simple date formater to use for the dates
     */
    SimpleDateFormat sdf;

    /**
     * Conversion class to change the time-based string into a long timestamp-like value
     */
    Conversion conv;

    /**
     * Constructor - can be used for different formats (and different groupings)
     * based on the formatting strings.
     *
     * @param format format for the date conversion
     */
    public TimeMaker(String format, Tablet tablet) { 
      if (format.equals(BY_AUTOTIME_STR)) {
        long ts0 = tablet.getBundles().ts0(), ts1 = tablet.getBundles().ts1(), interval = ts1 - ts0;
	if      (interval > 10*Utils.YEARS)  format = BY_YEAR_STR;
	else if (interval > 6 *Utils.MONTHS) format = BY_YEAR_MONTH_STR;
	else if (interval > 1 *Utils.MONTHS) format = BY_YEAR_MONTH_DAY_STR;
	else if (interval > 1 *Utils.WEEKS)  format = BY_YEAR_MONTH_DAY_HOUR_STR;
	else if (interval > 1 *Utils.DAYS)   format = BY_YEAR_MONTH_DAY_HOUR_MIN_STR;
	else if (interval > 1 *Utils.HOURS)  format = BY_YEAR_MONTH_DAY_HOUR_MIN_SEC_STR;
        else                                 format = BY_MILLIS_STR;
      }

      this.format = format;
      if      (format.equals(BY_YEAR_STR))                        { sdf = new SimpleDateFormat("yyyy");                    conv = new FormatConversion();      }
      else if (format.equals(BY_YEAR_MONTH_STR))                  { sdf = new SimpleDateFormat("yyyy-MM");                 conv = new FormatConversion();      }
      else if (format.equals(BY_YEAR_MONTH_DAY_STR))              { sdf = new SimpleDateFormat("yyyy-MM-dd");              conv = new FormatConversion();      }
      else if (format.equals(BY_YEAR_MONTH_DAY_HOUR_STR))         { sdf = new SimpleDateFormat("yyyy-MM-dd HH");           conv = new FormatConversion();      }
      else if (format.equals(BY_YEAR_MONTH_DAY_HOUR_MIN_STR))     { sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");        conv = new FormatConversion();      }
      else if (format.equals(BY_YEAR_MONTH_DAY_HOUR_MIN_SEC_STR)) { sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");     conv = new FormatConversion();      }
      else if (format.equals(BY_MONTH_STR))                       { sdf = new SimpleDateFormat("MM");                      conv = new MonthConversion(false);  }
      else if (format.equals(BY_MONTH_DAY_STR))                   { sdf = new SimpleDateFormat("MM-dd");                   conv = new MonthConversion(true);   }
      else if (format.equals(BY_DAYOFWEEK_STR))                   { sdf = new SimpleDateFormat("EEE");                     conv = new WeeklyConversion(false); }
      else if (format.equals(BY_DAYOFWEEK_HOUR_STR))              { sdf = new SimpleDateFormat("EEE HH");                  conv = new WeeklyConversion(true);  }
      else if (format.equals(BY_HOUR_STR))                        { sdf = new SimpleDateFormat("HH");                      conv = new PeriodicConversion(DAYS,    HOURS);   }
      else if (format.equals(BY_HOUR_MINUTE_STR))                 { sdf = new SimpleDateFormat("HH:mm");                   conv = new PeriodicConversion(DAYS,    MINUTES); }
      else if (format.equals(BY_MINUTE_STR))                      { sdf = new SimpleDateFormat("mm");                      conv = new PeriodicConversion(HOURS,   MINUTES); }
      else if (format.equals(BY_MINUTE_SECOND_STR))               { sdf = new SimpleDateFormat("mm:ss");                   conv = new PeriodicConversion(HOURS,   SECONDS); }
      else if (format.equals(BY_SECOND_STR))                      { sdf = new SimpleDateFormat("ss");                      conv = new PeriodicConversion(MINUTES, SECONDS); }
      else if (format.equals(BY_MILLIS_STR))                      { sdf = new SimpleDateFormat("SSS");                     conv = new PeriodicConversion(SECONDS, 0);  }
      else if (format.equals(BY_STRAIGHT_STR))                    { sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); conv = new StraightConversion();      }
      else                                                        { sdf = new SimpleDateFormat("yyyy");                    conv = new FormatConversion();      }
      sdf.setTimeZone(TimeZone.getTimeZone("GMT")); 
    }

    /**
     * Returns if this time maker is linear (versus periodic).
     *
     *@return true if linear time
     */
    public boolean linearTime() { return conv.linearTime(); }

    /**
     * Return the minimum value returned for periodic time conversions.
     *
     *@return minimum periodic value
     */
    public long   minPeriodicValue() { return conv.minPeriodicValue(); }

    /**
     * Return the maximum value returned for periodic time conversions.
     *
     *@return maximum periodic value
     */
    public long   maxPeriodicValue() { return conv.maxPeriodicValue(); }

    /**
     * Convert the bundle's timestamp into a long timestamp-like value.
     *
     *@param  bundle bundle to pull the timestamp from
     *
     *@return timestamp-like value
     */
    public long timeStampKey(Bundle bundle) { return conv.timeStampKey(bundle); }

    /**
     * Convert the bundle's end timestamp into a long timestamp-like value.
     *
     *@param  bundle bundle to pull the timestamp from
     *
     *@return timestamp-like value
     */
    public long endTimeStampKey(Bundle bundle) { return conv.endTimeStampKey(bundle); }

    /**
     * Convert a timestamp into a long timestamp-like value.
     *
     *@param  l timestamp
     *
     *@return timestamp-like value
     */
    public long timeStampKey(long l) { return conv.timeStampKey(l); }

    /**
     * Convert the bundles timestamp into the specified date string.
     *
     *@param bundle record to convert
     *
     *@return date formatted string
     */
    public String[] stringKeys(Bundle bundle) {
      String strs[] = new String[1]; strs[0] = sdf.format(new Date(bundle.ts0()));  return strs; 
    }
    
    /**
     * Convert the long timestamp into the specified date string.
     *
     *@param l timestamp as epoch
     *
     *@return date formatted string
     */
    public String toString(long l) { return conv.toString(l); }

    class Conversion { 
      public long    timeStampKey(Bundle bundle)    { return 0L;   } 
      public long    endTimeStampKey(Bundle bundle) { return 0L;   } 
      public long    timeStampKey(long l)           { return 0L;   }
      public boolean linearTime()                   { return true; }
      public long    minPeriodicValue()             { return 0L;   }
      public long    maxPeriodicValue()             { return 0L;   }
      public String  toString(long l)               { return "Not Implemented"; }
    }

    /**
     * Conversion class for just a straight return of the millisecond value.
     */
    class StraightConversion extends Conversion {
      public long    timeStampKey   (Bundle bundle) { return timeStampKey(bundle.ts0()); }
      public long    endTimeStampKey(Bundle bundle) { return timeStampKey(bundle.ts1()); }
      public long    timeStampKey   (long l)        { return l; }
      public String  toString       (long l)        { return sdf.format(new Date(l)); }
    }

    /**
     * Conversion class for most-significant date portions.  Enables yearly down to year-month-day-hour-min conversions.
     */
    class FormatConversion extends Conversion { 
      Calendar gmtcal; { gmtcal = Calendar.getInstance(TimeZone.getTimeZone("GMT")); }
      Map<Integer,Integer> calsettings = new HashMap<Integer,Integer>();
      public FormatConversion() { 
        // Figure out which settings to nullify when a new date is converted
        calsettings.put(Calendar.MILLISECOND,  0);
        if (format.equals(BY_YEAR_STR))                    { calsettings.put(Calendar.SECOND, 0); calsettings.put(Calendar.MINUTE, 0); calsettings.put(Calendar.HOUR_OF_DAY, 0); calsettings.put(Calendar.DAY_OF_MONTH, 1); calsettings.put(Calendar.MONTH,        0); }
        if (format.equals(BY_YEAR_MONTH_STR))              { calsettings.put(Calendar.SECOND, 0); calsettings.put(Calendar.MINUTE, 0); calsettings.put(Calendar.HOUR_OF_DAY, 0); calsettings.put(Calendar.DAY_OF_MONTH, 1); }
        if (format.equals(BY_YEAR_MONTH_DAY_STR))          { calsettings.put(Calendar.SECOND, 0); calsettings.put(Calendar.MINUTE, 0); calsettings.put(Calendar.HOUR_OF_DAY, 0); }
        if (format.equals(BY_YEAR_MONTH_DAY_HOUR_STR))     { calsettings.put(Calendar.SECOND, 0); calsettings.put(Calendar.MINUTE, 0); }
        if (format.equals(BY_YEAR_MONTH_DAY_HOUR_MIN_STR)) { calsettings.put(Calendar.SECOND, 0); }
      }
      public long timeStampKey(long l) {
        gmtcal.setTimeInMillis(l);
	Iterator<Integer> it = calsettings.keySet().iterator();
	while (it.hasNext()) { int field = it.next(); int value = calsettings.get(field); gmtcal.set(field,value); }
	return gmtcal.getTimeInMillis();
      }
      public long timeStampKey(Bundle bundle) {
        return timeStampKey(bundle.ts0());
      }
      public long endTimeStampKey(Bundle bundle) {
        return timeStampKey(bundle.ts1());
      }
      public String toString(long l) {
        return sdf.format(new Date(l));
      }
    }

    /**
     * Conversion class for monthly (and monthly-day) conversions.
     */
    class MonthConversion extends Conversion { 
      Calendar gmtcal; { gmtcal = Calendar.getInstance(TimeZone.getTimeZone("GMT")); }  boolean include_days;
      public MonthConversion(boolean include_days) { this.include_days = include_days; }
      public long timeStampKey(Bundle bundle) {
        gmtcal.setTimeInMillis(bundle.ts0());
	if (include_days) { return gmtcal.get(Calendar.MONTH)*31 + gmtcal.get(Calendar.DAY_OF_MONTH) - 1; // Day is one-based
	} else            { return gmtcal.get(Calendar.MONTH); }
      }
      public boolean linearTime()       { return false; }
      public long    minPeriodicValue() { return 0L; }
      public long    maxPeriodicValue() { if (include_days) return 31L*12L-1L; else return 12L-1L; }
      public String  toString(long l) {
        if (include_days) {
          String month = monthAsString(l/31), day = "" + (1 + l%31); 
	  if (day.length()   == 1) day   = "0" + day;
	  return month + "-" + day;
	} else {
	  String month = monthAsString(l);
	  return month;
	}
      }
      private String monthAsString(long l) {
        switch ((int) l) {
	  case  0:  return "Jan";
	  case  1:  return "Feb";
	  case  2:  return "Mar";
	  case  3:  return "Apr";
	  case  4:  return "May";
	  case  5:  return "Jun";
	  case  6:  return "Jul";
	  case  7:  return "Aug";
	  case  8:  return "Sep";
	  case  9:  return "Oct";
	  case 10:  return "Nov";
	  case 11:  return "Dec";
	  default:  return "Huh";
	}
      }
    }

    /**
     * Conversion class for weekly (and weekly-hour) conversions.
     */
    class WeeklyConversion extends Conversion { 
      Calendar gmtcal; { gmtcal = Calendar.getInstance(TimeZone.getTimeZone("GMT")); } boolean include_hours;
      public WeeklyConversion(boolean include_hours) { this.include_hours = include_hours; }
      public long timeStampKey(Bundle bundle) {
        gmtcal.setTimeInMillis(bundle.ts0());
	if (include_hours) { return 24*(gmtcal.get(Calendar.DAY_OF_WEEK) - 1) + gmtcal.get(Calendar.HOUR_OF_DAY);
	} else             { return     gmtcal.get(Calendar.DAY_OF_WEEK) - 1; }
      }
      public long endTimeStampKey(Bundle bundle) {
        gmtcal.setTimeInMillis(bundle.ts1());
	if (include_hours) { return 24*(gmtcal.get(Calendar.DAY_OF_WEEK) - 1) + gmtcal.get(Calendar.HOUR_OF_DAY);
	} else             { return     gmtcal.get(Calendar.DAY_OF_WEEK) - 1; }
      }
      public boolean linearTime() { return false; }
      public long    minPeriodicValue() { return 0L; }
      public long    maxPeriodicValue() { if (include_hours) return 24L*7L-1L; else return 7L-1L; }
      public String toString(long l) {
        if (include_hours) {
          long dow = l/24, hour = l%24; 
	  return dayAsString(dow) + " " + (hour < 10 ? "0" + hour: "" + hour) + ":00";
	} else {
	  return dayAsString(l);
	}
      }
      private String dayAsString(long l) {
        switch ((int) l) {
	  case 0:  return "Sun";
	  case 1:  return "Mon";
	  case 2:  return "Tue";
	  case 3:  return "Wed";
	  case 4:  return "Thu";
	  case 5:  return "Fri";
	  case 6:  return "Sat";
	  default: return "Huh";
	}
      }
    }

    /**
     * Conversion class for periodic conversion.  Also includes ability to remove components to make it more discrete.
     */
    class PeriodicConversion extends Conversion { 
      long mod, sub;
      public PeriodicConversion(long mod, long sub) { this.mod = mod; this.sub = sub; }
      public long timeStampKey(Bundle bundle) { long ts = bundle.ts0(); ts = ts%mod; if (sub > 0) ts = ts - ts%sub; return ts; }
      public long endTimeStampKey(Bundle bundle) { long ts = bundle.ts1(); ts = ts%mod; if (sub > 0) ts = ts - ts%sub; return ts; }
      public boolean linearTime() { return false; }
      public long    minPeriodicValue() { return 0L; }
      public long    maxPeriodicValue() {
        switch ((int) mod) {
	  case DAYS:     return 24L * 60L * 60L * 1000L - sub;
	  case HOURS:    return       60L * 60L * 1000L - sub;
	  case MINUTES:  return             60L * 1000L - sub;
	  case SECONDS:  return                   1000L - sub;
          case MILLIS:   return 1000L;
	  default:       throw new RuntimeException("Unknown Periodicity");
	}
      }
      public String toString(long l) { 
        int minutes = 0, hours = 0, seconds = 0;
        switch ((int) mod) {
          case DAYS:    hours   = (int) (l/HOURS);
                        l = l - hours*HOURS;
                        minutes = (int) (l/MINUTES);
                        l = l  - minutes*MINUTES;
                        return ((hours < 10) ? "0" + hours : "" + hours) + ":" + ((minutes < 10) ? "0" + minutes : "" + minutes);
          case HOURS:   minutes = (int) (l/MINUTES);
                        l = l - minutes*MINUTES;
                        seconds = (int) (l/1000);
                        return ((minutes < 10) ? "0" + minutes : "" +minutes) + ":" +
                               ((seconds < 10) ? "0" + seconds : "" +seconds);
          case MINUTES: return (l/1000) + " Seconds";
	  case SECONDS: return "" + (l/1000);
          case MILLIS:  return "" + (l%1000);
          default:      return "Unknown Periodicity";
        }
      }
    }
  }

  /**
   * Tablet to apply conversions to
   */
  private Tablet tablet;

  /**
   * Specific blank name to use 
   */
  private String blank;

  /**
   * Maker created from the specified blank and tablet
   */
  private Maker  maker;

  /**
   * Create a Keymaker based on the specific tablet and blank.  Some amount
   * of parsing is required to find the right {@link Maker}.
   *
   * @param tablet tablet for the conversions
   * @param blank  specified blank
   */
  public KeyMaker(Tablet tablet, String blank) {
    this.tablet = tablet; this.blank = blank;
    if        (blank.equals(TABLET_SEP_STR))         {
                                                maker = new TabletSepMaker(tablet);
    } else if (blank.equals(ALL_ENTITIES_STR))       {
                                                maker = new AllEntitiesMaker(tablet);
    } else if (blank.startsWith("|Tm|"))             {
                                                maker = new TimeMaker(blank, tablet);
    } else if (blank.startsWith("|Dur|"))            {
      if      (blank.equals(BY_DUR_SEC_STR))    maker = new DurationMaker(SECONDS);
      else if (blank.equals(BY_DUR_HOUR_STR))   maker = new DurationMaker(HOURS);
      else throw new RuntimeException("Do Not Understand Duration KeyMaker \"" + blank + "\"");
    } else if (blank.startsWith(BundlesDT.TAGS))     {
      if      (blank.equals(BundlesDT.TAGS))    maker = new SimpleMaker(BundlesDT.TAGS);
      else if (blank.endsWith(BundlesDT.MULTI)) maker = new MultiTagMaker();
      else                                      maker = new TypeTagMaker(blank.substring(blank.lastIndexOf(BundlesDT.DELIM)+1,blank.length()));
    } else if (blank.endsWith(BundlesDT.MULTI))      { maker = new MultiMaker(blank.substring(0,blank.indexOf(BundlesDT.DELIM)));
    } else if (blank.indexOf(BundlesDT.MULTI)  >= 0) { 
      maker = new MultiPostMaker(blank.substring(0,blank.indexOf(BundlesDT.DELIM)),
                                 blank.substring(blank.indexOf(BundlesDT.DELIM) + BundlesDT.MULTI.length() + 1,
				                 blank.length()));
    } else if (blank.indexOf(BundlesDT.DELIM)  >= 0) { maker = new SimplePostMaker(blank.substring(0,blank.indexOf(BundlesDT.DELIM)), blank.substring(blank.indexOf(BundlesDT.DELIM)+1,blank.length()));
    } else {                                           maker = new SimpleMaker(blank); }
  }

  /**
   * Determine if a key blank is time-based.
   *
   *@param blank blank to examine
   *
   *@return true if blank is time-based
   */
  public static boolean isTimeBlank(String blank) {
    return blank.startsWith("|Tm|");
  }

  /**
   * Determine if the KeyMaker is based on time elements.
   *
   *@return true if the key maker is time oriented
   */
  public boolean isTimeBased() { return maker instanceof TimeMaker; }

  /**
   * Determine if the KeyMaker is based on linear time.
   *
   *@return true if linear-time-based
   */
  public boolean linearTime() {
    return (maker instanceof TimeMaker) && ((TimeMaker) maker).linearTime();
  }

  /**
   * Determine the minimum value for the periodic function.
   *
   *@return min value for periodic function
   */
  public long minPeriodicValue() {
    if (maker instanceof TimeMaker && ((TimeMaker) maker).linearTime() == false) return ((TimeMaker) maker).minPeriodicValue();
    throw new RuntimeException("minPeriodicValue() called for non-time maker / linear time maker");
  }

  /**
   * Determine the max value for the periodic function.
   *
   *@return max value for periodic function
   */
  public long maxPeriodicValue() {
    if (maker instanceof TimeMaker && ((TimeMaker) maker).linearTime() == false) return ((TimeMaker) maker).maxPeriodicValue();
    throw new RuntimeException("maxPeriodicValue() called for non-time maker / linear time maker");
  }

  /**
   * Special string conversion for time-base makers.  All others return null.
   *
   *@param timestamp timestamp to convert
   *
   *@return string in timemaker format
   */
  public String toString(long timestamp) {
    if (maker instanceof TimeMaker) return ((TimeMaker) maker).toString(timestamp);
    else return null;
  }

  /**
   * Turn a bundle into the timestamp for the bin in the TimeMaker conversion.
   *
   *@param bundle bundle to convert
   *
   *@return long timestamp value of the bin the bundle fits into
   */
  public long timeStampKey(Bundle bundle) { return ((TimeMaker) maker).timeStampKey(bundle); }

  /**
   * Turn a bundle into the end timestamp for the bin in the TimeMaker conversion.
   *
   *@param bundle bundle to convert
   *
   *@return long timestamp value of the bin the bundle fits into
   */
  public long endTimeStampKey(Bundle bundle) { return ((TimeMaker) maker).endTimeStampKey(bundle); }

  /**
   * Convert a bundle over to the specific keys.
   *
   * @param  bundle bundle/record to convert
   * @return        array of strings after the conversion to keys
   */
  public String[] stringKeys(Bundle bundle) { return maker.stringKeys(bundle); }
  
  /**
   * Reverse lookup to convert integers back to strings
   */
  public Map<Integer,String> reverse_lu = new HashMap<Integer,String>();

  /**
   * Convert a bundle over to the specific keys as integers.  Keep track
   * of the conversions in case they need to be converted back.  
   *
   * @param  bundle bundle/record to convert
   * @return        array of integers after the conversion
   */
  public int[]    intKeys   (Bundle bundle) { 
    String strs[] = stringKeys(bundle);
    int    ints[] = new int[strs.length];
    for (int i=0;i<strs.length;i++) {
      ints[i] = tablet.getBundles().getGlobals().toInt(strs[i]);
      reverse_lu.put(ints[i], strs[i]);
    }
    return ints;
  }

  /**
   * Reverse an index back into a string.  This causes problems because it only works if this
   * key maker was used to do the conversion in the first place.  See RTXYPanel.java as an
   * example.
   *
   * @param  index index to convert back
   * @return       String representation of the index
   */
  public String   toString(int index) { return reverse_lu.get(index); }
}
