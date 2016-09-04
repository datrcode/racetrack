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
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesDT;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;
import racetrack.util.Utils;
import racetrack.visualization.RTColorManager;

/**
 * Frame for capturing sparklines and providing management for their display.
 *
 *@author  D. Trimm
 *@version 0.1
 */
public class RTSparkLines extends JFrame {
  /**
   * Reference to main application
   */
  private RT rt;

  /**
   * Component for this frame
   */
  private SLComponent slcomp;

  /**
   * Private constructor.  Force the need to have exactly one instance of this
   * class.
   *
   *@param rt reference to main application
   */
  private RTSparkLines(RT rt) { 
    super("RT Sparklines"); 
    this.rt = rt; 
    slcomp = new SLComponent(); 
    getContentPane().add("Center", slcomp); 
  }

  /**
   * Singleton reference for the only instance of the class.
   */
  private static RTSparkLines singleton = null;

  /**
   * Return the singleton instance of this class.
   *
   *@param rt main application reference
   *
   *@return singleton instance of this class
   */
  public static RTSparkLines getSingleton(RT rt) {
    if (singleton == null) { singleton = new RTSparkLines(rt); singleton.pack(); singleton.setSize(200,400); }
    singleton.setVisible(true);
    return singleton;
  }

  /**
   * List of sparklines currently in the display
   */
  private List<SparkLine> sparklines = new ArrayList<SparkLine>();

  /**
   * Add a new sparkline to the visualization.
   *
   *@param bundles      subset of data
   *@param description  simple/short description of this sparkline
   *@param linear       use a linear scaling model
   *@param count_by     metric for counting
   */
  public void addSparkLine(Bundles bundles, String description, boolean linear, String count_by) {
    if (count_by == null) count_by = BundlesDT.COUNT_BY_DEFAULT;
    if (count_by.equals(BundlesDT.COUNT_BY_DEFAULT)) count_by = rt.getCountBy();
    sparklines.add(new SparkLine(bundles, description, linear, count_by));
    slcomp.repaint();
  }

  /**
   *
   */
  public void addSparkLine(SparkLine sparkline, int at) {
    sparklines.add(at, sparkline);
    slcomp.repaint();
  }

  /**
   *
   */
  public void removeSparkLine(SparkLine sparkline) {
    sparklines.remove(sparkline);
  }

  /**
   *
   */
  public class SLComponent extends JComponent implements MouseListener, MouseMotionListener {
    /**
     *
     */
    Map<SparkLine,BufferedImage> rendered_sparks = new HashMap<SparkLine,BufferedImage>();

    /**
     * Maximum width of the last description width calculation
     */
    int last_description_max_w = -1;

    /**
     *
     */
    int last_component_w = -1;

    /**
     * Popup Menu
     */
    JPopupMenu popup_menu = new JPopupMenu();
    /**
     * Radio button for choosing sparklines
     */
    JRadioButtonMenuItem sparks_rbmi,
    /**
     * Radio button for choosing horizons
     */
                         horizon_rbmi;
    /**
     *
     */
    public SLComponent() {
      ButtonGroup bg = new ButtonGroup();
      popup_menu.add(sparks_rbmi  = new JRadioButtonMenuItem("Sparklines")); bg.add(sparks_rbmi);
      popup_menu.add(horizon_rbmi = new JRadioButtonMenuItem("Horizons"));   bg.add(horizon_rbmi);

      addMouseListener(this);
      addMouseMotionListener(this);

      ChangeListener change_listener = new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          rendered_sparks.clear(); repaint(); } };

      sparks_rbmi.addChangeListener(change_listener);
      horizon_rbmi.addChangeListener(change_listener);
    }

    /**
     *
     */
    SparkLine sparkline_drag        = null;
    int       sparkline_drag_insert = -1;
    int       mx0                   = -1,
              my0                   = -1;

    /**
     *
     */
    public void mousePressed(MouseEvent me) {
      SparkLine sparkline  = sparkLineAt(me.getX(), me.getY()); if (sparkline == null) return;
      boolean   shift_down = (InputEvent.SHIFT_DOWN_MASK & me.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK;
      if (me.getButton() == MouseEvent.BUTTON1 && shift_down == false) {
	sparkline_drag = sparkline; mx0 = me.getX(); my0 = me.getY();
	repaint();
      }
    }

    /**
     *
     */
    public void mouseReleased(MouseEvent me) { 
      if (sparkline_drag != null) {
        removeSparkLine(sparkline_drag);
        addSparkLine(sparkline_drag, sparkline_drag_insert);
        sparkline_drag = null;
      }
      mx0            = -1;
      my0            = -1;
      repaint();
    }


    /**
     *
     */
    public void mouseClicked(MouseEvent me) {
      SparkLine sparkline  = sparkLineAt(me.getX(), me.getY()); if (sparkline == null) return;
      boolean   shift_down = (InputEvent.SHIFT_DOWN_MASK & me.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK;
      if (me.getButton() == MouseEvent.BUTTON1)        {
        if (shift_down) {
	  removeSparkLine(sparkline);
	  repaint();
	} else          {
          rt.push(sparkline.getBundles());
	}
      } else if (me.getButton() == MouseEvent.BUTTON3) {
        popup_menu.show(this, me.getX(), me.getY());
      }
    }

    /**
     *
     */
    public void mouseEntered(MouseEvent me) { }

    /**
     *
     */
    public void mouseExited(MouseEvent me) { }


    /**
     *
     */
    public void mouseMoved(MouseEvent me) {
      mx = me.getX();
      my = me.getY();
    }

    /**
     *
     */
    public void mouseDragged(MouseEvent me) {
      mx = me.getX();
      my = me.getY();
      if (sparkline_drag != null) repaint();
    }

    /**
     *
     */
    int mx, 

    /**
     *
     */
        my;

    /**
     *
     */
    private void renderIfNeeded() {
      // Check the max width first
      BufferedImage bi  = new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB);
      Graphics2D    g2d = (Graphics2D) bi.getGraphics(); int description_max_w = -1; int txt_h = Utils.txtH(g2d, "0");
      Iterator<SparkLine> it = sparklines.iterator();
      while (it.hasNext()) {
        SparkLine sparkline = it.next();
	int width = Utils.txtW(g2d,sparkline.getDescription());
	if (width > description_max_w) description_max_w = width;
      }
      g2d.dispose();

      // Figure out if there's a discrepancy...  if so, re-render them all...
      if ((description_max_w != last_description_max_w) || (last_component_w != getWidth())) {
        last_description_max_w = description_max_w;
        last_component_w       = getWidth();
        rendered_sparks.clear(); 
      }

      // Determine the global ts0 and ts1 for this render... it's possible that the root is no longer representative
      // of the overall dataset...
      long ts0 = rt.getRootBundles().ts0(), ts1 = rt.getRootBundles().ts1();
      it = sparklines.iterator();
      while (it.hasNext()) {
        SparkLine sparkline = it.next();
	if (sparkline.getBundles().ts0() < ts0) ts0 = sparkline.getBundles().ts0();
	if (sparkline.getBundles().ts1() > ts1) ts1 = sparkline.getBundles().ts1();
      }

      // Re-render the needed sparks
      it = sparklines.iterator();
      while (it.hasNext()) {
        SparkLine sparkline = it.next();
        if (rendered_sparks.containsKey(sparkline) == false) {
          rendered_sparks.put(sparkline, renderSpark(sparkline, horizon_rbmi.isSelected(), last_description_max_w, last_component_w, txt_h, ts0, ts1));
	}
      }
    }

    /**
     *
     */
    private BufferedImage renderSpark(SparkLine sparkline, boolean horizon_graph, int desc_max_w, int comp_w, int txt_h, long ts0, long ts1) {
      // Allocate the image and calculate the geometry
      BufferedImage bi  = new BufferedImage(comp_w, 2*txt_h, BufferedImage.TYPE_INT_RGB);
      Graphics2D    g2d = null;
     try {
      g2d = (Graphics2D) bi.getGraphics();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      RTColorManager.renderVisualizationBackground(bi, g2d);

      int x_lft   = 4 + desc_max_w, x_rgt = 2, y_top = 2, y_bot = 2;
      int graph_w = bi.getWidth() - (x_lft + x_rgt), graph_h = bi.getHeight() - (y_top + y_bot);
      int sx_min = bi.getWidth() + 1, sx_max = -1;
      Set<String> coords = new HashSet<String>();

      // Count the data items and allocate them to the map
      BundlesCounterContext bcc = new BundlesCounterContext(sparkline.getBundles(),sparkline.countBy(),null);
      Iterator<Tablet> it_tab = sparkline.getBundles().tabletIterator();
      while (it_tab.hasNext()) {
        Tablet tablet = it_tab.next();
        if (tablet.hasTimeStamps() && (sparkline.countBy().equals(BundlesDT.COUNT_BY_BUNS) || KeyMaker.tabletCompletesBlank(tablet, sparkline.countBy()))) {
	  Iterator<Bundle> it_bun = tablet.bundleIterator();
	  while (it_bun.hasNext()) {
	    Bundle bundle = it_bun.next();
            int    sx     = x_lft + (int) ((graph_w*(bundle.ts0() - ts0))/(ts1 - ts0));
	    if (sx < sx_min) sx_min = sx; if (sx > sx_max) sx_max = sx;
            bcc.count(bundle,""+sx); coords.add("" + sx);
	  }
	}
      }

      // Render the label
      g2d.setColor(RTColorManager.getColor(sparkline.getDescription()));
      g2d.drawString(sparkline.getDescription(), 1, bi.getHeight()/2 + txt_h/2);

      // Draw the actual sparkline
      if (horizon_graph) renderHorizonGraph(sparkline, bi, g2d, bcc, coords, y_top, graph_h, sx_min, sx_max);
      else               renderSparkLine(sparkline, bi, g2d, bcc, coords, y_top, graph_h, sx_min, sx_max);

      // Cleanup and return the image
     } finally {
       if (g2d != null) g2d.dispose();
     }
     return bi;
    }

    /**
     * Method to render the geometry of a sparkline.
     *
     *@param sparkline sparkline description
     *@param bi        image for rendering
     *@param g2d       graphics primitive
     *@param bcc       counter context describings counts for x coordinates
     *@param coords    set of x coordinates as strings
     *@param y_top     top inset for the image
     *@param graph_h   height of the actual graph
     *@param sx_min    minimum screen x coordinate
     *@param sx_max    maximum screen x coordinate
     */
    private void renderHorizonGraph(SparkLine sparkline, BufferedImage bi, Graphics2D g2d,
                                    BundlesCounterContext bcc, Set<String> coords,
                                    int y_top, int graph_h, int sx_min, int sx_max) {
      Composite orig_comp = g2d.getComposite();
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
      // Render the spark line
      // - Get the min/max
      int min_sx = -1, max_sx = -1;
      double min = bcc.total("" + sx_min), max = bcc.total("" + sx_min); 
      for (int sx=sx_min;sx<=sx_max;sx++) {
        if (coords.contains("" + sx)) {
          double val = bcc.total("" + sx);
	  if (val < min) { min = val; min_sx = sx; }
	  if (val > max) { max = val; max_sx = sx; }
        }
      }
      // - Render
      for (int sx=sx_min;sx<=sx_max;sx++) {
	if (coords.contains("" + sx)) {
	  int    height;
	  if (sparkline.linearScaling()) height = (int) (4*graph_h*bcc.totalNormalized(""+sx));
	  else                           height = (int) (4*graph_h*Math.log(bcc.total(""+sx))/Math.log(max));
	  while (height > 0) {
	    if (height >= graph_h) { g2d.drawLine(sx, y_top,                    sx, y_top + graph_h); height = height - graph_h; }
	    else                   { g2d.drawLine(sx, y_top + graph_h - height, sx, y_top + graph_h); height = 0;                }
          }
        } 
      }
      g2d.setComposite(orig_comp);
    }

    /**
     * Method to render the geometry of a sparkline.
     *
     *@param sparkline sparkline description
     *@param bi        image for rendering
     *@param g2d       graphics primitive
     *@param bcc       counter context describings counts for x coordinates
     *@param coords    set of x coordinates as strings
     *@param y_top     top inset for the image
     *@param graph_h   height of the actual graph
     *@param sx_min    minimum screen x coordinate
     *@param sx_max    maximum screen x coordinate
     */
    private void renderSparkLine(SparkLine sparkline, BufferedImage bi, Graphics2D g2d,
                                 BundlesCounterContext bcc, Set<String> coords,
                                 int y_top, int graph_h, int sx_min, int sx_max) {
      // Render the spark line
      // - Get the min/max
      int min_sx = -1, max_sx = -1;
      double min = bcc.total("" + sx_min), max = bcc.total("" + sx_min); if (min == max) min = 0.0;
      for (int sx=sx_min;sx<=sx_max;sx++) {
        if (coords.contains("" + sx)) {
          double val = bcc.total("" + sx);
	  if (val < min) { min = val; min_sx = sx; }
	  if (val > max) { max = val; max_sx = sx; }
        }
      }
      // - Render
      if (sparkline.linearScaling()) {
        int last_sy = -1;
        for (int sx=sx_min;sx<=sx_max;sx++) {
          double total;
	  if (coords.contains("" + sx)) total = bcc.total("" + sx); else total = 0.0;
	  if (total > 0.0) {
	    int    sy    = y_top + graph_h - (int) ((graph_h*(total-min))/(max-min));
	    if (last_sy >= 0) g2d.drawLine(sx-1,last_sy,sx,sy);
	    if (coords.contains("" + (sx+1)) == false) g2d.drawLine(sx,sy,sx+1,y_top+graph_h);
	    if (coords.contains("" + (sx-1)) == false) g2d.drawLine(sx-1,y_top+graph_h,sx,sy);
	    last_sy = sy;
	  } else           {
	    if (last_sy >= 0) g2d.drawLine(sx-1,last_sy,sx,y_top+graph_h);
	    last_sy = -1;
	  }
	}
      } else                         {
        if (min < 1) min = 1; if (max <= min) max = min+1; double logdiff = Math.log(max) - Math.log(min); // MOD FROM LINEAR LINES
        int last_sy = -1;
        for (int sx=sx_min;sx<=sx_max;sx++) {
          double total;
	  if (coords.contains("" + sx)) total = bcc.total("" + sx); else total = 0.0;
	  if (total > 0.0) {
	    int    sy    = y_top + graph_h - (int) ((graph_h*(Math.log(total)-Math.log(min)))/(logdiff)); // MOD FROM LINEAR LINES
	    if (last_sy >= 0) g2d.drawLine(sx-1,last_sy,sx,sy);
	    if (coords.contains("" + (sx+1)) == false) g2d.drawLine(sx,sy,sx+1,y_top+graph_h);
	    if (coords.contains("" + (sx-1)) == false) g2d.drawLine(sx-1,y_top+graph_h,sx,sy);
	    last_sy = sy;
	  } else           {
	    if (last_sy >= 0) g2d.drawLine(sx-1,last_sy,sx,y_top+graph_h);
	    last_sy = -1;
	  }
	}
      }
      // Draw a glyph for the min/max locations on the graph
      if (min_sx != -1) { g2d.setColor(RTColorManager.getColor("data", "min")); 
                          g2d.drawLine(min_sx-2,bi.getHeight()-1,min_sx+2,bi.getHeight()-1);
                          g2d.drawLine(min_sx-1,bi.getHeight()-2,min_sx+1,bi.getHeight()-2);
                          g2d.drawLine(min_sx-0,bi.getHeight()-3,min_sx+0,bi.getHeight()-3); }
      if (max_sx != -1) { g2d.setColor(RTColorManager.getColor("data", "max")); 
                          g2d.drawLine(max_sx-2,0,               max_sx+2,0);
                          g2d.drawLine(max_sx-1,1,               max_sx+1,0);
                          g2d.drawLine(max_sx-0,2,               max_sx+0,0); }
    }

    /**
     *
     */
    Map<SparkLine,Integer> sparkline_min = new HashMap<SparkLine,Integer>(),
                           sparkline_max = new HashMap<SparkLine,Integer>();

    /**
     *
     */
    public SparkLine sparkLineAt(int x, int y) {
      Iterator<SparkLine> it = sparklines.iterator();
      while (it.hasNext()) {
        SparkLine sparkline = it.next();
	if (y >= sparkline_min.get(sparkline) && y <= sparkline_max.get(sparkline))
	  return sparkline;
      }
      return null;
    }

    /**
     *
     */
    public void paintComponent(Graphics g) {
      // Clear the background
      g.setColor(RTColorManager.getColor("background","default")); g.fillRect(0,0,getWidth(),getHeight());

      // If drag...
      if (sparkline_drag != null) {
        boolean drawn = false; int insert = 0;
        BufferedImage bi_drag = rendered_sparks.get(sparkline_drag); if (bi_drag == null) return;
        int sy = 0;
	if (my < bi_drag.getHeight()) { g.drawImage(bi_drag, 0, sy, null); sy += bi_drag.getHeight(); drawn = true; sparkline_drag_insert = 0; }

        Iterator<SparkLine> it = sparklines.iterator();
        while (it.hasNext()) {
          SparkLine     sparkline = it.next(); if (sparkline == sparkline_drag) continue;
          BufferedImage bi        = rendered_sparks.get(sparkline);
	  if (bi != null) {
	    g.drawImage(bi, 0, sy, null); 
	    sy += bi.getHeight(); 
	    insert++;
	    if (my >= sy && my < sy+bi_drag.getHeight()) { g.drawImage(bi_drag, 0, sy, null); sy += bi_drag.getHeight(); drawn = true; sparkline_drag_insert = insert; }
	  }
        }

        if (!drawn) { g.drawImage(bi_drag, 0, my, null); }
      } else {
        renderIfNeeded(); int sy = 0; sparkline_min.clear(); sparkline_max.clear();
        Iterator<SparkLine> it = sparklines.iterator();
        while (it.hasNext()) {
          SparkLine     sparkline = it.next();
          BufferedImage bi        = rendered_sparks.get(sparkline);
          if (bi != null) { 
	    g.drawImage(bi, 0, sy, null); 
	    sparkline_min.put(sparkline,sy);
	    sparkline_max.put(sparkline,sy+bi.getHeight());
	    sy += bi.getHeight(); 
          }
	}
      }
    }
  }
}

/**
 *
 */
class SparkLine {
  /**
   *
   */
  Bundles bs;

  /**
   *
   */
  String  desc,

  /**
   *
   */
          count_by;

  /**
   *
   */
  boolean linear;

  /**
   *
   */
  public SparkLine(Bundles bs, String desc, boolean linear, String count_by) {
    this.bs = bs; this.desc = desc; this.linear = linear; this.count_by = count_by;
  }

  /**
   *
   */
  public String getDescription() { return desc; }

  /**
   *
   */
  public Bundles getBundles() { return bs; }

  /**
   *
   */
  public boolean linearScaling() { return linear; }

  /**
   *
   */
  public String countBy() { return count_by; }
}

