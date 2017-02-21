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

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import racetrack.gui.RT;
import racetrack.gui.TimeMarker;
import racetrack.kb.EntityTag;
import racetrack.kb.RTComment;
import racetrack.util.CSVTokenConsumer;
import racetrack.util.Utils;

/**
 * The initial implementation for aggregating bundles together.  Bundles
 * are most equivalent to rows in a structure database/csv and are held in
 * {@link Tablet} which are same-type structured data.  The framework class
 * also contains a {@link BundlesG} reference that contains the long-lived
 * reference lookup tables.
 *
 * @author  D. Trimm
 * @version 1.0
 */
public class BundlesRecs extends Bundles {
  /**
   * Set of {@link Tablet} within this view of the data set
   */
  private Set<Tablet> tablet_set = new HashSet<Tablet>();

  /**
   * Set of {@link Bundle} within this data set
   */
  private Set<Bundle> bundle_set = new HashSet<Bundle>();

  /**
   * Default constructor
   */
  public BundlesRecs() { }
  
  /**
   * Internal method to add a {@link Bundle} to this data set.  Maintains the
   * first and last timestamp for the data.
   *
   * @param bundle Bundle to add
   */
  @Override
  public void             add(Bundle bundle)          { bundle_set.add(bundle); 
                                                        if (bundle.hasTime()) {
							  if (t0    > bundle.ts0()) t0    = bundle.ts0();
							  if (t1    < bundle.ts0()) t1    = bundle.ts0();
                                                          if (t1dur < bundle.ts1()) t1dur = bundle.ts1();
							}
						      }

  /**
   * Return a tablet object constructed with this bundles.
   *
   *@return tablet object
   */
  @Override
  Tablet createTablet() { return new MyTablet(this); }

  /**
   * Internal method to add a {@link Tablet} to this data set.  Tablets are used
   * to hold the same type of structured data.
   *
   * @param tablet Tablet to add
   */
  @Override
  public void             add(Tablet tablet)          { tablet_set.add(tablet); }

  /**
   * Find or create a tablet with the specified header labels.
   *
   *@param hdrs headers in the tablet-to-find
   *
   *@return existing tablet if it exists, otherwise a new table (already added to the bundles)
   */
  @Override
  public Tablet findOrCreateTablet(String hdrs[]) {
    Tablet tablet = new MyTablet(this);
    // Parse the headers and put them into the global fields -- this should really only be used
    // by internal methods that understand what they are asking for...
    int local_i = 0;
    for (int i=0;i<hdrs.length;i++) {
      if        (hdrs[i].equals("timestamp")     || hdrs[i].equals("beg")) { tablet.setHasTimeStamps();
      } else if (hdrs[i].equals("timestamp_end") || hdrs[i].equals("end")) { tablet.setHasDurations();
      } else {
        int fld_i = globals.getOrCreateField(hdrs[i], Utils.isAllUpper(hdrs[i]));
	tablet.setFieldIndex(fld_i, local_i);
	local_i++;
      }
    }
    // See if a previous tablet already exists -- if so, return it
    Iterator<Tablet> it = tabletIterator();
    while (it.hasNext()) { Tablet other_tablet = it.next(); if (other_tablet.equals(tablet)) return other_tablet; }
    // Add the tablet and return it
    add(tablet); 
    return tablet;
  }

  /**
   * Method to create an iterator through the tablets in this data set.
   *
   * @return Iterator over the tablets in this data set
   */
  @Override
  public Iterator<Tablet> tabletIterator()            { return tablet_set.iterator(); }

  /**
   * Return the total number of tablets.
   *
   * @return Number of tablets
   */
  @Override
  public int tabletCount() { return tablet_set.size(); }

  /**
   * Method to create an iterator over all of the bundles in this data set.
   *
   * @return Iterator over the bundles in this data set
   */
  @Override
  public Iterator<Bundle> bundleIterator()            { return bundle_set.iterator(); }

  /**
   * Method to return the set of bundles in this data set.
   *
   * @return Set of the bundles
   */
  @Override
  public Set<Bundle>  bundleSet()                 { return bundle_set; }

  /**
   * Returns the number of bundles in this data set.
   *
   * @return The number of bundles in this data set
   */
  @Override
  public int              size()                      { return bundle_set.size();}

  /**
   * subset(); make a subset of these bundles that includes the same globals.
   */
  /**
   * Create a new data set based on a subset of the bundles provided.  This is
   * the primary method for data filtering operations.
   *
   * @param  set subset of bundles to use to create the new set
   * @return new {@link Bundles} object encapsulating the subset
   */
  @Override
  public Bundles          subset(Set<Bundle> set) { 
    // Make the new one, copy over the globals
    Bundles new_bundles = new BundlesRecs();
    new_bundles.globals = getGlobals();
    // Go through the individual elements and add them to the new bundles
    Map<Tablet,Tablet> tablet_lu = new HashMap<Tablet,Tablet>();
    Iterator<Bundle> it = set.iterator();
    while (it.hasNext()) {
      Bundle bundle = it.next();
      Tablet tablet = bundle.getTablet();
      if (tablet_lu.containsKey(tablet) == false) {
        Tablet new_tablet = new MyTablet(new_bundles, (MyTablet) tablet);
	tablet_lu.put(tablet, new_tablet);
	new_bundles.add(new_tablet);
      }
      // Add them to the sets
      ((MyTablet) tablet_lu.get(tablet)).add(bundle); new_bundles.add(bundle);
    }
    // Return the set
    return new_bundles;
  }

  /**
   * Return a string representation of the bundles.
   *
   * @return String representation
   */
  public String toString() {
    return "|Bundles|Sz=" + bundle_set.size() + "|" + Utils.humanReadableDate(ts0()) + " => " + Utils.humanReadableDate(ts1()) +
           "|Tablets=" + tablet_set.size() + "|";
  }

  /**
   * Clear the tags for the existing bundles (if they have the field).  Do not create the field if it's not already there.
   */
  @Override
  public void clearTags() {
    BundlesG globals = getGlobals();
    int      fld_i   = globals.fieldIndex(BundlesDT.TAGS);
    Iterator<Tablet> it_tab = tabletIterator(); while (it_tab.hasNext()) { Tablet tablet = it_tab.next();
      if (tablet.hasField(fld_i)) {
        int              local_i = ((MyTablet) tablet).localIndex(fld_i);
        Iterator<Bundle> it_bun  = tablet.bundleIterator();
	while (it_bun.hasNext()) {
          Bundle bundle = it_bun.next();
	  ((MyBundle) bundle).getStrs()[local_i] = BundlesDT.NOTSET;
	}
      }
    }
  }

  /**
   * Add the specified tags to the specified records/bundles.  Create new space within bundles if necessary.
   *
   *@param visible records to apply the new tags to
   *@param tags    tags to add
   */
  @Override
  public boolean addTags(Bundles visible, String tags) {
    BundlesG globals = getGlobals(); boolean field_added = false;
    int      fld_i   = globals.getOrCreateField(BundlesDT.TAGS, false);

    // For the visible bundles, determine if any tablets need the tag field and add it
    Iterator<Tablet> it_tab = visible.tabletIterator(); while (it_tab.hasNext()) { Tablet tablet = it_tab.next();
      String hdr = tablet.fileHeader();
      if (tablet.hasField(fld_i) == false) { // Does not have the tag field -- add it in the root
        Iterator<Tablet> it_root_tabs = tabletIterator();  while (it_root_tabs.hasNext()) { Tablet root_tablet = it_root_tabs.next();
          if (root_tablet.fileHeader().equals(hdr)) {
            root_tablet.addField(BundlesDT.TAGS); field_added = true;
    } } } }

    // Now set the records
    it_tab = visible.tabletIterator(); while (it_tab.hasNext()) { Tablet tablet = it_tab.next();
      int              local_i = ((MyTablet) tablet).localIndex(fld_i);
      Iterator<Bundle> it_bun  = tablet.bundleIterator();
      while (it_bun.hasNext()) {
        Bundle bundle  = it_bun.next();
	String old_tag = ((MyBundle) bundle).getStrs()[local_i];
	if (old_tag.equals(BundlesDT.NOTSET)) {
          ((MyBundle) bundle).getStrs()[local_i] = tags; 
          globals.addFieldEntity(fld_i, ((MyBundle) bundle).getStrs()[local_i]);
        } else {
	  ((MyBundle) bundle).getStrs()[local_i] = Utils.normalizeTag(old_tag + "|" + tags);
          globals.addFieldEntity(fld_i, ((MyBundle) bundle).getStrs()[local_i]);
	}
      }
    }

    // Return if a new field was added
    return field_added;
  }

  /**
   * Replace the tag field with the specified tag.  Create new space within bundles if necessary.
   *
   *@param visible bundles to apply the change to
   *@param tags    tags for replacement
   *
   *@return true if a new field was added
   */
  @Override
  public boolean replaceTags(Bundles visible, String tags) {
    BundlesG globals = getGlobals(); boolean field_added = false;
    int      fld_i   = globals.getOrCreateField(BundlesDT.TAGS, false);

    // For the visible bundles, determine if any tablets need the tag field and add it
    Iterator<Tablet> it_tab = visible.tabletIterator(); while (it_tab.hasNext()) { Tablet tablet = it_tab.next();
      String hdr = tablet.fileHeader();
      if (tablet.hasField(fld_i) == false) { // Does not have the tag field -- add it in the root
        Iterator<Tablet> it_root_tabs = tabletIterator();  while (it_root_tabs.hasNext()) { Tablet root_tablet = it_root_tabs.next();
          if (root_tablet.fileHeader().equals(hdr)) {
            root_tablet.addField(BundlesDT.TAGS); field_added = true;
    } } } }

    // Now execute the replacement operation
    it_tab = visible.tabletIterator(); while (it_tab.hasNext()) { Tablet tablet = it_tab.next();
      int              local_i = ((MyTablet) tablet).localIndex(fld_i);
      Iterator<Bundle> it_bun  = tablet.bundleIterator();
      while (it_bun.hasNext()) {
        Bundle bundle  = it_bun.next();
	((MyBundle) bundle).getStrs()[local_i] = tags;
        globals.addFieldEntity(fld_i, ((MyBundle) bundle).getStrs()[local_i]);
      }
    }

    // Return if a new field was added
    return field_added;
  }

  /**
   * Replace just the specified type value tags in the visible bundles.
   *
   *@param to_replace tags to replace
   *
   *@return true if any fields were added
   */
  @Override
  public boolean replaceTypeValueTags(Bundles visible, String to_replace) {
    // Arrange the to_replace for easier access
    Map<String, List<String>> map = new HashMap<String, List<String>>();

    List<String> replaces = Utils.tokenizeTags(to_replace);
    for (int i=0;i<replaces.size();i++) {
      String tag = replaces.get(i);
      if (Utils.tagIsTypeValue(tag)) {
        String type = (Utils.separateTypeValueTag(tag))[0];
        if (map.containsKey(type) == false) map.put(type, new ArrayList<String>());
        map.get(type).add(tag);
      }
    }

    // Prepare the underlying data set for the replace operation
    BundlesG globals = getGlobals(); boolean field_added = false;
    int      fld_i   = globals.getOrCreateField(BundlesDT.TAGS, false);

    // For the visible bundles, determine if any tablets need the tag field and add it
    Iterator<Tablet> it_tab = visible.tabletIterator(); while (it_tab.hasNext()) { Tablet tablet = it_tab.next();
      String hdr = tablet.fileHeader();
      if (tablet.hasField(fld_i) == false) { // Does not have the tag field -- add it in the root
        Iterator<Tablet> it_root_tabs = tabletIterator();  while (it_root_tabs.hasNext()) { Tablet root_tablet = it_root_tabs.next();
          if (root_tablet.fileHeader().equals(hdr)) {
            root_tablet.addField(BundlesDT.TAGS); field_added = true;
    } } } }


    // Go through the visible records and replace those types
    it_tab = visible.tabletIterator(); while (it_tab.hasNext()) { Tablet tablet = it_tab.next();
      int local_i = ((MyTablet) tablet).localIndex(fld_i);
      Iterator<Bundle> it_bun = tablet.bundleIterator(); 
      while (it_bun.hasNext()) {
        Bundle bundle = it_bun.next();
        String tags   = ((MyBundle) bundle).getStrs()[local_i];

        // Search for any types that match those in the replacement specification
        List<String> tokens = Utils.tokenizeTags(tags);
        Iterator<String> it = tokens.iterator();
        while (it.hasNext()) {
          String tag = it.next();
          if (Utils.tagIsTypeValue(tag) && map.containsKey((Utils.separateTypeValueTag(tag))[0])) it.remove();
        }

        // Recombine the individual tags back into a string
        tokens.addAll(replaces); // Add in the replacers first
        ((MyBundle) bundle).getStrs()[local_i] = Utils.combineTags(tokens);
        globals.addFieldEntity(fld_i, ((MyBundle) bundle).getStrs()[local_i]);
      }
    }

    // Return if a new field was added
    return field_added;
  }

  /**
   * Remove the specified tags from the visible bundles.
   *
   *@param to_remove tags to remove
   */
  @Override
  public void removeTags(String to_remove) {
    // Create a removal set
    Set<String> set = new HashSet<String>();
    set.addAll(Utils.tokenizeTags(to_remove));

    // Go through the tablets
    BundlesG globals = getGlobals(); boolean field_added = false;
    int      fld_i   = globals.getOrCreateField(BundlesDT.TAGS, false);
    Iterator<Tablet> it_tab = tabletIterator(); while (it_tab.hasNext()) { Tablet tablet = it_tab.next();
      if (tablet.hasField(fld_i)) {
        int local_i = ((MyTablet) tablet).localIndex(fld_i);
        Iterator<Bundle> it_bun = tablet.bundleIterator(); 
        while (it_bun.hasNext()) {
          Bundle bundle = it_bun.next();
          String tags   = ((MyBundle) bundle).getStrs()[local_i];

          // Search for any tags in the removal set and drop those
          List<String> tokens = Utils.tokenizeTags(tags);
          Iterator<String> it = tokens.iterator();
          while (it.hasNext()) { String tag = it.next(); if (set.contains(tag)) it.remove(); }

          // Recombine the individual tags back into a string
          if (tokens.size() > 0) { ((MyBundle) bundle).getStrs()[local_i] = Utils.combineTags(tokens); globals.addFieldEntity(fld_i, ((MyBundle) bundle).getStrs()[local_i]); }
          else                   { ((MyBundle) bundle).getStrs()[local_i] = BundlesDT.NOTSET; }
        }
      }
    }
  }

  /**
   * Method to test the bundles object.
   *
   * @param args command line arguments - in this case dataset file names
   */
  public static void main(String args[]) {
    try {
      Bundles bundles = new BundlesRecs();
      for (int i=0;i<args.length;i++) {
        System.err.println("Loading File \"" + args[i] + "\"");
	BundlesUtils.parse(bundles, null, new File(args[i]), null);
	System.err.println("  Done!");
      }
    } catch (Throwable t) {
      System.err.println("Throwable : " + t);
      t.printStackTrace(System.err);
    }
  }

  /**
   *
   */
  public class MyBundle extends Bundle { 
    /**
     * Corresponding tablet for this bundle.
     */
    private Tablet tablet; 
  
    /**
     * Set of strings that this bundle represents.  The lookup correspondence
     * is maintained in the table.
     */
    private String strs[]; 
  
    /**
     * Create a new bundle within the specified tablet.
     *
     * @param tablet umbrella tablet for this bundle
     */
    public MyBundle(Tablet tablet) { this.tablet = tablet; }
  
    /**
     * Method to return the basic array of strings for this bundle.  Limited
     * to this package for access.
     *
     *@return string array for this bundle
     */
    protected String[] getStrs() { return strs; }
  
    /**
     * Method to set the basic array of strings for this bundle.  Limited to
     * this package for access.
     *
     *@param s new strings for bundle
     */
    void     setStrs(String s[]) { strs = s; }
  
    /**
     * Return the string within the specified field index of this bundle.
     *
     * @param  fld_i field index
     * @return       corresponding string
     */
    public String  toString(int fld_i)   { return strs[(tablet.getFields())[fld_i]]; }
  
    /**
     * Return the value of this field.
     *
     * @param  fld_i field index
     * @return       corresponding integer value
     */
    public int     toValue(int fld_i)    { return tablet.getBundles().getGlobals().toInt(strs[(tablet.getFields())[fld_i]]); }

    /**
     * Get the {@link Tablet} for this bundle.  Useful when you only have a reference to the bundle
     * but need to know about the table or other structured data.
     *
     * @return bundle's tablet
     */
    public Tablet  getTablet()           { return tablet; }
  
    /**
     * Save the bundle to a printstream so that it can be re-parsed.
     *
     * @param out print stream to save the bundle to
     */
    protected void    save(PrintStream out) { for (int i=0;i<strs.length;i++) out.print((i == 0) ? Utils.encToURL(strs[i]) : "," + Utils.encToURL(strs[i]));
                                              out.println(""); }
  
    /**
     * Return a uniform, normalized string representation of this bundle.
     *
     * @return normalized string representation
     */
    public String  toString()            {
      BundlesG globals         = tablet.getBundles().getGlobals();
      int      sorted_fld_is[] = ((MyTablet) tablet).getSortedFieldIndices();
      StringBuffer sb = new StringBuffer();
      for (int i=0;i<sorted_fld_is.length;i++) {
        sb.append(globals.fieldHeader(sorted_fld_is[i]) + "=" + Utils.encToURL(toString(sorted_fld_is[i])));
        if (i < (sorted_fld_is.length-1)) sb.append(BundlesDT.DELIM);
      }
      if (hasTime())     { sb.append(BundlesDT.DELIM); sb.append(Utils.exactDate(ts0())); }
      if (hasDuration()) { sb.append(BundlesDT.DELIM); sb.append(Utils.exactDate(ts1())); }
      return sb.toString();
    }
  }

  /**
   * Derivative of a basic bundle that includes a begin timestamp.
   *
   * @author  D. Trimm
   * @version 1.0
   */
  public class MyBundleTime extends MyBundle           {
    /**
     * begin timestamp
     */
    private long t0; 
  
    /**
     * Return the begin time stamp for this record.
     *
     * @return begin timestamp
     */
    @Override
    public long ts0() { return t0; } 
  
  
    /**
     * Indicate that this record has time.
     *
     *@return true
     */
    @Override
    public boolean hasTime() { return true; }
  
    /**
     * Set the begin time stamp for this record.
     *
     * @param new_ts0 new begin timestamp
     */
    @Override
    void setTS0(long new_ts0) { t0 = new_ts0; }
  
    /**
     * Return the end time stamp which corresponds to the begin timestamp
     *
     * @return begin timestamp
     */
    @Override
    public long ts1() { return t0; }
  
    /**
     * Constructor for creating a bundle to the corresponding tablet.
     *
     * @param tablet tablet to associate with this bundle
     */
    public MyBundleTime(Tablet tablet) { super(tablet); }
  
    /**
     * Save the bundle to printstream in a normalized form.
     *
     * @param out printstream for outputing the bundle to
     */
    @Override
    protected void    save(PrintStream out) { out.print(Utils.exactDate(ts0()));
                                              for (int i=0;i<getStrs().length;i++) out.print("," + Utils.encToURL(getStrs()[i]));
                                              out.println(""); }
  }
  
  /**
   * Bundle derivative that incorporates an end time stamp as well.
   *
   * @author  D. Trimm
   * @version 1.0
   */
  public class MyBundleDuration extends MyBundleTime   {
    /**
     * Ending timestamp
     */
    private long t1;
  
    /**
     * Return the ending timestamp for this record.
     *
     * @return end timestamp
     */
    @Override
    public long ts1() { return t1; }
  
    /**
     * Indicate that this record has a duration.
     */
    @Override
    public boolean hasDuration() { return true; }
  
    /**
     * Set the end time stamp for this record.
     *
     * @param new_ts1 new end timestamp
     */
    @Override
    void setTS1(long new_ts1) { 
      t1 = new_ts1; 
      // Make sure the end timestamp is after the begin timestamp
      // - This assumes that the end timestamp is set after the beginning...
      if (ts0() != 0L && ts0() > ts1()) {
        long swap = ts0();
        setTS0(t1);
        setTS1(swap);
      }
    }
  
    /**
     * Constructor for creating a bundle to the corresponding tablet.
     *
     * @param tablet tablet to associate with this bundle
     */
    public MyBundleDuration(Tablet tablet) { super(tablet); }
  
    /**
     * Save the bundle to printstream in a normalized form.
     *
     * @param out printstream for outputing the bundle to
     */
    @Override
    protected void    save(PrintStream out) { out.print(Utils.exactDate(ts0()) + "," + Utils.exactDate(ts1()));
                                              for (int i=0;i<getStrs().length;i++) out.print("," + Utils.encToURL(getStrs()[i]));
                                              out.println(""); }
  }

  /**
   * Provides a logical grouping for bundles (records) that have the same
   * exact fields.  For performance, tablets enable other methods to
   * apply operations (additions, set operations) without the need to
   * continuously check to see if the data types are present or the same.
   *
   * @author  D. Trimm
   * @version 1.0
   */
  public class MyTablet extends Tablet {
    /**
     * Overarching {@link Bundles} instance that this tablet
     * belongs to.
     */
    private Bundles         bundles        = null;
  
    /**
     * Set of the actual bundles in this tablet.  Used to iterate over.
     */
    private Set<Bundle>     bundle_set     = new HashSet<Bundle>();
  
    /**
     * Mapping of the fields indices to the corresponding field inside
     * each bundle/record.  If a -1 entry exist, that data is not held
     * by these records.
     */
    private int             fields[]       = new int[BundlesG.MAX_FIELDS];
  
    /**
     * Add a bundle (record) to this tablet.  Should probably only be
     * called by classes within this file since many assumptions are made
     * about the bundle's data fields.
     *
     *@param bundle bundle to add
     */
    void            add(Bundle bundle) { bundle_set.add(bundle); }

    /**
     * Create the a specific, instantiated bundle that can be added to this tablet.
     *
     *@return bundle instance for this tablet
     */
    Bundle createBundle() {
      if      (hasDurations())   return new MyBundleDuration(this);
      else if (hasTimeStamps())  return new MyBundleTime(this);
      else                       return new MyBundle(this);
    }
  
    /**
     * Set a field mapping index to convert from global indices to local indices.
     * For Internal Use (This File) Only
     *
     * @param fld_i   global index
     * @param local_i local index
     */
    void setFieldIndex    (int fld_i, int local_i) { fields[fld_i] = local_i; }
  
    /**
     * Return the local index for the specified field index.
     *
     *@param fld_i     global index
     *
     *@return local index
     */
    int  localIndex       (int fld_i) { return fields[fld_i]; }
  
    /**
     * Construct a new tablet with the corresponding {@link Bundles} class.
     *
     * @param bundles overarching data structure
     */
    public MyTablet(Bundles bundles) { this.bundles = bundles; for (int i=0;i<fields.length;i++) fields[i] = -1; }
  
    /**
     * Construct a new table with the corresponding {@link Bundles} class and by
     * copying the specified tablets attributes.  This is the constructor used
     * to subset data fields during filtering operations
     *
     * @param bundles  overarching data structure
     * @param to_copy  tablet's fields to use for mapping, timestamps, etc.
     */
    public MyTablet(Bundles bundles, MyTablet to_copy) { 
      this(bundles);
      this.has_timestamps = to_copy.has_timestamps;
      this.has_durations  = to_copy.has_durations;
      this.fields         = to_copy.fields;
    }
  
    /**
     * Add a field to this tablet and all associated records (bundles).
     *
     *@param fld field to add
     */
    void             addField(String fld) {
      // Get the field index
      int fld_i = bundles.getGlobals().getOrCreateField(fld, Utils.isAllUpper(fld));
      // Make sure the field doesn't already exist
      if (fields[fld_i] < 0) {
        // Determine the local index
        int local_i = 0;
        for (int i=0;i<fields.length;i++) if (fields[i] >= 0) local_i++;
  
        // Add the new field to each record
        if (bundleSet().size() > 0) {
          Iterator<Bundle> it = bundleIterator();
          while (it.hasNext()) {
            Bundle bundle     = it.next();
            String new_strs[] = new String[local_i+1];
            System.arraycopy(((MyBundle) bundle).getStrs(), 0, new_strs, 0, ((MyBundle) bundle).getStrs().length);
            new_strs[local_i] = BundlesDT.NOTSET;
            ((MyBundle) bundle).setStrs(new_strs);
          }
        }
  
        // Set the local index lookup
        fields[fld_i] = local_i;
  
        // Nullify some of the state to force a recalc
        sorted_fld_is = null;
  
      } // else we're done -- field already present
    }
  
    /**
     * For the records, set the specific field to the specific value.
     *
     *@param fld field to set
     *@param val value to set
     */
    void setField(String fld, String val) {
      // Get the field index
      int fld_i = bundles.getGlobals().getOrCreateField(fld, Utils.isAllUpper(fld));
      // Get the local index
      int local_i = fields[fld_i];
      // Go through the bundles
      Iterator<Bundle> it = bundleIterator();
      while (it.hasNext()) {
        Bundle bundle = it.next();
        ((MyBundle) bundle).getStrs()[local_i] = val;
        bundles.getGlobals().addFieldEntity(fld_i, val);
      }
    }
  
    /**
     * Return the overarching datastructure for this tablet.
     * 
     * @return Bundles
     */
    public Bundles          getBundles()        { return bundles; }
  
    /**
     * Return the set collection for the bundles.  Useful for other set operations in bulk.
     *
     *@return set of bundle/records
     */
    public Set<Bundle>  bundleSet()         { return bundle_set; }
  
    /**
     * Provide an interator over the individual bundles/records within this tablet.
     *
     * @return iterator over the records/bundles.
     */
    public Iterator<Bundle> bundleIterator()    { return bundle_set.iterator(); }
  
    /**
     * Remove the specified fields from this class.
     *
     *@param flds fields to remove
     */
    void removeFields(Set<String> flds) {
      Map<Integer,Integer> old_to_new_map = new HashMap<Integer,Integer>();
  
      int remove_count = 0;
      for (int i=0;i<fields.length;i++) { if (fields[i] >= 0) {
        String fld = bundles.getGlobals().fieldHeader(fields[i]);
        if (flds.contains(fld)) { fields[i] = -1; remove_count++; } else {
          old_to_new_map.put(fields[i], old_to_new_map.keySet().size());
          fields[i] = old_to_new_map.get(fields[i]);
      } } }
  
      // Bail out if no mods are needed
      if (remove_count == 0) return;
  
      // Make a faster old_to_new_map
      int new_to_old[] = new int[old_to_new_map.keySet().size()];
      Iterator<Integer> it_int = old_to_new_map.keySet().iterator(); 
      while (it_int.hasNext()) { int old_i = it_int.next(); int new_i = old_to_new_map.get(old_i); new_to_old[new_i] = old_i; }
  
      // Modify the individual bundles to remove those fields
      Iterator<Bundle> it_bun = bundleIterator(); while (it_bun.hasNext()) {
        Bundle bundle     = it_bun.next();
        String new_strs[] = new String[new_to_old.length];
        for (int i=0;i<new_strs.length;i++) new_strs[i] = ((MyBundle) bundle).getStrs()[new_to_old[i]];
        ((MyBundle) bundle).setStrs(new_strs);
      }
  
      // Fix up other variables
      sorted_fld_is = null;
    }
  
    /**
     * Returns if this tablet contains data matching the global field index.
     *
     * @param  fld_i global field index
     * @return       flag indicating that the data is present
     */
    public boolean          hasField(int fld_i) { if (fld_i < 0) return false; else return fields[fld_i] != -1; }
  
    /**
     * Returns the number of records / bundles within this tablet.
     *
     * @return the number of records
     */
    public int              size()              { return bundle_set.size();     }
  
    /**
     * Return the mapping array for this tablet.
     *
     * 2015-01-10 modified to return a copy of the data - makes it safer.
     *
     * @return array mapping global to local indices
     */
    public int[]            getFields()         { 
      int copy[] = new int[fields.length]; System.arraycopy(fields, 0, copy, 0, copy.length);
      return copy;
    }
  
    /**
     * Return a string that can be used to represent data stored  out of this tablet set.
     *
     * @return comma-separated string for the field headers in this tablet.
     */
    public String           fileHeader()        {
      List<String> al = new ArrayList<String>();
      int max_index = 0; for (int fld_i=0;fld_i<fields.length;fld_i++) if (max_index < fields[fld_i]) max_index = fields[fld_i];
      if (hasTimeStamps()) al.add("timestamp");
      if (hasDurations())  al.add("timestamp_end");
      for (int index=0;index<=max_index;index++) 
        for (int fld_i=0;fld_i<fields.length;fld_i++) 
          if (fields[fld_i] == index) 
	    al.add(bundles.getGlobals().fieldHeader(fld_i));
      StringBuffer sb = new StringBuffer(); for (int i=0;i<al.size();i++) sb.append((i == 0) ? al.get(i) : "," + al.get(i));
      return sb.toString();
    }
  
    /**
     * Sorted list of field indices.  List is sorted by header name.
     */
    protected int sorted_fld_is[] = null;
  
    /**
     * Return the sorted list of field indices.
     *
     * @return sorted list of field indices (sorted by header name)
     */
    int[]            getSortedFieldIndices() {
      if (sorted_fld_is == null) {
        List<String> al = new ArrayList<String>();
        int max_index = 0; for (int fld_i=0;fld_i<fields.length;fld_i++) if (max_index < fields[fld_i]) max_index = fields[fld_i];
        for (int index=0;index<=max_index;index++) 
          for (int fld_i=0;fld_i<fields.length;fld_i++) 
            if (fields[fld_i] == index) 
	      al.add(bundles.getGlobals().fieldHeader(fld_i));
        Collections.sort(al);
        sorted_fld_is = new int[al.size()];
        for (int i=0;i<al.size();i++) sorted_fld_is[i] = bundles.getGlobals().fieldIndex(al.get(i));
      }
      return sorted_fld_is;
    }
  
    /**
     * Mostly used for debugging purposes.
     *
     * @return high-level information about the underlying tablet data.
     */
    public String           toString() {
      List<String> al = new ArrayList<String>();
      for (int i=0;i<fields.length;i++) if (fields[i] != -1) al.add(bundles.getGlobals().fieldHeader(i));
      Collections.sort(al); StringBuffer sb = new StringBuffer(); sb.append("|Tablet|sz=" + bundle_set.size());
      for (int i=0;i<al.size();i++) { sb.append("|" + al.get(i)); }
      if (hasTimeStamps()) sb.append(" Tm");
      if (hasDurations())  sb.append(" Dur");
      return sb.toString();
    }
  
    /**
     * Determine if this tablet equals another.  Equals means that it has
     * the exact same number of named fields.  Timestamps are also checked.
     *
     *@param object to compare to
     *
     *@return true if the tablets are exactly the same
     */
    public boolean equals(Object object) {
      if (object instanceof Tablet) {
        Tablet o = (Tablet) object;
        int sorted[]   = getSortedFieldIndices(),
            o_sorted[] = ((MyTablet) o).getSortedFieldIndices();
        if (hasTimeStamps() != o.hasTimeStamps()) return false;
        if (hasDurations()  != o.hasDurations())  return false;
        if (sorted.length == o_sorted.length) {
          for (int i=0;i<sorted.length;i++) if (sorted[i] != o_sorted[i]) return false;
          return true;
        } else return false;
      } else return false;
    }
  
    /**
     * Add a new bundle.  Create the elements from the attribute map and the timestamps (if not null).
     * Note that for this method to succeed, all fields in this tablet must be satisfied.
     *
     *@param attr          attribute mapping
     *@param timestamp     if the tablet has timestamps, this parameter must not be null
     *@param timestamp_end if the tablet has timestamps and durations, this parameter must not be null
     *
     *@return the created bundle if successful, null otherwise
     */
    public Bundle addBundle(Map<String,String> attr, String timestamp, String timestamp_end) {
      // Check for the basics
      if (hasTimeStamps() &&                   timestamp     == null) return null;
      if (hasTimeStamps() && hasDurations() && timestamp_end == null) return null;
  
      // Allocate the correct time of bundle
      Bundle bundle = createBundle();
  
      // Set the timestamps if they exist in this type of bundle
      if (bundle.hasTime())     bundle.setTS0(Utils.parseTimeStamp(timestamp));
      if (bundle.hasDuration()) bundle.setTS1(Utils.parseTimeStamp(timestamp_end));
  
      // Allocate the strings
      ((MyBundle) bundle).setStrs(new String[attr.keySet().size()]);
      // System.err.println("Adding To Tablet \"" + toString() + "\"");
  
      // Go through the fields
      Iterator<String> it = attr.keySet().iterator();
      while (it.hasNext()) {
        String fld = it.next(); String val = attr.get(fld);
  
        // Normalize the tag
        if (fld.equals(BundlesDT.TAGS)) val = Utils.normalizeTag(val);
  
        // Get the field index
        int fld_i = bundles.getGlobals().getOrCreateField(fld, Utils.isAllUpper(fld));
  
        // Get the lcoal index
        int i     = fields[fld_i];
  
        // System.err.println("  Fld = \"" + fld + "\", fld_i=" + fld_i + ", i="+i);
        bundles.getGlobals().addFieldEntity(fld_i, val);
  
        // Set the field value
        ((MyBundle) bundle).getStrs()[i] = val;
      }
  
      // Add duration entity lookups
      if (bundle.hasDuration()) {
        String blanks[] = KeyMaker.blanks(bundles.getGlobals(), false, false, false, true);
        for (int i=0;i<blanks.length;i++) {
          if (blanks[i].startsWith("|Dur|")) {
            KeyMaker km = new KeyMaker(this, blanks[i]);
            bundles.getGlobals().addFieldEntity(-1, (km.stringKeys(bundle))[0]);
          }
        }
      }
  
      // Add the bundle to the roots
      bundles.add(bundle); add(bundle);
      return bundle;
    }
  
    /**
     * Add a new bundle.  Create the elements from the attribute map and the timestamps (if not null).
     * Note that for this method to succeed, all fields in this tablet must be satisfied.
     *
     *@param attr          attribute mapping
     *@param timestamp     if the tablet has timestamps, this parameter must not be null
     *
     *@return the created bundle if successful, null otherwise
     */
    public Bundle addBundle(Map<String,String> attr, String timestamp) { return addBundle(attr, timestamp, null); }
  
    /**
     * Add a new bundle.  Create the elements from the attribute map and the timestamps (if not null).
     * Note that for this method to succeed, all fields in this tablet must be satisfied.
     *
     *@param attr          attribute mapping
     *
     *@return the created bundle if successful, null otherwise
     */
    public Bundle addBundle(Map<String,String> attr)                   { return addBundle(attr, null,      null); }
  }
}
