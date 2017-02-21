/* 

Copyright 2016 David Trimm

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

import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;

import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.BundlesDT;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;

import racetrack.util.Utils;

import racetrack.visualization.RTColorManager;


/**
 * Displays distribution of a scalar field for the currently visible dataset.  Differs from histogram in
 * that values are sorted in their natural scalar order.  Differs from boxplot in that scalar ranges may
 * be grouped together into bins.
 *
 *@version 0.1
 */
public class RTDistributionPanel extends RTPanel {
  /**
   *
   */
  private static final long serialVersionUID = -6261148125010348467L;

  /**
   * Construct the distribution panel.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt application parent
   */
  public RTDistributionPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      { 
    super(win_type,win_pos,win_uniq,rt);   
    // Make the GUI
    add("Center",  component = new RTDistributionComponent());

    JPanel south_panel = new JPanel(new FlowLayout());
    south_panel.add(new JLabel("Distribution"));
    south_panel.add(value_cb = new JComboBox());
    add("South", south_panel);

    // Update the menu
    ButtonGroup bg = new ButtonGroup();
    getRTPopupMenu().add(stdev_buckets_rbmi  = new JRadioButtonMenuItem("Std Deviation Buckets", true));  bg.add(stdev_buckets_rbmi);  defaultListener(stdev_buckets_rbmi);
    getRTPopupMenu().add(divide_range_rbmi   = new JRadioButtonMenuItem("Div Range Buckets",     false)); bg.add(divide_range_rbmi);   defaultListener(divide_range_rbmi);
    getRTPopupMenu().add(equal_scalars_rbmi  = new JRadioButtonMenuItem("Equal Scalar Buckets",  false)); bg.add(equal_scalars_rbmi);  defaultListener(equal_scalars_rbmi);
    getRTPopupMenu().add(buckets_topped_rbmi = new JRadioButtonMenuItem("Top Off Buckets",       false)); bg.add(buckets_topped_rbmi); defaultListener(buckets_topped_rbmi);

    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(vary_color_cbmi  = new JCheckBoxMenuItem("Vary Color", false));  defaultListener(vary_color_cbmi);
    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(fixed_range_cbmi = new JCheckBoxMenuItem("Fixed Range",false));  defaultListener(fixed_range_cbmi);

    // Update the panels
    updateBys();
  }

  /**
   * Vary color checkbox menu item
   */
  JCheckBoxMenuItem    vary_color_cbmi,

  /**
   * Fixed range checkbox menu item
   */
                       fixed_range_cbmi;

  /**
   * Use a standard deviation approach to creating ranged buckets
   */
  JRadioButtonMenuItem stdev_buckets_rbmi,

  /**
   * Divide the range into equal divisions
   */
                       divide_range_rbmi,

  /**
   * Place the same number of scalars into each bucket (buckets will vary in width / range)
   */
                       equal_scalars_rbmi,

  /**
   * Evenly fill the buckets up...
   */
                       buckets_topped_rbmi;
                       
  /**
   * Value to calculate distribution... only scalars fit into here.
   */
  JComboBox value_cb;

  /**
   * Get the value field in the combobox.
   *
   *@return value field
   */
  public String valueField() { return (String) value_cb.getSelectedItem(); }

  /**
   * Set the value field combobox.
   *
   *@param str string to set the combobox
   */
  public void valueField(String str) { value_cb.setSelectedItem(str); }

  /**
   * Return the vary color flag value.
   *
   *@return true to vary color
   */
  public boolean varyColor() { return vary_color_cbmi.isSelected(); }

  /**
   * Set the vary color flag.
   *
   *@param b true to vary by color_by option
   */
  public void varyColor(boolean b) { vary_color_cbmi.setSelected(b); }

  /**
   * Get the fixed range flag.  Fixed range means that ranges will not be calculated dynamically -- instead, the last render will be used.
   *
   *@return fixed range flag
   */
  public boolean fixedRange() { return fixed_range_cbmi.isSelected(); }

  /**
   * Set the fixed range flag.  Fixed range means that ranges will not be calculated dynamically -- instead, the last render will be used.
   *
   *@param b new value for fixed range flag
   */
  public void fixedRange(boolean b) { fixed_range_cbmi.setSelected(b); }

  /**
   * RangeRender enumeration
   */
  enum RangeRender { StDevBuckets,
                     DivideRange,
		     EqualScalars,
		     BucketsTopped };

  /**
   * Return the range render setting.
   *
   *@return range render setting
   */
  public RangeRender rangeRender() { if      (stdev_buckets_rbmi.isSelected())  return RangeRender.StDevBuckets;
                                     else if (divide_range_rbmi.isSelected())   return RangeRender.DivideRange;
				     else if (equal_scalars_rbmi.isSelected())  return RangeRender.EqualScalars;
				     else if (buckets_topped_rbmi.isSelected()) return RangeRender.BucketsTopped;
				     else                                       return RangeRender.StDevBuckets;
  }

  /**
   * Set the range render setting.
   *
   *@param rr range render setting
   */
  public void rangeRender(RangeRender rr) {
    switch (rr) {
      case DivideRange:    divide_range_rbmi.setSelected(true);   break;
      case EqualScalars:   equal_scalars_rbmi.setSelected(true);  break;
      case BucketsTopped:  buckets_topped_rbmi.setSelected(true); break;
      case StDevBuckets:
      default:             stdev_buckets_rbmi.setSelected(true);  break;
    }
  }

  /**
   * Set the range render setting (string version).
   *
   *@param rr_str range render setting as a string
   */
  public void rangeRender(String rr_str) {
    if      (rr_str.equals(""+RangeRender.StDevBuckets))  rangeRender(RangeRender.StDevBuckets);
    else if (rr_str.equals(""+RangeRender.DivideRange))   rangeRender(RangeRender.DivideRange);
    else if (rr_str.equals(""+RangeRender.EqualScalars))  rangeRender(RangeRender.EqualScalars);
    else if (rr_str.equals(""+RangeRender.BucketsTopped)) rangeRender(RangeRender.BucketsTopped);
    else                                                  rangeRender(RangeRender.StDevBuckets);
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "distribution"; }

  /**
   * Update the dropdown boxes with scalar field updates.
   */
  @Override
  public void updateBys() {
    Object sel = value_cb.getSelectedItem();
    value_cb.removeAllItems();
    String strs[] = KeyMaker.scalarBlanks(getRTParent().getRootBundles().getGlobals());
    for (int i=0;i<strs.length;i++) value_cb.addItem(strs[i]);
    if (sel == null) value_cb.setSelectedIndex(0); else value_cb.setSelectedItem(sel);
  }

  /**
   * Get the configuration for this panel.  Planned to be used for bookmarking.
   *
   *@return string representation of this configuration
   */
  public String       getConfig    ()           { 
    return "RTDistributionPanel"                                         + BundlesDT.DELIM +
           "value_field="             + Utils.encToURL(valueField())     + BundlesDT.DELIM +
	   "vary_color="              + varyColor()                      + BundlesDT.DELIM +
	   "fixed_range="             + fixedRange()                     + BundlesDT.DELIM +
	   "range_render="            + Utils.encToURL(""+rangeRender());
  }

  /**
   * Set the configuration for this panel.  Could be used to recall bookmarks.
   *
   *@param str string representation for new configuration
   */
  public void         setConfig    (String str) {
    StringTokenizer st = new StringTokenizer(str, BundlesDT.DELIM);
    if (st.nextToken().equals("RTDistributionPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not A RTDistributionPanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st2.hasMoreTokens() ? st2.nextToken() : "";

      if      (type.equals("value_field"))          valueField(Utils.decFmURL(value));
      else if (type.equals("vary_color"))           varyColor(value.toLowerCase().equals("true"));
      else if (type.equals("fixed_range"))          fixedRange(value.toLowerCase().equals("true"));
      else if (type.equals("range_render"))         rangeRender(Utils.decFmURL(value));
      else throw new RuntimeException("Do Not Understand Type Value Pair \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * {@link JComponent} implementing the distribution visualization
   */
  public class RTDistributionComponent extends RTComponent {
    private static final long serialVersionUID = 822469822534318221L;
    @Override
    public Set<Shape>      allShapes()                     {
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      return set; }
    @Override
    public Set<Shape>  shapes(Set<Bundle> bundles) {
      Set<Shape> shapes = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return shapes;
      return shapes; }
    @Override
    public Set<Bundle> shapeBundles(Shape shape)       { 
      Set<Bundle> set = new HashSet<Bundle>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      return set; }
    @Override
    public Set<Shape>  overlappingShapes(Shape shape)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      return set; }
    public Set<Shape>  containingShapes(int x, int y)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      return set; }

    /**
     * Pull the current configurations from the view and instantiate
     * the renderer for this visualization.
     *
     *@param id render id used to abort superceded renderings
     */
    @Override
    public RTRenderContext render(short id) {
      clearNoMappingSet();
      Bundles    bs        = getRenderBundles();
      String     count_by  = getRTParent().getCountBy(),
                 color_by  = getRTParent().getColorBy();
      int last_min_value = 0,
          last_max_value = 100;
      RenderContext myrc = (RenderContext) rc; if (myrc != null) { last_min_value = myrc.min_value; last_max_value = myrc.max_value; }
      myrc = new RenderContext(id, bs, count_by, color_by, valueField(), varyColor(), fixedRange(), last_min_value, last_max_value, rangeRender(), getWidth(), getHeight());

      return myrc;
    }
    
    /**
     * RenderContext implementation for the GeoSpatial histogram.
     */
    public class RenderContext extends RTRenderContext {
      /**
       * Bundles/records for this rendering
       */
      Bundles bs; 

      /**
       * Width of component in pixels
       */
      int     rc_w, 

      /**
       * Height of the component in pixels
       */
              rc_h;

      /**
       * Count specification for how a bundle contributes thee view
       */
      String                count_by, 

      /**
       * Color variable for the rendering
       */
                            color_by,

      /**
       * Field for distribution calculation
       */
                            value_field;

      /**
       * Vary color flag
       */
      boolean               vary_color = false;

      /**
       * Range render option
       */
      RangeRender           range_render;

      /**
       * Counter context used by the rendered to accumulate
       * sums.
       */
      BundlesCounterContext counter_context;

      /**
       * Construct the rendering context for the distribution with the specified settings.
       *
       *@param id                 render id
       *@param bs                 bundles to render
       *@param count_by           how to count the record contribution to each country
       *@param color_by           color option based on global settings
       *@param w                  width for this render
       *@param h                  height for this render
       */
      public RenderContext(short id, Bundles bs, String count_by, String color_by, String value_field, boolean vary_color, boolean fixed_range, int last_min_value, int last_max_value, RangeRender range_render, int w, int h) {
	// Save the state variables
        render_id = id; this.bs = bs; this.rc_w = w; this.rc_h = h;
	this.count_by           = count_by;
	this.color_by           = color_by;
	this.value_field        = value_field;
	this.vary_color         = vary_color;
	if (fixed_range) { min_value = last_min_value; max_value = last_max_value; }
	this.range_render       = range_render;

        counter_context = new BundlesCounterContext(bs, count_by, color_by);

	// Organize all of the bundles into a reverse lookup map
	Iterator<Tablet> it_tab = bs.tabletIterator();
	while (it_tab.hasNext() && currentRenderID() == getRenderID()) {
          Tablet  tablet           = it_tab.next();
	  boolean tablet_can_count = count_by.equals(BundlesDT.COUNT_BY_BUNS) || KeyMaker.tabletCompletesBlank(tablet, count_by);
          if (KeyMaker.tabletCompletesBlank(tablet, value_field) && tablet_can_count) {
	    KeyMaker km = new KeyMaker(tablet, value_field);
	    Iterator<Bundle> it_bun = tablet.bundleIterator();
	    while (it_bun.hasNext() && currentRenderID() == getRenderID()) {
	      Bundle bun    = it_bun.next();
	      int    ints[] = km.intKeys(bun);
	      if (ints != null) for (int i=0;i<ints.length;i++) {
	        if (fixed_range == false) {
	          if (ints[i] < min_value) min_value = ints[i];
		  if (ints[i] > max_value) max_value = ints[i];
	          if (rmap.containsKey(ints[i]) == false) rmap.put(ints[i], new HashSet<Bundle>());
		  rmap.get(ints[i]).add(bun);
                } else if (ints[i] >= min_value && ints[i] <= max_value) {
	          if (rmap.containsKey(ints[i]) == false) rmap.put(ints[i], new HashSet<Bundle>());
		  rmap.get(ints[i]).add(bun);
		}
	      }
	    }
	  } else { addToNoMappingSet(tablet); }
	}
	range = max_value - min_value + 1;
      }

      /**
       * Minimum value found in this rendering
       */
      int min_value = Integer.MAX_VALUE,

      /**
       * Maximum value found in this rendering
       */
          max_value = Integer.MIN_VALUE,

      /**
       * Range of the values
       */
          range     = 1;

      /**
       * Reverse mapping from integers to their integer value
       */
      Map<Integer,Set<Bundle>> rmap = new HashMap<Integer,Set<Bundle>>();

      @Override
      public int           getRCHeight() { return rc_h; }

      @Override
      public int           getRCWidth()  { return rc_w; }

      /**
       * Base image for this render context
       */
      BufferedImage base_bi = null;

      @Override
      public BufferedImage getBase() { 
        if (base_bi == null) {
	 Graphics2D g2d = null;
	 try {
          base_bi         = new BufferedImage(rc_w, rc_h, BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) base_bi.getGraphics();
	  g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	  RTColorManager.renderVisualizationBackground(base_bi, g2d);
	  int     txt_h   = Utils.txtH(g2d,"0");
          
	  // Figure out the graph dimensions
	  graph_w = rc_w - lft_ins - rgt_ins;
	  graph_h = rc_h - top_ins - bot_ins;

          // Determine if we can do one for one...  4pix to one... or have to start grouping into ranged bins
          if        (graph_w >= 10 * range) { /* 10 pixel bars */ oneToOne(10, g2d);
          } else if (graph_w >= 8  * range) { /* 8  pixel bars */ oneToOne(8,  g2d);
          } else if (graph_w >= 4  * range) { /* 4  pixel bars */ oneToOne(4,  g2d);
	  } else if (graph_w >= 2  * range) { /* 2  pixel bars */ oneToOne(2,  g2d);
	  } else                            { /* ranged bins   */ 
	    switch (range_render) {
	      // --------------------------------------------------------------------------------------
	      case DivideRange:   rangedBinsDivideRange(g2d);  break;
	      // --------------------------------------------------------------------------------------
	      case EqualScalars:  equalScalars(g2d);           break;
	      // --------------------------------------------------------------------------------------
	      case BucketsTopped: bucketsTopped(g2d);          break;
	      // --------------------------------------------------------------------------------------
	      case StDevBuckets:
	      default:            rangedBinsStDevBuckets(g2d); break;
	    }
	  }

	  // Draw the axes
          g2d.setColor(RTColorManager.getColor("axis", "major"));
	  g2d.drawLine(lft_ins, top_ins,           lft_ins,           top_ins + graph_h);
	  g2d.drawLine(lft_ins, top_ins + graph_h, lft_ins + graph_w, top_ins + graph_h);

	  g2d.setColor(RTColorManager.getColor("label", "default"));
	  g2d.drawString(min_label, lft_ins,                                        top_ins + graph_h + txt_h + 2);
	  g2d.drawString(max_label, lft_ins + graph_w - Utils.txtW(g2d, max_label), top_ins + graph_h + txt_h + 2);
	  String str = "" + ((int) counter_context.totalMaximum());
          Utils.drawRotatedString(g2d, str, lft_ins, top_ins + Utils.txtW(str));

	 } finally { if (g2d != null) g2d.dispose(); }
        }
        return base_bi;
      }

      /**
       * left insert
       */
      final int lft_ins = 20,
      /**
       * bottom insert
       */
                bot_ins = 20,

      /**
       * top insert
       */
                top_ins = 10,

      /**
       * right insert
       */
                rgt_ins = 10;

      /**
       * graph width
       */
      int       graph_w,

      /**
       * graph height
       */
                graph_h;

      /**
       * Maximum label
       */
      String    max_label = "Not Set",

      /**
       * Minimum label
       */
                min_label = "Not Set";

      /**
       * Use a one to one bar version - to include specified bar_width...  one to one means that each distribution
       * bin maps to one exact scalar value (versus the ranged version).
       *
       *@param bar_w width of the bar
       *@param g2d   graphics primitive
       */
      protected void oneToOne(int bar_w, Graphics2D g2d) {
        Map<String,Integer> bin_to_x = new HashMap<String,Integer>(),
	                    bin_to_w = new HashMap<String,Integer>();

	// Accumulate into a counter context
        Iterator<Integer> it = rmap.keySet().iterator(); while (it.hasNext()) {
	  int x = it.next(); Set<Bundle> set = rmap.get(x); // Get the value... get the bundles
	  x = lft_ins + (x - min_value) * bar_w;            // Adjust the value into a coordinate
	  String bin = "" + x; bin_to_x.put(bin,x); bin_to_w.put(bin, bar_w);
	  Iterator<Bundle> it_bun = set.iterator(); while (it_bun.hasNext()) { counter_context.count(it_bun.next(), bin); }
	}

	// Set the labels
	min_label = "" + min_value;
	max_label = "" + max_value;

	// Run the generic renderer
	genericRender(bin_to_x, bin_to_w, g2d);
      }

      /**
       * Generic render
       */
      protected void genericRender(Map<String,Integer> bin_to_x, Map<String,Integer> bin_to_w, Graphics2D g2d) {
	// If vary color, draw the graph with colors... otherwise, just black and white
        if (vary_color && color_by != null) {
	//
	// Vary color... more challenging (copied roughly from RTHistoPanel)
	//
	  List<String> cbins = counter_context.getColorBinsSortedByCount();
	  Iterator<String> it_str = counter_context.binIterator(); while (it_str.hasNext()) {
            String bin = it_str.next(); int x = bin_to_x.get(bin); int bar_w = bin_to_w.get(bin);
	    int bin_h = (int) (graph_h * counter_context.totalNormalized(bin));
	    Rectangle2D rect = new Rectangle2D.Double(x, top_ins + graph_h - bin_h, bar_w, bin_h); int y_inc = top_ins + graph_h;
	    for (int i=cbins.size()-1;i>=0;i--) {
	      String cbin   = cbins.get(i);
	      double ctotal = counter_context.total(bin,cbin);
	      if (ctotal > 0L) {
                int sub_w = (int) ((ctotal * bin_h)/counter_context.binColorTotal(bin));
	        if (sub_w > 0) {
                  g2d.setColor(RTColorManager.getColor(cbin));
		  y_inc -= sub_w;
		  g2d.fillRect(x, y_inc, bar_w - 1, sub_w);
                }
	      }
	    }
	  }

	} else          {
	//
	// Fixed color - easiest case...
	//
	  g2d.setColor(RTColorManager.getColor("data", "default"));
	  double total_max = counter_context.totalMaximum();
	  Iterator<String> it_str = counter_context.binIterator(); while (it_str.hasNext()) {
	    String bin = it_str.next(); int x = bin_to_x.get(bin); int bar_w = bin_to_w.get(bin);
	    double total_bin    = counter_context.total(bin);
	    int    bin_h        = (int) ((total_bin * graph_h)/total_max);
	    Rectangle2D rect    = new Rectangle2D.Double(x, top_ins + graph_h - bin_h, bar_w,     bin_h);
	    Rectangle2D rect_m1 = new Rectangle2D.Double(x, top_ins + graph_h - bin_h, bar_w - 1, bin_h);
	    g2d.fill(rect_m1);
	  }
	}
      }

      /**
       * Construct the buckets so that they have about the same number of scalar values in them.
       *
       *@param g2d graphics primitive
       */
      protected void equalScalars(Graphics2D g2d) {
	// Get all the values and sort them
        int vals[] = new int[rmap.keySet().size()]; Iterator<Integer> it = rmap.keySet().iterator(); 
	for (int i=0;i<vals.length;i++) vals[i] = it.next();
	Arrays.sort(vals);

	min_label = "" + vals[0];
	max_label = "" + vals[vals.length-1];

        // Put them into bins
	int bar_w_guess  = 8, 
	    min_bar_w    = 3,
	    num_of_bins  = graph_w/bar_w_guess, 
	    vals_per_bin = vals.length/num_of_bins; if (vals_per_bin < 1) vals_per_bin = 1;
        List<List<Integer>> bins = new ArrayList<List<Integer>>();
        bins.add(new ArrayList<Integer>());
        for (int i=0;i<vals.length;i++) {
          if (bins.get(bins.size()-1).size() == vals_per_bin) bins.add(new ArrayList<Integer>());
          bins.get(bins.size()-1).add(vals[i]);
        }

        // Stretch them across the axis
	Map<String,Integer> bin_to_x = new HashMap<String,Integer>(),
	                    bin_to_w = new HashMap<String,Integer>();
	int x0           = lft_ins,             // minimum x value
	    x0_val       = vals[0],             // value at minimum x value
	    xm           = lft_ins + graph_w,   // maximum x value
	    xm_val       = vals[vals.length-1]; // value at maximum x value
        for (int i=0;i<bins.size();i++) {
	  int x = x0, x_val = bins.get(i).get(0), x_next, x_next_val;

	  if (i == bins.size()-1) { x_next = xm; x_next_val = xm_val; } else {
	    x_next_val = bins.get(i+1).get(0);
	    x_next     = prop(x0, x0_val, xm, xm_val, x_next_val);
	    if ((x_next - x) < min_bar_w) x_next = x + min_bar_w;
	  }

	  // Create a bin key and set the x position and width
	  String bin_key = "" + bins.get(i).get(0) + "..." + bins.get(i).get(bins.get(i).size()-1);
	  bin_to_x.put(bin_key, x);
	  bin_to_w.put(bin_key, x_next - x);

	  // Add up the bundles for this bin
	  for (int j=0;j<bins.get(i).size();j++) {
	    int val = bins.get(i).get(j);
	    Set<Bundle> set = rmap.get(val);
	    Iterator<Bundle> it_bun = set.iterator(); while (it_bun.hasNext()) { counter_context.count(it_bun.next(), bin_key); }
	  }

	  // For the next iteration, set the beginning
	  x0 = x_next; x0_val = x_next_val;
        }

	// Render the configuration
	genericRender(bin_to_x, bin_to_w, g2d);
      }

      /**
       * Return a proportional distance along an x axis given two other points on the line.
       */
      private int prop(int x0, int x0_val, int xm, int xm_val, int x_val) {
        return x0 + (int) (((xm - x0) * (x_val - x0_val)) / (xm_val - x0_val));
      }

      /**
       * Construct the buckets so that they are all about equally full with the count value.
       *
       *@param g2d graphics primitive
       */
      protected void bucketsTopped(Graphics2D g2d) {

      }

      /**
       * Use ranged bins.
       *
       * stdev help:  (from wikipedia.org)
       * - 1 stdev == 34.1 x 2
       * - 2 stdev == 13.6 x 2
       * - 3 stdev ==  2.1 x 2
       * - ...     ==  0.1 x 2
       */
      protected void rangedBinsStDevBuckets(Graphics2D g2d) {
        int    bar_w = graph_w/100; if (bar_w < 1) bar_w = 1;
        double avg   = calculateAverage(), 
               std   = calculateStDev();
        Map<String,Integer> bin_to_x = new HashMap<String,Integer>(), // records min x for bin
	                    bin_to_w = new HashMap<String,Integer>();

	// divide the bins up by percents from the 3x standard deviations
        for (int i=1;i<50;i++) {
	  int    x_l0 = lft_ins + bar_w * (50 - i), // Before the stdev
	         x_l1 = lft_ins + bar_w * (51 - i),

	         x_u0 = lft_ins + bar_w * (49 + i), // After the stdev
	         x_u1 = lft_ins + bar_w * (50 + i);

	  double s_l0 = avg - std * (i-0) * 3 / 100.0,
	         s_l1 = avg - std * (i-1) * 3 / 100.0,

		 s_u0 = avg + std * (i-1) * 3 / 100.0,
		 s_u1 = avg + std * (i-0) * 3 / 100.0;

          if (i == 49) { 
	    if (min_value < s_l0) s_l0 = min_value - 0.1; 

	    min_label = "" + (int) Math.floor(s_l0);

	    if (max_value > s_u1) s_u1 = max_value + 0.1; 

	    max_label = "" + (int) Math.ceil (s_u1);
	  }

          String bin_l = "" + s_l0 + " " + s_l1,
	         bin_u = "" + s_u0 + " " + s_u1;

          bin_to_x.put(bin_l, x_l0); bin_to_w.put(bin_l, bar_w);
          bin_to_x.put(bin_u, x_u0); bin_to_w.put(bin_u, bar_w);

	  Set<Integer> handled = new HashSet<Integer>();
          Iterator<Integer> it = rmap.keySet().iterator(); while (it.hasNext()) {
	    int scalar = it.next(); if (handled.contains(scalar)) continue; // Slight possibility a set will overlap...
	    Set<Bundle> set = rmap.get(scalar); handled.add(scalar);
	    if        (scalar >= s_l0 && scalar <= s_l1) { Iterator<Bundle> it_bun = set.iterator(); while (it_bun.hasNext()) { counter_context.count(it_bun.next(), bin_l); }
	    } else if (scalar >= s_u0 && scalar <= s_u1) { Iterator<Bundle> it_bun = set.iterator(); while (it_bun.hasNext()) { counter_context.count(it_bun.next(), bin_u); } }
	  }
	}

	// Run the generic renderer
	genericRender(bin_to_x, bin_to_w, g2d);
      }

      /**
       * Average value - set to NaN if not yet calculated
       */
      double average = Double.NaN;

      /**
       * Calculate the average
       */
      protected double calculateAverage() {
        if (Double.isNaN(average) == false) return average;
        double sum = 0.0; int samples = 0;
        Iterator<Integer> it = rmap.keySet().iterator(); while (it.hasNext()) {
	  int scalar = it.next(); int count = rmap.get(scalar).size();
	  sum += scalar * count; samples += count;
	}
	if (samples == 0) samples = 1;
	return (average = (sum / samples));
      }

      /**
       * Standard deviation - set to NaN if not yet calculated
       */ 
      double stdev = Double.NaN;

      /**
       * Calculate the standard deviation
       */
      protected double calculateStDev() {
        if (Double.isNaN(stdev) == false) return stdev;
        double avg = calculateAverage();
        double sum = 0.0; int samples = 0;
        Iterator<Integer> it = rmap.keySet().iterator(); while (it.hasNext()) {
	  int scalar = it.next(); int count = rmap.get(scalar).size();
	  for (int i=0;i<count;i++) sum += (scalar - avg) * (scalar - avg);
	  samples += count;
	}
	if (samples == 0) samples = 1;
	return (stdev = Math.sqrt(sum/samples));
      }

      /**
       * Divide the space up equally (both in screen realestate and in world coords)
       *
       *@param g2d graphics primitive
       */
      protected void rangedBinsDivideRange(Graphics2D g2d) {
        Map<String,Integer> bin_to_x = new HashMap<String,Integer>(),
	                    bin_to_w = new HashMap<String,Integer>();
        int bar_w    = 4;
        int bin_size = (max_value - min_value + 1) / (rc_w / bar_w);
        Iterator<Integer> it = rmap.keySet().iterator(); while (it.hasNext()) {
	  int    scalar = it.next(); 
	  int    x      = (lft_ins + bar_w * (scalar - min_value) / (rc_w / bar_w));
	  String bin    = "" + x; bin_to_x.put(bin,x); bin_to_w.put(bin,bar_w);
	  Set<Bundle> set = rmap.get(scalar);
	  Iterator<Bundle> it_bun = set.iterator(); while (it_bun.hasNext()) { counter_context.count(it_bun.next(), bin); }
        }
	genericRender(bin_to_x, bin_to_w, g2d);
      }
    }
  }
}

