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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ListModel;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesG;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.BundlesDT;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;
import racetrack.util.Utils;
import racetrack.visualization.RTColorManager;

/**
 * Preliminary implementation of a parallel coordinates view
 * for the RACETrack application.  Implementation only renders
 * in its current form and does not provide interaction or
 * control over how elements are rendered.
 *
 * Version 0.9 - added blend option
 *
 *@author  D. Trimm
 *@version 0.9
 */
public class RTParCoPanel extends RTPanel {
  /**
   * 
   */
  private static final long serialVersionUID = -4940154312088194598L;


  /**
   * Field list to include in the parallel coordinates view.
   */
  JList fields_ls;

  /**
   * Blending slider
   */
  JSlider blend_sl;

  /**
   * Construct an instance of the parallel coordinates panel using
   * the specified parent object.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt parent GUI instance
   */
  public RTParCoPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      { 
    super(win_type, win_pos, win_uniq, rt);   
    // Construct the GUI
    JPanel east = new JPanel(new BorderLayout()); JButton bt;
    east.add("North",  blend_sl = new JSlider(1,100,40));
    east.add("Center",            new JScrollPane(fields_ls = new JList()));
    east.add("South",  bt       = new JButton("Sel Non-Pipes"));
    add("East",    east);
    add("Center",  component = new RTParCoComponent());
    // Update members with application data
    updateBys();
    // Listeners
    defaultListener(blend_sl);
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
  public String     getPrefix() { return "parallelcoords"; }

  /**
   * Set capturing the last set of axes displayed.  Useful to determine
   * which new axes are added so that the user can construct the correct
   * ordering for the axes...
   */
  Set<String> last_set   = new HashSet<String>(); 

  /**
   * Last string array of selected axes.  Useful to determine which
   * axes are new so that the user can control the ordering.
   */
  String          last_sel[] = new String[0];

  /**
   * Return the selection of axes for the view.  The method is complicated by
   * the fact that it needs to return the list as the user adds each axis.  Not
   * in the order of the JList values.
   *
   *@return ordered list of axes to display
   */
  public synchronized String[] getSelection() {
    // What does the gui show?
    java.util.List<String> list = Utils.jListGetValuesWrapper(fields_ls);  Set<String> set = new HashSet<String>();
    String sel[] = new String[list.size()]; for (int i=0;i<sel.length;i++) { sel[i] = list.get(i); set.add(sel[i]); }
    // Let's add from the last selection to figure out what to show
    String strs[] = new String[sel.length]; int strs_i = 0;
    for (int i=0;i<last_sel.length;i++) {
      if (set.contains(last_sel[i]) == true  && strs_i < strs.length) strs[strs_i++] = last_sel[i];
    }
    for (int i=0;i<sel.length;i++) {
      if (last_set.contains(sel[i]) == false && strs_i < strs.length) strs[strs_i++] = sel[i];
    }
    if (strs_i != strs.length) {
      System.err.println("RTParCoPanel - error calculating the new selection");
      strs = sel;
    }
    // Save it for next time
    last_set = set; last_sel = strs;
    // Return it
    return strs;
  }

  /**
   * Get the current blending amount.
   *
   *@return blend value on a 0.0 to 1.0 scale
   */
  public float blend() { return blend_sl.getValue()/100f; }

  /**
   * Set the blending slider.  Float value will be converted to a numeric percentage.
   *
   *@param new_value new value on the 0.0 to 1.0 scale
   */
  public void blend(float new_value) { blend_sl.setValue((int) (new_value*100f)); }

  /**
   * Return a string representing the configuration of this component.  Used for
   * bookmarking views to more easily recall them.
   *
   *@return string representing view configuration
   */
  @Override
  public String       getConfig    ()           { 
    return "RTParCoPanel"                                + BundlesDT.DELIM + 
           "lastset="     + commaDelimited(last_sel)     + BundlesDT.DELIM +
           "blend="       + Utils.encToURL("" + blend());
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
   * Adjust the configuration of this component based on the specified configuration
   * string.
   *
   *@param str configuration string
   */
  @Override
  public void         setConfig    (String str) { 
    StringTokenizer st = new StringTokenizer(str, BundlesDT.DELIM);
    if (st.nextToken().equals("RTParCoPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not A RTParCoPanel");

    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st2.hasMoreTokens() ? st2.nextToken() : "";
      if (type.equals("lastset")) {
        String strs[]  = commaDelimited(value);
        List<Integer> indexes = new ArrayList<Integer>(); ListModel lm = fields_ls.getModel();
        for (int i=0;i<strs.length;i++) {
          for (int j=0;j<lm.getSize();j++) if (strs[i].equals("" + lm.getElementAt(j))) indexes.add(j);
        }
        int index_array[] = new int[indexes.size()]; for (int i=0;i<index_array.length;i++) index_array[i] = indexes.get(i);
        last_sel = strs; last_set = new HashSet<String>(); for (int i=0;i<last_sel.length;i++) last_set.add(last_sel[i]);
        fields_ls.setSelectedIndices(index_array);
      } else if (type.equals("blend")) {
        blend(Float.parseFloat(Utils.decFmURL(value)));
      } else throw new RuntimeException("Do Not Understand Type Value Pair \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * Update the list of possible axis because new fields were
   * added to the application.  In this case, we need to keep the previously
   * selected elements selected and in the correct order.
   */
  public void updateBys() {
    // Object sels[] = fields_ls.getSelectedValues();
    String strs[] = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals(), false, true, true, true);
    List<String> remove_multis = new ArrayList<String>();
    for (int i=0;i<strs.length;i++) {
      if (strs[i].endsWith(BundlesDT.MULTI) || (strs[i].indexOf(BundlesDT.MULTI + BundlesDT.DELIM) >= 0)) {
      } else remove_multis.add(strs[i]);
    }
    String as_array[] = new String[remove_multis.size()]; remove_multis.toArray(as_array);
    fields_ls.setListData(as_array);
  }

  /**
   * GUI component implementing the (future) interaction with the parallel coordinates view.
   */
  public class RTParCoComponent extends RTComponent {
    /**
     * 
     */
    private static final long serialVersionUID = 3001791924170722238L;

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
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      return myrc.geom_to_skey.keySet(); }

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
      while (it.hasNext()) {
        Bundle bundle = it.next();
	if (myrc.bun_to_geom.containsKey(bundle)) shapes.addAll(myrc.bun_to_geom.get(bundle));
      }
      return shapes; }

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
      String skey = myrc.geom_to_skey.get(shape);
      if (skey != null) return myrc.skey_to_buns.get(skey);
      return set; }

    /**
     * Find the rendered shapes that overlapp with the specified shapes.
     *
     *@param shape shape to test against... can be generic
     *
     *@return rendered shapes that overlap
     */
    public Set<Shape>  overlappingShapes(Shape shape)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      Iterator<Shape> it = myrc.geom_to_skey.keySet().iterator();
      while (it.hasNext()) {
        Shape to_test = it.next();
        if (Utils.genericIntersects(shape, to_test)) set.add(to_test);
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
      clearNoMappingSet();
      Bundles bs       = getRenderBundles();
      String  sels[]   = getSelection();
      String  count_by = getRTParent().getCountBy(),
              color_by = getRTParent().getColorBy();
      float   blend    = blend();
      if (bs != null && sels.length > 1) {
        RenderContext myrc = new RenderContext(id, bs, count_by, color_by, sels, blend, getWidth(), getHeight());
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
       * Amount to blend the lines with the background
       */
      float   blend;
      /**
       * Fields to use for the parallel coordinate axes
       */
      String  fields[]; 

      /**
       * Various timestamps to measure and report the performance of this component
       */
      long    ts0, 
              ts1, 
	      ts2, 
	      ts3;

      /**
       * Data structure for keeping track on countercontexts.  The first lookup is for the 
       * pairwise axis while the second lookup encapsulates the a key that connects the
       * first axis to the second axis.
       */
      Map<Integer,Map<String,BundlesCounterContext>> map   = new HashMap<Integer,Map<String,BundlesCounterContext>>();

      /**
       * Lookup for each axis-to-axis pairing of the string representation to the integer value (from axis)
       */
      Map<Integer,Map<String,Long>>                  y0_lu = new HashMap<Integer,Map<String,Long>>(),

      /**
       * Lookup for each axis-to-axis pairing of the string representation to the integer value (to axis)
       */
                                                     y1_lu = new HashMap<Integer,Map<String,Long>>();
      /**
       * Array with the min and max bounds for each axis
       */
      long   y_bounds[][];

      /**
       * Array with the string version of the bounds for each axis
       */
      String y_bounds_str[][];

      /**
       *
       */
      Map<Integer, Map<Long,Double>> axis_mappers = new HashMap<Integer,Map<Long,Double>>();

      /**
       *
       */
      Map<Integer, List<Long>>    sorter_lists = new HashMap<Integer,List<Long>>();
      Map<Integer, Set<Long>>     sorter_sets  = new HashMap<Integer,Set<Long>>();

      /**
       * Construct the rendering variables for this rendering.
       *
       *@param id       render id (used to abort superceded renders)
       *@param bs       dataset to render
       *@param count_by how to count elements
       *@param color_by coloring method for shapes int the scene
       *@param sels     an ordered list of axis for the parallel coordinates
       *@param blend    how much to blend the lines with the background
       *@param w        width of rendering in pixels
       *@param h        height of rendering in pixels
       */
      public RenderContext(short id, Bundles bs, String count_by, String color_by, String sels[], float blend, int w, int h) {
        render_id = id; this.bs = bs; this.count_by = count_by; this.color_by = color_by; this.blend = blend; this.rc_w = w; this.rc_h = h; ts0 = System.currentTimeMillis(); fields = new String[sels.length]; System.arraycopy(sels, 0, fields, 0, fields.length);
	// Field information
	BundlesG globals = bs.getGlobals();
        boolean time_based[]; time_based = new boolean[fields.length];
        y_bounds     = new long   [fields.length][2]; 
	y_bounds_str = new String [fields.length][2];
	for (int i=0;i<fields.length;i++) {
	  if (globals.isScalar(globals.fieldIndex(fields[i])) == false) {
	    sorter_sets.put(i,new HashSet<Long>());
	    sorter_lists.put(i,new ArrayList<Long>());
          }
          if (KeyMaker.isTimeBlank(fields[i])) time_based[i] = true; else time_based[i] = false;
	  y_bounds[i][0] = Long.MAX_VALUE;  y_bounds_str[i][0] = "Undefined";
	  y_bounds[i][1] = Long.MIN_VALUE;  y_bounds_str[i][1] = "Undefined";
        }

        // Go through the tablets making key makers along the way
        Iterator<Tablet> it_tablet = bs.tabletIterator();
	while (it_tablet.hasNext() && currentRenderID() == getRenderID()) {
	  Tablet tablet = it_tablet.next();
	  // handle each pair of coordinate axes separately
          for (int i=0;i<fields.length-1;i++) {
	    // Make sure the associative arrays are in place for this axis-to-axis
	    if (map.containsKey(i)   == false) map.put(i, new HashMap<String,BundlesCounterContext>());
	    if (y0_lu.containsKey(i) == false) y0_lu.put(i, new HashMap<String,Long>());
	    if (y1_lu.containsKey(i) == false) y1_lu.put(i, new HashMap<String,Long>());
	    // If the tablet completes both blanks, make a key maker and go through the bundles
	    if (KeyMaker.tabletCompletesBlank(tablet, fields[i]) && KeyMaker.tabletCompletesBlank(tablet, fields[i+1])) {
	      KeyMaker km0 = new KeyMaker(tablet, fields[i]), km1 = new KeyMaker(tablet, fields[i+1]);
              Iterator<Bundle> it_bundle = tablet.bundleIterator();
	      while (it_bundle.hasNext() && currentRenderID() == getRenderID()) {
	        Bundle bundle = it_bundle.next();
		// Make the string keys - if they're good proceed with counting...
		String fms[]  = km0.stringKeys(bundle), tos[]  = km1.stringKeys(bundle);
		if (fms != null && fms.length > 0 && tos != null && tos.length > 0) {

		  long fmi[], toi[];
                  if (time_based[i  ]) { fmi = new long[1]; fmi[0] = km0.timeStampKey(bundle); } else fmi = asLongs(km0.intKeys(bundle)); 
                  if (time_based[i+1]) { toi = new long[1]; toi[0] = km1.timeStampKey(bundle); } else toi = asLongs(km1.intKeys(bundle));

		  for (int f=0;f<fms.length;f++) {
                    // For equally spaced fields, keep track of the values, mins, and maxes
		    if (sorter_sets.containsKey(i)   && sorter_sets.get(i).contains(fmi[f])   == false) { 
		      sorter_lists.get(i  ).add(fmi[f]); sorter_sets.get(i  ).add(fmi[f]); }
		    if (y_bounds[i  ][0] > fmi[f]) { y_bounds[i  ][0] = fmi[f]; y_bounds_str[i  ][0] = fms[f]; }
		    if (y_bounds[i  ][1] < fmi[f]) { y_bounds[i  ][1] = fmi[f]; y_bounds_str[i  ][1] = fms[f]; }

		    for (int t=0;t<tos.length;t++) {
		      String key = fms[f] + BundlesDT.DELIM + tos[t];
		      // Make the associative arrays and the lookups
		      if (map.get(i).containsKey(key) == false) map.get(i).put(key,new BundlesCounterContext(bs, count_by, color_by));
		      y0_lu.get(i).put(key,fmi[f]); y1_lu.get(i).put(key,toi[t]);

		      // Keep the bounds per axis
		      if (y_bounds[i+1][0] > toi[t]) { y_bounds[i+1][0] = toi[t]; y_bounds_str[i+1][0] = tos[t]; }
		      if (y_bounds[i+1][1] < toi[t]) { y_bounds[i+1][1] = toi[t]; y_bounds_str[i+1][1] = tos[t]; }

                      // For equally spaced fields, keep track of the values, mins, and maxes (this should be put in its own loop...  it gets run multiple times for no reason)
		      if (sorter_sets.containsKey(i+1) && sorter_sets.get(i+1).contains(toi[t]) == false) { 
		        sorter_lists.get(i+1).add(toi[t]); sorter_sets.get(i+1).add(toi[t]); }

                      // Count it
		      map.get(i).get(key).count(bundle,key);
		    }
		  }
                }
	      }
            }
          }
        }

	// Create the equal space mapping
        Iterator<Integer> it_i = sorter_lists.keySet().iterator();
	while (it_i.hasNext()) {
	  int field_i = it_i.next();
	  axis_mappers.put(field_i, AxisMapper.calculateMapping(AxisMapper.EQUAL_SCALE_STR, sorter_lists.get(field_i), y_bounds[field_i][0], y_bounds[field_i][1]));
	}

        ts1 = System.currentTimeMillis();
        // System.err.println("PC Calc Time = " + (ts1 - ts0));
      }

      /**
       *
       */
      private long[] asLongs(int ints[]) { long[] array = new long[ints.length]; for (int i=0;i<array.length;i++) array[i] = ints[i]&LONGMASK; return array; }

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
       *
       */
      Map<String,Shape> skey_to_geom = new HashMap<String,Shape>();

      /**
       *
       */
      Map<String,Set<Bundle>> skey_to_buns = new HashMap<String,Set<Bundle>>();

      /**
       *
       */
      Map<Shape,String> geom_to_skey = new HashMap<Shape,String>();

      /**
       *
       */
      Map<Bundle,Set<Shape>> bun_to_geom = new HashMap<Bundle,Set<Shape>>();

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
          ts2 = System.currentTimeMillis();
	  // Create the image
          base_bi = new BufferedImage(rc_w, rc_h, BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) base_bi.getGraphics();
          g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	  RTColorManager.renderVisualizationBackground(base_bi, g2d);
	  // Make the insets
          int fld0_w = Utils.txtW(g2d,fields[0]), fldx_w = Utils.txtW(g2d,fields[fields.length-1]);
          int x_ins = (fld0_w > fldx_w) ? (15+fld0_w/2) : (15+fldx_w);
	  int txt_h = Utils.txtH(g2d,"0"), y_ins = txt_h+5, graph_w = rc_w - 2*x_ins, graph_h = rc_h - 2*y_ins - txt_h, axis_w = graph_w/(fields.length-1);
	  // Draw some labels and axes
          for (int xi=0;xi<fields.length;xi++) {
	    g2d.setColor(RTColorManager.getColor("label", "default"));
            int x0 = x_ins + xi*axis_w;
	    // Draw the axis
	    g2d.drawLine(x0,y_ins,x0,y_ins+graph_h); 
	    // Draw the mins and maxes
	    g2d.drawString(y_bounds_str[xi][1], x0 - Utils.txtW(g2d,y_bounds_str[xi][1])/2,  y_ins - 2);
	    g2d.drawString(y_bounds_str[xi][0], x0 - Utils.txtW(g2d,y_bounds_str[xi][0])/2,  y_ins + graph_h + txt_h);
	    // Draw the field name... color by the scaling
	    if (axis_mappers.containsKey(xi)) g2d.setColor(RTColorManager.getColor("label", "equal"));
	    else                              g2d.setColor(RTColorManager.getColor("label", "linear"));
	    g2d.drawString(fields[xi],          x0 - Utils.txtW(g2d,fields[xi])/2,           rc_h  - y_ins   + txt_h);
	  }
          // Render the visualization
          g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, blend));
          Iterator<Integer> it_axis = map.keySet().iterator();
	  while (it_axis.hasNext() && getRenderID() == currentRenderID()) {
            int  xi = it_axis.next(); int x0 = x_ins + xi*axis_w, x1 = x_ins + (xi+1)*axis_w; 
	    long y0_min = y_bounds[xi][0], y0_max = y_bounds[xi][1], y1_min = y_bounds[xi+1][0], y1_max = y_bounds[xi+1][1];
	    // Check for min max equals
	    if (y0_min == y0_max) { y0_min--; y0_max++; }
	    if (y1_min == y1_max) { y1_min--; y1_max++; }
            // Go through the keys and draw the corresponding lines
	    Iterator<String> it_key = map.get(xi).keySet().iterator();
	    while (it_key.hasNext() && getRenderID() == currentRenderID()) {
	      String                key = it_key.next();
	      BundlesCounterContext bcc = map.get(xi).get(key);
	      long                  wy0 = y0_lu.get(xi).get(key), 
	                            wy1 = y1_lu.get(xi).get(key);
              int                   sy0, sy1;
	      if (axis_mappers.containsKey(xi))   { sy0 = (int) (y_ins + graph_h - graph_h * axis_mappers.get(xi).get(wy0));
	      } else sy0 = (int) (y_ins + graph_h - (graph_h*(wy0 - y0_min))/(y0_max - y0_min));
	      if (axis_mappers.containsKey(xi+1)) { sy1 = (int) (y_ins + graph_h - graph_h * axis_mappers.get(xi+1).get(wy1));
	      } else sy1 = (int) (y_ins + graph_h - (graph_h*(wy1 - y1_min))/(y1_max - y1_min));

	      // Create and draw the line
              g2d.setColor(bcc.binColor(key));
	      g2d.setStroke(new BasicStroke(0.2f + (float) (bcc.totalNormalized(key) * 2.4)));
	      Line2D.Float line = new Line2D.Float(x0,sy0,x1,sy1);
	      g2d.draw(line);

	      // Update the mappings
	      geom_to_skey.put(line,key);
	      skey_to_geom.put(key,line);
	      skey_to_buns.put(key,bcc.getBundles(key));
	      Iterator<Bundle> it_bun = bcc.getBundles(key).iterator();
	      while (it_bun.hasNext()) {
	        Bundle bundle = it_bun.next();
		if (bun_to_geom.containsKey(bundle) == false) bun_to_geom.put(bundle, new HashSet<Shape>());
		bun_to_geom.get(bundle).add(line);
	      }
	    }
	  }
	  ts3 = System.currentTimeMillis();
         } finally { if (g2d != null) g2d.dispose(); } // Cleanup...
        }
        return base_bi;
      }
    }
  }
}

