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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesDT;
import racetrack.framework.Tablet;

import racetrack.kb.RTComment;
import racetrack.kb.RTCommentPanel;

import racetrack.util.Interval;
import racetrack.util.SubText;
import racetrack.util.TimeStamp;
import racetrack.util.Utils;
import racetrack.visualization.BrewerColorScale;
import racetrack.visualization.RTColorManager;

/**
 * Panel for dynamically adjusting reports (comments) and interacting with knowledge base
 * information.
 *
 *@author  D. Trimm
 *@version 0.1
 */
public class RTReports extends RTPanel {
  /**
   * Textfield for searching through the reports
   */
  JTextField search_tf,
  /**
   * Textfield for searching through a specific report
   */
             report_search_tf;

  /**
   * Full title of the report
   */
  public static final String TITLES            = "Titles",
  /**
   * Abreviated title of the report
   */
                             SHORT_TITLES      = "Short Titles",
  /**
   * Total number of entities in the report
   */
			     TOTAL_ENTITIES    = "Total Entities",
  /**
   * Date of report
   */
			     DATE              = "Date",
  /**
   * Number of selected entities contained in the report
   */
			     SELECTED_ENTITIES = "Selected Entities",
  /**
   * Report tags
   */
                             TAGS              = "Tags";

  /**
   * Array of the labeling options
   */
  public static final String labeling_options[] = { TITLES, SHORT_TITLES, TOTAL_ENTITIES, DATE, TAGS, SELECTED_ENTITIES };

  /**
   * Mode for when cursor is over a report
   */
  public static final String MODE_NONE       = "None",
  /**
   * Show excerpts around selected entities
   */
                             MODE_EXCERPTS   = "Excerpts";

  /**
   * Array of the mode options
   */
  public static final String mode_options[] = { MODE_NONE, MODE_EXCERPTS };

  /**
   * GUI component for labeling options
   */
  JCheckBoxMenuItem labeling_cbmis[];

  /**
   * GUI option to indicate if selection of reports should propagate to the application selection
   */
  JCheckBoxMenuItem propagate_selection_cbmi,

  /**
   * GUI option to dynamically highlight entities and timestamps in other windows
   */
                    highlight_entities_cbmi,
  /**
   * GUI option to indicate that deletes should be confirmed via dialog
   */
                    safe_delete_cbmi;

  /**
   * GUI component for the mode options
   */
  JRadioButtonMenuItem mode_rbmis[];

  /**
   * Return the current mode of the visualization.
   *
   *@return mode string
   */
  public String getMode() {
    for (int i=0;i<mode_options.length;i++) if (mode_rbmis[i].isSelected()) return mode_options[i];
    return null;
  }

  /**
   * Mapping of the comment to the suggested color - the mapped value is the index
   * of a categorical color brewer scheme.
   */
  private static Map<RTComment,Integer> suggested_color_map = new HashMap<RTComment,Integer>();

  /**
   * Set the suggested color index for a report.
   *
   *@param comment comment to assign color index
   *@param color_i color index from the categorical brewer map
   */
  public static void setSuggestedColor(RTComment comment, int color_i) {
    suggested_color_map.put(comment, color_i);
  }

  /**
   * Mapping of the comments to the geometrical shape
   */
  private static Map<RTComment,String> suggested_shape_map = new HashMap<RTComment,String>();

  /**
   * Set the suggested shape for a report
   *
   *@param comment   comment to assign color index
   *@param shape_str shape string described in the Utils class
   */
  public static void setSuggestedShape(RTComment comment, String shape_str) {
    suggested_shape_map.put(comment, shape_str);
  }

  /**
   * Card layout for the main component
   */
  CardLayout cards;

  /**
   * Center panel of the display
   */
  JPanel        center_panel;

  /**
   * Card name for the dots view
   */
  final String  CARD_DOTS   = "Dots",

  /**
   * Card name for the report view
   */
                CARD_REPORT = "Report";

  /**
   * Report view panel
   */
  RTCommentPanel report_panel;

  /**
   * Radio button to indicate that the body of the text should be searched
   */
  JRadioButton   body_radio,

  /**
   * Radio button to indicate that just the title should be searched
   */
                 title_radio;

  /**
   * Constructor
   */
  public RTReports(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt) {
    super(win_type, win_pos, win_uniq, rt);
    center_panel = new JPanel(cards = new CardLayout());
    add("Center", center_panel);

      JPanel sub_panel = new JPanel(new BorderLayout());
      sub_panel.add("Center", component = new RTReportsComponent());

        JPanel search_panel = new JPanel(new FlowLayout());
        search_panel.add(new JLabel("Search"));
        search_panel.add(search_tf = new JTextField(18)); JButton bt;
        ButtonGroup bg = new ButtonGroup();
        search_panel.add(body_radio  = new JRadioButton("Body", true));  bg.add(body_radio);
        search_panel.add(title_radio = new JRadioButton("Title",false)); bg.add(title_radio);
        search_tf.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { genericSearch(title_radio.isSelected()); } } );

      sub_panel.add("South", search_panel);

    center_panel.add(sub_panel, CARD_DOTS);

      sub_panel = new JPanel(new BorderLayout());
      sub_panel.add("Center", report_panel = new RTCommentPanel(this, new RTComment("Test", "Test", "Test", System.currentTimeMillis(), UUID.randomUUID())));
       
        JPanel options_panel = new JPanel(new FlowLayout());
        options_panel.add(report_search_tf = new JTextField(18));
        report_search_tf.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { reportTextSearch(false); } } );
        options_panel.add(bt = new JButton("Next"));  bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { reportTextSearch(false); } } );
        options_panel.add(bt = new JButton("Prev"));  bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { reportTextSearch(true);  } } );
	options_panel.add(bt = new JButton("Close")); bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { cards.show(center_panel, CARD_DOTS); } } );

      sub_panel.add("South", options_panel);

    center_panel.add(sub_panel, CARD_REPORT);

    //
    // Populate the popup menu
    //

    JMenuItem mi;
    getRTPopupMenu().add(mi = new JMenuItem("New Report...")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { newReport(); } } );
    getRTPopupMenu().addSeparator();

    // Load files as reports
    getRTPopupMenu().add(mi = new JMenuItem("Load Files As Reports..."));               mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { loadFiles(false); } } );
    getRTPopupMenu().add(mi = new JMenuItem("Load Files As Reports (Match Selected)")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { loadFiles(true);  } } );
    getRTPopupMenu().addSeparator();

    // Clipboard operations
    getRTPopupMenu().add(mi = new JMenuItem("Copy (Sel) Report Entities")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { copyEntities(); } } );
    getRTPopupMenu().add(mi = new JMenuItem("Copy (Sel) Report Titles"));   mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { copyTitles(); } } );
    getRTPopupMenu().addSeparator();

    // Tablet operations
    getRTPopupMenu().add(mi = new JMenuItem("Create (Sel) Reports to Entities Tablet")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { createReportToEntityTablets(); } } );
    getRTPopupMenu().addSeparator();

    // Set operations
    getRTPopupMenu().add(mi = new JMenuItem("Subset Bundles With Entities")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { subsetBundles(); } } );
    getRTPopupMenu().addSeparator();

    // Mode Options
    mode_rbmis = new JRadioButtonMenuItem[mode_options.length]; bg = new ButtonGroup();
    for (int i=0;i<mode_rbmis.length;i++) {
      mode_rbmis[i] = new JRadioButtonMenuItem(mode_options[i]);
      bg.add(mode_rbmis[i]); if (i == 0) mode_rbmis[i].setSelected(true);
      getRTPopupMenu().add(mode_rbmis[i]);
      defaultListener(mode_rbmis[i]);
    }

    // Layouts
    getRTPopupMenu().addSeparator();

    JMenu layouts_menu = new JMenu("Layouts"); getRTPopupMenu().add(layouts_menu);
    layouts_menu.add(mi = new JMenuItem("By Time (Y-Axis Unchanged)"));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { layoutByTime(TimeLayouts.DEFAULT);  } } );
    layouts_menu.add(mi = new JMenuItem("By Time (Y Scaled By Entities)")); 
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { layoutByTime(TimeLayouts.ENTITIES); } } );
    layouts_menu.add(mi = new JMenuItem("By Time (Y Org By Shape/Color)")); 
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { layoutByTime(TimeLayouts.SHAPES);   } } );

    // Labeling
    getRTPopupMenu().addSeparator();
    
    labeling_cbmis = new JCheckBoxMenuItem[labeling_options.length];
    for (int i=0;i<labeling_cbmis.length;i++) {
      getRTPopupMenu().add(labeling_cbmis[i] = new JCheckBoxMenuItem(labeling_options[i]));
      defaultListener(labeling_cbmis[i]);
    }

    // Other options
    getRTPopupMenu().addSeparator();

    getRTPopupMenu().add(propagate_selection_cbmi = new JCheckBoxMenuItem("Select Entities Based On Comment Selection"));
    getRTPopupMenu().add(highlight_entities_cbmi  = new JCheckBoxMenuItem("Dynamically Highlight Entities", false));
    getRTPopupMenu().add(safe_delete_cbmi         = new JCheckBoxMenuItem("Confirm Deletes", true));

  }

  /**
   * Create a new report.  Popup the report entry components.
   */
  public void newReport() {
    RTComment comment = new RTComment("Title", "", System.getProperty("user.name"), System.currentTimeMillis(), UUID.randomUUID());
    getRTParent().addRTComment(comment);
    report_panel.setRTComment(comment);
    cards.show(center_panel, CARD_REPORT);
  }

  /**
   * File chooser object for loading files
   */
  JFileChooser file_chooser;

  /**
   * Load user-selected ascii/text files.  If the match_selected flag is set, only keep those that match.
   *
   *@param match_selected only keep a file if it matches an entity that is selected
   */
  public void loadFiles(boolean match_selected) {
    System.err.println("**\n** Load Comments From File:  Probably doesn't work correctly with CIDR...\n**");

    int errors = 0;
    // Create the file chooser
    if (file_chooser == null) { 
      file_chooser = new JFileChooser("."); 
      file_chooser.setMultiSelectionEnabled(true); 
      file_chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    }

    // Show the open file version
    if (file_chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

      // Try to load each file
      File files[] = file_chooser.getSelectedFiles();
      for (int i=0;i<files.length;i++) {
        errors += loadFile(files[i], match_selected);
      }
    }
  }

  /**
   * Load an individual file - recurse into directories (probably a bad idea).  Report the total number of errors encountered.
   *
   *@param file            file to load
   *@param match_selected  only keep files that match one of the selected entities
   */
  public int loadFile(File file, boolean match_selected) {
    if (file.isDirectory()) {
      int errors = 0;
      File files[] = file.listFiles(); for (int i=0;i<files.length;i++) errors += loadFile(files[i], match_selected);
      return errors;
    } else {
      try {
        // Create the report...  check valid flag
        RTComment comment = new RTComment(file); if (comment.valid()) {
          //
          // if match selected is set, iterator through the report entities -- add upon a match
          //
          if (match_selected) {
            Iterator<SubText> its = comment.listEntitiesAndRelationships().iterator();
            while (its.hasNext()) {
              String entity = its.next().toString();
              if (getRTParent().getSelectedEntities().contains(entity)) { getRTParent().addRTComment(comment); break; }
            }
          //
          // Otherwise, just add the report
          //
          } else getRTParent().addRTComment(comment);
          return 0;
        } else return 1;
      } catch (IOException ioe) { System.err.println("IOException: " + ioe); return 1; }
    }
  }

  /**
   * Create a special tablet that encapulates the entity to report relationships.
   */
  public void createReportToEntityTablets() {
    System.err.println("createReportToEntityTablets() - Need To Dedupe!");
    Iterator<RTComment> it;
    // Choose the selected if they are present, else use all the reports
    if (selected != null && selected.size() > 0) it = selected.iterator();
    else                                         it = getRTParent().commentsIterator();
    // Get the tablet
    String headers[] = { "timestamp", "timestamp_end", "title", "entity", "source" };
    Tablet tablet = getRTParent().getRootBundles().findOrCreateTablet(headers);
    // Go through each of the report
    while (it.hasNext()) {
      RTComment comment = it.next();
      String    title   = comment.getTitle();
      String    ts0_str = Utils.exactDate(comment.ts0()),
                ts1_str = Utils.exactDate(comment.ts1());
      String    source  = comment.getSource();

      // Get the entities
      Iterator<SubText> its = comment.listEntitiesAndRelationships().iterator();
      while (its.hasNext()) {
        SubText subtext = its.next();
        if (subtext instanceof TimeStamp || subtext instanceof Interval) { } else {
          String             entity = subtext.toString();
	  Map<String,String> attr   = new HashMap<String,String>();
          attr.put("title",  title);  // These need to match the headers array above
	  attr.put("entity", entity);
	  attr.put("source", source);
          tablet.addBundle(attr, ts0_str, ts1_str);
        }
      }
    }
    // Finalize the new tablet and bundles
    // Reset the transforms to force the lookups to be created
    System.err.println("**\n** Probably Need To Include Cached Bundles...\n**");
    Set<Bundles> bundles_set = new HashSet<Bundles>(); bundles_set.add(getRTParent().getRootBundles());
    getRTParent().getRootBundles().getGlobals().cleanse(bundles_set);
    // Update the dropdowns
    getRTParent().updateBys();
  }

  /**
   * Place the entities from the selected reports (or all if no selection is present) onto the clipboard.
   */
  public void copyEntities() {
    Iterator<RTComment> it;
    // Choose the selected if they are present, else use all the reports
    if (selected != null && selected.size() > 0) it = selected.iterator();
    else                                         it = getRTParent().commentsIterator();
    // Go through the reports and accumulate them to a stringbuffer
    StringBuffer sb = new StringBuffer(); Set<String> added = new HashSet<String>();
    while (it.hasNext()) {
      RTComment comment = it.next();
      Iterator<SubText> its = comment.listEntitiesAndRelationships().iterator();
      while (its.hasNext()) {
        String entity = its.next().toString();
	if (added.contains(entity) == false) { sb.append(entity + "\n"); added.add(entity); }
      }
    }
    // Push them to the string buffer
    Clipboard  clipboard = getToolkit().getSystemClipboard();
    StringSelection selection = new StringSelection(sb.toString());
    clipboard.setContents(selection, null);
  }

  /**
   * Copy the selected report titles to the clipboard. If none are selected, copy them all.
   */
  public void copyTitles() {
    Iterator<RTComment> it;
    // Choose the selected if they are present, else use all the reports
    if (selected != null && selected.size() > 0) it = selected.iterator();
    else                                         it = getRTParent().commentsIterator();
    // Go through the reports and accumulate them to a stringbuffer
    StringBuffer sb = new StringBuffer(); Set<String> added = new HashSet<String>();
    while (it.hasNext()) {
      RTComment comment = it.next();
      sb.append(comment.getTitle() + "\n");
    }
    // Push them to the string buffer
    Clipboard  clipboard = getToolkit().getSystemClipboard();
    StringSelection selection = new StringSelection(sb.toString());
    clipboard.setContents(selection, null);
  }

  /**
   * Extract the entities from the selected (or all if none are selected) reports.  Subset the visible
   * bundles to only those contained in the extracted entities list. This could be optimized based on
   * entity datatypes.
   */
  public void subsetBundles() {
    Set<String> entities = new HashSet<String>();

    // Aggregate all of the selected (or all) reports into a set of entities
    Iterator<RTComment> it = (selected.size() > 0) ? selected.iterator() : getRTParent().commentsIterator();
    while (it.hasNext()) {
      RTComment     comment = it.next();
      List<SubText> list    = comment.listEntitiesAndRelationships();
      for (int i=0;i<list.size();i++) entities.add(list.get(i).toString().toLowerCase());
    }

    // Check each visible record for inclusion
    Set<Bundle> subset = new HashSet<Bundle>();

    Bundles bundles = getRTParent().getVisibleBundles();
    Iterator<Tablet> it_tab = bundles.tabletIterator();
    while (it_tab.hasNext()) {
      Tablet tablet   = it_tab.next();
      int    fields[] = tablet.getFields();

      Iterator<Bundle> it_bun = tablet.bundleIterator();
      while (it_bun.hasNext()) {
        Bundle bundle = it_bun.next();
	for (int fld_i=0;fld_i<fields.length;fld_i++) {
	  if (fields[fld_i] != -1 && entities.contains(bundle.toString(fld_i))) subset.add(bundle);
        }
      }
    }

    // If that subset has any values, push it onto the stack
    if (subset.size() > 0) getRTParent().push(bundles.subset(subset));
  }

  /**
   * Search the report for the user supplied substring (or the selected entities).
   *
   *@param search_backwards use selected entities for the search
   */
  public void reportTextSearch(boolean search_backwards) {
    report_panel.find(report_search_tf.getText(), search_backwards);
  }

  /**
   * Generic search that places the matching comments into a selected set.
   *
   *@param title_search use the title for checking
   */
  public void genericSearch(boolean title_search) {
    String sub = search_tf.getText().toLowerCase();
    Set<RTComment> set = new HashSet<RTComment>();
    Iterator<RTComment> it = wxy_map.keySet().iterator();
    while (it.hasNext()) {
      RTComment comment = it.next();
      if      (title_search) { if (comment.getTitle().toLowerCase().indexOf(sub) >= 0) set.add(comment); }
      else if (comment.getText().toLowerCase().indexOf(sub)  >= 0) set.add(comment);
    }
    setSelected(set);
    getRTComponent().repaint();
  }

  /**
   *
   */
  Set<String> current_focus = new HashSet<String>();

  /**
   * Update the entities that are the focus of analysis in other windows.
   */
  public void updateFocusEntities(Set<String> focus) {
    if (focus.equals(current_focus) == false) { current_focus.clear(); current_focus.addAll(focus); repaint(); }
  }

  /**
   * Return the prefix for this component that uniquely identifies it.
   *
   *@return prefix for uniquifying this component
   */
  @Override
  public String getPrefix() { return "reports"; }

  /**
   * Set the configuration of this component by parsing the supplied string.
   *
   *@param config configuration string
   */
  @Override
  public void setConfig(String config) { 
    StringTokenizer st = new StringTokenizer(config, BundlesDT.DELIM);
    if (st.nextToken().equals("RTReports") == false) throw new RuntimeException("setConfig(" + config + ") - Not A RTReports");
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      StringTokenizer st2 = new StringTokenizer(token, "="); String type = st2.nextToken(), value = st2.nextToken(); 
      if        (type.equals("labeling")) {
	List<String> list = new ArrayList<String>();
	StringTokenizer st3 = new StringTokenizer(value, ",");
	while (st3.hasMoreTokens()) { list.add(Utils.decFmURL(st3.nextToken())); }
	String strs[] = new String[list.size()]; for (int i=0;i<strs.length;i++) strs[i] = list.get(i);
	setLabelingOptions(strs);
      } else if (type.equals("extents")) {
        StringTokenizer st3 = new StringTokenizer(value, ",");
	double x = Double.parseDouble(st3.nextToken()), y = Double.parseDouble(st3.nextToken()),
	       w = Double.parseDouble(st3.nextToken()), h = Double.parseDouble(st3.nextToken());
        setExtents(new Rectangle2D.Double(x,y,w,h));
      } else throw new RuntimeException("Do Not Understand Type Value \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * Return a string representing the configuration of this component.
   *
   *@return configuration string
   */
  @Override
  public String getConfig() {
    StringBuffer sb = new StringBuffer(); sb.append("RTReports");
    // Extents
    sb.append(BundlesDT.DELIM);
    sb.append("extents=" + extents.getX() + "," + extents.getY() + "," + extents.getWidth() + "," + extents.getHeight());
    // Labeling
    String strs[] = getLabelingOptions(); if (strs != null && strs.length > 0) {
      sb.append(BundlesDT.DELIM); sb.append("labeling=" + Utils.encToURL(strs[0]));
      for (int i=1;i<strs.length;i++) sb.append("," + Utils.encToURL(strs[i]));
    }
    return sb.toString();
  }

  /**
   * Indicate that additional state needs to be saved for this component.
   *
   *@return true
   */
  @Override
  public boolean hasAdditionalConfig() { return true; }

  /**
   * Add additional configuration information to the list parameter.  These configuration lines
   * will then be embedded into the save file.
   *
   *@param list              list to add the additional configuration lines
   *@param save_visible_only only save the visible information (doesn't apply to this specific component)
   */
  @Override
  public void addAdditionalConfig(List<String> list, boolean save_visible_only) {
    Iterator<RTComment> it = getRTParent().commentsIterator();
    while (it.hasNext()) {
      RTComment comment = it.next();
      int       color_i; if (suggested_color_map.containsKey(comment)) color_i = suggested_color_map.get(comment); else color_i = -1;
      list.add("#AC rep|uuid=" + comment.getUUID() + "|wx=" + wxy_map.get(comment).getX() + "|wy=" + wxy_map.get(comment).getY() + 
               "|shape=" + suggested_shape_map.get(comment) + "|color=" + color_i);
    }
    list.add("#AC reportsend");
  }

  /**
   * Parse additional configuraiton information from the list parameter.  This is additional
   * configuration necessary for the component to fully restore the state.
   *
   *@param lines  additional lines to process
   *@param line_i starting line for the parser
   *
   *@return next line for the overal application to parse (after this panels configuration)
   */
  @Override
  public int parseAdditionalConfig(List<String> lines, int line_i) {
    while (line_i < lines.size()) {
      String line = lines.get(line_i++);
      if        (line.startsWith("#AC rep|")) {
        StringTokenizer st        = new StringTokenizer(line, "|"), st2; st.nextToken();
	String          uuid_str  = st.nextToken(); st2 = new StringTokenizer(uuid_str, "="); st2.nextToken(); UUID   uuid    = UUID.fromString(st2.nextToken());
	String          wx_str    = st.nextToken(); st2 = new StringTokenizer(wx_str,   "="); st2.nextToken(); double wx      = Double.parseDouble(st2.nextToken());
	String          wy_str    = st.nextToken(); st2 = new StringTokenizer(wy_str,   "="); st2.nextToken(); double wy      = Double.parseDouble(st2.nextToken());
	String          shape_str = st.nextToken(); st2 = new StringTokenizer(shape_str,"="); st2.nextToken(); String shape   = st2.nextToken();
	String          color_str = st.nextToken(); st2 = new StringTokenizer(color_str,"="); st2.nextToken(); int    color_i = Integer.parseInt(st2.nextToken());

        RTComment comment = getRTParent().findRTComment(uuid);
	if (comment != null) {
	  wxy_map.put(comment, new Point2D.Double(wx, wy));
	  if (shape.equals("null") == false) suggested_shape_map.put(comment, shape);
	  if (color_i != -1)                 suggested_color_map.put(comment, color_i);
	}
      } else if (line.startsWith("#AC reportsend")) break;
    }
    return line_i;
  }

  /**
   * Get an array of the selected labels.
   *
   *@return array of labels
   */
  public String[] getLabelingOptions() {
    List<String> list = new ArrayList<String>();
    for (int i=0;i<labeling_cbmis.length;i++) { if (labeling_cbmis[i].isSelected()) list.add(labeling_options[i]); }
    String strs[] = new String[list.size()]; for (int i=0;i<strs.length;i++) strs[i] = list.get(i);
    return strs;
  }

  /**
   * Set the specified labeling options.
   *
   *@param str array of labels
   */
  public void setLabelingOptions(String str[]) {
    for (int i=0;i<labeling_cbmis.length;i++) labeling_cbmis[i].setSelected(false);
    for (int i=0;i<str.length;i++) {
      for (int j=0;j<labeling_options.length;j++) {
        if (str[i].equals(labeling_options[j])) labeling_cbmis[j].setSelected(true);
      }
    }
  }

  enum TimeLayouts { DEFAULT, ENTITIES, SHAPES };

  /**
   * Layout the reports by their creation time.
   */
  public void layoutByTime(TimeLayouts y_layout) {
    // Find the min/max times (and max entities, shape/color combos)
    long t_min = Long.MAX_VALUE, t_max = Long.MIN_VALUE; int max_entities = 1; List<String> shape_color_strs = new ArrayList<String>();
    Iterator<RTComment> it = getRTParent().commentsIterator();
    while (it.hasNext()) {
      RTComment comment = it.next();
      // Times
      long      t       = comment.getCreationTime();
      if (t < t_min) t_min = t; if (t > t_max) t_max = t;
      // Entities
      List<SubText> ents = comment.listEntitiesAndRelationships();
      if (ents.size() > max_entities) max_entities = ents.size();
      // Shapes / Colors
      String shape_str = Utils.CIRCLE_STR; int color_i = -1;
      if (suggested_shape_map.containsKey(comment)) shape_str = suggested_shape_map.get(comment);
      if (suggested_color_map.containsKey(comment)) color_i   = suggested_color_map.get(comment);
      shape_color_strs.add(shape_str + " | " + color_i);
    }
    if (t_min == Long.MAX_VALUE) return;           // No Reports...
    if (t_min == t_max)          t_max = t_min+1L; // Don't div by zero...

    // Sort the shape/color strings if that's the y-option
    Map<String,Integer> shape_color_lu = new HashMap<String,Integer>();
    if (y_layout == TimeLayouts.SHAPES) {
      Collections.sort(shape_color_strs);
      for (int i=0;i<shape_color_strs.size();i++) shape_color_lu.put(shape_color_strs.get(i), i);
    }

    // Layout the reports
    it = getRTParent().commentsIterator();
    while (it.hasNext()) {
      RTComment comment = it.next();
      long      t       = comment.getCreationTime();
      double    wx      = (10.0*(t - t_min))/(t_max - t_min);
      double    wy      = 0.0;
      if        (y_layout == TimeLayouts.ENTITIES) {
        List<SubText> ents = comment.listEntitiesAndRelationships();
	wy = 1.0 - ((double) ents.size())/max_entities;
      } else if (y_layout == TimeLayouts.SHAPES) {
        String shape_str = Utils.CIRCLE_STR; int color_i = -1;
        if (suggested_shape_map.containsKey(comment)) shape_str = suggested_shape_map.get(comment);
        if (suggested_color_map.containsKey(comment)) color_i   = suggested_color_map.get(comment);
        wy = ((double) shape_color_lu.get(shape_str + " | " + color_i))/shape_color_lu.keySet().size();
      } else wy = wxy_map.get(comment).getY();
      wxy_map.put(comment, new Point2D.Double(wx,wy));
    }

    // Ask for a render
    ((RTReportsComponent) getRTComponent()).fit();
    getRTComponent().render();
  }

  /**
   * Map from the comment to the world coordinates
   */
  Map<RTComment,Point2D> wxy_map = new HashMap<RTComment,Point2D>();

  /**
   * Viewport
   */
  Rectangle2D extents = new Rectangle2D.Double(0.0,0.0,1.0,1.0);

  /**
   * Return the current viewport.
   *
   *@return rectangle representing the screen coordinates in world space
   */
  public Rectangle2D getExtents() { return extents; }

  /**
   * Set the viewport extents and force the component to re-render.
   *
   *@param new_extents new extents
   */
  public void setExtents(Rectangle2D new_extents) { extents = new_extents; getRTComponent().render(); }

  /**
   * Set of visually-selected comments
   */
  Set<RTComment> selected = new HashSet<RTComment>();

  /**
   * Set the selected comments.  If user selected, set the selection to the application.
   *
   *@param set new selection set
   */
  public void setSelected(Set<RTComment> set) {
    selected = set;
    if (propagate_selection_cbmi.isSelected()) {
      Set<String> selected_entities = new HashSet<String>();
      Iterator<RTComment> it = set.iterator();
      while (it.hasNext()) {
        RTComment comment = it.next();
	List<SubText> subtexts = comment.listEntitiesAndRelationships();
	for (int i=0;i<subtexts.size();i++) {
	  selected_entities.add(subtexts.get(i).toString());
	}
      }
      getRTParent().setSelectedEntities(selected_entities);
    }
    repaint();
  }

  /**
   * Association of comments to bundles that contain at least one entity
   * from the comment
   */
  Map<RTComment,Set<Bundle>> comment_to_bundles = new HashMap<RTComment,Set<Bundle>>();

  /**
   * Component for this panel
   */
  public class RTReportsComponent extends RTComponent implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    /**
     *
     */
    @Override
    public Set<Shape> overlappingShapes(Shape shape) { return new HashSet<Shape>(); }

    /**
     *
     */
    @Override
    public Set<Bundle> shapeBundles(Shape shape) { return new HashSet<Bundle>(); }

    /**
     *
     */
    @Override
    public Set<Shape> shapes(Set<Bundle> bundles) { return new HashSet<Shape>(); }

    /**
     *
     */
    @Override
    public Set<Shape> allShapes() { return new HashSet<Shape>(); }

    /**
     * Mouse X Coordinate
     */
    int m_sx,
    /**
     * Mouse X Coordinate at press
     */
        m_sx0,
    /**
     * Mouse X Coordinate at release
     */
        m_sx1,
    /**
     * Mouse Y Coordinate
     */
        m_sy,
    /**
     * Mouse Y Coordinate at press
     */
        m_sy0,
    /**
     * Mouse Y Coordinate at release
     */
        m_sy1;
    /**
     * Mouse X World Coordinate
     */
    double m_wx,
    /**
     * Mouse X at press
     */
           m_wx0,
    /**
     * Mouse X at press
     */
           m_wx1,
    /**
     * Mouse Y World Coordinate
     */
           m_wy,
    /**
     * Mouse Y at press
     */
           m_wy0,
    /**
     * Mouse Y at release
     */
           m_wy1;

    /**
     * Comment under the mouse pointer
     */
    Set<RTComment> under_mouse = new HashSet<RTComment>();

    /**
     * Copy of the extents for panning the view around
     */
    Rectangle2D orig_extents;

    /**
     * Copy of the extents for panning the view around
     */
    RenderContext orig_myrc;

    /**
     * Grid layout key pressed
     */
    boolean grid_layout   = false,
    /**
     * Line layout key pressed
     */
            line_layout   = false,
    /**
     * Circle layout key pressed
     */
	    circle_layout = false,
    /**
     *  Laying out based on key pressed
     */
            laying_out    = false,
    /**
     * Selecting activated
     */
            selecting     = false;
    /**
     *
     */
    public void mousePressed(MouseEvent me) {
      RenderContext myrc = (RenderContext) rc;
      m_sx0 = me.getX(); if (myrc != null) m_wx0 = myrc.sxToWx(m_sx0);
      m_sy0 = me.getY(); if (myrc != null) m_wy0 = myrc.syToWy(m_sy0);
      if      (me.getButton() == MouseEvent.BUTTON3) super.mousePressed(me);
      else if (me.getButton() == MouseEvent.BUTTON2) { orig_extents = getExtents(); orig_myrc = myrc; }
      else if (me.getButton() == MouseEvent.BUTTON1) {
        if (under_mouse.size() > 0) { // Moving
	  // See if the what's under the mouse is a proper subset of selected
	  boolean under_mouse_is_subset_of_selected = true;
	  Iterator<RTComment> it = under_mouse.iterator();
	  while (it.hasNext() && under_mouse_is_subset_of_selected) {
            if (selected.contains(it.next()) == false) under_mouse_is_subset_of_selected = false;
	  }
	  // If it's not, reset the selected
          if (under_mouse_is_subset_of_selected == false) setSelected(under_mouse);
          // Set this group as the moving group
	  moving = selected; orig_myrc = myrc; repaint();
	} else                      { // (Probably) Selecting
          if (grid_layout || line_layout || circle_layout) { // layout
	    laying_out = true; repaint();
          } else { // selection
            selecting = true;  repaint();
          }
	}
      }
    }

    /**
     * Set of reports that are being moved
     */
    Set<RTComment> moving = null;

    /**
     *
     */
    public void mouseReleased(MouseEvent me) {
      RenderContext myrc = (RenderContext) rc;
      m_sx1 = me.getX(); if (myrc != null) m_wx1 = myrc.sxToWx(m_sx1);
      m_sy1 = me.getY(); if (myrc != null) m_wy1 = myrc.syToWy(m_sy1);
      Rectangle2D screen_rect = new Rectangle2D.Double(m_sx0 < m_sx1 ? m_sx0 : m_sx1,
                                                       m_sy0 < m_sy1 ? m_sy0 : m_sy1, Math.abs(m_sx1 - m_sx0), Math.abs(m_sy1 - m_sy0));

      if      (me.getButton() == MouseEvent.BUTTON3) super.mouseReleased(me);
      else if (me.getButton() == MouseEvent.BUTTON2) { orig_extents = null; orig_myrc = null; }
      else if (me.getButton() == MouseEvent.BUTTON1) { 
	/*        */
	/* Moving */
	/*        */
        if        (moving != null) { moving     = null; 

	/*        */
	/* Layout */
	/*        */
	} else if (laying_out)     { laying_out = false;
	  if        (line_layout)   { 
            double dx, dy;
            if      (last_shft_down) { dx = m_wx1 - m_wx0; dy = 0.0;           }
            else if (last_ctrl_down) { dx = 0.0;           dy = m_wy1 - m_wy0; }
            else                     { dx = m_wx1 - m_wx0; dy = m_wy1 - m_wy0; }
            double denom = selected.size()-1; if (denom < 1.0) denom = 1.0;
	    Iterator<RTComment> it = selected.iterator(); int i = 0;
	    while (it.hasNext()) { wxy_map.put(it.next(), new Point2D.Double(m_wx0 + dx*i/denom, m_wy0 + dy*i/denom)); i++; }
	  } else if (circle_layout) {


	  } else                    {
            double dx = m_wx1 - m_wx0, dy = m_wy1 - m_wy0; 
            // Copy from RTGraphPanel
            int    sqrt = (int) Math.sqrt(selected.size()), max_x_i = 1, max_y_i = 1;
            if (dx < 0.01) dx = 0.01; if (dy < 0.01) dy = 0.01;
            if        ((dx/dy) > 1.5 || (dy/dx) > 1.5) { // Rectangular
              double closest_dist = Double.POSITIVE_INFINITY;
              for (int i=1;i<=sqrt;i++) {
                int    other = selected.size()/i;
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
	    if (max_x_i < 1) max_x_i = 1; if (max_y_i < 1) max_y_i = 1;

	    // Go through the elements and lay them out
            int x_i = 0, y_i = 0;
	    Iterator<RTComment> it = selected.iterator();
	    while (it.hasNext()) {
	      RTComment comment = it.next();
	      wxy_map.put(comment, new Point2D.Double(m_wx0 + x_i*dx/max_x_i, m_wy0 + y_i*dy/max_y_i));
	      x_i++;
              if (x_i >= max_x_i) { y_i++; x_i = 0; }
	    }
	  }
	  render();

	/*           */
	/* Selecting */
	/*           */
	} else if (selecting)      { selecting  = false;
	  if (myrc != null) {
	    Set<RTComment>      in_rect = new HashSet<RTComment>();
            Iterator<RTComment> it      = myrc.shape_map.keySet().iterator();
	    while (it.hasNext()) {
	      RTComment comment = it.next(); Shape comment_shape = myrc.shape_map.get(comment);
              if (Utils.genericIntersects(screen_rect, comment_shape)) in_rect.add(comment);
	    }
	    Set<RTComment> selected_copy = new HashSet<RTComment>(); selected_copy.addAll(selected);
            if        (last_shft_down && last_ctrl_down) { selected_copy.retainAll(in_rect);
	    } else if (last_ctrl_down)                   { selected_copy.addAll(in_rect);
	    } else if (last_shft_down)                   { selected_copy.removeAll(in_rect);
	    } else                                       { selected_copy = in_rect; }
	    setSelected(selected_copy);
	    repaint();
          }
	}
	orig_myrc = null; 
      }
    }

    /**
     *
     */
    public void mouseClicked(MouseEvent me) {
      if      (me.getButton() == MouseEvent.BUTTON3) super.mouseClicked(me);
      else if (me.getButton() == MouseEvent.BUTTON2) fit();
      else if (me.getButton() == MouseEvent.BUTTON1) {
        if (me.getClickCount() == 2 && under_mouse != null && under_mouse.size() > 0) {
	  RTComment comment = under_mouse.iterator().next();
	  report_panel.setRTComment(comment);
          cards.show(center_panel, CARD_REPORT);
	} else {
          setSelected(under_mouse);
	  repaint();
        }
      }
    }

    /**
     *
     */
    public void mouseMoved(MouseEvent me) {
      RenderContext myrc = (RenderContext) rc;
      m_sx = me.getX(); if (myrc != null) m_wx = myrc.sxToWx(m_sx);
      m_sy = me.getY(); if (myrc != null) m_wy = myrc.syToWy(m_sy);
      // Determine what's under the mouse
      Iterator<RTComment> it = getRTParent().commentsIterator();
      Set<RTComment> set = new HashSet<RTComment>();
      if (myrc != null) {
        while (it.hasNext()) {
          RTComment comment = it.next();
          if (myrc.shape_map.containsKey(comment)) {
            Shape shape = myrc.shape_map.get(comment);
            if (shape.contains(m_sx, m_sy)) set.add(comment);
          }
        }
      }
      // Don't overdraw if we can help it
      Set<RTComment> under_mouse_copy = under_mouse; boolean update_required = false;
      if (under_mouse_copy != null && under_mouse_copy.size() == set.size()) {
        it = under_mouse_copy.iterator(); while (it.hasNext()) {
	  if (set.contains(it.next()) == false) {
	    under_mouse = set;
	    setEntityHighlights(set);
	    render(); update_required = true;
	    break;
	  }
	}
	repaint();
      } else {
        under_mouse = set;
	setEntityHighlights(set);
	render(); update_required = true;
      }

      // If update required, push the subtexts to the parent if the excerpt mode is enabled
      if (update_required && getMode().equals(MODE_EXCERPTS)) {
        Map<String,Set<SubText>> entity_subtext_lu = new HashMap<String,Set<SubText>>();
	// Go through and pull the subtexts with excerpts
	it = under_mouse.iterator();
	while (it.hasNext()) {
	  List<SubText> subtexts = it.next().listEntitiesAndRelationships();
	  if (subtexts != null && subtexts.size() > 0) {
	    Iterator<SubText> it_sub = subtexts.iterator();
	    while (it_sub.hasNext()) {
	      SubText subtext = it_sub.next();
              String  excerpt = SubText.extractExcerpt(subtext);
              if (excerpt != null) {
	        if (entity_subtext_lu.containsKey(subtext.toString()) == false) entity_subtext_lu.put(subtext.toString(), new HashSet<SubText>());
		entity_subtext_lu.get(subtext.toString()).add(subtext);
	      }
	    }
	  }
	}

        // Let the application know...
        getRTParent().setExcerptMap(entity_subtext_lu);
      }
    }

    /**
     *
     */
    public void setEntityHighlights(Set<RTComment> comments) {
      Set<SubText> subs = new HashSet<SubText>();

      if (highlight_entities_cbmi.isSelected()) {
        if (comments != null && comments.size() > 0) {
          Iterator<RTComment> it = comments.iterator(); 
          while (it.hasNext()) {
            RTComment     comment  = it.next();
            List<SubText> entities = comment.listEntitiesAndRelationships();
	    subs.addAll(entities);
	    subs.add(new TimeStamp(comment.getText(), Utils.exactDate(comment.getCreationTime()), 0, 1));
          }
        }
        getRTParent().setEntityHighlights(subs);
      } else if (getRTParent().getEntityHighlights().size() != 0) getRTParent().setEntityHighlights(new HashSet<SubText>());
    }

    /**
     * Handle mouse drag events.
     *
     *@param em mouse event
     */
    public void mouseDragged(MouseEvent me) {
      RenderContext myrc = (RenderContext) rc;
      m_sx = me.getX(); 
      m_sy = me.getY(); 

      double d_wx = 0.0, d_wy = 0.0;
      if (myrc != null) {
        d_wx = m_wx - myrc.sxToWx(m_sx);
        d_wy = m_wy - myrc.syToWy(m_sy);
        m_wx = myrc.sxToWx(m_sx);
        m_wy = myrc.syToWy(m_sy);
      }

      if (orig_extents != null && orig_myrc != null) {
        setExtents(new Rectangle2D.Double(orig_extents.getX() - (orig_myrc.sxToWx(m_sx) - m_wx0),
	                                  orig_extents.getY() - (orig_myrc.syToWy(m_sy) - m_wy0),
				 	  orig_extents.getWidth(),
					  orig_extents.getHeight()));
      } else if (moving != null && orig_myrc != null) {
        Iterator<RTComment> it = moving.iterator();
	while (it.hasNext()) {
	  RTComment comment = it.next();
	  if (wxy_map.containsKey(comment)) {
	    Point2D pt = wxy_map.get(comment);
	    wxy_map.put(comment, new Point2D.Double(pt.getX() - d_wx, pt.getY() - d_wy));
	  }
	}
	render();
      } else if (selecting || laying_out) {
        repaint();
      }
    }

    /**
     * Handle mouse wheel movement -- in this case, zoom in/out the view.
     *
     *@param mwe mouse wheel event
     */
    public void mouseWheelMoved(MouseWheelEvent mwe) {
      zoomIn(-mwe.getWheelRotation(), m_wx, m_wy);
    }

    /**
     * Handle key press events.
     *
     *@param ke key event
     */
    public void keyPressed(KeyEvent ke) {
      super.keyPressed(ke);
      // Keep track of the layout modifiers
      if      (ke.getKeyCode() == KeyEvent.VK_G) grid_layout   = true;
      else if (ke.getKeyCode() == KeyEvent.VK_Y) line_layout   = true;
      else if (ke.getKeyCode() == KeyEvent.VK_C) circle_layout = true;
      else if (ke.getKeyCode() == KeyEvent.VK_T) {
        if        (last_shft_down) {
	  Iterator<RTComment> it = selected.iterator(); while (it.hasNext()) { RTComment comment = it.next(); Point2D pt = wxy_map.get(comment);
	    wxy_map.put(comment, new Point2D.Double(pt.getX(), m_wy)); } render();
	} else if (last_ctrl_down)  {
	  Iterator<RTComment> it = selected.iterator(); while (it.hasNext()) { RTComment comment = it.next(); Point2D pt = wxy_map.get(comment);
	    wxy_map.put(comment, new Point2D.Double(m_wx, pt.getY())); } render();
	} else                      {
	  Iterator<RTComment> it = selected.iterator(); while (it.hasNext()) { RTComment comment = it.next(); Point2D pt = wxy_map.get(comment);
	    wxy_map.put(comment, new Point2D.Double(m_wx, m_wy)); } render();
	}
      } else if (ke.getKeyCode() == KeyEvent.VK_Q) {
        Set<RTComment> set = new HashSet<RTComment>();
        Iterator<RTComment> it = getRTParent().commentsIterator();
	while (it.hasNext()) {
	  RTComment comment = it.next();
	  if (selected.contains(comment) == false) set.add(comment);
	}
	setSelected(set); repaint();
      } else if (ke.getKeyCode() == KeyEvent.VK_DELETE) {
        boolean delete_confirmed = false;
        if (safe_delete_cbmi.isSelected()) {
	  delete_confirmed = JOptionPane.showConfirmDialog(this, "Press 'Yes' To Confirm Delete", "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	} else delete_confirmed = true;
	if (delete_confirmed) { 
	  Set<RTComment> selected_copy = new HashSet<RTComment>(); selected_copy.addAll(selected);
	  setSelected(new HashSet<RTComment>());
	  getRTParent().deleteRTComments(selected_copy); 
	}
      }
    }

    /**
     * Handle key releases.  Mostly turning off layout options.
     *
     *@param ke key event
     */
    public void keyReleased(KeyEvent ke) {
      super.keyReleased(ke);
      // Keep track of the layout modifiers
      if      (ke.getKeyCode() == KeyEvent.VK_G) grid_layout   = false;
      else if (ke.getKeyCode() == KeyEvent.VK_Y) line_layout   = false;
      else if (ke.getKeyCode() == KeyEvent.VK_C) circle_layout = false;
    }

    /**
     * Handle key type events.  In this case, pass to the super class.
     *
     *@param ke key event
     */
    public void keyTyped(KeyEvent ke) { super.keyTyped(ke); }

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
     * Fit the current view port to the data elements in the view.
     */
    public void fit() {
      double x0 = Double.POSITIVE_INFINITY, y0 = Double.POSITIVE_INFINITY,
	     x1 = Double.NEGATIVE_INFINITY, y1 = Double.NEGATIVE_INFINITY;
      Iterator<RTComment> it = getRTParent().commentsIterator();
      while (it.hasNext()) {
        Point2D pt = wxy_map.get(it.next());
	if (pt.getX() < x0) x0 = pt.getX();
	if (pt.getY() < y0) y0 = pt.getY();
	if (pt.getX() > x1) x1 = pt.getX();
	if (pt.getY() > y1) y1 = pt.getY();
      }
      if (Double.isInfinite(x0)) { setExtents(new Rectangle2D.Double(0.0, 0.0, 1.0, 1.0)); render(); 
      } else {
        if (x0 == x1) {x0 -= 0.5; x1 += 0.5; }
	if (y0 == y1) {y0 -= 0.5; y1 += 0.5; }
	double ten_perc = (x1 - x0) * 0.10; x0 -= ten_perc; x1 += ten_perc;
	       ten_perc = (y1 - y0) * 0.10; y0 -= ten_perc; y1 += ten_perc;
	setExtents(new Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0));
      }
      render();
    }

    /**
     * Create the nender context for the GUI settings and application state.
     *
     *@param id render id in case the user creates a new rendering before this one completes
     *
     *@return render context describing the geometry for the current view
     */
    public RTRenderContext render(short id) {
      Bundles bs = getRenderBundles(); 
      if (bs != null) {
        RenderContext myrc = new RenderContext(id, bs, getWidth(), getHeight(), extents, wxy_map, getLabelingOptions());
	return myrc;
      }
      return null;
    }

    /**
     * Render the base image (through the superclass) and draw the interaction.
     *
     *@param g graphics primitive
     */
    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      RenderContext myrc = (RenderContext) rc;
      if (myrc != null) {
        Graphics2D g2d = (Graphics2D) g; Composite orig_comp = g2d.getComposite(); Stroke orig_stroke = g2d.getStroke();

	// Some helper variables
	int txt_h = Utils.txtH(g2d, "0123456789");
	boolean labels_showing = false;
	for (int i=0;i<labeling_cbmis.length;i++) if (labeling_cbmis[i].isSelected()) labels_showing = true;

        // Re-render if the selection has changed
        if (getRTParent().getSelectedEntities().equals(myrc.selected_entities) == false) { render(); return; }

	// Outline the selected nodes
        g2d.setStroke(new BasicStroke(2.0f));
        Iterator<RTComment> it = selected.iterator();
        while (it.hasNext()) {
	  RTComment comment = it.next();
	  Shape shape = myrc.shape_map.get(comment);
	  if (shape != null) {
	    g2d.setColor(RTColorManager.getColor("linknode", "movenodes"));
	    g2d.draw(shape);

	    // Draw the title for selected reports... try to keep it on the screen
	    if (labels_showing == false) {
	      String title = comment.getTitle();
	      int title_cx  = (int) shape.getBounds().getCenterX();
	      int txt_w     = Utils.txtW(g2d, title);
	      int title_x0  = title_cx - txt_w/2, title_x1  = title_cx + txt_w/2;
	      if      (title_x0 <  0 && title_x1 <  getWidth()) { title_x0 = 0;                  }
	      else if (title_x0 >= 0 && title_x1 >= getWidth()) { title_x0 = getWidth() - txt_w; }
	      clearStr(g2d, title, title_x0, (int) (shape.getBounds().getMaxY() + txt_h), RTColorManager.getColor("annotate", "labelfg"), RTColorManager.getColor("annotate", "labelbg"));
	    }
	  }
	}
        g2d.setStroke(orig_stroke);

	// Draw the interaction
	if (selecting || laying_out) {
          g2d.setColor(RTColorManager.getColor("select", "region"));
	  Shape shape;
	  if        (laying_out && line_layout)   { 
            if      (last_shft_down) shape = new Line2D.Double(m_sx0, m_sy0, m_sx,  m_sy0);
            else if (last_ctrl_down) shape = new Line2D.Double(m_sx0, m_sy0, m_sx0, m_sy);
            else                     shape = new Line2D.Double(m_sx0, m_sy0, m_sx,  m_sy);
	  } else if (laying_out && circle_layout) { double rw = Math.abs(m_sx - m_sx0)/2, rh = Math.abs(m_sy - m_sy0)/2;
					            shape = new Ellipse2D.Double(m_sx0 - rw, m_sy0 - rh, rw*2, rh*2);
	  } else                                  { shape = new Rectangle2D.Double(m_sx < m_sx0 ? m_sx : m_sx0,
	                                                                           m_sy < m_sy0 ? m_sy : m_sy0,
										   Math.abs(m_sx - m_sx0), Math.abs(m_sy - m_sy0)); }
	  g2d.draw(shape); g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
	  g2d.fill(shape); g2d.setComposite(orig_comp); 

          int txt_x = m_sx < m_sx0 ? m_sx : m_sx0,
	      txt_y = m_sy < m_sy0 ? m_sy : m_sy0;

	  if      (laying_out && grid_layout)                     g2d.drawString("Grid Layout",          txt_x, txt_y);
	  else if (selecting && last_shft_down && last_ctrl_down) g2d.drawString("Selecting (Intersect)",txt_x, txt_y);
	  else if (selecting && last_shft_down)                   g2d.drawString("Selecting (Subtract)", txt_x, txt_y);
	  else if (selecting && last_ctrl_down)                   g2d.drawString("Selecting (Add)",      txt_x, txt_y);
	  else if (selecting)                                     g2d.drawString("Selecting",            txt_x, txt_y);
	} else if (under_mouse.size() > 0 && labels_showing == false) {
	  int txt_y_base = Integer.MAX_VALUE;
	  it = under_mouse.iterator();
	  while (it.hasNext()) {
	    RTComment comment = it.next();
	    Shape     shape   = myrc.shape_map.get(comment); if (shape == null) continue;
	    if (txt_y_base == Integer.MAX_VALUE) txt_y_base = (int) (shape.getBounds().getMaxY() + txt_h); else txt_y_base += txt_h;
	    if (shape != null) {
	      String title = comment.getTitle();
	      int title_cx  = (int) shape.getBounds().getCenterX();
	      int txt_w     = Utils.txtW(g2d, title);
	      int title_x0  = title_cx - txt_w/2, title_x1  = title_cx + txt_w/2;
	      if      (title_x0 <  0 && title_x1 <  getWidth()) { title_x0 = 0;                  }
	      else if (title_x0 >= 0 && title_x1 >= getWidth()) { title_x0 = getWidth() - txt_w; }
	      clearStr(g2d, title, title_x0, txt_y_base, RTColorManager.getColor("annotate", "labelfg"), RTColorManager.getColor("annotate", "labelbg"));
	    }
	  }
	}

        // Provide contextual information
        Set<String> focus_copy = new HashSet<String>(); if (current_focus != null) focus_copy.addAll(current_focus);
	String      mode       = getMode();
        if (mode != null && mode.equals(MODE_EXCERPTS) && focus_copy.size() > 0) {
          Area fill_area = new Area();  // Keeps track of what's been filled with callouts
          Iterator<RTComment> itr = myrc.sxy_map.keySet().iterator();
          while (itr.hasNext()) {
	    RTComment comment = itr.next(); int sx = (int) myrc.sxy_map.get(comment).getX(), sy = (int) myrc.sxy_map.get(comment).getY();
            Set<String> intersection = new HashSet<String>(); intersection.addAll(focus_copy); intersection.retainAll(commentEntities(comment));
            if (intersection.size() > 0) {
              // g2d.drawString("" + intersection, (int) myrc.sxy_map.get(comment).getX(), (int) myrc.sxy_map.get(comment).getY());
              Iterator<SubText> its = comment.listEntitiesAndRelationships().iterator(); List<SubText> list = new ArrayList<SubText>();
              while (its.hasNext()) {
	        SubText subtext = its.next();
		if (intersection.contains(subtext.toString())) list.add(subtext);
              }
	      SubText.renderContextHints(g2d, list, sx, sy, new Rectangle2D.Double(0,0,getWidth(),getHeight()), fill_area);
	    }
          }
        }
      }
    }

    /**
     * Return the entities within a comment as a set.
     *
     *@param comment comment to examine for entities
     *
     *@return set of entities extracted from that comment
     */
    public Set<String> commentEntities(RTComment comment) {
      Set<String>   set      = new HashSet<String>();
      List<SubText> subtexts = comment.listEntitiesAndRelationships();
      for (int i=0;i<subtexts.size();i++) set.add(subtexts.get(i).toString());
      return set;
    }

    /**
     * Class maintaining information on this specific rendering.
     */
    public class RenderContext extends RTRenderContext {
      /**
       * Width of the context
       */
      int         rc_w, 
      /**
       * Height of the context
       */
                  rc_h;
      /**
       * Bundles to render
       */
      Bundles     bs;
      /**
       * Viewport extents
       */
      Rectangle2D ext;
      /**
       * Labeling options
       */
      String      labels[];
      /**
       * Mapping of the report to the screen coordinate
       */
      Map<RTComment,Point2D>   sxy_map = new HashMap<RTComment,Point2D>();

      /**
       * Mapping of the comments to the geometrical shape
       */
      Map<RTComment,Shape>     shape_map = new HashMap<RTComment,Shape>();


      /**
       * Selected entities at the rendering of this context
       */
      Set<String> selected_entities = new HashSet<String>();

      /**
       * Construct the render context.
       *
       *@param id     render id for early termination
       *@param bs     bundles to render
       *@param w0     width of the context
       *@param h0     height of the context
       *@param ext    viewport extents
       *@param wxy    comment to world coordinates
       *@param labels labeling options
       */
      public RenderContext(short id, Bundles bs, int w0, int h0, Rectangle2D ext, Map<RTComment,Point2D> wxy, String labels[]) {
        this.render_id = id; this.bs   = bs; this.rc_w = w0; this.rc_h = h0;
	this.ext       = new Rectangle2D.Double(ext.getX(), ext.getY(), ext.getWidth(), ext.getHeight());
        this.labels    = labels;

        this.selected_entities = getRTParent().getSelectedEntities();

        // Go through the application-level reports
        Iterator<RTComment> it = getRTParent().commentsIterator(); Set<RTComment> in_map = new HashSet<RTComment>(); in_map.addAll(wxy.keySet());
	while (it.hasNext()) {
          RTComment comment = it.next(); in_map.remove(comment);
	  if (wxy.containsKey(comment) == false) wxy.put(comment, new Point2D.Double(Math.random(), Math.random()));
	  int sx = wxToSx(wxy.get(comment).getX()), sy = wyToSy(wxy.get(comment).getY());
          sxy_map.put(comment, new Point2D.Double(sx,sy));
	}

	// Remove mappings for any comments that no longer exist
	it = in_map.iterator(); while (it.hasNext()) wxy.remove(it.next());
      }

      /**
       * Convert a world x coordinate into a screen x coordinate
       *
       *@param wx world coordinate
       *
       *@return screen coordinate
       */
      public int     wxToSx  (double wx)     { return (int) (rc_w * (wx - ext.getMinX()) / ext.getWidth());  }

      /**
       * Convert a world y coordinate into a screen y coordinate
       *
       *@param wy world coordinate
       *
       *@return screen coordinate
       */
      public int     wyToSy  (double wy)     { return (int) (rc_h * (wy - ext.getMinY()) / ext.getHeight()); }

      /**
       * Convert a screen x coordinate into a world x coordiante.
       *
       *@param sx screen x coordinate
       *
       *@return world x coordinate
       */
      public double  sxToWx  (int    sx)     { return ((sx * ext.getWidth()) /rc_w)  + ext.getMinX(); }

      /**
       * Convert a screen y coordinate into a world y coordiante.
       *
       *@param sy screen y coordinate
       *
       *@return world y coordinate
       */
      public double  syToWy  (int    sy)     { return ((sy * ext.getHeight())/rc_h) + ext.getMinY(); }

      /**
       * Return the width of the render context
       *
       *@return render context width
       */
      @Override
      public int getRCWidth() { return rc_w; }

      /**
       * Return the height of the render context
       *
       *@return render context height
       */
      @Override
      public int getRCHeight() { return rc_h; }

      /**
       * Rendered image
       */
      BufferedImage base_bi;

      /**
       *
       */
      BrewerColorScale brewer = new BrewerColorScale(BrewerColorScale.BrewerType.QUALITATIVE, 7);

      /**
       *
       */
      @Override
      public BufferedImage getBase() {
        if (base_bi == null) {
          base_bi = new BufferedImage(rc_w, rc_h, BufferedImage.TYPE_INT_RGB); Graphics2D g2d = (Graphics2D) base_bi.getGraphics();
	  int txt_h = Utils.txtH(g2d, "0123456789");
	  try {
	    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    RTColorManager.renderVisualizationBackground(base_bi, g2d);

	    Iterator<RTComment> it = sxy_map.keySet().iterator();
	    while (it.hasNext()) {
              RTComment comment = it.next();
	      Point2D   sxy     = sxy_map.get(comment);
	      if (sxy != null) {
	        int sx = (int) sxy.getX(), sy = (int) sxy.getY();
		if (sx > -10 && sy > -10 && sx < rc_w+10 && sy < rc_w+10) {
		  Shape shape = null; Color color = Color.lightGray;
		  if (suggested_shape_map.containsKey(comment)) { 
                    Utils.Symbol symbol = Utils.parseSymbol(suggested_shape_map.get(comment));
		    shape = Utils.shape(symbol, sx - 6, sy - 6, 12);
                  } else shape = new Ellipse2D.Double(sx - 6, sy - 6, 12, 12);
		  if (suggested_color_map.containsKey(comment)) { 
		    int color_i = suggested_color_map.get(comment); if (color_i < 0) color_i = 0;
                    color = brewer.atIndex(color_i % brewer.numOfColors());
                  } else color = RTColorManager.getColor("data","default");
                  g2d.setColor(color);
		  g2d.fill(shape);
		  shape_map.put(comment, shape.getBounds());

		  // Set up the labeler
		  List<String> to_draw = new ArrayList<String>(); List<Color> to_color = new ArrayList<Color>();
		  // Titles
                  for (int i=0;i<labels.length;i++) if (labels[i].equals(TITLES))
		    { to_draw.add(comment.getTitle()); to_color.add(RTColorManager.getColor("label","default")); }
		  // Short Titles
                  for (int i=0;i<labels.length;i++) if (labels[i].equals(SHORT_TITLES))
		    { to_draw.add(Utils.shorten(comment.getTitle(), 16)); to_color.add(RTColorManager.getColor("label","default")); }
		  // Entities 
                  for (int i=0;i<labels.length;i++) if (labels[i].equals(TOTAL_ENTITIES)) { 
		    int size = comment.listEntitiesAndRelationships().size();
		    if (size > 0) to_draw.add("" + size + " Ents"); to_color.add(RTColorManager.getColor("label","major")); 
		  }
		  // Date
                  for (int i=0;i<labels.length;i++) if (labels[i].equals(DATE))
		    { to_draw.add(Utils.dayDateStr(comment.getCreationTime())); to_color.add(RTColorManager.getColor("label","major")); }
                  // Tags
                  for (int i=0;i<labels.length;i++) if (labels[i].equals(TAGS))
                    { to_draw.add(comment.getTags()); to_color.add(RTColorManager.getColor("label","major")); }
		  // Selected Entities
                  for (int i=0;i<labels.length;i++) if (labels[i].equals(SELECTED_ENTITIES)) { 
                     List<SubText> entities = comment.listEntitiesAndRelationships(); int found = 0;
		     for (int j=0;j<entities.size();j++) {
		       String str = entities.get(j).toString();
                       if (getRTParent().getSelectedEntities().contains(str) || getRTParent().getSelectedEntities().contains(str.toLowerCase())) found++;
		     }
		     if      (found == 1) { to_draw.add("" + found + " Match");   to_color.add(RTColorManager.getColor("label","errorfg")); }
		     else if (found >  1) { to_draw.add("" + found + " Matches"); to_color.add(RTColorManager.getColor("label","errorfg")); }
		  }
		  // Actually draw the text
                  sy += 6+txt_h;
		  for (int i=0;i<to_draw.size();i++) {
		    g2d.setColor(to_color.get(i));
		    g2d.drawString(to_draw.get(i), sx - Utils.txtW(g2d,to_draw.get(i))/2, sy);
		    sy += txt_h;
		  }
		}
	      }
	    }
	  } finally { g2d.dispose(); }
	}
        return base_bi;
      }
    }
  }
}

