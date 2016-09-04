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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
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
import javax.swing.JMenu;
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

interface StatsInterface { public void addInts(String entity, int ints[]); public double getStat(String entity); }

/**
 * Visualization for showing how an entity within a bundle is affected by the
 * related scalar values in the bundle.  Not fully complete in implementation.
 *
 *@author  D. Trimm
 *@version 0.9
 */
public class RTXYEntityPanel extends RTPanel {
  /**
   * 
   */
  private static final long serialVersionUID = -400131513295998028L;

  /**
   * Entity to place in xy coordinate space
   */
  JComboBox            ent_cb, 

  /**
   * Sub-entity to concatenate in xy coordinate space
   */
                       ent2_cb, 

  /**
   * Scalar field for x-axis
   */
                       x_cb, 

  /**
   * Scalar field for y-axis
   */
		       y_cb, 

  /**
   * Mathematical operation for scalar field in x
   */
		       xm_cb, 

  /**
   * Mathematical operation for scalar field in y
   */
		       ym_cb;

  /**
   * Checkbox to render width (otherwise, constant)
   */
  JCheckBoxMenuItem    width_cbmi, 

  /**
   * Checkbox to render color (otherwise, light gray)
   */
                       color_cbmi, 

  /**
   * Checkbox to render labels (otherwise, none)
   */
		       labels_cbmi;

  /**
   * Methods to scale x-axis
   */
  JRadioButtonMenuItem x_scales[], 

  /**
   * Methods to scale y-axis
   */
                       y_scales[], 

  /**
   * Shape highlight options
   */
		       highlight_shapes[];

  /**
   * Strings representing scaling options for axes
   */
  final static String  LINEAR_SCALE_STR       = "Linear",
                       LOG_SCALE_STR          = "Log",
		       EQUAL_SCALE_STR        = "Equal";


  /**
   * Array for axes scales
   */
  final static String scale_strs[] = { LINEAR_SCALE_STR, LOG_SCALE_STR, EQUAL_SCALE_STR };

  /**
   * Options for brushing shape
   */
  final static String HL_CIRCULAR_STR        = "Circular",
                      HL_SQUARE_STR          = "Square",
                      HL_HORIZONTAL_STR      = "Horizontal",
                      HL_VERTICAL_STR        = "Vertical";

  /**
   * Array for the brushing shape strings
   */
  final static String hl_shape_strs[] = { HL_CIRCULAR_STR, HL_SQUARE_STR, HL_HORIZONTAL_STR, HL_VERTICAL_STR };

  /**
   * Strings for mathematical operations
   */
  final static String MATH_SUM_STR           = "Sum",
                      MATH_AVG_STR           = "Avg",
		      MATH_STD_STR           = "StDv",
		      MATH_MIN_STR           = "Min",
		      MATH_MAX_STR           = "Max",
		      MATH_MED_STR           = "Median";

  /**
   * Array to hold mathematical string operations
   */
  final static String math_strs[] = { MATH_SUM_STR, MATH_AVG_STR, MATH_STD_STR, MATH_MIN_STR, MATH_MAX_STR, MATH_MED_STR };

  /**
   * Construct a new panel with the specified GUI parent.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt       application reference
   */
  public RTXYEntityPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      { 
    super(win_type, win_pos, win_uniq, rt);   
    // Main component
    add("Center", component = new RTXYEntityComponent());

    // Popup menu
    getRTPopupMenu().add(labels_cbmi = new JCheckBoxMenuItem("Draw Labels"));
    getRTPopupMenu().add(width_cbmi  = new JCheckBoxMenuItem("Vary Size"));
    getRTPopupMenu().add(color_cbmi  = new JCheckBoxMenuItem("Vary Color"));

    // Scales
    getRTPopupMenu().addSeparator();
    JMenu submenu; ButtonGroup bg;
    submenu = new JMenu("X Scale"); bg = new ButtonGroup(); getRTPopupMenu().add(submenu);
    x_scales = new JRadioButtonMenuItem[scale_strs.length]; for (int i=0;i<x_scales.length;i++) { 
      x_scales[i] = new JRadioButtonMenuItem(scale_strs[i], i == 0); submenu.add(x_scales[i]); bg.add(x_scales[i]); defaultListener(x_scales[i]); }
    submenu = new JMenu("Y Scale"); bg = new ButtonGroup(); getRTPopupMenu().add(submenu);
    y_scales = new JRadioButtonMenuItem[scale_strs.length]; for (int i=0;i<y_scales.length;i++) { 
      y_scales[i] = new JRadioButtonMenuItem(scale_strs[i], i == 0); submenu.add(y_scales[i]); bg.add(y_scales[i]); defaultListener(y_scales[i]); }

    // Highlight shapes
    getRTPopupMenu().addSeparator();
    submenu = new JMenu("HL Shape"); bg = new ButtonGroup(); getRTPopupMenu().add(submenu);
    highlight_shapes = new JRadioButtonMenuItem[hl_shape_strs.length]; for (int i=0;i<highlight_shapes.length;i++) {
      highlight_shapes[i] = new JRadioButtonMenuItem(hl_shape_strs[i], i == 0); submenu.add(highlight_shapes[i]); bg.add(highlight_shapes[i]); defaultListener(highlight_shapes[i]); }

    // Configuration panel
    JPanel panel = new JPanel(new FlowLayout());
    panel.add(new JLabel("Ent"));  panel.add(ent_cb  = new JComboBox());
                                   panel.add(ent2_cb = new JComboBox());
    panel.add(new JLabel("X"));    panel.add(x_cb    = new JComboBox()); panel.add(xm_cb = new JComboBox(math_strs));
    panel.add(new JLabel("Y"));    panel.add(y_cb    = new JComboBox()); panel.add(ym_cb = new JComboBox(math_strs));
    defaultListener(ent_cb); defaultListener(ent2_cb); defaultListener(x_cb); defaultListener(xm_cb); defaultListener(y_cb); defaultListener(ym_cb);
    defaultListener(width_cbmi); defaultListener(color_cbmi); defaultListener(labels_cbmi);
    // Fill the comboboxes
    updateBys();
    add("South", panel);
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "xyentity"; }

  /**
   * Get the entiity to perform the operations on
   *
   *@return entity for operations
   */
  public String       entity        ()           { return (String) ent_cb.getSelectedItem(); }

  /**
   * Set the entity to perform the operation on
   *
   *@param str entity for operations
   */
  public void         entity        (String str) { ent_cb.setSelectedItem(str); }

  /**
   * Get the sub-entity to perform the operations on
   *
   *@return sub-entity for operations
   */
  public String       entity2       ()           { return (String) ent2_cb.getSelectedItem(); }

  /**
   * Set the sub-entity to perform operations on
   *
   *@param str sub-entity for operations
   */
  public void         entity2       (String str) { ent2_cb.setSelectedItem(str); }

  /**
   * Return the x-axis scalar field to use for operations
   *
   *@return x-axis scalar field
   */
  public String       xAxis         ()           { return (String) x_cb.getSelectedItem(); }

  /**
   * Set the x-axis scalar field to use for operations
   *
   *@param str x-axis scalar field
   */
  public void         xAxis         (String str) { x_cb.setSelectedItem(str); }

  /**
   * Return the mathematical operation for the scalar x field/axis
   *
   *@return math operation
   */
  public String       xMath         ()           { return (String) xm_cb.getSelectedItem(); }

  /**
   * Set the mathematical operatino for the scalar x field/axis
   *
   *@param math operation
   */
  public void         xMath         (String str) { xm_cb.setSelectedItem(str); }

  /**
   * Return the scale to use on the x-axis
   *
   *@return x-axis scale
   */
  public String       xScale        ()           { return selectedItem(x_scales); }

  /**
   * Set the scale to use on the x-axis
   *
   *@param str x-axis scale
   */
  public void         xScale        (String str) { setSelectedItem(x_scales, str); }

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
   * Return the mathematical operation for the scalar y field/axis
   *
   *@return math operation
   */
  public String       yMath         ()           { return (String) ym_cb.getSelectedItem(); }

  /**
   * Set the mathematical operation for hte scalar y field/axis
   *
   *@param str math operation
   */
  public void         yMath         (String str) { ym_cb.setSelectedItem(str); }

  /**
   * Return the scale to use on the y-axis
   *
   *@return y-axis scale
   */
  public String       yScale        ()           { return selectedItem(y_scales); }

  /**
   * Set the scale to use on the y-axis.
   *
   *@param str y-axis scale
   */
  public void         yScale        (String str) { setSelectedItem(y_scales, str); }


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
   * Determine if the width should be varied in the visualization.
   *
   *@return true if width should be varied, false for constant width on the scatter plots
   */
  public boolean      varyWidth     ()           { return width_cbmi.isSelected(); }

  /**
   * Set the flag for varying with width in the rendering.
   *
   *@param f flag to vary width
   */
  public void         varyWidth     (boolean f)  { width_cbmi.setSelected(f); }

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
   * Return the highlighted shape setting.
   *
   *@return highlighted shape string
   */
  public String       highlightShape()           { return selectedItem(highlight_shapes); }

  /**
   * Set the highlighted shape setting.
   *
   *@param str highlighted shape string
   */
  public void         highlightShape(String str) { setSelectedItem(highlight_shapes, str); }

  /**
   * Get the configuration of this component as a string.  Supposed to be used for
   * bookmarking a view so that it can be re-rendered.
   *
   *@return string representing configuration of the panel
   */
  public String  getConfig()  { return "RTXYEntityPanel" + BundlesDT.DELIM +
                                       "entity="    + Utils.encToURL(entity())          + BundlesDT.DELIM +
                                       "entity2="   + Utils.encToURL(entity2())         + BundlesDT.DELIM +
                                       "xaxis="     + Utils.encToURL(xAxis())           + BundlesDT.DELIM +
                                       "xmath="     + Utils.encToURL(xMath())           + BundlesDT.DELIM +
                                       "xscale="    + Utils.encToURL(xScale())          + BundlesDT.DELIM +
                                       "yaxis="     + Utils.encToURL(yAxis())           + BundlesDT.DELIM +
                                       "ymath="     + Utils.encToURL(yMath())           + BundlesDT.DELIM +
                                       "yscale="    + Utils.encToURL(yScale())          + BundlesDT.DELIM +
				       "labels="    + (drawLabels() ? "true" : "false") + BundlesDT.DELIM +
                                       "vwidth="    + (varyWidth()  ? "true" : "false") + BundlesDT.DELIM +
                                       "vcolor="    + (varyColor()  ? "true" : "false") + BundlesDT.DELIM +
                                       "hlshape="   + Utils.encToURL(highlightShape()) ; }

  /**
   * Set the configuration of this component from the string representation.
   *
   *@return previously returned string from the getConfig() method
   */
  public void    setConfig(String str) {
    StringTokenizer st = new StringTokenizer(str,BundlesDT.DELIM);
    if (st.nextToken().equals("RTXYEntityPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not An RTXYEntityPanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st.hasMoreTokens() ? st2.nextToken() : "";
      if      (type.equals("entity"))   entity(Utils.decFmURL(value));
      else if (type.equals("entity2"))  entity2(Utils.decFmURL(value));
      else if (type.equals("xaxis"))    xAxis(Utils.decFmURL(value));
      else if (type.equals("xmath"))    xMath(Utils.decFmURL(value));
      else if (type.equals("xscale"))   xScale(Utils.decFmURL(value));
      else if (type.equals("yaxis"))    yAxis(Utils.decFmURL(value));
      else if (type.equals("ymath"))    yMath(Utils.decFmURL(value));
      else if (type.equals("yscale"))   yScale(Utils.decFmURL(value));
      else if (type.equals("labels"))   drawLabels(value.toLowerCase().equals("true"));
      else if (type.equals("vwidth"))   varyWidth(value.toLowerCase().equals("true"));
      else if (type.equals("vcolor"))   varyColor(value.toLowerCase().equals("true"));
      else if (type.equals("hlshape"))  highlightShape(Utils.decFmURL(value));
      else throw new RuntimeException("Do Not Understand Type Value \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * Update the comboboxes for selecting global fields when new data is
   * loaded.
   */
  public void         updateBys() { updateEntityBys(ent_cb); updateEntityBys(ent2_cb); updateScalarBys(x_cb); updateScalarBys(y_cb); }

  /**
   * Generic method to update a combobox.
   *
   *@param cb combobox to update
   */
  public void         updateEntityBys(JComboBox cb) {
    String strs[]; Object sel = cb.getSelectedItem();
    cb.removeAllItems();
    strs = KeyMaker.entityBlanks(getRTParent().getRootBundles().getGlobals());
    if (cb == ent2_cb) cb.addItem(BundlesDT.COUNT_BY_NONE);
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
    strs = KeyMaker.scalarBlanks(getRTParent().getRootBundles().getGlobals());
    for (int i=0;i<strs.length;i++) cb.addItem(strs[i]);
    if (sel == null) cb.setSelectedIndex(0); else cb.setSelectedItem(cb);
  }

  /**
   * Component that handles painting and interacting with the visualization.
   */
  public class RTXYEntityComponent extends RTComponent {
    /**
     * 
     */
    private static final long serialVersionUID = 7912789159783639133L;

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
     * Strings that represent different highlight brushes for the interactive
     * visualization.
     */
    final static String HL_CIRCULAR_STR     = "Circular",
                        HL_SQUARE_STR       = "Square",
                        HL_HORIZONTAL_STR   = "Horiz",
                        HL_VERTICAL_STR     = "Vertical";
    /**
     * Return the shape used to match shapes directly under the mouse.
     *
     *@param  x x-coordinate of mouse
     *@param  y y-coordinate of mouse
     *
     *@return shape under mouse
     */
    public Shape getZeroOrderShape(int x, int y) {
      String str = highlightShape();
      if        (str.equals(HL_CIRCULAR_STR))   { return new Ellipse2D.Double(x-2,y-2,5,5);
      } else if (str.equals(HL_SQUARE_STR))     { return new Rectangle2D.Double(x-1,y-1,3,3);
      } else if (str.equals(HL_HORIZONTAL_STR)) { return new Rectangle2D.Double(x-getWidth()*2,y-1,getWidth()*4,3);
      } else if (str.equals(HL_VERTICAL_STR))   { return new Rectangle2D.Double(x-1,y-getHeight()*2,3,getHeight()*4);
      } else                                      return new Rectangle2D.Double(x,y,1,1);
    }

    /**
     * Return the shape used to match shapes directly near the mouse.
     *
     *@param  x x-coordinate of mouse
     *@param  y y-coordinate of mouse
     *
     *@return shape near mouse
     *
     */
    public Shape getFirstOrderShape(int x, int y) {
      String str = highlightShape();
      if        (str.equals(HL_CIRCULAR_STR))   { return new Ellipse2D.Double(x-5,y-5,11,11);
      } else if (str.equals(HL_SQUARE_STR))     { return new Rectangle2D.Double(x-5,y-5,11,11);
      } else if (str.equals(HL_HORIZONTAL_STR)) { return new Rectangle2D.Double(x-getWidth()*2,y-3,getWidth()*4,7);
      } else if (str.equals(HL_VERTICAL_STR))   { return new Rectangle2D.Double(x-3,y-getHeight()*2,7,getHeight()*4);
      } else                                      return new Rectangle2D.Double(x,y,1,1);
    }

    /**
     * Return the shape used to match shapes directly further from the mouse.
     *
     *@param  x x-coordinate of mouse
     *@param  y y-coordinate of mouse
     *
     *@return shape further from mouse mouse
     */
    public Shape getSecondOrderShape(int x, int y) {
      String str = highlightShape();
      if        (str.equals(HL_CIRCULAR_STR))   { return new Ellipse2D.Double(x-10,y-10,21,21);
      } else if (str.equals(HL_SQUARE_STR))     { return new Rectangle2D.Double(x-10,y-10,21,21);
      } else if (str.equals(HL_HORIZONTAL_STR)) { return new Rectangle2D.Double(x-getWidth()*2,y-5,getWidth()*4,11);
      } else if (str.equals(HL_VERTICAL_STR))   { return new Rectangle2D.Double(x-5,y-getHeight()*2,11,getHeight()*4);
      } else                                      return new Rectangle2D.Double(x,y,1,1);
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
      Bundles bs       = getRenderBundles();
      String  count_by = getRTParent().getCountBy(),
              color_by = getRTParent().getColorBy();
      String  entity   = entity(),
              entity2  = entity2(),
              x_axis   = xAxis(),
              x_math   = xMath(),
	      x_scale  = xScale(),
              y_axis   = yAxis(),
	      y_math   = yMath(),
	      y_scale  = yScale();
      if (entity2 != null && entity2.equals(BundlesDT.COUNT_BY_NONE)) entity2 = null;
      if (bs != null && count_by != null && x_axis != null && y_axis != null && x_math != null && y_math != null && x_scale != null && y_scale != null) {
        RenderContext myrc = new RenderContext(id, bs, count_by, color_by, entity, entity2, x_axis, x_math, x_scale, y_axis, y_math, y_scale, drawLabels(), varyWidth(), varyColor(), getWidth(), getHeight());
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
       * Field used to color the entities
       */
              color_by, 
      /**
       * Entity to use for the scatter plot
       */
	      entity_hdr, 
      /**
       * Sub-entity to use for the scatter plot
       */
	      entity2_hdr, 
      /**
       * Scalar field for the x-axis
       */
	      x_axis, 
      /**
       * Mathematical operation on the x-axis field
       */
	      x_math, 
      /**
       * Scale to use for the x-axis
       */
	      x_scale, 
      /**
       * Scalar field for the y-axis
       */
	      y_axis, 
      /**
       * Mathematical operation on the y-axis field
       */
	      y_math, 
      /**
       * Scale to use for the y-axis
       */
	      y_scale; 
      /**
       * Flag to indicate to draw labels
       */
      boolean draw_labels, 
      /**
       * Flag to indicate to vary width
       */
              vary_width, 
      /**
       * Flag to indicate to vary the color
       */
	      vary_color;
      /**
       * Counter context for each point in the scatter plot
       */
      BundlesCounterContext            counter_context;
      /**
       * Map to transform world x to screen x
       */
      Mapper                           xmap, 
      /**
       * Map to transform wold y to screen y
       */
                                       ymap;
      /**
       * Statistical interface for x-axis operation
       */
      StatsInterface                   xstat, 
      /**
       * Statistical interface for y-axis operation
       */
                                       ystat;
      /**
       * Translation for the geometrically rendered shape to the screen key
       */
      Map<Shape,String>            geom_to_skey            = new HashMap<Shape,String>();
      /**
       * Translation for the screen key to the geometrically rendered shape
       */
      Map<String,Shape>            skey_to_geom            = new HashMap<String,Shape>();
      /**
       * Translation for the bundle (record) to the screen key
       */
      Map<Bundle,Set<String>>  bundle_to_skeys         = new HashMap<Bundle,Set<String>>();
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
       * Render timing parameters for measuring performance
       */
      long                             rts0,
                                       rts1, 
				       rts2, 
				       rts3;

      /**
       * Construct the render context with the specified dataset and GUI configurations.  Use the render ID to 
       * ensure that out-of-date renderings are canceled as soon as possible.
       * Data set to render
       *
       *@param id           render id to abort unnecessary renderings
       *@param bs           dataset to render
       *@param count_by     Field used to count the entities by (for width of icon in xy grid)
       *@param color_by     Field used to color the entities
       *@param entity_hdr   Entity to use for the scatter plot
       *@param entity2_hdr  Sub-entity to use for the scatter plot
       *@param x_axis       Scalar field for the x-axis
       *@param x_math       Mathematical operation on the x-axis field
       *@param x_scale      Scale to use for the x-axis
       *@param y_axis       Scalar field for the y-axis
       *@param y_math       Mathematical operation on the y-axis field
       *@param y_scale      Scale to use for the y-axis
       *@param draw_labels  Flag to indicate to draw labels
       *@param vary_width   Flag to indicate to vary width
       *@param vary_color   Flag to indicate to vary the color
       *@param w            Width (in pixels) of the rendering
       *@param h            Height (in pixels) of the rendering
       */
      public               RenderContext(short id, Bundles bs, String count_by, String color_by, String entity_hdr, String entity2_hdr, String x_axis, String x_math, String x_scale, String y_axis, String y_math, String y_scale, boolean draw_labels, boolean vary_width, boolean vary_color, int w, int h) {
        render_id = id; this.bs = bs; this.w = w; this.h = h; this.count_by = count_by; this.color_by = color_by; this.entity_hdr = entity_hdr; this.entity2_hdr = entity2_hdr; this.x_axis = x_axis; this.x_math = x_math; this.x_scale = x_scale; this.y_axis = y_axis; this.y_math = y_math; this.y_scale = y_scale; this.draw_labels = draw_labels; this.vary_width = vary_width; this.vary_color = vary_color;
	rts0 = System.currentTimeMillis();

	// Figure out the geometry
	x_ins = y_ins = 18;
	graph_w = w - 2*x_ins;
	graph_h = h - 2*y_ins;

	// Initialize the counter context
	counter_context = new BundlesCounterContext(bs, count_by, color_by);
        xstat = createStats(x_math); ystat = createStats(y_math);

	// Go through the tablets
        Iterator<Tablet> it_t = bs.tabletIterator();
	while (it_t.hasNext() && currentRenderID() == getRenderID()) {
	  Tablet tablet = it_t.next(); KeyMaker ekm = null, e2km = null, xkm = null, ykm = null;

	  // Check to see if the bundle can be used for counting
          boolean tablet_can_count = count_by.equals(BundlesDT.COUNT_BY_BUNS) || KeyMaker.tabletCompletesBlank(tablet, count_by);

	  // Check to see what the bundle can provide
	  if (KeyMaker.tabletCompletesBlank(tablet, entity_hdr))  ekm  = new KeyMaker(tablet, entity_hdr);
	  if (entity2_hdr != null &&
	      KeyMaker.tabletCompletesBlank(tablet, entity2_hdr)) e2km = new KeyMaker(tablet, entity2_hdr);
	  if (KeyMaker.tabletCompletesBlank(tablet, x_axis))      xkm  = new KeyMaker(tablet, x_axis);
	  if (KeyMaker.tabletCompletesBlank(tablet, y_axis))      ykm  = new KeyMaker(tablet, y_axis);

	  // If it provides something, go through the bundles
	  if (ekm != null && (xkm != null || ykm != null)) {
	    Iterator<Bundle> it_b = tablet.bundleIterator();
	    while (it_b.hasNext() && currentRenderID() == getRenderID()) {
	      Bundle bundle = it_b.next(); if (bundle_to_skeys.containsKey(bundle) == false) bundle_to_skeys.put(bundle, new HashSet<String>());
              // If second level of entities is active, apply that
	      if (e2km != null) {
	        String ents[] = ekm.stringKeys(bundle), ents2[] = e2km.stringKeys(bundle);
                if (ents2 != null && ents2.length > 0) { // If entity second level results, concatenate
	          for (int i=0;i<ents.length;i++) {
		    for (int j=0;j<ents2.length;j++) {
                      String entity = ents[i] + " " + BundlesDT.DELIM + " " + ents2[j];
		      if (tablet_can_count) counter_context.count(bundle, entity); 
		      bundle_to_skeys.get(bundle).add(entity);
		      if (xkm != null) { int ints[] = xkm.intKeys(bundle); xstat.addInts(entity, ints); }
		      if (ykm != null) { int ints[] = ykm.intKeys(bundle); ystat.addInts(entity, ints); }
                    }
	          }
                } else { // Else, no second level results
	          for (int i=0;i<ents.length;i++) {
                    String entity = ents[i];
		    if (tablet_can_count) counter_context.count(bundle, entity); 
		    bundle_to_skeys.get(bundle).add(entity);
		    if (xkm != null) { int ints[] = xkm.intKeys(bundle); xstat.addInts(entity, ints); }
		    if (ykm != null) { int ints[] = ykm.intKeys(bundle); ystat.addInts(entity, ints); }
                  }
	        }
	      } else            { // Otherwise, entity second level not active
	        String ents[] = ekm.stringKeys(bundle);
	        for (int i=0;i<ents.length;i++) {
                  String entity = ents[i];
		  if (tablet_can_count) counter_context.count(bundle, entity); 
		  bundle_to_skeys.get(bundle).add(entity);
		  if (xkm != null) { int ints[] = xkm.intKeys(bundle); xstat.addInts(entity, ints); }
		  if (ykm != null) { int ints[] = ykm.intKeys(bundle); ystat.addInts(entity, ints); }
	        }
              }
	    }
	  } else {
	    Iterator<Bundle> it_b = tablet.bundleIterator();
	    while (it_b.hasNext()) addToNoMappingSet(it_b.next());
	  }
 	}

	// Figure out the scale and mappings
	xmap = new Mapper(); ymap = new Mapper();
	Iterator<String> it = counter_context.binIterator();
	while (it.hasNext()) {
	  String entity = it.next();
	  xmap.add(xstat.getStat(entity));
	  ymap.add(ystat.getStat(entity));
	}
	xmap.calculateMapping(x_scale, x_ins,            graph_w);
	ymap.calculateMapping(y_scale, y_ins + graph_h, -graph_h);

	// Calculate the render time
	rts1 = System.currentTimeMillis();
      }

      /**
       * Inner class used to convert world coordinates to screen coordinates.
       */
      class Mapper {
        double              min       = Double.POSITIVE_INFINITY,
	                    max       = Double.NEGATIVE_INFINITY;
        List<Double>        list      = new ArrayList<Double>(); 
	Set<Double>         set       = new HashSet<Double>();
	Map<Double,Integer> to_screen = new HashMap<Double,Integer>();
	Color               color     = RTColorManager.getColor("data", "default");

	/**
	 * Add a new coordinate to the list of coordinates.  Check to see if it's a min or a max.
	 *
	 *@param d coordinate to add
	 */
        public void add(double d) { if (d < min) min = d; if (d > max) max = d; if (set.contains(d) == false) { set.add(d); list.add(d); } }

	/**
	 * Calculate the mapping based on the specified method.  Use the screen inset and width parameters
	 * for the transformation.
	 *
	 *@param method scale to use
	 *@param inset  inset from 0 for the actual graph
	 *@param width  graph width
	 */
	public void calculateMapping(String method, int inset, int width) {
	  if        (method.equals(LINEAR_SCALE_STR)) {
	    color = RTColorManager.getColor("label", "linear");
            for (int i=0;i<list.size();i++) to_screen.put(list.get(i), inset + (int) ((width * (list.get(i) - min))/(max - min)));
	  } else if (method.equals(LOG_SCALE_STR))    {
	    min   = 0.0;
	    color = RTColorManager.getColor("label", "log"); 
	    for (int i=0;i<list.size();i++) {
	      if (list.get(i) <= 0.0) to_screen.put(list.get(i), inset);
	      else                    to_screen.put(list.get(i), inset + (int) (width * Math.log(list.get(i)) / Math.log(max)));
            }
	  } else if (method.equals(EQUAL_SCALE_STR))  {
	    color = RTColorManager.getColor("label", "equal");
	    Collections.sort(list);
	    if (list.size() == 1) { to_screen.put(list.get(0), inset + width/2);
	    } else                { int denom = list.size()-1; if (denom <= 0) denom = 1;
	                            for (int i=0;i<list.size();i++) to_screen.put(list.get(i), inset + (int) ((width * i) / denom));
            }
	  }
	}

	/**
	 * Transform a previously added double coordinate to the screen coordinate.
	 *
	 *@param  d world coordinate to transform
	 *
	 *@return screen coordinate
	 */
        public int    toScreen(double d) { return to_screen.get(d); }

	/**
	 * Return the minimum world coordinate.
	 *
	 *@return minimum coordinate
	 */
	public double getMin()           { return min; }

	/**
	 * Return the maximum world coordinate.
	 *
	 *@return maximum coordinate
	 */
	public double getMax()           { return max; }

	/**
	 * Return the color of the legend for this scale.
	 *
	 *@return legend color
	 */
	public Color  getColor()         { return color; }
      }

      /** 
       * Abstraction for simple statistics.
       */
      class SimpleStats {
        /**
	 * Map to transform entities to the cooresponding stats record.
	 */
        Map<String,StatsRec> recs = new HashMap<String,StatsRec>();
	/**
	 * Add values for the specified entities to be calculated into the statistics.
	 *
	 *@param entity entity to add stats for
	 *@param ints   values related to entity
	 */
	public void addInts(String entity, int ints[]) { if (recs.containsKey(entity) == false) recs.put(entity, new StatsRec()); recs.get(entity).addIntsSimple(ints);  }
      }

      /**
       * Abstract for more complex statistics.
       */
      class ArrayStats  {
        /**
	 * Map to transform entities to the cooresponding stats record.
	 */
        Map<String,StatsRec> recs = new HashMap<String,StatsRec>();
	/**
	 * Add values for the specified entities to be calculated into the statistics.
	 *
	 *@param entity entity to add stats for
	 *@param ints   values related to entity
	 */
	public void addInts(String entity, int ints[]) { if (recs.containsKey(entity) == false) recs.put(entity, new StatsRec()); recs.get(entity).addIntsComplex(ints); }
      }

      /**
       * Statistical record on a per entity basis.  Keeps track of various statistical properties of an entity.
       */
      class StatsRec    {
        /**
	 * Number of samples added to the statistics
	 */
        int                samples, 
	/**
	 * Minimum value
	 */
	                   min, 
        /**
	 * Maximum value
	 */
			   max; 
        /**
	 * Sum of values
	 */
	long               sum;
        /**
	 * List of the values added (for median calculation)
	 */
	List<Integer>      list   = new ArrayList<Integer>();
        /**
	 * Flag to indicate that the list is sorted.
	 */
	boolean            sorted = false;

        /**
	 * Add values for the simple statistic calculation.
	 *
	 *@param ints values to add
	 */
	public void addIntsSimple  (int ints[]) { if (samples == 0) { min = max = ints[0]; }
	  for (int i=0;i<ints.length;i++) { samples++; sum+=ints[i];
	                                    if (min > ints[i]) min = ints[i];
	                                    if (max < ints[i]) max = ints[i]; } }

	/**
	 * Add values for the complex statistic calculation
	 *
	 *@param ints values to add
	 */
	public void addIntsComplex (int ints[]) {
	  sorted = false; for (int i=0;i<ints.length;i++) { sum+=ints[i]; samples++; list.add(ints[i]); } }
      }

      /**
       * Summation statistic
       */
      class SumStats    implements StatsInterface {
        SimpleStats stats = new SimpleStats();
	public void    addInts(String entity, int ints[]) { stats.addInts(entity,ints);   }
	public double  getStat(String entity)             { return stats.recs.get(entity).sum; } }
      /**
       * Average statistic
       */
      class AvgStats    implements StatsInterface {
        SimpleStats stats = new SimpleStats();
	public void    addInts(String entity, int ints[]) { stats.addInts(entity,ints);   }
	public double  getStat(String entity)             { return stats.recs.get(entity).sum / ((double) stats.recs.get(entity).samples); } }
      /**
       * Minimum statistic
       */
      class MinStats    implements StatsInterface {
        SimpleStats stats = new SimpleStats();
	public void    addInts(String entity, int ints[]) { stats.addInts(entity,ints);   }
	public double  getStat(String entity)             { return stats.recs.get(entity).min; } }
      /**
       * Maximum statistic
       */
      class MaxStats    implements StatsInterface {
        SimpleStats stats = new SimpleStats();
	public void    addInts(String entity, int ints[]) { stats.addInts(entity,ints);   }
	public double  getStat(String entity)             { return stats.recs.get(entity).max; } }
      /**
       * Standard deviation statistic
       */
      class StDevStats  implements StatsInterface {
        ArrayStats stats = new ArrayStats(); Map<String,Double> stat_lu = new HashMap<String,Double>();
	public void    addInts(String entity, int ints[]) { stats.addInts(entity,ints);   }
	public double  getStat(String entity) {
	  if (stat_lu.containsKey(entity) == false) {
            stat_lu.put(entity, Utils.calculateStandardDeviation(stats.recs.get(entity).list, stats.recs.get(entity).sum / ((double) stats.recs.get(entity).samples)));
	  }
	  return stat_lu.get(entity); } }
      /**
       * Median statistics
       */
      class MedianStats implements StatsInterface {
        ArrayStats stats = new ArrayStats();
	public void    addInts(String entity, int ints[]) { stats.addInts(entity,ints);   }
	public double  getStat(String entity) {
	  if (stats.recs.get(entity).sorted == false) { Collections.sort(stats.recs.get(entity).list); stats.recs.get(entity).sorted = true; } 
	                                                return stats.recs.get(entity).list.get(stats.recs.get(entity).list.size()/2); } }
      /**
       * Return the specific statistics from this instance.
       *
       *@param desc description of the statistic
       *
       *@return statistical interface to retrieve calculation
       */
      public StatsInterface createStats(String desc) {
        if      (desc.equals(MATH_SUM_STR))   return new SumStats();
	else if (desc.equals(MATH_AVG_STR))   return new AvgStats();
	else if (desc.equals(MATH_STD_STR))   return new StDevStats();
	else if (desc.equals(MATH_MIN_STR))   return new MinStats();
	else if (desc.equals(MATH_MAX_STR))   return new MaxStats();
	else if (desc.equals(MATH_MED_STR))   return new MedianStats();
	else                                  return new AvgStats();
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
	  rts2 = System.currentTimeMillis();
          base_bi = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) base_bi.getGraphics();
          g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	  RTColorManager.renderVisualizationBackground(base_bi, g2d);

          // Calculate the dimensions and draw the legend
	  String str; int txt_h = Utils.txtH(g2d, "0");
	  g2d.setColor(RTColorManager.getColor("axis", "major"));
	                                 g2d.drawLine(x_ins, y_ins,           x_ins,           y_ins + graph_h); 
	                                 g2d.drawLine(x_ins, y_ins + graph_h, x_ins + graph_w, y_ins + graph_h);
          g2d.setColor(xmap.getColor()); str = "" + xmap.getMin(); g2d.drawString(str, x_ins,                                 y_ins + graph_h + txt_h);
	                                 str = "" + xmap.getMax(); g2d.drawString(str, x_ins + graph_w - Utils.txtW(g2d,str), y_ins + graph_h + txt_h);
					 str = x_axis + " : " + x_math + " (" + x_scale + ")";
					 g2d.drawString(str, x_ins + graph_w/2 - Utils.txtW(g2d, str)/2, y_ins + graph_h + txt_h);
          g2d.setColor(ymap.getColor()); str = "" + ymap.getMin(); Utils.drawRotatedString(g2d, str, x_ins, y_ins + graph_h);
	                                 str = "" + ymap.getMax(); Utils.drawRotatedString(g2d, str, x_ins, y_ins + Utils.txtW(g2d,str));
					 str = y_axis + " : " + y_math + " (" + y_scale + ")";
					 Utils.drawRotatedString(g2d, str, x_ins, y_ins + graph_h/2 + Utils.txtW(g2d,str)/2);

          // Draw the entities
          Iterator<String> it = counter_context.binIterator();
	  while (it.hasNext()) {
	    String entity = it.next();
	    int sx = xmap.toScreen(xstat.getStat(entity)), 
	        sy = ymap.toScreen(ystat.getStat(entity));
	    if (vary_color) g2d.setColor(counter_context.binColor(entity)); else g2d.setColor(RTColorManager.getColor("data","default"));
	    if (vary_width) {
              double width = counter_context.totalNormalized(entity) * 16.0 + 1.8;
	      Ellipse2D ellipse = new Ellipse2D.Double(sx-width/2,sy-width/2,width,width);
	      g2d.fill(ellipse);
	      geom_to_skey.put(ellipse,entity); skey_to_geom.put(entity,ellipse);
	    } else          {
	      Rectangle2D rectangle = new Rectangle2D.Float(sx-1,sy-1,3,3);
	      g2d.fill(rectangle);
	      geom_to_skey.put(rectangle,entity); skey_to_geom.put(entity,rectangle);
	    }
	  }

	  // Draw the labels
	  if (draw_labels) {
            it = counter_context.binIterator();
	    while (it.hasNext()) {
	      String entity = it.next();
	      int sx = xmap.toScreen(xstat.getStat(entity)), 
	          sy = ymap.toScreen(ystat.getStat(entity));
	      Color color; if (vary_color) color = counter_context.binColor(entity); else color = RTColorManager.getColor("label", "major");
	      clearStr(g2d, entity, sx - Utils.txtW(g2d, entity)/2, sy + Utils.txtH(g2d, entity)/2, color, RTColorManager.getColor("label", "defaultbg"));
	    }
          }
         } finally { if (g2d != null) g2d.dispose(); }
	}
	return base_bi;
      }
    }
  }
}

