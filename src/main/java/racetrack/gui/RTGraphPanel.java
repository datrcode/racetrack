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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.BundlesDT;
import racetrack.framework.BundlesG;
import racetrack.framework.KeyMaker;
import racetrack.framework.PostProc;
import racetrack.framework.Tablet;
import racetrack.graph.BiConnectedComponents;
import racetrack.graph.Conductance;
import racetrack.graph.DijkstraSingleSourceShortestPath;
import racetrack.graph.GraphFactory;
import racetrack.graph.GraphLayouts;
import racetrack.graph.GraphUtils;
import racetrack.graph.MyGraph;
import racetrack.graph.OptDistFunc;
import racetrack.graph.SimpleMyGraph;
import racetrack.graph.UniGraph;
import racetrack.graph.UniTwoPlusDegreeGraph;
import racetrack.graph.WorldToScreenTransform;
import racetrack.kb.BundlesTimeExpander;
import racetrack.transform.CCShapeRec;
import racetrack.transform.GeoData;
import racetrack.util.CaseInsensitiveComparator;
import racetrack.util.Entity;
import racetrack.util.EntityExtractor;
import racetrack.util.JTextFieldHistory;
import racetrack.util.LineCountSorter;
import racetrack.util.LineIntersection;
import racetrack.util.StrCountSorter;
import racetrack.util.SubText;
import racetrack.util.Utils;
import racetrack.visualization.ColorScale;
import racetrack.visualization.RTColorManager;
import racetrack.visualization.ShapeFile;
import racetrack.visualization.TreeMap;


/**
 * Visualization component for handling link-node graphs.  By far the most
 * complex visualization component due to the unique characteristics of
 * link-node analysis -- predominantly related to laying out nodes and
 * manipulating their locations.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class RTGraphPanel extends RTPanel implements WorldToScreenTransform {
  /**
   *
   */
  private static final long serialVersionUID = -6648601887937566057L;

  /**
   * Enumeration of mouse events
   */
  enum ME_ENUM        { PRESSED, RELEASED, CLICKED, MOVED, DRAGGED, WHEEL };

  /**
   * Enumeration of different UI interaction events
   */
  enum UI_INTERACTION { NONE, PANNING, SELECTING, MOVING, GRID_LAYOUT, LINE_LAYOUT, CIRCLE_LAYOUT };

  /**
   * Enumeration of the modes for the UI
   */
  enum UI_MODE        { EDIT, FILTER, EDGELENS, TIMELINE };

  /**
   * Radio choices for the node size
   */
  JRadioButtonMenuItem         node_sizes[], 
  /**
   * Radio choices for the node color
   */
                               node_colors[], 
  /**
   * Radio choices for the link size
   */
			       link_sizes[], 
  /**
   * Radio choices for the link color
   */
			       link_colors[], 
  /**
   * Radio choice for the edit graph mode
   */
                               edit_rbmi, 
  /**
   * Radio choice for the filter graph mode - this mode is most similar to all of the other
   * visualization components
   */
			       filter_rbmi, 
  /**
   * Radio choice for the edgelens mode which provides a means to understand congested areas
   */
			       edgelens_rbmi,
  /**
   * Radio choice for the timeline mode which provides a way to combine spatial with temporal
   */
                               timeline_rbmi,
  /**
   * Radio choice for no background
   */
			       nobg_rbmi, 
  /**
   * Radio choice for geo outline background
   */
			       geo_out_rbmi, 
  /**
   * Radio choice for geo filled backgroun
   */
			       geo_fill_rbmi, 
  /**
   * Radio choice for geo filled on only those countries containing a node
   */
			       geo_touch_rbmi,
  /**
   * Radio choice for kcore background
   */
                               kcores_rbmi;
  /**
   * Enable link transparency
   */
  JCheckBoxMenuItem            link_trans_cbmi, 
  /**
   * Enable link curves (for directed graph analysis)
   */
                               link_curves_cbmi,
  /**
   * Enable link arrows (for directed graph analysis)
   */
                               arrows_cbmi, 
  /**
   * Enable link timing marks (to enable time analysis across edges)
   */
			       timing_cbmi, 
  /**
   * Enable strict matches which selecting from the clipboard
   */
                               strict_matches_cbmi,
  /**
   * Recalculate the boundaries upon redraw
   */
                               recalc_bounds_cbmi,
  /**
   * Enable dynamic lables that occur under the mouse and with the nearest neighbors
   */
			       dyn_label_cbmi,
  /**
   * Vertex placement heatmap option (experimental)
   */
                               vertex_placement_heatmap_cbmi;

  /**
   * Enable node labels
   */
  JCheckBox                    node_labels_cb,
  /**
   *
   */
                               link_labels_cb;
  /**
   * List for selecting node labels
   */
  JList                        entity_label_list, 
  /**
   * List for selecting node color when based on a label
   */
                               entity_color_list,
  /**
   * List for selecting edge labels
   */
                               bundle_label_list;
  /**
   * Textfield for selecting entities (nodes)
   */
  JTextField                   select_tf, 
  /**
   * Textfield for tagging selected entities (nodes)
   */
                               tag_tf;
  /**
   * Menu item to add a relationship (i.e., edges) to the graph
   */
  JMenuItem                    add_relationship_mi, 
  /**
   * Menu item to delete a relationship (i.e., edges) in the graph
   */
                               delete_relationship_mi,
  /**
   * Menu item to add header relationships (tablet headers) as a graph
   */
                               add_header_relationships_mi, 
  /**
   * Menu item for adding header relationships (with links to their data types)
   */
			       add_header_relationships_types_mi,
  /**
   * Menu item to add header star relationships (tablet headers) as a graph
   */
                               add_header_relationships_stars_mi, 
  /**
   * Menu item for adding header star relationships (with links to their data types)
   */
			       add_header_relationships_types_stars_mi;

  /**
   * String for no nodes
   */
  final static String NODE_SZ_INVISIBLE = "Hidden",     
  /**
   * String for small nodes
   */
                      NODE_SZ_SMALL     = "Small",
  /**
   * String for large nodes - most useful for manipulating the graph
   */
                      NODE_SZ_LARGE     = "Large",      
  /**
   * String for varying the node sizes
   */
		      NODE_SZ_VARY      = "Vary",
  /**
   * String for varying the node sizes (logarithmic)
   */
		      NODE_SZ_VARY_LOG  = "Vary (Log)", 
  /**
   * String for making the nodes into glyphs for their data types
   */
		      NODE_SZ_TYPE      = "Type",
  /**
   * String for showing graph characteristics
   */
		      NODE_SZ_GRAPHINFO = "Graph Info",
  /**
   * String for cluster coefficient sizing
   */
                      NODE_SZ_CLUSTERCO = "Cluster Coefficient",
  /**
   * String to make the node equal to the label
   */
                      NODE_SZ_LABEL     = "Label",
  /**
   * String for coloring the nodes white
   */
                      NODE_CO_WHITE     = "Default",      
  /**
   * String for varying the color of nodes based on the global color option
   */
		      NODE_CO_VARY      = "Vary",   
  /**
   * String for varying the node color based on a label option
   */
		      NODE_CO_LABEL     = "Label",
  /**
   * String for varying color by the cluster coefficient
   */
                      NODE_CO_CLUSTERCO = "Cluster Coefficient",
  /**
   * String for making the link (edge) size normal (constant)
   */
                      LINK_SZ_NORMAL    = "Normal",     
  /**
   * String for making the link (edge) size thin (constant)
   */
		      LINK_SZ_THIN      = "Thin",
  /**
   * String for making the link (edge) size thick (constant)
   */
                      LINK_SZ_THICK     = "Thick",      
  /**
   * String for varying the link (edge) size linearly
   */
		      LINK_SZ_VARY      = "Vary",
  /**
   * String for hiding the links (edges)
   */
                      LINK_SZ_INVISIBLE = "Hidden",
  /**
   * String for link size by conductance
   */
                      LINK_SZ_CONDUCT   = "Conductance",
  /**
   *
   */
                      LINK_SZ_CLUSTERP = "Cluster Prob",
  /**
   * String for making the link (edge) color gray (constant)
   */
		      LINK_CO_GRAY      = "Gray",   
  /**
   * String for varying the link (edge) color based on the bundles
   */
		      LINK_CO_VARY      = "Vary";
  /**
   * Array for holding the node size strings
   */
  final static String NODE_SZ_STRS[]    = { NODE_SZ_GRAPHINFO, NODE_SZ_LARGE, NODE_SZ_SMALL, NODE_SZ_VARY,  NODE_SZ_VARY_LOG, NODE_SZ_TYPE,  NODE_SZ_INVISIBLE, NODE_SZ_CLUSTERCO, NODE_SZ_LABEL },
  /**
   * Array for holding the node color strings
   */
                      NODE_CO_STRS[]    = { NODE_CO_VARY, NODE_CO_WHITE, NODE_CO_LABEL, NODE_CO_CLUSTERCO },
  /**
   * Array for holding the link (edge) size strings
   */
                      LINK_SZ_STRS[]    = { LINK_SZ_THIN, LINK_SZ_NORMAL, LINK_SZ_THICK, LINK_SZ_VARY, LINK_SZ_INVISIBLE, LINK_SZ_CONDUCT, LINK_SZ_CLUSTERP },
  /**
   * Array for holding the link (edge) color strings
   */
                      LINK_CO_STRS[]    = { LINK_CO_GRAY, LINK_CO_VARY };
  /**
   * String for the filtering mode - this mode is most like the other visualization component interactions
   */
  final static String MODE_FILTER       = "Filter",
  /**
   * String for the edit mode - this mode is used to manipulate the location of nodes
   */
                      MODE_EDIT         = "Edit";
  /**
   * Array for holding the UI mode strings
   */
  final static String MODE_STRS[]       = { MODE_FILTER, MODE_EDIT };

  /**
   * Construct the panel by placing the GUI panel, setting up the popup menu, and adding
   * listeners for callback events.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt       application reference
   */
  public RTGraphPanel              (RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      {
    super(win_type, win_pos, win_uniq, rt); JMenuItem mi;
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, component = new RTGraphComponent(), createLabelsPanel(getRTParent().getEntityTagTypes()));
    split.setOneTouchExpandable(true); split.setResizeWeight(1.0);
    add("Center", split);

    // - Relationships...
    getRTPopupMenu().add(add_relationship_mi               = new JMenuItem("Add Edge Relationship..."));
    getRTPopupMenu().add(delete_relationship_mi            = new JMenuItem("Delete Edge Relationship..."));
    JMenu datascience_menu = new JMenu("Data Science"); getRTPopupMenu().add(datascience_menu);
      datascience_menu.add(add_header_relationships_mi             = new JMenuItem("Add Header Relationships"));
      datascience_menu.add(add_header_relationships_types_mi       = new JMenuItem("Add Header Relationships (Types)"));
      datascience_menu.add(add_header_relationships_stars_mi       = new JMenuItem("Add Header Star Relationships"));
      datascience_menu.add(add_header_relationships_types_stars_mi = new JMenuItem("Add Header Star Relationships (Types)"));
    common_relationships_menu = new JMenu("Common Relationships"); getRTPopupMenu().add(common_relationships_menu); 
      fillCommonRelationshipsMenu();

    // - Copy and paste
    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(mi = new JMenuItem("Copy Selected Entities"));  mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { copySelection();          } } );
    getRTPopupMenu().add(mi = new JMenuItem("Select (Clipboard)"));      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { selectFromClipboard();    } } );
    getRTPopupMenu().add(mi = new JMenuItem("Add To (Clipboard)"));      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { addFromClipboard();       } } );
    getRTPopupMenu().add(mi = new JMenuItem("Remove From (Clipboard)")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { removeFromClipboard();    } } );
    getRTPopupMenu().add(mi = new JMenuItem("Intersect Clipboard"));     mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { intersectFromClipboard(); } } );
    getRTPopupMenu().add(strict_matches_cbmi = new JCheckBoxMenuItem("Strict Substring Matches", true));

    // - Background Stuff
    getRTPopupMenu().addSeparator();
    JMenu background_menu = new JMenu("Background"); ButtonGroup bg; bg = new ButtonGroup();
    getRTPopupMenu().add(background_menu);
    nobg_rbmi      = new JRadioButtonMenuItem("None", true);   bg.add(nobg_rbmi);      background_menu.add(nobg_rbmi);      defaultListener(nobg_rbmi);
    background_menu.addSeparator();
    geo_out_rbmi   = new JRadioButtonMenuItem("Geo Outline");  bg.add(geo_out_rbmi);   background_menu.add(geo_out_rbmi);   defaultListener(geo_out_rbmi);
    geo_fill_rbmi  = new JRadioButtonMenuItem("Geo Fill");     bg.add(geo_fill_rbmi);  background_menu.add(geo_fill_rbmi);  defaultListener(geo_fill_rbmi);
    geo_touch_rbmi = new JRadioButtonMenuItem("Geo Touch");    bg.add(geo_touch_rbmi); background_menu.add(geo_touch_rbmi); defaultListener(geo_touch_rbmi);
    background_menu.addSeparator();
    kcores_rbmi    = new JRadioButtonMenuItem("KCores");       bg.add(kcores_rbmi);    background_menu.add(kcores_rbmi);    defaultListener(kcores_rbmi);

    // - Layout Stuff
    getRTPopupMenu().addSeparator();
    JMenu layouts_menu = new JMenu("Layout");
    getRTPopupMenu().add(layouts_menu);
    getRTPopupMenu().add(mi = new JMenuItem("Layout Small Multiples Dialog (Beta)..."));
    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new LayoutSmallMultiplesDialog(); } } );

    // - Layout I/O
    layouts_menu.add(mi = new JMenuItem("Save Layout..."));
    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { saveLayout(false, null); } } );
    layouts_menu.add(mi = new JMenuItem("Save Layout (Selected Entities)..."));
    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { saveLayout(true,  null);  } } );
    layouts_menu.add(mi = new JMenuItem("Load Layout..."));
    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { loadLayout(false); } } );
    layouts_menu.add(mi = new JMenuItem("Load Layout (Apply To Selected)..."));
    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { loadLayout(true);  } } );

    // - Layout helpers
    layouts_menu.addSeparator();
    layouts_menu.add(mi = new JMenuItem("Subset One Bundle Per Edge (Fast UI)"));
    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { subsetOneBundlePerEdge(); } } );
    layouts_menu.add(mi = new JMenuItem("IP Logical Octet Layout"));
    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { ipLogicalOctetLayout(); } } );
    layouts_menu.add(mi = new JMenuItem("Temporal Layout"));
    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { temporalLayout(); } } );
    layouts_menu.add(mi = new JMenuItem("Collapse Blocks"));
    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { collapseBlocks(); } } );
    layouts_menu.add(mi = new JMenuItem("Node Color TreeMap Layout"));
    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { nodeColorTreeMapLayout(); } } );

    // - Specialized layouts
    layouts_menu.addSeparator();
    layouts_menu.add(mi = new JMenuItem("Geospatial"));      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { geospatialLayout(false); } } );
    // layouts_menu.add(mi = new JMenuItem("Geospatial (cc)")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { geospatialLayout(true);  } } );

    // - Layouts
    layouts_menu.addSeparator(); Map<String,JMenu> layout_cats = new HashMap<String,JMenu>();
    String layouts[] = GraphLayouts.getLayoutAlgorithms();
    for (int i=0;i<layouts.length;i++) {

      // Categorize
      String category = GraphLayouts.getLayoutCategory(layouts[i]);
      if (layout_cats.containsKey(category) == false) {
        JMenu jmenu;
        layouts_menu.add(jmenu = new JMenu(category));
        layout_cats.put(category, jmenu);
      }
      layout_cats.get(category).add(mi = new JMenuItem(layouts[i]));

      // Add the action listener for each layout
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
        RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) (getRTComponent().getRTRenderContext()); if (myrc == null) return;
        String algorithm = ((JMenuItem) ae.getSource()).getText();
        (new GraphLayouts()).executeLayoutAlgorithm(
          algorithm, graph, myrc.filterEntities(getRTParent().getSelectedEntities()), entity_to_wxy);
        Iterator<String> it = entity_to_wxy.keySet().iterator();
        String str = null; double x0 = 0.0, x1 = 1.0, y0 = 0.0, y1 = 1.0;
        if (it.hasNext()) {
          str = it.next(); 
          x0 = entity_to_wxy.get(str).getX();
          y0 = entity_to_wxy.get(str).getY();
          x1 = entity_to_wxy.get(str).getX();
          y1 = entity_to_wxy.get(str).getY();
        }
        while (it.hasNext()) { 
          str = it.next(); 
          if (x0 > entity_to_wxy.get(str).getX()) x0 = entity_to_wxy.get(str).getX();
          if (y0 > entity_to_wxy.get(str).getY()) y0 = entity_to_wxy.get(str).getY();
          if (x1 < entity_to_wxy.get(str).getX()) x1 = entity_to_wxy.get(str).getX();
          if (y1 < entity_to_wxy.get(str).getY()) y1 = entity_to_wxy.get(str).getY();
        }
        if (x1 == x0) { x0 -= 1.0; x1 += 1.0; }
        if (y1 == y0) { y0 -= 1.0; y1 += 1.0; }
        double xp = (x1 - x0)*0.05, yp = (y1 - y0)*0.05;
        extents = new Rectangle2D.Double(x0-xp,y0-yp,x1-x0+2*xp,y1-y0+2*yp);
        transform(); getRTComponent().render();
      } } );
    }

    // - Mode
    getRTPopupMenu().addSeparator();
    JMenu menu; 
    menu = new JMenu("Mode"); getRTPopupMenu().add(menu); bg = new ButtonGroup();
    menu.add(edit_rbmi     = new JRadioButtonMenuItem("Edit",true)); bg.add(edit_rbmi);
    menu.add(filter_rbmi   = new JRadioButtonMenuItem("Filter"));    bg.add(filter_rbmi);
    menu.add(edgelens_rbmi = new JRadioButtonMenuItem("EdgeLens"));  bg.add(edgelens_rbmi);
    menu.add(timeline_rbmi = new JRadioButtonMenuItem("Timeline"));  bg.add(timeline_rbmi);

    // - Sizes, Colors
    getRTPopupMenu().addSeparator();
    menu = new JMenu("Node Size");  getRTPopupMenu().add(menu); bg = new ButtonGroup();
    node_sizes  = new JRadioButtonMenuItem[NODE_SZ_STRS.length]; for (int i=0;i<NODE_SZ_STRS.length;i++) { menu.add(node_sizes[i]  = new JRadioButtonMenuItem(NODE_SZ_STRS[i],i==0)); bg.add(node_sizes[i]);  defaultListener(node_sizes[i]);  }
    menu = new JMenu("Node Color"); getRTPopupMenu().add(menu); bg = new ButtonGroup();
    node_colors = new JRadioButtonMenuItem[NODE_CO_STRS.length]; for (int i=0;i<NODE_CO_STRS.length;i++) { menu.add(node_colors[i] = new JRadioButtonMenuItem(NODE_CO_STRS[i],i==0)); bg.add(node_colors[i]); defaultListener(node_colors[i]); }
    menu = new JMenu("Link Size");  getRTPopupMenu().add(menu); bg = new ButtonGroup();
    link_sizes  = new JRadioButtonMenuItem[LINK_SZ_STRS.length]; for (int i=0;i<LINK_SZ_STRS.length;i++) { menu.add(link_sizes[i]  = new JRadioButtonMenuItem(LINK_SZ_STRS[i],i==0)); bg.add(link_sizes[i]);  defaultListener(link_sizes[i]);  }
      menu.addSeparator();
      menu.add(link_trans_cbmi    = new JCheckBoxMenuItem("Enable Link Transpency", true));
      menu.add(link_curves_cbmi   = new JCheckBoxMenuItem("Use Curves (Beta)"));
      menu.add(arrows_cbmi        = new JCheckBoxMenuItem("Draw Arrows"));
      menu.add(timing_cbmi        = new JCheckBoxMenuItem("Draw Timing Marks"));
    menu = new JMenu("Link Color"); getRTPopupMenu().add(menu); bg = new ButtonGroup();
    link_colors = new JRadioButtonMenuItem[LINK_CO_STRS.length]; for (int i=0;i<LINK_CO_STRS.length;i++) { menu.add(link_colors[i] = new JRadioButtonMenuItem(LINK_CO_STRS[i],i==0)); bg.add(link_colors[i]); defaultListener(link_colors[i]); }

    // - Rendering options
    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(dyn_label_cbmi                = new JCheckBoxMenuItem("Dynamic Labels",                          true));
    getRTPopupMenu().add(recalc_bounds_cbmi            = new JCheckBoxMenuItem("Recalculate Bounds On Re-Draw",           false));
    getRTPopupMenu().add(vertex_placement_heatmap_cbmi = new JCheckBoxMenuItem("Vertex Placement Heatmap (Experimental)", false));

    // - Add growth options
    menu = new JMenu("Expansion/Filter");
    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(menu);
      menu.add(mi = new JMenuItem("Add All Bundles On Visible Links"));         mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { addAllOnVisibleLinks();    } } );
      menu.add(mi = new JMenuItem("Make 1-Hop Links Visible"));                 mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { makeOneHopsVisible(false); } } );
      menu.add(mi = new JMenuItem("Make 1-Hop Links Visible (Directional)"));   mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { makeOneHopsVisible(true);  } } );
      menu.addSeparator();
      menu.add(mi = new JMenuItem("Only Keep Bidirectional Links"));            mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { filterToBidirectionalLinks(); } } );

    // - Selection
    menu = new JMenu("Select By"); getRTPopupMenu().add(menu);
    Iterator<Utils.Symbol> it_sym = EnumSet.allOf(Utils.Symbol.class).iterator();
    while (it_sym.hasNext()) { Utils.Symbol symbol = it_sym.next(); menu.add(mi = new JMenuItem(""+symbol)); mi.addActionListener(new SymbolSelector(symbol)); }

    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(mi = new JMenuItem("Retain Visible Nodes Only (Destructive, Locally)"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { retainOnlyVisibleNodes(); } } );
    getRTPopupMenu().add(mi = new JMenuItem("Add Selection To Retained"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { addSelectionToRetained(); } } );
    getRTPopupMenu().add(mi = new JMenuItem("Clear Retained Set"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { clearRetained(); } } );

    getRTPopupMenu().addSeparator();

    // - Test graphs
    // -- Create the listener
    ActionListener graphfactory_al = new ActionListener() { public void actionPerformed(ActionEvent ae) {
      if (ae.getSource() instanceof JMenuItem) { 
        // Get the type from the label and instantiate the graph
        String type_str = ((JMenuItem) ae.getSource()).getText();
	GraphFactory.Type type = GraphFactory.toType(type_str);
        MyGraph mygraph = GraphFactory.createInstance(type, null);

	// Create the specialized tablet
        String header[] = { "from", "to", "source" };
        Tablet tablet = getRTParent().getRootBundles().findOrCreateTablet(header);

	// Go through the nodes and add them to the tablet
        for (int i=0;i<mygraph.getNumberOfEntities();i++) {
          String node = mygraph.getEntityDescription(i);
	  for (int j=0;j<mygraph.getNumberOfNeighbors(i);j++) {
	    String nbor = mygraph.getEntityDescription(mygraph.getNeighbor(i,j));

	    // Add the record
	    Map<String,String> attr = new HashMap<String,String>();
	    attr.put("from",   node);
	    attr.put("to",     nbor);
	    attr.put("source", "GraphFactory");

	    tablet.addBundle(attr);
        } } 
        // Force the change
        Set<Bundles> bundles_set = new HashSet<Bundles>(); bundles_set.add(getRTParent().getRootBundles());
        getRTParent().getRootBundles().getGlobals().cleanse(bundles_set);
        // Update the dropdowns
        getRTParent().updateBys();
    } } };

    // -- Fill out the menu
    menu = new JMenu("Test Graphs"); getRTPopupMenu().add(menu);
    Iterator<GraphFactory.Type> it_gt = GraphFactory.graphTypeIterator();
    while (it_gt.hasNext()) {
      mi = new JMenuItem(GraphFactory.toString(it_gt.next()));
      menu.add(mi);
      mi.addActionListener(graphfactory_al);
    }
    
    // Create the southern panel
    JPanel panel = new JPanel(new FlowLayout()); JButton clear_tags_bt;
    panel.add(new JLabel("Select"));
    panel.add(select_tf     = new JTextField(16)); select_tf.setToolTipText("(+/-/*/! sub/IP/CIDR/REGEX:)");  new JTextFieldHistory(select_tf);
    panel.add(new JLabel("Tag"));
    panel.add(tag_tf        = new JTextField(16)); tag_tf.setToolTipText(Utils.getTagToolTip());       new JTextFieldHistory(tag_tf);
    panel.add(clear_tags_bt = new JButton("Clear Tags"));
    add("South", panel);

    // Listeners
    add_relationship_mi.addActionListener                     (new ActionListener() { public void actionPerformed(ActionEvent ae) { addRelationshipDialog();            } } );
    delete_relationship_mi.addActionListener                  (new ActionListener() { public void actionPerformed(ActionEvent ae) { deleteRelationshipDialog();         } } );
    add_header_relationships_mi.addActionListener             (new ActionListener() { public void actionPerformed(ActionEvent ae) { addHeaderRelationships(false);      } } );
    add_header_relationships_types_mi.addActionListener       (new ActionListener() { public void actionPerformed(ActionEvent ae) { addHeaderRelationships(true);       } } );
    add_header_relationships_stars_mi.addActionListener       (new ActionListener() { public void actionPerformed(ActionEvent ae) { addHeaderRelationshipsStars(false); } } );
    add_header_relationships_types_stars_mi.addActionListener (new ActionListener() { public void actionPerformed(ActionEvent ae) { addHeaderRelationshipsStars(true);  } } );
    defaultListener(link_curves_cbmi);
    defaultListener(link_trans_cbmi);
    defaultListener(arrows_cbmi);
    defaultListener(timing_cbmi);

    select_tf.    addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { select(select_tf.getText());           } } );
    tag_tf.       addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { tagSelectedEntities(tag_tf.getText()); } } );
    clear_tags_bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { clearEntityTagsForSelectedEntities();  } } );
  }

  /**
   * Simple class to select by symbol
   */
  class SymbolSelector implements ActionListener {
    Utils.Symbol symbol; public SymbolSelector(Utils.Symbol symbol) { this.symbol = symbol; }
    public void actionPerformed(ActionEvent ae) {
      RTGraphComponent comp = ((RTGraphComponent) getRTComponent()); Set<String> sel = new HashSet<String>();
      Iterator<String> it = entity_to_shape.keySet().iterator();
      while (it.hasNext()) {
        String entity = it.next();
	if (entity_to_shape.get(entity).equals(symbol)) sel.add(entity);
      }
      comp.setOperation(sel);
    }
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "linknode"; }

  /**
   * Simple accessor to get the transformative version of this class.
   *
   *@return this
   */
  public WorldToScreenTransform getWorldToScreenTransform() { return this; }

  /**
   * Return the configuration of this panel as a string.  Originally intended
   * for bookmarking and recalling views.
   *
   *@return configuration as a string
   */
  public String       getConfig       ()           { 
    StringBuffer ar_sb = new StringBuffer();
    if (active_relationships.size() > 0) {
      ar_sb.append(Utils.encToURL(active_relationships.get(0)));
      for (int i=1;i<active_relationships.size();i++) ar_sb.append("," + Utils.encToURL(active_relationships.get(i)));
    }

    return "RTGraphPanel" +                                           BundlesDT.DELIM +
           "nodesize="    + Utils.encToURL(nodeSize())              + BundlesDT.DELIM +
           "nodecolor="   + Utils.encToURL(nodeColor())             + BundlesDT.DELIM +
	   "linksize="    + Utils.encToURL(linkSize())              + BundlesDT.DELIM +
	   "linkcolor="   + Utils.encToURL(linkColor())             + BundlesDT.DELIM +
	   "linkcurves="  + Utils.encToURL("" + linkCurves())       + BundlesDT.DELIM +
	   "linktrans="   + Utils.encToURL("" + linksTransparent()) + BundlesDT.DELIM +
	   "arrows="      + Utils.encToURL("" + drawArrows())       + BundlesDT.DELIM +
	   "timing="      + Utils.encToURL("" + drawTiming())       + BundlesDT.DELIM +
	   "strict="      + Utils.encToURL("" + strictMatches())    + BundlesDT.DELIM +
	   "dynlabels="   + Utils.encToURL("" + dynamicLabels())    + BundlesDT.DELIM +
	   "nodelabels="  + Utils.encToURL("" + nodeLabels())       + BundlesDT.DELIM +
	   "linklabels="  + Utils.encToURL("" + linkLabels())       + BundlesDT.DELIM +
	   "nlabels="     + commaDelimited(nodeLabelsArray())       + BundlesDT.DELIM +
	   "clabels="     + commaDelimited(colorLabelsArray())      + BundlesDT.DELIM +
	   "llabels="     + commaDelimited(linkLabelsArray())       +
	   (ar_sb.length() > 0 ? BundlesDT.DELIM + "relates=" + ar_sb.toString() : "");
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
   * Helper method to set all the correct strings in a JList.
   */
  private void setJList(JList list, String strs[]) {
    List<Integer> indexes = new ArrayList<Integer>(); ListModel lm = list.getModel();
    for (int i=0;i<strs.length;i++) { for (int j=0;j<lm.getSize();j++) if (strs[i].equals("" + lm.getElementAt(j))) indexes.add(j); }
    int index_array[] = new int[indexes.size()]; for (int i=0;i<index_array.length;i++) index_array[i] = indexes.get(i);
    list.setSelectedIndices(index_array);
  }

  /**
   * Set the configuration of this panel based on a previously returned configuration string.
   * Not implemented.
   *
   *@param str configuration string
   */
  public void         setConfig       (String str) { 
    StringTokenizer st = new StringTokenizer(str, BundlesDT.DELIM);
    if (st.nextToken().equals("RTGraphPanel") == false) throw new RuntimeException("setConfig(" + str + ") - not a RTGraphPanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st2.hasMoreTokens() ? st2.nextToken() : "";

      if      (type.equals("nodesize"))    nodeSize(Utils.decFmURL(value));
      else if (type.equals("nodecolor"))   nodeColor(Utils.decFmURL(value));
      else if (type.equals("linksize"))    linkSize(Utils.decFmURL(value));
      else if (type.equals("linkcolor"))   linkColor(Utils.decFmURL(value));
      else if (type.equals("linkcurves"))  linkCurves(value.toLowerCase().equals("true"));
      else if (type.equals("linktrans"))   linksTransparent(value.toLowerCase().equals("true"));
      else if (type.equals("arrows"))      drawArrows(value.toLowerCase().equals("true"));
      else if (type.equals("timing"))      drawTiming(value.toLowerCase().equals("true"));
      else if (type.equals("strict"))      strictMatches(value.toLowerCase().equals("true"));
      else if (type.equals("dynlabels"))   dynamicLabels(value.toLowerCase().equals("true"));
      else if (type.equals("nodelabels"))  nodeLabels(value.toLowerCase().equals("true"));
      else if (type.equals("linklabels"))  linkLabels(value.toLowerCase().equals("true"));
      else if (type.equals("nlabels")) { if (!value.equals("")) setJList(entity_label_list, commaDelimited(value)); }
      else if (type.equals("clabels")) { if (!value.equals("")) setJList(entity_color_list, commaDelimited(value)); }
      else if (type.equals("llabels")) { if (!value.equals("")) setJList(bundle_label_list, commaDelimited(value)); }
      else if (type.equals("relates")) {
        if (!value.equals("")) {
          st2 = new StringTokenizer(value, ",");
	  while (st2.hasMoreTokens()) active_relationships.add(Utils.decFmURL(st2.nextToken()));
        }
      } else throw new RuntimeException("Do Not Understand Type Value Pair \"" + type + "\" = \"" + value + "\"");
    }

    // Apply the active_relationships
    newBundlesRoot(getRTParent().getRootBundles());
  }

  /**
   * Override to indicate that this component needs to save additional information.
   *
   *@return true
   */
  @Override
  public boolean hasAdditionalConfig() { return true; }

  /**
   * Add the additional configuration for this component so that the state can be restored.
   *
   *@param list         Additional configuration strings added here
   *@param visible_only only save information about the visible components -- important because of the need to
   *                    save the world positions of all the nodes
   */
  @Override
  public void addAdditionalConfig(List<String> list, boolean visible_only) {
    RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) getRTComponent().rc; RTGraphComponent mycomp = (RTGraphComponent) getRTComponent();
    list.add("#AC extents|" + extents.getX() + "|" + extents.getY() + "|" + extents.getWidth() + "|" + extents.getHeight());
    // Save the retained nodes
    if (retained_nodes.size() > 0) {
      StringBuffer     sb = new StringBuffer();
      Iterator<String> it = retained_nodes.iterator();
      sb.append("#AC retain"); while (it.hasNext()) sb.append("|" + Utils.encToURL(it.next()));
      list.add(sb.toString());
    }
    // Save the sticky labels
    if (mycomp.sticky_labels != null && mycomp.sticky_labels.size() > 0) {
      StringBuffer sb = new StringBuffer(); 
      Iterator<String> it = mycomp.sticky_labels.iterator();
      sb.append("#AC sticky"); while (it.hasNext()) sb.append("|" + Utils.encToURL(it.next()));
      list.add(sb.toString());
    }
    // Pick out the right iterator -- if the visible one is available, use that
    Iterator<String> it;
    if (visible_only && myrc != null) it = myrc.entity_counter_context.binIterator();
    else                              it = entity_to_wxy.keySet().iterator();
    while (it.hasNext()) {
      String  entity = it.next(); Point2D point  = entity_to_wxy.get(entity);
      list.add("#AC wxy|" + Utils.encToURL(entity) + "|" + point.getX() + "|" + point.getY());
    }
    list.add("#AC graphend");
  }

  /**
   * Parse additional configuration information.
   */
  public int parseAdditionalConfig(List<String> lines, int line_i) {
    RTGraphComponent mycomp = (RTGraphComponent) getRTComponent();
    // Parse the extents
    if (lines.get(line_i).startsWith("#AC extents|")) {
      StringTokenizer st = new StringTokenizer(lines.get(line_i++),"|");
      st.nextToken();
      extents = new Rectangle2D.Double(Double.parseDouble(st.nextToken()),
                                       Double.parseDouble(st.nextToken()),
				       Double.parseDouble(st.nextToken()),
				       Double.parseDouble(st.nextToken()));
    }
    // Parse the retained nodes
    if (lines.get(line_i).startsWith("#AC retain|"))  {
      StringTokenizer st = new StringTokenizer(lines.get(line_i++),"|"); st.nextToken();
      Set<String> new_retained_nodes = new HashSet<String>();
      while (st.hasMoreTokens()) new_retained_nodes.add(Utils.decFmURL(st.nextToken()));
      if (retained_nodes != null) {
        retained_nodes.clear();
        retained_nodes.addAll(new_retained_nodes);
      } else retained_nodes = new_retained_nodes;
    }
    // Parse the sticky labels
    if (lines.get(line_i).startsWith("#AC sticky|")) {
      StringTokenizer st = new StringTokenizer(lines.get(line_i++),"|"); st.nextToken();
      Set<String> new_sticky_labels = new HashSet<String>();
      while (st.hasMoreTokens()) new_sticky_labels.add(Utils.decFmURL(st.nextToken()));
      if (mycomp.sticky_labels != null) {
        mycomp.sticky_labels.clear();
        mycomp.sticky_labels.addAll(new_sticky_labels);
      } else mycomp.sticky_labels = new_sticky_labels;
    }
    // Parse the coordinates
    while (lines.get(line_i).startsWith("#AC wxy|")) {
      StringTokenizer st = new StringTokenizer(lines.get(line_i++),"|"); st.nextToken();
      String entity = Utils.decFmURL(st.nextToken());
      double x      = Double.parseDouble(st.nextToken()),
             y      = Double.parseDouble(st.nextToken());
      entity_to_wxy.put(entity, new Point2D.Double(x,y));
    }
    if (lines.get(line_i).equals("#AC graphend")) line_i++;
    else throw new RuntimeException("Incorrect Ending For LinkNode Parser \"" + lines.get(line_i) + "\"");

    // Apply the settings
    newBundlesRoot(getRTParent().getRootBundles());
    transform();
    return line_i;
  }

  /**
   * Perform a geospatial layout of the nodes.
   *
   *@param cc_level if true, keep the nodes at a single position for each country (not implemented)
   */
  protected void geospatialLayout(boolean cc_level) {
    boolean one_trans = false, transform_available = true;
    Iterator<String> it = entity_to_wxy.keySet().iterator();
    while (it.hasNext()) {
      String       orig_entity = it.next(), entity = orig_entity;
      // Check for a concat
      if (entity.indexOf(BundlesDT.DELIM) >= 0) entity = entity.substring(entity.lastIndexOf(BundlesDT.DELIM)+1, entity.length());
      // Get the data type
      BundlesDT.DT datatype = BundlesDT.getEntityDataType(entity);
      // Attempt the transforms
      String lats[] = null, lons[] = null;
      try {
        if (transform_available) {
          lats = getRTParent().getRootBundles().getGlobals().transform(datatype, "latitude",  entity);
          lons = getRTParent().getRootBundles().getGlobals().transform(datatype, "longitude", entity);
        } else lats = lons = null;
      } catch (NullPointerException npe) { transform_available = false; lats = lons = null; }
      // Check for validity
      if (lats != null && lats.length > 0 && lats[0].equals(BundlesDT.NOTSET) == false &&
          lons != null && lons.length > 0 && lons[0].equals(BundlesDT.NOTSET) == false) {
        // System.err.println("Putting \"" + orig_entity + "\" (\"" + entity + "\") @ " + lats[0] + "," + lons[0]);
        entity_to_wxy.put(orig_entity, new Point2D.Double(-Double.parseDouble(lons[0]), -Double.parseDouble(lats[0])));
	transform(orig_entity); one_trans = true;
      } else if (GeoData.getInstance().geoDataAvailable(datatype)) {
        Point2D pt = GeoData.getInstance().geoLocate(datatype, entity);
        if (pt != null) {
	  entity_to_wxy.put(orig_entity, new Point2D.Double(pt.getX(), -pt.getY()));
	  transform(orig_entity); one_trans = true;
        }
      }
    }
    if (one_trans) getRTComponent().render();
  }

  /**
   * Submenu for the common edge relationships.
   */
  JMenu common_relationships_menu;

  /**
   * Adds common relationship types to the common_relationships_menu including recently used relationships.
   * These relationships describe the links (or edges) in the link-node graph.
   */
  protected void fillCommonRelationshipsMenu() {
    common_relationships_menu.removeAll();
    // -- Common relationships
    JMenuItem mi;
    BundlesG  globals = getRTParent().getRootBundles().getGlobals();
    //
    //
    //
    if (globals.fieldIndex("sip") != -1 && globals.fieldIndex("dip") != -1) {
      common_relationships_menu.add(mi = new JMenuItem("sip => dip"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
        addRelationship("sip",  Utils.SQUARE_STR,   false, "dip",  Utils.SQUARE_STR,   false, STYLE_SOLID_STR, true, true); } } );
    }
    //
    //
    //
    if (globals.fieldIndex("srcip") != -1 && globals.fieldIndex("dstip") != -1) {
      common_relationships_menu.add(mi = new JMenuItem("srcip => dstip"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
        addRelationship("srcip",  Utils.SQUARE_STR,   false, "dstip",  Utils.SQUARE_STR,   false, STYLE_SOLID_STR, true, true); } } );
    }
    //
    //
    //
    if (globals.fieldIndex("sip") != -1 && globals.fieldIndex("dip") != -1 && globals.fieldIndex("dpt") != -1) {
      common_relationships_menu.add(mi = new JMenuItem("sip => dpt => dip"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
        addRelationship("sip",  Utils.SQUARE_STR, false, "dpt",  Utils.CIRCLE_STR, true,  STYLE_SOLID_STR, true, true);
        addRelationship("dpt",  Utils.CIRCLE_STR, true,  "dip",  Utils.SQUARE_STR, false, STYLE_SOLID_STR, true, true); } } );
    }
    //
    //
    //
    if (globals.fieldIndex("sip") != -1 && globals.fieldIndex("dip") != -1 && globals.fieldIndex("dpt") != -1 && globals.fieldIndex("spt") != -1) {
      common_relationships_menu.add(mi = new JMenuItem("sip => spt => dpt => dip"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
        addRelationship("sip",  Utils.SQUARE_STR, false, "spt",  Utils.CIRCLE_STR,   true,  STYLE_SOLID_STR, true, true);
        addRelationship("spt",  Utils.CIRCLE_STR, true,  "dpt",  Utils.CIRCLE_STR,   true,  STYLE_SOLID_STR, true, true);
        addRelationship("dpt",  Utils.CIRCLE_STR, true,  "dip",  Utils.SQUARE_STR,   false, STYLE_SOLID_STR, true, true); } } );
    }
    //
    //
    //
    if (globals.fieldIndex("DBYT") != -1 && globals.fieldIndex("SBYT") != -1 && globals.fieldIndex("dpt") != -1 && globals.fieldIndex("spt") != -1) {
      common_relationships_menu.add(mi = new JMenuItem("DBYT => dpt => SBYT"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
        addRelationship("DBYT", Utils.DIAMOND_STR, true, "dpt",  Utils.CIRCLE_STR,  true,  STYLE_SOLID_STR, true, true);
        addRelationship("dpt",  Utils.CIRCLE_STR,  true, "SBYT", Utils.DIAMOND_STR, true,  STYLE_SOLID_STR, true, true); } } );
    }
    //
    //
    //
    if (globals.fieldIndex("DOCT") != -1 && globals.fieldIndex("SOCT") != -1 && globals.fieldIndex("dpt") != -1 && globals.fieldIndex("spt") != -1) {
      common_relationships_menu.add(mi = new JMenuItem("DOCT => dpt => SOCT"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
        addRelationship("DOCT", Utils.DIAMOND_STR, true, "dpt",  Utils.CIRCLE_STR,  true,  STYLE_SOLID_STR, true, true);
        addRelationship("dpt",  Utils.CIRCLE_STR,  true, "SOCT", Utils.DIAMOND_STR, true,  STYLE_SOLID_STR, true, true); } } );
    }
    //
    //
    //
    if (globals.fieldIndex("domain") != -1 && globals.fieldIndex("ip") != -1) {
      common_relationships_menu.add(mi = new JMenuItem("domain => ip"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
        addRelationship("domain", Utils.TRIANGLE_STR, false,  "ip",  Utils.SQUARE_STR, false,  STYLE_SOLID_STR, true, true); } } );
    }
    // -- Recent Relationships
    String recents[] = RTPrefs.retrieveStrings(RECENT_RELATIONSHIPS_PREF_STR);
    if (recents != null && recents.length > 0) {
      // Prepare the lookup...
      recent_relationships_lu = new HashMap<String,String>();
      // and the menu...
      common_relationships_menu.addSeparator();
      // and the blanks list...
      Set<String> blanks_set = new HashSet<String>();
      String blanks[] = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals());
      for (int i=0;i<blanks.length;i++) blanks_set.add(blanks[i]);
      // Go through the recents...
      for (int i=0;i<recents.length;i++) {
	// Extract the relationships
        StringTokenizer st = new StringTokenizer(recents[i], BundlesDT.DELIM);
	String delimited        = Utils.decFmURL(st.nextToken());
	// Get the header elements -- make sure they exist in the current data set
	String fm_hdr  = relationshipFromHeader(delimited),
	       to_hdr  = relationshipToHeader(delimited);
        if (blanks_set.contains(fm_hdr) == false || blanks_set.contains(to_hdr) == false) continue; 
	// Change them to a human representation and add them to the lookups
        String recents_in_human = fm_hdr + " => " + to_hdr;
        recent_relationships_lu.put(recents_in_human, delimited);
        common_relationships_menu.add(mi = new JMenuItem(recents_in_human));
	mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
	  String  cmd      = ae.getActionCommand(); cmd = recent_relationships_lu.get(cmd);
	  // System.err.println("Command = \"" + cmd + "\"");
	  String  fm_hdr   = relationshipFromHeader(cmd), fm_ico   = relationshipFromIcon(cmd); boolean fm_typed  = relationshipFromTyped(cmd);
	  String  to_hdr   = relationshipToHeader(cmd),   to_ico   = relationshipToIcon(cmd);   boolean to_typed  = relationshipToTyped(cmd);
          String  style    = relationshipStyle(cmd);                                            boolean ignore_ns = relationshipIgnoreNotSet(cmd);
	  addRelationship(fm_hdr, fm_ico, fm_typed, to_hdr, to_ico, to_typed, style, ignore_ns, false);
	} } );
      }
      // Add an option to clear out the recent relationships
      common_relationships_menu.addSeparator();
      common_relationships_menu.add(mi = new JMenuItem("Clear Relationships"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
        RTPrefs.store(RECENT_RELATIONSHIPS_PREF_STR, new String[0]);
      } } );
    }
  }

  /**
   * Lookup table for converting the menu strings into the edge relationship strings
   */
  Map<String,String> recent_relationships_lu = new HashMap<String,String>();

  /**
   * Enumeration for the background options
   */
  enum GraphBG { NONE, GEO_OUT, GEO_FILL, GEO_TOUCH, KCORES };

  /**
   * Determine which radio button is selected and return the appropriate enum.
   *
   *@return background enumeration
   */
  public GraphBG getGraphBG() {
    if      (nobg_rbmi.isSelected())      return GraphBG.NONE;
    else if (geo_out_rbmi.isSelected())   return GraphBG.GEO_OUT;
    else if (geo_fill_rbmi.isSelected())  return GraphBG.GEO_FILL;
    else if (geo_touch_rbmi.isSelected()) return GraphBG.GEO_TOUCH;
    else if (kcores_rbmi.isSelected())    return GraphBG.KCORES;
    else                                  return GraphBG.NONE;
  }

  /**
   * For an array of radio buttons, determine which is selected and return that as a string.
   *
   *@param  items radio button list
   *
   *@return selected radio button label
   */
  public String findSelected(JRadioButtonMenuItem items[]) {
    for (int i=0;i<items.length;i++) if (items[i].isSelected()) return items[i].getText();
    return items[0].getText();
  }

  /**
   * For an array of radio buttons, determine which matches the specified string and set it as selected.
   *
   *@param items radio button list
   *@param str   text of button to set as selected
   */
  public void setSelected(JRadioButtonMenuItem items[], String str) {
    for (int i=0;i<items.length;i++) if (items[i].getText().equals(str)) items[i].setSelected(true);
  }

  /**
   * Enumeration for the node sizes
   * 
   */
  enum NodeSize { INVISIBLE, SMALL, LARGE, VARY, VARY_LOG, TYPE, GRAPHINFO, CLUSTERCO, LABEL };

  /**
   * Return the enumeration of the node size based on the selected radio
   * button.
   *
   *@return node size enumeration
   */
  public NodeSize  getNodeSize() {
    String str = findSelected(node_sizes);
     if       (str.equals(NODE_SZ_INVISIBLE))      { return NodeSize.INVISIBLE;
    } else if (str.equals(NODE_SZ_SMALL))          { return NodeSize.SMALL;
    } else if (str.equals(NODE_SZ_LARGE))          { return NodeSize.LARGE;
    } else if (str.equals(NODE_SZ_VARY))           { return NodeSize.VARY; 
    } else if (str.equals(NODE_SZ_VARY_LOG))       { return NodeSize.VARY_LOG;
    } else if (str.equals(NODE_SZ_TYPE))           { return NodeSize.TYPE;
    } else if (str.equals(NODE_SZ_GRAPHINFO))      { return NodeSize.GRAPHINFO;
    } else if (str.equals(NODE_SZ_CLUSTERCO))      { return NodeSize.CLUSTERCO;
    } else if (str.equals(NODE_SZ_LABEL))          { return NodeSize.LABEL;
    } else                                           return NodeSize.LARGE;
  }

  /**
   * Return the node size setting.
   *
   *@return node size setting as a string
   */
  public String nodeSize()           { return findSelected(node_sizes); }

  /**
   * Set the node size setting.
   *
   *@param str node size setting as a string
   */
  public void   nodeSize(String str) { setSelected(node_sizes, str); }

  /**
   * Enumeration for the node color
   */
  enum NodeColor { WHITE, VARY, LABEL, CLUSTERCO };

  /**
   * Return the node color enumeration based on the radio button
   * status for the node color options.
   *
   *@return node color enumeration
   */
  public NodeColor getNodeColor() {
    String str = findSelected(node_colors);
    if        (str.equals(NODE_CO_WHITE))          { return NodeColor.WHITE;
    } else if (str.equals(NODE_CO_VARY))           { return NodeColor.VARY;
    } else if (str.equals(NODE_CO_LABEL))          { return NodeColor.LABEL;
    } else if (str.equals(NODE_CO_CLUSTERCO))      { return NodeColor.CLUSTERCO;
    } else return NodeColor.WHITE;
  }

  /**
   * Return the node color setting as a string.
   *
   *@return node color as a string
   */
  public String nodeColor() { return findSelected(node_colors); }

  /**
   * Set the node color setting.
   *
   *@param str node color setting as a string
   */
  public void nodeColor(String str) { setSelected(node_colors, str); }

  /**
   * Return the flag indicating if node labels should be drawn.
   *
   *@return node labels flag
   */
  public boolean nodeLabels() { return node_labels_cb.isSelected(); }

  /**
   * Set the flag indicating that node labels should / should not be drawn.
   *
   *@param b new node label flag setting
   */
  public void nodeLabels(boolean b) { node_labels_cb.setSelected(b); }

  /**
   * Return the selected entries in the node labels list.
   *
   *@return array of strings that are selected
   */
  public String[] nodeLabelsArray() {
    List<String> list   = Utils.jListGetValuesWrapper(entity_label_list);
    String       strs[] = new String[list.size()]; for (int i=0;i<strs.length;i++) strs[i] = list.get(i);
    return strs;
  }

  /**
   * Return the selected entries in the node colors list.
   *
   *@return array of strings that are selected
   */
  public String[] colorLabelsArray() {
    List<String> list   = Utils.jListGetValuesWrapper(entity_color_list);
    String       strs[] = new String[list.size()]; for (int i=0;i<strs.length;i++) strs[i] = list.get(i);
    return strs;
  }

  /**
   * Return the selected entries in the link labels list.
   *
   *@return array of strings that are selected
   */
  public String[] linkLabelsArray() {
    List<String> list   = Utils.jListGetValuesWrapper(bundle_label_list);
    String       strs[] = new String[list.size()]; for (int i=0;i<strs.length;i++) strs[i] = list.get(i);
    return strs;
  }

  /**
   * Link color enumeration
   */
  enum LinkColor {  GRAY, VARY };

  /**
   * Return the link color enumeration based on the link color
   * radio button.
   *
   *@return link color enumeration
   */
  public LinkColor getLinkColor() {
    String str = findSelected(link_colors);
    if (str.equals(LINK_CO_GRAY))        { return LinkColor.GRAY;
    } else if (str.equals(LINK_CO_VARY)) { return LinkColor.VARY;
    } else return LinkColor.GRAY;
  }

  /**
   * Return the link color setting as a string.
   *
   *@return link color as a string
   */
  public String linkColor() { return findSelected(link_colors); }

  /**
   * Set the link color setting.
   *
   *@param str link color setting as a string
   */
  public void linkColor(String str) { setSelected(link_colors, str); }

  /**
   * Enumeration for the link sizes
   */
  enum LinkSize { INVISIBLE, THIN, NORMAL, THICK, VARY, CONDUCT, CLUSTERP };

  /**
   * Return the link size enumeration based on the link size radio buttons
   *
   *@return link size enumeration
   */
  public LinkSize getLinkSize() {
    String str = findSelected(link_sizes);
    if        (str.equals(LINK_SZ_INVISIBLE)) { return LinkSize.INVISIBLE;
    } else if (str.equals(LINK_SZ_THIN))      { return LinkSize.THIN;
    } else if (str.equals(LINK_SZ_NORMAL))    { return LinkSize.NORMAL;
    } else if (str.equals(LINK_SZ_THICK))     { return LinkSize.THICK;
    } else if (str.equals(LINK_SZ_VARY))      { return LinkSize.VARY;
    } else if (str.equals(LINK_SZ_CONDUCT))   { return LinkSize.CONDUCT;
    } else if (str.equals(LINK_SZ_CLUSTERP))  { return LinkSize.CLUSTERP;
    } else return LinkSize.NORMAL;
  }

  /**
   * Return the link size as a string.
   *
   *@return link size as string
   */
  public String linkSize() { return findSelected(link_sizes); }

  /**
   * Set the link size setting.
   *
   *@param str link size setting as string
   */
  public void   linkSize(String str) { setSelected(link_sizes, str); }

  /**
   * Return the flag indicating if link labels should be drawn.
   *
   *@return link labels flag
   */
  public boolean linkLabels() { return link_labels_cb.isSelected(); }

  /**
   * Set the flag to indicate link labels should be drawn.
   *
   *@param b new setting for drawing link labels
   */
  public void linkLabels(boolean b) { link_labels_cb.setSelected(b); }

  /**
   * Return true if link curves is enabled.
   *
   *@return true for link curves
   */
  public boolean           linkCurves() { return link_curves_cbmi.isSelected(); }

  /**
   * Set the link curves option.
   *
   *@param b new link curves setting
   */
  public void              linkCurves(boolean b) { link_curves_cbmi.setSelected(b); }

  /**
   * Return true if link transparency is enabled.
   *
   *@return true for link transparency
   */
  public boolean           linksTransparent() { return link_trans_cbmi.isSelected();  }

  /**
   * Set the link transparency option.
   *
   *@param b new link transparency setting
   */
  public void              linksTransparent(boolean b) { link_trans_cbmi.setSelected(b); }

  /**
   * Return true if link arrows are enabled.
   *
   *@return true for link arrows
   */
  public boolean           drawArrows()       { return arrows_cbmi.isSelected();      }

  /**
   * Set the draw arrows option.
   *
   *@param b new draw arrows flag
   */
  public void              drawArrows(boolean b) { arrows_cbmi.setSelected(b); }

  /**
   * Return the flag indicating if dynamic labeling is enabled.  Dynamic labeling
   * shows the node under the mouse as well as the neighbors of that node (if they
   * are visible).
   *
   *@return true if dynamic labels should be shown under the mouse
   */
  public boolean           dynamicLabels()    { return dyn_label_cbmi.isSelected(); }

  /**
   * Set the dynamic labeling option.
   *
   *@param b new dynamic labeling setting
   */
  public void              dynamicLabels(boolean b) { dyn_label_cbmi.setSelected(b); }

  /**
   * Return true if timing marks are enabled.
   *
   *@return true for timing marks
   */
  public boolean           drawTiming()       { return timing_cbmi.isSelected();      }

  /**
   * Set the option to draw timing marks on the links.
   *
   *@param b new timing marks setting
   */
  public void              drawTiming(boolean b) { timing_cbmi.setSelected(b); }

  /**
   * Return true if node label rendering is enabled
   *
   *@return true for node labels
   */
  public boolean           drawNodeLabels()       { return node_labels_cb.isSelected();        }

  /**
   * Return true if link labeling is enabled
   *
   *@return true for link labels
   */
  public boolean           drawLinkLabels()       { return link_labels_cb.isSelected();        }

  /**
   * Toggle labeling drawing.
   */
  public void              toggleLabels()     { node_labels_cb.setSelected(!node_labels_cb.isSelected()); }

  /**
   * Return a list of the labels that are selected for viewing related to the bundles (links).
   *
   *@return list of labels for the links
   */
  public java.util.List<String> listBundleLabels() { return Utils.jListGetValuesWrapper(bundle_label_list); }

  /** 
   * Return a list of the labels that are selected for viewing related to entities (nodes).
   *
   *@return list of labels for nodes
   */
  public java.util.List<String> listEntityLabels() { return Utils.jListGetValuesWrapper(entity_label_list); }

  /** 
   * Return a list of the labels for coloring the nodes.
   *
   *@return list of labels for node coloring
   */
  public java.util.List<String> listEntityColor() { return Utils.jListGetValuesWrapper(entity_color_list); }

  /**
   * Update the labels menu based on the various fields in application.
   */
  @Override
  public void updateBys() { updateLabelLists(getRTParent().getEntityTagTypes()); fillCommonRelationshipsMenu(); }

  /**
   * Update the tag types for the labeling menu so that individual tag types can be used
   * for labeling and coloring.
   */
  @Override
  public void updateEntityTagTypes(Set<String> types) { updateLabelLists(types); fillCommonRelationshipsMenu(); }

  /**
   * Upon new bundles (records) being added to the application, ensure that those
   * links (edges) are in the graph and have the corresponding bundle-to-edge mapping.
   *
   *@param set new bundles
   */
  @Override
  public void newBundlesAdded(Set<Bundle> set) {
// System.err.println("newBundlesAdded(set.size = " + set.size() + ")");
    Bundles as_bundles = getRTParent().getRootBundles().subset(set);
    for (int i=0;i<active_relationships.size();i++) {
// System.err.println("  newBundlesAdded():  Adding In Relationship " + i + " \"" + active_relationships.get(i) + "\"...  bundles.size() = " + as_bundles.size());
      addRelationship(active_relationships.get(i), false, as_bundles);
    }
  }

  /**
   * Set a new root bundle set.  This is used at the application level to
   * permanently remove unwanted data so that relevant/verified data can
   * be better examinined.  This method is complicated because the state
   * of the component has to be managed (most other components are stateless).
   *
   *@param new_root new root bundle set
   */
  @Override
  public void newBundlesRoot(Bundles new_root) {
    // Adjust state of super
    super.newBundlesRoot(new_root);
    // Save some state
    Map<String,Point2D> wxy_copy = entity_to_wxy;
    // Reset stateful variables for this class
    digraph = new SimpleMyGraph<Bundle>();
    graph   = new SimpleMyGraph<Bundle>();
    entity_to_shape = new HashMap<String,Utils.Symbol>();
    entity_to_wxy   = new HashMap<String,Point2D>();
    entity_to_sxy   = new HashMap<String,String>();
    entity_to_sx    = new HashMap<String,Integer>();
    entity_to_sy    = new HashMap<String,Integer>();
    // Reapply all the relationships
    for (int i=0;i<active_relationships.size();i++) {
      // System.err.println("newBundlesRoot():  Adding In Relationship \"" + active_relationships.get(i) + "\"...  bundles.size() = " + new_root.size());
      addRelationship(active_relationships.get(i), false, new_root);
    }
    // Reapply the world coordinates
    Iterator<String> it = entity_to_wxy.keySet().iterator();
    while (it.hasNext()) {
      String entity = it.next();
      if (wxy_copy.containsKey(entity)) entity_to_wxy.put(entity, wxy_copy.get(entity));
    }
    transform();
  }

  /**
   * Indicate that entities exist in the view and can be selected for comments.
   *
   *@return true
   */
  public boolean supportsEntityComments() { return true; }

  /**
   *
   */
  Set<String> retained_nodes = new HashSet<String>();

  /**
   * Reform the graph to only have the visible nodes present.  Destructive to the graph... (experimental)
   */
  public void retainOnlyVisibleNodes() {
    RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) getRTComponent().rc; if (myrc == null) return;
    Set<String> new_retained_nodes = new HashSet<String>();
    Iterator<String> it = myrc.entity_counter_context.binIterator();
    while (it.hasNext()) new_retained_nodes.add(it.next());
    retained_nodes = new_retained_nodes;
    newBundlesRoot(getRTParent().getRootBundles());
  }

  /**
   * Add the application selection (usually the same linknode window) to the retained nodes set.  Re-create the graph
   * with the new settings.
   */
  public void addSelectionToRetained() {
    retained_nodes.addAll(getRTParent().getSelectedEntities());
    newBundlesRoot(getRTParent().getRootBundles());
  }

  /**
   * Clear the retained node set, basically re-adding all the nodes to the graph.
   */
  public void clearRetained() {
    retained_nodes.clear();
    newBundlesRoot(getRTParent().getRootBundles());
  }

  /**
   * For the edges that are visible, add all of the bundles (records) that relate to those edges.
   */
  public void addAllOnVisibleLinks() {
    // Check for a valid render context and create the new set
    RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) getRTComponent().rc; if (myrc == null) return;
    Set<Bundle> set = new HashSet<Bundle>();
    // Go through visible links and find the related bundles
    Iterator<String> it = myrc.graphedgeref_to_link.keySet().iterator();
    while (it.hasNext()) {
      String graphedgeref = it.next();
      Iterator<Bundle> itb = digraph.linkRefIterator(graphedgeref);
      while (itb.hasNext()) set.add(itb.next());
    }
    // Add the no mapping set and push it to the RTParent
    set.addAll(getRTComponent().getNoMappingSet()); getRTParent().push(getRTParent().getRootBundles().subset(set));
  }

  /**
   * Filter the visible records to only those links/edges that have both nodes as sources.
   */
  public void filterToBidirectionalLinks() {
    RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) getRTComponent().rc; if (myrc == null) return;

    // Get the visible links, extract the direction, create the reverse direction -- if that exists, add those records to keepers
    Iterator<String> it = myrc.graphedgeref_to_link.keySet().iterator(); Set<Bundle> to_keep = new HashSet<Bundle>();
    while (it.hasNext()) {
      String graph_edge_ref = it.next(); String line_ref = myrc.graphedgeref_to_link.get(graph_edge_ref);
      int fm_i = digraph.linkRefFm(graph_edge_ref);
      int to_i = digraph.linkRefTo(graph_edge_ref);
      String other_dir = digraph.getLinkRef(to_i,fm_i);
      if (myrc.graphedgeref_to_link.containsKey(other_dir)) { to_keep.addAll(myrc.link_counter_context.getBundles(line_ref)); }
    }

    // Add the no mapping set and push it to the RTParent
    to_keep.addAll(getRTComponent().getNoMappingSet()); getRTParent().push(getRTParent().getRootBundles().subset(to_keep));
  }

  /**
   * Expand the graph (non-time specific) by one hop.  If the digraph option is set, only
   * expand using the directional graph.
   *
   *@param use_digraph use directed edges only if true
   */
  public void makeOneHopsVisible(boolean use_digraph) {
    RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) getRTComponent().rc; if (myrc == null) return;
    // Get the visible links, extract the nodes
    Iterator<String> it = myrc.graphedgeref_to_link.keySet().iterator(); Set<Integer> nodes = new HashSet<Integer>();
    while (it.hasNext()) {
      String linkref = it.next();
      nodes.add(digraph.linkRefFm(linkref));
      nodes.add(digraph.linkRefTo(linkref));
    }
    // Go through the nodes, find all their edges, and re-add the bundles to the set
    SimpleMyGraph<Bundle> g = use_digraph ? digraph : graph;
    Set<Bundle> set = new HashSet<Bundle>();

    Iterator<Integer> iti = nodes.iterator();
    while (iti.hasNext()) {
      int node_i = iti.next();
      for (int i=0;i<g.getNumberOfNeighbors(node_i);i++) {
        int    nbor_i  = g.getNeighbor(node_i,i);
        String linkref = g.getLinkRef(node_i,nbor_i);

        // Add the associated bundles
	Iterator<Bundle> itb = g.linkRefIterator(linkref);
	while (itb.hasNext()) set.add(itb.next());
      }
    }
    // Add the no mapping set and push it to the RTParent
    set.addAll(getRTComponent().getNoMappingSet()); getRTParent().push(getRTParent().getRootBundles().subset(set));
  }

  /**
   * Apply the tags to the selected entities.
   *
   *@param tags tags to apply
   */
  public void tagSelectedEntities(String tags) {
    RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) (getRTComponent().rc); if (myrc == null) return;
    if (tags != null && tags.equals("") == false) {
      getRTParent().setSelectedEntities(myrc.filterEntities(getRTParent().getSelectedEntities()));
      getRTParent().tagSelectedEntities(tags, myrc.bs.ts0(), myrc.bs.ts1());
    }
  }

  /**
   * Clear out the tags for the selected entities.
   */
  public void clearEntityTagsForSelectedEntities() {
    getRTParent().clearEntityTagsForSelectedEntities();
  }

  /**
   * Select nodes in the current graph based on either a CIDR, IPv4, or substring.
   * - if the selection string is preceded by the following, perform a set operation:
   *   - "!" - select the inverse
   *   - "*" - intersect with selection
   *   - "+" - add to selection
   *   - "-" - remove from selection
   * REFACTOR into a utility that works across components for entity selection.
   *
   *@param pattern pattern for string selection
   */
  public void select(String pattern) {
    RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) getRTComponent().rc; if (myrc == null) return;
    if (pattern == null || pattern.length()==0) return;
    // Extract the set operation first
    char first = pattern.charAt(0); boolean invert = false;
    if (first == '!') { pattern = pattern.substring(1,pattern.length()); invert = true; }
         first = pattern.charAt(0); 
    if (first == '*' || first == '-' || first == '+') pattern = pattern.substring(1,pattern.length());

    // Get the various sets...
    Set<String> matched_entities          = new HashSet<String>(), 
                all_entities              = new HashSet<String>(), 
                already_selected_entities = myrc.filterEntities(getRTParent().getSelectedEntities());
    if (already_selected_entities == null) already_selected_entities = new HashSet<String>();

    if        (Utils.isIPv4CIDR(pattern)) {
      Iterator<String> it = myrc.visible_entities.iterator();
      while (it.hasNext()) {
        String str = it.next(); String orig_str = str; if (str.indexOf(BundlesDT.DELIM) >= 0) str = str.substring(str.lastIndexOf(BundlesDT.DELIM)+1,str.length());
        if (Utils.isIPv4(str) && Utils.ipMatchesCIDR(str,pattern)) matched_entities.add(orig_str);
      }
    } else if (Utils.isIPv4(pattern))   {
      Iterator<String> it = myrc.visible_entities.iterator();
      while (it.hasNext()) {
        String str = it.next(); String orig_str = str;  if (str.indexOf(BundlesDT.DELIM) >= 0) str = str.substring(str.lastIndexOf(BundlesDT.DELIM)+1,str.length());
	if (str.equals(pattern)) matched_entities.add(orig_str);
      }
    } else if (pattern.startsWith("tag" + BundlesDT.DELIM)) {
      Set<String> tagged_entities = getRTParent().getEntitiesWithTag(pattern.substring(4,pattern.length()));
      if (tagged_entities.size() < myrc.visible_entities.size()) {
        Iterator<String> it = tagged_entities.iterator();
        while (it.hasNext()) {
          String entity = it.next();
          if (myrc.visible_entities.contains(entity)) matched_entities.add(entity);
        }
      } else {
        Iterator<String> it = myrc.visible_entities.iterator();
        while (it.hasNext()) {
          String entity = it.next();
          if (tagged_entities.contains(entity)) matched_entities.add(entity);
        }
      }
    } else if (pattern.startsWith("REGEX:")) {
      Pattern regex = Pattern.compile(pattern.substring(6));
      Iterator<String> it = myrc.visible_entities.iterator();
      while (it.hasNext()) {
        String entity = it.next();
	if (regex.matcher(entity).matches()) matched_entities.add(entity);
      }
    } else                               {
      Iterator<String> it = myrc.visible_entities.iterator();
      while (it.hasNext()) {
        String str = it.next(); if (str.toLowerCase().indexOf(pattern.toLowerCase()) >= 0) matched_entities.add(str);
      }
    }

    // Apply the set operation
    if        (invert)       { 
      all_entities.removeAll(matched_entities);
      if        (first == '*') { Set<String> set = new HashSet<String>();
                                 Iterator<String> it = all_entities.iterator();
                                 while (it.hasNext()) { 
                                   String str = it.next(); 
                                   if (already_selected_entities.contains(str)) 
                                     set.add(str); 
                                 }
                                                                                    getRTParent().setSelectedEntities(set);
      } else if (first == '-') { already_selected_entities.removeAll(all_entities); getRTParent().setSelectedEntities(already_selected_entities);
      } else if (first == '+') { already_selected_entities.addAll(all_entities);    getRTParent().setSelectedEntities(already_selected_entities);
      } else                   {                                                    getRTParent().setSelectedEntities(all_entities); }
    } else if (first == '*') { Set<String> set = new HashSet<String>();
                               Iterator<String> it = matched_entities.iterator();
                               while (it.hasNext()) { 
                                 String str = it.next(); 
                                 if (already_selected_entities.contains(str)) 
                                   set.add(str); 
                               }
                                                                                      getRTParent().setSelectedEntities(set);
    } else if (first == '-') { already_selected_entities.removeAll(matched_entities); getRTParent().setSelectedEntities(already_selected_entities);
    } else if (first == '+') { already_selected_entities.addAll(matched_entities);    getRTParent().setSelectedEntities(already_selected_entities);
    } else                   {                                                        getRTParent().setSelectedEntities(matched_entities); }
  }


  /**
   * Create panel for handling labeling.
   *
   *@param  entity_tag_types types from type-value tags
   *
   *@return gui panel for labeling
   */
  private JPanel createLabelsPanel(Set<String> entity_tag_types) {
    JPanel panel  = new JPanel(new BorderLayout());
     JPanel labels_panel = new JPanel(new GridLayout(1,3,4,4));
      labels_panel.add(new JLabel("Labels"));
      labels_panel.add(node_labels_cb = new JCheckBox("Node", false)); defaultListener(node_labels_cb);
      labels_panel.add(link_labels_cb = new JCheckBox("Link", false)); defaultListener(link_labels_cb);
      panel.add("North", labels_panel);
    JPanel center = new JPanel(new GridLayout(3,1)); JScrollPane scroll;
      // Node Labels
      center.add(scroll = new JScrollPane(entity_label_list = new JList())); defaultListener(entity_label_list);
      scroll.setBorder(BorderFactory.createTitledBorder("Node Label"));
      // Node Color
      center.add(scroll = new JScrollPane(entity_color_list = new JList())); defaultListener(entity_color_list);
      scroll.setBorder(BorderFactory.createTitledBorder("Node Color"));
      entity_color_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      // Link Labels
      center.add(scroll = new JScrollPane(bundle_label_list = new JList())); defaultListener(bundle_label_list);
      scroll.setBorder(BorderFactory.createTitledBorder("Link Label"));
      panel.add("Center", center);
    updateLabelLists(entity_tag_types);
    return panel;
  }

  /**
   * Create the labels menu from the existing fields within the bundles.
   * In addition to data types, there are various high-level labeling options
   * at the beginning of each list (entity, number of bundles, etc.)
   *
   *@param entity_tag_types types from type-value tags
   */
  private void updateLabelLists(Set<String> entity_tag_types) {
    updateEntityLabelList(entity_tag_types, entity_label_list);
    updateEntityLabelList(entity_tag_types, entity_color_list);
    updateBundleLabelList();
  }

  /**
   * Update the labels specific for entities.  These are more related to entity data
   * types.
   *
   *@param entity_tag_types types from type-value tags
   *@param gui_list         list to update
   */
  private void updateEntityLabelList(Set<String> entity_tag_types, JList gui_list) {
    List<String> selection = Utils.jListGetValuesWrapper(gui_list);
    List<String> items     = new ArrayList<String>();
    // Add the transforms
    BundlesG globals = getRTParent().getRootBundles().getGlobals();
    String postproc[] = BundlesDT.listAvailablePostProcessors();
    for (int i=0;i<postproc.length;i++) items.add(postproc[i]);
    /*
      String transforms[] = globals.getTransforms();
      for (int i=0;i<transforms.length;i++) items.add(transforms[i]);
    */
    // Add the tag types
    Iterator<String> it = entity_tag_types.iterator(); 
    while (it.hasNext()) items.add(TAG_TYPE_LM + it.next());
    Collections.sort(items, new CaseInsensitiveComparator());
    // Insert the default items
    items.add(0, TIMEFRAME_LM);
    items.add(0, LASTHEARD_LM);
    items.add(0, FIRSTHEARD_LM);
    items.add(0, DEGREE_LM);
    items.add(0, TAGS_LM);
    items.add(0, BUNDLECOUNT_LM);
    items.add(0, ENTITYCOUNT_LM);
    items.add(0, ENTITY_LM);
    // Convert and update the list
    setListAndResetSelection(gui_list, items, selection);
  }

  /**
   * Update the edge (link) labeling list.
   *
   *@param entity_tag_types types from type-value tags
   */
  private void updateBundleLabelList() {
    List<String> selection  = Utils.jListGetValuesWrapper(bundle_label_list);
    List<String> items      = new ArrayList<String>();
    // Go through all of the header fields and add them with their variations
    boolean tags_present = false;
    BundlesG globals = getRTParent().getRootBundles().getGlobals();
    Iterator<String> it = globals.fieldIterator();
    while (it.hasNext()) {
      String fld = it.next(); int fld_i = globals.fieldIndex(fld);
      if (globals.isScalar(fld_i))   {
        items.add(fld + SIMPLESTAT_LM);
	items.add(fld + COMPLEXSTAT_LM);
      } else if (fld.equals("tags")) {
        tags_present = true;
      } else                         {
        items.add(fld + ITEMS_LM);
	items.add(fld + COUNT_LM);
      }
      BundlesDT.DT datatype = globals.getFieldDataType(fld_i);
      if (globals.isScalar(fld_i) == false && datatype == BundlesDT.DT.INTEGER) datatype = null;
      if (datatype != null) {
        String appends[] = BundlesDT.dataTypeVariations(datatype, globals);
	for (int i=0;i<appends.length;i++) items.add(fld + BundlesDT.DELIM + appends[i]);
      }
    }
    Collections.sort(items, new CaseInsensitiveComparator());
    // Add the tag options at the end
    if (tags_present) {
      items.add("tags" + ITEMS_LM);
      items.add("tags" + BundlesDT.MULTI + ITEMS_LM);
      items.add("tags" + BundlesDT.MULTI + COUNT_LM);
      Iterator<String> it2 = globals.tagTypeIterator();
      while (it2.hasNext()) {
        String tag_type = it2.next();
        items.add("tags" + BundlesDT.MULTI + BundlesDT.DELIM + tag_type + ITEMS_LM);
        items.add("tags" + BundlesDT.MULTI + BundlesDT.DELIM + tag_type + COUNT_LM);
      }
    }
    // Insert the default items
    items.add(0, TIMEFRAME_LM);
    items.add(0, LASTHEARD_LM);
    items.add(0, FIRSTHEARD_LM);
    items.add(0, BUNDLECOUNT_LM);
    // Convert and update the list
    setListAndResetSelection(bundle_label_list, items, selection);
  }

  /**
   * Wrapper to set the list of items in a JList and then to keep the
   * selected elements.
   *
   *@param list       gui list to modify
   *@param items      all items that should be in the list
   *@param selection  selected items to retain in the list
   */
  private void setListAndResetSelection(JList list, List<String> items, java.util.List selection) {
    Map<String,Integer> str_to_i = new HashMap<String,Integer>();
    String as_str[] = new String[items.size()]; for (int i=0;i<as_str.length;i++) { as_str[i] = items.get(i); str_to_i.put(as_str[i], i); }
    list.setListData(as_str);
    if (selection.size() > 0) {
      List<Integer> ints = new ArrayList<Integer>();      
      // Find the indices for the previous settings
      for (int i=0;i<selection.size();i++)
        if (str_to_i.containsKey(selection.get(i))) 
	  ints.add(str_to_i.get(selection.get(i)));
      // Convert back to ints
      int as_ints[] = new int[ints.size()];
      for (int i=0;i<as_ints.length;i++) as_ints[i] = ints.get(i);
      list.setSelectedIndices(as_ints);
    }
  }

  /**
   * Graph instance (directed)
   */
  SimpleMyGraph<Bundle> digraph = new SimpleMyGraph<Bundle>(),
  /**
   * Graph instance (undirected)
   */
                        graph   = new SimpleMyGraph<Bundle>();
  /**
   * Bi-connected component analysis of the graph
   */
  BiConnectedComponents graph_bcc, 

  /**
   * Bi-connected component analysis of the graph after single node neighbors are removed.
   * This provides non-trivial bi-connected components.
   */
                        graph2p_bcc;

  /**
   * Cluster Coefficients
   */
  Map<String,Double>    cluster_cos;

  /**
   *
   */
  Conductance           conductance;

  /** 
   * Create a subset of the data that only leaves one record per edge in the graph.  This
   * is used to make the graph render faster for interactive layout.
   */
  public void subsetOneBundlePerEdge() {
    Set<Bundle> set = new HashSet<Bundle>();
    for (int ent_i=0;ent_i<digraph.getNumberOfEntities();ent_i++) {
      for (int i=0;i<digraph.getNumberOfNeighbors(ent_i);i++) {
        int nbor_i = digraph.getNeighbor(ent_i,i);
	Iterator<Bundle> it = digraph.linkRefIterator(digraph.linkRef(ent_i,nbor_i));
	set.add(it.next());
      }
    }
    getRTParent().push(getRTParent().getRootBundles().subset(set));
  }

  /**
   * Layout IP address nodes by sub-dividing the octets recursively.
   */
  public void ipLogicalOctetLayout() {
    // Choose the appropriate set
    Set<String> sel = getRTParent().getSelectedEntities(); Iterator<String> it;
    if (sel != null && sel.size() > 0) it = sel.iterator(); else it = entity_to_wxy.keySet().iterator();

    // Go through the set and layout the IP addresses
    double base_x = -32.0, base_y = -32.0;
    while (it.hasNext()) {
      String entity = it.next();
      if (Utils.isIPv4(entity)) {
        StringTokenizer st = new StringTokenizer(entity,"."); 
	int ip0 = Integer.parseInt(st.nextToken()), mx0 = ip0/16, my0 = ip0%16,
	    ip1 = Integer.parseInt(st.nextToken()), mx1 = ip1/16, my1 = ip1%16,
	    ip2 = Integer.parseInt(st.nextToken()), mx2 = ip2/16, my2 = ip2%16,
	    ip3 = Integer.parseInt(st.nextToken()), mx3 = ip3/16, my3 = ip3%16;
        entity_to_wxy.put(entity, new Point2D.Double(base_x + mx0 + mx1/16.0 + mx2/256.0 + mx3/8192.0,
	                                             base_y + my0 + my1/16.0 + my2/256.0 + my3/8192.0));
        transform(entity);
      }
    }
    zoomToFit(); repaint();
  }

  /**
   * Layout the nodes by their color using a treemap.  Assumes that the color option is not trivial.
   */
  public void nodeColorTreeMapLayout() {
    // Get a valid render context
    RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) (getRTComponent().getRTRenderContext()); if (myrc == null) return;

    // Check for a valid color setting
    NodeColor node_color = getNodeColor();
    if (node_color == NodeColor.VARY || node_color == NodeColor.LABEL) {
      myrc.nodeColorTreeMapLayout(node_color);
    } else System.err.println("RTGraphPanel.nodeColorTreeMapLayout() - Only works with VARY or LABEL");
  }

  /**
   * Collapse blocks of the graph (based on the biconnected components) into single aggregate
   * nodes.
   */
  public void collapseBlocks() {
    // Create it if it's null
    if (graph_bcc == null) {
      // Create graph parametrics (Only add linear time algorithms here...)
      graph_bcc     = new BiConnectedComponents(graph);
      graph2p_bcc   = new BiConnectedComponents(new UniTwoPlusDegreeGraph(graph));
    }
    BiConnectedComponents bcc = graph_bcc, bcc_2p = graph2p_bcc; if (bcc != null && bcc_2p != null) {
      // Get the vertex to block lookup
      Map<String,Set<MyGraph>> v_to_b = bcc.getVertexToBlockMap();
      // Go through the entities and accumulate the positions
      Map<MyGraph,Double> x_sum = new HashMap<MyGraph,Double>(), y_sum = new HashMap<MyGraph,Double>();
      Iterator<String> it_e = entity_to_wxy.keySet().iterator();
      while (it_e.hasNext()) {
        String entity = it_e.next(); Point2D pt = entity_to_wxy.get(entity);
	if (v_to_b.containsKey(entity)) {
	  Iterator<MyGraph> it_mg = v_to_b.get(entity).iterator();
	  while (it_mg.hasNext()) {
	    MyGraph mg = it_mg.next();
	    if (x_sum.containsKey(mg) == false) { x_sum.put(mg,0.0); y_sum.put(mg,0.0); }
	    x_sum.put(mg,x_sum.get(mg)+pt.getX()); y_sum.put(mg,y_sum.get(mg)+pt.getY());
	  }
        } else System.err.println("Vertex To Block Lookup missing \"" + entity + "\"");
      }
      // Now position those entities that aren't cut vertices at the center of the graph
      it_e = entity_to_wxy.keySet().iterator();
      while (it_e.hasNext()) {
        String entity = it_e.next(); Point2D pt = entity_to_wxy.get(entity);
	if (v_to_b.containsKey(entity) && bcc.getCutVertices().contains(entity) == false) {
          MyGraph mg = v_to_b.get(entity).iterator().next();
	  entity_to_wxy.put(entity, new Point2D.Double(x_sum.get(mg)/mg.getNumberOfEntities(),y_sum.get(mg)/mg.getNumberOfEntities()));
	  transform(entity);
	}
      }
      // Re-render
      zoomToFit(); repaint();
    }
  }

  /**
   * Layout the nodes in the x-axis based on their temporal occurence within the data set.
   * The y-position is calculated based on the corresponding integer values for the entities.
   */
  public void temporalLayout() {
    // Choose the appropriate set
    Set<String> sel = getRTParent().getSelectedEntities(); Iterator<String> it;
    if (sel != null && sel.size() > 0) it = sel.iterator(); else it = entity_to_wxy.keySet().iterator();
    // Get globals for string to int conversion
    Bundles  root    = getRTParent().getRootBundles();
    BundlesG globals = root.getGlobals();
    // Go through the selection and place the nodes
    while (it.hasNext()) {
      String entity     = it.next(); int entity_i = graph.getEntityIndex(entity);
      int    entity_int = globals.toInt(entity); 
      long   ts0 = root.ts1(), ts1 = root.ts0();
      // Go through the neighbors and iterate through the underlying bundles
      for (int i=0;i<graph.getNumberOfNeighbors(entity_i);i++) {
        int              nbor_i   = graph.getNeighbor(entity_i, i);
	String           linkref  = graph.getLinkRef(entity_i, nbor_i);
        Iterator<Bundle> it_b     = graph.linkRefIterator(linkref);
	while (it_b.hasNext()) {
	  Bundle bundle = it_b.next();
	  if (bundle.hasTime()) {
	    if (ts0 > bundle.ts0()) ts0 = bundle.ts0();
	    if (ts1 < bundle.ts0()) ts1 = bundle.ts0();
	  }
	}
      }
      // Place the node
      Point2D point = new Point2D.Double((((double) ts0)/(root.ts1() - root.ts0())),((double) entity_int)/Integer.MAX_VALUE);
      entity_to_wxy.put(entity, point);
      // Do the transformation
      transform(entity);
    }
    zoomToFit(); repaint();
  }

  /**
   * Edge (link) style strings
   */
  final static String STYLE_SOLID_STR     = "Style - Solid",
                      STYLE_LONG_DASH_STR = "Style - Long Dash",
                      STYLE_DOTTED_STR    = "Style - Dotted",
                      STYLE_ALTERNATE_STR = "Style - Long/Short Alt";

  /**
   * Array of edge style strings
   */
  final static String STYLE_STRS[] = { STYLE_SOLID_STR,
                                       STYLE_LONG_DASH_STR,
                                       STYLE_DOTTED_STR,
                                       STYLE_ALTERNATE_STR };

  /**
   * Enumeration for edge styles
   */
  enum LineStyle { SOLID, LONG_DASH, DOTTED, ALTERNATE };

  /**
   * Map to convert link style strings to the corresponding enumeration.
   */
  static Map<String,LineStyle> str_to_linsty;
  static {
    str_to_linsty = new HashMap<String,LineStyle>();
    str_to_linsty.put(STYLE_SOLID_STR, LineStyle.SOLID);
    str_to_linsty.put(STYLE_LONG_DASH_STR, LineStyle.LONG_DASH);
    str_to_linsty.put(STYLE_DOTTED_STR, LineStyle.DOTTED);
    str_to_linsty.put(STYLE_ALTERNATE_STR, LineStyle.ALTERNATE);
  }

  /**
   * Select nodes by their symbol.
   *
   *@param symbol symbol to select by
   */
  public void selectBySymbol(Utils.Symbol symbol) {
    Set<String> sel = new HashSet<String>();
    Iterator<String> it = entity_to_shape.keySet().iterator();
    while (it.hasNext()) { String entity = it.next(); if (entity_to_shape.get(entity) == symbol) sel.add(entity); }
    ((RTGraphComponent) getRTComponent()).setOperation(sel);
  }

  /**
   * Method to convert strings to line styles.
   *
   *@param  str edge/line/link style string
   *
   *@return enumeration
   */
  public LineStyle parseStyle(String str) { return str_to_linsty.get(str); }

  /**
   * Entity_to_shape - the shape of the entity when drawn
   */
  Map<String,Utils.Symbol>       entity_to_shape = new HashMap<String,Utils.Symbol>();

  /**
   * Entity_to_wxy - the entities world x, y coordinates
   */
  Map<String,Point2D>      entity_to_wxy   = new HashMap<String,Point2D>();

  /**
   * Entity_to_sxy - the entities screen x, y coordinates (transformed from the world coordinates)
   * - the sxy is stored as a string for mapping in other contexts
   */
  Map<String,String>       entity_to_sxy   = new HashMap<String,String>();

  /**
   * entity_to_sx, _to_sy - entity to screen x and screen y separately -- much faster for conversion
   */
  Map<String,Integer>      entity_to_sx    = new HashMap<String,Integer>(),
                           entity_to_sy    = new HashMap<String,Integer>();

  /**
   * Current set of active relationships.
   */
  List<String>       active_relationships   = new ArrayList<String>();

  /**
   * Mapping of the active relationship string to the edge/link/line style.
   */
  Map<String,String>  relationships_to_style = new HashMap<String,String>();

  /**
   * Static string for storing application parameters related to recent relationships.
   */
  static final String RECENT_RELATIONSHIPS_PREF_STR = "RTGraphPanel.recentRelationships";

  /**
   * Keep recently used relationships in the common relationships menu.
   * Write them to preferences, limit to 20
   *
   *@param encoded encoded version of the relationship string (maybe)
   */
  public void updateRecentRelationships(String encoded) {
    String strs[]     = RTPrefs.retrieveStrings(RECENT_RELATIONSHIPS_PREF_STR);
    String new_strs[];
    if (strs == null || strs.length == 0) {
      // Make a default one
      new_strs = new String[1]; new_strs[0] = Utils.encToURL(encoded) + BundlesDT.DELIM + System.currentTimeMillis();
    } else {
      // Transfer existing to a map for recency/set update
      Map<String,Long> map = new HashMap<String,Long>();
      long earliest = 0L;
      for (int i=0;i<strs.length;i++) { StringTokenizer st = new StringTokenizer(strs[i], BundlesDT.DELIM);
                                        String str = st.nextToken(); long ts = Long.parseLong(st.nextToken());
					if (earliest == 0L) earliest = ts; else if (ts < earliest) earliest = ts;
					map.put(str,ts); }
      // Add the new one
      map.put(Utils.encToURL(encoded),System.currentTimeMillis());
      // Only keep 20 or less based on recency
      if (map.keySet().size() > 20) {
        Iterator<String> it = map.keySet().iterator();
	while (it.hasNext()) {
	  String str = it.next(); if (earliest == map.get(str)) it.remove();
	}
      }
      // Transfer to a set of strings
      new_strs = new String[map.keySet().size()];
      Iterator<String> it = map.keySet().iterator();
      for (int i=0;i<new_strs.length;i++) {
        String str = it.next(); long ts = map.get(str);
	new_strs[i] = str + BundlesDT.DELIM + ts;
      }
    }
    // Store it off and update the menu // Note that this won't go across other RTGraphPanel windows -- will need a restart
    RTPrefs.store(RECENT_RELATIONSHIPS_PREF_STR, new_strs);
    fillCommonRelationshipsMenu();
  }

  /**
   * Extract the "from typed" field from an encoded relationship string.
   *
   *@param  rel_str encoded relationship string
   *@return "from typed" flag
   */
  public boolean relationshipFromTyped(String rel_str) {
    StringTokenizer st = new StringTokenizer(rel_str, BundlesDT.DELIM);
    String  fm_hdr = Utils.decFmURL(st.nextToken()), fm_ico = Utils.decFmURL(st.nextToken()); boolean fm_typed = st.nextToken().toLowerCase().equals("true");
    String  to_hdr = Utils.decFmURL(st.nextToken()), to_ico = Utils.decFmURL(st.nextToken()); boolean to_typed = st.nextToken().toLowerCase().equals("true");
    String  style  = Utils.decFmURL(st.nextToken()); return fm_typed; }

  /**
   * Extract the "to typed" field from an encoded relationship string.
   *
   *@param  rel_str encoded relationship string
   *@return "to typed" flag
   */
  public boolean relationshipToTyped(String rel_str) {
    StringTokenizer st = new StringTokenizer(rel_str, BundlesDT.DELIM);
    String  fm_hdr = Utils.decFmURL(st.nextToken()), fm_ico = Utils.decFmURL(st.nextToken()); boolean fm_typed = st.nextToken().toLowerCase().equals("true");
    String  to_hdr = Utils.decFmURL(st.nextToken()), to_ico = Utils.decFmURL(st.nextToken()); boolean to_typed = st.nextToken().toLowerCase().equals("true");
    String  style  = Utils.decFmURL(st.nextToken()); return to_typed; }

  /**
   * Extract the "from header" field from an encoded relationship string.
   *
   *@param  rel_str encoded relationship string
   *@return "from header" flag
   */
  public String relationshipFromHeader(String rel_str) {
    StringTokenizer st = new StringTokenizer(rel_str, BundlesDT.DELIM);
    String  fm_hdr = Utils.decFmURL(st.nextToken()), fm_ico = Utils.decFmURL(st.nextToken()); boolean fm_typed = st.nextToken().toLowerCase().equals("true");
    String  to_hdr = Utils.decFmURL(st.nextToken()), to_ico = Utils.decFmURL(st.nextToken()); boolean to_typed = st.nextToken().toLowerCase().equals("true");
    String  style  = Utils.decFmURL(st.nextToken()); return fm_hdr; }

  /**
   * Extract the "to header" field from an encoded relationship string.
   *
   *@param  rel_str encoded relationship string
   *@return "to header" flag
   */
  public String relationshipToHeader(String rel_str) {
    StringTokenizer st = new StringTokenizer(rel_str, BundlesDT.DELIM);
    String  fm_hdr = Utils.decFmURL(st.nextToken()), fm_ico = Utils.decFmURL(st.nextToken()); boolean fm_typed = st.nextToken().toLowerCase().equals("true");
    String  to_hdr = Utils.decFmURL(st.nextToken()), to_ico = Utils.decFmURL(st.nextToken()); boolean to_typed = st.nextToken().toLowerCase().equals("true");
    String  style  = Utils.decFmURL(st.nextToken()); return to_hdr; }

  /**
   * Extract the "from icon" field from an encoded relationship string.
   *
   *@param  rel_str encoded relationship string
   *@return "from icon" flag
   */
  public String  relationshipFromIcon(String rel_str) {
    StringTokenizer st = new StringTokenizer(rel_str, BundlesDT.DELIM);
    String  fm_hdr = Utils.decFmURL(st.nextToken()), fm_ico = Utils.decFmURL(st.nextToken()); boolean fm_typed = st.nextToken().toLowerCase().equals("true");
    String  to_hdr = Utils.decFmURL(st.nextToken()), to_ico = Utils.decFmURL(st.nextToken()); boolean to_typed = st.nextToken().toLowerCase().equals("true");
    String  style  = Utils.decFmURL(st.nextToken()); return fm_ico; }

  /**
   * Extract the "to icon" field from an encoded relationship string.
   *
   *@param  rel_str encoded relationship string
   *@return "to icon" flag
   */
  public String  relationshipToIcon(String rel_str) {
    StringTokenizer st = new StringTokenizer(rel_str, BundlesDT.DELIM);
    String  fm_hdr = Utils.decFmURL(st.nextToken()), fm_ico = Utils.decFmURL(st.nextToken()); boolean fm_typed = st.nextToken().toLowerCase().equals("true");
    String  to_hdr = Utils.decFmURL(st.nextToken()), to_ico = Utils.decFmURL(st.nextToken()); boolean to_typed = st.nextToken().toLowerCase().equals("true");
    String  style  = Utils.decFmURL(st.nextToken()); return to_ico; }

  /**
   * Extract the edge style field from an encoded relationship string.
   *
   *@param  rel_str encoded relationship string
   *@return edge style
   */
  public String  relationshipStyle(String rel_str) {
    StringTokenizer st = new StringTokenizer(rel_str, BundlesDT.DELIM);
    String  fm_hdr = Utils.decFmURL(st.nextToken()), fm_ico = Utils.decFmURL(st.nextToken()); boolean fm_typed = st.nextToken().toLowerCase().equals("true");
    String  to_hdr = Utils.decFmURL(st.nextToken()), to_ico = Utils.decFmURL(st.nextToken()); boolean to_typed = st.nextToken().toLowerCase().equals("true");
    String  style  = Utils.decFmURL(st.nextToken()); return style; }

  /**
   * Extract the "ignore not sets" flag from an encoded relationship string.
   *
   *@param  rel_str encoded relationship string
   *@return "ignore not sets" flag
   */
  public boolean relationshipIgnoreNotSet(String rel_str) {
    StringTokenizer st = new StringTokenizer(rel_str, BundlesDT.DELIM);
    String  fm_hdr = Utils.decFmURL(st.nextToken()), fm_ico = Utils.decFmURL(st.nextToken()); boolean fm_typed  = st.nextToken().toLowerCase().equals("true");
    String  to_hdr = Utils.decFmURL(st.nextToken()), to_ico = Utils.decFmURL(st.nextToken()); boolean to_typed  = st.nextToken().toLowerCase().equals("true");
    String  style  = Utils.decFmURL(st.nextToken());                                          boolean ignore_ns = st.nextToken().toLowerCase().equals("true");
    return ignore_ns; }

  /**
   * Add nodes that show how headers connect with one another.
   *
   *@param include_types if true, include the header types as part of the graph
   */
  public void addHeaderRelationshipsStars(boolean include_types) { 
    Bundles bs = getRTParent().getRootBundles();
    Iterator<Tablet> it_tab = bs.tabletIterator();
    while (it_tab.hasNext()) {
      Tablet tablet   = it_tab.next();
      int    fields[] = tablet.getFields();

      // Add the tablet node
      String tablet_str = (new KeyMaker(tablet, KeyMaker.TABLET_SEP_STR).stringKeys(tablet.bundleIterator().next()))[0];
      if (entity_to_shape.containsKey(tablet_str) == false) entity_to_shape.put(tablet_str, Utils.Symbol.SQUARE); 
      if (entity_to_wxy.containsKey(tablet_str) == false) { entity_to_wxy.put(tablet_str, new Point2D.Double(Math.random()*2 - 1, Math.random()*2 - 1)); transform(tablet_str); }
      graph.addNode(tablet_str); digraph.addNode(tablet_str);

      // Only keep the non-null fields
      List<Integer> al = new ArrayList<Integer>(); for (int i=0;i<fields.length;i++) if (fields[i] != -1) al.add(i);
      fields = new int[al.size()]; for (int i=0;i<fields.length;i++) fields[i] = al.get(i);

      // Go through the valid fields now
      for (int i=0;i<fields.length;i++) {
        int    hdr_i     = fields[i]; String hdr_i_str = bs.getGlobals().fieldHeader(hdr_i);

        // Put the symbol
        if (entity_to_shape.containsKey(hdr_i_str) == false) entity_to_shape.put(hdr_i_str, Utils.Symbol.CIRCLE); 

	// Make the world location
	if (entity_to_wxy.containsKey(hdr_i_str) == false) { entity_to_wxy.put(hdr_i_str, new Point2D.Double(Math.random()*2 - 1, Math.random()*2 - 1)); transform(hdr_i_str); }

	// Figure out the weights
	graph.addNode(hdr_i_str); digraph.addNode(hdr_i_str);
	double g_w  =   graph.getConnectionWeight(graph.getEntityIndex(hdr_i_str), graph.getEntityIndex(tablet_str)),
	       dg_w = digraph.getConnectionWeight(graph.getEntityIndex(hdr_i_str), graph.getEntityIndex(tablet_str));
        if (Double.isInfinite(g_w)) g_w = 0.0; if (Double.isInfinite(dg_w)) dg_w = 0.0;
        // Add the edges
	  graph.addNeighbor(hdr_i_str,  tablet_str, g_w  + tablet.size());
	  graph.addNeighbor(tablet_str, hdr_i_str,  g_w  + tablet.size());
	digraph.addNeighbor(hdr_i_str,  tablet_str, dg_w + tablet.size());
	// Associate the bundles with the edge
	Iterator<Bundle> it_bun = tablet.bundleIterator();
	while (it_bun.hasNext()) {
	  Bundle bundle = it_bun.next();
	    graph.addLinkReference(  graph.getEntityIndex(hdr_i_str),   graph.getEntityIndex(tablet_str), bundle);
	    graph.addLinkReference(  graph.getEntityIndex(tablet_str),  graph.getEntityIndex(hdr_i_str),  bundle);
	  digraph.addLinkReference(digraph.getEntityIndex(hdr_i_str), digraph.getEntityIndex(tablet_str), bundle);
          // Include the data types if specified
	  if (include_types) {
            BundlesDT.DT datatype = BundlesDT.getEntityDataType(bundle.toString(hdr_i));
            // System.err.println("Including Type For \"" + bundle.toString(hdr_i) + "\" ==> " + datatype);
	    if (datatype != null) {
                graph.addNode("" + datatype);  entity_to_shape.put("" + datatype, Utils.Symbol.TRIANGLE);
	      digraph.addNode("" + datatype);  
	      if (entity_to_wxy.containsKey("" + datatype) == false) {
	        entity_to_wxy.put("" + datatype, new Point2D.Double(Math.random()*2 - 1, Math.random()*2 - 1)); 
		transform("" + datatype);
	      }
	      g_w  = graph.getConnectionWeight(graph.getEntityIndex(hdr_i_str), graph.getEntityIndex("" + datatype));
	      dg_w = graph.getConnectionWeight(graph.getEntityIndex(hdr_i_str), graph.getEntityIndex("" + datatype));
              if (Double.isInfinite(g_w)) g_w = 0.0; if (Double.isInfinite(dg_w)) dg_w = 0.0;
	        graph.addNeighbor("" + datatype, hdr_i_str,      g_w + 1);
	        graph.addNeighbor(hdr_i_str,     "" + datatype,  g_w + 1);
	      digraph.addNeighbor(hdr_i_str,     "" + datatype, dg_w + 1);
	        graph.addLinkReference(  graph.getEntityIndex(hdr_i_str),      graph.getEntityIndex("" + datatype), bundle);
	        graph.addLinkReference(  graph.getEntityIndex("" + datatype),  graph.getEntityIndex(hdr_i_str),     bundle);
	      digraph.addLinkReference(digraph.getEntityIndex(hdr_i_str),    digraph.getEntityIndex("" + datatype), bundle);
              digraph.addLinkStyle(digraph.getEntityIndex(hdr_i_str), digraph.getEntityIndex("" + datatype), STYLE_DOTTED_STR);
	    }
	  }
	}
	// Add the style
        digraph.addLinkStyle(digraph.getEntityIndex(hdr_i_str), digraph.getEntityIndex(tablet_str), STYLE_SOLID_STR);
      }
    }
    getRTComponent().render();
  }

  /**
   * Add nodes that show how headers connect with one another.
   *
   *@param include_types if true, include the header types as part of the graph
   */
  public void addHeaderRelationships(boolean include_types) { 
    Bundles bs = getRTParent().getRootBundles();
    Iterator<Tablet> it_tab = bs.tabletIterator();
    while (it_tab.hasNext()) {
      Tablet tablet   = it_tab.next();
      // System.err.println("Tablet \"" + tablet + "\"");
      int    fields[] = tablet.getFields();
      // Only keep the non-null fields
      List<Integer> al = new ArrayList<Integer>(); for (int i=0;i<fields.length;i++) if (fields[i] != -1) al.add(i);
      fields = new int[al.size()]; for (int i=0;i<fields.length;i++) fields[i] = al.get(i);
      // Go through the valid fields now
      for (int i=0;i<fields.length;i++) {
        int    hdr_i     = fields[i];
        int    hdr_j     = fields[(i+1)%fields.length];
	String hdr_i_str = bs.getGlobals().fieldHeader(hdr_i),
	       hdr_j_str = bs.getGlobals().fieldHeader(hdr_j);
        // System.err.println("  \"" + hdr_i_str + "\" => \"" + hdr_j_str + "\"");
        // Put the symbol
        if (entity_to_shape.containsKey(hdr_i_str) == false) entity_to_shape.put(hdr_i_str, Utils.Symbol.CIRCLE); 
        if (entity_to_shape.containsKey(hdr_j_str) == false) entity_to_shape.put(hdr_j_str, Utils.Symbol.CIRCLE); 
	// Make the world location
	if (entity_to_wxy.containsKey(hdr_i_str) == false) { entity_to_wxy.put(hdr_i_str, new Point2D.Double(Math.random()*2 - 1, Math.random()*2 - 1)); transform(hdr_i_str); }
	if (entity_to_wxy.containsKey(hdr_j_str) == false) { entity_to_wxy.put(hdr_j_str, new Point2D.Double(Math.random()*2 - 1, Math.random()*2 - 1)); transform(hdr_j_str); }
	// Figure out the weights
	graph.addNode(hdr_i_str); graph.addNode(hdr_j_str); digraph.addNode(hdr_i_str); digraph.addNode(hdr_j_str);
	double g_w  =   graph.getConnectionWeight(graph.getEntityIndex(hdr_i_str), graph.getEntityIndex(hdr_j_str)),
	       dg_w = digraph.getConnectionWeight(graph.getEntityIndex(hdr_i_str), graph.getEntityIndex(hdr_j_str));
        if (Double.isInfinite(g_w)) g_w = 0.0; if (Double.isInfinite(dg_w)) dg_w = 0.0;
        // Add the edges
	  graph.addNeighbor(hdr_i_str, hdr_j_str, g_w  + tablet.size());
	  graph.addNeighbor(hdr_j_str, hdr_i_str, g_w  + tablet.size());
	digraph.addNeighbor(hdr_i_str, hdr_j_str, dg_w + tablet.size());
	// Associate the bundles with the edge
	Iterator<Bundle> it_bun = tablet.bundleIterator();
	while (it_bun.hasNext()) {
	  Bundle bundle = it_bun.next();
	    graph.addLinkReference(  graph.getEntityIndex(hdr_i_str),   graph.getEntityIndex(hdr_j_str), bundle);
	    graph.addLinkReference(  graph.getEntityIndex(hdr_j_str),   graph.getEntityIndex(hdr_i_str), bundle);
	  digraph.addLinkReference(digraph.getEntityIndex(hdr_i_str), digraph.getEntityIndex(hdr_j_str), bundle);
          // Include the data types if specified
	  if (include_types) {
            BundlesDT.DT datatype = BundlesDT.getEntityDataType(bundle.toString(hdr_i));
            // System.err.println("Including Type For \"" + bundle.toString(hdr_i) + "\" ==> " + datatype);
	    if (datatype != null) {
                graph.addNode("" + datatype);  entity_to_shape.put("" + datatype, Utils.Symbol.TRIANGLE);
	      digraph.addNode("" + datatype);  
	      if (entity_to_wxy.containsKey("" + datatype) == false) {
	        entity_to_wxy.put("" + datatype, new Point2D.Double(Math.random()*2 - 1, Math.random()*2 - 1)); 
		transform("" + datatype);
	      }
	      g_w  = graph.getConnectionWeight(graph.getEntityIndex(hdr_i_str), graph.getEntityIndex("" + datatype));
	      dg_w = graph.getConnectionWeight(graph.getEntityIndex(hdr_i_str), graph.getEntityIndex("" + datatype));
              if (Double.isInfinite(g_w)) g_w = 0.0; if (Double.isInfinite(dg_w)) dg_w = 0.0;
	        graph.addNeighbor("" + datatype, hdr_i_str,      g_w + 1);
	        graph.addNeighbor(hdr_i_str,     "" + datatype,  g_w + 1);
	      digraph.addNeighbor(hdr_i_str,     "" + datatype, dg_w + 1);
	        graph.addLinkReference(  graph.getEntityIndex(hdr_i_str),      graph.getEntityIndex("" + datatype), bundle);
	        graph.addLinkReference(  graph.getEntityIndex("" + datatype),  graph.getEntityIndex(hdr_i_str),     bundle);
	      digraph.addLinkReference(digraph.getEntityIndex(hdr_i_str),    digraph.getEntityIndex("" + datatype), bundle);
              digraph.addLinkStyle(digraph.getEntityIndex(hdr_i_str), digraph.getEntityIndex("" + datatype), STYLE_DOTTED_STR);
	    }
	  }
	}
	// Add the style
        digraph.addLinkStyle(digraph.getEntityIndex(hdr_i_str), digraph.getEntityIndex(hdr_j_str), STYLE_SOLID_STR);
      }
    }
    getRTComponent().render();
  }

  /**
   * Go through the bundles and create both a unidirectional and a directional graph.
   * - For the graphs, keep the relationship of the edges to the bundles.
   *
   *@param encoded_str relationship string (encoded)
   *@param built_in    is the relationship builtin?
   *@param to_add      bundles (records) to add to the graph
   */
  public void addRelationship(String encoded_str, boolean built_in, Bundles to_add) {
    StringTokenizer st = new StringTokenizer(encoded_str, BundlesDT.DELIM);
    String  fm_hdr    = Utils.decFmURL(st.nextToken()),
            fm_ico    = Utils.decFmURL(st.nextToken());
    boolean fm_typed  = st.nextToken().toLowerCase().equals("true");
    String  to_hdr    = Utils.decFmURL(st.nextToken()),
            to_ico    = Utils.decFmURL(st.nextToken());
    boolean to_typed  = st.nextToken().toLowerCase().equals("true");
    String  style     = Utils.decFmURL(st.nextToken());
    boolean ignore_ns = st.nextToken().toLowerCase().equals("true");
    // Determine if relationship is possible with the current data set...  if not return
    String blanks[] = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals());
    boolean to_found = false, fm_found = false;
    for (int i=0;i<blanks.length;i++) {
      if (blanks[i].equals(fm_hdr)) fm_found = true;
      if (blanks[i].equals(to_hdr)) to_found = true;
    }
    if (fm_found && to_found) addRelationship(fm_hdr, fm_ico, fm_typed, to_hdr, to_ico, to_typed, style, ignore_ns, false, to_add);
  }

  /**
   * Add an edge relationship to the graph.
   *
   *@param fm_field       from node field
   *@param fm_symbol_str  symbol for the from nodes
   *@param fm_typed       add type of label on the from nodes
   *@param to_field       to node field
   *@param to_symbol_str  symbol for the to nodes
   *@param to_typed       add type of label on the to nodes
   *@param style_str      style of edge
   *@param ignore_ns      ignore not sets in fields (i.e., don't include them in the graph)
   *@param built_in       relationship description is built-in to the application
   */
  public void addRelationship(String fm_field,  String  fm_symbol_str, boolean fm_typed,
                              String to_field,  String  to_symbol_str, boolean to_typed, 
                              String style_str, boolean ignore_ns,     boolean built_in) {
    addRelationship(fm_field, fm_symbol_str, fm_typed, to_field, to_symbol_str, to_typed, style_str, ignore_ns, built_in, null); }

  /**
   * Add an edge relationship to the graph.
   *
   *@param fm_field       from node field
   *@param fm_symbol_str  symbol for the from nodes
   *@param fm_typed       add type of label on the from nodes
   *@param to_field       to node field
   *@param to_symbol_str  symbol for the to nodes
   *@param to_typed       add type of label on the to nodes
   *@param style_str      style of edge
   *@param ignore_ns      ignore not sets in fields (i.e., don't include them in the graph)
   *@param built_in       relationship description is built-in to the application
   *@param to_add         records (bundles) to add to the graph
   */
  public void addRelationship(String fm_field,  String  fm_symbol_str, boolean fm_typed,
                              String to_field,  String  to_symbol_str, boolean to_typed, 
                              String style_str, boolean ignore_ns,     boolean built_in, Bundles to_add) {
    String  fm_pre  = "", to_pre = "";

    // DEBUG
    // System.err.println("fm_field =\"" + fm_field + "\""); System.err.println("fm_symbol=\"" + fm_symbol_str + "\""); System.err.println("fm_typed =\"" + fm_typed + "\"");
    // System.err.println("to_field =\"" + to_field + "\""); System.err.println("to_symbol=\"" + to_symbol_str + "\""); System.err.println("to_typed =\"" + to_typed + "\"");

    if (fm_typed) fm_pre = fm_field + BundlesDT.DELIM;
    if (to_typed) to_pre = to_field + BundlesDT.DELIM;

    Utils.Symbol fm_symbol = Utils.parseSymbol(fm_symbol_str),
                 to_symbol = Utils.parseSymbol(to_symbol_str);

      // Keep track of existing relationships, update the longer term ones
      String encoded_relationship_str = Utils.encToURL(fm_field)  + BundlesDT.DELIM + Utils.encToURL(fm_symbol_str) + BundlesDT.DELIM + Utils.encToURL("" + fm_typed) + BundlesDT.DELIM +
				        Utils.encToURL(to_field)  + BundlesDT.DELIM + Utils.encToURL(to_symbol_str) + BundlesDT.DELIM + Utils.encToURL("" + to_typed) + BundlesDT.DELIM +
				        Utils.encToURL(style_str) + BundlesDT.DELIM + Utils.encToURL("" + ignore_ns);
      if (active_relationships.contains(encoded_relationship_str) == false) active_relationships.add(encoded_relationship_str);
      if (built_in == false) updateRecentRelationships(encoded_relationship_str);
      // Is this an addition or from scratch?
      Bundles bundles; if (to_add == null) bundles = getRTParent().getRootBundles(); else bundles = to_add;
      BundlesG globals = bundles.getGlobals();
      // Go through the tablets
      Iterator<Tablet> it_tablet = bundles.tabletIterator();
      while (it_tablet.hasNext()) {
        Tablet tablet = it_tablet.next();
	// Check to see if this table will complete both blanks, if so, go through the bundles adding the edges to the graphs
	if (KeyMaker.tabletCompletesBlank(tablet,fm_field) && KeyMaker.tabletCompletesBlank(tablet,to_field)) {
	  // Create the key makers
	  KeyMaker fm_km = new KeyMaker(tablet,fm_field), to_km = new KeyMaker(tablet,to_field);
	  // Go through the bundles
	  Iterator<Bundle> it_bundle = tablet.bundleIterator();
	  while (it_bundle.hasNext()) {
	    Bundle bundle    = it_bundle.next();
	    // Create the combinator for the from and to keys
	    String fm_keys[], to_keys[];
            // Transform the bundle to keys
	    fm_keys = fm_km.stringKeys(bundle);
	    to_keys = to_km.stringKeys(bundle);
	    // Make the relationships
            if (fm_keys != null && fm_keys.length > 0 && to_keys != null && to_keys.length > 0) {
              for (int i=0;i<fm_keys.length;i++) for (int j=0;j<to_keys.length;j++) {
	        // Check for not sets if the flag is specified
                if (ignore_ns && (fm_keys[i].equals(BundlesDT.NOTSET) || to_keys[j].equals(BundlesDT.NOTSET))) continue;
		// The key will be a combination of the header and the entity
                String fm_fin = fm_pre + fm_keys[i], to_fin = to_pre + to_keys[j];
		// If we're in retain mode only, make sure both nodes exist in the set
		if (retained_nodes != null && retained_nodes.size() > 0 && (retained_nodes.contains(fm_fin) == false || retained_nodes.contains(to_fin) == false)) continue;
                // Set the shape
                if (entity_to_shape.containsKey(fm_fin) == false) entity_to_shape.put(fm_fin, fm_symbol);
		if (entity_to_shape.containsKey(to_fin) == false) entity_to_shape.put(to_fin, to_symbol);
                // Create the initial world coordinate and transform as appropriate 
		if (entity_to_wxy.containsKey(fm_fin) == false) { 
		  entity_to_wxy.put(fm_fin, new Point2D.Double(Math.random()*2 - 1, Math.random()*2 - 1));
                  transform(fm_fin); }
		if (entity_to_wxy.containsKey(to_fin) == false) { 
		  entity_to_wxy.put(to_fin, new Point2D.Double(Math.random()*2 - 1, Math.random()*2 - 1));
		  transform(to_fin); }
                // Add the reference back to this object
                graph.addNode(fm_fin);   graph.addNode(to_fin);
                digraph.addNode(fm_fin); digraph.addNode(to_fin);
                // Set the weights equal to the number of bundles on the edge
                double    previous_weight =   graph.getConnectionWeight(  graph.getEntityIndex(fm_fin),   graph.getEntityIndex(to_fin)),
                       di_previous_weight = digraph.getConnectionWeight(digraph.getEntityIndex(fm_fin), digraph.getEntityIndex(to_fin));
                // Check for infinite because the graph class returns infinite if two nodes are not connected
                if (Double.isInfinite(   previous_weight))    previous_weight = 0.0;
                if (Double.isInfinite(di_previous_weight)) di_previous_weight = 0.0;
                // Finally, add them to both forms of the graphs
	  	  graph.addNeighbor(fm_fin, to_fin,    previous_weight + 1.0);
                  graph.addNeighbor(to_fin, fm_fin,    previous_weight + 1.0);
		digraph.addNeighbor(fm_fin, to_fin, di_previous_weight + 1.0);
                // System.err.println("RTGraphPanel.addRelationship() : \"" + fm_fin + "\" => \"" + to_fin + "\": w=" + (previous_weight+1.0) + " | di_w=" + (di_previous_weight+1.0));
		  graph.addLinkReference(  graph.getEntityIndex(fm_fin),   graph.getEntityIndex(to_fin), bundle);
		  graph.addLinkReference(  graph.getEntityIndex(to_fin),   graph.getEntityIndex(fm_fin), bundle);
		digraph.addLinkReference(digraph.getEntityIndex(fm_fin), digraph.getEntityIndex(to_fin), bundle);
                // Keep track of the link style
                digraph.addLinkStyle(digraph.getEntityIndex(fm_fin), digraph.getEntityIndex(to_fin), style_str);
	      }
	    }
          }
	}
      }
    // Nullify the biconnected components
    graph_bcc   = null;
    graph2p_bcc = null;
    cluster_cos = null;
    conductance = null;
    // Re-render
    getRTComponent().render();
  }

  /**
   * Show a dialog to delete a realtionship in the graph
   */
  public void deleteRelationshipDialog() { new DeleteRelationshipDialog(); }

  /**
   * Dialog to allow a user to specify a relationship to remove from the current graph.
   */
  class DeleteRelationshipDialog extends JDialog {
    /**
     * Translates the abbreviated relationship string into the encoded string.
     */
    Map<String,String> relationship_lu = new HashMap<String,String>();

    /**
     *
     */
    JComboBox          relationship_cb;

    /**
     * Construct the dialog and instantiate the listeners.
     */
    public DeleteRelationshipDialog() {
      super(getRTParent(), "Add Relationship...", true);
      getContentPane().setLayout(new BorderLayout(5,5));

      // Make the dropdown box
      Iterator<String> it = active_relationships.iterator(); while (it.hasNext()) {
        String encoded_str     = it.next();
	String abbreviated_str = relationshipFromHeader(encoded_str) + " => " + relationshipToHeader(encoded_str);
	relationship_lu.put(abbreviated_str, encoded_str);
      }
      String strs[] = new String[relationship_lu.keySet().size()];
      it = relationship_lu.keySet().iterator(); for (int i=0;i<strs.length;i++) strs[i] = it.next();

      getContentPane().add("Center", relationship_cb = new JComboBox(strs));

      // Add the buttons
      JPanel  buttons = new JPanel(new FlowLayout());
      JButton bt;
      buttons.add(bt = new JButton("Delete Relationship"));
        bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
          String abbr = (String) relationship_cb.getSelectedItem();
	  // Translate the abbreviate string into the encoded string and remove it from active relationships
          active_relationships.remove(relationship_lu.get(abbr));
          // Apply the active_relationships
          newBundlesRoot(getRTParent().getRootBundles());
	  // Close the dialog and dispose
	  setVisible(false); dispose();
	} } );
      buttons.add(bt = new JButton("Cancel"));
        bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
	  // Close the dialog and dispose
	  setVisible(false); dispose();
	} } );
      getContentPane().add("South", buttons);

      pack(); setVisible(true);
    }
  }

  /**
   * Show a dialog for adding new relationships to the graph.
   */
  public void addRelationshipDialog() { new RelationshipDialog(); }

  /**
   * Dialog for allowing user to specfic a graph edge relationship
   */
  class RelationshipDialog extends JDialog {
    /**
     * 
     */
    private static final long serialVersionUID = 1584278233303146912L;

    /**
     * Choice for the from field
     */
    JComboBox from_cb, 
    /**
     * Choice for the to field
     */
              to_cb, 
    /**
     * Choice for the symbol for the from node
     */
              from_symbol_cb, 
    /**
     * Choice for the symbol for the to node
     */
	      to_symbol_cb,
    /**
     * Choice for the style of the node
     */
              style_cb;
    /**
     * Checkbox to "type" the from nodes
     */
    JCheckBox from_typed_cb, 

    /**
     * Checkbox to "type" the to nodes
     */
              to_typed_cb,

    /**
     * Checkbox to ignore "notset" values in a bundle
     */
	      ignore_ns_cb;

    /**
     * Construct the dialog by laying out the components and attaching listeners.
     */
    public RelationshipDialog() {
      super(getRTParent(), "Add Relationship...", true);
      getContentPane().setLayout(new BorderLayout(5,5));
      JPanel center = new JPanel(new GridLayout(5,2,5,5));
      center.add(new JLabel("From"));    
      center.add(new JLabel("To"));

      String blanks[] = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals());
      center.add(from_cb = new JComboBox(blanks));
      center.add(to_cb   = new JComboBox(blanks));

      center.add(from_symbol_cb = new JComboBox(Utils.SHAPE_STRS));
      center.add(to_symbol_cb   = new JComboBox(Utils.SHAPE_STRS));
      center.add(from_typed_cb  = new JCheckBox("Field Typed",     false));
      center.add(to_typed_cb    = new JCheckBox("Field Typed",     false));
      center.add(ignore_ns_cb   = new JCheckBox("Ignore Not Sets", true));
      getContentPane().add("Center", center);

      getContentPane().add("North",  style_cb = new JComboBox(STYLE_STRS));

      JPanel bottom = new JPanel(new FlowLayout());
      JButton add_bt, cancel_bt;
      bottom.add(add_bt    = new JButton("Add"));
      bottom.add(cancel_bt = new JButton("Cancel"));
      getContentPane().add("South", bottom);

      // Add listeners
      cancel_bt.addActionListener(new ActionListener() { 
        public void actionPerformed(ActionEvent ae) { setVisible(false); dispose(); } } );
      add_bt.addActionListener(new ActionListener() { 
        public void actionPerformed(ActionEvent ae) {
          setVisible(false); dispose();
	  addRelationship((String) from_cb.getSelectedItem(), 
	                  (String) from_symbol_cb.getSelectedItem(),
	  		  from_typed_cb.isSelected(),
	                  (String) to_cb.getSelectedItem(), 
	                  (String) to_symbol_cb.getSelectedItem(),
		  	  to_typed_cb.isSelected(),
                          (String) style_cb.getSelectedItem(), 
			  ignore_ns_cb.isSelected(), 
			  false);
      } } );
      pack(); setVisible(true);
    }
  }

  /**
   * File chooser for saving and loading layouts.
   */
  JFileChooser file_chooser = new JFileChooser(".");

  /**
   * Save the layout of the graph to a file.
   *
   *@param selected_only only save positions for selected nodes
   *@param file          null to show popup, otherwise, use the specified file
   */
  public void saveLayout(boolean selected_only, File save_file) {
    RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) (getRTComponent().getRTRenderContext()); if (myrc == null) return;
    if (save_file != null || file_chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      try {
        Set<String> sel = myrc.filterEntities(getRTParent().getSelectedEntities());
	if (selected_only && (sel == null || sel.size() == 0)) {
	} else {
	  int entities_written = 0;
	  System.out.println("Saving Layout (Selected Only = " + selected_only + ")...");
          if (save_file == null) save_file = file_chooser.getSelectedFile();
          PrintStream out = new PrintStream(new FileOutputStream(save_file));
	  Iterator<String> it = (selected_only) ? (sel.iterator()) : (entity_to_wxy.keySet().iterator());
	  while (it.hasNext()) {
	    String entity = it.next();
            out.println(Utils.encToURL(entity) + "," + Utils.encToURL(""+entity_to_wxy.get(entity).getX()) + "," + Utils.encToURL(""+entity_to_wxy.get(entity).getY()));
	    entities_written++;
	  }
	  out.close();
          System.out.println("  Done Saving Layout! (" + entities_written + " Entities Written)");
        }
      } catch (IOException ioe) {
        JOptionPane.showInternalMessageDialog(this, "Save Layout Error", "IOException : " + ioe, JOptionPane.ERROR_MESSAGE);
	System.err.println("IOException : " + ioe); ioe.printStackTrace(System.err);
      }
    }
  }

  /**
   * Load the graph layout from a file.
   *
   *@param selected_only only apply new positions to the selected entities
   */
  public void loadLayout(boolean selected_only) {
    RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) (getRTComponent().getRTRenderContext()); if (myrc == null) return;
    if (file_chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      try {
        int entities_read = 0, entities_applied = 0;
	Set<String> sel = myrc.filterEntities(getRTParent().getSelectedEntities());
        System.out.println("Loading Layout (Selected Only = " + selected_only + ")...");
        BufferedReader in = new BufferedReader(new FileReader(file_chooser.getSelectedFile()));
        String line = in.readLine();
	while (line != null) {
	  StringTokenizer st     = new StringTokenizer(line,",");
	  String          entity = Utils.decFmURL(st.nextToken());
	  double          wx     = Double.parseDouble(Utils.decFmURL(st.nextToken())),
	                  wy     = Double.parseDouble(Utils.decFmURL(st.nextToken()));
          if (entity_to_wxy.containsKey(entity)) {
	    if (selected_only == false || sel.contains(entity)) {
	      entity_to_wxy.put(entity, new Point2D.Double(wx,wy)); transform(entity);
	      entities_applied++;
	    }
	  }
	  line = in.readLine(); entities_read++;
	}
	in.close();
	System.out.println("  Done Loading Layout! (" + entities_read + " Entities Read, " + entities_applied + " Entities Applied)");
	zoomToFit(); repaint(); 
      } catch (IOException ioe) {
        JOptionPane.showInternalMessageDialog(this, "Save Layout Error", "IOException : " + ioe, JOptionPane.ERROR_MESSAGE);
	System.err.println("IOException : " + ioe); ioe.printStackTrace(System.err);
      }
    }
  }

  /**
   * Toggle the mode for the panel interactions.
   * - 2015-11-06:  only two modes are really ever used - just toggle between those...  the other modes are still in the dropdown
   */
  public void toggleMode() {
    if      (edit_rbmi.isSelected())     filter_rbmi.setSelected(true);
    // else if (filter_rbmi.isSelected())   edgelens_rbmi.setSelected(true);
    // else if (edgelens_rbmi.isSelected()) timeline_rbmi.setSelected(true);
    else                                 edit_rbmi.setSelected(true);
  }

  /**
   * Return the mode for the panel interactions.
   *
   *@return user interaction mode
   */
  public UI_MODE mode() { 
    if      (filter_rbmi.isSelected())   return UI_MODE.FILTER;
    else if (edgelens_rbmi.isSelected()) return UI_MODE.EDGELENS;
    else if (timeline_rbmi.isSelected()) return UI_MODE.TIMELINE;
    else                                 return UI_MODE.EDIT;
  }

  /**
   * Return true if strict substring matches from the clipboard are in effect.
   *
   *@return true for strict matches
   */
  public boolean strictMatches() { return strict_matches_cbmi.isSelected(); }

  /**
   * Set the strict substring matches setting.
   *
   *@param b new setting
   */
  public void strictMatches(boolean b) { strict_matches_cbmi.setSelected(b); }

  /**
   * The current world viewport for the rendering as extents.
   */
  Rectangle2D extents      = new Rectangle2D.Double(-180,-90,360,180); // For geospatial testing

  /**
   * Incremental id to keep the rendering synced with the transformations
   */
  long        transform_id = 1L;

  /**
   * Return the current width of the rendering.
   *
   *@return width
   */
  public int     getMyWidth()            { RTComponent.RTRenderContext myrc = getRTComponent().rc; if (myrc != null) return myrc.getRCWidth();  else return getRTComponent().getWidth();  }

  /**
   * Return the current height of the rendering.
   *
   *@return height
   */
  public int     getMyHeight()           { RTComponent.RTRenderContext myrc = getRTComponent().rc; if (myrc != null) return myrc.getRCHeight(); else return getRTComponent().getHeight(); }

  /**
   * Convert a world x coordinate into a screen x coordiante.
   *
   *@param wx world x coordinate
   *
   *@return screen x coordinate
   */
  public int     wxToSx  (double wx)     { return (int) (getMyWidth()  * (wx - extents.getMinX()) / extents.getWidth());  }

  /**
   * Convert a world y coordinate into a screen y coordiante.
   *
   *@param wy world y coordinate
   *
   *@return screen y coordinate
   */
  public int     wyToSy  (double wy)     { return (int) (getMyHeight() * (wy - extents.getMinY()) / extents.getHeight()); }

  /**
   * Convert a screen x coordinate into a world x coordiante.
   *
   *@param sx screen x coordinate
   *
   *@return world x coordinate
   */
  public double  sxToWx  (int    sx)     { return ((sx * extents.getWidth()) /getMyWidth())  + extents.getMinX(); }

  /**
   * Convert a screen y coordinate into a world y coordiante.
   *
   *@param sy screen y coordinate
   *
   *@return world y coordinate
   */
  public double  syToWy  (int    sy)     { return ((sy * extents.getHeight())/getMyHeight()) + extents.getMinY(); }

  /**
   * Return the correct world increment value for slowly panning the viewport.
   *
   *@return world x increment value
   */
  public double  getWxInc()              { return extents.getWidth()  /getMyWidth();  }

  /**
   * Return the correct world increment value for slowly panning the viewport.
   *
   *@return world y increment value
   */
  public double  getWyInc()              { return extents.getHeight() /getMyHeight(); }

  /**
   * Convert the world coordinates to screen coordinates for the current view for
   * a specific entity (useful when only that entity has moved).
   *
   *@param entity for this specific entity
   */
  public void    transform(String entity) {
    double wx = entity_to_wxy.get(entity).getX(),
           wy = entity_to_wxy.get(entity).getY();
    int    sx, sy;
    entity_to_sxy.put(entity, (sx = wxToSx(wx)) + BundlesDT.DELIM + (sy = wyToSy(wy)));
    entity_to_sx.put(entity, sx); entity_to_sy.put(entity, sy);
    transform_id++;
  }

  /**
   * Convert the world coordinates to screen coordinates for the current view for
   * all entities.
   */
  public synchronized void transform() {
    Iterator<String> it = entity_to_wxy.keySet().iterator();
    while (it.hasNext()) {
      String entity = it.next();
      double wx = entity_to_wxy.get(entity).getX(),
             wy = entity_to_wxy.get(entity).getY();
      int    sx, sy;
      entity_to_sxy.put(entity, (sx = wxToSx(wx)) + BundlesDT.DELIM + (sy = wyToSy(wy)));
      entity_to_sx.put(entity, sx); entity_to_sy.put(entity, sy);
    }
    transform_id++;
  }

  /**
   * Set the new viewport extents, force a transform across all the nodes.
   *
   *@param new_extents new viewport extents
   */
  public void   setExtents(Rectangle2D new_extents) {
    this.extents = new_extents; transform(); getRTComponent().render();
  }

  /**
   * Return the current viewport extents.
   *
   *@return current viewport extents
   */
  public Rectangle2D getExtents() { return this.extents; }

  /**
   * Zoom in by half on the current view.
   */
  public void zoomIn() { zoomIn(1); }   

  /**
   * Zoom in by the desired magnification.
   *
   *@param i magnification
   */
  public void zoomIn(double i)  {
    Rectangle2D r = getExtents();
    double exp = Math.pow(1.5,i); double cx = r.getX() + r.getWidth()/2, cy = r.getY() + r.getHeight()/2;
    setExtents(new Rectangle2D.Double(cx - r.getWidth()/(exp*2), cy - r.getHeight()/(exp*2), r.getWidth()/exp, r.getHeight()/exp));
  }

  /**
   * Zoom in by the desired magnificant leaving the specified coordinate in the same place.
   *
   *@param i      magnification
   *@param ref_cx reference x to keep in the same proportional place
   *@param ref_cy reference y to keep in the same proportional place
   */
  public void zoomIn(double i, double ref_cx, double ref_cy) {
    Rectangle2D r = getExtents();
    if (ref_cx >= r.getMinX() && ref_cx <= r.getMaxX() && ref_cy >= r.getMinY() && ref_cy <= r.getMaxY()) {
      double exp, new_width, new_height;
      if (i > 0.0) { exp = Math.pow(1.5,i);  new_width  = r.getWidth()/exp; new_height = r.getHeight()/exp; }
      else         { exp = Math.pow(1.5,-i); new_width  = r.getWidth()*exp; new_height = r.getHeight()*exp; }
      double x_perc     = (ref_cx - r.getMinX())/(r.getMaxX() - r.getMinX()),
             y_perc     = (ref_cy - r.getMinY())/(r.getMaxY() - r.getMinY());
      double new_xmin   = ref_cx - x_perc*new_width,
             new_ymin   = ref_cy - y_perc*new_height;
      setExtents(new Rectangle2D.Double(new_xmin, new_ymin, new_width, new_height));
    } else zoomIn(i);
  }

  /**
   * Zoom out by half of the current view.
   */
  public void zoomOut() { zoomOut(1); } public void zoomOut(int i) {
    Rectangle2D r = getExtents();
    double exp = Math.pow(1.5,i); double cx = r.getX() + r.getWidth()/2, cy = r.getY() + r.getHeight()/2;
    setExtents(new Rectangle2D.Double(cx - r.getWidth()*exp/2, cy - r.getHeight()*exp/2, r.getWidth()*exp, r.getHeight()*exp));
  }

  /**
   * Zoom to fit all the nodes.
   */
  public void zoomToFit() { zoomToFit(null); }

  /**
   * Zoom to fit all the nodes.
   *
   *@param myrc render context to use for the calculation
   */
  public void zoomToFit(RTGraphComponent.RenderContext myrc) {
    RTGraphComponent.RenderContext original_myrc = myrc;
    // Get the render context
    if (myrc == null) myrc = (RTGraphComponent.RenderContext) (getRTComponent().getRTRenderContext()); if (myrc == null) return;
    // Go through the entities...  may be faster to iterate over visible entities...
    Iterator<String> it = entity_to_wxy.keySet().iterator(); if (it.hasNext() == false) return;
    double x0 = Double.POSITIVE_INFINITY, y0 = Double.POSITIVE_INFINITY,
           x1 = Double.NEGATIVE_INFINITY, y1 = Double.NEGATIVE_INFINITY;
    // Check bounds for each one and adjust mins/maxes as appropriate
    while (it.hasNext()) { 
      String str = it.next(); if (myrc.visible_entities.contains(str) == false) continue;
      if (x0 > entity_to_wxy.get(str).getX()) x0 = entity_to_wxy.get(str).getX();
      if (y0 > entity_to_wxy.get(str).getY()) y0 = entity_to_wxy.get(str).getY();
      if (x1 < entity_to_wxy.get(str).getX()) x1 = entity_to_wxy.get(str).getX();
      if (y1 < entity_to_wxy.get(str).getY()) y1 = entity_to_wxy.get(str).getY();
    }
    // Validate the output
    if (Double.isInfinite(x0) || Double.isInfinite(y0)) return;
    // Give it a border
    if (x1 == x0) x1 = x0 + 0.5; if (y1 == y0) y1 = y0 + 0.5;
    double xp = (x1 - x0)*0.05, yp = (y1 - y0)*0.05;
    // Transform and redraw
    if (getGraphBG() == GraphBG.GEO_OUT || getGraphBG() == GraphBG.GEO_FILL || getGraphBG() == GraphBG.GEO_TOUCH) {
      if (x0 > -180) { x0 = -180; xp = 0.0; }
      if (y0 > -90)  { y0 = -90;  yp = 0.0; }
      if (x1 <  180) { x1 =  180; xp = 0.0; }
      if (y1 <  90)  { y1 =  90;  yp = 0.0; }
    }
    extents = new Rectangle2D.Double(x0-xp,y0-yp,x1-x0+2*xp,y1-y0+2*yp);
    transform(); if (original_myrc == null) getRTComponent().render();
  }

  /**
   * Shift the current selection nodes by the specified amount.  Force a transform on those nodes.
   *
   *@param d_sx amount to shift in x direction
   *@param d_sy amount to shift in y direction
   */
  public void shiftSelection(int d_sx, int d_sy) {
    RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) (getRTComponent().getRTRenderContext()); if (myrc == null) return;
    Set<String> sel = myrc.filterEntities(getRTParent().getSelectedEntities());
    if (sel != null && sel.size() > 0) {
      Iterator<String> it = sel.iterator();
      double x_inc = getWxInc(),
             y_inc = getWyInc();
      while (it.hasNext()) {
        String entity = it.next();
	if (entity_to_wxy.containsKey(entity)) {
          Point2D point = entity_to_wxy.get(entity);
	  entity_to_wxy.put(entity, new Point2D.Double(point.getX() + d_sx * x_inc,
	                                               point.getY() + d_sy * y_inc));
	  transform(entity);
	}
      }
      getRTComponent().render();
    }
  }

    /**
     * Copy the selected item names to the clipboard.
     */
    public void copySelection() {
      RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) (getRTComponent().getRTRenderContext()); if (myrc == null) return;
      Clipboard        clipboard = getToolkit().getSystemClipboard();
      StringBuffer     sb        = new StringBuffer();
      Iterator<String> it        = (myrc.filterEntities(getRTParent().getSelectedEntities())).iterator();
      while (it.hasNext()) sb.append(it.next() + "\n");
      StringSelection selection = new StringSelection(sb.toString());
      clipboard.setContents(selection, null);
    }

    /**
     * Enumeration for set-based operations. REFACTOR
     */
    enum SetOp { SELECT, ADD, REMOVE, INTERSECT };

    /**
     * Select nodes based on the contents of the clipboard.
     */
    public void selectFromClipboard()    { genericClipboard(SetOp.SELECT);    }

    /**
     * Add selected nodes based on the contents of the clipboard.
     */
    public void addFromClipboard()       { genericClipboard(SetOp.ADD);       }

    /**
     * Unselect nodes based on the contents of the clipboard.
     */
    public void removeFromClipboard()    { genericClipboard(SetOp.REMOVE);    }

    /**
     * Intersect the selected nodes based on the contents of the clipboard.
     */
    public void intersectFromClipboard() { genericClipboard(SetOp.INTERSECT); }

    /**
     * Perform a generic set operation of the contents of the clipboard and the
     * currently selected entities.  When the operation is complete, assign the results
     * to the selected entities.
     *
     *@param op set-based operation
     */
    protected void genericClipboard(SetOp op) {
      RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) (getRTComponent().getRTRenderContext()); if (myrc == null) return;
      Clipboard    clipboard = getToolkit().getSystemClipboard();
      Transferable trans     = clipboard.getContents(this);
      if (trans != null && trans.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        try {
	  // Extract the entities
          String        clip = (String) trans.getTransferData(DataFlavor.stringFlavor);
          List<SubText> al   = EntityExtractor.list(clip);

          // Also, extract just the single lines
	  if (strictMatches() == false) {
            StringTokenizer st = new StringTokenizer(clip, "\n");
	    while (st.hasMoreTokens()) {
	      String str = st.nextToken();
	      // Remove before and after whitespaces...
	      while (str.length() > 0 && (str.charAt(0)              == ' ' || str.charAt(0)              == '\t')) 
	        str = str.substring(1,str.length());
	      while (str.length() > 0 && (str.charAt(str.length()-1) == ' ' || str.charAt(str.length()-1) == '\t'))
	        str = str.substring(0,str.length()-1);
              al.add(new Entity(str,str,null,0,0));
	    }
          }

	  // Get the new items
	  Set<String> new_set = new HashSet<String>();
          for (int i=0;i<al.size();i++) { 
	    String           clip_str = al.get(i).toString();
            Iterator<String> it       = entity_to_wxy.keySet().iterator();
            while (it.hasNext()) {
	      String entity = it.next();
	      // Check for a concat
	      if (entity.indexOf(BundlesDT.DELIM) >= 0) entity = entity.substring(entity.lastIndexOf(BundlesDT.DELIM)+1, entity.length());
	      if (Utils.isIPv4CIDR(clip_str) && Utils.isIPv4(entity) && Utils.ipMatchesCIDR(entity, clip_str))
	        new_set.add(entity);
	      if (entity.indexOf(clip_str) >= 0) 
	        new_set.add(entity);
            }
	  }

	  // Get the old items
	  Set<String> old_set = new HashSet<String>(myrc.filterEntities(getRTParent().getSelectedEntities()));

	  // Do the set operation
	  switch (op) {
	    case SELECT:    getRTParent().setSelectedEntities(new_set);
	                    break;
	    case ADD:       new_set.addAll(old_set);
	                    getRTParent().setSelectedEntities(new_set);
			    break;
	    case REMOVE:    old_set.removeAll(new_set);
	                    getRTParent().setSelectedEntities(old_set);
			    break;
	    case INTERSECT: old_set.retainAll(new_set);
	                    getRTParent().setSelectedEntities(old_set);
			    break;
	  }
        } catch (UnsupportedFlavorException ufe) {
          System.err.println("genericClipboard failed...  unsupported data flavor : " + ufe);
          ufe.printStackTrace(System.err);
        } catch (IOException ioe) {
          System.err.println("genericClipboard failed...  io exception : " + ioe);
          ioe.printStackTrace(System.err);
        }
      }
    }

  /**
   * Component to handle the painting of the rendering and interaction with the
   * user to manipulate nodes/views.
   */
  public class RTGraphComponent extends RTComponent {
    /**
     * 
     */
    private static final long serialVersionUID = -2088687525263801720L;

    /**
     * Entities that should always display node labels
     */
    Set<String> sticky_labels = new HashSet<String>();

    /**
     * Flag to overlay help on the screen for keyboard shortcuts
     */
    boolean         draw_help     = false;

    /**
     * Copy the current screen rendering to the clipboard as an image.
     * Does not seem to work across platforms.
     *
     *@param shft shift-key down
     *@param alt  alternate-key down
     */
    @Override
    public void copyToClipboard    (boolean shft, boolean alt) {
      RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) getRTComponent().rc;
      if      (shft == false && alt  == false) copySelection();
      else if (shft == true  && myrc != null)  Utils.copyToClipboard(myrc.getBase());
    }

    /**
     * Select the nodes from the clipboard that match.
     *
     *@param shft shift key down
     *@param alt  alternate key down
     */
    @Override
    public void pasteFromClipboard (boolean shft, boolean alt) {
      if (shft == false && alt == false) selectFromClipboard();
    }

    /**
     * Return the entities bounded by the specified rectangular coordinates.  Useful for appending
     * the entities to comments.
     *
     *@param sx0 lower bounding x coordinate
     *@param sy0 lower bounding y coordinate
     *@param sx1 upper bounding x coordinate
     *@param sy1 upper bounding y coordinate
     *
     *@return list of entities within the specified bounding coordinates
     */
    public String[] getEntitiesAt(int sx0, int sy0, int sx1, int sy1) {
      RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) getRTComponent().rc; if (myrc == null) return null;
      Rectangle2D      rect = new Rectangle2D.Double((sx0 < sx1) ? sx0 : sx1, (sy0 < sy1) ? sy0 : sy1, Math.abs(sx0-sx1), Math.abs(sy0-sy1));
      List<String> strs_as_al = new ArrayList<String>();

      // Go through the entities
      Iterator<String> it = myrc.node_to_geom.keySet().iterator();
      while (it.hasNext()) {
        String node_coord = it.next(); 
	
        if (Utils.genericIntersects(rect, myrc.node_to_geom.get(node_coord))) { 
	  Set<String> entities = myrc.node_coord_set.get(node_coord);
	  Iterator<String> it_entities = entities.iterator();
	  while (it_entities.hasNext()) {
	    String entity = it_entities.next(); 
	    // Entity may be after the delimiter
	    if (entity.indexOf(BundlesDT.DELIM) >= 0) entity = entity.substring(entity.lastIndexOf(BundlesDT.DELIM)+1, entity.length());
	    strs_as_al.add(entity); 
          }
	}
      }
    
      // Transfer it back to a string array
      String strs[] = new String[strs_as_al.size()];
      for (int i=0;i<strs.length;i++) strs[i] = strs_as_al.get(i);

      return strs;
    }

    /**
     * Return the set of all shapes on the screen.
     *
     *@return set of all rendered shapes
     */
    public Set<Shape>      allShapes()                     { 
      RenderContext myrc = (RenderContext) rc;
      if (myrc == null) return new HashSet<Shape>();
      else              return myrc.all_shapes;            
    }

    /**
     * For the set of bundles, return the corresponding set of shapes.
     *
     *@param bundles records to correlate
     *
     *@return set of corresponding shapes
     */
    public Set<Shape>  shapes(Set<Bundle> bundles) { 
      RenderContext  myrc = (RenderContext) rc;
      Set<Shape> set  = new HashSet<Shape>();
      if (myrc == null) return set;
      Iterator<Bundle> it  = bundles.iterator();
      while (it.hasNext()) {
        Bundle bundle = it.next();
        if (myrc.bundle_to_shapes.containsKey(bundle)) set.addAll(myrc.bundle_to_shapes.get(bundle));
      }
      return set;
    }

    /**
     * Calculate the highlighted bundles based on the current position of the mouse.  Needs to be
     * improved to handle neighboring edges for first and second order derivatives.  FEATURE
     *
     *@param me            mouse event
     *@param highlights    records directly under the mouse
     *@param highlights_p  records near the mouse
     *@param highlights_pp records further from the mouse (but still close)
     */
    public void calculateHighlights(MouseEvent me, Set<Bundle> highlights, Set<Bundle> highlights_p, Set<Bundle> highlights_pp) {
      // The problem with this approach is that the screen rep is not equivalent to the graph rep
      // - for example, when multiple nodes are combined into a single node...
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return;
      if (getRTParent().highlight())            { 
        boolean first_order  = getRTParent().highlightFirstOrder(),
	        second_order = getRTParent().highlightSecondOrder();
        Shape shape = graphGeometryAt(me.getX(),me.getY());
        if        (shape == null)           {
        } else if (shape instanceof Line2D) { 
          String          link          = myrc.line_to_link.get(shape);
	  if (link          == null) throw new RuntimeException("Link Null - link_to_link.get()");
          highlights.addAll(myrc.link_counter_context.getBundles(link));

	  if (first_order) {
            System.err.println("RTGraph:  1st Order HL - Not Impl");
	    Set<String> graphedgerefs = myrc.link_to_graphedgerefs.get(link);
	    if (graphedgerefs == null) throw new RuntimeException("GraphEdgeRefs Null");
            Iterator<String> it = graphedgerefs.iterator();
	    while (it.hasNext()) {
	      String graphedgeref = it.next();
	      // abc -
	      // linkrefs.add(graphedgeref_to_link.get(graphedgeref));
	    }
          }
	  if (second_order) { System.err.println("RTGraph:  2nd Order HL - Not Impl");
	  }
	} else {
	  highlights.addAll(myrc.geom_to_bundles.get(shape));
	  if (first_order)  { System.err.println("RTGraph:  1st Order HL - Not Impl"); }
	  if (second_order) { System.err.println("RTGraph:  2nd Order HL - Not Impl"); }
	}
      }
    }

    /**
     * Find the graph geometry at the specified screen location
     * - Give priority to the nodes
     * - If a node doesn't match, check the lines for a closest match.
     *
     *@param  x x coordinate
     *@param  y y coordinate
     *
     *@return most appropriate shape under the mouse position
     */
    public Shape graphGeometryAt(int x, int y) {
      RenderContext myrc = (RenderContext) rc;
      if (myrc == null) return null;
      Iterator<Shape> it_shape = myrc.geom_to_bundles.keySet().iterator();
      while (it_shape.hasNext()) { Shape shape = it_shape.next();
                                   if (shape.contains(x,y)) return shape; }
      if (myrc.line_to_bundles.keySet().size() == 0) return null;
      Iterator<Line2D> it_line = myrc.line_to_bundles.keySet().iterator();
      Line2D closest_line = it_line.next(); double closest_distance = LineIntersection.distanceFromPointToLineSegment(x,y, closest_line);
      while (it_line.hasNext()) {
        Line2D line =it_line.next();
        double dist = LineIntersection.distanceFromPointToLineSegment(x,y, line);
        if (dist < closest_distance) {
          closest_line = line; closest_distance = dist;
        }
      }
      if (closest_distance < 10.0) return closest_line; else return null;
    }

    /**
     * For a specific shape, return the corresponding bundles.
     *
     *@param  shape specified  shape
     *
     *@return set of records corresponding to the specified shape
     */
    public Set<Bundle> shapeBundles(Shape shape)       { 
      RenderContext myrc = (RenderContext) rc;
      if (myrc == null) return new HashSet<Bundle>();
      if      (myrc.line_to_bundles.containsKey(shape)) return myrc.line_to_bundles.get(shape);
      else if (myrc.geom_to_bundles.containsKey(shape)) return myrc.geom_to_bundles.get(shape);
      else return new HashSet<Bundle>(); }

    /**
     * Determine which shapes overlap with the specified shape.  The specified shape
     * can be generic (i.e., it doesn't have to be created as part of the rendering process).
     *
     *@param shape shape to check for overlaps
     *
     *@return set of overlapping, rendered shapes
     */
    public Set<Shape>  overlappingShapes(Shape shape)  { 
      RenderContext myrc = (RenderContext) rc;
      if (myrc == null) return new HashSet<Shape>();
      Set<Shape> set = new HashSet<Shape>();  
      Iterator<Shape> its = myrc.geom_to_bundles.keySet().iterator();
      while (its.hasNext()) {
        Shape its_shape = its.next();
	if (Utils.genericIntersects(shape, its_shape)) set.add(its_shape);
      }
      Iterator<Line2D> itl = myrc.line_to_bundles.keySet().iterator();
      while (itl.hasNext()) {
         Line2D line = itl.next(); 
	 if (line.contains(shape.getBounds())) set.add(line);
      }
      return set;
    }

    /**
     * Flag for rendering edge templates
     */
    boolean edge_template_flag = false;

    /**
     * Return true to draw the edge templates.
     *
     *@return true for edge templates
     */
    public boolean drawEdgeTemplates() {
      return edge_template_flag;
    }

    /**
     * Flag for rendering the node legend
     */
    boolean node_legend_flag = false;

    /**
     * Return true if node legend should be rendered.
     *
     *@return true for node legend
     */
    public boolean drawNodeLegend() {
      return node_legend_flag;
    }

    /**
     * Render the current configurations/bundle set.  Use a render id to abort unnecessary renderings.
     *
     *@param id render id
     */
    public RTRenderContext render(short id) {
      clearNoMappingSet();
      // Don't draw if not visible...
      if (isVisible() == false) { repaint(); return null; }
      // Get the parameters
      Bundles bs = getRenderBundles();
      String count_by = getRTParent().getCountBy(), color_by = getRTParent().getColorBy();
      if (bs != null && count_by != null) {
        RenderContext myrc = new RenderContext(id, bs, extents, getRTParent().getColorBy(), getRTParent().getCountBy(), 
	                                       getNodeColor(), getNodeSize(), getLinkColor(), getLinkSize(), drawArrows(), linksTransparent(), linkCurves(),
                                               drawTiming(), drawNodeLabels(), drawLinkLabels(), listEntityLabels(), listEntityColor(), listBundleLabels(), getGraphBG(),
					       drawEdgeTemplates(), drawNodeLegend(), getWidth(), getHeight(), (RenderContext) rc);
        // Recalculate the bounds if checked
        if (recalc_bounds_cbmi.isSelected()) { 
          zoomToFit(myrc); 
          myrc = new RenderContext(id, bs, extents, getRTParent().getColorBy(), getRTParent().getCountBy(), 
	                           getNodeColor(), getNodeSize(), getLinkColor(), getLinkSize(), drawArrows(), linksTransparent(), linkCurves(),
                                   drawTiming(), drawNodeLabels(), drawLinkLabels(), listEntityLabels(), listEntityColor(), listBundleLabels(), getGraphBG(),
				   drawEdgeTemplates(), drawNodeLegend(), getWidth(), getHeight(), (RenderContext) rc);
        }
        return myrc;
      }
      return null;
    }

    /**
     * Expand the currently selected node by one neighbor.  If the flag is specified, only consider directed edges.
     *
     *@param use_digraph only considered directed neighbors
     */
    public void expandSelection(boolean use_digraph) {
      RenderContext   myrc    = (RenderContext) rc; if (myrc == null) return;
      Set<String>     sel     = myrc.filterEntities(getRTParent().getSelectedEntities()),
                      new_sel = new HashSet<String>();
      MyGraph         to_use  = (use_digraph) ? digraph : graph;
      if (sel != null && sel.size() > 0) {
        Iterator<String> it = sel.iterator();
	while (it.hasNext()) {
	  String entity = it.next();
	  new_sel.add(entity);
	  int entity_i = to_use.getEntityIndex(entity);
	  for (int i=0;i<to_use.getNumberOfNeighbors(entity_i);i++) {
            String nbor = to_use.getEntityDescription(to_use.getNeighbor(entity_i, i));
            if (myrc.visible_entities.contains(nbor)) new_sel.add(nbor);
	  }
	}
      }
      getRTParent().setSelectedEntities(new_sel);
      repaint();
    }

    /**
     * Select the opposite set of nodes from the currently visible nodes.
     */
    public void invertSelection() {
      RenderContext myrc    = (RenderContext) rc; if (myrc == null) return;
      Set<String>   sel     = myrc.filterEntities(getRTParent().getSelectedEntities());
      Set<String>   new_sel = new HashSet<String>();
      if (myrc != null && sel != null) {
        Iterator<String> it = myrc.visible_entities.iterator();
	while (it.hasNext()) {
	  String str = it.next();
	  if (sel.contains(str)) { } else new_sel.add(str);
	}
      }
      getRTParent().setSelectedEntities(new_sel);
      repaint();
    }

    /**
     * Select nodes with a specific degree.  Note that this method uses the underlying graph
     * for the selection -- not the currently displayed number of neighbors.
     *
     *@param degree degree to select
     */
    public void selectNodesByDegree(int degree) {
      Set<String> new_sel = new HashSet<String>();
      for (int i=0;i<graph.getNumberOfEntities();i++) {
        if (graph.getNumberOfNeighbors(i) == degree) new_sel.add(graph.getEntityDescription(i));
      }
      setOperation(new_sel);
    }

    /**
     * Select non-trivial cut vertices.  Useful for arranging the graph by blocks.
     */
    public void selectCutVertices() {
      BiConnectedComponents bcc_2p = graph2p_bcc;
      if (bcc_2p != null) {
        Set<String> new_sel = new HashSet<String>();
	new_sel.addAll(bcc_2p.getCutVertices());
        setOperation(new_sel);
      }
    }

    /**
     * Select nodes with a specific degree or higher.
     *
     *@param degree minmum degree necessary
     */
    public void selectNodesWithDegreeAtLeast(int degree) {
      Set<String> new_sel = new HashSet<String>();
      for (int i=0;i<graph.getNumberOfEntities();i++) {
        if (graph.getNumberOfNeighbors(i) >= degree) new_sel.add(graph.getEntityDescription(i));
      }
      setOperation(new_sel);
    }

    /**
     * Remove the selected nodes (and their associated bundles) from the visible set.
     */
    public void filterOutSelection() {
      RenderContext   myrc        = (RenderContext) rc; if (myrc == null) return;
      Set<String>     sel         = myrc.filterEntities(getRTParent().getSelectedEntities());
      if (sel == null || sel.size() == 0) { getRTParent().pop(); repaint(); return; }
      Set<Bundle> new_bundles = new HashSet<Bundle>();
      if (sel != null && sel.size() > 0) {
        new_bundles.addAll(myrc.bs.bundleSet());
        Iterator<String> it = sel.iterator();
	while (it.hasNext()) new_bundles.removeAll(myrc.entity_counter_context.getBundles(it.next()));
        getRTParent().setSelectedEntities(new HashSet<String>());
	getRTParent().push(myrc.bs.subset(new_bundles));
        repaint();
      }
    }

    /**
     * Scale the current selection by the specified multiple value.  Values greater than one
     * expand the positions while those less than one contract the nodes.  The mouse point is
     * used as the center for the transformation.
     *
     *@param mult multiple value to use for expansion or contraction
     */
    public void scaleSelection(double mult) {
      RenderContext   myrc        = (RenderContext) rc; if (myrc == null) return;
      Set<String>     sel         = myrc.filterEntities(getRTParent().getSelectedEntities());
      if (sel != null && sel.size() > 0) {
        Iterator<String> it = sel.iterator();
	while (it.hasNext()) { 
	  String ent = it.next(); Point2D point = entity_to_wxy.get(ent);
	  double px    = point.getX(), py    = point.getY(),
	         dx    = px - m_wx,    dy    = py - m_wy;
          double dist  = Math.sqrt((px - m_wx)*(px - m_wx) + (py - m_wy)*(py - m_wy));
	  if (dist > 0.0001) {
	    double ndx = dx/dist, ndy = dy/dist;
            double nx  = px + mult * ndx,
	           ny  = py + mult * ndy;
	    entity_to_wxy.put(ent, new Point2D.Double(nx,ny));
	    transform(ent); 
          }
	}
        getRTComponent().render();
      }
    }

    /**
     * Rotate the current selection by the specified angle value.  The mouse point is
     * used as the center for the transformation.
     *
     *@param degrees degrees to rotate the nodes
     */
    public void rotateSelection(double degrees) {
      RenderContext myrc    = (RenderContext) rc; if (myrc == null) return;
      double        radians = (2*Math.PI)/(360/degrees);
      Set<String>   sel     = myrc.filterEntities(getRTParent().getSelectedEntities());
      if (sel != null && sel.size() > 0) {
        Iterator<String> it = sel.iterator();
	while (it.hasNext()) { 
	  String ent = it.next(); Point2D point = entity_to_wxy.get(ent);
	  double px    = point.getX(), py    = point.getY();
          double dist  = Math.sqrt((px - m_wx)*(px - m_wx) + (py - m_wy)*(py - m_wy));
	  double angle = Utils.direction(px - m_wx, py - m_wy);
          double nx    = m_wx + dist * Math.cos(angle + radians),
	         ny    = m_wy + dist * Math.sin(angle + radians);
	  entity_to_wxy.put(ent, new Point2D.Double(nx,ny));
	  transform(ent); 
	}
        getRTComponent().render();
      }
    }

    /**
     * For the selected entities, assign them to the last location of the mouse.
     * force a transform and re-render.
     */
    public void centerSelection() {
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return;
      Set<String>   sel  = myrc.filterEntities(getRTParent().getSelectedEntities());
      if (sel != null && sel.size() > 0) {
        Iterator<String> it = sel.iterator();
	while (it.hasNext()) { 
	  String ent = it.next(); Point2D point = entity_to_wxy.get(ent);
	  if      (last_shft_down) entity_to_wxy.put(ent, new Point2D.Double(point.getX(),m_wy)); 
	  else if (last_ctrl_down) entity_to_wxy.put(ent, new Point2D.Double(m_wx,point.getY()));
	  else                     entity_to_wxy.put(ent, new Point2D.Double(m_wx,m_wy));
	  transform(ent); 
	}
        getRTComponent().render();
      }
    }

    /**
     * Function key saves for the coordinates
     */
    Map<Integer,Map<String,Point2D>> function_key_saves = new HashMap<Integer,Map<String,Point2D>>();

    /**
     * Save the layout based on a function key press.  Shift and control will be used as follows:
     * - shift | control | results
     * - no    | no      | save the layout
     * - yes   | *       | restore the layout
     * - no    | yes     | delete layout copy
     *
     *@param key   key to save/restore the layout from
     *@param shft  indicates layout should be restored
     *@param ctrl  indicates layout should be erased
     */
    protected void saveLayoutByFunctionKey(int fn_key, boolean shft, boolean ctrl) {
      if        (shft && function_key_saves.containsKey(fn_key)) {
        //
	// Restore the save
	//
	System.err.println("RTGraphPanel: Restoring Layout From KeyCode " + fn_key);
	Iterator<String> it = function_key_saves.get(fn_key).keySet().iterator();
	while (it.hasNext()) {
	  String k = it.next();
	  if (entity_to_wxy.keySet().contains(k)) entity_to_wxy.put(k, function_key_saves.get(fn_key).get(k));
	}
        RenderContext myrc = (RenderContext) rc;
        transform(); if (myrc != null) getRTComponent().render();

      } else if (ctrl && function_key_saves.containsKey(fn_key)) { 
        //
	// Remove the previously saved coordinates
	//
	System.err.println("RTGraphPanel: Deleting Layout From KeyCode " + fn_key);
        function_key_saves.remove(fn_key);

      } else                                                  {
        //
	// Save off the coordinates
	//
	System.err.println("RTGraphPanel:  Saving Layout To KeyCode " + fn_key);
        function_key_saves.put(fn_key, new HashMap<String,Point2D>());
	Iterator<String> it = entity_to_wxy.keySet().iterator(); 
	while (it.hasNext()) {
	  String k = it.next();
	  function_key_saves.get(fn_key).put(k, entity_to_wxy.get(k));
	}
      }
    }

  /**
   *
   */
  public void mousePressed    (MouseEvent me) { if (mode() == UI_MODE.FILTER || me.getButton() == MouseEvent.BUTTON3) super.mousePressed(me);  
                                                if (mode() == UI_MODE.EDIT   || ui_inter != UI_INTERACTION.NONE || me.getButton() == MouseEvent.BUTTON2) genericMouse(ME_ENUM.PRESSED,  me); }
  public void mouseReleased   (MouseEvent me) { if (mode() == UI_MODE.FILTER || me.getButton() == MouseEvent.BUTTON3) super.mouseReleased(me); 
                                                if (mode() == UI_MODE.EDIT   || ui_inter != UI_INTERACTION.NONE) genericMouse(ME_ENUM.RELEASED, me); }
  public void mouseClicked    (MouseEvent me) { if (mode() == UI_MODE.FILTER || me.getButton() == MouseEvent.BUTTON3) super.mouseClicked(me);  
                                                if (mode() == UI_MODE.EDIT   || ui_inter != UI_INTERACTION.NONE || me.getButton() == MouseEvent.BUTTON2) genericMouse(ME_ENUM.CLICKED,  me); }
  public void mouseMoved      (MouseEvent me) { if (mode() == UI_MODE.FILTER || mode() == UI_MODE.EDGELENS || mode() == UI_MODE.TIMELINE) super.mouseMoved(me);    
                                                if (mode() == UI_MODE.EDIT)   genericMouse(ME_ENUM.MOVED,    me); }
  public void mouseDragged    (MouseEvent me) { if (mode() == UI_MODE.FILTER || mdrag) super.mouseDragged(me);  
                                                if (mode() == UI_MODE.EDIT   || ui_inter != UI_INTERACTION.NONE) genericMouse(ME_ENUM.DRAGGED,  me); }
  public void mouseExited     (MouseEvent me) { super.mouseExited(me);  }
  public void mouseEntered    (MouseEvent me) { super.mouseEntered(me); grabFocus(); }
  public void keyPressed      (KeyEvent   ke) { super.keyPressed(ke);   
                                                int mover = 1; if (last_shft_down) mover *= 5; if (last_ctrl_down) mover *= 20;
                                                if      (ke.getKeyCode() == KeyEvent.VK_M)       toggleMode();
						else if (ke.getKeyCode() == KeyEvent.VK_E)       expandSelection(last_shft_down);
						else if (ke.getKeyCode() == KeyEvent.VK_Q)       invertSelection();
						else if (ke.getKeyCode() == KeyEvent.VK_X)       filterOutSelection();
						else if (ke.getKeyCode() == KeyEvent.VK_G)       grid_mode = true;
						else if (ke.getKeyCode() == KeyEvent.VK_Y)       line_mode = true;
                                                // else if (ke.getKeyCode() == KeyEvent.VK_Z)       zoom_mode = true;
                                                else if (ke.getKeyCode() == KeyEvent.VK_A)       pan_mode  = true;
                                                else if (ke.getKeyCode() == KeyEvent.VK_F)       zoomToFit();
						else if (ke.getKeyCode() == KeyEvent.VK_C && (last_shft_down == false && last_ctrl_down == false))       circ_mode = true;
						else if (ke.getKeyCode() == KeyEvent.VK_T)             centerSelection();
						else if (ke.getKeyCode() == KeyEvent.VK_OPEN_BRACKET)  scaleSelection(last_shft_down ? -2.0 : (last_ctrl_down ? -4 : -1));
						else if (ke.getKeyCode() == KeyEvent.VK_CLOSE_BRACKET) scaleSelection(last_shft_down ?  2.0 : (last_ctrl_down ?  4 :  1));
						else if (ke.getKeyCode() == KeyEvent.VK_COMMA)         rotateSelection(last_shft_down ? -15 : (last_ctrl_down ? -90 : -1));
						else if (ke.getKeyCode() == KeyEvent.VK_PERIOD)        rotateSelection(last_shft_down ?  15 : (last_ctrl_down ?  90 :  1));
						else if (ke.getKeyCode() == KeyEvent.VK_1)       selectNodesByDegree(1);
						else if (ke.getKeyCode() == KeyEvent.VK_2)       selectNodesByDegree(2);
						else if (ke.getKeyCode() == KeyEvent.VK_3)       selectNodesByDegree(3);
						else if (ke.getKeyCode() == KeyEvent.VK_4)       selectNodesByDegree(4);
						else if (ke.getKeyCode() == KeyEvent.VK_5)       selectNodesByDegree(5);
						else if (ke.getKeyCode() == KeyEvent.VK_6)       selectNodesByDegree(6);
						else if (ke.getKeyCode() == KeyEvent.VK_7)       selectNodesByDegree(7);
						else if (ke.getKeyCode() == KeyEvent.VK_8)       selectNodesByDegree(8);
						else if (ke.getKeyCode() == KeyEvent.VK_9)       selectNodesByDegree(9);
						else if (ke.getKeyCode() == KeyEvent.VK_R)       selectCutVertices();
						else if (ke.getKeyCode() == KeyEvent.VK_S)       setStickyLabels();
						else if (ke.getKeyCode() == KeyEvent.VK_0)       selectNodesWithDegreeAtLeast(10);
                                                else if (ke.getKeyCode() == KeyEvent.VK_V && (last_shft_down == false && last_ctrl_down == false))       nextNodeSize();
                                                else if (ke.getKeyCode() == KeyEvent.VK_L &&
						         last_shft_down)                         { edge_template_flag = !edge_template_flag; render(); }
						else if (ke.getKeyCode() == KeyEvent.VK_L &&
						         last_ctrl_down)                         { node_legend_flag   = !node_legend_flag; render(); }
						else if (ke.getKeyCode() == KeyEvent.VK_L)       toggleLabels();
                                                else if (ke.getKeyCode() == KeyEvent.VK_MINUS && 
						                                last_shft_down)  zoomToFit();
                                                else if (ke.getKeyCode() == KeyEvent.VK_MINUS)   zoomOut();
                                                else if (ke.getKeyCode() == KeyEvent.VK_EQUALS)  zoomIn();
                                                else if (ke.getKeyCode() == KeyEvent.VK_UP)      shiftSelection(0,     -mover);
                                                else if (ke.getKeyCode() == KeyEvent.VK_DOWN)    shiftSelection(0,      mover);
                                                else if (ke.getKeyCode() == KeyEvent.VK_LEFT)    shiftSelection(-mover, 0);
                                                else if (ke.getKeyCode() == KeyEvent.VK_RIGHT)   shiftSelection( mover, 0);
						else if (ke.getKeyCode() == KeyEvent.VK_W)       makeOneHopsVisible(last_shft_down);
						else if (ke.getKeyCode() == KeyEvent.VK_H)       { draw_help = !draw_help; repaint(); }
                                                else if (ke.getKeyCode() == KeyEvent.VK_F2 ||
						         ke.getKeyCode() == KeyEvent.VK_F3 ||
						         ke.getKeyCode() == KeyEvent.VK_F4 ||
						         ke.getKeyCode() == KeyEvent.VK_F5)      { saveLayoutByFunctionKey(ke.getKeyCode(), last_shft_down, last_ctrl_down); }

                                              }
  public void keyReleased     (KeyEvent   ke) { super.keyReleased(ke);  
                                                if      (ke.getKeyCode() == KeyEvent.VK_G) grid_mode = false;
						else if (ke.getKeyCode() == KeyEvent.VK_Y) line_mode = false;
						else if (ke.getKeyCode() == KeyEvent.VK_C) circ_mode = false;
                                                // else if (ke.getKeyCode() == KeyEvent.VK_Z) zoom_mode = false;
                                                else if (ke.getKeyCode() == KeyEvent.VK_A) pan_mode  = false;
                                              }
  public void keyTyped        (KeyEvent   ke) { super.keyTyped(ke);
                                              }
 
  public void mouseWheelMoved (MouseWheelEvent mwe) { 
    zoomIn(-mwe.getWheelRotation(), m_wx, m_wy);
    // int inc = (int) Math.abs(mwe.getWheelRotation());
    // if (mwe.getWheelRotation() < 0) zoomIn(inc*0.2, m_wx, m_wy); else zoomOut(inc);
  }

  /**
   * Method to handle the generic mouse interactions -- by centralizing the interaction, the state of various operations 
   * can be more easily maintained.
   *
   *@param me_enum mouse action
   *@param me      original mouse event
   */
  protected void genericMouse(ME_ENUM me_enum, MouseEvent me) {
// System.err.println("" + me_enum);
    boolean op_in_effect = false;
    if        (ui_inter == UI_INTERACTION.NONE && me_enum == ME_ENUM.PRESSED) {
      op_in_effect = true;
      if         (me.getButton() == MouseEvent.BUTTON2 || pan_mode)   { panOrZoom(me_enum, me); 
      } else if  (me.getButton() == MouseEvent.BUTTON1 && grid_mode)  { gridLayout(me_enum, me);
      } else if  (me.getButton() == MouseEvent.BUTTON1 && line_mode)  { lineLayout(me_enum, me);
      } else if  (me.getButton() == MouseEvent.BUTTON1 && circ_mode)  { circleLayout(me_enum, me);
      } else                                                          { selectOrMove(me_enum,me);
      }
    } else if (ui_inter == UI_INTERACTION.NONE && me_enum == ME_ENUM.CLICKED && me.getButton() == MouseEvent.BUTTON2) { panOrZoom(me_enum,me);    op_in_effect = true;
    } else if (ui_inter == UI_INTERACTION.NONE && me_enum == ME_ENUM.CLICKED && me.getButton() == MouseEvent.BUTTON1) { selectOrMove(me_enum,me); op_in_effect = true;
    } else if (ui_inter == UI_INTERACTION.PANNING)                            { panOrZoom   (me_enum,me); op_in_effect = true;
    } else if (ui_inter == UI_INTERACTION.SELECTING)                          { selectOrMove(me_enum,me); op_in_effect = true;
    } else if (ui_inter == UI_INTERACTION.MOVING)                             { selectOrMove(me_enum,me); op_in_effect = true;
    } else if (ui_inter == UI_INTERACTION.GRID_LAYOUT)                        { gridLayout  (me_enum,me); op_in_effect = true;
    } else if (ui_inter == UI_INTERACTION.LINE_LAYOUT)                        { lineLayout  (me_enum,me); op_in_effect = true;
    } else if (ui_inter == UI_INTERACTION.CIRCLE_LAYOUT)                      { circleLayout(me_enum,me); op_in_effect = true;
    }
    // Capture mouse positions
    m_x = me.getX(); m_y = me.getY(); m_wx = sxToWx(m_x); m_wy = syToWy(m_y);
    // Inform the application of the entity under the mouse
    getRTParent().setEntitiesUnderMouse(underMouse(me));
    // If no operation is in effect, find the closest node and draw the labels
    RenderContext myrc = (RenderContext) rc;
    if (op_in_effect == false && myrc != null && myrc.draw_node_labels == false) {
      String new_dyn_labeler = underMouseSimple(me);
      if (dyn_labeler != null) {
        if (dyn_labeler.equals(new_dyn_labeler) == false) { dyn_labeler = new_dyn_labeler; repaint(); }
      } else if (new_dyn_labeler != null) { dyn_labeler = new_dyn_labeler; repaint(); }
    } else if (dyn_labeler != null) { dyn_labeler = null; repaint(); }
  }
  
  /**
   * Dynamic labeler variable holder
   */
  String dyn_labeler = null;

  /**
   * Initialize the variables used for a mouse drag
   *
   *@param me mouse event
   */
  private void initializeDragVars(MouseEvent me) { m_wx0 = m_wx1 = sxToWx(m_x0 = m_x1 = me.getX()); m_wy0 = m_wy1 = syToWy(m_y0 = m_y1 = me.getY()); }

  /**
   * Update the variables used for a mouse drag
   *
   *@param me mouse event
   */
  private void updateDragVars    (MouseEvent me) { updateDragVars(me,false); }

  /**
   * Update the variables used for a mouse drag
   *
   *@param me           mouse event
   *@param ui_constrain if true, restricts movement to just horizontal or just vertical
   */
  private void updateDragVars    (MouseEvent me, boolean ui_constrain) {
    if        (ui_constrain && last_shft_down) {
      m_wx1 = sxToWx(       m_x1 = me.getX());         
      m_wy1 = m_wy0;
      m_y1  = m_y0;
    } else if (ui_constrain && last_ctrl_down)  {
      m_wx1 = m_wx0;
      m_x1  = m_x0;
      m_wy1 = syToWy(       m_y1 = me.getY()); 
    } else {
      m_wx1 = sxToWx(       m_x1 = me.getX());         
      m_wy1 = syToWy(       m_y1 = me.getY()); 
    }
  }

  /**
   * User interface mode
   */
  UI_INTERACTION  ui_inter = UI_INTERACTION.NONE;
  /**
   * X screen coordinate for beginning of mouse drag
   */
  int             m_x0,  
  /**
   * Y screen coordinate for beginning of mouse drag
   */
                  m_y0, 
  /**
   * X screen coordinate for end of mouse drag
   */
		  m_x1, 
  /**
   * Y screen coordinate for end of mouse drag
   */
		  m_y1, 
  /**
   * Current mouse x screen coordinate
   */
		  m_x, 
  /**
   * Current mouse y screen coordinate
   */
		  m_y; 
  /**
   * X world coordinate for beginning of mouse drag
   */
  double          m_wx0, 
  /**
   * Y world coordinate for beginning of mouse drag
   */
                  m_wy0, 
  /**
   * X world coordinate for end of mouse drag
   */
		  m_wx1, 
  /**
   * Y world coordinate for end of mouse drag
   */
		  m_wy1, 
  /**
   * X world coordinate for mouse current position
   */
		  m_wx, 
  /**
   * Y world coordinate for mouse current position
   */
		  m_wy;
  /**
   * Flag to indicate to layout the nodes in a grid
   */
  boolean         grid_mode = false, 
  /**
   * Flag to indicate to layout the nodes in a line
   */
                  line_mode = false, 
  /**
   * Flag to indicate to layout the nodes in a circle
   */
		  circ_mode = false,
  /**
   *
   */
                  zoom_mode = false,
  /**
   * 
   */
                  pan_mode  = false;
  /**
   * Set of nodes that is currently being moved
   */
  Set<String>    moving_set = null;

  /**
   * Handle the interaction for a grid layout with the mouse.
   *
   *@param me_enum mouse action
   *@param me      mouse event
   */
  public void gridLayout(ME_ENUM me_enum, MouseEvent me) {
    RenderContext myrc = (RenderContext) rc; if (myrc == null) return;
    switch (me_enum) {
      case PRESSED:  initializeDragVars(me); ui_inter = UI_INTERACTION.GRID_LAYOUT; repaint(); break;
      case DRAGGED:  updateDragVars(me);                                    repaint(); break;
      case RELEASED: updateDragVars(me);     ui_inter = UI_INTERACTION.NONE;        
                     Set<String> set = myrc.filterEntities(getRTParent().getSelectedEntities());
                     double dx = m_wx1 - m_wx0,
		            dy = m_wy1 - m_wy0;
		     if (set != null && set.size() > 1) {
                       int    sqrt = (int) Math.sqrt(set.size()), max_x_i = 1, max_y_i = 1;
                       if (dx < 0.0001) dx = 0.0001; if (dy < 0.0001) dy = 0.0001;
		       if        ((dx/dy) > 1.5 || (dy/dx) > 1.5) { // Rectangular
		         double closest_dist = Double.POSITIVE_INFINITY;
                         for (int i=1;i<=sqrt;i++) {
			   int    other = set.size()/i;
			   double ratio = ((double) other)/((double) i);
			   double dist  = Math.abs(ratio - dx/dy);
			   if (dist < closest_dist) {
			     if (dx/dy > 1.0) {
			       max_x_i = (i > other) ? i : other;
			       max_y_i = (i > other) ? other : i;
                             } else           {
			       max_x_i = (i > other) ? other : i;
			       max_y_i = (i > other) ? i : other;
			     }
			   }
			 }
		       } else if ((dy/dx) > 1.5) { // Rectangular

		       } else                    { // Roughly square
		         max_x_i = max_y_i = sqrt;
		       }
		       int x_i = 0, y_i = 0;
		       // Sort the elements
		       List<StrCountSorter> sorter = new ArrayList<StrCountSorter>();
		       Iterator<String> it = set.iterator();
		       while (it.hasNext()) {
		         String entity = it.next();
                         int    total  = (int) myrc.entity_counter_context.total(entity); 
			 sorter.add(new StrCountSorter(entity,total));
		       }
		       Collections.sort(sorter);
		       // Do the  layout
		       for (int i=0;i<sorter.size();i++) {
		         String entity = sorter.get(i).toString();
			 entity_to_wxy.put(entity,new Point2D.Double(m_wx0 + x_i*(dx/max_x_i),
			                                             m_wy0 + y_i*(dy/max_y_i)));
			 transform(entity);
			 x_i++; if (x_i >= max_x_i) { x_i = 0; y_i++; }
		       }
		       getRTComponent().render();
		     } else if (set != null && set.size() == 1) {
		       entity_to_wxy.put(set.iterator().next(), new Point2D.Double(m_wx0 + dx/2, m_wy0 + dy/2));
		       transform(set.iterator().next());
		       getRTComponent().render();
		     }
                     repaint(); break;
	case CLICKED:
		break;
	case MOVED:
		break;
	case WHEEL:
		break;
	default:
		break;
    }
  }

  /**
   * Handle the interactions for a line layout.
   *
   *@param me_enum mouse action
   *@param me      mouse event
   */
  public void lineLayout(ME_ENUM me_enum, MouseEvent me) {
    RenderContext myrc = (RenderContext) rc; if (myrc == null) return;
    switch (me_enum) {
      case PRESSED:  initializeDragVars(me);   ui_inter = UI_INTERACTION.LINE_LAYOUT; repaint(); break;
      case DRAGGED:  updateDragVars(me, true);                                        repaint(); break;
      case RELEASED: updateDragVars(me, true); ui_inter = UI_INTERACTION.NONE;
                     Set<String> set = myrc.filterEntities(getRTParent().getSelectedEntities());
		     if (set != null && set.size() > 0) {
		       // Calculate the line equation
                       double dx = m_wx1 - m_wx0, dy = m_wy1 - m_wy0;
		       double t  = 0.0, inc = 1.0 / (set.size() - 1);
		       // Sort the elements
		       List<StrCountSorter> sorter = new ArrayList<StrCountSorter>();
		       Iterator<String> it = set.iterator();
		       while (it.hasNext()) {
		         String entity = it.next();
                         int    total  = (int) myrc.entity_counter_context.total(entity); 
			 sorter.add(new StrCountSorter(entity,total));
		       }
		       Collections.sort(sorter);
		       // Do the  layout
		       for (int i=0;i<sorter.size();i++) {
		         String entity = sorter.get(i).toString();
		         entity_to_wxy.put(entity, new Point2D.Double(m_wx0 + t*dx, m_wy0 + t*dy));
			 transform(entity);
			 t += inc;
		       }
		       getRTComponent().render();
		     }
		     repaint(); break;
	case CLICKED:
		break;
	case MOVED:
		break;
	case WHEEL:
		break;
	default:
		break;
    }
  }

  /**
   * Handle the interations for a circle layout.
   *
   *@param me_enum mouse action
   *@param me      mouse event
   */
  public void circleLayout(ME_ENUM me_enum, MouseEvent me) {
    RenderContext myrc = (RenderContext) rc; if (myrc == null) return;
    switch (me_enum) {
      case PRESSED:  initializeDragVars(me); ui_inter = UI_INTERACTION.CIRCLE_LAYOUT; repaint(); break;
      case DRAGGED:  updateDragVars(me);                                              repaint(); break;
      case RELEASED: updateDragVars(me);     ui_inter = UI_INTERACTION.NONE;          repaint();
                     double dx = m_wx1 - m_wx0, dy = m_wy1 - m_wy0, radius = Math.sqrt(dx*dx+dy*dy);
		     Set<String> set = myrc.filterEntities(getRTParent().getSelectedEntities());
		     if        (set != null && set.size() == 1) {
		       entity_to_wxy.put(set.iterator().next(), new Point2D.Double(m_wx0 + dx/2, m_wy0 + dy/2));
		       transform(set.iterator().next());
		       getRTComponent().render();
		     } else if (set != null && set.size() >  0) {
		       double angle = 0.0, angle_inc = 2.0 * Math.PI / set.size();
		       // Sort the elements
		       List<StrCountSorter> sorter = new ArrayList<StrCountSorter>();
                       Iterator<String> it = set.iterator();
		       while (it.hasNext()) {
		         String entity = it.next();
                         int    total  = (int) myrc.entity_counter_context.total(entity); 
			 sorter.add(new StrCountSorter(entity,total));
		       }
		       Collections.sort(sorter);
		       // Do the  layout
		       for (int i=0;i<sorter.size();i++) {
		         String entity = sorter.get(i).toString();
                         entity_to_wxy.put(entity, new Point2D.Double(m_wx0 + Math.cos(angle)*radius,
			                                              m_wy0 + Math.sin(angle)*radius));
			 transform(entity);
		         angle += angle_inc;
		       }
		       getRTComponent().render();
		     }
                     break;
	case CLICKED:
		break;
	case MOVED:
		break;
	case WHEEL:
		break;
	default:
		break;
    }
  }

  /**
   * Handle the interactions for a pan/zoom operations.
   *
   *@param me_enum mouse action
   *@param me      mouse event
   */
  public void panOrZoom(ME_ENUM me_enum, MouseEvent me) {
    if (me.getButton() != MouseEvent.BUTTON2 && ui_inter != UI_INTERACTION.PANNING && pan_mode == false) return;
    switch (me_enum) {
      case PRESSED:  initializeDragVars(me);  ui_inter = UI_INTERACTION.PANNING; repaint(); break;
      case DRAGGED:  updateDragVars(me,true);                                    repaint(); break;
      case RELEASED: updateDragVars(me,true); ui_inter = UI_INTERACTION.NONE; 
                     double dx = m_wx1 - m_wx0, dy = m_wy1 - m_wy0;
                     if (dx != 0.0 || dy != 0.0) {
                       Rectangle2D r = getExtents();
                       setExtents(new Rectangle2D.Double(r.getX() - dx, r.getY() - dy, r.getWidth(), r.getHeight()));
                     }
                     break;
      case CLICKED:  zoomToFit(); break;
	case MOVED:
		break;
	case WHEEL:
		break;
	default:
		break;
    }
  }

  /**
   * Handle the interactions for a select entities, move entities operation.
   *
   *@param me_enum mouse action
   *@param me      mouse event
   */
  public void selectOrMove(ME_ENUM me_enum, MouseEvent me) {
    if (me.getButton() != MouseEvent.BUTTON1 && ui_inter != UI_INTERACTION.SELECTING && ui_inter != UI_INTERACTION.MOVING) return;
    RenderContext myrc = (RenderContext) rc;
    if (myrc != null) {
      switch (me_enum) {
        case PRESSED:  Set<String>  sel = myrc.filterEntities(getRTParent().getSelectedEntities());

		       // Figure what's under the mouse
                       Set<String>  under_mouse = underMouse(me);

                       // If what's under the mouse is already selected, then cause it to be moving
		       if      (Utils.overlap(sel,under_mouse))     { ui_inter = UI_INTERACTION.MOVING; 
		                                                      moving_set = sel; }
                       else if (under_mouse.size() > 0 && 
		                (last_shft_down || last_ctrl_down)) { setOperation(under_mouse); }
		       else if (under_mouse.size() > 0)             { ui_inter = UI_INTERACTION.MOVING; 
		                                                      getRTParent().setSelectedEntities(under_mouse);
		                                                       moving_set = under_mouse; }
		       else                                         { ui_inter = UI_INTERACTION.SELECTING; }

		       initializeDragVars(me);

		       repaint();
		       break;
        case DRAGGED:  updateDragVars(me, ui_inter == UI_INTERACTION.MOVING); repaint(); break;
	case RELEASED: updateDragVars(me, ui_inter == UI_INTERACTION.MOVING);
                       boolean no_move = false;
		       if        (ui_inter == UI_INTERACTION.MOVING)    {
                         double dx = m_wx1 - m_wx0, dy = m_wy1 - m_wy0;
                         if (dx != 0 || dy != 0) {
                           Iterator<String> it = moving_set.iterator();
                           while (it.hasNext()) {
                             String  entity  = it.next();
                             Point2D pt      = entity_to_wxy.get(entity);
                             entity_to_wxy.put(entity, new Point2D.Double(pt.getX() + dx, pt.getY() + dy));
                             transform(entity);
                           }
                         getRTComponent().render();
                         } else no_move = true;
		       } 
                       if (ui_inter == UI_INTERACTION.SELECTING || no_move) {
                         int x0 = m_x0 < m_x1 ? m_x0 : m_x1,   y0 = m_y0 < m_y1 ? m_y0 : m_y1,
			     dx = (int) Math.abs(m_x1 - m_x0), dy = (int) Math.abs(m_y1 - m_y0);
			 if (dx == 0) dx = 1; if (dy == 0) dy = 1;
			 Rectangle2D rect = new Rectangle2D.Double(x0,y0,dx,dy);

		         Set<String> new_sel = new HashSet<String>();
		         Iterator<String> it = myrc.node_to_geom.keySet().iterator();
			 while (it.hasNext()) {
			   String node = it.next(); Shape shape = myrc.node_to_geom.get(node);
			   if (rect.intersects(shape.getBounds())) new_sel.addAll(myrc.node_coord_set.get(node));
			 }
                         setOperation(new_sel);
		       }
                       ui_inter = UI_INTERACTION.NONE;
		       repaint(); break;
        case CLICKED:  setOperation(underMouse(me));
                       repaint(); break;
	case MOVED:
		break;
	case WHEEL:
		break;
	default:
		break;
      }
    }
  }

  /**
   * Set the nodes for sticky labels.  Sticky labels show for the set of node irregardless of
   * the draw labels flag.  They are useful for maintaining context in the graph for the key nodes.
   */
  public void setStickyLabels() {
    RenderContext myrc = (RenderContext) rc; if (myrc == null) return;
    Set<String>   sel  = myrc.filterEntities(getRTParent().getSelectedEntities());
    if (sel != null) {
      if        (last_shft_down && last_ctrl_down) { /* Intersect */ sticky_labels.retainAll(sel);
      } else if (last_shft_down)                   { /* Subtract  */ sticky_labels.removeAll(sel);
      } else if (last_ctrl_down)                   { /* Add       */ sticky_labels.addAll(sel);
      } else                                       { /* Set/Clear */ sticky_labels.clear(); sticky_labels.addAll(sel); }
    }
    getRTComponent().render(); repaint();
  }

  /**
   * Based on the keys pressed (ctrl, shft), perform the correct set operation for selected entities.
   *
   *@param sel selection set
   */
  public void setOperation(Set<String> sel) {
    RenderContext myrc = (RenderContext) rc; if (myrc == null) return;
    Set<String> new_sel = new HashSet<String>(), old_sel;
    if        (last_shft_down && last_ctrl_down) { old_sel = myrc.filterEntities(getRTParent().getSelectedEntities());
                                                   old_sel.retainAll(sel);
						   new_sel = old_sel;
    } else if (last_shft_down)                   { old_sel = myrc.filterEntities(getRTParent().getSelectedEntities());
                                                   old_sel.removeAll(sel);
						   new_sel = old_sel;
    } else if (                  last_ctrl_down) { old_sel = myrc.filterEntities(getRTParent().getSelectedEntities());
                                                   old_sel.addAll(sel);
						   new_sel = old_sel;
    } else new_sel = sel;
    getRTParent().setSelectedEntities(new_sel);
  }

  /**
   * Find the entities under the mouse.  Useful for selecting and moving operation.
   *
   *@param  me mouse event
   *
   *@return all entities under the mouse as a set
   */
  Set<String> underMouse(MouseEvent me) {
    RenderContext    myrc        = (RenderContext) rc; if (myrc == null) return new HashSet<String>();
    Rectangle2D      mouse_rect  = new Rectangle2D.Double(me.getX()-1,me.getY()-1,3,3);
    Set<String>      under_mouse = new HashSet<String>();
    Iterator<String> it          = myrc.node_coord_set.keySet().iterator();
    while (it.hasNext()) {
      String node_coord = it.next(); 
      if (myrc.node_to_geom.containsKey(node_coord) && mouse_rect.intersects(myrc.node_to_geom.get(node_coord).getBounds())) {
        under_mouse.addAll(myrc.node_coord_set.get(node_coord));
      }
    }
    if (under_mouse.size() == 0) { // If nothing, check the links
    }
    return under_mouse;
  }

  /**
   * Find the entities under the mouse.  For this version, only nodes are considered and no accumulateion occurs.
   *
   *@param me mouse event
   *
   *@return node_coord for the object under the mouse
   */
  String underMouseSimple(MouseEvent me) {
    RenderContext    myrc        = (RenderContext) rc; if (myrc == null) return null;
    Rectangle2D      mouse_rect  = new Rectangle2D.Double(me.getX()-1,me.getY()-1,3,3);
    Iterator<String> it          = myrc.node_coord_set.keySet().iterator();
    while (it.hasNext()) {
      String node_coord = it.next(); 
      if (myrc.node_to_geom.containsKey(node_coord) && 
          mouse_rect.intersects(myrc.node_to_geom.get(node_coord).getBounds())) return node_coord;
    }
    return null;
  }

  /**
   * Set the color that indicates the current UI mode.
   *
   *@param g2d graphics primitive object
   */
  public void modeColor(Graphics2D g2d) { g2d.setColor(RTColorManager.getColor("label", "default")); }

  /**
   * Paint the component through the super class.  Overlay the current interactions on the screen.
   * Draw dynamic labels, selected entities, highlighted entities, help as specified by user.
   *
   *@param g graphics primitive
   */
  public void paintComponent(Graphics g) {
    // Setup
    Graphics2D g2d = (Graphics2D) g; int txt_h = Utils.txtH(g2d, "0");
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Paint the super
    super.paintComponent(g2d); 

    // Get the render context and draw if it's valid
    RenderContext myrc = (RenderContext) rc;
    if (myrc != null) {
      // Draw the interactions
      drawInteractions(g2d, myrc);

      // Draw the edge lens
      if      (mode() == UI_MODE.EDGELENS) drawEdgeLens(g2d, myrc);
      else if (mode() == UI_MODE.TIMELINE) drawTimeLine(g2d, myrc);

      // Draw the selection
      Set<String> sel = getRTParent().getSelectedEntities(); if (sel == null) sel = new HashSet<String>();
      if (sel.size() > 0 || sticky_labels.size() > 0) { drawSelectedEntities(g2d, myrc, sel, sticky_labels, txt_h);
      } else { String str = "" + mode(); modeColor(g2d); g2d.drawString("" + str, 5, txt_h); }

      // Draw the dynamic labels -- under the mouse for context
      String dyn_labeler_copy = dyn_labeler;
      if (dyn_label_cbmi.isSelected() && 
          dyn_labeler_copy != null    && 
          myrc.node_coord_set.containsKey(dyn_labeler_copy)) drawDynamicLabels(g2d, myrc, dyn_labeler_copy);

      // If graph info mode, provide additional information
      if (myrc.provideGraphInfo()) { drawGraphInfo(g2d, myrc, sel); }

      // Draw the vertex placement heatmap (experimental)
      if (vertex_placement_heatmap_cbmi.isSelected()) {
	// Make sure it's a single selection (and that it's also a vertex);
        if (sel != null && sel.size() == 1) {
	  String sel_str = sel.iterator().next(); if (entity_to_wxy.containsKey(sel_str)) {
	    int hm_w = myrc.getRCWidth()/4, hm_h = myrc.getRCHeight()/4;
	    // Determine if the heatmap will be large enough to provide value
	    if (hm_w >= 50 && hm_h >= 50) {
	      // Determine if the heatmap needs to be recalculated (doesn't consider if the size is still correct... or if the node shifted...
	      if (hm_sel == null || hm_sel.equals(sel_str) == false || hm_bi == null) {
	        hm_bi  = GraphUtils.vertexPlacementHeatmap(new UniGraph(graph), sel_str, entity_to_wxy, hm_w, hm_h);
		hm_sel = sel_str;
              }
              Composite orig_comp = g2d.getComposite(); g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f)); // Partially transparent
	      g2d.drawImage(hm_bi, myrc.getRCWidth() - hm_bi.getWidth(null), myrc.getRCHeight() - hm_bi.getHeight(null), null);
	      g2d.setComposite(orig_comp);
	    }
	  }
	}
      }

      // Draw the highlighted entities
      highlightEntities(g2d, myrc);

      // Draw the excerpts
      drawExcerpts(g2d, myrc);
    }

    // Draw the help chart
    if (draw_help) drawHelp(g2d);
  }

  /**
   * State for the heatmap (so that it doesn't need to be recalculated on every redraw)
   */
  private String        hm_sel = null;
  private BufferedImage hm_bi  = null;

  /**
   * Experimental feature to draw the excerpts from reports within the window.
   *
   *@param g2d  graphics device
   *@param myrc render context -- assumes already checked for null
   */
  private void drawExcerpts(Graphics2D g2d, RenderContext myrc) {
    Rectangle2D bounds     = new Rectangle2D.Double(0,0,getWidth(),getHeight());
    Area        fill_state = new Area();

    Map<String,Set<SubText>> excerpt_map = getRTParent().getExcerptMap();
    Iterator<String> it = excerpt_map.keySet().iterator();
    while (it.hasNext()) {
      String entity     = it.next();
      String node_coord = entity_to_sxy.get(entity);
      Shape  shape      = myrc.node_to_geom.get(node_coord);
      if (shape != null) {
        SubText.renderContextHints(g2d, excerpt_map.get(entity), (int) shape.getBounds().getCenterX(), (int) shape.getBounds().getCenterY(), 
	                           bounds, fill_state);
      }
    }
  }

  /**
   * Experimental feature to draw a timeline with the line (and corresponding records) under
   * the mouse.  Timeline is drawn at the bottom of the view with matching edges stacked ontop
   * of the original edge.
   *
   * Probably need to throttle / bail out if taking too long...
   *
   *@param g2d  graphics device
   *@param myrc render context
   */
  private void drawTimeLine(Graphics2D g2d, RenderContext myrc) {
    if (mouseIn()) {
      double mxc = mx, myc = my; // Save the mouse coords so they are static

      // Find the closest line segment
      Line2D closest_line = null; double closest_line_d = Double.MAX_VALUE;
      Iterator<Line2D> it = myrc.line_to_bundles.keySet().iterator();
      while (it.hasNext()) {
        Line2D line = it.next();
	double d    = line.ptSegDist(mxc,myc);
	if (d < closest_line_d) { closest_line = line; closest_line_d = d; }
      }

      // If we found an edge, create the timeline
      if (closest_line != null && closest_line_d < 20.0) {
	// Darken background
        Composite orig_comp = g2d.getComposite(); 
	g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
	g2d.setColor(RTColorManager.getColor("brush", "dim"));
	g2d.fillRect(0,0,myrc.getRCWidth(),myrc.getRCHeight());
	g2d.setComposite(orig_comp);

        // Create the composites
	Composite trans = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f), full  = g2d.getComposite();

	// Determine the timeline geometry
	int txt_h = Utils.txtH(g2d, "0");
        int tl_x0 = 10, tl_x1 = myrc.w - 10,        tl_w = tl_x1 - tl_x0;
	int             tl_y0 = myrc.h - txt_h - 4, tl_h = 6;

        // Get the time bounds
	long ts0 = getRenderBundles().ts0(), ts1 = getRenderBundles().ts1dur();

	// Give it labels
	String str = Utils.humanReadableDate(ts0); Color fg = RTColorManager.getColor("label", "defaultfg"), bg = RTColorManager.getColor("label", "defaultbg");
	g2d.setColor(RTColorManager.getColor("axis","default")); g2d.drawLine(tl_x0, tl_y0+1, tl_x1, tl_y0+1);
        clearStr(g2d, str, tl_x0, tl_y0 + txt_h+2, fg, bg);
	str = Utils.humanReadableDate(ts1);
        clearStr(g2d, str, tl_x1 - Utils.txtW(g2d,str), tl_y0 + txt_h+2, fg, bg);

        // Highlight the line itself
        g2d.setColor(RTColorManager.getColor("annotate","cursor"));
	drawTimeLineForLine(g2d, myrc, closest_line, full, trans, tl_x0, tl_x1, tl_w, tl_y0, tl_h, ts0, ts1);

	// Create a bitvector representing locations in the timeline...  use minute resolution... unless unreasonable (like decades...)
	long resolution = 60L*1000L;
	int  minutes    = (int) ((ts1 - ts0)/resolution);
	if (minutes > 0 && minutes < 5*365*24*60) {
	  boolean mask[] = new boolean[minutes+1]; // Give it a little extra...
	  fillTimeLineMask(myrc,closest_line,mask,ts0,resolution);

          // Count matches across the rest of the lines...
	  List<LineCountSorter> sorter = new ArrayList<LineCountSorter>();
          it = myrc.line_to_bundles.keySet().iterator();
          while (it.hasNext()) {
            Line2D line = it.next();
	    if (line != closest_line) {
	      int matches = countTimeLineMatches(myrc, line, mask, ts0, resolution);
	      if (matches > 0) { sorter.add(new LineCountSorter(line, matches)); }
	    }
	  }

	  // If we have matches, sort them
          Collections.sort(sorter);

	  // Display the top twenty or so...
	  for (int i=0;i<(sorter.size() > 20 ? 20 : sorter.size());i++) {
	    tl_y0 -= tl_h; g2d.setColor(RTColorManager.getColor(sorter.get(i).getLine().toString()));
	    drawTimeLineForLine(g2d, myrc, sorter.get(i).getLine(), full, trans, tl_x0, tl_x1, tl_w, tl_y0, tl_h, ts0, ts1);
	  }

	  if (sorter.size() > 20) { // Let the user know that the display is abridged...
	    tl_y0 -= tl_h;
	    fg = RTColorManager.getColor("label", "errorfg"); bg = RTColorManager.getColor("label", "errorbg");
	    clearStr(g2d, "Edges Truncated", tl_x0, tl_y0, fg, bg);
	  }
        }
      }
    }
  }

  /**
   * Fill the provided bit-vector mask with the times of the records (bundles) in the specified
   * line.
   *
   *@param myrc       render context
   *@param line       line to reference records for filling bitvector
   *@param mask       pre-allocated bitvector
   *@param ts0        initial timestamp of records
   *@param resolution divisor for time different for calculating bitvector index
   */
  private void fillTimeLineMask(RenderContext myrc, Line2D line, boolean mask[], long ts0, long resolution) {
    Iterator<Bundle> it = myrc.line_to_bundles.get(line).iterator();
    while (it.hasNext()) {
      Bundle bundle = it.next();
      if (bundle.hasTime()) {
        int i0 = (int) ((bundle.ts0() - ts0)/resolution) - 1; if (i0 <  0)           i0 = 0;
	int i1 = (int) ((bundle.ts1() - ts0)/resolution) + 1; if (i1 >= mask.length) i1 = mask.length-1;
	for (int i=i0;i<=i1;i++) mask[i] = true;
      }
    }
  }

  /**
   * Count the number of overlapping bitvector indices with the specified line's records.
   *
   *@param myrc       render context
   *@param line       line to reference records for matching bitvector
   *@param mask       pre-allocated bitvector
   *@param ts0        initial timestamp of records
   *@param resolution divisor for time different for calculating bitvector index
   *
   *@return number of bit positions matching specific line records
   */
  private int countTimeLineMatches(RenderContext myrc, Line2D line, boolean mask[], long ts0, long resolution) {
    int matches = 0;
    Iterator<Bundle> it = myrc.line_to_bundles.get(line).iterator();
    while (it.hasNext()) {
      Bundle bundle = it.next();
      if (bundle.hasTime()) { 
        int i0 = (int) ((bundle.ts0() - ts0)/resolution), i1 = (int) ((bundle.ts1() - ts0)/resolution);
	for (int i=i0;i<=i1;i++) if (mask[i]) matches++;
      }
    }
    return matches;
  }

  /**
   * Render a line (and its corresponding records) on the timeline.
   *
   *@param g2d graphics device
   *@param myrc render context
   *@param line line to render (and corresponding records)
   *@param full full composite (no transparency)
   *@param trans transparent composite
   *@param tl_x0 min x of timeline
   *@param tl_x1 max x of timeline
   *@param tl_w  width of timeline
   *@param tl_y0 base y of timeline
   *@param ts0   minimum timestamp for visible records
   *@param ts1   maximum timestamp for visible records
   */
  private void drawTimeLineForLine(Graphics2D g2d, RenderContext myrc, Line2D line, 
                                   Composite full, Composite trans,
                                   int tl_x0, int tl_x1, int tl_w, int tl_y0, int tl_h, 
                                   long ts0, long ts1) {
        g2d.setComposite(full); 
	// Parametric formula for line
        double dx  = line.getX2() - line.getX1(), dy  = line.getY2() - line.getY1();
	double len = Utils.length(dx,dy);
	if (len < 0.001) len = 0.001; dx = dx/len; dy = dy/len; double pdx = dy, pdy = -dx;
	if (pdy < 0.0)   { pdy = -pdy; pdx = -pdx; } // Always point down
        double gather_x = line.getX1() + dx*len/2 + pdx*40, gather_y = line.getY1() + dy*len/2 + pdy*40;

	// Find the bundles, for this with timestamps, construct the timeline
	Set<Bundle> set = myrc.line_to_bundles.get(line);
	Iterator<Bundle> itb = set.iterator(); double x_sum = 0.0; int x_samples = 0;
        while (itb.hasNext()) {
	  Bundle bundle = itb.next();
	  if (bundle.hasTime()) { x_sum += (int) (tl_x0 + (tl_w*(bundle.ts0() - ts0))/(ts1 - ts0)); x_samples++; }
        }
	if (x_samples == 0) return;
	double x_avg = x_sum/x_samples;
        ColorScale timing_marks_cs = RTColorManager.getTemporalColorScale();
        itb = set.iterator();
	while (itb.hasNext()) { 
	  Bundle bundle = itb.next();

	  if (bundle.hasTime()) {
            double ratio = ((double) (bundle.ts0() - ts0))/((double) (ts1 - ts0));
	    g2d.setColor(timing_marks_cs.at((float) ratio));

	    int xa = (int) (tl_x0 + (tl_w*(bundle.ts0() - ts0))/(ts1 - ts0));
            // g2d.setComposite(full); 
            g2d.draw(line);
	    if        (bundle.hasDuration()) {
              int xb = (int) (tl_x0 + (tl_w*(bundle.ts1() - ts0))/(ts1 - ts0)); 
              double r = (tl_h-2)/2;
              g2d.fill(new Ellipse2D.Double(xa-r,tl_y0-tl_h/2-r,2*r,2*r));
              g2d.fill(new Ellipse2D.Double(xb-r,tl_y0-tl_h/2-r,2*r,2*r));
              if (xa != xb) g2d.fill(new Rectangle2D.Double(xa, tl_y0-tl_h/2-r, xb-xa, 2*r));
	      if (xa != xb) { g2d.drawLine(xa,tl_y0-tl_h,xb,tl_y0); g2d.drawLine(xa,tl_y0,     xb,tl_y0); }
	    }
	    g2d.drawLine(xa,tl_y0-tl_h,xa,tl_y0); // Make it slightly higher at the start
            double x0 = line.getX1() + dx * len * 0.1 + dx * len * 0.8 * ratio,
	           y0 = line.getY1() + dy * len * 0.1 + dy * len * 0.8 * ratio;
	    g2d.draw(new CubicCurve2D.Double(x0, y0, 
                                               x0       + pdx*10, y0       + pdy*10,
                                               gather_x - pdx*10, gather_y - pdy*10,
                                             gather_x, gather_y));
            g2d.draw(new CubicCurve2D.Double(gather_x, gather_y, 
	                                       gather_x + pdx*40, gather_y + pdy*40,
					       x_avg,             tl_y0 - 10*tl_h, 
	                                     x_avg, tl_y0 - 8*tl_h));
            g2d.draw(new CubicCurve2D.Double(x_avg,         tl_y0 - 8*tl_h,
                                               (x_avg+xa)/2,  tl_y0 - 6*tl_h,
                                               xa,            tl_y0 - 2*tl_h,
                                             xa,            tl_y0 - tl_h/2));
          }
	}
      }
  private void drawTimeLineForLineWORKS(Graphics2D g2d, RenderContext myrc, Line2D line, 
                                   Composite full, Composite trans,
                                   int tl_x0, int tl_x1, int tl_w, int tl_y0, int tl_h, 
                                   long ts0, long ts1) {
        g2d.setComposite(full); 
	// Parametric formula for line
        double dx  = line.getX2() - line.getX1(), dy  = line.getY2() - line.getY1();
	double len = Utils.length(dx,dy);
	if (len < 0.001) len = 0.001; dx = dx/len; dy = dy/len; double pdx = dy, pdy = -dx;

        double gather_x = line.getX1() + dx*len/2 + pdx*40, gather_y = line.getY1() + dy*len/2 + pdy*40; // Precalculation for Cubic Approach

	// Find the bundles, for this with timestamps, construct the timeline
	Set<Bundle> set = myrc.line_to_bundles.get(line);
	Iterator<Bundle> itb = set.iterator();

	while (itb.hasNext()) {
	  Bundle bundle = itb.next();

	  if (bundle.hasTime()) {
	    int xa = (int) (tl_x0 + (tl_w*(bundle.ts0() - ts0))/(ts1 - ts0));
            // g2d.setComposite(full); 
            g2d.draw(line);
	    if        (bundle.hasDuration()) {
              int xb = (int) (tl_x0 + (tl_w*(bundle.ts1() - ts0))/(ts1 - ts0)); 
              double r = (tl_h-2)/2;
              g2d.fill(new Ellipse2D.Double(xa-r,tl_y0-tl_h/2-r,2*r,2*r));
              g2d.fill(new Ellipse2D.Double(xb-r,tl_y0-tl_h/2-r,2*r,2*r));
              if (xa != xb) g2d.fill(new Rectangle2D.Double(xa, tl_y0-tl_h/2-r, xb-xa, 2*r));
	      if (xa != xb) { g2d.drawLine(xa,tl_y0-tl_h,xb,tl_y0); g2d.drawLine(xa,tl_y0,     xb,tl_y0); }
	    }
	    g2d.drawLine(xa,tl_y0-tl_h,xa,tl_y0); // Make it slightly higher at the start
            double ratio = ((double) (bundle.ts0() - ts0))/((double) (ts1 - ts0));
            double x0 = line.getX1() + dx * len * 0.1 + dx * len * 0.8 * ratio,
	           y0 = line.getY1() + dy * len * 0.1 + dy * len * 0.8 * ratio;
	    g2d.draw(new CubicCurve2D.Double(x0, y0, gather_x, gather_y, xa, tl_y0 - 20, xa, tl_y0)); // Cubic Approach // Works reasonably
          }
	}
      }

  /**
   * Draw the edge lens interactive technique.  See the following for a description:
   * http://innovis.cpsc.ucalgary.ca/Research/EdgeLens
   *
   *@param g2d  graphics primitive
   *@param myrc current render context
   */
  private void drawEdgeLens(Graphics2D g2d, RenderContext myrc) {
    if (mouseIn()) {
      double           edgelens_dist = 50.0, mxc = mx, myc = my;

      // Figure out which edges to bend
      Set<Line2D>  to_bend       = new HashSet<Line2D>();
      Iterator<Line2D> it            = myrc.line_to_bundles.keySet().iterator();
      while (it.hasNext()) {
        Line2D line = it.next();
        double dist = line.ptSegDist(mxc,myc), dist_v0 = line.getP1().distance(mxc,myc), dist_v1 = line.getP2().distance(mxc,myc);
	if (dist < edgelens_dist && dist < dist_v0 && dist < dist_v1) to_bend.add(line);
      }

      // If there are edges to bend, darken the background and bend the edges
      if (to_bend.size() > 0) {
	// Darken background
        Composite orig_comp = g2d.getComposite(); 
	g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
	g2d.setColor(RTColorManager.getColor("brush", "dim"));
	g2d.fillRect(0,0,myrc.getRCWidth(),myrc.getRCHeight());
	g2d.setComposite(orig_comp);

	// Draw the bended edges
	// - Approximates "EdgeLens:  An Interactive Methd for Managing Edge Congestion in Graphs", Wong, Caprendale, and Greenberg.  2003.
	g2d.setColor(RTColorManager.getColor("linknode", "edgelens"));
        it = to_bend.iterator();
	while (it.hasNext()) {
	  Line2D line = it.next();
	  double dist = line.ptSegDist(mxc,myc);
          double dx   = line.getX2() - line.getX1(),
	         dy   = line.getY2() - line.getY1(),
		 mdx  = mxc          - line.getX1(),
		 mdy  = myc          - line.getY1(),
                 len  = Math.sqrt(dx*dx + dy*dy);
          if (len > 0.001) {
	    dx /= len; dy /= len; 
	    
	    double pdx, pdy;
	    if (Utils.shortestAngle(dx,dy,mdx,mdy) < 0.0) { pdx = -dy; pdy = dx; } else { pdx = dy; pdy = -dx; }

	    double sc_x       = mxc + pdx*dist,   sc_y      = myc + pdy*dist;   // Uses notation from "EdgeLens" paper
	    double disp_sc_x  = mxc + pdx*dist*3, disp_sc_y = myc + pdy*dist*3; // Unsure of this one...  paper cites another paper... math :(
            double n2_dist    = Math.sqrt((sc_x - line.getX1()) * (sc_x - line.getX1()) + (sc_y - line.getY1()) * (sc_y - line.getY1())),
                   n1_dist    = Math.sqrt((sc_x - line.getX2()) * (sc_x - line.getX2()) + (sc_y - line.getY2()) * (sc_y - line.getY2()));
            double c1_x       = disp_sc_x - dx * 0.4 * n1_dist, c1_y       = disp_sc_y - dy * 0.4 * n1_dist,
                   c2_x       = disp_sc_x + dx * 0.4 * n2_dist, c2_y       = disp_sc_y + dy * 0.4 * n2_dist;

	    g2d.draw(new CubicCurve2D.Double(line.getX1(), line.getY1(), c1_x, c1_y, c2_x, c2_y, line.getX2(), line.getY2()));
	  }
	}
      }
    }
  }

  /**
   * Draw the current interactions as the mouse is dragged.
   *
   *@param g2d  graphics primitive
   *@param myrc current render context
   */
  private void drawInteractions(Graphics2D g2d, RenderContext myrc) {
      Composite orig_comp = g2d.getComposite();
      // Draw the interaction
      switch (ui_inter) {
        case PANNING:       g2d.setColor(RTColorManager.getColor("background", "default")); g2d.fillRect(0,0,getWidth(),getHeight());
	                    g2d.drawImage(myrc.getBase(), m_x1 - m_x0, m_y1 - m_y0, null); 
                            g2d.setColor(RTColorManager.getColor("annotate", "cursor")); int cx = getWidth()/2, cy = getHeight()/2; g2d.drawLine(cx-12,cy,cx+12,cy); g2d.drawLine(cx,cy-12,cx,cy+12);
                            break;
        case GRID_LAYOUT:
	case CIRCLE_LAYOUT:
        case SELECTING:     int x0 = m_x0 < m_x1 ? m_x0 : m_x1,   y0 = m_y0 < m_y1 ? m_y0 : m_y1,
			        dx = (int) Math.abs(m_x1 - m_x0), dy = (int) Math.abs(m_y1 - m_y0);
			    if (dx == 0) dx = 1; if (dy == 0) dy = 1;
			    g2d.setColor(RTColorManager.getColor("select", "region"));
			    Shape shape;
			    if (ui_inter == UI_INTERACTION.SELECTING || ui_inter == UI_INTERACTION.GRID_LAYOUT) {
                              shape = new Rectangle2D.Double(x0,y0,dx,dy);
			    } else {
                              double radius = Math.sqrt(dx*dx+dy*dy);
                              shape = new Ellipse2D.Double(m_x0-radius,m_y0-radius,2*radius,2*radius);
                            }
			    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			    g2d.fill(shape);
			    g2d.setComposite(orig_comp);
			    if (ui_inter == UI_INTERACTION.SELECTING) {
                              String str = "Selecting..."; if      (last_shft_down && last_ctrl_down) str += " (Intersect)";
			                                   else if (last_shft_down                  ) str += " (Remove From)";
							   else if (                  last_ctrl_down) str += " (Add To)";
                              g2d.drawString(str, x0, y0); 
                            } else g2d.drawString("Layout...", x0, y0);
			    g2d.draw(shape);
			    break;
        case MOVING:        g2d.drawString("Moving...", 5, getHeight()-5); 
	                    Set<String> moving_set_dup = moving_set;
                            if (moving_set_dup != null && moving_set_dup.size() > 0) {
			      AffineTransform orig = g2d.getTransform(); g2d.setColor(RTColorManager.getColor("linknode", "movenodes"));
			      g2d.translate(m_x1 - m_x0, m_y1 - m_y0);
                              Iterator<String> it = moving_set_dup.iterator();
			      while (it.hasNext()) {
			        String str = it.next(); shape = myrc.node_to_geom.get(entity_to_sxy.get(str));
			        if (shape != null) { g2d.draw(shape); }
			      }
			      g2d.setTransform(orig);
		  	    }
                            break;
	case LINE_LAYOUT:   g2d.setColor(RTColorManager.getColor("linknode", "layout"));
	                    g2d.drawLine(m_x0, m_y0, m_x1, m_y1);
			    break;
	case NONE:
		break;
	default:
		break;
      }
  }

  /**
   * Draw the number of selected entities.  Draw the current mode in text.
   *
   *@param g2d    graphics primitive
   *@param myrc   current render context
   *@param sel    currently selected entities
   *@param sticks sticky label set
   *@param txt_h  text height for current font
   */
  private void drawSelectedEntities(Graphics2D g2d, RenderContext myrc, Set<String> sel, Set<String> sticks, int txt_h) {
	Stroke orig_stroke = g2d.getStroke(); g2d.setStroke(new BasicStroke(3.0f));

	// Draw the selected entities - outline the shape with red and draw labels if appropriate
	Iterator<String> it = sel.iterator();
	while (it.hasNext()) { 
	  String node  = it.next();  String node_coord = entity_to_sxy.get(node);
	  Shape  shape = myrc.node_to_geom.get(node_coord);
	  if (shape != null) {
	    g2d.setColor(RTColorManager.getColor("linknode", "movenodes")); g2d.draw(shape);
	    if (myrc.node_lm != null && sel.size() < 100 && sticks.contains(node) == false) { 
	      // Bound it by 100...  otherwise, it's going to be unreasonable
	      myrc.node_lm.draw(g2d, node_coord, (int) shape.getBounds().getCenterX(), (int) shape.getBounds().getMaxY(), true);
            }
          }
	}

	// Draw the sticky labels
	it = sticks.iterator();
	while (it.hasNext()) {
	  String node  = it.next();  String node_coord = entity_to_sxy.get(node);
	  Shape  shape = myrc.node_to_geom.get(node_coord);
	  if (shape != null && myrc.node_lm != null) {
	    myrc.node_lm.draw(g2d, node_coord, (int) shape.getBounds().getCenterX(), (int) shape.getBounds().getMaxY(), true);
          }
	}
	g2d.setStroke(orig_stroke);

	// Provide information on what's selected
        g2d.setColor(RTColorManager.getColor("label", "default"));
        g2d.drawString("" + sel.size() + " Selected", 5, txt_h);

	int x_off = Utils.txtW(g2d, "" + sel.size() + " Selected");

        // Print the mode string...  doesn't really belong here...
        String str = "| " + mode();
	modeColor(g2d);
        g2d.drawString("" + str, 10 + x_off, txt_h);
  }

  /**
   * Results of dijkstra's single source shortest path algorithm.  Kept for caching purposes.
   */
  DijkstraSingleSourceShortestPath last_ssp; 

  /**
   * Last source for the single source shortest path algorithm.
   */
  String                           last_ssp_source;

  /**
   * Draw information on the nodes related to graph algorithm.  Currently, the shortest path
   * between two selected nodes is shown as well as the nearest three neighbors from the selected
   * node.  Additionally, cut vertices are also depicted.
   *
   *@param g2d   graphics primitive
   *@param myrc  current render context
   *@param sel   current selection set
   */
  private void drawGraphInfo(Graphics2D g2d, RenderContext myrc, Set<String> sel) {
    if        (sel.size() == 1) { // Draw distances to... probably less than 4 to be useful
      // Make sure the selection equals a node in the graph
      String node = sel.iterator().next(); if (entity_to_wxy.containsKey(node) == false) return;

      // Run the algorithm
      DijkstraSingleSourceShortestPath ssp;
      if (last_ssp != null && node.equals(last_ssp_source)) ssp = last_ssp;
      else ssp = new DijkstraSingleSourceShortestPath(graph, graph.getEntityIndex(node));

      // Draw a number next to those numbers that are within 4 edges (arbitrary number...)
      for (int i=0;i<graph.getNumberOfEntities();i++) {
        int dist = (int) ssp.getDistanceTo(i);
        if (dist > 0 && dist <= 4) {
	  Color color;
	  switch (dist) {
	    case 1:  color = RTColorManager.getColor("linknode", "nbor");
	    case 2:  color = RTColorManager.getColor("linknode", "nbor+");
	    case 3:  color = RTColorManager.getColor("linknode", "nbor++");
	    case 4:  
	    default: color = RTColorManager.getColor("linknode", "nbor+++");
	  }
          String entity = graph.getEntityDescription(i);
	  int    x      = entity_to_sx.get(entity),
	         y      = entity_to_sy.get(entity);
          String str    = "" + dist;
          clearStr(g2d, str, x - Utils.txtW(g2d,str)/2, y + Utils.txtH(g2d,str)/2, color, RTColorManager.getColor("label", "defaultbg"));
	}
      }

      // Cache the results so that repaints are faster...  need to worry about tears
      last_ssp = ssp; last_ssp_source = node;
    } else if (sel.size() == 2) { // Draw shortest path between two nodes
      // Make sure the selection equals a node in the graph
      String node0,node1;
      Iterator<String> it = sel.iterator(); node0 = it.next(); node1 = it.next();
      if (entity_to_wxy.containsKey(node0) == false || entity_to_wxy.containsKey(node1) == false) return;

      // Run the algorithm
      DijkstraSingleSourceShortestPath ssp;
      if      (last_ssp != null && last_ssp_source.equals(node0)) { ssp = last_ssp; }
      else if (last_ssp != null && last_ssp_source.equals(node1)) { ssp = last_ssp; String tmp = node0; node0 = node1; node1 = tmp; }
      else ssp = new DijkstraSingleSourceShortestPath(graph, graph.getEntityIndex(node0));

      // Get the path
      int path[] = ssp.getPathTo(graph.getEntityIndex(node1));
      if (path != null && path.length > 1) {
        Stroke orig_stroke = g2d.getStroke(); g2d.setColor(RTColorManager.getColor("annotate", "cursor")); g2d.setStroke(new BasicStroke(3.0f));
        for (int i=0;i<path.length-1;i++) {
          String graph_linkref  = graph.getLinkRef(path[i],path[i+1]),
	         graph_linkref2 = graph.getLinkRef(path[i+1],path[i]);
	  String gui_linkref    = myrc.graphedgeref_to_link.get(graph_linkref),
	         gui_linkref2   = myrc.graphedgeref_to_link.get(graph_linkref2);
	  Line2D line           = myrc.link_to_line.get(gui_linkref),
	         line2          = myrc.link_to_line.get(gui_linkref2);
	  if      (line  != null) g2d.draw(line); 
	  else if (line2 != null) g2d.draw(line2);
	  else System.err.println("No Line Between \"" + graph.getEntityDescription(path[i]) + "\" and \"" + graph.getEntityDescription(path[i+1]) + "\"");
	}
	g2d.setStroke(orig_stroke);
      }

      // Cache the results so that repaints are faster...  need to worry about tears
      last_ssp = ssp; last_ssp_source = node0;
    } else if (sel.size() >= 3) { // Draw common neighbors
    }
  }

  /**
   * Draw dynamic labels for the node under the mouse as well as its neighbors.
   * - 2013-01-14 - added timers to prevent complicated graphs from taking minutes to redraw.
   *
   *@param g2d               graphics primitive
   *@param myrc              current render context
   *@param dyn_labeler_copy  node coordinate information to find geometry
   */
  private void drawDynamicLabels(Graphics2D g2d, RenderContext myrc, String dyn_labeler_copy) {
        long start_time = System.currentTimeMillis();
	// Get the node information for the one directly under the mouse
	String str;
        if (myrc.node_coord_set.get(dyn_labeler_copy).size() == 1) { // It's a single node -- just get the first string
	  str = myrc.node_coord_set.get(dyn_labeler_copy).iterator().next();
	} else {  // It's a group node -- summarize appropriately
	  str = Utils.calculateCIDR(myrc.node_coord_set.get(dyn_labeler_copy)); 
	}
	Point2D pt = myrc.node_coord_lu.get(dyn_labeler_copy);
	int txt_w = Utils.txtW(g2d,str), txt_h = Utils.txtH(g2d,str);
	clearStr(g2d, str, (int) (pt.getX() - txt_w/2), (int) (pt.getY() + 2*txt_h), RTColorManager.getColor("label", "defaultfg"), RTColorManager.getColor("label", "defaultbg"));
	Area already_taken = new Area(new Rectangle2D.Double(pt.getX()-txt_w/2,pt.getY()+txt_h,txt_w,txt_h));

	// Draw the neighbors
        Set<String>  set = myrc.node_coord_set.get(entity_to_sxy.get(str));
	if (set != null) {
	  // First iteration is to make sure the actual geometrical shapes are in the "already_taken" area
          Iterator<String> it  = set.iterator();
	  while (it.hasNext() && (System.currentTimeMillis() - start_time < 1000L)) {
	    String node   = it.next();
            int    node_i = graph.getEntityIndex(node);
	    for (int i=0;i<graph.getNumberOfNeighbors(node_i);i++) {
	      int    nbor_i = graph.getNeighbor(node_i, i);
	      String nbor   = graph.getEntityDescription(nbor_i);
	      if (entity_to_sxy.containsKey(nbor)) { 
                Shape shape = myrc.node_to_geom.get(entity_to_sxy.get(nbor));
		if (shape != null) {
	          g2d.setColor(RTColorManager.getColor("label", "major")); g2d.draw(shape);
	          Rectangle2D  bounds    = shape.getBounds2D(); already_taken.add(new Area(bounds));
		}
	      }
	    }
	  }
	  // Second iteration actually places them
          it  = set.iterator();
	  while (it.hasNext() && (System.currentTimeMillis() - start_time < 1000L)) {
	    String node   = it.next();
            int    node_i = graph.getEntityIndex(node);
	    for (int i=0;i<graph.getNumberOfNeighbors(node_i);i++) {
	      int    nbor_i = graph.getNeighbor(node_i, i);
	      String nbor   = graph.getEntityDescription(nbor_i);
	      if (entity_to_sxy.containsKey(nbor)) { 
                Shape shape = myrc.node_to_geom.get(entity_to_sxy.get(nbor));
		if (shape != null) {
	          g2d.setColor(RTColorManager.getColor("label", "major")); g2d.draw(shape);
	          Rectangle2D  bounds    = shape.getBounds2D();
		  txt_w     = Utils.txtW(g2d,nbor); 
		  txt_h     = Utils.txtH(g2d,nbor);
		  Rectangle2D  nbor_rect = new Rectangle2D.Double(bounds.getCenterX() - txt_w/2, bounds.getMaxY() + 0.1*txt_h, txt_w, txt_h);
		  // Find a good location for the text rectangle -- needs to check for overlap with other rectangles
		  int    iteration = 0; boolean original = true; 
		  double dx        = bounds.getCenterX() - pt.getX(), dy = bounds.getCenterY() - pt.getY(); double len = Math.sqrt(dx*dx+dy*dy);
		  if (len < 0.001) { len = 1.0; dx = 0.0; dy = 1.0; } dx /= len; dy /= len; // Normalize the vector
		  double pdx = -dy, pdy = dx; // Perpendicular vectors
		  while (iteration < 20 && already_taken.intersects(nbor_rect)) { // search for a better location
		    double distance = 15 * iteration / 3, perp_distance = 10 * ((iteration % 3) - 1);
		    nbor_rect = new Rectangle2D.Double(bounds.getCenterX() -     txt_w/2 + dx*distance + pdx*perp_distance, 
		                                       bounds.getMaxY()    + 0.1*txt_h   + dy*distance + pdy*perp_distance, 
						       txt_w, txt_h);
                    iteration++; original = false;
                  }
		  if (original == false) g2d.drawLine((int) nbor_rect.getCenterX(), (int) nbor_rect.getCenterY(), (int) bounds.getCenterX(), (int) bounds.getCenterY());
	          clearStr(g2d, nbor, (int) nbor_rect.getMinX(), (int) nbor_rect.getMaxY(), RTColorManager.getColor("label", "defaultfg"), RTColorManager.getColor("label", "defaultbg"));
		  already_taken.add(new Area(nbor_rect));
	        }
              }
	    }
	  }
	}
      }

  /**
   * Render a cheat sheet of all of the shortcut commands and their variations.
   *
   *@param g2d graphics primitive
   */
  private void drawHelp(Graphics2D g2d) {
    Composite orig_comp = g2d.getComposite();
    g2d.setColor(RTColorManager.getColor("brush", "dim")); g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f)); g2d.fillRect(0,0,getWidth(),getHeight()); g2d.setComposite(orig_comp);
    int base_x = 5, base_y = Utils.txtH(g2d,"0");
    base_y = drawKeyCombo(g2d, base_x, base_y, "Key",         "Normal",                  "Shift",                  "Ctrl",   "Ctrl-Shift");
    base_y = drawKeyCombo(g2d, base_x, base_y, "E",           "Expand Selection",        "Use Directed Graph");
    base_y = drawKeyCombo(g2d, base_x, base_y, "1 - 9",       "Select Degree",           "Subtract",               "Add",    "Intersect");
    base_y = drawKeyCombo(g2d, base_x, base_y, "0",           "Select Degree > 10",      "Subtract",               "Add",    "Intersect");
    base_y = drawKeyCombo(g2d, base_x, base_y, "R",           "Select Cut Vertices",     "Subtract",               "Add",    "Intersect");
    base_y = drawKeyCombo(g2d, base_x, base_y, "Q",           "Invert Selection");
    base_y = drawKeyCombo(g2d, base_x, base_y, "X",           "Hide Selection");
    base_y = drawKeyCombo(g2d, base_x, base_y, "M",           "Toggle Mode");
    base_y = drawKeyCombo(g2d, base_x, base_y, "G",           "Grid Layout Mode");
    base_y = drawKeyCombo(g2d, base_x, base_y, "Y",           "Line Layout Mode");
    base_y = drawKeyCombo(g2d, base_x, base_y, "C",           "Circle Layout Mode");
    base_y = drawKeyCombo(g2d, base_x, base_y, "T",           "Group Nodes",             "Align Horizontally",    "Align Vertically",    "");
    base_y = drawKeyCombo(g2d, base_x, base_y, "S",           "Set/Clear Sticky Labels", "Subtract From",         "Add To",             "Intersect With");
    base_y = drawKeyCombo(g2d, base_x, base_y, "V",           "Toggle Node Size");
    base_y = drawKeyCombo(g2d, base_x, base_y, "L",           "Toggle Labels",           "Toggle Edge Templates", "Toggle Node Legend", "");
    base_y = drawKeyCombo(g2d, base_x, base_y, "W",           "Make One Hops Visible",   "Use Directed Graph");
    base_y = drawKeyCombo(g2d, base_x, base_y, "N",           "Add Note");
    base_y = drawKeyCombo(g2d, base_x, base_y, "A",           "Pan Mode");
    base_y = drawKeyCombo(g2d, base_x, base_y, "F",           "Fit");
    base_y = drawKeyCombo(g2d, base_x, base_y, "Minus (-)",   "Zoom Out",                "Fit");
    base_y = drawKeyCombo(g2d, base_x, base_y, "Plus (+)",    "Zoom In");
    base_y = drawKeyCombo(g2d, base_x, base_y, "Cursor Keys", "Shift Selected Nodes");
    base_y = drawKeyCombo(g2d, base_x, base_y, "< >",         "Rotate Selected Nodes", "15 Degs", "90 Degs",  "");
    base_y = drawKeyCombo(g2d, base_x, base_y, "{ }",         "Scale Selected Nodes",  "More",    "Even More", "");
    base_y = drawKeyCombo(g2d, base_x, base_y, "F[2-5]",      "Save Layout To Fx",     "Restore", "Delete",    "Restore");
  }

  /**
   * For the help cheat sheet, draw the variations of commands (shift, alt, control) in different colors.
   *
   *@param g2d       graphics primitive
   *@param base_x    x position for string
   *@param base_y    y position for string
   *@param key       normal action
   *@param shft      shift action
   *@param ctrl      control action
   *@param ctrl_shft control-shift action
   *
   *@return new base y position
   */
  private int drawKeyCombo(Graphics2D g2d, int base_x, int base_y, String key, String norm, String shft, String ctrl, String ctrl_shft) {
    if (key       != null) { g2d.setColor(RTColorManager.getColor("label", "default")); g2d.drawString(key,       base_x, base_y); base_x += 100; /* Utils.txtW(g2d,key); */       }
    if (norm      != null) { g2d.setColor(RTColorManager.getColor("label", "major"));   g2d.drawString(norm,      base_x, base_y); base_x += 150; /* Utils.txtW(g2d,norm); */      }
    if (shft      != null) { g2d.setColor(RTColorManager.getColor("label", "minor"));     g2d.drawString(shft,      base_x, base_y); base_x += 150; /* Utils.txtW(g2d,shft); */      }
    if (ctrl      != null) { g2d.setColor(RTColorManager.getColor("label", "minor"));    g2d.drawString(ctrl,      base_x, base_y); base_x += 150; /* Utils.txtW(g2d,ctrl); */      }
    if (ctrl_shft != null) { g2d.setColor(RTColorManager.getColor("label", "minor"));    g2d.drawString(ctrl_shft, base_x, base_y); base_x += 150; /* Utils.txtW(g2d,ctrl_shft); */ }
    return base_y + Utils.txtH(g2d, "0");
  }
  private int drawKeyCombo(Graphics2D g2d, int base_x, int base_y, String key, String norm) {
    return drawKeyCombo(g2d, base_x, base_y, key, norm, null, null, null); }
  private int drawKeyCombo(Graphics2D g2d, int base_x, int base_y, String key, String norm, String shft) {
    return drawKeyCombo(g2d, base_x, base_y, key, norm, shft, null, null); }

  /**
   * Choose the next node size for rendering.
   */
  public void nextNodeSize() { 
    String str = findSelected(node_sizes); int i;
    for (i=0;i<NODE_SZ_STRS.length;i++) if (str.equals(NODE_SZ_STRS[i])) break;
    i = (i+1)%NODE_SZ_STRS.length;
    node_sizes[i].setSelected(true);
  }

  /**
   * Render contexts produce the visualization and maintain the state for the current rendering.
   * They are generated when the visualization changes and are used to correlate bundles (records)
   * with rendered geometry.
   */
    public class RenderContext extends RTRenderContext {
      /**
       * Data set to render
       */
      Bundles      bs; 
      /**
       * Width of rendering (in pixels)
       */
      int          w, 
      /**
       * Height of rendering (in pixels)
       */
                   h; 
      /**
       * Current view port extents
       */
      Rectangle2D  ext; 
      /**
       * Global header for coloring the visualization
       */
      String       color_by, 
      /**
       * Global header for counting objects within the visualization
       */
                   count_by; 
      /**
       * Node color setting
       */
      NodeColor    node_color; 
      /**
       * Node size setting
       */
      NodeSize     node_size; 
      /**
       * Link color setting
       */
      LinkColor    link_color; 
      /**
       * Link size setting
       */
      LinkSize     link_size;
      /**
       * Graph background image option
       */
      GraphBG      graph_bg;
      /**
       * Flag to draw link directions
       */
      boolean      arrows, 
      /**
       * Flag to draw curves
       */
                   curves,
      /**
       * Flag to enable link transparency
       */
                   link_trans, 
      /**
       * Flag to enable timing marks
       */
		   timing, 
      /**
       * Flag to enable node labels
       */
		   draw_node_labels,
      /**
       * Flag to enable link labels
       */
                   draw_link_labels;
      /**
       * Node labels to render
       */
      java.util.List<String> entity_labels, 
      /**
       * For labeling by color, list of which strings to use.  Only uses the first one.
       */
                             entity_color, 
      /**
       * Edge labels to render
       */
                             bundle_labels;

      /**
       * Method to accumulate values for links.
       * - link_counter_context: link=string_based rep "x0|y0|x1|y1"
       */
      BundlesCounterContext link_counter_context, 
      /**
       * Method to accumulate values for nodes.
       * - node_counter_context:   node=string_based rep "x|y"
       */
                            node_counter_context,
      /**
       * Method to accumulate values for entities.
       * - entity_counter_context: entity=string_based rep
       */
			    entity_counter_context;

      /**
       * Lookup from screen coordinates to the underlying entities.
       * - node_coord_set:         node="x|y" to the entity set ("ip", "ip", ...)
       */
      Map<String,Set<String>> node_coord_set = new HashMap<String,Set<String>>();

      /**
       * Lookup from the screen version of coordinates to the double version.
       * - node_coord_lu:          node="x|y" to the point on the screen
       */
      Map<String,Point2D>         node_coord_lu  = new HashMap<String,Point2D>();

      /**
       * Set of all rendered shapes.
       */
      Set<Shape>                   all_shapes            = new HashSet<Shape>();

      /**
       * Set of all visible entities within the current rendering.
       */
      Set<String>                  visible_entities      = new HashSet<String>();

      /**
       * Map from the line shape to the underlying bundles.
       */
      Map<Line2D, Set<Bundle>> line_to_bundles       = new HashMap<Line2D, Set<Bundle>>();

      /**
       * Map from the node shape to the underyling bundles.
       */
      Map<Shape,  Set<Bundle>> geom_to_bundles       = new HashMap<Shape,  Set<Bundle>>();

      /**
       * Map from the bundles (application records) to the geometrical shapes.
       */
      Map<Bundle, Set<Shape>>  bundle_to_shapes      = new HashMap<Bundle, Set<Shape>>();

      /**
       * Map from the string version of the screen coordinates to the geometry.
       * - node_to_geom:           node="x|y" to the geometry on the screen
       */
      Map<String, Shape>           node_to_geom          = new HashMap<String, Shape>();

      /**
       * Map from the link (string rep of a line) to the geometrical line.
       * - link_to_line:           link "x0|y0|x1|y1" to the actual screen line geometry
       */
      Map<String, Line2D>          link_to_line          = new HashMap<String, Line2D>();

      /**
       * Map from the geometrical line back to the string rep of the line (link)
       */
      Map<Line2D, String>          line_to_link          = new HashMap<Line2D, String>();

      /**
       * Map from the string based line (link) to the {@link MyGraph} edge references.
       * - link_to_graphedgerefs   link "x0|y0|x1|y1" to the graph edges references
       */
      Map<String, Set<String>> link_to_graphedgerefs = new HashMap<String, Set<String>>();

      /**
       * Map from the {@link MyGraph} edge references back to the string based line (link)
       * - graphedgerefs_to_link   graph edge references to the link "x0|y0|x1|y1"
       */
      Map<String, String>          graphedgeref_to_link  = new HashMap<String, String>();

      /**
       * Map from the string based link to the edge styles
       */
      Map<String, Set<String>> line_ref_to_styles    = new HashMap<String, Set<String>>();

      /**
       * Flag to render the active edge templates
       */
      boolean draw_edge_templates = false;

      /**
       * Flag to render the node legend
       */
      boolean draw_node_legend    = false;

      /**
       * Construct the render context based on the specified parameters.
       *
       *@param bs            Data set to render
       *@param w             Width of rendering (in pixels)
       *@param h             Height of rendering (in pixels)
       *@param ext           Current view port extents
       *@param color_by      Global header for coloring the visualization
       *@param count_by      Global header for counting objects within the visualization
       *@param node_color    Node color setting
       *@param node_size     Node size setting
       *@param link_color    Link color setting
       *@param link_size     Link size setting
       *@param graph_bg      Graph background image option
       *@param arrows        Flag to draw link directions
       *@param link_trans    Flag to enable link transparency
       *@param curves        Flag to enable link curves
       *@param timing        Flag to enable timing marks
       *@param draw_node_labels   Flag to enable node labels
       *@param draw_link_labels   Flag to enable link labels
       *@param entity_labels Node labels to render
       *@param entity_color  For labeling by color, list of which strings to use.  Only uses the first one.
       *@param bundles_label Edge labels to render
       */
      public RenderContext(short id, Bundles bs, Rectangle2D ext, String color_by, String count_by,
                           NodeColor node_color, NodeSize node_size, LinkColor link_color, LinkSize link_size,
			   boolean arrows, boolean link_trans, boolean curves, boolean timing, 
			   boolean draw_node_labels, boolean draw_link_labels, 
			   java.util.List<String> entity_labels, java.util.List<String> entity_color, java.util.List<String> bundle_labels, GraphBG graph_bg,
			   boolean draw_edge_templates, boolean draw_node_legend,
                           int w, int h, RenderContext last_rc) {
        timer_a = System.currentTimeMillis();
        render_id = id; this.bs = bs; this.w = w; this.h = h; this.ext = ext;
        this.node_color = node_color; this.node_size = node_size; this.link_color = link_color; this.link_size = link_size;
        this.arrows = arrows; this.link_trans = link_trans; this.curves = curves; this.timing = timing;
	this.draw_node_labels = draw_node_labels; this.draw_link_labels = draw_link_labels;
	this.entity_labels = entity_labels; this.entity_color = entity_color; this.bundle_labels = bundle_labels; this.graph_bg = graph_bg;
	this.color_by = color_by; this.count_by = count_by;
	this.draw_edge_templates = draw_edge_templates; this.draw_node_legend = draw_node_legend;
	// Create an initial no map set with all of the bundles
	Set<Bundle> no_maps = new HashSet<Bundle>(); no_maps.addAll(bs.bundleSet());
/*
    System.err.println("Extents:\tx=" + ext.getX() + "\ty=" + ext.getY() + 
                       "\tcx=" + ext.getCenterX() + "\tcy=" + ext.getCenterY() +
                       "\tw=" + ext.getWidth() + "\th=" + ext.getHeight());
*/
        // Create the counter contexts
        link_counter_context   = new BundlesCounterContext(bs, count_by, color_by);
        node_counter_context   = new BundlesCounterContext(bs, count_by, color_by);
	entity_counter_context = new BundlesCounterContext(bs, count_by, color_by);
        if (digraph.getNumberOfEntities() == 0) { return; }
	// Force a transform
        if (last_rc == null || last_rc.getRCWidth() != w || last_rc.getRCHeight() != h) transform();
        // transform();
        // Figure out which edges are active
        Iterator<Tablet> it_t = bs.tabletIterator();
        while (it_t.hasNext() && currentRenderID() == getRenderID()) { // RenderID used to abort early
          Tablet tablet = it_t.next();
          // Determine if the tablet can be used here
          boolean fills_relationship = (graph.getNumberOfEntities() > 0) && (active_relationships.size() == 0);
          if (fills_relationship == false) for (int i=0;i<active_relationships.size();i++) {
            StringTokenizer st = new StringTokenizer(active_relationships.get(i), BundlesDT.DELIM);
            String fm_e, to_e;
	    fm_e = Utils.decFmURL(st.nextToken()); st.nextToken() /* icon */ ; st.nextToken(); /* typed */
	    to_e = Utils.decFmURL(st.nextToken());
            if (KeyMaker.tabletCompletesBlank(tablet, fm_e) && KeyMaker.tabletCompletesBlank(tablet, to_e)) fills_relationship = true;
          }
          boolean tablet_can_count = count_by.equals(BundlesDT.COUNT_BY_BUNS) || KeyMaker.tabletCompletesBlank(tablet, count_by);
          // If it fills the relationship, add to the edges
          if (fills_relationship) {
            Iterator<Bundle> it_b = tablet.bundleIterator(); no_maps.removeAll(tablet.bundleSet());
	    while (it_b.hasNext() && currentRenderID() == getRenderID()) { // RenderID used to abort early
              Bundle bundle = it_b.next();
	      Iterator<String> it_l = digraph.linkUnRefIterator(bundle); if (it_l.hasNext() == false) no_maps.add(bundle);
              while (it_l.hasNext()) {
                String graph_edge_ref  = it_l.next();
                // Node counts
                String fm_entity  = digraph.getEntityDescription(digraph.linkRefFm(graph_edge_ref)), 
                       to_entity  = digraph.getEntityDescription(digraph.linkRefTo(graph_edge_ref));
                visible_entities.add(fm_entity); visible_entities.add(to_entity);
                String node_coord;
	        // - fm entity
	        node_coord = entity_to_sxy.get(fm_entity); 
	        if (node_coord_set.containsKey(node_coord) == false) { 
                  node_coord_set.put(node_coord,new HashSet<String>());
                  node_coord_lu.put(node_coord,new Point2D.Float(entity_to_sx.get(fm_entity),entity_to_sy.get(fm_entity)));
                }
	        node_coord_set.get(node_coord).add(fm_entity);
		if (tablet_can_count) {
                  node_counter_context.count(bundle, node_coord);
		  entity_counter_context.count(bundle, fm_entity);
                }
                // - to entity
                node_coord = entity_to_sxy.get(to_entity); 
	        if (node_coord_set.containsKey(node_coord) == false) {
                  node_coord_set.put(node_coord,new HashSet<String>());
                  node_coord_lu.put(node_coord,new Point2D.Float(entity_to_sx.get(to_entity),entity_to_sy.get(to_entity)));
                }
	        node_coord_set.get(node_coord).add(to_entity);
		if (tablet_can_count) {
                  node_counter_context.count(bundle, node_coord);
		  entity_counter_context.count(bundle, to_entity);
                }
                // Link counts
                String line_ref  = entity_to_sxy.get(fm_entity) + BundlesDT.DELIM + entity_to_sxy.get(to_entity);
                Line2D line      = new Line2D.Float(entity_to_sx.get(fm_entity),entity_to_sy.get(fm_entity),entity_to_sx.get(to_entity),entity_to_sy.get(to_entity));
                if (link_to_line.keySet().contains(line_ref) == false) { link_to_line.put(line_ref, line); line_to_link.put(line, line_ref); }

		if (link_to_graphedgerefs.containsKey(line_ref) == false) link_to_graphedgerefs.put(line_ref,new HashSet<String>());
		link_to_graphedgerefs.get(line_ref).add(graph_edge_ref);
		graphedgeref_to_link.put(graph_edge_ref,line_ref);

                if (tablet_can_count) link_counter_context.count(bundle, line_ref);
                if (line_ref_to_styles.containsKey(line_ref) == false) line_ref_to_styles.put(line_ref, new HashSet<String>());
                line_ref_to_styles.get(line_ref).addAll(digraph.getLinkStyles(graph_edge_ref));
              }
	    }
          }
        }
	// Add all to the no mapping set
	// System.err.println("RTGraph.no_maps().size = " + no_maps.size());
	addToNoMappingSet(no_maps);
	// Stop the timer
        timer_b = System.currentTimeMillis();
      }

      /**
       * Return if graph information is being drawn.
       *
       *@return true for rendering graph information
       */
      public boolean provideGraphInfo() { return node_size == NodeSize.GRAPHINFO; }

      /**
       * Tests to see if a specific string is a link in the current context.
       *
       *@return true if string is a link (edge)
       */
      public boolean isLink(String str) { return link_to_line.keySet().contains(str); }

      /**
       * Reverses a link so that it can be used to test for the other direction.
       *
       *@param str link
       *
       *@return reversed link
       */
      public String  reverseLink(String str) {
        int pip = str.indexOf(BundlesDT.DELIM, str.indexOf(BundlesDT.DELIM)+1);
	return str.substring(pip+1,str.length()) + BundlesDT.DELIM + str.substring(0,pip);
      }

      @Override
      public int             getRCWidth()      { return w; }
      @Override
      public int             getRCHeight()     { return h; }

      /**
       * Returns that this view represents entities directly.
       *
       *@return true
       */
      @Override
      public boolean         hasEntityShapes() { return true; }

      /**
       * Returns the entity shapes associated with the substrings extracted from a text.
       *
       *@param subtexts extracted entities to match
       *
       *@return shapes for the matching entities
       */
      @Override
      public Set<Shape>  entityShapes(Set<SubText> subtexts) {
        Set<String> straights = new HashSet<String>();
	// Go through the subtexts -- separate them into CIDRs and straights
        Iterator<SubText> it_sub = subtexts.iterator();
	while (it_sub.hasNext()) {
	  SubText subtext = it_sub.next();
	  // Only match entity subtexts
	  if (subtext instanceof Entity) {
	    Entity entity = (Entity) subtext;
            straights.add(subtext.toString());
	  }
	}
	return entityShapesInternal(straights);
      }

      /**
       * Return the entity shapes associated with a set of strings.
       *
       *@param superset strings to match
       *
       *@return shapes for the matching entities
       */
      public Set<Shape> entityShapesInternal(Set<String> superset) {
        Set<String> filtered = filterEntities(superset); Set<Shape> shapes = new HashSet<Shape>();
	Iterator<String> it = filtered.iterator();
	while (it.hasNext()) {
	  String node_coord = entity_to_sxy.get(it.next());
	  if (node_coord != null) {
	    Shape shape = node_to_geom.get(node_coord);
	    if (shape != null) shapes.add(shape);
	  }
	}
        return shapes;
      }

      /**
       * Return a set of just the entities within this link-node view.
       *
       *@param superset set to check against
       *
       *@return intersecting set
       */
      public Set<String> filterEntities(Set<String> superset) {
        Set<String> set = new HashSet<String>(); set.addAll(superset);
        // Check to see if any of the relationships are typed...  if so, we'll have to do the typed conversions...
        for (int i=0;i<active_relationships.size();i++) {
	  String relationship = active_relationships.get(i);
	  if (relationshipFromTyped(relationship)) addTypedStrings(set, relationshipFromHeader(relationship));
	  if (relationshipToTyped(relationship))   addTypedStrings(set, relationshipToHeader(relationship));
	} 
	// Retain all...
	set.retainAll(visible_entities);
	return set;
      }

      /**
       * For all of the strings in the set, add the prefix hdr for easier set matching.  This is a fix for the
       * occurence of the field header as part of the node name.
       *
       *@param set set of strings
       *@param hdr prepend the hdr for typing
       */
      private void addTypedStrings(Set<String> set, String hdr) {
        BundlesG globals = getRTParent().getRootBundles().getGlobals();  Set<String> to_add = new HashSet<String>();
	Set<BundlesDT.DT> datatypes = globals.getFieldDataTypes(globals.fieldIndex(hdr));

        // In transformed datatypes (e.g., dstip|ORG), the datatypes lookup fails and a null is returned.  I'm
        // unsure if the following will further break the selection/filtered data views -- the underlying assumption
        // in the original implementation was that all of the entities were strongly typed...  however, for IP Org
        // lookups, this is clearly not the case.
        if (datatypes == null) return;

	Iterator<BundlesDT.DT> it_dt = datatypes.iterator();
	while (it_dt.hasNext()) {
	  BundlesDT.DT dt = it_dt.next();
	  Iterator<String> it = set.iterator();
          while (it.hasNext()) {
	    String str = it.next();
	    if (BundlesDT.stringIsType(str, dt)) to_add.add(hdr + BundlesDT.DELIM + str);
	  }
	}
	set.addAll(to_add);
      }

      /**
       * Draw the K-Core analysis for the the current graph.
       *
       *@param g2d graphics primitive
       *@param bi  buffered image for the rendering
       */
      protected void drawKCores(Graphics2D g2d, BufferedImage bi) {
        Map<String,Integer> kcore_lu = GraphUtils.kCore(graph);
        RTGraphPanel rt_graph_panel = (RTGraphPanel) getRTPanel();
        GraphUtils.renderKCores(g2d, bi, entity_to_wxy, new UniGraph(graph), getWorldToScreenTransform(), visible_entities);
      }

      /**
       * Draw the specified background on the view prior to rendering the link node graph.
       *
       *@param g2d  graphics primitive
       *@param type type of background to draw
       */
      protected void drawGeoBackground(Graphics2D g2d, GraphBG type) {
	// Get the shapes, make sure we have them
	ShapeFile sf = GeoData.getInstance().getShapeFile(); if (sf == null) return;
	// Save original graphics context
        AffineTransform orig_trans  = g2d.getTransform();
	Stroke          orig_stroke = g2d.getStroke();
        // Set the transform 
	g2d.scale(getRCWidth()/ext.getWidth(),-getRCHeight()/ext.getHeight());
	g2d.translate(-ext.getX(), ext.getY());
        // Based on the type, draw the geo background
        if (type == GraphBG.GEO_FILL) {
	  // Draw the oceans
	  g2d.setColor(RTColorManager.getColor("linknode", "ocean"));
	  g2d.fillRect(-180,-90,360,180);
	  // Fill the continents
	  g2d.setColor(RTColorManager.getColor("background", "default"));
	  sf.fill(g2d);
	  // Color in the lines
	  g2d.setColor(RTColorManager.getColor("linknode", "ocean"));
	  g2d.setStroke(new BasicStroke((float) (0.0004*ext.getWidth())));
          sf.draw(g2d);
	} else if (type == GraphBG.GEO_TOUCH) {
	  g2d.setStroke(new BasicStroke((float) (0.0008*ext.getWidth())));
	  // Get the countries overlapping with the points
          Set<CCShapeRec>  recs = GeoData.getInstance().containingCountries(entity_to_wxy); 
	  // Draw only them...
	  Iterator<CCShapeRec> it   = recs.iterator();
	  while (it.hasNext()) {
	    CCShapeRec rec = it.next();
	    Color color        = RTColorManager.getColor(rec.getName());
	    g2d.setColor(color);
	    rec.getShapeRec().fill(g2d);
	  }
	} else {
	  g2d.setColor(RTColorManager.getColor("axis", "minor"));
	  g2d.setStroke(new BasicStroke((float) (0.0008*ext.getWidth())));
          sf.draw(g2d);
        }
	// Reset the transform
	g2d.setStroke(orig_stroke);
	g2d.setTransform(orig_trans);
      }

      /**
       * Draw the background.
       *
       *@param g2d graphics primitive
       */
      protected void drawBackground(Graphics2D g2d, BufferedImage bi) {
        switch (graph_bg) {
          /* ========================================================= */
	  case GEO_FILL:
	  case GEO_OUT:
	  case GEO_TOUCH: drawGeoBackground(g2d, graph_bg); break;
          /* ========================================================= */
          case KCORES:    drawKCores(g2d, bi); break;
	  default:        /* Draw nothing */
	}
      }

      /**
       * Cached copy of the rendered image
       */
      BufferedImage base_bi;

      /**
       * Timers for monitoring the performance of the renderer
       */
      long timer_a, timer_b, timer_c, timer_d;

      /**
       * Render the image.
       */
      @Override
      public BufferedImage getBase() {
        if (base_bi == null) {
	 Graphics2D g2d = null;
         try {
          timer_c = System.currentTimeMillis();
          base_bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
	  g2d = (Graphics2D) base_bi.getGraphics();
          g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	  RTColorManager.renderVisualizationBackground(base_bi, g2d);
	  // Draw the background
          if (graph_bg != GraphBG.NONE) { drawBackground(g2d, base_bi); }
          // Draw the links
          if (link_size != LinkSize.INVISIBLE) drawLinks(g2d);
          // Draw the nodes
          if (node_size != NodeSize.INVISIBLE) drawNodes(g2d);
	  // Draw the legend
          if (draw_edge_templates) drawEdgeTemplates(g2d);
	  if (draw_node_legend)    drawNodeLegend(g2d);
          // Provide stats on render time
          timer_d = System.currentTimeMillis();
          g2d.setColor(RTColorManager.getColor("label", "default")); 
          String  str = "RTime : " + ((timer_b - timer_a) + (timer_d - timer_c)); 
          g2d.drawString(str,w - Utils.txtW(g2d,str),Utils.txtH(g2d,"0"));
          // Let user now if the graph is limited
          if (retained_nodes != null && retained_nodes.size() > 0) {
            clearStr(g2d, "Limited", w - Utils.txtW(g2d,str), 2*Utils.txtH(g2d,"0"), RTColorManager.getColor("label", "errorfg"), RTColorManager.getColor("label", "errorbg"));
          }
          // Clean up
	 } finally { if (g2d != null) g2d.dispose(); }
        }
	return base_bi;
      }

      /**
       * Render the edge headers for the displayed graph.  This is useful for keeping
       * track of which headers are already being mapped to edges.
       *
       *@param g2d graphics primitive
       */
      public void drawEdgeTemplates(Graphics2D g2d) {
        Iterator<String> it = active_relationships.iterator();
	int              y  = getRCHeight() - 2 * Utils.txtH(g2d, "0");
	while (it.hasNext()) {
	  String str    = it.next();
          String fm_hdr = relationshipFromHeader(str),
	         to_hdr = relationshipToHeader(str);
          g2d.setColor(RTColorManager.getColor("label", "default"));
	  g2d.drawString(fm_hdr + " => " + to_hdr, 5, y);
	  y -= Utils.txtH(g2d, "0");
        }
      }

      /**
       * Render the legend for the node shapes.
       *
       *@param g2d graphics primitive;
       */
      public void drawNodeLegend(Graphics2D g2d) {
        Iterator<String> it    = active_relationships.iterator();
	int              y     = getRCHeight() - 2 * Utils.txtH(g2d, "0");
	int              max_w = 0;
        while (it.hasNext()) {
	  String str      = it.next();
          int    fm_hdr_w = Utils.txtW(g2d, relationshipFromHeader(str)),
	         to_hdr_w = Utils.txtW(g2d, relationshipToHeader(str));
          if (fm_hdr_w > max_w) max_w = fm_hdr_w;
	  if (to_hdr_w > max_w) max_w = to_hdr_w;
	}
                         it    = active_relationships.iterator();
        int              x     = getRCWidth() - max_w - 5;
        Set<String>      drawn = new HashSet<String>();
	while (it.hasNext()) {
	  String str    = it.next();
          String fm_hdr  = relationshipFromHeader(str),
	         fm_icon = relationshipFromIcon(str),
	         to_hdr  = relationshipToHeader(str),
		 to_icon = relationshipToIcon(str);
          g2d.setColor(RTColorManager.getColor("label", "default"));
          Shape  shape; String key;
	  key = fm_hdr + BundlesDT.DELIM + fm_icon;
	  if (drawn.contains(key) == false) {
	    shape = Utils.shape(Utils.parseSymbol(fm_icon), x - 10, y - 6, 6); g2d.draw(shape); g2d.drawString(fm_hdr, x, y); y -= Utils.txtH(g2d, "0");
	    drawn.add(key);
          }
	  key = to_hdr + BundlesDT.DELIM + to_icon;
	  if (drawn.contains(key) == false) {
            shape = Utils.shape(Utils.parseSymbol(to_icon), x - 10, y - 6, 6); g2d.draw(shape); g2d.drawString(to_hdr, x, y); y -= Utils.txtH(g2d, "0");
	    drawn.add(key);
          }
        }
      }

      /**
       * Abstract class to color links.
       */
      abstract class LinkColorer { 
        /**
	 * Based on the link string, return the appropriate color of that link (edge/line).
	 *
	 *@param link string version of edge
	 *
	 *@return color for rendering the edge on the visualization
	 */
        abstract Color linkColor(String link); 
      }

      /**
       * Fixed class for coloring the links in a constant color.
       */
      class FixedLinkColorer extends LinkColorer { Color color;
                                                   public FixedLinkColorer(Color color) { this.color = color; }
						   public Color linkColor(String link) { return color; } };

      /**
       * Variable class for coloring the links based on the counter context results.
       */
      class VaryLinkColorer extends LinkColorer {  public Color linkColor(String link) { return link_counter_context.binColor(link); } }

      /**
       * Method to draw the pre-calculated links onto the screen.  Handles coloring, style, etc.
       *
       *@param g2d graphics primitive
       */
      protected void drawLinks(Graphics2D g2d) {
        Rectangle2D screen = new Rectangle2D.Float(0,0,w,h);
        // Figure out the stroke, color, transparency
        Stroke orig_stroke = g2d.getStroke(); Composite orig_composite = g2d.getComposite();
	LinkSizer sizer = null; LinkColorer colorer = null; float default_size = 1.0f;
        switch (link_size) {
	  case INVISIBLE: return;
	  case THIN:      g2d.setStroke(new BasicStroke(default_size = 0.4f)); break;
	  case THICK:     g2d.setStroke(new BasicStroke(default_size = 2.5f)); break;
	  case NORMAL:    g2d.setStroke(new BasicStroke(default_size = 1.0f)); break;
	  case VARY:      sizer = new VaryLinkSizer();          break;
          case CONDUCT:   sizer = new ConductanceLinkSizer();   break;
          case CLUSTERP:  sizer = new ClusterProbSizer();       break;
          default:        throw new RuntimeException("Unknown Link Size " + link_size);
        }
        switch (link_color) {
	  case GRAY:      colorer = new FixedLinkColorer(RTColorManager.getColor("linknode", "edge")); break;
	  case VARY:      colorer = new VaryLinkColorer();                 break;
	  default:        throw new RuntimeException("Unknown Link Color " + link_color);
        }

	// Enable transparency if requested
        if (link_trans) { g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.6f)); }

	// Make the label maker
	LabelMaker lm = null;
        if (draw_link_labels) lm = new LabelMaker(bundle_labels, link_counter_context);

        // - Create the dashes
        float dashes[]    = null;
        float long_ds[]   = new float[2],
              dotted_ds[] = new float[2],
              alter_ds[]  = new float[4];
        long_ds[0]   = 10.0f; long_ds[1]   = 5.0f;
        dotted_ds[0] = 1.0f;  dotted_ds[1] = 5.0f;
        alter_ds[0]  = 10.0f; alter_ds[1]  = 5.0f; alter_ds[2] = 1.0f; alter_ds[3] = 5.0f;
        // Go through the links and draw them
        Iterator<String> it = link_counter_context.binIterator();
	while (it.hasNext()) {
          String link  = it.next(); if (link.equals(reverseLink(link))) continue;
          Line2D line  = link_to_line.get(link);
          if (line.intersects(screen)) {
            // Figure out the rendering...
            Set<String> styles = line_ref_to_styles.get(link);
            if (styles != null && styles.size() == 1) {
              String str = styles.iterator().next();
              if      (str.equals(STYLE_SOLID_STR))     dashes = null;
              else if (str.equals(STYLE_LONG_DASH_STR)) dashes = long_ds;
              else if (str.equals(STYLE_DOTTED_STR))    dashes = dotted_ds;
              else if (str.equals(STYLE_ALTERNATE_STR)) dashes = alter_ds;
              else System.err.println("Do Not Understand Line Style \"" + str + "\"");
            } else dashes = null;
	    if      (sizer != null && dashes != null) g2d.setStroke(new BasicStroke(sizer.linkSize(link), BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 1.0f, dashes, 0.0f));
            else if (sizer != null                  ) g2d.setStroke(new BasicStroke(sizer.linkSize(link)));
            else if (                 dashes != null) g2d.setStroke(new BasicStroke(default_size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 1.0f, dashes, 0.0f));
            else                                      g2d.setStroke(new BasicStroke(default_size));
            g2d.setColor(colorer.linkColor(link));

	    // Draw curved arcs to help differentiate direction
	    if (curves) {
	      //
	      // Calculate the arc
	      //
              double dx     =  line.getX2() - line.getX1(),
	             dy     =  line.getY2() - line.getY1();
              double l      = Math.sqrt(dx*dx+dy*dy); if (l < 0.0001) l = 0.0001;
	             dx     = dx/l;
		     dy     = dy/l;
              double cx     = (line.getX2() + line.getX1())/2,
	             cy     = (line.getY2() + line.getY1())/2;
              double ctrl_x = cx - (l/8)*dy,
	             ctrl_y = cy + (l/8)*dx;
	      g2d.draw(new QuadCurve2D.Double(line.getX1(), line.getY1(), ctrl_x, ctrl_y, line.getX2(), line.getY2()));

	      //
	      // Add a half arrow to aid with directionality
	      //
                     ctrl_x = cx - (l/4)*dy;
	             ctrl_y = cy + (l/4)*dx;
                     dx     = ctrl_x - line.getX2();
		     dy     = ctrl_y - line.getY2();
		     l      = Math.sqrt(dx*dx+dy*dy); if (l < 0.0001) l = 0.0001;
		     dx     = dx/l;
		     dy     = dy/l;
              g2d.draw(new Line2D.Double(line.getX2(), line.getY2(), line.getX2() + dx*16.0, line.getY2() + dy*16.0));

	    } else g2d.draw(line);

	    // Draw arrows if applicable
            if (arrows || timing) {
	      double dx  = line.getX2() - line.getX1(),
	             dy  = line.getY2() - line.getY1();
	      double len = Utils.length(dx,dy);
	      if (len < 0.001) len = 0.001; dx = dx/len; dy = dy/len; double pdx = dy, pdy = -dx;
	      if (arrows) {
                GeneralPath gp = new GeneralPath();
		// Triangle (best?... worst for performance)
		// - From "A User Study on Visualizing Directed Edges in Graphs", Danny Holten, Jarke J. van Wijk.  2009.
		//
		gp.moveTo(line.getX2(),         line.getY2());
		gp.lineTo(line.getX1() - 5*pdx, line.getY1() - 5*pdy);
		gp.lineTo(line.getX1() + 5*pdx, line.getY1() + 5*pdy);
		gp.closePath();
		g2d.fill(gp);
/*              // Mid Arrows (better)
	        double midx = (line.getX1() + line.getX2())/2.0, midy = (line.getY1() + line.getY2())/2.0;
	        g2d.draw(new Line2D.Double(midx - 5*dx, midy - 5*dy, midx - 10*dx + 5*pdx, midy - 10*dy + 5*pdy));
	        g2d.draw(new Line2D.Double(midx - 5*dx, midy - 5*dy, midx - 10*dx - 5*pdx, midy - 10*dy - 5*pdy));
*/
/*              // End Arrows (okay)
	        g2d.draw(new Line2D.Double(line.getX2() - 5*dx, line.getY2() - 5*dy, line.getX2() - 10*dx + 5*pdx, line.getY2() - 10*dy + 5*pdy));
	        g2d.draw(new Line2D.Double(line.getX2() - 5*dx, line.getY2() - 5*dy, line.getX2() - 10*dx - 5*pdx, line.getY2() - 10*dy - 5*pdy));
*/
              }
	      if (timing) {
                Iterator<Bundle> itb = link_counter_context.getBundles(link).iterator();
		while (itb.hasNext()) {
                  Bundle bundle = itb.next();
		  if (bundle.hasTime()) {
		    double ratio = ((double) (bundle.ts0() - bs.ts0()))/((double) (bs.ts1dur() - bs.ts0()));
                    g2d.setColor(timing_marks_cs.at((float) ratio));
		    double x0 = line.getX1() + dx * len * 0.1 + dx * len * 0.8 * ratio,
		           y0 = line.getY1() + dy * len * 0.1 + dy * len * 0.8 * ratio;
                    // g2d.draw(new Line2D.Double(x0 - 4*pdx, y0 - 4*pdy, x0 + 4*pdx, y0 + 4*pdy));
                    g2d.draw(new Line2D.Double(x0, y0, x0 + 4*pdx, y0 + 4*pdy));
                    
		    // Draw the duration if appropriate
		    double ratio2 = ((double) (bundle.ts1() - bs.ts0()))/((double) (bs.ts1dur() - bs.ts0()));
                    if (len * (ratio2 - ratio) > 4) {
		      double x1 = line.getX1() + dx * len * 0.1 + dx * len * 0.8 * ratio2,
		             y1 = line.getY1() + dy * len * 0.1 + dy * len * 0.8 * ratio2,
                             xa = line.getX1() + dx * len * 0.1 + dx * (len-0.05*len) * 0.8 * ratio2,
			     ya = line.getY1() + dy * len * 0.1 + dy * (len-0.05*len) * 0.8 * ratio2;
		      g2d.draw(new Line2D.Double(x0 + 4*pdx, y0 + 4*pdy, x1 + 4*pdx, y1 + 4*pdy));
		      g2d.draw(new Line2D.Double(xa + 8*pdx, ya + 8*pdy, x1 + 4*pdx, y1 + 4*pdy));
		      // g2d.draw(new Line2D.Double(x1 + 4*pdx, y1 + 4*pdy, x1 + 2*pdx + 2*pdy, y1 + 2*pdy + 2*pdx));
		    }
		  }
		}
	      }
	    }
	    // Draw labels if applicable
	    if (lm != null) lm.draw(g2d, link, (int) line.getBounds().getCenterX(), 
	                                       (int) line.getBounds().getCenterY(), false);

            // Accounting for interactivity
            all_shapes.add(line);
            // line_to_link.put(line, link); // Doesn't seem to be needed -- replaced with another function...
            line_to_bundles.put(line, link_counter_context.getBundles(link));

            Iterator<Bundle> itb = link_counter_context.getBundles(link).iterator();
            while (itb.hasNext()) {
	      Bundle bundle = itb.next();
              if (bundle_to_shapes.containsKey(bundle) == false) bundle_to_shapes.put(bundle, new HashSet<Shape>());
	      bundle_to_shapes.get(bundle).add(line);
            }
          }
	}
        g2d.setComposite(orig_composite); g2d.setStroke(orig_stroke);
      }

      ColorScale timing_marks_cs = RTColorManager.getTemporalColorScale();

      /**
       * Abstract class to handle the render size of links (edges/lines).
       */
      abstract class LinkSizer { 
        /**
	 * Based on the link, return the appropriate size for the line in the rendering.
	 *
	 *@param link link (string version of edge)
	 *
	 *@return line width for rendering
	 */
        abstract float linkSize(String link); 
      }

      /**
       * Link size based on conductance on the edge - won't work with aggregates.
       */
      class ConductanceLinkSizer extends LinkSizer {
        double min, max;
        public ConductanceLinkSizer() { 
	  if (conductance == null) conductance = new Conductance(graph, 100, 0.2); 
          min = conductance.getMin();
	  max = conductance.getMax();
	}
        public float linkSize(String link) {
	  Set<String> nodes         = new HashSet<String>();
	  Set<String> graphedgerefs = link_to_graphedgerefs.get(link);
          Iterator<String> it = graphedgerefs.iterator(); while (it.hasNext()) {
            String ref = it.next();
	    nodes.add(graph.getEntityDescription(graph.linkRefFm(ref)));
	    nodes.add(graph.getEntityDescription(graph.linkRefTo(ref)));
	  }
          if (nodes.size() != 2) return 0.5f;
	  it = nodes.iterator(); String n0 = it.next(), n1 = it.next();
	  double val = conductance.getResult(n0, n1);
	  return (float) (0.5 + 5.0*(val - min)/(max - min));
	}
      }

      /**
       * Link size based on cluster probability between two nodes - won't work with aggregates.
       */
      class ClusterProbSizer extends LinkSizer {
        public ClusterProbSizer() { if (conductance == null) conductance = new Conductance(graph, 100, 0.2); }
	public float linkSize(String link) {
	  Set<String> nodes         = new HashSet<String>();
	  Set<String> graphedgerefs = link_to_graphedgerefs.get(link);
          Iterator<String> it = graphedgerefs.iterator(); while (it.hasNext()) {
            String ref = it.next();
	    nodes.add(graph.getEntityDescription(graph.linkRefFm(ref)));
	    nodes.add(graph.getEntityDescription(graph.linkRefTo(ref)));
	  }
          if (nodes.size() != 2) return 0.5f;
	  it = nodes.iterator(); String n0 = it.next(), n1 = it.next();
	  double val = conductance.getClusterProbability(n0, n1);
	  return (float) (0.5 + 5.0*val);
	}
      }

      /**
       * Use a variable link sizer that queries the counter context to determine with
       * width of the edge/line.
       */
      class VaryLinkSizer extends LinkSizer { public float linkSize(String link) { 
        return (float) (link_counter_context.totalNormalized(link) * 5.0 + 0.5);
      } }

      /**
       * Abstraction to handle the color of nodes.
       */
      abstract class NodeColorer { 
        /**
	 * For the specified node, return the appropriate color for rendering that node.
	 *
	 *@param node node in graph (or collection of nodes)
	 *
	 *@return color for rendering the node
	 */
        abstract Color nodeColor(String node); }

      /**
       * Use a fixed node color class to render the nodes
       */
      class FixedNodeColorer  extends NodeColorer    { Color color;
                                                       public FixedNodeColorer(Color color) { this.color = color; } 
                                                       public Color nodeColor(String node)  { return color; } }
      /**
       * Use a variable node color that draws from the render context for the node color.
       */
      class VaryNodeColorer   extends NodeColorer    { public Color nodeColor(String node)  { return node_counter_context.binColor(node); } }

      /**
       * Use a variable node color that uses the label results for the node color.
       */
      class LabelNodeColorer  extends NodeColorer    { LabelMaker lm;
                                                       public LabelNodeColorer()            { lm = new LabelMaker(entity_color, node_counter_context); }
						       public Color nodeColor(String node)  { return lm.getColor(node); } }

      /**
       * Colorer that adjusts to the cluster coefficient for a node.
       */
      class ClusterCoefficientColorer extends NodeColorer {
        public ClusterCoefficientColorer()   { if (cluster_cos == null) cluster_cos = GraphUtils.clusterCoefficients(graph); }
        public Color nodeColor(String node) {
          if (node_coord_set.get(node).size() > 1) return RTColorManager.getColor("set", "multi");
	  else {
            String  n0         = node_coord_set.get(node).iterator().next();
            double coefficient = cluster_cos.get(n0);
	    return RTColorManager.getLogColor(Math.pow(10,10*coefficient));
          }
	}
      }

      /**
       * Lookup the geometrical representation of the node string version "x|y".
       *
       *@param node_coord string version of coordiante
       *
       *@return screen coordinate of node
       */
      public Point2D nodeToPoint(String node_coord) { return node_coord_lu.get(node_coord); }

      /**
       * Class to abstract the node shape information away.
       */
      abstract class NodeShaper  { 
        /**
	 * For a specific node coordinate string, return the proper shape for rendering the node.
	 *
	 *@param node node coordinate string
         *@param g2d  graphics primitive
	 *
	 *@return shape of node(s)
	 */
        abstract Shape nodeShape(String node, Graphics2D g2d); }

      /**
       * Fixed node shaper that renders the shape with a constant width/height.
       */
      class FixedNodeShaper extends NodeShaper {
        float size;
        public FixedNodeShaper(float size) { this.size = size; }
        public Shape nodeShape(String node, Graphics2D g2d) {
	  if (node_coord_set.get(node).size() > 1) return cloverShape(node, size);
	  else {
	    Point2D point = nodeToPoint(node);
            Utils.Symbol symbol = entity_to_shape.get(node_coord_set.get(node).iterator().next());
            float x0 = (float) (point.getX() - size/2),
	          y0 = (float) (point.getY() - size/2);
            return Utils.shape(symbol,x0,y0,size); } } } 

      /**
       * Special shaper for when the label becomes the node.
       */
      class LabelNodeShaper extends NodeShaper {
        LabelMaker node_lm; int txt_h = -1; FixedNodeShaper fixed = new FixedNodeShaper(10.0f);
        public LabelNodeShaper(LabelMaker node_lm) { this.node_lm = node_lm; }
        public Shape nodeShape(String node, Graphics2D g2d) {
	  // Get the coordinate
          Point2D point = nodeToPoint(node);
          int x = (int) point.getX(), y = (int) point.getY(); 

	  // Get the string
	  String strs[] = node_lm.toStrings(node); String str = "Not Set"; if (strs.length > 0) str = strs[0];

	  // Calculate the text dimensions
          if (txt_h == -1) txt_h = Utils.txtH(g2d, "0"); 
	  int txt_w = Utils.txtW(g2d, str);

	  // Make the area -- a rectangle with rounded begins and ends
          Area area = new Area(); 
          area.add(new Area(new Rectangle2D.Float(x-txt_w/2,y-txt_h,txt_w,txt_h)));
          area.add(new Area(new Ellipse2D.Float(x-txt_w/2 - txt_h/2,y-txt_h,txt_h,txt_h)));
          area.add(new Area(new Ellipse2D.Float(x+txt_w/2-txt_h/2,y-txt_h,txt_h,txt_h)));

          return area;
        }
      }

      /**
       * Create a shape basd on the counter context for the node.
       */
      class VaryNodeShaper extends NodeShaper {
        public Shape nodeShape(String node, Graphics2D g2d) {
	  float size  = (float) (1.0 + node_counter_context.totalNormalized(node) * 15.0);
          if (node_coord_set.get(node).size() > 1) return cloverShape(node, size);
	  else {
	    Point2D point = nodeToPoint(node);
            Utils.Symbol symbol = entity_to_shape.get(node_coord_set.get(node).iterator().next());
            float x0 = (float) (point.getX() - size/2),
	          y0 = (float) (point.getY() - size/2);
            return Utils.shape(symbol,x0,y0,size); } } } 

      /**
       * Create a shape based on the log of the counter context for the node
       */
      class VaryLogNodeShaper extends NodeShaper {
        public Shape nodeShape(String node, Graphics2D g2d) {
          double  total   = node_counter_context.total(node), 
	          maximum = node_counter_context.totalMaximum();
	  float   size = 1.0f; if (total > 0.0) size = (float) (1.0 + (Math.log(total) / Math.log(maximum)) * 15.0);
          if (node_coord_set.get(node).size() > 1) return cloverShape(node, size);
	  else {
	    Point2D point   = nodeToPoint(node);
            Utils.Symbol symbol = entity_to_shape.get(node_coord_set.get(node).iterator().next());
            float x0 = (float) (point.getX() - size/2),
	          y0 = (float) (point.getY() - size/2);
            return Utils.shape(symbol,x0,y0,size); } } } 

      /**
       * Shaper that adjusts to the cluster coefficient for a node.
       */
      class ClusterCoefficientShaper extends NodeShaper {
        public ClusterCoefficientShaper()   { if (cluster_cos == null) cluster_cos = GraphUtils.clusterCoefficients(graph); }
        public Shape nodeShape(String node, Graphics2D g2d) {
          if (node_coord_set.get(node).size() > 1) return cloverShape(node, 5.0f);
	  else {
            String  n0  = node_coord_set.get(node).iterator().next();
	    Point2D point = nodeToPoint(node);
            Utils.Symbol symbol = entity_to_shape.get(node_coord_set.get(node).iterator().next());
            double coefficient = cluster_cos.get(n0); float size = (float) (1.0 + coefficient * 10.0);
            float x0 = (float) (point.getX() - size/2),
	          y0 = (float) (point.getY() - size/2);
	    return Utils.shape(symbol,x0,y0,size); 
          }
	}
      }

      /**
       * Create a multiple node shape of the appropriate size.  These shapes are used to 
       * represent multiple nodes within the same pixel.
       *
       *@param node_coord string version of node coordinate
       *@param size       desired size of clover
       *
       *@return appropriate shape
       */
      public Shape cloverShape(String node_coord, float size) {
        Point2D point = nodeToPoint(node_coord);
        return Utils.createClover((float) point.getX(), (float) point.getY(), size, size);
      }

      /**
       * Render the graph information about the node.  Varies with other rendering
       * method since the shape is more complex and may need multiple primitive operations.
       *
       *@param g2d        graphics primitive
       *@param node_coord node to render
       *@param shape      recommended shape
       *
       *@return actual shape of rendered node
       */
      private Shape drawNodeGraphInfo(Graphics2D g2d, String node_coord, Shape shape) {
        if (graph_bcc == null) {
          // Create graph parametrics (Only add linear time algorithms here...)
          graph_bcc     = new BiConnectedComponents(graph);
          graph2p_bcc   = new BiConnectedComponents(new UniTwoPlusDegreeGraph(graph));
        }
        BiConnectedComponents bcc = graph_bcc, bcc_2p = graph2p_bcc; if (bcc != null && bcc_2p != null) {
	  // Get the graph info sources
	  Set<String>              cuts    = bcc.getCutVertices(),
	                           cuts_2p = bcc_2p.getCutVertices();
	  Map<String,Set<MyGraph>> v_to_b  = bcc.getVertexToBlockMap();

	  // Determine if we have a set of nodes or a single node
          Set<String> set = node_coord_set.get(node_coord);
	  if (set.size() == 1) {
	    String node = set.iterator().next();
	    if (cuts.contains(node)) {
              g2d.setColor(RTColorManager.getColor("background", "default")); g2d.fill(shape); g2d.setColor(RTColorManager.getColor("background", "reverse")); g2d.draw(shape);
	      if (cuts_2p.contains(node)) {
	        g2d.setColor(RTColorManager.getColor("annotate", "cursor"));
		double cx = shape.getBounds().getCenterX(),
		       cy = shape.getBounds().getCenterY(),
		       dx = shape.getBounds().getMaxX() - cx;
		g2d.draw(new Ellipse2D.Double(cx-1.8*dx,cy-1.8*dx,3.6*dx,3.6*dx));
	      }
	    } else {
	      MyGraph mg = v_to_b.get(node).iterator().next();
              if (mg.getNumberOfEntities() <= 2) g2d.setColor(RTColorManager.getColor("background", "nearbg"));
	      else                               g2d.setColor(RTColorManager.getColor(mg.toString())); 
	      g2d.fill(shape);
	    }
	  } else {
	    boolean lu_miss = false;
	    Set<MyGraph> graphs = new HashSet<MyGraph>();
	    Iterator<String> it = set.iterator();
	    while (it.hasNext()) {
	      String node = it.next();
	      if (v_to_b.containsKey(node)) graphs.addAll(v_to_b.get(node));
	      else { System.err.println("No V-to-B Lookup For Node \"" + node + "\""); lu_miss = true; }
            }
	    if (graphs.size() == 1) {
	      MyGraph mg = graphs.iterator().next();
	      g2d.setColor(RTColorManager.getColor(mg.toString()));
	    } else {
	      g2d.setColor(RTColorManager.getColor("set", "multi"));
	    }
	    g2d.fill(shape);
	    if (lu_miss) {
	      g2d.setColor(RTColorManager.getColor("label", "errorfg"));
	      Rectangle2D rect = shape.getBounds();
	      g2d.drawLine((int) rect.getMinX() - 5, (int) rect.getMinY() - 5, 
	                   (int) rect.getMaxX() + 5, (int) rect.getMaxY() + 5);
	      g2d.drawLine((int) rect.getMaxX() + 5, (int) rect.getMinY() - 5, 
	                   (int) rect.getMinX() - 5, (int) rect.getMaxY() + 5);
	    }
	  }
	}
	return shape;
      }

      /**
       * Make the node shape and color depict information about the entity.  Typically
       * this means drawing a specific color/glyph based on the entity type
       *
       *@param g2d         graphics primitive
       *@param node_coord  coordinate of node to render
       *
       *@return shape of rendered node
       */
      private Shape drawNodeType(Graphics2D g2d, String node_coord) {
        Point2D         point = nodeToPoint(node_coord); int x = (int) point.getX(), y = (int) point.getY();
        Set<String> set   = node_coord_set.get(node_coord);
	Shape           shape = null;
	if (set.size() == 1) {
	  String       str      = set.iterator().next();
	  BundlesDT.DT datatype = BundlesDT.getEntityDataType(str);
          if (datatype != null) { // If the data type is valid, draw the data type specific shape/color
	    switch (datatype) {
	      case IPv4:     shape = drawNodeTypeIPv4(g2d,point,str);   break;
	      case DOMAIN:   shape = drawNodeTypeDomain(g2d,point,str); break;
              case EMAIL:    shape = drawNodeTypeEmail(g2d,point,str);  break;
              case MD5:      shape = drawNodeTypeMD5(g2d,point,str);    break;
	      default:       shape = new Rectangle2D.Double(x - 5, y - 5, 10, 10); 
	                     g2d.setColor(RTColorManager.getColor("background", "reverse"));
			     g2d.fill(shape);
			     break;
            }
          } else { // Draw the string in red/type color 
	    if (str.indexOf(BundlesDT.DELIM) > 0) g2d.setColor(RTColorManager.getColor(str.substring(0,str.indexOf(BundlesDT.DELIM)))); else g2d.setColor(RTColorManager.getColor("default", "major"));
	    g2d.drawString(str, (int) (x - Utils.txtW(g2d,str)/2), y);
            shape = new Rectangle2D.Double(x - Utils.txtW(g2d,str)/2, y - Utils.txtH(g2d,str), Utils.txtW(g2d,str), Utils.txtH(g2d,str));
	  }
	} else               { // Draw the default clover shape
	  g2d.setColor(RTColorManager.getColor("background", "reverse")); 
          Iterator<String> it = set.iterator(); boolean first_ip = true; int min_ip = 0, max_ip = 0;
	  while (it.hasNext()) {
	    String       entity   = it.next();
	    BundlesDT.DT datatype = BundlesDT.getEntityDataType(entity);
	    if (datatype != null) {
              switch (datatype) {
	        case IPv4:  int ip = Utils.ipAddrToInt(entity); 
	                    if (first_ip) { min_ip = max_ip = ip; first_ip = false; } else {
	                      if (min_ip > ip) min_ip = ip; 
			      if (max_ip < ip) max_ip = ip;
			    }
			    break;
                default: break;
              }
	    }
	  }
	  g2d.draw(shape = cloverShape(node_coord, 10.0f));
	  if (first_ip == false) {
	    drawNodeTypeIPv4(g2d, new Point2D.Double(point.getX() - 4, point.getY() - 4), min_ip, max_ip);
	  }
	}
	return shape;
      }
      
      /**
       * Draw the glyph for a domain name and return the shape.
       *
       *@param g2d    graphics primitive
       *@param point  location for node
       *@param domain entity string
       *
       *@return shape of rendered domain
       */
      private Shape drawNodeTypeDomain(Graphics2D g2d, Point2D point, String domain) {
        int x = (int) point.getX(), y = (int) point.getY(), sw = 8, h2 = 8; boolean four_plus = false;
        StringTokenizer st = new StringTokenizer(domain,".");
        while (st.countTokens() > 3) { st.nextToken(); four_plus = true; }
        if (st.countTokens() == 3) { g2d.setColor(RTColorManager.getColor(st.nextToken())); g2d.fillRect(x-sw,y-h2,sw,  h2); }
        if (st.countTokens() == 2) { g2d.setColor(RTColorManager.getColor(st.nextToken())); g2d.fillRect(x,   y-h2,sw,  h2); }
        if (st.countTokens() == 1) { g2d.setColor(RTColorManager.getColor(st.nextToken())); g2d.fillRect(x-sw,y   ,2*sw,h2); }
        if (four_plus) { g2d.setColor(RTColorManager.getColor("label", "minor")); g2d.drawLine(x-sw,y-h2,x-sw-4,y-h2-4); g2d.drawLine(x-sw,y-h2-4,x-sw-4,y-h2); }
	return new Rectangle2D.Double(x-sw,y-h2,2*sw,2*h2);
      }

      /**
       * Draw the glyph for an IPv4 address and return the shape.
       *
       *@param g2d    graphics primitive
       *@param point  location for node
       *@param ipv4   IP address
       *
       *@return shape of rendered address
       */
      private Shape drawNodeTypeIPv4(Graphics2D g2d, Point2D point, String ipv4) {
        StringTokenizer st = new StringTokenizer(ipv4,".");
        int x = (int) point.getX(), y = (int) point.getY(), w = 8, h = 8;
        g2d.setColor(RTColorManager.getColor(st.nextToken())); g2d.fillOval(x-w,   y-h,   2*w,   2*h);
        g2d.setColor(RTColorManager.getColor(st.nextToken())); g2d.fillOval(x-w+3, y-h+3, 2*w-6, 2*h-6);
        g2d.setColor(RTColorManager.getColor(st.nextToken())); g2d.fillOval(x-w+6, y-h+6, 2*w-12,2*h-12);
	return new Ellipse2D.Double(x - 8, y - 8, 2*w, 2*h);
      }

      /**
       * Draw the glyph for an email address and return the shape.
       *
       *@param g2d    graphics primitive
       *@param point  location for node
       *@param ipv4   IP address
       *
       *@return shape of rendered address
       */
      private Shape drawNodeTypeEmail(Graphics2D g2d, Point2D point, String email) {
        Shape shape = Utils.createEnvelope((float) point.getX() - 4, (float) point.getY() - 4, 8, 8);
	g2d.setColor(RTColorManager.getColor(Utils.emailDomain(email)));
	g2d.fill(shape);
	return shape;
      }

      /**
       * Draw the glyph for an md5 and return the shape.
       *
       *@param g2d    graphics primitive
       *@param point  location for node
       *@param ipv4   IP address
       *
       *@return shape of rendered address
       */
      private Shape drawNodeTypeMD5(Graphics2D g2d, Point2D point, String email) {
        Shape shape = Utils.createDocumentShape((float) point.getX() - 4, (float) point.getY() - 4, 12, 12);
	g2d.setColor(RTColorManager.getColor("background", "default")); g2d.fill(shape);
	g2d.setColor(RTColorManager.getColor("data",       "default")); g2d.draw(shape);
	return shape;
      }

      /**
       * Draw the glyph for a range of IPv4 addresses.  Those that share octets
       * will have a uniform color.  If the octet varies, the glyph portion will be black.
       *
       *@param g2d    graphics primitive
       *@param point  location for node
       *@param min_ip lowest IP address
       *@param max_ip greatest IP address
       *
       *@return shape of rendered addresses
       */
      private Shape drawNodeTypeIPv4(Graphics2D g2d, Point2D point, int min_ip, int max_ip) {
        int x = (int) point.getX(), y = (int) point.getY(), w = 8, h = 8;
        if ((min_ip & 0xff000000) == (max_ip & 0xff000000)) g2d.setColor(RTColorManager.getColor("" + ((min_ip >> 24)&0x00ff))); else g2d.setColor(RTColorManager.getColor("background", "default"));
	g2d.fillOval(x-w,   y-h,   2*w,   2*h);
        if ((min_ip & 0x00ff0000) == (max_ip & 0x00ff0000)) g2d.setColor(RTColorManager.getColor("" + ((min_ip >> 16)&0x00ff))); else g2d.setColor(RTColorManager.getColor("background", "default"));
        g2d.fillOval(x-w+3, y-h+3, 2*w-6, 2*h-6);
        if ((min_ip & 0x0000ff00) == (max_ip & 0x0000ff00)) g2d.setColor(RTColorManager.getColor("" + ((min_ip >>  8)&0x00ff))); else g2d.setColor(RTColorManager.getColor("background", "default"));
        g2d.fillOval(x-w+6, y-h+6, 2*w-12,2*h-12);
	return new Ellipse2D.Double(x - 8, y - 8, 2*w, 2*h);
      }
      
      /**
       * Layout the nodes based on their colors using a treemap.
       *
       *@param node_color_for_layout node color for layout
       */
      public void nodeColorTreeMapLayout(NodeColor node_color_for_layout) {
	// Determine the correct colorer to use
        NodeColorer colorer = null;
	switch (node_color_for_layout) {
	  case VARY:      colorer = new VaryNodeColorer();   break;
	  case LABEL:     colorer = new LabelNodeColorer();  break;
	  default:        System.err.println("nodeColorTreeMapLayout() - only works for VARY or LABEL");
	}

	// If we have a valid colorer, collate nodes by the color into a map
	if (colorer != null) {
	  // Map for color to nodes sets
          Map<Color,Set<String>> map = new HashMap<Color,Set<String>>();

	  // Iterate over the nodes to put them into the correct color bins
	  Iterator<String> it = node_counter_context.binIterator();
	  while (it.hasNext()) {
	    String node_coord_str = it.next();
	    Color  color          = colorer.nodeColor(node_coord_str);
	    if (map.containsKey(color) == false) map.put(color, new HashSet<String>());
	    map.get(color).add(node_coord_str);
	  }

	  // Run the treemap algorithm
	  TreeMap treemap = new TreeMap(map);
          Map<Color,Rectangle2D> layout  = treemap.squarifiedTileMapping();
          Iterator<Color> itc = layout.keySet().iterator(); while (itc.hasNext()) {
            Color color = itc.next(); Set<String> set = map.get(color); Rectangle2D rect = layout.get(color);
            // Determine the adjusted rectangle to place them in
            double adj_x = rect.getX() + 0.1 * rect.getWidth(),
                   adj_y = rect.getY() + 0.1 * rect.getHeight(),
                   adj_w = rect.getWidth()  * 0.80,
                   adj_h = rect.getHeight() * 0.80;

            // Determine the increments
            double x_count = Math.ceil(set.size() / adj_h);
            double y_count = Math.ceil(set.size() / x_count);
            double inc_x   = adj_w / x_count,
                   inc_y   = adj_h / y_count;

            // Place the nodes
            Iterator<String> its = set.iterator();
            double x = adj_x, y = adj_y;
            while (its.hasNext()) {
              String node_coord_str = its.next(); Iterator<String> it_nodes = node_coord_set.get(node_coord_str).iterator(); while (it_nodes.hasNext()) {
                entity_to_wxy.put(it_nodes.next(), new Point2D.Double(x,y));
              }
              x += inc_x; if (x > adj_x+adj_w) { x = adj_x; y += inc_y; }
            }
          }
          transform(); zoomToFit(); getRTComponent().render();
	}
      }

      /**
       * Label maker for nodes
       */
      LabelMaker node_lm = null, special_lm =  null;

      /**
       * Take the precalculated information and render it on the screen in the right colors and shapes.
       *
       *@param g2d graphics primitive
       */
      protected void drawNodes(Graphics2D g2d) {
        Rectangle2D screen = new Rectangle2D.Float(0,0,w,h);
        NodeShaper  shaper = null; NodeColorer colorer = null;
        NodeShaper  bkup_shaper = null; // Backup shaper for the label shaper if sticky labels are enabled
	float       dist   = 6.0f;

	// Prepare the label maker (if node size equates to node label... prepare that as well)
        if (node_size == NodeSize.LABEL) {
          List<String> list = new ArrayList<String>();
          if (entity_labels.size() > 0) { String top = entity_labels.get(0); entity_labels.remove(0); list.add(top); } else list.add(ENTITY_LM);
          special_lm = new LabelMaker(list, node_counter_context);
        }
        node_lm = new LabelMaker(entity_labels, node_counter_context);

	// Allocate the shaper and colorer
        switch (node_size) {
	  case TYPE:
	  case GRAPHINFO:
	  case LARGE:     shaper      = new FixedNodeShaper(dist = 10.0f); break;
	  case LABEL:     shaper      = new LabelNodeShaper(special_lm);   
                          bkup_shaper = new FixedNodeShaper(dist = 10.0f); break;
	  case SMALL:     shaper      = new FixedNodeShaper(dist =  4.0f); break;
	  case VARY:      shaper      = new VaryNodeShaper();              break;
	  case VARY_LOG:  shaper      = new VaryLogNodeShaper();           break;
          case CLUSTERCO: shaper      = new ClusterCoefficientShaper();    break;
	  case INVISIBLE:
	  default:        return;
        }
	switch (node_color) {
          case WHITE:     colorer = new FixedNodeColorer(RTColorManager.getColor("background", "reverse")); break;
	  case VARY:      colorer = new VaryNodeColorer();             break;
	  case LABEL:     colorer = new LabelNodeColorer();            break;
          case CLUSTERCO: colorer = new ClusterCoefficientColorer();   break;
	}

	// Figure out the bounding box for clipping
        Rectangle2D screenplus = new Rectangle2D.Double(screen.getX()-10,screen.getY()-10,screen.getWidth()+20,screen.getHeight()+20);

        Iterator<String> it = node_counter_context.binIterator();
	while (it.hasNext()) {
          String node  = it.next();
          Shape  shape = shaper.nodeShape(node, g2d);
          if (screenplus.intersects(shape.getBounds())) {
	    Shape tmp_shape = null;
	    switch (node_size) {
	      case TYPE:      tmp_shape = drawNodeType(g2d, node);             if (tmp_shape != null) shape = tmp_shape; break;
              case GRAPHINFO: tmp_shape = drawNodeGraphInfo(g2d, node, shape); if (tmp_shape != null) shape = tmp_shape; break;
              case LABEL:     boolean single = (node_coord_set.get(node).size() == 1);
                              String  str    = ""; if (single && sticky_labels.size() > 0) str = node_coord_set.get(node).iterator().next();
                              if (sticky_labels.size() == 0 || sticky_labels.contains(str)) {
                                g2d.setColor(RTColorManager.getColor("background", "default")); g2d.fill(shape);
                                g2d.setColor(RTColorManager.getColor("background", "reverse")); g2d.draw(shape);
                                special_lm.draw(g2d, node, (int) shape.getBounds().getCenterX(), (int) shape.getBounds().getMinY()-3, true);
                              } else {
                                shape = bkup_shaper.nodeShape(node, g2d);
                                g2d.setColor(colorer.nodeColor(node)); g2d.fill(shape);
                              }
                              break;
              default:        g2d.setColor(colorer.nodeColor(node)); g2d.fill(shape); break;
	    }
	    if (node_lm != null && draw_node_labels) node_lm.draw(g2d, node, (int) shape.getBounds().getCenterX(), 
	                                                                     (int) shape.getBounds().getMaxY(), true);

            // Accounting for interactivity
            all_shapes.add(shape);
            geom_to_bundles.put(shape, node_counter_context.getBundles(node));
	    node_to_geom.put(node, shape);
            Iterator<Bundle> itb = node_counter_context.getBundles(node).iterator();
            while (itb.hasNext()) {
	      Bundle bundle = itb.next();
              if (bundle_to_shapes.containsKey(bundle) == false) bundle_to_shapes.put(bundle, new HashSet<Shape>());
	      bundle_to_shapes.get(bundle).add(shape);
            }
          }
	}
      }

      /**
       * Class and subclasses responsible for interpreting the label strings and defining the resulting label.
       */
      class LabelMaker {
	/**
	 * List of labels to show
	 */
        protected java.util.List<String> labels;

	/**
	 * Actual label maker subclasses
	 */
        protected LM                     makers[];

	/**
	 * Counter context for accumulating information for labeling
	 */
	protected BundlesCounterContext  cc;

	/**
	 * Construct a new label maker with the list of labels and the pre-calculated
	 * counter context.
	 *
	 *@param labels labels to render
	 *@param cc     counter context for nodes
	 */
        public LabelMaker(java.util.List<String> labels, BundlesCounterContext cc) {
	  this.labels = labels; this.cc = cc; makers = new LM[labels.size()];
	  for (int i=0;i<labels.size();i++) makers[i] = createLM(labels.get(i), labels.size() == 1);
	}

	/**
	 * Return the color of a specific bin (node coord string probably).
	 *
	 *@param bin node coordinate
	 *
	 *@return color for label (?)
	 */
        public Color getColor(String bin) {
	  Color color = RTColorManager.getColor("set", "multi");
          if (makers.length > 0) {
	    color = makers[0].draw(null, bin, 0, 0);
	    if (color == null) color = Color.darkGray;
          }
	  return color;
	}

	/**
	 * For a specific node coordinate (called a bin), render the set of labels from the construction stage.
	 *
	 *@param g2d            graphics primitive
	 *@param bin            node coordinate
	 *@param x              x coordinate for rendering labels
	 *@param y              y coordinate for rendering labels
	 *@param top_justified  determines how the vertical justification will occur
         *
         *@return the shape of the first label
	 */
        public Shape draw(Graphics2D g2d, String bin, int x, int y, boolean top_justified) {
	  // get txt height
	  int txt_h = Utils.txtH(g2d, "0"); 
	  // Figure you where to start
	  if (top_justified) y += txt_h; 
	  // else               y -= (int) (txt_h * makers.length/2 + (txt_h/2) * (makers.length%2));
	  else {
	    if      (makers.length   == 1) y += txt_h/2;
	    else if (makers.length%2 == 0) y -= (makers.length/2 - 1)*txt_h;
	    else {
	      y += txt_h/2;
	      y -= (makers.length/2)*txt_h;
	    }
          }
	  // draw
          Shape shape = null;
	  for (int i=0;i<makers.length;i++) if (makers[i] != null) { 
	    if (makers[i].draw(g2d, bin, x, y) != null) { y += txt_h; if (i == 0) shape = makers[i].getShape(); }
	  }
          return shape;
	}

	/**
	 * Return the strings for each label maker.
	 *
	 *@param bin node coordinate to use for the bin
	 *
	 *@return array of strings for each label maker result
	 */
        public String[] toStrings(String bin) {
	  String strs[] = new String[makers.length];
	  for (int i=0;i<strs.length;i++) strs[i] = makers[i].toString(bin);
	  return strs;
	}

	/**
	 * Abstract class for making a label.
	 */
	abstract class LM { 
	  /**
	   * Shape of resulting label
	   */
	  Shape shape = null;

	  /**
	   * Render a label to the screen.
	   *
	   *@param g2d  graphics primitive
	   *@param bin  node coordinate for label
	   *@param x    x coordinate for label
	   *@param y    y coordinate for label
	   *
	   *@return color of label (hack to get the "color-by-label" to work)
	   */
	  public abstract Color draw(Graphics2D g2d, String bin, int x, int y); 

	  /**
	   * Return the string that will be rendered for the specified bin (node coordinate).
	   *
	   *@param bin node coordinate for label
	   *
	   *@return actual string to be rendered
	   */
          public abstract String toString(String bin);

	  /**
	   * Return the actual shape of the label.
           *
	   *@return label shape
	   */
          public Shape getShape() { return shape; }
        }

	/**
	 * Label for showing time information
	 */
	protected class TimeLM extends LM {
          static final int BOTH=0, FIRST=1, LAST=2; int type; public TimeLM(int type) { this.type = type; }
	  public Color draw(Graphics2D g2d, String bin, int x, int y) { 
	    long ts0 = Long.MAX_VALUE, 
	         ts1 = 0L;
	    for (int b=0;b<2;b++) {
              Iterator<Bundle> it;
	      // Do both directins of the link
	      if      (b == 0)                                  it = cc.getBundles(bin).iterator();
	      else if (isLink(bin) && isLink(reverseLink(bin))) it = cc.getBundles(reverseLink(bin)).iterator();
              else                                              it = (new HashSet<Bundle>()).iterator();
	      // Tally the packets
	      while (it.hasNext()) {
	        Bundle bundle = it.next(); if (bundle.hasTime()) {
	          if (bundle.ts0() < ts0) ts0 = bundle.ts0();
	          if (bundle.ts1() > ts1) ts1 = bundle.ts1();
                }
	      }
	    }
	    if (ts0 == Long.MAX_VALUE) return null; // No time bundles found
	    String ts0_str, ts1_str; Color ts0_color, ts1_color;

	    double ts0_fraction = ((double) ts0 - bs.ts0())/(bs.ts1() - bs.ts0()), ts1_fraction = ((double) ts1 - bs.ts0())/(bs.ts1() - bs.ts0());
	    if (ts0_fraction < 0.0) ts0_fraction = 0.0; if (ts0_fraction > 1.0) ts0_fraction = 1.0;
	    if (ts1_fraction < 0.0) ts1_fraction = 0.0; if (ts1_fraction > 1.0) ts1_fraction = 1.0;

	    switch (type) {
	      case 0: ts0_str = Utils.humanReadableDate(ts0);
	              ts1_str = Utils.humanReadableDate(ts1); if (g2d != null) { x -= Utils.txtW(g2d, ts0_str + " - " + ts1_str)/2; }
		      ts0_color = timing_marks_cs.at((float) ts0_fraction);
		      ts1_color = timing_marks_cs.at((float) ts1_fraction);
		      if (g2d != null) {
	                g2d.setColor(ts0_color);   g2d.drawString(ts0_str, x, y); x += Utils.txtW(g2d, ts0_str);
		        g2d.setColor(RTColorManager.getColor("label", "minor")); g2d.drawString(" - ",   x, y); x += Utils.txtW(g2d, " - ");
	                g2d.setColor(ts1_color);   g2d.drawString(ts1_str, x, y);
		      }
		      return ts0_color;
	      case 1: ts0_str = Utils.humanReadableDate(ts0);
	              if (g2d != null) { x -= Utils.txtW(g2d, ts0_str + " - ")/2; }
		      ts0_color = timing_marks_cs.at((float) ts0_fraction);
		      if (g2d != null) {
	                g2d.setColor(ts0_color);   g2d.drawString(ts0_str, x, y); x += Utils.txtW(g2d, ts0_str);
		        g2d.setColor(RTColorManager.getColor("label", "minor")); g2d.drawString(" - ",   x, y);
		      }
	              return ts0_color;
	      case 2: ts1_str = Utils.humanReadableDate(ts1);
	              if (g2d != null) { x -= Utils.txtW(g2d, " - " + ts1_str)/2; }
		      ts1_color = timing_marks_cs.at((float) ts1_fraction);
		      if (g2d != null) {
		        g2d.setColor(RTColorManager.getColor("label", "minor")); g2d.drawString(" - ",   x, y); x += Utils.txtW(g2d, " - ");
	                g2d.setColor(ts1_color);   g2d.drawString(ts1_str, x, y);
                      }
	              return ts1_color;
	    }
	    return null;
	  }

	  /**
	   *
	   */
	  public String toString(String bin) { return "Not Implemented (TimeLM)"; }
	}

	/**
	 * Label for showing node degree information
	 */
        protected class DegreeLM extends LM {
	  public Color draw(Graphics2D g2d, String bin, int x, int y) { return gen(g2d, bin, x, y).col; }
	  protected ColStr gen(Graphics2D g2d, String bin, int x, int y) {
            int deg = 0; ColStr colstr = new ColStr();
            if (node_coord_set.get(bin).size() == 1) {
              String entity = node_coord_set.get(bin).iterator().next();
              colstr.str = "" + digraph.getNumberOfNeighbors(digraph.getEntityIndex(entity)) + "/" + graph.getNumberOfNeighbors(graph.getEntityIndex(entity)) + " Nbors";
	      deg = graph.getNumberOfNeighbors(graph.getEntityIndex(entity));
            } else {
              Iterator<String> it_str = node_coord_set.get(bin).iterator();
              String  entity = it_str.next(); int min_di, max_di, min, max;
              min_di = max_di = digraph.getNumberOfNeighbors(digraph.getEntityIndex(entity));
              min    = max    = graph.getNumberOfNeighbors(graph.getEntityIndex(entity));
              while (it_str.hasNext()) { entity = it_str.next();
                if (min_di > digraph.getNumberOfNeighbors(digraph.getEntityIndex(entity))) min_di = digraph.getNumberOfNeighbors(digraph.getEntityIndex(entity));
                if (max_di < digraph.getNumberOfNeighbors(digraph.getEntityIndex(entity))) max_di = digraph.getNumberOfNeighbors(digraph.getEntityIndex(entity));
                if (min    > graph.getNumberOfNeighbors(graph.getEntityIndex(entity)))     min    = graph.getNumberOfNeighbors(graph.getEntityIndex(entity));
                if (max    < graph.getNumberOfNeighbors(graph.getEntityIndex(entity)))     max    = graph.getNumberOfNeighbors(graph.getEntityIndex(entity));
              }
              colstr.str = min_di + "/" + min + " to " + max_di + "/" + max + " Nbors";
	      deg = max;
            }
	    colstr.col = RTColorManager.getLogColor(deg);
	    if (g2d != null) shape = clearStr(g2d, colstr.str, x, y, colstr.col, RTColorManager.getColor("label", "defaultbg"), true);
            return colstr;
	  }

	  /**
	   *
	   */
	  public String toString(String bin) { return gen(null, bin, 0, 0).str; }
	}

	/**
	 * Label for showing underlying bundle (record) counts
	 */
        protected class BundleCountLM extends LM {
	  public Color draw(Graphics2D g2d, String bin, int x, int y) {
	    String str; int total;
            if (isLink(bin)) {
	      total = cc.getBundles(bin).size();
	      String revlink = reverseLink(bin);
	      if (isLink(revlink)) total += cc.getBundles(revlink).size();
	      str = "" + total + " Buns";
	    } else str = "" + (total = cc.getBundles(bin).size()) + " Buns";
	    Color color = RTColorManager.getLogColor(total);
	    if (g2d != null) shape = clearStr(g2d, str, x, y, color, RTColorManager.getColor("label", "defaultbg"), true);
	    return color;
	  }

	  /**
	   *
	   */
	  public String toString(String bin) { 
	    int total;
	    if (isLink(bin)) {
	      total = cc.getBundles(bin).size();
	      String revlink = reverseLink(bin);
	      if (isLink(revlink)) total += cc.getBundles(revlink).size();
	    } else total = cc.getBundles(bin).size();
	    return "" + total + " Buns";
	  }
	}

	/**
	 * Label for counting entities within aggregated nodes.
	 */
        protected class EntityCountLM extends LM {
	  public Color draw(Graphics2D g2d, String bin, int x, int y) {
	    int total;
            String str = "" + (total = node_coord_set.get(bin).size()) + " Ents";
	    Color color = RTColorManager.getLogColor(total);
	    if (g2d != null) shape = clearStr(g2d, str, x, y, color, RTColorManager.getColor("label", "defaultbg"), true);
	    return color;
	  }

	  /**
	   *
	   */
	  public String toString(String bin) { return "" + node_coord_set.get(bin).size() + " Ents"; }
	}

	/**
	 * Label for showing just the entity name.
	 */
        protected class EntityLM extends LM {
	  public Color draw(Graphics2D g2d, String bin, int x, int y) {
	    String str;
            if (node_coord_set.get(bin).size() == 1) str = node_coord_set.get(bin).iterator().next();
            else                                     str = Utils.calculateCIDR(node_coord_set.get(bin));
	    Color color = RTColorManager.getColor(str);
	    if (g2d != null) shape = clearStr(g2d, str, x, y, color, RTColorManager.getColor("label", "defaultbg"), true);
	    return color;
	  }

	  public String toString(String bin) { 
            if (node_coord_set.get(bin).size() == 1) return node_coord_set.get(bin).iterator().next();
            else                                     return Utils.calculateCIDR(node_coord_set.get(bin));
	  }
	}

	/**
	 * Generic caching label maker for handling sets of labels.
	 */
        protected abstract class CacheLM extends LM {
          String fld; int fld_i; public CacheLM(String fld) { this.fld = fld; fld_i = getRTParent().getRootBundles().getGlobals().fieldIndex(fld); }
          Map<Tablet,KeyMaker> km_lu    = new HashMap<Tablet,KeyMaker>();
          Set<Tablet>          ignore   = new HashSet<Tablet>();
          //
          public String[] stringKeys(Bundle bundle) {
            if (ignore.contains(bundle.getTablet())) return null;
            if (km_lu.containsKey(bundle.getTablet()) == false) {
              // System.err.println("CacheLM.stringKeys(): fld = \"" + fld + "\"");
              if (KeyMaker.tabletCompletesBlank(bundle.getTablet(), fld)) {
                km_lu.put(bundle.getTablet(), new KeyMaker(bundle.getTablet(), fld));
              } else { ignore.add(bundle.getTablet()); return null; }
            }
            return km_lu.get(bundle.getTablet()).stringKeys(bundle);
            }
          //
          public int[]    intKeys(Bundle bundle) {
          if (ignore.contains(bundle.getTablet())) return null;
          if (km_lu.containsKey(bundle.getTablet()) == false) {
            if (KeyMaker.tabletCompletesBlank(bundle.getTablet(), fld)) {
              km_lu.put(bundle.getTablet(), new KeyMaker(bundle.getTablet(), fld));
            } else { ignore.add(bundle.getTablet()); return null; }
          }
          return km_lu.get(bundle.getTablet()).intKeys(bundle);
          }
	}

	/**
	 * Simple statistic labels (min/max/sum).
	 */
        protected class SimpleStatLM extends CacheLM {
	  boolean single; // Used to denote that the field header should not be displayed
	  public SimpleStatLM(String fld, boolean single) { super(fld); this.single = single; }
	  public Color draw(Graphics2D g2d, String bin, int x, int y) {
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE; long sum = 0L;
	    for (int b=0;b<2;b++) {
              Iterator<Bundle> it;
	      // Do both directins of the link
	      if      (b == 0)                                  it = cc.getBundles(bin).iterator();
	      else if (isLink(bin) && isLink(reverseLink(bin))) it = cc.getBundles(reverseLink(bin)).iterator();
              else                                              it = (new HashSet<Bundle>()).iterator();
	      // Tally the packets
              while (it.hasNext()) {
	        int ints[] = intKeys(it.next()); if (ints == null) continue;
	        for (int i=0;i<ints.length;i++) {
	          if (min > ints[i]) min = ints[i];
		  if (max < ints[i]) max = ints[i];
		  sum += ints[i];
	        }
	      }
            }
            String str = (single ? "" : fld + " ") + min + "/" + max + "/" + sum;
	    if (g2d != null) {
	      x -= Utils.txtW(g2d, str)/2;
              if (single == false) {
	        g2d.setColor(RTColorManager.getColor("label", "default")); g2d.drawString(fld + " ", x, y); x += Utils.txtW(g2d, fld + " ");
              }
	      g2d.setColor(RTColorManager.getColor("data",  "min"));     g2d.drawString("" + min,  x, y); x += Utils.txtW(g2d, "" + min);
	      g2d.setColor(RTColorManager.getColor("label", "minor"));   g2d.drawString("/",       x, y); x += Utils.txtW(g2d, "/");
	      g2d.setColor(RTColorManager.getColor("data",  "max"));     g2d.drawString("" + max,  x, y); x += Utils.txtW(g2d, "" + max);
	      g2d.setColor(RTColorManager.getColor("label", "minor"));   g2d.drawString("/",       x, y); x += Utils.txtW(g2d, "/");
	      g2d.setColor(RTColorManager.getColor("data",  "sum"));     g2d.drawString("" + sum,  x, y);
            }
	    return RTColorManager.getLogColor(sum);
	  }
	  public String toString(String bin) { return "Not Implemented (SimpleStatsLM)"; }
	}

	/**
	 * Complex stats label maker (mean/avg/stdev).
	 */
        protected class ComplexStatLM extends CacheLM {
	  boolean single; // Used to denote that the field header should not be displayed
	  public ComplexStatLM(String fld, boolean single) { super(fld); this.single = single; }
	  public Color draw(Graphics2D g2d, String bin, int x, int y) {
	    List<Integer> al = new ArrayList<Integer>(); double sum = 0.0;
	    for (int b=0;b<2;b++) {
              Iterator<Bundle> it;
	      // Do both directins of the link
	      if      (b == 0)                                  it = cc.getBundles(bin).iterator();
	      else if (isLink(bin) && isLink(reverseLink(bin))) it = cc.getBundles(reverseLink(bin)).iterator();
              else                                              it = (new HashSet<Bundle>()).iterator();
	      // Tally the packets
              while (it.hasNext()) {
	        int ints[] = intKeys(it.next()); if (ints == null) continue;
	        for (int i=0;i<ints.length;i++) { al.add(ints[i]); sum += ints[i]; }
              }
	    }
	    if (al.size() == 0) return null;
	    double avg   = sum/al.size();
	    double stdev = Utils.calculateStandardDeviation(al,avg);
	    Collections.sort(al);
	    int   median   = al.get(al.size()/2);

	    String median_str = "" + median,
	           avg_str    = Utils.humanReadableDouble(avg),
		   stdev_str  = Utils.humanReadableDouble(stdev);

            String str = (single ? "" : fld + " ") + median_str + "/" + avg_str + "/" + stdev_str;
	    if (g2d != null) {
	      x -= Utils.txtW(g2d, str)/2;
              if (single == false) {
	        g2d.setColor(RTColorManager.getColor("label", "default")); g2d.drawString(fld + " ",        x, y); x += Utils.txtW(g2d, fld + " | ");
              }
	      g2d.setColor(RTColorManager.getColor("data",  "median"));  g2d.drawString("" + median_str,  x, y); x += Utils.txtW(g2d, median_str);
	      g2d.setColor(RTColorManager.getColor("label", "minor"));   g2d.drawString("/",              x, y); x += Utils.txtW(g2d, "/");
	      g2d.setColor(RTColorManager.getColor("data",  "mean"));    g2d.drawString("" + avg_str,     x, y); x += Utils.txtW(g2d, avg_str);
	      g2d.setColor(RTColorManager.getColor("label", "minor"));   g2d.drawString("/",            x, y); x += Utils.txtW(g2d, "/");
	      g2d.setColor(RTColorManager.getColor("label", "stdev"));   g2d.drawString("" + stdev_str, x, y);
            }
	    return RTColorManager.getLogColor(avg);
	  }
	  public String toString(String bin) { return "Not Implemented (ComplexStatsLM)"; }
	}

	/**
	 * Label maker for handling sets of labels.
	 */
        protected class ItemsLM extends CacheLM {
	  boolean single; // Used to denote that the field header should not be displayed
	  public ItemsLM(String fld, boolean single) { super(fld); this.single = single;}
	  public Color draw(Graphics2D g2d, String bin, int x, int y) {
            Map<String,Integer> counter = new HashMap<String,Integer>();
	    // Count elements
	    for (int b=0;b<2;b++) {
              Iterator<Bundle> it;
	      // Do both directins of the link
	      if      (b == 0)                                  it = cc.getBundles(bin).iterator();
	      else if (isLink(bin) && isLink(reverseLink(bin))) it = cc.getBundles(reverseLink(bin)).iterator();
              else                                              it = (new HashSet<Bundle>()).iterator();
	      // Tally the packets
	      while (it.hasNext()) {
	        String strs[] = stringKeys(it.next());
	        if (strs != null) { 
	          for (int i=0;i<strs.length;i++) {
		    if (counter.containsKey(strs[i]) == false) counter.put(strs[i], 1);
		    else                                       counter.put(strs[i], counter.get(strs[i]) + 1);
		  }
	        }
	      }
            }
	    // Place them in an array and sort
            StrCountSorter sorter[] = new StrCountSorter[counter.keySet().size()];
	    Iterator<String> it_str = counter.keySet().iterator();
	    for (int i=0;i<sorter.length;i++) {
	      String str = it_str.next(); sorter[i] = new StrCountSorter(str, counter.get(str));
	    }
	    Arrays.sort(sorter);
            // Put the three most common
	    StringBuffer sb = new StringBuffer(); if (!single) sb.append(fld + " | ");
	    // for (int i=0;i<((sorter.length>2)?2:sorter.length);i++) sb.append(sorter[i].toString() + " (" + sorter[i].count() + ") ");
	    for (int i=0;i<((sorter.length>2)?2:sorter.length);i++) sb.append(sorter[i].toString() + " ");
	    if (sorter.length > 2) sb.append("... [" + sorter.length + " To]");
	    // Draw the string
            if (g2d != null && sb.toString().equals(BundlesDT.NOTSET + " ") == false) shape = clearStr(g2d, sb.toString(), x, y, RTColorManager.getColor(fld), RTColorManager.getColor("label", "defaultbg"), true);
	    return (sorter.length == 1 ? RTColorManager.getColor(sorter[0].toString()) : RTColorManager.getColor("set", "multi"));
	  }
	  public String toString(String bin) { return "Not Implemented (ItemsLM)"; }
	}

	/**
	 * Label maker for counting the size of sets.
	 */
        protected class CountLM extends CacheLM {
	  boolean single; // Used to denote that the field header should not be displayed
	  public CountLM(String fld, boolean single) { super(fld); this.single = single; }
	  public Color draw(Graphics2D g2d, String bin, int x, int y) {
	    Set<String> set = new HashSet<String>();
	    for (int b=0;b<2;b++) {
              Iterator<Bundle> it;
	      // Do both directins of the link
	      if      (b == 0)                                  it = cc.getBundles(bin).iterator();
	      else if (isLink(bin) && isLink(reverseLink(bin))) it = cc.getBundles(reverseLink(bin)).iterator();
              else                                              it = (new HashSet<Bundle>()).iterator();
	      // Tally the packets
	      while (it.hasNext()) {
	        String strs[] = stringKeys(it.next());
	        if (strs != null) { for (int i=0;i<strs.length;i++) set.add(strs[i]); }
	      }
            }
	    if (g2d != null) shape = clearStr(g2d, (single ? "" : fld + " | ") + set.size() + " Els", 
	                                      x, y, RTColorManager.getColor(fld), RTColorManager.getColor("label", "defaultbg"), true);
	    return RTColorManager.getLogColor(set.size());
	  }
	  public String toString(String bin) { return "Not Implemented (CountLM)"; }
	}

	/**
	 * Label maker for tag fields.
	 */
        protected class TagsLM extends LM {
	  public Color draw(Graphics2D g2d, String bin, int x, int y) { return gen(g2d, bin, x, y).col; }
	  protected ColStr gen(Graphics2D g2d, String bin, int x, int y) {
            // Initialize variables
	    StringBuffer sb = new StringBuffer(); Iterator<String> it, it_tags; ColStr colstr = new ColStr();
	    long ts0 = bs.ts0(), ts1 = bs.ts1();

	    // Determine if one entity or many
            if (node_coord_set.get(bin).size() == 1) {
	      Set<String> tags = getRTParent().getEntityTags(node_coord_set.get(bin).iterator().next(),ts0,ts1);
	      it_tags = tags.iterator();
	      while (it_tags.hasNext()) { if (sb.length() > 0) sb.append(BundlesDT.DELIM); sb.append(it_tags.next()); }
	      colstr.col = RTColorManager.getColor(sb.toString());
            } else                                   {
	      // Go through the entities, accumulating the tags
	      int entity_count = node_coord_set.get(bin).size();
	      it = node_coord_set.get(bin).iterator();  Map<String,Integer> map = new HashMap<String,Integer>();
	      while (it.hasNext()) {
	        String          entity = it.next();
		Set<String> tags   = getRTParent().getEntityTags(entity, ts0, ts1);
		// Count the tags if they exist
		if (tags != null && tags.size() > 0) {
		  it_tags = tags.iterator();
		  while (it_tags.hasNext()) {
		    String tag = it_tags.next();
		    if (map.containsKey(tag) == false) map.put(tag, 1); else map.put(tag, map.get(tag)+1);
		  }
		}
	      }
              // put the string buffer together
	      if        (map.keySet().size() == 1) {
	        it = map.keySet().iterator(); String tag = it.next(); int count = map.get(tag);
		if (count == entity_count) sb.append(tag); else sb.append(tag + " (" + count + ")");
		colstr.col = RTColorManager.getColor(tag);
	      } else if (map.keySet().size() >= 2) {
	        // Sort the counts
	        StrCountSorter sorter[] = new StrCountSorter[map.keySet().size()];
	        it = map.keySet().iterator();
	        for (int i=0;i<sorter.length;i++) {
                  String tag = it.next();
		  sorter[i] = new StrCountSorter(tag, map.get(tag));
                }
                Arrays.sort(sorter);
		// Add the first two to the string
		sb.append(sorter[0].toString()); if (sorter[0].count() != entity_count) sb.append(" (" + sorter[0].count() + ")");
		sb.append(" " + BundlesDT.DELIM + " ");
		sb.append(sorter[1].toString()); if (sorter[1].count() != entity_count) sb.append(" (" + sorter[1].count() + ")");
		if (sorter.length > 2) sb.append(" ... [" + sorter.length + " To]");
		colstr.col = RTColorManager.getColor("set", "multi");
              }
            }
	    if (sb.length() > 0) {
	      colstr.str = sb.toString();
	      if (g2d != null) shape = clearStr(g2d, colstr.str, x, y, RTColorManager.getColor("label", "default"), RTColorManager.getColor("label", "defaultbg"), true);
            }
	    return colstr;
	  }
	  public String toString(String bin) { return gen(null, bin, 0, 0).str; }
	}

	/**
	 * Label maker for a specific tag type in type-value pairs.
	 */
        protected class TagTypesLM extends LM {
	  String tag_type;
	  public TagTypesLM(String tag_type) { this.tag_type = tag_type; }
	  public Color draw(Graphics2D g2d, String bin, int x, int y) { return gen(g2d, bin, x, y).col; }
	  protected ColStr gen(Graphics2D g2d, String bin, int x, int y) {
            // Initialize variables
	    StringBuffer sb = new StringBuffer(); Iterator<String> it, it_tags; ColStr colstr = new ColStr();
	    long ts0 = bs.ts0(), ts1 = bs.ts1();

	    // Determine if one entity or many
            if (node_coord_set.get(bin).size() == 1) {
              int count = 0; // Keep track of the number of tags for the color
              // Get the tags for this entity and iterate over them
	      Set<String> tags = getRTParent().getEntityTags(node_coord_set.get(bin).iterator().next(),ts0,ts1);
              // For each tag, determine if it matches the specified type -- if so, accumulate them into a string buffer
	      it_tags = tags.iterator();
	      while (it_tags.hasNext()) { 
	        String tag = it_tags.next();
		if (Utils.tagIsTypeValue(tag)) {
		  String sep[] = Utils.separateTypeValueTag(tag);
		  if (sep[0].equals(tag_type)) {
	            if (sb.length() > 0) sb.append(BundlesDT.DELIM); 
		    sb.append(sep[1]); count++;
                  }
                }
	      }
              // Pick the color based on the number of types assigned to this entity
              if      (count == 0) colstr.col = RTColorManager.getColor("label", "minor");
              else if (count == 1) colstr.col = RTColorManager.getColor(sb.toString());
              else                 colstr.col = RTColorManager.getColor("set",  "multi");
              // Assign the string
	      colstr.str = sb.toString();
            } else                                   {
	      // Go through the entities, accumulating the tags
	      int entity_count = node_coord_set.get(bin).size();
	      it = node_coord_set.get(bin).iterator();  Map<String,Integer> map = new HashMap<String,Integer>();
	      while (it.hasNext()) {
	        String      entity = it.next();
		Set<String> tags   = getRTParent().getEntityTags(entity, ts0, ts1);
		// Count the tags if they exist
		if (tags != null && tags.size() > 0) {
		  it_tags = tags.iterator();
		  while (it_tags.hasNext()) {
		    String tag = it_tags.next();
		    if (Utils.tagIsTypeValue(tag)) {
		      String sep[] = Utils.separateTypeValueTag(tag);
		      if (sep[0].equals(tag_type)) {
		        if (map.containsKey(sep[1]) == false) map.put(sep[1], 1); else map.put(sep[1], map.get(sep[1])+1);
                      }
                    }
		  }
		}
	      }
              // put the string buffer together
	      if        (map.keySet().size() == 1) {
	        it = map.keySet().iterator(); String tag = it.next(); int count = map.get(tag);
		if (count == entity_count) sb.append(tag); else sb.append(tag + " (" + count + ")");
		colstr.col = RTColorManager.getColor(tag);
	      } else if (map.keySet().size() >= 2) {
	        // Sort the counts
	        StrCountSorter sorter[] = new StrCountSorter[map.keySet().size()];
	        it = map.keySet().iterator();
	        for (int i=0;i<sorter.length;i++) {
                  String tag = it.next();
		  sorter[i] = new StrCountSorter(tag, map.get(tag));
                }
                Arrays.sort(sorter);
		// Add the first two to the string
		sb.append(sorter[0].toString()); if (sorter[0].count() != entity_count) sb.append(" (" + sorter[0].count() + ")");
		sb.append(" " + BundlesDT.DELIM + " ");
		sb.append(sorter[1].toString()); if (sorter[1].count() != entity_count) sb.append(" (" + sorter[1].count() + ")");
		if (sorter.length > 2) sb.append(" ... [" + sorter.length + " To]");
		colstr.col = RTColorManager.getColor("set", "multi");
              }
              colstr.str = sb.toString();
            }
	    if (sb.length() > 0) {
	      colstr.str = sb.toString();
	      if (g2d != null) shape = clearStr(g2d, colstr.str, x, y, colstr.col, RTColorManager.getColor("label", "defaultbg"), true);
            }
	    return colstr;
	  }
	  public String toString(String bin) { return gen(null, bin, 0, 0).str; }
	}

        /**
	 * Label maker for post processed information.
	 */
        protected class LinkPostLM extends CacheLM {
	  PostProc proc; boolean single;
	  public LinkPostLM(String field, String postproc, BundlesG globals, boolean single) { 
            super(field); proc = BundlesDT.createPostProcessor(postproc, globals); this.single = single; }
	  public Color draw(Graphics2D g2d, String bin, int x, int y) {
            Map<String,Integer> counter = new HashMap<String,Integer>(); int no_match = 0;
	    // Count elements
	    for (int b=0;b<2;b++) {
              Iterator<Bundle> it;
	      // Do both directins of the link
	      if      (b == 0)                                  it = cc.getBundles(bin).iterator();
	      else if (isLink(bin) && isLink(reverseLink(bin))) it = cc.getBundles(reverseLink(bin)).iterator();
              else                                              it = (new HashSet<Bundle>()).iterator();
	      // Tally the packets
	      while (it.hasNext()) {
	        String strs[] = stringKeys(it.next());
	        if (strs != null) { 
	          for (int i=0;i<strs.length;i++) {
		    if (BundlesDT.getEntityDataType(strs[i]) != proc.type()) { no_match++; }
		    String post[] = proc.postProcess(strs[i]);
		    for (int j=0;j<post.length;j++) {
		      if (counter.containsKey(post[j]) == false) counter.put(post[j], 1);
		      else                                       counter.put(post[j], counter.get(post[j]) + 1);
                    }
		  }
	        }
	      }
            }
	    // Place them in an array and sort
            StrCountSorter sorter[] = new StrCountSorter[counter.keySet().size()];
	    Iterator<String> it_str = counter.keySet().iterator();
	    for (int i=0;i<sorter.length;i++) {
	      String str = it_str.next(); sorter[i] = new StrCountSorter(str, counter.get(str));
	    }
	    Arrays.sort(sorter);
            // Put the three most common
	    StringBuffer sb = new StringBuffer(); if (single == false) sb.append(fld + " | ");
	    // for (int i=0;i<((sorter.length>2)?2:sorter.length);i++) sb.append(sorter[i].toString() + " (" + sorter[i].count() + ") ");
	    for (int i=0;i<((sorter.length>2)?2:sorter.length);i++) sb.append(sorter[i].toString() + " ");
	    if (sorter.length > 2) sb.append("... [" + sorter.length + " To]");
	    if (no_match > 0) sb.append(" [" + no_match + " NoM]");
	    // Draw the string
	    Color color = (sorter.length == 1 ? RTColorManager.getColor(sorter[0].toString()) : RTColorManager.getColor("set", "multi"));
            if (g2d != null) shape = clearStr(g2d, sb.toString(), x, y, color, RTColorManager.getColor("label", "defaultbg"), true);
	    return color;
	  }
	  public String toString(String bin) { return "Not Implemented (LinkPostLM)"; }
	}

	/**
	 * Label maker for post processed node information.
	 */
        protected class NodePostLM extends LM {
	  PostProc proc;
	  public NodePostLM(String postproc, BundlesG globals) { proc = BundlesDT.createPostProcessor(postproc, globals); }
	  public Color draw(Graphics2D g2d, String bin, int x, int y) { return gen(g2d, bin, x, y).col; }
	  protected ColStr gen(Graphics2D g2d, String bin, int x, int y) {
            ColStr colstr = new ColStr();
            if (node_coord_set.get(bin).size() == 1) {
	      colstr.str = node_coord_set.get(bin).iterator().next();
	      if (BundlesDT.getEntityDataType(colstr.str) == proc.type()) {
	        String       post[] = proc.postProcess(colstr.str);
		if (post == null || post.length == 0 || (post.length == 1 && post[0].equals(BundlesDT.NOTSET))) return colstr;
		StringBuffer sb = new StringBuffer();
		sb.append(post[0]); for (int i=1;i<post.length;i++) sb.append(BundlesDT.DELIM + post[i]);
		colstr.str = sb.toString(); colstr.col = RTColorManager.getColor(colstr.str);
	      }  else return colstr;
            } else                                   {
              Map<String,Integer> counter = new HashMap<String,Integer>(); int no_match = 0;
              Iterator<String> it = node_coord_set.get(bin).iterator();
	      while (it.hasNext()) {
	        colstr.str = it.next();
	        if (BundlesDT.getEntityDataType(colstr.str) == proc.type()) {
		  String post[] = proc.postProcess(colstr.str);
		  for (int i=0;i<post.length;i++) {
                    if (counter.containsKey(post[i])) counter.put(post[i],counter.get(post[i])+1); else counter.put(post[i],1);
                  }
                } else no_match++;
              }
	      // Place them in an array and sort
              StrCountSorter sorter[] = new StrCountSorter[counter.keySet().size()];
	      Iterator<String> it_str = counter.keySet().iterator();
	      for (int i=0;i<sorter.length;i++) {
	        colstr.str = it_str.next(); sorter[i] = new StrCountSorter(colstr.str, counter.get(colstr.str));
	      }
	      Arrays.sort(sorter);
              // Put the three most common
	      StringBuffer sb = new StringBuffer();
	      for (int i=0;i<((sorter.length>2)?2:sorter.length);i++) sb.append(sorter[i].toString() + " (" + sorter[i].count() + ") ");
	      if (sorter.length > 2) sb.append("... [" + sorter.length + "]");
	      if (no_match > 0 && sb.length() > 0) sb.append(" + " + no_match + "NoM"); // Only add no-match when there's a string...
	      colstr.str = sb.toString();
	      if (sorter.length == 1) colstr.col = Utils.strColor(sorter[0].toString()); else colstr.col = RTColorManager.getColor("set", "multi");
            }
	    if (g2d != null) shape = clearStr(g2d, colstr.str, x, y, colstr.col, RTColorManager.getColor("label", "defaultbg"), true);
	    return colstr;
	  }
	  public String toString(String bin) { return gen(null, bin, 0, 0).str; }
	}

        /**
         * Simple structure to hold both a string and a color.
         */
        private class ColStr { public String str; public Color col; public ColStr() { str = "0"; col = RTColorManager.getColor("label", "minor"); } }

	/**
	 * From a label string, return the appropriate label maker.
	 *
	 *@param label labeling option
	 *
	 *@return label maker
	 */
	protected LM createLM(String label, boolean single) {
          BundlesG globals = getRTParent().getRootBundles().getGlobals();
          if      (label.equals(TIMEFRAME_LM))     return new TimeLM(TimeLM.BOTH);
	  else if (label.equals(FIRSTHEARD_LM))    return new TimeLM(TimeLM.FIRST);
	  else if (label.equals(LASTHEARD_LM))     return new TimeLM(TimeLM.LAST);
	  else if (label.equals(DEGREE_LM))        return new DegreeLM();
          else if (label.equals(BUNDLECOUNT_LM))   return new BundleCountLM();
	  else if (label.equals(TAGS_LM))          return new TagsLM();
	  else if (label.startsWith(TAG_TYPE_LM))  return new TagTypesLM(label.substring(TAG_TYPE_LM.length(),label.length()));
	  else if (label.equals(ENTITYCOUNT_LM))   return new EntityCountLM();
	  else if (label.equals(ENTITY_LM))        return new EntityLM();
          else if (label.endsWith(SIMPLESTAT_LM))  return new SimpleStatLM(label.substring(0,label.indexOf(BundlesDT.DELIM)),  single);
	  else if (label.endsWith(COMPLEXSTAT_LM)) return new ComplexStatLM(label.substring(0,label.indexOf(BundlesDT.DELIM)), single);
	  else if (label.endsWith(ITEMS_LM))       return new ItemsLM(label.substring(0,label.lastIndexOf(BundlesDT.DELIM)),       single);
	  else if (label.endsWith(COUNT_LM))       return new CountLM(label.substring(0,label.lastIndexOf(BundlesDT.DELIM)),       single);
	  else if (label.indexOf(BundlesDT.DELIM) >= 0) {
	    String   start   = label.substring(0,label.indexOf(BundlesDT.DELIM)),
	             rest    = label.substring(label.indexOf(BundlesDT.DELIM)+1,label.length());
            if (globals.fieldIndex(start) != -1) return new LinkPostLM(start,rest,globals,single);
	    else                                 return new NodePostLM(label,globals);
	  } else {
            return new NodePostLM(label,globals);
	  }
	}
      }
    }
  }

  /**
   * Strings for the variety of non-application specific labeling options.
   */
  public final static String TIMEFRAME_LM   = BundlesDT.DELIM + "Time Frame"   + BundlesDT.DELIM, // Bundle Set  <= CounterContext <= Bin
                             LASTHEARD_LM   = BundlesDT.DELIM + "Last Heard"   + BundlesDT.DELIM, // Bundle Set  <= CounterContext <= Bin
                             FIRSTHEARD_LM  = BundlesDT.DELIM + "First Heard"  + BundlesDT.DELIM, // Bundle Set  <= CounterContext <= Bin
                             DEGREE_LM      = BundlesDT.DELIM + "Degree"       + BundlesDT.DELIM, // Graph       <= entities       <= Node String
                             BUNDLECOUNT_LM = BundlesDT.DELIM + "Bundle Count" + BundlesDT.DELIM, // Bundle Set  <= CounterContext <= Bin
			     TAGS_LM        = BundlesDT.DELIM + "All Tags"     + BundlesDT.DELIM,
			     TAG_TYPE_LM    = BundlesDT.DELIM + "Tag Type"     + BundlesDT.DELIM,
                             ENTITYCOUNT_LM = BundlesDT.DELIM + "Entity Count" + BundlesDT.DELIM, // Node String (bin)
                             ENTITY_LM      = BundlesDT.DELIM + "Entity"       + BundlesDT.DELIM, // Node String (bin)
			     SIMPLESTAT_LM  = BundlesDT.DELIM + "Min/Max/Sum",                    // Bundle Set  <= CounterContext <= Bin
			     COMPLEXSTAT_LM = BundlesDT.DELIM + "Med/Avg/StDev",                  // Bundle Set  <= CounterContext <= Bin
			     ITEMS_LM       = BundlesDT.DELIM + "Items",                          // Bundle Set  <= CounterContext <= Bin
			     COUNT_LM       = BundlesDT.DELIM + "Count";                          // Bundle Set  <= CounterContext <= Bin


  /**
   * Dialog to display fast layout options so that user can more quickly select an initial layout.
   */
  class LayoutSmallMultiplesDialog extends JDialog implements WindowListener {
    // List of the layout threads to split the work
    List<LayoutThread> layout_threads = new ArrayList<LayoutThread>();

    // Viewer for layouts
    ViewerComponent    viewer;

    /**
     * Dimensions of the subimages
     */
    final int w = 256, h = 256;

    /**
     * Constructor
     */
    public LayoutSmallMultiplesDialog() {
      super(getRTParent(), "Small Multiples Layouts", true);

      // Execute the efficient layout options
      String layouts[] = GraphLayouts.getLayoutAlgorithms();
      for (int i=0;i<layouts.length;i++) {
        String layout = layouts[i]; if (GraphLayouts.layoutEfficient(layout, graph)) {
          LayoutThread layout_thread; layout_threads.add(layout_thread = new LayoutThread(layout, w, h)); (new Thread(layout_thread)).start();
	}
      }

      // Wait until they finish...
      Iterator<LayoutThread> it = layout_threads.iterator(); while (it.hasNext()) {
        LayoutThread layout_thread = it.next(); while (layout_thread.done == false) { try { Thread.sleep(100); } catch (InterruptedException ie) { } }
      }

      // Create the actual viewer
      JScrollPane scroll_pane; add("Center", scroll_pane = new JScrollPane(viewer = new ViewerComponent()));
      scroll_pane.getVerticalScrollBar().setUnitIncrement(32);
      pack(); setSize(1024+21,768+10); setVisible(true);

      addWindowListener(this);
    }

    /**
     * Close the dialog... dispose of the resources.
     */
    protected void closeDialog() { setVisible(false); dispose(); }

    /**
     * Window Listener
     */
    public void windowDeactivated(WindowEvent we) { } 
    public void windowActivated  (WindowEvent we) { } 
    public void windowDeiconified(WindowEvent we) { } 
    public void windowIconified  (WindowEvent we) { } 
    public void windowClosed     (WindowEvent we) { } 
    public void windowClosing    (WindowEvent we) { this.dispose(); }  // Not sure if this is really needed or not...
    public void windowOpened     (WindowEvent we) { } 

    /**
     * Thread to split each layout across multiple cores
     */
    class LayoutThread implements Runnable {
      String layout; int w, h; BufferedImage bi; boolean done = false; Map<String,Point2D> e2xy = new HashMap<String,Point2D>();

      // Construct -- really just save off the variables
      public LayoutThread(String layout, int w, int h) { 
        this.layout = layout; this.w = w; this.h = h; 
      }

      // Worker thread
      public void run() {
        try {
	  // Copy the current layout (it may be relevant to the layout...  not usually though)
          Iterator<String> it = entity_to_wxy.keySet().iterator(); while (it.hasNext()) { String e = it.next(); e2xy.put(e, entity_to_wxy.get(e)); }
	  // Execute the layout
          RTGraphComponent.RenderContext myrc = (RTGraphComponent.RenderContext) (getRTComponent().getRTRenderContext()); if (myrc == null) return;
          (new GraphLayouts()).executeLayoutAlgorithm(layout, graph, myrc.filterEntities(getRTParent().getSelectedEntities()), e2xy);
	  // Render to an image
          bi = GraphUtils.render(new UniGraph(graph), e2xy);
	  // Clear the done flag
	} finally { done = true; }
      }
    }

    /**
     * Simple viewer to show the various layouts.
     */
    class ViewerComponent extends JComponent implements MouseListener, MouseMotionListener {
      // Keep track of the placement of the layout renderings
      Map<LayoutThread,Rectangle2D> map = new HashMap<LayoutThread,Rectangle2D>();
      int mouse_x = -1, mouse_y = -1;
      // Construct the viewer...  get the dimensions right for the scroll pane
      public ViewerComponent() {
        int images = layout_threads.size();
        int comp_w = 4*w, comp_h = (h*images)/4; if ((images%4) != 0) comp_h += h;
        Dimension dimension = new Dimension(comp_w, comp_h); setPreferredSize(dimension); setMinimumSize(dimension);
        addMouseListener(this); addMouseMotionListener(this);
      }
      // Render by pasting the various images into the view - keep track of their locations for selection
      public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g; g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x = 0, y = 0; Iterator<LayoutThread> it = layout_threads.iterator(); while (it.hasNext()) {
	  LayoutThread layout_thread = it.next();
	  if (layout_thread.bi != null) { 
            g2d.drawImage(layout_thread.bi, x*w, y*h, w, h, Color.white, null); 
            if (GraphLayouts.layoutStable(layout_thread.layout) == false) {
              g2d.setColor(Color.red);
              g2d.drawOval(x*w+4,y*w+4,4,4);
            }
            map.put(layout_thread, new Rectangle2D.Double(x*w,y*h,w,h));
            // if the mouse is in this one, write the label beneath the image
            if (map.get(layout_thread).contains(mouse_x, mouse_y)) {
              if (GraphLayouts.layoutStable(layout_thread.layout)) g2d.setColor(Color.black);
              else                                                 g2d.setColor(Color.red);
              g2d.drawString(layout_thread.layout, (int) ((x*w + w/2) - Utils.txtW(g2d, layout_thread.layout)/2.0), (int) (y*h + h - 2));
            }
          }
	  x++; if (x >= 4) { x = 0; y++; }
	}
      }
      // Helper to find layout thread
      public LayoutThread find(int x, int y) {
        Iterator<LayoutThread> it = map.keySet().iterator(); while (it.hasNext()) {
          LayoutThread layout_thread = it.next(); if (map.get(layout_thread).contains(x,y)) return layout_thread;
        }
        return null;
      }
      // Listener for mouse motion to write the layout name beneath the image
      public void mouseMoved    (MouseEvent me) { mouse_x = me.getX(); mouse_y = me.getY(); repaint(); }
      public void mouseDragged  (MouseEvent me) { }

      // Listener to user input to select the correct layout
      public void mouseEntered  (MouseEvent me) { }
      public void mouseExited   (MouseEvent me) { }
      public void mousePressed  (MouseEvent me) { }
      public void mouseReleased (MouseEvent me) { }
      public void mouseClicked  (MouseEvent me) { 
        if (me.getButton() == MouseEvent.BUTTON1) { // Select the layout and apply to the parent panel
          LayoutThread layout_thread = find(me.getX(), me.getY());
          if (layout_thread != null) {
            entity_to_wxy = layout_thread.e2xy; closeDialog(); 
            transform(); zoomToFit(); getRTComponent().render(); // All of these shouldn't be required to get the component to re-render :(
          }
        } else { // Re-run the layout if the layout is an unstable approach
          LayoutThread layout_thread = find(me.getX(), me.getY());
          if (layout_thread != null && GraphLayouts.layoutStable(layout_thread.layout) == false) {
            layout_thread.run();
            repaint();
          }
        }
      }
    }
  }

  /**
   * Dialog to construct a graph pattern to search.
   */
  class IsomorphismPatternCreatorDialog extends JDialog {
    /**
     * Node Template - describes a node pattern
     */
    class NodeTemplate {
      Set<Utils.Symbol> symbol_req     = new HashSet<Utils.Symbol>();
      int               degree_min     = -1, // This should be consistent with the described pattern
                        degree_max     = -1, // Ditto
			bundles_min    = -1,
			bundles_max    = -1;
      Pattern           entity_pattern = null;
    }

    /**
     * Edge Template - describes an edge pattern
     *
     * edge pattern example:
     *
     * ((source in {x,y}) or (octs < 20)) and (spkt+dpkt > 5) or (dpt in {53,80})
     *
     * precedence:  {}
     *              ()
     *              in
     *              * /
     *              + -
     *              > < <= >= = <>
     *              !
     *              and or xor
     */
    class EdgeTemplate {
      NodeTemplate src_node,
                   dst_node;
      boolean      match_dir    = false; // if true, src must match src and dst must match dst
      int          bundles_min  = -1,
                   bundles_max  = -1;
      String       edge_pattern = "";
    }

    /**
     *
     */
    public IsomorphismPatternCreatorDialog() {
      super(getRTParent(), "Isomorphism Pattern Creator", false);
      add("Center", new CreatorComponent());
      setVisible(true);
    }

    /**
     *
     */
    class CreatorComponent extends JComponent {
      public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
      }
    }
  }
}

