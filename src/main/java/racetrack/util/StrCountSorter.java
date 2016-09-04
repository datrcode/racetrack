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
package racetrack.util;

/**
 * Class to easily sort string:count lists.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class StrCountSorter implements Comparable<StrCountSorter> {
  /**
   * String associated with the count
   */
  String str; 
  /**
   * Count
   */
  long l;

  /**
   * Construct a new sort element.
   *
   *@param str string associated with the count
   *@param l   count
   */
  public StrCountSorter(String str, long l) { this.str = str; this.l = l; }

  /**
   * Compare against another StrCountSorter.  Comparison is done
   * against the counts.
   *
   *@param  other to compare against
   *
   *@return -1 if less than, 1 if greater than, otherwise the compare results for the strings
   */
  public int compareTo(StrCountSorter other) {
    if      (other.l < l) return -1;
    else if (other.l > l) return  1;
    else                  return str.compareTo(other.str);
  }

  /**
   * Return the associated string.
   *
   *@return associated string
   */
  public String toString() { return str; }

  /**
   * Return the count
   *
   *@return count
   */
  public long   count()    { return l;   }
}

