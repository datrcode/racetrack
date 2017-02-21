/* 

Copyright 2017 David Trimm

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

import java.util.Comparator;

/**
 * Simple comparator to order bundles by their timestamps.
 */
public class BundleTimeComparator implements Comparator<Bundle> {
  /**
   * Return a negative number if the first bundle is less than the second, a positive number if the
   * reverse is true, and a zero if their timestamps are equal.
   *
   *@param b0 first bundle
   *@param b1 second bundle
   *
   *@return comparison result for the bundle timestamps
   */
  public int compare(Bundle b0, Bundle b1) {
    if      (b0.ts0() < b1.ts0()) return -1;
    else if (b0.ts0() > b1.ts0()) return  1;
    else                          return  0;
  }
}
