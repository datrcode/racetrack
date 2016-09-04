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
import java.awt.Composite;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
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
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.BundlesDT;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;

import racetrack.util.StrCountSorterD;
import racetrack.util.Utils;

import racetrack.visualization.RTColorManager;

/**
 * Implementation for a view that shows the timing of edge relationships.
 *
 *@author  D. Trimm
 *@version 0.9
 */
public class RTEdgeTimesPanel extends RTPanel {
  /**
   * 
   */
  private static final long serialVersionUID = -4940124342318124608L;

  /**
   * Menu item to indicate if the start-to-starts should be connected
   */
  JCheckBoxMenuItem start_to_start_cbmi,
  /**
   * Menu item to indicate that color should be varied by global color by option
   */
                    vary_color_cbmi,
  /**
   * Menu item to indicate that width should be varied by global count by option
   */
		    vary_width_cbmi,
  /**
   * Pixel-level accumulation -- more accurately models lines merging into a single line...  at the cost of performance
   */
                    pixel_accum_cbmi,
  /**
   * Keep the rows equally spaced
   */
                   equal_space_cbmi,
  /**
   * Show the lables
   */
                   render_labels_cbmi,
  /**
   * Show the horizontal lines
   */
		   render_horz_lines_cbmi,
  /**
   * Show the vertical lines
   */
		   render_vert_lines_cbmi;
  /**
   * From Field
   */
  private Set<String[]> edges = new HashSet<String[]>();

  /**
   * Location of the field entities in world coordinates
   */
  Map<String, Double> coord_lu = new HashMap<String,Double>();

  /**
   *
   */
  JRadioButtonMenuItem tm_straight_rbmi,
  /**
   *
   */
                       tm_weekly_rbmi,
  /**
   *
   */
                       tm_daily_rbmi;

  /**
   * Construct an instance of the edge times panel using
   * the specified parent object.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt parent GUI instance
   */
  public RTEdgeTimesPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      { 
    super(win_type, win_pos, win_uniq, rt);   

    add("Center", component = new RTEdgeTimesComponent());

    // Populate the popup menu
    JMenuItem mi;

    getRTPopupMenu().add(mi = new JMenuItem("Add Edge...")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { addEdge(); } } );
    getRTPopupMenu().addSeparator();

    ButtonGroup bg = new ButtonGroup();
    getRTPopupMenu().add(tm_straight_rbmi = new JRadioButtonMenuItem(KeyMaker.BY_STRAIGHT_STR, true)); bg.add(tm_straight_rbmi);
    getRTPopupMenu().add(tm_weekly_rbmi   = new JRadioButtonMenuItem(KeyMaker.BY_DAYOFWEEK_HOUR_STR)); bg.add(tm_weekly_rbmi);
    getRTPopupMenu().add(tm_daily_rbmi    = new JRadioButtonMenuItem(KeyMaker.BY_HOUR_MINUTE_STR));    bg.add(tm_daily_rbmi);
    getRTPopupMenu().addSeparator();

    getRTPopupMenu().add(start_to_start_cbmi = new JCheckBoxMenuItem("Connect Starts To Starts", false));
    getRTPopupMenu().add(vary_color_cbmi     = new JCheckBoxMenuItem("Vary Color", false));
    getRTPopupMenu().add(vary_width_cbmi     = new JCheckBoxMenuItem("Vary Width", false));
    getRTPopupMenu().add(pixel_accum_cbmi    = new JCheckBoxMenuItem("Pixel Level Accumulation (Slower)", false));
    getRTPopupMenu().addSeparator();

    getRTPopupMenu().add(render_labels_cbmi      = new JCheckBoxMenuItem("Render Labels", false));
    getRTPopupMenu().add(render_horz_lines_cbmi  = new JCheckBoxMenuItem("Render HLines", true));
    getRTPopupMenu().add(render_vert_lines_cbmi  = new JCheckBoxMenuItem("Render VLines", false));
    getRTPopupMenu().addSeparator();

    getRTPopupMenu().add(mi = new JMenuItem("Save Layout...")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { saveLayout(); } } );
    getRTPopupMenu().add(mi = new JMenuItem("Load Layout...")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { loadLayout(); } } );
    getRTPopupMenu().addSeparator();

    getRTPopupMenu().add(equal_space_cbmi = new JCheckBoxMenuItem("Keep Equally Spaced", false)); 
      equal_space_cbmi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { equallySpace(); } } );

    // Fill int the comboboxes
    updateBys();

    // Add the listeners
    defaultListener(tm_straight_rbmi);
    defaultListener(tm_weekly_rbmi);
    defaultListener(tm_daily_rbmi);
    defaultListener(start_to_start_cbmi);
    defaultListener(vary_color_cbmi);
    defaultListener(vary_width_cbmi);
    defaultListener(render_labels_cbmi);
    defaultListener(render_horz_lines_cbmi);
    defaultListener(render_vert_lines_cbmi);
    defaultListener(pixel_accum_cbmi);
  }

  /**
   * Equally space the coordinates
   */
  protected void equallySpace() {
    // Keep the ordering the same
    List<StrCountSorterD> list = new ArrayList<StrCountSorterD>();
    Iterator<String> it = coord_lu.keySet().iterator();
    while (it.hasNext()) {
      String key = it.next();
      double d   = coord_lu.get(key);
      list.add(new StrCountSorterD(key, d));
    }
    Collections.sort(list);

    // Set the spacing
    for (int i=0;i<list.size();i++) { 
      int index = list.size() - 1 - i;
      coord_lu.put(list.get(index).toString(), (double) i); 
    }

    // Force a re-render
    getRTComponent().render();
  }

  /**
   * File Chooser for saving and loading layout.
   */
  JFileChooser file_chooser;

  /**
   * Save the layout of the lines to a file.
   */
  protected void saveLayout() {
    try {
      if (file_chooser == null) file_chooser = new JFileChooser(".");
      if (file_chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = file_chooser.getSelectedFile();

	// Prompt for overwrite
	if (file.exists() && JOptionPane.showConfirmDialog(this, "File Exists -- Overwrite?", "Overwrite File?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) { return; }

	// Write it out
        PrintStream out = new PrintStream(new FileOutputStream(file));
	Iterator<String> it = coord_lu.keySet().iterator();
	while (it.hasNext()) {
          String key = it.next(); double value = coord_lu.get(key);
	  out.println(Utils.encToURL(key) + "," + value);
	}
	out.close();
      }
    } catch (IOException ioe) { JOptionPane.showMessageDialog(this, "IOException: " + ioe, "File Save Error", JOptionPane.ERROR_MESSAGE); }
  }

  /**
   * Load the layout of the lines from a file.
   */
  protected void loadLayout() {
    try {
      if (file_chooser == null) file_chooser = new JFileChooser(".");
      if (file_chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = file_chooser.getSelectedFile();

	// Load the data from the file
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line; while ((line = in.readLine()) != null) {
	  StringTokenizer st = new StringTokenizer(line, ","); 
	  String key = Utils.decFmURL(st.nextToken()); double value = Double.parseDouble(st.nextToken());
	  coord_lu.put(key, value);
	}
	in.close();

	// Request a render
        getRTComponent().render();
      }
    } catch (IOException ioe) { JOptionPane.showMessageDialog(this, "IOException: " + ioe, "File Save Error", JOptionPane.ERROR_MESSAGE); }
  }

  /**
   * Show a dialog to add an edge to the diagram.
   */
  protected void addEdge() { new AddEdgeDialog(); }

  /**
   *
   */
  class AddEdgeDialog extends JDialog implements ItemListener {
    JComboBox fm_cb, to_cb; JButton add_edge_bt;
    public AddEdgeDialog() {
      super(getRTParent(), "Add Edge", true);

      String fields[] = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals(), false, true, false);

      getContentPane().setLayout(new BorderLayout());
      getContentPane().add("West", fm_cb = new JComboBox(fields)); fm_cb.addItemListener(this);
      getContentPane().add("Center", new JLabel(" => "));
      getContentPane().add("East", to_cb = new JComboBox(fields)); to_cb.addItemListener(this);

      JPanel panel = new JPanel(new FlowLayout()); JButton bt;
      panel.add(add_edge_bt = new JButton("Add Edge")); add_edge_bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { addEdge(); } } ); add_edge_bt.setEnabled(false);
      panel.add(bt          = new JButton("Cancel"));   bt.         addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { cancel();  } } );
      getContentPane().add("South", panel);

      addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent we) { cancel(); } } );

      pack(); setVisible(true);
    }

    /**
     * Handle changes to the field comboboxes -- in this case, change the add edge button 
     * to disabled if the fields are both equal to each other.
     */
    public void itemStateChanged(ItemEvent ie) {
      String fm = (String) fm_cb.getSelectedItem(), to = (String) to_cb.getSelectedItem();
      if (fm.equals(to)) add_edge_bt.setEnabled(false); else add_edge_bt.setEnabled(true);
    }

    /**
     * Add the edge...  assuming it's not a dupe and not self-referential...
     */
    protected void addEdge() {
      // Form the edge
      String strs[] = new String[2];
        strs[0] = (String) fm_cb.getSelectedItem();
        strs[1] = (String) to_cb.getSelectedItem();
      // Check to make sure it's not a duplicate
      Iterator<String[]> it = edges.iterator(); boolean dupe = false;
      while (it.hasNext()) {
        String existing[] = it.next();
	if (existing[0].equals(strs[0]) && existing[1].equals(strs[1])) dupe = true;
      }
      // Add the edge and re-render
      if (dupe == false && strs[0].equals(strs[1]) == false) { edges.add(strs); getRTComponent().render(); }
      setVisible(false); dispose();
    }

    /**
     * Cancel the dialog by hiding and disposing.
     */
    protected void cancel() { setVisible(false); dispose(); } 
  }

  /**
   *
   */
  public void timeString(String str) {
    if      (str.equals(KeyMaker.BY_STRAIGHT_STR))       tm_straight_rbmi.setSelected(true);
    else if (str.equals(KeyMaker.BY_DAYOFWEEK_HOUR_STR)) tm_weekly_rbmi.setSelected(true);
    else if (str.equals(KeyMaker.BY_HOUR_MINUTE_STR))    tm_daily_rbmi.setSelected(true);
  }

  /**
   *
   */
  public String timeString() {
    if      (tm_straight_rbmi.isSelected()) return KeyMaker.BY_STRAIGHT_STR;
    else if (tm_weekly_rbmi.isSelected())   return KeyMaker.BY_DAYOFWEEK_HOUR_STR;
    else if (tm_daily_rbmi.isSelected())    return KeyMaker.BY_HOUR_MINUTE_STR;
    else                                    return KeyMaker.BY_STRAIGHT_STR;
  }

  /**
   * Set the connect start to start flag.  If true, the from-to edges will go from the start to the start
   * of the record.  If false, the from-to edges will go from the start to the end of the record.  If
   * records do not have timestamp ends then this flag has no meaning.
   *
   *@param b true for start to start connections
   */
  public void connectStartToStarts(boolean b) { start_to_start_cbmi.setSelected(b); }

  /**
   * Return the connect start to start flag.
   *
   *@return true if the start to starts should be connected
   */
  public boolean connectStartToStarts() { return start_to_start_cbmi.isSelected(); }

  /**
   *
   */
  public void    varyWidth(boolean b) { vary_width_cbmi.setSelected(b); }

  /**
   *
   */
  public boolean varyWidth()          { return vary_width_cbmi.isSelected(); }

  /**
   *
   */
  public void    varyColor(boolean b) { vary_color_cbmi.setSelected(b); }

  /**
   *
   */
  public boolean varyColor()          { return vary_color_cbmi.isSelected(); }

  /**
   *
   */
  public void    pixelAccumulation(boolean b) { pixel_accum_cbmi.setSelected(b); }

  /**
   *
   */
  public boolean pixelAccumulation()          { return pixel_accum_cbmi.isSelected(); }

  /**
   *
   */
  public void    renderLabels(boolean b) { render_labels_cbmi.setSelected(b); }

  /**
   *
   */
  public boolean renderLabels()          { return render_labels_cbmi.isSelected(); }

  /**
   *
   */
  public void    renderHLines(boolean b) { render_horz_lines_cbmi.setSelected(b); }

  /**
   *
   */
  public boolean renderHLines()          { return render_horz_lines_cbmi.isSelected(); }

  /**
   *
   */
  public void    renderVLines(boolean b) { render_vert_lines_cbmi.setSelected(b); }

  /**
   *
   */
  public boolean renderVLines()          { return render_vert_lines_cbmi.isSelected(); }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "edgetimes"; }

  /**
   * Return a string representing the configuration of this component.  Used for
   * bookmarking views to more easily recall them.
   *
   *@return string representing view configuration
   */
  @Override
  public String       getConfig    ()           { 
    // Format the edges
    StringBuffer edge_sb = new StringBuffer();
    Iterator<String[]> it = edges.iterator();
    while (it.hasNext()) {
      String edge[] = it.next();
      edge_sb.append(Utils.encToURL(edge[0])); edge_sb.append(">"); edge_sb.append(Utils.encToURL(edge[1]));
      if (it.hasNext()) edge_sb.append(BundlesDT.DELIM);
    }
    // Return as a string
    return "RTEdgeTimesPanel"                                  + BundlesDT.DELIM +
           "pixel="       + pixelAccumulation()                + BundlesDT.DELIM +
           "vcolor="      + varyColor()                        + BundlesDT.DELIM +
           "vwidth="      + varyWidth()                        + BundlesDT.DELIM +
           "start2start=" + connectStartToStarts()             + BundlesDT.DELIM +
           "edges="       + Utils.encToURL(edge_sb.toString()) + BundlesDT.DELIM +
           "time="        + Utils.encToURL(timeString())       + BundlesDT.DELIM +
	   "labels="      + renderLabels()                     + BundlesDT.DELIM +
	   "hlines="      + renderHLines()                     + BundlesDT.DELIM +
	   "vlines="      + renderVLines();
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
    if (st.nextToken().equals("RTEdgeTimesPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not A RTEdgeTimesPanel");
    while (st.hasMoreTokens()) {
      String          type_value = st.nextToken();
      StringTokenizer st2        = new StringTokenizer(type_value, "=");
      String          type       = st2.nextToken(),
                      value      = st2.nextToken();
      if      (type.equals("pixel"))       pixelAccumulation(value.toLowerCase().equals("true"));
      else if (type.equals("vcolor"))      varyColor(value.toLowerCase().equals("true"));
      else if (type.equals("vwidth"))      varyWidth(value.toLowerCase().equals("true"));
      else if (type.equals("labels"))      renderLabels(value.toLowerCase().equals("true"));
      else if (type.equals("hlines"))      renderHLines(value.toLowerCase().equals("true"));
      else if (type.equals("vlines"))      renderVLines(value.toLowerCase().equals("true"));
      else if (type.equals("start2start")) connectStartToStarts(value.toLowerCase().equals("true"));
      else if (type.equals("time"))        timeString(Utils.decFmURL(value));
      else if (type.equals("edges")) {
        StringTokenizer st_edges = new StringTokenizer(Utils.decFmURL(value), BundlesDT.DELIM);
        while (st_edges.hasMoreTokens()) {
	  String edge = st_edges.nextToken();
          String from = Utils.decFmURL(edge.substring(0, edge.indexOf(">")));
	  String to   = Utils.decFmURL(edge.substring(edge.indexOf(">")+1,edge.length()));
	  String strs[] = new String[2]; strs[0] = from; strs[1] = to; edges.add(strs);
	}
      } else System.err.println("Do Not Understand Type \"" + type + "\" ==> \"" + value + "\" In RTEdgeTimesPanel");
    }
  }

  /**
   * Override the parent method to indicate that this component has additional configuration values to save to the file.
   *
   *@return true
   */
  @Override
  public boolean hasAdditionalConfig() { return true; }

  /**
   * Override the parent method to add the additional configuration lines to the save file.
   *
   *@param list         lines to add the additional configuration to
   *@param visible_only indicates that only the visible information is being saved -- does not apply to this component
   */
  @Override
  public void addAdditionalConfig(List<String> list, boolean visible_only) {
    Iterator<String> it = coord_lu.keySet().iterator();
    while (it.hasNext()) {
      String value = it.next();
      double coord = coord_lu.get(value);
      list.add("#AC coord" + BundlesDT.DELIM + Utils.encToURL(value) + BundlesDT.DELIM + coord);
    }
  }

  /**
   * Override the parent method to parse the additional configuration information.
   *
   *@param list   lines to parse
   *@param line_i line to start on
   *
   *@return next line to parse by the callee
   */
  public int parseAdditionalConfig(List<String> list, int line_i) {
    while (line_i < list.size() && list.get(line_i).startsWith("#AC coord" + BundlesDT.DELIM)) {
      StringTokenizer st = new StringTokenizer(list.get(line_i), BundlesDT.DELIM);
      st.nextToken();
      String value = Utils.decFmURL(st.nextToken());
      double coord = Double.parseDouble(st.nextToken());
      coord_lu.put(value, coord);
      line_i++;
    }
    return line_i;
  }

  /**
   * Update the x-axis, y-axis, and y2-axis dropdown box with the latest application fields.
   */
  public void         updateBys() { }

  /**
   * Mode for the GPS component
   */
  enum Mode { FILTER, EDIT };

  /**
   * GUI component implementing the GPS visualization
   */
  public class RTEdgeTimesComponent extends RTComponent {
    /**
     * 
     */
    private static final long serialVersionUID = 3001233921171421238L;

    /**
     * Current mode of the component
     */
    private Mode    mode = Mode.FILTER;

    /**
     *
     */
    private boolean line_layout = false;

    /**
     *
     */
    private Set<String> selected = new HashSet<String>();

    /**
     *
     */
    private boolean moving    = false,
    /**
     *
     */
                    selecting = false;

    /**
     *
     */
    public void paintComponent(Graphics g) {
      super.paintComponent(g); Graphics2D g2d = (Graphics2D) g;
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc == null) return;

      // If something is selected, draw it in the selected color
      if (selected.size() > 0) {

        Iterator<String> it = selected.iterator();
	while (it.hasNext()) {
	  String      key  = it.next();
	  Rectangle2D rect = myrc.grabbers.get(key);
	  if (rect != null) {
	    // Draw the label
	    g2d.setColor(RTColorManager.getColor(key)); 
	    g2d.drawString(key, myrc.x_ins + 2, myrc.wyToSy(coord_lu.get(key)) - 1);

	    // Draw the grabber
            g2d.setColor(RTColorManager.getColor("linknode", "movenodes"));
	    g2d.draw(rect);
          }
	}
      }

      //
      // Draw the selecting interaction
      //
      if        (selecting) {
        Rectangle2D shape = new Rectangle2D.Double(mx0 < mx1 ? mx0 : mx1, my0 < my1 ? my0 : my1, Math.abs(mx1 - mx0), Math.abs(my1 - my0));
        g2d.setColor(RTColorManager.getColor("select", "region"));
	Composite orig_comp = g2d.getComposite();
	g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); g2d.fill(shape);
	g2d.setComposite(orig_comp);                                                 g2d.draw(shape);
      //
      // Draw the moving interaction
      //
      } else if (moving && line_layout == false && selected.size() > 0)    {
        g2d.setColor(RTColorManager.getColor("linknode", "movenodes"));
        int dy = my1 - my0;
        Iterator<String> it = selected.iterator();
	while (it.hasNext()) {
	  Rectangle2D rect = myrc.grabbers.get(it.next()); if (rect == null) continue;
	  g2d.draw(new Rectangle2D.Double(rect.getX(), rect.getY() + dy, rect.getWidth(), rect.getHeight()));
	}
      } else if (moving && line_layout) {
        g2d.setColor(RTColorManager.getColor("linknode", "movenodes"));
        g2d.drawLine(mx0,my0,mx,my); 
      }
    }

    /**
     * Handle key presses.  For this component, key presses allow combining selected grabbers into one and also expanding them.
     *
     *@param ke key event structure
     */
    public void keyPressed(KeyEvent ke) { 
      super.keyPressed(ke); 

      if        (ke.getKeyCode() == KeyEvent.VK_T) { // Combine grabbers into one
        RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc != null && selected.size() > 0) {
          double wy = myrc.syToWy(my);
          Iterator<String> it = selected.iterator();
	  while (it.hasNext()) coord_lu.put(it.next(), wy);
	  render();
	}
      } else if (ke.getKeyCode() == KeyEvent.VK_Y) { // Enable line layout
        line_layout = true;
      } else if (ke.getKeyCode() == KeyEvent.VK_L) { // Invert labeling
        renderLabels(!renderLabels());
      }
    }
    
    /**
     * Handle key release events.
     *
     *@param ke key event structure
     */
    public void keyReleased(KeyEvent ke) { 
      super.keyReleased(ke); 
      if (ke.getKeyCode() == KeyEvent.VK_Y) { // Disable line layout
        line_layout = false;
      }
    }

    /**
     * Handle key type events.
     *
     *@param ke key event structure
     */
    public void keyTyped(KeyEvent ke) { 
      super.keyTyped(ke); 
    }

    /**
     *
     */
    public void mouseEntered(MouseEvent me) { super.mouseEntered(me); selecting = false; repaint(); }

    /**
     *
     */
    public void mouseExited(MouseEvent me)  { super.mouseExited(me);  selecting = false; repaint(); }

    int mx0, my0, mx, my, mx1, my1;

    /**
     *
     */
    public void mousePressed(MouseEvent me) { 
      mx0 = mx1 = mx = me.getX(); my0 = my1 = my = me.getY();
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc != null) {
        if (line_layout) {
	  moving = true;
	} else if (me.getX() < myrc.x_ins) {
	  Set<String> under = new HashSet<String>();

          // Determine if it's a drag or if it's a select
	  // - What is under the mouse?
          Iterator<String> it = myrc.grabbers.keySet().iterator();
	  while (it.hasNext()) {
	    String key = it.next();
            if (myrc.grabbers.get(key).contains(me.getX(), me.getY())) under.add(key);
	  }

	  if (under.size() == 0) { // Probably a select
            selecting = true; repaint(); 
	  } else {
	    if (selected.size() == 0) { // Move of whatever is under the mouse -- set that to selected
	      selected.addAll(under); moving = true; repaint();
	    } else {
              Set<String> intersect = new HashSet<String>(); intersect.addAll(selected); intersect.retainAll(under);
	      if (intersect.size() == 0) {
                selected.clear(); selected.addAll(under); moving = true; repaint();
	      } else {
	        moving = true; repaint();
	      }
	    }
	  }
	} else super.mousePressed(me);
      } else super.mousePressed(me); 
    }

    /**
     *
     */
    public void mouseReleased(MouseEvent me) { 
      mx1 = me.getX(); my1 = me.getY();
      RenderContext myrc = (RenderContext) getRTComponent().rc;
      if (myrc != null && (moving || selecting)) {
        if        (selecting) { // Handle Selecting
	  Set<String> in_selection = new HashSet<String>();

          Rectangle2D selection = new Rectangle2D.Double(mx0 < mx1 ? mx0 : mx1, my0 < my1 ? my0 : my1, Math.abs(mx1 - mx0), Math.abs(my1 - my0));
	  Iterator<String> it = myrc.grabbers.keySet().iterator(); 
	  while (it.hasNext()) {
	    String key = it.next(); Rectangle2D grab = myrc.grabbers.get(key);
	    if (Utils.genericIntersects(grab, selection)) in_selection.add(key);
	  }

	  // Do the set operation
	  if        (last_shft_down && last_ctrl_down) { selected.retainAll(in_selection);
	  } else if (last_shft_down) { selected.removeAll(in_selection);
	  } else if (last_ctrl_down) { selected.addAll(in_selection);
	  } else                     { selected.clear(); selected.addAll(in_selection); }
	} else if (moving && line_layout) { // Handle line layout
	  if (selected.size() > 0) {
	    // Determine the spacing
            double wy0 = myrc.syToWy(my0),
	           wy1 = myrc.syToWy(my1);
            double dwy = wy1 - wy0; if (dwy < 0.01) dwy = 1.0;
	    double inc = dwy/selected.size();
	    double wy  = wy0;
	    // Adjust the values
            Iterator<String> it = selected.iterator();
	    while (it.hasNext()) { String key = it.next(); coord_lu.put(key, wy); wy+=inc; }
	    getRTComponent().render();
	  }
	} else if (moving)    { // Handle Moving
	  // Figure out the delta in world coordinates
          double wy0 = myrc.syToWy(my0),
	         wy1 = myrc.syToWy(my1);
          double dy  = wy1 - wy0;
	  // Adjust the values
          Iterator<String> it = selected.iterator();
	  while (it.hasNext()) { String key = it.next(); coord_lu.put(key, coord_lu.get(key) + dy); }
	  // Re-render (equally space first is selected)
	  if (equal_space_cbmi.isSelected()) equallySpace(); else getRTComponent().render();
	}
        moving = selecting = false; repaint();
      } else super.mouseReleased(me); 
    }

    /**
     *
     */
    public void mouseClicked(MouseEvent me) { super.mouseClicked(me); }

    /**
     *
     */
    public void mouseDragged(MouseEvent me) { 
      mx = me.getX(); my = me.getY();
      if (selecting || moving) { mx1 = me.getX(); my1 = me.getY(); repaint();
      } else super.mouseDragged(me); 
    }

    /**
     *
     */
    public void mouseMoved(MouseEvent me) { super.mouseMoved(me); mx = me.getX(); my = me.getY(); }

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
      Set<Shape> shapes = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return shapes;
      shapes.addAll(myrc.geom_to_skey.keySet());
      return shapes;
    }

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
	if (myrc.bundle_to_skeys.keySet().contains(bundle)) {
          Iterator<String> it_str = myrc.bundle_to_skeys.get(bundle).iterator();
	  while (it_str.hasNext()) {
	    String skey = it_str.next();
	    shapes.add(myrc.skey_to_geom.get(skey));
	  }
        }
      }
      return shapes;
    }

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
      if (skey != null) { set.addAll(myrc.counter_context.getBundles(skey)); }
      return set; }

    /**
     * Find the rendered shapes that overlapp with the specified shapes.
     *
     *@param to_check shape to test against... can be generic
     *
     *@return rendered shapes that overlap
     */
    public Set<Shape>  overlappingShapes(Shape to_check)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      Iterator<Shape> it = myrc.geom_to_skey.keySet().iterator();
      while (it.hasNext()) {
        Shape shape = it.next();
	if (Utils.genericIntersects(to_check, shape)) set.add(shape);
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
      Iterator<Shape> it = myrc.geom_to_skey.keySet().iterator();
      while (it.hasNext()) {
        Shape shape = it.next();
	if (shape.contains(x, y)) set.add(shape);
      }
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
      Bundles bs                       = getRenderBundles();
      String  count_by                 = getRTParent().getCountBy(),
              color_by                 = getRTParent().getColorBy();

      if (bs != null) {
        RenderContext myrc = new RenderContext(id, bs, count_by, color_by, getWidth(), getHeight(), timeString(), connectStartToStarts(), varyColor(), varyWidth(), pixelAccumulation(), renderLabels(), renderHLines(), renderVLines());
        return myrc;
      } else return null;
    }
    
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
       * Flag indicating that the start of the edge should be connected the start of the destination...
       * as opposed of start to end time
       */
      boolean start_to_start,
      /**
       * Vary the color of the lines by the global color option
       */
              vary_color,
      /**
       * Vary the width of the lines by the global count-by option
       */
              vary_width,
      /**
       * For horizontal lines, accumulate pixel by pixel for a more accurate rendering
       */
              pixel_accumulation,
      /**
       * Flag indicating that labels should be rendered
       */
	      render_labels,
      /**
       * Render horizontal guidelines
       */
	      render_hlines,
      /**
       * Render vertical guidelines
       */
	      render_vlines;
      /**
       * Active fields... helps remove parts that don't need to be drawn
       */
      Set<String> actives = new HashSet<String>();

      /**
       * Time function choice
       */
      String  time_str;

      /**
       * Flag to indicate that the time function is periodic
       */
      boolean periodic;

      /**
       * Construct the rendering variables for this rendering.
       *
       *@param id             render id (used to abort superceded renders)
       *@param bs             dataset to render
       *@param count_by       how to count elements
       *@param color_by       coloring method for shapes int the scene
       *@param w              width of rendering in pixels
       *@param h              height of rendering in pixels
       */
      public RenderContext(short id, Bundles bs, String count_by, String color_by, int w, int h, 
                           String time_str, boolean start_to_start, boolean vary_color, boolean vary_width, boolean pixel_accumulation, 
			   boolean render_labels, boolean render_hlines, boolean render_vlines) {
        render_id = id; this.bs = bs; this.count_by = count_by; this.color_by = color_by; this.rc_w = w; this.rc_h = h; 

	this.time_str           = time_str;
	this.start_to_start     = start_to_start;
	this.vary_color         = vary_color;
	this.vary_width         = vary_width;
	this.pixel_accumulation = pixel_accumulation;
	this.render_labels      = render_labels;
	this.render_hlines      = render_hlines;
	this.render_vlines      = render_vlines;

	// Create the data structure for the time axis
	List<Long> times    = new ArrayList<Long>();
	Set<Long>  time_set = new HashSet<Long>();
	long       min_time   = Long.MAX_VALUE, 
	           max_time   = Long.MIN_VALUE,
		   min_period = 0L,
		   max_period = 0L;

	// Do each edge separately
        Iterator<String[]> it_edge = edges.iterator();
	while (it_edge.hasNext()) {
	  String edge[] = it_edge.next();

          // Go through the tablets...  they have to satisfy both edge fields and have a timestamp
	  Iterator<Tablet> it_tab = bs.tabletIterator();
	  while (it_tab.hasNext()) {
            Tablet tablet = it_tab.next();
	    if (KeyMaker.tabletCompletesBlank(tablet, edge[0]) &&
	        KeyMaker.tabletCompletesBlank(tablet, edge[1]) &&
	        tablet.hasTimeStamps()) {
              // Create the keymakers
              KeyMaker km0 = new KeyMaker(tablet, edge[0]),
	               km1 = new KeyMaker(tablet, edge[1]),
		       kmt = new KeyMaker(tablet, time_str);
              periodic = !kmt.linearTime(); if (periodic) { min_period = kmt.minPeriodicValue(); max_period = kmt.maxPeriodicValue(); }
              // Process each bundle
              Iterator<Bundle> it_bun = tablet.bundleIterator();
	      while (it_bun.hasNext()) {
	        Bundle bundle  = it_bun.next();
		String keys0[] = km0.stringKeys(bundle),
		       keys1[] = km1.stringKeys(bundle);
		// Have to have a valid return
                if (keys0.length == 0 || keys0[0].equals(BundlesDT.NOTSET) || keys1.length == 0 || keys1[0].equals(BundlesDT.NOTSET)) continue;

                // Figure out the timestamps...  keep track to make the scale
		long t0 = kmt.timeStampKey(bundle),
		     t1 = kmt.endTimeStampKey(bundle);

		if (start_to_start || t0 == t1) {
                  if (time_set.contains(t0) == false) { time_set.add(t0); times.add(t0); 
		                                        if (min_time > t0) min_time = t0;
							if (max_time < t0) max_time = t0; }
		  long ts[] = new long[2]; ts[0] = ts[1] = t0; b_to_t.put(bundle, ts);
		} else {
                  if (time_set.contains(t0) == false) { time_set.add(t0); times.add(t0); 
		                                        if (min_time > t0) min_time = t0; }
                  if (time_set.contains(t1) == false) { time_set.add(t1); times.add(t1); 
							if (max_time < t1) max_time = t1; }
		  long ts[] = new long[2]; ts[0] = t0; ts[1] = t1; b_to_t.put(bundle, ts);
		}

		// Make the data structure
		b_to_w.put(bundle, new ArrayList<double[]>());
                
                // Get the world coordinates
                for (int i=0;i<keys0.length;i++) {
		  // Keep track of the worlds to bundle
                  if (coord_lu.containsKey(keys0[i]) == false) coord_lu.put(keys0[i], Math.random());
		  double w0 = coord_lu.get(keys0[i]); actives.add(keys0[i]);
		  // keep the mins and maxes
		  if (w0 < w_min) w_min = w0; if (w0 > w_max) w_max = w0;
		  for (int j=0;j<keys1.length;j++) {
                    if (coord_lu.containsKey(keys1[j]) == false) coord_lu.put(keys1[j], Math.random());
                    double w1 = coord_lu.get(keys1[j]); actives.add(keys1[j]);
		    // Keep the mins and maxes
		    if (w1 < w_min) w_min = w1; if (w1 > w_max) w_max = w1;
		    // Keep track of the bundle to worlds
                    double ds[] = new double[2]; ds[0] = w0; ds[1] = w1;
		    b_to_w.get(bundle).add(ds);
		  }
		}
	      }
	    } else {
	      Iterator<Bundle> it = tablet.bundleIterator();
	      while (it.hasNext()) addToNoMappingSet(it.next());
            }
	  }
	}

	// Calculate the xmapping
	if (periodic) { min_time = min_period; max_time = max_period; }
	xmap = AxisMapper.calculateMapping(AxisMapper.LINEAR_SCALE_STR, times, min_time, max_time);

	// Make sure that w_min and w_max are valid and not the same
	if (Double.isInfinite(w_min) || Double.isInfinite(w_max) || w_min == w_max) { w_min = 0.0; w_max = 1.0; }
      }

      /**
       *
       */
      Map<Bundle, long[]> b_to_t = new HashMap<Bundle, long[]>();

      /**
       *
       */
      Map<Long, Double> xmap;

      /**
       *
       */
      double w_min = Double.POSITIVE_INFINITY,
             w_max = Double.NEGATIVE_INFINITY;

      /**
       *
       */
      Map<Bundle, List<double[]>> b_to_w = new HashMap<Bundle, List<double[]>>();

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
       * Rendered version of this configuration
       */
      BufferedImage base_bi = null;

      int x_ins, y_ins, graph_w, graph_h;

      /**
       * Transform a world x coordinate (some type of time index) into its screen equivalent.
       *
       *@param wx world x coordinate
       *
       *@return screen x coordinate
       */
      int wxToSx(long wx) { return (int) (x_ins + xmap.get(wx) * graph_w); }

      /** 
       * Transform a world y coordinate into its screen equivalent.
       *
       *@param wy world y coordinate
       *
       *@return screen y coordinate
       */
      int wyToSy(double wy) { return (int) (y_ins + graph_h * (wy - w_min) / (w_max - w_min)); }

      /** 
       *
       */
      double syToWy(int sy) { 
	// sy = y_ins + graph_h * (wy - w_min) / (w_max - w_min);
	// (sy - y_ins) * (w_max - w_min) = graph_h * (wy - w_min) 
	// (sy - y_ins) * (w_max - w_min) / graph_h = wy - w_min
	// wy = w_min + (sy - y_ins) * (w_max - w_min) / graph_h
        return w_min + (sy - y_ins) * (w_max - w_min) / graph_h;
      }

      /**
       *
       */
      BundlesCounterContext counter_context;

      /**
       *
       */
      Map<String,Shape> skey_to_geom = new HashMap<String,Shape>();

      /**
       *
       */
      Map<Shape,String> geom_to_skey = new HashMap<Shape,String>();

      /**
       *
       */
      Map<Bundle,Set<String>> bundle_to_skeys = new HashMap<Bundle,Set<String>>();

      /**
       *
       */
      Map<String,Rectangle2D> grabbers = new HashMap<String,Rectangle2D>();

      /**
       * Render the visualization and return it as a {@link BufferedImage}
       *
       *@return rendered image
       */
      public BufferedImage getBase() { 
        if (base_bi == null) {
	 Graphics2D g2d = null;
	 try {
	  // Create the image
          base_bi = new BufferedImage(rc_w, rc_h, BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) base_bi.getGraphics();
          g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	  RTColorManager.renderVisualizationBackground(base_bi, g2d);

	  // Calculate the geometry
	  int txt_h = Utils.txtH(g2d, "0");
	  x_ins = 15; y_ins = txt_h+2;; graph_w = rc_w - 2*x_ins; graph_h = rc_h - 2*y_ins; if (graph_w < 10) graph_w = 10; if (graph_h < 10) graph_h = 10;

	  // Draw the guidlines for each world coordinate
	  g2d.setColor(RTColorManager.getColor("background", "nearbg"));
          Iterator<String> it_str = actives.iterator();
	  while (it_str.hasNext()) {
	    String field = it_str.next();
            int sy = wyToSy(coord_lu.get(field));
            if (render_hlines) g2d.drawLine(x_ins, sy, x_ins + graph_w, sy);
	    // Make little grabbers to manipulate the position of the line
	    grabbers.put(field, new Rectangle2D.Float(0, sy - 3, x_ins, 7));
	    // Display the grabbers
            g2d.draw(new Rectangle2D.Float(1, sy - 3, x_ins - 2, 7));
	  }

          // Draw the periodic markers
	  if        (time_str.equals(KeyMaker.BY_HOUR_MINUTE_STR)) {
	    int inc = 24;
	    if (graph_w > 800) { inc = 1; } else if (graph_w > 400) { inc = 4; } else if (graph_w > 200) { inc = 12; }
            for (int i=0;i<=24;i+=inc) {
	      int x = x_ins + (i * graph_w) / (24);
	      g2d.setColor(RTColorManager.getColor("background", "nearbg"));
              if (render_vlines) g2d.drawLine(x, y_ins, x, y_ins + graph_h);
	      String str = "" + i; if (str.length() == 1) str = "0" + str;
	      g2d.setColor(RTColorManager.getColor("label", "minor"));
	      g2d.drawString(str, x - Utils.txtW(g2d,str)/2, getRCHeight());
	    }
	  } else if (time_str.equals(KeyMaker.BY_DAYOFWEEK_HOUR_STR)) {
            for (int i=0;i<=6;i++) {
              int x = x_ins + (i * graph_w) / 7;
	      g2d.setColor(RTColorManager.getColor("background", "nearbg"));
              if (render_vlines) g2d.drawLine(x, y_ins, x, y_ins + graph_h);
	      String str = ""; switch (i) {
	        case 0: str = "Sun"; break; case 1: str = "Mon"; break; case 2: str = "Tue"; break;
	        case 3: str = "Wed"; break; case 4: str = "Thu"; break; case 5: str = "Fri"; break;
	        case 6: str = "Sat"; break;
	      };
	      g2d.setColor(RTColorManager.getColor("label", "minor"));
	      g2d.drawString(str, x + 2, getRCHeight());
            }
	  }

	  // Draw the labels
	  if (render_labels) {
            it_str = actives.iterator();
	    while (it_str.hasNext()) {
	      String field = it_str.next();
              int sy = wyToSy(coord_lu.get(field));
	      g2d.setColor(RTColorManager.getColor(field)); 
	      g2d.drawString(field, x_ins + 2, sy - 1);
	    }
	  }

	  // Accumulate the counter context
	  counter_context = new BundlesCounterContext(bs, count_by, color_by);
	  Iterator<Bundle> it_bun = b_to_w.keySet().iterator();
	  while (it_bun.hasNext()) {
            Bundle bundle = it_bun.next(); bundle_to_skeys.put(bundle, new HashSet<String>());
	    long           ts[] = b_to_t.get(bundle); int x0 = wxToSx(ts[0]), x1 = wxToSx(ts[1]);
            List<double[]> wys  = b_to_w.get(bundle);
	    for (int i=0;i<wys.size();i++) {
	      int y0 = wyToSy((wys.get(i))[0]), y1 = wyToSy((wys.get(i))[1]);

              if (periodic && (x0 > x1)) {
                //
                // Time function is periodic and it's a back edge
                //
		int x0p = x0, x1p = x1;

		//
		// First half
		//
		x0 = x0p;
		x1 = graph_w + x1p - x_ins;
	        if (pixel_accumulation && y0 == y1) {
                  for (int x=x0;x<x1-1;x++) {
	            String skey = x + "-" + y0 + "-" + (x+1) + "-" + y0; Line2D line = new Line2D.Float(x, y0, x+1, y0); bundle_to_skeys.get(bundle).add(skey);
	            if (skey_to_geom.containsKey(skey) == false) { skey_to_geom.put(skey, line); geom_to_skey.put(line, skey); }
	            counter_context.count(bundle, skey);
		  }
	        } else {
	          String skey = x0 + "-" + y0 + "-" + x1 + "-" + y1; Line2D line = new Line2D.Float(x0, y0, x1, y1); bundle_to_skeys.get(bundle).add(skey);
	          if (skey_to_geom.containsKey(skey) == false) { skey_to_geom.put(skey, line); geom_to_skey.put(line, skey); }
	          counter_context.count(bundle, skey);
                }

		//
		// Second half
		//
                x0 = x_ins - (graph_w - x0p);
		x1 = x1p;
	        if (pixel_accumulation && y0 == y1) {
                  for (int x=x0;x<x1-1;x++) {
	            String skey = x + "-" + y0 + "-" + (x+1) + "-" + y0; Line2D line = new Line2D.Float(x, y0, x+1, y0); bundle_to_skeys.get(bundle).add(skey);
	            if (skey_to_geom.containsKey(skey) == false) { skey_to_geom.put(skey, line); geom_to_skey.put(line, skey); }
	            counter_context.count(bundle, skey);
		  }
	        } else {
	          String skey = x0 + "-" + y0 + "-" + x1 + "-" + y1; Line2D line = new Line2D.Float(x0, y0, x1, y1); bundle_to_skeys.get(bundle).add(skey);
	          if (skey_to_geom.containsKey(skey) == false) { skey_to_geom.put(skey, line); geom_to_skey.put(line, skey); }
	          counter_context.count(bundle, skey);
                }
              } else {
                //
                // Else the general case -- either linear in time (no back edges) or the edge if forward facing
                //
	        if (pixel_accumulation && y0 == y1) {
                  for (int x=x0;x<x1-1;x++) {
	            String skey = x + "-" + y0 + "-" + (x+1) + "-" + y0; Line2D line = new Line2D.Float(x, y0, x+1, y0); bundle_to_skeys.get(bundle).add(skey);
	            if (skey_to_geom.containsKey(skey) == false) { skey_to_geom.put(skey, line); geom_to_skey.put(line, skey); }
	            counter_context.count(bundle, skey);
		  }
	        } else {
	          String skey = x0 + "-" + y0 + "-" + x1 + "-" + y1; Line2D line = new Line2D.Float(x0, y0, x1, y1); bundle_to_skeys.get(bundle).add(skey);
	          if (skey_to_geom.containsKey(skey) == false) { skey_to_geom.put(skey, line); geom_to_skey.put(line, skey); }
	          counter_context.count(bundle, skey);
                }
              }
	    }
	  }

	  g2d.setClip(new Rectangle2D.Double(x_ins, y_ins-2, graph_w, graph_h+4));

	  // Render the visualization
	  g2d.setColor(RTColorManager.getColor("data", "default"));
	  g2d.setStroke(new BasicStroke(1.5f));
	  it_str = skey_to_geom.keySet().iterator();
	  while (it_str.hasNext()) {
	    String     skey   = it_str.next(); Shape shape = skey_to_geom.get(skey);  Line2D line = (Line2D) shape;
	    if (vary_color) g2d.setColor(counter_context.binColor(skey));
	    if (vary_width) g2d.setStroke(new BasicStroke(0.4f + 6.0f*((float) (counter_context.total(skey)/counter_context.totalMaximum()))));
	    g2d.draw(shape); 
	    if (line.getY1() != line.getY2()) {
	      g2d.fill(new Ellipse2D.Double(line.getX1()-2, line.getY1()-2, 4, 4));
	      g2d.fill(new Ellipse2D.Double(line.getX2()-2, line.getY2()-2, 4, 4));
	    }
	  }
         } finally { if (g2d != null) g2d.dispose(); } // Cleanup...
        }
        return base_bi;
      }
    }
  }
}

