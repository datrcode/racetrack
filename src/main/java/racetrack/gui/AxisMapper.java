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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class to map a varying number of long values to an axis.  Extracted from the
 * RTXYPanel class.
 *
 *@author  D. Trimm
 *@version 1.0
 */
class AxisMapper {
  /**
   *
   */
  public static final String LINEAR_SCALE_STR       = "Linear",
  /**
   *
   */
                             LOG_SCALE_STR          = "Log",
  /**
   *
   */
                             EQUAL_SCALE_STR        = "Equal",
  /**
   *
   */
                             SORT_STR               = "Sort",
  /**
   *
   */
                             SORT_REVERSE_STR       = "Sort (R)",
  /**
   *
   */
                             SORT_ITEMS_STR         = "Sort Rec",
  /**
   *
   */
                             SORT_ITEMS_REVERSE_STR = "Sort Rec (R)";
  /**
   *
   */
  private static final String      simple_scales[] = { LINEAR_SCALE_STR, LOG_SCALE_STR, EQUAL_SCALE_STR };
  /**
   *
   */
  private static final String      all_scales[]    = { LINEAR_SCALE_STR, LOG_SCALE_STR, EQUAL_SCALE_STR,
                                                       SORT_STR, SORT_REVERSE_STR,
					               SORT_ITEMS_STR, SORT_ITEMS_REVERSE_STR };

  /**
   *
   */
  public static final String[] allScales() { return all_scales; }

  /**
   *
   */
  public static final String[] simpleScales() { return simple_scales; }

  /**
   * Calculate the mapping (based on a method) for a global set of lookups into the screen space.
   *
   *@param method           string describing method
   *@param sorter           (to be?) sorted value of coodinates on the axis
   *@param int_to_sum       lookup to convert the coordinate on the axis to the sum of countercontexts
   *@param int_to_itemcount lookup to convert the coordiante on the axis to the number of items with that coordinate
   *@param min              minimum coordinate value
   *@param max              maximum coordinate value
   *
   *@return map converting the world coordinate to a uniform 0...1 value
   */
  public static Map<Long,Double> calculateMapping(String     method, 
                                                  List<Long> sorter,
						  long       min,
						  long       max) {
    return calculateMapping(method, sorter, null, null, min, max);
  }

  /**
   * Calculate the mapping (based on a method) for a global set of lookups into the screen space.
   *
   *@param method           string describing method
   *@param sorter           (to be?) sorted value of coodinates on the axis
   *@param int_to_sum       lookup to convert the coordinate on the axis to the sum of countercontexts
   *@param int_to_itemcount lookup to convert the coordiante on the axis to the number of items with that coordinate
   *@param min              minimum coordinate value
   *@param max              maximum coordinate value
   *
   *@return map converting the world coordinate to a uniform 0...1 value
   */
  public static Map<Long,Double> calculateMapping(String           method,
                                                  List<Long>       sorter,
				   		  Map<Long,Double> int_to_sum,
						  Map<Long,Double> int_to_itemcount,
						  long             min,
						  long             max) {
        // Check for degenerate cases
        if (sorter.size() <= 2) { 
	  Collections.sort(sorter);
	  Map<Long,Double> map = new HashMap<Long,Double>();
	  if      (sorter.size() == 1) { map.put(sorter.get(0), 0.5); }
	  else if (sorter.size() == 2) { map.put(sorter.get(0), 0.10); map.put(sorter.get(1), 0.90); }
	  return map;
	}

        // Do the regular conversion
	if        (method == null || method.equals(LINEAR_SCALE_STR)) { return linearScale(sorter, min, max);
	} else if (method.equals(LOG_SCALE_STR))                      { return logScale(sorter, min, max);
	} else if (method.equals(EQUAL_SCALE_STR))                    { return equalScale(sorter);
	} else if (method.equals(SORT_STR))                           { return sortOn(int_to_sum, false); // abc - broke here
	} else if (method.equals(SORT_REVERSE_STR))                   { return sortOn(int_to_sum, true);  // abc - broke here
	} else if (method.equals(SORT_ITEMS_STR))                     { return sortOn(int_to_itemcount, false);
	} else if (method.equals(SORT_ITEMS_REVERSE_STR))             { return sortOn(int_to_itemcount, true);
	} else throw new RuntimeException("Don't Understand Method \"" + method + "\"");
      }

      /**
       * Calculate the linear scaling for an array.
       *
       *@param sorter list of coordinate values
       *@param min    minimum value
       *@param max    maximum value
       *
       *@return map converting the world coordinate to a uniform 0...1 value
       */
      private static Map<Long,Double> linearScale(List<Long> sorter, long min, long max) {
        Map<Long,Double> mapping = new HashMap<Long,Double>();
        if (min == max) { mapping.put(min, 0.5); return mapping; }
        Iterator<Long> it = sorter.iterator();
	while (it.hasNext()) { long val = it.next(); mapping.put(val,((double) (val - min))/((double) (max - min))); }
	return mapping;
      }

      /**
       * Calculate the log scaling for an array of values.
       *
       *@param sorter list of coordinate values
       *@param max    maximum value
       *
       *@return map converting the world coordinate to a uniform 0...1 value
       */
      private static Map<Long,Double> logScale(List<Long> sorter, long min, long max) {
        if (min < 1) min = 1;

        Map<Long,Double> mapping = new HashMap<Long,Double>();
        Iterator<Long> it = sorter.iterator();
	while (it.hasNext()) { 
	  long val = it.next(); 
	  if (val <= 1) mapping.put(val,0.0);
	  else          mapping.put(val,(Math.log(val) - Math.log(min))/(Math.log(max) - Math.log(min)));
	}
	return mapping;
      }

      /**
       * Calculate equal scaling for an array of values.  Requires sorting the values.
       *
       *@param sorter list of coordinate values
       *
       *@return map converting the world coordinate to a uniform 0...1 value
       */
      private static Map<Long,Double> equalScale(List<Long> sorter) {
        Map<Long,Double> mapping = new HashMap<Long,Double>();
        if (sorter.size() == 1) { mapping.put(sorter.get(0),0.5); return mapping; }
	Collections.sort(sorter);
	for (int i=0;i<sorter.size();i++) mapping.put(sorter.get(i), ((double) i)/(sorter.size()-1));
	return mapping;
      }

      /**
       * Calculate the scale based on counts (sums) associated with each world coordinate.
       *
       *@param sum_map map from the coordinate to the sum
       *@param reverse flag to reverse the sort
       *
       *@return map converting the world coordinate to a uniform 0...1 value
       */
      private static Map<Long,Double> sortOn(Map<Long,Double> sum_map, boolean reverse) {
        Map<Long,Double> mapping = new HashMap<Long,Double>();
	if (reverse) {
	  List<RSorter> sorter = new ArrayList<RSorter>();
          Iterator<Long> it = sum_map.keySet().iterator();
	  while (it.hasNext()) { long i = it.next(); sorter.add(new RSorter(i,sum_map.get(i))); }
	  Collections.sort(sorter);
	  for (int i=0;i<sorter.size();i++) mapping.put(sorter.get(i).getIndex(), ((double) i)/(sorter.size()-1));
	} else       {
	  List<Sorter> sorter = new ArrayList<Sorter>();
          Iterator<Long> it = sum_map.keySet().iterator();
	  while (it.hasNext()) { long i = it.next(); sorter.add(new Sorter(i,sum_map.get(i))); }
	  Collections.sort(sorter);
	  for (int i=0;i<sorter.size();i++) mapping.put(sorter.get(i).getIndex(), ((double) i)/(sorter.size()-1));
	}
	return mapping;
      }

      /**
       * Class for (reverse) sorting an index to count mapping.
       */
      static class RSorter implements Comparable<RSorter> {
        long i; double d;
	public RSorter(long i, double d) { this.i = i; this.d = d; }
        public int compareTo(RSorter other) {
          if      (other.d < d) return -1; else if (other.d > d) return  1;
	  else if (other.i < i) return -1; else if (other.i > i) return  1;
	  else                  return  0;
        }
	public long getIndex() { return i; }
      }

      /**
       * Class for sorting an index to count mapping.
       */
      static class Sorter implements Comparable<Sorter> {
        long i; double d;
	public Sorter(long i, double d) { this.i = i; this.d = d; }
        public int compareTo(Sorter other) {
          if      (other.d < d) return  1; else if (other.d > d) return -1;
	  else if (other.i < i) return  1; else if (other.i > i) return -1;
	  else                  return  0;
        }
	public long getIndex() { return i; }
      }
}

