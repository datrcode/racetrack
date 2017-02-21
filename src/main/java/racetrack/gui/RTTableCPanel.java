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

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesDT;
import racetrack.framework.BundlesG;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;

import racetrack.util.Utils;

import racetrack.visualization.RTColorManager;
import racetrack.visualization.StatsOverlay;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Class that implements a table (excel-like) view of the visible data.  This
 * version implements a custom table view that provides additional context
 * to the information/data.
 *
 *@author  D. Trimm
 *@version 0.1
 */
public class RTTableCPanel extends RTPanel {
  /**
   *
   */
  private static final long serialVersionUID = -5363124321439222969L;

  /**
   * Draw the header info
   */
  JCheckBoxMenuItem header_cbmi,

  /**
   * Limit the width of all of the columns to a specific amount - helps with columns that may fill up the screen
   */
                    limit_width_to_x_cbmi,

  /**
   * Highlight tagged entities within the cells
   */
		    highlight_tagged_cbmi;

  /**
   * No color option
   */
  JRadioButtonMenuItem color_none_rbmi,

  /**
   * Color each row by the global "Color By" option
   */
                       color_rows_rbmi,

  /**
   * Only color the cell that is selected in the "Color By" option
   */
                       color_cell_rbmi,

  /**
   * Color every cell by it's natural (value) color...  probably not that useful
   */
                       color_all_cells_rbmi,

  /**
   * No interaction
   */
                       interact_none_rbmi,

  /**
   * Interact by showing the local graph for identifiers -- will only work for specific fields...
   * - from  == to
   * - sip   == dip
   * - srcip == dstip
   */
                       interact_graph_rbmi;

  /**
   * Construct the correlation panel with the specified parent.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt application parent
   */
  public RTTableCPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt) { 
    super(win_type,win_pos,win_uniq,rt);   

    // Configure the components within the panel
    add("Center", component = new TableCComponent());
    add("South",  tablet_cb = new JComboBox());

    // Popup menu info
    // - Copy operation
    JMenuItem mi;
    getRTPopupMenu().add(mi = new JMenuItem("Copy Contents")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { ((TableCComponent) getRTComponent()).copyToClipboard(false,false); } } );

    getRTPopupMenu().addSeparator();

    // - General rendering options
    getRTPopupMenu().add(header_cbmi            = new JCheckBoxMenuItem("Header", true));
    getRTPopupMenu().add(highlight_tagged_cbmi  = new JCheckBoxMenuItem("Highlight Tagged"));
    getRTPopupMenu().add(limit_width_to_x_cbmi  = new JCheckBoxMenuItem("Limit Column Width"));

    getRTPopupMenu().addSeparator();

    // - Color rendering options
    ButtonGroup bg = new ButtonGroup();
    getRTPopupMenu().add(color_none_rbmi      = new JRadioButtonMenuItem("No Color", true));  bg.add(color_none_rbmi);
    getRTPopupMenu().add(color_rows_rbmi      = new JRadioButtonMenuItem("Color By Row"));    bg.add(color_rows_rbmi);
    getRTPopupMenu().add(color_cell_rbmi      = new JRadioButtonMenuItem("Color Cell"));      bg.add(color_cell_rbmi);
    getRTPopupMenu().add(color_all_cells_rbmi = new JRadioButtonMenuItem("Color All Cells")); bg.add(color_all_cells_rbmi);

    getRTPopupMenu().addSeparator();

    // - Interactivity choices
    bg = new ButtonGroup();
    getRTPopupMenu().add(interact_none_rbmi   = new JRadioButtonMenuItem("No Interact", true)); bg.add(interact_none_rbmi);
    getRTPopupMenu().add(interact_graph_rbmi  = new JRadioButtonMenuItem("Local Graph"));       bg.add(interact_graph_rbmi);

    getRTPopupMenu().addSeparator();

    // - Other actions/options
    getRTPopupMenu().add(mi = new JMenuItem("Clear Sorts")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { clearSorts(); } } );

    // - Column manipulation
    getRTPopupMenu().add(mi = new JMenuItem("Remove Column"));        mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { ((TableCComponent) getRTComponent()).removeColumn();   } } );
    getRTPopupMenu().add(mi = new JMenuItem("Add Column Before...")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { ((TableCComponent) getRTComponent()).addColumn(true);  } } );
    getRTPopupMenu().add(mi = new JMenuItem("Add Column After..."));  mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { ((TableCComponent) getRTComponent()).addColumn(false); } } );

    // Fill in the combobox
    updateBys();

    // Add the listeners
    // - Popup menu listeners
    defaultListener(header_cbmi);
    defaultListener(highlight_tagged_cbmi);
    defaultListener(limit_width_to_x_cbmi);

    defaultListener(color_none_rbmi);
    defaultListener(color_rows_rbmi);
    defaultListener(color_cell_rbmi);
    defaultListener(color_all_cells_rbmi);

    // - Tablet selection listener
    tablet_cb.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ie) {
        String tablet_str = (String) tablet_cb.getSelectedItem();
        if (tablet_str != null) ((TableCComponent) getRTComponent()).configureForTablet(tablet_str);
      }
    } );
  }

  /**
   * Return the flag indicating that the header should be rendered.
   *
   *@return render header flag
   */
  public boolean renderHeader()                 { return header_cbmi.isSelected(); }

  /**
   * Set the flag indicating that the header should be rendered.
   *
   *@param b render header flag
   */
  public void    renderHeader(boolean b)       { header_cbmi.setSelected(b);      }
  
  /**
   * Return the flag indicating that the column widths should be limited.
   *
   *@return limit column width flag
   */
  public boolean limitWidthToX()                 { return limit_width_to_x_cbmi.isSelected(); }

  /**
   * Set the flag indicating that the column widths should be limited.
   *
   *@param b limit column width flag
   */
  public void    limitWidthToX(boolean b)       { limit_width_to_x_cbmi.setSelected(b);      }

  /**
   * Return the flag indicating that if tagged entities should be highlighted
   *
   *@return highlight tagged entities flag
   */
  public boolean highlightTagged()          { return highlight_tagged_cbmi.isSelected(); }

  /**
   * Set the flag indicating that entities with tags will be highlighted
   *
   *@param b highlight tagged entities
   */
  public void    highlightTagged(boolean b) { highlight_tagged_cbmi.setSelected(b);      }

  /**
   * Enumeration describing the various color options
   */
  enum ColorOption { NONE, ROWS, CELL, ALL_CELLS };

  /**
   * Return the user-selected color option.
   *
   *@return color option
   */
  public ColorOption colorOption() { if      (color_none_rbmi.     isSelected()) return ColorOption.NONE;
                                     else if (color_rows_rbmi.     isSelected()) return ColorOption.ROWS;
				     else if (color_cell_rbmi.     isSelected()) return ColorOption.CELL;
				     else if (color_all_cells_rbmi.isSelected()) return ColorOption.ALL_CELLS;
				     else                                        return ColorOption.NONE;       }

  /**
   * Configure the color option based on the enumeration typed value.
   *
   *@param co Color option
   */
  public void        colorOption(ColorOption co) { 
    switch (co) {
      case NONE:      color_none_rbmi.     setSelected(true); break;
      case ROWS:      color_rows_rbmi.     setSelected(true); break;
      case CELL:      color_cell_rbmi.     setSelected(true); break;
      case ALL_CELLS: color_all_cells_rbmi.setSelected(true); break;
      default:        color_none_rbmi.     setSelected(true); break;
  } }

  /**
   * Configure the color option based on a string input.
   *
   *@param str String representing color option
   */
  public void        colorOption(String str) {
    if      (str.equals("" + ColorOption.NONE))      colorOption(ColorOption.NONE);
    else if (str.equals("" + ColorOption.ROWS))      colorOption(ColorOption.ROWS);
    else if (str.equals("" + ColorOption.CELL))      colorOption(ColorOption.CELL);
    else if (str.equals("" + ColorOption.ALL_CELLS)) colorOption(ColorOption.ALL_CELLS);
    else System.err.println("RTTableCPanel.colorOption(\"" + str + "\") - Value Not Parseable");
  }

  /**
   * Interactivity enumerations
   */
  enum Interactivity { NONE, GRAPH };

  /**
   * Return the interactivity setting.
   *
   *@return interactivity option
   */
  public Interactivity interactivity() { 
    if      (interact_none_rbmi.isSelected())  return Interactivity.NONE;
    else if (interact_graph_rbmi.isSelected()) return Interactivity.GRAPH;
    else                                       return Interactivity.NONE;
  }

  /**
   * Set the interactivity option.
   *
   *@param inter interactivity enumeration option
   */
  public void interactivity(Interactivity inter) {
    switch (inter) {
      case NONE:   interact_none_rbmi.setSelected(true);  break;
      case GRAPH:  interact_graph_rbmi.setSelected(true); break;
      default:     interact_none_rbmi.setSelected(true);  break;
    }
  }

  /**
   * Set the interactivity option (as a string).
   *
   *@param str interactity option as a string
   */
  public void interactivity(String str) {
    if      (str.equals("" + Interactivity.NONE))  interactivity(Interactivity.NONE); 
    else if (str.equals("" + Interactivity.GRAPH)) interactivity(Interactivity.GRAPH);
    else                                           interactivity(Interactivity.NONE);
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String getPrefix() { return "tablec"; }

  /**
   * Get the configuration for this panel.  Planned to be used for bookmarking.
   *
   *@return string representation of this configuration
   */
  public String getConfig() { return "RTTableCPanel"                                                              + BundlesDT.DELIM +
                                     "tablet="           + (Utils.encToURL((String) tablet_cb.getSelectedItem())) + BundlesDT.DELIM +
                                     "header="           + (renderHeader()       ? "true" : "false")              + BundlesDT.DELIM +
				     "highlighttag="     + (highlightTagged()    ? "true" : "false")              + BundlesDT.DELIM +
				     "limitwidth="       + (limitWidthToX()      ? "true" : "false")              + BundlesDT.DELIM +
				     "coloroption="      + (colorOption())                                        + BundlesDT.DELIM +
				     "interact="         + (interactivity())                                      + BundlesDT.DELIM +
                                     "sort1="            + (sort_1 == null ? "null" : Utils.encToURL(sort_1))     + BundlesDT.DELIM +
                                     "sort1inv="         + sort_1_inv                                             + BundlesDT.DELIM +
                                     "sort2="            + (sort_2 == null ? "null" : Utils.encToURL(sort_2))     + BundlesDT.DELIM +
                                     "sort2inv="         + sort_2_inv                                             + BundlesDT.DELIM +
                                     "sort3="            + (sort_3 == null ? "null" : Utils.encToURL(sort_3))     + BundlesDT.DELIM +
                                     "sort3inv="         + sort_3_inv                                             + BundlesDT.DELIM +
				     "visflds="          + Utils.encToURL(((TableCComponent) getRTComponent()).getVisibleFields()); }

  /**
   * Set the configuration for this panel.  Could be used to recall bookmarks.
   *
   *@param str string representation for new configuration
   */
  public void   setConfig    (String str) {
    StringTokenizer st = new StringTokenizer(str, BundlesDT.DELIM);
    if (st.nextToken().equals("RTTableCPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not a RTTableCPanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2  = new StringTokenizer(st.nextToken(), "=");
      String          type = st2.nextToken(), value = st2.hasMoreTokens() ? st2.nextToken() : "";

      if      (type.equals("tablet"))       tablet_cb.setSelectedItem(Utils.decFmURL(value)); 
      else if (type.equals("header"))       renderHeader(value.toLowerCase().equals("true"));
      else if (type.equals("highlighttag")) highlightTagged(value.toLowerCase().equals("true"));
      else if (type.equals("limitwidth"))   limitWidthToX(value.toLowerCase().equals("true"));
      else if (type.equals("coloroption"))  colorOption(value);
      else if (type.equals("interact"))     interactivity(value);
      else if (type.equals("sort1"))        { if (value.equals("null")) sort_1 = null; else sort_1 = Utils.decFmURL(value); }
      else if (type.equals("sort2"))        { if (value.equals("null")) sort_2 = null; else sort_2 = Utils.decFmURL(value); }
      else if (type.equals("sort3"))        { if (value.equals("null")) sort_3 = null; else sort_3 = Utils.decFmURL(value); }
      else if (type.equals("sort1inv"))     sort_1_inv = value.toLowerCase().equals("true");
      else if (type.equals("sort2inv"))     sort_2_inv = value.toLowerCase().equals("true");
      else if (type.equals("sort3inv"))     sort_3_inv = value.toLowerCase().equals("true");
      else if (type.equals("visflds"))      ((TableCComponent) getRTComponent()).setVisibleFields(Utils.decFmURL(value));
      else System.err.println("RTTableCPane.setConfig() - Do Not Understand Type Value \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * Map that converts a stringified-bundles rep into the bundle that should be at the top of the table
   */
  private Map<String,Bundle> top_bundle = new HashMap<String,Bundle>();

  /**
   * Assign the top bundle for the specified set of bundles.
   *
   *@param bundles  current data for render
   *@param bundle   top bundle
   */
  public void assignTopBundle(Bundles bundles, Bundle bundle) {
    if (top_bundle.size() > 4096) top_bundle.clear(); // Put in a safeguard to prevent the map from becoming too large
    top_bundle.put("" + bundles.hashCode(), bundle);
  }

  /**
   * Retrieve the top bundle for the specified set of bundles.
   *
   *@param bundles data for render
   *
   *@return bundle that should be on top
   */
  public Bundle retrieveTopBundle(Bundles bundles) {
    String key = "" + bundles.hashCode();
    if (top_bundle.containsKey(key)) return top_bundle.get(key);
    else                             return null;
  }

  /**
   * ComboBox for choosing tablet
   */
  JComboBox tablet_cb;
  
  /**
   * Update occured to the application data structures -- make sure the dropdowns reflect the options.
   */
  @Override
  public void updateBys() {
    // Get all of the file headers for the tablets
    Set<String> set = new HashSet<String>();
    Iterator<Tablet> it_tablet = getRTParent().getRootBundles().tabletIterator();
    while (it_tablet.hasNext()) {
      Tablet tablet = it_tablet.next();
      set.add(tablet.fileHeader());
    }
    List<String> list = new ArrayList<String>(); list.addAll(set); Collections.sort(list);
    // for (int i=0;i<list.size();i++) System.out.println(list.get(i)); // DEBUG

    // Reinstantiate the combobox with the list... make sure to keep the user selection (if there is one)
    Object sel = tablet_cb.getSelectedItem();
    tablet_cb.removeAllItems();
    for (int i=0;i<list.size();i++) tablet_cb.addItem(list.get(i));
    if (sel != null) tablet_cb.setSelectedItem(sel);

    // Keep the current selection
    if (sel != null) {
      String configged = ((TableCComponent) getRTComponent()).tabletConfiguration();
      if      (configged                      == null)  ((TableCComponent) getRTComponent()).configureForTablet((String) sel);
      else if (configged.equals((String) sel) == false) ((TableCComponent) getRTComponent()).configureForTablet((String) sel);

    // If no current selection, use the first item as the configuration
    } else if (list.size() > 0) {
      tablet_cb.setSelectedItem(list.get(0));
      ((TableCComponent) getRTComponent()).configureForTablet(list.get(0));
    }
  }

  /**
   * Primary sort
   */
  private String sort_1,

  /**
   * Secondary sort
   */
                 sort_2,

  /**
   * Tertiary sort
   */
		 sort_3;

  /**
   * Primary sort invert flag
   */
  private boolean sort_1_inv = false,

  /**
   * Secondary sort invert flag
   */
                  sort_2_inv = false,

  /**
   * Tertiary sort invert flag
   */
		  sort_3_inv = false;

  /**
   * Clear the sort order
   */
  public void clearSorts() {
    sort_1     = sort_2     = sort_3     = null;
    sort_1_inv = sort_2_inv = sort_3_inv = false;
    getRTComponent().render();
  }

  /**
   * Custom table component
   */
  public class TableCComponent extends RTComponent {
    /**
     * Default header/field names
     */
    /**
     * Timestamp field
     */
    public static final String TS0_FLD = "Timestamp",

    /**
     * End timestamp field
     */
                               TS1_FLD = "Timestamp E";

    /**
     * String representing this tablet
     */
    String tablet_str;

    /**
     * Fields within the tablet
     */
    String flds[];

    /**
     * Tablet has timestamps
     */
    boolean has_timestamps = false,

    /**
     * Tablet has durations
     */
            has_durations  = false;

    /**
     * Max width indexed by the column header
     */
    Map<String, Integer>     max_width_lu = new HashMap<String, Integer>();

    /**
     * Return the maximum width needed for a field.
     *
     *@param field field to find the maximum width for
     *
     *@return maximum width in pixels
     */
    public int maxWidth(String field) {
      // If we haven't done the calculation for this specific field, do it now
      if (max_width_lu.containsKey(field) == false) {
        BufferedImage bi = new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB); Graphics2D g2d = (Graphics2D) bi.getGraphics(); int max_w = Utils.txtW(g2d, field + " ");

	if (field.equals(TS0_FLD) || field.equals(TS1_FLD)) {
	  //
	  // Timestamp fields
	  //
          max_width_lu.put(TS0_FLD, Utils.txtW(g2d, Utils.shortDateSecsStr(0L) + "  "));
          max_width_lu.put(TS1_FLD, Utils.txtW(g2d, Utils.shortDateSecsStr(0L) + "  "));

	} else {
	  //
	  // Non-timestamp fields
	  //
          Iterator<Tablet> it_tab = getRTParent().getRootBundles().tabletIterator(); while (it_tab.hasNext()) {
	    Tablet tablet = it_tab.next(); if (KeyMaker.tabletCompletesBlank(tablet, field)) {
	      KeyMaker km = new KeyMaker(tablet, field);
	      Iterator<Bundle> it_bun = tablet.bundleIterator(); while (it_bun.hasNext()) {
                Bundle bundle = it_bun.next(); String keys[] = km.stringKeys(bundle);
	        if        (keys == null || keys.length == 0) {
	        } else if (                keys.length == 1) { int w = Utils.txtW(g2d, keys[0] + "  "); if (w > max_w) max_w = w;
	        } else                                       {
	          StringBuffer sb = new StringBuffer();
		  sb.append(keys[0]); for (int i=1;i<keys.length;i++) sb.append(BundlesDT.DELIM + keys[i]);
		  int w = Utils.txtW(g2d, sb.toString() + "  ");
		  if (w > max_w) max_w = w;
	        }
	      }
	    }
	  }
	  max_width_lu.put(field, max_w);
	  g2d.dispose();
	}
      }

      // Return the lookup or a cap depending on user settings
      if (limitWidthToX() && max_width_lu.get(field) > 120) return 120;

      return max_width_lu.get(field);
    }

    /**
     * Visible fields
     */
    String                   vis_flds[];

    /**
     * Visible field x offset
     */
    int                      vis_flds_x[];

    /**
     *
     */
    boolean                  vis_flds_scalar[];

    /**
     * Construct the tablec component
     */
    public TableCComponent() { }    

    /**
     * Return the visible fields as an encoded string.
     *
     *@return visible fields
     */
    public String getVisibleFields() {
      StringBuffer sb = new StringBuffer();
      for (int i=0;i<vis_flds.length;i++) {
        if (i > 0) sb.append(BundlesDT.DELIM);
	sb.append(Utils.encToURL(vis_flds[i]));
      }
      return sb.toString();
    }

    /**
     * Set the visible fields from an encoded string.
     *
     *@param enc encoded string
     */
    public void setVisibleFields(String enc) {
      BundlesG globals = getRTParent().getRootBundles().getGlobals();

      StringTokenizer st = new StringTokenizer(enc, BundlesDT.DELIM);
      String  new_vis_flds[]        = new String [st.countTokens()];
      int     new_vis_flds_x[]      = new int    [st.countTokens()];
      boolean new_vis_flds_scalar[] = new boolean[st.countTokens()];

      for (int i=0;i<new_vis_flds.length;i++) {
        new_vis_flds[i] = Utils.decFmURL(st.nextToken());
	if (i == 0) new_vis_flds_x[i] = 0;
	else        new_vis_flds_x[i] = new_vis_flds_x[i-1] + maxWidth(new_vis_flds[i-1]);
	new_vis_flds_scalar[i] = globals.isScalar(globals.fieldIndex(new_vis_flds[i]));
      }

      // Assign them over... should be synchronized because it could tear...
      vis_flds_x      = new_vis_flds_x;
      vis_flds_scalar = new_vis_flds_scalar;
      vis_flds        = new_vis_flds;
      getRTComponent().render();
    }

    /**
     * Remove the column where the mouse was clicked.  The column named should be stored in the rendercontext mouse click variable.
     */
    public void removeColumn() {
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc != null) {
        int to_remove_i = mouse_click_in_column_i; if (to_remove_i != -1) {
	  String  new_vis_flds[]        = new String [vis_flds.length - 1];
	  int     new_vis_flds_x[]      = new int    [vis_flds_x.length - 1];
	  boolean new_vis_flds_scalar[] = new boolean[vis_flds_scalar.length - 1];

          int new_i = 0;
          for (int i=0;i<vis_flds.length;i++) {
            if (i == to_remove_i) continue; // column to remove.. just go to next index
            // Copy field
            new_vis_flds[new_i] = vis_flds[i];
	    // Figure out x coordinate
	    if (new_i == 0) new_vis_flds_x[new_i] = 0;
	    else            new_vis_flds_x[new_i] = new_vis_flds_x[new_i-1] + maxWidth(new_vis_flds[new_i-1]);
            // Copy scalar
	    new_vis_flds_scalar[new_i] = vis_flds_scalar[i];
            // Increment the new index
	    new_i++;
          }

	  // Assign them over... should be synchronized because it could tear...
	  vis_flds_x      = new_vis_flds_x;
	  vis_flds_scalar = new_vis_flds_scalar;
	  vis_flds        = new_vis_flds;

          getRTComponent().render();
	}
      }
    }

    /**
     * Add a column to the table after asking the user for their selection via dialog.
     *
     *@param before_index add the new column to the left of the mouse clicked column
     */
    public void addColumn(boolean before_index) {
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc != null) {

        int to_add_before_i;
        if (before_index) to_add_before_i = mouse_click_in_column_i; 
        else              to_add_before_i = mouse_click_in_column_i + 1;

        if (to_add_before_i != -1) {
          Object new_fld_sel = JOptionPane.showInputDialog(null, "Choose one", "Input", JOptionPane.INFORMATION_MESSAGE, null, flds, flds[0]);
	  if (new_fld_sel != null) {
            BundlesG globals = getRTParent().getRootBundles().getGlobals();
	    String  new_vis_flds[]        = new String [vis_flds.length + 1];
	    int     new_vis_flds_x[]      = new int    [vis_flds_x.length + 1];
	    boolean new_vis_flds_scalar[] = new boolean[vis_flds_scalar.length + 1];

	    // Copy over old
	    for (int i=0;i<to_add_before_i;i++) {
	      new_vis_flds       [i] = vis_flds       [i];
	      if (i == 0) new_vis_flds_x[i] = 0;
              else        new_vis_flds_x[i] = new_vis_flds_x[i-1] + maxWidth(new_vis_flds[i-1]);
	      new_vis_flds_scalar[i] = vis_flds_scalar[i];
	    }

	    // Insert new
	    new_vis_flds       [to_add_before_i] = (String) new_fld_sel;
	    if (to_add_before_i == 0) new_vis_flds_x     [to_add_before_i] = 0;
	    else                      new_vis_flds_x     [to_add_before_i] = vis_flds_x[to_add_before_i - 1] + maxWidth(vis_flds[to_add_before_i-1]);
	    new_vis_flds_scalar[to_add_before_i] = globals.isScalar(globals.fieldIndex(new_vis_flds[to_add_before_i]));

	    // Copy rest
	    for (int i=to_add_before_i;i<vis_flds.length;i++) {
	      new_vis_flds       [i+1] = vis_flds       [i];
	      new_vis_flds_x     [i+1] = new_vis_flds_x [i] + maxWidth(new_vis_flds[i]);
	      new_vis_flds_scalar[i+1] = vis_flds_scalar[i];
	    }

	    // Assign them over... should be synchronized because it could tear...
	    vis_flds_x      = new_vis_flds_x;
	    vis_flds_scalar = new_vis_flds_scalar;
	    vis_flds        = new_vis_flds;

            getRTComponent().render();

	    // Save the new configuration as a preference
            // - Field ordering
	    StringBuffer sb = new StringBuffer(); for (int i=0;i<vis_flds.length;i++) { sb.append(Utils.encToURL(vis_flds[i])); sb.append(BundlesDT.DELIM); }
	    // - Additional options for the rendering
	    StringBuffer pref_sb = new StringBuffer();
	    pref_sb.append(Utils.encToURL(sb.toString())                      + BundlesDT.DELIM +
	                   "limitwidth=" + (limitWidthToX() ? "true":"false") + BundlesDT.DELIM +  
		           "header="     + (renderHeader()  ? "true":"false"));
	    RTPrefs.store("RTTableC_" + Utils.encToURL(tablet_str), pref_sb.toString());
	  }
	}
      }
    }

    /**
     * Copy all of the currently rendered rows (more than just "visible" in the component) to the clipboard.
     *
     *@param shft shift key pressed
     *@param alt  alt key pressed
     */
    @Override
    public void copyToClipboard(boolean shft, boolean alt) {
      Clipboard  clipboard = getToolkit().getSystemClipboard();
      StringBuffer sb = new StringBuffer();

      // Header first
      for (int j=0;j<vis_flds.length;j++) { if (j != 0) sb.append(','); sb.append(Utils.doubleQuotify(vis_flds[j])); }
      sb.append("\n");

      // Data rows next
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc != null) {
        // Create the keymakers
	Tablet tablet = myrc.bundle_list.get(0).getTablet();
        KeyMaker kms[] = new KeyMaker[vis_flds.length];
	for (int j=0;j<kms.length;j++) {
          if (vis_flds[j].equals(TS0_FLD) || vis_flds[j].equals(TS1_FLD)) {
          } else kms[j] = new KeyMaker(tablet, vis_flds[j]);
        }
        // Go through the records
	for (int i=0;i<myrc.bundle_list.size();i++) {
	  Bundle bundle = myrc.bundle_list.get(i);
          for (int j=0;j<vis_flds.length;j++) { 
	    if (j != 0) sb.append(','); 
	    
	    // Timestamps are special...
	    if        (vis_flds[j].equals(TS0_FLD)) { sb.append(Utils.shortDateSecsStr(bundle.ts0()));
	    } else if (vis_flds[j].equals(TS1_FLD)) { sb.append(Utils.shortDateSecsStr(bundle.ts1()));
	    } else                                  {
	      String strs[] = kms[j].stringKeys(bundle);
	      String str    = "";
	      for (int k=0;k<strs.length;k++) { if (k != 0) str += "|"; str += strs[k]; }
	      sb.append(Utils.doubleQuotify(str));
            }
	  }
          sb.append("\n");
	}
      }

      clipboard.setContents(new StringSelection(sb.toString()), null);
    }

    /**
     * Return the tables current configuration -- i.e., which tablet that it is configurred for...
     *
     *@return tablet the component is configgured for
     */
    public String tabletConfiguration() { return tablet_str; }

    /**
     * Flavor(s) of the tablet
     */
    Map<String,Map<String,String>> flavors;

    /**
     * Configure the table for a new tablet.
     *
     *@param tablet_str unique string identifying a tablet
     */
    public void configureForTablet(String tablet_str) {
      // Tablet string defines exactly what columns to show
      this.tablet_str = tablet_str; 

      // Find the tablet that matches this tablet string - slight issue if multiple tablets share the
      // exact same file header...
      // System.err.println("configureForTablet() - Looking for \"" + tablet_str + "\"");
      Tablet tablet =  null; Iterator<Tablet> it_tab = getRTParent().getRootBundles().tabletIterator();
      while (tablet == null && it_tab.hasNext()) {
        Tablet tab = it_tab.next();
	if (tab.fileHeader().equals(tablet_str)) { tablet = tab; /* System.err.println("  Match For Tablet \"" + tab + "\""); */ }
	else                                     {               /* System.err.println("  NonMatch \""         + tab + "\""); */ }
      }
      if (tablet == null) throw new RuntimeException("Unable To Find Tablet Matching String \"" + tablet_str + "\"");

      // Determine if there are timestamps
      has_timestamps = tablet.hasTimeStamps(); has_durations = tablet.hasDurations();

      // Deconstruct the columns 
      BundlesG globals = getRTParent().getRootBundles().getGlobals();
      List<String> fields_list = new ArrayList<String>(); 
      if (has_timestamps) fields_list.add(TS0_FLD);
      if (has_durations)  fields_list.add(TS1_FLD);

      // Use the blanks and tabletCompletesBlank to determine which apply to this tablet
      String blanks[] = KeyMaker.blanks(globals, false, true, true, true); 
      for (int i=0;i<blanks.length;i++) if (KeyMaker.tabletCompletesBlank(tablet,blanks[i])) fields_list.add(blanks[i]);
      flds = new String[fields_list.size()]; 
      for (int i=0;i<flds.length;i++) { flds[i] = fields_list.get(i); }

      // Determine the data flavor
      flavors = StatsOverlay.dataFlavors(tablet.bundleSet());

      // Put together a list of the normal fields -- i.e., fields that don't have a transform or other non-original data effect
      List<String> fields_normal = new ArrayList<String>();
      for (int i=0;i<flds.length;i++) { if (flds[i].indexOf(BundlesDT.DELIM) < 0) fields_normal.add(flds[i]); }

      // Make the visible columns
      int cols = fields_normal.size();
      vis_flds = new String[cols]; for (int i=0;i<cols;i++) vis_flds[i] = fields_normal.get(i);

      // If there's a preconfig, apply that here
      String preconfig = RTPrefs.retrieveString("RTTableC_" + Utils.encToURL(tablet_str)); if (preconfig != null) {
        StringTokenizer st = new StringTokenizer(preconfig, BundlesDT.DELIM);

	// Extract the preconfig fields
	String preconf_fields = Utils.decFmURL(st.nextToken());
	StringTokenizer st_fields = new StringTokenizer(preconf_fields, BundlesDT.DELIM);
	// - Reallocate the columns and vis_flds array
	cols = st_fields.countTokens();
	vis_flds = new String[cols]; for (int i=0;i<vis_flds.length;i++) vis_flds[i] = Utils.decFmURL(st_fields.nextToken());

	// Parse the additional options
	while (st.hasMoreTokens()) {
	  String str = st.nextToken(); 
	  StringTokenizer st_typeval = new StringTokenizer(str, "="); String type = st_typeval.nextToken(); String value = st_typeval.nextToken();
	  if        (type.equals("limitwidth")) { if (value.toLowerCase().equals("true")) limitWidthToX(true); else limitWidthToX(false);
	  } else if (type.equals("header"))     { if (value.toLowerCase().equals("true")) renderHeader(true);  else renderHeader(false);
	  } else System.err.println("RTTableCPanel.configureForTablet() - Do Not Understand Type/Val \"" + type + "\"/\"" + value + "\"");
	}
      }

      // Allocate x offsets for the columns
      vis_flds_x = new int[cols]; vis_flds_x[0] = 0;
      for (int i=1;i<vis_flds.length;i++) vis_flds_x[i] = vis_flds_x[i-1] + maxWidth(vis_flds[i-1]);

      // Determine if the fields are scalars (scalars need to be right justified)
      vis_flds_scalar = new boolean[cols];
      for (int i=0;i<vis_flds_scalar.length;i++) {
        if      (vis_flds[i].equals(TS0_FLD) || vis_flds[i].equals(TS1_FLD)) vis_flds_scalar[i] = false;
        else                                                                 vis_flds_scalar[i] = globals.isScalar(globals.fieldIndex(vis_flds[i]));
      }

      // Force a render
      getRTComponent().render();
    }

    /**
     * Local copy of last known mouse x position
     */
    private int my_mx,

    /**
     * Local copy of last known mouse y position
     */
                my_my;

    /**
     * Column that the mouse is in
     */
    private     Rectangle2D mouse_in_hdr_rect = null; 

    /**
     * Column that the mouse is in
     */
    private     String      mouse_in_hdr      = null;

    /**
     * Cell that the mouse is in
     */
    private     Rectangle2D mouse_in_ent_rect = null;

    /**
     * Entities within the cell that the mouse is in
     */
    private     Set<String> mouse_in_ents     = null;

    /**
     * Mouse in the scrollbar area
     */
    private     boolean     mouse_in_scroll_bar = false;

    /**
     * Mouse in the header labels area
     */
    private     boolean     mouse_in_hdr_labels = false;

    /**
     * Update the mouse position information to include which header and entity that the mouse is in.
     *
     *@param me   Mouse event information structure
     *@param myrc Current render context
     */
    private void updateMousePositionInfo(MouseEvent me, RenderContext myrc) {
      // Copy the coordinates
      my_mx = me.getX(); my_my = me.getY();

      // Find the column rect
      if (mouse_in_hdr_rect != null && mouse_in_hdr_rect.contains(my_mx, my_my)) {
      } else {
        mouse_in_hdr_rect = null; mouse_in_hdr = null;
        Iterator<Rectangle2D> it = myrc.geom_to_header.keySet().iterator(); while (it.hasNext() && mouse_in_hdr_rect == null) {
          Rectangle2D rect = it.next(); if (rect.contains(my_mx, my_my)) { mouse_in_hdr_rect = rect; mouse_in_hdr = myrc.geom_to_header.get(rect); }
        }
      }

      // Find the entity rect
      if (mouse_in_ent_rect != null && mouse_in_ent_rect.contains(my_mx, my_my)) {
      } else {
        mouse_in_ent_rect = null; mouse_in_ents = null;
        Iterator<Rectangle2D> it = myrc.geom_to_entities.keySet().iterator(); while (it.hasNext() && mouse_in_ent_rect == null) {
          Rectangle2D rect = it.next(); if (rect.contains(my_mx, my_my)) { mouse_in_ent_rect = rect; mouse_in_ents = myrc.geom_to_entities.get(rect); }
        } 
      }

      // Check the scrollbar area
      if (myrc != null && myrc.scroll_bar_all != null &&  myrc.scroll_bar_all.contains(my_mx, my_my)) mouse_in_scroll_bar = true; else mouse_in_scroll_bar = false;

      // Check the header labels area
      if (myrc != null && myrc.render_header && my_my < myrc.row_h && mouse_in_hdr != null) mouse_in_hdr_labels = true; else mouse_in_hdr_labels = false;
    }

    /**
     *
     */
    @Override
    public void mouseMoved(MouseEvent me) {
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc != null) {
        updateMousePositionInfo(me, myrc);
        super.mouseMoved(me);
	if (interactivity() != Interactivity.NONE) repaint();
      } else super.mouseMoved(me);
    }

    /**
     *
     */
    @Override
    public void mouseDragged(MouseEvent me) {
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc != null) {
	updateMousePositionInfo(me, myrc);
        //
	// Scrollbar stuff
	//
        if (scrolling) {
          int dy = my_my - scroll_start_y; int new_y = scroll_bar_y_at_start + dy;
          myrc.setTopRenderBundle( (int) ((myrc.bundle_list.size() *  (new_y - myrc.scroll_bar_all.getY()))/myrc.scroll_bar_all.getHeight()) );
	  repaint();

	//
	// super class
	//
        } else super.mouseDragged(me);
      } else super.mouseDragged(me);
    }

    /**
     * Flag to indicate that the scrollbar is active
     */
    private boolean scrolling = false;

    /**
     * Initial mouse placement for the scrollbar
     */
    private int scroll_start_y,

    /**
     * Placement of the scrollbar itself at the start of the scroll
     */
                scroll_bar_y_at_start;

    /**
     *
     */
    @Override
    public void mousePressed(MouseEvent me) {
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc != null) {
	updateMousePositionInfo(me, myrc);

        //
        // Record mouse press column information
	//
        mouse_click_in_column   = myrc.columnAt(me.getX());
	mouse_click_in_column_i = myrc.columnIndexAt(me.getX());

	//
	// Scrollbar stuff
	//
        if (mouse_in_scroll_bar && me.getButton() == MouseEvent.BUTTON1) {
          scrolling = true; scroll_start_y = my_my; 
	  if (myrc.scroll_bar.contains(my_mx, my_my)) { 
            scroll_bar_y_at_start = (int) myrc.scroll_bar.getY();
          } else {
            scroll_bar_y_at_start = my_my;
            myrc.setTopRenderBundle( (int) ((myrc.bundle_list.size() *  (scroll_start_y - myrc.scroll_bar_all.getY()))/myrc.scroll_bar_all.getHeight()) );
	    repaint();
	  }
        //
	// super class
	//
        } else super.mousePressed(me);
      } else super.mousePressed(me);
    }

    /**
     *
     */
    @Override
    public void mouseReleased(MouseEvent me) { 
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc != null) {
	updateMousePositionInfo(me, myrc);
	//
	// Scrollbar stuff
	//
        if (me.getButton() == MouseEvent.BUTTON1 && scrolling) {
          scrolling = false;

	//
	// super class
	//
        } else super.mouseReleased(me);
      } else super.mouseReleased(me);
    }

    /**
     * Mouse clicked in this column
     */
    public  String mouse_click_in_column   = null;

    /**
     * Mouse clicked in this column index
     */
    public  int    mouse_click_in_column_i = -1;

    /**
     *
     */
    @Override
    public void mouseClicked(MouseEvent me) {
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc != null) {
	updateMousePositionInfo(me, myrc);

        // Record the mouse field in case 
        mouse_click_in_column = myrc.columnAt(me.getX());

	//
	// If the user clicks on a header, update the sort and re-render
	//
	if        (me.getButton() == MouseEvent.BUTTON1 && mouse_in_hdr_labels) {
	  if (mouse_in_hdr == sort_1) {
	    sort_1_inv = ! sort_1_inv;
	  } else                      {
	    sort_3     = sort_2;     sort_2     = sort_1;      sort_1     = mouse_in_hdr;
	    sort_3_inv = sort_2_inv; sort_2_inv = sort_1_inv;  sort_1_inv = false;
	  } 
	  getRTComponent().render();

	//
	// Update the placement of the scrollbar
	//
	} else if (mouse_in_scroll_bar) {
	  scroll_start_y = my_my;
          myrc.setTopRenderBundle( (int) ((myrc.bundle_list.size() *  (scroll_start_y - myrc.scroll_bar_all.getY()))/myrc.scroll_bar_all.getHeight()) );
          repaint();

        //
	// Else allow the super class to handle the evetn
	//
	} else super.mouseClicked(me);
      } else super.mouseClicked(me);
    }

    /**
     *
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent mwe) {
      RenderContext myrc = (RenderContext) getRTComponent().rc;
      myrc.adjustTopRenderBundle(mwe.getWheelRotation());
      repaint();
    }

    /**
     * Create the render context
     *
     *@param id identifier to abort unnecessary computations
     */
    @Override
    public RTRenderContext render(short id) {
      clearNoMappingSet(); Bundles bs = getRenderBundles();
      String count_by = getRTParent().getCountBy(), color_by = getRTParent().getColorBy();
      if (bs != null && tablet_str != null) { 
        RenderContext myrc = new RenderContext(id, bs, count_by, color_by, 
	                                       renderHeader(), highlightTagged(),
					       colorOption(),
	                                       getWidth(), getHeight()); 
        return myrc; 
      } else return null;
    }

    /**
     * Add specific interactive renderings for this component.  Call super method in all cases to handle the rest.
     *
     *@param g graphics context
     */
    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g); Graphics2D g2d = (Graphics2D) g;

      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc != null) {
        Stroke orig_stroke = g2d.getStroke();

        //
	// If there are selected entities, highlight those interactively
	//
	g2d.setColor(RTColorManager.getColor("linknode","movenodes")); g2d.setStroke(new BasicStroke(2.0f));
        Set<String> sels_copy = new HashSet<String>(); sels_copy.addAll(getRTParent().getSelectedEntities());
	sels_copy.retainAll(myrc.entity_to_geom.keySet());
	Iterator<String> it_ents = sels_copy.iterator(); while (it_ents.hasNext()) {
	  String entity = it_ents.next(); Set<Rectangle2D> rects = myrc.entity_to_geom.get(entity);
	  Iterator<Rectangle2D> it_rects = rects.iterator(); while (it_rects.hasNext()) {
	    g2d.draw(Utils.enlargeBounds(it_rects.next(), 1));
          }
	}
	g2d.setStroke(orig_stroke);

	//
	// Draw stats
	//

	//
	// Draw graph... if all the necessary criteria are met...
	// 
	Set<String> mouse_in_ents_copy = mouse_in_ents; // Use copies to avoid synchronization issues
	String      mouse_in_hdr_copy  = mouse_in_hdr;
        if (interactivity()           == Interactivity.GRAPH && 
	    mouse_in_ents_copy        != null                && 
	    mouse_in_hdr_copy         != null                &&
	    mouse_in_ents_copy.size() >  0                   &&
	      (flavors.keySet().contains(StatsOverlay.NETFLOW_MINIMAL) ||
	       flavors.keySet().contains(StatsOverlay.NETFLOW_DEFAULT) ||
	       flavors.keySet().contains(StatsOverlay.NETFLOW_VOLUME)  ||
	       flavors.keySet().contains(StatsOverlay.NETFLOW_FULL))) {
          String flavor = flavors.keySet().iterator().next();
          //
	  // Figure out if we are in the right column...
	  //
          if (mouse_in_hdr_copy.equals(flavors.get(flavor).get(StatsOverlay.sip)) ||
	      mouse_in_hdr_copy.equals(flavors.get(flavor).get(StatsOverlay.dip))) {
            StatsOverlay stats_overlay = new StatsOverlay(getRTParent().getVisibleBundles().bundleSet(), getRTParent().getVisibleBundles(), getRTParent().getRootBundles(), mouse_in_ents_copy);
	    BufferedImage overlay = stats_overlay.overlay(myrc.getRCWidth(), myrc.getRCHeight());
	    g2d.drawImage(overlay, 0, 0, null);
          }
        }
      }
    }

    /**
     * For a specific shape, find all of the corresponding shapes that overlap with it.
     * - Not adding the scrollbar shapes here...  thinking this is only for filter ops - can't be applied to scrollbar context shapes
     *
     *@param to_check shape to evaluate for overlap
     *
     *@return set of all shapes that overlap
     */
    @Override
    public Set<Shape> overlappingShapes(Shape to_check) { 
      Set<Shape> set = new HashSet<Shape>(); 
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc != null) {
        Iterator<Shape> it = myrc.geom_to_bundle.keySet().iterator();
	while (it.hasNext()) {
	  Shape shape = it.next();
	  if (Utils.genericIntersects(shape, to_check)) set.add(shape);
	}
      }
      return set;
    }

    /**
     * For a specific shape, find all of the corresponding bundles that overlap with the shape.
     * - By default, will exclude the scrollbar shapes...  thinking this is only for filter ops - can't be applied to scrollbar context shapes
     *
     *@param shape shape to evaluate for overlap
     *
     *@return set of all bundles that overlap
     */
    @Override
    public Set<Bundle> shapeBundles(Shape shape) { 
      Set<Bundle> bundles = new HashSet<Bundle>(); 
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc != null) {
        Set<Shape>  shapes  = overlappingShapes(shape);
        Iterator<Shape> it = shapes.iterator(); while (it.hasNext()) {
	  bundles.add(myrc.geom_to_bundle.get(it.next()));
	}
      }
      return bundles;
    }

    /**
     * Return the shapes that match the specific bundles passed in.
     *
     *@param bundles bundles to lookup
     *
     *@return shapes that match specified bundles
     */
    @Override
    public Set<Shape> shapes(Set<Bundle> bundles) { 
      Set<Shape> shapes = new HashSet<Shape>();
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc != null) {
        Iterator<Bundle> it = bundles.iterator(); while (it.hasNext()) {
	  Bundle bundle = it.next();
	  if (myrc.bundle_to_geom.containsKey(bundle))    shapes.add(myrc.bundle_to_geom.get(bundle));
	  if (myrc.sb_bundle_to_geom.containsKey(bundle)) shapes.add(myrc.sb_bundle_to_geom.get(bundle));
	}
      }
      return shapes;
    }

    /**
     * Return all shapes within the current rendering.
     *
     *@return set of all shapes
     */
    @Override
    public Set<Shape> allShapes() { 
      RenderContext myrc = (RenderContext) getRTComponent().rc; 
      if (myrc != null) {
        Set<Shape> set = new HashSet<Shape>();
        set.addAll(myrc.geom_to_bundle.keySet());
	set.addAll(myrc.sb_geom_to_bundles.keySet());
        return set;
      } else return new HashSet<Shape>(); 
    }

    /**
     * Context/state for rendering the view
     */
    class RenderContext extends RTRenderContext {
      /**
       * Bundles to render
       */
      Bundles bs;

      /**
       * Count by
       */
      String  count_by,

      /**
       * Color by
       */
              color_by;

      /**
       * Render the header
       */
      boolean render_header,

      /**
       * Highlight tagged entities
       */
              highlight_tagged;

      /**
       * Color option
       */
      ColorOption color_option;

      /**
       * Width of rendering
       */
      int     rc_w,

      /**
       * Height of rendering
       */
              rc_h;

      /**
       * Bundles as a list
       */
      List<Bundle> bundle_list = new ArrayList<Bundle>();

      /**
       * Construct the render context
       */
      public RenderContext(short id, Bundles bs, String count_by, String color_by, 
                           boolean render_header, boolean highlight_tagged,
			   ColorOption color_option,
                           int rc_w, int rc_h) {
        render_id = id; this.bs = bs; this.count_by = count_by; this.color_by = color_by; 
	this.render_header = render_header; this.highlight_tagged = highlight_tagged;
	this.color_option  = color_option;
	this.rc_w = rc_w; this.rc_h = rc_h;

	// Find the top bundle if it has been stored off previously
	top_render_bundle = retrieveTopBundle(bs);

	// Convert to list
	Tablet tablet = null;
	Iterator<Tablet> it_tab = bs.tabletIterator(); while (it_tab.hasNext()) {
          Tablet tab = it_tab.next(); if (tab.fileHeader().equals(tablet_str)) {
	    tablet = tab;
	    bundle_list.addAll(tab.bundleSet());
	  }
	}

	// Sort
        if (sort_1 != null) {
          Comparator<Bundle> comparator = new MyComparator(tablet, sort_1, sort_1_inv, sort_2, sort_2_inv, sort_3, sort_3_inv);
          Collections.sort(bundle_list, comparator);
        }

        // Scan for which bundle should be on top
        if (top_render_bundle != null) {
          for (int i=0;i<bundle_list.size();i++) {
	    if (bundle_list.get(i) == top_render_bundle) { top_render_bundle_i = i; break; }
	  }
        }
      }

      /**
       * Comparator for the bundles
       */
      class MyComparator implements Comparator<Bundle> {
        KeyMaker km_sort[]; boolean inverts[]; String s[]; BundlesG globals = getRTParent().getRootBundles().getGlobals();
        public MyComparator(Tablet tablet, String s1, boolean s1_inv, String s2, boolean s2_inv, String s3, boolean s3_inv) {
	  if      (s2 == null) { km_sort = new KeyMaker[1]; inverts = new boolean[1]; s = new String[1]; }
	  else if (s3 == null) { km_sort = new KeyMaker[2]; inverts = new boolean[2]; s = new String[2]; }
	  else                 { km_sort = new KeyMaker[3]; inverts = new boolean[3]; s = new String[3]; }

                          { if (s1.equals(TS0_FLD) || s1.equals(TS1_FLD)) { } else { km_sort[0] = new KeyMaker(tablet, s1); } inverts[0] = s1_inv; s[0] = s1; }
	  if (s2 != null) { if (s2.equals(TS0_FLD) || s2.equals(TS1_FLD)) { } else { km_sort[1] = new KeyMaker(tablet, s2); } inverts[1] = s2_inv; s[1] = s2; }
	  if (s3 != null) { if (s3.equals(TS0_FLD) || s3.equals(TS1_FLD)) { } else { km_sort[2] = new KeyMaker(tablet, s3); } inverts[2] = s3_inv; s[2] = s3; }
        }
        public int compare(Bundle b1, Bundle b2) {
	  for (int i=0;i<km_sort.length;i++) {
	    //
	    // Check to see if it's a timestamp field
	    //
            if (km_sort[i] == null) {
              if        (s[i].equals(TS0_FLD)) {
                long ts1 = b1.ts0(), ts2 = b2.ts0();
                if        (ts1 > ts2) { if (inverts[i]) return -1; else return  1;
                } else if (ts1 < ts2) { if (inverts[i]) return  1; else return -1; }
              } else if (s[i].equals(TS1_FLD)) {
                long ts1 = b1.ts1(), ts2 = b2.ts1();
                if        (ts1 > ts2) { if (inverts[i]) return -1; else return  1;
                } else if (ts1 < ts2) { if (inverts[i]) return  1; else return -1; }
              } else throw new RuntimeException("Do Not Understand Field \"" + s[i] + "\"");
	    //
	    // Otherwise it's a regular field -- use the keymakers
	    //
            } else {
	      String strs1[] = km_sort[i].stringKeys(b1), strs2[] = km_sort[i].stringKeys(b2);
  
	      if        (strs1 == null   &&  strs2 == null)    { 
	      } else if (strs1 == null                    )    { if (inverts[i]) return -1; else return  1;
	      } else if (                    strs2 == null)    { if (inverts[i]) return  1; else return -1;
	      } else if (strs1.length > 0 && strs2.length > 0) {
	        //
	        // Do the actual compare
	        //
	        //
	        // - Compare ints
	        //
	        if     (globals.isScalar(globals.fieldIndex(s[i]))) {
                  int int1 = Integer.parseInt(strs1[0]), int2 = Integer.parseInt(strs2[0]);
		  if        (int1 > int2) {
		    if (inverts[i]) return -1;
		    else            return  1;
		  } else if (int1 < int2) {
		    if (inverts[i]) return  1;
		    else            return -1;
		  }
	        //
	        // - Compare strings
	        //
	        } else                                              {
	          int value = strs1[0].compareTo(strs2[0]);
		  if        (value > 0) {
		    if (inverts[i]) return -1;
		    else            return  1;
		  } else if (value < 0) {
		    if (inverts[i]) return  1;
		    else            return -1;
		  }
	        }
              } else return 0;
            }
	  }
	  return 0;
        }
      }

      /**
       * Scrollbar lookups for bundle_to_geom (doesn't change basd on the scroll position)
       */
      Map<Bundle,Shape>      sb_bundle_to_geom  = new HashMap<Bundle,Shape>();

      /**
       * Scrollbar lookups for geom_to_bundle (doesn't change based on the scroll position)
       */
      Map<Shape,Set<Bundle>> sb_geom_to_bundles = new HashMap<Shape,Set<Bundle>>();

      /**
       * Lookup to convert a bundle (record) to a shape on the view
       */
      Map<Bundle,Shape> bundle_to_geom = new HashMap<Bundle,Shape>();

      /**
       * Lookup to convert a shape to a bundle (record) on the view
       */
      Map<Shape,Bundle> geom_to_bundle = new HashMap<Shape,Bundle>();

      /**
       * Entity to geometry
       */
      Map<String,Set<Rectangle2D>> entity_to_geom = new HashMap<String,Set<Rectangle2D>>();

      /**
       * Geometry to entity
       */
      Map<Rectangle2D,Set<String>> geom_to_entities = new HashMap<Rectangle2D,Set<String>>();

      /**
       * Geometry to the column header name
       */
      Map<Rectangle2D,String>      geom_to_header = new HashMap<Rectangle2D,String>();

      /**
       * Return the height of the rendering
       */
      @Override
      public int getRCHeight() { return rc_h; }

      /**
       * Return the width of the rendering
       */
      @Override
      public int getRCWidth() { return rc_w; }

      /**
       * Base image
       */
      BufferedImage base_bi;

      /**
       * Font metrics - text height
       */
      int           txt_h,

      /**
       * Row height
       */
                    row_h;

      /**
       * Index of the bundle at the very top of the display.  This index is relative to the sorted list.
       */
      int           top_render_bundle_i = 0;

      /**
       * Bundle at the very top of the display
       */
      Bundle        top_render_bundle   = null;

      /**
       * Adjust which render bundle is on top.  Prepare the rendercontext to re-render.
       *
       *@param offset index offset to adjust the list
       */
      public void adjustTopRenderBundle(int offset) {
        int new_top_render_bundle_i = top_render_bundle_i + offset;
	if (new_top_render_bundle_i < 0)                   new_top_render_bundle_i = 0;
	if (new_top_render_bundle_i >= bundle_list.size()) new_top_render_bundle_i = bundle_list.size() -1;
	top_render_bundle_i = new_top_render_bundle_i;
	base_bi = null;
      }

      /**
       * Set new position for the scroll.
       *
       *@param new_position new position within the list
       */
      public void setTopRenderBundle(int new_position) {
        int new_top_render_bundle_i = new_position;
	if (new_top_render_bundle_i < 0)                   new_top_render_bundle_i = 0;
	if (new_top_render_bundle_i >= bundle_list.size()) new_top_render_bundle_i = bundle_list.size() -1;
	top_render_bundle_i = new_top_render_bundle_i;
	base_bi = null;
      }

      /**
       * Determine the column header at the specific x value.  If no header is found, return null.
       *
       *@param x x coordiante
       *
       *@return column header name for x coordinate. null if no header found.
       */
      public String columnAt(int x) {
        Iterator<Rectangle2D> it = geom_to_header.keySet().iterator();
	while (it.hasNext()) {
	  Rectangle2D rect = it.next();
	  if (x >= rect.getMinX() && x <= rect.getMaxX()) return geom_to_header.get(rect);
	}
	return null;
      }

      /**
       * Return the column index of the x position
       *
       *@param x x coordinate
       *
       *@return index of column for x
       */
      public int columnIndexAt(int x) {
	for (int i=0;i<vis_flds_x.length;i++) {
	  if (x >= vis_flds_x[i] && x <= vis_flds_x[i] + maxWidth(vis_flds[i])) return i;
	}
	return -1;
      }

      /**
       * Map that converts geometry to tagged lookups
       */
      Map<Rectangle2D,Set<String>> tagged_lu = new HashMap<Rectangle2D,Set<String>>();

      /**
       * Return the base image
       */
      @Override
      public BufferedImage getBase() {
        if (base_bi == null) {
	  Graphics2D g2d = null; try {
	    // Recalc the x offsets
            if (vis_flds_x.length > 0) { vis_flds_x[0] = 0; for (int i=1;i<vis_flds_x.length;i++) vis_flds_x[i] = vis_flds_x[i-1] + maxWidth(vis_flds[i-1]); }

	    // Construct the image, setup the background
	    base_bi = new BufferedImage(rc_w, rc_h, BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) base_bi.getGraphics();
	    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); txt_h = Utils.txtH(g2d, "0"); row_h = txt_h + 2;
	    RTColorManager.renderVisualizationBackground(base_bi, g2d);

            // Clear state... this getBase() may be called multiple times because of the scrolling issue
            tagged_lu.clear(); bundle_to_geom.clear(); geom_to_bundle.clear(); entity_to_geom.clear(); geom_to_entities.clear(); geom_to_header.clear();
            
	    //
	    // Make every other column the near bg color
	    //
	    g2d.setColor(RTColorManager.getColor("background", "almostbg"));
	    for (int i=0;i<vis_flds.length;i+=2) { g2d.fillRect(vis_flds_x[i], 0, maxWidth(vis_flds[i]), getRCHeight()); }

	    //
            // Render column lines
	    //
            g2d.setColor(RTColorManager.getColor("axis", "minor"));
            for (int i=0;i<vis_flds.length;i++) { 
	      g2d.drawLine(vis_flds_x[i]-1, 0, vis_flds_x[i]-1, rc_h); 
	      geom_to_header.put(new Rectangle2D.Double(vis_flds_x[i], 0, maxWidth(vis_flds[i]), rc_h), vis_flds[i]);
	    }

	    //
	    // Render the header (if enabled)
	    //
	    int y_off = row_h; 
            if (render_header) {
              for (int i=0;i<vis_flds.length;i++) { 
                g2d.setColor(RTColorManager.getColor("label", "header"));
                g2d.drawString(fit(vis_flds[i], vis_flds[i], g2d), vis_flds_x[i], y_off); 
  
	        // Indicate the sorting priority and direction
	        String sort_str = null; boolean sort_inv = false;
                if      (vis_flds[i].equals(sort_1)) { sort_str = "1"; sort_inv = sort_1_inv; } 
	        else if (vis_flds[i].equals(sort_2)) { sort_str = "2"; sort_inv = sort_2_inv; } 
	        else if (vis_flds[i].equals(sort_3)) { sort_str = "3"; sort_inv = sort_3_inv; }
  
	        if (sort_str != null) {
	          int w = Utils.txtW(g2d, vis_flds[i] + " ");
  
		  if (sort_inv) { g2d.setColor(RTColorManager.getColor("background", "reverse")); g2d.drawRect(vis_flds_x[i] + w, y_off - row_h + 3, Utils.txtW(g2d, sort_str) + 4, row_h - 1);
		                                                                                  g2d.drawString(sort_str, vis_flds_x[i] + w + 2, y_off);
                  } else        { g2d.setColor(RTColorManager.getColor("background", "reverse")); g2d.fillRect(vis_flds_x[i] + w, y_off - row_h + 3, Utils.txtW(g2d, sort_str) + 4, row_h - 1);
		                  g2d.setColor(RTColorManager.getColor("background", "default")); g2d.drawString(sort_str, vis_flds_x[i] + w + 2, y_off); }
  
	          g2d.setColor(RTColorManager.getColor("label", "major"));
	        }
              }
              y_off += row_h;
            }

	    //
	    // Check to see if we have data -- if so, render it
	    //
	    if (bundle_list != null && bundle_list.size() > 0 && vis_flds.length > 0) {
	      // Create the keymakers
	      Tablet tablet = bundle_list.get(0).getTablet();
              KeyMaker kms[] = new KeyMaker[vis_flds.length];
	      for (int i=0;i<kms.length;i++) {
                if (vis_flds[i].equals(TS0_FLD) || vis_flds[i].equals(TS1_FLD)) {
                } else kms[i] = new KeyMaker(tablet, vis_flds[i]);
              }

              // Determine the coloring options
              boolean tablet_supports_color = false; KeyMaker color_km = null;
	      if (color_by != null && KeyMaker.tabletCompletesBlank(tablet, color_by)) {
	        tablet_supports_color = true; color_km = new KeyMaker(tablet, color_by);
	      }

              // Track the number of cells not rendered
              int hidden_above = 0,
	          hidden_below = 0;

	      // Render the cells
	      g2d.setColor(RTColorManager.getColor("data", "default"));
              int bundle_list_i = top_render_bundle_i; hidden_above = bundle_list_i;
              while (y_off < getRCHeight() + txt_h && bundle_list_i < bundle_list.size()) {
	        Bundle bundle = bundle_list.get(bundle_list_i); hidden_below = bundle_list.size() - 1 - bundle_list_i;
                if (bundle_list_i == top_render_bundle_i) { top_render_bundle = bundle; assignTopBundle(bs,bundle); }
                bundle_list_i++;

                // If the "by row" color option is set, configure the color
		if (color_option == ColorOption.ROWS && tablet_supports_color) {
                  String strs[] = color_km.stringKeys(bundle); if (strs != null && strs.length > 0) {
                    if (strs.length == 1) g2d.setColor(RTColorManager.getColor(strs[0]));
                    else                  g2d.setColor(RTColorManager.getColor("set", "multi"));
                  }
                }

                // Calculate and store row geometry (lookups between geometry and records)
                Rectangle2D geom = new Rectangle2D.Double(0, y_off - txt_h + 1, vis_flds_x[vis_flds_x.length - 1] + maxWidth(vis_flds[vis_flds.length-1]), row_h);
                bundle_to_geom.put(bundle, geom);
		geom_to_bundle.put(geom, bundle);
  
                // Render the fields
	        for (int i=0;i<vis_flds.length;i++) {
                  if (kms[i] == null) {
                    // Timestamps are special
		    // - Configure the colors
		    if (color_option == ColorOption.ALL_CELLS) g2d.setColor(RTColorManager.getColor("data", "default"));

		    // - Render the actual timestamp
                    if        (vis_flds[i].equals(TS0_FLD)) { g2d.drawString(fit(TS0_FLD,Utils.shortDateSecsStr(bundle.ts0()),g2d), vis_flds_x[i], y_off);
                    } else if (vis_flds[i].equals(TS1_FLD)) { g2d.drawString(fit(TS1_FLD,Utils.shortDateSecsStr(bundle.ts1()),g2d), vis_flds_x[i], y_off);
                    }
                  } else {
                    // Regular fields
	            String strs[] = kms[i].stringKeys(bundle); if (strs != null && strs.length > 0) {
                      // - Handle the tagged field option
                      if (highlight_tagged) {
			// Check the strings... check each for tags
		        Set<String> all_tags = new HashSet<String>(); boolean something_tagged = false; for (int j=0;j<strs.length;j++) {
			  Set<String> tags = getRTParent().getEntityTags(strs[j], bs.ts0(), bs.ts1());
			  if (tags.size() > 0) { something_tagged = true; all_tags.addAll(tags); }
			}
			// If anything is tagged...draw something to indicate that... and keep track of the location, drawing
                        if (something_tagged) {
			  Color color = g2d.getColor(); 
                          g2d.setColor(RTColorManager.getColor("brush", "0"));
                          Rectangle2D rect = new Rectangle2D.Double(vis_flds_x[i]-1, y_off - txt_h + 1, maxWidth(vis_flds[i]) - 3, txt_h + 2);
                          if (all_tags.size() == 1) g2d.setColor(RTColorManager.getColor(all_tags.iterator().next()));
                          g2d.draw(rect);
                          Composite composite = g2d.getComposite(); g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
                          g2d.fill(rect);
			  g2d.setComposite(composite);
			  g2d.setColor(color);
                          tagged_lu.put(rect, all_tags);
			}
                      }

		      // - Configure the colors
		      if        (color_option == ColorOption.ALL_CELLS || (color_option == ColorOption.CELL && color_by != null && vis_flds[i].equals(color_by))) { 
		        if (strs.length == 1)             g2d.setColor(RTColorManager.getColor(strs[0]));
		        else                              g2d.setColor(RTColorManager.getColor("set", "multi"));
		      }

		      // - Render the actual field
                      if (vis_flds_scalar[i]) {
                        // Right justify scalars
		        g2d.drawString(fit(vis_flds[i], strs[0], g2d), vis_flds_x[i] + maxWidth(vis_flds[i]) - 3 - Utils.txtW(g2d, strs[0]), y_off);
                      } else {
                        // Left justify everything else
			if (strs.length == 1) { g2d.drawString(fit(vis_flds[i], strs[0], g2d), vis_flds_x[i]+1, y_off); } else {
			  Arrays.sort(strs); StringBuffer sb = new StringBuffer(); sb.append(strs[0]); for (int j=1;j<strs.length;j++) { sb.append(BundlesDT.DELIM); sb.append(strs[j]); }
			  g2d.drawString(fit(vis_flds[i], sb.toString(), g2d), vis_flds_x[i]+1, y_off);
			}
                      }
                      Rectangle2D rect = new Rectangle2D.Double(vis_flds_x[i] - 2, y_off - row_h + 2, maxWidth(vis_flds[i]), row_h);

                      for (int k=0;k<strs.length;k++) {
		        if (entity_to_geom.  containsKey(strs[k]) == false) entity_to_geom.  put(strs[k], new HashSet<Rectangle2D>());
		        entity_to_geom.get(strs[k]).add(rect);
			if (geom_to_entities.containsKey(rect)    == false) geom_to_entities.put(rect,    new HashSet<String>());
		        geom_to_entities.get(rect).add(strs[k]);
                      }

		      // - Return the color back to default if it's by CELL
		      if (color_option == ColorOption.CELL) g2d.setColor(RTColorManager.getColor("data", "default"));
		    }
                  }
	        }
	        y_off += row_h;
	      }

	      // Draw the scroll bar
	      scroll_bar_all = new Rectangle2D.Double(rc_w - scroll_bar_w - 1, txt_h, scroll_bar_w, rc_h - txt_h - 2);
	      g2d.setColor(RTColorManager.getColor("background", "nearbg"));  g2d.fill(scroll_bar_all);
	      g2d.setColor(RTColorManager.getColor("data",       "default")); g2d.draw(scroll_bar_all);
	      scroll_bar_y  = (int) (txt_h + (scroll_bar_all.getHeight() * top_render_bundle_i) / bundle_list.size());
	      scroll_bar_y1 = (int) (txt_h + (scroll_bar_all.getHeight() * bundle_list_i)       / bundle_list.size());
	      scroll_bar_h  = scroll_bar_y1 - scroll_bar_y; if (scroll_bar_h < scroll_bar_w) scroll_bar_h = scroll_bar_w;
	      if ((scroll_bar_y + scroll_bar_h) > scroll_bar_all.getMaxY()) scroll_bar_y = (int) (scroll_bar_all.getMaxY() - scroll_bar_h);

	      scroll_bar     = new Rectangle2D.Double(rc_w - scroll_bar_w - 1, scroll_bar_y, scroll_bar_w, scroll_bar_h);
	      g2d.fill(scroll_bar);

	      // Draw the numbers hidden
              if ((hidden_above > 0 || hidden_below > 0)) {
                g2d.setColor(RTColorManager.getColor("label", "errorfg"));
                if (hidden_above > 0) {
                  String s = "" + hidden_above; int s_w = Utils.txtW(g2d,s); int x = (int) (getRCWidth()-scroll_bar_all.getWidth()-2), y = 7 + s_w + txt_h;   Utils.drawRotatedString(g2d, s, x, y);
                  g2d.drawLine(x, txt_h, x,   y);
                  g2d.drawLine(x, txt_h, x+5, txt_h+5);
                }
                if (hidden_below > 0) {
                  String s = "" + hidden_below; int s_w = Utils.txtW(g2d,s); int x = (int) (getRCWidth()-scroll_bar_all.getWidth()-2), y = getRCHeight() - 1; Utils.drawRotatedString(g2d, s, x, y);
                  g2d.drawLine(x, y-txt_h/2, x,   y - s_w - txt_h/2);
                  g2d.drawLine(x, y-txt_h/2, x+5, y-txt_h/2-5);
                }
              }

	      // If the bundle to scrollbar geometry is empty, fill it (only want to do this once)
	      Rectangle2D last_rect = null;
	      if (sb_geom_to_bundles.keySet().size() == 0) {
	        for (int i=0;i<bundle_list.size();i++) {
		  int y = (int) (txt_h + (scroll_bar_all.getHeight() * i) / bundle_list.size());
		  if (last_rect == null || y != (int) last_rect.getY()) last_rect = new Rectangle2D.Double(scroll_bar.getX(), y, scroll_bar_w, 2);

		  if (sb_geom_to_bundles.containsKey(last_rect) == false) sb_geom_to_bundles.put(last_rect, new HashSet<Bundle>());
		  sb_geom_to_bundles.get(last_rect).add(bundle_list.get(i));
		  sb_bundle_to_geom.put(bundle_list.get(i), last_rect);
		}
	      }
	    }
	  } finally { if (g2d != null) g2d.dispose(); }
	}
	return base_bi;
      }

      /**
       * Determine how the correct length of a string to fit within a specific fields allotment of pixels.
       *
       *@param fld field to fit within
       *@param str string to modify
       *@param g2d graphics primitive
       *
       *@return modified string to fit within fields width
       */
      public String fit(String fld, String str, Graphics2D g2d) {
        int fld_w = maxWidth(fld); int str_w = Utils.txtW(g2d, str); if (str_w < fld_w) return str;

	// guess the length
	float char_w = ((float) str_w)/((float) str.length());
        int   index  = (int) (fld_w/char_w);

	// Shrink string until it fits
	while (index > 0 && Utils.txtW(g2d,str.substring(0,index) + " >") > fld_w) index--;
	return str.substring(0,index) + " >";
      }

      /**
       * Complete scroll bar region
       */
      Rectangle2D scroll_bar_all,

      /**
       * Actual scroll bar
       */
                  scroll_bar;

      /**
       * Scroll bar width
       */
      int         scroll_bar_w = 12,

      /**
       * Scroll bar y coordinate
       */
                  scroll_bar_y,
      /**
       * Scroll bar upper y coordinate
       */
                  scroll_bar_y1,

      /**
       * Scroll bar height
       */
		  scroll_bar_h;
    }
  }
}

