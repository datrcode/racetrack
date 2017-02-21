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
package racetrack.visualization;

import java.awt.Color;
import java.awt.Graphics2D; 
import java.awt.RenderingHints;

import java.awt.geom.Point2D;

import java.awt.image.BufferedImage;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesDT;
import racetrack.framework.BundlesG;
import racetrack.framework.BundlesRecs;
import racetrack.framework.BundlesUtils;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;

import racetrack.graph.GraphLayouts;
import racetrack.graph.GraphUtils;
import racetrack.graph.UniGraph;

import racetrack.util.StrCountSorter;
import racetrack.util.Utils;

/**
 * Class to help render the underlying stats about records...  used to interactively
 * show what's under the mouse in views.  Unfortunately, implementation requires this
 * class to match the record types to predefined types.
 *
 *@author  D. Trimm
 *@version 0.1
 */
public class StatsOverlay {
  /**
   * Tablet separated bundle sets
   */
  Map<Tablet,Set<Bundle>> tablet_sep = new HashMap<Tablet,Set<Bundle>>();

  /**
   * Tablet lookups for flavor and canonical field names
   */
  Map<Tablet,Map<String,Map<String,String>>> tablet_flavor_lu = new HashMap<Tablet,Map<String,Map<String,String>>>();

  /**
   * Original data
   */
  Set<Bundle> original;

  /**
   * Visible data (i.e., data that was rendered to the view)
   */
  Bundles     visible,
  /**
   * Root data
   */
              root;

  /**
   * Focus entities
   */
  Set<String> focus_entities;

  /**
   * Constructor
   *
   *@param original bundles under the mouse (kindof expects this to be a reasonable number -- less then 10000)
   *@param visible  all visible records
   *@param root     root bundles
   */
  public StatsOverlay(Set<Bundle> original, Bundles visible, Bundles root) { this(original, visible, root, null); }

  /**
   * Constructor
   *
   *@param original bundles under the mouse (kindof expects this to be a reasonable number -- less then 10000)
   *@param visible  all visible records
   *@param root     root bundles
   *@param focus    focus entities -- can be null, a String, or a Set of Strings
   */
  public StatsOverlay(Set<Bundle> original, Bundles visible, Bundles root, Object focus) {
    this.original = original; this.visible = visible; this.root = root; 
    if        (focus == null)           {
    } else if (focus instanceof String) { focus_entities = new HashSet<String>(); focus_entities.add((String) focus);
    } else if (focus instanceof Set)    { focus_entities = (Set<String>) focus; }

    if (original.size() < 20000) { // Stats only works for a reasonable amount of records...  reasonable should be adaptive
      // Separate into tablets
      Iterator<Bundle> it_bun = original.iterator(); while (it_bun.hasNext()) {
        Bundle bun    = it_bun.next(); 
        Tablet tablet = bun.getTablet(); if (tablet_sep.containsKey(tablet) == false) tablet_sep.put(tablet, new HashSet<Bundle>());
	tablet_sep.get(tablet).add(bun);
      }

      // Find the flavor for each tablet
      Iterator<Tablet> it_tab = tablet_sep.keySet().iterator(); while (it_tab.hasNext()) {
        Tablet tablet = it_tab.next(); Bundle bundle = tablet_sep.get(tablet).iterator().next(); Set<Bundle> set = new HashSet<Bundle>(); set.add(bundle);
        Map<String,Map<String,String>>  flavor_lu = dataFlavors(set);
	if (flavor_lu.keySet().size() > 0) {
          tablet_flavor_lu.put(tablet, flavor_lu);
	}
      }
    }
  }

  /**
   * Create the overlay image and return it.
   *
   *@param w_rec recommended width
   *@param h_rec recommended height
   *
   *@return overlay image
   */
  public BufferedImage overlay(int w_rec, int h_rec) {
    // Find the tablet with a flavor with the most records
    Iterator<Tablet> it_tab = tablet_sep.keySet().iterator(); Tablet tablet_max = null;
    while (it_tab.hasNext()) {
      Tablet tablet = it_tab.next();
      if      (tablet_max == null && 
               tablet_flavor_lu.containsKey(tablet))                              tablet_max = tablet;
      else if (tablet_max != null && 
               tablet_flavor_lu.containsKey(tablet) && 
               tablet_sep.get(tablet).size() > tablet_sep.get(tablet_max).size()) tablet_max = tablet;
    }

    // Find the right renderer
    if (tablet_max != null) {
      String flavor = tablet_flavor_lu.get(tablet_max).keySet().iterator().next();
      if (flavor.startsWith(NETFLOW_PREFIX)) return (new NetflowOverlay(flavor, tablet_max, w_rec, h_rec)).getOverlay();
      else                                   return (new GenericOverlay(                    w_rec, h_rec)).getOverlay();
    } else return (new GenericOverlay(w_rec,h_rec)).getOverlay();
  }
  
  /**
   * Generic implementation for any type of data.
   */
  class GenericOverlay {
    /**
     * Recommended width
     */
    int w_rec,
    /**
     * Recommended height
     */
        h_rec;
    /**
     * Construct an overlay with the specified width and height.
     *
     *@param w_rec recommended width
     *@param h_rec recommended height
     */
    public GenericOverlay(int w_rec, int h_rec) {
      // Check max record counts
      if (original.size() > 30000) return;

      // Create the makers
      createMakers();

      // Render the image
      bi  = new BufferedImage(w_rec, h_rec, BufferedImage.TYPE_INT_ARGB); g2d = null;
      try {
        g2d = (Graphics2D) bi.getGraphics(); txt_h = Utils.txtH(g2d, "0"); txt_h2 = txt_h/2; txt_h4 = txt_h/4;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.black); g2d.fillRect(0,0,bi.getWidth(),bi.getHeight());

        // Create the default strings that don't depend on the individual records
        String hdr_str = "" + original.size() + " Stats / " + visible.bundleSet().size() + " Render Recs";

	// Go over the bundles
	iterateOverBundles();

        // Render the strings
        x_brd   = 3; 
        y_brd   = 2;
        x_rgt_w = Utils.txtW(g2d, "- yyyy-mm-dd hh:mm:ss..") + 2*x_brd;
        x_rgt   = w_rec - x_rgt_w;
        y       = 0 + txt_h + y_brd;
      
        // Get rid of the non-stats area by manipulating the alpha mask
        for (int y0=0;y0<bi.getHeight();y0++) {
          int row[] = new int[bi.getWidth()];
          bi.getRGB(0, y0, bi.getWidth(), 1, row, 0, 1);
          for (int x0=0;x0<row.length;x0++) if (x0 < x_rgt-x_brd) row[x0] = 0x00ffffff & row[x0];
          bi.setRGB(0, y0, bi.getWidth(), 1, row, 0, 1);
        }

        // - Overall Stats
        g2d.setColor(Color.white); g2d.drawString(hdr_str, x_rgt + x_brd, y); y += txt_h + y_brd;

        // - Timeframes
        if (has_tm || has_dur) renderTimeFrame();

        // Histograms
        Collections.sort(histo_keys); Iterator<String> it_fld = histo_keys.iterator(); while (it_fld.hasNext()) {
	  String fld = it_fld.next();
          createHistogram(fld);
	}
        it_fld = histo_keys.iterator(); while (it_fld.hasNext()) {
	  String fld = it_fld.next();
          renderHistogram(fld, 3);
	}
      } finally { if (g2d != null) g2d.dispose(); }
    }

    /**
     * Create the histogram for the specified field
     */
    private void createHistogram(String fld) {
      // Count up the values
      List<StrCountSorter> list = new ArrayList<StrCountSorter>();
      Iterator<String>     it   = histo.get(fld).keySet().iterator(); while (it.hasNext()) {
        String key = it.next(); int val = histo.get(fld).get(key); list.add(new StrCountSorter(key,val));
      }
      Collections.sort(list);
      histo_lists.put(fld, list);
      if (list.size() > 0 && list.get(0).count() > max_histo_count) max_histo_count = list.get(0).count();
    }

    /**
     * Render the histogram for the specified field
     */
    private void renderHistogram(String fld, int entries) {
      List<StrCountSorter> list = histo_lists.get(fld);
      g2d.setColor(Color.yellow); g2d.drawString(fld, x_rgt + x_brd, y); y+=txt_h;
      for (int i=0;i<entries;i++) {
        if (i < list.size()) {
          int bar_w = (int) ((list.get(i).count() * (x_rgt_w - 6)) / max_histo_count);
	  g2d.setColor(Color.blue);  g2d.fillRect(x_rgt + 3, y - txt_h, bar_w, txt_h);
	  g2d.setColor(Color.white); g2d.drawString(list.get(i).toString(), x_rgt + 5, y - 2);
	}
	y += txt_h+1;
      }
    }

    /**
     * Histogram with the most counts
     */
    long max_histo_count = 1;

    /**
     * Histogram information - sorted
     */
    Map<String,List<StrCountSorter>> histo_lists = new HashMap<String,List<StrCountSorter>>();

    /**
     * Histogram information - read as Field to Entity to Record Count
     */
    Map<String,Map<String,Integer>> histo = new HashMap<String,Map<String,Integer>>();

    /**
     * Listing of the histo keys (used for sorting later)
     */
    List<String> histo_keys = new ArrayList<String>();

    /**
     * Go through the records and tabulate the histogram stats
     */
    private void iterateOverBundles() {
      Iterator<Bundle> it_bun = original.iterator(); while (it_bun.hasNext()) {
        Bundle bundle = it_bun.next(); Tablet tablet = bundle.getTablet();
        Iterator<String> it_fld = makers.get(tablet).keySet().iterator(); while (it_fld.hasNext()) {
          String fld = it_fld.next(); String keys[] = makers.get(tablet).get(fld).stringKeys(bundle);
	  for (int i=0;i<keys.length;i++) {
	    if (histo.get(fld).containsKey(keys[i]) == false) histo.get(fld).put(keys[i], 0);
	    histo.get(fld).put(keys[i], histo.get(fld).get(keys[i]) + 1);
	  }
	}
      }
    }

    /**
     * Return overlay image
     */
    BufferedImage bi;

    /**
     * Graphics primitive - only valid while the constructor is running
     */
    Graphics2D    g2d;

    /**
     * Makers on a per tablet basis
     */
    Map<Tablet,Map<String,KeyMaker>> makers = new HashMap<Tablet,Map<String,KeyMaker>>();
    /**
     * (at least some of the) bundles have timestamps
     */
    boolean has_tm  = false,
    /**
     * (at least some of the) bundles have a duration
     */
            has_dur = false;
    /**
     * Min timestamp value
     */
    long ts0 = Long.MAX_VALUE,
    /**
     * Max timestamp value
     */
         ts1 = Long.MIN_VALUE;
    /**
     * X border size (separation of columns)
     */
    int x_brd;
    /**
     * Y border size (separation of lines)
     */
    int y_brd;
    /**
     * Width of the right region
     */
    int x_rgt_w;
    /**
     * Right margin x value
     */
    int x_rgt;
    /**
     * Current y location (renders down the screen)
     */
    int y;
    /**
     * Text height
     */
    int txt_h,
    /**
     * Half text height
     */
        txt_h2,
    /**
     * Quarter text height
     */
	txt_h4;
    /**
     * Create the key makers for all of the fields
     */
    private void createMakers() {
      BundlesG globals = root.getGlobals();
      Iterator<Bundle> it = original.iterator(); while (it.hasNext()) {
        Bundle bundle = it.next(); Tablet tablet = bundle.getTablet(); 
	// For any new tablets, fill in the key makers
	if (makers.containsKey(tablet) == false) {
	  makers.put(tablet, new HashMap<String,KeyMaker>());
	  int fields[] = tablet.getFields(); for (int fld_i=0;fld_i<fields.length;fld_i++) {
	      if (fields[fld_i] >= 0) {
              String fld = globals.fieldHeader(fld_i);
	      makers.get(tablet).put(fld, new KeyMaker(tablet, fld));
	      // Make sure the entries are in the histogram counter
	      if (histo.containsKey(fld) == false) { histo.put(fld, new HashMap<String,Integer>()); histo_keys.add(fld); }
            }
	  }
        }
	//Track the minimum/maximum timestamp
	if        (bundle.hasDuration()) { has_tm = has_dur = true;
          if (bundle.ts0() < ts0) ts0 = bundle.ts0();
	  if (bundle.ts0() > ts1) ts1 = bundle.ts0();
	}
	else if (bundle.hasTime())     { has_tm = true;
          if (bundle.ts0() < ts0) ts0 = bundle.ts0();
	  if (bundle.ts1() > ts1) ts1 = bundle.ts1();
	}
      }
    }

    /**
     * Render information about the timeframe to include a graphical timeline.
     */
    private void renderTimeFrame() {
        String tm_str = Utils.exactDate(ts0); tm_str = tm_str.substring(0,tm_str.lastIndexOf(".")); 
        g2d.setColor(Color.yellow); g2d.drawString(tm_str, x_rgt + x_brd, y); y += txt_h + y_brd;
	       tm_str = "- " + Utils.exactDate(ts1); tm_str = tm_str.substring(0,tm_str.lastIndexOf("."));
        g2d.setColor(Color.orange); g2d.drawString(tm_str, x_rgt + x_brd, y);

        // Annotate with a relative view between root, visible, and these recs
        g2d.setColor(Color.lightGray);
        g2d.drawLine(x_rgt+2,         y+txt_h2, x_rgt+x_rgt_w-4, y+txt_h2);               // Timeline

        g2d.drawLine(x_rgt+2,         y+txt_h2+txt_h4, x_rgt+2,         y+txt_h2-txt_h4); // Root
        g2d.drawLine(x_rgt+x_rgt_w-4, y+txt_h2+txt_h4, x_rgt+x_rgt_w-4, y+txt_h2-txt_h4);

        g2d.setColor(Color.orange); int x0, x1;                                           // Visible
        x0 = (int) (((x_rgt_w-4) * (visible.ts0() - root.ts0()))/(root.ts1() - root.ts0())) + x_rgt + 2;
        x1 = (int) (((x_rgt_w-4) * (visible.ts1() - root.ts0()))/(root.ts1() - root.ts0())) + x_rgt + 2;
        g2d.drawLine(x0,y+txt_h2,x0-txt_h4,y+txt_h2-txt_h4); g2d.drawLine(x0,y+txt_h2,x0+txt_h4,y+txt_h2-txt_h4);
        g2d.drawLine(x1,y+txt_h2,x1-txt_h4,y+txt_h2-txt_h4); g2d.drawLine(x1,y+txt_h2,x1+txt_h4,y+txt_h2-txt_h4);
        g2d.drawLine(x0+txt_h4,y+txt_h2-txt_h4,x1-txt_h4,y+txt_h2-txt_h4);

        g2d.setColor(Color.yellow);                                                       // Stats Recs
        x0 = (int) (((x_rgt_w-4) * (ts0 - root.ts0()))/(root.ts1() - root.ts0())) + x_rgt + 2;
        x1 = (int) (((x_rgt_w-4) * (ts1 - root.ts0()))/(root.ts1() - root.ts0())) + x_rgt + 2;
        g2d.drawLine(x0,y+txt_h2,x0-txt_h4,y+txt_h2+txt_h4); g2d.drawLine(x0,y+txt_h2,x0+txt_h4,y+txt_h2+txt_h4);
        g2d.drawLine(x1,y+txt_h2,x1-txt_h4,y+txt_h2+txt_h4); g2d.drawLine(x1,y+txt_h2,x1+txt_h4,y+txt_h2+txt_h4);
        g2d.drawLine(x0+txt_h4,y+txt_h2+txt_h4,x1-txt_h4,y+txt_h2+txt_h4);

        y += 2*txt_h;
      }

    /**
     * Return the overlay for the stats.
     *
     *@return overlay image
     */
    public BufferedImage getOverlay() { return bi; }
  }

  /**
   * Specific implementation for netflow overlays.
   */
  class NetflowOverlay {
    /**
     * Flavor to render
     */
    String  flavor;
    /**
     * Tablet to render from (should really handle multiple tablets that match...)
     */
    Tablet  tablet;
    /**
     * Recommended width
     */
    int     w_rec,
    /**
     * Recommended Height
     */
            h_rec;
    /**
     * Tablet has timestamps
     */
    boolean has_tm  = false,

    /**
     * Talet has durations
     */
            has_dur = false;

    /**
     * Map for the canonical fields to the corresponding maker
     */
    Map<String,KeyMaker> makers = new HashMap<String,KeyMaker>();

    /**
     * Create the makers that correspond the canonical field names.
     */
    private void createMakers() {
      Iterator<String> it = tablet_flavor_lu.get(tablet).get(flavor).keySet().iterator();
      while (it.hasNext()) {
        String key = it.next();                                         // tablet name
        String val = tablet_flavor_lu.get(tablet).get(flavor).get(key); // canonical name
        makers.put(key, new KeyMaker(tablet, val));
      }
    }

    /**
     * Total packets
     */
    long pkts     = 0L,                
    /**
     * Total octets
     */
         octs     = 0L, 
    /**
     * Earliest timestamp
     */
         ts0      = Long.MAX_VALUE,    
    /**
     * Latest timestamp
     */
	 ts1      = Long.MIN_VALUE;
    /**
     * Minimum number of packets
     */
    int  pkt_min  = Integer.MAX_VALUE, 
    /**
     * Maximum number of packets
     */
         pkt_max  = Integer.MIN_VALUE;
    /**
     * Minimum number of octets
     */
    int  oct_min  = Integer.MAX_VALUE, 
    /**
     * Maximum number of octets
     */
         oct_max  = Integer.MIN_VALUE;
    /**
     * Minimum duration
     */
    long dur_min  = Long.MAX_VALUE,    
    /**
     * Maximum duration
     */
         dur_max  = Long.MIN_VALUE;
    /**
     * Src field counts (for histogram)
     */
    Map<String,Integer> src_recs = new HashMap<String,Integer>(),
    /**
     * Dst field counts (for histogram)
     */
                        dst_recs = new HashMap<String,Integer>();
    /**
     * Src field octet counts (for histogram)
     */
    Map<String,Long>    src_octs = new HashMap<String,Long>(),
    /**
     * Dst field octet counts (for histogram)
     */
                        dst_octs = new HashMap<String,Long>();

    /**
     * Iterate over the bundles and fill in data structures for overlay stats.
     */
    private void iterateOverBundles() {
      // BFS Boundary if specified entities included
      Set<String> bfs_boundary = new HashSet<String>();

      // Iterate once through the bundles
      Iterator<Bundle> it_bun = original.iterator(); while (it_bun.hasNext()) {
        Bundle bundle = it_bun.next();

	// Count packets and octets
        int pkt = -1, oct = -1;
        if (flavor.equals(NETFLOW_FULL) || flavor.equals(NETFLOW_VOLUME)) {
          pkt = (makers.get(PKTS).intKeys(bundle))[0];
	  oct = (makers.get(OCTS).intKeys(bundle))[0];

	  pkts += pkt; if (pkt_min > pkt) pkt_min = pkt; if (pkt_max < pkt) pkt_max = pkt;
	  octs += oct; if (oct_min > oct) oct_min = oct; if (oct_max < oct) oct_max = oct;
        }

	// Track time
	if        (has_dur) { if (bundle.ts0() < ts0) ts0 = bundle.ts0(); if (bundle.ts1() > ts1) ts1 = bundle.ts1();
	                      long dur = bundle.ts1() - bundle.ts0();
			      if (dur_min > dur) dur_min = dur; if (dur_max < dur) dur_max = dur;
	} else if (has_tm)  { if (bundle.ts0() < ts0) ts0 = bundle.ts0(); if (bundle.ts0() > ts1) ts1 = bundle.ts0(); }

	// Histogram srcs & dsts
        String str, proto = (makers.get(pro).stringKeys(bundle))[0], my_sip, my_dip;
        my_sip = str = (makers.get(sip).stringKeys(bundle))[0];               if (src_recs.containsKey(str) == false) src_recs.put(str, 0);  src_recs.put(str, src_recs.get(str) + 1);
        if (oct != -1) {                                                      if (src_octs.containsKey(str) == false) src_octs.put(str, 0L); src_octs.put(str, src_octs.get(str) + oct); }
        if (flavor.equals(NETFLOW_MINIMAL) == false) {
          str = (makers.get(spt).stringKeys(bundle))[0] + " (" + proto + ")"; if (src_recs.containsKey(str) == false) src_recs.put(str, 0);  src_recs.put(str, src_recs.get(str) + 1);
          if (oct != -1) {                                                    if (src_octs.containsKey(str) == false) src_octs.put(str, 0L); src_octs.put(str, src_octs.get(str) + oct); }
        }
        my_dip = str = (makers.get(dip).stringKeys(bundle))[0];               if (dst_recs.containsKey(str) == false) dst_recs.put(str, 0);  dst_recs.put(str, dst_recs.get(str) + 1);
        if (oct != -1) {                                                      if (dst_octs.containsKey(str) == false) dst_octs.put(str, 0L); dst_octs.put(str, dst_octs.get(str) + oct); }
        str = (makers.get(dpt).stringKeys(bundle))[0] + " (" + proto + ")";   if (dst_recs.containsKey(str) == false) dst_recs.put(str, 0);  dst_recs.put(str, dst_recs.get(str) + 1);
        if (oct != -1) {                                                      if (dst_octs.containsKey(str) == false) dst_octs.put(str, 0L); dst_octs.put(str, dst_octs.get(str) + oct); }

        // For XY, calculate the mins and maxes
        if (render_xy) {
          int sip_i = makers.get(sip).intKeys(bundle)[0],
	      spt_i = -1,
              dip_i = makers.get(dip).intKeys(bundle)[0],
              dpt_i = makers.get(dpt).intKeys(bundle)[0];

          if (sip_set.contains(sip_i) == false) { sip_set.add(sip_i); sip_list.add(sip_i); }
          if (dip_set.contains(dip_i) == false) { dip_set.add(dip_i); dip_list.add(dip_i); }
          if (dpt_set.contains(dpt_i) == false) { dpt_set.add(dpt_i); dpt_list.add(dpt_i); }

          if (flavor.equals(NETFLOW_MINIMAL) == false) {
	    spt_i = makers.get(spt).intKeys(bundle)[0];
            spt_set.add(spt_i); 
	    spt_list.add(spt_i); 
	  }
	}

        // Create the graph (assuming that there's no specific focus)
        if (focus_entities == null || focus_entities.size() == 0 || focus_entities.contains(my_sip) || focus_entities.contains(my_dip)) {
	  if (focus_entities != null && focus_entities.contains(my_sip) == false) bfs_boundary.add(my_sip);
	  if (focus_entities != null && focus_entities.contains(my_dip) == false) bfs_boundary.add(my_dip);
          unigraph.addNeighbor(my_sip,my_dip);
        }
      }

      // Do the second layer if focus entities were specified
      if (focus_entities != null && focus_entities.size() > 0) {
        it_bun = original.iterator(); while (it_bun.hasNext()) {
          Bundle bundle = it_bun.next(); String my_sip = (makers.get(sip).stringKeys(bundle))[0], my_dip = (makers.get(dip).stringKeys(bundle))[0];
	  if (focus_entities.contains(my_sip) || focus_entities.contains(my_dip)) { } else if (bfs_boundary.contains(my_sip) || bfs_boundary.contains(my_dip)) {
	    unigraph.addNeighbor(my_sip,my_dip);
	  }
        }
      }

      // Do a second pass for xy's
      if (render_xy) { 
        calculateLookups(sip_list, sip_to_sy); calculateLookups(spt_list, spt_to_sy);
        calculateLookups(dip_list, dip_to_sy); calculateLookups(dpt_list, dpt_to_sy);

        // Allocate the four xy scatterplots
        sip_xy_bi = new BufferedImage(256, xy_h, BufferedImage.TYPE_INT_RGB);
        spt_xy_bi = new BufferedImage(256, xy_h, BufferedImage.TYPE_INT_RGB);
        dip_xy_bi = new BufferedImage(256, xy_h, BufferedImage.TYPE_INT_RGB);
        dpt_xy_bi = new BufferedImage(256, xy_h, BufferedImage.TYPE_INT_RGB);

        // Draw the xy axis
        int dark_gray = 0x00404040;
        for (int my_y=0;my_y<=xy_h-8;my_y++) {
	  int my_x = 2+txt_h;
          sip_xy_bi.setRGB(my_x, 4+my_y, dark_gray); spt_xy_bi.setRGB(my_x, 4+my_y, dark_gray);
          dip_xy_bi.setRGB(my_x, 4+my_y, dark_gray); dpt_xy_bi.setRGB(my_x, 4+my_y, dark_gray);
	}
	for (int my_x=0;my_x<=(250-txt_h);my_x++) {
          int my_y = xy_h-4;
          sip_xy_bi.setRGB(my_x+2+txt_h, my_y, dark_gray); spt_xy_bi.setRGB(my_x+2+txt_h, my_y, dark_gray);
          dip_xy_bi.setRGB(my_x+2+txt_h, my_y, dark_gray); dpt_xy_bi.setRGB(my_x+2+txt_h, my_y, dark_gray);
	}

        // Acculate each pixel into a set of ports
	Map<Integer,Map<Integer,Set<Integer>>> sip_xy_map = new HashMap<Integer,Map<Integer,Set<Integer>>>(),
	                                       spt_xy_map = new HashMap<Integer,Map<Integer,Set<Integer>>>(),
	                                       dip_xy_map = new HashMap<Integer,Map<Integer,Set<Integer>>>(),
	                                       dpt_xy_map = new HashMap<Integer,Map<Integer,Set<Integer>>>();

        it_bun = original.iterator(); while (it_bun.hasNext()) {
	  Bundle bundle = it_bun.next();
          int sip_i = makers.get(sip).intKeys(bundle)[0],
              dip_i = makers.get(dip).intKeys(bundle)[0],
              dpt_i = makers.get(dpt).intKeys(bundle)[0];

          int x = (int) (((bundle.ts0() - visible.ts0()) * (250-txt_h))/(visible.ts1() - visible.ts0())) + 2 + txt_h;

          int sip_y = sip_to_sy.get(sip_i);
          int dip_y = dip_to_sy.get(dip_i);
	  int dpt_y = dpt_to_sy.get(dpt_i);

	  accum(sip_xy_map, x, sip_y, dpt_i);
	  accum(dip_xy_map, x, dip_y, dpt_i);
	  accum(dpt_xy_map, x, dpt_y, dpt_i);

          if (flavor.equals(NETFLOW_MINIMAL) == false) { // Minimal is missing source port
	    int spt_i = makers.get(spt).intKeys(bundle)[0];
            int spt_y = spt_to_sy.get(spt_i);
	    accum(spt_xy_map, x, spt_y, dpt_i);
	  }
        }

	// Go through the maps and plot the points
	plot(sip_xy_map, sip_xy_bi);
	plot(dip_xy_map, dip_xy_bi);
	plot(dpt_xy_map, dpt_xy_bi);

	if (flavor.equals(NETFLOW_MINIMAL) == false) plot(spt_xy_map, spt_xy_bi);
      }
    }

    /**
     * Accumulate ports in the xy map lookup
     */
    private void accum(Map<Integer,Map<Integer,Set<Integer>>> map, int my_x, int my_y, int my_dpt) {
      if (map.          containsKey(my_x) == false) map.put(my_x, new HashMap<Integer,Set<Integer>>());
      if (map.get(my_x).containsKey(my_y) == false) map.get(my_x).put(my_y, new HashSet<Integer>());
      map.get(my_x).get(my_y).add(my_dpt);
    }

    /**
     * Plot the mapped values into an xy scatter
     */
    private void plot(Map<Integer,Map<Integer,Set<Integer>>> map, BufferedImage my_bi) {
      Iterator<Integer> itx = map.keySet().iterator(); 
      while (itx.hasNext()) {
        int my_x = itx.next(); Iterator<Integer> ity = map.get(my_x).keySet().iterator(); 
	while (ity.hasNext()) {
          int my_y = ity.next(); int color_i = 0x00ffffff; 
          if (map.get(my_x).get(my_y).size() == 1) {
	    int dport = map.get(my_x).get(my_y).iterator().next();
	    color_i = Utils.strColor(""+dport).getRGB();
          }
          my_bi.setRGB(my_x, my_y, color_i);
        }
      }
    }

    /**
     * Calculate the lookups for a list of integer to the xy_h
     */
    private void calculateLookups(List<Integer> list, Map<Integer,Integer> to_sy) {
      Collections.sort(list);
      if        (list.size() == 1) { to_sy.put(list.get(0),   xy_h/2);
      } else if (list.size() == 2) { to_sy.put(list.get(0), 2*xy_h/3);
                                     to_sy.put(list.get(1), 1*xy_h/3);
      } else                       {
        for (int i=0;i<list.size();i++) {
          int sy = xy_h - ((xy_h-8) * i)/list.size() - 4;
	  to_sy.put(list.get(i), sy);
	}
      }
    }

    /**
     * Sets and lists for the XY scatter plots
     */
    Set<Integer> sip_set = new HashSet<Integer>(); List<Integer> sip_list = new ArrayList<Integer>(); 
    Set<Integer> spt_set = new HashSet<Integer>(); List<Integer> spt_list = new ArrayList<Integer>(); 
    Set<Integer> dip_set = new HashSet<Integer>(); List<Integer> dip_list = new ArrayList<Integer>(); 
    Set<Integer> dpt_set = new HashSet<Integer>(); List<Integer> dpt_list = new ArrayList<Integer>(); 
    Map<Integer,Integer> sip_to_sy = new HashMap<Integer,Integer>(),
                         spt_to_sy = new HashMap<Integer,Integer>(),
                         dip_to_sy = new HashMap<Integer,Integer>(),
                         dpt_to_sy = new HashMap<Integer,Integer>();
    /**
     * BufferedImages for the xy scatterplots
     */
    BufferedImage sip_xy_bi,
                  spt_xy_bi,
		  dip_xy_bi,
		  dpt_xy_bi;

    /**
     * Undirected graph - will need to be smaller than say 75 nodes to make it to stats overlay render
     */
    UniGraph unigraph = new UniGraph();

    /**
     * Render information about the timeframe to include a graphical timeline.
     */
    private void renderTimeFrame() {
        String tm_str = Utils.exactDate(ts0);
        g2d.setColor(Color.yellow); g2d.drawString(tm_str, x_rgt + x_brd, y); y += txt_h + y_brd;
	       tm_str = "- " + Utils.exactDate(ts1);
        g2d.setColor(Color.orange); g2d.drawString(tm_str, x_rgt + x_brd, y);

        // Annotate with a relative view between root, visible, and these recs
        g2d.setColor(Color.lightGray);
        g2d.drawLine(x_rgt+2,         y+txt_h2, x_rgt+x_rgt_w-4, y+txt_h2);               // Timeline

        g2d.drawLine(x_rgt+2,         y+txt_h2+txt_h4, x_rgt+2,         y+txt_h2-txt_h4); // Root
        g2d.drawLine(x_rgt+x_rgt_w-4, y+txt_h2+txt_h4, x_rgt+x_rgt_w-4, y+txt_h2-txt_h4);

        g2d.setColor(Color.orange); int x0, x1;                                           // Visible
        x0 = (int) (((x_rgt_w-4) * (visible.ts0() - root.ts0()))/(root.ts1() - root.ts0())) + x_rgt + 2;
        x1 = (int) (((x_rgt_w-4) * (visible.ts1() - root.ts0()))/(root.ts1() - root.ts0())) + x_rgt + 2;
        g2d.drawLine(x0,y+txt_h2,x0-txt_h4,y+txt_h2-txt_h4); g2d.drawLine(x0,y+txt_h2,x0+txt_h4,y+txt_h2-txt_h4);
        g2d.drawLine(x1,y+txt_h2,x1-txt_h4,y+txt_h2-txt_h4); g2d.drawLine(x1,y+txt_h2,x1+txt_h4,y+txt_h2-txt_h4);
        g2d.drawLine(x0+txt_h4,y+txt_h2-txt_h4,x1-txt_h4,y+txt_h2-txt_h4);

        g2d.setColor(Color.yellow);                                                       // Stats Recs
        x0 = (int) (((x_rgt_w-4) * (ts0 - root.ts0()))/(root.ts1() - root.ts0())) + x_rgt + 2;
        x1 = (int) (((x_rgt_w-4) * (ts1 - root.ts0()))/(root.ts1() - root.ts0())) + x_rgt + 2;
        g2d.drawLine(x0,y+txt_h2,x0-txt_h4,y+txt_h2+txt_h4); g2d.drawLine(x0,y+txt_h2,x0+txt_h4,y+txt_h2+txt_h4);
        g2d.drawLine(x1,y+txt_h2,x1-txt_h4,y+txt_h2+txt_h4); g2d.drawLine(x1,y+txt_h2,x1+txt_h4,y+txt_h2+txt_h4);
        g2d.drawLine(x0+txt_h4,y+txt_h2+txt_h4,x1-txt_h4,y+txt_h2+txt_h4);

        y += 2*txt_h;
      }

    /**
     * X border size (separation of columns)
     */
    int x_brd;
    /**
     * Y border size (separation of lines)
     */
    int y_brd;
    /**
     * Width of the right region
     */
    int x_rgt_w;
    /**
     * Right margin x value
     */
    int x_rgt;
    /**
     * Current y location (renders down the screen)
     */
    int y;
    /**
     * Text height
     */
    int txt_h,
    /**
     * Half text height
     */
        txt_h2,
    /**
     * Quarter text height
     */
	txt_h4,
    /**
     * Height of each xy scatterplot
     */
        xy_h;
    /**
     * Flag indicating that the xy's should be rendered
     */
    boolean render_xy;
    /**
     * Overlay image
     */
    BufferedImage bi;
    /**
     * Graphics object for rendering (only valid during constructor)
     */
    Graphics2D    g2d;

    /**
     * Constructor for netflow overlays
     *
     *@param flavor  flavor for overlay
     *@param tablet  tablet containing netflow data
     *@param w_rec   recommended width
     *@param h_rec   recommended height
     */
    public NetflowOverlay(String flavor, Tablet tablet, int w_rec, int h_rec) {
      // Set the default variables
      this.flavor = flavor; this.tablet = tablet; this.w_rec = w_rec; this.h_rec = h_rec;
      has_tm  = tablet.hasTimeStamps(); has_dur = tablet.hasDurations();

      // Create the makers -- map will be canonical name to the maker for that specific translation
      createMakers();

      // Create the image and render
      bi  = new BufferedImage(w_rec, h_rec, BufferedImage.TYPE_INT_ARGB); g2d = null;
      try {
        g2d = (Graphics2D) bi.getGraphics(); txt_h = Utils.txtH(g2d, "0"); txt_h2 = txt_h/2; txt_h4 = txt_h/4;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.black); g2d.fillRect(0,0,bi.getWidth(),bi.getHeight());

        // Create the default strings that don't depend on the individual records
        String hdr_str = "" + original.size() + " Stats / " + visible.bundleSet().size() + " Render Recs";

        // Calculate the geometry
        xy_h    = 1 + (bi.getHeight() - 256)/4; if (xy_h > 96) xy_h = 96;
        x_brd   = 3; 
        y_brd   = 2;
        x_rgt_w = Utils.txtW(g2d, " 000.000.000.000 000.000.000.000 ") + 2*x_brd;
        x_rgt   = w_rec - x_rgt_w;
        y       = 0 + txt_h + y_brd;
        if (xy_h > 24 && (has_tm || has_dur)) render_xy = true; else render_xy = false;

	// Go over the bundles
	iterateOverBundles();

        // Render the strings
        // Get rid of the non-stats area by manipulating the alpha mask
        for (int y0=0;y0<bi.getHeight();y0++) {
          int row[] = new int[bi.getWidth()];
          bi.getRGB(0, y0, bi.getWidth(), 1, row, 0, 1);
          for (int x0=0;x0<row.length;x0++) if (x0 < x_rgt-x_brd-256) row[x0] = 0x00ffffff & row[x0];
          bi.setRGB(0, y0, bi.getWidth(), 1, row, 0, 1);
        }

        // - Overall Stats
        g2d.setColor(Color.white); g2d.drawString(hdr_str, x_rgt + x_brd, y); y += txt_h + y_brd;

        // - Timeframes
        if (has_tm || has_dur) renderTimeFrame();

        // - Durations
        if (has_dur) {
          g2d.setColor(Color.green); g2d.drawString("Dur: " + Utils.humanReadableDuration(dur_min) + " - " + Utils.humanReadableDuration(dur_max), x_rgt + x_brd, y); y += txt_h + y_brd;
        }

        // - Min and Max Packets, Octets, Durations
        if (flavor.equals(NETFLOW_FULL) || flavor.equals(NETFLOW_VOLUME)) {
          g2d.setColor(Color.yellow); g2d.drawString("Pkt: " + Utils.humanReadable(pkt_min) + " - " + Utils.humanReadable(pkt_max) + " | " + Utils.humanReadable(pkts) + " Tot",
                                                     x_rgt + x_brd, y); y += txt_h + y_brd;
	  g2d.setColor(Color.orange); g2d.drawString("Oct: " + Utils.humanReadable(oct_min) + " - " + Utils.humanReadable(oct_max) + " | " + Utils.humanReadable(octs) + " Tot",
                                                     x_rgt + x_brd, y); y += txt_h + y_brd;
        }

        // Histograms
        int histos_count; // number of lines for the histogram
        if (flavor.equals(NETFLOW_FULL) || flavor.equals(NETFLOW_VOLUME)) {
          histos_count = 1 + ((bi.getHeight() - (y + 2*txt_h))/txt_h)/2;
        } else {
          histos_count = 1 + ((bi.getHeight() - (y + 2*txt_h))/txt_h)/1;
        }

        List<StrCountSorter> srcs = new ArrayList<StrCountSorter>(),
                             dsts = new ArrayList<StrCountSorter>();
	Iterator<String>     it   = null;
        it = src_recs.keySet().iterator(); while (it.hasNext()) { String key = it.next(); srcs.add(new StrCountSorter(key, (long) src_recs.get(key))); } Collections.sort(srcs);
        it = dst_recs.keySet().iterator(); while (it.hasNext()) { String key = it.next(); dsts.add(new StrCountSorter(key, (long) dst_recs.get(key))); } Collections.sort(dsts);
        renderHistogram("Recs", srcs, dsts, histos_count);

        // Octet Histogram
        if (flavor.equals(NETFLOW_FULL) || flavor.equals(NETFLOW_VOLUME)) {
          // Histograms
          srcs = new ArrayList<StrCountSorter>();
          dsts = new ArrayList<StrCountSorter>();
          it = src_octs.keySet().iterator(); while (it.hasNext()) { String key = it.next(); srcs.add(new StrCountSorter(key, src_octs.get(key))); } Collections.sort(srcs);
          it = dst_octs.keySet().iterator(); while (it.hasNext()) { String key = it.next(); dsts.add(new StrCountSorter(key, dst_octs.get(key))); } Collections.sort(dsts);
	  renderHistogram("Octets", srcs, dsts, histos_count);
        }

        // Determine if the second column should be rendered
        if (x_rgt - 256 > 0 ) {
          // Determine if the linknode graph should be rendered
          if (unigraph.getNumberOfEntities() <  200 && 
              bi.getHeight()                 >= 256) {
            // Create the map
            Map<String,Point2D> map = new HashMap<String,Point2D>();
            for (int i=0;i<unigraph.getNumberOfEntities();i++) map.put(unigraph.getEntityDescription(i), new Point2D.Double(0.0, 0.0));

            // Do a layout appropriate to the focus
	    if      (focus_entities == null || focus_entities.size() == 0) (new GraphLayouts()).executeLayoutAlgorithm(GraphLayouts.HYPERTREE_PLUS_STR, unigraph, new HashSet<String>(), map);
	    else if (focus_entities != null && focus_entities.size() == 1) (new GraphLayouts()).executeLayoutAlgorithm(GraphLayouts.BY_SOURCE_STR,      unigraph, focus_entities,        map);     
	    else if (focus_entities != null && focus_entities.size() >  1) (new GraphLayouts()).executeLayoutAlgorithm(GraphLayouts.FOCUS_SELECTED_STR, unigraph, focus_entities,        map);
	    else                                                           (new GraphLayouts()).executeLayoutAlgorithm(GraphLayouts.HYPERTREE_PLUS_STR, unigraph, new HashSet<String>(), map);

            // Render to a subimage
            BufferedImage graph_bi = GraphUtils.render(unigraph,map,256,256,Color.black,Color.white,Color.darkGray);
            // Place it in the lower corner
            g2d.drawImage(graph_bi, x_rgt - graph_bi.getWidth(), bi.getHeight() - graph_bi.getHeight(), null);
          }
          // Determine if the xy scatters (x = time) should be rendered
          if (render_xy) {
            g2d.drawImage(sip_xy_bi, x_rgt - sip_xy_bi.getWidth(), 0*xy_h, null);
            g2d.drawImage(spt_xy_bi, x_rgt - spt_xy_bi.getWidth(), 1*xy_h, null);
            g2d.drawImage(dpt_xy_bi, x_rgt - dpt_xy_bi.getWidth(), 2*xy_h, null);
            g2d.drawImage(dip_xy_bi, x_rgt - dip_xy_bi.getWidth(), 3*xy_h, null);
          }
        }
      } finally { if (g2d != null) g2d.dispose(); }
    }

    /**
     * Render a histogram.
     *
     *@param label label of histogram
     *@param srcs  source histogram information
     *@param dsts  destination histogram information
     */
    private void renderHistogram(String label, List<StrCountSorter> srcs, List<StrCountSorter> dsts, int lines) {
      y += txt_h/4;
      g2d.setColor(Color.white); g2d.drawString(label, x_rgt + x_rgt_w/2 - Utils.txtW(g2d, label)/2, y);
      y += txt_h/4;
      g2d.setColor(Color.green); g2d.drawString("Source", x_rgt + x_brd, y);
      g2d.setColor(Color.red);   g2d.drawString("Dest",   x_rgt + x_rgt_w - x_brd - Utils.txtW(g2d, "Dest"), y); y += txt_h + 2;
      int histo_w    = x_rgt_w/2 - 4;
      int x_hist_src = x_rgt + 2, x_hist_dst = x_rgt + x_rgt_w/2 + 2;
      Color darkGreen = new Color(0,97,2), darkRed = new Color(166, 14, 14);
      for (int i=0;i<lines;i++) {
        if (i < srcs.size()) { int bar_w = (int) (srcs.get(i).count() * histo_w / srcs.get(0).count()); 
	                       g2d.setColor(darkGreen);   g2d.fillRect(x_hist_src, y - txt_h, bar_w, txt_h);
			       g2d.setColor(Color.white); g2d.drawString(srcs.get(i).toString(), x_hist_src + 2, y-2); }
        if (i < dsts.size()) { int bar_w = (int) (dsts.get(i).count() * histo_w / dsts.get(0).count()); 
	                       g2d.setColor(darkRed);     g2d.fillRect(x_hist_dst, y - txt_h, bar_w, txt_h);
			       g2d.setColor(Color.white); g2d.drawString(dsts.get(i).toString(), x_hist_dst + 2, y-2); }
	y += txt_h + 1;
      }
    }

    /**
     * Return the overlay image.
     *
     *@return overlay image
     */
    public BufferedImage getOverlay() { return bi; }
  }

  /**
   * Identify data flavors within the sets of bundles.  For those identified, provide the field
   * mapping for canonical names.
   *
   *@param buns set of bundles (records)
   *
   *@return map from flavor name to another map of the field lookups
   */
  public static Map<String,Map<String,String>> dataFlavors(Set<Bundle> buns) {
    Set<Tablet>                    seen    = new HashSet<Tablet>(); // Tablets that have already been processed
    Map<String,Map<String,String>> flavors = new HashMap<String,Map<String,String>>();

    //
    // Go through the records -- get the tablet and try to line it up with a data flavor
    //
    Iterator<Bundle> it      = buns.iterator(); 
    while (it.hasNext()) {
      Bundle bundle = it.next(); 
      Tablet tablet = bundle.getTablet(); Bundles bundles = tablet.getBundles(); BundlesG globals = bundles.getGlobals();
      if (seen.contains(tablet)) continue; seen.add(tablet);

      //
      // Else check for the fields in this tablet
      //
      Iterator<String> it_flavor = flavor_map_lu.keySet().iterator();

      //
      // For each flavor...
      //
      while (it_flavor.hasNext()) {
        String             flavor           = it_flavor.next();              //System.err.println("** Flavor: " + flavor);
	Map<String,String> canonical_lu     = new HashMap<String,String>();
	Set<String[]>      canon_fields_set = flavor_map_lu.get(flavor);
	Iterator<String[]> it_canon_fields  = canon_fields_set.iterator();

	//
	// Check each set of canonical fields
	//
	while (it_canon_fields.hasNext()) {
	  String[] canon_fields = it_canon_fields.next();                    //System.err.print("  ** Canon Field \"" + canon_fields[0] + "\"");
	  for (int i=0;i<canon_fields.length;i++) {
	    String canon_field   = canon_fields[i]; 
	    int    canon_field_i = globals.fieldIndex(canon_field);

	    //
	    // If this tablet matches any of the alias for the canonical field, set the canonical field lookup
	    //
	    if (canon_field_i >= 0 && KeyMaker.tabletCompletesBlank(tablet, canon_field)) {
	      canonical_lu.put(canon_fields[0], canon_field);                //System.err.print(" => " + canon_field);
	    }
	  }
	                                                                     //System.err.println();
	}

	//
	// If we have a match for all the canonical fields for this tablet, update the flavors lookup map with the field mapping
	//
	if (canonical_lu.keySet().size() == canon_fields_set.size()) flavors.put(flavor, canonical_lu);
      }
    }
    return flavors;
  }

  /**
   * Default/Canonical Field Names
   */
  public static String sip   = "sip",
		       spt   = "spt",
		       pro   = "pro",
		       dpt   = "dpt",
		       dip   = "dip",
                       OCTS  = "OCTS",
                       PKTS  = "PKTS",
                       SOCTS = "SOCTS",
                       DOCTS = "DOCTS",
                       SPKTS = "SPKTS",
                       DPKTS = "DPKTS";

  /**
   * Helper strings for the flavor maps - provides a single place to update the aliases... should
   * probably be in a resource section somewhere...  The first string is the canonical name - the rest are aliases.
   */
  static String[]   sip_strs     = { sip,   "srcip", "src_ip",   "SrcIP", "id.orig_h"                                 },
		    dip_strs     = { dip,   "dstip", "dst_ip",   "DstIP", "id.resp_h"                                 },
                    pro_strs     = { pro,   "proto", "protocol", "Protocol","Proto"                                   },
                    spt_strs     = { spt,   "srcpt", "src_pt",   "srcport", "src_port", "SrcPort", "sport", "SrcPt", "Sport", "SPort", "id.orig_p" },
		    dpt_strs     = { dpt,   "dstpt", "dst_pt",   "dstport", "dst_port", "DstPort", "dport", "DstPt", "Dport", "DPort", "id.resp_p" },
                    oct_strs     = { OCTS,  "OCT",   "BYTES",    "BYTS",    "OCTETS",   "OCTET",   "BYT", "BYTE"      },
                    pkt_strs     = { PKTS,  "PKT",   "PACKETS",  "PACKET"                                             },
                    soct_strs    = { SOCTS, "SOCT",  "SBYTES",   "SBYT",    "SBYTE", "ORIG_IP_BYTES"                  }, 
                    spkt_strs    = { SPKTS, "SPKT",  "SPACKETS", "ORIG_PKTS"                                          },
                    doct_strs    = { DOCTS, "DOCT",  "DBYTES",   "DBYT",    "DBYTE", "RESP_IP_BYTES"                  }, 
                    dpkt_strs    = { DPKTS, "DPKT",  "DPACKETS", "RESP_PKTS"                                          };

  /**
   * Flavor string names
   */
  public static String NETFLOW_PREFIX      = "netflow",
                       NETFLOW_FULL        = NETFLOW_PREFIX + " (full)",
                       NETFLOW_MOSTLY_FULL = NETFLOW_PREFIX + " (~full)",
                       NETFLOW_VOLUME      = NETFLOW_PREFIX + " (vol)",
		       NETFLOW_DEFAULT     = NETFLOW_PREFIX,
		       NETFLOW_MINIMAL     = NETFLOW_PREFIX + " (min)";

  /**
   * Flavor map look -- contains the flavor names with key looks for the field and field aliases
   */
  public static Map<String,Set<String[]>> flavor_map_lu;
  static {
    flavor_map_lu = new HashMap<String,Set<String[]>>(); Set<String[]> set;
    //
    // NETFLOW FULL
    //
    set = new HashSet<String[]>(); set.add(sip_strs);  set.add(spt_strs);  set.add(pro_strs);  set.add(dpt_strs);  set.add(dip_strs); 
				   set.add(soct_strs); set.add(spkt_strs); set.add(doct_strs); set.add(dpkt_strs);
                                   set.add(oct_strs);  set.add(pkt_strs);  
    flavor_map_lu.put(NETFLOW_FULL,set); 
    //
    // NETFLOW MOSTLY FULL
    //
    set = new HashSet<String[]>(); set.add(sip_strs);  set.add(spt_strs);  set.add(pro_strs);  set.add(dpt_strs);  set.add(dip_strs); 
				   set.add(soct_strs); set.add(spkt_strs); set.add(doct_strs); set.add(dpkt_strs);
    flavor_map_lu.put(NETFLOW_MOSTLY_FULL,set); 
    //
    // NETFLOW VOLUME
    //
    set = new HashSet<String[]>(); set.add(sip_strs);  set.add(spt_strs);  set.add(pro_strs);  set.add(dpt_strs);  set.add(dip_strs); 
                                   set.add(oct_strs);  set.add(pkt_strs);  
    flavor_map_lu.put(NETFLOW_VOLUME,set); 
    //
    // NETFLOW DEFAULT
    //
    set = new HashSet<String[]>(); set.add(sip_strs);  set.add(spt_strs);  set.add(pro_strs);  set.add(dpt_strs);  set.add(dip_strs); 
    flavor_map_lu.put(NETFLOW_DEFAULT,set); 
    //
    // NETFLOW DEFAULT
    //
    set = new HashSet<String[]>(); set.add(sip_strs);  set.add(pro_strs);  set.add(dpt_strs);  set.add(dip_strs); 
    flavor_map_lu.put(NETFLOW_MINIMAL,set); 
  }

  /**
   * Test main procedure
   */
  public static void main(String args[]) {
    try {
      for (int i=0;i<args.length;i++) {
        Bundles bundles = new BundlesRecs();
	Set<Bundle> bundle_set = BundlesUtils.parse(bundles, null, new File(args[i]), new ArrayList<String>());

        Map<String,Map<String,String>> flavors = dataFlavors(bundle_set);
	System.err.println(args[i] + " :");
	Iterator<String> it = flavors.keySet().iterator(); 
        while (it.hasNext()) {
          String flavor = it.next();
          System.err.println("  " + flavor);
          //
          // Dump the canonical names as well
	  //
          Iterator<String> it_fld = flavors.get(flavor).keySet().iterator();
	  while (it_fld.hasNext()) {
	    String fld = it_fld.next(); String can = flavors.get(flavor).get(fld);
	    System.err.println("    " + fld + " => " + can);
	  }
        }
	System.err.println("");
      }
    } catch (Throwable t) { System.err.println("Throwable: " + t); }
  }
}

