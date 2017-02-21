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
package racetrack.gui;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesG;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.BundlesDT;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;

import racetrack.util.QuickStats;
import racetrack.util.Utils;

import racetrack.visualization.RTColorManager;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;

import java.awt.geom.Ellipse2D;

import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Collections;
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

/**
 * Visualization for showing time series information.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class RTTimeSeriesPanel extends RTPanel {
  /**
   * 
   */
  private static final long serialVersionUID = -100123537899698028L;

  /**
   * Timeseries to use... x axis... to include periodic type
   */
  JComboBox            timeseries_cb, 

  /**
   * Scalar for the y axis
   */
                       y_cb,

  /**
   * Entity combobox
   */
                       entity_cb;

  /**
   * Options for the scaling on the y axis
   */
  JRadioButtonMenuItem y_scales[];

  /**
   * Checkbox to use the global color_by option
   */
  JCheckBoxMenuItem    color_cbmi,

  /**
   * Draw the actual dots for each data point
   */
                       draw_dots_cbmi,
  /**
   * For aggregate dots (e.g., periodic mapping), draw a boxplot
   */
                       draw_boxplots_cbmi,

  /**
   * Draw the entity lines
   */
                       draw_entities_cbmi,

  /**
   * Draw the global version
   */
                       draw_globals_cbmi;

  /**
   * Scale options for the y-axis
   */
  String y_scales_strs[] = { AxisMapper.LINEAR_SCALE_STR, AxisMapper.LOG_SCALE_STR };

  /**
   * Construct a new panel with the specified GUI parent.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt       application reference
   */
  public RTTimeSeriesPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      { 
    super(win_type, win_pos, win_uniq, rt);   
    // Main component
    add("Center", component = new RTTimeSeriesComponent());

    // Popup menu
    y_scales = new JRadioButtonMenuItem[y_scales_strs.length]; ButtonGroup bg = new ButtonGroup();
    for (int i=0;i<y_scales.length;i++) { getRTPopupMenu().add(y_scales[i] = new JRadioButtonMenuItem(y_scales_strs[i])); bg.add(y_scales[i]); }
    y_scales[0].setSelected(true);
    
    getRTPopupMenu().addSeparator();

    getRTPopupMenu().add(draw_entities_cbmi = new JCheckBoxMenuItem("Render Entities", true));
    getRTPopupMenu().add(draw_dots_cbmi     = new JCheckBoxMenuItem("Render Dots",     true));
    getRTPopupMenu().add(draw_boxplots_cbmi = new JCheckBoxMenuItem("Render BoxPlots", false));
    getRTPopupMenu().add(draw_globals_cbmi  = new JCheckBoxMenuItem("Render Global",   true));

    getRTPopupMenu().addSeparator();

    getRTPopupMenu().add(color_cbmi         = new JCheckBoxMenuItem("Vary Color", false));

    // Make the southern panel
    JPanel panel = new JPanel(new FlowLayout());
    panel.add(new JLabel("Entity"));     panel.add(entity_cb     = new JComboBox());
    panel.add(new JLabel("Timeseries")); panel.add(timeseries_cb = new JComboBox());
    panel.add(new JLabel("Y Axis"));     panel.add(y_cb          = new JComboBox());
    add("South", panel);

    // Default listeners
    defaultListener(entity_cb); defaultListener(timeseries_cb); defaultListener(y_cb); defaultListener(color_cbmi); 
    defaultListener(draw_entities_cbmi); defaultListener(draw_dots_cbmi); defaultListener(draw_boxplots_cbmi); defaultListener(draw_globals_cbmi);
    for (int i=0;i<y_scales.length;i++)     defaultListener(y_scales[i]);

    // Fill the comboboxes
    updateBys();
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "xytimeseries"; }

  /**
   * Return the transformation for the timeseries mapping.
   *
   *@return setting for the mapping
   */
  public String       timeSeries() { return (String) timeseries_cb.getSelectedItem(); }

  /**
   * Set the transformation for the timeseries mapping.
   *
   *@param str new setting for the mapping
   */
  public void         timeSeries(String str) { timeseries_cb.setSelectedItem(str); }

  /**
   * Get the entity field to use for the lines.
   *
   *@return entity field
   */
  public String entity() { return (String) entity_cb.getSelectedItem(); }

  /**
   * Set the entity field to use for the lines
   *
   *@param str entity field to use
   */
  public void entity(String str) { entity_cb.setSelectedItem(str); }

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
   * Return the scale to use on the y-axis
   *
   *@return y-axis scale
   */
  public String yScale        ()           { 
    for (int i=0;i<y_scales.length;i++) {
      if (y_scales[i].isSelected()) return y_scales_strs[i];
    }
    return y_scales_strs[0];
  }

  /**
   * Set the scale to use on the y-axis.
   *
   *@param str y-axis scale
   */
  public void         yScale        (String str) { 
    for (int i=0;i<y_scales_strs.length;i++) if (str.equals(y_scales_strs[i])) y_scales[i].setSelected(true);
  }

  /**
   * Determine if the color should be varied in the visualization.
   *
   *@return true if color should vary, false if color is to be constant
   */
  public boolean      varyColor     ()           { return color_cbmi.isSelected(); }

  /**
   * Set the flag for varying the color in the rendering.
   *
   *@param f flag to vary color
   */
  public void         varyColor     (boolean f)  { color_cbmi.setSelected(f); }

  /**
   * Return if the rendering should show the entity lines.
   *
   *@return true to render entity lines
   */
  public boolean      drawEntities  () { return draw_entities_cbmi.isSelected(); }

  /**
   * Set the option to render entity lines.
   *
   *@param f new setting for option
   */
  public void         drawEntities  (boolean f) { draw_entities_cbmi.setSelected(f); }

  /**
   * Return if the rendering should show the actual data points.
   *
   *@return true to render data points
   */
  public boolean      drawDots      () { return draw_dots_cbmi.isSelected(); }

  /**
   * Set the option to render actual data points.
   *
   *@param f new setting for option
   */
  public void         drawDots      (boolean f) { draw_dots_cbmi.setSelected(f); }

  /**
   * Return if the rendering should show the global average.
   *
   *@return true to render global average
   */
  public boolean      drawGlobals      () { return draw_globals_cbmi.isSelected(); }

  /**
   * Set the option to render the global average.
   *
   *@param f new setting for option
   */
  public void         drawGlobals   (boolean f) { draw_globals_cbmi.setSelected(f); }

  /**
   * Return if the rendering should show a boxplots of the points.
   *
   *@return true to render boxplots
   */
  public boolean      drawBoxPlots  () { return draw_boxplots_cbmi.isSelected(); }

  /**
   * Set the option to render boxplots.
   *
   *@param f new setting for option
   */
  public void         drawBoxPlots  (boolean f) { draw_boxplots_cbmi.setSelected(f); }

  /**
   * Get the configuration of this component as a string.  Supposed to be used for
   * bookmarking a view so that it can be re-rendered.
   *
   *@return string representing configuration of the panel
   */
  public String  getConfig()  { return "RTTimeSeriesPanel" + BundlesDT.DELIM +
                                       "entity="      + Utils.encToURL(entity())              + BundlesDT.DELIM +
                                       "timeseries="  + Utils.encToURL(timeSeries())          + BundlesDT.DELIM +
                                       "yaxis="       + Utils.encToURL(yAxis())               + BundlesDT.DELIM +
                                       "yscale="      + Utils.encToURL(yScale())              + BundlesDT.DELIM +
				       "entities="    + (drawEntities() ? "true" : "false")   + BundlesDT.DELIM +
				       "dots="        + (drawDots()     ? "true" : "false")   + BundlesDT.DELIM +
				       "global="      + (drawGlobals()  ? "true" : "false")   + BundlesDT.DELIM +
				       "boxplots="    + (drawBoxPlots() ? "true" : "false")   + BundlesDT.DELIM +
                                       "vcolor="      + (varyColor()    ? "true" : "false"); }

  /**
   * Set the configuration of this component from the string representation.
   *
   *@return previously returned string from the getConfig() method
   */
  public void    setConfig(String str) {
    StringTokenizer st = new StringTokenizer(str,BundlesDT.DELIM);
    if (st.nextToken().equals("RTTimeSeriesPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not An RTTimeSeriesPanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st.hasMoreTokens() ? st2.nextToken() : "";
      if      (type.equals("entity"))      entity(Utils.decFmURL(value));
      else if (type.equals("timeseries"))  timeSeries(Utils.decFmURL(value));
      else if (type.equals("yaxis"))       yAxis(Utils.decFmURL(value));
      else if (type.equals("yscale"))      yScale(Utils.decFmURL(value));
      else if (type.equals("entities"))    drawEntities(value.toLowerCase().equals("true"));
      else if (type.equals("dots"))        drawDots(value.toLowerCase().equals("true"));
      else if (type.equals("global"))      drawGlobals(value.toLowerCase().equals("true"));
      else if (type.equals("boxplots"))    drawBoxPlots(value.toLowerCase().equals("true"));
      else if (type.equals("vcolor"))      varyColor(value.toLowerCase().equals("true"));
      else throw new RuntimeException("Do Not Understand Type Value \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * Update the comboboxes for selecting global fields when new data is
   * loaded.
   */
  public void         updateBys() { updateEntityBys(entity_cb); updateScalarBys(y_cb); updateTimeSeriesBys(timeseries_cb); }

  /**
   * Generic method to update a combobox with just entity fields.
   */
  public void updateEntityBys(JComboBox cb) {
    Object sel = cb.getSelectedItem(); cb.removeAllItems(); List<String> list = new ArrayList<String>();

    // Start with entity blanks
    String strs[] = KeyMaker.entityBlanks(getRTParent().getRootBundles().getGlobals());
    for (int i=0;i<strs.length;i++) list.add(strs[i]);

    // Convert back to an array
    strs = new String[list.size()]; for (int i=0;i<strs.length;i++) strs[i] = list.get(i); 

    // Add the finalized strings to the combobox... if a previous one was selected, select it as well
    for (int i=0;i<strs.length;i++) cb.addItem(strs[i]);
    if (sel == null) cb.setSelectedIndex(0); else cb.setSelectedItem(sel);
  }

  /**
   * Generic method to update a combobox with just scalar fields.
   *
   *@param cb combobox to update
   */
  public void         updateScalarBys(JComboBox cb) {
    Object sel = cb.getSelectedItem(); cb.removeAllItems(); List<String> list = new ArrayList<String>();

    // Start with scalar strings
    String strs[] = KeyMaker.scalarBlanks(getRTParent().getRootBundles().getGlobals());
    for (int i=0;i<strs.length;i++) list.add(strs[i]);

    // Check for any fields that are just floats...
    BundlesG globals = getRTParent().getRootBundles().getGlobals();
    String all_strs[] = KeyMaker.blanks(globals, true, true, true, false);
    for (int i=0;i<all_strs.length;i++) {
      if (globals.fieldIndex(all_strs[i]) >= 0) {
        Set<BundlesDT.DT> datatypes = globals.getFieldDataTypes(globals.fieldIndex(all_strs[i]));
        if (datatypes.size() == 1 && datatypes.contains(BundlesDT.DT.FLOAT)) list.add(all_strs[i]);
      }
    }

    // Convert back to an array
    strs = new String[list.size()]; for (int i=0;i<strs.length;i++) strs[i] = list.get(i); 

    // Add the finalized strings to the combobox... if a previous one was selected, select it as well
    for (int i=0;i<strs.length;i++) cb.addItem(strs[i]);
    if (sel == null) cb.setSelectedIndex(0); else cb.setSelectedItem(sel);
  }

  /**
   * Generic method to update a combobox with just the time options.
   *
   *@param cb combobox to update
   */
  public void updateTimeSeriesBys(JComboBox cb) {
    String strs[]; Object sel = cb.getSelectedItem();
    cb.removeAllItems();
    strs = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals(), false, false, false, true);

    List<String> list = new ArrayList<String>();
    for (int i=0;i<strs.length;i++) if (strs[i].startsWith(KeyMaker.BY_TIME_PREFIX_STR)) list.add(strs[i]);
    
    strs = new String[list.size()];
    for (int i=0;i<strs.length;i++) strs[i] = list.get(i);

    for (int i=0;i<strs.length;i++) cb.addItem(strs[i]);
    if (sel == null) cb.setSelectedIndex(0); else cb.setSelectedItem(sel);
  }

  /**
   * Component that handles painting and interacting with the visualization.
   */
  public class RTTimeSeriesComponent extends RTComponent {
    /**
     * 
     */
    private static final long serialVersionUID = 1212489657784629112L;

    /**
     * Return all of the shapes in the current rendering.
     *
     *@return set of rendered shapes
     */
    public Set<Shape>      allShapes()                     { 
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return new HashSet<Shape>();
      return myrc.geom_to_skey.keySet();
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
        Set<String>  str_set = myrc.bundle_to_skeys.get(it.next());
	if (str_set == null) continue;
        Iterator<String> it_skey = str_set.iterator();
        while (it_skey.hasNext()) {
          String skey = it_skey.next();
          shapes.add(myrc.skey_to_geom.get(skey));
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
      return myrc.counter_context.getBundles(myrc.geom_to_skey.get(shape));
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
      Iterator<Shape> it = myrc.geom_to_skey.keySet().iterator();
      while (it.hasNext()) {
        Shape test = it.next();
        if (Utils.genericIntersects(test,shape)) shapes.add(test);
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
      Iterator<Shape> it = myrc.geom_to_skey.keySet().iterator();
      while (it.hasNext()) {
        Shape shape = it.next();
        if (shape.contains(x,y)) shapes.add(shape);
      }
      return shapes;
    }

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
      Bundles     bs          = getRenderBundles();
      String      count_by    = getRTParent().getCountBy(),
                  color_by    = getRTParent().getColorBy();
      String      timeseries  = timeSeries(),
                  y_axis      = yAxis(),
		  entity      = entity(),
                  y_scale     = yScale();

      if (bs != null && color_by != null && count_by != null && timeseries != null && y_axis != null && y_scale != null) {
        RenderContext myrc = new RenderContext(id, bs, count_by, color_by, entity, timeseries, y_axis, y_scale, drawEntities(), drawDots(), drawBoxPlots(), drawGlobals(), varyColor(), getWidth(), getHeight());
        return myrc;
      } else return null;
    }

    /**
     * Class to perform the actual rendering of the view.  This class uses several additional inner classes
     * to both map the axes and to perform statistical operations on the data.
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
       * Field used to color the entities... this is also the per continuous line entity
       */
              color_by, 

      /**
       * Scalar field for the x-axis
       */
	      timeseries, 
      /**
       * Scalar field for the y-axis
       */
	      y_axis,

      /**
       * Entity
       */
              entity,

      /**
       * Scale to use for the y-axis
       */
              y_scale; 

      /**
       * Draw actual data points
       */
      boolean draw_dots,

      /**
       * Draw the entity lines
       */
              draw_entities,

      /**
       * Draw boxplots
       */
              draw_boxplots,

      /**
       * Draw global
       */
               draw_globals,

      /**
       * Flag to indicate to vary the color
       */
              vary_color;

      /**
       * Counter context for each point in the scatter plot
       */
      BundlesCounterContext            counter_context;

      /**
       * Correlate the shape to the string in the counter context
       */
      Map<Shape,String> geom_to_skey = new HashMap<Shape,String>();

      /**
       * Correlate the string in the counter context to the shape
       */
      Map<String,Shape> skey_to_geom = new HashMap<String,Shape>();

      /**
       * Correlate a bundle to the skeys it maps to
       */
      Map<Bundle,Set<String>> bundle_to_skeys = new HashMap<Bundle,Set<String>>();

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
       * List of x-values (timestamps) seen for sorting
       */
      List<Long>        x_sorter     = new ArrayList<Long>(); 

      /**
       * List of y-values for sorting
       */
      List<Long>        y_sorter     = new ArrayList<Long>(); 
      
      /**
       * Set ox x-values (timestamps) seen to make sorter more efficient
       */
      Set<Long>         x_sorter_set = new HashSet<Long>();

      /**
       * Set ox y-values seen to make sorter more efficient
       */
      Set<Long>         y_sorter_set = new HashSet<Long>();

      /**
       * Axis Mapper for x (timestamps)
       */
      Map<Long,Double>  xmap,

      /**
       * Axis Mapper for y-scale
       */
                        ymap;

      /**
       *
       */
      long min_x = Long.MAX_VALUE, 

      /**
       *
       */
           max_x = Long.MIN_VALUE;

      /**
       *
       */
      long min_y = Long.MAX_VALUE, 

      /**
       *
       */
           max_y = Long.MIN_VALUE;

      /**
       * Construct the render context with the specified dataset and GUI configurations.  Use the render ID to 
       * ensure that out-of-date renderings are canceled as soon as possible.
       * Data set to render
       *
       *@param id           render id to abort unnecessary renderings
       *@param bs           dataset to render
       *@param count_by     Field used to count the entities by (for width of icon in xy grid)
       *@param color_by     Field used to color the entities
       *@param entity       Entity to separate out the series data
       *@param timeseries   X-axis...  can be periodic
       *@param y_axis       Scalar field for the y-axis
       *@param y_scale      Scale to use for the y-axis
       *@param vary_color   Flag to indicate to vary the color
       *@param w            Width (in pixels) of the rendering
       *@param h            Height (in pixels) of the rendering
       */
      public               RenderContext(short        id, 
                                         Bundles      bs, 
					 String       count_by, 
					 String       color_by, 
					 String       entity, 
                                         String       timeseries, 
					 String       y_axis, 
					 String       y_scale, 
					 boolean      draw_entities,
					 boolean      draw_dots, 
					 boolean      draw_boxplots, 
					 boolean      draw_globals,
					 boolean      vary_color, 
					 int          w, 
					 int          h) {
        render_id = id; this.bs = bs; this.w = w; this.h = h; this.count_by = count_by; this.color_by = color_by; this.entity = entity;
	this.timeseries = timeseries; this.y_axis = y_axis; this.y_scale = y_scale; this.draw_entities = draw_entities; this.draw_dots = draw_dots; 
	this.draw_boxplots = draw_boxplots; this.draw_globals = draw_globals; this.vary_color = vary_color;

	// Figure out the geometry
	x_ins = y_ins = 18;
	graph_w = w - 2*x_ins;
	graph_h = h - 2*y_ins;

	// Initialize the counter context
	counter_context = new BundlesCounterContext(bs, count_by, color_by);

	// Go through the tablets - first pass is to determine the x coordinates/x axis
        Iterator<Tablet> it_tablet = bs.tabletIterator(); while (it_tablet.hasNext()) {
	  Tablet tablet = it_tablet.next(); if (tablet.hasTimeStamps() && KeyMaker.tabletCompletesBlank(tablet,entity) && KeyMaker.tabletCompletesBlank(tablet,y_axis)) {
	    KeyMaker entity_km = new KeyMaker(tablet, entity); KeyMaker time_km = new KeyMaker(tablet, timeseries), y_km = new KeyMaker(tablet, y_axis);
	    Iterator<Bundle> it_bundle = tablet.bundleIterator(); while (it_bundle.hasNext() && currentRenderID() == getRenderID()) {
	      Bundle bundle = it_bundle.next(); String entities[] = entity_km.stringKeys(bundle); int ys[] = y_km.intKeys(bundle); 
	      if (entities != null && entities.length > 0 && ys != null && ys.length > 0) {
	        long ts = time_km.timeStampKey(bundle);
		if (x_sorter_set.contains(ts) == false) { x_sorter.add(ts); x_sorter_set.add(ts); }
		if (min_x > ts) min_x = ts; if (max_x < ts) max_x = ts;
                for (int i=0;i<ys.length;i++) {
		  if (ys[i] < min_y) min_y = ys[i];
		  if (ys[i] > max_y) max_y = ys[i];
		}
	      }
	    }
	  }
	}
	if ((max_y - min_y) == 0.0) max_y = min_y + 1; // protect against div by zero

        // X-axis Mapper
	if (currentRenderID() == getRenderID()) xmap = AxisMapper.calculateMapping(AxisMapper.LINEAR_SCALE_STR, x_sorter, min_x, max_x);

	// Go through tablets a second time and map to screen space
        it_tablet = bs.tabletIterator(); while (it_tablet.hasNext()) {
	  Tablet tablet = it_tablet.next(); if (tablet.hasTimeStamps() && KeyMaker.tabletCompletesBlank(tablet,entity) && KeyMaker.tabletCompletesBlank(tablet,y_axis)) {
	    KeyMaker entity_km = new KeyMaker(tablet, entity); KeyMaker time_km = new KeyMaker(tablet, timeseries), y_km = new KeyMaker(tablet, y_axis);
	    Iterator<Bundle> it_bundle = tablet.bundleIterator(); while (it_bundle.hasNext() && currentRenderID() == getRenderID()) {
	      Bundle bundle = it_bundle.next(); String entities[] = entity_km.stringKeys(bundle); int ys[] = y_km.intKeys(bundle); 
	      if (entities != null && entities.length > 0 && ys != null && ys.length > 0) {
	        long ts = time_km.timeStampKey(bundle);
		int  sx = (int) (x_ins + xmap.get(ts) * graph_w); sx_set.add(sx);
		if (global_list.containsKey(sx) == false) { global_list.put(sx, new ArrayList<Integer>()); }

                for (int entity_i=0;entity_i<entities.length;entity_i++) {
                  String ent = entities[entity_i];
                  if (entity_list.         containsKey(ent) == false) { entity_list.   put(ent, new HashMap<Integer,List<Integer>>()); }
		  if (entity_list.get(ent).containsKey(sx)  == false) { entity_list.   get(ent).put(sx, new ArrayList<Integer>()); }

		  for (int i=0;i<ys.length;i++) {
                    long l = ys[i];
		    // Screen coordinate conversion
		    int sy = worldYToScreenY(l); String skey = sx + "," + sy; skey_to_sx.put(skey,sx); skey_to_sy.put(skey,sy);
		    // Counter context update
		    counter_context.count(bundle,skey);
		    // Tabulate based on entities
                    entity_list.   get(ent).get(sx).add((int) l);
		    // Tabulate based on globals
		    global_list.get(sx).add((int) l);
                  }
                }
	      }
	    }
	  }
	}
      }

      /**
       * Set of screen x coordinates
       */
      Set<Integer> sx_set = new HashSet<Integer>();

      /**
       * Convert a world y coordinate to a screen y coordinate.
       *
       *@param wy world y coordinate
       *
       *@return screen y coordinate
       */
      public int worldYToScreenY(long wy) {
        return (int) (y_ins + graph_h - (graph_h * (wy - min_y))/(max_y - min_y));
      }

      /**
       * Map from entity to the screen x coordinate to the list of values at that location
       */
      Map<String,Map<Integer,List<Integer>>> entity_list = new HashMap<String,Map<Integer,List<Integer>>>();

      /**
       * X coordinate to the list of values - used for boxplot calculation.  Also used to calculate the number of samples for the average.
       */
      Map<Integer,List<Integer>>       global_list    = new HashMap<Integer,List<Integer>>();

      /**
       * Lookup for screen key to screen x coordinate
       */
      Map<String,Integer> skey_to_sx = new HashMap<String,Integer>(),

      /**
       * Lookup for screen key to screen y coordinate
       */
                          skey_to_sy = new HashMap<String,Integer>();

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

	   //
	   // Get the screen coordinates in a sorted list
	   //
           List<Integer> sx_sort = new ArrayList<Integer>(); sx_sort.addAll(sx_set); Collections.sort(sx_sort);

           //
           // Draw dots
           //
           if (draw_dots) {
	     Composite orig_comp = g2d.getComposite();
	     if (draw_globals) g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.4f));
	     g2d.setColor(RTColorManager.getColor("data", "default"));
	     Iterator<String> it = counter_context.binIterator(); while (it.hasNext()) {
	       String skey = it.next(); int sx = skey_to_sx.get(skey), sy = skey_to_sy.get(skey);
               if (vary_color) g2d.setColor(counter_context.binColor(skey));
               Ellipse2D.Float ellipse = new Ellipse2D.Float(sx,sy,2,2);
               g2d.fill(ellipse);
               geom_to_skey.put(ellipse, skey);
               skey_to_geom.put(skey,    ellipse);
	       Iterator<Bundle> it_bun = counter_context.getBundles(skey).iterator();
	       while (it_bun.hasNext()) {
	         Bundle bundle = it_bun.next();
		 if (bundle_to_skeys.containsKey(bundle) == false) bundle_to_skeys.put(bundle, new HashSet<String>());
		 bundle_to_skeys.get(bundle).add(skey);
	       }
	     }
	     g2d.setComposite(orig_comp);
           }

	   //
	   // Draw the global average
	   //
	   if (draw_globals) {
	     int last_sx = -1; QuickStats last_qs = null;
	     Stroke orig_stroke = g2d.getStroke();
	     for (int i=0;i<sx_sort.size()-1;i++) {
	       int sx0 = sx_sort.get(i), sx1 = sx_sort.get(i+1);
	       if (global_list.containsKey(sx0) && global_list.containsKey(sx1)) {
	         QuickStats qs_sx0, qs_sx1;
	         if (last_sx == sx0 && last_qs != null) qs_sx0 = last_qs; else qs_sx0 = new QuickStats(global_list.get(sx0));
		 qs_sx1 = new QuickStats(global_list.get(sx1));

                 int sy0, sy1;

	         g2d.setStroke(new BasicStroke(1.8f)); g2d.setColor(RTColorManager.getColor("data", "default"));
		 sy0 = worldYToScreenY((long) qs_sx0.average()); sy1 = worldYToScreenY((long) qs_sx1.average()); g2d.drawLine(sx0,sy0,sx1,sy1);

	         g2d.setStroke(new BasicStroke(0.6f)); 
                 g2d.setColor(RTColorManager.getColor("data", "min"));
		 sy0 = worldYToScreenY((long) qs_sx0.min());     sy1 = worldYToScreenY((long) qs_sx1.min());     g2d.drawLine(sx0,sy0,sx1,sy1);
                 g2d.setColor(RTColorManager.getColor("data", "max"));
		 sy0 = worldYToScreenY((long) qs_sx0.max());     sy1 = worldYToScreenY((long) qs_sx1.max());     g2d.drawLine(sx0,sy0,sx1,sy1);
	         // g2d.setStroke(new BasicStroke(0.8f)); 
                 // g2d.setColor(RTColorManager.getColor("data", "stdev"));

                 last_sx = sx1; last_qs = qs_sx1;
	       }
	     }
	     g2d.setStroke(orig_stroke);
	   }

	   //
	   // Draw the entity paths
	   //
	   if (draw_entities) {
             Iterator<String> it_ent = entity_list.keySet().iterator(); while (it_ent.hasNext()) {
	       int last_sx = -1; QuickStats last_qs = null;
	       String entity = it_ent.next(); 
	       if (vary_color) g2d.setColor(RTColorManager.getColor(entity));
	       for (int i=0;i<sx_sort.size()-1;i++) {
	         int sx0 = sx_sort.get(i), sx1 = sx_sort.get(i+1);
		 if (entity_list.get(entity).containsKey(sx0) && entity_list.get(entity).containsKey(sx1)) {
		   QuickStats qs_sx0, qs_sx1;
		   if (last_sx == sx0 && last_qs != null) qs_sx0 = last_qs; else qs_sx0 = new QuickStats(entity_list.get(entity).get(sx0));
		   qs_sx1 = new QuickStats(entity_list.get(entity).get(sx1));
		   int sy0 = worldYToScreenY((long) qs_sx0.average()), sy1 = worldYToScreenY((long) qs_sx1.average());
		   g2d.drawLine(sx0,sy0,sx1,sy1);
                   last_sx = sx1; last_qs = qs_sx1;
		 }
	       }
	     }
           }
         } finally { if (g2d != null) g2d.dispose(); }
	}
	return base_bi;
      }
    }
  }
}

