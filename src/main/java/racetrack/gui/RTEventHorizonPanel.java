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

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesG;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.BundlesDT;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;
import racetrack.util.Utils;
import racetrack.visualization.RTColorManager;

/**
 *
 *@author   D. Trimm
 *@version  0.1
 */
public class RTEventHorizonPanel extends RTPanel {
  /**
   * Field to bin by
   */
  JComboBox bin_by_cb,
  /**
   * Sub-field to bin by
   */
            bin_by2_cb;
  /**
   * Bundles for the comparison
   */
  Bundles   eh_bs;
  /**
   *
   */
  final String HORIZON_01MIN_STR = "01 Min Horizon",
               HORIZON_05MIN_STR = "05 Min Horizon",
               HORIZON_10MIN_STR = "10 Min Horizon",
               HORIZON_30MIN_STR = "30 Min Horizon",
	       HORIZON_60MIN_STR = "60 Min Horizon";

  /**
   *
   */
  final String horizon_strs[] = { HORIZON_01MIN_STR,
                                  HORIZON_05MIN_STR,
                                  HORIZON_10MIN_STR,
				  HORIZON_30MIN_STR,
				  HORIZON_60MIN_STR };
  /**
   *
   */
  JRadioButtonMenuItem horizon_rbmis[],

  /**
   *
   */
                       scale_rbmis[];

  /**
   *
   */
  JCheckBoxMenuItem    nearest_match_only_cbmi,
  /**
   *
   */
                       vary_size_cbmi;
  /**
   * Construct a default event horizon panel.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt application parent
   */
  public RTEventHorizonPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt) {
    super(win_type, win_pos, win_uniq, rt);

    // Add the component
    add("Center", component = new RTEventHorizonComponent());

    // Make the bottom panel
    JPanel bottom_panel = new JPanel(new FlowLayout()); JButton horizons_bt;
    bottom_panel.add(horizons_bt = new JButton("Set"));
    bottom_panel.add(new JLabel("Pri")); bottom_panel.add(bin_by_cb  = new JComboBox());
    bottom_panel.add(new JLabel("2nd")); bottom_panel.add(bin_by2_cb = new JComboBox()); updateBys();
    add("South", bottom_panel);

    // Make the popup menu
    ButtonGroup bg = new ButtonGroup();
    final String[] scale_strs = AxisMapper.simpleScales();
    scale_rbmis = new JRadioButtonMenuItem[scale_strs.length];
    for (int i=0;i<scale_rbmis.length;i++) {
      getRTPopupMenu().add(scale_rbmis[i] = new JRadioButtonMenuItem(scale_strs[i]));
      if (i==0) scale_rbmis[i].setSelected(true);
      bg.add(scale_rbmis[i]); defaultListener(scale_rbmis[i]);
    }
    getRTPopupMenu().addSeparator();
    bg =new ButtonGroup();
    horizon_rbmis = new JRadioButtonMenuItem[horizon_strs.length];
    for (int i=0;i<horizon_rbmis.length;i++) {
      getRTPopupMenu().add(horizon_rbmis[i] = new JRadioButtonMenuItem(horizon_strs[i]));
      if (i==0) horizon_rbmis[i].setSelected(true);
      bg.add(horizon_rbmis[i]); defaultListener(horizon_rbmis[i]);
    }
    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(nearest_match_only_cbmi = new JCheckBoxMenuItem("Nearest Match Only", true));
    getRTPopupMenu().add(vary_size_cbmi          = new JCheckBoxMenuItem("Vary Size", true));

    // Add the listeners
    defaultListener(bin_by_cb);
    defaultListener(bin_by2_cb);
    defaultListener(nearest_match_only_cbmi);
    defaultListener(vary_size_cbmi);
    horizons_bt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        eh_bs = getRTParent().getVisibleBundles();
	getRTComponent().render();
      }
    } );
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "histogram"; }

  /**
   * Update the "bin by"  and the "sub-bin-by" options to show the current bundle headers.
   */
  public void updateBys() { updateBys(bin_by_cb); updateBys(bin_by2_cb); }

  /**
   * Update the specific combobox with the global set of heades for choosing
   * data types.
   *
   *@param cb combobox to update
   */
  public void updateBys(JComboBox cb) {
    String strs[]; Object sel = cb.getSelectedItem();
    cb.removeAllItems(); 
    BundlesG globals = getRTParent().getRootBundles().getGlobals();
    if (cb == bin_by_cb) strs = KeyMaker.blanks(globals, false, true, true, false);
    else                 strs = KeyMaker.blanks(globals, false, true, true, false);
    if (cb == bin_by2_cb) cb.addItem(BundlesDT.COUNT_BY_NONE);
    for (int i=0;i<strs.length;i++) cb.addItem(strs[i]);
    if (sel == null) cb.setSelectedIndex(0); else cb.setSelectedItem(sel);
  }

  /**
   * Return the header element to bin by
   *
   *@return bin by setting
   */
  public String  binBy()                     { return (String) bin_by_cb.getSelectedItem(); }

  /**
   * Set the bin by option
   *
   *@param str bin by option
   */
  public void    binBy(String str)           { bin_by_cb.setSelectedItem(str); }

  /**
   * Return the header element to sub-bin by
   *
   *@return sub-bin by setting
   */
  public String  binBy2()                    { return (String) bin_by2_cb.getSelectedItem(); }

  /**
   * Set the sub-bin by option
   *
   *@param str sub-bin setting
   */
  public void    binBy2(String str)          { bin_by2_cb.setSelectedItem(str); }

  /**
   * Return the event horizon for the rendering.
   *
   *@return event horizon in milliseconds
   */
  public long    eventHorizon()              {
    String str = null; long ms;
    for (int i=0;i<horizon_rbmis.length;i++) { if (horizon_rbmis[i].isSelected()) str = horizon_rbmis[i].getText(); }
    if      (str == null)                   ms =  5L*60L*1000L;
    else if (str.equals(HORIZON_01MIN_STR)) ms =  1L*60L*1000L;
    else if (str.equals(HORIZON_05MIN_STR)) ms =  5L*60L*1000L;
    else if (str.equals(HORIZON_10MIN_STR)) ms = 10L*60L*1000L;
    else if (str.equals(HORIZON_30MIN_STR)) ms = 30L*60L*1000L;
    else if (str.equals(HORIZON_60MIN_STR)) ms = 60L*60L*1000L;
    else                                    ms =  5L*60L*1000L;
    return ms;
  }

  /**
   * Set the event horizon for the rendering.  This describes how long the time frame is for measuring events
   * against the base bundles.
   *
   *@param ms event horizon in milliseconds
   */
  public void    eventHorizon(long ms)       {
    String str;
    if      (ms ==  1L*60L*1000L) str = HORIZON_01MIN_STR;
    else if (ms ==  5L*60L*1000L) str = HORIZON_05MIN_STR;
    else if (ms == 10L*60L*1000L) str = HORIZON_10MIN_STR;
    else if (ms == 30L*60L*1000L) str = HORIZON_30MIN_STR;
    else if (ms == 60L*60L*1000L) str = HORIZON_60MIN_STR;
    else                          str = HORIZON_05MIN_STR;
    for (int i=0;i<horizon_rbmis.length;i++) { if (horizon_rbmis[i].getText().equals(str)) horizon_rbmis[i].setSelected(true); }
  }

  /**
   * Get the scale for the y-axis.
   *
   *@return scale for y-axis
   */
  public String scale() {
    for (int i=0;i<scale_rbmis.length;i++) if (scale_rbmis[i].isSelected()) return scale_rbmis[i].getText();
    return AxisMapper.LINEAR_SCALE_STR;
  }

  /**
   * Set the scale for the y-axis.
   *
   *@param scale new scale setting
   */
  public void scale(String scale) {
    for (int i=0;i<scale_rbmis.length;i++) if (scale_rbmis[i].getText().equals(scale)) scale_rbmis[i].setSelected(true);
  }

  /**
   * Return the option to only consider nearest matches.
   *
   *@return nearest match only flag
   */
  public boolean nearestMatchOnly() { return nearest_match_only_cbmi.isSelected(); }

  /**
   * Set the option to only contribute on the nearest match.
   *
   *@param nmo flag to match on nearest only
   */
  public void nearestMatchOnly(boolean nmo) { nearest_match_only_cbmi.setSelected(nmo); }

  /**
   * Return the option to vary size of the plotted points.
   *
   *@return vary size flag
   */
  public boolean varySize() { return vary_size_cbmi.isSelected(); }

  /**
   * Set the option to vary the size of the plotted points.
   *
   *@param nmo vary size flag
   */
  public void varySize(boolean vsize) { vary_size_cbmi.setSelected(vsize); }

  /**
   * Return a string that represent this component's current configuration.  This
   * string can then be used for bookmarks and returning to specific views.
   *
   *@return string representing component's configuration
   */
  public String  getConfig()  { return "RTEventHorizonPanel"                                              + BundlesDT.DELIM +
                                       "binby="   + Utils.encToURL((String) bin_by_cb.getSelectedItem())  + BundlesDT.DELIM +
                                       "binby2="  + Utils.encToURL((String) bin_by2_cb.getSelectedItem()) + BundlesDT.DELIM +
				       "scale="   + Utils.encToURL(scale())                               + BundlesDT.DELIM +
				       "horizon=" + Utils.encToURL((String) "" + eventHorizon())          + BundlesDT.DELIM +
				       "nearest=" + Utils.encToURL("" + nearestMatchOnly())               + BundlesDT.DELIM +
				       "vsize="   + Utils.encToURL("" + varySize()); }

  /**
   * Set the view's current configuration based on a string return
   * from the getConfig() method.
   *
   *@param str configuration string
   */
  public void    setConfig(String str) {
    StringTokenizer st = new StringTokenizer(str,BundlesDT.DELIM);
    if (st.nextToken().equals("RTEventHorizonPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not An RTEventHorizonPanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st2.hasMoreTokens() ? st2.nextToken() : "";
      if      (type.equals("binby"))   bin_by_cb.setSelectedItem(Utils.decFmURL(value));
      else if (type.equals("binby2"))  bin_by2_cb.setSelectedItem(Utils.decFmURL(value));
      else if (type.equals("scale"))   scale(Utils.decFmURL(value));
      else if (type.equals("horizon")) eventHorizon(Long.parseLong(Utils.decFmURL(value)));
      else if (type.equals("nearest")) nearestMatchOnly(value.toLowerCase().equals("true"));
      else if (type.equals("vsize"))   varySize(value.toLowerCase().equals("true"));
      else throw new RuntimeException("Do Not Understand Type Value Pair \"" + type + "\"=\"" + value + "\"");
    }
  }

  /**
   * Component that implments interaction and display of the histogram rendering.
   */
  public class RTEventHorizonComponent extends RTComponent {
    /**
     * Return the {@link Shape}s that represent the specified {@link Bundle}s.
     * Note that in the current implementation, most bundle records will have
     * two shapes - one for the regular histogram bar and the other for the
     * stacked histogram representation.
     *
     *@param  bundles specific bundles to return shape records for
     *
     *@return         set of shapes that represent the specified bundles
     */
    public Set<Shape>  shapes(Set<Bundle> bundles) {
      Set<Shape> set = new HashSet<Shape>();
      RenderContext myrc = (RenderContext) rc;
      if (myrc != null) {
        Iterator<Bundle> it_bun = bundles.iterator();
	while (it_bun.hasNext()) {
	  Bundle bundle = it_bun.next();
	  if (myrc.bundle_to_skeys.containsKey(bundle)) {
	    Iterator<String> it_skey = myrc.bundle_to_skeys.get(bundle).iterator();
	    while (it_skey.hasNext()) {
	      String skey = it_skey.next();
	      set.add(myrc.skey_to_geom.get(skey));
	    }
	  }
	}
      }
      return set;
    }

    /**
     * For a specific {@link Bundle}, return the its associated shapes.
     *
     *@param  bundle bundle to find shapes for
     *
     *@return        set of shapes that represent this bundle in the current view
     */
    public Set<Shape>  shapes(Bundle bundle) { 
      Set<Bundle> set = new HashSet<Bundle>(); set.add(bundle); 
      return shapes(set); 
    }

    /**
     * For a specific shape, return the associated bundles as a set.
     *
     *@param  shape shape must have been provided by the rendering (i.e.,
     *              cannot handle generic shapes)
     *
     *@return       set of bundle records that the specified shape represent
     */
    public Set<Bundle> shapeBundles(Shape shape) {
      Set<Bundle> set = new HashSet<Bundle>();
      RenderContext myrc = (RenderContext) rc;
      if (myrc != null) {
        String skey = myrc.geom_to_skey.get(shape);
	if (skey != null) { return myrc.counter_context.getBundles(skey); }
      } 
      return set;
    }

    /**
     * Return the set with all the existing shapes in the rendering.
     *
     *@return set of all shapes in the view
     */
    public Set<Shape> allShapes() {
      Set<Shape> set = new HashSet<Shape>();
      RenderContext myrc = (RenderContext) rc;
      if (myrc != null) { return myrc.geom_to_skey.keySet(); }
      return set;
    }

    /**
     * For a generic specified shape, return the overlapping shapes.
     *
     *@param  shape_to_check shape to check against
     *
     *@return                set of shapes in the rendering that overlap
     *                       with the specified shape
     */
    public Set<Shape> overlappingShapes(Shape shape_to_check) {
      Set<Shape> set = new HashSet<Shape>();
      RenderContext myrc = (RenderContext) rc;
      if (myrc != null) {
        Iterator<Shape> it = myrc.geom_to_skey.keySet().iterator();
        while (it.hasNext()) {
	  Shape shape = it.next();
	  if (Utils.genericIntersects(shape, shape_to_check)) set.add(shape);
	}
      }
      return set;
    }

    /**
     * Render the current view by creating a new render context
     * based on the GUI configuration and current set of bundle
     * records.
     *
     *@param  id render id
     *
     *@return    a render context
     */
    public RTRenderContext render(short id) {
      clearNoMappingSet();
      // Don't draw if not visible
      if (isVisible() == false) { return null; }
      // Get the variables
      Bundles bs = getRenderBundles(), base_bs = eh_bs;
      String  bin_by  = binBy(), bin_by2 = binBy2(), count_by = getRTParent().getCountBy(), color_by = getRTParent().getColorBy();
      String  scale   = scale();
      long    horizon = eventHorizon();
      boolean nearest = nearestMatchOnly();
      boolean vsize   = varySize();
      if (bin_by2 != null && bin_by2.equals(BundlesDT.COUNT_BY_NONE)) bin_by2 = null;
      // Create the render context and set the base image
      if (bs != null && bin_by != null && count_by != null) { // color_by can be null...
        RenderContext myrc = new RenderContext(id, bs, base_bs, bin_by, bin_by2, count_by, color_by, scale, horizon, nearest, vsize, getWidth(), getHeight());
        return myrc;
      }
      return null;
    }

    /**
     * The shape that describes what is under the mouse.
     *
     *@param  x mouse x coordinate for the shape
     *@param  y mouse y coordinate for the shape
     *
     *@return shape directly relevant to mouse postion
     */
    public Shape getZeroOrderShape(int x, int y) { return new Ellipse2D.Double(x-2, y-2, 5, 5); }

    /**
     * The shape that describes what is near the mouse.
     *
     *@param  x mouse x coordinate for the shape
     *@param  y mouse y coordinate for the shape
     *
     *@return shape near to mouse postion
     *
     */
    public Shape getFirstOrderShape(int x, int y) { return new Rectangle2D.Double(x-5,y-5,11,11); }

    /**
     * The shape that describes what is further from the mouse.
     *
     *@param  x mouse x coordinate for the shape
     *@param  y mouse y coordinate for the shape
     *
     *@return shape further from the mouse postion
     */
    public Shape getSecondOrderShape(int x, int y) { return new Rectangle2D.Double(x-10,y-10,21,21); }

    /**
     * The render context is responsible for rendering the view based on
     * the GUI parameters.  As such, it contains the mapping between the
     * rendered shapes and the records.
     */
    class RenderContext extends RTRenderContext {
      /**
       * Width (in pixels) of this renderering
       */
      int     w, 
      /**
       * Height (in pixels) of this rendering
       */
              h; 
      /**
       * Field to use that corresponds to a bar in the histogram
       */
      String  bin_by, 
      /**
       * Subfields for the bar - this will cause more bars to occur as the
       * primary bar is further qualified
       */
              bin_by2, 
      /**
       * Length of the bar - which field to count by
       */
	      count_by, 
      /**
       * How to subcolor each bar
       */
	      color_by; 
      /**
       * Scale for the y-axis
       */
      String  scale;
      /**
       * How long to extend the event horizon
       */
      long    horizon;
      /**
       * Data to render
       */
      Bundles bs,
      /**
       * Data to compare against
       */
              base_bs;
      /**
       * Key makers to transform data
       */
      List<KeyMaker> ykms  = new ArrayList<KeyMaker>(), 
                     y2kms = new ArrayList<KeyMaker>();
      /**
       *
       */
      public String find(List<KeyMaker> kms, int value) {
        Iterator<KeyMaker> it = kms.iterator();
        while (it.hasNext()) { String str = it.next().toString(value); if (str != null) return str; }
        return "Not Found";
      }

      /**
       * Bundle to World Y mapping
       */
      Map<Bundle,Set<Long>>   bundle_to_wys = new HashMap<Bundle,Set<Long>>();

      /**
       * Bundle to normalized x coordinates (-1.0 ... 1.0)
       */
      Map<Bundle,Set<Double>> bundle_to_nxs = new HashMap<Bundle,Set<Double>>();

      /**
       * World Y to Normalized Y (0.0 ... 1.0) mapping
       */
      Map<Long,Double>        wy_to_ny;

      /**
       * Is a rendering possible?  Based on memory needs...
       */
      boolean render_possible;

      /**
       * Flag to only count the nearest (in time) match for the contributing bundle
       */
      boolean nearest;

      /**
       * Flag to vary the size of the plotted points by their contributions.
       */
      boolean vsize;

      /**
       * Minimum world y value - used for scaling and labeling
       */
      long y_min,

      /**
       * Maximum world y value - used for scaling and labeling
       */
           y_max;

      /**
       * Construct the render context with the specified fields in the arguments which are
       * a snapshot of the GUI settings for this component.
       *
       *@param id        render id
       *@param bs        data to render
       *@param base_bs   data to compare against
       *@param bin_by    bars in histogram
       *@param bin_by2   secondary field to further qualify the bars
       *@param color_by  how to differentiate each bar by another field
       *@param scale     scale for the y-axis
       *@param horizon   how long to extend the event horizon
       *@param nearest   only match against the nearest location
       *@param vsize     vary size flag
       *@param w         width of the rendering
       *@param h         height of the rendering
       */
      public RenderContext(short id, Bundles bs, Bundles base_bs, String bin_by, String bin_by2, String count_by, String color_by, String scale, long horizon, boolean nearest, boolean vsize, int w, int h) {
	// Save variables...
        render_id = id; this.bs = bs; this.base_bs = base_bs; this.w = w; this.h = h; this.bin_by = bin_by; this.bin_by2 = bin_by2; this.count_by = count_by; this.color_by = color_by; this.scale = scale; this.horizon = horizon; this.nearest = nearest; this.vsize = vsize;

	// Determine if a rendering is even possible...
        if (base_bs == null) { render_possible = false; return; }
	long base_ts0    = base_bs.ts0() - horizon,
	     base_ts1    = base_bs.ts1() + horizon;
        long time_diff   = base_ts1 - base_ts0;
        int  bitvec_len  = (int) (time_diff/horizon);
	if (bitvec_len > 300000) { render_possible = false; return; } else { render_possible = true; }

	// Create the bit vector
        // - used to do a gross match on if the bundle is near the base bundles
        boolean                   bitvec[]  = new boolean[bitvec_len+1];
	Map<Integer,List<Bundle>> bitvec_lu = new HashMap<Integer,List<Bundle>>();
	Iterator<Tablet> tablet_i = base_bs.tabletIterator();
	while (tablet_i.hasNext() && currentRenderID() == getRenderID()) {
	  Tablet tablet = tablet_i.next();
	  if (tablet.hasTimeStamps()) {
	    Iterator<Bundle> bundle_i = tablet.bundleIterator();
	    while (bundle_i.hasNext() && currentRenderID() == getRenderID()) {
	      Bundle bundle   = bundle_i.next();
              int    bitvec_i = (int) ((bundle.ts0() - base_ts0)/horizon);
	      if (bitvec[bitvec_i] == false) {
	        bitvec_lu.put(bitvec_i, new ArrayList<Bundle>());
	        bitvec[bitvec_i] = true;
	      }
	      bitvec_lu.get(bitvec_i).add(bundle);
	    }
	  }
	}

        // Set the mins and maxes for the y value
        y_min = Long.MAX_VALUE;
        y_max = Long.MIN_VALUE;
        Set<Long>  y_set  = new HashSet<Long>();
        List<Long> y_list = new ArrayList<Long>();

	// Go through the bundles
        tablet_i = bs.tabletIterator();
        while (tablet_i.hasNext() && currentRenderID() == getRenderID()) {
	  Tablet tablet = tablet_i.next();
          boolean tablet_can_count = count_by.equals(KeyMaker.RECORD_COUNT_STR) || KeyMaker.tabletCompletesBlank(tablet, count_by);
	  // Differentiate binning by time
          if (KeyMaker.tabletCompletesBlank(tablet, bin_by) && tablet.hasTimeStamps() && (count_by.equals(KeyMaker.RECORD_COUNT_STR) || KeyMaker.tabletCompletesBlank(tablet, count_by))) {
	    // Make the binner
	    KeyMaker y_km  = new KeyMaker(tablet, bin_by); ykms.add(y_km);
	    KeyMaker y2_km = null; if (bin_by2 != null && KeyMaker.tabletCompletesBlank(tablet, bin_by2)) { y2_km = new KeyMaker(tablet, bin_by2); y2kms.add(y2_km); }
            // Go through the bundles
	    Iterator<Bundle> bundle_i = tablet.bundleIterator();
	    while (bundle_i.hasNext() && currentRenderID() == getRenderID()) {
	      Bundle bundle = bundle_i.next();
	      // Base can't contribute to itself...
	      if (base_bs.bundleSet().contains(bundle)) continue;

	      // Determine if it contributes
              if (bundle.ts0() < base_ts0) continue;
	      if (bundle.ts1() > base_ts1) continue;
	      int bitvec_i = (int) ((bundle.ts0() - base_ts0)/horizon);

	      boolean test0 = false, test1 = false, test2 = false; List<Bundle> list0 = null, list1 = null, list2 = null;
	      if (                              bitvec[bitvec_i])   { test0 = true; list0 = bitvec_lu.get(bitvec_i);   }
	      if (bitvec_i > 0               && bitvec[bitvec_i-1]) { test1 = true; list1 = bitvec_lu.get(bitvec_i-1); }
	      if (bitvec_i < bitvec.length-1 && bitvec[bitvec_i+1]) { test2 = true; list2 = bitvec_lu.get(bitvec_i+1); }

	      if (test0 == false && test1 == false && test2 == false) continue;

              List<Integer> xs = new ArrayList<Integer>();

	      // Find the x locations
	      boolean bundle_contributes = false; double nearest_double = 5.0;
	      for (int i=0;i<3;i++) {
	        List<Bundle> list = null; if (i == 0) list = list0; else if (i == 1) list = list1; else if (i == 2) list = list2;
		if (list == null) continue;
		for (int j=0;j<list.size();j++) {
		  Bundle compare   = list.get(j);
		  long   diff      = compare.ts0() - bundle.ts0();
		  double as_double = ((double) diff)/horizon;
		  if (Math.abs(diff) < horizon) {
		    if (nearest) {
		      if (Math.abs(as_double) < Math.abs(nearest_double)) nearest_double = as_double;
		    } else       {
                      if (bundle_to_nxs.containsKey(bundle) == false) bundle_to_nxs.put(bundle, new HashSet<Double>());
                      bundle_to_nxs.get(bundle).add(as_double);
                    }
		    bundle_contributes = true;
		  }
		}
	      }

              // Figure out the contributions
	      if (bundle_contributes) {
	        // Only contribute on the nearest (if set)
                if (nearest) {
		  bundle_to_nxs.put(bundle, new HashSet<Double>());
		  bundle_to_nxs.get(bundle).add(nearest_double);
	        }

		// Calculate the y's
                long   ys[] = toLongs(y_km.intKeys(bundle));
	        if (ys != null && ys.length > 0) {
	          if (y2_km != null) {
	            long y2s[]    = toLongs(y2_km.intKeys(bundle));
		    long new_ys[] = new long[ys.length * y2s.length];
		    int k=0;
                    for (int i=0;i<ys.length;i++) { for (int j=0;j<y2s.length;j++) { new_ys[k++] = (ys[i]<<32) | (0x00FFFFFFFFL & y2s[j]); } }
		    ys = new_ys;
	          } 
                  if (bundle_to_wys.containsKey(bundle) == false) bundle_to_wys.put(bundle, new HashSet<Long>());
	          for (int i=0;i<ys.length;i++) {
                    bundle_to_wys.get(bundle).add(ys[i]);
                    if (y_set.contains(ys[i]) == false) { 
                      y_set.add(ys[i]); y_list.add(ys[i]);
                      if (ys[i] > y_max) y_max = ys[i];
                      if (ys[i] < y_min) y_min = ys[i];
                    }
                  }
                }
	      }
	    }
	  } else { // Put the other bundles into the no mapping set
	    Iterator<Bundle> bundle_i = tablet.bundleIterator();
	    while (bundle_i.hasNext() && currentRenderID() == getRenderID()) {
	      Bundle bundle = bundle_i.next();
              addToNoMappingSet(bundle);
            }
          }
        }

	// Figure out the mapping for the scale/axis
        wy_to_ny = AxisMapper.calculateMapping(scale, y_list, y_min, y_max);
      }

    /**
     * Convert integer array to long array.  Useful for converting the application values (integers)
     * into those needed by the xy-scatterplot.  Longs are used by xy to encompass timestamps and the
     * primary/alternate y axis fields.
     *
     *@param  ints integers to convert
     *
     *@return long array
     */
    private long[] toLongs(int ints[]) { long longs[] = new long[ints.length]; for (int i=0;i<longs.length;i++) longs[i] = (long) ints[i]; return longs; }

    /**
     * Return the width (in pixels) of this rendering
     *
     *@return width of render
     */
    public int getRCWidth()  { return w; }

    /**
     * Return the height (in pixels) of this rendering
     *
     *@return height of render
     */
    public int getRCHeight() { return h; }

    /**
     *
     */
    BundlesCounterContext counter_context;

    /**
     *
     */
    Map<Shape,String>       geom_to_skey    = new HashMap<Shape,String>();

    /**
     *
     */
    Map<String,Shape>       skey_to_geom    = new HashMap<String,Shape>();

    /**
     *
     */
    Map<Bundle,Set<String>> bundle_to_skeys = new HashMap<Bundle,Set<String>>();


    /**
     * Rendered image - set if already rendered
     */
    BufferedImage base_bi;

    /**
     * Render the image onto a {@link BufferedImage}. DECOMPOSE
     *
     *@return image with the rendering
     */
    @Override
    public BufferedImage getBase() {
      if (base_bi != null) return base_bi;
      Graphics2D g2d = null; BufferedImage bi = null;
      try {
        // Buffered image...
        bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        // Graphics context...
        g2d = (Graphics2D) bi.getGraphics(); 
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RTColorManager.renderVisualizationBackground(bi,g2d);
        if (render_possible) {
          // Figure out the geometry
          int           txt_h = Utils.txtW(g2d, "0");
          int ins_rgt = 4, ins_lft = txt_h + 4, ins_top = 4, ins_bot = txt_h + 4, 
              graph_w = w - (ins_rgt + ins_lft),
              graph_h = h - (ins_top + ins_bot),
              x_cen   = ins_lft + graph_w/2,
              lobe_w  = graph_w/2;

	  // Axes
          g2d.setColor(RTColorManager.getColor("axis", "minor"));
          g2d.drawLine(ins_lft,ins_top,ins_lft,ins_top+graph_h);
	  g2d.drawLine(ins_lft,ins_top,ins_lft+graph_w,ins_top);
          g2d.drawLine(ins_lft+graph_w,ins_top,ins_lft+graph_w,ins_top+graph_h);
	  g2d.drawLine(ins_lft,ins_top+graph_h,ins_lft+graph_w,ins_top+graph_h);
	  g2d.drawLine(x_cen,0,x_cen,h);

	  // Labels
	  // - X Labels
	  g2d.setColor(RTColorManager.getColor("label", "default"));
	  String str = Utils.humanReadableDuration(horizon);
          g2d.drawString(str,ins_lft+graph_w-Utils.txtW(g2d,str),ins_top+graph_h+txt_h+2);
	  str = "-" + str;
	  g2d.drawString(str,ins_lft,ins_top+graph_h+txt_h+2);
	  // - Y Labels
	  String y_max_label, y_min_label;
          if (bin_by2 != null) {
            y_max_label = find(ykms,  (int) ((y_max >> 32) & 0x00ffffffffL)) + ":" +
	                  find(y2kms, (int) ((y_max >>  0) & 0x00ffffffffL));
            y_min_label = find(ykms,  (int) ((y_min >> 32) & 0x00ffffffffL)) + ":" +
	                  find(y2kms, (int) ((y_min >>  0) & 0x00ffffffffL));
	  } else {
	    y_max_label = find(ykms, (int) y_max);
	    y_min_label = find(ykms, (int) y_min);
	  }
          Utils.drawRotatedString(g2d, y_min_label, ins_lft, ins_top + graph_h);
	  Utils.drawRotatedString(g2d, y_max_label, ins_lft, ins_top + Utils.txtW(g2d, y_max_label));

	  // Create the counter context
	  counter_context = new BundlesCounterContext(bs, count_by, color_by);
          Map<String,Integer> key_to_sx = new HashMap<String,Integer>(),
	                      key_to_sy = new HashMap<String,Integer>();

          // Go through the bundles and figure out the placement
          Iterator<Bundle> it = bundle_to_wys.keySet().iterator();
          while (it.hasNext()) {
	    Bundle bundle = it.next();
            bundle_to_skeys.put(bundle, new HashSet<String>());
	    Iterator<Long> it_wy = bundle_to_wys.get(bundle).iterator();
	    while (it_wy.hasNext()) {
	      long wy = it_wy.next(); int sy = ins_top + (int) (graph_h * wy_to_ny.get(wy));
	      Iterator<Double> it_nx = bundle_to_nxs.get(bundle).iterator();
	      while (it_nx.hasNext()) {
	        double nx = it_nx.next(); int sx = x_cen + (int) (lobe_w*nx);
                String skey = sx + "," + sy;
		key_to_sx.put(skey,sx);
		key_to_sy.put(skey,sy);
		counter_context.count(bundle, skey);
                bundle_to_skeys.get(bundle).add(skey);
	      }
	    }
          }

	  // Now draw the picture
          Iterator<String> it_bin = counter_context.binIterator();
	  while (it_bin.hasNext()) {
	    String skey = it_bin.next();
	    int    sx   = key_to_sx.get(skey),
	           sy   = key_to_sy.get(skey);
            g2d.setColor(counter_context.binColor(skey));
	    Ellipse2D ellipse;
	    if (vsize) {
              double width = 1.8 + 10.0 * Math.sqrt(Math.abs(counter_context.total(skey))) / Math.sqrt(Math.abs(counter_context.totalMaximum()));
	      ellipse = new Ellipse2D.Double(sx-width/2,sy-width/2,width,width);
	      g2d.draw(ellipse);
            } else {
	      ellipse = new Ellipse2D.Double(sx-1,sy-1,3,3);
	      g2d.fill(ellipse);
	    }
	    geom_to_skey.put(ellipse, skey);
	    skey_to_geom.put(skey, ellipse);
	  }
        } else { 
	  String str = "Not Possible To Render";
          g2d.setColor(RTColorManager.getColor("label","default"));
	  g2d.drawString(str, getWidth()/2 - Utils.txtW(g2d,str)/2, getHeight()/2);
        } 
      } finally { if (g2d != null) g2d.dispose(); } // Clean up, return the image
      return (base_bi = bi);
      }
    }
  }
}

