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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import java.io.InputStream;
import java.io.OutputStreamWriter;

import java.net.HttpURLConnection;
import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesG;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.BundlesDT;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;
import racetrack.util.EntityExtractor;
import racetrack.util.JTextFieldHistory;
import racetrack.util.Utils;
import racetrack.visualization.ColorScale;
import racetrack.visualization.RTColorManager;

/**
 * Primary visualization component for showing histogram data.
 * This implementation uses horizontal bars so that the bin
 * names can be more easily read.  The component also provides
 * two separate look and feels - one is simple and provides
 * only a single binning option while the other provides sub-binning,
 * tagging, and filtering options.
 *
 * Version 1.1 incorporates global time-binning options.
 *
 * Version 1.2 uses RTColorManager
 *
 * Version 1.3 added count-by override for the complex histogram
 *             re-arranged some of the panel to make it thinner
 *
 * Version 1.4 added export feature for pynotebooks
 *
 *@author   D. Trimm
 *@version  1.4
 */
public class RTHistoPanel extends RTPanel {
  /**
   * 
   */
  private static final long serialVersionUID = -683976831668514915L;

  /**
   * RTPrefs setting name for server-port
   */
  private static final String SERVER_PORT_SETTING_STR = "ipynotebook.server_port",
  /**
   * RTPrefs setting name for path
   */
                              PATH_SETTING_STR        = "ipynotebook.path",
  /**
   * RTPrefs setting name for filename
   */
                              FILENAME_SETTING_STR    = "ipynotebook.filename";


  /**
   * Binniing combobox - bins correspond exactly to the bar labels
   */
  JComboBox         bin_by_cb,  

  /**
   * Sub-binning combobox
   */
                    bin_by2_cb,
  /**
   * Count by override combobox
   */
                    count_by_cb;

  /**
   * Logarithmic scaling
   */
  JCheckBoxMenuItem log_cbmi, 

  /** 
   * Labeling option
   */
                    label_cbmi, 

  /**
   * Includes tags for entities on the bars
   */
		    tags_cbmi, 

  /**
   * Reverse the order of the histogram so that smallest is first
   */
		    reverse_cbmi;

  /**
   * Textfield for enabling filtering on substrings or by datatype (e.g., CIDR)
   */
  JTextField        filter_tf, 
  
  /**
   * Textfield for tagging currently displayed bars
   */
                    tag_tf;

  /**
   * Construct a default histogram that includes the binning panel.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt application parent
   */
  public RTHistoPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt) { this(win_type, win_pos, win_uniq, rt,true); }

  /**
   * Construct a histogram frame with option of including the binning method.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt            application parent
   *@param include_panel if true, include the binning method combobox
   */
  public RTHistoPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt, boolean include_panel) { this(win_type, win_pos, win_uniq, rt,include_panel,true); }

  /**
   * Construct a histogram frame with option of including the binning method.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt             application parent
   *@param include_panel  if true, include the binning method combobox
   *@param include_extras include the extra panels for sub-binning, filering, and tagging
   *
   */
  public RTHistoPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt, boolean include_panel, boolean include_extras) { 
    super(win_type, win_pos, win_uniq, rt); JMenuItem mi; JButton bt;
    add("Center", component = new RTHistoComponent());

    // Popup menu
    getRTPopupMenu().add(log_cbmi     = new JCheckBoxMenuItem("Log Scale"));
    getRTPopupMenu().add(label_cbmi   = new JCheckBoxMenuItem("Show Labels", true));
    getRTPopupMenu().add(tags_cbmi    = new JCheckBoxMenuItem("Show Tags", true));
    getRTPopupMenu().add(reverse_cbmi = new JCheckBoxMenuItem("Reverse Sort", false));
    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(mi = new JMenuItem("Copy Bins"));                   mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { copyBins();              } } );
    getRTPopupMenu().add(mi = new JMenuItem("Copy Bins (Counts)"));          mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { copyBinsWithCounts();    } } );
    getRTPopupMenu().add(mi = new JMenuItem("Keep Only Bins In Clipboard")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { keepBinsInClipboard();   } } );
    getRTPopupMenu().add(mi = new JMenuItem("Remove Bins In Clipboard"));    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { removeBinsInClipboard(); } } );
    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(mi = new JMenuItem("Export To iPython Notebook...")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { exportToPyNotebookDialog(); } } );

    // Southern panel
    // - Dropdown box for histogram binning option.
    // - Two types - simple just has dropdown, defaul has sub-binning and additional options
    JPanel panel, panel2, panel3;
    if (include_extras) {
      //
      // Complex Histogram
      // 
      panel = new JPanel(new GridLayout(4,1,2,2));
      panel2 = new JPanel(new BorderLayout(2,2)); panel.add(panel2); 
        panel2.add("Center", bin_by_cb  = new JComboBox()); 
      panel2 = new JPanel(new GridLayout(1,2));
        panel3 = new JPanel(new BorderLayout(2,2)); panel2.add(panel3);
	panel3.add("West", new JLabel("Sub")); panel3.add("Center", bin_by2_cb  = new JComboBox());
        panel3 = new JPanel(new BorderLayout(2,2)); panel2.add(panel3);
	panel3.add("West", new JLabel("Cnt")); panel3.add("Center", count_by_cb = new JComboBox());
	updateBys();
	panel.add(panel2);
      panel2 = new JPanel(new BorderLayout(2,2)); panel.add(panel2); 
        panel2.add("West",new JLabel("Fltr")); panel2.add("Center", filter_tf  = new JTextField(8)); new JTextFieldHistory(filter_tf);
      panel2 = new JPanel(new BorderLayout(2,2)); panel.add(panel2); 
        panel2.add("West",new JLabel("Tag"));  panel2.add("Center", tag_tf     = new JTextField(3)); tag_tf.setToolTipText(Utils.getTagToolTip()); new JTextFieldHistory(tag_tf);
        panel2.add("East", bt = new JButton("Clear")); bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { clearEntityTags(); } } );

      // Specific listeners for the complex histogram
      defaultListener(bin_by2_cb);
      defaultListener(count_by_cb);
      filter_tf.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) { filter(filter_tf.getText()); }});
      tag_tf.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) { tagDisplayedEntities(); }});
    } else              {

      //
      // Simple Histogram
      // 
      panel = new JPanel(new GridLayout(1,1,2,2)); 
      panel2 = new JPanel(new BorderLayout(2,2)); panel.add(panel2); panel2.add("Center", bin_by_cb  = new JComboBox()); 
      updateBys();
    }

    // Add listeners
    defaultListener(bin_by_cb);
    defaultListener(log_cbmi);
    defaultListener(label_cbmi);
    defaultListener(tags_cbmi);
    defaultListener(reverse_cbmi);
    if (include_panel) add("South", panel);
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "histogram"; }

  /**
   * Indicate that this component supports entity comments. 
   *
   *@return true because this component supports entity comments
   */
  public boolean supportsEntityComments() { return true; }

  /**
   * Increment the bin by the specified amount.
   *
   *@param inc amount to increment by, can be negative
   */
  public void nextBin(int inc) {
    int bin_i = bin_by_cb.getSelectedIndex() + inc;
    while (bin_i < 0) bin_i += bin_by_cb.getItemCount();
    bin_i = bin_i % bin_by_cb.getItemCount();
    bin_by_cb.setSelectedIndex(bin_i);
  }

  /**
   * Clears the tags on the currently displayed entities.
   */
  public void clearEntityTags() {
    RTHistoComponent.RenderContext myrc = (RTHistoComponent.RenderContext) (getRTComponent().rc); if (myrc == null) return;
    if (myrc.bs.getGlobals().isScalar(myrc.bs.getGlobals().fieldIndex(myrc.bin_by))) { getRTParent().reportToUser("Can't Clear Tags For Non-Entities"); return; } // Can't tag scalars...
    List<String> bins = myrc.counter_context.getBinsSortedByCount();
    if (bins != null) getRTParent().clearEntityTags(bins);
  }

  /**
   * Tag the displayed entities with the textfield text.
   */
  public void tagDisplayedEntities() {
    RTHistoComponent.RenderContext myrc = (RTHistoComponent.RenderContext) (getRTComponent().rc); if (myrc == null) return;
    if (myrc.bs.getGlobals().isScalar(myrc.bs.getGlobals().fieldIndex(myrc.bin_by))) { getRTParent().reportToUser("Can't Tag Non-Entities"); return; } // Can't tag scalars...
    String tags = tag_tf.getText();
    Iterator<String> it = myrc.counter_context.getBinsSortedByCount().iterator();
    while (it.hasNext()) getRTParent().tagEntity(it.next(), tags, myrc.bs.ts0(), myrc.bs.ts1());
    getRTParent().refreshAll();
  }

  /**
   * Copy the current set of displayed bins
   */
  public void copyBins() {
    RTHistoComponent.RenderContext myrc = (RTHistoComponent.RenderContext) (getRTComponent().rc); if (myrc == null) return;
    Clipboard  clipboard = getToolkit().getSystemClipboard();
    StringBuffer sb = new StringBuffer();
    Iterator<String> it = myrc.counter_context.getBinsSortedByCount().iterator();
    while (it.hasNext()) {
      String str = it.next() + "\n";
      if (reverseSort()) sb.append(str); else sb.insert(0, str);
    }
    StringSelection selection = new StringSelection(sb.toString());
    clipboard.setContents(selection, null);
  }

  /**
   *
   */
  public String[] getBins(boolean with_counts) {
    RTHistoComponent.RenderContext myrc = (RTHistoComponent.RenderContext) (getRTComponent().rc); if (myrc == null) return new String[0];
    List<String> list = new ArrayList<String>(); list.addAll(myrc.counter_context.getBinsSortedByCount());
    String array[] = new String[list.size()];
    for (int i=0;i<list.size();i++) {
      int list_i = i, array_i = reverseSort() ? i : array.length - 1 - i;
      array[array_i] = list.get(list_i);
      if (with_counts) array[array_i] += "," + myrc.counter_context.total(array[array_i]);
    }
    return array;
  }

  /**
   * Copy the current set of displayed bins with their counts
   */
  public void copyBinsWithCounts() {
    RTHistoComponent.RenderContext myrc = (RTHistoComponent.RenderContext) (getRTComponent().rc); if (myrc == null) return;
    Clipboard  clipboard = getToolkit().getSystemClipboard();
    StringBuffer sb = new StringBuffer();
    Iterator<String> it = myrc.counter_context.getBinsSortedByCount().iterator();
    while (it.hasNext()) {
      String bin = it.next(); String str = bin + "," + ((int) (myrc.counter_context.total(bin))) + "\n";
      if (reverseSort()) sb.append(str); else sb.insert(0,str);
    }
    StringSelection selection = new StringSelection(sb.toString());
    clipboard.setContents(selection, null);
  }

  /**
   * Keep the bins in the clipboard.
   * Should refactor with next method... they vary by 8 chars :(
   */
  public void keepBinsInClipboard() {
    RTHistoComponent.RenderContext myrc = (RTHistoComponent.RenderContext) (getRTComponent().rc); if (myrc == null) return;
    String          clipboard_txt = Utils.getClipboardText(this); if (clipboard_txt == null) return;
    Set<String> clipboard_set = EntityExtractor.stringSet(clipboard_txt);
    Set<Bundle> new_bundles   = new HashSet<Bundle>(); new_bundles.addAll(getRTComponent().getNoMappingSet());
    Iterator<String> it = myrc.counter_context.getBinsSortedByCount().iterator();
    while (it.hasNext()) {
      String bin = it.next();
      if (clipboard_set.contains(bin)) new_bundles.addAll(myrc.counter_context.getBundles(bin));
    }
    if (new_bundles.size() > 0) getRTParent().push(getRenderBundles().subset(new_bundles));
  }

  /**
   * Remove bins that are in the clipboard.
   * Should refactor with the previous method.
   */
  public void removeBinsInClipboard() {
    RTHistoComponent.RenderContext myrc = (RTHistoComponent.RenderContext) (getRTComponent().rc); if (myrc == null) return;
    String          clipboard_txt = Utils.getClipboardText(this);
    Set<String> clipboard_set = EntityExtractor.stringSet(clipboard_txt);
    Set<Bundle> new_bundles   = new HashSet<Bundle>(); new_bundles.addAll(getRTComponent().getNoMappingSet());
    Iterator<String> it = myrc.counter_context.getBinsSortedByCount().iterator();
    while (it.hasNext()) {
      String bin = it.next();
      if (clipboard_set.contains(bin) == false) new_bundles.addAll(myrc.counter_context.getBundles(bin));
    }
    getRTParent().push(getRenderBundles().subset(new_bundles));
  }

  /**
   * Dialog for exporting current histogram to a python notebook instance.  Dialog enables
   * user to specify server and host as well as the path to the notebook.  These values should
   * be persisted for this user.
   */
  class PyNotebookDialog extends JDialog {
    /**
     * Server and port textfield
     * - should be in the format: ip:port
     * -                          hostname:port
     */
    JTextField   server_port_tf,
    /**
     * Complete path for the put command
     */
                 path_tf,
    /**
     * Filename for the file
     */
	         filename_tf,
    /**
     * URL formed from other params (not editable)
     */
                 url_tf;
    /**
     * Export the data as simple counts
     */
    JRadioButton counts_rb,
    /**
     * Export the data as a more complex table -- table is based on the color-by option
     */
                 table_rb;

    /**
     * Construct the dialog gui.
     */
    public PyNotebookDialog() {
      super(getRTParent(), "Export to iPython Notebook", true);
      getContentPane().setLayout(new BorderLayout(10,10));
      JPanel labels  = new JPanel(new GridLayout(5,1,10,10)); getContentPane().add("West",   labels);
        labels.add(new JLabel("Hostname:Port"));
	labels.add(new JLabel("Path"));
	labels.add(new JLabel("Filename"));
	labels.add(new JLabel("URL"));
	labels.add(new JLabel("Mode"));
      
      // Get the previous user supplied inputs
      String server_port = RTPrefs.retrieveString(SERVER_PORT_SETTING_STR),
             path        = RTPrefs.retrieveString(PATH_SETTING_STR),
	     filename    = RTPrefs.retrieveString(FILENAME_SETTING_STR);

      if (server_port == null || server_port.equals("")) server_port = "localhost:8888";
      if (path        == null || path.       equals("")) path        = "MyStuff";
      if (filename    == null || filename.   equals("")) filename    = "histo.csv";

      JPanel widgets = new JPanel(new GridLayout(5,1,10,10)); getContentPane().add("Center", widgets);
        widgets.add(server_port_tf = new JTextField(server_port));
	widgets.add(path_tf        = new JTextField(path));
	widgets.add(filename_tf    = new JTextField(filename));
	widgets.add(url_tf         = new JTextField(getURL())); url_tf.setEditable(false);
        JPanel mode = new JPanel(new GridLayout(1,2,5,5));
	widgets.add(mode); ButtonGroup bg = new ButtonGroup();
	  mode.add(counts_rb = new JRadioButton("Counts Only", true)); bg.add(counts_rb);
	  mode.add(table_rb  = new JRadioButton("Table (Colors)"));    bg.add(table_rb);
      JPanel buttons = new JPanel(new FlowLayout());        getContentPane().add("South",  buttons); JButton bt;
        buttons.add(bt = new JButton("Cancel")); bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { setVisible(false); dispose(); } } );
	buttons.add(bt = new JButton("Export")); bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { export(); } } );

      // Add listeners
      CaretListener cl = new CaretListener() { public void caretUpdate(CaretEvent ce) { url_tf.setText(getURL()); } };
      server_port_tf.addCaretListener(cl);
      path_tf.       addCaretListener(cl);
      filename_tf.   addCaretListener(cl);

      // Pack and show
      pack(); setVisible(true);
    }

    /**
     * Construct the URL from the user input.
     *
     *@return url
     */
    public String getURL() { 
      // Start with the base URL
      String url  = "http://" + server_port_tf.getText() + "/api/contents/";
      // Fix up the path
      String path = path_tf.getText();
      while (path.startsWith("/")) path = path.substring(1,path.length());
      while (path.endsWith  ("/")) path = path.substring(0,path.length()-1);
      if (path.equals("") == false) url += path + "/";
      // Fix up the filename
      String filename = filename_tf.getText();
      while (filename.startsWith("/")) filename = filename.substring(1,filename.length());
      url += filename;
      // Return the complete URL
      return url;
    }

    /**
     * Export the results out.  If successful, save the values for persistence.
     */
    protected void export() {
      try {
        // Get the string for export
	String csv_str = null;
	if (counts_rb.isSelected()) csv_str = csvStringCounts();
	else                        csv_str = csvStringTable();
	if (csv_str == null) throw new RuntimeException("CSV String Null - Everything Implemented?");

	// Prepare to send the string to the pynotebook
        String put_str = "{\"type\": \"file\", \"format\": \"text\", \"content\": \"" + csv_str + "\" }";

        // Execute the put (from http://stackoverflow.com/questions/1051004/how-to-send-put-delete-http-request-in-httpurlconnection)
        URL url = new URL(getURL());
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection(); 
	httpCon.setDoOutput(true);
	httpCon.setRequestMethod("PUT");
	OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
	out.write(put_str);
	out.close();
        InputStream        in  = httpCon.getInputStream();
        in.close();
        httpCon.disconnect();

        // Store out the settings for next time
        RTPrefs.store(SERVER_PORT_SETTING_STR, server_port_tf.getText());
        RTPrefs.store(PATH_SETTING_STR,        path_tf.       getText());
	RTPrefs.store(FILENAME_SETTING_STR,    filename_tf.   getText());

        // If successful, close out the dialog
        setVisible(false); dispose();
      } catch (Throwable t) {
	// Print out an exception dialog
        JOptionPane.showMessageDialog(this, "Exception During Export: " + t, "Export Exception", JOptionPane.ERROR_MESSAGE);
      }
    }

    /**
     * Return a string that represents the currently displayed histogram - the 
     * string should be in the format expected by the pynotebook.
     *
     *@return string that represents the data in the proper format
     */
    protected String csvStringCounts() {
      // Get the render context -- it has the info for the csv
      RTHistoComponent.RenderContext myrc = (RTHistoComponent.RenderContext) (getRTComponent().rc);
      if (myrc != null) {
        // Prepare the string buffer with a table header
	StringBuffer sb = new StringBuffer(); sb.append("bin,count\\r\\n");

	// Go through the sorted bins (doesn't taken reversed into consideration...)
        List<String> bins = myrc.counter_context.getBinsSortedByCount();
	for (int i=0;i<bins.size();i++) {
	  // Get the bin and the count
	  String bin   = bins.get(i);
	  double count = myrc.counter_context.total(bin);

	  // Append them to the string buffer
          sb.append(csvSafe(bin) + "," + count + "\\r\\n");
	}
	return sb.toString();
      } else return null;
    }

    /**
     * Make a string safe according to the RFC4180 standard (or an approximation)
     *
     *@param s string to make safe
     *
     *@return safe version of the string
     */
    private String csvSafe(String s) {
      if (s.indexOf("," ) >= 0 ||
          s.indexOf("\"") >= 0 ||
	  s.indexOf("\n") >= 0) {
        StringBuffer sb = new StringBuffer();
	sb.append("\\\"");
	for (int i=0;i<s.length();i++) {
	  char c = s.charAt(i);
	  if        (c == '\"') { sb.append("\\\"\\\"");
	  } else if (c == '\n') { sb.append("\\\\n");
	  } else                { sb.append(c); }
	}
	sb.append("\\\"");
	return sb.toString();
      } else return s;
    }

    /**
     * Return a string that represents the currently displayed histogram - this
     * string needs to represent the color-by option so that the table can
     * be recreated by the pynotebook.
     *
     *@return string that represents the data in the proper format
     */
    protected String csvStringTable() {
      // Get the render context -- it has the info for the csv
      RTHistoComponent.RenderContext myrc = (RTHistoComponent.RenderContext) (getRTComponent().rc);
      if (myrc != null) {
        // Prepare the string buffer with a table header
	StringBuffer sb = new StringBuffer();

	// Go through the sorted bins (doesn't taken reversed into consideration...)
        List<String> bins  = myrc.counter_context.getBinsSortedByCount();
        List<String> cbins = myrc.counter_context.getColorBinsSortedByCount();

	// Construct the header
	sb.append("bin"); for (int j=0;j<cbins.size();j++) sb.append("," + csvSafe(cbins.get(j)));
	sb.append("\\r\\n");

	// Construct the table
	for (int i=0;i<bins.size();i++) {
	  // Get the bin
	  String bin   = bins.get(i);
	  sb.append(csvSafe(bin));

          // Go through the color bins and get their counts
	  for (int j=0;j<cbins.size();j++) {
	    String cbin  = cbins.get(j);
	    double count = myrc.counter_context.total(bin,cbin);

	    // Append them to the string buffer
            sb.append(",");
            sb.append(count);
	  }
	  sb.append("\\r\\n");
	}
	return sb.toString();
      } else return null;
    }
  }

  /**
   * Create the dialog for the iPython Notebook export.
   */
  public void exportToPyNotebookDialog() { new PyNotebookDialog(); }

  /**
   * Filter the current histogram by the substring.  The specified string
   * can have different meanings depending on it's data type.  Be default
   * it is treated as a case insensitive substring match.  Should probably
   * be made into an abstraction so that this component does not need to
   * understand the underlying datatypes (cleaner design). REFACTOR
   *
   *@param expr expression to use for the filter operation
   */
  public void filter(String expr) {
    // Parse the not
    boolean not = false;
    if (expr.length() == 0) return;
    if (expr.charAt(0) == '!') { not = true; expr = expr.substring(1,expr.length()); }
    if (expr.length() == 0) return;
    expr = expr.toLowerCase();

    // Check to make sure the render context exists...
    RTHistoComponent.RenderContext myrc = (RTHistoComponent.RenderContext) (getRTComponent().rc);
    if (myrc != null) {
      Set<Bundle> to_keep = new HashSet<Bundle>();
      to_keep.addAll(getRTComponent().no_mapping_set);
      Iterator<String> it = myrc.counter_context.binIterator();

      // Handle each expression type differently
      if        (Utils.isIPv4CIDR(expr)) {
        while (it.hasNext()) {
	  String bin = it.next();
	  boolean matches = Utils.isIPv4(bin) && Utils.ipMatchesCIDR(bin,expr);
	  if ((not == false && matches == true) || (not == true && matches == false)) 
	    to_keep.addAll(myrc.counter_context.getBundles(bin));
        }
      } else if (Utils.isIPv4(expr))   {
        while (it.hasNext()) {
	  String bin = it.next();
	  boolean contains = bin.toLowerCase().equals(expr);
	  if ((not == false && contains == true) || (not == true && contains == false)) 
	    to_keep.addAll(myrc.counter_context.getBundles(bin));
	}
      } else                         {
        while (it.hasNext()) {
          String  bin      = it.next();
          boolean contains = bin.toLowerCase().indexOf(expr) >= 0;
	  if ((not == false && contains == true) || (not == true && contains == false)) 
	    to_keep.addAll(myrc.counter_context.getBundles(bin));
        }
      }
      if (to_keep.size() > 0) getRTParent().push(myrc.bs.subset(to_keep));
    }
  }

  /**
   * Update the "bin by"  and the "sub-bin-by" options to show the current bundle headers.
   */
  public void updateBys() { updateBys(bin_by_cb); updateBys(bin_by2_cb); updateBys(count_by_cb); }

  /**
   * Update the specific combobox with the global set of heades for choosing
   * data types.
   *
   *@param cb combobox to update
   */
  public void updateBys(JComboBox cb) {
    if (cb == null) return;
    String strs[];  Object sel = cb.getSelectedItem();
    cb.removeAllItems(); 
    BundlesG globals = getRTParent().getRootBundles().getGlobals();
    if      (cb == bin_by_cb)  strs = KeyMaker.blanks(globals, false, true, true, true);
    else if (cb == bin_by2_cb) strs = KeyMaker.blanks(globals, false, true, true, true);
    else                       strs = KeyMaker.blanks(globals, true,  true, true, true);
    if (cb == bin_by2_cb)  cb.addItem(BundlesDT.COUNT_BY_NONE);
    if (cb == count_by_cb) cb.addItem(BundlesDT.COUNT_BY_DEFAULT);
    for (int i=0;i<strs.length;i++) cb.addItem(strs[i]);
    if (sel == null) cb.setSelectedIndex(0); else cb.setSelectedItem(sel);
  }

  /**
   * Log scale representation.
   *
   *@return true if logscale is selected
   */
  public boolean logScale()                  { return log_cbmi.isSelected();   }

  /**
   * Set the log scale option
   *
   *@param f log scale option
   */
  public void    logScale(boolean f)         { log_cbmi.setSelected(f); }

  /**
   * Reverse the order of the sort (so that smallest is first)
   *
   *@return true if reverse sort
   */
  public boolean reverseSort()               { return reverse_cbmi.isSelected(); }

  /**
   * Set the reverse sort option
   *
   *@param f reverse sort option
   */
  public void    reverseSort(boolean f)      { reverse_cbmi.setSelected(f); }

  /**
   * Return the header element to bin by
   *
   *@return bin by setting
   */
  public String  binBy()                     { return (String) bin_by_cb.getSelectedItem(); }

  /**
   * Set the bin by option
   *
   *@param str bin by option
   */
  public void    binBy(String str)           { bin_by_cb.setSelectedItem(str); }

  /**
   * Return the header element to sub-bin by
   *
   *@return sub-bin by setting
   */
  public String  binBy2()                    { if (bin_by2_cb == null) return BundlesDT.COUNT_BY_NONE;
                                               return (String) bin_by2_cb.getSelectedItem(); }

  /**
   * Set the sub-bin by option
   *
   *@param str sub-bin setting
   */
  public void    binBy2(String str)          { if (bin_by2_cb == null) return;
                                               bin_by2_cb.setSelectedItem(str); }

  /**
   * Return the header element to count by
   *
   *@return local count by setting
   */
  public String  countBy()                   { if (count_by_cb == null) return BundlesDT.COUNT_BY_DEFAULT;
                                               return (String) count_by_cb.getSelectedItem(); }

      String count_by = getRTParent().getCountBy();

  /**
   * Set the count-by option
   *
   *@param str new count by setting
   */
  public void    countBy(String str)         { if (count_by_cb == null) return;
                                               count_by_cb.setSelectedItem(str); }

  /**
   * Return if labels are to be drawn.
   *
   *@return true if labels should be drawn
   */
  public boolean drawLabels()                { return label_cbmi.isSelected(); }

  /**
   * Set the option to draw labels
   *
   *@param f true if labels are to be drawn
   */
  public void    drawLabels(boolean f)       { label_cbmi.setSelected(f); }

  /**
   * Return if entity tags are to be drawn.
   *
   *@return true if tags should be drawn
   */
  public boolean drawTags()                  { return tags_cbmi.isSelected(); }

  /**
   * Set the option to draw entity tags
   *
   *@param f true if entity tags are to be drawn
   *
   */
  public void    drawTags(boolean f)         { tags_cbmi.setSelected(f); }

  /**
   * Return if th truncated warning string should be drawn.  Truncated warning
   * indicates that more elements exist but were not drawn.  The reason to
   * turn off the warning is when many histograms are to be displayed and that
   * the warning itself takes up too much space.
   *
   *@return true if the truncated warning will be shown
   */
  public boolean truncatedWarning()          { return truncated_warning; }

  /**
   * Set the truncated warning display flag.
   *
   *@param f true if the warning should be displayed
   */
  public void    truncatedWarning(boolean f) { truncated_warning = f; }

  /**
   * Flag to indicate if the truncated warning should be shown.
   * Specific for one type of panel and not accessible through the GUI setting.
   */
  boolean truncated_warning = true;

  /**
   * Return a string that represent this component's current configuration.  This
   * string can then be used for bookmarks and returning to specific views.
   *
   *@return string representing component's configuration
   */
  @Override
  public String  getConfig()  { return "RTHistoPanel"                       + BundlesDT.DELIM +
                                       "binby="  + Utils.encToURL(binBy())  + BundlesDT.DELIM +
                                       "binby2=" + Utils.encToURL(binBy2()) + BundlesDT.DELIM +
				       "countby="+ Utils.encToURL(countBy())+ BundlesDT.DELIM +
                                       "log="    + logScale()               + BundlesDT.DELIM +
                                       "label="  + drawLabels()             + BundlesDT.DELIM +
				       "tags="   + drawTags()               + BundlesDT.DELIM +
				       "reverse="+ reverseSort(); }

  /**
   * Set the view's current configuration based on a string return
   * from the getConfig() method.
   *
   *@param str configuration string
   */
  @Override
  public void    setConfig(String str) {
    StringTokenizer st = new StringTokenizer(str,BundlesDT.DELIM);
    if (st.nextToken().equals("RTHistoPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not An RTHistoPanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2  = new StringTokenizer(st.nextToken(), "=");
      String          type = st2.nextToken(), value = st2.hasMoreTokens() ? st2.nextToken() : "";
      // System.err.println("RTHisto.setConfig() : \"" + type + "\" = \"" + value + "\"");
      if      (type.equals("binby"))    binBy(Utils.decFmURL(value));
      else if (type.equals("binby2"))   binBy2(Utils.decFmURL(value));
      else if (type.equals("countby"))  countBy(Utils.decFmURL(value));
      else if (type.equals("log"))      logScale(value.toLowerCase().equals("true"));
      else if (type.equals("label"))    drawLabels(value.toLowerCase().equals("true"));
      else if (type.equals("tags"))     drawTags(value.toLowerCase().equals("true"));
      else if (type.equals("reverse"))  reverseSort(value.toLowerCase().equals("true"));
      else throw new RuntimeException("Do Not Understand Type Value \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * Component that implments interaction and display of the histogram rendering.
   */
  public class RTHistoComponent extends RTComponent implements KeyListener {
    /**
      * 
      */
    private static final long serialVersionUID = -14372052592367866L;

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
      if      (shft == false && alt  == false) copyBins();
      else if (shft == true  && myrc != null)  Utils.copyToClipboard(myrc.getBase());
    }

    /**
     * Implement the default past from clipboard.  This method filters
     * to the clipboard based on the filtering rules.  May need to make
     * the copyToClipboard work symmetrically instead of current implementation.
     *
     *@param shft shift key pressed
     *@param alt  alt key pressed
     */
    @Override
    public void pasteFromClipboard (boolean shft, boolean alt) {
      if (shft == false && alt == false) keepBinsInClipboard();
    }

    /**
     * Modify the highlight transparency for this component.
     *
     *@return alpha transparency
     */
    @Override
    public float getHighlightTransparency() { return 0.3f; }

    /**
     * Return false to not darken this component during highlights.
     *
     *@return false
     */
    @Override
    public boolean componentNeedsDarkening() { return false; }

    /**
     * Scroll the histogram by the wheel movements.  Provide some acceleration to get through
     * the bins faster.  Note that users should leverage the stacked histogram to determine
     * where they are in the histogram.
     *
     *@param mwe wheel event
     */
    @Override
    public void mouseWheelMoved (MouseWheelEvent mwe) { 
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return;
      int inc = mwe.getWheelRotation();
      if (System.currentTimeMillis() - last_bin_offset_adjust < 400L && (last_inc == 0 || (inc > 0 && last_inc > 0) || (inc < 0 && last_inc < 0))) {
        if (last_inc != 0) inc = last_inc*2;
        int max_inc = 128;
        if (inc > max_inc) inc = max_inc; if (inc < -max_inc) inc = -max_inc;
        last_inc = inc;
      } else last_inc = 0;
      last_bin_offset_adjust = System.currentTimeMillis();
      myrc.adjustBinOffset(inc);
      repaint();
    }
    long last_bin_offset_adjust = 0L; int last_inc = 0;

    /**
     * Handler for key press events.
     *
     *@param ke key event
     */
    @Override
    public void keyPressed(KeyEvent ke) { super.keyPressed(ke); }

    /**
     * Handler for key release events.
     *
     *@param ke key event
     */
    @Override
    public void keyReleased(KeyEvent ke) { super.keyReleased(ke); }

    /**
     * Handler for key type events.  Used to handle shortcuts/accelerators.
     *
     *@param ke key event
     */
    @Override
    public void keyTyped(KeyEvent ke) { 
      super.keyTyped(ke); 
      char c = ke.getKeyChar();
      if        (c == ' ') logScale(!logScale());
      else if   (c == 'l') {
        if (drawLabels())  { drawLabels(false); drawTags(false); }
	else               { drawLabels(true);  drawTags(true);  }
      } else if (c == 'b') { nextBin(1);
      } else if (c == 'B') { nextBin(-1); }
    }

    /**
     * Return an array f the entities within the specified rectangle.  Entities
     * correspond to bins within the rendered view.
     *
     *@param  sx0 x boundary for rectangle
     *@param  sy0 y boundary for rectangle
     *@param  sx1 x boundary for rectangle (other side)
     *@param  sy1 y boundary for rectangle (other side)
     *
     *@return     array of entities within the specified rectangle.
     */
    @Override
    public String[] getEntitiesAt(int sx0, int sy0, int sx1, int sy1) {
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return null;
      if (sx0 == sx1) sx1 = sx0+1; if (sy0 == sy1) sy1 = sy0+1;
      Rectangle2D rect = new Rectangle2D.Double(sx0<sx1?sx0:sx1,sy0<sy1?sy0:sy1,Math.abs(sx1-sx0),Math.abs(sy1-sy0));

      // Go through the bins -- find what intersects
      Set<String>   entities = new HashSet<String>();
      Iterator<String>  it       = myrc.bin_to_shapes.keySet().iterator();
      while (it.hasNext()) {
        String bin = it.next();
	Iterator<Rectangle2D> it_rect = myrc.bin_to_shapes.get(bin).iterator();
	while (it_rect.hasNext()) {
          if (rect.intersects(it_rect.next())) entities.add(bin);
        }
      }

      // Convert to an array and return
      String strs[] = new String[entities.size()];
      entities.toArray(strs);
      return strs;
    }

    /**
     * Return the {@link Shape}s that represent the specified {@link Bundle}s.
     * Note that in the current implementation, most bundle records will have
     * two shapes - one for the regular histogram bar and the other for the
     * stacked histogram representation.
     *
     *@param  bundles specific bundles to return shape records for
     *
     *@return         set of shapes that represent the specified bundles
     */
    @Override
    public Set<Shape>  shapes(Set<Bundle> bundles) {
      Set<Shape> set = new HashSet<Shape>();
      RenderContext myrc = (RenderContext) rc;
      if (myrc != null) {
        Iterator<Bundle> it = bundles.iterator();
	while (it.hasNext()) {
	  Bundle bundle = it.next();
          String bins[] = myrc.bundle_to_bins.get(bundle);
	  if (bins != null && bins.length > 0) {
	    for (int i=0;i<bins.length;i++) set.addAll(myrc.bin_to_shapes.get(bins[i]));
	  }
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
    @Override
    public Set<Shape>  shapes(Bundle bundle) { 
      Set<Bundle> set = new HashSet<Bundle>(); set.add(bundle); 
      return shapes(set); 
    }

    /**
     * For a specific shape, return the associated bundles as a set.
     *
     *@param  shape shape must have been provided by the rendering (i.e.,
     *              cannot handle generic shapes)
     *
     *@return       set of bundle records that the specified shape represent
     */
    @Override
    public Set<Bundle> shapeBundles(Shape shape) {
      Set<Bundle> set = new HashSet<Bundle>();
      RenderContext myrc = (RenderContext) rc;
      if (myrc != null) {
        Set<String>  bins = myrc.shape_to_bins.get(shape);
	Iterator<String> it   = bins.iterator();
	while (it.hasNext()) set.addAll(myrc.counter_context.getBundles(it.next()));
      } 
      return set;
    }

    /**
     * Return the set with all the existing shapes in the rendering.
     *
     *@return set of all shapes in the view
     */
    @Override
    public Set<Shape> allShapes() {
      RenderContext myrc = (RenderContext) rc;
      if (myrc != null) { return myrc.shape_to_bins.keySet();
      } else return new HashSet<Shape>();
    }

    /**
     * For a generic specified shape, return the overlapping shapes.
     *
     *@param  shape_to_check shape to check against
     *
     *@return                set of shapes in the rendering that overlap
     *                       with the specified shape
     */
    @Override
    public Set<Shape> overlappingShapes(Shape shape_to_check) {
      Set<Shape> set = new HashSet<Shape>();
      RenderContext myrc = (RenderContext) rc;
      if (myrc != null) {
        Iterator<Shape> it = myrc.shape_to_bins.keySet().iterator();
	while (it.hasNext()) {
	  Shape  shape = it.next();
          if (Utils.genericIntersects(shape, shape_to_check)) set.add(shape);
	}
      }
      return set;
    }

    /**
     * Render the current view by creating a new render context
     * based on the GUI configuration and current set of bundle
     * records.
     *
     *@param  id render id
     *
     *@return    a render context
     */
    @Override
    public RTRenderContext render(short id) {
      clearNoMappingSet();
      // Don't draw if not visible
      if (isVisible() == false) { return null; }
      // Get the variables
      Bundles bs = getRenderBundles();
      String bin_by = binBy(), bin_by2 = binBy2(), count_by = countBy(), color_by = getRTParent().getColorBy();

      // If the count by is set to default, pull the value from the globals;
      if (count_by.equals(BundlesDT.COUNT_BY_DEFAULT)) count_by = getRTParent().getCountBy();

      if (bin_by2 != null && bin_by2.equals(BundlesDT.COUNT_BY_NONE)) bin_by2 = null;

      // Create the render context and set the base image
      if (bs != null && bin_by != null && count_by != null) { // color_by can be null...
        RenderContext myrc = new RenderContext(id, bs, bin_by, bin_by2, count_by, color_by, logScale(),
                                               drawLabels(), drawTags(), reverseSort(), getWidth(), getHeight());
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
    @Override
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
    @Override
    public Shape getFirstOrderShape(int x, int y) {
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return null;
      return new Rectangle2D.Double(x, y-1*myrc.bar_h, 1, 2*myrc.bar_h + 1);
    }

    /**
     * The shape that describes what is further from the mouse.
     *
     *@param  x mouse x coordinate for the shape
     *@param  y mouse y coordinate for the shape
     *
     *@return shape further from the mouse postion
     */
    @Override
    public Shape getSecondOrderShape(int x, int y) {
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return null;
      return new Rectangle2D.Double(x, y-2*myrc.bar_h, 1, 4*myrc.bar_h + 1);
    }

    /**
     * Storage for the bin offsets to recall after filter/un-filter interactions
     */
    protected Map<String,Integer> bin_offset_map = new HashMap<String,Integer>();

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
       * Field to use that corresponds to a bar in the histogram
       */
      String  bin_by, 
      /**
       * Subfields for the bar - this will cause more bars to occur as the
       * primary bar is further qualified
       */
              bin_by2, 
      /**
       * Length of the bar - which field to count by
       */
	      count_by, 
      /**
       * How to subcolor each bar
       */
	      color_by; 
      /**
       * Use a logarithmic scale
       */
      boolean log_scale, 
      /**
       * Draw labels
       */
              labels, 
      /**
       * Draw entity tags
       */
	      tags, 
      /**
       * Bars are entity based (depends on the bin_by field)
       */
	      entity_based, 
      /**
       * Reverse sort the histogram (smallest first)
       */
	      reverse;
      /**
       * Data to render
       */
      Bundles bs;
      /**
       * Bar height for histogram bars - varies based on label flag
       */
      int     bar_h,
      /**
       * Text height
       */
              txt_h = 10,
      /**
       * Offset of the first bucket (bin) to draw
       */
              bin_offset = 0;
      /**
       * Method to convert the bin_by (and sub bin_by) fields into the bar string
       */
      KeyMaker                     binner;
      /**
       * Method to count the individual bundles (records)
       */
      BundlesCounterContext        counter_context;
      /**
       * Lookup to convert a specific bin into the corresponding shapes
       */
      Map<String,Set<Rectangle2D>> bin_to_shapes  = new HashMap<String,Set<Rectangle2D>>();
      /**
       * Lookup to convert a shape into the underlying bins
       */
      Map<Shape,Set<String>>       shape_to_bins  = new HashMap<Shape,Set<String>>();
      /**
       * Lookup to convert a single bundle into the bins that contain it
       */
      Map<Bundle,String[]>         bundle_to_bins = new HashMap<Bundle,String[]>();
      /**
       * Minimum size bar length
       */
      final int LOG1 = 4;
      /**
       * Free up some of the memory to make the garbage collector's job easier.
       */
      @Override
      public void finalize() {
        counter_context = null;
	bin_to_shapes.clear();
	shape_to_bins.clear();
	bundle_to_bins.clear();
      }
      /**
       * Enable limited scrolling in the histogram without re-calculating all of the bins.
       *
       *@param amount number of bins to skip
       */
      public void adjustBinOffset(int amount) {
        bin_offset += amount; 
        // Keep the offset from going out-of-bounds
        int size       = counter_context.getBinsSortedByCount().size(),
            to_display = (getRCHeight()-3*txt_h)/bar_h;
        if (bin_offset >= size - 1 - to_display) bin_offset = size - 1 - to_display;
        if (bin_offset < 0) bin_offset = 0; 
        // Save the state in case the same configuration appears
        String bin_offset_key = getConfig() + "::" + bs.hashCode(); 
        if (bin_offset == 0) bin_offset_map.remove(bin_offset_key); else bin_offset_map.put(bin_offset_key, bin_offset);
        // Force the base to re-render // abc
        base_bi = null;
      }

      /**
       * Construct the render context with the specified fields in the arguments which are
       * a snapshot of the GUI settings for this component.
       *
       *@param id        render id
       *@param bs        data to render
       *@param bin_by    bars in histogram
       *@param bin_by2   secondary field to further qualify the bars
       *@param color_by  how to differentiate each bar by another field
       *@param log_scale use a logarithmic scale
       *@param labels    draw the field labels on each bar
       *@param tags      draw te entity tags on each bar
       *@param reverse   reverse the sort, smallest first
       *@param w         width of the rendering
       *@param h         height of the rendering
       */
      public RenderContext(short id, Bundles bs, String bin_by, String bin_by2, String count_by, String color_by, boolean log_scale, boolean labels, boolean tags, boolean reverse, int w, int h) {
	// Save variables...
        render_id = id; this.bs = bs; this.w = w; this.h = h; this.bin_by = bin_by; this.bin_by2 = bin_by2; this.count_by = count_by; this.color_by = color_by; this.log_scale = log_scale; this.labels = labels; this.tags = tags; this.reverse = reverse;
        entity_based = bs.getGlobals().isScalar(bs.getGlobals().fieldIndex(bin_by)) == false && bin_by2 == null;
        // Set the bin offset if it exists
        String bin_offset_key = getConfig() + "::" + bs.hashCode(); if (bin_offset_map.containsKey(bin_offset_key)) bin_offset = bin_offset_map.get(bin_offset_key);
	// Get the counter context
	counter_context = new BundlesCounterContext(bs, count_by, color_by);
	// Go through the bundles
        Iterator<Tablet> tablet_i = bs.tabletIterator();
        while (tablet_i.hasNext() && currentRenderID() == getRenderID()) {
	  Tablet tablet = tablet_i.next();
          boolean tablet_can_count = count_by.equals(KeyMaker.RECORD_COUNT_STR) || KeyMaker.tabletCompletesBlank(tablet, count_by);
	  // Differentiate binning by time
          if (KeyMaker.tabletCompletesBlank(tablet, bin_by) && (count_by.equals(KeyMaker.RECORD_COUNT_STR) || KeyMaker.tabletCompletesBlank(tablet, count_by))) {
	    // Make the binner
	    binner = new KeyMaker(tablet, bin_by);
	    KeyMaker binner2 = null; if (bin_by2 != null && KeyMaker.tabletCompletesBlank(tablet, bin_by2)) binner2 = new KeyMaker(tablet, bin_by2);
            // Go through the bundles
	    Iterator<Bundle> bundle_i = tablet.bundleIterator();
	    while (bundle_i.hasNext() && currentRenderID() == getRenderID()) {
	      Bundle bundle = bundle_i.next();
              String bins[] = binner.stringKeys(bundle);
	      if (bins != null && bins.length > 0) {
	        if (binner2 != null) {
	          String seconds[]  = binner2.stringKeys(bundle);
		  String new_bins[] = new String[bins.length * seconds.length];
		  int k=0;
		  for (int i=0;i<bins.length;i++) for (int j=0;j<seconds.length;j++)
		    new_bins[k++] = bins[i] + " " + BundlesDT.DELIM + " " + seconds[j];
		  bins = new_bins;
	        }
	        bundle_to_bins.put(bundle,bins);
	        for (int i=0;i<bins.length;i++) {
                  if (tablet_can_count) counter_context.count(bundle, bins[i]);
                }
	      }
	    }
	  } else { // Put the other bundles into the no mapping set
	    Iterator<Bundle> bundle_i = tablet.bundleIterator();
	    while (bundle_i.hasNext() && currentRenderID() == getRenderID()) {
	      Bundle bundle = bundle_i.next();
              addToNoMappingSet(bundle);
            }
          }
        }
      }

    /**
     * Return the width (in pixels) of this rendering
     *
     *@return width of render
     */
    @Override
    public int getRCWidth()  { return w; }

    /**
     * Return the height (in pixels) of this rendering
     *
     *@return height of render
     */
    @Override
    public int getRCHeight() { return h; }

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
      // Sorted bins...
      List<String> sorted = counter_context.getBinsSortedByCount();
      // Buffered image...
      bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
      // Graphics context...
      g2d = (Graphics2D) bi.getGraphics(); 
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      RTColorManager.renderVisualizationBackground(bi,g2d);
      // Rendering params...
      txt_h = Utils.txtH(g2d, "0123456789");
      int x0 = 4, y0 = 1, relw = bi.getWidth() - x0 - txt_h, index;
      // Grid lines... and legend/scale
      g2d.setColor(RTColorManager.getColor("axis", "major"));
      g2d.drawLine(x0 + relw, 0, x0 + relw, bi.getHeight() - 2*txt_h);
      String str ="" + Utils.humanReadable((long) counter_context.totalMaximum()); int str_w = Utils.txtW(g2d,str);
      g2d.drawString(str, bi.getWidth() - str_w - 2, bi.getHeight()-txt_h-1);
      Rectangle2D no_over = new Rectangle2D.Double(bi.getWidth() - str_w - 2,bi.getHeight()-2*txt_h-1,str_w,txt_h);
      // Human readable numbers
      if (log_scale) {
        long logx = 1;
        while (logx < counter_context.totalMaximum()) {
          int logw;
          logw = (int) (x0 + LOG1 + (relw-LOG1) * (Math.log(logx)/Math.log(counter_context.totalMaximum())));
          g2d.setColor(RTColorManager.getColor("axis",  "minor"));
          g2d.drawLine(logw, 0, logw, bi.getHeight() - 2*txt_h);
          str ="" + Utils.humanReadable(logx); str_w = Utils.txtW(g2d,str);
          g2d.setColor(RTColorManager.getColor("label", "minor"));
	  Rectangle2D over = new Rectangle2D.Double(logw-str_w/2,bi.getHeight()-2*txt_h-1,str_w,txt_h);
          if (over.intersects(no_over) == false) g2d.drawString(str, logw-str_w/2, bi.getHeight()-txt_h-1);
          logx *= 10;
        }
      } else {
        if (counter_context.totalMaximum() > 1) {
          long mid = (long) (counter_context.totalMaximum()/2);
          int  sx  = (int)  (x0 + (mid * relw)/counter_context.totalMaximum());
	  g2d.setColor(RTColorManager.getColor("axis", "minor"));
          g2d.drawLine(sx, 0, sx, bi.getHeight() - 2*txt_h);
          str ="" + Utils.humanReadable((long) (counter_context.totalMaximum()/2)); str_w = Utils.txtW(g2d,str);
	  g2d.setColor(RTColorManager.getColor("label", "minor"));
          g2d.drawString(str, sx, bi.getHeight()-txt_h-1);
        }
      }

      // Calculate the offset based on the scroll position
      int hidden_above = bin_offset, hidden_below = 0;
      if (reverse) index = 0 + bin_offset; else index = sorted.size() - 1 - bin_offset;

      // Keep track for the stacked histogram
      int stacked[] = new int[relw+1]; int stacked_max = 1; Set<String> accounted_for = new HashSet<String>();

      // Clear out the mappings
      bin_to_shapes.clear(); shape_to_bins.clear();

      // Rendering loop...
      bar_h = (labels || tags) ? txt_h : 4; // int bar_h = labels ? txt_h : 2;
      int y_spc = (labels || tags) ? 0 : 1;
      while (y0 < (bi.getHeight() - 3*txt_h) && index >= 0 && index < sorted.size()) {
        String bin;
	if (reverse) bin = sorted.get(index++); else bin = sorted.get(index--); accounted_for.add(bin);

        if (color_by != null) { // In color
          int bar_w, x_inc = x0;
          if (log_scale) bar_w = (int) (LOG1 + (relw-LOG1) * Math.log(counter_context.total(bin))/Math.log(counter_context.totalMaximum()));
          else           bar_w = (int) (relw * counter_context.totalNormalized(bin)); 
          if (bar_w < LOG1) bar_w = LOG1;
	  // Increment the stacked histogram block
          int tmp_bar_w = bar_w - bar_w%txt_h; stacked[tmp_bar_w]++; if (stacked_max < stacked[tmp_bar_w]) stacked_max = stacked[tmp_bar_w];
          Rectangle2D rect = new Rectangle2D.Double(x0+tmp_bar_w,bi.getHeight()-txt_h+1,txt_h,txt_h);
	  if (bin_to_shapes.containsKey(bin) == false) bin_to_shapes.put(bin,new HashSet<Rectangle2D>());
	  bin_to_shapes.get(bin).add(rect);
          if (shape_to_bins.containsKey(rect) == false) shape_to_bins.put(rect,new HashSet<String>());
	  shape_to_bins.get(rect).add(bin);
	  // Draw the actual shape
          bin_to_shapes.get(bin).add(rect = new Rectangle2D.Float(x0, y0, relw, bar_h + y_spc));
          if (shape_to_bins.containsKey(rect) == false) shape_to_bins.put(rect,new HashSet<String>());
	  shape_to_bins.get(rect).add(bin);
	  // Use sorted color bins to draw the different components so that they occur in the same ordering
          List<String> cbins = counter_context.getColorBinsSortedByCount();
          for (int i=cbins.size()-1;i>=0;i--) {
            String cbin   = cbins.get(i);
            double ctotal = counter_context.total(bin,cbin);
            if (ctotal > 0L) {
              int sub_w = (int) ((ctotal * bar_w)/counter_context.binColorTotal(bin));
              if (sub_w > 0) {
                g2d.setColor(RTColorManager.getColor(cbin));
                g2d.fillRect(x_inc, y0, sub_w, bar_h);
                x_inc += sub_w;
              }
            }
          }
          if ((bar_w + x0) > x_inc) {
            g2d.setColor(RTColorManager.getColor("set", "multi"));
            g2d.fillRect(x_inc, y0, (bar_w + x0 - x_inc), bar_h);
          }
	  int x_str = x0+2;
	  if (labels || (tags && entity_based)) {
	    if (labels) { clearStr(g2d, bin, x_str, y0 + txt_h - 1, Color.black, Color.white); x_str += Utils.txtW(g2d, bin); }
	    if (tags && entity_based) { drawTags(g2d, bin, x_str + 20, y0 + txt_h - 1); }
          }
        } else        { // Not in color
          g2d.setColor(counter_context.binColor(bin)); // g2d.setColor(Color.blue); // 2013-01-07...  giving color all the time
          if (log_scale) {
            int logw;
            if (counter_context.total(bin) <= 0.0) {
            } else {
              logw = (int) (LOG1 + (relw-LOG1) * (Math.log(counter_context.total(bin))/Math.log(counter_context.totalMaximum())));
              if (logw < LOG1) logw = LOG1;
	      // Increment the stacked histogram block
	      int bar_w = logw;
              int tmp_bar_w = bar_w - bar_w%txt_h; stacked[tmp_bar_w]++; if (stacked_max < stacked[tmp_bar_w]) stacked_max = stacked[tmp_bar_w];
              Rectangle2D rect = new Rectangle2D.Double(x0+tmp_bar_w,bi.getHeight()-txt_h+1,txt_h,txt_h);
	      if (bin_to_shapes.containsKey(bin) == false) bin_to_shapes.put(bin, new HashSet<Rectangle2D>());
	      bin_to_shapes.get(bin).add(rect);
              if (shape_to_bins.containsKey(rect) == false) shape_to_bins.put(rect,new HashSet<String>());
	      shape_to_bins.get(rect).add(bin);
	      // Draw the actual shape
              bin_to_shapes.get(bin).add(rect = new Rectangle2D.Float(x0, y0, relw, bar_h + y_spc)); // This makes it the exact visual width...  hard to select sometimes
              if (shape_to_bins.containsKey(rect) == false) shape_to_bins.put(rect,new HashSet<String>());
	      shape_to_bins.get(rect).add(bin);
	      rect = new Rectangle2D.Float(x0, y0, logw, bar_h);
              g2d.fill(rect);
            }
          } else {
            Rectangle2D rect;
            int bar_w = (int) (relw * counter_context.totalNormalized(bin));
            if (bar_w < LOG1) bar_w = LOG1;
	    // Increment the stacked histogram block
            int tmp_bar_w = bar_w - bar_w%txt_h; stacked[tmp_bar_w]++; if (stacked_max < stacked[tmp_bar_w]) stacked_max = stacked[tmp_bar_w];
            rect = new Rectangle2D.Double(x0+tmp_bar_w,bi.getHeight()-txt_h+1,txt_h,txt_h);
	    if (bin_to_shapes.containsKey(bin) == false) bin_to_shapes.put(bin, new HashSet<Rectangle2D>());
	    bin_to_shapes.get(bin).add(rect);
            if (shape_to_bins.containsKey(rect) == false) shape_to_bins.put(rect,new HashSet<String>());
	    shape_to_bins.get(rect).add(bin);
	    // Draw the actual shape
            bin_to_shapes.get(bin).add(rect = new Rectangle2D.Float(x0, y0, relw, bar_h + y_spc));
            if (shape_to_bins.containsKey(rect) == false) shape_to_bins.put(rect,new HashSet<String>());
	    shape_to_bins.get(rect).add(bin);
	    rect = new Rectangle2D.Float(x0, y0, bar_w, bar_h);
            g2d.fill(rect);
          }
	  int x_str = x0+2;
	  if (labels || (tags && entity_based)) {
	    if (labels) { clearStr(g2d, bin, x_str, y0 + txt_h - 1, Color.black, Color.white); x_str += Utils.txtW(g2d, bin); }
	    if (tags && entity_based) { drawTags(g2d, bin, x_str + 20, y0 + txt_h - 1); }
          }
        }
        y0 += (bar_h + y_spc);
      }

      // Calculate the number hidden below
      if (reverse) hidden_below = sorted.size() - index - hidden_above; else hidden_below = index + 1;

      // Provide a label to show what the bins/counts are...
      if (bin_by2 == null) str = bin_by                   + " @ " + count_by;
      else                 str = bin_by + " | " + bin_by2 + " @ " + count_by;
      // - If the number of bins exceeds the display, append a truncated warning/color
      if (truncatedWarning() && (hidden_above > 0 || hidden_below > 0)) {
        g2d.setColor(RTColorManager.getColor("label", "errorfg"));   str += " TRUNC";
        if (hidden_above > 0) { 
          String s = "" + hidden_above; int s_w = Utils.txtW(g2d,s); int x = getRCWidth()-txt_h-2, y = 7 + s_w;                 Utils.drawRotatedString(g2d, s, x, y); 
          g2d.drawLine(x, 3, x,   y);
          g2d.drawLine(x, 3, x+5, 7+5);
        }
        if (hidden_below > 0) { 
          String s = "" + hidden_below; int s_w = Utils.txtW(g2d,s); int x = getRCWidth()-txt_h-2, y = getRCHeight() - 2*txt_h; Utils.drawRotatedString(g2d, s, x, y); 
          g2d.drawLine(x, y-txt_h/2, x,   y - s_w - txt_h/2);
          g2d.drawLine(x, y-txt_h/2, x+5, y-txt_h/2-5);
        }
      }  else {
        g2d.setColor(RTColorManager.getColor("label", "defaultfg"));
      }
      Utils.drawRotatedString(g2d, str, getRCWidth(), getRCHeight()/2 + Utils.txtW(g2d,str)/2);

      // Add the rest to the stacked histogram
      for (index=0;index<sorted.size();index++) {
	// Get the bin, check to see if it's already been accounted for
	String bin = sorted.get(index); if (accounted_for.contains(bin)) continue;
	// Determine the bar width
	int bar_w;
        if (log_scale) bar_w = (int) (LOG1 + (relw-LOG1) * Math.log(counter_context.total(bin))/Math.log(counter_context.totalMaximum()));
        else           bar_w = (int) (relw * counter_context.totalNormalized(bin)); 
        if (bar_w < LOG1) bar_w = LOG1;
	// Increment the stacked histogram block
        bar_w = bar_w - bar_w%txt_h; stacked[bar_w]++; if (stacked_max < stacked[bar_w]) stacked_max = stacked[bar_w];
        Rectangle2D rect = new Rectangle2D.Double(x0+bar_w,bi.getHeight()-txt_h+1,txt_h,txt_h);
	if (bin_to_shapes.containsKey(bin) == false) bin_to_shapes.put(bin, new HashSet<Rectangle2D>());
	bin_to_shapes.get(bin).add(rect);
        if (shape_to_bins.containsKey(rect) == false) shape_to_bins.put(rect,new HashSet<String>());
	shape_to_bins.get(rect).add(bin);
      }

      // Draw the stacked version
      // ColorScale cs = new AbridgedSpectra();
      ColorScale cs = RTColorManager.getContinuousColorScale();
      for (int i=0;i<stacked.length;i++) {
        if (stacked[i] > 0) {
          Rectangle2D rect = new Rectangle2D.Double(x0+i+1,bi.getHeight()-txt_h+2,txt_h-2,txt_h-2);
          g2d.setColor(cs.at(stacked[i]/((float) stacked_max)));
	  g2d.fill(rect);
	}
      }
      } finally { if (g2d != null) g2d.dispose(); } // Clean up, return the image
      return (base_bi = bi);
      }

      /**
       * Draw the entity tags in the histogram rendering.
       *
       *@param  g2d     graphic primitive object
       *@param  entity  entity to draw the tags for
       *@param  sx      screen x coordinate (image coordinate) for the label to be drawn
       *@param  sy      screen y coordinate (image coordinate) for the label to be drawn
       *
       *@return         new x coordinate after tags have been drawn
       */
      private int drawTags(Graphics2D g2d, String entity, int sx, int sy) {
        Set<String> tags = getRTParent().getEntityTags(entity, bs.ts0(), bs.ts1());
	if (tags != null && tags.size() > 0) {
	  String strs[] = new String[tags.size()]; Iterator<String> it = tags.iterator(); for (int i=0;i<strs.length;i++) strs[i] = it.next();
	  Arrays.sort(strs);
	  for (int i=0;i<strs.length;i++) {
            if (Utils.tagIsTypeValue(strs[i])) clearStr(g2d, strs[i], sx, sy, Color.darkGray, Color.white);
	    else                               clearStr(g2d, strs[i], sx, sy, Color.darkGray, Color.white);
	    sx += Utils.txtW(g2d, strs[i]) + 12;
	  }
	}
        return sx;
      }
    }
  }
}

