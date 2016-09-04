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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.image.BufferedImage;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.BundlesDT;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;
import racetrack.util.Utils;
import racetrack.visualization.ColorScale;
import racetrack.visualization.RTColorManager;

/**
 * Implements a scatterplot MDS display
 *
 *@author  D. Trimm
 *@version 0.1
 */
public class RTMDSPanel extends RTPanel {
  /**
   *
   */
  private static final long serialVersionUID = -6233227789311942269L;

  /**
   * Small width radio button menu item
   */
  JRadioButtonMenuItem  width_small_rbmi,
  /**
   * Medium width radio button menu item (default)
   */
                        width_medium_rbmi,
  /**
   * Large width radio button menu item
   */
                        width_large_rbmi,
  /**
   * Vary width radio button menu item
   */
			width_vary_rbmi;
  /**
   * Vary color checkbox menu item
   */
  JCheckBoxMenuItem     vary_color_cbmi;

  /**
   * Construct the correlation panel with the specified parent.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt application parent
   */
  public RTMDSPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      { 
    super(win_type,win_pos,win_uniq,rt);   
    // Make the GUI
    add("Center",  component = new RTMDSComponent());

    // Update the menu
    JMenuItem mi;
    getRTPopupMenu().add(mi                = new JMenuItem("Execute MDS..."));                        mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { executeMDS(); } } );
    ButtonGroup bg = new ButtonGroup();
    getRTPopupMenu().add(width_small_rbmi  = new JRadioButtonMenuItem("Fixed Width (Small)"));        bg.add(width_small_rbmi);
    getRTPopupMenu().add(width_medium_rbmi = new JRadioButtonMenuItem("Fixed Width (Medium)", true)); bg.add(width_medium_rbmi);
    getRTPopupMenu().add(width_large_rbmi  = new JRadioButtonMenuItem("Fixed Width (Large)"));        bg.add(width_large_rbmi);
    getRTPopupMenu().add(width_vary_rbmi   = new JRadioButtonMenuItem("Vary Width"));                 bg.add(width_vary_rbmi);
    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(vary_color_cbmi   = new JCheckBoxMenuItem("Vary Color"));

    // Add the listeners
    defaultListener(width_small_rbmi);
    defaultListener(width_medium_rbmi);
    defaultListener(width_large_rbmi);
    defaultListener(width_vary_rbmi);
    defaultListener(vary_color_cbmi);

    // Assign a position to all bundles
    Bundles root = rt.getRootBundles();
    Iterator<Tablet> it_tab = root.tabletIterator(); while (it_tab.hasNext()) { Tablet tablet = it_tab.next();
      Iterator<Bundle> it_bun = tablet.bundleIterator(); while (it_bun.hasNext()) { Bundle bundle = it_bun.next();
        bundle_to_wx.put(bundle, Math.random());
        bundle_to_wy.put(bundle, Math.random());
      }
    }
    wx_min = 0.0; wx_max = 1.0;
    wy_min = 0.0; wy_max = 1.0;
  }

  /**
   * Display the MDS dialog -- dialog will then enable execution of the MDS algorithm
   */
  protected void executeMDS() { new MDSDialog(); }

  /**
   * Dialog to enable user to specify MDS settings / MDS algorithm.
   */
  class MDSDialog extends JDialog {
    /**
     * Construct the dialog including a method for the user to describe the feature vector comparison as
     * well as the specific type of MDS algorithm.
     */
    public MDSDialog() {
      super(getRTParent(), "MDS Dialog", true);

      setVisible(true);
    }
  }

  /**
   * Determines if the render should vary by color.
   *
   *@return true if vary by color
   */
  public boolean varyColor() { return vary_color_cbmi.isSelected(); }

  /**
   * Set the vary color option.
   *
   *@param b new vary color setting
   */
  public void varyColor(boolean b) { vary_color_cbmi.setSelected(b); }

  /**
   * Point width enumeration
   */
  enum PTWIDTH { VARY, SMALL, MEDIUM, LARGE };

  /**
   * Determine the point width of the render.
   *
   *@return point width
   */
  public PTWIDTH pointWidth() { if      (width_small_rbmi. isSelected()) return PTWIDTH.SMALL;
                                else if (width_medium_rbmi.isSelected()) return PTWIDTH.MEDIUM;
                                else if (width_large_rbmi. isSelected()) return PTWIDTH.LARGE;
				else if (width_vary_rbmi.  isSelected()) return PTWIDTH.VARY;
				else                                     return PTWIDTH.MEDIUM; }

  /**
   * Set the point width for the render
   *
   *@param s string-based representation of the point width
   */
  public void pointWidth(String s) { if      (s.equals("" + PTWIDTH.SMALL))    width_small_rbmi.  setSelected(true); 
                                     else if (s.equals("" + PTWIDTH.MEDIUM))   width_medium_rbmi. setSelected(true); 
                                     else if (s.equals("" + PTWIDTH.LARGE))    width_large_rbmi.  setSelected(true); 
				     else if (s.equals("" + PTWIDTH.VARY))     width_vary_rbmi.   setSelected(true);
				     else                                      width_medium_rbmi. setSelected(true); } 
  /**
   * Correlates record to its x world coordinate
   */	
  Map<Bundle,Double>    bundle_to_wx = new HashMap<Bundle,Double>(),
  /**
   * Correlates record to its y world coordinate
   */
                        bundle_to_wy = new HashMap<Bundle,Double>();
  /**
   * X world coordinate minimum
   */
  double                wx_min = 0.0,
  /**
   * X world coordinate maximum
   */
                        wx_max = 1.0,
  /**
   * Y world coordinate minimum
   */
			wy_min = 0.0,
  /**
   * Y world coordinate maximum
   */
			wy_max = 1.0;

  /**
   * Recalculate the mins and maxs for the world coordinates
   */
  protected void recalculateWorldMinsAndMaxs() {
    if (bundle_to_wx.keySet().size() > 0) {
      Iterator<Bundle> it = bundle_to_wx.keySet().iterator();
      Bundle bundle = it.next();
      wx_min = wx_max = bundle_to_wx.get(bundle);
      wy_min = wy_max = bundle_to_wy.get(bundle);
      while (it.hasNext()) {
        bundle = it.next();
        double wx = bundle_to_wx.get(bundle); if (wx < wx_min) wx_min = wx; if (wx > wx_max) wx_max = wx;
        double wy = bundle_to_wy.get(bundle); if (wy < wy_min) wy_min = wy; if (wy > wy_max) wy_max = wy;
      }
    }
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "mds"; }

  /**
   * Get the configuration for this panel.  Planned to be used for bookmarking.
   *
   *@return string representation of this configuration
   */
  public String       getConfig    ()           { 
    return "RTMDSPanel"                                    + BundlesDT.DELIM +
           "width="     + pointWidth()                     + BundlesDT.DELIM +
	   "vcolor="    + (varyColor() ? "true" : "false");
  }

  /**
   * Set the configuration for this panel.  Could be used to recall bookmarks.
   *
   *@param str string representation for new configuration
   */
  public void         setConfig    (String str) {
    StringTokenizer st = new StringTokenizer(str, BundlesDT.DELIM);
    if (st.nextToken().equals("RTMDSPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not A RTMDSPanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st2.hasMoreTokens() ? st2.nextToken() : "";
      if        (type.equals("width"))  { pointWidth(value);
      } else if (type.equals("vcolor")) { varyColor(value.toLowerCase().equals("true"));
      } else throw new RuntimeException("Do Not Understand Type Value Pair \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * {@link JComponent} implementing the correlation matrix.
   */
  public class RTMDSComponent extends RTComponent {
    private static final long serialVersionUID = 123639511323818321L;
    @Override
    public Set<Shape>      allShapes()                     {
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      set.addAll(myrc.geom_to_skey.keySet());
      return set; }
    @Override
    public Set<Shape>  shapes(Set<Bundle> bundles) {
      Set<Shape> shapes = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return shapes;
      Iterator<Bundle> it = bundles.iterator();
      while (it.hasNext()) {
        Bundle bundle = it.next();
	shapes.add(myrc.skey_to_geom.get(myrc.bundle_to_skey.get(bundle)));
      }
      return shapes; }
    @Override
    public Set<Bundle> shapeBundles(Shape shape)       { 
      Set<Bundle> set = new HashSet<Bundle>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      String skey = myrc.geom_to_skey.get(shape);
      if (skey != null) { set.addAll(myrc.skey_to_bundles.get(skey)); }
      return set; }
    @Override
    public Set<Shape>  overlappingShapes(Shape shape)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      Iterator<Ellipse2D> it = myrc.geom_to_skey.keySet().iterator();
      while (it.hasNext()) {
        Ellipse2D ellipse = it.next();
	if (Utils.genericIntersects(ellipse, shape)) set.add(ellipse);
      }
      return set; }
    public Set<Shape>  containingShapes(int x, int y)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      Iterator<Ellipse2D> it = myrc.geom_to_skey.keySet().iterator();
      while (it.hasNext()) {
        Ellipse2D ellipse = it.next();
	if (ellipse.contains(x,y)) set.add(ellipse);
      }
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
      // Basics...
      Bundles    bs        = getRenderBundles();
      String     count_by  = getRTParent().getCountBy(),
                 color_by  = getRTParent().getColorBy();
      // Create the render context based on the user's parameters
      RenderContext myrc = new RenderContext(id, bs, count_by, color_by, pointWidth(), varyColor(), getWidth(), getHeight());
      return myrc;
    }
    
    /**
     * RenderContext implementation for the correlation matrix
     */
    public class RenderContext extends RTRenderContext {
      /**
       * Bundles/records for this rendering
       */
      Bundles bs; 

      /**
       * Width of component in pixels
       */
      int                   rc_w, 

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
                            color_by;

      /**
       * Vary color based on global color setting
       */
      boolean               vary_color;

      /**
       * Point width setting
       */
      PTWIDTH               point_width;

      /**
       * Lookup for a record to the string keys
       */
      Map<Bundle,String>      bundle_to_skey = new HashMap<Bundle,String>();

      /**
       * Lookup for a string key to the bundles
       */
      Map<String,Set<Bundle>> skey_to_bundles = new HashMap<String,Set<Bundle>>();
      
      /**
       * String key to x coordinate (saves a parse int)
       */
      Map<String,Integer>     skey_to_x = new HashMap<String,Integer>(),

      /**
       * String key to y coordinate (saves a parse int)
       */
                              skey_to_y = new HashMap<String,Integer>();

      /**
       * Lookup for a string key to the geometry
       */
      Map<String,Ellipse2D>   skey_to_geom = new HashMap<String,Ellipse2D>();

      /**
       * Lookup for a geometry to a string key
       */
      Map<Ellipse2D,String>   geom_to_skey = new HashMap<Ellipse2D,String>();

      /**
       * Local copy of the world x coordinate min
       */
      double                  my_wx_min,

      /**
       * Local copy of the world x coordinate max
       */
                              my_wx_max,

      /**
       * Local copy of the world y coordinate min
       */
			      my_wy_min,

      /**
       * Local copy of the world y coordinate max
       */
			      my_wy_max;

      /**
       * Transform a world x coordinate to its corresponding screen x coordinate.
       *
       *@param wx world x coordinate to transform
       *
       *@return screen x coordinate
       */
      protected int worldXToScreenX(double wx) { return (int) (((rc_w-6) * (wx - my_wx_min))/(my_wx_max - my_wx_min)) + 3; }

      /**
       * Transform a world y coordinate to its corresponding screen y coordinate.
       *
       *@param wy world y coordinate to transform
       *
       *@return screen y coordinate
       */
      protected int worldYToScreenY(double wy) { return (int) (((rc_h-6) * (wy - my_wy_min))/(my_wy_max - my_wy_min)) + 3; }

      /**
       * Screen counter context - used to determine color and width of points
       */
      protected BundlesCounterContext screen_counter_context;

      /**
       * Construct the rendering context for the day matrix
       * with the specified settings.
       *
       *@param id                 render id
       *@param bs                 bundles to render
       *@param count_by           how to count the record contribution to each country
       *@param color_cy           color option based on global settings
       *@param cs                 colorscale to use for the rendering
       *@param use_log_color      flag to indicate logarithmic coloring is in effect
       *@param point_width        point width setting
       *@param vary_color         vary color flag
       *@param w                  width for this render
       *@param h                  height for this render
       */
      public RenderContext(short id, Bundles bs, String count_by, String color_by, PTWIDTH point_width, boolean vary_color, int w, int h) {
        render_id = id; this.bs = bs; this.rc_w = w; this.rc_h = h;
	this.count_by           = count_by;
	this.color_by           = color_by;

	// Get the render specific settings
        this.point_width = point_width;
	this.vary_color  = vary_color;

	// Copy the global information
	my_wx_min = wx_min; my_wx_max = wx_max;
	my_wy_min = wy_min; my_wy_max = wy_max;

	// Screen counter context
        screen_counter_context = new BundlesCounterContext(bs, count_by, color_by);

	// Map the bundles to their position on the screen
	Iterator<Tablet> it_tab = bs.tabletIterator();
	while (it_tab.hasNext() && currentRenderID() == getRenderID()) {
          Tablet  tablet           = it_tab.next();
	  boolean tablet_can_count = count_by.equals(BundlesDT.COUNT_BY_BUNS) || KeyMaker.tabletCompletesBlank(tablet, count_by);
          if (tablet.hasTimeStamps() && tablet_can_count) {
            Iterator<Bundle> it_bun = tablet.bundleIterator();
	    while (it_bun.hasNext() && currentRenderID() == getRenderID()) {
	      Bundle bundle     = it_bun.next();

	      double wx         = bundle_to_wx.get(bundle),
	             wy         = bundle_to_wy.get(bundle);
              int    sx         = worldXToScreenX(wx),
	             sy         = worldYToScreenY(wy);
              String skey       = sx + "," + sy;

	      bundle_to_skey.put(bundle, skey);
	      if (skey_to_bundles.containsKey(skey) == false) {
	        skey_to_bundles.put(skey, new HashSet<Bundle>());
		skey_to_x.put(skey,sx);
		skey_to_y.put(skey,sy);
              }
	      skey_to_bundles.get(skey).add(bundle);

	      screen_counter_context.count(bundle, skey);
	    }
	  } else { addToNoMappingSet(tablet); }
	}
      }

      @Override
      public int           getRCHeight() { return rc_h; }
      @Override
      public int           getRCWidth()  { return rc_w; }
      BufferedImage base_bi = null;
      @Override
      public BufferedImage getBase() { 
        if (base_bi == null) {
	 Graphics2D g2d = null;
	 try {
	  // Create the image and prepare it for render
          base_bi         = new BufferedImage(rc_w, rc_h, BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) base_bi.getGraphics();
	  g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	  RTColorManager.renderVisualizationBackground(base_bi, g2d);

	  // Use the default color
          g2d.setColor(RTColorManager.getColor("data","default"));

	  // Go through the skeys -- i.e., "<sx>,<sy>" for example "5,9"
	  Iterator<String> it = screen_counter_context.binIterator();
	  while (it.hasNext()) {
	    String skey = it.next();
	    int    sx   = skey_to_x.get(skey),
	           sy   = skey_to_y.get(skey);

            // Modulate the size if necessary
            Ellipse2D geom;
	    
	    switch (point_width) {
	      case SMALL:  geom = new Ellipse2D.Float(sx-0,sy-0,1,1); break;
	      case LARGE:  geom = new Ellipse2D.Float(sx-2,sy-2,5,5); break;
              case VARY:   double width = screen_counter_context.totalNormalized(skey) * 5.0 + 2.0;
	                   geom = new Ellipse2D.Double(sx-width/2,sy-width/2,width,width);
	                   break;
	      case MEDIUM:
	      default:     geom = new Ellipse2D.Float(sx-1,sy-1,3,3);
	                   break;
            }

	    // Modulate the color if necessary
            if (vary_color) g2d.setColor(screen_counter_context.binColor(skey));

	    // Store the geometry lookups and render
            skey_to_geom.put(skey,geom);    
	    geom_to_skey.put(geom,skey);
	    g2d.fill(geom);
	  }
	 } finally { if (g2d != null) g2d.dispose(); }
        }
        return base_bi;
      }
    }
  }
}

