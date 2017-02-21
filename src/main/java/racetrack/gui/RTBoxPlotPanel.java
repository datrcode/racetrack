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

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import java.text.SimpleDateFormat;

import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.BundlesDT;
import racetrack.framework.BundlesG;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;
import racetrack.util.Utils;
import racetrack.visualization.RTColorManager;

/**
 * Visualization for showing the boxplot for an entity or time period.
 * Box plots communicate the min, max, standard deviation, and median.
 *
 * Version 1.1.  Updated box and whiskers to Tufte recommended plots
 *
 * Version 1.2.  Fixed quartile locations so that they don't use stdev...
 *               instead they're based on the the 9% and 91% data marks.
 *
 *@author  D. Trimm
 *@version 1.2
 */
public class RTBoxPlotPanel extends RTPanel {
  /**
   * X-axis binning option
   */
  JComboBox            bin_cb, 

  /**
   * X-axis sub-binning option...  can be none
   */
                       bin2_cb, 

  /**
   * Scalar field for y-axis
   */
		       y_cb;

  /**
   * Checkbox to render labels (otherwise, none)
   */
  JCheckBoxMenuItem    labels_cbmi;

  /**
   * Methods to scale x-axis
   */
  JRadioButtonMenuItem x_scales[],

  /**
   * Methods to scale y-axis - only linear works for boxplot mode
   */
                       y_scales[],

  /**
   * Modes for visualization
   */
                       modes[];

  /**
   * Strings representing scaling options for axes
   */
  final static String  LINEAR_SCALE_STR       = "Linear",
                       LOG_SCALE_STR          = "Log",
		       EQUAL_SCALE_STR        = "Equal";

  /**
   * Array for x-axis scale
   */
  final static String scale_strs[] = { LINEAR_SCALE_STR, LOG_SCALE_STR, EQUAL_SCALE_STR };

  /** 
   * Modes for the application
   */
  final static String MODE_SUM_STR                = "Bar Chart",
                      MODE_ENTROPY_STR            = "Entropy",
                      MODE_BOXPLOT_STR            = "Box Plot", // Used as a substring match !!!
                      MODE_BOXPLOT_PER_MINUTE_STR = MODE_BOXPLOT_STR + " (Minute)",
                      MODE_BOXPLOT_PER_HOUR_STR   = MODE_BOXPLOT_STR + " (Hour)",
                      MODE_BOXPLOT_PER_DAY_STR    = MODE_BOXPLOT_STR + " (Day)",
		      MODE_BOXPLOT_PER_MONTH_STR  = MODE_BOXPLOT_STR + " (Month)";

  /**
   * Array of the mode strings
   */ 
  final static String mode_strs[] = { MODE_SUM_STR, MODE_ENTROPY_STR, MODE_BOXPLOT_STR, MODE_BOXPLOT_PER_MINUTE_STR,
                                      MODE_BOXPLOT_PER_HOUR_STR, MODE_BOXPLOT_PER_DAY_STR,
                                      MODE_BOXPLOT_PER_MONTH_STR };

  /**
   * Construct a new panel with the specified GUI parent.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt      GUI parent
   *@param master  determines if this panel is the master in a master/slave paradigm
   */
  public RTBoxPlotPanel                 (RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt, boolean master) { 
    super(win_type,win_pos,win_uniq,rt);   
    // Main component
    add("Center", component = new RTBoxPlotComponent());

    // Popup menu
    if (master) { getRTPopupMenu().add(labels_cbmi = new JCheckBoxMenuItem("Draw Labels")); }

    // Scales
    getRTPopupMenu().addSeparator();
    JMenu submenu; ButtonGroup bg;

    int j = 0;
    if (master) {
      submenu = new JMenu("X Scale"); bg = new ButtonGroup(); getRTPopupMenu().add(submenu);
      x_scales = new JRadioButtonMenuItem[scale_strs.length-1]; for (int i=0;i<scale_strs.length;i++) { 
        if (!scale_strs[i].equals(LOG_SCALE_STR)) {
          x_scales[j] = new JRadioButtonMenuItem(scale_strs[i], i == 0); 
          submenu.add(x_scales[j]); bg.add(x_scales[j]); defaultListener(x_scales[j]); j++; } }
    }

    j = 0;
    submenu = new JMenu("Y Scale"); bg = new ButtonGroup(); getRTPopupMenu().add(submenu);
    y_scales = new JRadioButtonMenuItem[scale_strs.length-1]; for (int i=0;i<scale_strs.length;i++) { 
      if (!scale_strs[i].equals(EQUAL_SCALE_STR)) {
        y_scales[j] = new JRadioButtonMenuItem(scale_strs[i], i == 0); 
        submenu.add(y_scales[j]); bg.add(y_scales[j]); defaultListener(y_scales[j]); j++; } }

    // Modes
    getRTPopupMenu().addSeparator(); bg = new ButtonGroup();
    modes = new JRadioButtonMenuItem[mode_strs.length]; for (int i=0;i<modes.length;i++) {
      modes[i] = new JRadioButtonMenuItem(mode_strs[i], i == 0); getRTPopupMenu().add(modes[i]); bg.add(modes[i]); defaultListener(modes[i]);
    }    

    // Configuration panel
    JPanel panel = new JPanel(new FlowLayout());
    if (master) {
      panel.add(new JLabel("X Bin")); panel.add(bin_cb  = new JComboBox());
      panel.add(new JLabel("X Sub")); panel.add(bin2_cb = new JComboBox());
    }
    panel.add(new JLabel("Y"));     panel.add(y_cb    = new JComboBox());

    // Add the default listeners
    if (master) { defaultListener(bin_cb); defaultListener(bin2_cb); defaultListener(labels_cbmi); }
    defaultListener(y_cb);

    // Fill the comboboxes
    updateBys();
    add("South", panel);
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "boxplot"; }

  /**
   * Link the controls of this panel to the specified master panel.
   *
   *@param master master panel that contains the overriding controls
   */
  public void linkControls(RTBoxPlotPanel master) {
    bin_cb      = master.bin_cb;      defaultListener(bin_cb);
    bin2_cb     = master.bin2_cb;     defaultListener(bin2_cb);
    labels_cbmi = master.labels_cbmi; defaultListener(labels_cbmi);
    x_scales    = new JRadioButtonMenuItem[master.x_scales.length];
    for (int i=0;i<x_scales.length;i++) {
      x_scales[i] = master.x_scales[i];
      defaultListener(x_scales[i]);
    }
  }

  /**
   * Get the bin for the x-axis
   *
   *@return bin for x-axis
   */
  public String       bin           ()           { return (String) bin_cb.getSelectedItem(); }

  /**
   * Set the bin for the x-axis
   *
   *@param str bin for x-axis
   */
  public void         bin           (String str) { bin_cb.setSelectedItem(str); }

  /**
   * Get the sub-bin for the x-axis
   *
   *@return sub-bin for x-axis
   */
  public String       bin2          ()           { 
    // If the primary bin is based on time, then no secondardy binning is possible...
    String bin_str = bin();
    if (bin_str != null && bin_str.startsWith("|Tm|")) return BundlesDT.COUNT_BY_NONE;

    return (String) bin2_cb.getSelectedItem(); 
  }

  /**
   * Set the sub-bin for the x-axis
   *
   *@param str sub-bin for x-axis
   */
  public void         bin2          (String str) { bin2_cb.setSelectedItem(str); }

  /**
   * Return the y-axis scalar field.
   *
   *@return y-axis scalar field
   */
  public String       yAxis         ()           { return (String) y_cb.getSelectedItem(); }

  /**
   * Set the y-axis scalar field.
   *
   *@param str y-axis scalar field
   */
  public void         yAxis         (String str) { y_cb.setSelectedItem(str); }

  /**
   * Return the scale to use on the x-axis
   *
   *@return x-axis scale
   */
  public String       xScale        ()           { return selectedItem(x_scales); }

  /**
   * Set the scale to use on the x-axis.
   *
   *@param str x-axis scale
   */
  public void         xScale        (String str) { setSelectedItem(x_scales, str); }

  /**
   * Return the scale to use on the y-axis.  Only applies to non-box plot modes.
   *
   *@return y-axis scale
   */
  public String       yScale        ()           { return selectedItem(y_scales); }

  /**
   * Set the scale to usse on the y-axis
   *
   *@param str y-axis scale
   */
  public void         yScale        (String str) { setSelectedItem(y_scales, str); }

  /**
   * Return the mode of the application.  Currently supports box-plot or counting modes.  Could
   * eventually incorporate sums, maxes, mins, standard deviations, etc.
   *
   *@return application mode
   */
  public String       mode          ()           { return selectedItem(modes); }

  /**
   *
   */
  public void         mode          (String str) { setSelectedItem(modes, str); }

  /**
   * Determine if labels should be rendered in visualization.
   *
   *@return true if labels should be rendered
   */
  public boolean      drawLabels    ()           { return labels_cbmi.isSelected(); }

  /**
   * Set the flag for labels to be rendered.
   *
   *@param f true to draw labels
   */
  public void         drawLabels    (boolean f)  { labels_cbmi.setSelected(f); }

  /**
   * Get the configuration of this component as a string.  Supposed to be used for
   * bookmarking a view so that it can be re-rendered.
   *
   *@return string representing configuration of the panel
   */
  public String  getConfig()  { return "RTBoxPlotPanel" + BundlesDT.DELIM +
                                       "mode="      + Utils.encToURL(mode())   + BundlesDT.DELIM +
                                       "bin="       + Utils.encToURL(bin())    + BundlesDT.DELIM +
                                       "bin2="      + Utils.encToURL(bin2())   + BundlesDT.DELIM +
                                       "xscale="    + Utils.encToURL(xScale()) + BundlesDT.DELIM +
                                       "yaxis="     + Utils.encToURL(yAxis())  + BundlesDT.DELIM +
				       "yscale="    + Utils.encToURL(yScale()) + BundlesDT.DELIM +
				       "labels="    + (drawLabels() ? "true" : "false"); }

  /**
   * Set the configuration of this component from the string representation.
   *
   *@return previously returned string from the getConfig() method
   */
  public void    setConfig(String str) {
    StringTokenizer st = new StringTokenizer(str,BundlesDT.DELIM);
    if (st.nextToken().equals("RTBoxPlotPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not A RTBoxPlotPanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st.hasMoreTokens() ? st2.nextToken() : "";
      if      (type.equals("mode"))    mode(Utils.decFmURL(value));
      else if (type.equals("bin"))     bin(Utils.decFmURL(value));
      else if (type.equals("bin2"))    bin2(Utils.decFmURL(value));
      else if (type.equals("xscale"))  xScale(Utils.decFmURL(value));
      else if (type.equals("yaxis"))   yAxis(Utils.decFmURL(value));
      else if (type.equals("yscale"))  yScale(Utils.decFmURL(value));
      else if (type.equals("labels"))  drawLabels(value.toLowerCase().equals("true"));
      else throw new RuntimeException("Do Not Understand Type-Value Pair \"" + type + "\"=\"" + value + "\"");
    }
  }

  /**
   * Update the comboboxes for selecting global fields when new data is
   * loaded.
   */
  public void         updateBys() { 
    if (bin_cb  != null) updateEntityBys(bin_cb); 
    if (bin2_cb != null) updateEntityBys(bin2_cb); 
    if (y_cb    != null) updateScalarBys(y_cb); 
  }

  /**
   * Generic method to update a combobox.
   *
   *@param cb combobox to update
   */
  public void         updateEntityBys(JComboBox cb) {
    String strs[]; Object sel = cb.getSelectedItem();
    cb.removeAllItems();
    if (cb == bin2_cb) strs = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals(), false, true, true, false);
    else               strs = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals(), false, true, true, true);
    if (cb == bin2_cb) cb.addItem(BundlesDT.COUNT_BY_NONE);
    for (int i=0;i<strs.length;i++) cb.addItem(strs[i]);
    if (sel == null) cb.setSelectedIndex(0); else cb.setSelectedItem(sel);
  }

  /**
   * Generic method to update a combobox with just scalar fields.
   *
   *@param cb combobox to update
   */
  public void         updateScalarBys(JComboBox cb) {
    String strs[]; Object sel = cb.getSelectedItem();
    cb.removeAllItems();
    strs = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals(), true, true, true, false);
    cb.addItem(BundlesDT.COUNT_BY_DEFAULT);
    for (int i=0;i<strs.length;i++) cb.addItem(strs[i]);
    if (sel == null) cb.setSelectedIndex(0); else cb.setSelectedItem(sel);
  }

  /**
   * Component that handles painting and interacting with the visualization.
   */
  public class RTBoxPlotComponent extends RTComponent implements KeyListener {
    /**
     * Key pressed interface for KeyListener.  Just passes to the parent for handling...
     *
     *@param ke key event
     */
    public void keyPressed(KeyEvent ke) { super.keyPressed(ke); }

    /**
     * Key released interface for KeyListener.  Just passes to the parent for handling...
     *
     *@param ke key event
     */
    public void keyReleased(KeyEvent ke) { super.keyReleased(ke); }

    /**
     *
     */
    public void keyTyped(KeyEvent ke) { 
      super.keyTyped(ke);
      if (ke.getKeyChar() == ' ') {
        if (yScale().equals(LINEAR_SCALE_STR)) yScale(LOG_SCALE_STR);
	else                                   yScale(LINEAR_SCALE_STR);
      }
    }

    /**
     * Copy a screenshot of the rendering to the clipboard.
     *
     *@param shft shift key down
     *@param alt  alt key down
     */
    @Override
    public void copyToClipboard    (boolean shft, boolean alt) {
      RenderContext myrc = (RenderContext) getRTComponent().rc;
      if (shft == true && myrc != null)  Utils.copyToClipboard(myrc.getBase());
    }

    /**
     * Return all of the shapes in the current rendering.
     *
     *@return set of rendered shapes
     */
    public Set<Shape>      allShapes()                     { 
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return new HashSet<Shape>();
      return myrc.geom_to_key.keySet();
    }

    /**
     * Return the rendered shapes that correspond to the specified bundles.
     *
     *@param  bundles bundles/records to match for shapes
     *
     *@return set of shapes that correspond to the specified bundles
     */
    public Set<Shape>  shapes(Set<Bundle> bundles) { 
      Set<Shape> shapes = new HashSet<Shape>();
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return shapes;
      Iterator<Bundle> it = bundles.iterator();
      while (it.hasNext()) {
        Bundle bundle = it.next();
        if (myrc.bundle_to_keys.containsKey(bundle)) {
	  Iterator<String> it_key = myrc.bundle_to_keys.get(bundle).iterator();
	  while (it_key.hasNext()) {
	    String key = it_key.next();
	    shapes.add(myrc.key_to_geom.get(key));
	  }
	}
      }
      return shapes;
    }

    /**
     * Return the bundles records associated with the specified shape.  Note that the shape
     * cannot be generic and must have been returned by this component.
     *
     *@param  shape shape to lookup for records
     *
     *@return set of bundles that correspond to the shape
     */
    public Set<Bundle> shapeBundles(Shape shape)       {
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return new HashSet<Bundle>();
      Set<Bundle> set = new HashSet<Bundle>();
      String key = myrc.geom_to_key.get(shape);
      if (key != null) {
        Set<Bundle> set_to_add = myrc.counter_context.getBundles(key);
        if (set_to_add != null) set.addAll(set_to_add);
        else System.err.println("RTBoxPlotPane.shapeBundles() - null pointer for key \"" + key + "\"");
      }
      else System.err.println("Key For \"" + shape + "\" Is Null");
      return set;
    }

    /**
     * Find the rendered shapes that overlap with the specified shape.  Note that the specified
     * shape can be generic.
     *
     *@param  shape general shape to match against rendered shapes
     *
     *@return rendered shapes that overlap with the specified shape
     */
    public Set<Shape>  overlappingShapes(Shape shape)  { 
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return new HashSet<Shape>();
      Set<Shape> shapes = new HashSet<Shape>();
      Iterator<Shape> it = myrc.geom_to_key.keySet().iterator();
      while (it.hasNext()) {
        Shape rendered_shape = it.next();
	if (Utils.genericIntersects(shape, rendered_shape)) shapes.add(rendered_shape);
      }
      return shapes;
    }

    /**
     * Return the rendered shapes that contain the specified x and y coordinate.
     *
     *@param  x x-coordinate
     *@param  y y-coordinate
     *
     *@return set of shapes that contain the x/y coordinate
     */
    public Set<Shape>  containingShapes(int x, int y)  { 
      Set<Shape> shapes = new HashSet<Shape>();
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return shapes;
      Iterator<Shape> it = myrc.geom_to_key.keySet().iterator();
      while (it.hasNext()) {
        Shape rendered_shape = it.next();
	if (rendered_shape.contains(x,y)) shapes.add(rendered_shape);
      }
      return shapes;
    }

    /**
     * Return the shape used to match shapes directly under the mouse.
     *
     *@param  x x-coordinate of mouse
     *@param  y y-coordinate of mouse
     *
     *@return shape under mouse
     */
    public Shape getZeroOrderShape(int x, int y) { return new Rectangle2D.Double(x,0,1,getHeight()); }

    /**
     * Return the shape used to match shapes directly near the mouse.
     *
     *@param  x x-coordinate of mouse
     *@param  y y-coordinate of mouse
     *
     *@return shape near mouse
     *
     */
    public Shape getFirstOrderShape(int x, int y) { return new Rectangle2D.Double(x,y,1,1); }

    /**
     * Return the shape used to match shapes directly further from the mouse.
     *
     *@param  x x-coordinate of mouse
     *@param  y y-coordinate of mouse
     *
     *@return shape further from mouse mouse
     */
    public Shape getSecondOrderShape(int x, int y) { return new Rectangle2D.Double(x,y,1,1); }

    /**
     * Create a render context with the specified render ID.  The render context will be used to 
     * create the actual visualization.  The render id ensures that unused/unneeded visualization
     * renderings will be canceled.
     *
     *@param  id render id for aborting unnecessary renders
     *
     *@return    render context based on visible dataset and GUI parameters
     */
    public RTRenderContext render(short id) {
      clearNoMappingSet();
      Bundles bs       = getRenderBundles();
      String  count_by = getRTParent().getCountBy(),
              color_by = getRTParent().getColorBy();
      String  mode     = mode(),
              bin_hdr  = bin(),
              bin2_hdr = bin2(),
	      x_scale  = xScale(),
              y_axis   = yAxis(),
	      y_scale  = yScale();
      if (y_axis == null || y_axis.equals(BundlesDT.COUNT_BY_DEFAULT)) y_axis = count_by;
      if (bin2_hdr != null && bin2_hdr.equals(BundlesDT.COUNT_BY_NONE)) bin2_hdr = null;
      if (bs != null && count_by != null && bin_hdr != null && y_axis != null) {
        RenderContext myrc = new RenderContext(id, bs, count_by, color_by, mode, bin_hdr, bin2_hdr, x_scale, y_axis, y_scale, drawLabels(), getWidth(), getHeight());
        return myrc;
      } else return null;
    }

    /**
     * Class to perform the actual rendering of the view.  This class uses several additional inner classes
     * to map the axes and to perform statistical operations on the data.
     */
    public class RenderContext extends RTRenderContext {
      /**
       * Data set to render
       */
      Bundles bs; 
      /**
       * Width (in pixels) of the rendering
       */
      int     w, 
      /**
       * Height (in pixels) of the rendering
       */
              h; 
      /**
       * Field used to count the entities by (for width of icon in xy grid)
       */
      String  count_by, 
      /**
       * Field used to color the entities
       */
              color_by, 
      /**
       * Mode to use - boxplot, count/sum
       */
              mode,
      /**
       * Entity to use for the scatter plot
       */
	      bin_hdr, 
      /**
       * Sub-entity to use for the scatter plot
       */
	      bin2_hdr, 
      /**
       * Scale to use for the x-axis
       */
	      x_scale, 
      /**
       * Scalar field for the y-axis
       */
	      y_axis,
      /**
       * Scale to use for the y-axis
       */
              y_scale;
      /**
       * Flag to indicate to draw labels
       */
      boolean draw_labels;
      /**
       * Counter context for each point in the scatter plot
       */
      BundlesCounterContext            counter_context;
      /**
       * X inset for rendering
       */
      int                              x_ins, 
      /**
       * Y inset for rendering
       */
                                       y_ins, 
      /**
       * XY Graph width in pixels
       */
				       graph_w, 
      /**
       * XY Graph height in pixels
       */
				       graph_h;

      /**
       * Map to translate the x-axis entity into the screen x-coordinate
       */
      Mapper                           xmap;

      /**
       * Special key maker state if the scale is time-based (needed later for labels)...
       */
      KeyMaker                         time_km = null;

      /**
       * map from the geometry to the associated key
       */
      Map<Shape,String>  geom_to_key = new HashMap<Shape,String>();

      /**
       * Map from the panel key to the asssociated geometry
       */
      Map<String,Shape>  key_to_geom = new HashMap<String,Shape>();

      /**
       *
       */
      Map<Bundle,Set<String>> bundle_to_keys = new HashMap<Bundle,Set<String>>();

      /**
       * String capturing errors during the render process.  Should be recast as a set of errors.
       */
      String error_str = "";

      /**
       * Construct the render context with the specified dataset and GUI configurations.  Use the render ID to 
       * ensure that out-of-date renderings are canceled as soon as possible.
       *
       *@param id           render id to abort unnecessary renderings
       *@param bs           dataset to render
       *@param count_by     Field used to count the entities by (for width of icon in xy grid)
       *@param color_by     Field used to color the entities
       *@param mode         Mode of application - boxplot or count/sum
       *@param bin_hdr      Entity to use for the x-axis
       *@param bin2_hdr     Sub-entity to use for the x-axis
       *@param x_scale      Scale to use for the x-axis
       *@param y_axis       Scalar field for the y-axis
       *@param y_scale      Scale to use for the y-axis
       *@param draw_labels  Flag to indicate to draw labels
       *@param w            Width (in pixels) of the rendering
       *@param h            Height (in pixels) of the rendering
       */
      public               RenderContext(short id, Bundles bs, String count_by, String color_by, String mode, String bin_hdr, String bin2_hdr, String x_scale, String y_axis, String y_scale, boolean draw_labels, int w, int h) {
        render_id = id; this.bs = bs; this.w = w; this.h = h; this.count_by = count_by; this.color_by = color_by; this.mode = mode; this.bin_hdr = bin_hdr; this.bin2_hdr = bin2_hdr; this.x_scale = x_scale; this.y_axis = y_axis; this.y_scale = y_scale; this.draw_labels = draw_labels;
        BundlesG globals = getRTParent().getRootBundles().getGlobals();

	// Initialize the counter context and the mapper
	counter_context = new BundlesCounterContext(bs, y_axis, color_by);
	xmap = new Mapper();

	// Figure out if this mode aggregates records on a per time-frame basis
	Map<String,Map<String,Set<String>>> time_framer_cat  = null;
	Map<String,Map<String,Long>>        time_framer_sum  = null;
        boolean                             y_is_categorical = false;
	boolean                             aggregate_mode   = false;
        KeyMaker                            time_framer_km   = null;

        // Determine the type (numerical/nominal versus categorical) of data
        if      (y_axis.equals(KeyMaker.RECORD_COUNT_STR))     y_is_categorical = false;
        else if (globals.isScalar(globals.fieldIndex(y_axis))) y_is_categorical = false;
        else                                                   y_is_categorical = true;

        // Special consideration for boxplots (because they have to have a specific flavor of data)
        if (mode.startsWith(MODE_BOXPLOT_STR) && !mode.equals(MODE_BOXPLOT_STR)) {
	  aggregate_mode = true;
          // Allocate the right counter
          if (y_is_categorical) time_framer_cat = new HashMap<String,Map<String,Set<String>>>();
	  else                  time_framer_sum = new HashMap<String,Map<String,Long>>();
	  // Create the time binning construct
	  String key_maker_type_str = null;
	  if        (mode.equals(MODE_BOXPLOT_PER_MINUTE_STR)) { key_maker_type_str = KeyMaker.BY_YEAR_MONTH_DAY_HOUR_MIN_STR;
	  } else if (mode.equals(MODE_BOXPLOT_PER_HOUR_STR))   { key_maker_type_str = KeyMaker.BY_YEAR_MONTH_DAY_HOUR_STR;
	  } else if (mode.equals(MODE_BOXPLOT_PER_DAY_STR))    { key_maker_type_str = KeyMaker.BY_YEAR_MONTH_DAY_STR;
	  } else if (mode.equals(MODE_BOXPLOT_PER_MONTH_STR))  { key_maker_type_str = KeyMaker.BY_YEAR_MONTH_STR;
	  } else throw new RuntimeException("Do Number Understand BoxPlot Aggregate Mode \"" + mode + "\"");
          time_framer_km = new KeyMaker(bs.tabletIterator().next(), key_maker_type_str);
	}

	// Go through the tablets
        Iterator<Tablet> it_t = bs.tabletIterator();
	while (it_t.hasNext() && currentRenderID() == getRenderID()) {
	  Tablet tablet = it_t.next(); KeyMaker ekm = null, e2km = null, ykm = null;

	  // Can the tablet contribute to "count_by"
	  boolean tablet_can_count = count_by.equals(KeyMaker.RECORD_COUNT_STR) || KeyMaker.tabletCompletesBlank(tablet, count_by);

	  // Check to see what the bundle can provide
	  if      (                    KeyMaker.tabletCompletesBlank(tablet, bin_hdr))  ekm  = new KeyMaker(tablet, bin_hdr);
	  if      (bin2_hdr != null && KeyMaker.tabletCompletesBlank(tablet, bin2_hdr)) e2km = new KeyMaker(tablet, bin2_hdr);
	  boolean bundle_y_counting = false;
	  if      (y_axis.equals(KeyMaker.RECORD_COUNT_STR))                            { ykm  = null; bundle_y_counting = true; }
	  else if (                    KeyMaker.tabletCompletesBlank(tablet, y_axis))   { ykm  = new KeyMaker(tablet, y_axis); }

	  // If it provides something, go through the bundles
	  if (ekm != null && (bundle_y_counting || ykm != null) && (!aggregate_mode || tablet.hasTimeStamps())) {

            // Make the periodic time functions stretch the entire period
	    if (ekm.isTimeBased() && ekm.linearTime() == false) {
	      xmap.add("Min", ekm.minPeriodicValue());
              xmap.add("Max", ekm.maxPeriodicValue());
	    }

	    // Go through the bundles
	    Iterator<Bundle> it_b = tablet.bundleIterator(); if (ekm.isTimeBased()) time_km = ekm;
	    while (it_b.hasNext() && currentRenderID() == getRenderID()) {
	      Bundle bundle = it_b.next(); bundle_to_keys.put(bundle, new HashSet<String>());
              String strs[] = ekm.stringKeys(bundle); int strs_i[] = null; if (ekm.isTimeBased() == false) strs_i  = ekm.intKeys(bundle);

	      // Create the y-axis values
	      int ys[] = null; String ystrs[] = null; 
              if (bundle_y_counting) { 
                ys = new int[1]; ys[0] = 1; 
              } else                 { 
                if (y_is_categorical) { } else { ys = ykm.intKeys(bundle);  }
                ystrs = ykm.stringKeys(bundle); 
              }

	      // Determine if a secondary axis is in effect
              String strs2[]; int strs2_i[];
	      if (e2km != null) { 
	        strs2   = e2km.stringKeys(bundle); 
		strs2_i = e2km.intKeys(bundle);
              } else { 
	        strs2   = new String[1]; strs2[0]   = ""; 
		strs2_i = new int[1];    strs2_i[0] = 0;
	      }

	      for (int i=0;i<strs.length;i++) {
	        for (int j=0;j<strs2.length;j++) {
		  String key;
		  if (strs2[j].equals("") == false) key = strs[i] + ":" + strs2[j]; else key = strs[i];
		  // Store of the bundle to keys information
		  bundle_to_keys.get(bundle).add(key);

		  // Figure out the ordering for the x-map
		  long   key_l;
		  if (ekm.isTimeBased()) key_l = ekm.timeStampKey(bundle);
		  // else                   key_l = ((strs_i[i] & 0x00ffffffffL)<<32L) | (strs2_i[j] & 0x00ffffffffL);
		  else                   key_l = (strs_i[i] & 0x00ffffffffL) | ((strs2_i[j] & 0x00ffffffffL)<<32L);
		  xmap.add(key, key_l);

		  // Even though the counter context isn't used for boxplots, we need it
		  // to correlate shapes to bundles...
                  counter_context.count(bundle, key);
		  if        (mode.equals(MODE_BOXPLOT_STR)) {
		    // Update statistics
		    if (stats_map.containsKey(key)   == false) stats_map.put(key, new Stat(key));
		    for (int k=0;k<ys.length;k++) stats_map.get(key).add(ys[k]);
                  } else if (mode.equals(MODE_ENTROPY_STR)) {
		    if (entropy_map.containsKey(key) == false) entropy_map.put(key, new Entropy(key));
		    for (int k=0;k<ys.length;k++) entropy_map.get(key).add(ys[k]);
                  } else if (mode.equals(MODE_SUM_STR))     {
		  } else if (mode.startsWith(MODE_BOXPLOT_STR)) {
                    String time_bin = time_framer_km.toString(bundle.ts0());
		    if (y_is_categorical) {
		      if (time_framer_cat.containsKey(key) == false)               time_framer_cat.put(key, new HashMap<String,Set<String>>());
		      if (time_framer_cat.get(key).containsKey(time_bin) == false) time_framer_cat.get(key).put(time_bin, new HashSet<String>());
		      for (int k=0;k<ystrs.length;k++) time_framer_cat.get(key).get(time_bin).add(ystrs[k]);
                    } else                {
		      if (time_framer_sum.containsKey(key) == false)               time_framer_sum.put(key, new HashMap<String,Long>());
		      if (time_framer_sum.get(key).containsKey(time_bin) == false) time_framer_sum.get(key).put(time_bin, 0L);
		      for (int k=0;k<ys.length;k++) time_framer_sum.get(key).put(time_bin, time_framer_sum.get(key).get(time_bin) + ys[k]);
                    }
		  }

                  // Track bundles
		  if (bin_map.containsKey(key) == false) bin_map.put(key, new HashSet<Bundle>());
		  bin_map.get(key).add(bundle);
		}
	      }
	    }
	  } else {
	    Iterator<Bundle> it_b = tablet.bundleIterator();
	    while (it_b.hasNext()) addToNoMappingSet(it_b.next());
	  }
 	}

	// Add the aggregates
	if (aggregate_mode) {
	  if (y_is_categorical) {
	    // Calculate the statistics
	    Iterator<String> it = time_framer_cat.keySet().iterator();
	    while (it.hasNext()) {
	      String key = it.next();
	      if (stats_map.containsKey(key) == false) stats_map.put(key, new Stat(key));
	      // For the existing values, add them to the statistics
	      Iterator<String> it2 = time_framer_cat.get(key).keySet().iterator();
	      while (it2.hasNext()) stats_map.get(key).add(time_framer_cat.get(key).get(it2.next()).size());
	      // Need to make sure the zero time frames are accounted for
              int needed_slots = calculateSlots(mode, key, bin_hdr);
	      if (needed_slots > 0) {
                if (stats_map.get(key).samples() > needed_slots) System.err.println("Stat Samples Greater Than Expected Slots");
	        while (stats_map.get(key).samples() < needed_slots) stats_map.get(key).add(0L);
	      } else error_str = "Possible Settings Error";
	    }
	  } else                {
	    // Calculate the statistics
	    Iterator<String> it = time_framer_sum.keySet().iterator();
	    while (it.hasNext()) {
	      String key = it.next();
	      if (stats_map.containsKey(key) == false) stats_map.put(key, new Stat(key));
	      // For the existing values, add them to the statistics
	      Iterator<String> it2 = time_framer_sum.get(key).keySet().iterator();
	      while (it2.hasNext()) stats_map.get(key).add(time_framer_sum.get(key).get(it2.next()));
	      // Need to make sure the zero time frames are accounted for
              int needed_slots = calculateSlots(mode, key, bin_hdr);
	      if (needed_slots > 0) {
                if (stats_map.get(key).samples() > needed_slots) System.err.println("Stat Samples Greater Than Expected Slots");
	        while (stats_map.get(key).samples() < needed_slots) stats_map.get(key).add(0L);
	      } else error_str = "Possible Settings Error";
	    }
	  }
	}

	// Calculate the statistics for each entity
	ys_min = Double.POSITIVE_INFINITY;
	ys_max = Double.NEGATIVE_INFINITY;
	if (mode.startsWith(MODE_BOXPLOT_STR)) {
	  Iterator<String> it = stats_map.keySet().iterator();
	  while (it.hasNext()) {
	    String key  = it.next();
	    Stat   stat = stats_map.get(key);
	    stat.calc();
	    if (ys_min > stat.min) ys_min = stat.min;
	    if (ys_max < stat.max) ys_max = stat.max;
          }
        } else if (mode.equals(MODE_ENTROPY_STR)) {
          Iterator<String> it = entropy_map.keySet().iterator();
	  while (it.hasNext()) {
	    String key     = it.next();
	    double entropy = entropy_map.get(key).calc();
	    if (ys_min > entropy) ys_min = entropy;
	    if (ys_max < entropy) ys_max = entropy;
	  }
        } else if (mode.equals(MODE_SUM_STR)) {
          ys_min = 0.0;
	  ys_max = counter_context.totalMaximum();
	}
      }

      /**
       * Calculate the number of slots that should be filled for the statistics.  This is
       * important because without this functionality, timeframes that have zero contributions
       * will not add to the statistics causing the statistics to be incorrect.
       *
       *@param boxplot_mode the mode of the boxplot (what to count over what timeframe)
       *@param timeframe    the timeframe in question (needed because months and higher have variable amounts of time increments)
       *@param grouping     the actual grouping on the screen
       *
       *@return number of expected statistics for that grouping over the mode
       */
      public int calculateSlots(String boxplot_mode, String timeframe, String group) {
	if        (group.equals(KeyMaker.BY_YEAR_MONTH_DAY_HOUR_STR))         {
	  if (boxplot_mode.equals(MODE_BOXPLOT_PER_MINUTE_STR))      { return 60;    }
	} else if (group.equals(KeyMaker.BY_YEAR_MONTH_DAY_STR))              {
	  if      (boxplot_mode.equals(MODE_BOXPLOT_PER_MINUTE_STR)) { return 60 * 24; }
	  else if (boxplot_mode.equals(MODE_BOXPLOT_PER_HOUR_STR))   { return      24; }
	} else if (group.equals(KeyMaker.BY_YEAR_MONTH_STR))                  {
	  int days_in_month = Utils.daysInMonth(timeframe);
	  if      (boxplot_mode.equals(MODE_BOXPLOT_PER_DAY_STR))    { return days_in_month; }
	  else if (boxplot_mode.equals(MODE_BOXPLOT_PER_HOUR_STR))   { return days_in_month * 24; }
	  else if (boxplot_mode.equals(MODE_BOXPLOT_PER_MINUTE_STR)) { return days_in_month * 24 * 60; }
	} else if (group.equals(KeyMaker.BY_YEAR_STR))                        {
	  int days_in_year = Utils.daysInYear(timeframe);
          if      (boxplot_mode.equals(MODE_BOXPLOT_PER_MONTH_STR))  { return 12; }
	  else if (boxplot_mode.equals(MODE_BOXPLOT_PER_DAY_STR))    { return days_in_year; }
	  else if (boxplot_mode.equals(MODE_BOXPLOT_PER_HOUR_STR))   { return days_in_year * 24; }
	  else if (boxplot_mode.equals(MODE_BOXPLOT_PER_MINUTE_STR)) { return days_in_year * 24 * 60; }
	} else if (group.startsWith("|Tm|")) {
          // Probably an error condition...  not satisfiable by this implementation...
          // Probably need to consider adding periodic components
	  return -2;
        } else if (boxplot_mode.equals(MODE_BOXPLOT_PER_MINUTE_STR)) {
	  return (int) ((bs.ts1() - bs.ts0())/(60L*1000L));
        } else if (boxplot_mode.equals(MODE_BOXPLOT_PER_HOUR_STR))   {
	  return (int) ((bs.ts1() - bs.ts0())/(60L*60L*1000L));
	} else if (boxplot_mode.equals(MODE_BOXPLOT_PER_DAY_STR))    {
	  return (int) ((bs.ts1() - bs.ts0())/(24*60L*60L*1000L));
	} else if (boxplot_mode.equals(MODE_BOXPLOT_PER_MONTH_STR))  {
	  return (int) ((bs.ts1() - bs.ts0())/(30L*60L*60L*1000L)); // Roughly...
	}
	return -1;
      }

      /**
       * Minimum y screen coordinate
       */
      double ys_min,
      /**
       * Maximum y screen coordinate
       */
             ys_max;
      /**
       * Map for entities to the statistical calculation
       */
      Map<String,Stat>        stats_map = new HashMap<String,Stat>();
      /**
       * Map for entities to entropy calculation
       */
      Map<String,Entropy>     entropy_map = new HashMap<String,Entropy>();
      /**
       * Map for from the bin to the bundle set
       */
      Map<String,Set<Bundle>> bin_map = new HashMap<String,Set<Bundle>>();
      /**
       * Class for tracking the statistics on a per-entity basis
       */
      class Stat {
        String entity; double min, max, avg, med, stdev, sum, x02, x09, x91, x98; List<Long> values = new ArrayList<Long>();
        public Stat(String entity) { this.entity = entity; sum = 0.0; }
        // public void add(int i) { values.add(i); sum += i; }
	public void add(long l) { values.add(l); sum += l; }
        public int  samples() { return values.size(); }
        public void calc() {
	  Collections.sort(values);
	  min = values.get(0);
	  max = values.get(values.size()-1);
	  med = values.get(values.size()/2);
          x02 = values.get((values.size()* 2)/100);
          x09 = values.get((values.size()* 9)/100);
          x91 = values.get((values.size()*91)/100);
          x98 = values.get((values.size()*98)/100);
	  avg = sum / values.size(); double sqr = 0.0;
	  for (int i=0;i<values.size();i++) sqr += (values.get(i) - avg) * (values.get(i) - avg);
	  stdev = Math.sqrt(sqr/values.size());
	}
        public String toString() {
          return "For \"" + entity + "\" -- Stat:  " + min + "," + avg + "," + med + ",+/- " + stdev + "," + max;
        }
      }
      /**
       * Class for calculating the entropy on a per-entity basis
       */
      class Entropy {
        String entity; Map<Long,Integer> freq = new HashMap<Long,Integer>(); int size = 0; double entropy = -1.0;
	public Entropy(String entity) { this.entity = entity; }
	public void add(long l) { size++; if (freq.containsKey(l) == false) freq.put(l,0); freq.put(l, freq.get(l) + 1); }
	public double calc() {
	  if (entropy >= 0.0) return entropy;
          Iterator<Long> it = freq.keySet().iterator(); double sum = 0.0;
	  while (it.hasNext()) {
	    double frequency = freq.get(it.next()); double prob = frequency / size;
	    sum += prob * Math.log(prob)/Math.log(2);
	  }
	  entropy = -sum;
	  return entropy;
	}
      }
      /**
       * Class to map the world coordinates for entities to screen coordinates.
       */
      class Mapper {
	/**
	 *
	 */
        long xw_min, 
	/**
	 *
	 */
	     xw_max;
	/**
	 *
	 */
        Map<String,Long>   map   = new HashMap<String,Long>();
	/**
	 *
	 */
	Map<Long,String>   rmap  = new HashMap<Long,String>();
	/**
	 * World x coordinates for current rendering
	 */
	List<Long>         xws   = new ArrayList<Long>();
	/**
	 * Inset of the graph
	 */
        int                ins,
	/**
	 * Length of the graph
	 */
	                   len;
	/**
	 *
	 */
        public void add(String entity, long world_x) {
	  // Keep track of all of the entities
	  if (map.containsKey(entity) == false) {
	    // Convert to an unsigned double...  I think this works
	    if (map.size() == 0) xw_min = xw_max = world_x;

	    map.put(entity,world_x); rmap.put(world_x,entity); xws.add(world_x);

	    if (xw_min > world_x) xw_min = world_x;
	    if (xw_max < world_x) xw_max = world_x;
          }
	}
	/**
	 * Calculate the specific mapping for a value.  Only used by the linear-time-based
	 * labeling system.
	 */
	public int  calculateMapping(long l) {
	  if (xws.size() == 1 || xws.size() == 2) return ins; // No easy calculation...
	  return (int) (1 + ins + (len * (l - xw_min))/(xw_max - xw_min));
	}
	/**
	 *
	 */
	public void calculateMapping(String x_scale, int ins, int len) { 
	  Collections.sort(xws); this.ins = ins; this.len = len;
	  if        (xws.size() == 1) {
	    conversion.put(xws.get(0), ins + len/2);
	  } else if (xws.size() == 2) {
	    conversion.put(xws.get(0), ins + (1*len)/3);
	    conversion.put(xws.get(1), ins + (2*len)/3);
	  } else if (x_scale.equals(LINEAR_SCALE_STR)) {
            for (int i=0;i<xws.size();i++) {
	      conversion.put(xws.get(i), (int) (1 + ins + (len * (xws.get(i) - xw_min))/(xw_max - xw_min)));
	    }
	  } else if (x_scale.equals(EQUAL_SCALE_STR))  {
            int divisor = xws.size()-1; if (divisor < 1) divisor = 1;
	    for (int i=0;i<xws.size();i++) {
	      conversion.put(xws.get(i), (int) (1 + ins + (i*len)/(divisor))); // The one makes space for the axis...
	    }
	  } else throw new RuntimeException("Scale \"" + x_scale + "\" Not Understood");
	}
	/**
	 * Return the number of mapped entities.
	 *
	 *@return number of entities
	 */
        public int size() { return xws.size(); }

	/**
	 * Conversion from world coordinates to screen coordinates
	 */
        Map<Long,Integer> conversion = new HashMap<Long,Integer>();

	/**
	 * Calculate the x-position of an entity in screen space.
	 *
	 *@param  entity entity to position
	 *
	 *@return screen x-coordinate
	 */
	public int  toScreen(String entity) { 
	  return conversion.get(map.get(entity));
	}

        /**
	 * Return the earliest entity on the x-axis.  Useful for first labels...
	 *
	 *@return first entity
	 */
        public String firstEntity() { return rmap.get(xws.get(0)); }

	/**
	 * Return the last entity on the x-axis.  Useful for last labels...
	 *
	 *@return last entity
	 */
        public String lastEntity() { return rmap.get(xws.get(xws.size()-1)); }
      }

      /**
       * Return the width of the rendering in pixels.
       *
       *@return width in pixels
       */
      public int           getRCWidth()  { return w; }

      /**
       * Return the height of the rendering in pixels
       *
       *@return height in pixels
       */
      public int           getRCHeight() { return h; }

      /**
       * Colors for labels in time
       */
      private final Color            COLOR_YEAR   = RTColorManager.getColor("label", "year"),
                                     COLOR_MONTH  = RTColorManager.getColor("label", "month"),
			             COLOR_DAY    = RTColorManager.getColor("label", "day"),
			             COLOR_HOUR   = RTColorManager.getColor("label", "hour"),
			             COLOR_MINUTE = RTColorManager.getColor("label", "minute");

      /** 
       * Draw markers for time-based graphs.
       *
       *@param g2d graphics primitive
       */
      private void drawTimeGrid(Graphics2D g2d) {
        SimpleDateFormat major_sdf = null, minor_sdf = null; Color major_color = null, minor_color = null;
        long time_diff = bs.ts1() - bs.ts0();
        if        (time_diff > 3 *Utils.YEARS)  { major_sdf = new SimpleDateFormat("yyyy"); major_color = COLOR_YEAR;
	                                          minor_sdf = new SimpleDateFormat("MMM");  minor_color = COLOR_MONTH;
	} else if (time_diff > 1 *Utils.YEARS)  { major_sdf = new SimpleDateFormat("yyyy"); major_color = COLOR_YEAR;
	                                          minor_sdf = new SimpleDateFormat("MMM");  minor_color = COLOR_MONTH;
	} else if (time_diff > 6 *Utils.MONTHS) { major_sdf = new SimpleDateFormat("yyyy"); major_color = COLOR_YEAR;
	                                          minor_sdf = new SimpleDateFormat("MMM");  minor_color = COLOR_MONTH;
	} else if (time_diff > 1 *Utils.MONTHS) { major_sdf = new SimpleDateFormat("MMM");  major_color = COLOR_MONTH;
	                                          minor_sdf = new SimpleDateFormat("dd");   minor_color = COLOR_DAY;
	} else if (time_diff > 12*Utils.DAYS)   { major_sdf = new SimpleDateFormat("MMM");  major_color = COLOR_MONTH;
	                                          minor_sdf = new SimpleDateFormat("dd");   minor_color = COLOR_DAY;
	} else if (time_diff > 3 *Utils.DAYS)   { major_sdf = new SimpleDateFormat("dd");   major_color = COLOR_DAY;
	                                          minor_sdf = new SimpleDateFormat("HH");   minor_color = COLOR_HOUR;
	} else if (time_diff > 1 *Utils.DAYS)   { major_sdf = new SimpleDateFormat("dd");   major_color = COLOR_DAY;
	                                          minor_sdf = new SimpleDateFormat("HH");   minor_color = COLOR_HOUR;
	} else                                  { major_sdf = new SimpleDateFormat("HH");   major_color = COLOR_HOUR;
	                                          minor_sdf = new SimpleDateFormat("MM");   minor_color = COLOR_MINUTE;
        }
	major_sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	minor_sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

	String last_major = major_sdf.format(new Date(bs.ts0())),
               last_minor = minor_sdf.format(new Date(bs.ts0()));
        long inc = (bs.ts1() - bs.ts0())/graph_w;
        Area taken = new Area(); Rectangle2D rect; int txt_w = 0, txt_h = 0, spc = 2;
        for (long ts=bs.ts0()+1;ts<=bs.ts1();ts+=inc) {
	  String major = major_sdf.format(new Date(ts));
	  int    sx    = xmap.calculateMapping(ts);
	  if        (major.equals(last_major) == false) {
            txt_w = Utils.txtW(g2d, major); txt_h = Utils.txtH(g2d, major);
            rect = new Rectangle2D.Float(sx - txt_w/2 - spc, 0, txt_w + 2*spc, txt_h);
            if (taken.intersects(rect) == false) { 
              g2d.setColor(major_color);
	      g2d.drawString(major, sx - txt_w/2, txt_h); 
	      last_major = major; 
              taken.add(new Area(rect));
            }
	  } 
        }
        for (long ts=bs.ts0()+1;ts<=bs.ts1();ts+=inc) {
	  String minor = minor_sdf.format(new Date(ts));
	  int    sx    = xmap.calculateMapping(ts);
          if (minor.equals(last_minor) == false) {
            txt_w = Utils.txtW(g2d, minor); txt_h = Utils.txtH(g2d, minor);
            rect = new Rectangle2D.Float(sx - txt_w/2 - spc, txt_h+1, txt_w + 2*spc, txt_h);
            if (taken.intersects(rect) == false) { 
              g2d.setColor(minor_color);
	      g2d.drawString(minor, sx - txt_w/2, (int) (txt_h*2)); 
	      last_minor = minor; 
              taken.add(new Area(rect));
            }
	  }
	}
      }

      /**
       * Copy of the rendered image
       */
      BufferedImage base_bi = null;

      /**
       * Render the previously calculated values to the actual image buffer.  Save a copy in case
       * it is requested again.
       *
       *@return rendered image
       */
      public BufferedImage getBase()     { 
        if (base_bi == null) {
	 Graphics2D g2d = null;
	 try {
          base_bi = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) base_bi.getGraphics();
          g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	  RTColorManager.renderVisualizationBackground(base_bi, g2d);
	  int txt_h = Utils.txtH(g2d, "0"), LOG_1_H = 10; String max_log_str = "notset";

	  // Only use linear mode for boxplots...
	  if (mode.startsWith(MODE_BOXPLOT_STR) && y_scale.equals(LOG_SCALE_STR)) y_scale = LINEAR_SCALE_STR;

	  // Figure out the geometry
	  x_ins = y_ins = 16;
          if (time_km != null && time_km.linearTime() && x_scale.equals(LINEAR_SCALE_STR)) { y_ins = 2*Utils.txtH(g2d,"1") + 5; } // More space for the labels...
	  graph_w = w - 2*x_ins;
	  graph_h = h - 2*y_ins;
          int w2 = 4; // Half the width of a bar
          w2 = (graph_w/xmap.size())/2; 
	  if      (w2 < 1)                                     w2 = 1; 
	  else if (x_scale.equals(LINEAR_SCALE_STR) && w2 > 4) w2 = 4;
	  else if (w2 > 8)                                     w2 = 8;

	  // Figure out the scale and mappings
	  xmap.calculateMapping(x_scale, x_ins+w2, graph_w-2*w2);

	  // Draw labels, axes, etc...
	  Iterator<String> it;
	  int max_label_w = 0;
	  if (draw_labels) {
	    if (mode.startsWith(MODE_BOXPLOT_STR)) it = stats_map.keySet().iterator(); else it = counter_context.binIterator();
	    while (it.hasNext()) {
	      String str   = it.next();
	      int    str_w = Utils.txtW(g2d, str);
	      if (str_w > max_label_w) max_label_w = str_w;
	    }
	    graph_h = h - (y_ins + max_label_w + 2);
	  }
	  
	  // - Axes
	  g2d.setColor(RTColorManager.getColor("axis", "major"));
	  g2d.drawLine(x_ins, y_ins + graph_h, x_ins + graph_w, y_ins + graph_h);
	  g2d.drawLine(x_ins, y_ins,           x_ins,           y_ins + graph_h);
	  Utils.drawRotatedString(g2d, "" + ys_min,  x_ins, y_ins + graph_h);
	  Utils.drawRotatedString(g2d, "" + ys_max,  x_ins, y_ins + Utils.txtW(g2d, "" + ys_max));

	  // For the log scale, draw lines for context...
	  if (y_scale.equals(LOG_SCALE_STR)) {
	    g2d.setColor(RTColorManager.getColor("axis", "minor"));
	    long log = 1;
	    while (log < ys_max) {
	      int sy = (int) (y_ins + graph_h - LOG_1_H - ((graph_h - LOG_1_H)*Math.log(log))/Math.log(counter_context.totalMaximum()));
	      g2d.drawLine(x_ins+1,sy,x_ins+graph_w-1,sy);
	      log *= 10;
	    }
          }

	  // Draw the y-axis string, color it different for log scale
	  String y_str = y_axis;
	  if (y_scale.equals(LOG_SCALE_STR)) { g2d.setColor(RTColorManager.getColor("label", "log")); y_str += " (Log)";
	  } else                             { g2d.setColor(RTColorManager.getColor("label", "linear")); }
	  Utils.drawRotatedString(g2d, y_str,  x_ins, y_ins + graph_h/2 + Utils.txtW(g2d, y_str)/2);

	  // If it's in linear time mode, draw context for the timeframes
          if (time_km != null && time_km.linearTime() && x_scale.equals(LINEAR_SCALE_STR)) { drawTimeGrid(g2d); }

	  // Draw the boxplots
	  Area label_area = new Area();
          if (mode.startsWith(MODE_BOXPLOT_STR)) {
	    it = stats_map.keySet().iterator();
	    while (it.hasNext()) {
	      String key     = it.next();
	      Stat   stat    = stats_map.get(key);
	      int    sx      = xmap.toScreen(key);
	      int    sy_min  = (int) (y_ins + graph_h - ((graph_h * (stat.min   - ys_min))/(ys_max - ys_min))),
	             sy_max  = (int) (y_ins + graph_h - ((graph_h * (stat.max   - ys_min))/(ys_max - ys_min))),
	             sy_avg  = (int) (y_ins + graph_h - ((graph_h * (stat.avg   - ys_min))/(ys_max - ys_min))),
	             sy_med  = (int) (y_ins + graph_h - ((graph_h * (stat.med   - ys_min))/(ys_max - ys_min))),
                     // Uses the sorted list indices for 9% and 91% 
	             sy_stdp  = (int) (y_ins + graph_h - ((graph_h * (stat.x91 - ys_min))/(ys_max - ys_min))),
	             sy_stdm  = (int) (y_ins + graph_h - ((graph_h * (stat.x09 - ys_min))/(ys_max - ys_min))),
	             sy_stdp2 = (int) (y_ins + graph_h - ((graph_h * (stat.x98 - ys_min))/(ys_max - ys_min))),
	             sy_stdm2 = (int) (y_ins + graph_h - ((graph_h * (stat.x02 - ys_min))/(ys_max - ys_min)));

              // Update the geometry to bin mapping
              Rectangle2D rect = new Rectangle2D.Double(sx - w2, sy_max, 2*w2+1, sy_min - sy_max); // Remember, y is upside down in screen coordinates
	      geom_to_key.put(rect, key);
	      key_to_geom.put(key, rect);

	      // Whiskers Lines
              g2d.setColor(RTColorManager.getColor("data", "deviation"));
	      g2d.drawLine(sx,      sy_min,   sx,      sy_stdm);
	      g2d.drawLine(sx,      sy_max,   sx,      sy_stdp);

              g2d.drawLine(sx-2,    sy_stdp2, sx+2,    sy_stdp2); // 2% and 98%
              g2d.drawLine(sx-2,    sy_stdm2, sx+2,    sy_stdm2);

	      // Box
	      // g2d.drawRect(sx-w2,   sy_stdp,  2*w2+1,  sy_stdm - sy_stdp);
	      // Mins and Maxes
              g2d.setColor(RTColorManager.getColor("data", "min"));
              g2d.fillRect(sx-1, sy_min-1, 3, 3);
	      g2d.setColor(RTColorManager.getColor("data", "max"));
              g2d.fillRect(sx-1, sy_max-1, 3, 3);
	      // Median
              g2d.setColor(RTColorManager.getColor("data", "median"));
              g2d.fillRect(sx-1, sy_med-1, 3, 3);
	      // Labels...
	      if (draw_labels) { 
	        Rectangle2D label_rect = new Rectangle2D.Double(sx - txt_h/2 - 1, y_ins + graph_h, txt_h + 2, h - y_ins - graph_h);
	        if (label_area.intersects(label_rect) == false) {
	          g2d.setColor(RTColorManager.getColor("label", "major"));
	          Utils.drawRotatedString(g2d, key, sx + txt_h/2, h - max_label_w + Utils.txtW(g2d, key) + 3); 
		  label_area.add(new Area(label_rect));
                }
	      }
	    }
          } else if (mode.equals(MODE_ENTROPY_STR)) {
	    // Plot the values
	    it = entropy_map.keySet().iterator();
	    while (it.hasNext()) {
	      String key     = it.next();
	      double entropy = entropy_map.get(key).calc();
	      int    sx      = xmap.toScreen(key);
	      int    sy      = (int) (y_ins + graph_h - ((graph_h * (entropy - ys_min))/(ys_max - ys_min)));
	      g2d.setColor(RTColorManager.getColor("data", "median"));
	      g2d.fillRect(sx-1,sy-1,3,3);
              // Update the geometry to bin mapping
              Rectangle2D rect = new Rectangle2D.Double(sx-1,sy-1,3,3);
	      geom_to_key.put(rect, key);
	      key_to_geom.put(key, rect);
	      // Labels...
	      if (draw_labels) { 
	        Rectangle2D label_rect = new Rectangle2D.Double(sx - txt_h/2 - 1, y_ins + graph_h, txt_h + 2, h - y_ins - graph_h);
	        if (label_area.intersects(label_rect) == false) {
	          g2d.setColor(RTColorManager.getColor("label", "major"));
	          Utils.drawRotatedString(g2d, key, sx + txt_h/2, h - max_label_w + Utils.txtW(g2d, key) + 3); 
		  label_area.add(new Area(label_rect));
                }
	      }
	    }
          } else if (mode.equals(MODE_SUM_STR)) {
	    it = counter_context.binIterator();
	    while (it.hasNext()) {
	      String key   = it.next();
	      int    sx    = xmap.toScreen(key), sy;

	      // Calculate the bar height
	      int    bar_h;
	      if (y_scale.equals(LINEAR_SCALE_STR)) {
	        bar_h = (int) (counter_context.totalNormalized(key)*graph_h);
	      } else {
	        if        (counter_context.total(key) > 1.0) {
	          bar_h = LOG_1_H + (int) (((graph_h - LOG_1_H)*Math.log(counter_context.total(key)))/Math.log(counter_context.totalMaximum()));
                } else if (counter_context.total(key) > 0.0) {
		  bar_h = LOG_1_H;
                } else {
		  bar_h = 0;
                }
	      }
	      sy = y_ins + graph_h - bar_h;

              // Update the geometry to bin mapping
              Rectangle2D rect = new Rectangle2D.Double(sx - w2, sy, 2*w2+1, bar_h);
	      geom_to_key.put(rect, key);
	      key_to_geom.put(key, rect);

	      // Determine if the bar should be in color or not
              if (color_by != null) {
                int y_inc = y_ins + graph_h;
                List<String> cbins = counter_context.getColorBinsSortedByCount();
                for (int i=cbins.size()-1;i>=0;i--) {
                  String cbin   = cbins.get(i);
                  double ctotal = counter_context.total(key,cbin);
                  if (ctotal > 0L) {
                    int sub_h = (int) ((ctotal * bar_h)/counter_context.binColorTotal(key));
                    if (sub_h > 0) {
                      g2d.setColor(RTColorManager.getColor(cbin));
                      g2d.fillRect(sx - w2, y_inc - sub_h, 2*w2+1, sub_h);
                      y_inc -= sub_h;
                    }
                  }  
                }
                if (y_inc != sy) {
                  g2d.setColor(RTColorManager.getColor("set", "multi"));
                  g2d.fillRect(sx - w2, sy, 2*w2+1, y_inc - sy);
                }
              } else {
	        g2d.setColor(counter_context.binColor(key));
	        g2d.fillRect(sx - w2, sy, 2*w2+1, bar_h);
              }

	      // Labels...
	      if (draw_labels) { 
	        Rectangle2D label_rect = new Rectangle2D.Double(sx - txt_h/2 - 1, y_ins + graph_h, txt_h + 2, h - y_ins - graph_h);
	        if (label_area.intersects(label_rect) == false) {
	          g2d.setColor(RTColorManager.getColor("label", "major"));
	          Utils.drawRotatedString(g2d, key, sx + txt_h/2, h - max_label_w + Utils.txtW(g2d, key) + 3); 
		  label_area.add(new Area(label_rect));
                }
	      }
	    }
	  }

	  // If not drawing labels, just draw the first and last strings
          if (!draw_labels) {
	    g2d.setColor(RTColorManager.getColor("label", "major"));
	    g2d.drawString(xmap.firstEntity(), x_ins,                                               h - 2);
	    g2d.drawString(xmap.lastEntity(),  x_ins + graph_w - Utils.txtW(g2d,xmap.lastEntity()), h - 2);
	    String str = bin_hdr; if (bin2_hdr != null) str += ":" + bin2_hdr;
	    if (x_scale.equals(EQUAL_SCALE_STR)) { g2d.setColor(RTColorManager.getColor("label", "equal")); str += " (Equal)"; } else g2d.setColor(RTColorManager.getColor("label", "linear"));
	    g2d.drawString(str, x_ins + graph_w/2 - Utils.txtW(g2d,str)/2, h - 2);
	  }

	  // Draw the error strings
          if (error_str.equals("") == false) {
	    clearStr(g2d, error_str, base_bi.getWidth() - Utils.txtW(g2d, error_str), Utils.txtH(g2d, error_str), RTColorManager.getColor("label", "errorfg"), RTColorManager.getColor("label", "errorbg"));
	  }
	 } finally { if (g2d != null) g2d.dispose(); }
	}
	return base_bi;
      }
    }
  }
}

