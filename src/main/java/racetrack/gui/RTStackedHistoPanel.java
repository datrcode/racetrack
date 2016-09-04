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
 * Class to show all of the stacked histograms for the fields in the data set.
 * Is not too useful in its current implementation.  In particular, it is difficult
 * to interpret what the information means.
 *
 * Version 0.95 - added support for global continuous color scale
 *
 *@author  D. Trimm
 *@version 0.9
 */
public class RTStackedHistoPanel extends RTPanel {
  /**
   * 
   */
  private static final long serialVersionUID = 1687667330135237846L;

  /**
   * Checkbox for normalizing the length across fields.
   */
  JCheckBoxMenuItem    normalize_len_cbmi, 
  /**
   * Checkbox for using a linear length function
   */
                       linear_len_cbmi, 
  /**
   * Checkbox for normaling the color across all of the charts
   */
                       normalize_color_cbmi, 
  /**
   * Checkbox for using a linear color model
   */
                       linear_color_cbmi; 

  /**
   * Construct a new panel by creating the necessary gui components and adding
   * elements to the popup menu.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt  parent GUI application
   */
  public RTStackedHistoPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      { 
    super(win_type,win_pos,win_uniq,rt);   
    add("Center",  component = new RTStackedHistoComponent());
    getRTPopupMenu().add(normalize_len_cbmi   = new JCheckBoxMenuItem("Normalize Length",true)); defaultListener(normalize_len_cbmi);
    getRTPopupMenu().add(linear_len_cbmi      = new JCheckBoxMenuItem("Linear Length",true));    defaultListener(linear_len_cbmi);
    getRTPopupMenu().add(normalize_color_cbmi = new JCheckBoxMenuItem("Normalize Color",true));  defaultListener(normalize_color_cbmi);
    getRTPopupMenu().add(linear_color_cbmi    = new JCheckBoxMenuItem("Linear Color",true));     defaultListener(linear_color_cbmi);
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "stackedhistogram"; }

  /**
   * Return true if the length of each histogram should be normalized
   * across all of the datasets.
   *
   *@return true to normalize lengths
   */
  public boolean    normalizeLength()          { return normalize_len_cbmi.isSelected();   }

  /**
   * Set the normalize length flag.
   *
   *@param b true to normalize lengths
   */
  public void       normalizeLength(boolean b) { normalize_len_cbmi.setSelected(b);        }

  /**
   * Return true if a linear length model should be used.  Otherwise, a logarithmic
   * scale will be used.
   *
   *@return true for linear, false for log
   */
  public boolean    linearLength()             { return linear_len_cbmi.isSelected();      }

  /**
   * Set the linear length flag.
   *
   *@param b true for linear length
   */
  public void       linearLength(boolean b)    { linear_len_cbmi.setSelected(b);           };

  /**
   * Return true if the colors should be normalized across fields.
   *
   *@return true to normalize colors
   */
  public boolean    normalizeColor()           { return normalize_color_cbmi.isSelected(); }

  /**
   * Set the normalize color flag.
   *
   *@param b true to normalize colors
   */
  public void       normalizeColor(boolean b)  { normalize_color_cbmi.setSelected(b);      }

  /**
   * Return true if a linear color model should be used.  False indicates a log-based
   * color model.
   *
   *@return true for linear colors, false for log colors
   */
  public boolean    linearColor()              { return linear_color_cbmi.isSelected();    }

  /**
   * Set the linear color flag.
   *
   *@param b true for linear colors, false for log colors
   */
  public void       linearColor(boolean b)     { linear_color_cbmi.setSelected(b);         }

  /**
   * Not implemented.  Should return a string representing this panels configuration.
   *
   *@return string containing panel settings
   */
  public String       getConfig    ()           {
    return "RTStackedHistoPanel"                     + BundlesDT.DELIM + 
           "normlen="            + normalizeLength() + BundlesDT.DELIM + 
	   "linlen="             + linearLength()    + BundlesDT.DELIM + 
	   "normcolor="          + normalizeColor()  + BundlesDT.DELIM + 
	   "lincolor="           + linearColor();
  }

  /**
   * Not implemented.  Should set the configuration based on a representation string.
   *
   *@param str string containing settings
   */
  public void         setConfig    (String str) { 
    StringTokenizer st = new StringTokenizer(str, BundlesDT.DELIM);
    if (st.nextToken().equals("RTStackedHistoPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not A RTStackedHistoPanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st2.nextToken();
      if      (type.equals("normlen"))   normalizeLength(value.toLowerCase().equals("true"));
      else if (type.equals("linlen"))    linearLength(value.toLowerCase().equals("true"));
      else if (type.equals("normcolor")) normalizeColor(value.toLowerCase().equals("true"));
      else if (type.equals("lincolor"))  linearColor(value.toLowerCase().equals("true"));
      else throw new RuntimeException("Do Not Understand Type Value \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * Implement the actual component to be used for painting and interacting with the visualization.
   */
  public class RTStackedHistoComponent extends RTComponent {
    /**
     * 
     */
    private static final long serialVersionUID = -3337684074287661503L;

    /**
     * Return all of the shapes in the current rendering.
     *
     *@return set of all shapes in current rendering
     */
    public Set<Shape>      allShapes()                     {
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      set.addAll(myrc.shape_to_buns.keySet()); return set; }

    /**
     * Return the shapes associated with the specified bundles in the current view.
     *
     *@param  bundles specific bundles to match to shapes
     *
     *@return shapes that correspond to the bundles
     */
    public Set<Shape>  shapes(Set<Bundle> bundles) {
      Set<Shape> shapes = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return shapes;
      Iterator<Bundle> it = bundles.iterator();
      while (it.hasNext()) shapes.addAll(myrc.bun_to_shapes.get(it.next()));
      return shapes; }

    /**
     * For a specific shape, return the bundles that make up that shape.  Note
     * that the specified shape cannot be generic -- it must be a shape object
     * that was created by this component.
     *
     *@param  shape shape record created by this component to match for bundles
     *
     *@return bundles that compose the shape
     */
    public Set<Bundle> shapeBundles(Shape shape)       { 
      Set<Bundle> set = new HashSet<Bundle>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      return myrc.shape_to_buns.get(shape); }

    /**
     * For a general shape, find all of the overlapping shapes in the current
     * rendering.
     *
     *@param  shape general shape to match against
     *
     *@return scene-specific shapes overlapping the shape_to_check
     */
    public Set<Shape>  overlappingShapes(Shape shape)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      Iterator<Shape> it = myrc.shape_to_buns.keySet().iterator();
      while (it.hasNext()) { Shape other = it.next(); if (Utils.genericIntersects(other,shape)) set.add(other); }
      return set; }
    public Set<Shape>  containingShapes(int x, int y)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      Iterator<Shape> it = myrc.shape_to_buns.keySet().iterator();
      while (it.hasNext()) {
        Rectangle2D rect = (Rectangle2D) it.next(); // This class only deals with rectanges
        if (rect.contains(x,y)) set.add(rect);
      }
      return set; }

    /**
     * Render the current scene with the visible data and panel GUI
     * configuration settings.  Use a unique render id to ensure only
     * the most up-to-date rendering exists.
     *
     *@param  id unique render id to ensure only one renderer is running
     *
     *@return render context with current data and settings
     */
    public RTRenderContext render(short id) {
      clearNoMappingSet();
      Bundles bs       = getRenderBundles();
      String  count_by = getRTParent().getCountBy(), color_by = getRTParent().getColorBy();
      if (bs != null && count_by != null) {
        RenderContext myrc = new RenderContext(id, bs, count_by, color_by, 
	                                       normalizeLength(), linearLength(), 
	                                       normalizeColor(), linearColor(),
	                                       getWidth(), getHeight());
        return myrc;
      } else return null;
    }
    
    /**
     * Class that produces the visualization.  Contains all the information
     * for the current rendering including how to correlate data records
     * with rendered shapes.
     */
    public class RenderContext extends RTRenderContext {
      /**
       * Bundles (records) to render
       */
      Bundles bs; 
      /**
       * Width of rendering in pixels
       */
      int     rc_w, 
      /**
       * Height of rendering in pixels
       */
              rc_h; 
      /**
       * Normalize the length of histograms across all fields
       */
      boolean normalize_len = true, 
      /**
       * Use a linear (vice log) length
       */
              linear_len = true, 
      /**
       * Normalize the colors across all fields
       */
	      normalize_color = true, 
      /**
       * Use a linear (vice log) color model
       */
	      linear_color = false; 
      /**
       * Graph width in pixels
       */
      int graph_w = 20;
      /**
       * List of fields to render into stacked histograms
       */
      List<String>                fields = new ArrayList<String>();
      /**
       * Counter contexts per field (correlates at the index level with fields ArrayList)
       */
      List<BundlesCounterContext> bccs   = new ArrayList<BundlesCounterContext>();
      /**
       * Color model to use
       */
      ColorScale                       cs     = RTColorManager.getContinuousColorScale();
      /**
       * Map to convert shapes to bundles
       */
      Map<Shape,Set<Bundle>>   shape_to_buns = new HashMap<Shape,Set<Bundle>>();
      /**
       * Map to convert bundles to shapes
       */
      Map<Bundle,Set<Shape>>   bun_to_shapes = new HashMap<Bundle,Set<Shape>>();

      /**
       * Timers used to measure render performance
       */
      long timer0, timer1, timer2, timer3;

      /**
       * Create the render context with the specified parameters.
       *
       *@param id           render id used to abort unnecessary
       *@param bs           data set to render
       *@param count_by     how to count the data elements
       *@param color_by     how to color the data elements
       *@param norm_len     normalize the length across fields
       *@param linear_len   use a linear length model
       *@param norm_color   normalize the color across fields
       *@param linear_color use a linear color model
       *@param w            width of rendering in pixels
       *@param h            height of rendering in pixels
       */
      public RenderContext(short id, Bundles bs, String count_by, String color_by, 
                           boolean norm_len, boolean linear_len, boolean norm_color, boolean linear_color, 
                           int w, int h) {
        render_id = id; this.bs = bs; 
	this.normalize_len   = norm_len;   this.linear_len   = linear_len;
	this.normalize_color = norm_color; this.linear_color = linear_color;
	this.cs              = RTColorManager.getContinuousColorScale();
	this.rc_w = w; this.rc_h = h;
	// Get the fields first & create counter contexts
	Iterator<String> it_fld = bs.getGlobals().fieldIterator(); while (it_fld.hasNext()) fields.add(it_fld.next());
	Collections.sort(fields);
        for (int i=0;i<fields.size();i++) bccs.add(new BundlesCounterContext(bs, count_by, color_by));
	// Go through the tablets
        timer0 = System.currentTimeMillis();
        Iterator<Tablet> it_tab = bs.tabletIterator();
	while (it_tab.hasNext()) {
	  // Get the table and make the cooresponding key makers
	  Tablet              tablet = it_tab.next();
	  List<KeyMaker> kms    = new ArrayList<KeyMaker>();
	  for (int i=0;i<fields.size();i++) {
	    String fld = fields.get(i);
	    if (KeyMaker.tabletCompletesBlank(tablet,fld)) kms.add(new KeyMaker(tablet,fld)); else kms.add(null);
	  }
	  // Go through the bundles and add them appropriately
	  Iterator<Bundle> it_bun = tablet.bundleIterator();
	  while (it_bun.hasNext() && currentRenderID() ==  getRenderID()) {
	    Bundle bundle = it_bun.next(); bun_to_shapes.put(bundle,new HashSet<Shape>());
	    for (int i=0;i<kms.size();i++) {
	      KeyMaker km = kms.get(i);
	      if (km != null) {
                String bins[] = km.stringKeys(bundle);
		for (int j=0;j<bins.length;j++) bccs.get(i).count(bundle,bins[j]);
	      }
	    }
          }
	}
	timer1 = System.currentTimeMillis();
      }

      /**
       * Return the visualizations height in pixels.
       *
       *@return height in pixels
       */
      public int           getRCHeight() { return rc_h; }

      /**
       * Return the visualizations width in pixels.
       *
       *@return width in pixels
       */
      public int           getRCWidth()  { return rc_w; }

      /**
       * Copy of the rendered image in cases it's requested again
       */
      BufferedImage base_bi = null;

      /**
       * Render the image by drawing the shapes onto an image buffer.
       *
       *@return image of rendering
       */
      public BufferedImage getBase() { 
        if (base_bi == null) {
	 Graphics2D g2d = null;
	 try {
          base_bi = new BufferedImage(rc_w, rc_h, BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) base_bi.getGraphics();
	  g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	  RTColorManager.renderVisualizationBackground(base_bi, g2d);
	  // Determine the maximum field width
	  int txt_h = Utils.txtH(g2d,"0"); int sqr_size = txt_h;
	  int max_fld_w = 0; for (int i=0;i<fields.size();i++) { int fld_w = Utils.txtW(g2d,fields.get(i)); if (fld_w > max_fld_w) max_fld_w = fld_w; }
          max_fld_w += 5; graph_w = rc_w - max_fld_w - 2*sqr_size; if (graph_w < 20) graph_w = 20;
          // Draw the field names
          for (int i=0;i<fields.size();i++) {
	    if ((i%4) == 0) g2d.setColor(new Color(0.2f,0.2f,0.2f)); else g2d.setColor(new Color(0.1f,0.1f,0.1f));
	    g2d.drawLine(0, (i+0)*txt_h, rc_w, (i+0)*txt_h);
	    g2d.setColor(RTColorManager.getColor("label", "default"));
	    g2d.drawString(fields.get(i), max_fld_w - Utils.txtW(g2d, fields.get(i)),(i+1)*txt_h);
          }
          // Figure out the overall maximum...
	  timer2 = System.currentTimeMillis();
          double g_max_len = 0.0;
	  for (int i=0;i<bccs.size();i++) if (g_max_len < bccs.get(i).totalMaximum()) g_max_len = bccs.get(i).totalMaximum();
	  // Synthesize the data for the field representation - figure out which bins are the most to normalize the data across the viz
          Map<Integer,Map<Integer,Set<String>>> y_to_x_to_binset = new HashMap<Integer,Map<Integer,Set<String>>>();
	  Map<Integer,Integer>                  y_to_maxsetsize  = new HashMap<Integer,Integer>();
	  Map<Integer,BundlesCounterContext>    y_to_bcc         = new HashMap<Integer,BundlesCounterContext>();
	  for (int i=0;i<fields.size();i++) {
	    // Initialize variables
	    int y  = i*txt_h; String fld = fields.get(i); BundlesCounterContext bcc = bccs.get(i); y_to_bcc.put(y,bcc);
            y_to_x_to_binset.put(y,new HashMap<Integer,Set<String>>()); y_to_maxsetsize.put(y,0);
	    // Put the bin into the associative array structure
            Iterator<String> it_bin = bcc.binIterator(); 
	    while (it_bin.hasNext()) {
	      // Get the normalized count for this bin
	      String bin = it_bin.next(); double tot = bcc.total(bin), nrm; 
	      if (linear_len) { if (normalize_len) nrm = tot/g_max_len;                     else nrm = tot/bcc.totalMaximum(); }
	      else            { if (normalize_len) nrm = Math.log(tot)/Math.log(g_max_len); else nrm = Math.log(tot)/Math.log(bcc.totalMaximum()); }
	      // Figure out where it should go
	      int    x   = (int) (nrm*graph_w); x -= (x%sqr_size); x += max_fld_w; // put it into square bins
              if (y_to_x_to_binset.get(y).containsKey(x) == false ) y_to_x_to_binset.get(y).put(x,new HashSet<String>());
	      y_to_x_to_binset.get(y).get(x).add(bin);
	      if (y_to_maxsetsize.get(y) < y_to_x_to_binset.get(y).get(x).size()) y_to_maxsetsize.put(y,y_to_x_to_binset.get(y).get(x).size());
	    }
	  }
	  // Figure out the global_max_size
	  Iterator<Integer> it_y = y_to_maxsetsize.keySet().iterator();
	  int g_max_sz = 0; while (it_y.hasNext()) { int y = it_y.next(); if (y_to_maxsetsize.get(y) > g_max_sz) g_max_sz = y_to_maxsetsize.get(y); }
          // Place each bin into a rectangle 
          it_y = y_to_x_to_binset.keySet().iterator();
	  while (it_y.hasNext()) {
	    int y = it_y.next(); BundlesCounterContext bcc = y_to_bcc.get(y);
	    Iterator<Integer> it_x = y_to_x_to_binset.get(y).keySet().iterator();
	    int max = (normalize_color ? g_max_sz : y_to_maxsetsize.get(y));
	    while (it_x.hasNext()) {
	      int x = it_x.next();
	      Set<String> binset = y_to_x_to_binset.get(y).get(x);
	      float ratio = (linear_color) ? (((float) binset.size())/max) : 
	                                     (float) (Math.log(binset.size())/Math.log((max > 1) ? max : 2)); // if it's sz=1,mx=1... you get div/0
              Rectangle2D rect = new Rectangle2D.Double(x+1,y+3,sqr_size-2,sqr_size-3);
              g2d.setColor(RTColorManager.getColor("axis", "minor")); g2d.fill(rect);
              g2d.setColor(cs.at(ratio));   g2d.fillRect(x+2,y+4,sqr_size-4,sqr_size-5);
	      // Keep track of the conversions
	      shape_to_buns.put(rect, new HashSet<Bundle>());
	      Iterator<String> it_bin = binset.iterator();
	      while (it_bin.hasNext()) {
	        String bin = it_bin.next();
	        Iterator<Bundle> it_bun = bcc.getBundles(bin).iterator();
		while (it_bun.hasNext()) { 
		  Bundle bundle = it_bun.next();
		  bun_to_shapes.get(bundle).add(rect); 
		  shape_to_buns.get(rect).add(bundle); 
		}
	      }
	    }
	  }
	  timer3 = System.currentTimeMillis();
	  System.err.println("RTStackedHistoPanel:  Stage 1: " + (timer1-timer0) + " | Stage 2: " + (timer3-timer2));
	  // Draw a legend at the bottom
	  // Normalized/Log
	  int y = fields.size()*txt_h, x = max_fld_w + graph_w/2;
	  if (linear_len)    { g2d.setColor(RTColorManager.getColor("label", "linear"));      g2d.drawString("Linear |",    x - Utils.txtW(g2d,"Linear |"), y+txt_h); }
	  else               { g2d.setColor(RTColorManager.getColor("label", "log"));         g2d.drawString("Log |",       x - Utils.txtW(g2d,"Log |"),    y+txt_h); }
	  if (normalize_len) { g2d.setColor(RTColorManager.getColor("label", "normalized"));  g2d.drawString("Normalized",  x + Utils.txtW(g2d," "),        y+txt_h); }
	  else               { g2d.setColor(RTColorManager.getColor("label", "independent")); g2d.drawString("Independent", x + Utils.txtW(g2d," "),        y+txt_h); }
	  // Colorscale
	  y = (fields.size()+2)*txt_h; x = 10;
	  for (int i=0;i<=400;i++) { g2d.setColor(cs.at(i/400f)); g2d.drawLine(x+i,y,x+i,y+txt_h); }
	  g2d.setColor(RTColorManager.getColor("label", "default"));
	  if (normalize_color) {
	    g2d.drawString("1",x,y+2*txt_h); g2d.drawString(""+g_max_sz,      x+400-Utils.txtW(g2d,""+g_max_sz),      y+2*txt_h);
          } else {
	    g2d.drawString("1",x,y+2*txt_h); g2d.drawString("Field Dependent",x+400-Utils.txtW(g2d,"Field Dependent"),y+2*txt_h);
	  }
	  // Color linearity
	  if (linear_color) {
	    g2d.setColor(RTColorManager.getColor("label", "linear"));
	    g2d.drawString("Linear", x + 400/2 - Utils.txtW(g2d,"Linear")/2, y+2*txt_h);
	  } else            {
	    g2d.setColor(RTColorManager.getColor("label", "log"));
	    g2d.drawString("Log",    x + 400/2 - Utils.txtW(g2d,"Log")/2,    y+2*txt_h);
	  }
         } finally { if (g2d != null) g2d.dispose(); } // Clean up
        }
        return base_bi;
      }
    }
  }
}

