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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.image.BufferedImage;

import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import racetrack.analysis.ClassicalMDS;
import racetrack.analysis.DistEq;
import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesG;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.BundlesDT;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;
import racetrack.util.Utils;
import racetrack.visualization.ColorScale;
import racetrack.visualization.RTColorManager;
import racetrack.visualization.TreeMap;

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
  JCheckBoxMenuItem     vary_color_cbmi,

  /**
   * Provide a sample of the distance distribution
   */
                        distance_distro_cbmi,

  /**
   * Annotate the re-use records (either origin or the x/y axis)
   */
                        annotate_reuse_cbmi;

  /**
   * Component to graphically depict/manipulate the distance equation for the MDS algorithm
   */
  // DistanceEquationComponent distance_equation_component; // previous implementation

  /**
   * Map of the field to field linkages.  Second map contains a weight lookup.  All
   * relationships need to be unidirectional.  Weight needs to be on the 0.0 to 1.0 scale.
   */
  Map<String,Map<String,Double>> fld_to_fld_weight = new HashMap<String,Map<String,Double>>();

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
    getRTPopupMenu().add(mi                = new JMenuItem("Distance Equation Dialog..."));  mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { distanceEquationDialog(); } } );

    getRTPopupMenu().addSeparator();

    getRTPopupMenu().add(mi                = new JMenuItem("Execute MDS..."));               mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { executeMDS();       } } ); mi.setEnabled(false);
    getRTPopupMenu().add(mi                = new JMenuItem("Classic MDS"));                  mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { classicMDS();       } } );
    getRTPopupMenu().add(mi                = new JMenuItem("Simple KMeans..."));             mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { showKMeansDialog(); } } );

    getRTPopupMenu().addSeparator();

    getRTPopupMenu().add(mi                = new JMenuItem("Org By Field..."));              mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { organizeByField();      } } );
    getRTPopupMenu().add(mi                = new JMenuItem("Sort Horiz By Count"));          mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { sortAxis(true);         } } );
    getRTPopupMenu().add(mi                = new JMenuItem("Sort Vert By Count"));           mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { sortAxis(false);        } } );

    getRTPopupMenu().addSeparator();

    getRTPopupMenu().add(mi                = new JMenuItem("Add Closest Rec Back"));         mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { addClosestRec();        } } ); mi.setEnabled(false);
    getRTPopupMenu().add(mi                = new JMenuItem("Sort Recs By Visible"));         mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { sortHiddenByVisible(false);  } } );
    getRTPopupMenu().add(mi                = new JMenuItem("Reuse Sort Recs"));              mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { sortHiddenByVisible(true);   } } );
    getRTPopupMenu().add(mi                = new JMenuItem("Randomize Placement"));          mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { randomizePlacement();        } } );

    getRTPopupMenu().addSeparator();

    getRTPopupMenu().add(distance_distro_cbmi = new JCheckBoxMenuItem("Distance Distro", true));

    getRTPopupMenu().addSeparator();

    ButtonGroup bg = new ButtonGroup();
    getRTPopupMenu().add(width_small_rbmi    = new JRadioButtonMenuItem("Fixed Width (Small)"));        bg.add(width_small_rbmi);
    getRTPopupMenu().add(width_medium_rbmi   = new JRadioButtonMenuItem("Fixed Width (Medium)", true)); bg.add(width_medium_rbmi);
    getRTPopupMenu().add(width_large_rbmi    = new JRadioButtonMenuItem("Fixed Width (Large)"));        bg.add(width_large_rbmi);
    getRTPopupMenu().add(width_vary_rbmi     = new JRadioButtonMenuItem("Vary Width"));                 bg.add(width_vary_rbmi);
    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(vary_color_cbmi     = new JCheckBoxMenuItem("Vary Color"));
    getRTPopupMenu().add(annotate_reuse_cbmi = new JCheckBoxMenuItem("Annotate Reuse", true));

    // Add the listeners
    defaultListener(width_small_rbmi);
    defaultListener(width_medium_rbmi);
    defaultListener(width_large_rbmi);
    defaultListener(width_vary_rbmi);
    defaultListener(vary_color_cbmi);
    defaultListener(annotate_reuse_cbmi);

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
   * Distance equation dialog
   */
  // JDialog distance_equation_dialog;
  DistanceEquationDialog distance_equation_dialog;

  /**
   * Create the distance equation dialog if it has not already been created.
   */
  protected void createDialogIfNecessary() {
    if (distance_equation_dialog == null) {
      Container container = getParent(); while (container instanceof Frame == false) container = container.getParent();
      distance_equation_dialog = new DistanceEquationDialog((Frame) container);
    }
  }

  /**
   * Create and show the distance equation dialog.
   */
  protected void distanceEquationDialog() {
    createDialogIfNecessary();
    distance_equation_dialog.setVisible(true);

    /* // Prior implementation
    if (distance_equation_dialog == null) {
      Container container = getParent(); while (container instanceof Frame == false) container = container.getParent();
      distance_equation_dialog = new JDialog((Frame) container, "Distance Equation", false);
      distance_equation_dialog.add("Center", distance_equation_component = new DistanceEquationComponent());
      distance_equation_dialog.pack(); distance_equation_dialog.setSize(256,256);
    }
    distance_equation_dialog.setVisible(true);
    */
  }

  /**
   * Execute the MDS algorithm
   */
  protected void executeMDS() {  }

  /**
   * Run the classic MDS algorithm on the data.
   */
  protected void classicMDS() { 
    Bundles      root     = getRTParent().getRootBundles();

    // Get the records into an ordered list...  that's how the results come back
    List<Bundle> order    = new ArrayList<Bundle>();
    order.addAll(root.bundleSet());

    // Create the distance equation
    DistEq  disteq = new DistEq(root, fld_to_fld_weight);

    // Calculate the distance matrix
    double       dmat[][] = new double[order.size()][order.size()];
    for (int i=0;i<dmat.length;i++) {
      for (int j=0;j<dmat[i].length;j++) {
        dmat[i][j] = disteq.d(order.get(i),order.get(j));
      }
    }

    // Run classical mds algorithm
    ClassicalMDS classical_mds = new ClassicalMDS(dmat);

    // Put the results into the xy coordinate system
    double       coords[][]    = classical_mds.getResults();
    for (int i=0;i<coords.length;i++) {
      bundle_to_wx.put(order.get(i), coords[i][0]);
      bundle_to_wy.put(order.get(i), coords[i][1]);
    }

    // Ask for a re-render
    recalculateWorldMinsAndMaxs();
    getRTComponent().render();
  }

  /**
   * Create and display the k-means dialog.
   */
  public void showKMeansDialog() {
    if (kmeans_dialog == null) {
      Container container = getParent(); while (container instanceof Frame == false) container = container.getParent();
      kmeans_dialog = new KMeansDialog((Frame) container);
    }
    kmeans_dialog.setVisible(true);
  }

  /**
   * KMeans dialog
   */
  KMeansDialog kmeans_dialog = null;

  /**
   * Dialog for KMeans.
   */
  class KMeansDialog extends JDialog {
    /**
     * Number of clusters (k)
     */
    JSlider    k_sl,

    /**
     * Number of iterations
     */
               iterations_sl;

    /**
     * Tag to apply to record to record cluster number (if blank, no tag)
     */
    JTextField tag_tf,

    /**
     * TextField for the k value
     */
               k_tf,

    /**
     * TextField for the iterations value
     */
               iterations_tf;

    /**
     * Create the k-means dialog.
     *
     *@param owner frame owner
     */
    public KMeansDialog(Frame owner) {
      super(owner, "Distance Equation", false);

      JPanel labels = new JPanel(new GridLayout(3,1,5,5));
      labels.add(new JLabel("Num of Clusters (K)"));
      labels.add(new JLabel("Iterations"));
      labels.add(new JLabel("Tag"));
      getContentPane().add("West", labels);

      JPanel comps = new JPanel(new GridLayout(3,1,5,5));
      comps.add(k_sl          = new JSlider(2, 30, 20));
      comps.add(iterations_sl = new JSlider(1, 30, 10));
      comps.add(tag_tf        = new JTextField());
      getContentPane().add("Center", comps);

      JPanel texts = new JPanel(new GridLayout(3,1,5,5));
      texts.add(k_tf          = new JTextField(5)); k_tf.         setText("" + k_sl.getValue());          k_tf.         setEnabled(false);
      texts.add(iterations_tf = new JTextField(5)); iterations_tf.setText("" + iterations_sl.getValue()); iterations_tf.setEnabled(false);
      getContentPane().add("East", texts);

      JPanel buttons = new JPanel(new FlowLayout()); JButton bt;
      buttons.add(bt = new JButton("Cluster")); bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { kMeans(k_sl.getValue(), iterations_sl.getValue(), tag_tf.getText()); } } );
      buttons.add(bt = new JButton("Close"));   bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { setVisible(false); } } );
      getContentPane().add("South", buttons);

      k_sl.         addChangeListener(new ChangeListener() { public void stateChanged(ChangeEvent ce) { k_tf.         setText("" + k_sl.getValue());          } } );
      iterations_sl.addChangeListener(new ChangeListener() { public void stateChanged(ChangeEvent ce) { iterations_tf.setText("" + iterations_sl.getValue()); } } );

      pack(); setSize(320,160);
    }
  }

  /**
   * Run a simplified k-means algorithm and then layout in a treemap.
   *
   *@param k            number of clusters
   *@param iters        number of iterations for th k-means algorithm
   *@param cluster_tag  tag to add to the records for their clusters... null means no tagging
   */
  protected void kMeans(int k, int iters, String cluster_tag) {
    Bundles root   = getRTParent().getRootBundles();
    DistEq  disteq = new DistEq(root, fld_to_fld_weight);

    // Add bundles randomly at first...
    List<Set<Bundle>> random = new ArrayList<Set<Bundle>>(); for (int i=0;i<k;i++) random.add(new HashSet<Bundle>());
    Iterator<Bundle> it = root.bundleIterator(); int mod = 0; while (it.hasNext()) { random.get(mod).add(it.next()); mod = (mod + 1)%k; }

    // Create the data structure for the clusters
    Map<DistEq.BundleCenter,Set<Bundle>> clusters = new HashMap<DistEq.BundleCenter,Set<Bundle>>();
    Set<Bundle>                          nan_set  = new HashSet<Bundle>();

    // Fill in the centers
    for (int i=0;i<random.size();i++) clusters.put(disteq.createBundleCenter(random.get(i)), random.get(i));

    // Keep track of the closeness of each bundle to it's assigned center
    Map<Bundle,Double> closeness_map = new HashMap<Bundle,Double>();

    // Run the iterations
    for (int iteration=0;iteration<iters;iteration++) {
      // Reform the cluster centers
      Map<DistEq.BundleCenter,Set<Bundle>> new_clusters = new HashMap<DistEq.BundleCenter,Set<Bundle>>();
      Iterator<DistEq.BundleCenter> centers = clusters.keySet().iterator(); while (centers.hasNext()) {
        DistEq.BundleCenter center = centers.next(); Set<Bundle> bundle_set = clusters.get(center);
	if (bundle_set.size() > 0) new_clusters.put(disteq.createBundleCenter(bundle_set), new HashSet<Bundle>());
      }

      // Make sure we still have k centers...  otherwise, add centers
      if (new_clusters.keySet().size() < k && closeness_map.keySet().size() > 0) {
        // Prep the closeness map for use
        double sorted[] = new double[closeness_map.keySet().size()]; Map<Double,Set<Bundle>> rmap = new HashMap<Double,Set<Bundle>>();
        it = closeness_map.keySet().iterator(); 
        for (int i=0;i<sorted.length;i++) {
          Bundle bundle = it.next();
          sorted[i] = closeness_map.get(bundle);
          if (rmap.containsKey(sorted[i]) == false) rmap.put(sorted[i], new HashSet<Bundle>());
          rmap.get(sorted[i]).add(bundle);
        }
        Arrays.sort(sorted);

        // Create fill in centers from the records that were furthest from their centers...
        // Use at least two records at a time so that records don't get stuck in a center
        int sorted_i = sorted.length - 1;
        while (new_clusters.keySet().size() < k && sorted_i >= 1) {
          Set<Bundle> set = new HashSet<Bundle>();
          set.addAll(rmap.get(sorted[sorted_i--]));
          set.addAll(rmap.get(sorted[sorted_i--]));
	  if (set.size() > 1) new_clusters.put(disteq.createBundleCenter(set), new HashSet<Bundle>());
        }
      }

      // Switch them into play
      clusters = new_clusters;

      // Fill in the clusters
      closeness_map.clear();
      it = root.bundleIterator(); while (it.hasNext()) {
        Bundle b = it.next(); DistEq.BundleCenter closest = null; double closest_d = Double.NaN;
        centers = clusters.keySet().iterator(); while (centers.hasNext()) {
          DistEq.BundleCenter center = centers.next();
	  double d      = disteq.d(b, center);
	  if (Double.isNaN(d) == false && (Double.isNaN(closest_d) || d < closest_d)) { closest = center; closest_d = d; }
        }
	  // Add this record to it's closest center
        if (closest != null) { clusters.get(closest).add(b); closeness_map.put(b, closest_d); } else nan_set.add(b);
      }
    }

    // Tag if set
    boolean new_tags = false;
    if (cluster_tag != null && cluster_tag.equals("") == false) {
      int cluster_no = 0;
      Iterator<DistEq.BundleCenter> centers = clusters.keySet().iterator(); while (centers.hasNext()) {
        DistEq.BundleCenter center = centers.next();
	Set<Bundle>         set    = clusters.get(center);
	root.replaceTypeValueTags(root.subset(set), cluster_tag + "=" + cluster_no);
        cluster_no++;
      }
      new_tags = true;
    }

    // Apply tree map layout
    applyTreeMapLayout(clusters);

    // If there are new tags, force update global options
    if (new_tags) { getRTParent().updateBys(); getRTParent().refreshAll(); } // Make sure the new option shows up

    // Show the cluster centers distribution
    if (cluster_centers_dialog == null) {
      Container container = getParent(); while (container instanceof Frame == false) container = container.getParent();
      cluster_centers_dialog = new ClusterCenterDistributionDialog((Frame) container, clusters);
    } else {
      cluster_centers_dialog.setClusters(clusters); cluster_centers_dialog.setVisible(true);
    }
  }

  /**
   * Dialog showing the distribution of clusters
   */
  ClusterCenterDistributionDialog cluster_centers_dialog;

  /**
   * Apply a treemap layout to a cluster data structure.
   *
   *@param clusters clusters data structure
   */
   public <K> void applyTreeMapLayout(Map<K,Set<Bundle>> clusters) {
     TreeMap            treemap = new TreeMap(clusters);
     Map<K,Rectangle2D> layout  = treemap.squarifiedTileMapping();
     Iterator<K> it = layout.keySet().iterator(); while (it.hasNext()) {
      K center = it.next(); Set<Bundle> set = clusters.get(center); Rectangle2D rect = layout.get(center);
      // Determine the adjusted rectangle to place them in
      double adj_x = rect.getX() + 0.1 * rect.getWidth(),
	     adj_y = rect.getY() + 0.1 * rect.getHeight(),
	     adj_w = rect.getWidth()  * 0.80, 
	     adj_h = rect.getHeight() * 0.80;

      // Determine the increments
      double x_count = Math.ceil(set.size() / adj_h);
      double y_count = Math.ceil(set.size() / x_count);
      // System.out.println("For " + rect + " ... x_count = " + x_count + " | y_count = " + y_count);
      double inc_x   = adj_w / x_count,
             inc_y   = adj_h / y_count;

      // Place the bundles
      Iterator<Bundle> itb = set.iterator();
      double x = adj_x, y = adj_y;
      while (itb.hasNext()) {
        Bundle b = itb.next(); bundle_to_wx.put(b, x); bundle_to_wy.put(b, y);
        x += inc_x; if (x > adj_x+adj_w) { x = adj_x; y += inc_y; }
      }
    }

    recalculateWorldMinsAndMaxs();
    getRTComponent().render();
  }

  /**
   * Dialog to show the distribution of clusters from their centers.
   */
  class ClusterCenterDistributionDialog extends JDialog {
    /**
     * Normalize the x axis - x axis contains the distances from the center
     */
    JCheckBox normalize_x_axis_cb,

    /**
     * Normalize the y axis - y axis contains the height of bars
     */
              normalize_y_axis_cb;

    /**
     * Visualization component
     */
    VizComponent viz_component;

    /**
     * Clusters to display
     */
    Map<DistEq.BundleCenter,Set<Bundle>> clusters;

    /**
     * Construct the dialog and make it visible
     */
    public ClusterCenterDistributionDialog(Frame owner, Map<DistEq.BundleCenter,Set<Bundle>> clusters) {
      super(owner, "Cluster Center Distros", false); this.clusters = clusters;
      getContentPane().add("Center", new JScrollPane(viz_component = new VizComponent()));

      JPanel bottom = new JPanel(new GridLayout(2,1,2,2));
      bottom.add(normalize_x_axis_cb = new JCheckBox("Normalize X", true));
      bottom.add(normalize_y_axis_cb = new JCheckBox("Normalize Y", true));
      getContentPane().add("South", bottom);

      addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent we) { setVisible(false); dispose(); } } );

      normalize_x_axis_cb.addItemListener(new ItemListener() { public void itemStateChanged(ItemEvent ie) { viz_component.repaint(); } } );
      normalize_y_axis_cb.addItemListener(new ItemListener() { public void itemStateChanged(ItemEvent ie) { viz_component.repaint(); } } );

      pack(); setVisible(true);
    }

    /**
     * Set a new set of clusters.
     *
     *@param new_clusters new clusters
     */
    public void setClusters(Map<DistEq.BundleCenter,Set<Bundle>> new_clusters) {
      clusters = new_clusters;
      viz_component.updateClusterInformation();
    }

    /**
     *
     */
    class VizComponent extends JComponent {
      /**
       * Cell width
       */
      final int cell_w     = 256,
  
      /**
       * Cell height
       */
                cell_h     = 40,
      /**
       * Cell bar width
       */
	        cell_bar_w = 8,
      /**
       * Buffer between cells and edges
       */
                cell_buf   = 4;
  
      /**
       * CellRenderer handles each cell which correlates to one center
       */
      class CellRenderer implements Comparable<CellRenderer> {
        DistEq.BundleCenter center; double max_d = 0.0; double ds[]; double sum = 0.0; double avg = 0.0;
        public CellRenderer(DistEq.BundleCenter center) {
          this.center = center; 
          this.ds     = new double[clusters.get(center).size()];
          Iterator<Bundle> it = clusters.get(center).iterator(); int i = 0;
          while (it.hasNext()) {
            double d = center.d(it.next()); 
            ds[i++] = d; sum += d; avg += d;
            if (d > max_d) max_d = d;
	  }
          if (ds.length > 0) avg = avg/ds.length;
        }

        /**
	 * Compare against another cell renderer.
	 *
	 *@param other to compare against
	 *
	 *@return ordering result
	 */
        public int compareTo(CellRenderer other) {
          if      (avg       > other.avg)       return -1;
	  else if (avg       < other.avg)       return  1;
	  else if (ds.length < other.ds.length) return -1;
	  else if (ds.length > other.ds.length) return  1;
	  else                                  return  0;
	}

	/**
	 * Return the max distance for this cell - max distance is the furthest record from the center.
	 *
	 *@return max distance
	 */
	public double maxDistance() { return max_d; }

	/**
	 * Return the maximum bar height for this cell.  This uses the internal max distance for the calculation.
	 *
	 *@return maximum bar height
	 */
	public int maxBarH()                 { return maxBarH(max_d); }
	
	/**
	 * Return the maximum bar height for this cell.  This uses an externally supplied max distance.
	 *
	 *@param adj_max_d max distance to use (for normalization purposes)
	 *
	 *@return maximum bar height
	 */
        public int maxBarH(double adj_max_d) {
	  int bins[] = calcBins(adj_max_d);
	  int max    = bins[0]; for (int i=1;i<bins.length;i++) if (max < bins[i]) max = bins[i];
	  return max;
        }

	/**
	 * Return the histogram bins based on the adjusted max distance parameter.
	 *
	 *@param adj_max_d max distance to use (for normalization purposes)
	 *
	 *@return bins indicating how many distances fall into each one
	 */
        protected int[] calcBins(double adj_max_d) {
	  int bins[] = new int[cell_w / cell_bar_w];
	  for (int i=0;i<ds.length;i++) {
	    int index = (int) ((ds[i] / adj_max_d) * (bins.length - 1));
	    bins[index]++;
	  }
	  return bins;
	}

        /**
	 * Render the cell.
	 *
	 *@param adj_max_d maximum distance to use (for normalization)
	 *@param adj_max_h maximum bar height to use (for normalization)
	 */
	public Image render(double adj_max_d, double adj_max_h) {
	  int           bins[] = calcBins(adj_max_d);
	  BufferedImage bi     = new BufferedImage(cell_w, cell_h, BufferedImage.TYPE_INT_RGB);
	  Graphics2D    g2d    = (Graphics2D) bi.getGraphics(); 
	  RTColorManager.renderVisualizationBackground(bi, g2d);

	  g2d.setColor(RTColorManager.getColor("data","default"));

	  for (int i=0;i<bins.length;i++) {
	    if (bins[i] > 0) {
	      int bar_h = (int) ((cell_h * bins[i]) / adj_max_h); if (bar_h == 0) bar_h = 1;
              g2d.fillRect(i * cell_bar_w, cell_h - 1 - bar_h, cell_bar_w - 1, cell_h);
	    }
	  }

          g2d.setColor(RTColorManager.getColor("label", "default"));
	  g2d.drawString("" + ds.length, cell_w - Utils.txtW(g2d, "" + ds.length) - 3, cell_h - 3);

	  g2d.dispose();

	  return bi;
	}
      }

      /**
       * List of the cell renderers...
       */
      List<CellRenderer> cells = new ArrayList<CellRenderer>();

      /**
       * Construct the visualization component by creating a list of cell renderers.
       */
      public VizComponent() { updateClusterInformation(); }

      /**
       * Update the cluster information.
       */
      public void updateClusterInformation() {
        int num_of_clusters = clusters.keySet().size();
	Dimension dimension = new Dimension(2*cell_buf + cell_w, num_of_clusters * (cell_buf + cell_h) + cell_buf);
	setPreferredSize(dimension); setMinimumSize(dimension); setMaximumSize(dimension);
	cells.clear();
	Iterator<DistEq.BundleCenter> it = clusters.keySet().iterator(); while (it.hasNext()) {
	  cells.add(new CellRenderer(it.next()));
	}
	Collections.sort(cells);
	repaint();
      }
      
      /**
       * Ask each renderer to create their portion.
       */
      public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g; g2d.setColor(RTColorManager.getColor("background", "default")); g2d.fillRect(0,0,getWidth(),getHeight());

	//
	// Normalize both x and y
	//
        if        (normalize_x_axis_cb.isSelected() && normalize_y_axis_cb.isSelected()) {

	  // Get the overall maximum distance
	  double max_d_overall = 0.0;
	  for (int i=0;i<cells.size();i++) {
            CellRenderer cr = cells.get(i);
	    if (cr.maxDistance() > max_d_overall) max_d_overall = cr.maxDistance();
	  }

	  // Get the overall maximum height
	  int max_bar_h_overall = 0;
	  for (int i=0;i<cells.size();i++) {
            CellRenderer cr = cells.get(i);
	    if (cr.maxBarH(max_d_overall) > max_bar_h_overall) max_bar_h_overall = cr.maxBarH(max_d_overall);
	  }

	  // Render
          int x = cell_buf, y = cell_buf;
          for (int i=0;i<cells.size();i++) { 
            CellRenderer cr = cells.get(i);
	    g.setColor(RTColorManager.getColor("axis", "major")); g.drawLine(x - 1, y, x - 1, y + cell_h + 1); g.drawLine(x - 1, y + cell_h + 1, x + cell_w + 1, y + cell_h + 1);
	    g.drawImage(cr.render(max_d_overall,max_bar_h_overall), x, y, null); 
	    y += cell_h + cell_buf; 
	  }

	//
	// Just normalize x (maximum distance)
	//
	} else if (normalize_x_axis_cb.isSelected()                                 ) {

	  // Get the overall maximum distance
	  double max_d_overall = 0.0;
	  for (int i=0;i<cells.size();i++) {
            CellRenderer cr = cells.get(i);
	    if (cr.maxDistance() > max_d_overall) max_d_overall = cr.maxDistance();
	  }

          int x = cell_buf, y = cell_buf;
          for (int i=0;i<cells.size();i++) { 
            CellRenderer cr = cells.get(i);
	    g.setColor(RTColorManager.getColor("axis", "major")); g.drawLine(x - 1, y, x - 1, y + cell_h + 1); g.drawLine(x - 1, y + cell_h + 1, x + cell_w + 1, y + cell_h + 1);
	    g.drawImage(cr.render(max_d_overall,cr.maxBarH(max_d_overall)), x, y, null); 
	    y += cell_h + cell_buf; 
	  }

	//
	// Just normalize y (bar height)
	//
	} else if (                                 normalize_y_axis_cb.isSelected()) {

	  // Get the overall maximum height
	  int max_bar_h_overall = 0;
	  for (int i=0;i<cells.size();i++) {
            CellRenderer cr = cells.get(i);
	    if (cr.maxBarH() > max_bar_h_overall) max_bar_h_overall = cr.maxBarH();
	  }

	  // Render
          int x = cell_buf, y = cell_buf;
          for (int i=0;i<cells.size();i++) { 
            CellRenderer cr = cells.get(i);
	    g.setColor(RTColorManager.getColor("axis", "major")); g.drawLine(x - 1, y, x - 1, y + cell_h + 1); g.drawLine(x - 1, y + cell_h + 1, x + cell_w + 1, y + cell_h + 1);
	    g.drawImage(cr.render(cr.maxDistance(),max_bar_h_overall), x, y, null); 
	    y += cell_h + cell_buf; 
	  }

	//
	// Don't normalize either
	//
	} else                                                                     {

          int x = cell_buf, y = cell_buf;
          for (int i=0;i<cells.size();i++) { 
            CellRenderer cr = cells.get(i);
	    g.setColor(RTColorManager.getColor("axis", "major")); g.drawLine(x - 1, y, x - 1, y + cell_h + 1); g.drawLine(x - 1, y + cell_h + 1, x + cell_w + 1, y + cell_h + 1);
	    g.drawImage(cr.render(cr.maxDistance(),cr.maxBarH()), x, y, null); 
	    y += cell_h + cell_buf; 
	  }
	}
      }
    }
  }

  /**
   * Add the closest record to the visible points back into the view.  Probably should only work with less than 100 visible points...
   */
  public void addClosestRec() {
  }

  /**
   *
   */
  public void randomizePlacement() {
    Bundles root = getRTParent().getRootBundles();
    Iterator<Bundle> it = root.bundleIterator(); while (it.hasNext()) {
      Bundle b = it.next();
      bundle_to_wx.put(b, Math.random()); bundle_to_wy.put(b, Math.random());
    }
    recalculateWorldMinsAndMaxs();
    getRTComponent().render();
  }

  /**
   * Last bundle used as an origin... has potential to make the whole application hang onto a root bundle [memory leak]
   */
  private Bundle last_origin = null,

  /**
   * Last bundle used as an x-axis... has potential to make the whole application hang onto a root bundle [memory leak]
   */
                 last_xaxis  = null,

  /**
   * Last bundle used as a y-axis... has potential to make the whole application hang onto a root bundle [memory leak]
   */
		 last_yaxis  = null;

  /**
   * Sort the currently filtered records (hidden records) by the visible ones.  If there is one record, it is used as the origin.  If there are
   * two records, they are used as x and y axes.  If there are three or more, a random bundle is selected as the origin.
   *
   *@param reuse_last reuse the last set origin or x, y axis bundles
   */
  public void sortHiddenByVisible(boolean reuse_last) {
    Bundles root = getRTParent().getRootBundles(), visible = getRTParent().getVisibleBundles(); 
    
    if        (reuse_last && (last_xaxis != null || last_origin != null)) {
      if (last_origin != null) sort(root, visible, last_origin);
      else                     sort(root, visible, last_xaxis, last_yaxis);
    } else if (visible.bundleSet().size() != 2)                           {
      last_xaxis = last_yaxis = null;
      last_origin = visible.bundleIterator().next();
      sort(root, visible, last_origin);
    } else                                                                {
      last_origin = null;
      Iterator<Bundle> it = visible.bundleIterator(); last_xaxis = it.next(); last_yaxis = it.next();
      sort(root, visible, last_xaxis, last_yaxis);
    }
  }

  /**
   * Sort the root bundles against a single origin bundle.
   *
   *@param root     root bundles
   *@param visible  visible bundles
   *@param origin   bundle to use as the origin
   */
  public void sort(Bundles root, Bundles visible, Bundle origin) {
    DistEq disteq = new DistEq(root, fld_to_fld_weight);

    // Aggregate by distance
    int max_set_size = 1; 
    Map<Double,Set<Bundle>> dmap = new HashMap<Double,Set<Bundle>>();
    Iterator<Bundle> itb = root.bundleIterator(); while (itb.hasNext()) {
      Bundle b = itb.next(); double d = disteq.d(origin, b); 
      if (dmap.containsKey(d) == false) dmap.put(d, new HashSet<Bundle>());
      dmap.get(d).add(b);
      // Keep track of maximum set size;
      if (dmap.get(d).size() > max_set_size) max_set_size = dmap.get(d).size();
    }

    bundle_to_wx.put(origin, 0.0); bundle_to_wy.put(origin, 0.0);

    Iterator<Double> itd = dmap.keySet().iterator();
    while (itd.hasNext()) {
      double d = itd.next(); Set<Bundle> set = dmap.get(d);
      double x = d; double y = 0.0;
      itb = set.iterator(); while (itb.hasNext()) {
        Bundle b = itb.next();
        bundle_to_wx.put(b, x);
	bundle_to_wy.put(b, y); y += 0.01;
      }
    }

    recalculateWorldMinsAndMaxs();
    getRTComponent().render();
  }

  /**
   * Sort the root bundles against two bundles that form an adhoc x and y axis.
   *
   *@param root     root bundles
   *@param visible  visible bundles
   *@param xaxis    bundle that forms xaxis distance origin
   *@param yaxis    bundle that forms yaxis distance origin
   */
  public void sort(Bundles root, Bundles visible, Bundle xaxis, Bundle yaxis) {
    DistEq disteq = new DistEq(root, fld_to_fld_weight);
    double bnan_x = -0.1, bnan_y = -0.1, // Both NaN coordinate start
           xnan_x =  0.1, xnan_y = -0.1, // Just x NaN coordinate start
	   ynan_x = -0.1, ynan_y =  0.1; // Just y NaN coordinate start

    Iterator<Bundle> it = root.bundleIterator();
    while (it.hasNext()) {
      Bundle b = it.next();
      double x = disteq.d(b, xaxis), y = disteq.d(b, yaxis);
      if (Double.isNaN(x) || Double.isNaN(y)) {
        if        (Double.isNaN(x) && Double.isNaN(y)) { x = bnan_x; y = bnan_y;
	                                                 bnan_x = bnan_x - 0.01; if (bnan_x < -1.0) { bnan_x = -0.1; bnan_y = bnan_y - 0.01; }
	} else if (Double.isNaN(x))                    { x = xnan_x; y = xnan_y;
	                                                 xnan_x = xnan_x + 0.01; if (xnan_x >  1.0) { xnan_x =  0.1; xnan_y = xnan_y - 0.01; }
	} else                                         { x = ynan_x; y = ynan_y;
	                                                 ynan_y = ynan_y + 0.01; if (ynan_y >  1.0) { ynan_y =  0.1; ynan_x = ynan_x - 0.01; }
        }
      } 
      bundle_to_wx.put(b, x); bundle_to_wy.put(b, y);
    }

    recalculateWorldMinsAndMaxs();
    getRTComponent().render();
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
   * Annotate the reuse bundles.
   *
   *@return true if reuse should be annotated
   */
  public boolean annotateReuse() { return annotate_reuse_cbmi.isSelected(); }

  /**
   * Set the annotate reuse flag.
   *
   *@param b true to annotate the reuse bundles
   */
  public void annotateReuse(boolean b) { annotate_reuse_cbmi.setSelected(b); }

  /**
   * True if the component should render the distance distribution.
   */
  public boolean renderDistanceDistro() { return distance_distro_cbmi.isSelected(); }

  /**
   * Set the flag to render the distance distribution.
   *
   *@param b flag setting
   */
  public void renderDistanceDistro(boolean b) { distance_distro_cbmi.setSelected(b); }

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
   * Organize all records by the current color (global color).
   *
   *@param keep_distinct keep each bundle distinct
   */
  public void organizeByColor(boolean keep_distinct) {
  }

  /**
   * Organize all records by a field selected via combobox.
   *
   *@param keep_distinct keep each bundle distinct
   */
  public void organizeByField() {
    Bundles  root    = getRTParent().getRootBundles();
    BundlesG globals = root.getGlobals();
    String flds[] = KeyMaker.blanks(globals, false, true, true, true);
    Object new_fld_sel = JOptionPane.showInputDialog(null, "Choose one", "Input", JOptionPane.INFORMATION_MESSAGE, null, flds, flds[0]); 

    // If the user chose a field, organize by that construct
    if (new_fld_sel != null) {
      String new_fld = (String) new_fld_sel;
      Map<String,Set<Bundle>> org = new HashMap<String,Set<Bundle>>();

      // Go through the tablets
      Iterator<Tablet> it_tab = root.tabletIterator(); while (it_tab.hasNext()) {
	Tablet tablet = it_tab.next(); 

	// If the tablet satisfies the blank, sort into different maps
	if (KeyMaker.tabletCompletesBlank(tablet, new_fld)) {
	  KeyMaker km = new KeyMaker(tablet, new_fld);
	  // Go through the bundles
	  Iterator<Bundle> it_bun = tablet.bundleIterator(); while (it_bun.hasNext()) {
            Bundle bundle = it_bun.next(); String keys[] = km.stringKeys(bundle);
	    if        (keys        == null || keys.length == 0) {
	      if (org.keySet().contains("Unmapped") == false) org.put("unmapped", new HashSet<Bundle>());
	      org.get("unmapped").add(bundle);
	    } else if (keys.length == 1) {
	      if (org.keySet().contains(keys[0]) == false) org.put(keys[0], new HashSet<Bundle>());
	      org.get(keys[0]).add(bundle);
	    } else {
	      StringBuffer sb  = new StringBuffer(); for (int i=0;i<keys.length;i++) { sb.append(keys[i]); sb.append(BundlesDT.DELIM); }
	      String       key = sb.toString();
	      if (org.keySet().contains(key) == false) org.put(key, new HashSet<Bundle>());
	      org.get(key).add(bundle);
	    }
	  }

        // Else, put it in an unmapped version
	} else {
	  if (org.keySet().contains("Unmapped") == false) org.put("unmapped", new HashSet<Bundle>());
	  org.get("unmapped").addAll(tablet.bundleSet());
	}
      }

      // Use a treemap layout
      applyTreeMapLayout(org);
    }
  }

  /**
   * Organize all records by the current count (global count).
   *
   *@param horiz in the horizontal (versus vertical) axis
   */
  public void sortAxis(boolean horiz) {
    // Get root... check the basics... setup the map
    Bundles root = getRTParent().getRootBundles(); String count_by = getRTParent().getCountBy();
    if (count_by.equals(BundlesDT.COUNT_BY_BUNS)) return;

    Map<Integer,Set<Bundle>> map = new HashMap<Integer,Set<Bundle>>();

    // Go through the tablets
    Iterator<Tablet> it_tab = root.tabletIterator(); while (it_tab.hasNext()) {
      Tablet tablet = it_tab.next();

      //
      // If applicable, setup the mapping
      //
      if (KeyMaker.tabletCompletesBlank(tablet, count_by)) {
	KeyMaker km = new KeyMaker(tablet, count_by);
        Iterator<Bundle> it_bun = tablet.bundleIterator(); while (it_bun.hasNext()) {
	  Bundle bundle = it_bun.next();
          int keys[] = km.intKeys(bundle);
	  if (keys != null && keys.length > 0) {
            if (map.containsKey(keys[0]) == false) map.put(keys[0], new HashSet<Bundle>());
	    map.get(keys[0]).add(bundle);
	  } else {
            if (map.containsKey(0) == false) map.put(0, new HashSet<Bundle>());
	    map.get(0).add(bundle);
	  }
        }

      //
      // Otherwise add them all to zero
      //
      } else {
        if (map.containsKey(0) == false) map.put(0, new HashSet<Bundle>());
	map.get(0).addAll(tablet.bundleSet());
      }
    }

    // Use the map to place on the correct axis
    Iterator<Integer> it_int = map.keySet().iterator(); while (it_int.hasNext()) {
      int value = it_int.next(); double value_d = (double) value;
      Iterator<Bundle> it_bun = map.get(value).iterator(); while (it_bun.hasNext()) {
        Bundle bundle = it_bun.next();
	if (horiz) bundle_to_wx.put(bundle, value_d);
	else       bundle_to_wy.put(bundle, value_d);
      }
    }

    recalculateWorldMinsAndMaxs();
    getRTComponent().render();
  }

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

      // Provide a border
      double delta = 1.0;
      delta = wx_max - wx_min;
      if        (Double.isNaN(delta)) { wx_max = 1.0;          wx_min = 0.0;
      } else if (delta == 0.0)        { wx_max = wx_max + 1.0; wx_min = wx_min - 1.0;
      } else                          { double ten = delta/20.0; wx_min -= ten; wx_max += ten; }

      delta = wy_max - wy_min;
      if        (Double.isNaN(delta)) { wy_max = 1.0;          wy_min = 0.0;
      } else if (delta == 0.0)        { wy_max = wy_max + 1.0; wy_min = wy_min - 1.0;
      } else                          { double ten = delta/20.0; wy_min -= ten; wy_max += ten; }

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
    createDialogIfNecessary();
    return "RTMDSPanel"                                       + BundlesDT.DELIM +
           "width="        + pointWidth()                     + BundlesDT.DELIM +
	   "vcolor="       + (varyColor() ? "true" : "false") + BundlesDT.DELIM +
           "annreuse="     + annotateReuse()                  + BundlesDT.DELIM +
	   "rendistro="    + renderDistanceDistro()           + BundlesDT.DELIM +
	   "disteqdialog=" + Utils.encToURL(distance_equation_dialog.getConfig());
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
      if        (type.equals("width"))        { pointWidth(value);
      } else if (type.equals("vcolor"))       { varyColor(value.toLowerCase().equals("true"));
      } else if (type.equals("annreuse"))     { annotateReuse(value.toLowerCase().equals("true"));
      } else if (type.equals("rendistro"))    { renderDistanceDistro(value.toLowerCase().equals("true"));
      } else if (type.equals("disteqdialog")) {
        createDialogIfNecessary();
	distance_equation_dialog.setConfig(Utils.decFmURL(value));
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
     * Sample size to use for the distance distro component
     */
    int distro_sample_size = 200;

    /**
     * Calculate the distance for a random sample of bundles.
     */
    protected void calculateDistanceDistro() {
      Bundles rb = getRTParent().getRootBundles(); 
      if (rb.size() < 5*distro_sample_size) { distro_bins = null; distro_uncompares = 0; return; } // make sure we have enough to choose from

      // Get a random sample
      Bundle sample[] = new Bundle[distro_sample_size]; int sample_i = 0;
      Iterator<Bundle> it = rb.bundleIterator();
      while (it.hasNext() && sample_i < sample.length) {
        Bundle bundle = it.next();
	if (Math.random() < ((double) distro_sample_size) / rb.size()) {
          sample[sample_i++] = bundle;
	}
      }
      if (sample_i != distro_sample_size) { // If we don't have enough, just fill with the first ones...
        it = rb.bundleIterator(); while (sample_i < sample.length) { sample[sample_i++] = it.next(); }
      }

      // Create the distance equation object
      DistEq disteq = new DistEq(rb, fld_to_fld_weight);

      // Do the compares
      int bins[] = new int[10]; int uncompares = 0;
      for (int i=1;i<sample.length;i++) {
        for (int j=0;j<i;j++) {
	  double score = disteq.d(sample[i], sample[j]);

	  if      (Double.isNaN(score)) uncompares++;
	  else if (score <  0.0)         bins[0            ]++;
	  else if (score >= 1.0)         bins[bins.length-1]++;
	  else {
	    int bin_i = (int) (score * bins.length); if (bin_i == bins.length) bin_i--;
	    bins[bin_i]++;
	  }
	}
      }

      // Set the method variables
      distro_bins = bins; distro_uncompares = uncompares;
      repaint();
    }

    /**
     * Provide a customization to show the spread for distance equation.
     */
    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g); Graphics2D g2d = (Graphics2D) g;
      RenderContext myrc = (RenderContext) getRTComponent().rc;

      // Render reuse bundles if the flag is set
      if (annotateReuse() && myrc != null) {
        g2d.setColor(RTColorManager.getColor("annotate", "labelfg"));
        Bundle origin_b = last_origin, x_b = last_xaxis, y_b = last_yaxis; int sx, sy;
	if (origin_b != null) {
          sx = myrc.worldXToScreenX(bundle_to_wx.get(origin_b));
          sy = myrc.worldYToScreenY(bundle_to_wy.get(origin_b));
	  g2d.draw(new Ellipse2D.Double(sx - 4, sy - 4, 9, 9));
	} else if (x_b != null && y_b != null) {
          sx = myrc.worldXToScreenX(bundle_to_wx.get(x_b));
          sy = myrc.worldYToScreenY(bundle_to_wy.get(x_b));
	  g2d.draw(new Rectangle2D.Double(sx - 5, sy - 3, 10, 6));
	  
          sx = myrc.worldXToScreenX(bundle_to_wx.get(y_b));
          sy = myrc.worldYToScreenY(bundle_to_wy.get(y_b));
	  g2d.draw(new Rectangle2D.Double(sx - 3, sy - 5, 6, 10));
	}
      }

      // Render the distance distribution if the flag is set
      if (renderDistanceDistro()) {
        Stroke orig_stroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(0.5f));
        // Render the scoring bins (if they exist)
        int bins[] = distro_bins; int uncompares = distro_uncompares; // copy to avoid race conditions
          if (bins != null && bins.length > 0) {
            int max   = uncompares; for (int i=0;i<bins.length;i++) if (bins[i] > max) max = bins[i];
	    int bar_w = 10; int x = getWidth() - bar_w * 14; int y = getHeight() - 3*Utils.txtH(g2d, "0")/2;
	    int bar_h = 50;

	    // Uncompares first
	    g2d.setColor(RTColorManager.getColor("label", "defaultbg")); g2d.fillRect(x, y - bar_h, bar_w, bar_h);
	    g2d.setColor(RTColorManager.getColor("label", "errorfg"));   g2d.drawRect(x, y - bar_h, bar_w, bar_h);
	    g2d.setColor(RTColorManager.getColor("label", "errorfg"));   int h = (uncompares * bar_h) / max; g2d.fillRect(x, y - h, bar_w, h);
	    g2d.drawString("n/a", x + bar_w/2 - Utils.txtW(g2d, "n/a")/2, y + Utils.txtH(g2d, "0"));
	    x += 3*bar_w;

	    // Comparison histogram
	    g2d.setColor(RTColorManager.getColor("label", "defaultfg")); g2d.drawString("0.0", x, y + Utils.txtH(g2d, "0"));
	    for (int i=0;i<bins.length;i++) {
	      g2d.setColor(RTColorManager.getColor("label", "defaultbg")); g2d.fillRect(x, y - bar_h, bar_w-1, bar_h);
	      g2d.setColor(RTColorManager.getColor("label", "defaultfg")); g2d.drawRect(x, y - bar_h, bar_w-1, bar_h);
              h = (bins[i] * bar_h) / max;
	      g2d.setColor(RTColorManager.getColor("label", "defaultfg")); g2d.fillRect(x, y - h, bar_w-1, h);
	      x += bar_w;
	    }
	    g2d.drawString("1.0", x - Utils.txtW(g2d, "1.0"), y + Utils.txtH(g2d, "0"));
        }
        g2d.setStroke(orig_stroke);
      }
    }

    /**
     * Samples that were binned by their distribution
     */
    int distro_bins[];

    /**
     * Number of samples that couldn't be compared for the distribution helper
     */
    int distro_uncompares = 0;
    
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
          if (tablet_can_count) {
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

  /**
   * Distance equation dialog.  Another attempt to implement a dynamic way for users to manipulate a
   * distance function.  This uses traditional Swing components...
   */
  class DistanceEquationDialog extends JDialog implements ItemListener, ChangeListener {
    /**
     * Maximum number of comparison parameters
     */
    final int params = 8;

    /**
     * Fields to be used for comparison
     * fields_cb[0][0] == first comparison, first field
     * fields_cb[0][1] == first comparison, second field (can be same as first field)
     */
    JComboBox fields_cb[][];

    /**
     * Textfields to count number of bundles that each field will apply to
     * infos_tf[0][0] == first comparison, first field
     * infos_tf[0][1] == first comparison, second field
     * infos_tf[0][2] == first comparison, intersection of the first and second field
     * infos_tf[0][3] == first comparison, weight info
     */
    JTextField infos_tf[][];

    /**
     * Weights for comparisons
     */
    JSlider   weight_sl[];

    /**
     * Combobox to choose interactivity options as weights and fields are adjusted
     */
    JComboBox interactivity_cb;

    /**
     * Get a configuration string for the dialog settings.
     */
    public String getConfig() {
      StringBuffer sb = new StringBuffer();
      for (int i=0;i<fields_cb.length;i++) {
        if (i > 0) sb.append(",");
        sb.append("field" + i + ":" + Utils.encToURL((String) fields_cb[i][0].getSelectedItem())  +
	                        ":" + Utils.encToURL((String) fields_cb[i][1].getSelectedItem()) +
				":" + weight_sl[i].getValue());
      }
      return sb.toString();
    }

    /**
     * Set the configuration string to restore a dialog settings.
     *
     *@param str  configuration string previously returned by getConfig().
     */
    public void setConfig(String str) {
      StringTokenizer settings = new StringTokenizer(str, ",");
      while (settings.hasMoreTokens()) {
        String setting = settings.nextToken();
        if (setting.startsWith("field")) {
	  StringTokenizer st = new StringTokenizer(setting, ":");
	  String fld = st.nextToken(),
	         s0  = Utils.decFmURL(st.nextToken()),
		 s1  = Utils.decFmURL(st.nextToken());
          int    v   = Integer.parseInt(st.nextToken());

	  int i = Integer.parseInt(fld.substring("field".length(), fld.length()));

	  fields_cb[i][0].setSelectedItem(s0);
	  fields_cb[i][1].setSelectedItem(s1);
	  weight_sl[i]   .setValue(v);
	} else throw new RuntimeException("Do not understand setting \"" + setting + "\"");
      }
    }

    /**
     * Construct the dialog
     */
    public DistanceEquationDialog(Frame owner) {
      super(owner, "Distance Equation", false);

      String fields[] = applicableFields();

      // Construct the gui
      JPanel panel                = new JPanel(new BorderLayout(3,3));
      JPanel fields_panel         = new JPanel(new GridLayout(0,2,3,3));
      JPanel slider_panel         = new JPanel(new BorderLayout(3,3));
      JPanel slider_actuals_panel = new JPanel(new GridLayout(0,1,3,3)); slider_panel.add("Center", slider_actuals_panel);
      JPanel slider_values_panel  = new JPanel(new GridLayout(0,1,3,3)); slider_panel.add("East",   slider_values_panel);

      fields_panel.add(new JLabel("Field 1"));
      fields_panel.add(new JLabel("Field 2"));
      slider_actuals_panel.add(new JLabel("Weight"));
      slider_values_panel. add(new JLabel("V"));

      fields_cb = new JComboBox  [params][2];
      infos_tf  = new JTextField [params][1];
      weight_sl = new JSlider    [params];
      for (int i=0;i<params;i++) {
        fields_panel.add(fields_cb[i][0] = new JComboBox(fields));           fields_cb[i][0].addItemListener(this);
	fields_panel.add(fields_cb[i][1] = new JComboBox(fields));           fields_cb[i][1].addItemListener(this);
	slider_actuals_panel.add(weight_sl[i]    = new JSlider(0,100,100));  weight_sl[i]   .addChangeListener(this);
	slider_values_panel. add(infos_tf [i][0] = new JTextField("1.00"));  infos_tf[i][0] .setEnabled(false);
      }

      panel.add("West",   fields_panel);
      panel.add("Center", slider_panel);
      add("Center", panel);

      add("South", interactivity_cb = new JComboBox(interaction_strs));

      pack();
    }

    /**
     * Interaction strings -- no interaction
     */
    final String INTERACT_NONE = "No Interaction",

    /**
     * Interaction strings -- sort by records
     */
                 INTERACT_SORT = "Sort in 1D/2D";

    /**
     * Interaction strings array
     */
    final String interaction_strs[] = { INTERACT_NONE, INTERACT_SORT };

    /**
     * Encode the settings into the fld_to_fld_wgt structure.
     *
     *@param e item event
     */
    public void itemStateChanged(ItemEvent e) { encodeParams(); }

    /**
     * Encode the settings into the fld_to_fld_wgt structure.
     *
     *@param e change event
     */
    public void stateChanged(ChangeEvent e) { encodeParams(); } 

    /**
     * Encode the settings into the fld_to_fld_wgt structure.
     */
    protected void encodeParams() {
      fld_to_fld_weight.clear();
      for (int i=0;i<fields_cb.length;i++) {
        String fld0 = (String) fields_cb[i][0].getSelectedItem(),
	       fld1 = (String) fields_cb[i][1].getSelectedItem();
        double wgt  = weight_sl[i].getValue() / 100.0;
	infos_tf[i][0].setText("" + wgt);

	if (fld0.equals(UNUSED_STR) || fld1.equals(UNUSED_STR)) {
	} else {
	  if (fld_to_fld_weight.containsKey(fld0) == false) fld_to_fld_weight.put(fld0, new HashMap<String,Double>());
	  fld_to_fld_weight.get(fld0).put(fld1, wgt);
	  if (fld_to_fld_weight.containsKey(fld1) == false) fld_to_fld_weight.put(fld1, new HashMap<String,Double>());
	  fld_to_fld_weight.get(fld1).put(fld0, wgt);
	}
      }

      // Force a resampling (messy...)
      if (renderDistanceDistro()) {
        ((RTMDSComponent) getRTComponent()).calculateDistanceDistro();
        ((RTMDSComponent) getRTComponent()).repaint();
      }

      // Check for interaction -- execute the correct method if it's set to an interactive value
      String interactivity = (String) interactivity_cb.getSelectedItem();
      if (interactivity.equals(INTERACT_SORT)) { sortHiddenByVisible(true); }
    }

    /**
     * String to indicate that the parameter it not used
     */
    final String UNUSED_STR = "|Unused|";

    /**
     * Return the fields applicable to distance comparisons.
     */
    public String[] applicableFields() {
      String fields[]    = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals(), false, true, true, true);
      String fields_pp[] = new String[fields.length + 1];
      System.arraycopy(fields, 0, fields_pp, 1, fields.length);
      fields_pp[0] = UNUSED_STR;
      return fields_pp;
    }
  }

  /**
   * Component to create distance metrics graphically.
   */
  class DistanceEquationComponent extends JComponent implements MouseListener, MouseMotionListener {
    /**
     * Last recorded mouse x coordinate
     */
    int mx,

    /**
     * Last recorded mouse y coordinate
     */
        my;

    /**
     * Mouse x coordinate at button press
     */
    int mx0,

    /**
     * Mouse y coordinate at button press
     */
        my0,

    /**
     * Mouse x coordinate while dragged
     */
        mx1,

    /**
     * Mouse y coordinate while dragged
     */
        my1;

    /**
     *
     */
    public DistanceEquationComponent() { addMouseListener(this); addMouseMotionListener(this); }

    /**
     *
     */
    protected void drawSelfReference(Graphics2D g2d, Rectangle2D rect) {
	  g2d.drawLine((int) rect.getCenterX(),                          (int) rect.getCenterY(), 
		       (int) (rect.getCenterX() + rect.getWidth()),      (int) rect.getCenterY());

	  g2d.drawLine((int) (rect.getCenterX() + rect.getWidth()),      (int) rect.getCenterY(),
	               (int) (rect.getCenterX() + rect.getWidth()),      (int) (rect.getCenterY() - rect.getHeight()));

	  g2d.drawLine((int) (rect.getCenterX() + rect.getWidth()),      (int) (rect.getCenterY() - rect.getHeight()),
	               (int) rect.getCenterX(),                          (int) (rect.getCenterY() - rect.getHeight()));

	  g2d.drawLine((int) rect.getCenterX(),                          (int) (rect.getCenterY() - rect.getHeight()),
	               (int) rect.getCenterX(),                          (int) rect.getCenterY());
    }

    /**
     *
     */
    public void paintComponent(Graphics g) {
      Graphics2D g2d = (Graphics2D) g; g2d.setColor(Color.black); g2d.fillRect(0,0,getWidth(),getHeight());
      Stroke     orig_stroke = g2d.getStroke();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      String in_fld = inField(mx, my);

      add_link_areas.clear();
      delete_areas.clear();

      String in_field = inField(mx,my);

      // Draw the interactions
      if (adding_link && fld_to_geom.containsKey(adding_link_source)) {
	if        (in_field == null)                    {
          g2d.setColor(Color.lightGray); g2d.setStroke(new BasicStroke(1.5f));
	  g2d.drawLine((int) fld_to_geom.get(adding_link_source).getCenterX(),
	               (int) fld_to_geom.get(adding_link_source).getCenterY(),
		       mx, my);
	} else if (in_field.equals(adding_link_source)) {
          g2d.setColor(Color.white); g2d.setStroke(new BasicStroke(2.0f));
	  drawSelfReference(g2d, fld_to_geom.get(in_field));
	} else                                          {
	  g2d.setColor(Color.white); g2d.setStroke(new BasicStroke(2.0f));
	  g2d.drawLine((int) fld_to_geom.get(adding_link_source).getCenterX(),
	               (int) fld_to_geom.get(adding_link_source).getCenterY(),
	               (int) fld_to_geom.get(in_field).getCenterX(),
	               (int) fld_to_geom.get(in_field).getCenterY());
	}
      }
      g2d.setStroke(orig_stroke);

      // Draw the links
      Iterator<String> it = fld_to_fld_weight.keySet().iterator();
      while (it.hasNext()) {
        String fld0 = it.next();
	Iterator<String> it2 = fld_to_fld_weight.get(fld0).keySet().iterator();
	while (it2.hasNext()) {
	  String fld1 = it2.next();
	  double w    = fld_to_fld_weight.get(fld0).get(fld1);
          g2d.setColor(Color.lightGray);
	  g2d.setStroke(new BasicStroke((float) (0.5f + 4.0f*w)));
	  if (fld0.equals(fld1)) {
	    Rectangle2D rect0 = fld_to_geom.get(fld0);
	    if (dragging_field && fld0.equals(dragging_field_str)) rect0  = new Rectangle2D.Double(mx1 - mx0 + rect0.getX(), my1 - my0 + rect0.getY(), rect0.getWidth(), rect0.getHeight());
	    drawSelfReference(g2d,rect0);
	  } else {
	    Rectangle2D rect0 = fld_to_geom.get(fld0), rect1 = fld_to_geom.get(fld1);

	    if (dragging_field && fld0.equals(dragging_field_str)) { rect0 = new Rectangle2D.Double(mx1 - mx0 + rect0.getX(), my1 - my0 + rect0.getY(), rect0.getWidth(), rect0.getHeight()); }
	    if (dragging_field && fld1.equals(dragging_field_str)) { rect1 = new Rectangle2D.Double(mx1 - mx0 + rect1.getX(), my1 - my0 + rect1.getY(), rect1.getWidth(), rect1.getHeight()); }

	    g2d.drawLine((int) rect0.getCenterX(), (int) rect0.getCenterY(), (int) rect1.getCenterX(), (int) rect1.getCenterY());
	  }
	}
      }
      g2d.setStroke(orig_stroke);

      // Draw the nodes
      it = fld_to_geom.keySet().iterator();
      while (it.hasNext()) {
        String      fld  = it.next();
	Rectangle2D rect = fld_to_geom.get(fld);

	if (dragging_field && fld.equals(dragging_field_str)) {
          rect = new Rectangle2D.Double(mx1 - mx0 + rect.getX(), my1 - my0 + rect.getY(), rect.getWidth(), rect.getHeight());
	}

	Area area = new Area(new Rectangle2D.Double(rect.getMinX() + 10, rect.getMinY(), rect.getWidth() - 20, rect.getHeight()));
        Ellipse2D delete_ellipse    = new Ellipse2D.Double(rect.getMinX(),        rect.getMinY(),   20, rect.getHeight());
        Ellipse2D delete_ellipse_sm = new Ellipse2D.Double(rect.getMinX()+3,      rect.getMinY()+3, 14, rect.getHeight() - 6);
	Ellipse2D link_ellipse      = new Ellipse2D.Double(rect.getMaxX() - 20,   rect.getMinY(),   20, rect.getHeight());
	Ellipse2D link_ellipse_sm   = new Ellipse2D.Double(rect.getMaxX()-3 - 14, rect.getMinY()+3, 14, rect.getHeight() - 6);
	area.add(new Area(delete_ellipse));
	area.add(new Area(link_ellipse));
	add_link_areas.put(fld, link_ellipse);
	delete_areas.put(fld, delete_ellipse);

	g2d.setColor(Color.darkGray);  g2d.fill(area);

	if (adding_link == false && in_fld != null && in_fld.equals(fld)) {
          g2d.setColor(Color.red);    g2d.fill(delete_ellipse_sm);
	  g2d.setColor(Color.yellow); g2d.fill(link_ellipse_sm);
	}

	g2d.setColor(Color.lightGray); g2d.draw(area);
	g2d.setColor(Color.white);     g2d.drawString(fld, (int) (rect.getCenterX() - Utils.txtW(g2d, fld)/2), (int) rect.getMaxY() - 3);
      }


      // Render the scoring bins (if they exist)
      int bins[] = score_bins; int uncompares = score_uncompares; // copy to avoid race conditions
      if (bins != null && bins.length > 0) {
        int max   = uncompares; for (int i=0;i<bins.length;i++) if (bins[i] > max) max = bins[i];
	int bar_w = 10; int x = getWidth() - bar_w * 14; int y = getHeight() - 3*Utils.txtH(g2d, "0")/2;
	int bar_h = 50;

	// Uncompares first
	g2d.setColor(Color.darkGray); g2d.fillRect(x, y - bar_h, bar_w, bar_h);
	g2d.setColor(Color.red); int h = (uncompares * bar_h) / max; g2d.fillRect(x, y - h, bar_w, h);
	g2d.drawString("n/a", x + bar_w/2 - Utils.txtW(g2d, "n/a")/2, y + Utils.txtH(g2d, "0"));
	x += 3*bar_w;

	// Comparison histogram
	g2d.setColor(Color.lightGray); g2d.drawString("0.0", x, y + Utils.txtH(g2d, "0"));
	for (int i=0;i<bins.length;i++) {
	  g2d.setColor(Color.darkGray); g2d.fillRect(x, y - bar_h, bar_w-1, bar_h);
          h = (bins[i] * bar_h) / max;
	  g2d.setColor(Color.lightGray); g2d.fillRect(x, y - h, bar_w-1, h);
	  x += bar_w;
	}
	g2d.drawString("1.0", x - Utils.txtW(g2d, "1.0"), y + Utils.txtH(g2d, "0"));
      }
    }

    /**
     * Map of the field string to their respective geometry within the component.
     */
    Map<String,Rectangle2D> fld_to_geom = new HashMap<String,Rectangle2D>();

    /**
     * Map of the delete buttons - filled in by render loop... not necessarily application data
     */
    Map<String,Shape> delete_areas = new HashMap<String,Shape>(),

    /**
     * Map of the add link buttons - filled in by render loop... not necessarily application data
     */
                      add_link_areas = new HashMap<String,Shape>();

    /**
     * For coordinate point, return the fld that is rendered in that location.  
     * If no geometry is found, return null.
     *
     *@param x x coordinate
     *@param y y coordinate
     *
     *@return field represented at this specific x/y coordinate - null if no field
     */
    protected String inField(int x, int y) {
      Iterator<String> it = fld_to_geom.keySet().iterator(); while (it.hasNext()) {
        String fld = it.next();
	if (fld_to_geom.containsKey(fld) && fld_to_geom.get(fld).contains(x, y)) return fld; 
      }
      return null;
    }

    /**
     * Mouse entered -- record location and grab focus.
     *
     *@param me mouse event info
     */
    public void mouseEntered(MouseEvent me) { record(me); grabFocus(); }

    /**
     * Mouse exited -- change all of the interactions to false.
     *
     *@param me mouse event info
     */
    public void mouseExited(MouseEvent me) { record(me); adding_link = false; adding_link_source = null; dragging_field = false; dragging_field_str = null; repaint(); }

    /**
     * Flag to indicate that field is being dragged
     */
    boolean dragging_field = false;

    /**
     * Actual field that is being dragged
     */
    String dragging_field_str = null;

    /**
     * Mouse pressed - handle a drag event.
     *
     *@param me mouse event object
     */
    public void mousePressed(MouseEvent me) {
      record(me); mx0 = mx1 = mx; my0 = my1 = my;
      if (me.getButton() == MouseEvent.BUTTON1) {
        String field = inField(me.getX(), me.getY());
	if (field != null) { dragging_field = true; dragging_field_str = field; repaint(); }
      }
    }

    /**
     * Mouse released - handle the end of a drag event
     *
     *@param me mouse event information
     */
    public void mouseReleased(MouseEvent me) { 
      record(me); mx1 = mx; my1 = my;
      if (dragging_field) {
        Rectangle2D rect = fld_to_geom.get(dragging_field_str);
        fld_to_geom.put(dragging_field_str, new Rectangle2D.Double(mx1 - mx0 + rect.getX(), my1 - my0 + rect.getY(), rect.getWidth(), rect.getHeight()));
	dragging_field     = false;
	dragging_field_str = null;
        repaint();
      }
    }

    /**
     * Adding link flag
     */
    boolean adding_link = false;

    /**
     * Source for adding link interaction
     */
    String adding_link_source = null;

    /**
     *
     */
    public void mouseClicked(MouseEvent me) { 
      record(me);
      if (me.getButton() == MouseEvent.BUTTON1) {
        String field = inField(me.getX(), me.getY());

	//
	// If no field, provide the field addition dialog
	//
        if (field == null) {
	  if (adding_link) { adding_link = false; adding_link_source = null; } else {
	    // Make a list of fields that have not yet been added
            BundlesG globals = getRTParent().getRootBundles().getGlobals();
            String flds[] = KeyMaker.blanks(globals, false, true, true, true);
	    List<String> flds_poss = new ArrayList<String>(); for (int i=0;i<flds.length;i++) if (fld_to_geom.containsKey(flds[i]) == false) flds_poss.add(flds[i]);
	    Object objs[] = new Object[flds_poss.size()]; for (int i=0;i<flds_poss.size();i++) objs[i] = flds_poss.get(i);

	    Object new_fld_sel = JOptionPane.showInputDialog(null, "Choose one", "Input", JOptionPane.INFORMATION_MESSAGE, null, objs, objs[0]); 
	    if (new_fld_sel != null) {
	      String new_fld = (String) new_fld_sel;
	      int    txt_w = Utils.txtW(new_fld), txt_h = Utils.txtH(new_fld);
	      fld_to_geom.put(new_fld, new Rectangle2D.Double(mx - txt_w/2 - 20, my - txt_h/2 - 3, txt_w + 40, txt_h + 6));
	      repaint();
	    }
          }
        //
	// See if it's the delete or add link area...
	//
        } else {
	  if (adding_link) {
            if (fld_to_fld_weight.containsKey(adding_link_source) == false) fld_to_fld_weight.put(adding_link_source, new HashMap<String,Double>());
            fld_to_fld_weight.get(adding_link_source).put(field, 1.0);
	    if (fld_to_fld_weight.containsKey(field)              == false) fld_to_fld_weight.put(field,              new HashMap<String,Double>());
            fld_to_fld_weight.get(field).put(adding_link_source, 1.0);
            adding_link = false; adding_link_source = null;
            calculateSampleScores();
	  } else           {
	    //
	    // Handle delete
	    //
	    if        (delete_areas.  containsKey(field) && delete_areas.  get(field).contains(me.getX(), me.getY())) {
	      fld_to_geom.remove(field);
	      fld_to_fld_weight.remove(field);
	      Iterator<String> it = fld_to_fld_weight.keySet().iterator(); while (it.hasNext()) { fld_to_fld_weight.get(it.next()).remove(field); }
              calculateSampleScores();
  
	    //
	    // Handle add link
	    //
	    } else if (add_link_areas.containsKey(field) && add_link_areas.get(field).contains(me.getX(), me.getY())) {
	      adding_link = true; adding_link_source = field;
              calculateSampleScores();
	    }
	  }
	}
      }
      repaint();
    }

    /**
     * Mouse move event -- record position information.
     *
     *@param me mouse event info
     */
    public void mouseMoved(MouseEvent me) { 
      record(me); 
      repaint();
    }

    /**
     * Mouse drag event -- just call repaint if they're dragging.
     *
     *@param me mouse event info
     */
    public void mouseDragged(MouseEvent me) { 
      record(me); 
      mx1 = mx; my1 = my;
      if (dragging_field) repaint();
    }

    /**
     * Record the information from the specified mouse event.
     *
     *@param me mouse event to capture information from
     */
    protected void record(MouseEvent me) { mx = me.getX(); my = me.getY(); }

    /**
     * Calculate the distance for a random sample of bundles.
     */
    protected void calculateSampleScores() {
      Bundles rb = getRTParent().getRootBundles(); 
      if (rb.size() < 5*score_sample_size) { score_bins = null; score_uncompares = 0; return; } // make sure we have enough to choose from

      // Get a random sample
      Bundle sample[] = new Bundle[score_sample_size]; int sample_i = 0;
      Iterator<Bundle> it = rb.bundleIterator();
      while (it.hasNext() && sample_i < sample.length) {
        Bundle bundle = it.next();
	if (Math.random() < ((double) score_sample_size) / rb.size()) {
          sample[sample_i++] = bundle;
	}
      }
      if (sample_i != score_sample_size) { // If we don't have enough, just fill with the first ones...
        it = rb.bundleIterator(); while (sample_i < sample.length) { sample[sample_i++] = it.next(); }
      }

      // Create the distance equation object
      DistEq disteq = new DistEq(rb, fld_to_fld_weight);

      // Do the compares
      int bins[] = new int[10]; int uncompares = 0;
      for (int i=1;i<sample.length;i++) {
        for (int j=0;j<i;j++) {
	  double score = disteq.d(sample[i], sample[j]);

	  if      (Double.isNaN(score)) uncompares++;
	  else if (score <  0.0)         bins[0            ]++;
	  else if (score >= 1.0)         bins[bins.length-1]++;
	  else {
	    int bin_i = (int) (score * bins.length); if (bin_i == bins.length) bin_i--;
	    bins[bin_i]++;
	  }
	}
      }

      // Set the method variables
      score_bins = bins; score_uncompares = uncompares;
      repaint();
    }

    /**
     * Sample size to use for the calculateSampleScores method
     */
    final int score_sample_size = 200;

    /**
     * Scoring bins
     */
    int score_bins[];

    /**
     * Number of bundles that couldn't be compared
     */
    int score_uncompares = 0;
  }
}

