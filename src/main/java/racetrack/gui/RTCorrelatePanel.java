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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;

import java.awt.geom.Rectangle2D;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
// import racetrack.framework.BundlesCounterContext;
import racetrack.framework.BundlesDT;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;
// import racetrack.util.Utils;
import racetrack.visualization.ColorScale;
import racetrack.visualization.RTColorManager;

/**
 * Class that implemens a correlation matrix
 *
 *@author  D. Trimm
 *@version 0.1
 */
public class RTCorrelatePanel extends RTPanel {
  /**
   *
   */
  private static final long serialVersionUID = -6263127385911942269L;

  /**
   * Radio button indicating that the first colorscale should be used
   */
  private JRadioButtonMenuItem colorscale_1_rbmi,
  /**
   * Radio button indicating that the second colorscale should be used
   */
                               colorscale_2_rbmi,
  /**
   * Radio button to correlate across fields (within a bundle)
   */
			       correlate_by_field_rbmi,
  /**
   * Radio button to correlate across 10 seconds (across bundles)
   */
			       correlate_by_10sec_rbmi,
  /**
   * Radio button to correlate across 5 minutes (across bundles)
   */
			       correlate_by_5min_rbmi;
  /**
   * Construct the correlation panel with the specified parent.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt application parent
   */
  public RTCorrelatePanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      { 
    super(win_type,win_pos,win_uniq,rt);   
    // Make the GUI
    add("Center",  component = new RTCorrelateComponent());

    // Update the menu
    // - Correlation type
    ButtonGroup bg = new ButtonGroup();
    getRTPopupMenu().add(correlate_by_field_rbmi = new JRadioButtonMenuItem("Field Correlation", true)); bg.add(correlate_by_field_rbmi); defaultListener(correlate_by_field_rbmi);
    getRTPopupMenu().add(correlate_by_10sec_rbmi = new JRadioButtonMenuItem("Time 10 sec"));             bg.add(correlate_by_10sec_rbmi); defaultListener(correlate_by_10sec_rbmi);
    getRTPopupMenu().add(correlate_by_5min_rbmi  = new JRadioButtonMenuItem("Time 5 min"));              bg.add(correlate_by_5min_rbmi);  defaultListener(correlate_by_5min_rbmi);

    // - Colorscale options
    bg = new ButtonGroup();
    getRTPopupMenu().add(colorscale_1_rbmi = new JRadioButtonMenuItem("Color Scale 1", true)); bg.add(colorscale_1_rbmi); defaultListener(colorscale_1_rbmi);
    getRTPopupMenu().add(colorscale_2_rbmi = new JRadioButtonMenuItem("Color Scale 2"));       bg.add(colorscale_2_rbmi); defaultListener(colorscale_2_rbmi);
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "correlate"; }

  /**
   * Get the configuration for this panel.  Planned to be used for bookmarking.
   *
   *@return string representation of this configuration
   */
  public String       getConfig    ()           { 
    return "RTCorrelatePanel";
  }

  /**
   * Set the configuration for this panel.  Could be used to recall bookmarks.
   *
   *@param str string representation for new configuration
   */
  public void         setConfig    (String str) {
    StringTokenizer st = new StringTokenizer(str, BundlesDT.DELIM);
    if (st.nextToken().equals("RTCorrelatePanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not A RTCorrelatePanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st2.hasMoreTokens() ? st2.nextToken() : "";
      throw new RuntimeException("Do Not Understand Type Value Pair \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * {@link JComponent} implementing the correlation matrix.
   */
  public class RTCorrelateComponent extends RTComponent {
    private static final long serialVersionUID = 122239531323718261L;
    @Override
    public Set<Shape>      allShapes()                     {
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      // set.addAll(myrc.geom_to_skey.keySet());
      return set; }
    @Override
    public Set<Shape>  shapes(Set<Bundle> bundles) {
      Set<Shape> shapes = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return shapes;
      // Iterator<Bundle> it = bundles.iterator();
      // while (it.hasNext()) {
        // Bundle bundle = it.next();
	// Iterator<String> its = myrc.bundle_to_skeys.get(bundle).iterator();
	// while (its.hasNext()) shapes.add(myrc.skey_to_geom.get(its.next()));
      // }
      return shapes; }
    @Override
    public Set<Bundle> shapeBundles(Shape shape)       { 
      Set<Bundle> set = new HashSet<Bundle>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      // String skey = myrc.geom_to_skey.get(shape);
      // if (skey != null) { set.addAll(myrc.skey_to_bundles.get(skey)); }
      return set; }
    @Override
    public Set<Shape>  overlappingShapes(Shape shape)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      // Iterator<Rectangle2D> it = myrc.geom_to_skey.keySet().iterator();
      // while (it.hasNext()) {
        // Rectangle2D rect = it.next();
	// if (Utils.genericIntersects(rect, shape)) set.add(rect);
      // }
      return set; }
    public Set<Shape>  containingShapes(int x, int y)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      // Iterator<Rectangle2D> it = myrc.geom_to_skey.keySet().iterator();
      // while (it.hasNext()) {
        // Rectangle2D rect = it.next();
	// if (rect.contains(x,y)) set.add(rect);
      // }
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
      // Decode the appropriate colorscale
      ColorScale cs;
      if (colorscale_1_rbmi.isSelected()) cs = RTColorManager.getContinuousColorScale();
      else                                cs = RTColorManager.getTemporalColorScale();
      // Create the render context based on the user's parameters
      RenderContext myrc = new RenderContext(id, bs, count_by, color_by, cs, getWidth(), getHeight());
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
                            color_by;
      /**
       * ColorScale to use for this rendering
       */
      ColorScale            cs;

      /**
       * Lookup for a record to the string keys
       */
      Map<Bundle,Set<String>> bundle_to_skeys = new HashMap<Bundle,Set<String>>();

      /**
       * Lookup for a string key to the bundles
       */
      Map<String,Set<Bundle>> skey_to_bundles = new HashMap<String,Set<Bundle>>();

      /**
       * Lookup for a string key to the geometry
       */
      Map<String,Rectangle2D> skey_to_geom    = new HashMap<String,Rectangle2D>();

      /**
       * Lookup for a geometry to a string key
       */
      Map<Rectangle2D,String> geom_to_skey    = new HashMap<Rectangle2D,String>();

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
       *@param w                  width for this render
       *@param h                  height for this render
       */
      public RenderContext(short id, Bundles bs, String count_by, String color_by, ColorScale cs, int w, int h) {
        render_id = id; this.bs = bs; this.rc_w = w; this.rc_h = h;
	this.count_by           = count_by;
	this.color_by           = color_by;
	this.cs                 = cs;
	Iterator<Tablet> it_tab = bs.tabletIterator();
	while (it_tab.hasNext() && currentRenderID() == getRenderID()) {
          Tablet  tablet           = it_tab.next();
	  boolean tablet_can_count = count_by.equals(BundlesDT.COUNT_BY_BUNS) || KeyMaker.tabletCompletesBlank(tablet, count_by);
          if (tablet.hasTimeStamps() && tablet_can_count) {
            Iterator<Bundle> it_bun = tablet.bundleIterator();
	    while (it_bun.hasNext() && currentRenderID() == getRenderID()) {
	      Bundle bundle     = it_bun.next();
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
          base_bi         = new BufferedImage(rc_w, rc_h, BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) base_bi.getGraphics();
	  g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	  RTColorManager.renderVisualizationBackground(base_bi, g2d);
	 } finally { if (g2d != null) g2d.dispose(); }
        }
        return base_bi;
      }
    }
  }
}

