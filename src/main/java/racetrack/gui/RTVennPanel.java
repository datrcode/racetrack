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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesDT;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;
import racetrack.util.Utils;
import racetrack.visualization.RTColorManager;

/**
 * Primary visualization component for showing set data.  Uses
 * the geomery developed by W.F. Edwards.  See the following
 * page:  http://en.wikipedia.org/wiki/Venn_diagram
 *
 *@author   D. Trimm
 *@version  0.9
 */
public class RTVennPanel extends RTPanel {
  /**
   * Derive from fields
   */
  JList             fields_ls;

  /**
   * Magnitude coloring (versus Set coloring)
   */
  JCheckBoxMenuItem magnitude_coloring_cbmi;

  /**
   * Option to draw set counts on the graph
   */
  JCheckBoxMenuItem draw_set_counts_cbmi;

  /**
   * Option to draw set operation labels on the graph
   */
  JCheckBoxMenuItem draw_labels_cbmi;

  /**
   * Option to put each tablet into its own set
   */
  JCheckBoxMenuItem separate_tablets_cbmi;

  /**
   * Construct a Venn Diagram frame with options for tablets/fields/datatypes.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt             application parent
   */
  public RTVennPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt) {
    super(win_type, win_pos, win_uniq, rt);
    add("Center", component = new RTVennComponent());
    add("East",   new JScrollPane(fields_ls = new JList())); updateBys();

    // Popup menu
    getRTPopupMenu().add(separate_tablets_cbmi   = new JCheckBoxMenuItem("Separate Tablets",  false));
    getRTPopupMenu().add(magnitude_coloring_cbmi = new JCheckBoxMenuItem("Log Bin Coloring",  true));
    getRTPopupMenu().add(draw_labels_cbmi        = new JCheckBoxMenuItem("Show SetOp Labels", true));
    getRTPopupMenu().add(draw_set_counts_cbmi    = new JCheckBoxMenuItem("Show Set Counts",   true));

    // Add listeners
    defaultListener(fields_ls);
    defaultListener(magnitude_coloring_cbmi);
    defaultListener(draw_set_counts_cbmi);
    defaultListener(draw_labels_cbmi);
    defaultListener(separate_tablets_cbmi);
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "venndiagram"; }

  /**
   * Update the tablets and field headers combobox
   */
  public void updateBys() {
    // Object sels[] = fields_ls.getSelectedValues();
    String strs[] = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals());
    List<String> remove_multis = new ArrayList<String>();
    for (int i=0;i<strs.length;i++) {
      if        (strs[i].startsWith(BundlesDT.TAGS)) { remove_multis.add(strs[i]); 
      } else if (strs[i].endsWith(BundlesDT.MULTI) || (strs[i].indexOf(BundlesDT.MULTI + BundlesDT.DELIM) >= 0)) {
      } else remove_multis.add(strs[i]);
    }
    String as_array[] = new String[remove_multis.size()]; remove_multis.toArray(as_array);
    fields_ls.setListData(as_array);
  }

  /**
   * Magnitude scale representation.
   *
   *@return true if magnitude scale is selected
   */
  public boolean magnitudeScale()            { return magnitude_coloring_cbmi.isSelected();   }

  /**
   * Set the magnitude scale option
   *
   *@param f magnitude scale option
   */
  public void    magnitudeScale(boolean f)   { magnitude_coloring_cbmi.setSelected(f); }

  /**
   * Separate tablets into different sets.
   *
   *@return true for separate tablets
   */
  public boolean separateTablets() { return separate_tablets_cbmi.isSelected(); }

  /**
   * Set the separate tablets option.
   */
  public void separateTablets(boolean f) { separate_tablets_cbmi.setSelected(f); }

  /**
   * Set count labels.
   *
   *@return true to draw set counts
   */
  public boolean drawSetCounts() { return draw_set_counts_cbmi.isSelected(); }

  /**
   * Set the draw set counts option.
   *
   *@param f draw counts flag
   */
  public void    drawSetCounts(boolean f) { draw_set_counts_cbmi.setSelected(f); }

  /**
   * Labels for boolean set operations.
   *
   *@return true to draw operations
   */
  public boolean drawLabels() { return draw_labels_cbmi.isSelected(); }

  /**
   * Set the draw labels option.
   *
   *@param f draw labels flag
   */
  public void drawLabels(boolean f) { draw_labels_cbmi.setSelected(f); }

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
   *
   */
  public void setSelection(String strs[]) {
    List<Integer> indexes = new ArrayList<Integer>(); ListModel lm = fields_ls.getModel();
    for (int i=0;i<strs.length;i++) {
      for (int j=0;j<lm.getSize();j++) if (strs[i].equals("" + lm.getElementAt(j))) indexes.add(j);
    }
    int index_array[] = new int[indexes.size()]; for (int i=0;i<index_array.length;i++) index_array[i] = indexes.get(i);
    fields_ls.setSelectedIndices(index_array);
  }

  /**
   * Encode an array of strings into a comma-delimited, url-encoded string.  Used for the getConfig() routine.
   */
  private String commaDelimited(String strs[]) {
    StringBuffer sb = new StringBuffer();
    if (strs.length > 0) {
      sb.append(Utils.encToURL(strs[0]));
      for (int i=1;i<strs.length;i++) sb.append("," + Utils.encToURL(strs[i]));
    }
    return sb.toString();
  }

  /**
   * Decode a comma-delimited, url-encoded string into an array of strings.  Used for the setConfig() routine.
   */
  private String[] commaDelimited(String str) {
    StringTokenizer st = new StringTokenizer(str,",");
    String strs[] = new String[st.countTokens()];
    for (int i=0;i<strs.length;i++) strs[i] = Utils.decFmURL(st.nextToken());
    return strs;
  }

  /**
   * Return a string that represent this component's current configuration.  This
   * string can then be used for bookmarks and returning to specific views.
   *
   *@return string representing component's configuration
   */
  public String  getConfig()  {
    return "RTVennPanel" + BundlesDT.DELIM + 
           "selection="  + commaDelimited(getSelection()) + BundlesDT.DELIM +
	   "magscale="   + magnitudeScale()               + BundlesDT.DELIM +
	   "septab="     + separateTablets()              + BundlesDT.DELIM +
	   "setcounts="  + drawSetCounts()                + BundlesDT.DELIM +
	   "labels="     + drawLabels();
  }

  /**
   * Set the view's current configuration based on a string return
   * from the getConfig() method.
   *
   *@param str configuration string
   */
  public void    setConfig(String str) {
    StringTokenizer st = new StringTokenizer(str, BundlesDT.DELIM);
    if (st.nextToken().equals("RTVennPanel") == false) throw new RuntimeException("setConfig(" + str + ") - not a RTVennPanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st2.hasMoreTokens() ? st2.nextToken() : "";
      if      (type.equals("selection")) setSelection(commaDelimited(value));
      else if (type.equals("magscale"))  magnitudeScale(value.toLowerCase().equals("true"));
      else if (type.equals("septab"))    separateTablets(value.toLowerCase().equals("true"));
      else if (type.equals("setcounts")) drawSetCounts(value.toLowerCase().equals("true"));
      else if (type.equals("labels"))    drawLabels(value.toLowerCase().equals("true"));
      else throw new RuntimeException("Do Not Understand Type Value \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * Component that implments interaction and display of the Venn Diagram rendering.
   */
  public class RTVennComponent extends RTComponent {
    /**
     * Implement the default copy to clipboard.  Currently this is
     * supposed to perform a screen capture but doesn't work correctly
     * across platforms.
     *
     *@param shft shift key pressed
     *@param alt  alt key pressed
     */
    @Override
    public void copyToClipboard    (boolean shft, boolean alt) {
      RenderContext myrc = (RenderContext) getRTComponent().rc;
      Utils.copyToClipboard(myrc.getBase());
    }

    /**
     * Return the {@link Shape}s that represent the specified {@link Bundle}s.
     *
     *@param  bundles specific bundles to return shape records for
     *
     *@return         set of shapes that represent the specified bundles
     */
    public Set<Shape>  shapes(Set<Bundle> bundles) { 
      Set<Shape> set = new HashSet<Shape>();
      RenderContext myrc = (RenderContext) rc; if (myrc != null) {
        Iterator<Bundle> it = bundles.iterator();
	while (it.hasNext()) {
	  Bundle bundle = it.next();
	  if (myrc.bun_to_areas.containsKey(bundle)) set.addAll(myrc.bun_to_areas.get(bundle));
        }
      }
      return set;
    }

    /**
     * For a specific {@link Bundle}, return the its associated shapes.
     *
     *@param  bundle bundle to find shapes for
     *
     *@return        set of shapes that represent this bundle in the current view
     */
    public Set<Shape>  shapes(Bundle bundle) { 
      RenderContext myrc = (RenderContext) rc; if (myrc != null) {
        return myrc.bun_to_areas.get(bundle);
      } else return new HashSet<Shape>(); 
    }

    /**
     * For a specific shape, return the associated bundles as a set.
     *
     *@param  shape shape must have been provided by the rendering (i.e.,
     *              cannot handle generic shapes)
     *
     *@return       set of bundle records that the specified shape represent
     */
    public Set<Bundle> shapeBundles(Shape shape) { 
      Set<Bundle> set = new HashSet<Bundle>();
      RenderContext myrc = (RenderContext) rc; if (myrc != null) {
        if (myrc.area_to_desc.containsKey(shape)) {
	  String desc = myrc.area_to_desc.get(shape);
	  Set<Bundle> bundles = myrc.counter_context.getBundles(desc);
	  if (bundles != null) set.addAll(bundles);
	}
      } 
      return set;
    }

    /**
     * Return the set with all the existing shapes in the rendering.
     *
     *@return set of all shapes in the view
     */
    public Set<Shape> allShapes() { 
      RenderContext myrc = (RenderContext) rc; if (myrc != null) {
        return myrc.area_to_desc.keySet();
      }
      return new HashSet<Shape>(); 
    }

    /**
     * For a generic specified shape, return the overlapping shapes.
     *
     *@param  shape_to_check shape to check against
     *
     *@return                set of shapes in the rendering that overlap
     *                       with the specified shape
     */
    public Set<Shape> overlappingShapes(Shape shape_to_check) { 
      Set<Shape> set     = new HashSet<Shape>();
      RenderContext myrc = (RenderContext) rc; if (myrc != null) {
        Iterator<Shape> it_area = myrc.area_to_desc.keySet().iterator();
	while (it_area.hasNext()) {
          Shape shape = it_area.next();
	  if (Utils.genericIntersects(shape_to_check, shape)) set.add(shape);
	}
      }
      return set;
    }

    /**
     * Render the current view by creating a new render context
     * based on the GUI configuration and current set of bundle
     * records.  Use a render ID to abort unnecessary renderings.
     *
     *@param  id render id
     *
     *@return    a render context
     */
    public RTRenderContext render(short id) {
      clearNoMappingSet();
      // Don't draw if not visible
      if (isVisible() == false) { return null; }
      // Get the variables
      Bundles bs = getRenderBundles();
      boolean  magnitude_scale  = magnitudeScale(),
               draw_set_counts  = drawSetCounts(),
	       draw_labels      = drawLabels(),
	       separate_tablets = separateTablets();
      String[] fields           = getSelection();
      String   count_by         = getRTParent().getCountBy();
      String   color_by         = getRTParent().getColorBy();

      // Create the render context and set the base image
      if (bs != null && fields != null && fields.length > 0 && count_by != null) { // color_by can be null...
        RenderContext myrc = new RenderContext(id, bs, fields, count_by, color_by, magnitude_scale, draw_set_counts, draw_labels, separate_tablets, getWidth(), getHeight());
        return myrc;
      }
      return null;
    }

    /**
     * The shape that describes what is under the mouse.
     *
     *@param  x mouse x coordinate for the shape
     *@param  y mouse y coordinate for the shape
     *
     *@return shape directly relevant to mouse postion
     */
    public Shape getZeroOrderShape(int x, int y) {
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return null;
      return new Rectangle2D.Double(x, y, 1, 1);
    }

    /**
     * The shape that describes what is near the mouse.
     *
     *@param  x mouse x coordinate for the shape
     *@param  y mouse y coordinate for the shape
     *
     *@return shape near to mouse postion
     *
     */
    public Shape getFirstOrderShape(int x, int y) {
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return null;
      return new Rectangle2D.Double(x-1, y-1, 3, 3);
    }

    /**
     * The shape that describes what is further from the mouse.
     *
     *@param  x mouse x coordinate for the shape
     *@param  y mouse y coordinate for the shape
     *
     *@return shape further from the mouse postion
     */
    public Shape getSecondOrderShape(int x, int y) {
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return null;
      return new Rectangle2D.Double(x-2, y-2, 5, 5);
    }

    /**
     * The render context is responsible for rendering the view based on
     * the GUI parameters.  As such, it contains the mapping between the
     * rendered shapes and the records.
     */
    class RenderContext extends RTRenderContext {
      /**
       * Width (in pixels) of this renderering
       */
      int     w, 
      /**
       * Height (in pixels) of this rendering
       */
              h; 
      /**
       * Fields for each Venn set
       */
      String fields[];
      /**
       * Area counting option
       */
      String  count_by, 
      /**
       * How to subcolor each bar
       */
	      color_by; 
      /**
       * Use magnitude coloring
       */
      boolean magnitude_scale;

      /**
       * Flag to draw the number of items in the set
       */
      boolean draw_set_counts;

      /**
       * Flag to draw the labels for the set operations
       */
      boolean draw_labels;

      /**
       * Flag to separate tablets, otherwise only selected fields will be used
       */
      boolean separate_tablets;

      /**
       * Flag for too many sets
       */
      boolean too_many_sets = false;

      /**
       * Data to render
       */
      Bundles bs;

      /**
       * Field to string sets
       */
      Map<String,Set<String>>          field_to_set   = new HashMap<String,Set<String>>();

      /**
       * Field to shape
       */
      Map<String,Shape>                field_to_shape = new HashMap<String,Shape>();

      /** 
       * Maps entity strings to the underlying bundles where they came from.
       */
      Map<String,Set<Bundle>>          entity_to_bundles = new HashMap<String,Set<Bundle>>();

      /**
       * Maps bundles (application records) to the areas that they relate to
       */
      Map<Bundle,Set<Shape>>           bun_to_areas = new HashMap<Bundle,Set<Shape>>();

      /**
       * Counter context for counting and coloring bins
       */
      BundlesCounterContext counter_context;

      /**
       * Construct the render context with the specified fields in the arguments which are
       * a snapshot of the GUI settings for this component.
       *
       *@param id               render id
       *@param bs               data to render
       *@param fields_in        application fields for each Venn set
       *@param count_by         how to count inside each overlap
       *@param color_by         how to differentiate each bar by another field
       *@param magnitude_scale  use a logarithmic scale
       *@param separate_tablets put each tablet into a different set
       *@param w                width of the rendering
       *@param h                height of the rendering
       */
      public RenderContext(short id, Bundles bs, String fields_in[], String count_by, String color_by, boolean magnitude_scale, boolean draw_set_counts, boolean draw_labels, boolean separate_tablets, int w, int h) {
	// Save variables...
        render_id = id; this.bs = bs; this.w = w; this.h = h; this.fields = fields_in; this.count_by = count_by; this.color_by = color_by; this.magnitude_scale = magnitude_scale; this.draw_set_counts = draw_set_counts; this.draw_labels = draw_labels; this.separate_tablets = separate_tablets;

	// Determine if separate tablets are needed
	List<String>            new_fields   = new ArrayList<String>();
        Map<String,Set<Tablet>> separate_map = new HashMap<String,Set<Tablet>>();
	Map<Tablet,String>      tab_to_str   = new HashMap<Tablet,String>(); int index = 0;
	Map<String,Tablet>      str_to_tab   = new HashMap<String,Tablet>();
	if (separate_tablets) {
          for (int i=0;i<fields.length;i++) {
	    Iterator<Tablet> tablet_i = bs.tabletIterator();
            while (tablet_i.hasNext()) {
	      Tablet tablet = tablet_i.next(); 
	      // Map the tablets to strings and vice versa
	      if (tab_to_str.containsKey(tablet) == false) { tab_to_str.put(tablet,"TAB"+index); str_to_tab.put("TAB"+index,tablet); index++; }
              boolean tablet_can_count = count_by.equals(BundlesDT.COUNT_BY_BUNS) || KeyMaker.tabletCompletesBlank(tablet, count_by);
	      if (KeyMaker.tabletCompletesBlank(tablet, fields[i]) && tablet_can_count) {
	        if (separate_map.containsKey(tablet) == false) separate_map.put(fields[i],new HashSet<Tablet>());
		separate_map.get(fields[i]).add(tablet);
		new_fields.add(Utils.encToURL(tab_to_str.get(tablet)) + "|" + Utils.encToURL(fields[i]));
	      }
	    }
	  }
	} else { // Just encode the regular fields
          for (int i=0;i<fields.length;i++) new_fields.add(Utils.encToURL(fields[i]));
	}

	// Check the length... can only handle six sets
        if (new_fields.size() > 6) too_many_sets = true;
	fields = new String[new_fields.size() > 6 ? 6 : new_fields.size()];
	for (int i=0;i<fields.length;i++) fields[i] = new_fields.get(i);

	// Go through the bundles
	for (int i=0;i<fields.length;i++) {
	  field_to_set.put(fields[i], new HashSet<String>());
          // System.err.println("field_to_set.put(\"" + fields[i] + "\", new Set())");
          Iterator<Tablet> tablet_i; String app_field_name;
	  if (fields[i].indexOf("|") >= 0) {
	    StringTokenizer st = new StringTokenizer(fields[i], "|");
	    Set<Tablet> set = new HashSet<Tablet>(); set.add(str_to_tab.get(Utils.decFmURL(st.nextToken())));
	    tablet_i = set.iterator();
	    app_field_name = Utils.decFmURL(st.nextToken());
	  } else  { tablet_i = bs.tabletIterator(); app_field_name = Utils.decFmURL(fields[i]); }
	  while (tablet_i.hasNext() && currentRenderID() == getRenderID()) {
	    Tablet  tablet = tablet_i.next();
            boolean tablet_can_count = count_by.equals(BundlesDT.COUNT_BY_BUNS) || KeyMaker.tabletCompletesBlank(tablet, count_by) || magnitude_scale;
	    if (KeyMaker.tabletCompletesBlank(tablet, app_field_name) && tablet_can_count) {
	      // Make the binner
	      KeyMaker binner = new KeyMaker(tablet, app_field_name);
	      Iterator<Bundle> bundle_i = tablet.bundleIterator();
              // Go through the bundles
	      while (bundle_i.hasNext() && currentRenderID() == getRenderID()) {
	        Bundle bundle = bundle_i.next();
		// Get the associated entities from this field
                String bins[] = binner.stringKeys(bundle);
	        if (bins != null && bins.length > 0) {
                  for (int j=0;j<bins.length;j++) {
		    // Add them to the set
		    field_to_set.get(fields[i]).add(bins[j]);
		    // Keep track of which bins map to bundles...
		    if (entity_to_bundles.containsKey(bins[j]) == false) entity_to_bundles.put(bins[j], new HashSet<Bundle>());
		    entity_to_bundles.get(bins[j]).add(bundle);
                  }
	        }
	      }
	    }
	  }
	}
      }

    /**
     * Return the width (in pixels) of this rendering
     *
     *@return width of render
     */
    public int getRCWidth()  { return w; }

    /**
     * Return the height (in pixels) of this rendering
     *
     *@return height of render
     */
    public int getRCHeight() { return h; }

    /**
     * Maps the intersected area to the set of strings
     */
    Map<Area,Set<String>> area_to_set = new HashMap<Area,Set<String>>();

    /**
     * Maps the intersected area to the description of the boolean equation
     */
    Map<Shape,String>     area_to_desc = new HashMap<Shape,String>();

    /**
     * Rendered image - set if already rendered
     */
    BufferedImage base_bi;

    /**
     * Render the image onto a {@link BufferedImage}. DECOMPOSE
     *
     *@return image with the rendering
     */
    @Override
    public BufferedImage getBase() {
      if (base_bi != null) return base_bi;
      Graphics2D g2d = null; BufferedImage bi = null;
      try {
      // Allocate the image
      bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);

      // Configure the renderer
      g2d = (Graphics2D) bi.getGraphics();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // Give each field its own shape
      assignFieldsToShapes();
      
      // Do the combinatorics...
      int max_set_size = 0;
      for (int i=1;i<Math.pow(2,fields.length);i++) {
        boolean first = true; Area area = new Area(); Set<String> set = new HashSet<String>(); String desc = "";
	// Intersect
        for (int j=0;j<fields.length;j++) {
          // System.err.println("Combining \"" + fields[j] + "\"");
	  if (((i >> j) & 0x01) == 0x01) { 
	    if (first) { area.add(new Area(field_to_shape.get(fields[j])));
	                 set.addAll(field_to_set.get(fields[j]));
	                 desc = fields[j];
	                 first = false;
	    } else     { area.intersect(new Area(field_to_shape.get(fields[j])));
	                 set.retainAll(field_to_set.get(fields[j])); 
			 desc += " \u2229 " + fields[j]; } } }
	// Remove
        for (int j=0;j<fields.length;j++) {
	  if (((i >> j) & 0x01) == 0x00) { area.subtract(new Area(field_to_shape.get(fields[j])));
	                                   set.removeAll(field_to_set.get(fields[j])); } }
        area_to_set.put(area, set); if (set.size() > max_set_size) max_set_size = set.size();
	area_to_desc.put(area, desc);
      }

      // Fill the counter context... sort of unnecessary for magnitude coloring...  but needed in other places
      counter_context = new BundlesCounterContext(bs, count_by, color_by);
      Iterator<Area> it_area = area_to_set.keySet().iterator();
      while (it_area.hasNext()) {
        Area area = it_area.next();
        Iterator<String> it_str = area_to_set.get(area).iterator();
        while (it_str.hasNext()) {
	  String str = it_str.next();
	  Iterator<Bundle> it_bun = entity_to_bundles.get(str).iterator();
	  while (it_bun.hasNext()) {
	    Bundle bundle = it_bun.next();
	    counter_context.count(bundle, area_to_desc.get(area));
	    if (bun_to_areas.containsKey(bundle) == false) bun_to_areas.put(bundle, new HashSet<Shape>());
	    bun_to_areas.get(bundle).add(area);
	  }
	}
      }

      // Determine the coloring method
      if (magnitude_scale) {
        it_area = area_to_set.keySet().iterator();
	while (it_area.hasNext()) {
	  Area area = it_area.next();
	  g2d.setColor(RTColorManager.getLogColor(area_to_set.get(area).size()));
	  g2d.fill(area);
	}
      } else {
        it_area = area_to_set.keySet().iterator();
        while (it_area.hasNext()) {
          Area area = it_area.next();
          if (area_to_set.get(area).size() > 0) {
	    g2d.setColor(counter_context.binColor(area_to_desc.get(area)));
            g2d.fill(area);
          }
        }
      }

      // Show labels or set counts based on user settings
      if (draw_labels || draw_set_counts) {
	Iterator<Area> it = area_to_set.keySet().iterator();
	while (it.hasNext()) {
	  Area area = it.next();
	  String str;
	  if (draw_labels) {
	    str = Utils.decFmURL(area_to_desc.get(area));
            clearStr(g2d, str, ((int) (area.getBounds().getCenterX())) - Utils.txtW(g2d, str)/2, 
	                       ((int) (area.getBounds().getCenterY())) + (draw_set_counts ? 0 : Utils.txtH(g2d,str)/2),
		  	       RTColorManager.getColor("label", "defaultfg"), RTColorManager.getColor("label", "defaultbg"));
          }
	  if (draw_set_counts) {
	    str = "" + area_to_set.get(area).size();
            clearStr(g2d, str, ((int) (area.getBounds().getCenterX())) - Utils.txtW(g2d, str)/2, 
	                       ((int) (area.getBounds().getCenterY())) + (draw_labels ? Utils.txtH(g2d, str) : Utils.txtH(g2d,str)/2),
		  	       RTColorManager.getColor("label", "defaultfg"), RTColorManager.getColor("label", "defaultbg"));
          }
	}
      }

      // For debug, render the shapes to make sure they have the right coordinates
      g2d.setStroke(new BasicStroke(3.0f));
      Iterator<String> it = field_to_shape.keySet().iterator();
      while (it.hasNext()) { 
        String field = it.next(); 
	g2d.setColor(RTColorManager.getColor(field));
	Shape shape = field_to_shape.get(field); 
	g2d.draw(shape); 
      }

      } finally { if (g2d != null) g2d.dispose(); } // Clean up

      return (base_bi = bi);
      }


    /**
     * Assign fields to shapes.  Use W.F. Edwards shapes because those seem most reasonable...
     * http://en.wikipedia.org/wiki/Venn_diagram
     */
    private void assignFieldsToShapes() {
      int inset = 5; double radius = getRCWidth() < getRCHeight() ? 5*getRCWidth()/16.0 : 5*getRCHeight()/16.0;
      double cx = getRCWidth()/2.0, cy = getRCHeight()/2.0;

      if (fields.length >= 1) field_to_shape.put(fields[0], new RoundRectangle2D.Double(inset, cy, getRCWidth()-2*inset, cy-inset, inset, inset));
      if (fields.length >= 2) field_to_shape.put(fields[1], new RoundRectangle2D.Double(cx,    inset, cx-inset, getRCHeight()-2*inset, inset, inset));
      if (fields.length >= 3) field_to_shape.put(fields[2], new Ellipse2D.Double(cx - radius, cy - radius, radius*2.0, radius*2.0));
      // This is where the shapes become complicated...
      double radius_off = 26*radius/32;
      if (fields.length >= 4) {
          Area area = new Area();
            area.add(     new Area(new Ellipse2D.Double(cx + radius - radius_off,
                                                        cy - radius_off,
					                radius_off*2.0,
					                radius_off*2.0)));
            area.add(     new Area(new Ellipse2D.Double(cx - radius - radius_off,
                                                        cy - radius_off,
				          	        radius_off*2.0,
					                radius_off*2.0)));
            area.add(     new Area(new Ellipse2D.Double(cx - radius_off,
	                                                cy - radius_off,
							radius_off*2.0,
							radius_off*2.0)));
            area.subtract(new Area(new Ellipse2D.Double(cx - radius_off, cy - 2.6*radius_off, radius_off*2, radius_off*2)));
            area.subtract(new Area(new Ellipse2D.Double(cx - radius_off, cy + 0.6*radius_off, radius_off*2, radius_off*2)));
          field_to_shape.put(fields[3], area);
        }

      double radius_sm = radius/3.0;
      // And more complicated...
      if (fields.length >= 5) {
        Area area = new Area();
	  area.add(new Area(new Ellipse2D.Double(cx-radius_sm, cy - radius - radius_sm, 2*radius_sm, 2*radius_sm)));
	  area.add(new Area(new Ellipse2D.Double(cx-radius_sm, cy + radius - radius_sm, 2*radius_sm, 2*radius_sm)));
	  area.add(new Area(new Ellipse2D.Double(cx-radius-radius_sm, cy - radius_sm, 2*radius_sm, 2*radius_sm)));
	  area.add(new Area(new Ellipse2D.Double(cx+radius-radius_sm, cy - radius_sm, 2*radius_sm, 2*radius_sm)));
	  area.add(new Area(new Rectangle2D.Double(cx-radius,cy-radius_sm,2*radius,2*radius_sm)));
	  area.add(new Area(new Rectangle2D.Double(cx-radius_sm,cy-radius,2*radius_sm,2*radius)));
	  area.add(new Area(new Rectangle2D.Double(cx-radius+radius_sm,cy-radius+radius_sm,2*radius-2*radius_sm,2*radius-2*radius_sm)));
	  area.subtract(new Area(new Ellipse2D.Double(cx-radius,cy-radius,   2*radius_sm,2*radius_sm)));
	  area.subtract(new Area(new Ellipse2D.Double(cx-radius,cy+radius_sm,2*radius_sm,2*radius_sm)));
	  area.subtract(new Area(new Ellipse2D.Double(cx+radius_sm,cy-radius,   2*radius_sm,2*radius_sm)));
	  area.subtract(new Area(new Ellipse2D.Double(cx+radius_sm,cy+radius_sm,2*radius_sm,2*radius_sm)));
	field_to_shape.put(fields[4], area);
        }
      // And last...
      double radius_rsm = radius/5.0;
      if (fields.length >= 6) {
        Area area = new Area();
	  area.add(new Area(new Ellipse2D.Double(cx - radius, cy - radius, radius*2.0, radius*2.0)));
	  for (int i=0;i<20;i++) { // 15
	    double angle = 2*Math.PI*i/16.0;
	    Ellipse2D.Double part = new Ellipse2D.Double(cx + Math.cos(angle)*radius - radius_rsm, cy + Math.sin(angle)*radius - radius_rsm, 2*radius_rsm, 2*radius_rsm);
	    if ((i%2) == 0) area.add(new Area(part)); else area.subtract(new Area(part));
	  }
        field_to_shape.put(fields[5], area);
        }
      }
    }
  }
}

