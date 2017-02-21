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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.BundlesDT;
import racetrack.framework.BundlesG;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;
import racetrack.util.Utils;
import racetrack.visualization.ColorScale;
import racetrack.visualization.RTColorManager;

/**
 * Panel for rendering an XY scatter plot of the individual bundles (records).
 *
 * Version 1.1 changed coloring to RT global color manager
 *
 *@author  D. Trimm
 *@version 1.1
 */
public class RTXYPanel extends RTPanel {
  /**
   * 
   */
  private static final long serialVersionUID = 3504478221768216901L;
  /**
   * Combobox for choosing x axis field
   */
  JComboBox            x_cb, 
  /**
   * Combobox for choosing y axis field (primary)
   */
                       y_cb, 
  /**
   * Combobox for choosing y axis sub field (alternate)
   */
		       y2_cb;
  /**
   * Checkbox to apply color to the scatterplot
   */
  JCheckBoxMenuItem    color_cbmi, 
  /**
   * Checkbox to include interactions that shows the closest point
   */
                       closest_points_cbmi, 
  /**
   * Checkbox to model duration in x-axis
   */
                       duration_cbmi,
  /**
   * Checkbox to render time markers
   */
		       timemarkers_cbmi,
  /**
   * Checkbox for stacked histograms along edges
   */
                       stacked_histograms_cbmi;
  /**
   * Radio button to vary the width of plots
   */
  JRadioButtonMenuItem width_vary_rbmi, 
  /**
   * Radio button to vary the width of the plots using a log scale
   */
                       width_vary_log_rbmi,
  /**
   * Radio button to fix the width of plots at small
   */
                       width_small_rbmi, 
  /**
   * Radio button to fix the width of plots to medium (default)
   */
		       width_medium_rbmi, 
  /**
   * Radio button to fix the width of plots to large
   */
		       width_large_rbmi,
  /**
   * Radio button to use a rectangular selection
   */
                       rectangle_selection_rbmi,
  /**
   * Radio button to use an elliptical selection
   */
                       ellipse_selection_rbmi;
  /**
   * Array or radio buttons for x scales
   */
  JRadioButtonMenuItem x_scales[], 
  /**
   * Array of radio buttons for y scales
   */
                       y_scales[], 
  /**
   * Array of radio buttons for the highlight shape
   */
		       highlight_shapes[];
  /**
   * Options for the highlight shapes
   */
  final static String  HL_CIRCULAR_STR     = "Circular",
                       HL_SQUARE_STR       = "Square",
                       HL_HORIZONTAL_STR   = "Horizontal",
                       HL_VERTICAL_STR     = "Vertical";
  /**
   * Array holding the highlight shapes
   */
  final static String  hl_shape_strs[] = { HL_CIRCULAR_STR, HL_SQUARE_STR, HL_HORIZONTAL_STR, HL_VERTICAL_STR };

  /**
   * Construct a default instance with the specified GUI parent.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt       application reference
   */
  public RTXYPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt) { 
    this(win_type, win_pos, win_uniq, rt, true,    true); 
  }

  /**
   * Construct an instance with the specified GUI parent and option x and y axis controls.  Useful
   * for when one XY panel is dependent on another to get an axis.  For example, two XY panels top-to-bottom
   * that will always share the x-axis variable.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt       application reference
   *@param x_ctrls  include the x-axis controls on the panel
   *@param y_ctrls  include the y-axis controls on the panel
   */
  public RTXYPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt, boolean x_ctrls, boolean y_ctrls) { 
    this(win_type, win_pos, win_uniq, rt, x_ctrls, y_ctrls, x_ctrls && y_ctrls); 
  }

  /**
   * Construct an instance with the specified GUI parent and option x and y axis controls.  Useful
   * for when one XY panel is dependent on another to get an axis.  For example, two XY panels top-to-bottom
   * that will always share the x-axis variable.
   *
   *@param win_type  type of window this panel is embedded into
   *@param win_pos   position of panel within window
   *@param win_uniq  UUID for parent window
   *@param rt        application reference
   *@param x_ctrls   include the x-axis controls on the panel
   *@param y_ctrls   include the y-axis controls on the panel
   *@param gen_ctrls determines if the popup-menu items should be included here
   */
  public RTXYPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt, boolean x_ctrls, boolean y_ctrls, boolean gen_ctrls) {
    super(win_type, win_pos, win_uniq, rt);   
    // Main component
    add("Center", component = new RTXYComponent());

    // Popup menu
    if (gen_ctrls) {
      ButtonGroup bg = new ButtonGroup();
      getRTPopupMenu().add(width_vary_rbmi          = new JRadioButtonMenuItem("Vary Width"));                 bg.add(width_vary_rbmi);
      getRTPopupMenu().add(width_vary_log_rbmi      = new JRadioButtonMenuItem("Vary Width (Log)"));           bg.add(width_vary_log_rbmi);
      getRTPopupMenu().add(width_small_rbmi         = new JRadioButtonMenuItem("Fixed Width (Small)"));        bg.add(width_small_rbmi);
      getRTPopupMenu().add(width_medium_rbmi        = new JRadioButtonMenuItem("Fixed Width (Medium)", true)); bg.add(width_medium_rbmi);
      getRTPopupMenu().add(width_large_rbmi         = new JRadioButtonMenuItem("Fixed Width (Large)"));        bg.add(width_large_rbmi);
      getRTPopupMenu().addSeparator();
      getRTPopupMenu().add(color_cbmi               = new JCheckBoxMenuItem("Vary Color"));
      getRTPopupMenu().addSeparator();

      bg = new ButtonGroup();
      getRTPopupMenu().add(rectangle_selection_rbmi = new JRadioButtonMenuItem("Rectangular Selection", true)); bg.add(rectangle_selection_rbmi);
      getRTPopupMenu().add(ellipse_selection_rbmi   = new JRadioButtonMenuItem("Elliptical Selection"));        bg.add(ellipse_selection_rbmi);

      getRTPopupMenu().addSeparator();
      getRTPopupMenu().add(closest_points_cbmi      = new JCheckBoxMenuItem("Closest Point Interaction", true));
      getRTPopupMenu().add(duration_cbmi            = new JCheckBoxMenuItem("Model Duration In X", true));
      getRTPopupMenu().add(timemarkers_cbmi         = new JCheckBoxMenuItem("Draw Time Markers", true));
      getRTPopupMenu().add(stacked_histograms_cbmi  = new JCheckBoxMenuItem("Stacked Histograms", true));
    }

    // Scales
    ButtonGroup bg;
    if (x_ctrls || y_ctrls) getRTPopupMenu().addSeparator();
    JMenu submenu; final String[] scale_strs = AxisMapper.allScales();
    if (x_ctrls) {
      submenu = new JMenu("X Scale"); bg = new ButtonGroup(); getRTPopupMenu().add(submenu);
      x_scales = new JRadioButtonMenuItem[scale_strs.length]; for (int i=0;i<x_scales.length;i++) { 
        x_scales[i] = new JRadioButtonMenuItem(scale_strs[i], i == 0); submenu.add(x_scales[i]); bg.add(x_scales[i]); defaultListener(x_scales[i]); }
    }
    if (y_ctrls) {
      submenu = new JMenu("Y Scale"); bg = new ButtonGroup(); getRTPopupMenu().add(submenu);
      y_scales = new JRadioButtonMenuItem[scale_strs.length]; for (int i=0;i<y_scales.length;i++) { 
        y_scales[i] = new JRadioButtonMenuItem(scale_strs[i], i == 0); submenu.add(y_scales[i]); bg.add(y_scales[i]); defaultListener(y_scales[i]); }
    }

    // Highlight shapes
    getRTPopupMenu().addSeparator();
    submenu = new JMenu("HL Shape"); bg = new ButtonGroup(); getRTPopupMenu().add(submenu);
    highlight_shapes = new JRadioButtonMenuItem[hl_shape_strs.length]; for (int i=0;i<highlight_shapes.length;i++) {
      highlight_shapes[i] = new JRadioButtonMenuItem(hl_shape_strs[i], i == 0); submenu.add(highlight_shapes[i]); bg.add(highlight_shapes[i]); defaultListener(highlight_shapes[i]); }

    // Removal options
    JMenuItem mi;
    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(mi = new JMenuItem("Remove 1 Item Rows"));    
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { ((RTXYComponent) getRTComponent()).removeSingleItemRows(); } } );
    getRTPopupMenu().add(mi = new JMenuItem("Keep Only 1 Item Rows"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { ((RTXYComponent) getRTComponent()).keepOnlySingleItemRows(); } } );
    getRTPopupMenu().add(mi = new JMenuItem("Remove 1 Item Cols"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { ((RTXYComponent) getRTComponent()).removeSingleItemCols(); } } );
    getRTPopupMenu().add(mi = new JMenuItem("Keep Only 1 Item Cols"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { ((RTXYComponent) getRTComponent()).keepOnlySingleItemCols(); } } );

    // Configuration panel
    JPanel panel = new JPanel(new FlowLayout());
    if (x_ctrls) { panel.add(new JLabel("X"));        panel.add(x_cb  = new JComboBox()); }
    if (y_ctrls) { panel.add(new JLabel("Y"));        panel.add(y_cb  = new JComboBox());
                   panel.add(new JLabel("Y2 (Opt)")); panel.add(y2_cb = new JComboBox()); }
    if (x_ctrls == false && y_ctrls == false) { String linked[] = { "Linked" }; panel.add(new JComboBox(linked)); }

    // Listeners
    if (x_ctrls) { defaultListener(x_cb); }
    if (y_ctrls) { defaultListener(y_cb);
                   defaultListener(y2_cb); }
    if (gen_ctrls) {
      defaultListener(width_vary_rbmi);
      defaultListener(width_vary_log_rbmi);
      defaultListener(width_small_rbmi);
      defaultListener(width_medium_rbmi);
      defaultListener(width_large_rbmi);
      defaultListener(color_cbmi);
      defaultListener(duration_cbmi);
      defaultListener(timemarkers_cbmi);
      defaultListener(stacked_histograms_cbmi);
    }

    // Fill the comboboxes
    updateBys();
    add("South", panel);
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "xyscatter"; }

  /**
   * Method to link the x-axis controls to another panel.
   *
   *@param master master control panel
   */
  public  void linkXControls(RTXYPanel master) {
    x_cb     = master.x_cb;  defaultListener(x_cb);
    x_scales = master.x_scales; for (int i=0;i<x_scales.length;i++) defaultListener(x_scales[i]);
  }

  /**
   * Method to link the y-axis controls to another panel.
   *
   *@param master master control panel
   */
  public  void linkYControls(RTXYPanel master) {
    y_cb     = master.y_cb;  defaultListener(y_cb);
    y2_cb    = master.y2_cb;
    y_scales = master.y_scales; for (int i=0;i<y_scales.length;i++) defaultListener(y_scales[i]);
  }

  /**
   * Method to link the popup-menu controls to another panel.
   *
   *@param master master control panel
   */
  public void linkGeneric(RTXYPanel master) {
    width_vary_rbmi          = master.width_vary_rbmi;
    width_vary_log_rbmi      = master.width_vary_log_rbmi;
    width_small_rbmi         = master.width_small_rbmi;
    width_medium_rbmi        = master.width_medium_rbmi;
    width_large_rbmi         = master.width_large_rbmi;
    color_cbmi               = master.color_cbmi;
    closest_points_cbmi      = master.closest_points_cbmi;
    duration_cbmi            = master.duration_cbmi;
    timemarkers_cbmi         = master.timemarkers_cbmi;
    stacked_histograms_cbmi  = master.stacked_histograms_cbmi;
    rectangle_selection_rbmi = master.rectangle_selection_rbmi;

    defaultListener(width_vary_rbmi);
    defaultListener(width_vary_log_rbmi);
    defaultListener(width_small_rbmi);
    defaultListener(width_medium_rbmi);
    defaultListener(width_large_rbmi);
    defaultListener(color_cbmi);
    defaultListener(closest_points_cbmi);
    defaultListener(duration_cbmi);
    defaultListener(timemarkers_cbmi);
    defaultListener(stacked_histograms_cbmi);
  }

  /**
   * Return the x-axis field.
   *
   *@return application field for x-axis
   */
  public String       xAxis         ()           { if (x_cb == null) return null; else return (String) x_cb.getSelectedItem(); }

  /**
   * Set the x-axis field
   *
   *@param str application field for x-axis
   */
  public void         xAxis         (String str) { x_cb.setSelectedItem(str); }

  /**
   * Return the x-scale option.
   *
   *@return string indicating scale
   */
  public String       xScale        ()           { return selectedItem(x_scales); }

  /**
   * Set the x-axis scaling option.
   *
   *@param str see the string constants for scale options
   */
  public void         xScale        (String str) { setSelectedItem(x_scales, str); }

  /**
   * Return the primary y-axis field.
   *
   *@return application field for primary y-axis
   */
  public String       yAxis         ()           { if (y_cb == null) return null; else return (String) y_cb.getSelectedItem(); }

  /**
   * Set the primary y-axis field
   *
   *@param str application field for primary y-axis
   */
  public void         yAxis         (String str) { y_cb.setSelectedItem(str); }

  /**
   * Return the secondary/alternative y-axis field.
   *
   *@return application field for secondary y-axis
   */
  public String       y2Axis        ()           { if (y2_cb == null) return null; else return (String) y2_cb.getSelectedItem(); }

  /**
   * Set the secondary/alternative y-axis field.  This options enables the rendering
   * to combine multiple fields into the y-axis.  For example, srcip:dstip which provides
   * a timing graph for each pair of ip addresses.
   *
   *@param str application field for secondary y-axis
   */
  public void         y2Axis        (String str) { y2_cb.setSelectedItem(str); }

  /**
   * Return the y-scale option.
   *
   *@return string indicating scale
   */
  public String       yScale        ()           { return selectedItem(y_scales); }

  /**
   * Set the y-axis scaling option.
   *
   *@param str see the string constants for scale options
   */
  public void         yScale        (String str) { setSelectedItem(y_scales, str); }

  /**
   * Return the flag to vary color in the rendering.
   *
   *@return true to vary color, false to use a constant gray
   */
  public boolean      varyColor     ()           { return color_cbmi.isSelected(); }

  /**
   * Set the flag to vary color in the rendering.
   *
   *@param f true to vary color, false to use a constant gray color
   */
  public void         varyColor     (boolean f)  { color_cbmi.setSelected(f); }

  /**
   * Return the shape for brushing / highlighting the visualization.
   *
   *@return string indicating shape
   */
  public String       highlightShape()           { return selectedItem(highlight_shapes); }

  /** 
   * Set the shape used for brushing or highlighting the rendered shapes.
   *
   *@param str see the string constans for highlight or brushed shapes
   */
  public void         highlightShape(String str) { setSelectedItem(highlight_shapes, str); }

  /**
   * Return the "draw point interactions" setting.
   *
   *@return true for interactive 
   */
  public boolean      interactiveClosestPoint()  { return closest_points_cbmi.isSelected(); }

  /**
   * Set the flag for interactive closest point painting.  This feature enables the view
   * to determine the two closest points on both axes.
   *
   *@param f true to interact
   */
  public void         interactiveClosestPoint(boolean f) { closest_points_cbmi.setSelected(f); }

  /**
   * Return true if the duration should be modeled in the x-axis.
   *
   *@return true to model duration
   */
  public boolean      modelDuration() { return duration_cbmi.isSelected(); }

  /**
   * Set the flag to determine if duration should be modeled in the x-axis.
   *
   *@param f true to model duration
   */
  public void         modelDuration(boolean f) { duration_cbmi.setSelected(f); }

  /**
   * Increment the chosen scale.
   *
   *@param axis either 'x' or 'y'
   *@param inc  amount to increment, may be negative
   */
  public void         nextScale(char axis, int inc) {
    JRadioButtonMenuItem items[] = (axis == 'x') ? x_scales : y_scales;
    int sel = -1; for (int i=0;i<items.length;i++) if (items[i].isSelected()) sel = i;
    if (sel != -1) {
      sel += inc;
      while (sel < 0) sel += items.length;
      sel = sel % items.length;
      items[sel].setSelected(true);
    }
  }

  /**
   * Return the "draw time markers" setting.
   *
   *@return true if markers are to be drawn
   */
  public boolean      drawTimeMarkers()          { return timemarkers_cbmi.isSelected(); }
  
  /**
   * Set the flag to render time markers.
   *
   *@param f true to render time markers
   */
  public void         drawTimeMarkers(boolean f) { timemarkers_cbmi.setSelected(f); }

  /**
   * Return true if the renderer should draw stacked histograms.
   *
   *@return true to draw stacked histograms
   */
  public boolean      drawStackedHistograms() { return stacked_histograms_cbmi.isSelected(); }

  /**
   * Set the flag to render the stacked histograms.
   *
   *@param f true to render stacked histograms
   */
  public void         drawStackedHistograms(boolean f) { stacked_histograms_cbmi.setSelected(f); }

  /**
   * Return the point width for the rendering.
   *
   *@return point width
   */
  public PTWIDTH      pointWidth    ()           { if      (width_vary_rbmi.isSelected())     return PTWIDTH.VARY;
                                                   else if (width_vary_log_rbmi.isSelected()) return PTWIDTH.VARY_LOG;
                                                   else if (width_small_rbmi.isSelected())    return PTWIDTH.SMALL;
						   else if (width_medium_rbmi.isSelected())   return PTWIDTH.MEDIUM;
						   else                                       return PTWIDTH.LARGE; }
  
  /**
   * Set the point width for the rendering.
   *
   *@return point width
   */
  public void         pointWidth    (PTWIDTH ptwid) { switch (ptwid) {
                                                       case VARY:     width_vary_rbmi.setSelected(true);     break;
						       case VARY_LOG: width_vary_log_rbmi.setSelected(true); break;
						       case SMALL:    width_small_rbmi.setSelected(true);    break;
						       case MEDIUM:   width_medium_rbmi.setSelected(true);   break;
						       case LARGE:    width_large_rbmi.setSelected(true);    break;
						     } }

  /**
   * Set the point width for the rendering.
   *
   *@param ptwid new point width as a string
   */
  public void         pointWidth    (String  ptwid) { if      (ptwid.equals(""+PTWIDTH.VARY))     pointWidth(PTWIDTH.VARY);
                                                      else if (ptwid.equals(""+PTWIDTH.VARY_LOG)) pointWidth(PTWIDTH.VARY_LOG);
                                                      else if (ptwid.equals(""+PTWIDTH.SMALL))    pointWidth(PTWIDTH.SMALL);
						      else if (ptwid.equals(""+PTWIDTH.MEDIUM))   pointWidth(PTWIDTH.MEDIUM);
						      else if (ptwid.equals(""+PTWIDTH.LARGE))    pointWidth(PTWIDTH.LARGE);
                                                    }

  /**
   * Enumeration for point width (plot width)
   */
  enum PTWIDTH { VARY, VARY_LOG, SMALL, MEDIUM, LARGE };

  /**
   * Return a configuration string that encapsulates the panels configuration.  Originally intended
   * to support bookmarking and shared views.
   *
   *@return string representing panel configuration
   */
  @Override
  public String  getConfig()  { return "RTXYPanel"                                         + BundlesDT.DELIM +
                                       "xaxis="     + Utils.encToURL(xAxis())              + BundlesDT.DELIM +
                                       "xscale="    + Utils.encToURL(xScale())             + BundlesDT.DELIM +
                                       "yaxis="     + Utils.encToURL(yAxis())              + BundlesDT.DELIM +
				       "y2axis="    + Utils.encToURL(y2Axis())             + BundlesDT.DELIM +
                                       "yscale="    + Utils.encToURL(yScale())             + BundlesDT.DELIM +
                                       "width="     + pointWidth()                         + BundlesDT.DELIM +
                                       "vcolor="    + (varyColor()     ? "true" : "false") + BundlesDT.DELIM +
				       "duration="  + (modelDuration() ? "true" : "false") + BundlesDT.DELIM +
                                       "hlshape="   + Utils.encToURL(highlightShape())     + BundlesDT.DELIM +
				       "drawtm="    + drawTimeMarkers()                    + BundlesDT.DELIM +
				       "drawsh="    + drawStackedHistograms(); }

  /**
   * Set the configuration of the panel based on a previously return configuration string.  Originally intended
   * to support bookmarking and shared views.
   *
   *@param str configuration string
   */
  @Override
  public void    setConfig(String str) {
    StringTokenizer st = new StringTokenizer(str,BundlesDT.DELIM);
    if (st.nextToken().equals("RTXYPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not An RTXYPanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st2.hasMoreTokens() ? st2.nextToken() : "";
      if      (type.equals("xaxis"))    xAxis(Utils.decFmURL(value));
      else if (type.equals("xscale"))   xScale(Utils.decFmURL(value));
      else if (type.equals("yaxis"))    yAxis(Utils.decFmURL(value));
      else if (type.equals("y2axis"))   y2Axis(Utils.decFmURL(value));
      else if (type.equals("yscale"))   yScale(Utils.decFmURL(value));
      else if (type.equals("width"))    pointWidth(value);
      else if (type.equals("vcolor"))   varyColor(value.toLowerCase().equals("true"));
      else if (type.equals("duration")) modelDuration(value.toLowerCase().equals("true"));
      else if (type.equals("hlshape"))  highlightShape(Utils.decFmURL(value));
      else if (type.equals("drawtm"))   drawTimeMarkers(value.toLowerCase().equals("true"));
      else if (type.equals("drawsh"))   drawStackedHistograms(value.toLowerCase().equals("true"));
      else throw new RuntimeException("Do Not Understand Type Value \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * Update the x-axis, y-axis, and y2-axis dropdown box with the latest application fields.
   */
  public void         updateBys() { updateBys(x_cb); updateBys(y_cb); updateBys(y2_cb); }

  /**
   * Update a specific combobox with the latest application fields.
   *
   *@param cb combobox to update
   */
  public void         updateBys(JComboBox cb) {
    if (cb == null) return;
    BundlesG globals = getRTParent().getRootBundles().getGlobals();
    
    Object sel = cb.getSelectedItem();

    // Start fresh
    cb.removeAllItems();
    
    // Add the none option for the secondary axis choices
    if (cb == y2_cb) cb.addItem(BundlesDT.COUNT_BY_NONE);

    // Add the default blanks
    String strs[];
    if (cb == x_cb || cb == y_cb) strs = KeyMaker.blanks(globals,false,true,true,true);
    else                          strs = KeyMaker.blanks(globals,false,true,true,false);
    for (int i=0;i<strs.length;i++) cb.addItem(strs[i]);

    if (sel == null) cb.setSelectedIndex(0); else cb.setSelectedItem(sel);
  }

  /**
   * Component to handle the painting and interaction of the xy scatter plot.
   */
  public class RTXYComponent extends RTComponent implements MouseListener, MouseMotionListener, KeyListener {
    /**
     * 
     */
    private static final long serialVersionUID = -4161559801280539845L;

    /**
     * Beginning of drag in world coordinates
     */
    long drag_time_x0, 
    /**
     * Beginning of drag in screen coordinates
     */
         drag_time_sx0, 
    /**
     * End of drag in world coordinates
     */
	 drag_time_x1, 
    /**
     * End of drag in screen coordinates
     */
	 drag_time_sx1; 
    /**
     * Flag to indicate that a time drag is in effect (time drags are used to mark time regions)
     */
    boolean time_drag = false;

    /**
     * Return the drag shape - overridden so that it can be an {@link Ellipse2D} or {@link Rectangle2D} object.
     *
     *@return shape of the mouse drag operation
     */
    @Override
    protected Shape dragShape() {
      if (rectangle_selection_rbmi.isSelected()) {
        int w = (int) Math.abs(mx0 - mx1), h = (int) Math.abs(my0 - my1); if (w == 0) w = 1; if (h == 0) h = 1;
        return new Rectangle2D.Double(mx0 < mx1 ? mx0 : mx1, my0 < my1 ? my0 : my1, w, h);
      } else {
        double dy = Math.abs(my1 - my0), dx = Math.abs(mx1 - mx0); if (dy < 1.0) dy = 1.0; if (dx < 1.0) dx = 1.0;
        return new Ellipse2D.Double(mx0 - dx, my0 - dy, dx*2, dy*2);
      }
    }

    /**
     * Handled by super class.
     */
    @Override
    public void mouseEntered  (MouseEvent me) { super.mouseEntered(me);  }

    /**
     * Handled by super class.
     */
    @Override
    public void mouseExited   (MouseEvent me) { super.mouseExited(me);   }

    /**
     * Handle mouse pressed events for the middle button.  These are events related to marking up timelines.
     *
     *@param me mouse event
     */
    @Override
    public void mousePressed  (MouseEvent me) { super.mousePressed(me);  
      RenderContext myrc = (RenderContext) getRTComponent().rc;
      if (myrc != null && myrc.xtime != null && myrc.xtime.linearTime() && me.getButton() == MouseEvent.BUTTON2 && myrc.x_scale.equals(AxisMapper.LINEAR_SCALE_STR)) {
        if (me.getX() >= myrc.x_lft && me.getX() <= myrc.x_lft+myrc.graph_w) {
          drag_time_x0  = drag_time_x1  = findClosestXApp(myrc, me.getX()); 
	  drag_time_sx0 = drag_time_sx1 = me.getX();
	  time_drag = true;
        }
      }
    }

    /**
     * Handle mouse released events for the middle button.  These are events related to marking up timelines.
     *
     *@param me mouse event
     */
    public void mouseReleased (MouseEvent me) { super.mouseReleased(me); 
      RenderContext myrc = (RenderContext) getRTComponent().rc;
      if (myrc != null && myrc.xtime != null && myrc.xtime.linearTime() && me.getButton() == MouseEvent.BUTTON2 && myrc.x_scale.equals(AxisMapper.LINEAR_SCALE_STR)) {
        if (time_drag) {
          drag_time_x1 = findClosestXApp(myrc, me.getX()); time_drag = false;
	  if (drag_time_x0 != drag_time_x1) {
	    if (drag_time_x0 > drag_time_x1) { long swap = drag_time_x0; drag_time_x0 = drag_time_x1; drag_time_x1 = swap; }
	    getRTParent().addTimeMarker(Utils.shortDateStr(drag_time_x0), drag_time_x0, drag_time_x1);
	  }
        }
      }
    }

    /**
     * Handle mouse clicked events for the middle button.  These are events related to marking up timelines.
     *
     *@param me mouse event
     */
    @Override
    public void mouseClicked  (MouseEvent me) { super.mouseClicked(me);
      RenderContext myrc = (RenderContext) getRTComponent().rc;
      if (myrc != null && myrc.xtime != null && myrc.xtime.linearTime() && me.getButton() == MouseEvent.BUTTON2 && myrc.x_scale.equals(AxisMapper.LINEAR_SCALE_STR)) {
        if (me.getX() >= myrc.x_lft && me.getX() <= myrc.x_lft+myrc.graph_w) {
          long closest_x_app = findClosestXApp(myrc, me.getX());
	  getRTParent().addTimeMarker(Utils.shortDateStr(closest_x_app), closest_x_app);
        }
      }
    }

    /**
     * Handled by the super-class
     */
    @Override
    public void mouseMoved(MouseEvent me)     { super.mouseMoved(me); }

    /**
     * Handle mouse dragged events for the middle button.  These are events related to marking up timelines.
     *
     *@param me mouse event
     */
    @Override
    public void mouseDragged(MouseEvent me)   { super.mouseDragged(me); 
      RenderContext myrc = (RenderContext) getRTComponent().rc;
      if (time_drag && myrc != null) {
        drag_time_x1  = findClosestXApp(myrc, me.getX()); 
	drag_time_sx1 = me.getX();
	repaint();
      } else if (time_drag) { time_drag = false; repaint(); }
    }

    /**
     * Key listener type event.
     *
     *@param ke key event
     */
    @Override
    public void keyTyped(KeyEvent ke) { 
      super.keyTyped(ke); char c = ke.getKeyChar();
      if        (c == 'x') { nextScale('x',  1);
      } else if (c == 'X') { nextScale('x', -1);
      } else if (c == 'y') { nextScale('y',  1);
      } else if (c == 'Y') { nextScale('y', -1);
      } else if (c == 'd' || c == 'D') { modelDuration(!modelDuration()); }
    }

    /**
     * Key listener press event.
     *
     *@param ke key event
     */
    @Override
    public void keyPressed(KeyEvent ke) { super.keyPressed(ke); }

    /**
     * Key listener release event.
     *
     *@param ke key event
     */
    @Override
    public void keyReleased(KeyEvent ke) { super.keyReleased(ke); }

    /**
     * Find the closest x application value to the specified screen position.  Only really used
     * for time markers in the x-axis.
     *
     *@param myrc render context containing information about current rendering
     *@param sx   screen x coordinate
     *
     *@return long value associated with the screen location (closest to x coordinate)
     */
    private long findClosestXApp(RenderContext myrc, int sx) {
      int dist = 0;
      // Linear search :(
      while (myrc.sx_to_xs.containsKey(sx + dist) == false &&
             myrc.sx_to_xs.containsKey(sx - dist) == false &&
	     dist < myrc.graph_w) { dist++; }
      // Return the value - prefer greater thans...
      if        (myrc.sx_to_xs.containsKey(sx+dist)) {
        Iterator<Long> it = myrc.sx_to_xs.get(sx+dist).iterator(); long smallest = it.next();
	while (it.hasNext()) { long l = it.next(); if (l < smallest) smallest = l; }
	return smallest;
      } else if (myrc.sx_to_xs.containsKey(sx-dist)) {
        Iterator<Long> it = myrc.sx_to_xs.get(sx-dist).iterator(); long biggest  = it.next();
	while (it.hasNext()) { long l = it.next(); if (l > biggest) biggest = l; }
	return biggest;
      } else return 0L;
    }

    /**
     * Copy the rendered image to the clipboard.  Not sure it works correctly across platforms.
     *
     *@param shft shift key down
     *@param ctrl control key down
     */
    @Override
    public void copyToClipboard    (boolean shft, boolean alt) {
      RenderContext myrc = (RenderContext) getRTComponent().rc;
      if (shft == true && myrc != null)  Utils.copyToClipboard(myrc.getBase());
    }

    /**
     * Draw the closest point interactions.  For this component, it identifies the closest
     * scatter plot points on both the x and y axis and draws lines and labels for those
     * points.
     *
     *@param g2d graphics drawing primitive
     *@param mx  mouse x position
     *@param my  mouse y position
     */
    @Override
    public void addGarnish(Graphics2D g2d, int mx, int my) {
      RenderContext myrc = (RenderContext) rc; int txt_h = Utils.txtH(g2d,"0");
      Stroke stroke = g2d.getStroke(); Composite composite = g2d.getComposite(); String more = "";
      // Check to see if we're doing a time drag
      if (myrc != null && time_drag) {
        g2d.setColor(RTColorManager.getColor("annotate", "region"));
	g2d.setStroke(new BasicStroke(0.5f)); 
	Rectangle2D rect = new Rectangle2D.Double(drag_time_sx0 < drag_time_sx1 ? drag_time_sx0 : drag_time_sx1,
	                                          myrc.y_top, Math.abs(drag_time_sx1 - drag_time_sx0), myrc.graph_h);
        g2d.draw(rect);
	g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
	g2d.fill(rect);
      }
      // Check to make sure the interaction is enabled and that we have a valid render context
      if (interactiveClosestPoint() == false) return;
      if (myrc != null && mx >= myrc.x_lft && mx <= (myrc.x_lft + myrc.graph_w) && my >= myrc.y_top && my <= (myrc.y_top + myrc.graph_h)) {
        int x_min = mx, x_max = mx, y_min = my, y_max = my;
	g2d.setStroke(new BasicStroke(0.5f)); g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));

        while (myrc.sx_to_xs.containsKey(x_min) == false && x_min >= 0)                 x_min--;
        while (myrc.sx_to_xs.containsKey(x_max) == false && x_max <  myrc.getRCWidth()) x_max++;

        while (myrc.sy_to_ys.containsKey(y_min) == false && y_min >= 0)                  y_min--;
        while (myrc.sy_to_ys.containsKey(y_max) == false && y_max <  myrc.getRCHeight()) y_max++;

        // Do a closest point search in +/-3 grid from the mouse (has to be the closest point
        int x_data = -1, y_data = -1; int d_data = Integer.MAX_VALUE;
        for (int dy=-3;dy<=3;dy++) for (int dx=-3;dx<=3;dx++) {
	  int x = mx + dx, y = my + dy;
	  String skey = x + "," + y; if (myrc.skey_to_geom.containsKey(skey)) {
            int d = dx*dx + dy*dy; if (d < d_data) { x_data = x; y_data = y; d_data = d;
	    }
	  }
        }

	// If we are close enough to a single point, just draw the stats about that point
        if (d_data < Integer.MAX_VALUE) {
          xGuide(g2d,myrc,x_data,x_data);
	  yGuide(g2d,myrc,y_data,y_data);
	// Otherwise, draw the stats about the closest in both the x and y directions
        } else {
	  // Handle the Xs -- if it's time-based, draw the duration/interval
	  long   ts0 = 0L, ts1 = 0L;
	  if (myrc.sx_to_xs.containsKey(x_min)) ts0 = xGuide(g2d,myrc,x_min,x_min);
	  if (myrc.sx_to_xs.containsKey(x_max)) ts1 = xGuide(g2d,myrc,x_max,x_max+txt_h);

	  // Handle time-specific issues in the x axis
	  boolean multi_values = (myrc.sx_to_xs.containsKey(x_min) && myrc.sx_to_xs.get(x_min).size() > 1) ||
	                         (myrc.sx_to_xs.containsKey(x_max) && myrc.sx_to_xs.get(x_max).size() > 1);
	  if (myrc.xtime != null && myrc.xtime.linearTime() && ts0 != 0L && ts1 != 0L && x_min != x_max) {
	    String str = Utils.humanReadableDuration(ts1 - ts0);
	    if (multi_values) str += " (Approx)";
	    clearStr(g2d, str, getMouseX(), getMouseY(), RTColorManager.getColor("annotate", "labelfg"), RTColorManager.getColor("annotate", "labelbg"), true);
	  }

	  // Handles the Ys -- complicated by the secondary axis...
	  if (myrc.sy_to_ys.containsKey(y_min)) yGuide(g2d,myrc,y_min,y_min);
	  if (myrc.sy_to_ys.containsKey(y_max)) yGuide(g2d,myrc,y_max,y_max+txt_h);
        }

	// Reset the GUI handlers
	g2d.setStroke(stroke); g2d.setComposite(composite);
      }
    }

    /**
     * Draw the x guideline
     *
     *@return associated timestamp value if the x axis is configured for time, 0 otherwise
     */
    private long xGuide(Graphics2D g2d, RenderContext myrc, int x, int x_pos) {
      g2d.setColor(RTColorManager.getColor("annotate", "cursor"));  String more = "", str = ""; if (myrc.sx_to_xs.get(x).size() > 1) { more = " ... "; }
      g2d.drawLine(x, myrc.y_top, x, myrc.y_top+myrc.graph_h); long l = myrc.sx_to_xs.get(x).iterator().next();
      if (myrc.xkms.size() > 0) str = myrc.find(myrc.xkms, (int) l); else str = myrc.xtime.toString(l);
      Utils.drawRotatedString(g2d, str + more, x_pos, myrc.y_top + myrc.graph_h, RTColorManager.getColor("annotate", "labelfg"), RTColorManager.getColor("annotate", "labelbg"));
      return l;
    }

    /**
     * Draw the y guideline
     */
    private void yGuide(Graphics2D g2d, RenderContext myrc, int y, int y_pos) {
      g2d.setColor(RTColorManager.getColor("annotate", "cursor"));  String more = "", str = ""; if (myrc.sy_to_ys.get(y).size() > 1) more = " ... ";
      g2d.drawLine(myrc.x_lft, y, myrc.x_lft + myrc.graph_w, y); long l = myrc.sy_to_ys.get(y).iterator().next();
      if (myrc.y2_axis != null) {
        if      (myrc.ykms.size() > 0 && myrc.y2kms.size() > 0)  str = myrc.find(myrc.ykms,  (int) (0x00ffffffff & (l>>32))) + 
	                                                               " " + BundlesDT.DELIM + " " +
	                                                               myrc.find(myrc.y2kms, (int) (0x00ffffffff & (l)));
        else if (myrc.ykms.size() > 0)                           str = myrc.find(myrc.ykms,  (int) (0x00ffffffff & (l>>32))); 
        else                                                     str = myrc.ytime.toString(l);
      } else                    {
	if (myrc.ykms.size() > 0)                                str = myrc.find(myrc.ykms,  (int) l); 
	else                                                     str = myrc.ytime.toString(l);
      }
      clearStr(g2d, str + more, myrc.x_lft, y_pos,  RTColorManager.getColor("annotate", "labelfg"), RTColorManager.getColor("annotate", "labelbg"));
    }

    /**
     * Return all shapes in the current view.
     *
     *@return all shapes
     */
    @Override
    public Set<Shape>      allShapes()                     { 
      RenderContext myrc = (RenderContext) rc;
      if (myrc != null) return myrc.geom_to_skey.keySet(); else return new HashSet<Shape>();
    }

    /**
     * Return the shapes related to the specified bundles.
     *
     *@param bundles bundles to correlate against rendered shapes
     *
     *@return set of shapes associated with the bundles
     */
    @Override
    public Set<Shape>  shapes(Set<Bundle> bundles) { 
      Set<Shape> shapes = new HashSet<Shape>();
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return shapes;

      Iterator<Bundle> it = bundles.iterator();
      while (it.hasNext()) {
        Bundle bundle = it.next();
        if (myrc.bundle_to_skeys.containsKey(bundle)) {
          Iterator<String> it_skey = myrc.bundle_to_skeys.get(bundle).iterator();
	  while (it_skey.hasNext()) {
            String skey = it_skey.next();
            shapes.add(myrc.skey_to_geom.get(skey));
	  }
        }
      }
      return shapes;
    }

    /**
     * Return the bundles associated with the specified shape.  The shape cannot be
     * generic -- i.e., it must have been created during the rendering process.
     *
     *@param shape shape to re-map to bundles (must be shape created by this render context)
     *
     *@return bundles associated with the shape
     */
    @Override
    public Set<Bundle> shapeBundles(Shape shape)       {
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return new HashSet<Bundle>();
      return myrc.screen_counter_context.getBundles(myrc.geom_to_skey.get(shape));
    }

    /**
     * Return the shapes that overlap with the specified shape.  The specified shape can
     * be a general shape object created elsewhere.
     *
     *@param shape generic shape to check for overlap
     *
     *@return set of rendered shapes that overlap
     */
    @Override
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
     * Return the shape associated with the 0th order highlights (directly under the mouse).
     *
     *@param x mouse x coordinate
     *@param y mouse y coordinate
     *
     *@return shape under the mouse
     */
    @Override
    public Shape getZeroOrderShape(int x, int y) {
      String str = highlightShape();
      if        (str.equals(HL_CIRCULAR_STR))   { return new Ellipse2D.Double(x-2,y-2,5,5);
      } else if (str.equals(HL_SQUARE_STR))     { return new Rectangle2D.Double(x-1,y-1,3,3);
      } else if (str.equals(HL_HORIZONTAL_STR)) { return new Rectangle2D.Double(x-getWidth()*2,y-1,getWidth()*4,3);
      } else if (str.equals(HL_VERTICAL_STR))   { return new Rectangle2D.Double(x-1,y-getHeight()*2,3,getHeight()*4);
      } else                                      return new Rectangle2D.Double(x,y,1,1);
    }

    /**
     * Return the shape associated with the 1st order highlights (near the mouse)
     *
     *@param x mouse x coordinate
     *@param y mouse y coordinate
     *
     *@return shape near the mouse
     */
    @Override
    public Shape getFirstOrderShape(int x, int y) {
      String str = highlightShape();
      if        (str.equals(HL_CIRCULAR_STR))   { return new Ellipse2D.Double(x-5,y-5,11,11);
      } else if (str.equals(HL_SQUARE_STR))     { return new Rectangle2D.Double(x-5,y-5,11,11);
      } else if (str.equals(HL_HORIZONTAL_STR)) { return new Rectangle2D.Double(x-getWidth()*2,y-3,getWidth()*4,7);
      } else if (str.equals(HL_VERTICAL_STR))   { return new Rectangle2D.Double(x-3,y-getHeight()*2,7,getHeight()*4);
      } else                                      return new Rectangle2D.Double(x,y,1,1);
    }

    /**
     * Return the shape associated with the 2nd order highlights (a little further from the mouse)
     *
     *@param x mouse x coordinate
     *@param y mouse y coordinate
     *
     *@return shape further from the mouse
     */
    @Override
    public Shape getSecondOrderShape(int x, int y) {
      String str = highlightShape();
      if        (str.equals(HL_CIRCULAR_STR))   { return new Ellipse2D.Double(x-10,y-10,21,21);
      } else if (str.equals(HL_SQUARE_STR))     { return new Rectangle2D.Double(x-10,y-10,21,21);
      } else if (str.equals(HL_HORIZONTAL_STR)) { return new Rectangle2D.Double(x-getWidth()*2,y-5,getWidth()*4,11);
      } else if (str.equals(HL_VERTICAL_STR))   { return new Rectangle2D.Double(x-5,y-getHeight()*2,11,getHeight()*4);
      } else                                      return new Rectangle2D.Double(x,y,1,1);
    }

    /**
     * Remove rows that just have a single item.
     */
    public void removeSingleItemRows()   { keepRowsGeneric(false); }

    /**
     * Keep only rows that have a single item.
     */
    public void keepOnlySingleItemRows() { keepRowsGeneric(true);  }

    /**
     * Protected method to keep/remove single item rows.
     *
     *@param keep determines if we are keeping or removing single item rows
     */
    protected void keepRowsGeneric(boolean keep) {
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return;
      Set<Bundle> set = new HashSet<Bundle>(); set.addAll(no_mapping_set);
      Iterator<Long> it = myrc.y_to_bs.keySet().iterator();
      while (it.hasNext()) {
        long l = it.next();
	if      (myrc.y_to_bs.get(l).size() == 1 && keep == true)  set.addAll(myrc.y_to_bs.get(l));
	else if (myrc.y_to_bs.get(l).size() != 1 && keep == false) set.addAll(myrc.y_to_bs.get(l));
      }
      getRTParent().push(myrc.bs.subset(set));
    }
    
    /**
     * Remove columns that just have a single item.
     */
    public void removeSingleItemCols()   { keepColsGeneric(false); }

    /**
     * Keep columns that just have a single item.
     */
    public void keepOnlySingleItemCols() { keepColsGeneric(true);  }

    /**
     * Protected method to keep/remove single item columns.
     *
     *@param keep determines if we are keeping or removing single item rows
     */
    protected void keepColsGeneric(boolean keep) {
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return;
      Set<Bundle> set = new HashSet<Bundle>(); set.addAll(no_mapping_set);
      Iterator<Long> it = myrc.x_to_bs.keySet().iterator();
      while (it.hasNext()) {
        long l = it.next();
	if      (myrc.x_to_bs.get(l).size() == 1 && keep == true)  set.addAll(myrc.x_to_bs.get(l));
	else if (myrc.x_to_bs.get(l).size() != 1 && keep == false) set.addAll(myrc.x_to_bs.get(l));
      }
      getRTParent().push(myrc.bs.subset(set));
    }

    /**
     * Render the current view based on the existing settings.  A render id is included so that unnecessary renderings
     * (those that have been overcome by newer requests) can abort.
     *
     *@param id render id to abort unnecessary renderings
     */
    @Override
    public RTRenderContext render(short id) {
      clearNoMappingSet();
      Bundles bs       = getRenderBundles();
      String  count_by = getRTParent().getCountBy(),
              color_by = getRTParent().getColorBy();
      String  x_axis   = xAxis(),
              y_axis   = yAxis(),
	      y2_axis  = y2Axis();
      if (x_axis  == null) return null;
      if (y2_axis != null && y2_axis.equals(BundlesDT.COUNT_BY_NONE)) y2_axis = null;
      if (bs != null && count_by != null && x_axis != null && y_axis != null) {
        RenderContext myrc = new RenderContext(id, bs, count_by, color_by, x_axis, xScale(), y_axis, y2_axis, yScale(), pointWidth(), varyColor(), modelDuration(), drawTimeMarkers(), drawStackedHistograms(), getWidth(), getHeight());
        return myrc;
      } else return null;
    }

    /**
     * Class that renders the visualization based on the panels current settings.  The render context
     * contains all of the necessary mappings to translate application records (bundles) to their
     * corresponding rendered shapes and vice versa.
     */
    public class RenderContext extends RTRenderContext {
      /**
       * Dataset to render
       */
      Bundles bs; 
      /**
       * Width of rendering in pixels
       */
      int     w, 
      /**
       * Height of rendering in pixels
       */
              h; 
      /**
       * Counting each data element for adjusting the width of the plotted points
       */
      String  count_by, 
      /**
       * Coloring each data element
       */
              color_by, 
      /**
       * X-axis field 
       */
	      x_axis, 
      /**
       * X-axis scale
       */
	      x_scale, 
      /**
       * Primary Y-axis field
       */
	      y_axis, 
      /**
       * Secondary Y-axis field
       */
	      y2_axis, 
      /**
       * Y-axis scale
       */
	      y_scale; 
      /**
       * Flag to indicate coloring is in effect
       */
      boolean vary_color, 
      /**
       * Flag to indicate that time markers should be drawn
       */
              draw_tms,
      /**
       * Flag to indicate to draw the stacked histograms
       */
              draw_sh,
      /**
       * Flag to indicate that duration should be modeled
       */
              duration = true;
      /**
       * Point width for the scatter plots
       */
      PTWIDTH ptwidth;
      /**
       * Counter context for accumulating the counter values for each xy pairing
       */
      BundlesCounterContext counter_context;
      /**
       * Sets for x values (for first seens to correctly add to the sorter lists)
       */
      Set<Long>                     x_set     = new HashSet<Long>(),            
      /**
       * Sets for y values (for first seens to correctly add to the sorter lists)
       */
                                    y_set     = new HashSet<Long>();
      /**
       * List of x values (for sorting... for equal spacing)
       */
      List<Long>                    x_sorter  = new ArrayList<Long>(),          
      /**
       * List of y values (for sorting... for equal spacing)
       */
                                    y_sorter  = new ArrayList<Long>();
      /**
       * Maps from key_str to x
       */
      Map<String,Long>              key_to_x  = new HashMap<String,Long>(),     
      /**
       * Maps from key_str to y
       */
                                    key_to_y  = new HashMap<String,Long>();
      /**
       * Counters for x = Sum of "Count By" ... for least to most
       */
      Map<Long,Double>              x_to_sum  = new HashMap<Long,Double>(),     
      /**
       * Counters for y = Sum of "Count By" ... for least to most
       */
                                    y_to_sum  = new HashMap<Long,Double>();
      /**
       * Counters for x = # of Bundles .. for least to most (items)
       */
      Map<Long,Double>              x_to_buns = new HashMap<Long,Double>(),     
      /**
       * Counters for y = # of Bundles .. for least to most (items)
       */
                                    y_to_buns = new HashMap<Long,Double>();
      /**
       * Converts the x coordinate to bundles
       */
      Map<Long,Set<Bundle>>         x_to_bs   = new HashMap<Long,Set<Bundle>>(), 
      /**
       * Converts the y coordinate to bundles
       */
                                    y_to_bs   = new HashMap<Long,Set<Bundle>>();
      /**
       * Key makers for the x-axis
       */
      List<KeyMaker> xkms  = new ArrayList<KeyMaker>(),
      /**
       * Key makers for the y-axis
       */
                          ykms  = new ArrayList<KeyMaker>(),
      /**
       * Key makers for the y2-axis
       */
                          y2kms = new ArrayList<KeyMaker>();
      /**
       * Minimum x value for linear scaling
       */
      long min_x = Long.MAX_VALUE, 
      /**
       * Minimum y value for linear scaling
       */
           min_y = Long.MAX_VALUE,
      /**
       * Maximum x value for linear scaling
       */
           max_x = Long.MIN_VALUE, 
      /**
       * Maximum y value for linear scaling
       */
	   max_y = Long.MIN_VALUE;
      /**
       * Map from the application values to the uniform 0 to 1 scale.
       */
      Map<Long,Double>         xmap,  
      /**
       * Map from the application values to the uniform 0 to 1 scale.
       */
                               ymap;
      /**
       * Time mapper for the x-axis
       */
      KeyMaker                 xtime, 

      /**
       * Time mapper for the y-axis
       */
                               ytime;

      /**
       * Render performance timers
       */
      long                     rts0, 
                               rts1, 
			       rts2, 
			       rts3;

      /**
       * Clear out the maps and other data structures.
       */
      @Override
      public void finalize() {
	xtime = ytime = null; bs = null; 
        xmap.clear();      ymap.clear(); 
	x_to_bs.clear();   y_to_bs.clear();
	x_to_sum.clear();  y_to_sum.clear();
	x_to_buns.clear(); y_to_buns.clear();
	bundle_to_skeys.clear();
	geom_to_skey.clear();
	skey_to_geom.clear();
      }

      /**
       * Construct the render context according to all the specified parameters.
       *
       *@param id         render id to abort unnecessary renderings
       *@param bs         dataset to render
       *@param count_by   method to accumulate the counts for each bundle
       *@param color_by   method to coloring the plots
       *@param x_axis     x-axis field
       *@param x_scale    x-axis scaling formula
       *@param y_axis     y-axis field (primary)
       *@param y2_axis    y-axis field (secondary/alternate)
       *@param y_scale    y-axis scaling formula
       *@param ptwidth    size of point plots
       *@param vary_color flag to vary the color of the rendering
       *@param duration   model duration in the x-axis
       *@param draw_tms   flag to draw time markers
       *@param draw_sh    flat to draw stacked histograms
       *@param w          width of rendering in pixels
       *@param h          height of rendering in pixels
       */
      public               RenderContext(short id, Bundles bs, String count_by, String color_by, String x_axis, String x_scale, 
                                         String y_axis, String y2_axis, String y_scale, PTWIDTH ptwidth, 
					 boolean vary_color, boolean duration, boolean draw_tms, boolean draw_sh, int w, int h) {
        render_id = id; this.bs = bs; this.w = w; this.h = h; this.count_by = count_by; this.color_by = color_by; this.x_axis = x_axis; this.x_scale = x_scale; this.y_axis = y_axis; this.y2_axis = y2_axis; this.y_scale = y_scale; this.ptwidth = ptwidth; this.vary_color = vary_color; this.duration = duration; this.draw_tms = draw_tms; this.draw_sh = draw_sh;
	rts0 = System.currentTimeMillis();
	counter_context = new BundlesCounterContext(bs, count_by, color_by);
        // Are either time mappers?
        xtime = KeyMaker.isTimeBlank(x_axis) ? new KeyMaker(bs.tabletIterator().next(), x_axis) : null;
	ytime = KeyMaker.isTimeBlank(y_axis) ? new KeyMaker(bs.tabletIterator().next(), y_axis) : null;
	// Initialize the values for the mappers
        Iterator<Tablet> it_tablet = bs.tabletIterator();
        while (it_tablet.hasNext() && currentRenderID() == getRenderID()) {
          Tablet tablet = it_tablet.next();
          // Check to see if this one counts
	  boolean tablet_can_count = count_by.equals(BundlesDT.COUNT_BY_BUNS) || KeyMaker.tabletCompletesBlank(tablet, count_by);
	  // Differentiate time-based versus non-time ... probably should write code that differentiates longs versus ints
	  if (  ((xtime != null && tablet.hasTimeStamps()) || (xtime == null && KeyMaker.tabletCompletesBlank(tablet, x_axis)))
	        && 
                ((ytime != null && tablet.hasTimeStamps()) || (ytime == null && KeyMaker.tabletCompletesBlank(tablet, y_axis)))  ) {
            KeyMaker x_km  = (xtime   == null) ? new KeyMaker(tablet, x_axis) : null, 
	             y_km  = (ytime   == null) ? new KeyMaker(tablet, y_axis) : null,
		     y2_km = (y2_axis == null) ? null : (KeyMaker.tabletCompletesBlank(tablet, y2_axis) ? new KeyMaker(tablet, y2_axis) : null);
            if (x_km  != null) xkms.add(x_km);
	    if (y_km  != null) ykms.add(y_km);
	    if (y2_km != null) y2kms.add(y2_km);

	    // Go through the bundles
            Iterator<Bundle> it_bundle = tablet.bundleIterator();
	    while (it_bundle.hasNext() && currentRenderID() == getRenderID()) {
              Bundle bundle = it_bundle.next();
              long xs[], ys[], y2s[];
	      if (xtime != null) { 
	        if (duration) {
                  xs = new long[2]; xs[0] = xtime.timeStampKey(bundle); 
		                    xs[1] = xtime.endTimeStampKey(bundle);
		} else {
                  xs = new long[1]; xs[0] = xtime.timeStampKey(bundle); 
	        }
              } else { 
                xs = Utils.toLongs(x_km.intKeys(bundle)); 
              }
	      if (ytime != null) { 
	        ys = new long[1]; ys[0] = ytime.timeStampKey(bundle); // Only allow durations in x-axis...
              } else { 
                ys = Utils.toLongs(y_km.intKeys(bundle)); 
              }
	      if (y2_km != null) { 
	        y2s = Utils.toLongs(y2_km.intKeys(bundle));
		// This is either a really dumb idea or a really good one...
		long new_ys[] = new long[ys.length*y2s.length];
		int k = 0;
		for (int i=0;i<ys.length;i++) {
		  for (int j=0;j<y2s.length;j++) {
		    new_ys[k++] = (ys[i]<<32) | (0x00FFFFFFFFL & y2s[j]);
		  }
		}
		ys = new_ys;
	      } else if (y2_axis != null) { // May need to put the ys into the high bits
                for (int i=0;i<ys.length;i++) ys[i] = ys[i]<<32;
	      }

	      double added = 0.0;
              for (int i=0;i<xs.length;i++) {
	        for (int j=0;j<ys.length;j++) {
	          String key = xs[i] + "," + ys[j]; 
		  key_to_x.put(key,xs[i]); key_to_y.put(key,ys[j]);
		  if (xs[i] == ys[j] && x_axis.equals(y_axis) && x_axis.indexOf(BundlesDT.MULTI)>=0) continue; // Don't add diagonals for equal multis
                  if (tablet_can_count) added = counter_context.count(bundle, key); // Need to keep track of the keys... get what we need to add for later
		}
	      }

	      /**
	       *
	       */
	      for (int i=0;i<xs.length;i++) {
		// Mins and maxes
                if (min_x > xs[i]) min_x = xs[i]; if (max_x < xs[i]) max_x = xs[i];
		// x,y sorters
                if (x_set.contains(xs[i]) == false) { x_sorter.add(xs[i]); x_set.add(xs[i]); }
		// x,y sums and item counters
		if (x_to_sum.containsKey(xs[i]) == false) {
                  x_to_sum.put(xs[i], added); x_to_buns.put(xs[i], 1.0); x_to_bs.put(xs[i],new HashSet<Bundle>()); x_to_bs.get(xs[i]).add(bundle);
		} else { x_to_sum.put(xs[i], x_to_sum.get(xs[i]) + added); x_to_buns.put(xs[i], x_to_buns.get(xs[i]) + 1); x_to_bs.get(xs[i]).add(bundle); }
	      }
	      
	      /**
	       *
	       */
	      for (int j=0;j<ys.length;j++) {
		// Mins and maxes
                if (min_y > ys[j]) min_y = ys[j]; if (max_y < ys[j]) max_y = ys[j];
		// x,y sorters
                if (y_set.contains(ys[j]) == false) { y_sorter.add(ys[j]); y_set.add(ys[j]); }
		// x,y sums and item counters
		if (y_to_sum.containsKey(ys[j]) == false) {
                  y_to_sum.put(ys[j], added); y_to_buns.put(ys[j], 1.0); y_to_bs.put(ys[j], new HashSet<Bundle>()); y_to_bs.get(ys[j]).add(bundle);
		} else { y_to_sum.put(ys[j], y_to_sum.get(ys[j]) + added); y_to_buns.put(ys[j], y_to_buns.get(ys[j]) + 1); y_to_bs.get(ys[j]).add(bundle); }
              }
	    }
	  } else {
            Iterator<Bundle> it_bundle = tablet.bundleIterator();
	    while (it_bundle.hasNext() && currentRenderID() == getRenderID()) addToNoMappingSet(it_bundle.next());
	  }
        }

	// Add in the mappings for the time markers if the stars align
        if (xtime != null && xtime.linearTime() && x_scale.equals(AxisMapper.LINEAR_SCALE_STR) && draw_tms) {
          Set<TimeMarker> markers = getRTParent().getTimeMarkers(bs.ts0(),bs.ts1()); if (markers != null) {
	    Iterator<TimeMarker> it_tm = markers.iterator();
	    while (it_tm.hasNext()) {
	      TimeMarker tm = it_tm.next();
	      if (tm.isTimeStamp()) { x_sorter.add(tm.ts0()); } else { x_sorter.add(tm.ts0()); x_sorter.add(tm.ts1()); }
	    }
	  }
        }

	// Calculate the mapping
        if (currentRenderID() == getRenderID()) {
          xmap = AxisMapper.calculateMapping(x_scale, x_sorter, x_to_sum, x_to_buns, min_x, max_x);
          ymap = AxisMapper.calculateMapping(y_scale, y_sorter, y_to_sum, y_to_buns, min_y, max_y);
        }

	// Calculate the render time
	rts1 = System.currentTimeMillis();
      }

      /**
       * Return the rendered width.
       *
       *@return width in pixels
       */
      @Override
      public int           getRCWidth()  { return w; }

      /**
       * Return the rendered height.
       *
       *@return height in pixels
       */
      @Override
      public int           getRCHeight() { return h; }

      /**
       * Find a key maker than can covnert a value back into a string.
       *
       *@param  kms   list of key makers
       *@param  value to convert
       *
       *@return string version of the value
       */
      public String find(List<KeyMaker> kms, int value) {
        Iterator<KeyMaker> it = kms.iterator();
        while (it.hasNext()) { String str = it.next().toString(value); if (str != null) return str; }
        return "Not Found";
      }

      /**
       * Counter context for accumulating pixel-level values
       */
      BundlesCounterContext            screen_counter_context; 
      /**
       * Conversion from bundles to screen keys
       */
      Map<Bundle,Set<String>>          bundle_to_skeys         = new HashMap<Bundle,Set<String>>();
      /**
       * screen x coordinates to world x coordinates
       */
      Map<Integer,Set<Long>>           sx_to_xs                = new HashMap<Integer,Set<Long>>(),
      /**
       * screen y coordinates to world y coordinates
       */
                                       sy_to_ys                = new HashMap<Integer,Set<Long>>();
      /**
       * geometry to screen keys maps (one-to-one)
       */
      Map<Shape,String>                geom_to_skey            = new HashMap<Shape,String>();
      /**
       * screen key to geometry (one-to-one)
       */
      Map<String,Shape>                skey_to_geom            = new HashMap<String,Shape>();
      /**
       * Rendered image 
       */
      BufferedImage base_bi = null; 
      /**
       * Insets for the actual graph
       */
      int           x_lft = 10, 
                    y_top = 10, 
		    x_rgt = 10, 
		    y_bot = 10, 
      /**
       * Graph width in pixels
       */
		    graph_w, 
      /**
       * Graph height in pixels
       */
		    graph_h;
      /**
       * Stacked histograms
       */
      double        x_sh[],
                    y_sh[];
      /**
       * Render the shapes to an image.
       *
       *@return rendered imge
       */
      @Override
      public BufferedImage getBase()     { 
        if (base_bi == null) {
	 Graphics2D g2d = null;
         // String keys to their integer equivalents
         Map<String,Integer> skey_to_x  = new HashMap<String,Integer>(),
                             skey_to_x2 = new HashMap<String,Integer>(),
                             skey_to_y  = new HashMap<String,Integer>();
	 try {
	  rts2 = System.currentTimeMillis();
          base_bi = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) base_bi.getGraphics();
          g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	  RTColorManager.renderVisualizationBackground(base_bi, g2d);
	  // Create the counter context
	  screen_counter_context = new BundlesCounterContext(bs, count_by, color_by);
          // Calculate the dimensions and draw the legend
          int txt_h = Utils.txtH(g2d, "0"); x_lft = x_rgt = y_top = y_bot = txt_h + 5;
	  // Allocate the stacked histograms
	  if (draw_sh) { 
	    x_lft += 5; y_bot += 5; 
	    int size;
	    size = w/5 + 1; if (size < 10) size = 10; // random bigger number... one would have worked as well...
	    x_sh = new double[size];
	    size = h/5 + 1; if (size < 10) size = 10;
	    y_sh = new double[size];
	  }
	  graph_w = w - (x_lft + x_rgt); graph_h = h - (y_top + y_bot);
	  g2d.setColor(RTColorManager.getColor("axis", "major")); g2d.drawLine(x_lft, y_top, x_lft, y_top + graph_h); g2d.drawLine(x_lft, y_top + graph_h, x_lft + graph_w, y_top + graph_h);

	  // Map the logical to the screen
          Iterator<String> it = key_to_x.keySet().iterator();
	  while (it.hasNext() && currentRenderID() == getRenderID()) {
	    String key = it.next(); long x_app = key_to_x.get(key), y_app = key_to_y.get(key);
	    int sx = (int) (x_lft + (xmap.get(x_app)*graph_w)),
	        sy = (int) (y_top + graph_h - (ymap.get(y_app)*graph_h));
            // if (sx < x_lft || sx > x_lft + graph_w || sy < y_top || sy > y_top + graph_h) System.err.println("" + sx + " ... " + xmap.get(x_app) + " , " + sy + " ... " + ymap.get(y_app));
            String skey = sx + "," + sy;  skey_to_x.put(skey,sx); skey_to_y.put(skey,sy);
            if (sx_to_xs.containsKey(sx) == false) sx_to_xs.put(sx,new HashSet<Long>()); sx_to_xs.get(sx).add(x_app);
            if (sy_to_ys.containsKey(sy) == false) sy_to_ys.put(sy,new HashSet<Long>()); sy_to_ys.get(sy).add(y_app);

            if (counter_context.getBundles(key) != null) {
	      Iterator<Bundle> it_bundle = counter_context.getBundles(key).iterator();
	      while (it_bundle.hasNext()) {
	        Bundle bundle = it_bundle.next();
	        screen_counter_context.count(bundle, skey);
	        if (bundle_to_skeys.containsKey(bundle) == false) bundle_to_skeys.put(bundle, new HashSet<String>());
	        bundle_to_skeys.get(bundle).add(skey);
                if (duration && xtime != null) { //  && xtime.linearTime()) { // abc
                  long x2_app = xtime.endTimeStampKey(bundle);
		  int sx2 = (int) (x_lft + (xmap.get(x2_app)*graph_w));
		  if (sx != sx2) {
                    if (sx2 > sx) {
		      String skey_mod = sx + "-" + sx2 + "," + sy;
		      screen_counter_context.count(bundle, skey_mod);
		      bundle_to_skeys.get(bundle).add(skey_mod);
		      skey_to_x .put(skey_mod,sx);
		      skey_to_y .put(skey_mod,sy);
		      skey_to_x2.put(skey_mod,sx2);
                    } else { // Some type of periodic mapping... we'll do the simple case---the real case may wrap several times
		      String skey_mod;

                      skey_mod = sx + "-" + (x_lft+graph_w) + "," + sy;
		      screen_counter_context.count(bundle, skey_mod);
		      bundle_to_skeys.get(bundle).add(skey_mod);
		      skey_to_x .put(skey_mod,sx);
		      skey_to_y .put(skey_mod,sy);
		      skey_to_x2.put(skey_mod,x_lft+graph_w);

                      skey_mod = x_lft + "-" + sx2 + "," + sy;
		      screen_counter_context.count(bundle, skey_mod);
		      bundle_to_skeys.get(bundle).add(skey_mod);
		      skey_to_x .put(skey_mod,x_lft);
		      skey_to_y .put(skey_mod,sy);
		      skey_to_x2.put(skey_mod,sx2);
                    }
		  }
		}
              }
            }
          }

	  // Draw the scale markings
	  g2d.setColor(RTColorManager.getColor("label", "major"));
          String str;
	  if        (x_scale.equals(AxisMapper.LINEAR_SCALE_STR) || x_scale.equals(AxisMapper.EQUAL_SCALE_STR) || x_scale.equals(AxisMapper.LOG_SCALE_STR))       {
            if (xkms.size() > 0) str = find(xkms, (int) min_x); else if (xtime != null) str = xtime.toString(min_x); else str = "xt,xkm null";
            if (str != null) g2d.drawString(str, x_lft,                                 y_top + graph_h + txt_h);

            if (xkms.size() > 0) str = find(xkms, (int) max_x); else if (xtime != null) str = xtime.toString(max_x); else str = "xt,xkm null";

            if (str != null) g2d.drawString(str, x_lft + graph_w - Utils.txtW(g2d, str), y_top + graph_h + txt_h);
	  }

	  // Y Axis is complicated if there's a secondary feature
	  if (y2_axis == null) {
	    if        (y_scale.equals(AxisMapper.LINEAR_SCALE_STR) || y_scale.equals(AxisMapper.EQUAL_SCALE_STR) || y_scale.equals(AxisMapper.LOG_SCALE_STR))       {
              if (ykms.size() > 0) str = find(ykms, (int) min_y); else if (ytime != null) str = ytime.toString(min_y); else str = "yt,ykm null";
              if (str != null) Utils.drawRotatedString(g2d, str, x_lft, y_top + graph_h);
              if (ykms.size() > 0) str = find(ykms, (int) max_y); else if (ytime != null) str = ytime.toString(max_y); else str = "yt,ykm null";
              if (str != null) Utils.drawRotatedString(g2d, str, x_lft, y_top + Utils.txtW(g2d,str));
	    }
          } else               {
	    if        (y_scale.equals(AxisMapper.LINEAR_SCALE_STR) || y_scale.equals(AxisMapper.EQUAL_SCALE_STR) || y_scale.equals(AxisMapper.LOG_SCALE_STR))       {
	      if        (ykms.size() > 0 && y2kms.size() > 0)   { str = find(ykms,  (int) (0x00ffffffff & (min_y>>32))) + 
	                                                                " " + BundlesDT.DELIM + " " +
									find(y2kms, (int) (0x00ffffffff & (min_y)));
	      } else if (ykms.size() > 0)                       { str = find(ykms, (int) (0x00ffffffff & (min_y>>32))); 
	      } else if (ytime    != null)                        str = ytime.toString(min_y); else str = "yt,ykm null";
              if (str != null) Utils.drawRotatedString(g2d, str, x_lft, y_top + graph_h);

	      if        (ykms.size() > 0 && y2kms.size() > 0)   { str = find(ykms, (int)  (0x00ffffffff & (max_y>>32))) + 
	                                                                " " + BundlesDT.DELIM + " " +
									find(y2kms, (int) (0x00ffffffff & (max_y)));
              } else if (ykms.size() > 0)                       { str = find(ykms, (int) (0x00ffffffff & (max_y>>32))); 
	      } else if (ytime != null) str = ytime.toString(max_y); else str = "yt,ykm null";
              if (str != null) Utils.drawRotatedString(g2d, str, x_lft, y_top + Utils.txtW(g2d,str));
	    }
	  }

	  // Print out some errors...
	  if (y2_axis != null) {
	    if (ytime !=  null) {
	      g2d.setColor(RTColorManager.getColor("label", "errorfg"));
	      String error_str = "Time In Y Not Good With Secondary Axis";
              g2d.drawString(error_str, x_lft,                                        y_top + Utils.txtH(g2d, error_str));
	      g2d.drawString(error_str, x_lft + graph_w - Utils.txtW(g2d, error_str), y_top + graph_h);
	    }
	    if (y_scale.equals(AxisMapper.LINEAR_SCALE_STR) || y_scale.equals(AxisMapper.LOG_SCALE_STR)) {
	      g2d.setColor(RTColorManager.getColor("label", "errorfg"));
	      String error_str = "Secondary Axis Needs Equal Spacing";
              g2d.drawString(error_str, x_lft + graph_w - Utils.txtW(g2d, error_str), y_top + Utils.txtH(g2d, error_str));
	      g2d.drawString(error_str, x_lft,                                        y_top + graph_h);
	    }
	  }

	  String axis_str = null;
	  if        (x_scale.equals(AxisMapper.LINEAR_SCALE_STR))       { axis_str = x_axis;                g2d.setColor(RTColorManager.getColor("label", "linear"));
	  } else if (x_scale.equals(AxisMapper.LOG_SCALE_STR))          { axis_str = x_axis + " (Log)";     g2d.setColor(RTColorManager.getColor("label", "log"));
	  } else if (x_scale.equals(AxisMapper.EQUAL_SCALE_STR))        { axis_str = x_axis + " (Equal)";   g2d.setColor(RTColorManager.getColor("label", "equal"));
	  } else if (x_scale.equals(AxisMapper.SORT_STR))               { axis_str = x_axis + " (Sort)";    g2d.setColor(RTColorManager.getColor("label", "sort"));
	  } else if (x_scale.equals(AxisMapper.SORT_REVERSE_STR))       { axis_str = x_axis + " (RSort)";   g2d.setColor(RTColorManager.getColor("label", "sort"));
	  } else if (x_scale.equals(AxisMapper.SORT_ITEMS_STR))         { axis_str = x_axis + " (Sort B)";  g2d.setColor(RTColorManager.getColor("label", "sort"));
	  } else if (x_scale.equals(AxisMapper.SORT_ITEMS_REVERSE_STR)) { axis_str = x_axis + " (RSort B)"; g2d.setColor(RTColorManager.getColor("label", "sort"));
	  } else throw new RuntimeException("Don't Understand Method \"" + x_axis + "\"");
	  g2d.drawString(axis_str, x_lft + graph_w/2 - Utils.txtW(g2d, axis_str)/2, y_top + graph_h + txt_h + 2);

	  String add_str = ""; if (y2_axis != null) add_str = " " + BundlesDT.DELIM + " " + y2_axis;
	  if        (y_scale.equals(AxisMapper.LINEAR_SCALE_STR))       { axis_str = y_axis + add_str;                g2d.setColor(RTColorManager.getColor("label", "linear"));
	  } else if (y_scale.equals(AxisMapper.LOG_SCALE_STR))          { axis_str = y_axis + add_str + " (Log)";     g2d.setColor(RTColorManager.getColor("label", "log"));
	  } else if (y_scale.equals(AxisMapper.EQUAL_SCALE_STR))        { axis_str = y_axis + add_str + " (Equal)";   g2d.setColor(RTColorManager.getColor("label", "equal"));
	  } else if (y_scale.equals(AxisMapper.SORT_STR))               { axis_str = y_axis + add_str + " (Sort)";    g2d.setColor(RTColorManager.getColor("label", "sort"));
	  } else if (y_scale.equals(AxisMapper.SORT_REVERSE_STR))       { axis_str = y_axis + add_str + " (RSort)";   g2d.setColor(RTColorManager.getColor("label", "sort"));
	  } else if (y_scale.equals(AxisMapper.SORT_ITEMS_STR))         { axis_str = y_axis + add_str + " (Sort B)";  g2d.setColor(RTColorManager.getColor("label", "sort"));
	  } else if (y_scale.equals(AxisMapper.SORT_ITEMS_REVERSE_STR)) { axis_str = y_axis + add_str + " (RSort B)"; g2d.setColor(RTColorManager.getColor("label", "sort"));
	  } else throw new RuntimeException("Don't Understand Method \"" + y_axis + "\"");
	  Utils.drawRotatedString(g2d, axis_str, x_lft, y_top + graph_h/2 + Utils.txtW(g2d, axis_str)/2);

	  // Go to the screen
	  if (ptwidth == PTWIDTH.VARY || ptwidth == PTWIDTH.VARY_LOG) g2d.setStroke(new BasicStroke(1.8f));

	  // Determine the shape of the duration bars
	  int dur_yoff = 0, dur_h = 1;
	  switch (ptwidth) { case LARGE: dur_yoff = -3; dur_h = 6; break; case MEDIUM: dur_yoff = -1; dur_h = 3; break; }

	  // Do the actual rendering
	  g2d.setColor(RTColorManager.getColor("data", "default")); // Set default color... varying will happen below if set
	  it = screen_counter_context.binIterator();
	  while (it.hasNext() && currentRenderID() == getRenderID()) {
	    String skey = it.next();
	    int sx  = skey_to_x.get(skey), sy = skey_to_y.get(skey);
            if (vary_color) g2d.setColor(screen_counter_context.binColor(skey)); 
           if (skey_to_x2.containsKey(skey)) {
	    int sx2 = skey_to_x2.get(skey);
	    Rectangle2D.Float rect = new Rectangle2D.Float(sx,sy+dur_yoff,sx2-sx,dur_h);
	    g2d.fill(rect); geom_to_skey.put(rect,skey); skey_to_geom.put(skey,rect);
	   } else {
	    if (draw_sh) { double total = screen_counter_context.total(skey); y_sh[sy/5] += total; x_sh[sx/5] += total; }
	    if        (ptwidth == PTWIDTH.VARY) {
              double width = screen_counter_context.totalNormalized(skey) * 20.0 + 1.8;
              Ellipse2D ellipse = new Ellipse2D.Double(sx-width/2,sy-width/2,width,width);
              g2d.draw(ellipse); geom_to_skey.put(ellipse,skey); skey_to_geom.put(skey,ellipse);
            } else if (ptwidth == PTWIDTH.VARY_LOG) {
	      double total = screen_counter_context.total(skey), width = 1.0;
	      if      (total == 1.0) width = 1.8;
	      else if (total >  1.0) width = 1.8 + 20.0*Math.log(total)/Math.log(screen_counter_context.totalMaximum());
              Ellipse2D ellipse = new Ellipse2D.Double(sx-width/2,sy-width/2,width,width);
              g2d.draw(ellipse); geom_to_skey.put(ellipse,skey); skey_to_geom.put(skey,ellipse);
            } else          {
	      Shape       shape;
	      switch (ptwidth) {
	        case SMALL:           shape = new Rectangle2D.Float(sx,sy,1,1);     break;
		case LARGE:           shape = new Ellipse2D.Float(sx-3,sy-3,6,6);   break;
		case MEDIUM: default: shape = new Rectangle2D.Float(sx-1,sy-1,3,3); 
	      }
              g2d.fill(shape); geom_to_skey.put(shape,skey); skey_to_geom.put(skey,shape);
	    }
	   }
	  }

          // Add timestamp if selected and applicable
          if (xtime != null && xtime.linearTime() && x_scale.equals(AxisMapper.LINEAR_SCALE_STR) && draw_tms) {
            int sy0 = y_top, sy1 = y_top + graph_h, sx = -100;
            Set<TimeMarker> markers = getRTParent().getTimeMarkers(bs.ts0(),bs.ts1()); if (markers != null) {
	      Iterator<TimeMarker> it_tm = markers.iterator();
	      while (it_tm.hasNext()) {
	        TimeMarker tm = it_tm.next();
                g2d.setColor(RTColorManager.getColor(tm.getDescription()));
                if (tm.isTimeStamp()) {
		  str = tm.getDescription();
	          sx = (int) (x_lft + (xmap.get(tm.ts0())*graph_w)); g2d.drawLine(sx,sy0,sx,sy1); g2d.drawLine(sx,sy0,sx-5,sy0-5); g2d.drawLine(sx,sy0,sx+5,sy0-5);
		  Utils.drawRotatedString(g2d, str, sx, y_top + graph_h/2 + Utils.txtW(g2d,str)/2);
	        } else                {
		  str = tm.getDescription();
	          int sx0 = (int) (x_lft + (xmap.get(tm.ts0())*graph_w)); g2d.drawLine(sx0,sy0,sx0,sy1);
		  Utils.drawRotatedString(g2d, str, sx0, y_top + graph_h/2 + Utils.txtW(g2d,str)/2);
	          int sx1 = (int) (x_lft + (xmap.get(tm.ts1())*graph_w)); g2d.drawLine(sx1,sy0,sx1,sy1);
                  g2d.drawLine(sx0,sy0,(sx0+sx1)/2,2);
                  g2d.drawLine(sx1,sy0,(sx0+sx1)/2,2);
		  str = Utils.humanReadableDuration(tm.ts1() - tm.ts0());
		  g2d.drawString(str, (sx0+sx1)/2 - Utils.txtW(g2d,str)/2, txt_h);
	        }
	      }
            }
          }

	  // Add the stacked histograms
	  if (draw_sh) {
	    // Find the max
	    double max = x_sh[0]; 
	    for (int i=0;i<x_sh.length;i++) if (x_sh[i] > max) max = x_sh[i];
	    for (int i=0;i<y_sh.length;i++) if (y_sh[i] > max) max = y_sh[i];

	    // draw the blocks
	    ColorScale cs = RTColorManager.getContinuousColorScale();
	    for (int i=0;i<x_sh.length;i++) {
	      if (x_sh[i] > 0.0) { g2d.setColor(cs.at((float) (x_sh[i]/max))); g2d.fillRect(5*i,h-5,5,5); }
	    }
	    for (int i=0;i<y_sh.length;i++) {
	      if (y_sh[i] > 0.0) {
	        g2d.setColor(cs.at((float) (y_sh[i]/max))); g2d.fillRect(0,5*i,5,5); }
	    }
	  }

	  // Print out information about the render time
	  rts3 = System.currentTimeMillis(); String time_str = "" + ((rts3 - rts2) + (rts1 - rts0)) + " ms";
	  g2d.setColor(RTColorManager.getColor("label", "performance")); g2d.drawString(time_str, getRCWidth() - Utils.txtW(g2d, time_str), txt_h);
         } finally { if (g2d != null) g2d.dispose(); }
	}
	return base_bi;
      }
    }
  }
}

