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
import java.awt.Color;
import java.awt.Composite;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.BundlesDT;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;
import racetrack.kb.BundlesTimeExpander;
import racetrack.util.EntityExtractor;
import racetrack.util.Interval;
import racetrack.util.SubText;
import racetrack.util.TimeStamp;
import racetrack.util.Utils;
import racetrack.visualization.RTColorManager;

/**
 * Panel for rendering time-based graphs of the data.  The panel
 * provides methods to manipulate the scale and ways to graph the
 * data.  The class should be re-written to be more extensible since
 * the types of implemented visualizations is rather rudimentary.
 *
 * Version 1.1 - Updated to work with global color manager
 *
 *
 *@author  D. Trimm
 *@version 1.1
 */
public class RTTimePanel extends RTPanel {
  /**
   * 
   */
  private static final long serialVersionUID = 4361113646437118043L;

  /**
   * Dropdown menu for choosing the type of graph
   */
  // JComboBox         type_cb,

  /**
   * Dropdown menu for overriding the global count-by method
   */
  JComboBox         count_cb,

  /**
   * Dropdown menu for how time will be mapped
   */
                    mapper_cb;

  /**
   * Checkbox for logarithmic scaling
   */
  JCheckBoxMenuItem log_cbmi, 

  /**
   * Checkbox to fix the timeframe
   */
                    fix_cbmi,

  /**
   * Checkbox for accumulative graph rendering
   */
                    agg_cbmi,

  /**
   * Checkbox for marker visibility
   */
		    markers_cbmi;


  /**
   * Various strings used for menus and dropdown boxes.  Name
   * should be sufficient explanation.
   */
  final static String TYPE_BARCHART       = "Bars",
                      TYPE_LINECHART      = "Lines",
                      TYPE_LINECHART_NORM = "Lines (Norm)";
  final static String TYPE_STRS[] = { TYPE_BARCHART,
                                      TYPE_LINECHART,
                                      TYPE_LINECHART_NORM };
  final static String TIMEBIN_CONTINUOUS     = "Continuous",
                      TIMEBIN_CONTINUOUS_DUR = "Continuous (Dur)",
                      TIMEBIN_CONTINUOUS_2   = "Continuous 2p",
                      TIMEBIN_CONTINUOUS_4   = "Continuous 4p",
                      TIMEBIN_CONTINUOUS_8   = "Continuous 8p",
                      TIMEBIN_MINUTES        = "Mins (Bin)",
                      TIMEBIN_5MINUTES       = "5 Mins (Bin)",
                      TIMEBIN_HOURS          = "Hrs (Bin)",
                      TIMEBIN_DAYS           = "Days (Bin)",
                      TIMEBIN_MONTHS         = "Months (Bin)";
  final static String PERIOD_MINUTES         = "Period: Mins",
                      PERIOD_HOURS           = "Period: Hrs",
                      PERIOD_DAYS            = "Period: Days",
                      PERIOD_WEEKS           = "Period: Weeks";
  final static String MAPPER_STRS[] = { TIMEBIN_CONTINUOUS,
                                        TIMEBIN_CONTINUOUS_DUR,
                                        TIMEBIN_CONTINUOUS_2,
                                        TIMEBIN_CONTINUOUS_4,
                                        TIMEBIN_CONTINUOUS_8,
                                        TIMEBIN_MINUTES,
                                        TIMEBIN_5MINUTES,
                                        TIMEBIN_HOURS,
                                        TIMEBIN_DAYS,
					TIMEBIN_MONTHS,
                                        PERIOD_MINUTES,
                                        PERIOD_HOURS,
                                        PERIOD_DAYS,
                                        PERIOD_WEEKS };
  final static String PERIOD_STRS[] = { PERIOD_MINUTES,
                                        PERIOD_HOURS,
                                        PERIOD_DAYS,
                                        PERIOD_WEEKS };

  /**
   * Constants for timeframes in milliseconds.
   */
  final static long MINUTES = 60L * 1000L,
                    HOURS   = 60L * MINUTES,
                    DAYS    = 24L * HOURS,
                    WEEKS   =  7L * DAYS,
		    MONTHS  = 30L * DAYS; // Approximately...

  /**
   * Align the timeframe so that the time bin has a full bin.
   *
   *@param  begin      fix the beginning or the ending bin?
   *@param  ts         time value to fix
   *@param  bin_in_ms  size of bin in milliseconds
   *
   *@return fixed time frame
   */
  public static long fixTS(boolean begin, long ts, long bin_in_ms) {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.setTimeInMillis(ts);
    cal.set(Calendar.MILLISECOND, 0);
    cal.set(Calendar.SECOND,      0);
    // Roll it back to the beginning of this period
    if (bin_in_ms == HOURS || bin_in_ms == DAYS || bin_in_ms == MONTHS) cal.set(Calendar.MINUTE,       0);
    if (                      bin_in_ms == DAYS || bin_in_ms == MONTHS) cal.set(Calendar.HOUR_OF_DAY,  0);
    if (                                           bin_in_ms == MONTHS) cal.set(Calendar.DAY_OF_MONTH, 0);
    // Roll it forward by the time if not begin
    if (!begin) {
      if      (bin_in_ms == MINUTES)   cal.add(Calendar.MINUTE,        1);
      else if (bin_in_ms == 5*MINUTES) cal.add(Calendar.MINUTE,        5);
      else if (bin_in_ms == HOURS)     cal.add(Calendar.HOUR_OF_DAY,   1);
      else if (bin_in_ms == DAYS)      cal.add(Calendar.HOUR_OF_DAY,  24);
      else if (bin_in_ms == MONTHS)    cal.add(Calendar.MONTH,         1);
    }
    return cal.getTimeInMillis();
  }

  /**
   * Create a default instance with the specified GUI parent.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt       application reference
   */
  public RTTimePanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt) { this(win_type,win_pos,win_uniq,rt,true); }

  /**
   * Create an instance with the specified GUI parent and include
   * the configuration panel if specified.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt            GUI parent
   *@param include_panel true to include the panel
   */
  public RTTimePanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt, boolean include_panel) { 
    super(win_type, win_pos, win_uniq, rt); JMenuItem mi;

    System.err.println("**\n** Inefficiency in map - should check for time at the tablet layer...\n**");

    add("Center", component = new RTTimeComponent());

    JPanel panel = new JPanel(new FlowLayout());
    panel.add(new JLabel("Count"));  panel.add(count_cb  = new JComboBox());
    // panel.add(new JLabel("Type"));   panel.add(type_cb   = new JComboBox(TYPE_STRS)); // 2015-02-09 - types don't work that well
    panel.add(new JLabel("Map"));    panel.add(mapper_cb = new JComboBox(MAPPER_STRS));

    // Markers
    getRTPopupMenu().add(mi = new JMenuItem("Add Marker"));                      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { addTimeMarker();        } } );
    getRTPopupMenu().add(mi = new JMenuItem("Add Marker (Timeframe Start)"));    mi.setEnabled(false);
    getRTPopupMenu().add(mi = new JMenuItem("Add Marker (Timeframe End)"));      mi.setEnabled(false);
    getRTPopupMenu().add(mi = new JMenuItem("Add Interval"));                    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { addIntervalMarker();    } } );
    getRTPopupMenu().add(mi = new JMenuItem("Add Interval (Entire Timeframe)")); mi.setEnabled(false);
    getRTPopupMenu().add(mi = new JMenuItem("Clear Markers"));                   mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { clearMarkers();         } } );

    // Render Options
    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(log_cbmi     = new JCheckBoxMenuItem("Log Scale"));
    getRTPopupMenu().add(fix_cbmi     = new JCheckBoxMenuItem("Fix Timeframe"));
    getRTPopupMenu().add(agg_cbmi     = new JCheckBoxMenuItem("Aggregate Data"));
    getRTPopupMenu().add(markers_cbmi = new JCheckBoxMenuItem("Time Markers", true));

    // Cut and Paste Options
    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(mi = new JMenuItem("Copy Timestamp"));                        mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { copyTimestamp();        } } );
    getRTPopupMenu().add(mi = new JMenuItem("Copy Interval Times"));                   mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { copyInterval();         } } );
    getRTPopupMenu().add(mi = new JMenuItem("Mark Times in Clipboard (Destructive)")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { markTimesInClipboard(); } } );
    getRTPopupMenu().add(mi = new JMenuItem("Add Times In Clipboard"));                mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { addTimesInClipboard();  } } );

    // Expand/Include Options
    getRTPopupMenu().addSeparator();
    JMenu menu = new JMenu("Expansion");
    getRTPopupMenu().add(menu);
      menu.add(mi = new JMenuItem("Include All w/in Timeframe"));    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { expandInterval(0L); } } );
      menu.add(mi = new JMenuItem("Expand Interval By 1 Minute"));   mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { expandInterval(1000L * 60L); } } );
      menu.add(mi = new JMenuItem("Expand Interval By 5 Minutes"));  mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { expandInterval(1000L * 60L * 5L); } } );
      menu.add(mi = new JMenuItem("Expand Interval By 30 Minutes")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { expandInterval(1000L * 60L * 30L); } } );
      menu.add(mi = new JMenuItem("Expand Interval By 1 Hour"));     mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { expandInterval(1000L * 60L * 60L); } } );
      menu.add(mi = new JMenuItem("Expand Interval By 8 Hours"));    mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { expandInterval(1000L * 60L * 60L * 8L); } } );
      menu.add(mi = new JMenuItem("Expand Interval By 1 Day"));      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { expandInterval(1000L * 60L * 60L * 24L); } } );
      menu.add(mi = new JMenuItem("Expand Interval By 1 Week"));     mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { expandInterval(1000L * 60L * 60L * 24L * 7L); } } );

      menu.addSeparator();
      menu.add(mi = new JMenuItem("Expand Visible By 1 Minute"));   mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { expandVisible(1000L * 60L); } } );
      menu.add(mi = new JMenuItem("Expand Visible By 5 Minutes"));  mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { expandVisible(1000L * 60L * 5L); } } );
      menu.add(mi = new JMenuItem("Expand Visible By 30 Minutes")); mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { expandVisible(1000L * 60L * 30L); } } );
      menu.add(mi = new JMenuItem("Expand Visible By 1 Hour"));     mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { expandVisible(1000L * 60L * 60L); } } );

    // Listeners
    // defaultListener(type_cb);
    defaultListener(count_cb);
    defaultListener(mapper_cb);

    defaultListener(log_cbmi);
    defaultListener(fix_cbmi);
    defaultListener(agg_cbmi);
    defaultListener(markers_cbmi);

    if (include_panel) add("South", panel);

    updateBys();
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "temporal"; }

  /**
   * Return if the scale should be logarithmic
   *
   *@return true if log scale is to be used
   */
  public boolean logScale()               { return log_cbmi.isSelected();   }

  /**
   * Set the logscale flag
   *
   *@param b true if log scale is to be used
   */
  public void    logScale(boolean b)      {        log_cbmi.setSelected(b); }

  /**
   * Return true if the rendering should use a fixed timeframe
   *
   *@return true for fixed timeframe
   */
  public boolean fixedTime()              { return fix_cbmi.isSelected();   }

  /**
   * Set the fixed timeframe flag
   *
   *@param b true if fixed timeframe is to be used
   */
  public void    fixedTime(boolean b)     {        fix_cbmi.setSelected(b);   }

  /**
   * Return true if time markers are to be displayed
   *
   *@return true for time marker display
   */
  public boolean timeMarkers()            { return markers_cbmi.isSelected();   }

  /**
   * Set the time marker flag
   *
   *@param b true if time markers are to be displayed
   */
  public void    timeMarkers(boolean b)   {        markers_cbmi.setSelected(b);   }

  /**
   * Return a string representing the chart type to render.
   *
   *@return chart type string
   */
  public String  chartType()              { return TYPE_BARCHART; } // (String) type_cb.getSelectedItem(); }

  /**
   * Set the chart type.  See the constants for choices.
   *
   *@param s chart type string
   */
  public void    chartType(String s)      { /*                type_cb.setSelectedItem(s); */ }

  /**
   * Return the mapping function for the time-based function.
   *
   *@return time mapper string
   */
  public String  mapper()                 { return (String) mapper_cb.getSelectedItem(); }

  /**
   * Set the mapping function for the time-based function.  See the constants for choices.
   *
   *@param s time mapper string
   */
  public void    mapper(String s)         {                 mapper_cb.setSelectedItem(s); }

  /**
   * Return the method to count records.
   *
   *@return field used for counting
   */
  public String  countBy()                { return (String) count_cb.getSelectedItem(); }

  /**
   * Set the method for counting records.  Includes a default value so that the global
   * count by method will be used.
   *
   *@param s field to use for counting
   */
  public void    countBy(String s)        {                 count_cb.setSelectedItem(s); }

  /**
   * Return true if the rendering should aggregate counts as time increases.
   *
   *@return true for aggregate rendering
   */
  public boolean aggregate()              { return agg_cbmi.isSelected(); }

  /**
   * Set the aggregate rendering flag.
   *
   *@param b true if aggregate rendering is to be used
   */
  public void    aggregate(boolean b)     {        agg_cbmi.setSelected(b); }
  
  /**
   * Get the configuration of this component as a string.  Intended for
   * bookmarking and recalling GUI configuration.  Need to include the 
   * fixed times for the fixed time flag...
   *
   *@return string representing GUI configuration
   */
  @Override
  public String getConfig() {
    return "RTTimePanel"                               + BundlesDT.DELIM +
           "charttype="  + Utils.encToURL(chartType()) + BundlesDT.DELIM +
	   "count="      + Utils.encToURL(countBy())   + BundlesDT.DELIM +
	   "mapper="     + Utils.encToURL(mapper())    + BundlesDT.DELIM +
	   "fixed="      + fixedTime()                 + BundlesDT.DELIM +
	   "log="        + logScale()                  + BundlesDT.DELIM +
	   "markers="    + timeMarkers()               + BundlesDT.DELIM +
           "aggregate="  + aggregate();
  }

  /**
   * Set the configuration of the panel.  Could be used for recalling
   * views.
   *
   *@param str configuration string
   */
  public void   setConfig(String str) {
    StringTokenizer st = new StringTokenizer(str,BundlesDT.DELIM);
    if (st.nextToken().equals("RTTimePanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not An RTTimePanel");
    while (st.hasMoreTokens()) {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
      String type = st2.nextToken(), value = st2.hasMoreTokens() ? st2.nextToken() : "";
      if      (type.equals("charttype")) { } // type_cb.setSelectedItem(Utils.decFmURL(value)); // Still want it parsed... just not have an effect
      else if (type.equals("count"))     count_cb.setSelectedItem(Utils.decFmURL(value));
      else if (type.equals("mapper"))    mapper_cb.setSelectedItem(Utils.decFmURL(value));
      else if (type.equals("fixed"))     fix_cbmi.setSelected(value.toLowerCase().equals("true"));
      else if (type.equals("log"))       log_cbmi.setSelected(value.toLowerCase().equals("true"));
      else if (type.equals("markers"))   markers_cbmi.setSelected(value.toLowerCase().equals("true"));
      else if (type.equals("aggregate")) agg_cbmi.setSelected(value.toLowerCase().equals("true"));
      else throw new RuntimeException("Do Not Understand Type Value \"" + type + "\" = \"" + value + "\"");
    }
  }

  /**
   * Update the dropdown box for the global application fields.
   */
  @Override
  public void updateBys() {
    String strs[]; Object sel = count_cb.getSelectedItem();
    count_cb.removeAllItems();
    strs = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals());
    count_cb.addItem(BundlesDT.COUNT_BY_DEFAULT);
    count_cb.addItem(BundlesDT.COUNT_BY_BUNS);
    for (int i=0;i<strs.length;i++) count_cb.addItem(strs[i]);
    if (sel == null) count_cb.setSelectedIndex(0); else count_cb.setSelectedItem(sel);
  }

  /**
   * Copy the timestamp of the last mouse drag position to the clipboard.
   */
  protected void copyTimestamp() {
    RTTimeComponent comp = (RTTimeComponent) getRTComponent();
    long timestamp = comp.getTimeAt(comp.getDragX1(),comp.getDragY1());
    if (timestamp == 0L) return;
    Clipboard  clipboard = getToolkit().getSystemClipboard();
    StringSelection selection = new StringSelection(Utils.humanReadableDate(timestamp));
    clipboard.setContents(selection, null);
  }

  /**
   * Copy the interval timestamps of the mouse drag to the clipboard.
   */
  protected void copyInterval() {
    RTTimeComponent comp = (RTTimeComponent) getRTComponent();
    long time_at_0 = comp.getTimeAt(comp.getDragX0(),comp.getDragY0()),
         time_at_1 = comp.getTimeAt(comp.getDragX1(),comp.getDragY1());
    if (time_at_0 == 0L || time_at_1 == 0L) return;
    if (time_at_0 > time_at_1) { long tmp = time_at_0; time_at_0 = time_at_1; time_at_1 = tmp; }
    Clipboard  clipboard = getToolkit().getSystemClipboard();
    StringSelection selection = new StringSelection(Utils.humanReadableDate(time_at_0) + " through " + Utils.humanReadableDate(time_at_1));
    clipboard.setContents(selection, null);
  }

  /**
   * Add a timemarker to the application for the last mouse drag position.
   */
  protected void addTimeMarker() {
    RTTimeComponent.RenderContext myrc = (RTTimeComponent.RenderContext) (getRTComponent().rc); if (myrc == null || myrc.mapper.linear() == false) { getRTParent().reportToUser("Markers Cannot Be Added To Non-Linear Timeframes"); return; }
    RTTimeComponent comp = (RTTimeComponent) getRTComponent();
    long timestamp = comp.getTimeAt(comp.getDragX1(),comp.getDragY1());
    if (timestamp == 0L) return;
    getRTParent().addTimeMarker(Utils.shortDateStr(timestamp), timestamp);
  }

  /**
   * Add a time interval based on the mouse drag positions.
   */
  protected void addIntervalMarker() {
    RTTimeComponent.RenderContext myrc = (RTTimeComponent.RenderContext) (getRTComponent().rc); if (myrc == null || myrc.mapper.linear() == false) { getRTParent().reportToUser("Markers Cannot Be Added To Non-Linear Timeframes"); return; }
    RTTimeComponent comp = (RTTimeComponent) getRTComponent();
    long time_at_0 = comp.getTimeAt(comp.getDragX0(),comp.getDragY0()),
         time_at_1 = comp.getTimeAt(comp.getDragX1(),comp.getDragY1());
    if (time_at_0 == 0L || time_at_1 == 0L) return;
    if (time_at_0 > time_at_1) { long tmp = time_at_0; time_at_0 = time_at_1; time_at_1 = tmp; }
    getRTParent().addTimeMarker(Utils.shortDateStr(time_at_0) + " (" + Utils.humanReadableDuration(time_at_1 - time_at_0) + ")", time_at_0, time_at_1);
  }

  /**
   * Clear the time markers from the application.  Should probably
   * have a user confirmation dialog to ensure accidents don't happen.
   */
  protected void clearMarkers() { getRTParent().clearTimeMarkers(); }

  /**
   * Add timemarkers from the clipboard to the application.  Clear
   * the existing ones first.  Should probably have a user confirmation
   * dialog for safety.
   */
  protected void markTimesInClipboard() { addTimes(true);  }

  /**
   * Add timemarkers from the clipboard to the application.
   */
  protected void addTimesInClipboard()  { addTimes(false); }

  /**
   * Generic method for adding time markers.
   *
   *@param clear_first indicates whether the existing markers should be cleared
   */
  protected void addTimes(boolean clear_first) {
    String str = Utils.getClipboardText(this); if (str == null) return;
    List<SubText> al = EntityExtractor.list(str);
    if (al != null && al.size() > 0) {
      if (clear_first) getRTParent().clearTimeMarkers();
      addTimes(al);
    }
  }

  /**
   * Add time markers based on subtext extractions.
   *
   *@param al list of subtexts to add as time markers
   */
  private void addTimes(List<SubText> al) {
    Iterator<SubText> it = al.iterator();
    while (it.hasNext()) {
      SubText sub = it.next();
      if      (sub instanceof Interval)  { Interval  interval  = (Interval)  sub; 
                                           getRTParent().addTimeMarker("Clipboard", interval.getMinTimeStamp(), interval.getMaxTimeStamp()); }
      else if (sub instanceof TimeStamp) { TimeStamp timestamp = (TimeStamp) sub; 
                                           getRTParent().addTimeMarker("Clipboard", timestamp.getTimeStamp()); }
    }
  }

  /**
   * Expands the ends of the first heard, last heard by the specified amount.  
   * All of the bundles are compared for adding.
   *
   *@param in_ms how much to expand the interval in milliseconds
   */
  protected void expandInterval(long in_ms) {
    Bundles bundles = getRenderBundles(); long ts0 = bundles.ts0() - in_ms, ts1 = bundles.ts1() + in_ms, ts1dur = bundles.ts1dur() + in_ms;
    Set<Bundle> set = new HashSet<Bundle>(); set.addAll(bundles.bundleSet());
    Iterator<Bundle> it  = getRTParent().getRootBundles().bundleIterator();
    while (it.hasNext()) {
      Bundle bundle = it.next();
      if        (bundle.hasDuration()) {
        if ((bundle.ts0() >= ts0 && bundle.ts0() <= ts1) ||
	    (bundle.ts1() >= ts1 && bundle.ts1() <= ts1) ||
	    (bundle.ts0() <  ts0 && bundle.ts1() >  ts1)) set.add(bundle);
      } else if (bundle.hasTime()) {
        if (bundle.ts0() >= ts0 && bundle.ts0() <= ts1) set.add(bundle);
      }
    }
    set.addAll(getRTComponent().getNoMappingSet());
    Bundles new_bundles = getRTParent().getRootBundles().subset(set);
    new_bundles.setTimestamps(ts0, ts1, ts1dur);
    getRTParent().push(new_bundles);
  }

  /**
   * Expand the current dataset to include to bundles that exist in the filtered
   * time frame.  A parameter can be specified to also expand the timeframe.
   *
   *@param in_ms amount to expand the timeframe in milliseconds.  Could be zero
   *             if the  user just wants to include the bundles in the timeframe
   *             without expansion.
   */
  protected void expandVisible(long in_ms) {
    // Get the prelims
    Bundles bundles = getRenderBundles(); long ts0 = bundles.ts0() - in_ms, ts1 = bundles.ts1() + in_ms, ts1dur = bundles.ts1dur() + in_ms;
    Set<Bundle> set = new HashSet<Bundle>(); set.addAll(bundles.bundleSet());

    // Create an array and mark it up with timeframes to include
    BundlesTimeExpander bte = new BundlesTimeExpander(set, ts0, ts1, in_ms);

    // Go through the root bundles, adding them
    Iterator<Bundle> it = getRTParent().getRootBundles().bundleIterator();
    while (it.hasNext()) {
      Bundle bundle = it.next();
      if (bte.shouldInclude(bundle)) set.add(bundle);
    }

    // Add non mapped and push it to the top of the stack
    set.addAll(getRTComponent().getNoMappingSet());
    Bundles new_bundles = getRTParent().getRootBundles().subset(set);
    new_bundles.setTimestamps(ts0, ts1, ts1dur);
    getRTParent().push(new_bundles);
  }

  /**
   * GUI component within the panel to handle painting the rendering
   * and interaction.
   */
  public class RTTimeComponent extends RTComponent implements KeyListener {
    /**
     * 
     */
    private static final long serialVersionUID = -5853965086541665730L;

    /**
     * Construct the component by adding the necessary listeners.
     */
    public RTTimeComponent() { super(); MyMouseListener mml; addMouseListener(mml = new MyMouseListener()); addMouseMotionListener(mml); }

    /**
     * Key pressed interface for KeyListener.  Just passes to the parent for handling...
     *
     *@param ke key event
     */
    public void keyPressed(KeyEvent ke) { super.keyPressed(ke); }

    /**
     * Key released interface. Just passes to the parent for handling...
     *
     *@param ke key event
     */
    public void keyReleased(KeyEvent ke) { super.keyReleased(ke); }

    /**
     * Key typed interface.  Passes control to the parent... and then sees if there are
     * any component specific actions.
     *
     *@param ke key event
     *
     *@param ke key event
     */
    public void keyTyped(KeyEvent ke) {
      super.keyTyped(ke);
      if        (ke.getKeyChar() == 's' || ke.getKeyChar() == 'S') {
        String description = (String) JOptionPane.showInputDialog(this, "Sparkline Description", "Add Sparkline", JOptionPane.PLAIN_MESSAGE, null, null, "bundles");
	if (description != null && description.length() > 0) {
          RTSparkLines spark_lines = RTSparkLines.getSingleton(getRTParent());
	  spark_lines.addSparkLine(getRenderBundles(), description, !logScale(), countBy());
        }
      } else if (ke.getKeyChar() == ' ') {
        logScale(!logScale());
      }
    }

    /**
     * Copy either the dynamic time marker to the clipboard or a
     * screenshot of the current view.
     *
     *@param shft shift key down - causes screenshot to be copied
     *@param alt  alt key down
     */
    @Override
    public void copyToClipboard    (boolean shft, boolean alt) {
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc == null) return;
      if      (shft == false && alt  == false) {
        long ts = getRTParent().getDynamicTimeMarker();
	if (ts > 0) Utils.copyToClipboard(Utils.exactDate(ts));
      }
      else if (shft == true  && myrc != null)  Utils.copyToClipboard(myrc.getBase());
    }

    /**
     * Middle drag button operation in effect.  Middle button
     * used to quickly add timemarkers.
     */
    boolean mid_drag = false; 

    /**
     * Beginning location of middle drag (add timemarker) operation
     */
    long    mid_ts0, 

    /**
     * Ending location of middle drag (add timemaker) operation
     */
            mid_ts1;

    /**
     * Class to handle mouse events for the time-based component
     */
    class MyMouseListener implements MouseListener, MouseMotionListener {
      public void mouseMoved   (MouseEvent me) {
        getRTParent().setDynamicTimeMarker(getTimeAt(me.getX(),me.getY()), getRTPanel());
      }
      public void mouseEntered (MouseEvent me) { mid_drag = false; }
      public void mouseExited  (MouseEvent me) { mid_drag = false; }
      public void mousePressed (MouseEvent me) { 
        if (me.getButton() == MouseEvent.BUTTON2) {
          mid_drag = true;
	  mid_ts0  = mid_ts1 = getTimeAt(me.getX(),me.getY());
	  if (mid_ts0 != 0L) repaint(); else mid_drag = false;
        }
      }
      public void mouseReleased(MouseEvent me) { 
        if (me.getButton() == MouseEvent.BUTTON2 && mid_ts0 != 0L) {
          mid_drag = false;
	  if (mid_ts0 != mid_ts1) {
            getRTParent().addTimeMarker(Utils.shortDateStr(mid_ts0) + " (" + Utils.humanReadableDuration(mid_ts1 - mid_ts0) + ")", mid_ts0, mid_ts1);
          }
        }
      }
      public void mouseDragged (MouseEvent me) { 
        if (mid_drag && mid_ts0 != 0L) {
          mid_ts1 = getTimeAt(me.getX(),me.getY());
	  if (mid_ts1 == 0L) mid_drag = false;
          repaint();
        }
      }
      public void mouseClicked (MouseEvent me) { 
        if (me.getButton() == MouseEvent.BUTTON2) {
	  mid_ts0 = getTimeAt(me.getX(),me.getY());
	  if (mid_ts0 != 0L) { getRTParent().addTimeMarker(Utils.shortDateStr(mid_ts0), mid_ts0); }
        }
      }
    }

    /**
     * Get the time at the specified screen coordinates.
     *
     *@param  sx x coordinate on the screen
     *@param  sy y coordinate on the screen
     *
     *@return    time in milliseconds of the coordinate
     */
    @Override
    public long    getTimeAt(int sx, int sy) {
      RenderContext myrc = (RenderContext) rc; if (myrc == null || myrc.mapper.linear() == false) return 0L;
      // Check for out-of-bounds
      if      (sx < myrc.graph_x_ins)                return myrc.ts0;
      else if (sx > myrc.graph_x_ins + myrc.graph_w) return myrc.ts1;
      else                                           return myrc.ts0 + ((myrc.ts1 - myrc.ts0)*(sx - myrc.graph_x_ins))/myrc.graph_w;
    }

    /**
     * Paint the component.  Draw te interaction for the middle button.
     *
     *@param g graphics primitive
     */
    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g); Graphics2D g2d = (Graphics2D) g;
      int mx = getMouseX(), my = getMouseY(); boolean m_in = mouseIn();  RenderContext myrc = (RenderContext) rc;
      if (m_in && myrc != null && mx >= myrc.graph_x_ins && mx <= myrc.graph_x_ins + myrc.graph_w) {
        String str = myrc.labelAt(mx); getRTParent().setEntityUnderMouse(str);
	clearStr((Graphics2D) g, str, mx - Utils.txtW((Graphics2D) g, str)/2, my, RTColorManager.getColor("annotate", "labelfg"), RTColorManager.getColor("annotate", "labelbg"));
	g2d.setColor(RTColorManager.getColor("annotate", "cursor"));
	g.drawLine(mx, myrc.graph_y_ins, mx, myrc.graph_y_ins + myrc.graph_h);
        if (mid_drag) {
	  int sx0 = myrc.mapper.mapTS(mid_ts0), 
	      sx1 = myrc.mapper.mapTS(mid_ts1);
          g2d.setColor(RTColorManager.getColor("annotate", "region"));
	  Composite orig_comp = g2d.getComposite(); g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
	  g2d.fillRect((sx0 < sx1) ? sx0 : sx1, myrc.graph_y_ins, (int) Math.abs(sx1 - sx0), myrc.graph_h);
	  g2d.setComposite(orig_comp);
	}
      } else if (m_in) { getRTParent().setEntityUnderMouse(null); }

      // Add the excerpts
      Map<String,Set<SubText>> excerpt_map = getRTParent().getExcerptMap();
      if (myrc != null && myrc.mapper.linear() && excerpt_map != null && excerpt_map.keySet().size() > 0) {
        Area fill_state = new Area(); int txt_h = Utils.txtH(g2d, "0");
	Rectangle2D bounds = new Rectangle2D.Double(0,0,getWidth(),getHeight());

        Iterator<String> it_ent = excerpt_map.keySet().iterator();
	while (it_ent.hasNext()) {
	  String entity = it_ent.next();
	  Iterator<SubText> it_sub = excerpt_map.get(entity).iterator();
	  while (it_sub.hasNext()) {
            SubText subtext = it_sub.next(); int y_exc = 4*txt_h + (subtext.toString().hashCode())%30;
	    if        (subtext instanceof Interval)  {
	      int x0 = myrc.mapper.mapTS(((Interval)  subtext).getMinTimeStamp()),
	          x1 = myrc.mapper.mapTS(((Interval)  subtext).getMaxTimeStamp()); Set<SubText> set = new HashSet<SubText>(); set.add(subtext);
              int midx = (x0 + x1) / 2;
	      g2d.setColor(RTColorManager.getColor("annotate", "cursor"));
	      g2d.drawLine(x0, y_exc, x1,   y_exc); g2d.drawLine(x0, y_exc, x0+3, y_exc+3); g2d.drawLine(x0, y_exc, x0+3, y_exc-3);
	                                            g2d.drawLine(x1, y_exc, x1-3, y_exc+3); g2d.drawLine(x1, y_exc, x1-3, y_exc-3);
              if      (midx >= 0 && midx <= getWidth()) { SubText.renderContextHints(g2d, set, midx, y_exc, bounds, fill_state); }
	      else if (x0   >= 0 && x0   <= getWidth()) { SubText.renderContextHints(g2d, set, x0,   y_exc, bounds, fill_state); }
	      else if (x1   >= 0 && x1   <= getWidth()) { SubText.renderContextHints(g2d, set, x1,   y_exc, bounds, fill_state); }
	    } else if (subtext instanceof TimeStamp) {
	      int x0 = myrc.mapper.mapTS(((TimeStamp) subtext).getTimeStamp());    Set<SubText> set = new HashSet<SubText>(); set.add(subtext);
	      if (x0 >= 0 && x0 <= getWidth()) { SubText.renderContextHints(g2d, set, x0, y_exc, bounds, fill_state); }
	    }
	  }
	}
      }
    }

    /**
     * Return the shapes associated with the specified bundles in the current view.
     *
     *@param  bundles specific bundles to match to shapes
     *
     *@return shapes that correspond to the bundles
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
            for (int i=0;i<bins.length;i++) set.add(myrc.bin_to_shape.get(bins[i]));
          }
        }
      }
      return set;
    }
    
    /**
     * Return the shapes that correspond to the specific bundle
     *
     *@param  bundle bundle to match for shapes
     *
     *@return corresponding shapes
     */
    @Override
    public Set<Shape>  shapes(Bundle bundle) { 
      Set<Bundle> set = new HashSet<Bundle>(); set.add(bundle); 
      return shapes(set); 
    }

    /**
     * For a specific shape, return the bundles that make up that shape.  Note
     * that the specified shape cannot be generic -- it must be a shape object
     * that was created by this component.
     *
     *@param  shape shape record created by this component to match for bundles
     *
     *@return bundles that compose the shape
     */
    @Override
    public Set<Bundle> shapeBundles(Shape shape) {
      RenderContext myrc = (RenderContext) rc;
      if (myrc != null) {
        return myrc.counter_context.getBundles(myrc.shape_to_bin.get(shape));
      } else return new HashSet<Bundle>();
    }

    /**
     * Return all of the shapes in the current rendering.
     *
     *@return set of all shapes in current rendering
     */
    @Override
    public Set<Shape> allShapes() {
      RenderContext myrc = (RenderContext) rc;
      if (myrc != null) { return myrc.allShapes(); } else return new HashSet<Shape>();
    }

    /**
     * For a general shape, find all of the overlapping shapes in the current
     * rendering.
     *
     *@param  shape_to_check general shape to match against
     *
     *@return scene-specific shapes overlapping the shape_to_check
     */
    @Override
    public Set<Shape> overlappingShapes(Shape shape_to_check) {
      Set<Shape> set = new HashSet<Shape>();
      RenderContext myrc = (RenderContext) rc;
      if (myrc != null) {
        Iterator<Shape> it = allShapes().iterator();
	while (it.hasNext()) {
          Shape shape = it.next();
          if (Utils.genericIntersects(shape, shape_to_check)) set.add(shape);
	}
      } 
      return set;
    }

    /**
     * Render the current scene with the visible data and panel GUI
     * configuration settings.  Use a unique render id to ensure only
     * the most up-to-date rendering exists.
     *
     *@param  render_id unique render id to ensure only one renderer is running
     *
     *@return render context with current data and settings
     */
    @Override
    public RTRenderContext render(short render_id) {
      clearNoMappingSet();
      if (isVisible() == false) { repaint(); return null; }
      Bundles bs = getRenderBundles();
      String count_by = getRTParent().getCountBy(), color_by = getRTParent().getColorBy();
      String count_by_setting = countBy();
      if (count_by_setting != null && count_by_setting.equals(BundlesDT.COUNT_BY_DEFAULT) == false) count_by = countBy();

      if (bs != null && count_by != null) {
        RenderContext myrc = new RenderContext(render_id, bs, count_by, color_by, chartType(), mapper(), logScale(), fixedTime(), aggregate(), timeMarkers(), getWidth(), getHeight(), (RenderContext) rc);
        return myrc;
      } else return null;
    }

    /**
     * Return the shape associated with brushing at the first level
     *
     *@param  x x coordinate for the shape
     *@param  y y coordinate for the shape
     *
     *@return shape representing information right under mouse.  In this
     *        component's case, this shape is a rectangle the complete height
     *        of the view.
     */
    @Override
    public Shape getZeroOrderShape(int x, int y) {
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return null;
      return new Rectangle2D.Double(x,                   0, 1,                 myrc.h);
    }

    /**
     * Return the shape associated with brushing near the mouse
     *
     *@param  x x coordinate for the shape
     *@param  y y coordinate for the shape
     *
     *@return shape representing information near the mouse.
     */
    @Override
    public Shape getFirstOrderShape(int x, int y) {
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return null;
      return new Rectangle2D.Double(x - 1*myrc.getInc(), 0, 2*myrc.getInc()+1, myrc.h);
    }

    /**
     * Return the shape associated with brushing a little further from the mouse
     *
     *@param  x x coordinate for the shape
     *@param  y y coordinate for the shape
     *
     *@return shape representing information a little further from the mouse.
     */
    @Override
    public Shape getSecondOrderShape(int x, int y) {
      RenderContext myrc = (RenderContext) rc; if (myrc == null) return null;
      return new Rectangle2D.Double(x - 2*myrc.getInc(), 0, 4*myrc.getInc()+1, myrc.h);
    }

    /**
     * Class representing the current rendering.  Responsible for taking the
     * visible dataset and GUI settings and renderig the appropriate scene.
     * Also maintains information on bundle to shapes for filtering and
     * brushing.
     */
    class RenderContext extends RTRenderContext {
      /**
       * Dataset to render
       */
      Bundles bs; 
      /**
       * Width of rendering in pixels
       */
      int     w, 
      /**
       * Height of rendering in pixels
       */
              h; 
      /**
       * Field to count by (height of bars in chart)
       */
      String  count_by, 
      /**
       * Field to color the bars by
       */
              color_by; 
      /**
       * Types of chart.  See string contants for options
       */
      String  chart_type, 
      /**
       * Mapping of time values to x-coordinates.  See string contants for options
       */
              mapper_str; 
      /**
       * Use a logarithmic scale
       */
      boolean log_scale,
      /**
       * Fix the timeframe so that it doesn't vary with the data
       */
              fixed, 
      /**
       * Aggregate the counts across time so that it is cumulative
       */
	      aggregate, 
      /**
       * Draw the time markers
       */
	      draw_markers = true;
      /**
       * Initial timeframe for rendering (necessary for remembering the fixed timeframe)
       */
      long    ts0, 
      /**
       * Ending timeframe for renderin (necessary for remembering the fixed timeframe)
       */
              ts1;
      /**
       * left-side x inset for actual graph (needs space for labels)
       */
      int     graph_x_ins, 
      /**
       * Right-side x inset for actual graph
       */
              graph_x_rgt, 
      /**
       * Actual graph width
       */
	      graph_w,
      /**
       * Top y inset for actual graph (needs space for labels and time markers)
       */
              graph_y_ins, 
      /**
       * Bottom y inset for actual graph (needs space for more labels)
       */
	      graph_y_bot, 
      /**
       * Actual graph height
       */
	      graph_h;
      /**
       * Counter context for accumulating bundles for each bar
       */
      BundlesCounterContext    counter_context;
      /**
       * Map to correspond bins (bars in this case) to the shape record
       */
      Map<String,Shape>    bin_to_shape   = new HashMap<String,Shape>();
      /**
       * Map to correspond shape to a bin
       */
      Map<Shape,String>    shape_to_bin   = new HashMap<Shape,String>();
      /**
       * All of the shapes in the dataset
       */
      Set<Shape>           all_shapes     = new HashSet<Shape>();
      /**
       * Map to convert a bundle to all of it corresponding bins
       */
      Map<Bundle,String[]> bundle_to_bins = new HashMap<Bundle,String[]>();
      /**
       * Mapper for the time function (continuous, hour-bins, periodic, etc.)
       */
      Mapper                   mapper         = null;

      /**
       * Return the specific time label from the mapper at a screen coordinate.
       *
       *@param  x screen x coordinate
       *
       *@return string for label
       */
      public String  labelAt(int x)    { return mapper.labelAt(x); }

      /**
       * Return the increment amount for the mapper.  This should be the
       * x spacing between bins.
       *
       *@return x spacing between bars
       */
      public int     getInc()          { return mapper.getInc();   }

      /**
       * Return the width of the rendering in pixels.
       *
       *@return width of rendering
       */
      @Override
      public int     getRCWidth()      { return w; }

      /**
       * Return the height of the rendering in pixels.
       *
       *@return height of rendering
       */
      @Override
      public int     getRCHeight()     { return h; }

      /**
       * Return if the component has entity shapes.  Appears to
       * be incorrect.  Should return false...  may be set this
       * way to show subtext highlights...  if so, the method should
       * be renamed to something more appropriate.
       *
       *@return true
       */
      @Override
      public boolean hasEntityShapes() { return true; }

      /**
       * Based on extracted subtexts, return the related shapes from
       * the rendering that correspond to the subtext strings.  In this
       * case, date strings.
       *
       *@param  subs extracted subtexts
       *
       *@return set of shapes that correspond to the subtext date strings
       */
      @Override
      public Set<Shape> entityShapes(Set<SubText> subs) {
        Set<Shape> shapes = new HashSet<Shape>();
        if (mapper instanceof PeriodicityMapper == false) {
	  Iterator<SubText> it = subs.iterator();
	  while (it.hasNext()) {
	    SubText subtext = it.next();
	    if        (subtext instanceof TimeStamp) {
	      int x0 = mapper.mapTS(((TimeStamp) subtext).getTimeStamp());
	      shapes.add(new Line2D.Double(x0, graph_y_ins, x0, graph_y_ins + graph_h));
	    } else if (subtext instanceof Interval)  {
	      int x0 = mapper.mapTS(((Interval)  subtext).getMinTimeStamp()),
	          x1 = mapper.mapTS(((Interval)  subtext).getMaxTimeStamp());
	      // both timestamps
	      shapes.add(new Line2D.Double(x0, graph_y_ins, x0, graph_y_ins + graph_h));
	      shapes.add(new Line2D.Double(x1, graph_y_ins, x1, graph_y_ins + graph_h));
	      // bar connecting two
	      int y_mid = subtext.hashCode(); if (y_mid < 0) y_mid *= -1; 
	      if (graph_h < 20) y_mid = graph_y_ins + graph_h/2;
	      else              y_mid = graph_y_ins + 10 +  y_mid%(graph_h-20);
	      shapes.add(new Line2D.Double(x0, y_mid,       x1,   y_mid));
	      // Arrows at the end of the bar
	      shapes.add(new Line2D.Double(x0, y_mid,       x0+5, y_mid-5));
	      shapes.add(new Line2D.Double(x0, y_mid,       x0+5, y_mid+5));
	      shapes.add(new Line2D.Double(x1, y_mid,       x1-5, y_mid-5));
	      shapes.add(new Line2D.Double(x1, y_mid,       x1-5, y_mid+5));
	    }
	  }
        }
        return shapes;
      }

      /**
       * The mapping function for converting timestamps into x coordinates
       * for the rendering.  The abstract version implements most of the
       * functionality for the continuous mapper.
       */
      abstract class Mapper { 
        /**
	 * Map a timestamp to an x coordinate
	 *
	 *@param  ts long timestamp value
	 *
	 *@return screen x coordinate
	 */
        public int mapTS(long ts) { return (int) (graph_x_ins + ((ts - ts0) * graph_w)/(ts1 - ts0)); }

        /**
	 * Create a string representing this bundles contribution
	 * to the overall chart.
	 *
	 *@param  bundle bundle to map
	 *
	 *@return string representing contribution --- may be either
	 *        a single integer "10" or a range "20,30".
	 */
        public abstract String  map(Bundle bundle); 

        /**
	 * Return the next index from the specified index.
	 *
	 *@param  index base index
	 *@return next index
	 */
        public abstract String  nextIndex(String index); 

        /**
	 * Return the previous index from the specified index.
	 *
	 *@param  index base index
	 *@return previous index
	 */
        public abstract String  prevIndex(String index);

        /**
	 * Return the increment value to move between x coordinates.
	 *
	 *@return x increment value
	 */
	public          int     getInc()                  { return 1; }

        /**
	 * Draw gridlines (and legends?) based on the mapper.
	 *
	 *@param g2d graphic primitive
	 */
        public          void    gridLines(Graphics2D g2d) { }

        /**
	 * Detemines if the mapper is periodic or linear.
	 *
	 *@return true if the mapper is linear, false if the mapper is periodic
	 */
	public          boolean linear()                  { return true; }

        /**
	 * Return the appropriate human readable label at the specified x coordinate.
	 *
	 *@param  x x coordinate
	 *@return human readable label corresponding to specified coordinate
	 */
	public          String  labelAt(int x)  { long ts = ts0 + ((ts1 - ts0) * (x - graph_x_ins))/graph_w; 
			                          return Utils.humanReadableDate(ts);  }

        /**
	 * Return the first label for the timeframe.  Used for the legend.
	 *
	 *@return human readable label
	 */
        public          String  begLabel()      { return Utils.humanReadableDate(ts0); }

        /**
	 * Return the end label for the timeframe.  Used for the legend.
	 *
	 *@return human readable label
	 */
        public          String  endLabel()      { return Utils.humanReadableDate(ts1); }

        /**
	 * Return the mid label for the timeframe.  Can be an empty string.
	 *
	 *@return human readable label in the middle of the graph
	 */
        public          String  midLabel()      { return "<= " + Utils.humanReadableDuration(ts1 - ts0) + " =>"; } }

      /**
       * Extends the abstract mapper to include the duration of the event.  This is tricky because
       * the contributing counts need to be subdivided by the number of bars that the event contributes
       * towards.
       */
      class DurationMapper extends Mapper {
	/**
	 * REFACTOR
	 */
        public int   mapTS(long ts) { 
          int value = (int) (graph_x_ins + ((ts - ts0) * graph_w)/(ts1 - ts0)); 
          return value;
        }
        /**
	 * Return either a single integer (as a string) for timestamp bundles or a
	 * comma-separated pair of integers (as a string) for a duration bundle.
	 *
	 *@param  bundle bundle to convert
	 *@return string representing contribution of the bundle to the graph
	 */
        public String map(Bundle bundle) {
          if        (bundle.hasDuration()) {
	    return "" + mapTS(bundle.ts0()) + "," + mapTS(bundle.ts1());
	  } else if (bundle.hasTime()) {
	    return "" + mapTS(bundle.ts0());
	  } else return null;
	}
        public String nextIndex(String index) { return "" + (Integer.parseInt(index) + 1); }
        public String prevIndex(String index) { return "" + (Integer.parseInt(index) - 1); }
      }
      /**
       * The most basic form for the mapper.  Straight forward, linear, continuous mapper.
       */
      class SimpleMapper extends Mapper {
	/**
	 * REFACTOR
	 */
        public int    mapTS(long ts) { return (int) (graph_x_ins + ((ts - ts0) * graph_w)/(ts1 - ts0)); }
        public String map(Bundle bundle) {
	  if (bundle.hasTime() || bundle.hasDuration()) {
	    return "" + mapTS(bundle.ts0());
	  } else return null;
	}
        public String nextIndex(String index) { return "" + (Integer.parseInt(index) + 1); }
        public String prevIndex(String index) { return "" + (Integer.parseInt(index) - 1); }
      }
      /**
       * Simple mapper that uses a larger bar width specified in the constructor.  Has aliasing
       * issues where the bar changes size based on subpixel placement of the bundles.
       */
      class SimpleMapperPix extends Mapper {
        /**
	 * Width of each bar
	 */
        int pix;
	/**
	 * Construct an instance with the specific bar width.
	 *
	 *@param pix bar width
	 */
        public SimpleMapperPix(int pix) { this.pix = pix; }
	public int    getInc() { return pix; }
	/**
	 * REFACTOR
	 */
        public int    mapTS(long ts) { return (int) (graph_x_ins + ((ts - ts0) * graph_w)/(ts1 - ts0)); }
        public String map(Bundle bundle) {
	  if (bundle.hasTime() || bundle.hasDuration()) {
            int x = (int) (((bundle.ts0() - ts0) * graph_w)/(ts1 - ts0));
	    return "" + (graph_x_ins + x - x%pix);
	  } else return null;
	}
        public String nextIndex(String index) { return "" + (Integer.parseInt(index) + pix); }
        public String prevIndex(String index) { return "" + (Integer.parseInt(index) - pix); }
      }
      /**
       * Mapper that wraps the timestamps based on a periodic value specified during
       * construction of the instance.  Note that this class uses a simplistic modulo
       * operator to perform the wrapping.  As such, it does not take leap seconds or
       * other anomalies into account.
       */
      class PeriodicityMapper extends Mapper {
        /**
	 * Period to use
	 */
        long period; 

	/**
	 * Consruct the periodic mapper with the specified period.
	 *
	 *@param period period in milliseconds
	 */
	public PeriodicityMapper(long period) { this.period = period; }
        public String map(Bundle bundle) {
	  if (bundle.hasTime() || bundle.hasDuration()) {
            return "" + (graph_x_ins + ((bundle.ts0() % period) * graph_w)/period);
	  } else return null;
        }
        public String nextIndex(String index) { return "" + (Integer.parseInt(index) + 1); }
        public String prevIndex(String index) { return "" + (Integer.parseInt(index) - 1); }
        public String begLabel()              { return "0"; }
        public String endLabel()              { return Utils.humanReadableDuration(period); }
        public String midLabel()              { return "Periodic"; }
        public String labelAt(int x)          { 
	  if        (period == MINUTES) { return "" + Utils.humanReadableDouble(60.0 * (x - graph_x_ins)/graph_w) + " secs";
	  } else if (period == HOURS)   { int secs = (int) (3600 * (x - graph_x_ins))/graph_w;
	                                  return "" + (secs/60) + " mins " + (secs%60) + " secs";
	  } else if (period == DAYS)    { int secs = (int) (24*60*60 * (x - graph_x_ins))/graph_w;
	                                  return "" + (secs/(60*60)) + " hours " + (secs%(60*60))/60 + " mins";
	  } else if (period == WEEKS)   { int secs = (int) (7*24*60*60 * (x - graph_x_ins))/graph_w;
	                                  int days = secs/(24*60*60); secs -= days*24*60*60;
					  int hors = secs/(60*60);    secs -= hors*60*60;
					  int mins = secs/60;
					  return days + " days " + hors + " hours " + mins + " mins";
	  } else return "Unknown Period \"" + period + "\"";
        }
	public boolean linear()                  { return false; }
        public void    gridLines(Graphics2D g2d) {
          if         (period == HOURS) { fullLine(g2d,graph_x_ins + (2*graph_w)/4);
	                                 halfLine(g2d,graph_x_ins + (1*graph_w)/4);
					 halfLine(g2d,graph_x_ins + (3*graph_w)/4);
	  } else if  (period == DAYS)  {
            for (int i=0;i<=24;i++) { if ((i%6) == 0) fullLine(g2d,graph_x_ins + (i*graph_w)/24);
	                              else            halfLine(g2d,graph_x_ins + (i*graph_w)/24); }
	  } else if  (period == WEEKS) {
            for (int i=0;i<=7;i++) fullLine(g2d,graph_x_ins + (i*graph_w)/7);
	  }
        }
      }

      /**
       * Special version of the {@link PeriodicityMapper} to re-align weeks to Sunday.  Apparently,
       * the epoch fell on a Thursday.
       */
      class WeeklyPeriodicityMapper extends PeriodicityMapper {
        public WeeklyPeriodicityMapper(long period) { super(period); }
	public String map(Bundle bundle) {
	  if (bundle.hasTime() || bundle.hasDuration()) {
            return "" + (graph_x_ins + (((bundle.ts0() + 4*DAYS) % period) * graph_w)/period);
	  } else return null;
	}
      }


      /**
       * Draw a half line from the x-axis.
       *
       *@param g2d graphics primitive
       *@param x   x coordinate for the half line
       */
      public void halfLine(Graphics2D g2d, int x) { g2d.setColor(RTColorManager.getColor("axis", "minor"));
                                                    g2d.drawLine(x, h-(h - graph_y_ins - graph_h)/2, x, graph_y_ins + graph_h); }

      /**
       * Draw a full line from the x-axis.
       *
       *@param g2d graphics primitive
       *@param x   x coordinate for the full line
       */
      public void fullLine(Graphics2D g2d, int x) { g2d.setColor(RTColorManager.getColor("axis", "minor"));
                                                    g2d.drawLine(x, h,   x, graph_y_ins + graph_h); }

      /**
       * Mapper that aggregates timestamps into a human-convenient bin (such as hours, days).
       * Unlike the periodicity mapper, this instance uses the Gregorian calendar implementation
       * to ensure that the anomalies are taken into consideration.
       */
      class TimeBinMapper extends Mapper {
        SimpleDateFormat    mysdf;
	int                 inc;
	Map<String,Integer> x_lu = new HashMap<String,Integer>();
	List<String>        xs   = new ArrayList<String>();

	/**
	 * Construct the mapper with the specified time bin.
	 *
	 *@param bin_in_ms bin size in milliseconds
	 */
        public TimeBinMapper(long bin_in_ms) {
	  // Determine the date formatter
          if      (bin_in_ms == DAYS)      mysdf = new SimpleDateFormat("yyyy.MM.dd");
	  else if (bin_in_ms == HOURS)     mysdf = new SimpleDateFormat("yyyy.MM.dd HH");
	  else if (bin_in_ms == MINUTES)   mysdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
          else if (bin_in_ms == 5*MINUTES) mysdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
	  else if (bin_in_ms == MONTHS)    mysdf = new SimpleDateFormat("yyyy.MM");
	  else throw new RuntimeException("Do Not Understand Time Bin " + bin_in_ms);
	  mysdf.setTimeZone(TimeZone.getTimeZone("GMT"));

	  // Calculate the various strings
	  List<String> time_strs = new ArrayList<String>();
	  long ts = ts0; while (ts < (ts1+bin_in_ms)) { time_strs.add(mysdf.format(new Date(ts))); ts += bin_in_ms; }

	  // Determine the number of bins;
          int inc = graph_w/time_strs.size(); if (inc < 1) inc = 1;

	  // Figure out the lookups
          System.err.println("Maybe Why TIME_BIN.Months Broken...");
	  ts = ts0; int x = graph_x_ins;
          for (int i=0;i<time_strs.size();i++) {
            x = (int) (graph_x_ins + ((ts - ts0) * graph_w)/(ts1 - ts0));
	    x_lu.put(time_strs.get(i), x); xs.add("" + x);
            // Hack to make 5 minute bin work...
            if (bin_in_ms == 5*MINUTES) { for (int j=1;j<=4;j++) { x_lu.put(mysdf.format(new Date(ts + j*60*1000L)), x); } }
	    ts += bin_in_ms;
	  }
          // Hack to make 5 minute bin work...
          if (bin_in_ms == 5*MINUTES) { for (int j=1;j<=4;j++) { x_lu.put(mysdf.format(new Date(ts + j*60L*1000L)), x); } }
	}
	public int    getInc() { return inc; }
	public String map(Bundle bundle) {
	  if (bundle.hasTime()) {
	    String formatted = mysdf.format(new Date(bundle.ts0()));
	    return "" + x_lu.get(formatted);
	  } else return null;
	}
	public String nextIndex(String index) { int i = xs.indexOf(index); 
	                                        if (i < 0)     return ""+graph_x_ins; if ((i+1) >= xs.size()) return ""+(graph_x_ins+graph_w);
	                                        return xs.get(i+1); }
	public String prevIndex(String index) { int i = xs.indexOf(index); 
	                                        if ((i-1) < 0) return ""+graph_x_ins;
	                                        return xs.get(i-1); }
      }


      /**
       * Construct the render context with the specified datasets and parameters. SUBDIVIDE
       *
       *@param id           render id - used to abort superceded renderings
       *@param bs           dataset
       *@param count_by     how to count the data
       *@param color_by     how to color the bars
       *@param chart_type   string denoting the chart type
       *@param mapper_str   string denoting mapping type
       *@param log_scale    flag for logarithmic scale
       *@param fixed        flag for fixed timeframe
       *@param aggregate    flag for aggregate (accumulative) graph
       *@param draw_markers flag for drawing time markers
       *@param w            width of rendering in pixels
       *@param h            height of rendering in pixels
       *@param previous     previous rendering - used to capture fixed positions
       */
      public RenderContext(short id, Bundles bs, String count_by, String color_by, String chart_type, String mapper_str,
                           boolean log_scale, boolean fixed, boolean aggregate, boolean draw_markers, 
			   int w, int h, RenderContext previous) {
        render_id = id; this.bs = bs; this.w = w; this.h = h; this.count_by = count_by; this.color_by = color_by; this.chart_type = chart_type; this.aggregate = aggregate; this.draw_markers = draw_markers;
        this.mapper_str = mapper_str; this.log_scale = log_scale; this.fixed = fixed;

	if (this.w <= 0) this.w = 1; if (this.h <= 0) this.h = 1;
	
	// Figure out the dimensions
	graph_x_ins = 60; graph_x_rgt = 5;  graph_w = w - (graph_x_ins + graph_x_rgt); 
	graph_y_ins = 30; graph_y_bot = 15; graph_h = h - (graph_y_ins + graph_y_bot);

	// Allocate the counter context
	counter_context = new BundlesCounterContext(bs, count_by, color_by);

        // Figure out the time range
        if (fixed && previous != null) { ts0 = previous.ts0; ts1 = previous.ts1; } else { ts0 = bs.ts0(); ts1 = (mapper_str.equals(TIMEBIN_CONTINUOUS_DUR) ? bs.ts1dur() : bs.ts1()); }
        if (ts0 == ts1) ts1 = ts0 + 1L;

        // Allocate the mapper based on the settings
        if        (mapper_str.equals(TIMEBIN_CONTINUOUS))     {
          mapper = new SimpleMapper();
        } else if (mapper_str.equals(TIMEBIN_CONTINUOUS_DUR)) {
          mapper = new DurationMapper();
        } else if (mapper_str.equals(TIMEBIN_CONTINUOUS_2))   {
          mapper = new SimpleMapperPix(2);
        } else if (mapper_str.equals(TIMEBIN_CONTINUOUS_4))   {
          mapper = new SimpleMapperPix(4);
        } else if (mapper_str.equals(TIMEBIN_CONTINUOUS_8))   {
          mapper = new SimpleMapperPix(8);
        } else if (mapper_str.equals(PERIOD_MINUTES) ||
                   mapper_str.equals(PERIOD_HOURS)   ||
                   mapper_str.equals(PERIOD_DAYS)    ||
                   mapper_str.equals(PERIOD_WEEKS))           {
          long periodicity_ms;
          if      (mapper_str.equals(PERIOD_MINUTES)) periodicity_ms = MINUTES;
          else if (mapper_str.equals(PERIOD_HOURS))   periodicity_ms = HOURS;
          else if (mapper_str.equals(PERIOD_DAYS))    periodicity_ms = DAYS;
          else if (mapper_str.equals(PERIOD_WEEKS))   periodicity_ms = WEEKS;
          else throw new RuntimeException("Don't Understand Mapper \"" + mapper_str + "\"");
          if (mapper_str.equals(PERIOD_WEEKS)) mapper = new WeeklyPeriodicityMapper(periodicity_ms);
          else                                 mapper = new PeriodicityMapper(periodicity_ms);
        } else if (mapper_str.equals(TIMEBIN_MINUTES)  ||
                   mapper_str.equals(TIMEBIN_5MINUTES) ||
                   mapper_str.equals(TIMEBIN_HOURS)    ||
                   mapper_str.equals(TIMEBIN_DAYS)     ||
		   mapper_str.equals(TIMEBIN_MONTHS)) {
          long time_bin_ms;
          if      (mapper_str.equals(TIMEBIN_MINUTES))  time_bin_ms = MINUTES;
          else if (mapper_str.equals(TIMEBIN_5MINUTES)) time_bin_ms = 5*MINUTES;
          else if (mapper_str.equals(TIMEBIN_HOURS))    time_bin_ms = HOURS;
          else if (mapper_str.equals(TIMEBIN_DAYS))     time_bin_ms = DAYS;
	  else if (mapper_str.equals(TIMEBIN_MONTHS))   time_bin_ms = MONTHS;
          else throw new RuntimeException("Don't Understand Mapper \"" + mapper_str + "\"");
          if (fixed == false) { ts0 = fixTS(true,  ts0, time_bin_ms);
	                        ts1 = fixTS(false, ts1, time_bin_ms); }
          mapper = new TimeBinMapper(time_bin_ms);
        } else                                                {
          System.err.println("Don't Understand Mapper \"" + mapper_str + "\"...  Defaulting To Simple!");
          mapper = new SimpleMapper();
        }

	// Go through the bundles adding them to the counter context
	Iterator<Tablet> it_t = bs.tabletIterator();
	while (it_t.hasNext()) {
	  Tablet tablet = it_t.next(); 
	  // Check to see if it has timestamps
	  if (tablet.hasTimeStamps() == false) { Iterator<Bundle> it = tablet.bundleIterator(); while (it.hasNext()) addToNoMappingSet(it.next()); }
	  // Check if it can count
	  boolean tablet_can_count = count_by.equals(BundlesDT.COUNT_BY_BUNS) || KeyMaker.tabletCompletesBlank(tablet, count_by);
	  // Go through the bundles
          Iterator<Bundle> it = tablet.bundleIterator();
	  while (it.hasNext() && currentRenderID() == getRenderID()) {
	    Bundle bundle = it.next();
	    String str = mapper.map(bundle);
	    if (str != null) {
	      if (str.indexOf(",") >= 0) {
	        StringTokenizer st = new StringTokenizer(str,",");
	        int i0 = Integer.parseInt(st.nextToken()), i1 = Integer.parseInt(st.nextToken());
		if ((i1 - i0 + 1) < 0) System.err.println("RTTimeComponent:  Negative Array Issue W/ String \"" + str + "\""); // DEBUG
		String strs[] = new String[i1 - i0 + 1];
	        for (int i=i0,j=0;i<=i1;i++,j++) {
                  if (tablet_can_count) counter_context.count(bundle,""+i,(double) (i1 - i0 + 1));
                  strs[j] = ""+i;
                }
                bundle_to_bins.put(bundle,strs);
	      } else                     {
	        if (tablet_can_count) counter_context.count(bundle,str); 
		String strs[] = new String[1]; strs[0] = str; bundle_to_bins.put(bundle,strs);
	      }
	    } else addToNoMappingSet(bundle);
	  }
        }

	// aggregate
	if (aggregate) {
	  List<Integer> sorted = new ArrayList<Integer>();
	  Iterator<String> it_str = counter_context.binIterator();
	  while (it_str.hasNext()) { sorted.add(Integer.parseInt(it_str.next())); }
	  Collections.sort(sorted);
	  for (int i=1;i<sorted.size();i++) {
	    String bin_p = "" + sorted.get(i-1), bin = "" + sorted.get(i);
	    counter_context.accumulate(bin_p, bin);
	  }
	}
      }

      /**
       * Draw the background for the time markers.
       *
       *@param g2d graphics drawing primitive
       */
      private void drawMarkersBG(Graphics2D g2d) { drawMarkersGeneric(g2d, true); }

      /**
       * Draw time markers generically
       *
       *@param g2d graphics drawing primitive
       *@param bg  indicates the background draw so that it doesn't obscure the foreground
       */
      private void drawMarkersGeneric(Graphics2D g2d, boolean bg) {
        Set<TimeMarker> markers = getRTParent().getTimeMarkers(ts0,ts1);
	if (markers != null) {
	  Iterator<TimeMarker> it    = markers.iterator();
	  int                  count = 0;
	  while (it.hasNext()) {
	    TimeMarker tm = it.next();
	    g2d.setColor(RTColorManager.getColor(tm.getDescription()));
	    if (tm.isTimeStamp()) {
	      int sx  = mapper.mapTS(tm.ts0());
	      if (bg) {
                g2d.drawLine(sx-5,graph_y_ins  ,sx,graph_y_ins+5);
                g2d.drawLine(sx+5,graph_y_ins  ,sx,graph_y_ins+5);
                g2d.drawLine(sx,  graph_y_ins+5,sx,graph_y_ins+graph_h);
	      } else {
	        String str = Utils.fitTxt(g2d, tm.getDescription(), graph_h);
	        Utils.drawRotatedString(g2d, str, sx, graph_y_ins+graph_h/2+Utils.txtW(g2d,str)/2);
              }
	    } else                {
              int sy = (graph_y_ins+graph_h/2) - (tm.getDescription().hashCode() & 0x00ffffff)%(graph_y_ins+graph_h/2) + 10;
	      int sx0 = mapper.mapTS(tm.ts0()), sx1 = mapper.mapTS(tm.ts1());
	      if (bg) {
                g2d.drawLine(sx0, sy-5, sx0,   sy+5); g2d.drawLine(sx1, sy-5, sx1,   sy+5);
	        g2d.drawLine(sx0, sy,   sx1,   sy);
	        g2d.drawLine(sx0, sy,   sx0+5, sy-5); g2d.drawLine(sx0, sy,   sx0+5, sy+5);
	        g2d.drawLine(sx1, sy,   sx1-5, sy-5); g2d.drawLine(sx1, sy,   sx1-5, sy+5);
		Composite composite = g2d.getComposite();
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
		g2d.fillRect(sx0, graph_y_ins, sx1 - sx0, graph_h);
		g2d.setComposite(composite);
	      } else {
	        if (sx0 < 0) sx0 = 0; if (sx1 > w) sx1 = w;
	        String str = Utils.fitTxt(g2d, tm.getDescription(), sx1-sx0);
                g2d.drawString(str, (sx0+sx1)/2 - Utils.txtW(g2d,str)/2, sy);
	      }
	    }
	  }
	}
      }

      /**
       * Draw the foreground for markers.
       *
       *@param g2d graphics drawing primitive
       */
      public void drawMarkersFG(Graphics2D g2d) { drawMarkersGeneric(g2d, false); }

      /**
       * Rendering - saved so it can be recalled easily (for repaints)
       */
      BufferedImage base_bi;

      /**
       * Render the shapes to an image for painting the component.
       *
       *@return rendered image
       */
      @Override
      public BufferedImage getBase() {
        if (base_bi != null) return base_bi;
	Graphics2D g2d = null; BufferedImage bi = null;
	try {
        bi = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) bi.getGraphics();
	g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	RTColorManager.renderVisualizationBackground(bi, g2d);

        mapper.gridLines(g2d);

	// Draw the time markers
        if (draw_markers && mapper.linear()) { drawMarkersBG(g2d); }

	// Draw the actual graph
	barChartColor(g2d, chart_type.equals(TYPE_BARCHART));
        if        (chart_type.equals(TYPE_BARCHART))       {
	} else if (chart_type.equals(TYPE_LINECHART))      { lineChartColor(g2d,false);
        } else if (chart_type.equals(TYPE_LINECHART_NORM)) { lineChartColor(g2d,true);
	} else throw new RuntimeException("Chart Type \"" + chart_type + "\" Unknown");

        int txt_h = Utils.txtH(g2d,"0");

	// Draw the scale labels on the y axis
        g2d.setColor(RTColorManager.getColor("label", "default"));
        String str;
        str = mapper.begLabel(); clearStr(g2d, str, graph_x_ins,                                     h - 2, RTColorManager.getColor("label", "defaultfg"), RTColorManager.getColor("label", "defaultbg"));
        str = mapper.endLabel(); clearStr(g2d, str, w - graph_x_rgt - Utils.txtW(g2d,str),           h - 2, RTColorManager.getColor("label", "defaultfg"), RTColorManager.getColor("label", "defaultbg"));
        str = mapper.midLabel(); clearStr(g2d, str, graph_x_ins + graph_w/2 - Utils.txtW(g2d,str)/2, h - 2, RTColorManager.getColor("label", "defaultfg"), RTColorManager.getColor("label", "defaultbg"));

	// Draw the time markers
        if (draw_markers && mapper.linear()) { drawMarkersFG(g2d); }

        } finally { if (g2d != null) g2d.dispose(); } // Clean-up

	return (base_bi = bi);
      }

      /**
       * Minimum height for logarithmic scale
       */
      final int   LOG1     = 6;

      /**
       * Constants for time-based calculations REFACTOR
       */
      final long  MINUTE   = 1000L * 60L,
                  TEN_MINS = 10L   * MINUTE,
                  HOUR     = 60L   * MINUTE,
		  DAY      = 24L   * HOUR,
		  WEEK     = 7L    * DAY,
		  MONTH    = 31L   * DAY,
		  YEAR     = 365L  * DAY;

      /**
       *
       */
      boolean draw_time_hints_err_f = true;

      /**
       * Time Hints
       */
      private void drawTimeHints(Graphics2D g2d) {
	  // Figure out the increments and formats
          Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	  cal.setTimeInMillis(ts0); cal.set(Calendar.MILLISECOND, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MINUTE, 0);
          long time_diff = ts1 - ts0;  SimpleDateFormat sdf = null; int add_to = -1, add = -1;
          if        (time_diff < HOUR)  { sdf = new SimpleDateFormat("HH:mm");       add_to = Calendar.MINUTE;       add = 10;
          } else if (time_diff < DAY)   { sdf = new SimpleDateFormat("HH:00");       add_to = Calendar.HOUR;         add =  1;
          } else if (time_diff < WEEK)  { cal.set(Calendar.HOUR_OF_DAY, 0);
	                                  sdf = new SimpleDateFormat("MM/dd - EEE"); add_to = Calendar.DAY_OF_MONTH; add = 1;
          } else if (time_diff < MONTH) { cal.set(Calendar.HOUR_OF_DAY, 0); 
	                                  sdf = new SimpleDateFormat("MM/dd");       add_to = Calendar.DAY_OF_MONTH; add = 1;
          } else if (time_diff < 5*YEAR){ cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.DAY_OF_MONTH, 1);
	                                  sdf = new SimpleDateFormat("MMM");         add_to = Calendar.MONTH;        add = 1;
          } else                        { cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.DAY_OF_MONTH, 1);
	                                  sdf = new SimpleDateFormat("MMM");         add_to = Calendar.MONTH;        add = 1;
					  if (draw_time_hints_err_f) {
					    System.err.println("drawTimeHints():  time_diff out of range (" + time_diff + ")");
					    draw_time_hints_err_f = false;
					  }
	  }
	  // Loop until we're out of space
          int last_sx = -5000, last_width = 0;
          sdf.setTimeZone(TimeZone.getTimeZone("GMT")); g2d.setColor(RTColorManager.getColor("label", "major"));
	  while (cal.getTimeInMillis() <= ts1) {
            cal.add(add_to, add);
	    long   ts  = cal.getTimeInMillis();
            int    sx  = (int) (graph_x_ins + ((ts - ts0) * graph_w)/(ts1 - ts0));
	    if (sx > (last_sx + last_width + 8)) {
	      String str = sdf.format(new Date(ts));
              g2d.setColor(RTColorManager.getColor("label", "major")); g2d.drawString(str, sx + 3, graph_y_ins - 2);   last_width = Utils.txtW(g2d,str);
	      g2d.setColor(RTColorManager.getColor("label", "minor")); g2d.drawLine(sx, 0, sx, graph_y_ins + graph_h); last_sx    = sx;
            }
	  }
      }

      /**
       * Colorized Bar Chart
       */
      private void barChartColor(Graphics2D g2d, boolean render) {
        int txt_h = Utils.txtH(g2d,"0");
        Iterator<String> it = counter_context.binIterator(); final double max = counter_context.totalMaximum();
        List<String> cbins = counter_context.getColorBinsSortedByCount();
        // Draw the scale
        String total_str = Utils.humanReadable((long) counter_context.totalMaximum());
        g2d.setColor(RTColorManager.getColor("axis",  "minor"));   g2d.drawLine(txt_h, graph_y_ins, graph_x_ins + graph_w, graph_y_ins);
        g2d.setColor(RTColorManager.getColor("label", "default")); g2d.drawString(total_str, txt_h + 2, graph_y_ins-1);
        Utils.drawRotatedString(g2d, count_by, txt_h, graph_y_ins + graph_h/2 + Utils.txtW(g2d,count_by)/2);
	if (log_scale) {
          long logger = 1; while (logger < max) {
            total_str = Utils.humanReadable(logger);
	    int log_h;
            if (logger == 1L) log_h = LOG1;
            else              log_h = (int) (LOG1 + (graph_h-LOG1) * Math.log(logger) / Math.log(max));
	    int label_y = graph_y_ins + graph_h - log_h;
            g2d.setColor(RTColorManager.getColor("axis",  "minor")); g2d.drawLine(txt_h, label_y, graph_x_ins + graph_w, label_y);
            g2d.setColor(RTColorManager.getColor("label", "log"));   g2d.drawString(total_str, txt_h+2, label_y-1);
	    logger = logger * 10;
	  }
	} else         {
          for (int i=1;i<4;i++) {
            long value  = (long) ((i*max)/4);
            total_str = Utils.humanReadable(value);
	    int scale_h = (int) (graph_h * value / max);
	    int label_y = graph_y_ins + graph_h - scale_h;
            g2d.setColor(RTColorManager.getColor("axis",  "minor"));   g2d.drawLine(txt_h, label_y, graph_x_ins + graph_w, label_y);
            g2d.setColor(RTColorManager.getColor("label", "linear"));  g2d.drawString(total_str, txt_h+2, label_y-1);
	  }
	}
        // Draw some time hints
        long time_diff = ts1 - ts0;
        if ((mapper instanceof PeriodicityMapper) == false && time_diff > TEN_MINS) {
	  drawTimeHints(g2d);
        }
        // Calculate the shapes (and render if appropriate)
	while (it.hasNext()) {
	  // Figure out the geometry and store the lookups
	  String bin = it.next(); 
          int    x   = Integer.parseInt(bin), h; double count = counter_context.total(bin);
          if (log_scale){
            if      (count <= 0.0) h = 0;
            else if (count <= 1.0) h = LOG1;
            else                   h = (int) (LOG1 + (graph_h-LOG1) * Math.log(count) / Math.log(max));
	  } else h = (int) (graph_h * count / max);
          int bar_w = Integer.parseInt(mapper.nextIndex(bin)) - x;
	  // Line2D.Double line = new Line2D.Double(x, graph_y_ins + graph_h, x, graph_y_ins + graph_h - h);
          Rectangle2D.Double rect = new Rectangle2D.Double(x, graph_y_ins + graph_h - h, bar_w, h);
          bin_to_shape.put(bin,rect);
          shape_to_bin.put(rect,bin);
	  all_shapes.add(rect);

          g2d.setColor(RTColorManager.getColor("set", "multi"));
          if (render) g2d.fill(rect);
 
          // Do the actual render
          if (render) {
	    int y_inc = graph_y_ins + graph_h;
            for (int i=cbins.size()-1;i>=0;i--) {
	      String cbin   = cbins.get(i);
	      double ctotal = counter_context.total(bin,cbin);
	      if (ctotal > 0.0) {
	        int sub_h = (int) ((ctotal * h)/counter_context.binColorTotal(bin));
	        if (sub_h > 0) {
	  	  g2d.setColor(RTColorManager.getColor(cbin));
                  g2d.fillRect(x, y_inc - sub_h, bar_w, sub_h);
		  y_inc = y_inc - sub_h;
	        }
	      }
	    }
          }
	}
      }
      /**
       * Colorized Line Chart
       */
      private void lineChartColor(Graphics2D g2d, boolean normalize) {
        double max = 0.0; if (normalize == false) max = counter_context.totalColorMaximum();
	Iterator<String> itcolor = counter_context.colorBinIterator();
	while (itcolor.hasNext()) {
	  String cbin = itcolor.next();
          if (normalize == true) max = counter_context.totalColorMaximum(cbin);
	  Iterator<String> itbin = counter_context.binIterator();
	  while (itbin.hasNext()) {
            String bin = itbin.next(); int x = Integer.parseInt(bin);
	    double ctotal   = counter_context.total(bin,cbin), 
	           ctotal_p = counter_context.total(mapper.prevIndex(bin), cbin), 
		   ctotal_n = counter_context.total(mapper.nextIndex(bin), cbin);
	    if (ctotal > 0.0) {
              int h, h_p, h_n;

	      if (log_scale) {
	        if (ctotal <= 1.0)   h   = LOG1;
		else                 h   = (int) (LOG1 + (graph_h - LOG1) * Math.log(ctotal) / Math.log(max));
	      } else               { h   = (int) (graph_h * ctotal / max); }

	      if (log_scale) {
	        if (ctotal_p <= 1.0) h_p = LOG1;
		else                 h_p = (int) (LOG1 + (graph_h - LOG1) * Math.log(ctotal_p) / Math.log(max));
	      } else               { h_p = (int) (graph_h * ctotal_p / max); }

	      if (log_scale) {
	        if (ctotal_n <= 1.0) h_n = LOG1;
		else                 h_n = (int) (LOG1 + (graph_h - LOG1) * Math.log(ctotal_n) / Math.log(max));
	      } else               { h_n = (int) (graph_h * ctotal_n / max); }

              g2d.setColor(RTColorManager.getColor(cbin));
	      g2d.fillOval(x - 1, graph_y_ins + graph_h - h - 1, 3, 3);
	      // g2d.drawLine(x, graph_y_ins + graph_h - h, Integer.parseInt(mapper.nextIndex(bin)), graph_y_ins + graph_h - h_n);
	      // if (ctotal_p == 0.0) g2d.drawLine(Integer.parseInt(mapper.prevIndex(bin)), graph_y_ins + graph_h - h_p, x, graph_y_ins + graph_h - h);
	    }
	  }
	}
      }

      /**
       *
       */
      public Set<Shape> allShapes() { return all_shapes; }
    }
  }
}

