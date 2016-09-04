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

import java.util.Iterator;
import java.util.Set;

import racetrack.framework.Bundle;

/**
 * Class used to expand the current records based on
 * other records around that timeframe.
 *
 * @author  D. Trimm
 * @version 1.0
 */
public class BundlesTimeExpander {
  /**
   * Minimum time to consider
   */
  long    ts0, 
  /**
   * Maximum time to consider
   */
          ts1, 
  /**
   * Expansion time to search around
   */
	  expansion_in_ms;
  /**
   * Giant array to quickly lookup if the time should be checked
   */
  boolean include[];

  /**
   * Create the expansion inclusion array by going through the bundles.
   *
   * @param seed             Bundles (records) to be used as a seed
   * @param ts0              Bounding timestamp (start)
   * @param ts1              Bounding timestamp (end)
   * @param expansion_in_ms  Expansion to consider for the inclusion array
   */
  public BundlesTimeExpander(Set<Bundle> seed, long ts0, long ts1, long expansion_in_ms) {
    this.ts0 = ts0; this.ts1 = ts1; this.expansion_in_ms = expansion_in_ms;
    include = new boolean[(int) (2 + (ts1 - ts0)/expansion_in_ms)];

    // Create an array and mark it up with timeframes to include
    Iterator<Bundle> it = seed.iterator();
    while (it.hasNext()) {
      Bundle bundle = it.next();
      if (bundle.hasDuration()) {
        int i0 = (int) ((bundle.ts0() - ts0)/expansion_in_ms),
	    i1 = (int) ((bundle.ts1() - ts1)/expansion_in_ms);
        if (i1 >= include.length) i1 = include.length-1;
        for (int i=i0;i<=i1;i++) if (i >= 0 && i < include.length) include[i] = true;
      } else if (bundle.hasTime()) {
        include[(int) ((bundle.ts0() - ts0)/expansion_in_ms)] = true;
      }
    }

    // Expand by one element
    for (int i=1;i<include.length;i++)    if (include[i]) include[i-1] = true;
    for (int i=include.length-2;i>=0;i--) if (include[i]) include[i+1] = true;
  }

  /**
   * Determine if a {@link Bundle} should be included based on the expansion.
   *
   * @param  bundle bundle to check
   * @return        flag if the bundle falls into the right timestamp include
   */
  public boolean  shouldInclude(Bundle bundle) {
    if (bundle.hasDuration()) {
      if (bundle.ts0() >= ts0 && bundle.ts0() <= ts1) {
        int i0 = (int) ((bundle.ts0() - ts0)/expansion_in_ms),
            i1 = (int) ((bundle.ts1() - ts1)/expansion_in_ms);
        if (i1 >= include.length) i1 = include.length - 1;
        for (int i=i0;i<=i1;i++) if (include[i]) return true;
      }
    } else if (bundle.hasTime()) {
      if (bundle.ts0() >= ts0 && bundle.ts0() <= ts1 && include[(int) ((bundle.ts0() - ts0)/expansion_in_ms)]) return true;
    }
    return false;
  }
}

