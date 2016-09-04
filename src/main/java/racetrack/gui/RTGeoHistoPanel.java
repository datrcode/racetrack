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
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
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
import racetrack.transform.CCShapeRec;
import racetrack.transform.GeoData;
import racetrack.util.Utils;
import racetrack.visualization.ColorScale;
import racetrack.visualization.RTColorManager;

/**
 * Class that implemens a geospatial histogram.  A
 * geospatial histogram displays histogram information
 * as color intentensities that correspond to the
 * related country shape.
 *
 * Version 1.1 - added support for global continuous color scale operation
 *
 *@version 1.1
 */
public class RTGeoHistoPanel extends RTPanel {
  /**
   *
   */
  private static final long serialVersionUID = -6260147185019948463L;

  /**
   * {@link JComboBox} member to determine which field(s)
   * to use for the geospatial contributions for each
   * bundle/record.
   */
  JComboBox            geolu_cb;

  /**
   * Checkbox menu item to determine a linear (or logarithmic)
   * colorscale will be used.
   */
  JCheckBoxMenuItem    linear_color_cbmi;

  /**
   *
   */
  JCheckBox            magnitude_coloring_cb;

  /**
   * Construct the geospatial histogram panel with the specified parent.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt application parent
   */
  public RTGeoHistoPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      { 
    super(win_type,win_pos,win_uniq,rt);   
    // Make the GUI
    add("Center",  component = new RTGeoHistoComponent());
    JPanel south = new JPanel(new FlowLayout());
    south.add(magnitude_coloring_cb = new JCheckBox("Magnitude Coloring", true)); defaultListener(magnitude_coloring_cb);
    south.add(new JLabel("GeoLU"));
    south.add(geolu_cb = new JComboBox()); defaultListener(geolu_cb);
    add("South", south);
    // Update the menu
    getRTPopupMenu().add(linear_color_cbmi    = new JCheckBoxMenuItem("Linear Color",true));  defaultListener(linear_color_cbmi);
    // Update the panels
    updateBys();
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "geohisto"; }

  /**
   * Update the dropdown boxes with new fields that are geospatially enabled.
   * Note that this method requires specific naming conventions for the fields.
   */
  @Override
  public void updateBys() {
    Object sel = geolu_cb.getSelectedItem();
    geolu_cb.removeAllItems();
    String strs[] = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals());
    for (int i=0;i<strs.length;i++) {
      if (strs[i].toLowerCase().endsWith("_cc") || strs[i].toLowerCase().endsWith(BundlesDT.DELIM + "cc"))
        geolu_cb.addItem(strs[i]);
    }
    if (sel == null) geolu_cb.setSelectedIndex(0); else geolu_cb.setSelectedItem(sel);
  }

  /**
   * Return the field to bin (geolocate) the records by.
   *
   *@return field for binning
   */
  public String binBy() { return (String) geolu_cb.getSelectedItem(); }

  /**
   * Set the field to bin (geolocate) the records by.
   *
   *@param str field to bin by
   */
  public void   binBy(String str) { geolu_cb.setSelectedItem(str); }

  /**
   * Linear color selection.
   *
   *@return  true for linear coloring, false for logarithmic coloring
   */
  public boolean    linearColor()                { return linear_color_cbmi.isSelected();    }

  /**
   * Linear color configuration.
   *
   *@param b true for linear coloring, false for logarithmic coloring
   */
  public void       linearColor(boolean b)       { linear_color_cbmi.setSelected(b);         }

  /**
   * Determines if sets should be counted by their set magnitude or
   * by their set elements (e.g., single color if all elements agree, multi
   * color otherwise).
   */
  public boolean    magnitudeColoring()          { return magnitude_coloring_cb.isSelected();    }

  /**
   *
   */
  public void       magnitudeColoring(boolean b) { magnitude_coloring_cb.setSelected(b);         }

  /**
   * Get the configuration for this panel.  Planned to be used for bookmarking.
   *
   *@return string representation of this configuration
   */
  public String       getConfig    ()           { 
    return "RTGeoHistoPanel"                              + BundlesDT.DELIM + 
           "bin="         + Utils.encToURL(binBy())       + BundlesDT.DELIM +
	   "linearcolor=" + linearColor()                 + BundlesDT.DELIM +
	   "magcolor="    + magnitudeColoring();
  }

  /**
   * Set the configuration for this panel.  Could be used to recall bookmarks.
   *
   *@param str string representation for new configuration
   */
  public void         setConfig    (String str) {
    StringTokenizer st = new StringTokenizer(str, BundlesDT.DELIM);
    if (st.nextToken().equals("RTGeoHistoPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not A RTGeoHistoPanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st2.hasMoreTokens() ? st2.nextToken() : "";

      if      (type.equals("bin"))         binBy(Utils.decFmURL(value));
      else if (type.equals("linearcolor")) linearColor(value.toLowerCase().equals("true"));
      else if (type.equals("magcolor"))    magnitudeColoring(value.toLowerCase().equals("true"));
      else throw new RuntimeException("Do Not Understand Type Value Pair \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * {@link JComponent} implementing the geospatial hisogram 
   */
  public class RTGeoHistoComponent extends RTComponent {
    private static final long serialVersionUID = 822469844334778261L;
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
                 color_by  = getRTParent().getColorBy(),
                 geolu_by  = binBy();
      ColorScale cs = RTColorManager.getContinuousColorScale();
      boolean    magnitude_coloring = magnitudeColoring();
      // if (color_by == null) magnitude_coloring = true; // Give it color by defaut if no color defined
      boolean    log_color          = (linearColor()==false);
      RenderContext myrc = new RenderContext(id, bs, count_by, color_by, geolu_by, cs, magnitude_coloring, log_color, getWidth(), getHeight());
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
       * Geospatial header selection
       */
			    geolu_by;
			    
      /**
       * ColorScale to use for this rendering
       */
      ColorScale            cs;

      /**
       * Flag to indcate magnitude coloring is in effect.  Magnitude
       * coloring changes how the application displays Sets.  If
       * selected, Sets are displayed by their overall total 
       * in elements.  Alternatively, if not selected, sets are
       * either displayed by a single color if all records have
       * identical settings for that field or by the multi-set color.
       */
      boolean               magnitude_coloring,

      /**
       * Flag to denote linear (or logarithmic) color scale
       */
                            use_log_color;

      /**
       * Counter context used by the rendered to accumulate
       * sums.
       */
      BundlesCounterContext counter_context;

      /**
       * Construct the rendering context for the geospatial histogram
       * with the specified settings.
       *
       *@param id                 render id
       *@param bs                 bundles to render
       *@param count_by           how to count the record contribution to each country
       *@param color_cy           color option based on global settings
       *@param geolu_by           global field to use for identifying geospatial lookups
       *@param cs                 colorscale to use for the rendering
       *@param magnitude_coloring color by set magnitude versus set elements
       *@param use_log_color      flag to indicate logarithmic coloring is in effect
       *@param w                  width for this render
       *@param h                  height for this render
       */
      public RenderContext(short id, Bundles bs, String count_by, String color_by, String geolu_by, 
                           ColorScale cs, boolean magnitude_coloring, boolean use_log_color, int w, int h) {
        render_id = id; this.bs = bs; this.rc_w = w; this.rc_h = h;
	this.count_by           = count_by;
	this.color_by           = color_by;
	this.geolu_by           = geolu_by;
	this.cs                 = cs;
	this.magnitude_coloring = magnitude_coloring;
	this.use_log_color      = use_log_color;
        counter_context = new BundlesCounterContext(bs, count_by, color_by);
	Iterator<Tablet> it_tab = bs.tabletIterator();
	while (it_tab.hasNext() && currentRenderID() == getRenderID()) {
          Tablet  tablet           = it_tab.next();
	  boolean tablet_can_count = count_by.equals(BundlesDT.COUNT_BY_BUNS) ||
	                             KeyMaker.tabletCompletesBlank(tablet, count_by);
          if (KeyMaker.tabletCompletesBlank(tablet, geolu_by)) {
            KeyMaker binner = new KeyMaker(tablet, geolu_by);          
            Iterator<Bundle> it_bun = tablet.bundleIterator();
	    while (it_bun.hasNext() && currentRenderID() == getRenderID()) {
	      Bundle bundle = it_bun.next();
	      String bins[] = binner.stringKeys(bundle);
	      for (int i=0;i<bins.length;i++) { if (tablet_can_count) counter_context.count(bundle, bins[i]); }
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
          GeoData geodata = GeoData.getInstance(); if (geodata == null) { return base_bi; }
	  int     txt_h   = Utils.txtH(g2d,"0");
          // Set up the transform
	  AffineTransform orig_trans = g2d.getTransform();
	  int adj_h = rc_h - 2*txt_h; if (adj_h < 4*txt_h) adj_h = 4*txt_h;
	  g2d.scale(rc_w/360.0, -adj_h/180.0); g2d.translate(180.0,-90);
	  Set<String> no_shapes = new HashSet<String>();
	  // Get the max
	  double  max     = counter_context.totalMaximum(); if (max < 2.0) max = 2.0;
	  // Go through the cc's
          Iterator<String> it_cc = counter_context.binIterator(); 
	  while (it_cc.hasNext()) {
	    String     cc           = it_cc.next();
	    CCShapeRec cc_shape_rec = geodata.getCCShapeRec(cc);
	    // Figure out coloring...
            if (magnitude_coloring) {
              if (use_log_color) {
	        double total = counter_context.total(cc); if (total > 0.0) {
	               g2d.setColor(cs.at((float) (Math.log(total)/Math.log(max)))); } else continue;
              } else g2d.setColor(cs.at((float) counter_context.totalNormalized(cc)));
            } else g2d.setColor(counter_context.binColor(cc));
            // Draw it...
	    if (cc_shape_rec != null) cc_shape_rec.getShapeRec().fill(g2d); else no_shapes.add(cc);
          }
	  g2d.setTransform(orig_trans);
	  // Put the no shapes at the bottom
          int x0 = 25;
	  Iterator<String> it = no_shapes.iterator();
	  while (it.hasNext()) {
	    String cc = it.next();
	    // Figure out coloring...
	    if (magnitude_coloring) {
              if (use_log_color) {
	        double total = counter_context.total(cc); if (total > 0.0) {
	               g2d.setColor(cs.at((float) (Math.log(total)/Math.log(max)))); } else continue;
              } else g2d.setColor(cs.at((float) counter_context.totalNormalized(cc)));
            } else g2d.setColor(counter_context.binColor(cc));
            // Draw it...
	    g2d.fillRect(x0, adj_h-txt_h, 40, txt_h);
	    g2d.setColor(Color.red);
	    g2d.drawString(cc, x0 + 20 - Utils.txtW(g2d,cc)/2, adj_h - txt_h - 2);
	    x0 += 45;
	  }
	  // draw a legend
          if (magnitude_coloring) {
	    int adj_w = rc_w - 40; if (adj_w < 40) adj_w = 40;
            // Draw the tick marks for log
            if (use_log_color) {
	      double var = 10.0; g2d.setColor(Color.white);
	      while (var < max) {
	        int x = (int) (20 + (adj_w * Math.log(var)/Math.log(max)));
	        g2d.drawLine(x, adj_h, x, adj_h + txt_h);
	        var *= 10.0;
	      }
	    }
	    // Draw the color bar
	    for (int i=0;i<adj_w;i++) {
              g2d.setColor(cs.at(((float) i)/(adj_w-1)));
	      g2d.drawLine(20 + i, adj_h+2, 20 + i, adj_h + txt_h - 4);
	    }
	    // Give it text
	    g2d.setColor(Color.white); g2d.drawString("" + max, 20+adj_w-Utils.txtW(g2d,"" + max), adj_h + 2*txt_h-2);
	    if (use_log_color)         g2d.drawString("1.0",    20,                                adj_h + 2*txt_h-2);
	    else                       g2d.drawString("0.0",    20,                                adj_h + 2*txt_h-2);
            g2d.setColor(Color.red);
            if (use_log_color)         g2d.drawString("Log",    20+adj_w/2-Utils.txtW(g2d,"Log"),  adj_h + 2*txt_h-2);
          }
	 } finally { if (g2d != null) g2d.dispose(); }
        }
        return base_bi;
      }
    }
  }
}

