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
import java.awt.BorderLayout;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

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
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
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

import racetrack.visualization.RTColorManager;

/**
 * Implementation of the rug plot visualization.
 *
 *@author  D. Trimm
 *@version 0.9
 */
public class RTRugPlotPanel extends RTPanel {
  /**
   * 
   */
  private static final long serialVersionUID = -4940154312018114598L;

  /**
   * Vary the width of the plots
   */
  JRadioButtonMenuItem width_vary_rbmi, 
  /**
   * Vary the width by a logarithmic scale
   */
                       width_vary_log_rbmi, 
  /**
   * Make the plots small
   */
		       width_small_rbmi, 
  /**
   * Make the plots medium
   */
		       width_medium_rbmi, 
  /**
   * Make the plots large
   */
		       width_large_rbmi;

  /**
   * Enable the interactive closet point overlap
   */
  JCheckBoxMenuItem    closest_points_cbmi,
  /**
   * Vary the color of the plots
   */
                       color_cbmi,
  /**
   * Model duration in the x axis for time-based axes
   */
                       duration_cbmi,
  /**
   * Show time markers
   */
		       timemarkers_cbmi,
  /**
   * Hide axis labels
   */
                       hide_labels_cbmi;

  /**
   * Construct an instance of the rug plot panel using
   * the specified parent object.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt parent GUI instance
   */
  public RTRugPlotPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      { 
    super(win_type, win_pos, win_uniq, rt);   

    // Construct the GUI
    add("Center", component = new RTRugPlotComponent());

    // Construct the popup menu
    JMenuItem mi;
    getRTPopupMenu().add(mi = new JMenuItem("Configure..."));
      mi.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          if (config_dialog == null) config_dialog = new ConfigDialog();
	  config_dialog.setVisible(true);
	}
      } );
    getRTPopupMenu().addSeparator();

    // Width of Dots
    ButtonGroup bg = new ButtonGroup();
    getRTPopupMenu().add(width_vary_rbmi          = new JRadioButtonMenuItem("Vary Width"));                 bg.add(width_vary_rbmi);
    getRTPopupMenu().add(width_vary_log_rbmi      = new JRadioButtonMenuItem("Vary Width (Log)"));           bg.add(width_vary_log_rbmi);
    getRTPopupMenu().add(width_small_rbmi         = new JRadioButtonMenuItem("Fixed Width (Small)"));        bg.add(width_small_rbmi);
    getRTPopupMenu().add(width_medium_rbmi        = new JRadioButtonMenuItem("Fixed Width (Medium)", true)); bg.add(width_medium_rbmi);
    getRTPopupMenu().add(width_large_rbmi         = new JRadioButtonMenuItem("Fixed Width (Large)"));        bg.add(width_large_rbmi);

    // Color of Dots
    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(color_cbmi               = new JCheckBoxMenuItem("Vary Color"));

    // Other options
    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(closest_points_cbmi      = new JCheckBoxMenuItem("Closest Point Interaction", true));
    getRTPopupMenu().add(duration_cbmi            = new JCheckBoxMenuItem("Model Duration In X", true));
    getRTPopupMenu().add(timemarkers_cbmi         = new JCheckBoxMenuItem("Draw Time Markers", true));
    getRTPopupMenu().add(hide_labels_cbmi         = new JCheckBoxMenuItem("Hide Axis Labels", false));

    // Add the default listeners
    defaultListener(width_vary_rbmi);
    defaultListener(width_vary_log_rbmi);
    defaultListener(width_small_rbmi);
    defaultListener(width_medium_rbmi);
    defaultListener(width_large_rbmi);

    defaultListener(color_cbmi);

    defaultListener(duration_cbmi);
    defaultListener(timemarkers_cbmi);
    defaultListener(hide_labels_cbmi);
  }

  /**
   * Return the flag to hide the axis labels in the rendering.
   *
   *@return true to hide labels
   */
  public boolean      hideLabels     ()           { return hide_labels_cbmi.isSelected(); }

  /**
   * Set the flag to hide the axis labels in the rendering.
   *
   *@param f true to hide labels
   */
  public void         hideLabels     (boolean f)  { hide_labels_cbmi.setSelected(f); }

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
   * Grid array containg each subplots configuration.  If you need more cells, this is the
   * place to change the settings.  Probably should be below 8x8 or so.
   */
  Config configs[][] = new Config[16][16];

  /**
   * Structure to hold information about each xy plot (each rug)
   */
  class Config { 
    int x, y, w, h; String x_axis, y_axis, y2_axis, x_scale, y_scale; 

    /**
     * Set the x-axis field.
     *
     *@param new_xaxis new x-axis
     */
    public void xAxis (String new_xaxis)  { x_axis  = new_xaxis;  }

    /**
     * Set the x-scale.
     *
     *@param new_xscale new scale xor y-axis
     */
    public void xScale(String new_xscale) { 
      if (new_xscale == null) { System.err.println("(new xscale == null)"); Throwable t = new RuntimeException(); t.printStackTrace(System.err); }
      x_scale = new_xscale; 
    }

    /**
     * Set the primary y-axis.
     *
     *@param new_yaxis new y-axis
     */
    public void yAxis (String new_yaxis)  { y_axis  = new_yaxis;  }

    /**
     * Set the secondary y-axis.
     *
     *@param new_y2axis new secondary y-axis
     */
    public void y2Axis(String new_y2axis) { y2_axis = new_y2axis; }

    /**
     * Set the y-scale.
     *
     *@param new_yscale new scale for y-axis
     */
    public void yScale(String new_yscale) { 
      if (new_yscale == null) { System.err.println("(new yscale == null)"); Throwable t = new RuntimeException(); t.printStackTrace(System.err); }
      y_scale = new_yscale; 
    }

    /**
     * Return a string representation of this config -- used for debugging.
     *
     *@return simple string rep of the variables in this config
     */
    public String toString() { return "x="+x+" y="+y+" w="+w+" h="+h+" x="+x_axis+ "("+x_scale+") y="+y_axis+" - "+y2_axis+" ("+y_scale+")"; }
  }

  /**
   * Configuration dialog for this instance of the component
   */
  private ConfigDialog config_dialog;

  /**
   * Update the fields for the component.  In this case, update the comboboxes in the configuration dialog.
   */
  public void updateBys() { if (config_dialog != null) config_dialog.updateBys(); }

  /**
   * Configuration dialog for the rug plot.  Allows a user to configure the size and position of each
   * rug in the rug plot.  Handles configuration of the axes and scales and ensures that they are coherent.
   */
  class ConfigDialog extends JDialog {
    /**
     * Cell width in pixels
     */
    final int cell_w = 24, 
    /**
     * Cell height in pixels
     */
              cell_h = 24; 
    /**
     * Last config manipulated by the dialog -- used by the axes and scale combobox listeners
     */
    Config    last_config = null;
    /**
     * Component used to depict the configuration
     */
    Comp      comp;

    /**
     * Combobox for the x-axis
     */
    JComboBox x_cb,
    /**
     * Combobox for the primary y-axis
     */
              y_cb,
    /**
     * Combobox for the secondary y-axis
     */
	      y2_cb,
    /**
     * Combobox for the x-scale
     */
              x_scale_cb,
    /**
     * Combobox for the x-scale
     */
              y_scale_cb;

    /**
     * Construct the configuration dialog by creating the components and adding the listeners.
     */
    public ConfigDialog() { 
      setTitle("Rug Plot Config"); 

      String scales[] = AxisMapper.simpleScales();

      // Configure the components
      getContentPane().add("Center", comp = new Comp()); 
      JPanel southern = new JPanel(new BorderLayout(5,5));
       JPanel southern_left  = new JPanel(new GridLayout(3,1,3,3)),
	      southern_mid   = new JPanel(new GridLayout(3,1,3,3)),
	      southern_right = new JPanel(new GridLayout(3,1,3,3));
        southern_left.add(new JLabel("X"));  southern_mid.add(x_cb  = new JComboBox()); southern_right.add(x_scale_cb = new JComboBox(scales));
	southern_left.add(new JLabel("Y"));  southern_mid.add(y_cb  = new JComboBox()); southern_right.add(y_scale_cb = new JComboBox(scales));
	southern_left.add(new JLabel("Y2")); southern_mid.add(y2_cb = new JComboBox());
       southern.add("West",   southern_left);
       southern.add("Center", southern_mid);
       southern.add("East",   southern_right);
      getContentPane().add("South",  southern);
      updateBys();

      // Add the listeners
      x_cb.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent ie) {
          String str = (String) x_cb.getSelectedItem();
	  if (last_config != null && str != null) { last_config.xAxis(str); propagate(last_config, false, true); getRTComponent().render(); } } } );
      x_scale_cb.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent ie) {
          String str = (String) x_scale_cb.getSelectedItem();
	  if (last_config != null && str != null) { last_config.xScale(str); propagate(last_config, false, true); getRTComponent().render(); } } } );
      y_cb.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent ie) {
          String str = (String) y_cb.getSelectedItem();
	  if (last_config != null && str != null) { last_config.yAxis(str); propagate(last_config, true, false); getRTComponent().render(); } } } );
      y2_cb.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent ie) {
          String str = (String) y2_cb.getSelectedItem();
	  if (last_config != null && str != null) { last_config.y2Axis(str); propagate(last_config, true, false); getRTComponent().render(); } } } );
      y_scale_cb.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent ie) {
          String str = (String) y_scale_cb.getSelectedItem();
	  if (last_config != null && str != null) { last_config.yScale(str); propagate(last_config, true, false); getRTComponent().render(); } } } );

      // Resolve the layout
      pack(); }

    /**
     * Propagate an axis or scale change to neighboring configurations.
     *
     *@param config configuration to propagate
     *@param dx     in the x-axis (horizontal)
     *@param dy     in the y-axis (vertical)
     */
    protected void propagate(Config config, boolean dx, boolean dy) {
      // Don't propagate unless the config is complete -- suspect that partial configs are being propagated by combo boxes as the
      // combo box is initialized with that value.
      if (config.x_axis  == null || config.y_axis  == null || config.y2_axis == null || config.x_scale == null || config.y_scale == null) return;

      // Determine the direction for propagation
      if        (dx) {
        for (int i=0;i<2;i++) {
          int x = config.x, y = config.y; int x_inc = (i == 0) ? 1 : -1;
          while (x >= 0 && x < configs[y].length && configs[y][x] != null) {
	    if (configs[y][x] != config) { configs[y][x].yAxis(config.y_axis); configs[y][x].yScale(config.y_scale); configs[y][x].y2Axis(config.y2_axis); }
	    x += x_inc;
	  }
        }
      } else if (dy) {
        for (int i=0;i<2;i++) {
          int x = config.x, y = config.y; int y_inc = (i == 0) ? 1 : -1;
          while (y >= 0 && y < configs.length && configs[y][x] != null) {
	    if (configs[y][x] != config) { configs[y][x].xAxis(config.x_axis);  configs[y][x].xScale(config.x_scale); }
	    y += y_inc;
	  }
        }
      }
      // Check the sanity of the system
      // - First
      for (int y=0;y<configs.length;y++) {
        Config cfg = null;
        for (int x=0;x<configs[y].length;x++) {
          if        (cfg           == null) { cfg = configs[y][x];
	  } else if (configs[y][x] == null) { cfg = null;
	  } else                            { configs[y][x].yAxis(cfg.y_axis);
	                                      configs[y][x].y2Axis(cfg.y2_axis);
	                                      configs[y][x].yScale(cfg.y_scale); }
	}
      }
      for (int x=0;x<configs[0].length;x++) {
        Config cfg = null;
        for (int y=0;y<configs.length;y++) {
          if        (cfg           == null) { cfg = configs[y][x];
	  } else if (configs[y][x] == null) { cfg = null;
	  } else                            { configs[y][x].xAxis(cfg.x_axis);
	                                      configs[y][x].xScale(cfg.x_scale); }
	}
      }
    }

    /**
     * Update the comboboxes with the new fields from the application
     */
    public void updateBys() {
	BundlesG globals = getRTParent().getRootBundles().getGlobals(); String fields[];

	// X and Y Axes are the same
	fields = KeyMaker.blanks(globals,false,true,true,true);
	x_cb.removeAllItems();  for (int i=0;i<fields.length;i++) x_cb.addItem(fields[i]);
        y_cb.removeAllItems();  for (int i=0;i<fields.length;i++) y_cb.addItem(fields[i]);

	// Y Axis 2 is a little different
	fields = KeyMaker.blanks(globals,false,true,true,false); 
	y2_cb.removeAllItems(); y2_cb.addItem(BundlesDT.COUNT_BY_NONE); for (int i=0;i<fields.length;i++) y2_cb.addItem(fields[i]);
    }

    /**
     * Primary component for the configuration dialog.  Shows a grid with the current configuration of xy plots.
     */
    class Comp extends JComponent implements MouseListener, MouseMotionListener {
      public Comp() { 
        Dimension d = new Dimension(configs[0].length * cell_w + 2, configs.length * cell_h + 2); setPreferredSize(d); setMinimumSize(d); setMaximumSize(d); 
	addMouseListener(this); addMouseMotionListener(this);
      }
      public void paintComponent(Graphics g) { Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	// Fill the background
        g2d.setColor(RTColorManager.getColor("background", "default")); g2d.fillRect(0,0,getWidth(),getHeight());
	// Draw the background grid
        g2d.setColor(RTColorManager.getColor("axis", "minor"));
	for (int y=0;y<configs.length;y++) for (int x=0;x<configs[y].length;x++) { g2d.drawRect(x*cell_w,y*cell_h,cell_w,cell_h); }
	// Draw the actual settings
	Stroke orig_stroke = g2d.getStroke();
	for (int y=0;y<configs.length;y++) for (int x=0;x<configs[y].length;x++) { 
	  if (configs[y][x] != null && configs[y][x].x == x && configs[y][x].y == y) {
            // Draw a faded background
            Composite orig_comp = g2d.getComposite();
            g2d.setColor(RTColorManager.getColor("axis", "minor"));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
	    g2d.fillRect(x*cell_w+3, y*cell_h+3, configs[y][x].w*cell_w-6, configs[y][x].h*cell_h-6);
            g2d.setComposite(orig_comp);

	    // Draw the selected one slightly different
	    if (configs[y][x] == last_config) {
              g2d.setColor(RTColorManager.getColor("annotate", "cursor"));
	      g2d.setStroke(new BasicStroke(3.0f)); 
	    } else                            {
              g2d.setColor(RTColorManager.getColor("axis", "major"));
	      g2d.setStroke(new BasicStroke(2.0f));
            }
	    g2d.drawRect(x*cell_w+2, y*cell_h+2, configs[y][x].w*cell_w-4, configs[y][x].h*cell_h-4);
	  }
	}
	g2d.setStroke(orig_stroke);
        // Draw the interactions
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g2d.setColor(RTColorManager.getColor("select", "region"));
	if (drag) {
          int cx0 = mx0/cell_w, cy0 = my0/cell_h, cx1 = mx1/cell_w, cy1 = my1/cell_h;
	  if (cx0 > cx1) { int tmp = cx0; cx0 = cx1; cx1 = tmp; }
	  if (cy0 > cy1) { int tmp = cy0; cy0 = cy1; cy1 = tmp; }
	  if (cy0 <  0)                 cy0 = 0;
          if (cy1 >= configs.length)    cy1 = configs.length-1;
	  if (cx0 <  0)                 cx0 = 0;
	  if (cx1 >= configs[0].length) cx1 = configs[0].length-1;
	  int cw = cx1 - cx0 + 1, ch = cy1 - cy0 + 1;
          g2d.fillRect(cx0 * cell_w, cy0 * cell_h, (cx1+1)*cell_w - cx0*cell_w, (cy1+1)*cell_h - cy0*cell_h);

	  // Draw any errors
          g2d.setColor(RTColorManager.getColor("label", "errorfg"));
	  // - Overlap errors
          for (int y=cy0;y<=cy1;y++) for (int x=cx0;x<=cx1;x++) { if (configs[y][x] != null) { g2d.fillRect(x*cell_w,y*cell_h,cell_w,cell_h); } }
	  // - Adjoining differences
          for (int y=cy0-1;y<=cy1+1;y++) {
	    for (int x=cx0-1;x<=cx1+1;x++) {
              if ((y == cy0-1 && x == cx0-1) || (y == cy0-1 && x == cx1+1) || (y == cy1+1 && x == cx0-1) || (y == cy1+1 && x == cx1+1)) continue;
	      if (y >= 0 && y < configs.length && x >= 0 && x < configs[0].length && configs[y][x] != null) {
                if ((y < cy0 || y > cy1) && (configs[y][x].w != cw || configs[y][x].x != cx0)) {
		  g2d.drawString("X-Axis Misalignment", cx0*cell_w, cy0*cell_h);
		}
		if ((x < cx0 || x > cx1) && (configs[y][x].h != ch || configs[y][x].y != cy0)) {
		  g2d.drawString("Y-Axis Misalignment", cx0*cell_w, cy0*cell_h);
		}
	      }
	    }
	  }
	}
      }

      /**
       * Beginning of mouse drag operation in x
       */
      int mx0, 
      /**
       * Beginning of mouse drag operation in y
       */
          my0, 
      /**
       * End of mouse drag operation in x
       */
	  mx1, 
      /**
       * End of mouse drag operation in y
       */
	  my1, 
      /**
       * Current mouse x location
       */
	  mx, 
      /**
       * Current mouse y location
       */
	  my; 
      /**
       * Drag operation in effect
       */
      boolean drag;

      /**
       * Mouse entered event - do nothing.
       *@param me mouse event
       */
      public void mouseEntered (MouseEvent me) { }

      /**
       * Mouse exited event - do nothing.
       *@param me mouse event
       */
      public void mouseExited  (MouseEvent me) { }

      /**
       * Mouse press event - begin a drag operation if the mouse is not in a config; otherwise, select the config.
       *@param me mouse event
       */
      public void mousePressed (MouseEvent me) { 
        if        (me.getButton() == MouseEvent.BUTTON1) {
	  mx0 = mx1 = me.getX(); my0 = my1 = me.getY(); 

          int cx0 = mx0/cell_w, cy0 = my0/cell_h; 
	  if (cx0 < 0) cx0 = 0; 
	  if (cy0 < 0) cy0 = 0;
	  if (cx0 >= configs[0].length) cx0 = configs[0].length-1;
	  if (cy0 >= configs.length)    cy0 = configs.length-1;
	  if (configs[cy0][cx0] == null) { drag = true; } else { last_config = configs[cy0][cx0]; }

	  repaint();
	}
      }

      /**
       * Mouse release event - create the config if it's valid.
       *@param me mouse event
       */
      public void mouseReleased(MouseEvent me) {
        if        (me.getButton() == MouseEvent.BUTTON1) {
	  drag = false;

	  // Compute the area
          int cx0 = mx0/cell_w, cy0 = my0/cell_h, cx1 = mx1/cell_w, cy1 = my1/cell_h;
	  if (cx0 > cx1) { int tmp = cx0; cx0 = cx1; cx1 = tmp; }
	  if (cy0 > cy1) { int tmp = cy0; cy0 = cy1; cy1 = tmp; }
	  if (cy0 <  0)                 cy0 = 0;
          if (cy1 >= configs.length)    cy1 = configs.length-1;
	  if (cx0 <  0)                 cx0 = 0;
	  if (cx1 >= configs[0].length) cx1 = configs[0].length-1;
	  int cw = cx1 - cx0 + 1, ch = cy1 - cy0 + 1;

	  // Determine if there are any overlapping errors
          boolean error = false; for (int y=cy0;y<=cy1;y++) for (int x=cx0;x<=cx1;x++) { if (configs[y][x] != null) { error = true; } }
          for (int y=cy0-1;y<=cy1+1;y++) {
	    for (int x=cx0-1;x<=cx1+1;x++) {
              if ((y == cy0-1 && x == cx0-1) || (y == cy0-1 && x == cx1+1) || (y == cy1+1 && x == cx0-1) || (y == cy1+1 && x == cx1+1)) continue;
	      if (y >= 0 && y < configs.length && x >= 0 && x < configs[0].length && configs[y][x] != null) {
                if ((y < cy0 || y > cy1) && (configs[y][x].w != cw || configs[y][x].x != cx0)) { error = true; }
		if ((x < cx0 || x > cx1) && (configs[y][x].h != ch || configs[y][x].y != cy0)) { error = true; }
	      }
	    }
	  }

	  // Add the region
	  if (error == false) {
	    Config config = new Config(); config.x = cx0; config.y = cy0; config.w = cx1 - cx0 + 1; config.h = cy1 - cy0 + 1;
            for (int y=cy0;y<=cy1;y++) for (int x=cx0;x<=cx1;x++) configs[y][x] = config;
            last_config = config; updateComboBoxes(); getRTComponent().render();
	  }
	}
	repaint();
      }

      /**
       * Update the fields within the comboboxes for the newly selected or created config.  If it's 
       * a new config, borrow from the neighbors.  Otherwise, set the comboboxes to the values in the config.
       */
      public void updateComboBoxes() {
	//
	// If it's not config'd, try to borrow the scale from a neighbor
	//
        if (last_config.x_axis == null) {
          Config side = null, vert = null;

	  // Look for a neighbor
	  // - Horizontally
          if (                last_config.x - 1             >= 0 && 
	                      configs[last_config.y][last_config.x - 1] != null)             side = configs[last_config.y][last_config.x - 1];
          if (side == null && last_config.x + last_config.w <  configs[0].length && 
	                      configs[last_config.y][last_config.x + last_config.w] != null) side = configs[last_config.y][last_config.x + last_config.w];
          // - Vertically
          if (                last_config.y - 1             >= 0 && 
	                      configs[last_config.y - 1][last_config.x] != null)             vert = configs[last_config.y - 1][last_config.x];
          if (vert == null && last_config.y + last_config.h <  configs.length && 
	                      configs[last_config.y + last_config.h][last_config.x] != null) vert = configs[last_config.y + last_config.h][last_config.x];

	  // Borrow from the vertical
	  if (vert == null) { x_cb.setSelectedItem(0);       last_config.xAxis((String) x_cb.getSelectedItem()); 
	                      x_scale_cb.setSelectedItem(0); last_config.xScale((String) x_scale_cb.getSelectedItem()); } else {
	    last_config.xAxis(vert.x_axis);   x_cb.setSelectedItem(last_config.x_axis);
	    last_config.xScale(vert.x_scale); x_scale_cb.setSelectedItem(last_config.x_scale);
	  }
          // Borrow from the side
	  if (side == null) { y_cb.setSelectedItem (0);      last_config.yAxis((String) y_cb.getSelectedItem());
	                      y2_cb.setSelectedItem(0);      last_config.y2Axis((String) y2_cb.getSelectedItem()); 
			      y_scale_cb.setSelectedItem(0); last_config.yScale((String) y_scale_cb.getSelectedItem()); } else {
	    last_config.yAxis(side.y_axis);   y_cb.setSelectedItem(last_config.y_axis);
	    last_config.y2Axis(side.y2_axis); y2_cb.setSelectedItem(last_config.y2_axis);
	    last_config.yScale(side.y_scale); y_scale_cb.setSelectedItem(last_config.y_scale);
          }
	//
	// Otherwise, just set the config
	//
	} else {
          x_cb.setSelectedItem (last_config.x_axis);  
	  y_cb.setSelectedItem (last_config.y_axis);  
	  y2_cb.setSelectedItem(last_config.y2_axis); 

	  x_scale_cb.setSelectedItem(last_config.x_scale); 
	  y_scale_cb.setSelectedItem(last_config.y_scale); 
	}
      }

      /**
       * Mouse click event -- select the config under the mouse.
       *@param me mouse event
       */
      public void mouseClicked (MouseEvent me) { 
        int cx = mx/cell_w, cy = my/cell_h; if (cx < 0 || cx >= configs[0].length || cy < 0 || cy >= configs.length) return;
	if (me.getButton() == MouseEvent.BUTTON1) { // Select
	  if (configs[cy][cx] != null) {
	    last_config = configs[cy][cx];
	    updateComboBoxes();
	    repaint(); getRTComponent().render();
	  }
	}

	if (me.getButton() == MouseEvent.BUTTON3) { // Delete
	  Config config = configs[cy][cx];
          if (configs[cy][cx] != null) {
	    for (int y=config.y;y<config.y+config.h;y++) for (int x=config.x;x<config.x+config.w;x++) configs[y][x] = null;
	    repaint(); getRTComponent().render();
	  }
	}
      }

      /**
       * Record the new position of the mouse.
       *@param me mouse event
       */
      public void mouseMoved   (MouseEvent me) { mx = mx0 = me.getX(); my = my0 = me.getY(); }

      /**
       * Record the new position of the mouse and request a repaint if a drag is in effect.
       *@param me mouse event
       */
      public void mouseDragged (MouseEvent me) { mx = mx1 = me.getX(); my = my1 = me.getY(); if (drag) repaint(); }
    }
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "rugplot"; }

  /**
   * Return a string representing the configuration of this component.  Used for
   * bookmarking views to more easily recall them.
   *
   *@return string representing view configuration
   */
  @Override
  public String       getConfig    ()           { 
    StringBuffer configs_sb = new StringBuffer();
    for (int y=0;y<configs.length;y++) { for (int x=0;x<configs[y].length;x++) {
	Config cfg = configs[y][x];
        if (cfg != null && y == cfg.y && x == cfg.x) {
          String str = cfg.x + "," + cfg.y + "," + cfg.w + "," + cfg.h + "," +
	               Utils.encToURL(cfg.x_axis)  + "," +
		       Utils.encToURL(cfg.y_axis)  + "," +
		       Utils.encToURL(cfg.y2_axis) + "," +
		       Utils.encToURL(cfg.x_scale) + "," + 
		       Utils.encToURL(cfg.y_scale);
          if (configs_sb.length() > 0) configs_sb.append(BundlesDT.DELIM);
	  configs_sb.append(Utils.encToURL(str));
	}
      }
    }

    return "RTRugPlotPanel" +                                         BundlesDT.DELIM + 
           "configs="       + Utils.encToURL(configs_sb.toString()) + BundlesDT.DELIM +
           "closest="       + interactiveClosestPoint()             + BundlesDT.DELIM +
           "color="         + varyColor()                           + BundlesDT.DELIM +
	   "duration="      + modelDuration()                       + BundlesDT.DELIM +
	   "timemarkers="   + drawTimeMarkers()                     + BundlesDT.DELIM +
	   "nolabels="      + hideLabels()                          + BundlesDT.DELIM +
	   "width="         + pointWidth();
  }

  /**
   * Adjust the configuration of this component based on the specified configuration
   * string.
   *
   *@param str configuration string
   */
  @Override
  public void         setConfig    (String str) { 
    StringTokenizer st = new StringTokenizer(str, BundlesDT.DELIM);
    if (st.nextToken().equals("RTRugPlotPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not A RTRugPlotPanel");
    while (st.hasMoreTokens()) {
      String type_value = st.nextToken();
      String type       = type_value.substring(0,type_value.indexOf("="));
      String value      = type_value.substring(type_value.indexOf("=")+1,type_value.length());
      if        (type.equals("configs"))     {
        StringTokenizer st2 = new StringTokenizer(Utils.decFmURL(value), BundlesDT.DELIM);
	while (st2.hasMoreTokens()) {
	  Config cfg     = new Config();
	  String cfg_str = Utils.decFmURL(st2.nextToken()); StringTokenizer st_cfg = new StringTokenizer(cfg_str, ",");
	  while (st_cfg.hasMoreTokens()) {
	    cfg.x       = Integer.parseInt(st_cfg.nextToken());
	    cfg.y       = Integer.parseInt(st_cfg.nextToken());
	    cfg.w       = Integer.parseInt(st_cfg.nextToken());
	    cfg.h       = Integer.parseInt(st_cfg.nextToken());
            cfg.x_axis  = Utils.decFmURL(st_cfg.nextToken());
            cfg.y_axis  = Utils.decFmURL(st_cfg.nextToken());
            cfg.y2_axis = Utils.decFmURL(st_cfg.nextToken());
            cfg.x_scale = Utils.decFmURL(st_cfg.nextToken());
            cfg.y_scale = Utils.decFmURL(st_cfg.nextToken());
	  }
          for (int y=cfg.y;y<cfg.y+cfg.h;y++) for (int x=cfg.x;x<cfg.x+cfg.w;x++) configs[y][x] = cfg;
	}
      } else if (type.equals("closest"))     { interactiveClosestPoint(value.toLowerCase().equals("true"));
      } else if (type.equals("color"))       { varyColor(value.toLowerCase().equals("true"));
      } else if (type.equals("duration"))    { modelDuration(value.toLowerCase().equals("true"));
      } else if (type.equals("timemarkers")) { drawTimeMarkers(value.toLowerCase().equals("true"));
      } else if (type.equals("nolabels"))    { hideLabels(value.toLowerCase().equals("true"));
      } else if (type.equals("width"))       { pointWidth(value);
      } else System.err.println("Do Not Understand Type Value \"" + type_value + "\" For RTRugPlotPanel Config");
    }
  }

  /**
   * GUI component implementing the interactive component layer.
   */
  public class RTRugPlotComponent extends RTComponent {
    /**
     * 
     */
    private static final long serialVersionUID = 3001791924171111238L;

    /**
     * Copy a screenshot of the current rendering to the clipboard. Does not
     * seem to work across platforms.
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
     * Return all the shapes in the current rendering.
     *
     *@return set of rendered shapes
     */
    public Set<Shape>      allShapes()                     {
      Set<Shape> shapes = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return shapes;
      return myrc.geom_to_buns.keySet();
    }

    /**
     * Return all the shapes associated with the specified bundles.
     *
     *@param bundles bundles to map against
     *
     *@return associted shapes
     */
    public Set<Shape>  shapes(Set<Bundle> bundles) {
      Set<Shape> shapes = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return shapes;
      Iterator<Bundle> it = bundles.iterator();
      while (it.hasNext()) { shapes.addAll(myrc.bun_to_geoms.get(it.next())); }
      return shapes;
    }

    /**
     * Return the bundles for a specific shape. Note that the shape must have been returned by this component - i.e.,
     * this method does not handle generic shapes.
     *
     *@param shape previously returned shape to map against
     *
     *@return set of associated bundles
     */
    public Set<Bundle> shapeBundles(Shape shape)       { 
      Set<Bundle> set = new HashSet<Bundle>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      if (myrc.geom_to_buns.containsKey(shape)) return myrc.geom_to_buns.get(shape);
      return set; }

    /**
     * Find the rendered shapes that overlapp with the specified shapes.
     *
     *@param to_check shape to test against... can be generic
     *
     *@return rendered shapes that overlap
     */
    public Set<Shape>  overlappingShapes(Shape to_check)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      Iterator<Shape> it = myrc.geom_to_buns.keySet().iterator();
      while (it.hasNext()) {
        Shape shape = it.next();
        if (Utils.genericIntersects(shape, to_check)) set.add(shape);
      }
      return set; }

    /**
     * Return the shape(s) that contains the specified coordinate.  Not sure if this is needed anymore...
     * 
     *@param x x-coordinate
     *@param y y-coordinate
     *
     *@return set of shapes containing xy
     */
    public Set<Shape>  containingShapes(int x, int y)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      Iterator<Shape> it = myrc.geom_to_buns.keySet().iterator();
      while (it.hasNext()) {
        Shape shape = it.next();
        if (shape.contains(x,y)) set.add(shape);
      }
      return set; }

    /**
     * Return the shape associated with the 0th order highlights (directly under the mouse).
     *
     *@param x mouse x coordinate
     *@param y mouse y coordinate
     *
     *@return shape under the mouse
     */
    public Shape getZeroOrderShape(int x, int y) { return new Rectangle2D.Double(x-2,y-2,5,5); }

    /**
     * Return the shape associated with the 1st order highlights (near the mouse)
     *
     *@param x mouse x coordinate
     *@param y mouse y coordinate
     *
     *@return shape near the mouse
     */
    public Shape getFirstOrderShape(int x, int y) { return new Rectangle2D.Double(x-5,y-5,11,11); }

    /**
     * Return the shape associated with the 2nd order highlights (a little further from the mouse)
     *
     *@param x mouse x coordinate
     *@param y mouse y coordinate
     *
     *@return shape further from the mouse
     */
    public Shape getSecondOrderShape(int x, int y) { return new Rectangle2D.Double(x-10,y-10,21,21); }

    boolean no_mapping_error = true;

    /**
     * Create the render context for the current view.  Each render context contains a unique
     * render ID so that it can be terminated if the render becomes out-of-date.
     *
     *@param  id render id
     *
     *@return    The render context representing the current view based on the dataset and
     *           the GUI configuration.
     */
    public RTRenderContext render(short id) {
      clearNoMappingSet(); if (no_mapping_error) { no_mapping_error = false; System.err.println("**\n** RTRugPlot:  Probably Not Updating The No Map Set...\n**"); }
      Bundles bs       = getRenderBundles();
      String  count_by = getRTParent().getCountBy(),
              color_by = getRTParent().getColorBy();
      if (bs != null) {
        RenderContext myrc = new RenderContext(id, bs, count_by, color_by, getWidth(), getHeight(), pointWidth(), varyColor(), modelDuration(), hideLabels());
        return myrc;
      } else return null;
    }
    
    /**
     * Mask for keeping a 32-bit number from sign extending when transferred to a long value.
     */
    final long LONGMASK = 0x00ffffffffL;

    /**
     * Class containin information on the  existing rendering.  Class also
     * constructs the rendering based on settings from the panel-level class.
     */
    public class RenderContext extends RTRenderContext {
      /**
       * Dataset to render
       */
      Bundles bs; 

      /**
       * Width of the rendering in pixels
       */
      int     rc_w, 

      /**
       * Height of the rendering in pixels
       */
              rc_h; 

      /**
       * How elements should be counted - determines the width of the lines (future...)
       */
      String  count_by, 

      /**
       * Which keymakers should be used to color the lines
       */
              color_by; 

      /**
       * Vary the color of the plots
       */
      boolean vary_color,
      /**
       * Model the duration of linear mappings
       */
              model_duration,

      /**
       * Hide the labels and axes to give the visualization a cleaner look with
       * more drawing area and less distractions
       */
              hide_labels;

      /**
       * Width of each plot
       */
      PTWIDTH                pt_width;

      /**
       * Lookup to convert geometrical figures to their underlying records
       */
      Map<Shape,Set<Bundle>> geom_to_buns = new HashMap<Shape,Set<Bundle>>();

      /**
       * Lookup to convert records to the geometrical figures
       */
      Map<Bundle,Set<Shape>> bun_to_geoms = new HashMap<Bundle,Set<Shape>>();

      /**
       * Construct the rendering variables for this rendering.
       *
       *@param id             render id (used to abort superceded renders)
       *@param bs             dataset to render
       *@param count_by       how to count elements
       *@param color_by       coloring method for shapes int the scene
       *@param w              width of rendering in pixels
       *@param h              height of rendering in pixels
       *@param pt_width       width of the plots
       *@param vary_color     vary the color of the plots
       *@param model_duration model the duration of bundles in linear time axes
       *@param hide_labels    hide labels to provide a cleaner look
       */
      public RenderContext(short id, Bundles bs, String count_by, String color_by, int w, int h, PTWIDTH pt_width, boolean vary_color, boolean model_duration, boolean hide_labels) {
        render_id = id; this.bs = bs; this.count_by = count_by; this.color_by = color_by; this.rc_w = w; this.rc_h = h; 
        this.pt_width       = pt_width;
        this.vary_color     = vary_color;
        this.model_duration = model_duration;
        this.hide_labels    = hide_labels;

	// Calculate the dimensions
	calculatePlotDimensions(null);

	// Field information
	BundlesG globals = bs.getGlobals();

        // Thread each axis
	// - Determine which axes need to be rendered
        Set<String> axes =  new HashSet<String>();
        for (int y=0;y<configs.length;y++) for (int x=0;x<configs[y].length;x++) {
	  if (configs[y][x] != null) { axes.add(Utils.encToURL(configs[y][x].x_axis));
	                               if (configs[y][x].y2_axis.equals(BundlesDT.COUNT_BY_NONE)) { axes.add(Utils.encToURL(configs[y][x].y_axis)); } 
				       else { axes.add(Utils.encToURL(configs[y][x].y_axis) + BundlesDT.DELIM + Utils.encToURL(configs[y][x].y2_axis)); } } }

	// - Provide a thread for each one
        Iterator<String> it = axes.iterator();
	while (it.hasNext()) { String axis = it.next(); AxisThread axis_thread = new AxisThread(axis); axis_thread_lu.put(axis, axis_thread);  axis_thread.start(); }
	// - Join the threads
	it = axis_thread_lu.keySet().iterator(); while (it.hasNext()) { try { axis_thread_lu.get(it.next()).join(); } catch (InterruptedException ie) { } }

        // Calculate the axis mapping -- threaded of course...
        for (int y=0;y<configs.length;y++) for (int x=0;x<configs[y].length;x++) {
	  if (configs[y][x] != null) { String axis = Utils.encToURL(configs[y][x].x_axis), scale = configs[y][x].x_scale;
                                       String key  = axis + " " + scale; 
                                       if (axis_mapper_lu.containsKey(key) == false) { axis_mapper_lu.put(key, new AxisMapperThread(axis,scale)); axis_mapper_lu.get(key).start(); }
                                       config_x_lu.put(configs[y][x], key);
	                               if (configs[y][x].y2_axis.equals(BundlesDT.COUNT_BY_NONE)) {
				         axis = Utils.encToURL(configs[y][x].y_axis); scale = configs[y][x].y_scale;
                                         key = axis + " " + scale;
                                         if (axis_mapper_lu.containsKey(key) == false) { axis_mapper_lu.put(key, new AxisMapperThread(axis,scale)); axis_mapper_lu.get(key).start(); }
                                         config_y_lu.put(configs[y][x], key);
                                       } else {
				         axis = Utils.encToURL(configs[y][x].y_axis) + BundlesDT.DELIM + Utils.encToURL(configs[y][x].y2_axis); scale = configs[y][x].y_scale;
                                         key = axis + " " + scale;
                                         if (axis_mapper_lu.containsKey(key) == false) { axis_mapper_lu.put(key, new AxisMapperThread(axis,scale)); axis_mapper_lu.get(key).start(); }
                                         config_y_lu.put(configs[y][x], key);
				       }
          } }
          // - Join the threads
          it = axis_mapper_lu.keySet().iterator(); while (it.hasNext()) { try { axis_mapper_lu.get(it.next()).join(); } catch (InterruptedException ie) { } }

      }

      /**
       * Lookup table to find the thread responsible for handling each config's rendering
       */
      Map<Config,ConfigThread> config_thread_lu = new HashMap<Config,ConfigThread>();

      /**
       * Class to parallellize the configuration of each xy plots rendering method.
       */
      class ConfigThread extends Thread {
        Config config; int x_base, y_base; int graph_w, graph_h, txt_h; BundlesCounterContext counter_context;

	/**
	 * Lookup to convert a string to the x component -- string will look like "x,y"
	 */
	Map<String,Integer> key_to_x = new HashMap<String,Integer>(),

        /**
	 * Lookup to convert a string to the y component -- string will look like "x,y"
	 */
	                    key_to_y = new HashMap<String,Integer>();

	/**
	 * Construct the thread by calculating the geometry and allocating the counter context
	 */
        public ConfigThread(Config config, int txt_h) { 
          if (hide_labels) txt_h = 2;
	  this.config = config; 
	  Rectangle2D clip = config_to_clip.get(config);
	  x_base  = (int) (clip.getX()      + 2 + txt_h);
	  y_base  = (int) (clip.getMaxY()   - 2 - txt_h);
	  graph_w = (int) (clip.getWidth()  - 4 - txt_h);
	  graph_h = (int) (clip.getHeight() - 4 - txt_h);
	  counter_context = new BundlesCounterContext(bs, count_by, color_by);
	}

	/**
	 * Render the xy component (I believe this has to be done in serial)
	 *@param g2d graphics primitive
	 */
	public void render(Graphics2D g2d) {
          // Draw the axis
          if (hide_labels) {
            g2d.setColor(RTColorManager.getColor("background", "nearbg"));  g2d.fill(config_to_clip.get(config));
            g2d.setColor(RTColorManager.getColor("background", "default")); g2d.fillRect(x_base, y_base - graph_h, graph_w + 1, graph_h + 1);
          } else {
            g2d.setColor(RTColorManager.getColor("axis", "minor"));
            g2d.drawLine(x_base, y_base, x_base + graph_w, y_base);
            g2d.drawLine(x_base, y_base, x_base,           y_base - graph_h);
          }

          g2d.setColor(RTColorManager.getColor("data", "default"));

          Iterator<String> it = counter_context.binIterator();
	  while (it.hasNext()) {
            String key = it.next(); int sx = key_to_x.get(key), sy = key_to_y.get(key);

	    // Modulate the color
            if (vary_color) g2d.setColor(counter_context.binColor(key));

	    // Determine the width and the shape
            Shape shape; double width = 1.0;
            switch (pt_width) {
              case VARY:     width = counter_context.totalNormalized(key) * 20.0 + 1.8;
                             shape = new Ellipse2D.Float((float) (sx-width/2),(float) (sy-width/2), (float) width, (float) width);
                             g2d.draw(shape); break;
              case VARY_LOG: double    total   = counter_context.total(key);
                             if      (total == 1.0) width = 1.8;
                             else if (total >  1.0) width = 1.8 + 20.0*Math.log(total)/Math.log(counter_context.totalMaximum());
                             shape = new Ellipse2D.Float((float) (sx-width/2),(float) (sy-width/2),(float) width,(float) width);
                             g2d.draw(shape); break;
              case SMALL:    shape = new Rectangle2D.Float(sx, sy, 1, 1); g2d.fill(shape); break;
              case LARGE:    shape = new Ellipse2D.Float(sx-2,sy-2,5,5);  g2d.fill(shape); break;
	      case MEDIUM:  
              default:       shape = new Rectangle2D.Float(sx, sy, 2, 2); g2d.fill(shape); break;
            }
            
            // Wire the shape to rec lookups
            geom_to_buns.put(shape, new HashSet<Bundle>());
            geom_to_buns.get(shape).addAll(counter_context.getBundles(key));

            Iterator<Bundle> it_bun = geom_to_buns.get(shape).iterator();
            while (it_bun.hasNext()) {
              Bundle bundle = it_bun.next();
              if (bun_to_geoms.containsKey(bundle) == false) bun_to_geoms.put(bundle, new HashSet<Shape>());
              bun_to_geoms.get(bundle).add(shape);
            }
	  }
	}

	/**
	 * Turn bundles into coordinates based on their presence within each axis
	 */
        public void run() {
          // Make the keys
	  String x_axis_key = Utils.encToURL(config.x_axis), y_axis_key;
	  if (config.y2_axis.equals(BundlesDT.COUNT_BY_NONE)) { y_axis_key = Utils.encToURL(config.y_axis); }
	  else { y_axis_key = Utils.encToURL(config.y_axis) + BundlesDT.DELIM + Utils.encToURL(config.y2_axis); }
	  String x_axis_scale_key = x_axis_key + " " + config.x_scale, y_axis_scale_key = y_axis_key + " " + config.y_scale;

          // Choose the axis with the lesser number of bundles...
	  // - Get the individual state pieces
          AxisThread        x_axis_thread   = axis_thread_lu.get(x_axis_key),       y_axis_thread   = axis_thread_lu.get(y_axis_key);
	  AxisMapperThread  x_mapper_thread = axis_mapper_lu.get(x_axis_scale_key), y_mapper_thread = axis_mapper_lu.get(y_axis_scale_key);
	  // - Choose the axis with a smaller number of records
          Iterator<Bundle> it_bun;
	  if (x_axis_thread.bundle_lu.keySet().size() < y_axis_thread.bundle_lu.keySet().size()) it_bun = x_axis_thread.bundle_lu.keySet().iterator();
	  else                                                                                   it_bun = y_axis_thread.bundle_lu.keySet().iterator();
	  // Go through the bundles -- if the individual bundle exists in both axis threads, map it into the counter context via the mappers
          while (it_bun.hasNext()) {
	    Bundle bundle = it_bun.next();
	    if (x_axis_thread.bundle_lu.containsKey(bundle) && y_axis_thread.bundle_lu.containsKey(bundle)) {
	      Iterator<Long> itl;
	      long xs[] = new long[x_axis_thread.bundle_lu.get(bundle).size()]; itl = x_axis_thread.bundle_lu.get(bundle).iterator(); for (int i=0;i<xs.length;i++) xs[i] = itl.next();
	      long ys[] = new long[y_axis_thread.bundle_lu.get(bundle).size()]; itl = y_axis_thread.bundle_lu.get(bundle).iterator(); for (int i=0;i<ys.length;i++) ys[i] = itl.next();

              for (int yi=0;yi<ys.length;yi++) {
	        int sy = (int) (y_base - graph_h * y_mapper_thread.w_to_n.get(ys[yi]));
	        for (int xi=0;xi<xs.length;xi++) {
                  int sx = (int) (x_base + graph_w * x_mapper_thread.w_to_n.get(xs[xi]));
                  String key = sx + "," + sy;
		  key_to_x.put(key, sx);
		  key_to_y.put(key, sy);
                  counter_context.count(bundle, key);
                }
	      }
	    }
	  }
	}
      }

      /**
       * Lookups from the config to the x coordinate
       */
      Map<Config,String>           config_x_lu = new HashMap<Config,String>(),

      /**
       * Lookups from the config to the y coordinate
       */
                                   config_y_lu = new HashMap<Config,String>();

      /**
       * Lookup to find a thread (and the results) for an axis mapper
       */
      Map<String,AxisMapperThread> axis_mapper_lu = new HashMap<String,AxisMapperThread>();

      /**
       * Simple thread to calculate the mapping from world coordinates to normalized coordinates
       */
      class AxisMapperThread extends Thread {
        String           axis, scale; Map<Long,Double> w_to_n;
	public AxisMapperThread(String axis, String scale) { this.axis = axis; this.scale = scale; }
        public void run() { AxisThread axis_thread = axis_thread_lu.get(axis); w_to_n = AxisMapper.calculateMapping(scale, axis_thread.values, axis_thread.min, axis_thread.max); }
      }

      /**
       * Lookup to find a thread (and the results) for an axis
       */
      Map<String,AxisThread>       axis_thread_lu = new HashMap<String,AxisThread>();

      /**
       * Thread to accumulate values for a single axis
       */
      class AxisThread extends Thread {
        // Axis name
        String                axis; 
	// List of world coordinates
	List<Long>            values     = new ArrayList<Long>(); 
	// World coordinates as a set -- used to make the values list non-duplicative
        Set<Long>             values_set = new HashSet<Long>();
	// Lookup to determine which world coordinates belong to a specific record
	Map<Bundle,Set<Long>> bundle_lu  = new HashMap<Bundle,Set<Long>>();
	// Lookup to convert the world coordinate to the string
        Map<Long,String>      str_lu     = new HashMap<Long,String>();
	// Minimum value on the axis
	long                  min = Long.MAX_VALUE, 
        // Maximum value on the axis
	                      max = Long.MIN_VALUE;
	// Key makers for the axis
        KeyMaker              km,
	// Secondary key maker for the axis
                              km_sub;

	/**
	 * Simple constructure -- just copy the axis field
	 */
        public AxisThread(String axis) { this.axis = axis; }

	/**
	 * Thread method to convert each bundle to its world coordinate and manage the state of the axis
	 */
        public void run() {
          final long LONG_MASK = 0x00ffffffffL;
          // 
          // Determine if the axis is a double field
          //
          if (axis.indexOf(BundlesDT.DELIM) >= 0) {
            String prime = Utils.decFmURL(axis.substring(0,axis.indexOf(BundlesDT.DELIM))),
	           sub   = Utils.decFmURL(axis.substring(axis.indexOf(BundlesDT.DELIM)+1,axis.length()));
            // Go through the tablets
            Iterator<Tablet> it_tablet = bs.tabletIterator();
	    while (it_tablet.hasNext() && currentRenderID() == getRenderID()) {
	      Tablet tablet = it_tablet.next();
              // If the tablet completes the field, go through the bundles
	      if (KeyMaker.tabletCompletesBlank(tablet, prime) && KeyMaker.tabletCompletesBlank(tablet,sub)) {
                Iterator<Bundle> it_bundle = tablet.bundleIterator();
                // Make the KeyMakers
	        km     = new KeyMaker(tablet, prime); km_sub = new KeyMaker(tablet, sub);
	        while (it_bundle.hasNext() && currentRenderID() == getRenderID()) {
                  // Get the record
	          Bundle bundle = it_bundle.next(); bundle_lu.put(bundle, new HashSet<Long>());
                  int    keys[] = km.intKeys(bundle);    int    keys_sub[] = km_sub.intKeys(bundle);
                  String strs[] = km.stringKeys(bundle); String strs_sub[] = km_sub.stringKeys(bundle);
                  
                  for (int i=0;i<keys.length;i++) {
                    for (int j=0;j<keys_sub.length;j++) {
                      long   l = ((keys[i] & LONG_MASK)<<32L) | (keys_sub[j] & LONG_MASK);
                      String s = strs[i] + BundlesDT.DELIM + strs_sub[j];
                      bundle_lu.get(bundle).add(l); str_lu.put(l, s); add(l);
                    }
                  }
                }
              }
            }
	  } else                                  {
          // 
          // Otherwise, it's just a single field
          //
            String field = Utils.decFmURL(axis);
            // Go through the tablets
            Iterator<Tablet> it_tablet = bs.tabletIterator();
	    while (it_tablet.hasNext() && currentRenderID() == getRenderID()) {
	      Tablet tablet = it_tablet.next();
              // If the tablet completes the field, go through the bundles
	      if (KeyMaker.tabletCompletesBlank(tablet, field)) {
                Iterator<Bundle> it_bundle = tablet.bundleIterator();
                // Make the KeyMaker
	        km = new KeyMaker(tablet, field);
	        while (it_bundle.hasNext() && currentRenderID() == getRenderID()) {
                  // Get the record
	          Bundle bundle = it_bundle.next(); bundle_lu.put(bundle, new HashSet<Long>());
                  // Split for time-based keys
                  if (km.linearTime()) {
                    long key = km.timeStampKey(bundle); bundle_lu.get(bundle).add(key); add(key);
                    // Model duration if appropriate
                    if (model_duration) { key = km.endTimeStampKey(bundle); bundle_lu.get(bundle).add(key); add(key); }
                  } else {
                    long keys[] = Utils.toLongs(km.intKeys(bundle)); String strs[] = km.stringKeys(bundle);
                    for (int i=0;i<keys.length;i++) { bundle_lu.get(bundle).add(keys[i]); str_lu.put(keys[i], strs[i]); add(keys[i]); }
                  }
                }
              }
	    }
	  }
	}

        /**
         * Add a long to the list if it's new... update other state.
         *
         *@param l long to add
         */
        private void add(long l) { if (values_set.contains(l) == false) { values_set.add(l); values.add(l); if (l < min) min = l; if (l > max) max = l; } }
      }

      /**
       * Return the height of this rendering in pixels
       *
       *@return height in pixels
       */
      public int           getRCHeight() { return rc_h; }

      /**
       * Return the width of this rendering in pixels
       *
       *@return width in pixels
       */
      public int           getRCWidth()  { return rc_w; }

      /**
       * Rendered version of this configuration
       */
      BufferedImage base_bi = null;

      /**
       * Render the visualization and return it as a {@link BufferedImage}
       *
       *@return rendered image
       */
      public BufferedImage getBase() { 
        if (base_bi == null) {
	 Graphics2D g2d = null;
	 try {
	  // Create the image
          base_bi = new BufferedImage(rc_w, rc_h, BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) base_bi.getGraphics();
          g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	  RTColorManager.renderVisualizationBackground(base_bi, g2d);

          // Create the geometry
	  if (hide_labels == false) calculatePlotDimensions(g2d);

          int txt_h = Utils.txtH(g2d, "0123456789");

          // Thread each XY plot
          Iterator<Config> it_config = config_x_lu.keySet().iterator();
          while (it_config.hasNext()) { Config config = it_config.next(); config_thread_lu.put(config, new ConfigThread(config, txt_h)); config_thread_lu.get(config).start(); }
	  // - Join the threads
	  it_config = config_thread_lu.keySet().iterator(); while (it_config.hasNext()) { 
	    try { 
	      Config config = it_config.next();
	      config_thread_lu.get(config).join(); 
	      config_thread_lu.get(config).render(g2d);
	    } catch (InterruptedException ie) { } 
	  }
         } finally { if (g2d != null) g2d.dispose(); } // Cleanup...
        }
        return base_bi;
      }

      /**
       * Map to convert geometry to the config
       */
      Map<Rectangle2D, Config> clip_to_config = new HashMap<Rectangle2D, Config>();

      /**
       * Map to convert config's to their geometry
       */
      Map<Config, Rectangle2D> config_to_clip = new HashMap<Config, Rectangle2D>();

      /**
       * Render a simplified version of the view that shows the layout of each plot and the axes.
       *
       *@param g2d    graphics primitive -- if null, just calculate the dimensions
       */
      protected void calculatePlotDimensions(Graphics2D g2d) {
        // Find the mins and maxes
        int x_pos0=0, y_pos0=0, x_pos1=1, y_pos1=1; boolean first = true;
        for (int y=0;y<configs.length;y++) for (int x=0;x<configs[y].length;x++) {
	  Config cfg = configs[y][x];
          if (cfg != null) {
	    if (first) { first = false; x_pos0 = cfg.x; y_pos0 = cfg.y; x_pos1 = cfg.x + cfg.w; y_pos1 = cfg.y + cfg.h;
	    } else     {
	      if (x_pos0 > cfg.x)         x_pos0 = cfg.x;
	      if (y_pos0 > cfg.y)         y_pos0 = cfg.y;
	      if (x_pos1 < cfg.x + cfg.w) x_pos1 = cfg.x + cfg.w;
	      if (y_pos1 < cfg.y + cfg.h) y_pos1 = cfg.y + cfg.h;
	    }
	  }
        }
	// Figure out the geometry
        int cell_w = rc_w / (x_pos1 - x_pos0), cell_h = rc_h / (y_pos1 - y_pos0); if (cell_w < 10) cell_w = 10; if (cell_h < 10) cell_h = 10;

	// Plot out the rectangles
        for (int y=0;y<configs.length;y++) for (int x=0;x<configs[y].length;x++) {
	  Config cfg = configs[y][x];
	  if (cfg != null && cfg.x == x && cfg.y == y) {
	    Rectangle2D clip = new Rectangle2D.Double((cfg.x - x_pos0)*cell_w, (cfg.y - y_pos0)*cell_h, cell_w * cfg.w, cell_h * cfg.h);
	    config_to_clip.put(cfg, clip); clip_to_config.put(clip, cfg);
	    if (g2d != null) {
	      g2d.setColor(RTColorManager.getColor("axis", "major"));
	      g2d.draw(clip);
	      String str;
	      str = cfg.x_axis + " (" + cfg.x_scale + ")";
              g2d.drawString(str, (int) (clip.getCenterX() - Utils.txtW(g2d, str)/2), (int) (clip.getMaxY() - 2));
	      str = cfg.y_axis; if (cfg.y2_axis != null && cfg.y2_axis.equals(BundlesDT.COUNT_BY_NONE) == false) str += " " + cfg.y2_axis;
	      str += " (" + cfg.y_scale + ")";
	      Utils.drawRotatedString(g2d, str, (int) (clip.getX() + Utils.txtH(g2d, str)), (int) (clip.getCenterY() + Utils.txtW(g2d, str)/2));
	    }
	  }
        }
      }
    }
  }
}

