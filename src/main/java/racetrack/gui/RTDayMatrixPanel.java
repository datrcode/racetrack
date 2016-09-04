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

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;
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
 * Class that implemens a day matrix.
 *
 *@author  D. Trimm
 *@version 0.1
 */
public class RTDayMatrixPanel extends RTPanel {
  /**
   *
   */
  private static final long serialVersionUID = -6261147185219948469L;

  /**
   * Checkbox menu item to determine a linear (or logarithmic)
   * colorscale will be used.
   */
  JCheckBoxMenuItem    log_color_cbmi;

  /**
   * Primary colorscale
   */
  JRadioButtonMenuItem colorscale_1_rbmi,

  /**
   * Secondary colorscale
   */
                       colorscale_2_rbmi;

  /**
   * Construct the day matrix panel with the specified parent.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt application parent
   */
  public RTDayMatrixPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      { 
    super(win_type,win_pos,win_uniq,rt);   
    // Make the GUI
    add("Center",  component = new RTDayMatrixComponent());
    // Update the menu
    // - Colorscale options
    ButtonGroup bg = new ButtonGroup();
    getRTPopupMenu().add(colorscale_1_rbmi = new JRadioButtonMenuItem("Color Scale 1", true)); bg.add(colorscale_1_rbmi); defaultListener(colorscale_1_rbmi);
    getRTPopupMenu().add(colorscale_2_rbmi = new JRadioButtonMenuItem("Color Scale 2"));       bg.add(colorscale_2_rbmi); defaultListener(colorscale_2_rbmi);

    getRTPopupMenu().addSeparator();

    // - Log coloring
    getRTPopupMenu().add(log_color_cbmi    = new JCheckBoxMenuItem("Log Color",true));  defaultListener(log_color_cbmi);
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "daymatrix"; }

  /**
   * Log color selection.
   *
   *@return  true for log coloring, false for linear coloring
   */
  public boolean    logColor()                { return log_color_cbmi.isSelected();    }

  /**
   * Log color configuration.
   *
   *@param b true for log coloring, false for linear coloring
   */
  public void       logColor(boolean b)       { log_color_cbmi.setSelected(b);         }

  /**
   * Get the configuration for this panel.  Planned to be used for bookmarking.
   *
   *@return string representation of this configuration
   */
  public String       getConfig    ()           { 
    return "RTDayMatrixPanel" + BundlesDT.DELIM + "logcolor=" + logColor();
  }

  /**
   * Set the configuration for this panel.  Could be used to recall bookmarks.
   *
   *@param str string representation for new configuration
   */
  public void         setConfig    (String str) {
    StringTokenizer st = new StringTokenizer(str, BundlesDT.DELIM);
    if (st.nextToken().equals("RTDayMatrixPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not A RTDayMatrixPanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st2.hasMoreTokens() ? st2.nextToken() : "";
      if      (type.equals("logcolor"))    logColor(value.toLowerCase().equals("true"));
      else throw new RuntimeException("Do Not Understand Type Value Pair \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * {@link JComponent} implementing the day matrix.
   */
  public class RTDayMatrixComponent extends RTComponent {
    private static final long serialVersionUID = 122469835334788261L;
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
	Iterator<String> its = myrc.bundle_to_skeys.get(bundle).iterator();
	while (its.hasNext()) shapes.add(myrc.skey_to_geom.get(its.next()));
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
      Iterator<Rectangle2D> it = myrc.geom_to_skey.keySet().iterator();
      while (it.hasNext()) {
        Rectangle2D rect = it.next();
	if (Utils.genericIntersects(rect, shape)) set.add(rect);
      }
      return set; }
    public Set<Shape>  containingShapes(int x, int y)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      Iterator<Rectangle2D> it = myrc.geom_to_skey.keySet().iterator();
      while (it.hasNext()) {
        Rectangle2D rect = it.next();
	if (rect.contains(x,y)) set.add(rect);
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
      // Decode the appropriate colorscale
      ColorScale cs;
      if (colorscale_1_rbmi.isSelected()) cs = RTColorManager.getContinuousColorScale();
      else                                cs = RTColorManager.getTemporalColorScale();
      // Determine if log coloring is selected
      boolean    log_color = logColor();
      // Create the render context based on the user's parameters
      RenderContext myrc = new RenderContext(id, bs, count_by, color_by, cs, log_color, getWidth(), getHeight());
      return myrc;
    }
    
    /**
     * RenderContext implementation for the day matrix
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
       * Flag to denote linear (or logarithmic) color scale
       */
      boolean               use_log_color;
      /**
       * Counter context used by the renderer to accumulate
       * sums for the year view.
       */
      BundlesCounterContext year_counter_context,
      /**
       * Counter contex used by the renderer to accumulate
       * sums for the day of week/hour.
       */
                            week_counter_context;
      /**
       * Flag indicating that the year_min and year_max variables have valid values.
       */
      boolean year_set = false; 
      /**
       * Min year seen -- plan to only render three years...
       */
      int     year_min, 
      /**
       * Max year seen -- plan to only render three years...
       */
              year_max;
      /**
       * Set of the year keys -- used to test if a key exists
       */
      Set<String> year_keys = new HashSet<String>(),
      /**
       * Set of the week keys -- used to test if a key exists
       */
                  week_keys = new HashSet<String>();
      /**
       * GMT-based calendar for calculating day of week, year, month, day, etc.
       */
      Calendar gmtcal = Calendar.getInstance(TimeZone.getTimeZone("GMT")),
               tstcal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

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
      public RenderContext(short id, Bundles bs, String count_by, String color_by, ColorScale cs, boolean use_log_color, int w, int h) {
        render_id = id; this.bs = bs; this.rc_w = w; this.rc_h = h;
	this.count_by           = count_by;
	this.color_by           = color_by;
	this.cs                 = cs;
	this.use_log_color      = use_log_color;
        year_counter_context = new BundlesCounterContext(bs, count_by, color_by);
	week_counter_context = new BundlesCounterContext(bs, count_by, color_by);
	Iterator<Tablet> it_tab = bs.tabletIterator();
	while (it_tab.hasNext() && currentRenderID() == getRenderID()) {
          Tablet  tablet           = it_tab.next();
	  boolean tablet_can_count = count_by.equals(BundlesDT.COUNT_BY_BUNS) || KeyMaker.tabletCompletesBlank(tablet, count_by);
          if (tablet.hasTimeStamps() && tablet_can_count) {
            Iterator<Bundle> it_bun = tablet.bundleIterator();
	    while (it_bun.hasNext() && currentRenderID() == getRenderID()) {
	      Bundle bundle     = it_bun.next(); bundle_to_skeys.put(bundle, new HashSet<String>());
              gmtcal.setTimeInMillis(bundle.ts0());

	      // Accumulate the day sums
	      String year_key = gmtcal.get(Calendar.YEAR) + "," + (gmtcal.get(Calendar.DAY_OF_YEAR) - 1); year_keys.add(year_key);
	      year_counter_context.count(bundle, year_key); bundle_to_skeys.get(bundle).add(year_key);
	      if (skey_to_bundles.containsKey(year_key) == false) skey_to_bundles.put(year_key, new HashSet<Bundle>());
	      skey_to_bundles.get(year_key).add(bundle);

	      // Accumulate the week sums
	      String week_key = (gmtcal.get(Calendar.DAY_OF_WEEK) - 1) + "*" + gmtcal.get(Calendar.HOUR_OF_DAY); week_keys.add(week_key);
	      week_counter_context.count(bundle, week_key); bundle_to_skeys.get(bundle).add(week_key);
	      if (skey_to_bundles.containsKey(week_key) == false) skey_to_bundles.put(week_key, new HashSet<Bundle>());
	      skey_to_bundles.get(week_key).add(bundle);

	      // Keep track of the year mins and maxes... will only display three...
              int year = gmtcal.get(Calendar.YEAR);
	      if (year_set) { if (year > year_max) year_max = year;  if (year < year_min) year_min = year; } else { year_min = year_max = year; year_set = true; }
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

	  //
	  // Render Years
	  //

	  // Figure out the geometrix
	  int     txt_h   = Utils.txtH(g2d,"0");
          int     day_h   = txt_h/2 + 2, day_w = txt_h/2 + 2;

	  int     YEARS_TO_RENDER = 4;

          // Calculate the min and max for the years
	  double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
          for (int year_off=0;year_off<YEARS_TO_RENDER;year_off++) {
            int year   = year_max - year_off; if (year < year_min) continue;
	    int day_of_year = 0;
            while (day_of_year < 367) {
	      String year_key = year + "," + day_of_year;
	      if (year_keys.contains(year_key)) {
                double total = year_counter_context.total(year_key);
		if (total < min) min = total;
		if (total > max) max = total;
	      }
	      day_of_year++;
            }
	  }

	  // Make sure that we don't div by zero
	  if ((max - min) < 0.1) { max = min + 0.1; }
          if (use_log_color && min > 0.0) { min = 0.0; }

	  // Render the years first
          for (int year_off=0;year_off<YEARS_TO_RENDER;year_off++) {
            int year   = year_max - year_off; if (year < year_min) continue;
	    int base_x = Utils.txtW(g2d, "Su") + 5,
	        base_y = txt_h + year_off * (2*txt_h + (7*day_h)),
		day_x  = base_x;
            g2d.setColor(RTColorManager.getColor("label", "major")); g2d.drawString(""+year, base_x + day_w*24, base_y + txt_h/2);
	    g2d.setColor(RTColorManager.getColor("label", "minor")); g2d.drawString("Su",    2,      base_y + txt_h +   day_h - day_h/2 + txt_h/2); 
	                                                             g2d.drawString("Sa",    2,      base_y + txt_h + 6*day_h - day_h/2 + txt_h/2);

	    // Determine the starting day of week...
            gmtcal.setTimeInMillis(0); gmtcal.set(Calendar.YEAR, year); gmtcal.set(Calendar.DAY_OF_YEAR, 1);
            int day_of_week = gmtcal.get(Calendar.DAY_OF_WEEK) - 1,  // DAY_OF_WEEK starts at 1...  needs to start at 0
	        day_of_year = 0;
            while (day_of_year < 367) {
	      String year_key = year + "," + day_of_year;
	      if (year_keys.contains(year_key)) {
                double total = year_counter_context.total(year_key);

                float ratio;
		if (use_log_color) { ratio = (float) (Math.log(total - min + 1.0) / Math.log(max - min + 1.0)); } else { ratio = (float) ((total - min)/(max - min)); }
		if (ratio < 0.0f) ratio = 0.0f; if (ratio > 1.0f) ratio = 1.0f;

                tstcal.setTimeInMillis(gmtcal.getTimeInMillis()); tstcal.add(Calendar.DAY_OF_MONTH, 7);
                int use_width;  if (tstcal.get(Calendar.MONTH) == gmtcal.get(Calendar.MONTH)) use_width  = day_w; else use_width  = day_w-1;

                tstcal.setTimeInMillis(gmtcal.getTimeInMillis()); tstcal.add(Calendar.DAY_OF_MONTH, 1);
                int use_height; if (tstcal.get(Calendar.MONTH) == gmtcal.get(Calendar.MONTH)) use_height = day_h; else use_height = day_h-1;
    

                g2d.setColor(cs.at(ratio));
		Rectangle2D rect = new Rectangle2D.Float(day_x, base_y + txt_h + day_of_week*day_h, use_width, use_height);
		g2d.fill(rect); skey_to_geom.put(year_key, rect); geom_to_skey.put(rect, year_key);
              }
              day_of_year++; day_of_week++; if (day_of_week >= 7) { day_of_week = 0; day_x += day_w; } gmtcal.add(Calendar.DAY_OF_MONTH, 1);
	    }
	  }

	  // Provide the colorscale legend
          int base_x   = Utils.txtW(g2d, "Su") + 5, 
	      base_y   = txt_h + YEARS_TO_RENDER * (2*txt_h + (7*day_h));
	  int legend_w = 25*day_w;
          for (int x=0;x<legend_w;x++) {
            g2d.setColor(cs.at((float) (((double) x)/((double) (legend_w - 1)))));
	    g2d.drawLine(base_x + x, base_y, base_x + x, base_y + txt_h/2);
	  }
          g2d.setColor(RTColorManager.getColor("label", "major")); 
	  String str = "" + min; g2d.drawString(str, base_x,                                   base_y + txt_h/2 + txt_h);
	         str = "" + max; g2d.drawString(str, base_x + legend_w - Utils.txtW(g2d, str), base_y + txt_h/2 + txt_h);
          if (use_log_color) {
	    g2d.setColor(RTColorManager.getColor("label", "log"));
	    str = "(log)";
	    g2d.drawString(str, base_x + legend_w/2 - Utils.txtW(g2d, str)/2, base_y + txt_h/2 + txt_h);
	  }

	  //
	  // Render the day of weeks next
	  //

	  // - Determine min and max
	  min = Double.POSITIVE_INFINITY; max = Double.NEGATIVE_INFINITY;
	  for (int dow=0;dow<7;dow++) {
	    for (int hod=0;hod<24;hod++) {
	      String week_key = dow + "*" + hod;
	      if (week_keys.contains(week_key)) {
	        double total = week_counter_context.total(week_key);
                if (total < min) min = total;
		if (total > max) max = total;
	      }
	    }
	  }

	  // Make sure that we don't div by zero
	  if ((max - min) < 0.1) { max = min + 0.1; }
          if (use_log_color && min > 0.0) { min = 0.0; }

	  // Figure out the geometry
	  base_x = Utils.txtW(g2d, "Su") + 5;
	  base_y = 3*txt_h + YEARS_TO_RENDER * (2*txt_h + (7*day_h)) + 3*txt_h;

	  // Draw the labels
	  String days[] = { "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa" };
          day_w = Utils.txtW(g2d, days[0]) + 6;
	  for (int i=1;i<days.length;i++) { int w = Utils.txtW(g2d, days[i]) + 6; if (w > day_w) day_w = w; }
	  day_h = txt_h/2;

          g2d.setColor(RTColorManager.getColor("label", "major")); 
	  for (int i=0;i<24;i+=4) {
	    str = "" + i; if (str.length() == 1) str = "0" + str;
	    g2d.drawString(str, 2, base_y + day_h * (i+1));
	  }
	  for (int i=0;i<days.length;i++) {
	    str = days[i];
	    g2d.drawString(str, base_x + i * day_w + day_w/2 - Utils.txtW(g2d, str)/2, base_y + 25 * day_h + txt_h/2);
	  }

          // Draw the grid
	  for (int dow=0;dow<7;dow++) {
	    for (int hod=0;hod<24;hod++) {
	      String week_key = dow + "*" + hod;
	      if (week_keys.contains(week_key)) {
	        double total = week_counter_context.total(week_key);

                float ratio;
		if (use_log_color) { ratio = (float) (Math.log(total - min + 1.0) / Math.log(max - min + 1.0)); } else { ratio = (float) ((total - min)/(max - min)); }
		if (ratio < 0.0f) ratio = 0.0f; if (ratio > 1.0f) ratio = 1.0f;

                g2d.setColor(cs.at(ratio));
		Rectangle2D rect = new Rectangle2D.Float(base_x + dow * day_w, base_y + hod*day_h, day_w, day_h);
		g2d.fill(rect); skey_to_geom.put(week_key, rect); geom_to_skey.put(rect, week_key);
              }
            }
          }

	  // Draw the legend
	  base_y += 24*day_h + 2*txt_h;
          for (int x=0;x<legend_w;x++) {
            g2d.setColor(cs.at((float) (((double) x)/((double) (legend_w - 1)))));
	    g2d.drawLine(base_x + x, base_y, base_x + x, base_y + txt_h/2);
	  }
          g2d.setColor(RTColorManager.getColor("label", "major")); 
	  str = "" + min; g2d.drawString(str, base_x,                                   base_y + txt_h/2 + txt_h);
	  str = "" + max; g2d.drawString(str, base_x + legend_w - Utils.txtW(g2d, str), base_y + txt_h/2 + txt_h);
          if (use_log_color) {
	    g2d.setColor(RTColorManager.getColor("label", "log"));
	    str = "(log)";
	    g2d.drawString(str, base_x + legend_w/2 - Utils.txtW(g2d, str)/2, base_y + txt_h/2 + txt_h);
	  }
	 } finally { if (g2d != null) g2d.dispose(); }
        }
        return base_bi;
      }
    }
  }
}

