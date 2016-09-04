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

import java.util.Iterator;
import java.util.Set;

/**
 * The overall framework class for aggregating bundles together.  Bundles
 * are most equivalent to rows in a structure database/csv and are held in
 * {@link Tablet} which are same-type structured data.  The framework class
 * also contains a {@link BundlesG} reference that contains the long-lived
 * reference lookup tables.
 *
 * @author  D. Trimm
 * @version 1.0
 */
public abstract class Bundles {
  /**
   * Globals reference variable
   */
  protected BundlesG        globals    = new BundlesG();

  /**
   * Initial time stamp for this data set
   */
  protected long          t0         = Long.MAX_VALUE,

  /**
   * Final time stamp for this data set
   */
                          t1         = 0L,

  /**
   * Final time stamp for this data set equivalent to last duration
   */
                          t1dur      = 0L;

  /**
   * Default constructor.
   */
  public Bundles() { }

  /**
   * Internal method to add a {@link Bundle} to this data set.  Needs to maintain the
   * first and last timestamp for the data.  Bundle should be added to the tablet
   * separately.
   *
   * @param bundle Bundle to add
   */
  public abstract void add(Bundle bundle);

  /**
   * Return a tablet object constructed with this bundles.
   *
   *@return tablet object
   */
  abstract Tablet createTablet();

  /**
   * Internal method to add a {@link Tablet} to this data set.  Tablets are used
   * to hold the same type of structured data.
   *
   * @param tablet Tablet to add
   */
  public abstract void add(Tablet tablet);

  /**
   * Find or create a tablet with the specified header labels.
   *
   *@param hdrs headers in the tablet-to-find
   *
   *@return existing tablet if it exists, otherwise a new table (already added to the bundles)
   */
  public abstract Tablet findOrCreateTablet(String hdrs[]);

  /**
   * Method to retrieve globals for the application.
   *
   * @return globals object reference
   */
  public BundlesG         getGlobals()                { return globals; }

  /**
   * Method to create an iterator through the tablets in this data set.
   *
   * @return Iterator over the tablets in this data set
   */
  public abstract Iterator<Tablet> tabletIterator();

  /**
   * Return the number of tablets.
   *
   * @return Number of tablets
   */
  public abstract int tabletCount();

  /**
   * Method to create an iterator over all of the bundles in this data set.
   *
   * @return Iterator over the bundles in this data set
   */
  public abstract Iterator<Bundle> bundleIterator();

  /**
   * Method to return the set of bundles in this data set.
   *
   * @return Set of the bundles
   */
  public abstract Set<Bundle>  bundleSet();

  /**
   * Returns the number of bundles in this data set.
   *
   * @return The number of bundles in this data set
   */
  public abstract int size();

  /**
   * Returns the earliest record's timestamp in the data set.
   *
   * @return The earliest record's timestamp in the data set
   */
  public long             ts0()                       { return t0; }

  /**
   * Returns the latest record's timestamp in the data set.
   *
   * @return The latest record's timestamp in the data set.
   */
  public long             ts1()                       { return t1; }

  /**
   * Return the last possible timestamp - this includes any duration events extending past t1.
   *
   *@return Last possible timestamp
   */
  public long             ts1dur()                    { return t1dur; }

  /**
   * Override the timestamps for the first/last heard.  Useful if the timeframe
   * needs to be set to certain limits.
   *
   * @param new_ts0 New initial time stamp value
   * @param new_ts1 New last time stamp value
   */
  public void             setTimestamps(long new_ts0, 
                                        long new_ts1,
                                        long new_ts1dur) { this.t0 = new_ts0; this.t1 = new_ts1; this.t1dur = new_ts1dur; }

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
  public abstract Bundles subset(Set<Bundle> set);

  /**
   * Clear the tags for the existing bundles (if they have the field).  Do not create the field if it's not already there.
   */
  public abstract void clearTags();

  /**
   * Add the specified tags to the specified records/bundles.  Create new space within bundles if necessary.
   *
   *@param visible records to apply the new tags to
   *@param tags    tags to add
   */
  public abstract boolean addTags(Bundles visible, String tags);

  /**
   * Replace the tag field with the specified tag.  Create new space within bundles if necessary.
   *
   *@param visible bundles to apply the change to
   *@param tags    tags for replacement
   *
   *@return true if a new field was added
   */
  public abstract boolean replaceTags(Bundles visible, String tags);

  /**
   * Replace just the specified type value tags in the visible bundles.
   *
   *@param to_replace tags to replace
   *
   *@return true if any fields were added
   */
  public abstract boolean replaceTypeValueTags(Bundles visible, String to_replace);

  /**
   * Remove the specified tags from the visible bundles.
   *
   *@param to_remove tags to remove
   */
  public abstract void removeTags(String to_remove);
}
