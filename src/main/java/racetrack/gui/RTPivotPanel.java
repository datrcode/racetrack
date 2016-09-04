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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesDT;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;
import racetrack.util.StrCountSorter;
import racetrack.util.Utils;
import racetrack.visualization.RTColorManager;

/**
 * Implements a rudimentary display to show how many
 * bundles share a particular value across the dataset.
 *
 *@author  D. Trimm
 *@version 0.9
 */
public class RTPivotPanel extends RTPanel {
  /**
   * 
   */
  private static final long serialVersionUID = 1624970251891439779L;

  /**
   * GUI component showing the list of fields that can be rendered
   * by the panel.
   */
  JList fields_ls;

  /**
   * Construct the panel with the specified GUI parent.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt       application refernce
   */
  public RTPivotPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      { 
    super(win_type, win_pos, win_uniq, rt);   
    // Construct the GUI
    JPanel east = new JPanel(new BorderLayout()); JButton bt;
    east.add("Center",      new JScrollPane(fields_ls = new JList()));
    east.add("South",  bt = new JButton("Sel Non-Pipes"));
    add("East",    east);
    add("Center",  component = new RTPivotComponent());
    // Update members with application data
    updateBys();
    // Listeners
    defaultListener(fields_ls);
    bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
      ListModel model = fields_ls.getModel(); List<Integer> selected = new ArrayList<Integer>();
      for (int i=0;i<model.getSize();i++) {
        String str = (String) model.getElementAt(i);
	if (str.indexOf(BundlesDT.DELIM) < 0) selected.add(i);
      } 
      int as_array[] = new int[selected.size()];
      for (int i=0;i<as_array.length;i++) as_array[i] = selected.get(i);
      fields_ls.setSelectedIndices(as_array);
    } } );
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "pivot"; }

  /**
   * Get the currently selected fields for the rendering.
   *
   *@return array of fields to use
   */
  public String[] getSelection() {
    java.util.List<String> list = Utils.jListGetValuesWrapper(fields_ls);
    String strs[] = new String[list.size()]; for (int i=0;i<strs.length;i++) strs[i] = list.get(i);
    // String strs[] = new String[sels.length]; for (int i=0;i<strs.length;i++) strs[i] = (String) sels[i];
    return strs;
  }

  /**
   * Not implemented.  Should return the configuration of the GUI for bookmarking.
   *
   *@return string representing gui configuration
   */
  public String       getConfig    ()           { return null; }

  /**
   * Not implemented.  Should take a configuration string and reset the display
   * accordingly.
   *
   *@param str configuration of GUI as a string
   */
  public void         setConfig    (String str) { }

  /**
   * Update the list of fields and ensure that the previously selected values
   * stay selected.
   */
  public void updateBys() {
    // Object sels[] = fields_ls.getSelectedValues();
    String strs[] = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals());
    List<String> remove_multis = new ArrayList<String>();
    for (int i=0;i<strs.length;i++) {
      if (strs[i].endsWith(BundlesDT.MULTI) || (strs[i].indexOf(BundlesDT.MULTI + BundlesDT.DELIM) >= 0)) {
      } else remove_multis.add(strs[i]);
    }
    String as_array[] = new String[remove_multis.size()]; remove_multis.toArray(as_array);
    fields_ls.setListData(as_array);
  }

  /**
   * GUI component responsible for displaying the rendering and providing interactivity.
   */
  public class RTPivotComponent extends RTComponent {
    /**
     * 
     */
    private static final long serialVersionUID = 8823620792911982173L;

    /**
     * Copy the current rendering to the clipboard.
     *
     *@param shft shift key down
     *@param alt  alt key down
     */
    @Override
    public void copyToClipboard    (boolean shft, boolean alt) {
      RenderContext myrc = (RenderContext) getRTComponent().rc;
      if (shft == true && myrc != null)  Utils.copyToClipboard(myrc.getBase());
    }

    /*
     * Return all of the shapes in the current rendering.
     *
     *@return set of shapes in rendering
     */
    public Set<Shape>      allShapes()                     {
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      return myrc.geom_to_bundles.keySet(); }

    /**
     * Return the set of shapes that represent the specified bundles in the current
     * rendering.
     *
     *@param  bundles set of bundles to find the shapes for
     *
     *@return         set of shapes that represent the bundles parameter
     */
    public Set<Shape>  shapes(Set<Bundle> bundles) {
      Set<Shape> shapes = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return shapes;
      Iterator<Bundle> it = bundles.iterator();
      while (it.hasNext()) { Bundle bundle = it.next(); if (bundle != null && myrc.bundle_to_geoms.containsKey(bundle)) shapes.addAll(myrc.bundle_to_geoms.get(bundle)); }
      return shapes; }

    /**
     * Return the bundles associated with the rendered shape.  The shape parameter
     * can only be used if it was created by this component (i.e., not a generic
     * shape).
     *
     *@param  shape shape associated with return bundles
     *
     *@return       associated bundles with the shape
     */
    public Set<Bundle> shapeBundles(Shape shape)       { 
      Set<Bundle> set = new HashSet<Bundle>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      return myrc.geom_to_bundles.get(shape); }

    /**
     * Identify rendered shapes that overlap with the specified shape.
     *
     *@param  shape generic shape to test against
     *
     *@return       set of rendered shapes that overlap
     */
    public Set<Shape>  overlappingShapes(Shape shape)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      Iterator<Shape> it = myrc.geom_to_bundles.keySet().iterator();
      while (it.hasNext()) { Shape to_check = it.next(); if (Utils.genericIntersects(shape, to_check)) set.add(to_check); }
      return set; }

    /**
     * Probably deprecated - did not find any references that used the method.  DELETE
     *
     *@param  x x coordinate to match against
     *@param  y y coordinate to match against
     *
     *@return   set of shapes that contain specified coordinate
     */
    public Set<Shape>  containingShapes(int x, int y)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      Iterator<Shape> it = myrc.geom_to_bundles.keySet().iterator();
      while (it.hasNext()) { Shape shape = it.next(); if (shape.contains(x,y)) set.add(shape); }
      return set; }

    /**
     * Render the current view.  Use a unique render id to ensure concurrent renders
     * do not proceed.
     *
     *@param  id unique render id
     *
     *@return    render context satisfying current state of application and GUI options
     */
    public RTRenderContext render(short id) {
      clearNoMappingSet();
      Bundles bs     = getRenderBundles();
      String  sels[] = getSelection();
      if (bs != null && sels.length > 0) {
        RenderContext myrc = new RenderContext(id, bs, sels, getWidth(), getHeight());
        return myrc;
      } else return null;
    }
    
    /**
     * Render context for the pivot panel.  Render contexts encapsulate the
     * current visualization rendering.  They maintain the state of translating
     * the dataset records and the rendered shapes.
     */
    public class RenderContext extends RTRenderContext {
      /**
       * Dataset to render
       */
      Bundles bs; 

      /**
       * Width in pixels of the current view
       */
      int     rc_w, 

      /**
       * Height in pixels of the current view
       */
              rc_h; 

      /**
       * Application fields to be rendered for pivot analysis
       */
      String  fields[]; 

      /**
       * Timing measures to assess performance
       */
      long    ts0, ts1, ts2, ts3;

      /**
       * Counter from pivot field to bundle key to count
       */
      Map<String,Map<String,Integer>>     fld_to_key_to_count   = new HashMap<String,Map<String,Integer>>();

      /**
       * Set maintainier from pivot field to bundley key to bundle elements (as a set)
       */
      Map<String,Map<String,Set<Bundle>>> fld_to_key_to_bundles = new HashMap<String,Map<String,Set<Bundle>>>();

      /**
       * Number of non pivots within each field
       */
      int non_pivots[], 

      /**
       * Maximum number of non-pivots per field (may be duplicative of non_pivots)
       */
          non_pivots_total[], 

      /**
       * Number of non-unique entities per field
       */
	  pivots[], 

      /**
       * Total number of records that have pivots
       */
	  pivots_total[], 

      /**
       * Maximum across all fields for non-pivots total
       */
	  max_non_pivots_total = 0, 

      /**
       * Maximum across all fields for pivots total
       */
	  max_pivots_total = 0;

      /**
       * Map from rendered fields to the key maker.  Only works for the
       * tablet loop because tablets are tied to the key maker instance.
       */
      Map<String,KeyMaker> kms = new HashMap<String,KeyMaker>();

      /**
       *  Map from rendered shape to associated bundles
       */
      Map<Shape,Set<Bundle>> geom_to_bundles = new HashMap<Shape,Set<Bundle>>();

      /**
       * Map from a bundle to the associated shapes.  Because multiple fields will be shown,
       * it is likely that a single record will map to multipe shapes.
       */
      Map<Bundle,Set<Shape>> bundle_to_geoms = new HashMap<Bundle,Set<Shape>>();

      /**
       * Constructor that takes the datatset and GUI options and arranges the data
       * for rendering.
       *
       *@param id   render id---used to ensure that only the latest rendering is active
       *@param bs   bundles (records) to render
       *@param sels fields to show pivots for
       *@param w    width in pixels
       *@param h    height in pixels
       */
      public RenderContext(short id, Bundles bs, String sels[], int w, int h) {
        render_id = id; this.bs = bs; this.rc_w = w; this.rc_h = h; ts0 = System.currentTimeMillis(); fields = new String[sels.length]; System.arraycopy(sels, 0, fields, 0, fields.length);
	non_pivots = new int[fields.length]; non_pivots_total = new int[fields.length]; pivots = new int[fields.length]; pivots_total = new int[fields.length];
        for (int i=0;i<fields.length;i++) { 
	  fld_to_key_to_count.put(fields[i], new HashMap<String,Integer>());
	  fld_to_key_to_bundles.put(fields[i], new HashMap<String, Set<Bundle>>());
	}
        // Go through the tablets
        Iterator<Tablet> it_tablet = bs.tabletIterator();
	while (it_tablet.hasNext()) {
	  Tablet tablet = it_tablet.next();
	  // Make the key makers
	  Set<String> local_fields = new HashSet<String>();
          for (int i=0;i<fields.length;i++) {
	    if (KeyMaker.tabletCompletesBlank(tablet, fields[i])) {
	      kms.put(fields[i], new KeyMaker(tablet, fields[i]));
	      local_fields.add(fields[i]);
            }
          }
	  // If any key makers exist, go through the bundles
	  if (local_fields.size() > 0) {
	    Iterator<Bundle> it_bundle = tablet.bundleIterator();
            while (it_bundle.hasNext()) {
	      Bundle bundle = it_bundle.next();
              // Iterator<String> it_field = kms.keySet().iterator();
              Iterator<String> it_field = local_fields.iterator();
	      while (it_field.hasNext()) {
	        String field  = it_field.next();
		String keys[] = kms.get(field).stringKeys(bundle);
                for (int i=0;i<keys.length;i++) {
                  if (fld_to_key_to_count.get(field).containsKey(keys[i]) == false) {
		    fld_to_key_to_count.get(field).put(keys[i], 0);
		    fld_to_key_to_bundles.get(field).put(keys[i], new HashSet<Bundle>());
		  }
		  fld_to_key_to_count.get(field).put(keys[i], fld_to_key_to_count.get(field).get(keys[i]) + 1);
		  fld_to_key_to_bundles.get(field).get(keys[i]).add(bundle);
		}
	      }
	    }
	  } else {
	    Iterator<Bundle> it_bundle = tablet.bundleIterator();
            while (it_bundle.hasNext()) { addToNoMappingSet(it_bundle.next()); }
	  }
	}
	// Figure out the aboves and belows -- aboves have a pivot, belows are single instances
        for (int i=0;i<fields.length;i++) {
          Iterator<String> it_key = fld_to_key_to_count.get(fields[i]).keySet().iterator();
	  while (it_key.hasNext()) {
	    String key = it_key.next(); int count = fld_to_key_to_count.get(fields[i]).get(key);
	    if (count == 1) { non_pivots[i]++; non_pivots_total[i] += count; if (max_non_pivots_total < non_pivots_total[i]) max_non_pivots_total = non_pivots_total[i]; } 
            else            { pivots[i]++;     pivots_total[i]     += count; if (max_pivots_total     < pivots_total[i])     max_pivots_total     = pivots_total[i];     }
	  }
	}
	if (max_non_pivots_total == 0) max_non_pivots_total++;
	if (max_pivots_total     == 0) max_pivots_total++;
        ts1 = System.currentTimeMillis();
      }

      /**
       * Return the rendered height (in pixels).
       *
       *@return height in pixels
       */
      public int           getRCHeight() { return rc_h; }

      /**
       * Return the rendered width (in pixels).
       *
       *@return width in pixels
       */
      public int           getRCWidth()  { return rc_w; }

      /**
       * Rendered image
       */
      BufferedImage base_bi = null;

      /**
       * Render the image.
       *
       *@return rendered image
       */
      public BufferedImage getBase() { 
        if (base_bi == null) {
	 Graphics2D g2d = null;
	 try {
          ts2 = System.currentTimeMillis();
          base_bi = new BufferedImage(rc_w, rc_h, BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) base_bi.getGraphics();
          g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	  RTColorManager.renderVisualizationBackground(base_bi, g2d);
	  // Figure out the geometry
          int y_top = 5, y_bot = 5, graph_h = rc_h - (y_top + y_bot); 
          int pivot_h = (graph_h * max_pivots_total)/(max_pivots_total + max_non_pivots_total), non_pivot_h = graph_h - (pivot_h+1);
          // int pivot_h = graph_h/2, non_pivot_h = graph_h/2;
	  int txt_h = Utils.txtH(g2d,"0");
          for (int i=0;i<fields.length;i++) {
            int x_off = 5 + (txt_h + 1) * i; g2d.setColor(RTColorManager.getColor("axis", "minor")); g2d.drawLine(x_off, 0, x_off, rc_h);
	    // Keep track of the bundles areas
            Set<Bundle>             non_pivot_set       = new HashSet<Bundle>(),
	                            pivot_aggregate_set = new HashSet<Bundle>();
            Map<String,Set<Bundle>> str_to_bundles      = new HashMap<String,Set<Bundle>>();
	    // Create the sorter from largest to smallest number of pivots
            List<StrCountSorter> sorter = new ArrayList<StrCountSorter>();
	    Iterator<String> it_key = fld_to_key_to_count.get(fields[i]).keySet().iterator();
	    while (it_key.hasNext()) {
              String key   = it_key.next();
	      int count = fld_to_key_to_count.get(fields[i]).get(key);
	      if (count == 1) {
                non_pivot_set.addAll(fld_to_key_to_bundles.get(fields[i]).get(key));
	      } else          {
                int screen_h = (pivot_h * count)/max_pivots_total;
	        if (screen_h < 3) {
                  pivot_aggregate_set.addAll(fld_to_key_to_bundles.get(fields[i]).get(key));
	        } else            {
		  sorter.add(new StrCountSorter(key, count));
		  str_to_bundles.put(key,fld_to_key_to_bundles.get(fields[i]).get(key));
	        }
	      }
	    }

            Collections.sort(sorter);
            int y     = y_top + pivot_h; Rectangle2D rect = null;
	    // First the pivot aggregates
	    int tmp_h = (pivot_aggregate_set.size() * pivot_h)/(max_pivots_total);
            g2d.setColor(RTColorManager.getColor("set", "multi")); g2d.fill(rect = new Rectangle2D.Double(x_off, y - tmp_h, txt_h, tmp_h)); fillLookUps(rect,pivot_aggregate_set);
	    y -= tmp_h;
	    // Then the sorted pivots
	    for (int j=0;j<sorter.size();j++) {
              tmp_h = (int) ((sorter.get(j).count() * pivot_h)/(max_pivots_total));
              g2d.setColor(RTColorManager.getColor(sorter.get(j).toString())); g2d.fill(rect = new Rectangle2D.Double(x_off, y -tmp_h, txt_h, tmp_h)); fillLookUps(rect,str_to_bundles.get(sorter.get(j).toString()));
	      y -= tmp_h;
	    }
	    // Last the non-pivot aggregates
            tmp_h = (non_pivot_set.size() * non_pivot_h)/(max_non_pivots_total);
	    g2d.setColor(RTColorManager.getColor("set", "multi"));
	    g2d.fill(rect = new Rectangle2D.Double(x_off, y_top + pivot_h + 1, txt_h, tmp_h)); fillLookUps(rect,non_pivot_set);
            // Field description
            Utils.drawRotatedString(g2d, fields[i], x_off + txt_h, y_top + pivot_h, RTColorManager.getColor("label", "defaultfg"), RTColorManager.getColor("label", "defaultbg"));
	  }
          ts3 = System.currentTimeMillis(); g2d.setColor(RTColorManager.getColor("label", "performance")); String timer_str = "" + ((ts3 - ts2) + (ts1 - ts0)); g2d.drawString(timer_str, rc_w - Utils.txtW(g2d,timer_str), txt_h);
         } finally { if (g2d != null) g2d.dispose(); }
        }
        return base_bi;
      }

      /**
       * Fill the lookup tables for converting shapes to bundles and vice versa.
       *
       *@param  rect    rendered shape
       *@paaram bundles associated bundles/records
       */
      private void fillLookUps(Rectangle2D rect, Set<Bundle> bundles) {
        if (geom_to_bundles.containsKey(rect)) geom_to_bundles.get(rect).addAll(bundles); else geom_to_bundles.put(rect,bundles);
	Iterator<Bundle> it = bundles.iterator();
	while (it.hasNext()) {
	  Bundle bundle = it.next(); if (bundle_to_geoms.containsKey(bundle) == false) bundle_to_geoms.put(bundle, new HashSet<Shape>());
	  bundle_to_geoms.get(bundle).add(rect);
	}
      }
    }
  }
}

