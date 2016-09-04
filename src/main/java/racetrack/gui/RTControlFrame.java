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
import java.awt.BorderLayout;
import java.awt.Composite;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesG;
import racetrack.framework.BundlesDT;
import racetrack.framework.KeyMaker;
import racetrack.framework.RFC4180Importer;
import racetrack.framework.Tablet;
import racetrack.kb.EntityTag;
import racetrack.kb.RTComment;
import racetrack.util.CacheManager;
import racetrack.util.CaseInsensitiveComparator;
import racetrack.util.CSVTokenConsumer;
import racetrack.util.JREMemComponent;
import racetrack.util.JTextFieldHistory;
import racetrack.util.RFC4180CSVReader;
import racetrack.util.ShuntingYardAlgorithm;
import racetrack.util.StrSet;
import racetrack.util.Utils;
import racetrack.visualization.RTColorManager;

/**
 * Control panel window for the application.  This window allows the creation of
 * all the different views and provides management features for time markers and
 * entity tags.
 *
 * V61 - Transitioned away from cached panel into manipulating records directly.
 * V62 - Added RT Table Component
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class RTControlFrame extends JFrame implements MouseListener {
  private static final long serialVersionUID = -5169469156525234838L;

  /**
   * Main application parent class
   */
  RT rt;

  /**
   * GUI member to determine if components darken during highlights/brushing
   */
  JCheckBoxMenuItem    darken_cbmi,
  /**
   * GUI member to determin if the components should render -- disabling render is useful
   * during file load and window configuration setup times.
   */
                       render_cbmi;
  /**
   * Checkbox Menu Item - No highlights
   */
  JRadioButtonMenuItem hl_none_rbmi, 
  /**
   * Checkbox Menu Item - Normal brushing
   */
                       hl_normal_rbmi, 
  /**
   * Checkbox Menu Item - Normal brushing with one derivative
   */
		       hl_normalp_rbmi, 
  /**
   * Checkbox Menu Item - Normal brushing with two derivatives
   */
		       hl_normalpp_rbmi, 
  /**
   * Checkbox Menu Item - Replacement brushing
   */
		       hl_replace_rbmi, 
  /**
   * Checkbox Menu Item - Replacement brushing with one derivative
   */
		       hl_replacep_rbmi, 
  /**
   * Checkbox Menu Item - Replacement brushing with two derivatives
   */
		       hl_replacepp_rbmi,
  /**
   * Checkbox Menu Item - Overlay Stats when brusing
   */
                       overlay_stats_rbmi;
  /**
   * Color by dropdown for global coloring
   */
  JComboBox            color_by_cb, 
  /**
   * Count by dropdown for global counting
   */
                       count_by_cb;
  /**
   * Table for managing the entity tags
   */
  JTable               entity_table, 
  /**
   * Table for managing the time markers
   */
		       times_table;
  /**
   * Popup menu for the time markers menu (remove rows...)
   */
  PopupListener        times_popup,  
  /**
   * Popup menu for the entity tags menu (remove rows...)
   */
		       entity_popup;
  /**
   * Table model for the entity tag table
   */
  EntityTableModel     entity_tm;
  /**
   * Table model for the time marker table
   */
  TimesTableModel      times_tm;
  /**
   * File chooser for loading files
   */
  JFileChooser         file_chooser = new JFileChooser(".");

  /**
   * Tabbed panel for switching between the cache, entity tags, and time marker tables
   */
  JTabbedPane          tabs;

  /**
   * Component to display the current data stack (root and filters)
   */
  StackComponent       stack_component;

  /**
   * Menu storing analyst preferred layouts
   */
  JMenu                layouts_menu;

  /**
   * Text field for expression
   */
  JTextField           expression_tf;

  /**
   * List of the post proc checkbox menu items
   */
  Set<JCheckBoxMenuItem> post_proc_cbmis = new HashSet<JCheckBoxMenuItem>();

  /**
   * Construct the control panel with the specified parent class.  Detect
   * if SQL is availble and create the GUI.
   *
   *@param rt parent GUI component
   */
  public RTControlFrame(RT rt) { 
    super("RACETrack Control - V62");
    this.rt  = rt; 
    try { createGUI(); } catch (MalformedURLException mue) { System.err.println("MalURLE: " + mue); }
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
  }

  /**
   * Return the GUI parent.
   *
   *@return GUI parent
   */
  public RT getRTParent() { return rt; }

  /**
   * Create the GUI for the control panel.
   */
  private void createGUI() throws MalformedURLException {
    //
    // Get the base directory   What if we are in a JAR file?
    //
    //URL src_url  = RTGraphPanel.class.getProtectionDomain().getCodeSource().getLocation();
    
    //
    // Create the menu first
    //
    JMenuItem mi; JButton bt;
    JMenuBar  menu_bar  = new JMenuBar();

    // File Menu
    JMenu     file_menu = new JMenu("File");       menu_bar.add(file_menu);
      file_menu.add(mi = new JMenuItem("Load File..."));          mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { loadFile(); } } );
      file_menu.add(mi = new JMenuItem("Import RFC4180 CSV...")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { loadRFC4180CSV(); } } );
      file_menu.addSeparator();
      file_menu.add(mi = new JMenuItem("Save Root To File...")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { saveFile(getRTParent().getRootBundles(), false); } } );
      file_menu.add(mi = new JMenuItem("Save Visible To File...")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { saveFile(getRTParent().getVisibleBundles(), true); } } );
      file_menu.addSeparator();

      // Entity Tag Menu Items
      file_menu.add(mi = new JMenuItem("Load Tags From File..."));      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { loadEntityTagsFromFile();  } } );
      if (RTStore.sqlAvailable()) { file_menu.add(mi = new JMenuItem("Retrieve Tags From Local DB")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { loadEntityTagsLocally();  } } ); }
      // file_menu.add(mi = new JMenuItem("Search P2P..."));          mi.setEnabled(false);
      file_menu.add(mi = new JMenuItem("Save Tags To File..."));        mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { saveEntityTagsToFile(); } } );
      if (RTStore.sqlAvailable()) { file_menu.add(mi = new JMenuItem("Save Tags To Local DB"));       mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { saveEntityTagsLocally(); } } ); }
      // file_menu.add(mi = new JMenuItem("Publish To P2P..."));      mi.setEnabled(false);
      // file_menu.addSeparator();
      // file_menu.add(mi = new JMenuItem("Consolidate Tag Time Frames...")); mi.setEnabled(false); // Provide a dialog for a slop value -- go through the entity tags and fix them up...
      file_menu.addSeparator();

      // Time Marker Menu Items
      file_menu.add(mi = new JMenuItem("Load Time Markers From File..."));       mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { loadTimeMarkersFromFile(); } } );
      if (RTStore.sqlAvailable()) { file_menu.add(mi = new JMenuItem("Retrieve Time Markers From Local DB"));  mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae)   { loadTimeMarkersLocally();  } } ); }
      // file_menu.add(mi = new JMenuItem("Search P2P..."));           mi.setEnabled(false);
      file_menu.add(mi = new JMenuItem("Save Time Markers To File..."));         mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { saveTimeMarkersToFile();  } } );
      if (RTStore.sqlAvailable()) { file_menu.add(mi = new JMenuItem("Save Time Markers To Local DB"));        mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { saveTimeMarkersLocally(); } } ); }
      // file_menu.add(mi = new JMenuItem("Publish To P2P..."));       mi.setEnabled(false);
      // file_menu.addSeparator();
      // file_menu.add(mi = new JMenuItem("Only Keep Bundles In Time Markers")); mi.setEnabled(false);

      file_menu.addSeparator();

      file_menu.add(mi = new JMenuItem("Exit")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { exit(); } } );

    // Preferences Menu
    JMenu     pref_menu = new JMenu("Preferences"); menu_bar.add(pref_menu);
      String theme_strs[] = RTColorManager.getThemes();
      for (int i=0;i<theme_strs.length;i++) {
        pref_menu.add(mi = new JMenuItem(theme_strs[i]));
	mi.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent ae) {
	    CacheManager.clearCaches();
	    RTColorManager.setTheme(((JMenuItem) ae.getSource()).getText());
	    rt.refreshAll();
	  }
	} );
      }
      pref_menu.addSeparator();
      pref_menu.add(layouts_menu = new JMenu("Layouts")); fillLayoutsMenu();
      pref_menu.addSeparator();
      pref_menu.add(darken_cbmi   = new JCheckBoxMenuItem("Darken During Highlights",      true));
      pref_menu.addSeparator();
      pref_menu.add(render_cbmi   = new JCheckBoxMenuItem("Render", true));

    // Highlights Menu
    JMenu     highlights_menu = new JMenu("Highlights"); menu_bar.add(highlights_menu); ButtonGroup bg = new ButtonGroup();
      highlights_menu.add(hl_none_rbmi      = new JRadioButtonMenuItem("No Highlights", true)); bg.add(hl_none_rbmi);
      highlights_menu.addSeparator();
      highlights_menu.add(hl_normal_rbmi    = new JRadioButtonMenuItem("Normal (R)"));          bg.add(hl_normal_rbmi);
      highlights_menu.add(hl_normalp_rbmi   = new JRadioButtonMenuItem("Normal+ (R-Y)"));       bg.add(hl_normalp_rbmi);
      highlights_menu.add(hl_normalpp_rbmi  = new JRadioButtonMenuItem("Normal++ (R-Y-G)"));    bg.add(hl_normalpp_rbmi);
      highlights_menu.addSeparator();
      highlights_menu.add(hl_replace_rbmi   = new JRadioButtonMenuItem("Replace"));             bg.add(hl_replace_rbmi);
      highlights_menu.add(hl_replacep_rbmi  = new JRadioButtonMenuItem("Replace+"));            bg.add(hl_replacep_rbmi);
      highlights_menu.add(hl_replacepp_rbmi = new JRadioButtonMenuItem("Replace++"));           bg.add(hl_replacepp_rbmi);
      highlights_menu.addSeparator();
      highlights_menu.add(overlay_stats_rbmi= new JRadioButtonMenuItem("Overlay Stats"));       bg.add(overlay_stats_rbmi);

    // Bundles Menu
    JMenu     bundles_menu = new JMenu("Bundles"); menu_bar.add(bundles_menu);
      bundles_menu.add(mi = new JMenuItem("Stack Top"));                 mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { stackTop(); } } );
      bundles_menu.add(mi = new JMenuItem("Re-Push Stack"));             mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { rePushStack(); } } );
      bundles_menu.add(mi = new JMenuItem("Top Minus Visible"));         mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { topMinusVisible(); } } );
      bundles_menu.addSeparator();
      bundles_menu.add(mi = new JMenuItem("Add & Set Field (Beta)...")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { addAndSetField(); } } );
      bundles_menu.add(mi = new JMenuItem("Manipulate Tags (Beta)...")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { manipulateTags(); } } );
      bundles_menu.add(mi = new JMenuItem("Remove Fields (Beta)..."));   mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { removeFields(); } } );
      bundles_menu.addSeparator();
      bundles_menu.add(mi = new JMenuItem("Set Visible As Root..."));    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { setVisibleAsRoot(); } } );
      bundles_menu.addSeparator();
      bundles_menu.add(mi = new JMenuItem("Zeroize Root..."));           mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { zeroizeRoot(); } } );

    // Analytical Views
    JMenu     views_menu = new JMenu("Views"); menu_bar.add(views_menu);
      views_menu.add(mi = new JMenuItem("Link/Node Graph..."));               mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.LINKNODE,    getRTParent()); } } );
      views_menu.addSeparator();
      views_menu.add(mi = new JMenuItem("Temporal Graph..."));                mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.TEMPORAL,    getRTParent()); } } );
      views_menu.add(mi = new JMenuItem("Temporal Graph (x3)..."));           mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.TEMPORALx3,  getRTParent()); } } );
      views_menu.add(mi = new JMenuItem("Temporal Graph (Grid)..."));         mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.TEMPORALg,   getRTParent()); } } );
      views_menu.addSeparator();
      views_menu.add(mi = new JMenuItem("Histogram..."));                     mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAM,      getRTParent()); } } );
      views_menu.add(mi = new JMenuItem("Histogram (s)..."));                 mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAMs,     getRTParent()); } } );
      views_menu.add(mi = new JMenuItem("Histogram (x3s)..."));               mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAMx3s,   getRTParent()); } } );
      JMenu histos_menu = new JMenu("Histograms"); views_menu.add(histos_menu);
        histos_menu.add(mi = new JMenuItem("Histogram (x5s)..."));            mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAMx5s,   getRTParent()); } } );
        histos_menu.add(mi = new JMenuItem("Histogram (x8s)..."));            mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAMx8s,   getRTParent()); } } );
        histos_menu.add(mi = new JMenuItem("Histogram (x8x2s)..."));          mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAMx8x2s, getRTParent()); } } );
        histos_menu.add(mi = new JMenuItem("Histogram (x8x3s)..."));          mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAMx8x3s, getRTParent()); } } );
        histos_menu.add(mi = new JMenuItem("Histogram (x8x4s)..."));          mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAMx8x4s, getRTParent()); } } );
        histos_menu.add(mi = new JMenuItem("Histogram (x8x5s)..."));          mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAMx8x5s, getRTParent()); } } );
        histos_menu.add(mi = new JMenuItem("Histogram (Grid)..."));           mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAMg,     getRTParent()); } } );
        histos_menu.add(mi = new JMenuItem("Histogram (Grid, Simple)..."));   mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAMgs,    getRTParent()); } } );
      views_menu.addSeparator();
      views_menu.add(mi = new JMenuItem("Stacked Histogram..."));             mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.STACKHISTO,   getRTParent()); } } );
      views_menu.add(mi = new JMenuItem("Geospatial Histogram..."));          mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.GEOHISTO,     getRTParent()); } } );
      views_menu.addSeparator();
      views_menu.add(mi = new JMenuItem("XY Scatter..."));                    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.XY,           getRTParent()); } } );
      views_menu.add(mi = new JMenuItem("XY Scatter (2x1)..."));              mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.XYsBs,        getRTParent()); } } );
      views_menu.add(mi = new JMenuItem("XY Scatter (1x2)..."));              mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.XYtTb,        getRTParent()); } } );
      views_menu.add(mi = new JMenuItem("XY Scatter (1x3)..."));              mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.XYtTbTb,      getRTParent()); } } );
      views_menu.add(mi = new JMenuItem("XY Scatter (1x4)..."));              mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.XY1x4,        getRTParent()); } } );
      views_menu.addSeparator();
      views_menu.add(mi = new JMenuItem("Reports..."));                      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.REPORTS,      getRTParent()); } } );
      views_menu.addSeparator();
      views_menu.add(mi = new JMenuItem("Box Plot"));                         mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.BOXPLOT,              getRTParent()); } } );
      views_menu.add(mi = new JMenuItem("Box Plot x2"));                      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.BOXPLOTx2,            getRTParent()); } } );
      views_menu.addSeparator();
      JMenu specialty_menu = new JMenu("Specialty Views"); views_menu.add(specialty_menu);
        specialty_menu.add(mi = new JMenuItem("XY Entity Scatter"));                mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.XYENTITY,             getRTParent()); } } );
        specialty_menu.add(mi = new JMenuItem("Pivot..."));                         mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.PIVOT,                getRTParent());  } } );
        specialty_menu.add(mi = new JMenuItem("Event Horizon..."));                 mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.EVENT_HORIZON,        getRTParent());  } } );
        specialty_menu.add(mi = new JMenuItem("Parallel Coordinates..."));          mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.PARALLEL_COORDINATES, getRTParent());  } } );
        specialty_menu.add(mi = new JMenuItem("Venn Diagram..."));                  mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.VENN,                 getRTParent());  } } );
        specialty_menu.add(mi = new JMenuItem("Day Matrix..."));                    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.DAY_MATRIX,           getRTParent());  } } );
        specialty_menu.add(mi = new JMenuItem("Rug Plot..."));                      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.RUGPLOT,              getRTParent());  } } );
        specialty_menu.add(mi = new JMenuItem("GPS..."));                           mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.GPS,                  getRTParent());  } } );
        specialty_menu.add(mi = new JMenuItem("MDS..."));                           mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.MDS,                  getRTParent());  } } );
        specialty_menu.add(mi = new JMenuItem("Edge Times..."));                    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.EDGE_TIMES,           getRTParent());  } } );
        specialty_menu.add(mi = new JMenuItem("Correlation..."));                   mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.CORRELATE,            getRTParent());  } } );
        specialty_menu.add(mi = new JMenuItem("TableC... (Beta)"));                 mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.TABLEC,               getRTParent());  } } );
      views_menu.addSeparator();
      JMenu combined_menu = new JMenu("Combined Views"); views_menu.add(combined_menu);
        combined_menu.add(mi = new JMenuItem("Temporal/Histo Combo..."));          mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.TIME_HISTO,           getRTParent()); } } );
        combined_menu.add(mi = new JMenuItem("LinkNode/Temporal Combo..."));       mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.LINKNODE_TIME,        getRTParent()); } } );
        combined_menu.add(mi = new JMenuItem("LinkNode/Temporal/Histo Combo...")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.LINKNODE_TIME_HISTO,  getRTParent()); } } );
    
    // Post Processors
    JMenu post_procs_menu = new JMenu("Transforms"); menu_bar.add(post_procs_menu);
    ItemListener post_procs_il = new ItemListener() {
      public void itemStateChanged(ItemEvent ie) {
        JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) ie.getSource();

        boolean prev_render_state = renderVisualizations();
        try {
          disableRenders();
          if (cbmi.isSelected()) BundlesDT.enablePostProcessor (getRTParent().getRootBundles(), cbmi.getText()); 
          else                   BundlesDT.disablePostProcessor(getRTParent().getRootBundles(), cbmi.getText());
          getRTParent().updateBys();
        } finally { 
          if (prev_render_state) enableRenders(); 
        }
      } };
    String post_procs[] = BundlesDT.listAvailablePostProcessors();
    for (int i=0;i<post_procs.length;i++) {
      JCheckBoxMenuItem cbmi = new JCheckBoxMenuItem(post_procs[i]); post_procs_menu.add(cbmi); post_proc_cbmis.add(cbmi);
      cbmi.addItemListener(post_procs_il);
    }

    // About menu
    JMenu about_menu = new JMenu("About"); menu_bar.add(about_menu);
    //
    // Dump Threads For Debugging
    //
    about_menu.add(mi = new JMenuItem("Dump Threads (Debug)"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { 
        Map<Thread,StackTraceElement[]> thread_map = Thread.getAllStackTraces();
	Iterator<Thread> it = thread_map.keySet().iterator();
	while (it.hasNext()) {
	  Thread thread = it.next();
	  System.err.println("For Thread \"" + thread + "\" -- " + (thread_map.containsKey(thread) && thread_map.get(thread) != null ? thread_map.get(thread).length : "Null"));
	  StackTraceElement stack[] = thread_map.get(thread);
	  if (stack != null && stack.length > 0) {
	    for (int i=0;i<stack.length;i++) System.err.println("  " + stack[i]); 
	  }
	}
      } } );
     
    // Add the menu bar to the frame
    setJMenuBar(menu_bar);

    //
    // Create the tabs for data management
    //
    tabs = new JTabbedPane();
    JPanel stack_panel = new JPanel(new BorderLayout()); stack_panel.add("Center", stack_component = new StackComponent());
    JPanel expr_panel  = new JPanel(new BorderLayout(5,5)); 
           expr_panel.add("West", new JLabel("Expr"));
           expr_panel.add("Center", expression_tf = new JTextField()); new JTextFieldHistory(expression_tf);
           stack_panel.add("South", expr_panel);
    tabs.add("Stack",    stack_panel);
    tabs.add("Entities", new JScrollPane(entity_table = new JTable(entity_tm = new EntityTableModel()))); entity_table.setAutoCreateRowSorter(true);
    tabs.add("Times",    new JScrollPane(times_table  = new JTable(times_tm  = new TimesTableModel())));  times_table.setAutoCreateRowSorter(true);
    getContentPane().add("Center", tabs);

    //
    // Create the Comboboxes
    //
    JPanel south_panel = new JPanel(new FlowLayout());
    south_panel.add(new JLabel("Color"));
    south_panel.add(color_by_cb = new JComboBox(Utils.prepend(BundlesDT.COUNT_BY_NONE, KeyMaker.blanks(rt.getRootBundles().getGlobals()))));
    south_panel.add(new JLabel("Count"));
    south_panel.add(count_by_cb = new JComboBox(KeyMaker.blanks(rt.getRootBundles().getGlobals(), true, true, true, true)));
    south_panel.add(bt = new JButton("Top"));   bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { stackTop(); } } );
    getContentPane().add("South", south_panel);

    // Create the memory component
    JPanel north = new JPanel(new BorderLayout());
    north.add("Center", new JREMemComponent());
    getContentPane().add("North", north);

    // Tools
    JPanel tools = new JPanel(new GridLayout(5,2));

    tools.add(bt = new JButton(new ImageIcon(getClass().getResource("/images/linknode.png")))); bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.LINKNODE,     getRTParent()); } } );
    tools.add(bt = new JButton(new ImageIcon(getClass().getResource("/images/histos.png"))));   bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAMs,   getRTParent()); } } );
    tools.add(bt = new JButton(new ImageIcon(getClass().getResource("/images/time.png"))));     bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.TEMPORAL,     getRTParent()); } } );
    tools.add(bt = new JButton(new ImageIcon(getClass().getResource("/images/histo.png"))));    bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAM,    getRTParent()); } } );
    tools.add(bt = new JButton(new ImageIcon(getClass().getResource("/images/timex3.png"))));   bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.TEMPORALx3,   getRTParent()); } } );
    tools.add(bt = new JButton(new ImageIcon(getClass().getResource("/images/histox3.png"))));  bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAMx3s, getRTParent()); } } );
    tools.add(bt = new JButton(new ImageIcon(getClass().getResource("/images/xy.png"))));       bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.XY,           getRTParent()); } } );
    tools.add(bt = new JButton(new ImageIcon(getClass().getResource("/images/histox8.png"))));  bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.HISTOGRAMx8s, getRTParent()); } } );
    tools.add(bt = new JButton(new ImageIcon(getClass().getResource("/images/xy1x2.png"))));    bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.XYtTb,        getRTParent()); } } );
    tools.add(bt = new JButton(new ImageIcon(getClass().getResource("/images/boxplot.png"))));  bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { new RTPanelFrame(RTPanelFrame.Type.BOXPLOT,      getRTParent()); } } );

    getContentPane().add("East", tools);

    // Time Marker Popup
    JPopupMenu times_popup_menu = new JPopupMenu();
    mi = new JMenuItem("Remove Row(s)");               times_popup_menu.add(mi);  mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { removeFromTimeMarkers  (times_popup.row_i); } } );
    mi = new JMenuItem("Remove Row(s) From Local DB"); times_popup_menu.add(mi);  mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { removeFromTimeMarkersDB(times_popup.row_i); } } );
    times_popup_menu.addSeparator();
    mi = new JMenuItem("Filter Viewable");             times_popup_menu.add(mi);  mi.setEnabled(false); // mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { filterViewableToTime(times_popup.row_i, 0L);             } } );
    mi = new JMenuItem("Filter Viewable (+/-5min)");   times_popup_menu.add(mi);  mi.setEnabled(false); // mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { filterViewableToTime(times_popup.row_i, 1000L*60L*5L);   } } );

    // Entity Tag Popup Menu
    JPopupMenu entity_popup_menu = new JPopupMenu();
    mi = new JMenuItem("Remove Row(s)");               entity_popup_menu.add(mi); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { removeFromEntityTags(entity_popup.row_i);   } } );
    mi = new JMenuItem("Remove Row(s) From Local DB"); entity_popup_menu.add(mi); mi.setEnabled(false); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { removeFromEntityTagsDB(entity_popup.row_i); } } );
    entity_popup_menu.addSeparator();
    mi = new JMenuItem("Set As Forever");              entity_popup_menu.add(mi); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { setEntityTagsToForever(entity_popup.row_i); } } );
    entity_popup_menu.addSeparator();
    mi = new JMenuItem("Set As Selected");             entity_popup_menu.add(mi); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { applyEntityTagsSetOp(entity_popup.row_i, StrSet.Op.SELECT);    } } );
    mi = new JMenuItem("Add To Selected");             entity_popup_menu.add(mi); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { applyEntityTagsSetOp(entity_popup.row_i, StrSet.Op.ADD);       } } );
    mi = new JMenuItem("Remove From Selected");        entity_popup_menu.add(mi); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { applyEntityTagsSetOp(entity_popup.row_i, StrSet.Op.REMOVE);    } } );
    mi = new JMenuItem("Intersect Selected");          entity_popup_menu.add(mi); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { applyEntityTagsSetOp(entity_popup.row_i, StrSet.Op.INTERSECT); } } );

    //
    // Add the listeners
    //
    ItemListener refresh_all_il = new ItemListener() { public void itemStateChanged(ItemEvent ie) { rt.refreshAll(); } };
    color_by_cb.addItemListener(refresh_all_il);
    count_by_cb.addItemListener(refresh_all_il);
    render_cbmi.addItemListener(refresh_all_il);
    times_table.addMouseListener  (times_popup  = new PopupListener(times_popup_menu,  times_table));
    entity_table.addMouseListener (entity_popup = new PopupListener(entity_popup_menu, entity_table));
    addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent we) { exit(); } } );
    expression_tf.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { applyExpression(); } } );

    // Show it
    pack(); setSize(500,300); setVisible(true);
  }

  /**
   * Map to store the layout configuration strings.
   */
  Map<String,String[]> layout_lu = new HashMap<String,String[]>();

  /**
   * Retrieve the information from prefs about layouts.  Populate the menu with the information.
   */
  public void fillLayoutsMenu() {
    layouts_menu.removeAll(); JMenuItem mi;
    layouts_menu.add(mi = new JMenuItem("Save GUI Layout...")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { saveGUILayout(); } } );
    layouts_menu.addSeparator();

    // Get the layout names and settings
    String layouts = RTPrefs.retrieveString("layouts");
    if (layouts != null && layouts.length() > 0) {
      StringTokenizer st = new StringTokenizer(layouts, "|");
      while (st.hasMoreTokens()) {
        String layout_name      = Utils.decFmURL(st.nextToken());
        String layout_setting[] = RTPrefs.retrieveStrings("layout." + Utils.encToURL(layout_name));
	if (layout_setting != null && layout_setting.length > 0) {
	  layout_lu.put(layout_name, layout_setting);
	}
      }
    }
    // For the validated ones, add them to the jmenu
    List<String> validated = new ArrayList<String>(); validated.addAll(layout_lu.keySet());
    Collections.sort(validated);
    Iterator<String> it = validated.iterator();
    while (it.hasNext()) {
      String layout_name = it.next();
      layouts_menu.add(mi = new JMenuItem(layout_name));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
        applyGUILayout(((JMenuItem) ae.getSource()).getText());
      } } );
    }
  }

  /**
   * Apply the expression in the textfield to the visible data.
   */
  public void applyExpression() {
    String expr = expression_tf.getText(); if (Utils.stripSpaces(expr).equals("")) return;
    try {
      ShuntingYardAlgorithm sya = new ShuntingYardAlgorithm(expr);
      Set<Bundle> matches = sya.matches(getRTParent().getVisibleBundles().bundleSet());
      if (matches.size() > 0) rt.push(getRTParent().getVisibleBundles().subset(matches));
    } catch (Throwable t) { JOptionPane.showMessageDialog(file_chooser, "Throwable: " + t, "Expression Error", JOptionPane.ERROR_MESSAGE); }
  }

  /**
   * For a named GUI layout, retrieve the settings and apply them.
   *
   *@param layout_name named layout
   */
  public void applyGUILayout(String layout_name) {
    // System.err.println("applyGUILayout(\"" + layout_name + "\")");
    String settings[] = layout_lu.get(layout_name);
    if (settings != null && settings.length > 0) {
      List<String> list = new ArrayList<String>();
      for (int i=0;i<settings.length;i++) {
        // System.err.println("  " + i + ".  \"" + settings[i] + "\"");
        list.add(settings[i]);
      }
      applyGUIConfiguration(list, false);
    }
  }

  /**
   * Enable user to name the current GUI configuration and save it to the preferences.
   */
  public void saveGUILayout() {
    String layout_name = JOptionPane.showInputDialog(this, "GUI Layout Name", "GUI Layout", JOptionPane.QUESTION_MESSAGE);
    if (layout_name != null) {
      // Get the gui configuration and store it
      List<String> settings = getGUIConfiguration(false, false);
      String strs[] = new String[settings.size()];
      for (int i=0;i<strs.length;i++) strs[i] = settings.get(i);
      RTPrefs.store("layout." + Utils.encToURL(layout_name), strs);
      // Add the layout name to the strings
      String layouts = RTPrefs.retrieveString("layouts");
      if (layouts == null || layouts.equals("")) {
        RTPrefs.store("layouts", Utils.encToURL(layout_name));
      } else {
        StringTokenizer st   = new StringTokenizer(layouts, "|");
	Set<String>     set  = new HashSet<String>(); set.add(layout_name);
        while (st.hasMoreTokens()) set.add(Utils.decFmURL(st.nextToken()));
        // Re-store
        List<String>    list = new ArrayList<String>(); list.addAll(set);
	StringBuffer    sb   = new StringBuffer();
	for (int i=0;i<list.size();i++) {
	  if (sb.length() > 0) sb.append("|");
	  sb.append(Utils.encToURL(list.get(i)));
	}
	RTPrefs.store("layouts", sb.toString());
      }
      fillLayoutsMenu();
    }
  }

  /**
   * Return true if views should darken their rendering during brushing operationss.
   *
   *@return true if they should darken
   */
  public boolean darkenBackground() { return darken_cbmi.isSelected(); }

  /**
   * Return true if windows/views should re-render themselves.  This option is
   * useful for turning off the rendering while files load / configurations are
   * applied / and window setup times.
   *
   *@return true if windows should render themselves
   */
  public boolean renderVisualizations() { return render_cbmi.isSelected(); }

  /**
   * disable the rendering engines.
   */
  public void disableRenders() { render_cbmi.setSelected(false); }

  /**
   * Enable the rendering engines.
   */
  public void enableRenders() { render_cbmi.setSelected(true); }

  /**
   * Determine if overlay stats is enabled.
   *
   *@return true if overlay stats is enabled
   */
  public boolean overlayStats()         { return overlay_stats_rbmi.isSelected(); }

  /**
   * Determine if highlighting (brushing) is enabled.
   *
   *@return true if highlights are enabled
   */
  public boolean highlight()            { return hl_none_rbmi.isSelected()       == false &&
                                                 overlay_stats_rbmi.isSelected() == false; }

  /**
   * Determine if first order (records near the mouse) is enabled
   *
   *@return true if first order highlighting is on
   */
  public boolean highlightFirstOrder()  { return hl_normalp_rbmi.isSelected()   || hl_normalpp_rbmi.isSelected()  ||
                                                 hl_replacep_rbmi.isSelected()  || hl_replacepp_rbmi.isSelected(); }
  /**
   * Determine if second order (records a little further from the mouse) is enabled
   *
   *@return true if second order highlighting is on
   */
  public boolean highlightSecondOrder() { return hl_normalpp_rbmi.isSelected()  || hl_replacepp_rbmi.isSelected(); }

  /**
   * Determine if components should replace their current view based
   * on highlighted (brushed) records.
   *
   *@return true if views should be in replace mode
   */
  public boolean highlightsReplace()    { return hl_replace_rbmi.isSelected()   ||
                                                 hl_replacep_rbmi.isSelected()  ||
						 hl_replacepp_rbmi.isSelected(); }
  /**
   * Cycle through the highlight settings by the specified amount.
   *
   *@param amount to change the highlight settings
   */
  public void    adjustHighlightSetting(int amount) {
    System.err.println("adjustHighlightSetting() - Not Implemented");
  }

  /**
   * Return the global color by string.  Null indicates that no
   * coloring is chosen and that default (logarithmic) coloring
   * will be used.
   *
   *@return global color by string
   */
  public String getColorBy() { 
    if (color_by_cb == null) return null;
    String color_by = (String) color_by_cb.getSelectedItem(); 
    if (color_by != null && color_by.equals(BundlesDT.COUNT_BY_NONE)) color_by = null;
    return color_by;
  }

  /**
   * Return the global count by string.
   *
   *@return global count by string
   */
  public String getCountBy() { 
    if (count_by_cb == null) return null;
    return (String) count_by_cb.getSelectedItem(); 
  }

  /**
   * Update the color by and count by dropdown boxes (most likely
   * because new data has been loaded and the fields have changed).
   */
  public void updateBys() {
    Object color_sel = color_by_cb.getSelectedItem();
    color_by_cb.removeAllItems();
    String strs[];
    strs = Utils.prepend(BundlesDT.COUNT_BY_NONE, KeyMaker.blanks(rt.getRootBundles().getGlobals()));   for (int j=0;j<strs.length;j++) color_by_cb.addItem(strs[j]);
    color_by_cb.setSelectedItem(color_sel);

    Object count_sel = count_by_cb.getSelectedItem();
    count_by_cb.removeAllItems();
    strs = KeyMaker.blanks(rt.getRootBundles().getGlobals(), true, true, true, true); for (int j=0;j<strs.length;j++) count_by_cb.addItem(strs[j]);
    count_by_cb.setSelectedItem(count_sel);
  }

  /**
   * Load a data file by providing the user with a file chooser then a set of
   * dialogs to specify the format.
   */
  private void loadRFC4180CSV() {
    file_chooser.setMultiSelectionEnabled(true);
    if (file_chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      try {
        File files[] = file_chooser.getSelectedFiles();
	for (int i=0;i<files.length;i++) {
          if (files[i].exists()) {
            new RFC4180ImportDialog(this, files[i]);
	  } else throw new IOException("File \"" + files[i].getName() + "\" Not Found");
        }
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(file_chooser, "IOException: " + ioe, "File Load Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /**
   * Load a data file by providing the user with a file chooser dialog and then
   * having the application parse the file.
   */
  private void loadFile() {
    file_chooser.setMultiSelectionEnabled(true);
    if (file_chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      boolean prev_render_state = renderVisualizations();
      try {
        disableRenders();
        File files[] = file_chooser.getSelectedFiles(); List<String> last_appconf = null;
	for (int i=0;i<files.length;i++) {
          if (files[i].exists()) {
	    last_appconf = getRTParent().load(files[i]);
	  } else throw new IOException("File \"" + files[i].getName() + "\" Not Found");
        }
	// If there are no panels open, try to apply the application configuration information (if available)
        if (getRTParent().rtPanelIterator().hasNext() == false && last_appconf.size() > 0) { 
          if (JOptionPane.showConfirmDialog(this, "Apply GUI Configuration From File?", "Apply GUI Config", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            applyGUIConfiguration(last_appconf, true); 
	  }
        }
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(file_chooser, "IOException: " + ioe, "File Load Error", JOptionPane.ERROR_MESSAGE);
      } finally { 
        if (prev_render_state) enableRenders(); 
      }
    }
  }

  /**
   * Apply a set of configuration lines to the current instance.  Abort if there are already existing panels.
   */
  public void applyGUIConfiguration(List<String> strs, boolean apply_additional_settings) {
    if (getRTParent().rtPanelIterator().hasNext() == true || strs == null || strs.size() == 0) return;

    StringTokenizer st; String last_uuid = "";

    int i = 0;
    while (i < strs.size()) {
      String line = strs.get(i++); if (line.startsWith("#AC") == false) continue;
      if        (line.startsWith("#AC Application Configuration")) {
        st = new StringTokenizer(line,"|"); st.nextToken();
	int    x        = (int) Double.parseDouble(st.nextToken()), y = (int) Double.parseDouble(st.nextToken()),
	       w        = (int) Double.parseDouble(st.nextToken()), h = (int) Double.parseDouble(st.nextToken());
        String count_by = Utils.decFmURL(st.nextToken()),
	       color_by = Utils.decFmURL(st.nextToken());
        setLocation(x,y); setSize(w,h); 

        // Check to see if their are post proc enable strings
        if (st.hasMoreTokens()) {
          StringTokenizer pp_st = new StringTokenizer(Utils.decFmURL(st.nextToken()), "|");
          while (pp_st.hasMoreTokens()) {
            String post_proc = Utils.decFmURL(pp_st.nextToken());
            Iterator<JCheckBoxMenuItem> it = post_proc_cbmis.iterator();
            while (it.hasNext()) {
              JCheckBoxMenuItem cbmi = it.next();
              if (cbmi.getText().equals(post_proc)) cbmi.setSelected(true);
            }
          }
        }

        // Update the JComboBoxes
        if (count_by != null && count_by.equals("null") == false) count_by_cb.setSelectedItem(count_by); 
        if (color_by != null && color_by.equals("null") == false) color_by_cb.setSelectedItem(color_by);
      } else if (line.startsWith("#AC Color")) {
        st = new StringTokenizer(line,"|"); st.nextToken(); String color_str = Utils.decFmURL(st.nextToken());
        RTColorManager.setTheme(color_str);
      } else if (line.startsWith("#AC RTPanelFrame"))              {
        st = new StringTokenizer(line,"|"); st.nextToken(); String type_str = st.nextToken(); st.nextToken();
	int    x        = (int) Double.parseDouble(st.nextToken()), y = (int) Double.parseDouble(st.nextToken()),
	       w        = (int) Double.parseDouble(st.nextToken()), h = (int) Double.parseDouble(st.nextToken());
        RTPanelFrame rt_panel_frame = new RTPanelFrame(RTPanelFrame.stringToType(type_str), getRTParent());
	rt_panel_frame.setLocation(x,y); rt_panel_frame.setSize(w,h); last_uuid = rt_panel_frame.getUUID();
      } else if (line.startsWith("#AC "))                          {
        st = new StringTokenizer(line, " "); 
        if (st.countTokens() != 3) System.err.println("Configuration Line Without Three Tokens \"" + line + "\"");
        st.nextToken(); int pos = Integer.parseInt(st.nextToken()); String config = st.nextToken();
        if (last_uuid != null && last_uuid.equals("") == false) {
          // Find the component to apply the configuration to
          Iterator<RTPanel> it = getRTParent().rtPanelIterator();
          while (it.hasNext()) {
            RTPanel panel = it.next();
            if (panel.getWinUniq().equals(last_uuid) && panel.getWinPos() == pos) {
              // System.err.println("Setting Config For Panel \"" + panel + "\"");
	      panel.setConfig(config);
              if (apply_additional_settings && panel.hasAdditionalConfig()) { i = panel.parseAdditionalConfig(strs, i); }
            }
          }
        }
      }
    }
  }

  /**
   * Save the specific dataset to a file.  Provide the user with a dialog to choose
   * the save file.
   *
   *@param bundles           bundles (records) to save
   *@param save_visible_only save the visible information only (saves space)
   */
  private void saveFile(Bundles bundles, boolean save_visible_only) {
    if (file_chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) 
      saveFile(bundles, file_chooser.getSelectedFile(), save_visible_only);
  }
  /**
   * Save the specific dataset to the specified file.  In addition to saving
   * the dataset, the entity tags, time markers, and comments are also included
   * into the file.  For all intense purposes, the file is a gzipped multi-csv
   * file separated by blank lines with included headers.
   *
   *@param bundes            bundles (records) to save
   *@param file              file to save to
   *@param save_visible_only save the visible information only (saves space)
   */
  public void saveFile(Bundles bundles, File file, boolean save_visible_only) {
      if (file.getName().toLowerCase().endsWith(".mcsv.gz") == false) {
        file = new File(file.getParentFile(), file.getName() + ".mcsv.gz");
      }
      System.err.println("Saving Bundles To File... (" + bundles + ")");
      PrintStream out = null;
      try {
        out = new PrintStream(new GZIPOutputStream(new FileOutputStream(file)));
	// Dump the tablets
	// - Sort the tablets by their header row
        Iterator<Tablet>  it_tab  = bundles.tabletIterator();
	List<Tablet> tablets = new ArrayList<Tablet>();
        while (it_tab.hasNext()) { tablets.add(it_tab.next()); }
	Collections.sort(tablets, new Comparator<Tablet>() { public int compare(Tablet t0, Tablet t1) { return t0.fileHeader().compareTo(t1.fileHeader()); } } );

        it_tab = tablets.iterator(); String last_header = null;
	while (it_tab.hasNext()) { 
	  Tablet tablet = it_tab.next(); 

	  boolean needs_header = (last_header == null) || (tablet.fileHeader().equals(last_header) == false);
	  if (last_header != null && needs_header) out.println("");
	  tablet.save(out, needs_header);

	  last_header = tablet.fileHeader();
	}

	// Dump the entity tags
	if (getRTParent().getNumberOfEntityTags() > 0) {
	  out.println("");
          out.println(EntityTag.getFileHeader());
	  for (int i=0;i<getRTParent().getNumberOfEntityTags();i++) out.println(getRTParent().getEntityTag(i).asFileLine());
	  out.println("");
	}

	// Dump the timing marks
	if (getRTParent().getNumberOfTimeMarkers()> 0) {
	  out.println("");
          out.println(TimeMarker.getFileHeader());
	  for (int i=0;i<getRTParent().getNumberOfTimeMarkers();i++) out.println(getRTParent().getTimeMarker(i).asFileLine());
	  out.println("");
        }

	// Comments
	Iterator<RTComment> it = getRTParent().commentsIterator(); boolean needs_header = true;
	while (it.hasNext()) {
	  if (needs_header) { out.println(""); out.println(RTComment.getFileHeader()); needs_header = false; }
	  while (it.hasNext()) { out.println(it.next().asFileLine()); }
	  out.println("");
	}

        // Application configuration
	out.println("");
	List<String> gui_config = getGUIConfiguration(true, save_visible_only);
	for (int i=0;i<gui_config.size();i++) out.println(gui_config.get(i));

        System.err.println("  Bundle Save Successful!");
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(this, "IOException: " + ioe, "File Save Error", JOptionPane.ERROR_MESSAGE);
      } finally { if (out != null) out.close(); }
    }

  /**
   * Return a list of strings (order matters) of the GUI settings.
   *
   *@param include_additional_settings include the additional information to fully restore the state of the component
   *@param save_visible_only           save the visible information only
   *
   *@return list of strings that encompass the GUI state
   */
  public List<String> getGUIConfiguration(boolean include_additional_settings, boolean save_visible_only) {
    List<String> list = new ArrayList<String>();
    String count_by = getCountBy(), color_by = getColorBy();
    // Gather up the post processors
    String       pps[]       = BundlesDT.listEnabledPostProcessors();
    StringBuffer postproc_sb = new StringBuffer();
    if (pps.length > 0) postproc_sb.append(Utils.encToURL(pps[0]));
    for (int i=1;i<pps.length;i++) postproc_sb.append("|" + pps[i]);
     // Add it all to the list
    list.add("#AC Application Configuration" +
	     "|" + getLocationOnScreen().getX()  + "|" + getLocationOnScreen().getY() + 
	     "|" + getSize().getWidth()          + "|" + getSize().getHeight()        +
	     "|" + (count_by == null ? "null" : Utils.encToURL(count_by))  + 
             "|" + (color_by == null ? "null" : Utils.encToURL(color_by))  +
             "|" + Utils.encToURL(postproc_sb.toString()));

    // Save the colors out
    list.add("#AC Color|" + Utils.encToURL(RTColorManager.getTheme()));

    List<RTPanel> sorter = new ArrayList<RTPanel>();
    Iterator<RTPanel> it_panel = getRTParent().rtPanelIterator(); while (it_panel.hasNext()) sorter.add(it_panel.next());
    Collections.sort(sorter, new Comparator<RTPanel>() {
      public int compare(RTPanel p0, RTPanel p1) {
        if (p0.getWinUniq().equals(p1.getWinUniq())) return p0.getWinPos() - p1.getWinPos();
        else                                         return p0.getWinUniq().compareTo(p1.getWinUniq());
      }
    } );
    // - Print them into the file
    it_panel = sorter.iterator(); String last_uniq = "";
    while (it_panel.hasNext()) {
      RTPanel panel = it_panel.next();
      if (panel.getWinUniq().equals(last_uniq) == false) {
        Container container = panel.getParent(); while (container instanceof Window == false) container = container.getParent();
        Window window = (Window) container;
        double win_x = window.getLocationOnScreen().getX(),
               win_y = window.getLocationOnScreen().getY(),
               win_w = window.getSize().getWidth(),
               win_h = window.getSize().getHeight();

System.err.println("Checking Position (Minimization Save Problem?)");
System.err.println("winpos = " + win_x + " " + win_y + " " + win_w + " " + win_h);

        list.add("#AC RTPanelFrame|" + panel.getWinType()  + "|" + panel.getWinUniq() + 
	         "|" + win_x + "|" + win_y + "|" + win_w + "|" + win_h);
        last_uniq = panel.getWinUniq();
      }
      list.add("#AC " + panel.getWinPos() + " " + panel.getConfig());
      if (include_additional_settings && panel.hasAdditionalConfig()) panel.addAdditionalConfig(list, save_visible_only);
    }
    return list;
  }

  /**
   * Load entity tags from a user chosen file.
   */
  public void loadEntityTagsFromFile() {
    if (file_chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = file_chooser.getSelectedFile();
	loadEntityTagsFromFile(file);
    }
  }

  /**
   * Load entity tags from a specific file.
   *
   *@param file specific file to load from
   */
  public void loadEntityTagsFromFile(File file) {
    try {
      List<EntityTag> list = new ArrayList<EntityTag>();
      BufferedReader in = new BufferedReader(new FileReader(file));
      String line = in.readLine(); if (line.equals(EntityTag.getFileHeader()) == false) {
    	  in.close();
    	  throw new IOException("File Header Does Not Match");
      }
      while ((line = in.readLine()) != null) { EntityTag et = new EntityTag(line); if (et.valid()) list.add(et); }
      getRTParent().addEntityTags(list);
      in.close();
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(entity_table, "IOException: " + ioe, "File Save Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Load entity tags from local database storage.
   */
  public void loadEntityTagsLocally() {
    List<EntityTag> new_ones = new ArrayList<EntityTag>();
    boolean result = RTStore.retrieveRelatedEntityTags(new_ones, getRTParent().getVisibleBundles().ts0(), getRTParent().getVisibleBundles().ts1());
    if (result) getRTParent().addEntityTags(new_ones);
    else        JOptionPane.showMessageDialog(this, "Error Retrieving Local Entity Tags", "Local DB Error", JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Save entity tags from a user chosen file.
   */
  public void saveEntityTagsToFile() {
    if (file_chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      try {
        File file = file_chooser.getSelectedFile();
        PrintStream out = new PrintStream(new FileOutputStream(file));
        out.println(EntityTag.getFileHeader());
	for (int i=0;i<getRTParent().getNumberOfEntityTags();i++) out.println(getRTParent().getEntityTag(i).asFileLine());
	out.close();
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(entity_table, "IOException: " + ioe, "File Save Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /**
   * Save entity tags to the local database.
   */
  public void saveEntityTagsLocally() {
    boolean result = RTStore.updateEntityTags(getRTParent().getEntityTags());
    if (!result) JOptionPane.showMessageDialog(this, "Error Storing/Updating Local Entity Tags", "Local DB Error", JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Load time markers from a user chosen file.
   */
  public void loadTimeMarkersFromFile() {
    if (file_chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = file_chooser.getSelectedFile();
	loadTimeMarkersFromFile(file);
    }
  }

  /**
   * Load time markers from the specified file.
   *
   *@return file file to load time markers from
   */
  public void loadTimeMarkersFromFile(File file) {
    try {
      List<TimeMarker> list = new ArrayList<TimeMarker>();
      BufferedReader in = new BufferedReader(new FileReader(file));
      String line = in.readLine(); if (line.equals(TimeMarker.getFileHeader()) == false) {
    	  in.close();
    	  throw new IOException("File Header Does Not Match");
      }
      while ((line = in.readLine()) != null) { TimeMarker tm = new TimeMarker(line); if (tm.valid()) list.add(tm); }
      getRTParent().addTimeMarkers(list);
      in.close();
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(times_table, "IOException: " + ioe, "File Save Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Load time markers from the local database.
   */
  public void loadTimeMarkersLocally() {
    List<TimeMarker> new_ones = new ArrayList<TimeMarker>();
    boolean result = RTStore.retrieveRelatedTimeMarkers(new_ones, getRTParent().getVisibleBundles().ts0(), getRTParent().getVisibleBundles().ts1());
    if (result) getRTParent().addTimeMarkers(new_ones);
    else        JOptionPane.showMessageDialog(this, "Error Retrieving Local Time Markers", "Local DB Error", JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Save time markers to the user chosen file.
   */
  public void saveTimeMarkersToFile() {
    if (file_chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      try {
        File file = file_chooser.getSelectedFile();
        PrintStream out = new PrintStream(new FileOutputStream(file));
        out.println(TimeMarker.getFileHeader());
	for (int i=0;i<getRTParent().getNumberOfTimeMarkers();i++) out.println(getRTParent().getTimeMarker(i).asFileLine());
	out.close();
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(times_table, "IOException: " + ioe, "File Save Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /**
   * Save time markers to the local database.
   */
  public void saveTimeMarkersLocally() {
    boolean result = RTStore.updateTimeMarkers(getRTParent().getTimeMarkers());
    if (!result) JOptionPane.showMessageDialog(this, "Error Storing/Updating Local Time Markers", "Local DB Error", JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Exit the application.  Should probably make it user-safe by asking for
   * confirmation...
   */
  private void exit() {
    System.exit(0);
  }

  /**
   * Set the visible dataset as the root dataset for the application.  Useful
   * for when the noise/non-relevant data has been removed from the view and the
   * user wants to reclaim memory.
   */
  private void setVisibleAsRoot() {
    // Get a positive confirmation
    if (JOptionPane.showConfirmDialog(this, "Really set visible as root?", "Set Visible As Root", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

    // Execute the transition
    Set<Bundles> actives = new HashSet<Bundles>(); // deprecated -- the cached instances don't exist anymore...
    getRTParent().setRootBundles(getRTParent().getVisibleBundles(), actives);
  }

  /**
   * Provide the user with a dialog of the fields in the application.  Allow user to
   * select fields for removal via a customized dialog.
   */
  private void removeFields() { new RemoveFieldsDialog(this); }

  /**
   * Show the dialog(s) to add a new field to the visible records.
   */
  private void addAndSetField() { new AddAndSetFieldDialog(this); }

  /**
   * Show the dialog(s) to manipulate tags.
   */
  private void manipulateTags() { new ManipulateTagsDialog(this); }

  /**
   * Clear out the root dataset.  leaves the entity tags and time markers
   * in the application.
   */
  private void zeroizeRoot() {
    if (JOptionPane.showConfirmDialog(this, "Really zeroize the root?", "Zeroize Root", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
    getRTParent().zeroizeRoot();
  }

  /**
   * Return to the root dataset.
   */
  private void stackTop() {
    rt.popAll();
  }

  /**
   * Refilter the last dataset
   */
  private void rePushStack() {
    rt.repush();
  }

  /**
   * Go back to the top but remove the visible set (the stack will have two bundles on it -- root and (root - visible)).
   */
  private void topMinusVisible() {
    // Get the root and visible sets
    Bundles root = getRTParent().getRootBundles();    Set<Bundle> root_set = root.bundleSet();
    Bundles vis  = getRTParent().getVisibleBundles(); Set<Bundle> vis_set  = vis.bundleSet();

    // Create the subset
    Set<Bundle> subset = new HashSet<Bundle>();
    Iterator<Bundle> it = root_set.iterator(); while (it.hasNext()) {
      Bundle bundle = it.next(); 
      if (vis_set.contains(bundle) == false) subset.add(bundle);
    }

    // Update the stack
    stackTop(); rt.push(root.subset(subset));
  }

  public void mousePressed  (MouseEvent e) { }
  public void mouseReleased (MouseEvent e) { }
  public void mouseClicked  (MouseEvent e) { }
  public void mouseEntered  (MouseEvent e) { }
  public void mouseExited   (MouseEvent e) { }

  /**
   * Popup Listener to dispay popup menu for a table.  The listener remembers which
   * row contained the mouse for processing.
   */
  class PopupListener extends MouseAdapter {
    JPopupMenu popup; int row_i; JTable table;
    public PopupListener(JPopupMenu popup0, JTable table) { this.popup = popup0; this.table = table; }
    public void mousePressed   (MouseEvent e) { maybeShowPopup(e); }
    public void mouseReleased  (MouseEvent e) { maybeShowPopup(e); }
    public void maybeShowPopup (MouseEvent e) {
      if (e.isPopupTrigger()) { row_i = table.convertRowIndexToModel(table.rowAtPoint(e.getPoint())); popup.show(e.getComponent(), e.getX(), e.getY()); } }
  }

  /**
   * Popup menu action to remove row(s) from time markers.
   *
   *@param row_i row index to remove
   */
  public void removeFromTimeMarkers(int row_i) {
    Set<Integer> set = new HashSet<Integer>(); int rows[] = times_table.getSelectedRows(); for (int i=0;i<rows.length;i++) { rows[i] = times_table.convertRowIndexToModel(rows[i]); set.add(rows[i]); }
    if (row_i >= 0 && row_i < getRTParent().getNumberOfTimeMarkers()) {
      if (set.contains(row_i)) { // Remove multiple highlighted entries
        Arrays.sort(rows);
	for (int j=rows.length-1;j>=0;j--) getRTParent().removeTimeMarker(getRTParent().getTimeMarker(rows[j]));
      } else getRTParent().removeTimeMarker(getRTParent().getTimeMarker(row_i)); // Remove just the single row under the mouse
    }
  }

  /**
   * Popup menu action to remove row(s) from time markers local database.
   *
   *@param row_i row index to remove
   */
  public void removeFromTimeMarkersDB(int row_i) {
    Set<TimeMarker> to_remove = new HashSet<TimeMarker>();
    Set<Integer> set = new HashSet<Integer>(); int rows[] = times_table.getSelectedRows(); for (int i=0;i<rows.length;i++) { rows[i] = times_table.convertRowIndexToModel(rows[i]); set.add(rows[i]); }
    if (row_i >= 0 && row_i < getRTParent().getNumberOfTimeMarkers()) {
      if (set.contains(row_i)) { // Remove multiple highlighted entries
        Arrays.sort(rows); for (int j=rows.length-1;j>=0;j--) to_remove.add(getRTParent().getTimeMarker(rows[j]));
      } else to_remove.add(getRTParent().getTimeMarker(row_i)); // Remove just the single row under the mouse
    }
    if (to_remove.size() > 0) {
      boolean result = RTStore.deleteTimeMarkers(to_remove);
      if (!result) JOptionPane.showMessageDialog(this, "Error Deleting Local Time Markers", "Local DB Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Popup menu action to remove row(s) from entity tags.
   *
   *@param row_i row index to remove
   */
  public void removeFromEntityTags(int row_i) {
    Set<Integer> set = new HashSet<Integer>(); int rows[] = entity_table.getSelectedRows(); for (int i=0;i<rows.length;i++) { rows[i] = entity_table.convertRowIndexToModel(rows[i]); set.add(rows[i]); }
    if (row_i >= 0 && row_i < getRTParent().getNumberOfEntityTags()) {
      if (set.contains(row_i)) { // Remove multiple highlighted entries
        Arrays.sort(rows);
	for (int j=rows.length-1;j>=0;j--) getRTParent().removeEntityTag(getRTParent().getEntityTag(rows[j]));
      } else getRTParent().removeEntityTag(getRTParent().getEntityTag(row_i)); // Remove just the single row under the mouse
    }
  }

  /**
   * Popup menu action to remove row(s) from entity tags local database.
   *
   *@param row_i row index to remove
   */
  public void removeFromEntityTagsDB(int row_i) {
    Set<EntityTag> to_remove = new HashSet<EntityTag>();
    Set<Integer> set = new HashSet<Integer>(); int rows[] = times_table.getSelectedRows(); for (int i=0;i<rows.length;i++) { rows[i] = times_table.convertRowIndexToModel(rows[i]); set.add(rows[i]); }
    if (row_i >= 0 && row_i < getRTParent().getNumberOfEntityTags()) {
      if (set.contains(row_i)) { // Remove multiple highlighted entries
        Arrays.sort(rows); for (int j=rows.length-1;j>=0;j--) to_remove.add(getRTParent().getEntityTag(rows[j]));
      } else to_remove.add(getRTParent().getEntityTag(row_i)); // Remove just the single row under the mouse
    }
    if (to_remove.size() > 0) {
      boolean result = RTStore.deleteEntityTags(to_remove);
      if (!result) JOptionPane.showMessageDialog(this, "Error Deleting Local Entity Tags", "Local DB Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Popup menu action to change the selected rows to forever (from just to/from the original timeframes.
   *
   *@param row_i row index to apply the change to
   */
  public void setEntityTagsToForever(int row_i) {
    Set<Integer> set = new HashSet<Integer>(); int rows[] = entity_table.getSelectedRows(); for (int i=0;i<rows.length;i++) { rows[i] = entity_table.convertRowIndexToModel(rows[i]); set.add(rows[i]); }
    if (row_i >= 0 && row_i < getRTParent().getNumberOfEntityTags()) {
      if (set.contains(row_i)) { for (int j=0;j<rows.length;j++) getRTParent().getEntityTag(rows[j]).setToForever();
      } else getRTParent().getEntityTag(row_i).setToForever();
    }
  }

  /**
   * Popup menu action to perform a boolean operation on the selected entities
   *
   *@param row_i row index to apply operation to
   *@param op    set operation to perform
   */
  public void applyEntityTagsSetOp(int row_i, StrSet.Op op) {
    // Get the selected rows as a set
    Set<Integer> set = new HashSet<Integer>(); int rows[] = entity_table.getSelectedRows(); for (int i=0;i<rows.length;i++) { rows[i] = entity_table.convertRowIndexToModel(rows[i]); set.add(rows[i]); }

    // Make sure the operation
    if (row_i >= 0 && row_i < getRTParent().getNumberOfEntityTags()) {
      Set<String> strset = new HashSet<String>();

      // Figure out if the selection is under the mouse... if selection, find rows and add to set
      if (set.contains(row_i)) { 
	for (int j=0;j<rows.length;j++) strset.add(getRTParent().getEntityTag(rows[j]).getEntity());
      } else strset.add(getRTParent().getEntityTag(row_i).getEntity());

      // Do the operation
      Set<String> result = StrSet.operation(op, getRTParent().getSelectedEntities(), strset);
      getRTParent().setSelectedEntities(result);
    }
  }

  /**
   * Class implementing a {@link TableModel} for the entity tags table.
   */
  class EntityTableModel implements TableModel, EntityTagListener {
    public EntityTableModel() { getRTParent().addEntityTagListener(this); }
    List<TableModelListener> listeners = new ArrayList<TableModelListener>();
    public void addTableModelListener(TableModelListener tml) { listeners.add(tml); }
    public void removeTableModelListener(TableModelListener tml) { listeners.remove(tml); }
    public Class<?> getColumnClass(int column_index) { return (new String()).getClass(); }
    public int      getColumnCount() { return 6; }
    public String   getColumnName(int column_index) {
      switch (column_index) { case 0:  return "Entity";
                              case 1:  return "Tag";
			      case 2:  return "First Heard";
			      case 3:  return "Last Heard";
			      case 4:  return "UUID";
			      case 5:  return "Creation";
			      default: return "Say What?"; } }
    public int      getRowCount() { return getRTParent().getNumberOfEntityTags(); }
    public Object   getValueAt(int row_i, int col_i) {
      EntityTag et = getRTParent().getEntityTag(row_i);
      if (et == null) return null;
      switch (col_i) { case 0:  return et.getEntity();
                       case 1:  return et.getTag();
		       case 2:  if (et.isForever()) return "\u221E"; // infinite symbol
		                else                return Utils.humanReadableDate(et.ts0());
		       case 3:  if (et.isForever()) return "\u221E"; // infinite symbol
		                else                return Utils.humanReadableDate(et.ts1());
		       case 4:  return et.getUUID().toString();
		       case 5:  return Utils.humanReadableDate(et.getCreateTime());
		       default: return "Say What?"; } }
    public boolean  isCellEditable(int row_i, int col_i) { return false; }
    public void     setValueAt(Object value, int row_i, int col_i) { }
    public void     newEntityTag(EntityTag et) { 
      int size = getRTParent().getNumberOfEntityTags();
      for (int i=0;i<listeners.size();i++) listeners.get(i).tableChanged(new TableModelEvent(this, size-1, size-1, 0, TableModelEvent.INSERT));
    }
    public void     entityTagListChanged()      { 
      for (int i=0;i<listeners.size();i++) listeners.get(i).tableChanged(new TableModelEvent(this));
    }
  }

  /**
   * Class implementing the {@link TableModel} for the time marker table.
   */
  class TimesTableModel implements TableModel, TimeMarkerListener {
    public TimesTableModel() { getRTParent().addTimeMarkerListener(this); }
    List<TableModelListener> listeners = new ArrayList<TableModelListener>();
    public void addTableModelListener(TableModelListener tml) { listeners.add(tml); }
    public void removeTableModelListener(TableModelListener tml) { listeners.remove(tml); }
    public Class<?> getColumnClass(int column_index) { return (new String()).getClass(); }
    public int      getColumnCount() { return 5; }
    public String   getColumnName(int column_index) {
      switch (column_index) { case 0:  return "Description";
			      case 1:  return "First Heard";
			      case 2:  return "Last Heard";
			      case 3:  return "UUID";
			      case 4:  return "Creation";
			      default: return "Say What?"; } }
    public int      getRowCount() { return getRTParent().getNumberOfTimeMarkers(); }
    public Object   getValueAt(int row_i, int col_i) {
      TimeMarker tm = getRTParent().getTimeMarker(row_i);
      if (tm == null) return null;
      switch (col_i) { case 0:  return tm.getDescription();
		       case 1:  return Utils.humanReadableDate(tm.ts0());
		       case 2:  return Utils.humanReadableDate(tm.ts1());
		       case 3:  return tm.getUUID().toString();
		       case 4:  return Utils.humanReadableDate(tm.getCreateTime());
		       default: return "Say What?"; } }
    public boolean  isCellEditable(int row_i, int col_i) { return col_i == 0; }
    public void     setValueAt(Object value, int row_i, int col_i) { 
      TimeMarker tm = getRTParent().getTimeMarker(row_i);
      if (tm == null || col_i != 0) return;
      tm.setDescription((String) value);
      getRTParent().refreshAll();
    }
    public void     newTimeMarker(TimeMarker tm) { 
      int size = getRTParent().getNumberOfTimeMarkers();
      for (int i=0;i<listeners.size();i++) listeners.get(i).tableChanged(new TableModelEvent(this, size-1, size-1, 0, TableModelEvent.INSERT));
    }
    public void     timeMarkerListChanged()      { 
      for (int i=0;i<listeners.size();i++) listeners.get(i).tableChanged(new TableModelEvent(this));
    }
  }

  /**
   * Component to view the current stack of bundles within the application.  Also provides
   * interactive capability to choose which part of the stack to go to.
   */
  class StackComponent extends JComponent implements MouseListener {
    /**
     * Keep a map of the geom to tablet or bundles
     */
    Map<Shape,Object> geom_to_data = new HashMap<Shape,Object>();

    /**
     * Construct the component by adding the mouse listener.
     */
    public StackComponent() { addMouseListener(this); }

    /**
     * Paint the component by pulling from the stacked bundles.
     *
     *@param g graphics primitive
     */
    public void paintComponent(Graphics g) { 
      Graphics2D g2d = (Graphics2D) g; Composite orig_comp = g2d.getComposite();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      geom_to_data.clear();

      // Clear the background
      g2d.setColor(RTColorManager.getColor("background", "default"));
      int w = getWidth(), h = getHeight(); int txt_h = Utils.txtH(g2d, "0");
      g2d.fillRect(0,0,w,h);

      // Calculate the geometry
      int ins_w = txt_h+4, ins_h = txt_h, graph_w = w - 2*ins_w, graph_h = h - (ins_h+5);

      // Copy the stack
      List<Bundles> dupe = rt.dupeBundlesStack(); Bundles visible = rt.getVisibleBundles();
      
      // Draw each stack layer
      int x = ins_w; int total_buns = dupe.get(0).bundleSet().size();
      for (int i=0;i<dupe.size();i++) {
        // Setup the render for this slice
        Bundles bundles = dupe.get(i); int y = ins_h;
	Iterator<Tablet> it_tab = bundles.tabletIterator(); 
        List<Tablet> sorter = new ArrayList<Tablet>(); while (it_tab.hasNext()) sorter.add(it_tab.next());
	Collections.sort(sorter, new Comparator<Tablet>() { public int compare(Tablet t0, Tablet t1) { return t0.fileHeader().compareTo(t1.fileHeader()); } } );

	// Draw a representation for this bundles
        Ellipse2D ellipse = new Ellipse2D.Float(x+1, 1, ins_h-2, ins_h-2);
	geom_to_data.put(ellipse, bundles);
	g2d.setColor(RTColorManager.getColor("label", "default"));
	if (visible == bundles) g2d.fill(ellipse); else g2d.draw(ellipse);

	// Go through the sorted tablets
	it_tab = sorter.iterator(); while (it_tab.hasNext()) {
	  Tablet tablet = it_tab.next();
	  g2d.setColor(RTColorManager.getColor(tablet.fileHeader()));
          int bar_h = (tablet.bundleSet().size() * graph_h) / total_buns; 
          if (bar_h < 4) bar_h = 4;
	  Rectangle2D rect = new Rectangle2D.Float(x, y, txt_h, bar_h); g2d.fill(rect);
	  geom_to_data.put(rect, tablet);
	  y += bar_h + 1;
	}

	// If this isn't the visible bundle, make it a little transparent
	if (visible != bundles) {
          g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
          g2d.setColor(RTColorManager.getColor("background", "default"));
	  g2d.fillRect(x, ins_h, txt_h, y - ins_h);
	  g2d.setComposite(orig_comp);
	}

	// Go to the next column
        x += txt_h + 1;
      }


      // Print info about the visible data
      int  num_of_tablets = visible.tabletCount(),
           num_of_bundles = visible.bundleSet().size();
      long ts0            = visible.ts0(),
	   ts1            = visible.ts1();
      String stats =          ((num_of_tablets == 1) ? ("1 Tablet") : (num_of_tablets + " Tablets"));
             stats += " | " + ((num_of_bundles == 1) ? ("1 Bundle") : (num_of_bundles + " Bundles"));
      String times = "", times2 = "";
      int    str_w = Utils.txtW(g2d, stats);
      g2d.setColor(RTColorManager.getColor("label", "default"));
      if (ts0 != Long.MAX_VALUE) {
        times  = Utils.exactDate(ts0);
	times2 = Utils.exactDate(ts1);
	int str_w2 = Utils.txtW(g2d, times);  if (str_w2 > str_w) str_w = str_w2;
	    str_w2 = Utils.txtW(g2d, times2); if (str_w2 > str_w) str_w = str_w2;
      }
                                      g2d.drawString(stats,  w - 2 - str_w, h - 2*txt_h - 2);
      if (times.equals("")  == false) g2d.drawString(times,  w - 2 - str_w, h - 1*txt_h - 2);
      if (times2.equals("") == false) g2d.drawString(times2, w - 2 - str_w, h - 0*txt_h - 2);
    }

    public void mouseEntered (MouseEvent me) { }
    public void mouseExited  (MouseEvent me) { }
    public void mousePressed (MouseEvent me) { }
    public void mouseReleased(MouseEvent me) { }

    /**
     * Find the geometry under the click and push that onto the stack.  If no
     * geometry is found, pop the stack
     */
    public void mouseClicked (MouseEvent me) { 
      int mx = me.getX(), my = me.getY(); Object data = null;

      // Find the shape under the mouse
      Iterator<Shape> it = geom_to_data.keySet().iterator();
      while (it.hasNext()) { Shape shape = it.next(); if (shape.contains(mx, my)) data = geom_to_data.get(shape); }

      // Push or pop...
      if        (data == null)            { rt.pop(); 
      } else if (data instanceof Bundles) { rt.push((Bundles) data);
      } else if (data instanceof Tablet)  { rt.push(rt.getVisibleBundles().subset(((Tablet) data).bundleSet())); }
    }
  }
}

/**
 * Dialog to add and set a field for the visible records.  Need to make shortcut buttons
 * so that user can quickly use different settings without re-typing.
 */
class AddAndSetFieldDialog extends JDialog {
  /**
   * Parent frame
   */
  protected RTControlFrame parent;

  /**
   * Textfield for user specified field name
   */
  JTextField field_name_tf,

  /**
   * Value field
   */
             value_tf,
  /**
   * Field to display the datatype
   */
	     datatype_tf;
  /**
   * ComboBox for an existing field
   */
  JComboBox<String>  existing_field_cb;

  /**
   * Checkbox to indicate that the field will be scalar (not enabled -- just reflects what the user specifies)
   */
  JCheckBox  scalar_cb;

  /**
   * Button to add/set the field/value
   */
  JButton    addset_bt;

  /**
   * Construct the dialog and display it.
   *
   *@param parent parent control frame
   */
  public AddAndSetFieldDialog(RTControlFrame parent) {
    super(parent, "Add Or Set Field Dialog (Visible Bundles)", false);
    this.parent = parent;
    getContentPane().setLayout(new BorderLayout(5,5));

    // Construct the GUI
    // - Options
    JPanel panel = new JPanel(new GridLayout(3,3,5,5));
    panel.add(new JLabel("Field Name")); panel.add(field_name_tf = new JTextField()); panel.add(existing_field_cb = new JComboBox<String>());
    panel.add(new JLabel("Value"));      panel.add(value_tf      = new JTextField()); panel.add(scalar_cb         = new JCheckBox("Scalar Field", false)); scalar_cb.setEnabled(false);
    panel.add(new JLabel("Data Type"));  panel.add(datatype_tf   = new JTextField()); datatype_tf.setEnabled(false);
    getContentPane().add("Center", panel);

    // - Buttons
    panel = new JPanel(new FlowLayout());
    JButton bt;
    panel.add(bt        = new JButton("Close"));   bt.       addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { close(); } } );
    panel.add(addset_bt = new JButton("Add/Set")); addset_bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { addAndSet(); } } ); addset_bt.setEnabled(false);
    getContentPane().add("South", panel);

    // Add listeners
    existing_field_cb.addItemListener(new ItemListener() { public void itemStateChanged(ItemEvent e) { checkComboBox(); } } );
    field_name_tf.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { if (addset_bt.isEnabled()) addAndSet(); } } );
    value_tf.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { if (addset_bt.isEnabled()) addAndSet(); } } );
    
    field_name_tf.addCaretListener(new CaretListener() { public void caretUpdate(CaretEvent e) { checkButtonState(); } } );
    value_tf.addCaretListener(new CaretListener() { public void caretUpdate(CaretEvent e) { checkButtonState(); parseDataType(); } } );

    addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent we) { setVisible(false); dispose(); } } );

    // Fill the combobox
    fillComboBox();
    
    // Show the dialog
    pack(); setVisible(true);
  }

  /**
   * Fill the combobox with the fields.
   */
  private void fillComboBox() {
    // Get a list of the fields
    String blanks[] = KeyMaker.blanks(parent.getRTParent().getRootBundles().getGlobals(), false, true, true, false);
    // Sort without case sensitivity
    Arrays.sort(blanks, new CaseInsensitiveComparator());
    // Add the "New Field" Option...
    List<String> fields_ls = new ArrayList<String>(); fields_ls.add("Add New Field...");
    // Don't add any fields that have a pipe in them (those are transforms, specialized...)
    for (int i=0;i<blanks.length;i++) if (blanks[i].indexOf("|") < 0) fields_ls.add(blanks[i]);
    // Convert to an array
    String fields[] = new String[fields_ls.size()]; fields_ls.toArray(fields);
    // Now add them to the combobox
    existing_field_cb.removeAllItems();
    for (int i=0;i<fields.length;i++) existing_field_cb.addItem(fields[i]);
  }

  /**
   * Manipulate the gui based on the combobox setting.
   */
  private void checkComboBox() {
    String field = (String) existing_field_cb.getSelectedItem(); if (field == null) { return; }
    if (field.equals("Add New Field...")) { field_name_tf.setEnabled(true); } else { 
      field_name_tf.setEnabled(false); scalar_cb.setEnabled(false);
      if (Utils.isAllUpper(field)) scalar_cb.setSelected(true); else scalar_cb.setSelected(false);
    }
    checkButtonState();
  }

  /**
   * Parse the user data type and display in the datatype textfield.
   */
  private void parseDataType() { // System.err.println("Value = \"" + value_tf.getText() + "\"");
                                 datatype_tf.setText("" + BundlesDT.getEntityDataType(value_tf.getText())); }

  /**
   * See if the add/set button should be enabled or not.
   */
  private void checkButtonState() {
    String field = (String) existing_field_cb.getSelectedItem();
    boolean field_valid = false;
    if (field.equals("Add New Field...")) { if (validFieldName(field = field_name_tf.getText())) { field_valid = true; } } else { field_valid = true; }

    String value = value_tf.getText();
    boolean value_valid = true;
    if (Utils.isAllUpper(field)) { value_valid = false; try { Integer.parseInt(value); value_valid = true; } catch (NumberFormatException nfe) { } }

    if (field_valid && value_valid) addset_bt.setEnabled(true); else addset_bt.setEnabled(false);
  }

  /**
   * Check to see if the field name is valid.
   */
  private boolean validFieldName(String str) {
    // Check for a blank field name or reserved ones...
    if (str.equals("") || str.toLowerCase().equals("tags")
                       || str.toLowerCase().equals("timestamp")
		       || str.toLowerCase().equals("timestamp_end")
		       || str.toLowerCase().equals("beg")
		       || str.toLowerCase().equals("end")) return false;

    // Check for datatype names -- not a good idea to use these either (they can be used for file-based transforms)
    Iterator<BundlesDT.DT> it_dt = BundlesDT.dataTypesIterator();
    while (it_dt.hasNext()) { if (str.equals("" + it_dt.next())) return false; }

    // Check to make sure we have legit characters (probably should do this on file read as well...)
    for (int i=0;i<str.length();i++) {
      char c = str.charAt(i);
      if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == ' ' || c == '-' || c == '_') { } else return false;
    }
    if (Utils.isAllUpper(str)) scalar_cb.setSelected(true); else scalar_cb.setSelected(false);
    return true;
  }

  /**
   * Close the dialog.
   */
  public void close() { setVisible(false); }

  /**
   * Add and set the field. Calls into the framework.
   */
  public void addAndSet() {
    // Execute the set operations
    Bundles root_bundles  = parent.getRTParent().getRootBundles(),
            toset_bundles = parent.getRTParent().getVisibleBundles();

    // Get the field from the appropriate place
    boolean field_valid = false, adding_new_field = false;
    String field = (String) existing_field_cb.getSelectedItem();
    if (field.equals("Add New Field...")) { if (validFieldName(field = field_name_tf.getText())) { field_valid = true; adding_new_field = true; } } else { field_valid = true; }

    // If it's valid, set it.
    if (field_valid) {
      root_bundles.getGlobals().setField(toset_bundles, root_bundles, field, value_tf.getText());
      // Update the bys
      parent.getRTParent().updateBys();
      if (adding_new_field) {
        // Clear the textfield and disable
        field_name_tf.setText(""); field_name_tf.setEnabled(false);
        // Update the combobox and include the new field
	fillComboBox();
        existing_field_cb.setSelectedItem(field);
      }
    }
  }
}

/**
 * Dialog used to manipulate record tags.
 */
class ManipulateTagsDialog extends JDialog  implements CaretListener {
  /**
   * Parent frame
   */
  RTControlFrame parent;

  /**
   * Create a panel to manipulate record tags.
   *
   *@param parent parent frame
   */
  public ManipulateTagsDialog(RTControlFrame parent) {
    super(parent, "Manipulate Tags...", false); this.parent = parent; JButton bt; getContentPane().setLayout(new BorderLayout());

    JPanel panel   = new JPanel(new GridLayout(3,1,5,5)); panel.setBorder(BorderFactory.createTitledBorder("New/Additional Tag(s)"));
    JPanel sub     = new JPanel(new BorderLayout(5,5)); sub.add("West", new JLabel("Tags")); sub.add(tags_tf = new JTextField(30));
    panel.add(sub);
    panel.add(tags_label = new JLabel(""));
    panel.add(info_label = new JLabel(""));
    getContentPane().add("North", panel);

    // Add the tag cache info
    quick_cache_panel = new JPanel(new GridLayout(0,1,5,5)); quick_cache_panel.setBorder(BorderFactory.createTitledBorder("Quick Cache"));
    getContentPane().add("Center", new JScrollPane(quick_cache_panel));

    // Create the close panel
    panel = new JPanel(new FlowLayout());
    panel.add(bt                       = new JButton("Clear"));    bt.                 setToolTipText("Clear");
      bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { clearTags(); } } );
    panel.add(bt = tags_replace_bt     = new JButton("Replace"));  tags_replace_bt.    setToolTipText("Replace");            tags_replace_bt.    setEnabled(false);
      bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { replaceTags(); } } );
    panel.add(bt = tags_add_bt         = new JButton("Add"));      tags_add_bt.        setToolTipText("Add To");             tags_add_bt.        setEnabled(false);
      bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { addToTags(); } } );
    panel.add(bt = tags_replacetype_bt = new JButton("Replace*")); tags_replacetype_bt.setToolTipText("Replace Type-Value"); tags_replacetype_bt.setEnabled(false);
      bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { replaceTypeValueTags(); } } );
    panel.add(bt = tags_remove_bt      = new JButton("Remove*"));  tags_remove_bt.     setToolTipText("Remove Tag(s)");      tags_remove_bt.     setEnabled(false);
      bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { removeTags(); } } );

    panel.add(bt = new JButton("Close"));
    bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { setVisible(false); dispose(); } } );
    getContentPane().add("South", panel);

    // Add the listeners
    addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent we) { setVisible(false); dispose(); } } );
    tags_tf.addCaretListener(this);
    tags_tf.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { addToTags(); } } );

    // Pack it and show it
    pack(); setVisible(true);
  }

  /**
   * Handle changes to the tags textfield.  Verify if the tag is correct and enable/disable the buttons accordingly.
   *
   *@param e caret event
   */
  public void caretUpdate(CaretEvent e) { 
    String str = Utils.stripSpaces(tags_tf.getText());
    if (str.equals("")) { disableAllButtons(); tags_label.setText(""); displayHelp(); } else {
      // Normalize the tag
      String       norm   = Utils.normalizeTag(str); tags_label.setText(norm);

      // Decompose the individual tokens
      List<String> tokens = Utils.tokenizeTags(norm); int simple_count = 0, typeval_count = 0, hier_count = 0, error_count = 0, tag_count = 0;
      for (int i=0;i<tokens.size();i++) {
        boolean simple  = Utils.tagIsSimple(tokens.get(i)),
                typeval = Utils.tagIsTypeValue(tokens.get(i)),
                hier    = Utils.tagIsHierarchical(tokens.get(i));
        if      ( simple && !typeval && !hier) { simple_count++;  tag_count++; }
        else if (!simple &&  typeval && !hier) { typeval_count++; tag_count++; }
        else if (!simple && !typeval &&  hier) { hier_count++;    tag_count++; }
        else                                   error_count++;
      }

      // Set the info string
      String info = tokens.size() + " Tag(s) - " + simple_count + " Simple, " + typeval_count + " TypeVal, " + hier_count + " Hier (" + error_count + " Errs)";
      info_label.setText(info);

      if (error_count > 0 || tag_count == 0) { disableAllButtons(); } else {
        if (simple_count == 0 && hier_count == 0 && typeval_count > 0) tags_replacetype_bt.setEnabled(true); else tags_replacetype_bt.setEnabled(false); 
        tags_replace_bt.setEnabled(true);
        tags_add_bt.setEnabled(true);
        tags_remove_bt.setEnabled(true);
      }
    }
  }

  /**
   * Draw a help string in the info label.
   */
  protected void displayHelp() { info_label.setText("Ex: simple_tag|type=value|parent::child|color=blue,black"); }

  /**
   * Disable all of the buttons (except clear -- that never gets disabled).
   */
  protected void disableAllButtons() { tags_replace_bt.setEnabled(false); tags_add_bt.setEnabled(false); tags_replacetype_bt.setEnabled(false); tags_remove_bt.setEnabled(false); }

  /**
   * Panel for the quick cache options.
   */
  JPanel quick_cache_panel;

  /**
   * Textfield for entering new tags.
   */
  JTextField tags_tf;

  /**
   * Label to describe current tags textfield.
   */
  JLabel     tags_label,
  /**
   * Label to show additional parsing information on the tag.
   */
             info_label;
  /**
   * Buttons for the tags modifiers.
   */
  JButton    tags_replace_bt,
             tags_add_bt,
	     tags_replacetype_bt,
             tags_remove_bt;

  /**
   * Clear the tags for the visible records.
   */
  protected void clearTags() {
    parent.getRTParent().getVisibleBundles().clearTags();
    parent.getRTParent().refreshAll();
  }

  /**
   * Replace the tag for the visible records with the user specified tag.
   */
  protected void replaceTags() {
    Bundles root = parent.getRTParent().getRootBundles(), visible = parent.getRTParent().getVisibleBundles();
    cacheButton("replaceTags", Utils.normalizeTag(tags_tf.getText()));
    root.replaceTags(visible, Utils.normalizeTag(tags_tf.getText()));
    parent.getRTParent().updateBys(); parent.getRTParent().refreshAll();
  }

  /**
   * Add the specified tag to the existing tag.
   */
  protected void addToTags() {
    Bundles root = parent.getRTParent().getRootBundles(), visible = parent.getRTParent().getVisibleBundles();
    cacheButton("addToTags", Utils.normalizeTag(tags_tf.getText()));
    root.addTags(visible, Utils.normalizeTag(tags_tf.getText()));
    parent.getRTParent().updateBys(); parent.getRTParent().refreshAll();
  }

  /**
   * Replace the type value tags that match the user specified tag.
   */
  protected void replaceTypeValueTags() {
    Bundles root = parent.getRTParent().getRootBundles(), visible = parent.getRTParent().getVisibleBundles();
    cacheButton("replaceTypeValueTags", Utils.normalizeTag(tags_tf.getText()));
    root.replaceTypeValueTags(visible, Utils.normalizeTag(tags_tf.getText()));
    parent.getRTParent().updateBys(); parent.getRTParent().refreshAll();
  }

  /**
   * Remove the specific tags that the user has entered.
   */
  protected void removeTags() {
    cacheButton("removeTags", Utils.normalizeTag(tags_tf.getText()));
    parent.getRTParent().getVisibleBundles().removeTags(Utils.normalizeTag(tags_tf.getText()));
    parent.getRTParent().refreshAll();
  }

  /**
   * Set of cached buttons
   */
  Set<String> cached = new HashSet<String>();

  /**
   * Create a cache button (unless it already exists)
   */
  protected void cacheButton(String action, String tags) {
    // Make sure it's not a duplicate
    String key = action + " ==> " + tags; if (cached.contains(key)) return; else cached.add(key);

    // Add the button and its listener
    JButton bt; quick_cache_panel.add(bt = new JButton(key));
    bt.addActionListener(new CachedActionListener(action, tags));

    // Force the gui to update
    validate();
  }

  /**
   * Create a internal class to handle the cached action.
   */
  class CachedActionListener implements ActionListener {
    String action, tags;
    public CachedActionListener(String action, String tags) { this.action = action; this.tags = tags; }
    public void actionPerformed(ActionEvent ae) {
      boolean field_added = false;

      // Get the root and visible bundles
      Bundles root = parent.getRTParent().getRootBundles(), visible = parent.getRTParent().getVisibleBundles();

      // Select the correct options
      if        (action.equals("replaceTags"))          { field_added = root.   replaceTags         (visible, Utils.normalizeTag(tags));
      } else if (action.equals("addToTags"))            { field_added = root.   addTags             (visible, Utils.normalizeTag(tags));
      } else if (action.equals("replaceTypeValueTags")) { field_added = root.   replaceTypeValueTags(visible, Utils.normalizeTag(tags));
      } else if (action.equals("removeTags"))           {               visible.removeTags          (         Utils.normalizeTag(tags));
      } else throw new RuntimeException("Do Not Understand Cached Action \"" + action + "\"");

      // Adjust the interface if a field was added
      if (field_added) parent.getRTParent().updateBys();

      // Refresh the views
      parent.getRTParent().refreshAll();
    }
  }
}

/**
 * Dialog to list fields and enable user to interactive delete the fields.
 */
class RemoveFieldsDialog extends JDialog {
  /**
   * Parent frame
   */
  protected RTControlFrame parent;

  /**
   * Sorted list of fields
   */
  List<String> fields = new ArrayList<String>();

  /**
   * Checkboxes to keep field
   */
  JCheckBox    cbs[];

  /*
   * Constructor
   *
   *@param parent  parent frame
   */
  public RemoveFieldsDialog(RTControlFrame parent) {
    super(parent, "Remove Fields...", true); this.parent = parent;

    // Get a list of sorted fields
    BundlesG globals = parent.getRTParent().getRootBundles().getGlobals();
    Iterator<String> it = globals.fieldIterator(); while (it.hasNext()) fields.add(it.next());
    Collections.sort(fields, new CaseInsensitiveComparator());

    // Construct the gui
    cbs = new JCheckBox[fields.size()];
    JPanel cbs_panel = new JPanel(new GridLayout(fields.size()+1, 1, 5, 5)), labs_panel = new JPanel(new GridLayout(fields.size()+1, 1, 5, 5));
    cbs_panel.add(new JLabel("Keep")); labs_panel.add(new JLabel("Field"));
    for (int i=0;i<fields.size();i++) { labs_panel.add(new JLabel(fields.get(i))); cbs_panel.add(cbs[i] = new JCheckBox("", true)); }

    JPanel center = new JPanel(new BorderLayout()); center.add("Center", labs_panel); center.add("East", cbs_panel);
    getContentPane().add("Center", new JScrollPane(center));

    // Construct the actions
    JButton bt;
    JPanel buttons = new JPanel(new FlowLayout());
    buttons.add(bt = new JButton("Cancel"));        bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { close(); } } );
    buttons.add(bt = new JButton("Remove Fields")); bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { removeFields(); } } );
    getContentPane().add("South", buttons);

    // Set it to visible
    pack(); setVisible(true);
  }

  /**
   * Close the dialog.
   */
  protected void close() { setVisible(false); dispose(); }

  /**
   *
   */
  protected void removeFields() {
    // Gather up the fields to remove
    Set<String> to_remove = new HashSet<String>(); for (int i=0;i<fields.size();i++) if (cbs[i].isSelected() == false) to_remove.add(fields.get(i));

    // Execute the remove operations
    Bundles root_bundles = parent.getRTParent().getRootBundles();
    root_bundles.getGlobals().removeFields(root_bundles, to_remove);

    // Update the bys
    parent.getRTParent().updateBys();

    // Close out the dialog
    close();
  }
}

/**
 * Provide a dialog for the user to enumerate timestamps and scalar fields.
 */
class RFC4180ImportDialog extends JDialog implements CSVTokenConsumer {
  /**
   * File to load
   */
  protected File file; 

  /**
   * Reference to the parent of this dialog (needed for constructor)
   */
  protected RTControlFrame parent;

  /**
   * The first so many lines of the file.
   */
  List<String[]> lines = new ArrayList<String[]>();

  /**
   * Key for RTStore procedure
   */
  String storage_key = null;

  /**
   * Constructor
   *
   *@param parent  parent frame
   *@param file    file to read
   */
  public RFC4180ImportDialog(RTControlFrame parent, File file) {
    super(parent, "Import Dialog...", true); this.parent = parent; this.file = file;
    // Get some of the file for examinination
    try {
      RFC4180CSVReader reader = new RFC4180CSVReader(file,this);
      if (lines.size() <= 2) {
        JOptionPane.showMessageDialog(this, "Not Enough Lines In The File For Parsing", "File Error", JOptionPane.ERROR_MESSAGE);
      } else {
        // Figure out which lines had a consistent number of tokens
        Map<Integer,Integer> token_histo = new HashMap<Integer,Integer>();
        for (int i=0;i<lines.size();i++) {
	  if (token_histo.containsKey(lines.get(i).length) == false) token_histo.put(lines.get(i).length, 0);
	  token_histo.put(lines.get(i).length, 1 + token_histo.get(lines.get(i).length));
	}
        // Find the maximum
        int max = -1, tokens = -1;
	Iterator<Integer> it = token_histo.keySet().iterator();
	while (it.hasNext()) {
	  int num_of_tokens = it.next();
	  if (token_histo.get(num_of_tokens) > max) { max = token_histo.get(num_of_tokens); tokens = num_of_tokens; }
	}
	// Check to see if the tokens are consistent with at least half of the lines
	if (max < lines.size()/2) {
          JOptionPane.showMessageDialog(this, "No Definitive Token Counts In File", "File Error", JOptionPane.ERROR_MESSAGE);
	} else {
	  // Find the first line, assume it's the header
	  first_line = -1; for (int i=0;i<lines.size();i++) if ((lines.get(i)).length == tokens && first_line == -1) first_line = i;
	  hdr = lines.get(first_line);
          // Check for a stored configuration of the header
          String copy[] = new String[hdr.length]; for (int i=0;i<copy.length;i++) copy[i] = hdr[i];
          Arrays.sort(copy); StringBuffer sb = new StringBuffer(); sb.append(copy[0].toLowerCase());
          for (int i=1;i<copy.length;i++) sb.append("_" + Utils.encToURL(copy[i].toLowerCase()));
          storage_key = "IMPORT_" + sb.toString();
          String prefs = RTPrefs.retrieveString(storage_key);
          // For the remaining lines, figure out some data types
          Map<String,Set<BundlesDT.DT>> datatypes = new HashMap<String,Set<BundlesDT.DT>>();
          for (int i=0;i<hdr.length;i++) datatypes.put(hdr[i], new HashSet<BundlesDT.DT>());
	  for (int i=first_line+1;i<lines.size();i++) {
            if (lines.get(i).length == tokens) {
	      for (int j=0;j<tokens;j++)
	        datatypes.get(hdr[j]).add(BundlesDT.getEntityDataType((lines.get(i))[j]));;
	    }
	  }

	  // Create the gui so that the user can configure the data
	  timestamps   = new JComboBox[tokens];
	  scalars      = new JCheckBox[tokens];
	  labels       = new JTextField[tokens];
	  to_lowercase = new JCheckBox[tokens];
	  ignore       = new JCheckBox[tokens];

          // Use a cascading borderlayout to get strips that are variable sized
          JPanel hdr_panel = new JPanel(new BorderLayout(5,0)); JPanel panel = hdr_panel, tmp_panel; JPanel strip = new JPanel(new GridLayout(hdr.length+1,1));
          panel.add("West", strip); strip.add(new JLabel("Orig"));       for (int i=0;i<hdr.length;i++) { strip.add(new JLabel(hdr[i])); }

          panel.add("East", tmp_panel=new JPanel(new BorderLayout(5,0))); strip     = new JPanel(new GridLayout(hdr.length+1,1));  tmp_panel.add("West",strip); panel = tmp_panel;
          strip.add(new JLabel("Field Name")); for (int i=0;i<hdr.length;i++) { strip.add(labels[i] = new JFocusTextField(restorePrefLabel(hdr[i], prefs, datatypes))); }

          panel.add("East", tmp_panel=new JPanel(new BorderLayout(5,0))); strip=new JPanel(new GridLayout(hdr.length+1,1)); tmp_panel.add("West",strip); panel = tmp_panel;
          strip.add(new JLabel("Scalar")); for (int i=0;i<hdr.length;i++) {
	    if (datatypes.get(hdr[i]).size() == 1 && datatypes.get(hdr[i]).iterator().next() == BundlesDT.DT.INTEGER) {
	      strip.add(scalars[i] = new JCheckBox("",restorePrefScalar(hdr[i], prefs))); 
              scalars[i].addChangeListener(new ChangeListener() {
	        public void stateChanged(ChangeEvent ce) {
		  for (int i=0;i<scalars.length;i++) {
		    if (scalars[i] == ce.getSource()) {
		      if (scalars[i].isSelected()) labels[i].setText(labels[i].getText().toUpperCase());
		      else                         labels[i].setText(labels[i].getText().toLowerCase());
		    }
		  }
		}
              } );
	    } else strip.add(new JLabel(""));
	  }

          panel.add("East", tmp_panel=new JPanel(new BorderLayout(5,0))); strip=new JPanel(new GridLayout(hdr.length+1,1)); tmp_panel.add("West",strip); panel = tmp_panel;
          strip.add(new JLabel("Lower")); for (int i=0;i<hdr.length;i++) { strip.add(to_lowercase[i] = new JCheckBox("",restorePrefLowerCase(hdr[i],prefs))); }

          panel.add("East", tmp_panel=new JPanel(new BorderLayout(5,0))); strip=new JPanel(new GridLayout(hdr.length+1,1)); tmp_panel.add("West",strip); panel = tmp_panel;
          strip.add(new JLabel("Ignore")); for (int i=0;i<hdr.length;i++) { 
            strip.add(ignore[i] = new JCheckBox("",restorePrefIgnore(hdr[i],prefs))); 
	    ignore[i].addChangeListener(new ChangeListener() {
	      public void stateChanged(ChangeEvent e) {
                for (int i=0;i<ignore.length;i++) {
		  if (e.getSource() == ignore[i]) {
		    boolean state;
		    if (ignore[i].isSelected()) { state = false; } else { state = true; }
                    labels[i].setEditable(state);
		    to_lowercase[i].setEnabled(state);
		    if (scalars[i]    != null) scalars[i].setEnabled(state);
		    if (timestamps[i] != null) timestamps[i].setEnabled(state);
		    return;
		  } } } } ); }

          panel.add("East", tmp_panel=new JPanel(new BorderLayout(5,0))); strip=new JPanel(new GridLayout(hdr.length+1,1)); tmp_panel.add("West",strip); panel = tmp_panel;
          strip.add(new JLabel("Time")); for (int i=0;i<hdr.length;i++) {
            if (datatypes.get(hdr[i]).size() == 1 && datatypes.get(hdr[i]).iterator().next() == BundlesDT.DT.TIMESTAMP) {
	      strip.add(timestamps[i] = new JComboBox(time_options));
              timestamps[i].setSelectedIndex(restorePrefTime(hdr[i],prefs));
	    } else strip.add(new JLabel(""));
	  }

	  // Add a checkbox to indicate that the data has a header
          panel = new JPanel(new GridLayout(3,1));
	  panel.add(data_has_header_cb = new JCheckBox("First Data Row Is A Header",    true));
	  panel.add(strip_spaces_cb    = new JCheckBox("Strip Before and After Spaces", restorePrefSpaces(prefs)));
	  panel.add(url_decode_cb      = new JCheckBox("URL Decode If Possible",        restorePrefURLDecode(prefs)));
            JPanel encode_panel = new JPanel(new BorderLayout()); encode_panel.add("Center", new JLabel("Encoding"));
	                                                          encode_panel.add("East",   encoding_cb = new JComboBox(encodings));
	  panel.add(encode_panel);
          getContentPane().add("North", panel);

	  getContentPane().add("Center", new JScrollPane(hdr_panel));

	  JPanel but_panel = new JPanel(new FlowLayout()); JButton bt;
	  but_panel.add(bt = new JButton("Import File")); bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { importFile(); } } );
	  but_panel.add(bt = new JButton("Cancel"));      bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { setVisible(false); } } );

	  getContentPane().add("South", but_panel);

	  pack(); setVisible(true);
	}
      }
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(this, "Error Retrieving Header Information", "File Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Simple class to select all the text when the component gains focus.  Simulates the
   * select all feature as a user tabs through the components.
   */
  class JFocusTextField extends JTextField implements FocusListener {
    public JFocusTextField(String str) { super(str); addFocusListener(this); }
    public void focusGained(FocusEvent fe) { selectAll(); }
    public void focusLost(FocusEvent fe) { }
  }

  /**
   * Return the preferred label based on the original header string.  If the prefs variable contains the preference,
   * restore that value.  Otherwise look to see if the datatypes are equivalent to integers.
   *
   *@param orig_hdr  original header from the file
   *@param prefs     preference settings for this header
   *@param datatypes parsed datatypes for the headers in the file
   *
   *@return preferred header
   */
  protected String restorePrefLabel(String orig_hdr, String prefs, Map<String,Set<BundlesDT.DT>> datatypes) {
    if (prefs != null) {
      String val = getPrefVal(prefs, Utils.encToURL(orig_hdr) + ".label");
      if (val != null) return val;
    }
    if (restorePrefScalar(orig_hdr, prefs)) {
      if (datatypes.get(orig_hdr).size() == 1 && datatypes.get(orig_hdr).iterator().next() == BundlesDT.DT.INTEGER) return orig_hdr.toUpperCase();
      else return orig_hdr.toLowerCase();
    } else return orig_hdr.toLowerCase();
  }

  /**
   * Generic method to get the specific key from the preference string.
   *
   *@param prefs preference string
   *@param key   setting to return
   *
   *@return value for the key/value pair if it exists, null otherwise
   */
  protected String getPrefVal(String prefs, String key) {
    key = "|" + key + "=";
    int    i   = prefs.indexOf(key);
    if (i >= 0) {
      String val = prefs.substring(i + key.length(), prefs.indexOf("|",i+1));
      return Utils.decFmURL(val);
    } else return null;
  }

  /**
   * Restore the preferred scalar setting for the original header string.
   *
   *@param orig_header header from the file
   *@param prefs       preference string
   *
   *@return preferred value setting, true otherwise
   */
  protected boolean restorePrefScalar(String orig_hdr, String prefs) {
    if (prefs != null) {
      String val = getPrefVal(prefs, Utils.encToURL(orig_hdr) + ".scalar");
      if (val != null) return val.equals("" + true);
    }
    return true;
  }

  /**
   * Restore the preferred lowercase setting for the original header string.
   *
   *@param orig_header header from the file
   *@param prefs       preference string
   *
   *@return preferred value setting, true otherwise
   */
  protected boolean restorePrefLowerCase(String orig_hdr, String prefs) {
    if (prefs != null) {
      String val = getPrefVal(prefs, Utils.encToURL(orig_hdr) + ".lower");
      if (val != null) return val.equals("" + true);
    } 
    return true;
  }

  /**
   * Restore the preferred ignore setting for the original header string.
   *
   *@param orig_header header from the file
   *@param prefs       preference string
   *
   *@return preferred value setting, false otherwise
   */
  protected boolean restorePrefIgnore(String orig_hdr, String prefs) {
    if (prefs != null) {
      String val = getPrefVal(prefs, Utils.encToURL(orig_hdr) + ".ignore");
      if (val != null) return val.equals("" + true);
    } 
    return false;
  }

  /**
   * Restore the preferred time setting for the original header string.
   *
   *@param orig_header header from the file
   *@param prefs       preference string
   *
   *@return preferred value setting
   */
  protected int restorePrefTime(String orig_hdr, String prefs) {
    if (prefs != null) {
      String val = getPrefVal(prefs, Utils.encToURL(orig_hdr) + ".time");
      if (val != null) return Integer.parseInt(val);
    } 
    return 0;
  }

  /**
   * Return the preference for the strip spaces setting.
   *
   *@param prefs       preference string
   *
   *@return the preference for the strip spaces setting
   */
  protected boolean restorePrefSpaces(String prefs) {
    if (prefs != null) {
      String val = getPrefVal(prefs, "-spaces-");
      if (val != null) return val.equals("" + true);
    } 
    return true;
  }

  /**
   * Return the preference for the URL-decode setting.
   *
   *@param prefs       preference string
   *
   *@return the preference for the URL-decode setting
   */
  protected boolean restorePrefURLDecode(String prefs) {
    if (prefs != null) {
      String val = getPrefVal(prefs, "-urlencode-");
      if (val != null) return val.equals("" + true);
    } 
    return false;
  }

  /**
   * Dropdown options for the timestamp setting combo box.
   */
  private String time_options[] = { "None", "Timestamp", "Timestamp End" }; 

  /**
   * The header from the filename
   */
  protected String     hdr[];

  /**
   * Number of the first line to begin parsing
   */
  protected int        first_line;

  /**
   * Comboboxes for the timestamps
   */
  protected JComboBox  timestamps[];

  /**
   * Checkbox for the scalar setting
   */
  protected JCheckBox  scalars[];

  /**
   * Checkbox for the lowercase setting
   */
  protected JCheckBox  to_lowercase[];

  /**
   * Checkbox for the ignore setting
   */
  protected JCheckBox  ignore[];

  /**
   * Checkbox for the strip spaces
   */
  protected JCheckBox  strip_spaces_cb;

  /**
   * Checkbox for the data header option
   */
  protected JCheckBox  data_has_header_cb;

  /**
   * Checkbox for the URL decode option
   */
  protected JCheckBox  url_decode_cb;

  /**
   * Combobox for the encoding options
   */
  protected JComboBox  encoding_cb;

  /**
   * List of supported encodings (probably a better way to get this -- i pulled this from the java docs)
   */
  protected String     encodings[] = { "None",
                                       "US-ASCII",
				       "ISO-8859-1",
                                       "UTF-8",
				       "UTF-16BE",
				       "UTF-16LE",
				       "UTF-16" };

  /**
   * Textfield for the user to modify the label
   */
  protected JTextField labels[];

  /**
   * Check the user settings (providing feedback as necessary) and then parse the file based
   * on the setting if everything is okay.
   */
  protected void importFile() {
    // Check requirements
    // - Are all fields ignored?
    boolean all_ignored = true;
    for (int i=0;i<ignore.length;i++) {
      if (ignore[i].isSelected() == false) all_ignored = false;
    }
    if (all_ignored) {
      JOptionPane.showMessageDialog(this, "All Fields Ignored, Try 'Cancel' Instead", "Labeling Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // - Are any fields duplicated?
    Set<String> dupe_test = new HashSet<String>();
    for (int i=0;i<labels.length;i++) {
      if (ignore[i].isSelected()) continue;
      if (dupe_test.contains(labels[i].getText())) {
        JOptionPane.showMessageDialog(this, "Label " + i + " Is A Duplicate (\"" + labels[i].getText() + "\")", "Labeling Error", JOptionPane.ERROR_MESSAGE);
	return;
      } else dupe_test.add(labels[i].getText());
    }

    // - Are any fields blank?
    for (int i=0;i<labels.length;i++) {
      if (ignore[i].isSelected()) continue;
      if (Utils.stripSpaces(labels[i].getText()).equals("")) {
        JOptionPane.showMessageDialog(this, "Label " + i + " Is Blank", "Labeling Error", JOptionPane.ERROR_MESSAGE);
	return;
      }
    }
    for (int i=0;i<labels.length;i++) {
      if (ignore[i].isSelected()) continue;
      if (scalars[i] != null && scalars[i].isSelected() && Utils.isAllUpper(labels[i].getText()) == false) {
        JOptionPane.showMessageDialog(this, "Change Label \"" + labels[i].getText() + "\" To All Uppercase (Scalar)", "Labeling Error", JOptionPane.ERROR_MESSAGE);
	return;
      }
      if ((scalars[i] == null || scalars[i].isSelected() == false) && Utils.isAllUpper(labels[i].getText()) == true)  {
        JOptionPane.showMessageDialog(this, "Change Label \"" + labels[i].getText() + "\" To At Least One Lowercase (Non-Scalar)", "Labeling Error", JOptionPane.ERROR_MESSAGE);
	return;
      }
    }
    int timestamp = 0, timestamp_end = 0; int timestamp_i = -1, timestamp_end_i = -1;
    for (int i=0;i<timestamps.length;i++) {
      if (ignore[i].isSelected()) continue;
      if (timestamps[i] != null) {
        if (timestamps[i].getSelectedIndex() == 1) { timestamp_i     = i; timestamp++;     }
        if (timestamps[i].getSelectedIndex() == 2) { timestamp_end_i = i; timestamp_end++; }
      }
    }
    if (timestamp == 0 && timestamp_end == 0) { } else if (timestamp == 1 && timestamp_end == 0) { } else if (timestamp == 1 && timestamp_end == 1) { } else {
        JOptionPane.showMessageDialog(this, "Timestamp Settings Incorrect - Must Be Unique", "Settings Error", JOptionPane.ERROR_MESSAGE);
	return;
    }
    
    // Create the prefs string
    StringBuffer prefs = new StringBuffer();
    for (int i=0;i<hdr.length;i++) {
      prefs.append("|" + Utils.encToURL(hdr[i]) + ".label=" + Utils.encToURL(labels[i].getText()) + "|");
      prefs.append("|" + Utils.encToURL(hdr[i]) + ".lower=" + to_lowercase[i].isSelected()        + "|");
      if (scalars[i]    != null) prefs.append("|" + Utils.encToURL(hdr[i]) + ".scalar=" + scalars[i].isSelected() + "|");
      prefs.append("|" + Utils.encToURL(hdr[i]) + ".ignore=" + ignore[i].isSelected() + "|");
      if (timestamps[i] != null) prefs.append("|" + Utils.encToURL(hdr[i]) + ".time=" + timestamps[i].getSelectedIndex() + "|");
    }
    prefs.append("|-urlencode-=" + url_decode_cb.isSelected()   +"|");
    prefs.append("|-spaces-="    + strip_spaces_cb.isSelected() +"|");

    RTPrefs.store(storage_key, prefs.toString());

    // Otherwise, we can parse the file
    try {
      // Copy the settings into arrays
      String  label_strs[]   = new String[labels.length];        for (int i=0;i<label_strs.length;i++)   label_strs[i]   = labels[i].getText();
      boolean scalar_flags[] = new boolean[scalars.length];      for (int i=0;i<scalar_flags.length;i++) if (scalars[i] == null) scalar_flags[i] = false; else scalar_flags[i] = scalars[i].isSelected();
      boolean ignore_flags[] = new boolean[ignore.length];       for (int i=0;i<ignore_flags.length;i++) ignore_flags[i] = ignore[i].isSelected();
      boolean lower_flags[]  = new boolean[to_lowercase.length]; for (int i=0;i<lower_flags.length;i++)  lower_flags[i]  = to_lowercase[i].isSelected();

      String  encoding = (String) encoding_cb.getSelectedItem(); if (encoding.equals("None")) encoding = null;

      // Call the parser
      Bundles bundles = parent.getRTParent().getRootBundles(); BundlesG globals = bundles.getGlobals();

      RFC4180Importer importer = new RFC4180Importer(file, 
                                                     data_has_header_cb.isSelected(), 
                                                     bundles, timestamp_i, timestamp_end_i, 
						     label_strs, scalar_flags, ignore_flags, lower_flags,
						     url_decode_cb.isSelected(),
						     strip_spaces_cb.isSelected(),
						     encoding);

      // Reset the transforms to force the lookups to be created
      System.err.println("**\n** Probably Need To Include Cached Bundles...\n**");
      Set<Bundles> bundles_set = new HashSet<Bundles>(); bundles_set.add(bundles);
      globals.cleanse(bundles_set);

      // Update the GUI with new field information
      parent.updateBys();
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(this, "Error Parsing File: " + ioe, "File Error", JOptionPane.ERROR_MESSAGE);
      System.err.println("IOException: " + ioe);
      ioe.printStackTrace(System.err);
    }
    setVisible(false);
  }

  /**
   * Consume a line of CSV input.  This provides the dialog with information about the headers
   * and fields.
   *
   *@param tokens     tokens from the line
   *@param line       complete line from the file
   *@param line_no    line number
   *
   *@return true to keep parsing
   */
  public boolean consume(String tokens[], String line, int line_no) {
    lines.add(tokens); 
    return lines.size() < 10;
  }

  /**
   * Handle (or rather don't handle) comment lines.
   *
   *@param line
   */
  public void    commentLine(String line) { }
}

