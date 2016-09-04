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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import racetrack.util.Utils;

/**
 * Provides a logical grouping for bundles (records) that have the same
 * exact fields.  For performance, tablets enable other methods to
 * apply operations (additions, set operations) without the need to
 * continuously check to see if the data types are present or the same.
 *
 * @author  D. Trimm
 * @version 1.0
 */
public abstract class Tablet {
  /**
   * Add a bundle (record) to this tablet.  Should probably only be
   * called by classes within this file since many assumptions are made
   * about the bundle's data fields.
   *
   *@param bundle bundle to add
   */
  // abstract void add(Bundle bundle);

  /**
   * Create a bundle that can be added to this tablet.
   *
   *@return bundle that works with this tablet
   */
  // abstract Bundle createBundle();

  /**
   * Set a field mapping index to convert from global indices to local indices.
   * For Internal Use (This File) Only
   *
   * @param fld_i   global index
   * @param local_i local index
   */
  abstract void setFieldIndex    (int fld_i, int local_i);

  /**
   * Flag to indicate that the tablet's records (bundles) have timestamps.
   */
  boolean has_timestamps = false,

  /**
   * Flag to indicate that the tablet's records (bundles) have durations.
   */
          has_durations  = false;

  /**
   * Set the timestamps flag.  Should only be used by classes and methods within this package.
   */
  void setHasTimeStamps ()                       { has_timestamps = true;   }

  /**
   * Set the durations flag.  Should only be used by classes and methods within this package.
   */
  void setHasDurations()                         { has_durations = true;    }

  /**
   * Add a field to this tablet and all associated records (bundles).
   *
   *@param fld field to add
   */
  abstract void addField(String fld);

  /**
   * For the records, set the specific field to the specific value.
   *
   *@param fld field to set
   *@param val value to set
   */
  abstract void setField(String fld, String val);

  /**
   * Return the overarching datastructure for this tablet.
   * 
   * @return Bundles
   */
  public abstract Bundles getBundles();

  /**
   * Return the set collection for the bundles.  Useful for other set operations in bulk.
   *
   *@return set of bundle/records
   */
  public abstract Set<Bundle>  bundleSet();

  /**
   * Provide an interator over the individual bundles/records within this tablet.
   *
   * @return iterator over the records/bundles.
   */
  public abstract Iterator<Bundle> bundleIterator();

  /**
   * Remove the specified fields from this class.
   *
   *@param flds fields to remove
   */
  abstract void removeFields(Set<String> flds);

  /**
   * Returns if this tablet contains timestamped records.
   *
   * @return flag if timestamps are present
   */
  public boolean          hasTimeStamps()     { return has_timestamps; }

  /**
   * Returns if this table contains records with durations.
   *
   *@return flag if durations are present
   */
  public boolean          hasDurations()      { return has_durations; }

  /**
   * Returns if this tablet contains data matching the global field index.
   *
   * @param  fld_i global field index
   * @return       flag indicating that the data is present
   */
  public abstract boolean hasField(int fld_i);

  /**
   * Returns the number of records / bundles within this tablet.
   *
   * @return the number of records
   */
  public abstract int size();

  /**
   * Return the mapping array for this tablet.  Probably need to get rid of this -
   * potentially dangerous if another method modifies the return values...
   *
   * @return array mapping global to local indices
   */
  public abstract int[] getFields();

  /**
   * Return a string that can be used to represent data stored out of this tablet set.  The
   * header should be the same even if the order of the field headers differed for the input.
   * That is, the file header result should be used to compare different tablets to see
   * if they contain exactly the same headers.
   *
   * @return comma-separated string for the field headers in this tablet.
   */
  public abstract String fileHeader();

  /**
   * Save the tablet to a printstream (such as a file).
   *
   * @param out            printstream to save data to
   * @param include_header flag indicating if the header should be printed first (multiple
   *                       tablets may be saved together in a file - if they tablets are
   *                       header-wise the same, they get merged together via this flag)
   */
  public void save(PrintStream out, boolean include_header) {
    if (size() > 0) { 
      if (include_header) out.println(fileHeader()); 
      Iterator<Bundle> it = bundleSet().iterator(); while (it.hasNext()) it.next().save(out); 
    }
  }

  /**
   * Add a new bundle.  Create the elements from the attribute map and the timestamps (if not null).
   * Note that for this method to succeed, all fields in this tablet must be satisfied.
   *
   *@param attr          attribute mapping
   *@param timestamp     if the tablet has timestamps, this parameter must not be null
   *@param timestamp_end if the tablet has timestamps and durations, this parameter must not be null
   *
   *@return The created bundle if successful, null otherwise
   */
  public abstract Bundle addBundle(Map<String,String> attr, String timestamp, String timestamp_end);

  /**
   * Add a new bundle.  Create the elements from the attribute map and the timestamps (if not null).
   * Note that for this method to succeed, all fields in this tablet must be satisfied.
   *
   *@param attr          attribute mapping
   *@param timestamp     if the tablet has timestamps, this parameter must not be null
   *
   *@return The created bundle if successful, null otherwise
   */
  public abstract Bundle addBundle(Map<String,String> attr, String timestamp);

  /**
   * Add a new bundle.  Create the elements from the attribute map and the timestamps (if not null).
   * Note that for this method to succeed, all fields in this tablet must be satisfied.
   *
   *@param attr          attribute mapping
   *
   *@return The created bundle if successful, null otherwise
   */
  public abstract Bundle addBundle(Map<String,String> attr);
}

