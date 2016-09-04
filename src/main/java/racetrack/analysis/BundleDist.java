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

package racetrack.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import racetrack.framework.Bundle;
import racetrack.framework.BundlesDT;
import racetrack.framework.BundlesG;
import racetrack.framework.Tablet;

/**
 * Used to create a distance function to compare individual elements
 * within a tablet (i.e., a bundle) to one another.
 *
 * @author  D. Trimm
 * @version 0.1
 */
public class BundleDist {
  /**
   * Specified tablet to pull the bundles from.
   */
  Tablet                 tablet;

  /**
   * Mapping of the bundles field name to the weight to provide to the differences
   * between two bundles.
   */
  Map<String,Double> fld_to_weight;

  /**
   * Intermediate holder for easy access to globals.  Can be derived from the tablet.
   */
  BundlesG               globals;

  /**
   * Mins and maxes for start and end timestamps and durations.
   */
  long                     t0_min  = -1L, 
                           t0_max, 
                           t1_min  = -1L, 
			   t1_max, 
                           dur_min = -1L, 
			   dur_max;

  /**
   *
   */
  Map<Integer,Boolean> bw_type = new HashMap<Integer,Boolean>();

  /**
   * String representations for time-based comparisons.
   */
  public static final String   TIMEBEG    = BundlesDT.DELIM + "TimeBeg" + BundlesDT.DELIM,
                               TIMEEND    = BundlesDT.DELIM + "TimeEnd" + BundlesDT.DELIM,
			       TIMEDUR    = BundlesDT.DELIM + "TimeDur" + BundlesDT.DELIM;
  /**
   * Array of the string representations for the time-based comparisons.
   */
  public static final String[] fld_extras = { TIMEBEG, TIMEEND, TIMEDUR };

  /**
   * Return the extra fields used to denote the time-based comparisons.
   *
   * @return An array of the specific strings used to denote time-based comparisons.
   */
  public static String[] getFieldExtras() { return fld_extras; }

  /**
   * Constructor which computes the mins and maxes for the bundles within a tablet.
   *
   * @param tablet        specific tablet to examine
   * @param fld_to_weight set of fields to use (keySet()) and their appropriate
   *                      weight to use during the distance function calculation.
   */
  public BundleDist(Tablet tablet, Map<String,Double> fld_to_weight) {
    // Save information
    this.tablet = tablet; this.fld_to_weight = fld_to_weight;

    // Get the globals
    globals = tablet.getBundles().getGlobals();

    // Find the ranges
    Iterator<Bundle> it_bun = tablet.bundleIterator();
    while (it_bun.hasNext()) {
      Bundle           bundle = it_bun.next();
      // Figure out this bundles contribution to the distance function
      Iterator<String> it_fld = fld_to_weight.keySet().iterator();
      while (it_fld.hasNext()) {
        String fld = it_fld.next(); 
	if        (fld.equals(TIMEBEG)) { // Begin Time
	  if (t0_min == -1L) { t0_min = t0_max = bundle.ts0();
	  } else             { if (t0_min > bundle.ts0()) t0_min = bundle.ts0();
	                       if (t0_max < bundle.ts0()) t0_max = bundle.ts0(); }
	} else if (fld.equals(TIMEEND)) { // End Time
	  if (t1_min == -1L) { t1_min = t1_max = bundle.ts1();
	  } else             { if (t1_min > bundle.ts1()) t1_min = bundle.ts1();
	                       if (t1_max < bundle.ts1()) t1_max = bundle.ts1(); }
	} else if (fld.equals(TIMEDUR)) { // Duration Time
	  long dur = bundle.ts1() - bundle.ts0();
	  if (dur_min == -1L) { dur_min = dur_max = dur;
	  } else              { if (dur_min > dur) dur_min = dur;
	                        if (dur_max < dur) dur_max = dur; }
	} else                                                                {
	  int fld_i = tablet.getBundles().getGlobals().fieldIndex(fld); BundlesDT.DT dt = globals.getFieldDataType(fld_i);
          if        (globals.isScalar(fld_i)) { // Find min and max
            if (bw_type.containsKey(fld_i) == false) {
	      bw_type.put(fld_i,false); 
            }
	  } else if (dt != null)              { // Switch the data type to find the compare method

	  } else                              { // Note that the field is binary...
            bw_type.put(fld_i,true);
	  }
        }
      }
    }
  }

  /**
   * Calculate the distance between two bundles.
   *
   * @param  b0 bundle one
   * @param  b1 bundle two
   * @return    distance between the two bundles
   */
  public double d(Bundle b0, Bundle b1) {
    return 0.0;
  }

  /**
   * Calculate the distance between a bundle and the mean of
   * a set of bundles.
   *
   * @param mean previously calculated mean of a set of bundles
   * @param b    bundle to determine distance between
   * @return     distance between the mean and the bundle
   */
  public double d(BundlesMean mean, Bundle b) {
    return 0.0;
  }

  /**
   * Calculate the mean of a set of bundles.  The mean is meant
   * to signify/represent the set for later comparisons against
   * the mean (or centroid).
   *
   * @param  set the set of bundles for the calculation
   * @return     an object representing the mean values based on the
   *             initial weights of the elements.
   */
  public BundlesMean mean(Set<Bundle> set) {
    return null;
  }
}

/**
 * Class representing the mean (average) of a set of bundles.  Used
 * for clustering methods that require a centroid to be calculated
 * through iterative clustering.
 *
 * @author  D. Trimm
 * @version 0.1
 */
class BundlesMean {
}

