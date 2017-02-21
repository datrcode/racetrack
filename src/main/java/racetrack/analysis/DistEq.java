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

package racetrack.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesDT;
import racetrack.framework.BundlesG;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;

/**
 * Creates a distance function to compare different records (to include those from different tablets).
 *
 * @author  D. Trimm
 * @version 0.1
 */
public class DistEq {
  /**
   * Field to field weighting
   */
  private Map<String,Map<String,Double>> f2fw;

  /**
   * Fields for comparison -- fields.length = total number of comparison, fields[i][0,1] = actual field
   */
  String fields[][];

  /**
   * Weighting for each specific distance calculation
   */
  double weights[];

  /**
   * Constructor
   *
   *@param root_bunldes   root bundles for the data set -- required to make normalization work correctly
   *@param fld_to_fld_wgt field to field weight lookup... needs to be symmetric (i.e., every edge is represented twice)
   */
  public DistEq(Bundles root_bundles, Map<String,Map<String,Double>> fld_to_fld_wgt) {
    this.f2fw = fld_to_fld_wgt; 
    List<String>  field_as = new ArrayList<String>(),
                  field_bs = new ArrayList<String>();
    List<Double>  field_ws = new ArrayList<Double>();

    // Flatten out the key-value map into arrays
    Iterator<String> it = f2fw.keySet().iterator();
    while (it.hasNext()) {
      String fld0 = it.next();
      Iterator<String> it2 = f2fw.get(fld0).keySet().iterator();
      while (it2.hasNext()) {
        String fld1 = it2.next();
	double w    = f2fw.get(fld0).get(fld1);
	if        (fld0.compareTo(fld1) <= 0) { // Add to list
	  field_as.add(fld0); field_bs.add(fld1); field_ws.add(w);
	} else { } // Ignore... should have been added already
      }
    }

    // Make into an array
    fields  = new String[field_as.size()][2];
    weights = new double[field_ws.size()];
    for (int i=0;i<fields.length;i++) { fields[i][0] = field_as.get(i); fields[i][1] = field_bs.get(i); weights[i] = field_ws.get(i); }

    // For all fields, determine their type -- IP, Time, Scalar, EE
    BundlesG globals = root_bundles.getGlobals();

    Set<String> field_done = new HashSet<String>();
    for (int i=0;i<fields.length;i++) for (int j=0;j<fields[i].length;j++) {
      String            field     = fields[i][j]; if (field_done.contains(field) == false) {
        field_done.add(field);

	if (field.equals(KeyMaker.BY_YEAR_MONTH_DAY_HOUR_MIN_SEC_STR) || field.equals(KeyMaker.BY_YEAR_MONTH_DAY_HOUR_MIN_STR) ||
            field.equals(KeyMaker.BY_YEAR_MONTH_DAY_HOUR_STR)         || field.equals(KeyMaker.BY_YEAR_MONTH_DAY_STR)          ||
            field.equals(KeyMaker.BY_YEAR_MONTH_STR)                  || field.equals(KeyMaker.BY_YEAR_STR)                    ||
            field.equals(KeyMaker.BY_STRAIGHT_STR)) {
            time_fields.add(field);
        } else {
          Set<BundlesDT.DT> datatypes = globals.getFieldDataTypes(globals.fieldIndex(field));
          if        (datatypes == null) {

          } else if (datatypes.contains(BundlesDT.DT.IPv4) && (datatypes.size() == 1 || (datatypes.size() == 2 && datatypes.contains(BundlesDT.NOTSET)))) {
	    ipv4_fields.add(field);
          } else if (datatypes.contains(BundlesDT.DT.FLOAT) && (datatypes.size() == 1)) {
            float_fields.add(field);
          } else if (datatypes.contains(BundlesDT.DT.FLOAT) && (datatypes.size() == 2) && datatypes.contains(BundlesDT.DT.INTEGER)) {
            float_fields.add(field);
	  } else if (globals.isScalar(globals.fieldIndex(field))) {
	    scalar_fields.add(field);
	  }
        }
      }
    }

    // Find the maximum deltas for fields
    Iterator<String> it_field = field_done.iterator(); while (it_field.hasNext()) {
      //
      // Scalar maximums & minimums
      //
      String field = it_field.next(); if (isScalar(field)) {
        int max = Integer.MIN_VALUE, min = Integer.MAX_VALUE; 
        Iterator<Tablet> it_tab = root_bundles.tabletIterator(); while (it_tab.hasNext()) {
	  Tablet tablet = it_tab.next(); if (KeyMaker.tabletCompletesBlank(tablet, field)) {
	    KeyMaker km = new KeyMaker(tablet, field);
	    Iterator<Bundle> it_bun = tablet.bundleIterator(); while (it_bun.hasNext()) {
	      Bundle bundle = it_bun.next();
	      int ints[] = km.intKeys(bundle); if (ints != null && ints.length > 0) {
	        for (int i=0;i<ints.length;i++) {
		  if (ints[i] > max) max = ints[i];
		  if (ints[i] < min) min = ints[i];
		}
	      }
	    }
	  }
	}
	scalar_mins.put(field, min);
	scalar_maxs.put(field, max);

      //
      // Float maximums & minimums
      //
      } else if (isFloat(field)) {
        float max = Float.NEGATIVE_INFINITY, min = Float.POSITIVE_INFINITY;
	Iterator<Tablet> it_tab = root_bundles.tabletIterator(); while (it_tab.hasNext()) {
	  Tablet tablet = it_tab.next(); if (KeyMaker.tabletCompletesBlank(tablet, field)) {
	    KeyMaker km = new KeyMaker(tablet, field);
	    Iterator<Bundle> it_bun = tablet.bundleIterator(); while (it_bun.hasNext()) {
	      Bundle bundle = it_bun.next();
              String strs[] = km.stringKeys(bundle); if (strs != null && strs.length > 0) {
	        for (int i=0;i<strs.length;i++) {
		  float f = Float.parseFloat(strs[i]);
		  if (f > max) max = f;
		  if (f < min) min = f;
		}
	      }
	    }
	  }
	}

	float_mins.put(field, min);
	float_maxs.put(field, max);
      }
    }

    // Capture time
    ts_min = root_bundles.ts0();
    ts_max = root_bundles.ts1();
  }

  /**
   * Minimum values for a specific scalar field - used for normalization
   */
  Map<String,Integer> scalar_mins = new HashMap<String,Integer>(),

  /**
   * Maximum values for a specific scalar field - used for normalization
   */
                      scalar_maxs = new HashMap<String,Integer>();

  /**
   * Minimum values for a specific float field
   */
  Map<String,Float>   float_mins  = new HashMap<String,Float>(),
  
  /**
   * Maximum values for a specific float field
   */
                      float_maxs  = new HashMap<String,Float>();

  /**
   * Set of float fields
   */
  Set<String>         float_fields = new HashSet<String>();

  /**
   * Is a field a float field?
   *
   *@param field field to compare
   *
   *@return true if field is a float field
   */
  public boolean isFloat(String field) { return float_fields.contains(field); }

  /**
   * Minimum timestamp (for normalization)
   */
  long ts_min,

  /**
   * Maximum timestamp (for normalization)
   */
       ts_max;

  /**
   * Categorization of fields - Time fields
   */
  Set<String> time_fields = new HashSet<String>();

  /**
   * Is a field a time field -- time in this case is long/epoch millis?
   *
   *@param field field to compare
   *
   *@return true if field is a long/epoch millis field
   */
  public boolean isTime(String field) { return time_fields.contains(field); }

  /**
   * Categorization of fields - IPv4 fields
   */
  Set<String> ipv4_fields = new HashSet<String>();

  /**
   * Is a field an IPv4 address field?
   *
   *@param field field to compare
   *
   *@return true if field is an IPv4 field
   */
  public boolean isIPv4(String field) { return ipv4_fields.contains(field); }

  /**
   * Categorization of fields - scalar fields
   */
  Set<String> scalar_fields = new HashSet<String>();

  /**
   * Is a field a scalar field?
   *
   *@param field field to compare
   *
   *@return true if field is a scalar value
   */
  public boolean isScalar(String field) { return scalar_fields.contains(field); }

  /**
   * Determine if a bundle has a field.
   *
   *@param bundle record for calculation
   *@param field  field of interest
   *
   *@return true if the bundle has that field
   */
  public boolean hasField(Bundle bundle, String field) { return KeyMaker.tabletCompletesBlank(bundle.getTablet(), field); }

  /**
   * KeyMaker map
   */
  Map<Tablet, Map<String, KeyMaker>> km_map = new HashMap<Tablet, Map<String,KeyMaker>>();

  /**
   * Make the integer keys for the specified bundle
   *
   *@param bundle record for calculation
   *@param field  field to extract integer values from
   *
   *@return integer keys from the bundle's specific field
   */
  public int[] intKeys(Bundle bundle, String field) {
    if (km_map.                        containsKey(bundle.getTablet()) == false) km_map.put(bundle.getTablet(), new HashMap<String,KeyMaker>());
    if (km_map.get(bundle.getTablet()).containsKey(field)              == false) km_map.get(bundle.getTablet()).put(field, new KeyMaker(bundle.getTablet(), field));
    return km_map.get(bundle.getTablet()).get(field).intKeys(bundle);
  }

  /**
   * Compare two values based on the fields that they were extracted from...  complicated because the field type indicates how
   * the comparison should occur...  Basic comparisons include scalar, categories, IP's, and time.
   */
  public double simpleDist(int a_val, String a_fld, Bundle a, int b_val, String b_fld, Bundle b) {
    //
    // Same field... therefore same data types
    //
    if (a_fld.equals(b_fld)) {
      if        (isIPv4(a_fld))   { return ipCompare(a_val,b_val);
      } else if (isTime(a_fld))   { return timeCompare(a.ts0(), b.ts0());
      } else if (isScalar(a_fld)) { int abs = (int) Math.abs(a_val - b_val);
                                    return ((double) abs) / (scalar_maxs.get(a_fld) - scalar_mins.get(a_fld));
      } else if (isFloat(a_fld))  { double abs = Math.abs(Float.intBitsToFloat(a_val) - Float.intBitsToFloat(b_val));
                                    return abs / (float_maxs.get(a_fld) - float_mins.get(a_fld));
      } else { if (a_val == b_val) return 0.0; else return 1.0; }

    //
    // Different fields... may have different data types
    //
    } else                   {
      if        (isIPv4(a_fld)   && isIPv4(b_fld))   { return ipCompare(a_val,b_val);
      } else if (isTime(a_fld)   && isTime(b_fld))   { return timeCompare(a.ts0(), b.ts0());
      } else if (isScalar(a_fld) && isScalar(b_fld)) { int abs = (int) Math.abs(a_val - b_val);
                                                       int max = scalar_maxs.get(a_fld) > scalar_maxs.get(b_fld) ? scalar_maxs.get(a_fld) : scalar_maxs.get(b_fld);
                                                       int min = scalar_mins.get(a_fld) < scalar_mins.get(b_fld) ? scalar_mins.get(a_fld) : scalar_mins.get(b_fld);
                                                       return ((double) abs) / (max - min);
      } else if (isScalar(a_fld) && isFloat (b_fld)) { double abs = Math.abs(a_val                       - Float.intBitsToFloat(b_val));
                                                       double max = scalar_maxs.get(a_fld) > float_maxs.get(b_fld) ? scalar_maxs.get(a_fld) : float_maxs.get(b_fld);
						       double min = scalar_mins.get(a_fld) < float_mins.get(b_fld) ? scalar_mins.get(a_fld) : float_mins.get(b_fld);
						       return ((double) abs) / (max - min);
      } else if (isFloat (a_fld) && isScalar(b_fld)) { double abs = Math.abs(Float.intBitsToFloat(a_val) - b_val);
                                                       double max = float_maxs.get(a_fld) > scalar_maxs.get(b_fld) ? float_maxs.get(a_fld) : scalar_maxs.get(b_fld);
						       double min = float_mins.get(a_fld) < scalar_mins.get(b_fld) ? float_mins.get(a_fld) : scalar_mins.get(b_fld);
						       return ((double) abs) / (max - min);
      } else if (isFloat (a_fld) && isFloat (b_fld)) { double abs = Math.abs(Float.intBitsToFloat(a_val) - Float.intBitsToFloat(b_val));
                                                       double max = float_maxs.get(a_fld) > float_maxs.get(b_fld) ? float_maxs.get(a_fld) : float_maxs.get(b_fld);
						       double min = float_mins.get(a_fld) < float_mins.get(b_fld) ? float_mins.get(a_fld) : float_mins.get(b_fld);
						       return ((double) abs) / (max - min);
      } else { if (a_fld == b_fld) return 0.0; else return 1.0; }
    }
  }

  /**
   * Compare two IP addresses (that are represented as int's)... difficult part is that it's a circular comparison...
   *
   *@param ip0 first ip address
   *@param ip1 second ip address
   *
   *@return normalized distance, i.e., between 0.0 and 1.0
   */
  public double ipCompare(int ip0, int ip1) {
    int straight = (int) Math.abs(ip0 - ip1);
    int wrapped;
    if (ip0 > ip1) { wrapped = (Integer.MAX_VALUE - ip0) + (ip1 - Integer.MIN_VALUE);
    } else         { wrapped = (Integer.MAX_VALUE - ip1) + (ip0 - Integer.MIN_VALUE); }

    int smaller = straight < wrapped ? straight : wrapped;

    return ((double) smaller)/(Integer.MAX_VALUE); // Only use half the space for divisor
  }

  /**
   * Compare two timestamps.
   *
   *@param t0 timestamp 0
   *@param t1 timestamp 1
   *
   *@return normalized distance, i.e., between 0.0 and 1.0
   */
  public double timeCompare(long t0, long t1) {
    long abs = (long) Math.abs(t0 - t1);
    return ((double) abs) / (ts_max - ts_min);
  }

  /**
   * Compute the distance between bundle a and bundle b...  probably make this stateless and require the calling class
   * to manage cacheing...
   *
   *@param a first bundle
   *@param b second bundle
   *
   *@return distance measure... normalized?
   */
  public double d(Bundle a, Bundle b) {
    double dist[] = new double[weights.length]; int array_of_1[] = new int[1];

    //
    // Go through each distance requirement -- if the bundles can satisfy, do the comparison
    //
    for (int i=0;i<dist.length;i++) {
      int as[] = null, bs[] = null; String a_fld = null, b_fld = null;
      // Compute the bundle values for this specific comparison
      if        (time_fields.contains(fields[i][0]) && a.hasTime() &&  time_fields.contains(fields[i][1]) && b.hasTime()) {
        as = bs = array_of_1; a_fld = fields[i][0]; b_fld = fields[i][1]; // Just force it to call the simpleDist function
      } else if (time_fields.contains(fields[i][0]) == false && time_fields.contains(fields[i][1]) == false) {
        if        (hasField(a, fields[i][0]) && hasField(b, fields[i][1])) { as = intKeys(a, fields[i][0]); bs = intKeys(b, fields[i][1]); a_fld = fields[i][0]; b_fld = fields[i][1];
        } else if (hasField(a, fields[i][1]) && hasField(b, fields[i][0])) { as = intKeys(a, fields[i][1]); bs = intKeys(b, fields[i][0]); a_fld = fields[i][1]; b_fld = fields[i][0];
        } else dist[i] = Double.NaN; // mark it as non-valid
     } else dist[i] = Double.NaN;

      // Calculate the distance from the bundle values
      if (Double.isNaN(dist[i]) == false) {
        if (as == null || as.length == 0 || bs == null || bs.length == 0) { dist[i] = 1.0; } else {
	  //
	  // Simple comparison
	  //
          if        (as.length == 1 && bs.length == 1) { dist[i] = simpleDist(as[0], a_fld, a, bs[0], b_fld, b);

	  //
	  // Choose the best distance if there are multiples to compare
	  //
	  } else if (as.length >  1 && bs.length == 1) { dist[i] = 1.0; for (int j=0;j<as.length;j++) { double d = simpleDist(as[j], a_fld, a, bs[0], b_fld, b); if (d < dist[i]) dist[i] = d; }
	  } else if (as.length == 1 && bs.length >  1) { dist[i] = 1.0; for (int j=0;j<bs.length;j++) { double d = simpleDist(as[0], a_fld, a, bs[j], b_fld, b); if (d < dist[i]) dist[i] = d; }
	  } else                                       { dist[i] = 1.0; for (int j=0;j<as.length;j++) for (int k=0;k<bs.length;k++) { double d = simpleDist(as[j], a_fld, a, bs[k], b_fld, b); if (d < dist[i]) dist[i] = d; }
	  }
	}
      }
    }

    //
    // Compile the valid distances together for a final distance
    //
    int samples = 0; double sum = 0.0;
    for (int i=0;i<dist.length;i++) {
      // System.out.print(dist[i] + " ");
      if (Double.isNaN(dist[i]) == false) {
        sum += dist[i] * weights[i]; samples++;
      }
    }
    // System.out.println();
    if (samples > 0) return sum/samples; else return 1.0;
  }

  /**
   * Compute distance between a bundle and a bundle center.
   *
   *@param a bundle
   *@param c bundle center
   *
   *@return distance on a 0.0 to 1.0
   */
  public double d(Bundle a, BundleCenter c) { return c.d(a); }

  /**
   * Create an object that represents the center of the specified bundles.
   *
   *@param set bundles for center calculation
   *
   *@return bundle center
   */
  public BundleCenter createBundleCenter(Set<Bundle> set) { return new BundleCenter(set); }

  /**
   * Inner class representing a center of a set of bundles.
   */
  public class BundleCenter {
    /**
     * Average for the time value (only valid after construction)
     */
    long                              time_avg       = 0L;

    /**
     * Number of samples used for the time value (only valid/used during construction)
     */
    int                               time_samples   = 0;

    /**
     * Average for the scalar values (only valid after construction)
     */
    Map<String,Double>                scalar_avgs    = new HashMap<String,Double>();

    /**
     * Number of samples used for the scalar values (only valid/used during construction)
     */
    Map<String,Integer>               scalar_samples = new HashMap<String,Integer>();

    /**
     * Average for the float values (only valid after construction)
     */
    Map<String,Double>                float_avgs     = new HashMap<String,Double>();

    /**
     * Number of samples used for the float values (only valid/used during construction)
     */
    Map<String,Integer>               float_samples  = new HashMap<String,Integer>();

    /**
     * Number of counts for each categorical value
     */
    Map<String,Map<Integer,Integer>>  cats_counts    = new HashMap<String,Map<Integer,Integer>>();

    /**
     * Total for the counts in that specified categorical value
     */
    Map<String,Integer>               cats_totals    = new HashMap<String,Integer>();

    /**
     * Construct the bundle center
     *
     *@param set bundle set
     */
    public BundleCenter(Set<Bundle> set) {
      // Initialize the data structures
      for (int i=0;i<fields.length;i++) for (int j=0;j<fields[i].length;j++) {
        String field = fields[i][j];
        if        (time_fields.contains(field))   { // do nothing
	} else if (scalar_fields.contains(field)) { if (scalar_avgs.containsKey(field) == false) { scalar_avgs.put(field, 0.0); scalar_samples.put(field, 0); }
	} else if (ipv4_fields.contains(field))   { if (cats_counts.containsKey(field) == false) { cats_counts.put(field, new HashMap<Integer,Integer>());    }
	} else if (float_fields.contains(field))  { if (float_avgs. containsKey(field) == false) { float_avgs .put(field, 0.0); float_samples .put(field, 0); }
	} else                                    { if (cats_counts.containsKey(field) == false) { cats_counts.put(field, new HashMap<Integer,Integer>());    } }
      }

      // Go through the bundles
      Iterator<Bundle> it = set.iterator(); while (it.hasNext()) {
        Bundle bundle = it.next(); // /* DEBUG */ System.err.println("  " + bundle);
        Set<String> done = new HashSet<String>(); 
        for (int i=0;i<fields.length;i++) for (int j=0;j<fields[i].length;j++) {
	  String field = fields[i][j]; if (done.contains(field)) continue; done.add(field);
	  if        (time_fields.  contains(field) && bundle.hasTime()) { sumTime(bundle);        
	  } else if (scalar_fields.contains(field))                     { sumScalar(bundle,field);
	  } else if (ipv4_fields.  contains(field))                     { addCategorical(bundle,field);
	  } else if (float_fields. contains(field))                     { sumFloat(bundle,field);
	  } else                                                        { addCategorical(bundle,field); }
	}
      }

      // Calculate averages for time and scalars (if they had samples)
      if (time_samples > 0) time_avg = time_avg / time_samples;
      Iterator<String> it_str = scalar_avgs.keySet().iterator(); while (it_str.hasNext()) {
        String field = it_str.next();
	if (scalar_samples.get(field) > 0) scalar_avgs.put(field, scalar_avgs.get(field) / scalar_samples.get(field));
      }

      // Calculate averages for floats (if they had samples)
      it_str = float_avgs.keySet().iterator(); while (it_str.hasNext()) {
        String field = it_str.next();
	if (float_samples.get(field) > 0) float_avgs.put(field, float_avgs.get(field) / float_samples.get(field));
      }

      // Calculate the categorical totals
      it_str = cats_counts.keySet().iterator(); while (it_str.hasNext()) {
        String field = it_str.next();
	int    sum   = 0;
	Iterator<Integer> it_int = cats_counts.get(field).keySet().iterator(); while (it_int.hasNext()) {
	  sum += cats_counts.get(field).get(it_int.next());
	}
	cats_totals.put(field, sum);
      }
      // /** DEBUG **/ it_str = scalar_avgs.keySet().iterator(); while (it_str.hasNext()) { String field = it_str.next(); System.err.println("    " + field + " | avg = " + scalar_avgs.get(field)); }
    }

    /**
     * Add the timestamp to the accumulated times.
     *
     *@param bundle bundle to process
     */
    protected void sumTime(Bundle bundle) { time_avg += bundle.ts0(); time_samples++; }

    /**
     * Add this bundles ints to the statistics.
     *@param bundle bundle to process
     */
    protected void sumScalar(Bundle bundle, String field) {
      int ints[] = intKeys(bundle, field);
      for (int i=0;i<ints.length;i++) {
        scalar_avgs.   put(field, scalar_avgs.   get(field) + ints[i]);
	scalar_samples.put(field, scalar_samples.get(field) + 1);
      }
    }

    /**
     *  Add this bundles floats to the statistics.
     *@param bundle bundle to process
     */
    protected void sumFloat(Bundle bundle, String field) {
      int ints[] = intKeys(bundle, field);
      for (int i=0;i<ints.length;i++) {
        float_avgs.   put(field, float_avgs.get(field) + Float.intBitsToFloat(ints[i]));
	float_samples.put(field, float_samples.get(field) + 1);
      }
    }

    /**
     *
     */
    protected void addCategorical(Bundle bundle, String field) {
      Map<Integer,Integer> counts = cats_counts.get(field);
      int ints[] = intKeys(bundle, field);
      for (int i=0;i<ints.length;i++) {
        if (counts.containsKey(ints[i]) == false) counts.put(ints[i], 1);
	else                                      counts.put(ints[i], counts.get(ints[i]) + 1);
      }
    }

    /**
     * Compute distance to a bundle from this bundler center.
     *
     *@param a bundle
     *
     *@return distance on a 0.0 to 1.0 scale
     */
    public double d(Bundle a) {
      double dist[] = new double[weights.length];
      for (int i=0;i<dist.length;i++) {
        // Set it to invalid (until proven otherwise)
        dist[i] = Double.NaN;

	// Extract the fields
        String a_fld = fields[i][0], c_fld = fields[i][1];

	// Look for how to match
	if      (time_fields.  contains   (a_fld) && a.hasTime()        && time_fields.  contains   (c_fld) && time_samples                              > 0) dist[i] = timeCompare(a);
	else if (scalar_fields.contains   (a_fld) && hasField(a, a_fld) && scalar_fields.contains   (c_fld) && scalar_samples.get(c_fld)                 > 0) dist[i] = scalarCompare(a, a_fld, c_fld);
	else if (ipv4_fields.  contains   (a_fld) && hasField(a, a_fld) && ipv4_fields.  contains   (c_fld) && cats_counts.   get(c_fld).keySet().size() > 0) dist[i] = categoricalCompare(a, a_fld, c_fld);
	else if (cats_counts.  containsKey(a_fld) && hasField(a, a_fld) && cats_counts.  containsKey(c_fld) && cats_counts.   get(c_fld).keySet().size() > 0) dist[i] = categoricalCompare(a, a_fld, c_fld); // may accidentally compare an ipv4 field with a categorical field...
	else if (float_fields. contains   (a_fld) && hasField(a, a_fld) && float_fields. contains   (c_fld) && float_samples. get(c_fld)                 > 0) dist[i] = floatCompare(a, a_fld, c_fld);
      }

      //
      // Compile the valid distances together for a final distance
      //
      int samples = 0; double sum = 0.0;
      for (int i=0;i<dist.length;i++) {
        // System.out.print(dist[i] + " ");
        if (Double.isNaN(dist[i]) == false) {
          sum += dist[i] * (weights[i] * weights[i]); samples++;
        }
      }
      // System.out.println();
      if (samples > 0) return Math.sqrt(sum)/samples; else return 1.0;
    }

    protected double timeCompare(Bundle a) {
      long abs = (long) Math.abs(a.ts0() - time_avg);
      return ((double) abs) / (ts_max - ts_min);
    }

    protected double scalarCompare(Bundle a, String a_fld, String c_fld) {
      int ints[] = intKeys(a, a_fld);
      if (ints != null && ints.length > 0) {
        double abs = Math.abs(ints[0] - scalar_avgs.get(c_fld));
        return abs / (scalar_maxs.get(a_fld) - scalar_mins.get(a_fld));
      } else return Double.NaN;
    }

    protected double floatCompare(Bundle a, String a_fld, String c_fld) {
      int ints[] = intKeys(a, a_fld);
      if (ints != null && ints.length > 0) {
        double abs = Math.abs(Float.intBitsToFloat(ints[0]) - float_avgs.get(c_fld));
        return abs / (float_maxs.get(a_fld) - scalar_mins.get(a_fld));
      } else return Double.NaN;
    }

    protected double categoricalCompare(Bundle a, String a_fld, String c_fld) {
      int ints[] = intKeys(a, a_fld);
      if (ints == null || ints.length == 0) return Double.NaN;
      if (cats_counts.get(c_fld).keySet().contains(ints[0]) == false) return 1.0;
      return 1.0 - ((double) cats_counts.get(c_fld).get(ints[0])) / cats_totals.get(c_fld);
    }
  }
}

