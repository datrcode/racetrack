/* 

Copyright 2015 David Trimm

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
import racetrack.framework.BundlesDT;
import racetrack.framework.Tablet;

import racetrack.util.Utils;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Shape;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import java.util.Arrays;
import java.util.ArrayList;
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
 * Class that implements a table (excel-like) view of the visible data.
 *
 *@author  D. Trimm
 *@version 0.1
 */
public class RTTablePanel extends RTPanel {
  /**
   *
   */
  private static final long serialVersionUID = -6263187325439922969L;

  /**
   * Tabbed pane - each tab represents a unique type of tablet
   */
  private JTabbedPane tabbed_pane;

  /**
   * Construct the correlation panel with the specified parent.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt application parent
   */
  public RTTablePanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      { 
    super(win_type,win_pos,win_uniq,rt);   
    add("Center", tabbed_pane = new JTabbedPane());
    component = new RTTableDummyComponent();
    updateBys();
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String getPrefix() { return "table"; }

  /**
   * Get the configuration for this panel.  Planned to be used for bookmarking.
   *
   *@return string representation of this configuration
   */
  public String getConfig() { return "RTTablePanel"; }

  /**
   * Set the configuration for this panel.  Could be used to recall bookmarks.
   *
   *@param str string representation for new configuration
   */
  public void         setConfig    (String str) {
    StringTokenizer st = new StringTokenizer(str, BundlesDT.DELIM);
    if (st.nextToken().equals("RTTablePanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not A RTTablePanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st2.hasMoreTokens() ? st2.nextToken() : "";
      throw new RuntimeException("Do Not Understand Type Value Pair \"" + type + "\" = \"" + value + "\"");
    }
  }
  
  /**
   * Maximum number of rows to display (even if application data exceeds this number)
   */
  private int MAX_ROWS = 1000;

  /**
   * Dummy class to maintain the necessary structure for an RTComponent
   */
  public class RTTableDummyComponent extends RTComponent {
    /**
     * Render the current view - for this class, implement the tables for 1000 items or less (1000 will (eventually) be a user defined variable)
     *
     *@return null by default since tables do not have the concept of a render context
     */
    @Override
    public RTRenderContext render(short id) {
      Iterator<String> it = table_panel_map.keySet().iterator(); while (it.hasNext()) { table_panel_map.get(it.next()).render(); }
      return null;
    }

    /**
     * Holder for this class - doesn't apply to how this component works
     */
    @Override
    public Set<Shape> overlappingShapes(Shape to_check) { return new HashSet<Shape>(); }

    /**
     * Holder for this class - doesn't apply to how this component works
     */
    @Override
    public Set<Bundle> shapeBundles(Shape shape) { return new HashSet<Bundle>(); }

    /**
     * Holder for this class - doesn't apply to how this component works
     */
    @Override
    public Set<Shape> shapes(Set<Bundle> bundles) { return new HashSet<Shape>(); }

    /**
     * Holder for this class - doesn't apply to how this component works
     */
    @Override
    public Set<Shape> allShapes() { return new HashSet<Shape>(); }
  }

  /**
   * Map to convert a tablet header string to a table panel
   */
  private Map<String,TablePanel> table_panel_map = new HashMap<String,TablePanel>();

  /**
   * Map to convert a tablepanel to the tablet header
   */
  private Map<TablePanel,String> table_panel_rmap = new HashMap<TablePanel,String>();

  /**
   * Update occured to the application data structures -- make sure the tablets are all represented by unique tabs within the window.
   */
  @Override
  public void updateBys() {
    Iterator<Tablet> it_tablet = getRTParent().getRootBundles().tabletIterator();
    while (it_tablet.hasNext()) {
      Tablet tablet     = it_tablet.next();
      String tablet_str = tablet.fileHeader();
      if (table_panel_map.keySet().contains(tablet_str) == false) {
        String tablet_str_short = tablet_str; if (tablet_str.length() > 10) tablet_str_short = tablet_str.substring(0,9) + "...";
	TablePanel table_panel; tabbed_pane.add(tablet_str_short, table_panel = new TablePanel(tablet_str));
	table_panel_map.put(tablet_str, table_panel);
	table_panel_rmap.put(table_panel, tablet_str);
      }
    }
  }

  /**
   * Contains a single tablet (or a set of tablets that match the exact header string)
   */
  class TablePanel extends JPanel implements TableModel, TableColumnModelListener, Comparator<String[]> {
    /**
     * Unique string identifying a tablet (or multiple tablets) within the application dataset
     */
    private String table_hdr_str = null;

    /**
     * Main component of this view
     */
    private JTable table;

    /**
     * Sorter comboboxes
     */
    private JComboBox sorter_cbs[];

    /**
     * Sorter inverter checkboxes
     */
    private JCheckBox sorter_inv_cbs[];

    /**
     * Construct the panel for this unique tablet(s)
     */
    public TablePanel(String table_hdr_str) {
      super(new BorderLayout());
      this.table_hdr_str = table_hdr_str; 
      
      add("Center", new JScrollPane(table = new JTable(this)));

      // Create the sorters
      JPanel sorters = new JPanel(new FlowLayout());
      sorters.add(new JLabel("Sort"));
      sorter_cbs     = new JComboBox[4];
      sorter_inv_cbs = new JCheckBox[4];
      for (int i=0;i<sorter_cbs.length;i++) {
        sorters.add(sorter_cbs[i] = new JComboBox());
	sorter_cbs[i].addItemListener(new ItemListener() { 
	  public void itemStateChanged(ItemEvent ie) { saveSortOrder(); render(); }
	  } );
	sorters.add(sorter_inv_cbs[i] = new JCheckBox());
	sorter_inv_cbs[i].addChangeListener(new ChangeListener() { 
	  public void stateChanged(ChangeEvent ce) { saveSortOrder(); render(); }
	  } );
      }
      add("South", sorters);

      // Add the listeners for the table
      table.getColumnModel().addColumnModelListener(this);

      // Configure the table
      table.setFillsViewportHeight(true);
      table.setAutoCreateRowSorter(false);
      table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    /**
     * Render the current table - this is the primary workhorse method for this component - it
     * prepares the structures for rendering into the JTable
     */
    protected synchronized void render() {
      // Temporary data structure to then copy into the main render component
      List<String[]> tmp_rows = new ArrayList<String[]>();

      // Check each tablet to see if it matches
      Iterator<Tablet> it_tablet = getRenderBundles().tabletIterator();
      while (it_tablet.hasNext()) {
        // Check for a match
        Tablet tablet = it_tablet.next(); if (tablet.fileHeader().equals(table_hdr_str)) {
	  // Grab the timestamp setting
	  has_timestamps = tablet.hasTimeStamps();
	  has_durations  = tablet.hasDurations();

	  // Figure out the mapping
	  int local_map[] = calculateMapping(tablet);
	  int allocate_len = local_map.length;
	  if (has_timestamps) allocate_len = local_map.length+1;
	  if (has_durations)  allocate_len = local_map.length+2;

	  // Go through the bundles/records and add them to the data structure
          Iterator<Bundle> it_bundle = tablet.bundleIterator(); while (tmp_rows.size() < MAX_ROWS && it_bundle.hasNext()) {
	    Bundle bundle = it_bundle.next();
            String strs[] = new String[allocate_len];
            for (int i=0;i<local_map.length;i++) { strs[i] = bundle.toString(local_map[i]); }
	    if (has_timestamps) strs[strs.length-2] = Utils.exactDate(bundle.ts0());
	    if (has_durations)  strs[strs.length-1] = Utils.exactDate(bundle.ts1());
            tmp_rows.add(strs);
	  }
	}
      }

      // If sorting is enabled, sort the fields
      if (sorter_cbs[0].getSelectedItem().equals(NONE_STR) == false) { Collections.sort(tmp_rows, this); }

      // Transition the new ones over into the current data structure
      rows = tmp_rows;

      // Indicate that the structures changed (this probably isn't the right way to do this...)
      table.setModel(new DefaultTableModel()); table.setModel(this);

      // Notify listeners to refresh the display
      // TableModelEvent tme = new TableModelEvent(this);
      // Iterator<TableModelListener> it = listeners.iterator(); while (it.hasNext()) { it.next().tableChanged(tme); }

      // Configure the component (really the table itself -- assuming there's a configuration string)
      configuring_component = true;
      // if (current_config == null) current_config = RTPrefs.retrieveString("RTTable_" + Utils.encToURL(table_hdr_str)); // DISABLED UNTIL DEBUG
      if (current_config != null) {
	// Get the width in pixels for header width adjustments
        TableColumnModel column_model = table.getColumnModel(); // column_model.getTotalColumnWidth();

	// Parse the table configuration string
	int pos = 0;
	String hdrs[]    = new String[headers.length];
        StringTokenizer st       = new StringTokenizer(current_config, ":");
        int             total_w  = Integer.parseInt(st.nextToken());
	while (st.hasMoreTokens()) {
	  String          col_str  = st.nextToken();
	  StringTokenizer st2      = new StringTokenizer(col_str, ";");
	  String          header   = Utils.decFmURL(st2.nextToken());
	  int             width_i  = Integer.parseInt(st2.nextToken());
	  for (int screen_i=0;screen_i<column_model.getColumnCount();screen_i++) {
            TableColumn table_column = column_model.getColumn(screen_i);
	    int         model_i      = table_column.getModelIndex(); // This is the value from the application model

            boolean     is_ts        = (model_i ==  headers.length    && header.equals(TS_HDR_STR)),
	                is_ts_end    = (model_i == (headers.length+1) && header.equals(TS_END_HDR_STR));

	    if (is_ts || is_ts_end || (model_i < headers.length && headers[model_i].equals(header))) {
	      table.moveColumn(screen_i, pos); 
              table_column.setPreferredWidth(width_i);
	    }
	  }
	  pos++;
        }
      }
      configuring_component = false;
    }

    /**
     * Header string for timestamps
     */
    public static final String TS_HDR_STR        = "Timestamp",
    /**
     * Header string for end timestamps
     */
                               TS_END_HDR_STR    = "Timestamp_End",
    /**
     * String for no sorting
     */
                               NONE_STR          = "|None|",
    /**
     * String for sort-by begin timestamp
     */
                               SORTBY_TS_STR     = "|Tm|Begin|",
    /**
     * String for sort-by end timestamp
     */
                               SORTBY_TS_END_STR = "|Tm|End|";

    /**
     * Mapping from header string name to integer index
     */
    Map<String,Integer> hdr_to_i = null;

    /**
     *
     */
    public int compare(String[] s, String[] t) {
      // Construct the header lookup
      if (hdr_to_i == null) {
        hdr_to_i = new HashMap<String,Integer>();
	for (int i=0;i<headers.length;i++) hdr_to_i.put(headers[i], i);
      }

      // Compare field by field
      for (int j=0;j<sorter_cbs.length;j++) {
        // Get the header to sort by
        String  hdr = (String) sorter_cbs[j].    getSelectedItem();
	boolean inv =          sorter_inv_cbs[j].isSelected();

	// Bail out early if it's None
	if (hdr.equals(NONE_STR)) return 0;

	// Get the index into the array
	int     index;
        if        (hdr.equals(SORTBY_TS_STR))     { index = headers.length;
        } else if (hdr.equals(SORTBY_TS_END_STR)) { index = headers.length+1;
	} else                                    { index = hdr_to_i.get(hdr);  }

	// get the comparison
	String s0  = s[index], t0 = t[index];
	int    cmp = s0.compareTo(t0);
	if        (cmp < 0) { if (inv) return  1; else return -1;
	} else if (cmp > 0) { if (inv) return -1; else return  1; } 
      }
      return 0;
    }

    /**
     * Header Names
     */
    String headers[] = new String[0];

    /**
     * Tablet contains timestamps -- complicates the header because timestamp field isn't included by default
     */
    boolean has_timestamps = false,

    /**
     * Tablet contains durations (begin and end timestamps) -- complicates the header because the two fields are included by default
     */
            has_durations  = false;

    /**
     * Rows
     */
    List<String[]> rows = new ArrayList<String[]>();

    /**
     * Calculate the mapping between the field indices and the bundle mappings... complicated because different
     * tablets could have the same header but in a different order (I think...)
     */
    private int[] calculateMapping(Tablet tablet) {
      List<Integer> fields = new ArrayList<Integer>();
      // Get the fields that this table contains
      final int fld_map[] = tablet.getFields();  // Don't modify this!!!

      // Copy the non-negatives into the field map
      for (int fld_i=0;fld_i<fld_map.length;fld_i++) { if (fld_map[fld_i] >= 0) fields.add(fld_i); }

      // Allocate the return array and copy them over
      int to_ret[] = new int[fields.size()];
      for (int i=0;i<to_ret.length;i++) { to_ret[i] = fields.get(i); }

      // Assign the header names if not assigned already...  otherwise, make sure the two sets align
      if (headers.length == 0) {
        headers = new String[to_ret.length];
	for (int i=0;i<headers.length;i++) headers[i] = getRTParent().getRootBundles().getGlobals().fieldHeader(to_ret[i]);
        
	// Fill in the JComboBoxes
	// - Get the headers sorted
	String sorted[] = new String[headers.length]; for (int i=0;i<headers.length;i++) sorted[i] = headers[i]; Arrays.sort(sorted);
        // - Fill in for all comboboxes
	for (int j=0;j<sorter_cbs.length;j++) {
          sorter_cbs[j].addItem(NONE_STR);
	  if (has_timestamps)              sorter_cbs[j].addItem(SORTBY_TS_STR);
	  if (has_durations)               sorter_cbs[j].addItem(SORTBY_TS_END_STR);
	  for (int i=0;i<sorted.length;i++)  sorter_cbs[j].addItem(sorted[i]);
        }
	// - Set the sort order
        String sort_config_str = null; // RTPrefs.retrieveString("RTTableS_" + Utils.encToURL(table_hdr_str)); // DISABLED UNTIL DEBUG
        if (sort_config_str != null) { 
	  StringTokenizer st = new StringTokenizer(sort_config_str, ":"); int st_i = 0;
	  while (st.hasMoreTokens()) {
            String          pair   = st.nextToken();
	    StringTokenizer st2    = new StringTokenizer(pair, ";");
	    String          header = Utils.decFmURL(st2.nextToken());
	    boolean         inv    = st2.nextToken().equals("true");
	    sorter_cbs     [st_i].setSelectedItem(header);
	    sorter_inv_cbs [st_i].setSelected(inv);
	    st_i++;
	  }
	}
      } else {
        if (debug_flag_1) { System.err.println("**\n** Multiple Tablet Issue Handled???\n**"); debug_flag_1 = false; }
      }

      return to_ret;
    }

    private boolean debug_flag_1 = true; // DEBUG - remove once issue is tested thoroughly

    // =================================================================================================================
    // =================================================================================================================
    // =================================================================================================================

    /**
     * TableModel Contract
     */

    /**
     * Listener list for table.
     */
    private Set<TableModelListener> listeners = new HashSet<TableModelListener>();

    /**
     * Add a listener to the table model listeners
     */
    public void addTableModelListener(TableModelListener l) { listeners.add(l); }

    /**
     * Remove a listener from the table model listeners
     */
    public void removeTableModelListener(TableModelListener l) { listeners.remove(l); }

    /**
     * Return the default class for each column - by default this will be a string.
     */
    public Class getColumnClass(int columnIndex) { return String.class; }

    /**
     * Return the column count for this table.
     */
    public int getColumnCount() { 
      if      (has_durations)  return headers.length + 2;
      else if (has_timestamps) return headers.length + 1;
      else                     return headers.length; 
    }

    /**
     * Return the column name for each column index.
     */
    public String getColumnName(int columnIndex) { 
      if (columnIndex >= headers.length) {
        if  (columnIndex == headers.length) return TS_HDR_STR;
	else                                return TS_END_HDR_STR;
      } else return headers[columnIndex]; 
    }

    /**
     * Return the rows for this table.
     *
     *@return number of rows (always less than the user defined max)
     */
    public int getRowCount() { return rows.size(); } 

    /**
     * Return the value at a specific row/column.
     */
    public Object getValueAt(int rowIndex, int columnIndex) { return (rows.get(rowIndex))[columnIndex]; }

    /**
     * Is the cell editable - no (at least for the first instance of this implementation)
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }

    /**
     * Set the value -- no implementation (at least for the first implementation
     */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) { }

    // =================================================================================================================
    // =================================================================================================================
    // =================================================================================================================

    /**
     * TableColumnModelListener Contract
     */

    /**
     *
     */
    public void columnAdded(TableColumnModelEvent e) { }

    /**
     *
     */
    public void columnMarginChanged(ChangeEvent e) { saveColumnConfiguration(); }

    /**
     *
     */
    public void columnMoved(TableColumnModelEvent e) { saveColumnConfiguration(); }

    /**
     * 
     */
    public void columnRemoved(TableColumnModelEvent e) { }

    /**
     *
     */
    public void columnSelectionChanged(ListSelectionEvent e) { }

    /**
     * Current configuration for this table
     */
    private String current_config = null;

    /**
     * Flag to indicate that the component is being configured (and therefore, the configuration string should not be saved)
     */
    private boolean configuring_component = false;

    /**
     * Method examines the table columns as configured, and if different than previous presets, saves them
     * as new.
     */
    private synchronized void saveColumnConfiguration() {
      TableColumnModel column_model = table.getColumnModel();
      int              total_w      = column_model.getTotalColumnWidth(); if (total_w < 1) total_w = 1;

      StringBuffer sb = new StringBuffer(); sb.append(total_w); 

      // Get the column order...  i will be the order on the screen
      for (int screen_i=0;screen_i<column_model.getColumnCount();screen_i++) {
        TableColumn table_column = column_model.getColumn(screen_i);
	int         model_i      = table_column.getModelIndex(); // This is the value from the application model
	int         column_w     = table_column.getWidth();
        String      header;

	if      (model_i <  headers.length) header = headers[model_i];
	else if (model_i == headers.length) header = TS_HDR_STR;
	else                                header = TS_END_HDR_STR;

	                                    sb.append(":"); // Colon separates different parts
	sb.append(Utils.encToURL(header));  sb.append(";"); // Semicolon separates inner parts
        sb.append(column_w);
      }

      if (configuring_component == false && (current_config == null || current_config.equals(sb.toString()) == false)) {
        RTPrefs.store("RTTable_" + Utils.encToURL(table_hdr_str), current_config = sb.toString());
      }

      // Save the sort order
      saveSortOrder();
    }

    /**
     * Method gets the sort configuration and saves it as a preference.
     */
    private synchronized void saveSortOrder() {
      if (
          sorter_cbs                      == null || 
          sorter_cbs[0]                   == null || 
          sorter_cbs[0].getSelectedItem() == null || 
          ((String) sorter_cbs[0].getSelectedItem()).equals(NONE_STR)
         ) return;

      // Get the sorter order and store that 
      StringBuffer sb = new StringBuffer();
      for (int i=0;i<sorter_cbs.length;i++) {
        String  header = (String) sorter_cbs[i].    getSelectedItem();
	boolean inv    =          sorter_inv_cbs[i].isSelected();
	if (sb.length() > 0) sb.append(":");
        sb.append(Utils.encToURL(header)); sb.append(";");
	if (inv) sb.append("true"); else sb.append("false");
      }
      RTPrefs.store("RTTableS_" + Utils.encToURL(table_hdr_str), sb.toString());
    }

    // =================================================================================================================
    // =================================================================================================================
    // =================================================================================================================
  }
}

