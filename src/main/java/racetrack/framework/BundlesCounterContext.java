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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import racetrack.gui.RT;
import racetrack.util.Utils;
import racetrack.visualization.RTColorManager;

/**
 * Class used to accumulate specific fields from the elements in the bundles.
 * This class forms the cornerstone of most, if not all, of the visual elements
 * by providing an abstraction for how to add elements together (either
 * arithmetically or using set operation).  Furthermore, it provides methods
 * to understand sub-coloring of the elements and normaling the counts so that
 * each GUI component can be abstracted away from the underlying data.
 * 
 * @author  D. Trimm
 * @version 1.0
 */
public class BundlesCounterContext {
  /**
   * Overall complete set of data elements over which to run the counters.
   */
  Bundles                           bundles; 

  /**
   * The application data field to use for counting by.
   */
  String                            count_by; 

  /**
   * The application data field to use for coloring by.
   */
  String                            color_by; 

  /**
   * A subclass that delineates whether the counting is done arithmetically or using
   * set-based operations.  Most of the work goes on in this class but the overall
   * BundlesCounterContext provides the abstraction so that the caller does not need
   * to know the differences.
   */
  Counter                           counter; 
  
  /**
   * Lookup table to compute which {@link KeyMaker} is used for a tablet.
   */
  Map<Tablet,KeyMaker>          color_tablet_lu   = new HashMap<Tablet,KeyMaker>();

  /**
   * Lookup table to convert the binning string (typically specific to the type
   * of view that the GUI is creating) with the set of corresponding bundles.  A
   * bundle can often exist in more than one bin.
   */
  Map<String,Set<Bundle>>   bin_to_bundle_set = new HashMap<String,Set<Bundle>>();

  /**
   * Subclass used to perform the actual counting dependent on how the data should be
   * added together.  The two default implementations assume either arithmetic operations
   * or set-based operations.
   */
  interface Counter {
    /**
     * Add the bundles contributions to a specific GUI-related binning string.
     *
     * @param  bundle contribution to add
     * @param  bin    GUI-specific binning string
     * @return        specific contribution of this bundle
     */
    public double count(Bundle bundle, String bin);

    /**
     * Add the bundle's contribution to a specific GUI-related binning string dividing
     * the contribution over a number of bins.  It is the GUI's responsibility to call
     * this method for each bin that the bundle needs to contribute to.
     *
     * @param  bundle contribution to add
     * @param  bin    GUI-specific binning string
     * @param  div    dividing factor for this contribution
     * @return        specific contribution of this bundle
     */
    public double count(Bundle bundle, String bin, double div);

    /**
     * Determine the overall total for a specific bin.
     *
     * @param  bin GUI-specific binning string
     * @return     total for all of the bundles contributing to this bin
     */
    public double total(String bin);

    /**
     * Determine the overall total for a bin based on the sub-coloring
     * for the bin.  It would seem that the value should have been equal
     * to the overall total but issues related to a single bundle equating to 
     * multiple colors cause the overall total() method to fail.
     *
     * @param  bin GUI-specific binning string
     * @return     total for all of the bundles contributing to this bin based
     *             on the sum of the coloring
     */
    public double binColorTotal(String bin);

    /**
     * Determine the total for a specific bin with the specific subcolor.
     *
     * @param   bin GUI-specific binning string
     * @param  cbin sub-binning method for colorizing renderings
     * @return      total for all of the bundles contributing to this bin with
     *              the specific color bin
     */
    public double total(String bin, String cbin);

    /**
     * Determine the overall contribution for the colorized bins irregardless
     * of the primary binning methods.  This is useful for ordering the colors
     * in bar charts so that the base of each bar has the same coloring.
     *
     * @param  cbin sub-binning method for colorizing renderings
     * @return      total for all of the bundles contributing to this color
     */
    public double totalColor(String cbin);

    /**
     * Determine the largest total for all of the bins.  Useful for calculating
     * normals and labeling axes.
     *
     * @return      maximum for all of the bins
     */
    public double totalMaximum();

    /**
     * Determine the largest total for all of the bins based on the color sum.
     *
     * @return      largest sum across bins based on the colorized contributions
     */
    public double totalColorMaximum();

    /**
     * Determine the largets total for all of the bins for the specified color.
     *
     * @param  cbin sub-binning colorization method to calculate across
     * @return      largest sum across the bins for that specific colorization
     */
    public double totalColorMaximum(String cbin);

    /**
     * Calculate the normalized size of this bin.
     *
     * @param  bin  GUI-specific binning string
     * @return      a normalized calculation for this specific bin
     */
    public double totalNormalized(String bin);

    /**
     * Determine the color for this bin.  If multiple colors exists within the bin,
     * return the multi-set color.
     *
     * @param  bin  GUI-specific binning string
     * @return      Sole color for this bin, or, the multi-set color if multiple
     *              colors exist.
     */
    public Color  binColor(String bin);

    /**
     * Accumulate one bin into another.  This is only useful for cumulative distribution
     * functions such as an aggregate time view.
     *
     * @param from  source bin
     * @param to    destination bin
     */
    public void   accumulate(String from, String into);

    /**
     * Return an iterator that traverses all of the bins in the counter context.
     *
     * @return an iterator that goes through each and every bin
     */
    public Iterator<String> binIterator();

    /**
     * Return an iterator that traverses all of the colorized bins in the counter context.
     *
     * @return an iterator that goes through each and every color bin
     */
    public Iterator<String> colorBinIterator();
  };

  /**
   * Implementation of a counter that accumulates using set-based operations.
   *
   * @author  D. Trimm
   * @version 1.0
   */
  class SetCounter implements Counter {
    Map<String,Set<String>>             bin_to_total           = new HashMap<String,Set<String>>();
    Map<String,Set<String>>             bin_to_ctotal          = new HashMap<String,Set<String>>();
    Map<String,Map<String,Set<String>>> bin_to_cbin_to_total   = new HashMap<String,Map<String,Set<String>>>();
    Map<String,Set<String>>             cbin_to_total          = new HashMap<String,Set<String>>();
    Map<String,Double>                  total_color_maximum_lu = new HashMap<String,Double>();
    double                              total_maximum          = 0.0, 
                                        total_color_maximum    = 0.0;
    Map<Tablet,KeyMaker>                count_by_lu            = new HashMap<Tablet,KeyMaker>();

    public double count(Bundle bundle, String bin, double div) { return count(bundle,bin); }

    public double count(Bundle bundle, String bin) {
      if (count_by_lu.containsKey(bundle.getTablet()) == false) {
	count_by_lu.put(bundle.getTablet(),new KeyMaker(bundle.getTablet(),count_by));
      }
      String sets[] = count_by_lu.get(bundle.getTablet()).stringKeys(bundle);
      if (sets.length == 0) return 0.0;
      // - create if necessary
      if (bin_to_total.containsKey(bin) == false) {
        bin_to_total.put(bin, new HashSet<String>());
	bin_to_cbin_to_total.put(bin, new HashMap<String,Set<String>>());
        bin_to_ctotal.put(bin, new HashSet<String>());
      }
      // - Add to
      for (int i=0;i<sets.length;i++) bin_to_total.get(bin).add(sets[i]);
      // - Update maximum and get return
      if (total_maximum < bin_to_total.get(bin).size()) total_maximum = bin_to_total.get(bin).size();
      double ret = bin_to_total.get(bin).size();
      // Add to the color bin
      if (color_by != null) {
        String cbins[] = colorBins(bundle);
	for (int i=0;i<cbins.length;i++) {
          bin_to_ctotal.get(bin).add(cbins[i]);
	  if (cbin_to_total.containsKey(cbins[i])                 == false) {
	    cbin_to_total.put(cbins[i], new HashSet<String>());
            total_color_maximum_lu.put(cbins[i], 0.0);
          }
	  if (bin_to_cbin_to_total.get(bin).containsKey(cbins[i]) == false)
	    bin_to_cbin_to_total.get(bin).put(cbins[i], new HashSet<String>());
          for (int j=0;j<sets.length;j++) {
	    cbin_to_total.get(cbins[i]).add(sets[j]);
// System.err.println("bin=" + bin + ":cbin_to_total.get(" + cbins[i] + ").add(" + sets[j] + ")");
	    bin_to_cbin_to_total.get(bin).get(cbins[i]).add(sets[j]);
// System.err.println("  bin_to_cbin_to_total.get(" + bin + ").get(" + cbins[i] + ").add(" + sets[j] + ")");
            if (bin_to_cbin_to_total.get(bin).get(cbins[i]).size() > total_color_maximum_lu.get(cbins[i]))
              total_color_maximum_lu.put(cbins[i], (double) bin_to_cbin_to_total.get(bin).get(cbins[i]).size());
// System.err.println("  total_color_maximum_lu.put(" + cbins[i] + "," + ((double) bin_to_cbin_to_total.get(bin).get(cbins[i]).size()) + ")");
            if (bin_to_cbin_to_total.get(bin).get(cbins[i]).size() > total_color_maximum)
              total_color_maximum = bin_to_cbin_to_total.get(bin).get(cbins[i]).size();
// System.err.println("  total_color_maximum = " + bin_to_cbin_to_total.get(bin).get(cbins[i]).size());
	  }
	}
      }
      // Return
      return ret;
    }
    // - totals
    public double total(String bin)              { return bin_to_total.get(bin).size(); }

    Map<String,Double> bin_color_total = new HashMap<String,Double>();
    public double binColorTotal(String bin)      { if (bin_color_total.containsKey(bin) == false) {
                                                     double sum = 0.0;
                                                     Iterator<String> it = bin_to_cbin_to_total.get(bin).keySet().iterator();
                                                     while (it.hasNext()) sum += bin_to_cbin_to_total.get(bin).get(it.next()).size();
                                                     bin_color_total.put(bin,sum);
                                                   }
                                                   return bin_color_total.get(bin); }

    public double total(String bin, String cbin) { if (bin_to_cbin_to_total.containsKey(bin) &&
                                                       bin_to_cbin_to_total.get(bin).containsKey(cbin)) return bin_to_cbin_to_total.get(bin).get(cbin).size();
                                                   else                                                 return 0.0; }
    public double totalColor(String cbin)        { return cbin_to_total.get(cbin).size(); }
    public double totalMaximum()                 { return total_maximum; }
    public double totalColorMaximum()            { return total_color_maximum; }
    public double totalColorMaximum(String cbin) { return total_color_maximum_lu.get(cbin); }
    public double totalNormalized(String bin)    { return total(bin) / total_maximum; }
    public Color  binColor(String bin)           { 
      if (bin_to_cbin_to_total.containsKey(bin) && bin_to_cbin_to_total.get(bin).size() == 1) return RTColorManager.getColor(bin_to_cbin_to_total.get(bin).keySet().iterator().next());
      else if (color_by == null) return RTColorManager.getLogColor(total(bin));
      else                       return RTColorManager.getColor("set", "multi");
    }
    public void   accumulate(String from, String into) { 
      // Update the total & adjust the maximum if necessary
      bin_to_total.get(into).addAll(bin_to_total.get(from));
      if (total_maximum < bin_to_total.get(into).size()) total_maximum = bin_to_total.get(into).size();
      // fix the color-bys
      if (color_by != null && bin_to_cbin_to_total.containsKey(from)) {
        // Make the into if it doesn't exist
        if (bin_to_cbin_to_total.containsKey(into) == false) bin_to_cbin_to_total.put(into, new HashMap<String,Set<String>>());

        // Go through each color bin updating or creating the place for the destination
        Iterator<String> it_cbin = bin_to_cbin_to_total.get(from).keySet().iterator();
        while (it_cbin.hasNext()) {
          String cbin = it_cbin.next();
          // Update or create the actual color bin
          if (bin_to_cbin_to_total.get(into).containsKey(cbin)) { bin_to_cbin_to_total.get(into).get(cbin).addAll(bin_to_cbin_to_total.get(from).get(cbin)); }
          else                                                  { bin_to_cbin_to_total.get(into).put(cbin, new HashSet<String>());
                                                                  bin_to_cbin_to_total.get(into).get(cbin).addAll(bin_to_cbin_to_total.get(from).get(cbin)); }
          bin_to_ctotal.get(into).addAll(bin_to_cbin_to_total.get(from).get(cbin));

          // track the maximum(s)
          cbin_to_total.get(cbin).addAll(bin_to_cbin_to_total.get(from).get(cbin)); // May be redundant...
          if (bin_to_cbin_to_total.get(into).get(cbin).size() > total_color_maximum)              total_color_maximum = bin_to_cbin_to_total.get(into).get(cbin).size();
          if (bin_to_cbin_to_total.get(into).get(cbin).size() > total_color_maximum_lu.get(cbin)) total_color_maximum_lu.put(cbin,(double)bin_to_cbin_to_total.get(into).get(cbin).size());
        }
      }
    }
    public Iterator<String> binIterator()        { return bin_to_total.keySet().iterator(); }
    public Iterator<String> colorBinIterator()   { return cbin_to_total.keySet().iterator(); }
  }

  /**
   * A limited version of the {@link ScalarCounter} that is used to just count the
   * actual bundle element itself.
   */
  class BundleCounter extends ScalarCounter {
    public BundleCounter() { }
    public double toAdd(Bundle bundle) { return 1.0; }
  }

  /**
   * Implementation of a counter that accumulates using arithmetic operations.
   *
   * @author  D. Trimm
   * @version 1.0
   */
  class ScalarCounter implements Counter {
    Map<String,Double>             bin_to_total           = new HashMap<String,Double>();
    Map<String,Double>             bin_to_ctotal          = new HashMap<String,Double>();
    Map<String,Map<String,Double>> bin_to_cbin_to_total   = new HashMap<String,Map<String,Double>>();
    Map<String,Double>             cbin_to_total          = new HashMap<String,Double>();
    Map<String,Double>             total_color_maximum_lu = new HashMap<String,Double>();
    double                         total_maximum          = 0.0, 
                                   total_color_maximum    = 0.0;
    int                            fldi                   = -1;

    public ScalarCounter() { if (this instanceof BundleCounter == false) fldi = bundles.getGlobals().fieldIndex(count_by); }
    // - toAdd();
    //   - 2012/10/01 - added check for valid fldi.  Not sure if this is the correct place to fix the problem:
    //     For panels like the RTGraph, there's no explicit check to see if a bundle contains the correct field (it doesn't check the tablet)...
    //     This fix may mess up the statistics panel...
    //   - 2012/10/01 - solution it to make RTGraph panel conform to Tablet test... otherwise we penalize every render
    //     public double toAdd(Bundle bundle) { if (fldi >= 0) return bundle.toValue(fldi); else return 0; }
    public double toAdd(Bundle bundle) { return bundle.toValue(fldi); }
    public double count(Bundle bundle, String bin) { return count(bundle,bin,1.0); }
    public double count(Bundle bundle, String bin, double div) {
      double to_add = toAdd(bundle), ret;
      // Add to the bin
      if (bin_to_total.containsKey(bin) == false) { 
        bin_to_total.put(bin, ret = to_add/div); 
        bin_to_cbin_to_total.put(bin, new HashMap<String,Double>()); 
        bin_to_ctotal.put(bin, 0.0);
      } else bin_to_total.put(bin, ret = bin_to_total.get(bin) + to_add/div);
      // Update the maximum
      if (bin_to_total.get(bin) > total_maximum) total_maximum = bin_to_total.get(bin);
      // Add to the color bin
// System.err.println("\"" + bundle + "\" => " + color_by);
      if (color_by != null) {
        String cbins[] = colorBins(bundle);
// System.err.println("  cbins = " + cbins + " ... len=" + cbins.length);
	for (int i=0;i<cbins.length;i++) {
// System.err.println("    cbins[" + i + "] = " + cbins[i]);
          bin_to_ctotal.put(bin, bin_to_ctotal.get(bin) + to_add/div);
	  if (cbin_to_total.containsKey(cbins[i]) == false)                 cbin_to_total.put(cbins[i], to_add/div);
	  else                                                              cbin_to_total.put(cbins[i], cbin_to_total.get(cbins[i]) + to_add/div);
	  if (bin_to_cbin_to_total.get(bin).containsKey(cbins[i]) == false) bin_to_cbin_to_total.get(bin).put(cbins[i], to_add/div);
	  else                                                              bin_to_cbin_to_total.get(bin).put(cbins[i], bin_to_cbin_to_total.get(bin).get(cbins[i]) + to_add/div);

          if (total_color_maximum_lu.containsKey(cbins[i]) == false || bin_to_cbin_to_total.get(bin).get(cbins[i]) > total_color_maximum_lu.get(cbins[i]))
            total_color_maximum_lu.put(cbins[i], bin_to_cbin_to_total.get(bin).get(cbins[i]));
          if (bin_to_cbin_to_total.get(bin).get(cbins[i]) > total_color_maximum)
            total_color_maximum = bin_to_cbin_to_total.get(bin).get(cbins[i]);
	}
      }
      // Return new count
      return ret;
    }
    public double total(String bin)              { return bin_to_total.get(bin); }
    public double binColorTotal(String bin)      { return bin_to_ctotal.get(bin); }
    public double total(String bin, String cbin) { if (bin_to_cbin_to_total.containsKey(bin) &&
                                                       bin_to_cbin_to_total.get(bin).containsKey(cbin)) return bin_to_cbin_to_total.get(bin).get(cbin);
                                                   else                                                 return 0.0; }
    public double totalColor(String cbin)        { return cbin_to_total.get(cbin); }
    public double totalMaximum()                 { return total_maximum; }
    public double totalColorMaximum()            { return total_color_maximum; }
    public double totalColorMaximum(String cbin) { return total_color_maximum_lu.get(cbin); }
    public double totalNormalized(String bin)    { return total(bin) / total_maximum; }
    public Color  binColor(String bin)           { 
      if (bin_to_cbin_to_total.containsKey(bin) && bin_to_cbin_to_total.get(bin).size() == 1) return RTColorManager.getColor(bin_to_cbin_to_total.get(bin).keySet().iterator().next());
      else if (color_by == null) return RTColorManager.getLogColor(total(bin));
      else                       return RTColorManager.getColor("set", "multi");
    }
    public void accumulate(String from, String into) { 
      // Update the total & adjust the maximum if necessary
      bin_to_total.put(into, bin_to_total.get(from) + bin_to_total.get(into));
      if (total_maximum < bin_to_total.get(into)) total_maximum = bin_to_total.get(into);

      // Update the color-bys if appropriate... probably will take awhile
      if (color_by != null && bin_to_cbin_to_total.containsKey(from)) {
	// Make the into if it doesn't exist
        if (bin_to_cbin_to_total.containsKey(into) == false) bin_to_cbin_to_total.put(into, new HashMap<String,Double>());

	// Go through each color bin updating or creating the place for the destination
        Iterator<String> it_cbin = bin_to_cbin_to_total.get(from).keySet().iterator();
	while (it_cbin.hasNext()) {
	  String cbin = it_cbin.next();
	  // Update or create the actual color bin
	  if (bin_to_cbin_to_total.get(into).containsKey(cbin)) { bin_to_cbin_to_total.get(into).put(cbin, bin_to_cbin_to_total.get(from).get(cbin) + bin_to_cbin_to_total.get(into).get(cbin)); } 
	  else                                                  { bin_to_cbin_to_total.get(into).put(cbin, bin_to_cbin_to_total.get(from).get(cbin)); }
	  bin_to_ctotal.put(into, bin_to_ctotal.get(into) + bin_to_cbin_to_total.get(from).get(cbin));

	  // track the maximum(s)
          cbin_to_total.put(cbin, cbin_to_total.get(cbin) + bin_to_cbin_to_total.get(from).get(cbin)); // May be unnecessary... or produce incorrect results
          if (bin_to_cbin_to_total.get(into).get(cbin) > total_color_maximum)              total_color_maximum = bin_to_cbin_to_total.get(into).get(cbin);
	  if (bin_to_cbin_to_total.get(into).get(cbin) > total_color_maximum_lu.get(cbin)) total_color_maximum_lu.put(cbin,bin_to_cbin_to_total.get(into).get(cbin));
	}
      }
    }
    public Iterator<String> binIterator()        { return bin_to_total.keySet().iterator(); }
    public Iterator<String> colorBinIterator()   { return cbin_to_total.keySet().iterator(); }
  }

  /**
   * Constructor to abstract away how to count across a specific field within the dataset.
   *
   * @param bundles  dataset to count over
   * @param count_by field to count by
   * @param color_by sub-field to color by
   */
  public BundlesCounterContext(Bundles bundles, String count_by, String color_by) {
    this.bundles = bundles; this.count_by = count_by; this.color_by = color_by;
    // Figure out the count_by's...
    if (count_by.equals(BundlesDT.COUNT_BY_BUNS)) {
      counter = new BundleCounter();
    } else if (count_by.indexOf(BundlesDT.DELIM) >= 0 || 
               bundles.getGlobals().isScalar(bundles.getGlobals().fieldIndex(count_by)) == false) { // Set based
      counter = new SetCounter();
    } else { // Scalar based
      counter = new ScalarCounter();
    }
  }

  /**
   * Method to return the color bins that this specific bundle maps to.
   *
   * @param  bundle specific bundle to map
   * @return        color bin strings that this bundle maps into
   */
  public String[] colorBins(Bundle bundle) { 
    if (color_tablet_lu.containsKey(bundle.getTablet()) == false) { // color_by KeyMaker missing, fill it in
      if (KeyMaker.tabletCompletesBlank(bundle.getTablet(), color_by)) color_tablet_lu.put(bundle.getTablet(),new KeyMaker(bundle.getTablet(),color_by));
      else                                                             color_tablet_lu.put(bundle.getTablet(),null);
    } 
    if (color_tablet_lu.get(bundle.getTablet()) == null)   { String strs[] = new String[1]; strs[0] = "[nocolor]"; return strs; // Doesn't complete the color_by blank
    } else                                                   return color_tablet_lu.get(bundle.getTablet()).stringKeys(bundle); 
  }

  // These are really just cut-outs to call the associated subset counter
  public double            count(Bundle bundle, String bin, double div) {
    if (bin_to_bundle_set.containsKey(bin) == false) bin_to_bundle_set.put(bin,new HashSet<Bundle>());
    bin_to_bundle_set.get(bin).add(bundle);
    return counter.count(bundle,bin,div);    
  }
  public double            count(Bundle bundle, String bin) { 
    if (bin_to_bundle_set.containsKey(bin) == false) bin_to_bundle_set.put(bin,new HashSet<Bundle>());
    bin_to_bundle_set.get(bin).add(bundle);
    return counter.count(bundle,bin);    
  }
  public double            total(String bin)                { return counter.total(bin);           }
  public double            binColorTotal(String bin)        { return counter.binColorTotal(bin);   }
  public double            total(String bin, String cbin)   { return counter.total(bin,cbin);      }
  public double            totalMaximum()                   { return counter.totalMaximum();       }
  public double            totalNormalized(String bin)      { return counter.totalNormalized(bin); }
  public Color             binColor(String bin)             { return counter.binColor(bin); }
  public double            totalColorMaximum()              { return counter.totalColorMaximum();     }
  public double            totalColorMaximum(String cbin)   { return counter.totalColorMaximum(cbin); }
  public void              accumulate(String from, String into) { counter.accumulate(from,into); }
  public Iterator<String>  binIterator()                    { return counter.binIterator();        }
  public Iterator<String>  colorBinIterator()               { return counter.colorBinIterator();   }

  /**
   * Sort the bins by their counts and return the sorted list.
   *
   * @return sorted list of bins by their overall counts
   */
  public List<String> getBinsSortedByCount() { 
    Iterator<String> it = counter.binIterator();
    List<Sorter> sorters = new ArrayList<Sorter>();
    while (it.hasNext()) {
      String bin = it.next();
      double d   = total(bin);
      sorters.add(new Sorter(bin, d));
    }
    Collections.sort(sorters);
    List<String> strs = new ArrayList<String>();
    for (int i=0;i<sorters.size();i++) strs.add(sorters.get(i).toString());
    return strs;
  }

  /**
   * Sort the color bins by their counts and return the sorted list.
   *
   * @return sorted list of color bins by their overall counts
   */
  public List<String> getColorBinsSortedByCount() {
    Iterator<String> it = counter.colorBinIterator();
    List<Sorter> sorters = new ArrayList<Sorter>();
    while (it.hasNext()) {
      String cbin = it.next();
      double d    = counter.totalColor(cbin);
      sorters.add(new Sorter(cbin, d));
    }
    Collections.sort(sorters);
    List<String> strs = new ArrayList<String>();
    for (int i=0;i<sorters.size();i++) strs.add(sorters.get(i).toString());
    return strs;
  }

  /**
   * Return the set of bundles that corresponds to a specific GUI-related string.
   *
   * @return the set of bundles that have been accumulated for this bin
   */
  public Set<Bundle>   getBundles(String bin)           { return bin_to_bundle_set.get(bin); }

  /**
   * Class used to sort double values that correspond to a specific string.
   */
  class Sorter implements Comparable<Sorter> { String str; double d; public Sorter(String str, double d) { this.str = str; this.d = d; }
    /**
     * Compares one sorter to another.
     * 
     * @param  other other sorter to compare to
     * @return       -1, 0, 1 depending if other sorter is less than, equal to, or greater than
     */
    public int compareTo(Sorter other) {
      if      (d < other.d) return -1;
      else if (d > other.d) return  1;
      else                  return str.compareTo(other.str);
    }

    /**
     * Return the corresponding string value for this sorter.
     *
     * @return the string value for this sorter
     */
    public String toString() { return str; }
  }
}

